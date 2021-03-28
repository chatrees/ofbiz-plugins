package org.apache.ofbiz.rest.operation;

import org.apache.juneau.rest.RestContext;
import org.apache.juneau.rest.RestRequest;
import org.apache.juneau.rest.util.UrlPathPatternMatch;
import org.apache.ofbiz.rest.RestConfigXMLReader;
import org.apache.ofbiz.service.DispatchContext;
import org.apache.ofbiz.service.GenericServiceException;
import org.apache.ofbiz.service.LocalDispatcher;
import org.apache.ofbiz.service.ModelService;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.w3c.dom.Element;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

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

        when(operation.getHandlerElement()).thenReturn(handlerElement);
        when(httpServletRequest.getServletContext()).thenReturn(servletContext);
        when(httpServletRequest.getSession()).thenReturn(httpSession);
        when(restRequest.getRequest()).thenReturn(httpServletRequest);
        when(restContext.getRequest()).thenReturn(restRequest);

        when(restRequest.getAttribute("dispatcher")).thenReturn(dispatcher);
        when(dispatcher.getDispatchContext()).thenReturn(dispatchContext);
        when(handlerElement.getAttribute("mode")).thenReturn("sync");
        when(handlerElement.getAttribute("name")).thenReturn("testService");

        ModelService modelService = mock(ModelService.class);
        when(dispatchContext.getModelService(anyString())).thenReturn(modelService);
        serviceOperationHandler.invoke(operation, urlPathPatternMatch, restContext);
    }
}
