package org.apache.ofbiz.rest.swagger.ui;

import org.apache.juneau.annotation.BeanConfig;
import org.apache.juneau.dto.swagger.Swagger;
import org.apache.juneau.html.HtmlDocSerializer;
import org.apache.juneau.html.annotation.HtmlDocConfig;
import org.apache.juneau.rest.RestRequest;
import org.apache.juneau.rest.RestServlet;
import org.apache.juneau.rest.annotation.RestMethod;
import org.apache.juneau.rest.config.BasicUniversalRest;

import static org.apache.juneau.http.HttpMethod.GET;

/**
 * @see HtmlDocSerializer#HtmlDocSerializer(org.apache.juneau.PropertyStore, java.lang.String, java.lang.String)
 */
@HtmlDocConfig(
        template = SwaggerHtmlDocTemplate.class
)
@BeanConfig(
        // POJO swaps to apply to all serializers/parsers on this method.
        swaps={
                // Use the SwaggerUI swap when rendering Swagger beans.
                // This is a per-media-type swap that only applies to text/html requests.
                SwaggerUI.class
        }
)
public class SwaggerUIServlet extends RestServlet implements BasicUniversalRest {

    private static final String MODULE = SwaggerUIServlet.class.getName();

    @RestMethod(name=GET, path="/*")
    public Swagger getOptions(RestRequest req) {
        return req.getSwagger();
    }
}
