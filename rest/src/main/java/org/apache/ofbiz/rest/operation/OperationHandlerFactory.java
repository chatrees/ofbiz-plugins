package org.apache.ofbiz.rest.operation;

import org.apache.ofbiz.base.util.Debug;
import org.apache.ofbiz.base.util.GeneralRuntimeException;
import org.apache.ofbiz.base.util.ObjectType;
import org.apache.ofbiz.rest.RestConfigXMLReader;

import javax.servlet.ServletContext;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

public class OperationHandlerFactory {

    private static final String MODULE = OperationHandlerFactory.class.getName();

    private final Map<String, OperationHandler> handlers = new HashMap<>();

    public OperationHandlerFactory(ServletContext servletContext, URL restConfigURL) {
        // load all the operation handlers
        try {
            Map<String, String> handlers = RestConfigXMLReader.getRestConfig(restConfigURL).getOperationHandlerMap();
            for (Map.Entry<String, String> handlerEntry: handlers.entrySet()) {
                OperationHandler operationHandler = (OperationHandler) ObjectType.getInstance(handlerEntry.getValue());
                operationHandler.init(servletContext);
                this.handlers.put(handlerEntry.getKey(), operationHandler);
            }
        } catch (Exception e) {
            Debug.logError(e, MODULE);
            throw new GeneralRuntimeException(e);
        }
    }

    public OperationHandler getOperationHandler(String type) throws OperationHandlerException {
        OperationHandler handler = handlers.get(type);
        if (handler == null) {
            throw new OperationHandlerException("No operation handler found for type: " + type);
        }
        return handler;
    }
}
