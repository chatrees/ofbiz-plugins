package org.apache.ofbiz.rest;

import org.apache.catalina.connector.RequestFacade;
import org.apache.juneau.http.exception.Forbidden;
import org.apache.juneau.http.exception.HttpException;
import org.apache.juneau.http.exception.InternalServerError;
import org.apache.juneau.http.exception.NotFound;
import org.apache.juneau.rest.RestRequest;
import org.apache.juneau.rest.RestResponse;
import org.apache.juneau.rest.util.UrlPathPattern;
import org.apache.juneau.rest.util.UrlPathPatternMatch;
import org.apache.ofbiz.base.util.Debug;
import org.apache.ofbiz.base.util.UtilHttp;
import org.apache.ofbiz.base.util.UtilMisc;
import org.apache.ofbiz.entity.GenericValue;
import org.apache.ofbiz.service.GenericServiceException;
import org.apache.ofbiz.service.LocalDispatcher;
import org.apache.ofbiz.service.ServiceUtil;

import javax.servlet.ServletContext;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;
import java.util.Map;

public class RestGuard extends org.apache.juneau.rest.RestGuard {

    private static final String MODULE = RestGuard.class.getName();

    protected boolean isSwaggerRequest(RestRequest req) {
        String method = req.getMethod();
        String pathInfo = req.getPathInfo();
        return pathInfo ==  null && "OPTIONS".equals(method);
    }

    protected void logInUser(RestRequest req) {
        String credentials = req.getHeader("Authorization");
        if (credentials != null && credentials.startsWith("Basic ")) {
            credentials = new String(Base64.getMimeDecoder().decode(credentials.replace("Basic ", "").getBytes(StandardCharsets.UTF_8)));
            if (Debug.verboseOn()) {
                Debug.logVerbose("Found HTTP Basic credentials", MODULE);
            }
            String[] parts = credentials.split(":");
            if (parts.length < 2) {
                throw new Forbidden();
            }

            final String username = parts[0];
            final String password = parts[1];

            LocalDispatcher dispatcher = (LocalDispatcher) req.getAttribute("dispatcher");
            Map<String, Object> serviceMap = UtilMisc.toMap(
                    "login.username", username,
                    "login.password", password);
            serviceMap.put("locale", UtilHttp.getLocale(req));
            try {
                Map<String, Object> result = dispatcher.runSync("userLogin", serviceMap);
                if (ServiceUtil.isError(result) || ServiceUtil.isFailure(result)) {
                    String errorMessage = ServiceUtil.getErrorMessage(result);
                    Debug.logError(errorMessage, MODULE);
                    throw new Forbidden();
                }
                GenericValue userLogin = (GenericValue) result.get("userLogin");
                req.setAttribute("userLogin", userLogin);
            } catch (GenericServiceException e) {
                Debug.logError(e.getMessage(), MODULE);
                throw new InternalServerError(e);
            }
        } else {
            // TODO handler other auth types
            throw new Forbidden();
        }
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
        if (restConfig == null) {
            throw new InternalServerError("Could not find a rest config");
        }
        List<RestConfigXMLReader.Operation> operations = restConfig.getOperations(req.getMethod().toLowerCase());
        if (operations == null) {
            throw new NotFound();
        }
        super.guard(req, res);
        for (RestConfigXMLReader.Operation operation : operations) {
            UrlPathPattern urlPathPattern = new UrlPathPattern(operation.getPath());
            UrlPathPatternMatch urlPathPatternMatch = urlPathPattern.match(req.getPathInfo());
            if (urlPathPatternMatch != null) {
                logInUser(req);
                req.setAttribute("_OPERATION_", operation);
                req.setAttribute("_URL_PATH_PATTERN_MATCH_", urlPathPatternMatch);
                return true;
            }
        }
        throw new NotFound();
    }
}
