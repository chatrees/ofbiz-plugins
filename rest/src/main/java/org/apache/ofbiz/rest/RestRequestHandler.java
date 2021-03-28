package org.apache.ofbiz.rest;

import org.apache.juneau.rest.RestContext;
import org.apache.juneau.rest.util.UrlPathPatternMatch;
import org.apache.ofbiz.base.util.Debug;
import org.apache.ofbiz.rest.operation.OperationHandler;
import org.apache.ofbiz.rest.operation.OperationHandlerException;
import org.apache.ofbiz.rest.operation.OperationHandlerFactory;
import org.apache.ofbiz.rest.operation.OperationResult;
import org.apache.ofbiz.webapp.control.WebAppConfigurationException;
import org.w3c.dom.Element;

import javax.servlet.ServletContext;
import java.net.URL;

public final class RestRequestHandler {

    private static final String MODULE = RestRequestHandler.class.getName();

    public static final String REST_REQUEST_HANDLER_ATTR_NAME = "_REST_REQUEST_HANDLER_";

    private final OperationHandlerFactory operationHandlerFactory;
    private final URL restConfigURL;

    private RestRequestHandler(ServletContext servletContext) {
        restConfigURL = RestConfigXMLReader.getRestConfigURL(servletContext);
        try {
            RestConfigXMLReader.getRestConfig(restConfigURL);
        } catch (WebAppConfigurationException e) {
            Debug.logError(e, "Exception thrown while parsing rest.xml file: ", MODULE);
        }
        this.operationHandlerFactory = new OperationHandlerFactory(servletContext, restConfigURL);
    }

    public static RestRequestHandler getRestRequestHandler(ServletContext servletContext) {
        RestRequestHandler restRequestHandler = (RestRequestHandler) servletContext.getAttribute(REST_REQUEST_HANDLER_ATTR_NAME);
        if (restRequestHandler == null) {
            restRequestHandler = new RestRequestHandler(servletContext);
            servletContext.setAttribute(REST_REQUEST_HANDLER_ATTR_NAME, restRequestHandler);
        }
        return restRequestHandler;
    }

    public RestConfigXMLReader.RestConfig getRestConfig() {
        try {
            return RestConfigXMLReader.getRestConfig(this.restConfigURL);
        } catch (WebAppConfigurationException e) {
            Debug.logError(e, "Exception thrown while parsing rest.xml file: ", MODULE);
        }
        return null;
    }

    public OperationHandler getOperationHandler(RestConfigXMLReader.Operation operation) throws OperationHandlerException {
        Element handlerElement = operation.getHandlerElement();
        String handlerType = handlerElement.getTagName();
        return operationHandlerFactory.getOperationHandler(handlerType);
    }

    public OperationResult runOperation(RestConfigXMLReader.Operation operation, UrlPathPatternMatch urlPathPatternMatch, RestContext restContext) throws OperationHandlerException {
        OperationHandler operationHandler = getOperationHandler(operation);
        return operationHandler.invoke(operation, urlPathPatternMatch, restContext);
    }
}
