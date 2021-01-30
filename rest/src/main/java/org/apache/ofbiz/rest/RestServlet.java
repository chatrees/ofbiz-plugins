package org.apache.ofbiz.rest;

import org.apache.juneau.dto.swagger.ParameterInfo;
import org.apache.juneau.dto.swagger.Swagger;
import org.apache.juneau.http.MediaType;
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
import org.apache.ofbiz.rest.operation.OperationResult;

import javax.servlet.ServletException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.apache.juneau.dto.swagger.SwaggerBuilder.*;
import static org.apache.juneau.http.HttpMethod.GET;
import static org.apache.juneau.http.HttpMethod.OPTIONS;

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

    @RestMethod(method = OPTIONS, path = "/", consumes = {})
    public Swagger getOptions(RestRequest req) {
        req.getSession().getServletContext().getServletContextName();
        String basePath = req.getContextPath() + req.getServletPath();

        Swagger originalSwagger = req.getSwagger();

        // TODO Custom Swagger
        try {
            RestRequestHandler restRequestHandler = RestRequestHandler.getRestRequestHandler(getServletContext());
            RestConfigXMLReader.RestConfig restConfig = restRequestHandler.getRestConfig();
            List<RestConfigXMLReader.Operation> operations = restConfig.getOperations();
            for (RestConfigXMLReader.Operation operation : operations) {
                // TODO generate tags
            }

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
                    .definitions(originalSwagger.getDefinitions())
                    .tags(
                            // TODO
                            //tag("pet").description("Pet")
                    );

            // schema and security
            List<String> schemes = UtilMisc.toList("http", "https");
            if (req.isSecure()) {
                Collections.reverse(schemes);
            }
            for (String schema : schemes) {
                swagger.securityDefinition(schema, securityScheme("basic").description(schema));
            }
            swagger.schemes(schemes);

            for (RestConfigXMLReader.Operation operation : operations) {
                RestConfigXMLReader.Security security = operation.getSecurity();
                String path = operation.getPath();
                String methodName = operation.getMethod();
                String scheme = security.isHttps() ? "https" : "http";
                List<ParameterInfo> parameterInfos = new ArrayList<>();

                // path parameters
                for (RestConfigXMLReader.VariableResource variableResource : operation.getVariableResources()) {
                    parameterInfos.add(parameterInfo("path", variableResource.getName())
                            // TODO
                            //.description()
                            .required(true)
                            .type(String.class.getSimpleName().toLowerCase())
                            // TODO
                            //.example()
                    );
                }
                swagger.path(path, methodName,
                        operation()
                                // TODO
                                //.tags("pet")

                                .operationId(operation.getId())
                                .consumes(MediaType.JSON)
                                .parameters(parameterInfos)
                                .response("200",
                                        responseInfo("successful operation"))
                                .security(scheme)
                );
            }

            return swagger;
        } catch (Exception e) {
            Debug.logError(e, MODULE);
            return null;
        }
    }

    @RestMethod(method = GET, path = "*")
    public void onGet(RestContext restContext) {
        RestResponse restResponse = restContext.getResponse();
        RestConfigXMLReader.Operation operation = (RestConfigXMLReader.Operation) restContext.getRequest().getAttribute("_OPERATION_");
        if (operation == null) {
            // TODO set an error
        }

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

            restResponse.setOutput(operationResult.getOutput());
        } catch (GeneralException e) {
            e.printStackTrace();
        }
    }
}
