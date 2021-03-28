package org.apache.ofbiz.rest;

import org.apache.juneau.dto.swagger.SchemaInfo;
import org.apache.juneau.dto.swagger.Swagger;
import org.apache.juneau.dto.swagger.Tag;
import org.apache.juneau.http.MediaType;
import org.apache.juneau.http.exception.InternalServerError;
import org.apache.juneau.json.JsonParser;
import org.apache.juneau.json.JsonSerializer;
import org.apache.juneau.rest.RestContext;
import org.apache.juneau.rest.RestRequest;
import org.apache.juneau.rest.RestResponse;
import org.apache.juneau.rest.annotation.Rest;
import org.apache.juneau.rest.annotation.RestMethod;
import org.apache.juneau.rest.util.UrlPathPatternMatch;
import org.apache.ofbiz.base.util.Debug;
import org.apache.ofbiz.base.util.GeneralException;
import org.apache.ofbiz.base.util.UtilMisc;
import org.apache.ofbiz.base.util.UtilValidate;
import org.apache.ofbiz.rest.operation.OperationHandler;
import org.apache.ofbiz.rest.operation.OperationHandlerException;
import org.apache.ofbiz.rest.operation.OperationResult;

import javax.servlet.ServletException;
import java.util.*;

import static org.apache.juneau.dto.swagger.SwaggerBuilder.*;
import static org.apache.juneau.http.HttpMethod.*;

@Rest(
        // Allow OPTIONS requests to be simulated using ?method=OPTIONS query parameter.
        allowedMethodParams="OPTIONS",
        serializers = {
                JsonSerializer.class,
        },
        parsers = {
                JsonParser.class,
        },
        guards = {
                RestGuard.class,
        }
)
public class RestServlet extends org.apache.juneau.rest.RestServlet {

    private static final String MODULE = RestServlet.class.getName();

    @Override
    public void init() throws ServletException {
        // Initialize the rest request handler.
        RestRequestHandler.getRestRequestHandler(getServletContext());
    }

    @RestMethod(method = OPTIONS, path = "*")
    public void getOptions(RestContext restContext) {
        RestRequest req = restContext.getRequest();
        RestResponse res = restContext.getResponse();
        String pathInfo = req.getPathInfo();
        if (pathInfo == null) {
            try {
                // create a swagger and set it as output
                res.setOutput(createSwagger(req));
            } catch (OperationHandlerException e) {
                Debug.logError(e, MODULE);
                throw new InternalServerError(e);
            }
        } else {
            handleRest(restContext);
        }
    }

    @RestMethod(method = GET, path = "*")
    public void onGet(RestContext restContext) {
        handleRest(restContext);
    }

    @RestMethod(method = POST, path = "*")
    public void onPost(RestContext restContext) {
        handleRest(restContext);
    }

    @RestMethod(method = PUT, path = "*")
    public void onPut(RestContext restContext) {
        handleRest(restContext);
    }

    @RestMethod(method = PATCH, path = "*")
    public void onPatch(RestContext restContext) {
        handleRest(restContext);
    }

    @RestMethod(method = DELETE, path = "*")
    public void onDelete(RestContext restContext) {
        handleRest(restContext);
    }

    private Swagger createSwagger(RestRequest req) throws OperationHandlerException {
        req.getSession().getServletContext().getServletContextName();
        String basePath = req.getContextPath() + req.getServletPath();

        Swagger originalSwagger = req.getSwagger();

        RestRequestHandler restRequestHandler = RestRequestHandler.getRestRequestHandler(getServletContext());
        RestConfigXMLReader.RestConfig restConfig = restRequestHandler.getRestConfig();
        if (UtilValidate.isEmpty(restConfig)) {
            throw new OperationHandlerException("Could not find rest config");
        }

        List<RestConfigXMLReader.Operation> operations = restConfig.getOperations();

        Map<String, Tag> tags = new HashMap<>();

        Swagger swagger = swagger()
                .swagger("2.0")
                .info(
                        info(restConfig.getTitle(), "1.0.0")
                                .description(restConfig.getDescription())
                                .termsOfService("http://swagger.io/terms/")
                                .contact(
                                        contact().email("apiteam@swagger.io")
                                )
                                .license(
                                        license("Apache 2.0").url("http://www.apache.org/licenses/LICENSE-2.0.html")
                                )
                )
                .basePath(basePath)
                .consumes(originalSwagger.getConsumes())
                .produces(originalSwagger.getProduces())
                .definitions(originalSwagger.getDefinitions());

        // schema and security
        List<String> schemes = UtilMisc.toList("http", "https");
        if (req.isSecure()) {
            Collections.reverse(schemes);
        }
        for (String schema : schemes) {
            swagger.securityDefinition(schema, securityScheme("basic").description(schema));
        }
        swagger.schemes(schemes);

        Map<String, SchemaInfo> definitions = new HashMap<>();

        for (RestConfigXMLReader.Operation operation : operations) {
            OperationHandler operationHandler = restRequestHandler.getOperationHandler(operation);
            RestConfigXMLReader.Security security = operation.getSecurity();
            String path = operation.getPath();
            String methodName = operation.getMethod();
            String scheme = security.isHttps() ? "https" : "http";
            Set<String> tagNames = operation.getTagNames();
            for (String tagName: tagNames) {
                Tag tag = restConfig.getTag(tagName);
                if (UtilValidate.isNotEmpty(tag)) {
                    if (!tags.containsKey(tagName)) {
                        tags.put(tagName, tag);
                    }
                } else {
                    Debug.logWarning("Tag not found: " + tagName, MODULE);
                }
            }
            swagger.path(path, methodName,
                    operation()
                            .tags(tagNames)
                            .operationId(operation.getId())
                            .consumes(MediaType.JSON)
                            .produces(MediaType.JSON)
                            .parameters(operationHandler.getParametersInfos(operation, req))
                            .responses(operationHandler.getResponseInfos(operation, req))
                            .security(scheme)
                            .summary(operationHandler.getSummary(operation, req))
                            .description(operationHandler.getDescription(operation, req))
            );

            Map<String, SchemaInfo> operationDefinitions = operationHandler.getDefinitions(operation, req);
            if (UtilValidate.isNotEmpty(operationDefinitions)) {
                definitions.putAll(operationDefinitions);
            }
        }

        swagger.tags(tags.values());
        swagger.definitions(definitions);

        return swagger;
    }

    protected void handleRest(RestContext restContext) {
        RestResponse restResponse = restContext.getResponse();
        RestConfigXMLReader.Operation operation = (RestConfigXMLReader.Operation) restContext.getRequest().getAttribute("_OPERATION_");
        UrlPathPatternMatch urlPathPatternMatch = (UrlPathPatternMatch) restContext.getRequest().getAttribute("_URL_PATH_PATTERN_MATCH_");

        try {
            RestRequestHandler restRequestHandler = RestRequestHandler.getRestRequestHandler(getServletContext());
            OperationResult operationResult = restRequestHandler.runOperation(operation, urlPathPatternMatch, restContext);

            // headers
            Map<String, String> headers = operationResult.getHeaders();
            if (UtilValidate.isNotEmpty(headers)) {
                for (String headerName : headers.keySet()) {
                    String headerValue = headers.get(headerName);
                    restResponse.setHeader(headerName, headerValue);
                }
            }

            restResponse.setStatus(operationResult.getStatus());
            Object output = operationResult.getOutput();
            if (output != null) {
                restResponse.setOutput(output);
            }
        } catch (GeneralException e) {
            Debug.logError(e, MODULE);
        }
    }
}
