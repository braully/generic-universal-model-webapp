package io.github.braully.web;

import io.github.braully.domain.Menu;
import io.github.braully.persistence.DAOJPA;
import io.github.braully.util.UtilValidation;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import javax.faces.application.FacesMessage;
import javax.faces.context.ExternalContext;
import javax.faces.context.FacesContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import lombok.Getter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Scope;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

/**
 *
 * @author raiz
 */
@Qualifier("genericMB")
@Component("genericMB")
@Scope("view")
//public class GenericMB extends CRUDGenericController {
public class GenericMB extends GenericControllerCRUD {

    public static final String NOME_PROPRIEDADE_INDICE_TAB = "tabIndex";

    @Getter
    @Resource
    protected DAOJPA dao;

    @Autowired(required = false)
    protected HttpSession session;
    @Autowired(required = false)
    protected HttpServletRequest request;

    protected int index;

    @PostConstruct
    @Override
    public void init() {
        super.init();
        /*
        try {
            String value = (String) this.getAtributeFromRequest(NOME_PROPRIEDADE_INDICE_TAB);
            if (value != null && !value.trim().isEmpty()) {
                Integer i = getInt(value);
                if (i != null && i > 0) {index = i;}
            }
        } catch (Exception e) {} 
         */
    }

    /*
        Codigo ficou duplicando com Generic Controller, inevitavel
     */
    public List<Menu> getUserMenus() {
        List<Menu> menusUser = (List<Menu>) this.getAtributeFromSession("menus");
        if (menusUser == null) {
            menusUser = this.menusUser();
            this.setAtributeInSession("menus", menusUser);
        }
        return menusUser;
    }

