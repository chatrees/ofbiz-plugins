package org.apache.ofbiz.rest.operation;

import org.apache.juneau.dto.swagger.ParameterInfo;
import org.apache.juneau.dto.swagger.SchemaInfo;
import org.apache.juneau.http.HttpMethod;
import org.apache.juneau.http.exception.InternalServerError;
import org.apache.juneau.http.exception.PreconditionRequired;
import org.apache.juneau.rest.RestContext;
import org.apache.juneau.rest.RestRequest;
import org.apache.juneau.rest.util.UrlPathPatternMatch;
import org.apache.ofbiz.base.util.*;
import org.apache.ofbiz.entity.GenericValue;
import org.apache.ofbiz.rest.RestConfigXMLReader;
import org.apache.ofbiz.service.*;
import org.w3c.dom.Element;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.math.BigDecimal;
import java.sql.Timestamp;
import java.util.*;

import static org.apache.juneau.dto.swagger.SwaggerBuilder.parameterInfo;
import static org.apache.juneau.dto.swagger.SwaggerBuilder.schemaInfo;
import static org.apache.ofbiz.webapp.event.ServiceEventHandler.ASYNC;
import static org.apache.ofbiz.webapp.event.ServiceEventHandler.SYNC;

public class ServiceOperationHandler implements OperationHandler {

    private static final String MODULE = ServiceOperationHandler.class.getName();

    @Override
    public void init(ServletContext context) throws OperationHandlerException {

    }

    @Override
    public Collection<ParameterInfo> getParametersInfos(RestConfigXMLReader.Operation operation, RestRequest restRequest) {
        TimeZone timeZone = UtilHttp.getTimeZone(restRequest);
        Locale locale = UtilHttp.getLocale(restRequest);
        LocalDispatcher dispatcher = (LocalDispatcher) restRequest.getAttribute("dispatcher");
        if (dispatcher == null) {
            throw new InternalServerError("The local service dispatcher is null");
        }
        DispatchContext dctx = dispatcher.getDispatchContext();
        if (dctx == null) {
            throw new InternalServerError("Dispatch context cannot be found");
        }

        Map<String, ParameterInfo> parameterInfoMap = new HashMap<>();

        Element handlerElement = operation.getHandlerElement();
        String serviceName = getServiceName(handlerElement);
        ModelService modelService = getModelService(dctx, serviceName);

        // path parameters
        for (RestConfigXMLReader.VariableResource variableResource : operation.getVariableResources()) {
            String parameterName = variableResource.getName();
            ModelParam modelParam = modelService.getParam(parameterName);
            ParameterInfo parameterInfo = parameterInfo("path", parameterName)
                    .required(true)
                    .type(String.class.getSimpleName().toLowerCase());

            if (modelParam != null) {
                parameterInfo.setDescription(modelParam.getShortDisplayDescription());
            }

            parameterInfoMap.put(parameterName, parameterInfo);
        }

        if (!HttpMethod.GET.equalsIgnoreCase(operation.getMethod())) { // not GET
            Map<String, SchemaInfo> properties = new HashMap<>();
            Map<String, Object> examples = new HashMap<>();
            for (ModelParam modelParam: modelService.getInModelParamList()) {
                String fieldName = modelParam.getFieldName();
                if (fieldName == null /* userLogin param has no field name */ ||
                        parameterInfoMap.containsKey(fieldName)
                ) {
                    continue;
                }

                properties.put(fieldName, schemaInfo().type(getModelParamSwaggerDataType(modelParam)));
                examples.put(fieldName, getModelParamExample(modelParam, timeZone, locale));
            }

            if (UtilValidate.isNotEmpty(properties)) {
                /*
                 Describing Request Body
                 https://swagger.io/docs/specification/2-0/describing-request-body/
                 */
                ParameterInfo parameterInfo = parameterInfo()
                        .name("body")
                        .in("body")
                        .required(true)
                        .schema(schemaInfo().type("object")
                                .properties(properties)
                                .example(examples)
                        );
                parameterInfoMap.put(parameterInfo.getName(), parameterInfo);
            }
        }

        return parameterInfoMap.values();
    }

