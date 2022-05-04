package io.github.braully.web;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.braully.util.UtilParse;
import io.github.braully.util.UtilValidation;
import io.github.braully.util.logutil;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.activation.MimetypesFileTypeMap;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import lombok.Getter;
import lombok.Setter;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 *
 * @author braully
 */
public abstract class ControllerAbstract {

    protected static final Logger log = LogManager.getLogger(ControllerAbstract.class);

    public static final String ID = "id";
    public static final String IDS = "ids";
    public static final String OPERATION = "operation";
    public static final String OP = "op";

    protected final Map CACHE = new HashMap<>();
    protected static final MimetypesFileTypeMap fileTypeMap = new MimetypesFileTypeMap();

    @Getter
    @Setter
    protected String operation;

    protected abstract HttpServletRequest getCurrentRequest();

    protected abstract HttpSession getCurrentSession();

    protected Long parseLong(Object att) {
        Long ret = null;
        if (att instanceof Number) {
            if (att instanceof Long) {
                ret = (Long) att;
            } else {
                ret = ((Number) att).longValue();
            }
        } else if (att != null) {
            String parse = att.toString();
            ret = Long.parseLong(parse);
        }
        return ret;
    }

    protected List<Long> getIdsFromRequest() {
        List<Long> ids = null;
        try {
            List<String> parseList = UtilParse.parseList("" + this.getAtributeFromRequest(IDS), ",");
            ids = new ArrayList<>();
            for (String id : parseList) {
                if (UtilValidation.isStringValid(id)) {
                    ids.add(parseLong(id.strip()));
                }
            }
        } catch (Exception e) {
            logutil.error("fail on parse", e);
        }
        return ids;
    }

    protected Object getIdFromRequest() {
        return this.parseLong(this.getAtributeFromRequest(ID));
    }

    protected Object getAtributeFromRequest(String nomeAtt) {
        Object ret = null;
        try {
            HttpServletRequest currentRequest = getCurrentRequest();
            if (currentRequest == null) {
                logutil.debug("No Request Scope");
                return null;
            }
            currentRequest.getAttributeNames();
            Enumeration<String> parameterNames = currentRequest.getParameterNames();

            while (parameterNames.hasMoreElements()) {
                String paramName = parameterNames.nextElement();
                logutil.info(paramName);
                String[] paramValues = currentRequest.getParameterValues(paramName);
                for (int i = 0; i < paramValues.length; i++) {
                    String paramValue = paramValues[i];
                    logutil.info("\t" + paramValue);
                }
            }
            ret = currentRequest.getAttribute(nomeAtt);
            if (ret == null) {
                ret = currentRequest.getParameter(nomeAtt);
            }
        } catch (Exception e) {
            logutil.warn("Fail on load atrribute from request", e);
        }
        return ret;
    }

    protected Object getAtributeFromSession(String nomeAtt) {
        return this.getCurrentSession().getAttribute(nomeAtt);
    }

    protected Object setAtributeInSession(String nomeAtt, Object obj) {
        this.getCurrentSession().setAttribute(nomeAtt, obj);
        return obj;
    }

    protected void removeAtributeInSession(String nomeAtt) {
        this.getCurrentSession().removeAttribute(nomeAtt);
    }

    protected Object popSession(String nomeBean) {
        Object ret = null;
        ret = this.getAtributeFromSession(nomeBean);
        if (ret != null) {
            removeAtributeInSession(nomeBean);
        }
        return ret;
    }

    protected Object pushSession(String nomeBean, Object obj) {
        Object ret = obj;
        setAtributeInSession(nomeBean, obj);
        return ret;
    }

    public Authentication getAuthentication() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return authentication;
    }

    public void loadOperationFromRequestIfPresent() {
        Object op = this.getAtributeFromRequest(OPERATION);
        if (op != null) {
            this.operation = op.toString();
        } else if ((op = this.getAtributeFromRequest(OP))
                != null) {
            this.operation = op.toString();
        }
    }

    public List convertJsonToObjects(String json, Class classe) {
        List result = null;
        if (UtilValidation.isStringValid(json)) {
            try {
                logutil.debug("JSON Received");
                logutil.debug(json);
                ObjectMapper objectMapper = new ObjectMapper();
                objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
                result = objectMapper.readValue(json, objectMapper.getTypeFactory().constructCollectionType(List.class, classe));
            } catch (Exception ex) {
                throw new IllegalStateException("Falha ao deserializar json: " + json, ex);
            }
        }
        return result;
    }

    protected List getListFromCache(String param) {
        return (List) CACHE.get(param);
    }

    protected ControllerAbstractCRUD getCrontrollerCrudFromCache(String alias) {
        return (ControllerAbstractCRUD) CACHE.get(alias);
    }

    public static String tratarExecao(Exception e) {
        String message = "";
        if (e != null) {
            message = e.getMessage();
        }
        return message;
    }

    public static String extenstionToMimeType(String filext) {
        String ret = null;
        if (UtilValidation.isStringEmpty(filext)) {
            return ret;
        }
        ret = fileTypeMap.getContentType(filext);
        if (UtilValidation.isStringEmpty(ret)) {
            if (filext.contains(".")) {
                ret = fileTypeMap.getContentType("a" + filext);
            } else {
                ret = fileTypeMap.getContentType("a." + filext);
            }
        }
        return ret;
    }

}
