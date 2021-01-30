package org.apache.ofbiz.rest.operation;

import org.apache.juneau.http.HttpMethod;
import org.apache.juneau.http.exception.InternalServerError;
import org.apache.juneau.http.exception.NotAcceptable;
import org.apache.juneau.rest.RestContext;
import org.apache.juneau.rest.RestRequest;
import org.apache.juneau.rest.util.UrlPathPatternMatch;
import org.apache.ofbiz.base.util.Debug;
import org.apache.ofbiz.base.util.UtilMisc;
import org.apache.ofbiz.base.util.UtilValidate;
import org.apache.ofbiz.rest.RestConfigXMLReader;
import org.apache.ofbiz.service.GenericServiceException;
import org.apache.ofbiz.service.LocalDispatcher;
import org.w3c.dom.Element;

import javax.servlet.ServletContext;
import java.util.HashMap;
import java.util.Map;

public class EntityOperationHandler implements OperationHandler {

    private static final String MODULE = EntityOperationHandler.class.getName();

    @Override
    public void init(ServletContext context) throws OperationHandlerException {

    }

    @Override
    public OperationResult invoke(RestConfigXMLReader.Operation operation, UrlPathPatternMatch urlPathPatternMatch, RestContext restContext) {
        Debug.logInfo("Entity: " + restContext.getRequest().getMethod() + " : " + restContext.getRequest().getPathInfo(), MODULE);
        RestRequest restRequest = restContext.getRequest();
        LocalDispatcher dispatcher = (LocalDispatcher) restRequest.getAttribute("dispatcher");
        String method = restRequest.getMethod();
        Element entityElement = operation.getHandlerElement();
        String name = entityElement.getAttribute("name");
        String action = entityElement.getAttribute("action");
        if (UtilValidate.isEmpty(action)) {
            throw new NotAcceptable("The action is missing");
        }

        if (HttpMethod.GET.equals(method)) {
            if ("list".equals(action)) {
                Map<String, Object> inputFields = new HashMap<>();
                Map<String, Object> performFindListInMap = UtilMisc.toMap(
                        "entityName", name,
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
            }
        }

        Map<String, Object> output = new HashMap<>();

        // TODO set fields returned from calling an event
        output.put("text", "Response from Entity handler.");

        return OperationResult.ok(output);
    }
}
