package org.apache.ofbiz.rest.operation;

import org.apache.juneau.rest.RestGuard;
import org.apache.juneau.rest.RestRequest;

public class OperationRestGuard extends RestGuard {

    @Override
    public boolean isRequestAllowed(RestRequest req) {
        String method = req.getMethod();
        String pathInfo = req.getPathInfo();
        if (pathInfo ==  null && "OPTIONS".equals(method)) { // Swagger JSON request
            return true;
        }

        return false;
    }
}
