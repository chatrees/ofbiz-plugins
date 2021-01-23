package org.apache.ofbiz.rest.swagger.ui;

import org.apache.juneau.html.BasicHtmlDocTemplate;
import org.apache.juneau.html.HtmlDocSerializerSession;
import org.apache.juneau.html.HtmlWriter;

/**
 * {@link org.apache.juneau.html.BasicHtmlDocTemplate}
 *
 * See swagger-ui-x.x.x/dist/index.html
 */
public class SwaggerHtmlDocTemplate extends BasicHtmlDocTemplate {
    @Override
    protected void head(HtmlDocSerializerSession session, HtmlWriter w, Object o) throws Exception {
        // TODO
        w.sTag("meta").attr("charset", "UTF-8");
    }

    @Override
    protected void body(HtmlDocSerializerSession session, HtmlWriter w, Object o) throws Exception {
        // TODO
        w.sTag("div").attr("id", "swagger-ui");
        w.eTag("div");
        w.flush();
        session.parentSerialize(w, o);
    }
}
