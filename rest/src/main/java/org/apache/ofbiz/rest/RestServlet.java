package org.apache.ofbiz.rest;

import org.apache.juneau.dto.swagger.Swagger;
import org.apache.juneau.http.MediaType;
import org.apache.juneau.json.JsonParser;
import org.apache.juneau.json.JsonSerializer;
import org.apache.juneau.rest.RestContext;
import org.apache.juneau.rest.RestRequest;
import org.apache.juneau.rest.annotation.Rest;
import org.apache.juneau.rest.annotation.RestMethod;
import org.apache.juneau.rest.util.UrlPathPatternMatch;
import org.apache.ofbiz.base.util.Debug;
import org.apache.ofbiz.base.util.GeneralException;
import org.apache.ofbiz.rest.operation.OperationRestGuard;
import org.apache.ofbiz.rest.operation.OperationResult;

import javax.servlet.ServletException;
import java.util.List;

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
                OperationRestGuard.class,
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
        String basePath = req.getContextPath() + req.getServletPath();

        Swagger originalSwagger = req.getSwagger();

        // TODO Custom Swagger
        try {
            RestRequestHandler restRequestHandler = RestRequestHandler.getRestRequestHandler(getServletContext());
            RestConfigXMLReader.RestConfig restConfig = restRequestHandler.getRestConfig();
            List<RestConfigXMLReader.Operation> operations = restConfig.getOperations();
            for (RestConfigXMLReader.Operation operation : operations) {

            }

            Swagger swagger = swagger()
                    .swagger("2.0")
                    .info(
                            info("Swagger Petstore", "1.0.0")
                                    .description("This is a sample server Petstore server.")
                                    .termsOfService("http://swagger.io/terms/")
                                    .contact(
                                            contact().email("apiteam@swagger.io")
                                    )
                                    .license(
                                            license("Apache 2.0").url("http://www.apache.org/licenses/LICENSE-2.0.html")
                                    )
                    )
                    .basePath(basePath)
                    .schemes("https")
                    .securityDefinition("https", securityScheme("basic"))
                    .consumes(originalSwagger.getConsumes())
                    .produces(originalSwagger.getProduces())
                    .definitions(originalSwagger.getDefinitions())
                    .tags(
                            tag("pet").description("Pet")
                    );

            for (RestConfigXMLReader.Operation operation : operations) {
                String path = operation.getPath();
                String methodName = operation.getMethod();
                swagger.path(path, methodName,
                        operation()
                                .tags("pet")
                                .operationId(operation.getId())
                                .consumes(MediaType.JSON)
                                .response("200",
                                        responseInfo("successful operation"))
                );
            }
            swagger.path("/pet", "get",
                            operation()
                            .tags("pet")
                            .operationId("getPet")
                            .consumes(MediaType.JSON)
                            .response("200",
                                    responseInfo("successful operation"))
                    )
                    .path("/pet", "post",
                            operation()
                                    .tags("pet")
                                    .summary("Add a new pet to the store")
                                    .description("")
                                    .operationId("addPet")
                                    .consumes(MediaType.JSON, MediaType.XML)
                                    .produces(MediaType.JSON, MediaType.XML)
                                    .parameters(
                                            parameterInfo("body", "body")
                                                    .description("Pet object that needs to be added to the store")
                                                    .required(true)
                                                    .type("String")
                                                    .example("body 1")
                                    )
                                    .response("405", responseInfo("Invalid input"))
                    );

            return swagger;
        } catch (Exception e) {
            Debug.logError(e, MODULE);
            return null;
        }
    }

    @RestMethod(method = GET, path = "*")
    public void onGet(RestContext restContext) {
        RestConfigXMLReader.Operation operation = (RestConfigXMLReader.Operation) restContext.getRequest().getAttribute("_OPERATION_");
        if (operation == null) {
            // TODO set an error
        }

        UrlPathPatternMatch urlPathPatternMatch = (UrlPathPatternMatch) restContext.getRequest().getAttribute("_URL_PATH_PATTERN_MATCH_");

        try {
            RestRequestHandler restRequestHandler = RestRequestHandler.getRestRequestHandler(getServletContext());
            OperationResult operationResult = restRequestHandler.runOperation(operation, urlPathPatternMatch, restContext);
            restContext.getResponse().setOutput(operationResult.getOutput());
        } catch (GeneralException e) {
            e.printStackTrace();
        }
    }
}
