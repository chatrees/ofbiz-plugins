package org.apache.ofbiz.rest.operation;

import org.apache.juneau.rest.RestContext;
import org.apache.juneau.rest.RestRequest;
import org.apache.juneau.rest.util.UrlPathPatternMatch;
import org.apache.ofbiz.base.util.UtilMisc;
import org.apache.ofbiz.rest.RestConfigXMLReader;
import org.apache.ofbiz.service.DispatchContext;
import org.apache.ofbiz.service.LocalDispatcher;
import org.apache.ofbiz.service.ModelService;
import org.junit.Before;
import org.junit.Test;
import org.w3c.dom.Element;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import java.util.Collections;
import java.util.List;

import static org.mockito.Mockito.*;

public class ServiceOperationHandlerTest {

    ServiceOperationHandler serviceOperationHandler = new ServiceOperationHandler();

    @Before
    public void setUp() throws Exception {
    }

    @Test
    public void testHandleParameters() throws Exception {
        Element handlerElement = mock(Element.class);
        RestConfigXMLReader.Operation operation = mock(RestConfigXMLReader.Operation.class);
        UrlPathPatternMatch urlPathPatternMatch = mock(UrlPathPatternMatch.class);
        ServletContext servletContext = mock(ServletContext.class);
        HttpSession httpSession = mock(HttpSession.class);
        HttpServletRequest httpServletRequest = mock(HttpServletRequest.class);
        RestContext restContext = mock(RestContext.class);
        RestRequest restRequest = mock(RestRequest.class);
        LocalDispatcher dispatcher = mock(LocalDispatcher.class);
        DispatchContext dispatchContext = mock(DispatchContext.class);

        List<String> parameterNames = UtilMisc.toList("test1");
        List<String> servletContextAttributeNames = UtilMisc.toList("test1");

        when(operation.getHandlerElement()).thenReturn(handlerElement);
        when(httpServletRequest.getServletContext()).thenReturn(servletContext);
        when(httpServletRequest.getSession()).thenReturn(httpSession);
        when(httpServletRequest.getParameterNames()).thenReturn(Collections.enumeration(parameterNames));
        when(servletContext.getAttributeNames()).thenReturn(Collections.enumeration(servletContextAttributeNames));
        when(restRequest.getRequest()).thenReturn(httpServletRequest);
        when(restContext.getRequest()).thenReturn(restRequest);

        when(httpServletRequest.getAttribute("servletContext")).thenReturn(servletContext);
        when(restRequest.getAttribute("dispatcher")).thenReturn(dispatcher);
        when(dispatcher.getDispatchContext()).thenReturn(dispatchContext);
        when(handlerElement.getAttribute("mode")).thenReturn("sync");
        when(handlerElement.getAttribute("name")).thenReturn("testService");

        ModelService modelService = mock(ModelService.class);
        when(dispatchContext.getModelService(anyString())).thenReturn(modelService);
        serviceOperationHandler.invoke(operation, urlPathPatternMatch, restContext);
    }
}
