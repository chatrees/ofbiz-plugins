package org.apache.ofbiz.rest.swagger.ui;

import org.apache.commons.io.FileUtils;
import org.apache.commons.net.util.Base64;
import org.apache.juneau.html.BasicHtmlDocTemplate;
import org.apache.juneau.html.HtmlDocSerializerSession;
import org.apache.juneau.html.HtmlWriter;
import org.apache.ofbiz.base.util.FileUtil;


/**
 * {@link org.apache.juneau.html.BasicHtmlDocTemplate}
 *
 * See swagger-ui-x.x.x/dist/index.html
 */
public class SwaggerHtmlDocTemplate extends BasicHtmlDocTemplate {

    private static final String SWAGGER_UI_VERSION = "3.40.0";
    private static final String SWAGGER_UI_RESOURCES_ROOT = "component://rest/src/main/resources/org.apache.ofbiz.rest.swagger.ui/swagger-ui-" + SWAGGER_UI_VERSION + "/";
    public static final String ICON_MIME_TYPE = "image/png";
    public static final String ICON_DATA_URL_PREFIX = "data:" + ICON_MIME_TYPE + ";base64,";

    @Override
    protected void head(HtmlDocSerializerSession session, HtmlWriter w, Object o) throws Exception {
        w.oTag("meta").attr("charset", "UTF-8").cTag();
        w.nl(0);
        w.sTag("title").text("Swagger UI").eTag("title");
        w.nl(0);

        String favicon32x32Data = Base64.encodeBase64String(FileUtils.readFileToByteArray(FileUtil.getFile(SWAGGER_UI_RESOURCES_ROOT + "favicon-32x32.png")));
        w.oTag("link")
                .attr("rel", "icon")
                .attr("type", ICON_MIME_TYPE)
                .attr("sizes", "32x32")
                .attr("href", ICON_DATA_URL_PREFIX + favicon32x32Data)
                .ceTag();

        String favicon16x16Data = Base64.encodeBase64String(FileUtils.readFileToByteArray(FileUtil.getFile(SWAGGER_UI_RESOURCES_ROOT + "favicon-16x16.png")));
        w.oTag("link")
                .attr("rel", "icon")
                .attr("type", ICON_MIME_TYPE)
                .attr("sizes", "16x16")
                .attr("href", ICON_DATA_URL_PREFIX + favicon16x16Data)
                .ceTag();

        String swaggerUiCss = FileUtil.readString("UTF-8", FileUtil.getFile(SWAGGER_UI_RESOURCES_ROOT + "swagger-ui.css"));
        w.sTag("style").append(swaggerUiCss).eTag("style");
        w.nl(0);

        w.sTag("style").append("" +
                "html\n" +
                "      {\n" +
                "        box-sizing: border-box;\n" +
                "        overflow: -moz-scrollbars-vertical;\n" +
                "        overflow-y: scroll;\n" +
                "      }\n" +
                "\n" +
                "      *,\n" +
                "      *:before,\n" +
                "      *:after\n" +
                "      {\n" +
                "        box-sizing: inherit;\n" +
                "      }\n" +
                "\n" +
                "      body\n" +
                "      {\n" +
                "        margin:0;\n" +
                "        background: #fafafa;\n" +
                "      }").eTag("style");
        w.nl(0);
    }

    @Override
    protected void body(HtmlDocSerializerSession session, HtmlWriter w, Object o) throws Exception {
        w.oTag("div").attr("id", "swagger-ui").cTag();
        w.eTag("div");
        w.nl(0);

        w.oTag("script").attr("charset", "UTF-8").cTag();
        String swaggerUiBundleScript = FileUtil.readString("UTF-8", FileUtil.getFile(SWAGGER_UI_RESOURCES_ROOT + "swagger-ui-bundle.js"));
        w.append(swaggerUiBundleScript);
        w.eTag("script");
        w.nl(0);

        w.oTag("script").attr("charset", "UTF-8").cTag();
        String swaggerUiStandalonePresetScript = FileUtil.readString("UTF-8", FileUtil.getFile(SWAGGER_UI_RESOURCES_ROOT + "swagger-ui-standalone-preset.js"));
        w.append(swaggerUiStandalonePresetScript);
        w.eTag("script");
        w.nl(0);

        w.flush();
        session.parentSerialize(w, o);
    }
}