    @Override
    public Authentication getAuthentication() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return authentication;
    }

    protected DAOJPA getDao() {
        return dao;
    }

    @Override
    public int getIndex() {
        return index;
    }

    @Override
    public void setIndex(int index) {
        this.index = index;
    }

    public void reload() throws IOException {
        ExternalContext ec = FacesContext.getCurrentInstance().getExternalContext();
        ec.redirect(((HttpServletRequest) ec.getRequest()).getRequestURI());
    }

    @Override
    protected void addMensagem(String title, String detail, String type) {
        addMensagemJsf(title, detail, type);
    }

    @Override
    public void addAlerta(String msg) {
        addAlertaMensagemJsf(msg);
    }

    public static void addAlertaMensagemJsf(String string) {
        if (UtilValidation.isStringEmpty(string)) {
            return;
        }
        FacesContext facesContext = FacesContext.getCurrentInstance();
        if (facesContext != null) {
            facesContext.addMessage(null,
                    new FacesMessage(FacesMessage.SEVERITY_WARN, string, ""));
        }
    }

    @Override
    public void addErro(String msg, Exception e) {
        addErroMensagemJsf(msg, e);
    }

    @Override
    public void addErro(String msg) {
        addErroMensagemJsf(msg);
    }

    public static void addErroMensagemJsf(String string, Exception e) {
        if (UtilValidation.isStringEmpty(string)) {
            return;
        }
        String detalhe = null;
        if (e != null) {
            detalhe = tratarExecao(e);
        }
        FacesContext facesContext = FacesContext.getCurrentInstance();
        if (facesContext != null) {
            facesContext.addMessage(null,
                    new FacesMessage(FacesMessage.SEVERITY_ERROR, string, detalhe));
        }
    }

    public static void addErroMensagemJsf(String string) {
        if (UtilValidation.isStringEmpty(string)) {
            return;
        }
        FacesContext facesContext = FacesContext.getCurrentInstance();
        if (facesContext != null) {
            facesContext.addMessage(null,
                    new FacesMessage(FacesMessage.SEVERITY_ERROR, string, ""));
        }
    }

    @Override
    public void addMensagem(String mensagem) {
        addMensagemJsf(mensagem);
    }

    public static void addMensagemJsf(String string) {
        if (UtilValidation.isStringEmpty(string)) {
            return;
        }
        FacesContext facesContext = FacesContext.getCurrentInstance();
        if (facesContext != null) {
            facesContext.addMessage(null,
                    new FacesMessage(FacesMessage.SEVERITY_INFO, string, ""));
        }
    }

    public static void addMensagemJsf(String title, String detail, String type) {
        if (UtilValidation.isStringEmpty(title)) {
            return;
        }
        FacesContext facesContext = FacesContext.getCurrentInstance();

        if (facesContext != null) {
            FacesMessage.Severity stype = FacesMessage.SEVERITY_INFO;
            if (type != null) {
                if ("error".equalsIgnoreCase(type)) {
                    stype = FacesMessage.SEVERITY_ERROR;
                } else if ("warn".equalsIgnoreCase(type)) {
                    stype = FacesMessage.SEVERITY_WARN;
                }
            }
            facesContext.addMessage(null,
                    new FacesMessage(stype, title, detail));
        }
    }

    @Override
    protected HttpSession getCurrentSession() {
        HttpSession currentSession = this.session;
        try {
            Object session1 = null;
            if (currentSession == null) {
                session1 = FacesContext.getCurrentInstance()
                        .getExternalContext().getSession(true);
                currentSession = (HttpSession) session1;
            }
        } catch (Exception e) {
            log.debug("Fail on load curresnt session", e);
        }
        return currentSession;
    }

    @Override
    protected HttpServletRequest getCurrentRequest() {
        HttpServletRequest currentRequest = this.request;
        try {
            Object request = FacesContext.getCurrentInstance()
                    .getExternalContext().getRequest();
            currentRequest = (HttpServletRequest) request;
        } catch (Exception e) {
            log.debug("Fail on load curresnt request", e);
        }
        return currentRequest;
    }

    @Override
    protected Object getAtributeFromRequest(String nomeAtt) {
        Object ret = super.getAtributeFromRequest(nomeAtt);
        if (ret == null) {
            try {
                ret = getCurrentRequest().getParameter(nomeAtt);
                //(HttpServletRequest) FacesContext.getCurrentInstance().getExternalContext().getRequest();
            } catch (Exception e) {

            }
        }
        return ret;
    }

    protected void sendFile(InputStream stream, String nomeArquivo) {
        try {
            showFileToUser(stream, nomeArquivo);
        } catch (IOException ex) {
            throw new IllegalArgumentException("Falha ao enviar arquivo", ex);
        }
    }

    protected Object popSessionJSF(String nome) {
        return this.popSession(nome);
    }

    public static void sendFileToClient(File pdfAsFile, String string) {
        try {
            showFileToUser(pdfAsFile, string);
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
    }

    // Legacy
    private static final int DEFAULT_BUFFER_SIZE = 512;

    public synchronized static void enviarMultiplosArquivoZipado(String nomeArquivoFinal, String offsetDir, Iterable<String> caminhos) {
        ZipOutputStream output = null;
        byte[] buffer = new byte[DEFAULT_BUFFER_SIZE];

        try {
            FacesContext ctx = FacesContext.getCurrentInstance();
            HttpServletResponse response = (HttpServletResponse) ctx.getExternalContext().getResponse();
            response.setHeader("Content-Disposition", "attachment; filename=\"" + nomeArquivoFinal + ".zip\"");
            output = new ZipOutputStream(new BufferedOutputStream(response.getOutputStream(), DEFAULT_BUFFER_SIZE));
            response.setContentType("application/zip");

            for (String fileId : caminhos) {
                InputStream input = null;
                try {
                    String fileName = fileId;
                    File file = new File(offsetDir, fileId);
                    input = new BufferedInputStream(new FileInputStream(file), DEFAULT_BUFFER_SIZE);
                    output.putNextEntry(new ZipEntry(fileName));
                    for (int length = 0; (length = input.read(buffer)) > 0;) {
                        output.write(buffer, 0, length);
                    }
                    output.closeEntry();
                } catch (IOException ex) {
                    ex.printStackTrace();
                } finally {
                    if (input != null) {
                        try {
                            input.close();
                        } catch (IOException logOrIgnore) {
                            /**/ }
                    }
                }
            }
        } catch (IOException ex) {
            log.error(ex);
        } finally {
            if (output != null) {
                try {
                    output.close();
                } catch (IOException logOrIgnore) {
                    /**/ }
            }
        }
    }

    public synchronized static void showFileToUser(byte[] arquivo, String mimeType, String fileName)
            throws IOException {
        FacesContext ctx = FacesContext.getCurrentInstance();
        try {
            HttpServletResponse response = (HttpServletResponse) ctx.getExternalContext().getResponse();
            //            fileName = fileName.replaceAll(" ", "\\ ");
            String attach = "attachment; filename=\"" + fileName + "\"";
            response.setHeader("Content-disposition", attach);
            response.setContentType(mimeType);
            OutputStream out = response.getOutputStream();
            out.write(arquivo);
            out.flush();
        } catch (IOException e) {
            throw e;
        } finally {
            ctx.responseComplete();
        }
    }

    public static void showFileToUser(InputStream in, String mimeType, String fileName)
            throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        int dado;
        while ((dado = in.read()) != -1) {
            out.write(dado);
        }
        out.flush();
        byte[] dados = out.toByteArray();
        showFileToUser(dados, mimeType, fileName);
        in.close();
    }

    public static void showFileToUser(InputStream in, String string)
            throws IOException {
        String tipoArquivo = extenstionToMimeType(string);
        showFileToUser(in, tipoArquivo, string);
    }

    public static void showFileToUser(File outFile, String string)
            throws FileNotFoundException, IOException {
        showFileToUser(new FileInputStream(outFile), string);
    }

    public static void showFileToUser(byte[] dados, String string)
            throws IOException {
        showFileToUser(dados, extenstionToMimeType(string),
                string);
    }

    public static void sendFileToClient(byte[] pdfs, String boletospdf) {
        try {
            showFileToUser(pdfs, boletospdf);
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
    }
}
