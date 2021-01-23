package org.apache.ofbiz.rest.swagger.ui;

import org.apache.juneau.BeanSession;
import org.apache.juneau.dto.html5.Script;
import org.apache.juneau.dto.swagger.Swagger;
import org.apache.juneau.transform.PojoSwap;

import static org.apache.juneau.dto.html5.HtmlBuilder.script;

public class SwaggerUI extends PojoSwap<Swagger, Script> {

    @Override
    public Script swap(BeanSession session, Swagger swagger) throws Exception {

        // TODO swagger JSON URL: /rest/?method=OPTIONS
        Script script = script()
                .type("text/javascript")
                .charset("UTF-8")
                .text(
                        "window.onload = function() {" +
                                "}");
        return script;
    }
}
