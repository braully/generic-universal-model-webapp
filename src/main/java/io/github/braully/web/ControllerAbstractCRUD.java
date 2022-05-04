package io.github.braully.web;

import io.github.braully.constant.Attr;
import io.github.braully.domain.IOrganiztionEntityDependent;
import io.github.braully.domain.Menu;
import io.github.braully.persistence.ILightRemoveEntity;
import io.github.braully.domain.Organization;
import io.github.braully.domain.OrganizationRole;
import io.github.braully.domain.Role;
import io.github.braully.domain.SysRole;
import io.github.braully.persistence.DAOJPA;
import io.github.braully.persistence.IEntity;
import io.github.braully.persistence.IEntityStatus;
import io.github.braully.persistence.Query;
import io.github.braully.util.UtilCollection;
import io.github.braully.util.UtilReflection;
import io.github.braully.util.UtilValidation;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import javax.annotation.PostConstruct;
import javax.persistence.EntityNotFoundException;
import javax.persistence.NoResultException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.exception.ConstraintViolationException;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 *
 * @author braully
 */
public abstract class ControllerAbstractCRUD<T extends IEntity> extends ControllerAbstract {

    /* */
    public static final String ID_USER = "user_id";
    public static final String NAME_PROP_INDEX_TAB = "tabIndex";
    public static final String USER_NAME_VARIABLE = "username";

    protected T entity;
    protected List<T> allEntities;
    protected Class<T> entityClass;
    protected Query queryResult;
    @Getter
    @Setter
    protected String searchString;
    @Getter
    protected Map<String, Object> extraSearchParams = new HashMap<>();
    @Getter
    protected Map<String, Object> extraProperties = new HashMap<>();
    @Getter
    @Setter
    protected int index;

    protected ExposedRestfulCRUD exposed;

    protected boolean validPreSave;

    protected abstract DAOJPA getDao();

    /*
    
     */
    public ControllerAbstractCRUD() {

    }

    public ControllerAbstractCRUD(Class<T> entidadeClass) {
        this.entityClass = entidadeClass;
    }

    @PostConstruct
    public void init() {
    }

    public List values(String entityNameField) {
        List tmp = (List) CACHE.get(entityNameField);
        try {
            if (tmp == null) {
                Field field = exposed.getExposedEntityField(entityNameField);
                Class<?> type = null;
                if (field != null) {
                    type = field.getType();
                } else {
                    type = Class.forName(entityNameField);
                }
                tmp = new ArrayList();
                if (IEntity.class.isAssignableFrom(type)) {
                    List loadCollection = this.getDao().search(type);
                    if (loadCollection != null) {
                        tmp.addAll(loadCollection);
                    }
                } else if (Enum.class.isAssignableFrom(type)) {
                    for (Object e : Arrays.asList(type.getEnumConstants())) {
                        tmp.add(e);
                    }
                }
                CACHE.put(entityNameField, tmp);
            }
        } catch (Exception ex) {
            log.error("error values dynamic", ex);
        }
        return tmp;
    }

    public Class<T> getEntityClass() {
        try {
            if (entityClass == null) {
                entityClass = UtilReflection.getGenericTypeArgument(this.getClass(), 0);
            }
        } catch (Exception e) {
            log.debug("fail on indentify class type");
            log.trace("fail on indentify class type", e);
        }
        return entityClass;
    }

    public T getEntity() {
        if (entity == null) {
            newEntity();
        }
        return entity;
    }

    public void newEntity() {
        entity = UtilReflection.createInstance(this.getEntityClass());
    }

    public void findSilent() {
        this.find();
    }

    public ControllerAbstractCRUD<T> find() {
        try {
            this.queryResult = this.getDao()
                    .query(this.getEntityClass()).where(getFindExtraSearchParams());
//                    .genericFulltTextSearchPagedQuery(this.getEntityClass(),
//                            getFindExtraSearchString(), getFindExtraSearchParams());
            log.info("query executed");
        } catch (Exception e) {
            log.error("Fail on query generic", e);
        }
        return this;
    }

    protected Query findQuery() {
        Query ret = this.getDao()
                .query(this.getEntityClass()).where(getFindExtraSearchParams());
        return ret;
    }

    protected String getFindExtraSearchString() {
        return this.searchString;
    }

    protected Map<String, Object> getFindExtraSearchParams() {
        return this.extraSearchParams;
    }