    @Override
    public OperationResult invoke(RestConfigXMLReader.Operation operation, UrlPathPatternMatch urlPathPatternMatch, RestContext restContext) {
        Debug.logInfo("Service: " + restContext.getRequest().getMethod() + " : " + restContext.getRequest().getPathInfo(), MODULE);
        Element handlerElement = operation.getHandlerElement();
        RestRequest restRequest = restContext.getRequest();
        HttpServletRequest httpServletRequest = (HttpServletRequest) restRequest.getRequest();

        // make sure we have a valid reference to the Service Engine
        LocalDispatcher dispatcher = (LocalDispatcher) restRequest.getAttribute("dispatcher");
        if (dispatcher == null) {
            throw new InternalServerError("The local service dispatcher is null");
        }
        DispatchContext dctx = dispatcher.getDispatchContext();
        if (dctx == null) {
            throw new InternalServerError("Dispatch context cannot be found");
        }

        // get the details for the service(s) to call
        String mode = SYNC;

        String modeAttr = handlerElement.getAttribute("mode");
        if (UtilValidate.isNotEmpty(modeAttr)) {
            mode = modeAttr;
        }

        // make sure we have a defined service to call
        String serviceName = getServiceName(handlerElement);
        if (Debug.verboseOn()) {
            Debug.logVerbose("[Set mode/service]: " + mode + "/" + serviceName, MODULE);
        }

        // some needed info for when running the service
        Locale locale = UtilHttp.getLocale(httpServletRequest);
        TimeZone timeZone = UtilHttp.getTimeZone(httpServletRequest);
        GenericValue userLogin = (GenericValue) restRequest.getAttribute("userLogin");

        // get the service model to generate context
        ModelService model = getModelService(dctx, serviceName);

        if (Debug.verboseOn()) {
            Debug.logVerbose("[Processing]: SERVICE Operation", MODULE);
            Debug.logVerbose("[Using delegator]: " + dispatcher.getDelegator().getDelegatorName(), MODULE);
        }

        try {
            String body = restRequest.getBody().asString();
            // TODO
        } catch (IOException e) {
            e.printStackTrace();
        }
        Map<String, Object> rawParametersMap = UtilHttp.getCombinedMap(httpServletRequest);
        Map<String, Object> multiPartMap = UtilGenerics.cast(httpServletRequest.getAttribute("multiPartMap"));

        // we have a service and the model; build the context
        Map<String, Object> serviceContext = new HashMap<>();
        for (ModelParam modelParam: model.getInModelParamList()) {
            String name = modelParam.getName();

            // don't include userLogin, that's taken care of below
            if ("userLogin".equals(name)) {
                continue;
            }
            // don't include locale, that is also taken care of below
            if ("locale".equals(name)) {
                continue;
            }
            // don't include timeZone, that is also taken care of below
            if ("timeZone".equals(name)) {
                continue;
            }
            // don't include theme, that is also taken care of below
            if ("visualTheme".equals(name)) {
                continue;
            }

            Object value = null;
            if (UtilValidate.isNotEmpty(modelParam.getStringMapPrefix())) {
                Map<String, Object> paramMap = UtilHttp.makeParamMapWithPrefix(httpServletRequest, multiPartMap,
                        modelParam.getStringMapPrefix(), null);
                value = paramMap;
                if (Debug.verboseOn()) {
                    Debug.logVerbose("Set [" + modelParam.getName() + "]: " + paramMap, MODULE);
                }
            } else if (UtilValidate.isNotEmpty(modelParam.getStringListSuffix())) {
                List<Object> paramList = UtilHttp.makeParamListWithSuffix(httpServletRequest, multiPartMap,
                        modelParam.getStringListSuffix(), null);
                value = paramList;
            } else {
                // first check the multi-part map
                value = multiPartMap.get(name);

                // next check attributes; do this before parameters so that attribute which can
                // be changed by code can override parameters which can't
                if (UtilValidate.isEmpty(value)) {
                    Object tempVal = httpServletRequest.getAttribute(UtilValidate.isEmpty(modelParam.getRequestAttributeName()) ? name
                            : modelParam.getRequestAttributeName());
                    if (tempVal != null) {
                        value = tempVal;
                    }
                }

                // check the request parameters
                if (UtilValidate.isEmpty(value)) {
                    // if the service modelParam has allow-html="any" then get this direct from the
                    // request instead of in the parameters Map so there will be no canonicalization
                    // possibly messing things up
                    if ("any".equals(modelParam.getAllowHtml())) {
                        value = httpServletRequest.getParameter(name);
                    } else {
                        // use the rawParametersMap from UtilHttp in order to also get pathInfo
                        // parameters, do canonicalization, etc
                        value = rawParametersMap.get(name);
                    }

                    // make any composite parameter data (e.g., from a set of parameters
                    // {name_c_date, name_c_hour, name_c_minutes})
                    if (value == null) {
                        value = UtilHttp.makeParamValueFromComposite(httpServletRequest, name);
                    }
                }

                // no field found
                if (value == null) {
                    //still null, give up for this one
                    continue;
                }

                if (value instanceof String && ((String) value).isEmpty()) {
                    // interpreting empty fields as null values for each in back end handling...
                    value = null;
                }
            }
            // set even if null so that values will get nulled in the db later on
            serviceContext.put(name, value);
        }

        // get only the parameters for this service - converted to proper type
        // TODO: pass in a list for error messages, like could not convert type or not a
        // proper X, return immediately with messages if there are any
        List<Object> errorMessages = new LinkedList<>();
        serviceContext = model.makeValid(serviceContext, ModelService.IN_PARAM, true, errorMessages, timeZone, locale);
        if (!errorMessages.isEmpty()) {
            // uh-oh, had some problems...
            throw new InternalServerError(errorMessages.toString());
        }

        // include the UserLogin value object
        if (userLogin != null) {
            serviceContext.put("userLogin", userLogin);
        }

        // include the Locale object
        if (locale != null) {
            serviceContext.put("locale", locale);
        }

        // include the TimeZone object
        if (timeZone != null) {
            serviceContext.put("timeZone", timeZone);
        }

        // invoke the service
        Map<String, Object> result = null;
        try {
            if (ASYNC.equalsIgnoreCase(mode)) {
                dispatcher.runAsync(serviceName, serviceContext);
            } else {
                result = dispatcher.runSync(serviceName, serviceContext);
            }
        } catch (ServiceAuthException e) {
            // not logging since the service engine already did
            throw new InternalServerError(e.getNonNestedMessage());
        } catch (ServiceValidationException e) {
            // not logging since the service engine already did
            if (e.getMessageList() != null) {
                throw new InternalServerError(e.getMessageList().toString());
            } else {
                throw new InternalServerError(e.getNonNestedMessage());
            }
        } catch (GenericServiceException e) {
            Debug.logError(e, "Service invocation error", MODULE);
            throw new InternalServerError("Service invocation error", e.getNested());
        }

        // set the results in the output
        Map<String, Object> output = new HashMap<>();
        for (Map.Entry<String, Object> rme: result.entrySet()) {
            String resultKey = rme.getKey();
            Object resultValue = rme.getValue();

            if (resultKey != null && !ModelService.RESPONSE_MESSAGE.equals(resultKey)
                    && !ModelService.ERROR_MESSAGE.equals(resultKey)
                    && !ModelService.ERROR_MESSAGE_LIST.equals(resultKey)
                    && !ModelService.ERROR_MESSAGE_MAP.equals(resultKey)
                    && !ModelService.SUCCESS_MESSAGE.equals(resultKey)
                    && !ModelService.SUCCESS_MESSAGE_LIST.equals(resultKey)) {
                output.put(resultKey, resultValue);
            }
        }

        return OperationResult.ok(output);
    }

