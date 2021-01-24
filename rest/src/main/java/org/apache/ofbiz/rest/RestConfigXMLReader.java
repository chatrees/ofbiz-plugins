package org.apache.ofbiz.rest;

import org.apache.juneau.dto.swagger.Tag;
import org.apache.ofbiz.base.util.*;
import org.apache.ofbiz.base.util.cache.UtilCache;
import org.apache.ofbiz.webapp.control.WebAppConfigurationException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import javax.servlet.ServletContext;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

import static org.apache.juneau.dto.swagger.SwaggerBuilder.tag;

public final class RestConfigXMLReader {

    private static final String MODULE = RestConfigXMLReader.class.getName();
    private static final Path REST_XML_FILE_PATH = Paths.get("WEB-INF", "rest.xml");
    private static final UtilCache<URL, RestConfig> REST_CACHE = UtilCache
            .createUtilCache("webapp.RestConfig");
    private static final UtilCache<URL, RestConfig> REST_SEARCH_RESULTS_CACHE = UtilCache
            .createUtilCache("webapp.RestSearchResults");

    public static RestConfig getRestConfig(URL url) throws WebAppConfigurationException {
//        RestConfig restConfig = REST_CACHE.get(url);
//        if (restConfig == null) {
//            restConfig = REST_CACHE.putIfAbsentAndGet(url, new RestConfig(url));
//        }
//        return restConfig;

        return new RestConfig(url);
    }

    public static URL getRestConfigURL(ServletContext context) {
        try {
            return context.getResource("/" + REST_XML_FILE_PATH);
        } catch (MalformedURLException e) {
            Debug.logError(e, "Error Finding REST XML Config File: " + REST_XML_FILE_PATH, MODULE);
            return null;
        }
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

        private final List<Operation> operations = new ArrayList<>();
        private final Map<String, List<Operation>> methodOperationNodesMap = new HashMap<>();
        private final Map<String, Tag> tags = new HashMap<>();

        public RestConfig(URL url) throws WebAppConfigurationException {
            Element rootElement = loadDocument(url);
            if (rootElement != null) {
                loadResource(rootElement);
            }
        }

        private void loadResource(Element root) throws WebAppConfigurationException {
            try {
                for (Element resourceElement : UtilXml.childElementList(root, "resource")) {
                    Resource resource = new Resource(resourceElement, null);
                    loadResource(resource);
                    for (Resource childResource : resource.getChildResourceMap().values()) {
                        loadResource(childResource);
                    }
                }
            } catch (GeneralException e) {
                throw new WebAppConfigurationException(e);
            }
        }

        private void loadResource(Resource resource) {
            Map<String, Operation> methodOperationMap = resource.getMethodOperationMap();
            for (String method : methodOperationMap.keySet()) {
                Operation operation = methodOperationMap.get(method);
                this.operations.add(operation);

                // add the operation to the method map
                List<Operation> operations = this.methodOperationNodesMap.computeIfAbsent(method, k -> new ArrayList<>());
                operations.add(operation);

                // add tags
                tags.putAll(operation.getTags());
            }
        }

        public List<Operation> getOperationNodes() {
            return operations;
        }

        public List<Operation> getOperationNodes(String method) {
            return methodOperationNodesMap.get(method);
        }

        public Collection<Tag> getTags() {
            return tags.values();
        }
    }

    public static class RestResult {

    }

    public static abstract class OperationHandler {
        protected Element element;
        public OperationHandler(Element element) {
            this.element = element;
        }
        public abstract RestResult run(Resource resource);
    }

    public static class ServiceOperationHandler extends OperationHandler {

        public ServiceOperationHandler(Element element) {
            super(element);
        }

        @Override
        public RestResult run(Resource resource) {
            return null;
        }
    }

    public static class EntityOperationHandler extends OperationHandler {

        public EntityOperationHandler(Element element) {
            super(element);
        }

        @Override
        public RestResult run(Resource resource) {
            return null;
        }
    }

