package org.apache.ofbiz.rest.operation;

import org.apache.juneau.dto.swagger.ParameterInfo;
import org.apache.juneau.rest.RestContext;
import org.apache.juneau.rest.RestRequest;
import org.apache.juneau.rest.util.UrlPathPatternMatch;
import org.apache.ofbiz.rest.RestConfigXMLReader;

import javax.servlet.ServletContext;
import java.util.Collection;

public interface OperationHandler {

    void init(ServletContext context) throws OperationHandlerException;

    OperationResult invoke(RestConfigXMLReader.Operation operation, UrlPathPatternMatch urlPathPatternMatch, RestContext restContext);

    Object getDescription(RestConfigXMLReader.Operation operation, RestRequest restRequest);

    Collection<ParameterInfo> getParametersInfos(RestConfigXMLReader.Operation operation, RestRequest restRequest);
}
