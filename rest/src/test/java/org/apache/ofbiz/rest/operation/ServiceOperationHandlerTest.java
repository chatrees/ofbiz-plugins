package org.apache.ofbiz.rest.operation;

import org.apache.juneau.rest.RestContext;
import org.apache.juneau.rest.RestRequest;
import org.apache.juneau.rest.util.UrlPathPatternMatch;
import org.apache.ofbiz.base.util.UtilGenerics;
import org.apache.ofbiz.rest.RestConfigXMLReader;
import org.apache.ofbiz.service.DispatchContext;
import org.apache.ofbiz.service.LocalDispatcher;
import org.apache.ofbiz.service.ModelService;
import org.apache.ofbiz.service.ServiceUtil;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.junit.MockitoJUnitRunner;
import org.mockito.stubbing.Answer;
import org.w3c.dom.Element;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import java.util.Collections;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import static org.junit.Assert.*;
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

    @Before
    public void setUp() throws Exception {
        servletContextAttributes = new HashMap<>();
        sessionAttributes = new HashMap<>();
        httpServletRequestAttributes = new HashMap<>();
        httpServletRequestParameters = new HashMap<>();

        httpServletRequestAttributes.put("servletContext", servletContext);

        when(servletContext.getAttributeNames()).thenReturn(Collections.enumeration(servletContextAttributes.keySet()));
        when(httpSession.getAttributeNames()).thenReturn(Collections.enumeration(sessionAttributes.keySet()));
        when(httpServletRequest.getServletContext()).thenReturn(servletContext);
        when(httpServletRequest.getSession()).thenReturn(httpSession);
        when(httpServletRequest.getParameterNames()).thenReturn(Collections.enumeration(httpServletRequestParameters.keySet()));
        when(httpServletRequest.getAttributeNames()).thenReturn(Collections.enumeration(httpServletRequestAttributes.keySet()));
        when(restRequest.getRequest()).thenReturn(httpServletRequest);
        when(restContext.getRequest()).thenReturn(restRequest);
        when(operation.getHandlerElement()).thenReturn(handlerElement);
        when(dispatcher.runSync(anyString(), serviceContextCaptor.capture())).thenReturn(ServiceUtil.returnSuccess());
        when(dispatchContext.getModelService(anyString())).thenReturn(modelService);

//        when(httpServletRequest.getAttribute("servletContext")).thenReturn(servletContext);
        when(httpServletRequest.getAttribute(anyString())).then(new Answer<Object>() {
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {
                String attrName = invocation.getArgument(0);
                return httpServletRequestAttributes.get(attrName);
            }
        });
        when(restRequest.getAttribute("dispatcher")).thenReturn(dispatcher);
        when(dispatcher.getDispatchContext()).thenReturn(dispatchContext);
        when(handlerElement.getAttribute("mode")).thenReturn("sync");
        when(handlerElement.getAttribute("name")).thenReturn("testService");
    }

    @Test
    public void testHandleParameters() {
        httpServletRequestParameters.put("attr1", "value1");
        httpServletRequestAttributes.put("attr1", "value1");
        servletContextAttributes.put("attr1", "value1");
        sessionAttributes.put("attr1", "value1");

        serviceOperationHandler.invoke(operation, urlPathPatternMatch, restContext);

        Map<String, Object> serviceContext = serviceContextCaptor.getValue();
        Locale locale = UtilGenerics.cast(serviceContext.get("locale"));
        assertNotNull(locale);
    }
}
