package org.apache.ofbiz.rest;

import org.apache.catalina.connector.RequestFacade;
import org.apache.juneau.http.exception.HttpException;
import org.apache.juneau.http.exception.NotFound;
import org.apache.juneau.rest.RestRequest;
import org.apache.juneau.rest.RestResponse;
import org.apache.juneau.rest.util.UrlPathPattern;
import org.apache.juneau.rest.util.UrlPathPatternMatch;

import javax.servlet.ServletContext;
import java.util.List;

public class RestGuard extends org.apache.juneau.rest.RestGuard {

    protected boolean isSwaggerRequest(RestRequest req) {
        String method = req.getMethod();
        String pathInfo = req.getPathInfo();
        return pathInfo ==  null && "OPTIONS".equals(method);
    }

    @Override
    public boolean isRequestAllowed(RestRequest req) {
        return true;
    }

    @Override
    public boolean guard(RestRequest req, RestResponse res) throws HttpException {
        if (isSwaggerRequest(req)) {
            return true;
        }

        ServletContext servletContext =((RequestFacade) req.getRequest()).getServletContext();
        RestRequestHandler handler = RestRequestHandler.getRestRequestHandler(servletContext);
        RestConfigXMLReader.RestConfig restConfig = handler.getRestConfig();
        List<RestConfigXMLReader.Operation> operations = restConfig.getOperations(req.getMethod().toLowerCase());
        if (operations == null) {
            throw new NotFound();
        }
        super.guard(req, res);
        for (RestConfigXMLReader.Operation operation : operations) {
            UrlPathPattern urlPathPattern = new UrlPathPattern(operation.getPath());
            UrlPathPatternMatch urlPathPatternMatch = urlPathPattern.match(req.getPathInfo());
            if (urlPathPatternMatch != null) {
                req.setAttribute("_OPERATION_", operation);
                req.setAttribute("_URL_PATH_PATTERN_MATCH_", urlPathPatternMatch);

                // TODO check user login
                return true;
            }
        }
        throw new NotFound();
    }
}
