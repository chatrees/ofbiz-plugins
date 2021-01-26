package org.apache.ofbiz.rest.operation;

import org.apache.catalina.connector.RequestFacade;
import org.apache.juneau.http.exception.HttpException;
import org.apache.juneau.rest.RestGuard;
import org.apache.juneau.rest.RestRequest;
import org.apache.juneau.rest.RestResponse;
import org.apache.juneau.rest.util.UrlPathPattern;
import org.apache.juneau.rest.util.UrlPathPatternMatch;
import org.apache.ofbiz.rest.RestConfigXMLReader;
import org.apache.ofbiz.rest.RestRequestHandler;

import javax.servlet.ServletContext;
import java.util.List;

public class OperationRestGuard extends RestGuard {

    @Override
    public boolean isRequestAllowed(RestRequest req) {
        String method = req.getMethod();
        String pathInfo = req.getPathInfo();
        if (pathInfo ==  null && "OPTIONS".equals(method)) { // Swagger JSON request
            return true;
        }

        // TODO check user login
        return true;
    }

    @Override
    public boolean guard(RestRequest req, RestResponse res) throws HttpException {
        boolean allowed = super.guard(req, res);
        ServletContext servletContext =((RequestFacade) req.getRequest()).getServletContext();
        RestRequestHandler handler = RestRequestHandler.getRestRequestHandler(servletContext);
        RestConfigXMLReader.RestConfig restConfig = handler.getRestConfig();
        List<RestConfigXMLReader.Operation> operations = restConfig.getOperations(req.getMethod().toLowerCase());
        if (operations == null) {
            return allowed;
        }

        for (RestConfigXMLReader.Operation operation : operations) {
            UrlPathPattern urlPathPattern = new UrlPathPattern(operation.getPath());
            UrlPathPatternMatch urlPathPatternMatch = urlPathPattern.match(req.getPathInfo());
            if (urlPathPatternMatch != null) {
                req.setAttribute("_OPERATION_", operation);
                req.setAttribute("_URL_PATH_PATTERN_MATCH_", urlPathPatternMatch);
                return allowed;
            }
        }

        return allowed;
    }
}