    private String getServiceName(Element handlerElement) {
        String serviceName;
        serviceName = handlerElement.getAttribute("name");
        if (serviceName == null) {
            throw new PreconditionRequired("Service name (eventMethod) cannot be null");
        }
        return serviceName;
    }

    private ModelService getModelService(DispatchContext dctx, String serviceName) {
        ModelService model;

        try {
            model = dctx.getModelService(serviceName);
        } catch (GenericServiceException e) {
            throw new InternalServerError("Problems getting the service model", e);
        }

        if (model == null) {
            throw new InternalServerError("Problems getting the service model");
        }

        return model;
    }

    /**
     * See "Data Types"
     * https://swagger.io/docs/specification/data-models/data-types/
     *
     * org.apache.ofbiz.widget.model.ModelFormFieldBuilder.induceFieldInfoFromServiceParam(org.apache.ofbiz.service.ModelService, org.apache.ofbiz.service.ModelParam, java.lang.String)
     * org.apache.ofbiz.base.util.ObjectType.simpleTypeOrObjectConvert(java.lang.Object, java.lang.String, java.lang.String, java.util.TimeZone, java.util.Locale, boolean)
     */
    private static String getModelParamSwaggerDataType(ModelParam modelParam) {
        String modelParamType = modelParam.getType();
        String type;
        if (String.class.getSimpleName().equals(modelParamType) || String.class.getName().equals(modelParamType) ||
                Timestamp.class.getSimpleName().equals(modelParamType) || Timestamp.class.getName().equals(modelParamType)) {
            type = "string";
        } else if (Integer.class.getSimpleName().equals(modelParamType) || Integer.class.getName().equals(modelParamType)) {
            type = "integer";
        } else if (Short.class.getSimpleName().equals(modelParamType) || Short.class.getName().equals(modelParamType) ||
                Long.class.getSimpleName().equals(modelParamType) || Long.class.getName().equals(modelParamType) ||
                Double.class.getSimpleName().equals(modelParamType) || Double.class.getName().equals(modelParamType) ||
                BigDecimal.class.getSimpleName().equals(modelParamType) || BigDecimal.class.getName().equals(modelParamType)) {
            type = "number";
        } else if (Boolean.class.getSimpleName().equals(modelParamType) || Boolean.class.getName().equals(modelParamType)) {
            type = "boolean";
        } else if (List.class.getSimpleName().equals(modelParamType) || List.class.getName().equals(modelParamType)) {
            type = "array";
        } else {
            type = "object";
        }
        return type;
    }

