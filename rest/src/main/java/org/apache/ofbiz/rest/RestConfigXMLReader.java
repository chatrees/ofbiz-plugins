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

        private final String title;
        private final String description;
        private final List<Operation> operations = new ArrayList<>();
        private final Map<String, List<Operation>> methodOperationNodesMap = new HashMap<>();
        private final Map<String, Tag> tags = new HashMap<>();

        public RestConfig(URL url) throws WebAppConfigurationException {
            Element rootElement = loadDocument(url);
            if (rootElement != null) {
                Element titleElement = UtilXml.firstChildElement(rootElement, "title");
                if (titleElement != null) {
                    title = titleElement.getTextContent();
                } else {
                    title = null;
                }

                Element descriptionElement = UtilXml.firstChildElement(rootElement, "description");
                if (descriptionElement != null) {
                    description = descriptionElement.getTextContent();
                } else {
                    description = null;
                }

                loadResource(rootElement);
            } else {
                title = null;
                description = null;
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

        public String getTitle() {
            return title;
        }

        public String getDescription() {
            return description;
        }

        public List<Operation> getOperations() {
            return Collections.unmodifiableList(operations);
        }

        public List<Operation> getOperations(String method) {
            return methodOperationNodesMap.get(method);
        }

        public Collection<Tag> getTags() {
            return tags.values();
        }

        public Map<String, String> getOperationHandlerMap() {
            // TODO load from rest.xml
            return UtilMisc.toMap(
                    "service", "org.apache.ofbiz.rest.operation.ServiceOperationHandler",
                    "entity", "org.apache.ofbiz.rest.operation.EntityOperationHandler");
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
                } else if ("variable".equals(tagName)) {
                    Resource childResource = new VariableResource(childElement, this);
                    this.childResourceMap.put(childResource.name, childResource);
                } else {
                    throw new GeneralException("Unknown child resource found: " + tagName);
                }
            }
        }

        public String getName() {
            return name;
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

    public static class VariableResource extends Resource {

        public VariableResource(Element resourceElement, Resource parent) throws GeneralException {
            super(resourceElement, parent);
        }

        @Override
        public String getPathComponent() {
            return "{" + super.getPathComponent() + "}";
        }
    }

    public static class Operation {
        private final String id;
        private final String method;
        private final String path;
        private final Security security;
        private final Element handlerElement;
        private final Map<String, Tag> tags;
        private final List<VariableResource> variableResources = new ArrayList<>();
        private String resourcePath;
        public Operation(Element element, Resource resource) throws GeneralException {
            loadResource(resource);
            String id = StringUtil.replaceString(resourcePath, "{", "");
            id = StringUtil.replaceString(id, "}", "");
            id = StringUtil.replaceString(id, "/", "_");
            this.id = id;
            this.method = element.getAttribute("method");
            this.path = "/" + resourcePath;
            this.handlerElement = getHandlerElement(element);
            this.tags = createTags(element);

            // security
            Element securityElement = UtilXml.firstChildElement(element, "security");
            if (securityElement != null) {
                this.security = new Security(securityElement);
            } else {
                this.security = new Security();
            }
        }

        private void loadResource(Resource resource) {
            List<String> pathComponents = new ArrayList<>();
            Resource current = resource;
            do {
                if (current instanceof VariableResource) {
                    variableResources.add((VariableResource) current);
                }
                pathComponents.add(current.getPathComponent());
                current = current.parent;
            } while (current != null);
            Collections.reverse(pathComponents);
            this.resourcePath = StringUtil.join(pathComponents, "/");
        }

        private Element getHandlerElement(Element element) throws GeneralException {
            Element handlerWrapperElement = UtilXml.firstChildElement(element, "handler");
            if (handlerWrapperElement == null) {
                throw new GeneralException("A handler for method \"" + method + "\" is missing");
            }
            Element handlerElement = UtilXml.firstChildElement(handlerWrapperElement);
            if (handlerElement == null) {
                throw new GeneralException("A handler for method \"" + method + "\" is missing");
            }
            return handlerElement;
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

        public String getId() {
            return id;
        }

        public String getMethod() {
            return method;
        }

        public String getPath() {
            return path;
        }

        public Security getSecurity() {
            return security;
        }

        public Element getHandlerElement() {
            return handlerElement;
        }

        public List<VariableResource> getVariableResources() {
            return Collections.unmodifiableList(variableResources);
        }

        public Map<String, Tag> getTags() {
            return tags;
        }
    }

    public static class Security {
        private final boolean https;
        private final boolean auth;

        Security(Element element) {
            https = "true".equals(element.getAttribute("https"));
            auth = "true".equals(element.getAttribute("auth"));
        }

        Security() {
            https = false;
            auth = false;
        }

        public boolean isHttps() {
            return https;
        }

        public boolean isAuth() {
            return auth;
        }
    }
}
