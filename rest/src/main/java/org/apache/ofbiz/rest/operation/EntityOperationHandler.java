package org.apache.ofbiz.rest.operation;

import org.apache.ofbiz.rest.RestConfigXMLReader;

import javax.servlet.ServletContext;

public class EntityOperationHandler implements OperationHandler {

    @Override
    public void init(ServletContext context) throws OperationHandlerException {

    }

    @Override
    public OperationResult invoke(RestConfigXMLReader.Operation operation) {
        return null;
    }
}
