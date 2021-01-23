package org.apache.ofbiz.rest.swagger.ui;

import org.apache.juneau.BeanSession;
import org.apache.juneau.dto.html5.Script;
import org.apache.juneau.dto.swagger.Swagger;
import org.apache.juneau.transform.PojoSwap;

import static org.apache.juneau.dto.html5.HtmlBuilder.script;

public class SwaggerUI extends PojoSwap<Swagger, Script> {

    @Override
    public Script swap(BeanSession session, Swagger swagger) throws Exception {
        String url = swagger.getBasePath() + "?method=OPTIONS";
        Script script = script()
                .type("text/javascript")
                .charset("UTF-8")
                .text(
                        "window.onload = function() {\n" +
                        "      // Begin Swagger UI call region\n" +
                        "      const ui = SwaggerUIBundle({\n" +
                        "        url: \"" + url + "\",\n" +
                        "        dom_id: '#swagger-ui',\n" +
                        "        deepLinking: true,\n" +
                        "        presets: [\n" +
                        "          SwaggerUIBundle.presets.apis,\n" +
                        "          SwaggerUIStandalonePreset\n" +
                        "        ],\n" +
                        "        plugins: [\n" +
                        "          SwaggerUIBundle.plugins.DownloadUrl\n" +
                        "        ],\n" +
                        "        layout: \"StandaloneLayout\"\n" +
                        "      })\n" +
                        "      // End Swagger UI call region\n" +
                        "\n" +
                        "      window.ui = ui\n" +
                        "    }"
                );
        return script;
    }
}