    public void save() {
        if (this.save(entity)) {
            /*this.loadAllEntities();*/
            this.clearCacheLoadAllEntities();
            this.newEntity();
        }
    }

    public boolean saveSilent(IEntity entity) {
        return this.save(entity, null, "Verifique todos os campos obrigatórios");
    }

    public boolean saveSilent() {
        return this.saveSilent(entity);
    }

    public void block(IEntityStatus entidadeStatus) {
        try {
//            entidadeStatus.block();
            if (this.save(entidadeStatus)) {
                this.loadAllEntities();
                this.newEntity();
            }

        } catch (RuntimeException e) {
        }
    }

    public void activate(IEntityStatus entidadeStatus) {
        try {
//            entidadeStatus.activate();
            if (this.save(entidadeStatus)) {
                this.loadAllEntities();
                this.newEntity();
            }

        } catch (RuntimeException e) {
        }
    }

    public List<T> getAllEntities() {
        try {
            if (allEntities == null || allEntities.isEmpty()) {
                loadAllEntitiesList();
            }
        } catch (Exception e) {
            log.error("Falha ao carregar entidades", e);
        }
        return allEntities;
    }

    public void clearCacheLoadAllEntities() {
        //just clear cache
        this.clearCache();
        this.allEntities = null;
        this.loadAllEntities();
    }

    public void clearCache() {
        //just clear cache
        this.CACHE.clear();
    }

    public void clearCacheFind() {
        //just clear cache
        this.clearCache();
        this.find();
    }

    public void lodEntity(Long id) {
        this.entity = this.getDao().get(this.entityClass, id);
    }

    public ControllerAbstractCRUD<T> lodEntity(IEntity ientity) {
        if (ientity != null && ientity.isPersisted()) {
            //this.entity = this.getGenericoBC().get((Class<T>) ientity.getClass(), ientity.getId());
            this.entity = this.getDao().get(this.entityClass, ientity.getId());
        }
        return this;
    }

    public void loadAllEntities() {
        this.loadAllEntitiesPaged();
        //just clear cache
        this.allEntities = null;
    }

    public void loadAllEntitiesList() {
        try {
            this.allEntities = this.getDao().search(this.getEntityClass());
            boolean sortIfComparable = UtilCollection.sortIfComparable(this.allEntities);
        } catch (Exception e) {
            log.error("Falha ao carregar entidades", e);
        }
    }

    public void loadAllEntitiesPaged() {
        Class eclass = this.getEntityClass();
        try {
            if (IOrganiztionEntityDependent.class.isAssignableFrom(eclass)) {
                Organization org = this.getMonoPermitedInstituicao();
                String attrOrganization = "organization";
                if (org != null) {
                    try {
                        Method method = eclass.getMethod("getOrganization");
                        if (method != null) {
                            Attr annotation = method.getAnnotation(Attr.class);
                            if (annotation != null && annotation.name().equals("organization")) {
                                attrOrganization = annotation.val();
                            }
                        }
                    } catch (Exception e) {
                        log.debug("no default propertie getOrganization", e);
                    }
                    this.extraSearchParams.put(attrOrganization, org);
                    find();
                    return;
                }
            }
            queryResult = this.getDao().query(eclass);
        } catch (Exception e) {
            addErro("Falha ao carregar entidades", e);
            log.error("Falha ao carregar entidades", e);
        }
    }

    public String removeEntityAndRedirect(String redirect) {
        try {
            if (this.remove(this.entity, "Removido com sucesso", "Falha ao remover")) {
                this.clearCacheLoadAllEntities();
                this.newEntity();
                return "redirect:" + redirect;
            }
        } catch (Exception e) {
            log.error("", e);
            addErro("Falha ao tentar remover: " + this.entity, e);

        }
        return null;
    }

    public void removeEntity() {
        this.remove(this.entity);
    }

    public void remove(T entidade) {
        if (this.remove(entidade, "Removido com sucesso", "Falha ao remover")) {
            this.loadAllEntities();
            this.newEntity();
        }
    }

