package org.apache.ofbiz.rest;

import org.apache.http.config.Registry;
import org.apache.http.config.RegistryBuilder;
import org.apache.http.conn.HttpClientConnectionManager;
import org.apache.http.conn.socket.ConnectionSocketFactory;
import org.apache.http.conn.socket.PlainConnectionSocketFactory;
import org.apache.http.conn.ssl.NoopHostnameVerifier;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.impl.conn.BasicHttpClientConnectionManager;
import org.apache.juneau.json.JsonParser;
import org.apache.juneau.marshall.Json;
import org.apache.juneau.rest.client2.RestClient;
import org.apache.juneau.rest.client2.RestClientBuilder;
import org.apache.juneau.rest.client2.RestResponse;
import org.apache.juneau.rest.mock2.MockRestClient;
import org.junit.Test;
import org.junit.*;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import java.util.HashMap;
import java.util.Map;

public class OperationTest {

    private RestClient restClient;

    @Before
    public void setUp() throws Exception {
        restClient = createAllSSLRestClient("https://localhost:8443/restexample/rest");
    }

    @Test
    public void testGET() throws Exception {
        RestResponse restResponse = restClient.get("examples/10082").run();
        restResponse.assertStatus().code().is(200);
        Map body = restResponse.getBody().parser(JsonParser.DEFAULT).as(Map.class);
        String exampleId = (String) body.get("exampleId");
        Assert.assertEquals("10082", exampleId);
    }

    @Test
    public void testPatch() throws Exception {
        Map<String, Object> body = new HashMap<>();
        body.put("exampleName", "test 4");
        RestResponse restResponse = restClient.patch("examples/10082", Json.DEFAULT.toString(body)).run();
        restResponse.assertStatus().code().is(200);
    }

    @Test
    public void testPut() throws Exception {
        Map<String, Object> body = new HashMap<>();
        body.put("exampleTypeId", "CONTRIVED");
        body.put("statusId", "EXST_IN_DESIGN");
        body.put("exampleName", "test 5");
        RestResponse restResponse = restClient.put("examples/10082", Json.DEFAULT.toString(body)).run();
        restResponse.assertStatus().code().is(200);
    }

    private RestClient createAllSSLRestClient(String rootUri) throws Exception {
        TrustManager[] trustAllCerts = new TrustManager[]{new X509TrustManager() {

            @Override
            public java.security.cert.X509Certificate[] getAcceptedIssuers() {
                return null;
            }

            @Override
            public void checkClientTrusted(java.security.cert.X509Certificate[] certs, String authType) {
            }

            @Override
            public void checkServerTrusted(java.security.cert.X509Certificate[] certs, String authType) {
            }
        }};

        SSLContext sslContext = SSLContext.getInstance("SSL");
        sslContext.init(null, trustAllCerts, null);
        SSLConnectionSocketFactory sslConnectionFactory = new SSLConnectionSocketFactory(sslContext, NoopHostnameVerifier.INSTANCE);

        PlainConnectionSocketFactory plainConnectionSocketFactory = new PlainConnectionSocketFactory();
        Registry<ConnectionSocketFactory> registry = RegistryBuilder.<ConnectionSocketFactory>create()
                .register("https", sslConnectionFactory)
                .register("http", plainConnectionSocketFactory)
                .build();

        HttpClientConnectionManager ccm = new BasicHttpClientConnectionManager(registry);

        RestClientBuilder restClientBuilder = MockRestClient.create()
                .sslSocketFactory(sslConnectionFactory)
                .connectionManager(ccm)
                .header("Accept", "application/json")
                .header("Content-Type", "application/json")
                .header("Authorization", "Basic YWRtaW46b2ZiaXo=")
                .rootUri(rootUri);

        return restClientBuilder.build();
    }
}
