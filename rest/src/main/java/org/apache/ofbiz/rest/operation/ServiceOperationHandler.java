package org.apache.ofbiz.rest.operation;

import org.apache.juneau.rest.RestContext;
import org.apache.juneau.rest.util.UrlPathPatternMatch;
import org.apache.ofbiz.base.util.Debug;
import org.apache.ofbiz.rest.RestConfigXMLReader;

import javax.servlet.ServletContext;
import java.util.HashMap;
import java.util.Map;

public class ServiceOperationHandler implements OperationHandler {

    private static final String MODULE = ServiceOperationHandler.class.getName();

    @Override
    public void init(ServletContext context) throws OperationHandlerException {

    }

    @Override
    public OperationResult invoke(RestConfigXMLReader.Operation operation, UrlPathPatternMatch urlPathPatternMatch, RestContext restContext) {
        Debug.logInfo("Service: " + restContext.getRequest().getMethod() + " : " + restContext.getRequest().getPathInfo(), MODULE);

        Map<String, Object> output = new HashMap<>();

        // TODO set fields returned from calling an event
        output.put("text", "Response from Service handler.");

        return OperationResult.ok(output);
    }
}
