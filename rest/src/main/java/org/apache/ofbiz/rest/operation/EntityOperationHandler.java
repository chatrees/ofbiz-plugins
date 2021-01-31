package org.apache.ofbiz.rest.operation;

import org.apache.juneau.dto.swagger.ParameterInfo;
import org.apache.juneau.http.exception.InternalServerError;
import org.apache.juneau.http.exception.NotAcceptable;
import org.apache.juneau.rest.RestContext;
import org.apache.juneau.rest.RestRequest;
import org.apache.juneau.rest.util.UrlPathPatternMatch;
import org.apache.ofbiz.base.util.Debug;
import org.apache.ofbiz.base.util.UtilMisc;
import org.apache.ofbiz.base.util.UtilValidate;
import org.apache.ofbiz.entity.Delegator;
import org.apache.ofbiz.entity.GenericEntityException;
import org.apache.ofbiz.entity.GenericPK;
import org.apache.ofbiz.entity.GenericValue;
import org.apache.ofbiz.entity.model.ModelEntity;
import org.apache.ofbiz.entity.model.ModelField;
import org.apache.ofbiz.entity.util.EntityQuery;
import org.apache.ofbiz.rest.RestConfigXMLReader;
import org.apache.ofbiz.service.GenericServiceException;
import org.apache.ofbiz.service.LocalDispatcher;
import org.w3c.dom.Element;

import javax.servlet.ServletContext;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import static org.apache.juneau.dto.swagger.SwaggerBuilder.parameterInfo;

public class EntityOperationHandler implements OperationHandler {

    private static final String MODULE = EntityOperationHandler.class.getName();

    @Override
    public void init(ServletContext context) throws OperationHandlerException {

    }

    @Override
    public Collection<ParameterInfo> getParametersInfos(RestConfigXMLReader.Operation operation, RestRequest restRequest) {
        ModelEntity modelEntity = getModelEntity(operation, restRequest);
        String action = getAction(operation);

        Map<String, ParameterInfo> parameterInfoMap = new HashMap<>();

        // path parameters
        for (RestConfigXMLReader.VariableResource variableResource : operation.getVariableResources()) {
            String parameterName = variableResource.getName();
            ModelField modelField = modelEntity.getField(parameterName);
            ParameterInfo parameterInfo = parameterInfo("path", parameterName)
                    .required(true)
                    .type(String.class.getSimpleName().toLowerCase());
            // TODO
            //.example()

            if (modelField != null) {
                parameterInfo.setDescription(modelField.getDescription());
            }

            parameterInfoMap.put(parameterName, parameterInfo);
        }

        if ("one".equals(action)) {
            Iterator<ModelField> iter = modelEntity.getPksIterator();
            while (iter.hasNext()) {
                ModelField curField = iter.next();
                String fieldName = curField.getName();
                if (!parameterInfoMap.containsKey(fieldName)) {
                    parameterInfoMap.put(fieldName, parameterInfo("query", fieldName)
                            .description(curField.getDescription())
                            .required(true)
                            .type(String.class.getSimpleName().toLowerCase()));
                }
            }
        } else if ("list".equals(action)) {
            Iterator<ModelField> iter = modelEntity.getFieldsIterator();
            while (iter.hasNext()) {
                ModelField curField = iter.next();
                String fieldName = curField.getName();
                if (!parameterInfoMap.containsKey(fieldName)) {
                    parameterInfoMap.put(fieldName, parameterInfo("query", fieldName)
                            .description(curField.getDescription())
                            .required(false)
                            .type(String.class.getSimpleName().toLowerCase()));
                }
            }
        }

        return parameterInfoMap.values();
    }