    private static Object getModelParamExample(ModelParam modelParam, TimeZone timeZone, Locale locale) {
        String modelParamType = modelParam.getType();
        Object example;
        if (String.class.getSimpleName().equals(modelParamType) || String.class.getName().equals(modelParamType)) {
            example = "Example " + modelParam.getFieldName();
        } else if (Timestamp.class.getSimpleName().equals(modelParamType) || Timestamp.class.getName().equals(modelParamType)) {
            Timestamp now = UtilDateTime.nowTimestamp();
            example = UtilDateTime.timeStampToString(now, UtilDateTime.getDateTimeFormat(), timeZone, locale);
        }else if (Integer.class.getSimpleName().equals(modelParamType) || Integer.class.getName().equals(modelParamType)) {
            example = 0;
        } else if (Short.class.getSimpleName().equals(modelParamType) || Short.class.getName().equals(modelParamType) ||
                Long.class.getSimpleName().equals(modelParamType) || Long.class.getName().equals(modelParamType) ||
                Double.class.getSimpleName().equals(modelParamType) || Double.class.getName().equals(modelParamType) ||
                BigDecimal.class.getSimpleName().equals(modelParamType) || BigDecimal.class.getName().equals(modelParamType)) {
            example = 0;
        } else if (Boolean.class.getSimpleName().equals(modelParamType) || Boolean.class.getName().equals(modelParamType)) {
            example = false;
        } else if (List.class.getSimpleName().equals(modelParamType) || List.class.getName().equals(modelParamType)) {
            example = new ArrayList<>();
        } else {
            example = new Object();
        }
        return example;
    }
}
