package org.apache.ofbiz.rest.operation;

import org.apache.juneau.rest.RestContext;
import org.apache.juneau.rest.util.UrlPathPatternMatch;
import org.apache.ofbiz.rest.RestConfigXMLReader;

import javax.servlet.ServletContext;

public interface OperationHandler {

    void init(ServletContext context) throws OperationHandlerException;

    OperationResult invoke(RestConfigXMLReader.Operation operation, UrlPathPatternMatch urlPathPatternMatch, RestContext restContext);
}