    public boolean remove(IEntity entidade, String mensagemSuc, String mensagemErro) {
        boolean ret = true;
        String strEntidade = "" + entidade;
        try {
            ret = ControllerAbstractCRUD.this.isValidPreSave(entidade);
            if (ret) {
                this.getDao().delete(entidade);
                addMensagem(mensagemSuc + ": " + strEntidade);
            } else {
                addAlerta(mensagemErro + ": " + strEntidade);
            }
        } catch (DataAccessException | ConstraintViolationException dex) {
            log.debug("Fail on hard delete, try soft if possible");
            try {
                if (entidade instanceof ILightRemoveEntity) {
                    this.getDao().deleteSoft((ILightRemoveEntity) entidade);
                    addMensagem(mensagemSuc + ": " + strEntidade);
                } else {
                    addErro(mensagemErro + ": " + strEntidade, dex);
                    ret = false;
                }
            } catch (RuntimeException e) {
                addErro(mensagemErro + ": " + strEntidade, e);
                ret = false;
            }
        } catch (RuntimeException e) {
            addErro(mensagemErro + ": " + strEntidade, e);
            ret = false;
            log.error("erro", e);
        }
        return ret;
    }

    public boolean validatePreRemove(IEntity entidade) {
        return true;
    }

    public void setEntity(T entidade) {
        this.entity = entidade;
    }

    public List subAllEntities(String attribute) {
        String nameSub = "subAll-" + attribute;
        List list = this.getListFromCache(nameSub);
        if (list == null || list.isEmpty()) {

            List<T> collections = this.getAllEntities();
            Collection sets = subAttributeCollection(collections, attribute);
            list = new ArrayList(sets);
            this.CACHE.put(nameSub, list);
        }
        return list;
    }

    public Collection subAttributeCollection(List<T> collections, String attribute) {
        Set sets = new LinkedHashSet();
        for (IEntity e : collections) {
            try {
                Object property = UtilReflection.getProperty(e, attribute);
                Object var = property;
                if (property != null) {
                    try {
                        if (property instanceof IEntity) {
                            var = this.getDao().get(property.getClass(), ((IEntity) property).getId());
                        }
                    } catch (Exception ex) {
                        log.debug("Fail on load fetch subentities", ex);
                    }
                    sets.add(var);
                }
            } catch (Exception ex) {
                log.debug("Fail on load subentities", ex);
            }
        }
        return sets;
    }

    public List subEntities(String attribute) {
        String nameSub = "sub-" + attribute;
        List list = this.getListFromCache(nameSub);
        if (list == null || list.isEmpty()) {
            Collection sets = subAttributeCollection(this.getEntities(), attribute);
            list = new ArrayList(sets);
            this.CACHE.put(nameSub, list);
        }
        return list;
    }

    public List<T> getEntities() {
        return getQueryResult().getResult();
    }

    public boolean save(IEntity entidade) {
        return this.save(entidade, "Salvo com sucesso", "Verifique todos os campos obrigatórios");
    }

    public boolean save(IEntity entidade, String mensagemSuc, String mensagemErro) {
        boolean ret = true;
        try {
            ret = ControllerAbstractCRUD.this.isValidPreSave(entidade);
            if (ret) {
                this.getDao().save(entidade);
                addMensagem(mensagemSuc);
            } else {
                addAlerta(mensagemErro);
            }
        } catch (RuntimeException e) {
            addErro("Falha ao salvar: " + e.getMessage(), e);
            ret = false;
            log.error("erro", e);
        }
        return ret;
    }

    public boolean isValidPreSave() {
        this.validPreSave = true;
        validatePreSave(entity);
        return this.validPreSave;
    }

    public boolean isValidPreSave(IEntity entidade) {
        this.validPreSave = true;
        validatePreSave(entidade);
        return this.validPreSave;
    }

    /* Alias for crud */
    public <E extends IEntity> ControllerAbstractCRUD<E> crud(Class<E> classEntity) {
        if (classEntity == null) {
            return null;
        }
        ControllerAbstractCRUD<E> crud = crud(classEntity.getSimpleName());
        if (crud == null) {
            crud = crudBuild(classEntity.getSimpleName().toLowerCase(), classEntity);
        }
        return crud;
    }

    /**
     * Alias para o metodo crud(Class)
     *
     * @param <E>
     * @param classEntit
     * @return
     */
    public <E extends IEntity> ControllerAbstractCRUD<E> c(Class<E> classEntit) {
        return crud(classEntit);
    }

    public ControllerAbstractCRUD c(String entityName) {
        return crud(entityName);
    }

    public ControllerAbstractCRUD crud(String crudPath) {
        return crud(null, crudPath);
    }

    public ControllerAbstractCRUD crud(String alias, Class entityclass) {
        return crud(alias, entityclass.getSimpleName());
    }