    public static class Resource {
        protected final String name;
        protected final Resource parent;
        protected final Map<String, Operation> methodOperationMap = new HashMap<>();
        protected final Map<String, Resource> childResourceMap = new HashMap<>();

        public Resource(Element resourceElement, Resource parent) throws GeneralException {
            this.name = resourceElement.getAttribute("name");
            this.parent = parent;

            // children
            List<? extends  Element> childElements = UtilXml.childElementList(resourceElement);
            for (Element childElement : childElements) {
                String tagName = childElement.getTagName();
                if ("operation".equals(tagName)) { // operation
                    Operation operation = new Operation(childElement, this);
                    this.methodOperationMap.put(operation.getMethod(), operation);
                } else if ("resource".equals(tagName)) {
                    Resource childResource = new Resource(childElement, this);
                    this.childResourceMap.put(childResource.name, childResource);
                } else if ("var".equals(tagName)) {
                    Resource childResource = new VarResource(childElement, this);
                    this.childResourceMap.put(childResource.name, childResource);
                } else {
                    throw new GeneralException("Unknown child resource found: " + tagName);
                }
            }
        }

        public String getPathComponent() {
            return name;
        }

        public Map<String, Operation> getMethodOperationMap() {
            return methodOperationMap;
        }

        public Map<String, Resource> getChildResourceMap() {
            return childResourceMap;
        }
    }

    public static class VarResource extends Resource {

        public VarResource(Element resourceElement, Resource parent) throws GeneralException {
            super(resourceElement, parent);
        }

        @Override
        public String getPathComponent() {
            return "{" + super.getPathComponent() + "}";
        }
    }

    public static class Operation {
        private final String method;
        private final String path;
        private final OperationHandler operationHandler;
        private final Map<String, Tag> tags;
        public Operation(Element element, Resource resource) throws GeneralException {
            this.method = element.getAttribute("method");
            this.path = createPath(resource);
            this.operationHandler = createOperationHandler(element);
            this.tags = createTags(element);
        }

        private String createPath(Resource resource) {
            List<String> pathComponents = new ArrayList<>();
            Resource current = resource;
            do {
                pathComponents.add(current.getPathComponent());
                current = current.parent;
            } while (current != null);
            Collections.reverse(pathComponents);
            return "/" + StringUtil.join(pathComponents, "/");
        }

        private OperationHandler createOperationHandler(Element element) throws GeneralException {
            Element handlerElement = UtilXml.firstChildElement(element, "handler");
            if (handlerElement == null) {
                throw new GeneralException("A handler for method \"" + method + "\" is missing");
            }
            Element handlerTypeElement = UtilXml.firstChildElement(handlerElement);
            if (handlerTypeElement == null) {
                throw new GeneralException("A handler type for method \"" + method + "\" is missing");
            }
            String handlerTypeName = handlerTypeElement.getTagName();
            if ("service".equals(handlerTypeName)) {
                return new ServiceOperationHandler(handlerElement);
            } else if ("entity".equals(handlerTypeName)) {
                return new EntityOperationHandler(handlerElement);
            } else {
                throw new GeneralException("Unknown handler type found: " + handlerTypeName);
            }
        }

        private Map<String, Tag> createTags(Element resourceElement) {
            Map<String, Tag> tags = new HashMap<>();
            String tagsAttr = resourceElement.getAttribute("tags");
            List<String> tokens = StringUtil.split(tagsAttr, " ");
            if (UtilValidate.isNotEmpty(tokens)) {
                for (String token : tokens) {
                    token = token.trim();
                    if (!UtilValidate.isEmpty(token)) {
                        Tag tag = tag(token).description(token);
                        tags.put(tag.getName(), tag);
                    }
                }
            }
            return tags;
        }

        public String getMethod() {
            return method;
        }

        public String getPath() {
            return path;
        }

        public OperationHandler getOperationHandler() {
            return operationHandler;
        }

        public Map<String, Tag> getTags() {
            return tags;
        }
    }
}
