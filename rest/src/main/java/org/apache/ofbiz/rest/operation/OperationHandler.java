package org.apache.ofbiz.rest.operation;

import org.apache.juneau.dto.swagger.ParameterInfo;
import org.apache.juneau.dto.swagger.ResponseInfo;
import org.apache.juneau.dto.swagger.SchemaInfo;
import org.apache.juneau.rest.RestContext;
import org.apache.juneau.rest.RestRequest;
import org.apache.juneau.rest.util.UrlPathPatternMatch;
import org.apache.ofbiz.rest.RestConfigXMLReader;

import javax.servlet.ServletContext;
import java.util.Collection;
import java.util.Map;

public interface OperationHandler {

    void init(ServletContext context) throws OperationHandlerException;

    OperationResult invoke(RestConfigXMLReader.Operation operation, UrlPathPatternMatch urlPathPatternMatch, RestContext restContext);

    Object getSummary(RestConfigXMLReader.Operation operation, RestRequest restRequest);

    Object getDescription(RestConfigXMLReader.Operation operation, RestRequest restRequest);

    Map<String, SchemaInfo> getDefinitions(RestConfigXMLReader.Operation operation, RestRequest restRequest);

    Collection<ParameterInfo> getParametersInfos(RestConfigXMLReader.Operation operation, RestRequest restRequest);

    Map<String, ResponseInfo> getResponseInfos(RestConfigXMLReader.Operation operation, RestRequest restRequest);
}
