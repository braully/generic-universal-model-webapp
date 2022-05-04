package io.github.braully.web;

import io.github.braully.persistence.DAOJPA;
import io.github.braully.persistence.IEntity;
import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;

/**
 *
 * @author braully
 */
@Controller
public class GenericControllerCRUD extends ControllerAbstractCRUD<IEntity> {

    protected static final Logger log = LogManager.getLogger(GenericControllerCRUD.class);

    @Autowired(required = false)
    protected HttpSession session;
    @Autowired(required = false)
    protected HttpServletRequest request;

    @Resource
    protected DAOJPA dao;

    /*
    
     */
    public GenericControllerCRUD() {

    }

    @Override
    protected DAOJPA getDao() {
        return dao;
    }

    @Override
    protected HttpServletRequest getCurrentRequest() {
        return this.request;
    }

    @Override
    protected HttpSession getCurrentSession() {
        return this.session;
    }
}
