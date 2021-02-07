package org.apache.ofbiz.rest.operation;

import org.apache.http.HttpStatus;

import java.util.Map;

public class OperationResult {
    private final int status;
    private final Object output;
    private final Map<String, String> headers;

    private OperationResult(int status, Object output, Map<String, String> headers) {
        this.status = status;
        this.output = output;
        this.headers = headers;
    }

    public static OperationResult ok() {
        return OperationResult.ok(null, null);
    }

    public static OperationResult ok(Object output) {
        return OperationResult.ok(output, null);
    }

    public static OperationResult ok(Object output, Map<String, String> headers) {
        return new OperationResult(HttpStatus.SC_OK, output, headers);
    }

    public int getStatus() {
        return status;
    }

    public Object getOutput() {
        return output;
    }

    public Map<String, String> getHeaders() {
        return headers;
    }
}
