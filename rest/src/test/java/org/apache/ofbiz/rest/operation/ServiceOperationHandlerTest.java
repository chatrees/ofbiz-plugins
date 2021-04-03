package org.apache.ofbiz.rest.operation;

import org.apache.juneau.http.HttpMethod;
import org.apache.juneau.rest.RestContext;
import org.apache.juneau.rest.RestRequest;
import org.apache.juneau.rest.util.UrlPathPatternMatch;
import org.apache.ofbiz.base.util.GeneralException;
import org.apache.ofbiz.base.util.ObjectType;
import org.apache.ofbiz.base.util.UtilGenerics;
import org.apache.ofbiz.base.util.test.ObjectTypeTests;
import org.apache.ofbiz.rest.RestConfigXMLReader;
import org.apache.ofbiz.service.*;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.w3c.dom.Element;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import java.sql.Date;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class ServiceOperationHandlerTest {

    @Mock
    private ServletContext servletContext;
    @Mock
    private HttpSession httpSession;
    @Mock
    private HttpServletRequest httpServletRequest;
    @Mock
    private RestContext restContext;
    @Mock
    private RestRequest restRequest;
    @Mock
    private UrlPathPatternMatch urlPathPatternMatch;
    @Mock
    private RestConfigXMLReader.Operation operation;
    @Mock
    private Element handlerElement;
    @Mock
    private LocalDispatcher dispatcher;
    @Mock
    private DispatchContext dispatchContext;
    @Mock
    private ModelService modelService;
    @Captor
    private ArgumentCaptor<Map<String, Object>> serviceContextCaptor;

    private final ServiceOperationHandler serviceOperationHandler = new ServiceOperationHandler();

    private Map<String, Object> servletContextAttributes;
    private Map<String, Object> sessionAttributes;
    private Map<String, Object> httpServletRequestAttributes;
    private Map<String, Object> httpServletRequestParameters;
    private Map<String, Object> restRequestAttributes;
    private Map<String, Object> handlerElementAttributes;

    private Map<String, ModelParam> inModelParamMap = new HashMap<>();

    @Before
    public void setUp() throws Exception {
        servletContextAttributes = new HashMap<>();
        sessionAttributes = new HashMap<>();
        httpServletRequestAttributes = new ConcurrentHashMap<>();
        httpServletRequestParameters = new HashMap<>();
        restRequestAttributes = new HashMap<>();
        handlerElementAttributes = new HashMap<>();

        httpServletRequestAttributes.put("servletContext", servletContext);
        httpServletRequestAttributes.put("multiPartMap", new HashMap<>());
        restRequestAttributes.put("dispatcher", dispatcher);
        handlerElementAttributes.put("mode", "sync");
        handlerElementAttributes.put("name", "testService");

        when(servletContext.getAttributeNames()).thenAnswer(invocation -> Collections.enumeration(servletContextAttributes.keySet()));
        when(httpSession.getAttributeNames()).thenAnswer(invocation -> Collections.enumeration(sessionAttributes.keySet()));
        lenient().when(httpServletRequest.getServletContext()).thenReturn(servletContext);
        when(httpServletRequest.getSession()).thenReturn(httpSession);
        when(httpServletRequest.getAttributeNames()).thenAnswer(invocation -> Collections.enumeration(httpServletRequestAttributes.keySet()));
        when(httpServletRequest.getParameterNames()).thenAnswer(invocation -> Collections.enumeration(httpServletRequestParameters.keySet()));
        when(restRequest.getRequest()).thenReturn(httpServletRequest);
        when(restContext.getRequest()).thenReturn(restRequest);
        lenient().when(operation.getHandlerElement()).thenReturn(handlerElement);
        when(dispatcher.runSync(anyString(), serviceContextCaptor.capture())).thenReturn(ServiceUtil.returnSuccess());
        when(dispatchContext.getModelService(anyString())).thenReturn(modelService);

        when(httpServletRequest.getAttribute(anyString())).then(invocation -> {
            String attrName = invocation.getArgument(0);
            return httpServletRequestAttributes.get(attrName);
        });
        lenient().when(httpServletRequest.getParameter(anyString())).thenAnswer(invocation -> {
            String paramName = invocation.getArgument(0);
            return httpServletRequestParameters.get(paramName);
        });
        lenient().when(httpServletRequest.getParameterValues(anyString())).thenAnswer(invocation -> {
            String paramName = invocation.getArgument(0);
            String paramValue = UtilGenerics.cast(httpServletRequestParameters.get(paramName));
            return new String[] { paramValue };
        });
        when(restRequest.getAttribute(anyString())).then(invocation -> {
            String attrName = invocation.getArgument(0);
            return restRequestAttributes.get(attrName);
        });
        when(handlerElement.getAttribute(anyString())).then(invocation -> {
            String attrName = invocation.getArgument(0);
            return handlerElementAttributes.get(attrName);
        });
        when(dispatcher.getDispatchContext()).thenReturn(dispatchContext);

        when(modelService.getInModelParamList()).thenAnswer(invocation -> new ArrayList<>(inModelParamMap.values()));
        when(modelService.makeValid(anyMap(), anyString(), anyBoolean(), anyList(), any(TimeZone.class), any(Locale.class))).thenAnswer(invocation -> {
            Map<String, Object> source =  invocation.getArgument(0);
            Map<String, Object> target = new HashMap<>();
            if (source == null) {
                return target;
            }

            source.forEach((k, v) -> {
                ModelParam modelParam = inModelParamMap.get(k);
                try {
                    target.put(k, ObjectType.simpleTypeConvert(v, modelParam.type, null, null, null, false));
                } catch (GeneralException e) {
                    e.printStackTrace();
                }
            });
            return target;
        });
    }

    @Test
    public void testHandleStringParameter() {
        assertModelParamValueTypeFromParameter("String", "value1", String.class);
    }

    @Test
    public void testHandleIntegerParameter() {
        assertModelParamValueTypeFromParameter("Integer", "3", Integer.class);
    }

    /**
     * {@link ObjectTypeTests#testSqlDate()}
     */
    @Test
    public void testHandleSqlDateParameter() {
        assertModelParamValueTypeFromParameter("Date", "1969-12-31", Date.class);
    }

    private void assertModelParamValueTypeFromParameter(String modelParamType, String paramValue, Class<?> modelParamValueType) {
        when(restRequest.getMethod()).thenReturn(HttpMethod.POST);

        ModelParam modelParam1 = new ModelParam();
        modelParam1.name = "param1";
        modelParam1.optional = false;
        modelParam1.type = modelParamType;
        inModelParamMap.put(modelParam1.name, modelParam1);
        httpServletRequestParameters.put(modelParam1.name, paramValue);

        serviceOperationHandler.invoke(operation, urlPathPatternMatch, restContext);

        Map<String, Object> serviceContext = serviceContextCaptor.getValue();
        Locale locale = UtilGenerics.cast(serviceContext.get("locale"));
        assertNotNull(locale);

        Object param1 = serviceContext.get("param1");
        assertNotNull(param1);
        assertTrue(modelParamValueType.isInstance(param1));
    }
}