    public ControllerAbstractCRUD crud(String alias, String entityCrudPath) {
        String entityName = entityCrudPath;
        int indexOf = entityCrudPath.indexOf('.');
        String subpath = null;
        if (indexOf > 0) {
            entityName = entityCrudPath.substring(0, indexOf);
            subpath = entityCrudPath.substring(indexOf + 1, entityCrudPath.length());
        }
        String entityNameLower = entityName.toLowerCase();
        if (UtilValidation.isStringEmpty(alias)) {
            alias = entityNameLower;
        }
        ControllerAbstractCRUD tmp = getCrontrollerCrudFromCache(alias);
        if (tmp == null) {
            DescriptorWebExposedEntity desc = exposed.getExposedEntity(entityName);
            Class classExposed = null;
            if (desc != null) {
                classExposed = desc.getClassExposed();
                tmp = crudBuild(alias, classExposed);
            } else {
                log.debug("Not found class: " + entityName + " trying atribute");
                Field field = UtilReflection.getDeclaredFieldAscending(this.getEntityClass(), entityName);
                if (field != null) {
                    Class<?> type = field.getType();
                    tmp = crudBuild(entityName, type);
                }
            }
        }
        if (tmp != null) {
            if (subpath != null) {
                return tmp.crud(subpath);
            }
        } else {
            log.debug("fail on create crud: " + entityCrudPath);
        }
        return tmp;
    }

    protected ControllerAbstractCRUD crudBuild(String alias, Class classExposed) {
        if (classExposed == null) {
            return null;
        }
        ControllerAbstractCRUD tmp = new ControllerAbstractCRUD(classExposed) {
            public DAOJPA getDao() {
                return ControllerAbstractCRUD.this.getDao();
            }

            protected HttpSession getCurrentSession() {
                return ControllerAbstractCRUD.this.getCurrentSession();
            }

            protected HttpServletRequest getCurrentRequest() {
                return ControllerAbstractCRUD.this.getCurrentRequest();
            }

            @Override
            protected void addMensagem(String title, String detail, String type) {
                ControllerAbstractCRUD.this.addMensagem(title, detail, type);
            }
        };
        CACHE.put(alias, tmp);
        return tmp;
    }

    public ControllerAbstractCRUD<T> crudType(String type) {
        if (type == null) {
            return null;
        }
        String typeLow = type.toLowerCase();
        if (!CACHE.containsKey(typeLow)) {
            Class<T> entityClass1 = ControllerAbstractCRUD.this.getEntityClass();
            ControllerAbstractCRUD tmpCrud = new ControllerAbstractCRUD(entityClass1) {
                final String localtype = typeLow;
                final String normaltype = type;

                public DAOJPA getDao() {
                    return ControllerAbstractCRUD.this.getDao();
                }

                protected HttpServletRequest getCurrentRequest() {
                    return ControllerAbstractCRUD.this.getCurrentRequest();
                }

                protected HttpSession getCurrentSession() {
                    return ControllerAbstractCRUD.this.getCurrentSession();
                }

                @Override
                public void newEntity() {
                    super.newEntity();
                    try {
                        UtilReflection.setProperty(this.entity, "type", normaltype);
                    } catch (IllegalAccessException | InvocationTargetException | NoSuchMethodException ex) {
                        log.debug("Fail on set type on variable", ex);
                    }
                }

                @Override
                public void loadAllEntitiesPaged() {
                    this.find();
                }

                @Override
                public Map getFindExtraSearchParams() {
                    Map extraParams = super.getFindExtraSearchParams();
                    extraParams.put("type", localtype);
                    //extraParams.put("ORDER BY", "id DESC");
                    return extraParams;
                }
            };
            //TODO: Do map case insenstive on key
            CACHE.put(typeLow, tmpCrud);
        }
        return crud(typeLow);
    }

