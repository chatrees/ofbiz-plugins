<div id="swagger-ui"></div>
<script>
window.onload = function() {
  // Begin Swagger UI call region
  const ui = SwaggerUIBundle({
    url: "<@ofbizContentUrl>${StringUtil.wrapString(swaggerUrl)?default("${StringUtil.wrapString(request.getContextPath())}/rest?method=OPTIONS")}</@ofbizContentUrl>",
    dom_id: '#swagger-ui',
    deepLinking: true,
    presets: [
      SwaggerUIBundle.presets.apis,
    ],
    plugins: [
      SwaggerUIBundle.plugins.DownloadUrl
    ],
  })
  // End Swagger UI call region

  window.ui = ui
}
</script>
