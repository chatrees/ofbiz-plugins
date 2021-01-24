package org.apache.ofbiz.rest;

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

    public OperationResult runOperation(RestConfigXMLReader.Operation operation) throws OperationHandlerException {
        Element handlerElement = operation.getHandlerElement();
        String handlerType = handlerElement.getTagName();
        OperationHandler operationHandler = operationHandlerFactory.getOperationHandler(handlerType);
        return operationHandler.invoke(operation);
    }
}