    @Override
    public OperationResult invoke(RestConfigXMLReader.Operation operation, UrlPathPatternMatch urlPathPatternMatch, RestContext restContext) {
        Debug.logInfo("Entity: " + restContext.getRequest().getMethod() + " : " + restContext.getRequest().getPathInfo(), MODULE);
        RestRequest restRequest = restContext.getRequest();
        LocalDispatcher dispatcher = (LocalDispatcher) restRequest.getAttribute("dispatcher");
        Delegator delegator = (Delegator) restRequest.getAttribute("delegator");
        String entityName = getEntityName(operation);
        String action = getAction(operation);
        if (UtilValidate.isEmpty(action)) {
            throw new NotAcceptable("The action is missing");
        }

        ModelEntity modelEntity = getModelEntity(operation, restRequest);

        if ("list".equals(action)) {
            Map<String, Object> inputFields = new HashMap<>();
            Map<String, Object> performFindListInMap = UtilMisc.toMap(
                    "entityName", entityName,
                    "inputFields", inputFields,
                    "orderBy", null,
                    "viewIndex", 0,
                    "viewSize", 20,
                    "noConditionFind", "Y"
            );
            try {
                Map<String, Object> performFindListResults = dispatcher.runSync("performFindList", performFindListInMap);
                Object list = performFindListResults.get("list");
                Object orderBy = performFindListInMap.get("orderBy");
                Object viewIndex = performFindListInMap.get("viewIndex");
                Object viewSize = performFindListInMap.get("viewSize");
                Object noConditionFind = performFindListInMap.get("noConditionFind");
                Object listSize = performFindListResults.get("listSize");
                return OperationResult.ok(list, UtilMisc.toMap(
                        "X-ORDER-BY", orderBy != null ? String.valueOf(orderBy) : null,
                        "X-VIEW-INDEX", viewIndex != null ? String.valueOf(viewIndex) : null,
                        "X-VIEW-SIZE", viewSize != null ? String.valueOf(viewSize) : null,
                        "X-NO-CONDITION-FIND", noConditionFind != null ? String.valueOf(noConditionFind) : null,
                        "X-LIST-SIZE", listSize != null ? String.valueOf(listSize) : null
                        ));
            } catch (GenericServiceException e) {
                throw new InternalServerError(e);
            }
        } else if ("one".equals(action)) {
            // assemble the field map
            Map<String, Object> entityContext = new HashMap<>();
            Map parameters = new HashMap();
            parameters.putAll(urlPathPatternMatch.getVars());
            parameters.putAll(restRequest.getParameterMap());
            // only need PK fields
            Iterator<ModelField> iter = modelEntity.getPksIterator();
            while (iter.hasNext()) {
                ModelField curField = iter.next();
                String fieldName = curField.getName();
                Object fieldValue = parameters.get(fieldName);
                entityContext.put(fieldName, fieldValue);
            }

            try {
                GenericValue valueOut = null;
                GenericPK entityPK = delegator.makePK(modelEntity.getEntityName(), entityContext);
                // make sure we have a full primary key, if any field is null then just log a warning and return null instead of blowing up
                if (entityPK.containsPrimaryKey(true)) {
                    valueOut = EntityQuery.use(delegator).from(entityPK.getEntityName()).where(entityPK).cache(false).queryOne();
                } else {
                    if (Debug.infoOn()) {
                        Debug.logInfo("Returning null because found incomplete primary key in find: " + entityPK, MODULE);
                    }
                }
                return OperationResult.ok(valueOut);
            } catch (GenericEntityException e) {
                throw new InternalServerError(e);
            }
        } else {
            throw new InternalServerError("Unknown entity action: " + action);
        }
    }

    private static String getEntityName(RestConfigXMLReader.Operation operation) {
        Element entityElement = operation.getHandlerElement();
        return entityElement.getAttribute("name");
    }

    private static ModelEntity getModelEntity(RestConfigXMLReader.Operation operation, RestRequest restRequest) {
        Delegator delegator = (Delegator) restRequest.getAttribute("delegator");
        String entityName = getEntityName(operation);
        ModelEntity modelEntity = delegator.getModelEntity(entityName);
        if (modelEntity == null) {
            throw new InternalServerError("No entity definition found for entity name [" + entityName + "]");
        }
        return modelEntity;
    }

    private static String getAction(RestConfigXMLReader.Operation operation) {
        Element entityElement = operation.getHandlerElement();
        return entityElement.getAttribute("action");
    }
}
