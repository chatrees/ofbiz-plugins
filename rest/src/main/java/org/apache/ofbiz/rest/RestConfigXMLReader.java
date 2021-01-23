package org.apache.ofbiz.rest;

import org.apache.ofbiz.base.util.Debug;
import org.apache.ofbiz.base.util.GeneralException;
import org.apache.ofbiz.base.util.UtilXml;
import org.apache.ofbiz.base.util.cache.UtilCache;
import org.apache.ofbiz.webapp.control.WebAppConfigurationException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class RestConfigXMLReader {

    private static final String MODULE = RestConfigXMLReader.class.getName();
    private static final Path REST_XML_FILE_PATH = Paths.get("WEB-INF", "rest.xml");
    private static final UtilCache<URL, RestConfig> REST_CACHE = UtilCache
            .createUtilCache("webapp.RestConfig");
    private static final UtilCache<URL, RestConfig> REST_SEARCH_RESULTS_CACHE = UtilCache
            .createUtilCache("webapp.RestSearchResults");

    public static RestConfig getRestConfig(URL url) throws WebAppConfigurationException {
        RestConfig restConfig = REST_CACHE.get(url);
        if (restConfig == null) {
            restConfig = REST_CACHE.putIfAbsentAndGet(url, new RestConfig(url));
        }
        return restConfig;
    }

    private static Element loadDocument(URL location) throws WebAppConfigurationException  {
        try {
            Document document = UtilXml.readXmlDocument(location, true);
            Element rootElement = document.getDocumentElement();
            if (!"rest-conf".equalsIgnoreCase(rootElement.getTagName())) {
                rootElement = UtilXml.firstChildElement(rootElement, "rest-conf");
            }
            if (Debug.verboseOn()) {
                Debug.logVerbose("Loaded REST Config - " + location, MODULE);
            }
            return rootElement;
        } catch (Exception e) {
            Debug.logError("When read " + (location != null ? location.toString() : "empty location (!)") + " threw "
                    + e.toString(), MODULE);
            throw new WebAppConfigurationException(e);
        }
    }

    public static class RestConfig {

        private URL url;
        private List<Resource> resources = new ArrayList<>();

        public RestConfig(URL url) throws WebAppConfigurationException {
            this.url = url;
            Element rootElement = loadDocument(url);
            if (rootElement != null) {
                loadResource(rootElement);
            }
        }

        private void loadResource(Element root) throws WebAppConfigurationException {
            try {
                for (Element resourceElement : UtilXml.childElementList(root, "resource")) {
                    Resource resource = new Resource(resourceElement, null);
                    resources.add(resource);
                }
            } catch (GeneralException e) {
                throw new WebAppConfigurationException(e);
            }
        }
    }

    public static class RestResult {

    }

    public static abstract class MethodHandler {
        protected Element element;
        public MethodHandler(Element element) {
            this.element = element;
        }
        public abstract RestResult run(Resource resource);
    }

    public static class ServiceMethodHandler extends MethodHandler {

        public ServiceMethodHandler(Element element) {
            super(element);
        }

        @Override
        public RestResult run(Resource resource) {
            return null;
        }
    }

    public static class EntityMethodHandler extends MethodHandler {

        public EntityMethodHandler(Element element) {
            super(element);
        }

        @Override
        public RestResult run(Resource resource) {
            return null;
        }
    }

    public static class Resource {
        private String name;
        private String tags;
        private Resource parent;
        private Map<String, MethodHandler> methodHandlerMap = new HashMap<>();
        private Map<String, Resource> childResourceMap = new HashMap<>();

        public Resource(Element resourceElement, Resource parent) throws GeneralException {
            this.name = resourceElement.getAttribute("name");
            this.tags = resourceElement.getAttribute("tags");
            this.parent = parent;

            // methods
            List<? extends  Element> methodElements = UtilXml.childElementList(resourceElement, "method");
            for (Element methodElement : methodElements) {
                String type = methodElement.getAttribute("type");
                Element handlerElement = UtilXml.firstChildElement(methodElement);
                String handlerElementName = handlerElement.getTagName();
                MethodHandler methodHandler;
                if ("service".equals(handlerElementName)) {
                    methodHandler = new ServiceMethodHandler(handlerElement);
                } else if ("entity".equals(handlerElementName)) {
                    methodHandler = new EntityMethodHandler(handlerElement);
                } else {
                    throw new GeneralException("Unknown method handler found: " + handlerElementName);
                }

                methodHandlerMap.put(type, methodHandler);
            }

            // resources
            List<? extends  Element> childResourceElements = UtilXml.childElementList(resourceElement, "resource");
            for (Element childResourceElement : childResourceElements) {
                Resource childResource = new Resource(childResourceElement, this);
                this.childResourceMap.put(childResource.name, childResource);
            }

            // vars
            List<? extends  Element> varElements = UtilXml.childElementList(resourceElement, "var");
            for (Element varElement : varElements) {
                Resource varResource = new VarResource(varElement, this);
                this.childResourceMap.put(varResource.name, varResource);
            }
        }
    }

    public static class VarResource extends Resource {

        public VarResource(Element resourceElement, Resource parent) throws GeneralException {
            super(resourceElement, parent);
        }
    }
}