    public void loadCrudPathCascade(String crudPath) {
        String attributeName = crudPath;
        int indexOf = crudPath.indexOf('.');
        String subpath = null;
        ControllerAbstractCRUD crud = null;
        if (indexOf > 0) {
            attributeName = crudPath.substring(0, indexOf);
            subpath = crudPath.substring(indexOf + 1, crudPath.length());
        }
        Field field = UtilReflection.getDeclaredFieldAscending(this.getEntityClass(), attributeName);
        try {
            IEntity attrValue = (IEntity) UtilReflection.getProperty(this.getEntity(), attributeName);
            if (attrValue != null) {
                if (attrValue.isPersisted()) {
                    //Conflito Aluno e Partner, diferente CRUDs, feito para unificar.
                    crud = this.crud(attributeName.toLowerCase(), (Class<? extends IEntity>) field.getType()).lodEntity(attrValue);
                }
            } //Renovar a entidade para não reutilizar uma entidade cacheada de casos anteriores
            else {
                this.crud(attributeName.toLowerCase(), (Class<? extends IEntity>) field.getType()).newEntity();
            }
            if (subpath != null && crud != null) {
                crud.loadCrudPathCascade(subpath);
            }
        } catch (Exception ex) {
            log.error("Fail on load cascade", ex);
        }
    }

    public void validatePreSave(IEntity entidade) {

    }

    protected void validatePreSave(boolean valid, String msgValid) {
        if (!valid) {
            this.addAlerta(msgValid);
            this.validPreSave = false;
        }
    }

    protected boolean validateNotEmpty(Object object, String campo, String msgValid) {
        boolean ret = true;
        try {
            Object property = UtilReflection.getProperty(object, campo);
            ret = property != null;
            if (ret) {
                if (property instanceof String) {
                    ret = ret && !UtilValidation.isStringEmpty((String) property);
                } else if (property instanceof Collection) {
                    ret = ret && !((Collection) property).isEmpty();
                }
            }
        } catch (Exception ex) {
            ret = false;
            log.warn("Fail on property", ex);
        }
        validatePreSave(ret, msgValid);
        return ret;
    }

    public int getTotalEntities() {
        return queryResult.getCount();
    }

    public Query getQueryResult() {
        if (queryResult == null) {
            loadAllEntitiesPaged();
        }
        return queryResult;
    }

    public boolean hasNextPageQueryResult() {
        return this.getQueryResult().hasNext();
    }

    public void nextPageQueryResult() {
        try {
            if (hasNextPageQueryResult()) {
                this.getQueryResult().next();
                this.getDao().executeQuery(this.getQueryResult());
            }
        } catch (Exception e) {
            log.error("Fail in next page query", e);
        }
    }

    public void previousPageQueryResult() {
        try {
            if (hasPreviousPageQueryResult()) {
                this.getQueryResult().previous();
                this.getDao().executeQuery(this.getQueryResult());
            }
        } catch (Exception e) {
            log.error("Fail in next page query", e);
        }
    }

    public List<Integer> getWindowPages() {
        return this.getQueryResult().getWindowPages();
    }

    public boolean hasWindowPageQueryResult() {
        return this.getQueryResult().hasWindowPageQueryResult();
    }

    public boolean hasPreviousPageQueryResult() {
        return this.getQueryResult().hasPrevious();
    }

    public void paginate(Integer toPage) {
        try {
            if (toPage == null || toPage < 0 || toPage > getTotalPages()) {
                String msg = "Page out of range: " + toPage + " 0/" + getTotalPages();
                addErro(msg);
                log.error(msg);
                return;
            }
            this.getQueryResult().setPage(toPage);
            this.getDao().executeQuery(this.getQueryResult());
        } catch (Exception e) {
            log.error("Fail in next page query", e);
        }
    }

    public int getWindowIni() {
        return this.getQueryResult().getWindowIni();
    }

    public int getWindowEnd() {
        return this.getQueryResult().getWindowEnd();
    }

    public int getCurrentPage() {
        return this.getQueryResult().getPage();
    }

    public int getTotalPages() {
        return this.getQueryResult().getTotalPages();
    }

    protected Integer getInt(String value) {
        Integer i = null;
        try {
            i = Integer.parseInt(value.trim().replaceAll("\\D+", ""));
        } catch (Exception e) {

        }
        return i;
    }

    public Locale getUserLocale() {
        Locale userLocale = null;
        try {
            //http://www.java2s.com/Tutorial/Java/0400__Servlet/LocaleSessionServlet.htm
            userLocale = (Locale) this.getCurrentSession().getAttribute("userLocale");
        } catch (Exception e) {

        }
        if (userLocale == null) {
            userLocale = Locale.getDefault();
        }
        return userLocale;
    }

    //For future use
    @Deprecated
    public List<Map<String, String>> getMessages() {
        List<Map<String, String>> mensagens = (List<Map<String, String>>) CACHE.get("messages");
        if (mensagens == null) {
            mensagens = new ArrayList<>();
            CACHE.put("messages", mensagens);
        }
        //Clear previous?
        //mensagens.clear();
        return mensagens;
    }

    //protected abstract void addMensagem(String title, String detail, String type) ;
    protected void addMensagem(String title, String detail, String type) {
        //Map.of("title", title, "detail", detail, "type", type) error on any atrib null
        this.getMessages().add(UtilCollection.mapOf("title", title, "detail", detail, "type", type));
    }

    //TODO: Setar a mensagem em algum paramêtro da resposta
    public void addMensagem(String mensagem) {
        this.addMensagem(mensagem, null, "info");
    }

    public void addErro(String msg) {
        this.addMensagem(msg, null, "error");
    }

    public void addErro(String msg, Exception e) {
        this.addMensagem(msg, exceptionToDetailMsg(e), "error");
    }

    public void add(String msg) {
        this.addMensagem(msg, null, "info");
    }

    public void addAlerta(String msg) {
        this.addMensagem(msg, null, "warn");
    }

    public void loadEntityFromRequestIfPresent() {
        T entityFromRequest = this.getEntityFromRequest();
        if (entityFromRequest != null) {
            log.debug("Entitty Loade From Request");
            this.entity = entityFromRequest;
        }
    }

    public void loadEntitiesFromRequestIfPresent() {
        List<T> entitiesFromRequest = this.getEntitiesFromRequest();
        if (entitiesFromRequest != null) {
            log.debug("Entities Loaded From Request");
            this.allEntities = entitiesFromRequest;
        }
    }

    public List<T> getEntitiesFromRequest() {
        List<T> ret = null;
        List<Long> idsFromRequest = getIdsFromRequest();
        if (idsFromRequest != null) {
            ret = new ArrayList<>();
            for (Long id : idsFromRequest) {
                try {
                    T loadEntity = this.getDao().get(this.getEntityClass(), id);
                    ret.add(loadEntity);
                } catch (Exception e) {
                    log.debug("Falha ao carregar entidade do request", e);
                }
            }
        }
        return ret;
    }

    public T getEntityFromRequest() {
        //Object atributeFromRequest = this.getAtributeFromRequest(ID);
        T ret = null;
        Object idFromRequest = getIdFromRequest();
        if (idFromRequest != null) {
            try {
                ret = this.getDao().get(this.getEntityClass(), idFromRequest);
            } catch (Exception e) {
                log.debug("Falha ao carregar entidade do request", e);
            }
        }
        return ret;
    }

    protected <U extends IEntity> U loadEntityFromRequestIfPresent(Class<U> classe, String parameter) {
        U ret = null;
        Long id = this.parseLong(this.getAtributeFromRequest(parameter));
        if (id != null) {
            try {
                ret = this.getDao().get(classe, id);
            } catch (Exception e) {
                log.debug("Falha ao carregar entidade do request", e);
            }
        }
        return ret;
    }

    public String getUserName() {
        String username = null;
        username = (String) this.getAtributeFromSession(USER_NAME_VARIABLE);
        if (username == null) {
            try {
                username = this.getAuthentication().getName();
                this.setAtributeInSession(USER_NAME_VARIABLE, username);
            } catch (Exception e) {

            }
        }
        return username;
    }

    protected List<Role> getUsuarioLogadoPapelGlobal() {
        List<Role> papeis = getListFromCache("getUsuarioLogadoPapelGlobal");

        if (papeis == null) {
            try {
//                Object idUsuario = this.getUserId();
//                papeis = this.getDao().queryList(
//                        "SELECT o FROM UserLogin u "
//                        + "JOIN u.roles o "
//                        + "WHERE u.id = ?1", idUsuario
//                );
            } catch (Exception e) {
                log.error("Falha ao obter usuario logado", e);
            }
            CACHE.put("getUsuarioLogadoPapelGlobal", papeis);
        }
        return papeis;
    }

    public boolean isMonoInstituicao() {
        Boolean permGlobal = (Boolean) this.getAtributeFromSession("USUARIO_MONO_INSTITUICAO");
        try {
            if (permGlobal == null) {
                if (permGlobal == null) {
                    permGlobal = this.getPermitedOrganizations().size() <= 1;
                    this.setAtributeInSession("USUARIO_MONO_INSTITUICAO", permGlobal);
                }
            }
        } catch (Exception e) {

        }
        if (permGlobal == null) {
            permGlobal = true;
        }
        return permGlobal;
    }

    public boolean isUsuarioPermissaoGlobal() {
        Boolean permGlobal = (Boolean) this.getAtributeFromSession("USUARIO_PERMISSAO_GLOBAL");
        try {
            if (permGlobal == null) {
                permGlobal = !this.getUsuarioLogadoPapelGlobal().isEmpty();
                this.setAtributeInSession("USUARIO_PERMISSAO_GLOBAL", permGlobal);
            }
        } catch (Exception e) {
            log.debug("Fail on check perm globla", e);
        }
        if (permGlobal == null) {
            permGlobal = false;
        }
        return permGlobal;
    }

    public List<Organization> getPermitedOrganizations() {
        return getUsuarioLogadoInstiuicao();
    }

    protected List<Organization> getUsuarioLogadoInstiuicao() {
        List<Organization> papeis = (List<Organization>) CACHE.get("getUsuarioLogadoInstiuicao");
        if (papeis == null) {
            try {
                if (isUsuarioPermissaoGlobal()) {
                    papeis = this.getDao().search(Organization.class);
                } else {
//                    Object idUsuario = this.getUserId();
//                    papeis = this.getDao().queryList(
//                            "SELECT DISTINCT o FROM UserLogin u "
//                            + "JOIN u.organizationRole org "
//                            + "JOIN org.organization o "
//                            + "WHERE u.id = ?1", idUsuario
//                    );
                }
            } catch (Exception e) {
                log.error("Falha ao obter usuario logado", e);
            }
            CACHE.put("getUsuarioLogadoInstiuicao", papeis);
        }
        return papeis;
    }

    protected List<OrganizationRole> getUsuarioLogadoPapeisInstiuicao() {
        List<OrganizationRole> papeis = getListFromCache("getUsuarioLogadoPapeisInstiuicao");
        if (papeis == null) {;
            try {
//                Object idUsuario = this.getUserId();
//                papeis = this.getDao().query("SELECT o FROM UserLogin u "
//                        + "JOIN u.organizationRole o "
//                        + "WHERE u.id = ?1", idUsuario);
            } catch (Exception e) {
                log.error("Falha ao obter usuario logado", e);
            }
            CACHE.put("getUsuarioLogadoPapeisInstiuicao", papeis);
        }
        return papeis;
    }

    private static final int DEFAULT_BUFFER_SIZE = 512;

    public synchronized static void enviarMultiplosArquivoZipado(String nomeArquivoFinal, String offsetDir, Iterable<String> caminhos) {
        ZipOutputStream output = null;
        byte[] buffer = new byte[DEFAULT_BUFFER_SIZE];

        try {
//            FacesContext ctx = FacesContext.getCurrentInstance();
            HttpServletResponse response = null;//(HttpServletResponse) ctx.getExternalContext().getResponse();
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
                    log.error("", ex);
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
            log.error("", ex);
        } finally {
            if (output != null) {
                try {
                    output.close();
                } catch (IOException logOrIgnore) {
                    /**/ }
            }
        }
    }

    public Organization getMonoPermitedInstituicao() {
        Organization ret = (Organization) this.CACHE.get("getMonoInstituicao");
        if (ret == null) {
            try {
                List<Organization> permitedOrganizations = this.getPermitedOrganizations();
                if (permitedOrganizations != null && permitedOrganizations.size() == 1) {
                    ret = permitedOrganizations.get(0);
                }
            } catch (Exception e) {

            }
        }
        return ret;
    }

    public ControllerAbstractCRUD<T> extraSearchParam(String param, Object value) {
        this.getExtraSearchParams().put(param, value);
        return this;
    }

    public ControllerAbstractCRUD<T> extraFetchParam(String... fetch) {
        //TODO: Refatorar esse parametro ou protege-lo
        this.getExtraSearchParams().put("fetch", fetch);
        return this;
    }

    public static String exceptionToDetailMsg(Exception e) {
        String message = "";
        if (e != null) {
            message = e.getMessage();
        }
        return message;
    }

    public boolean isAttr(String nameAtribute) {
        boolean ret = false;
        try {
            ret = (Boolean) this.CACHE.get(nameAtribute);
        } catch (Exception e) {

        }
        return ret;
    }

    public Boolean isAttribute(String nameAtribute) {
        Boolean ret = null;
        try {
            ret = (Boolean) this.CACHE.get(nameAtribute);
        } catch (Exception e) {

        }
        return ret;
    }

    public void setAttr(String nameAtribute, Object value) {
        try {
            this.CACHE.put(nameAtribute, value);
        } catch (Exception e) {

        }
    }

    public Number getCount(String nameCount) {
        Number count = 0;
        try {
            count = (Number) this.CACHE.get(nameCount);
        } catch (Exception e) {

        }
        return count;
    }

    public void addCount(String nameCount, Number incOffset) {
        Number count = (Number) this.CACHE.get(nameCount);
        if (count != null) {
            if (count instanceof Double) {
                count = ((Double) count) + incOffset.doubleValue();
            } else if (count instanceof Float) {
                count = ((Float) count) + incOffset.floatValue();
            } else {
                count = count.longValue() + incOffset.longValue();
            }
        } else {
            count = incOffset;
        }
        this.CACHE.put(nameCount, count);
    }

    public List<Menu> menusUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String name = authentication.getName();
        return this.montaMenu(this.rolesUser(name));
    }

    public List<Menu> menusUser(String nome) {
        return this.montaMenu(this.rolesUser(nome));
    }

    private List<Menu> montaMenu(Collection<Role> perfis) {
        /**
         * MENU MANTER
         */
        UtilReflection.UtilComparator<Menu> comparator = new UtilReflection.UtilComparator<Menu>("sortIndex", "name");
        Collection<Menu> menus = new TreeSet<Menu>(comparator);
        if (perfis != null) {
            for (Role p : perfis) {
                Collection<Menu> setMenus = p.getMenus();
                if (setMenus != null) {
                    for (Menu menu : setMenus) {
                        if (menu.getActive()) {
                            menus.add(menu);
                        }
                        //TODO: Menu superior
                        //menus.add(menu.getParent());
                    }
                }
                if (p.getSysRole() == SysRole.ADM || p.getSysRole() == SysRole.MNG) {
                    List<Menu> todosMenusValidos
                            = null;
//                    this.getDao().queryList("SELECT DISTINCT m FROM Menu m "
//                                    + "LEFT JOIN FETCH m.childs "
//                                    + "WHERE m.parent IS NULL"
//                                    + " AND (m.removed IS NULL OR m.removed = False)");
                    if (todosMenusValidos != null) {
                        menus.addAll(todosMenusValidos);
                    }
                }
            }
        }

        //TODO: Central property
        if (true) {//if (config.isProduction(env)) {//TODO: Remover na primeira relese
            Menu desenvMenu = new Menu("Development");
            desenvMenu
                    .addChild(new Menu("Usuario").link("/jsf/app/usuario.xhtml"))
                    .addChild(new Menu("Autogen").link("/jsf/entity-crud-autogen.xhtml"))
                    .addChild(new Menu("Scratch").link("/jsf/entity-crud-scratch.xhtml"))
                    .addChild(new Menu("Legacy").link("/jsf/tmp/index.xhtml"))
                    .addChild(new Menu("Layout-1").link("/pkg/startbootstrap-sb-admin/index.html"))
                    .addChild(new Menu("Layout-2").link("/pkg/startbootstrap-sb-admin-2/index.html"))
                    .addChild(new Menu("Layout-3").link("/tmp/sidebar/chart-chartjs.html"));
            menus.add(desenvMenu);
        }

        List<Menu> menusList = new ArrayList<>();
        menusList.addAll(menus);
        Collections.sort(menusList, comparator);
        return menusList;
    }

    public List<Role> rolesUser(String username) {
        List<Role> grupos = new ArrayList<>();
        try {
            if (username != null && !username.isEmpty()) {
                List result = null;
//                        this.getDao().queryList("select g from UserLogin u join u.roles g"
//                        + " where u.userName = ?1", username);

                if (result != null) {
                    grupos.addAll(result);
                }
                List<OrganizationRole> resultOr = null;
//                this.getDao().queryList("SELECT DISTINCT g FROM UserLogin u "
//                        + " JOIN u.organizationRole g  WHERE u.userName = ?1", username);
                if (result != null) {
                    for (OrganizationRole or : resultOr) {
                        grupos.add(or.getRole());
                    }
                }
            }

        } catch (NoResultException | EntityNotFoundException | EmptyResultDataAccessException e) {
            log.error("erro", e);
        }
        return grupos;
    }

}
