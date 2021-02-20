package org.apache.ofbiz.rest;

import org.apache.http.config.Registry;
import org.apache.http.config.RegistryBuilder;
import org.apache.http.conn.HttpClientConnectionManager;
import org.apache.http.conn.socket.ConnectionSocketFactory;
import org.apache.http.conn.socket.PlainConnectionSocketFactory;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.impl.conn.BasicHttpClientConnectionManager;
import org.apache.juneau.rest.client2.RestClient;
import org.apache.juneau.rest.client2.RestClientBuilder;
import org.apache.juneau.rest.client2.RestResponse;
import org.apache.juneau.rest.mock2.MockRestClient;
import org.junit.Test;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

public class OperationTest {

    @Test
    public void testGET() throws Exception {
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
        SSLConnectionSocketFactory sslConnectionFactory = new SSLConnectionSocketFactory(sslContext, SSLConnectionSocketFactory.ALLOW_ALL_HOSTNAME_VERIFIER);

        PlainConnectionSocketFactory plainConnectionSocketFactory = new PlainConnectionSocketFactory();
        Registry<ConnectionSocketFactory> registry = RegistryBuilder.<ConnectionSocketFactory>create()
                .register("https", sslConnectionFactory)
                .register("http", plainConnectionSocketFactory)
                .build();

        HttpClientConnectionManager ccm = new BasicHttpClientConnectionManager(registry);

        RestClientBuilder restClientBuilder = MockRestClient.create()
                .sslSocketFactory(sslConnectionFactory)
                .connectionManager(ccm)
                .header("Authorization", "Basic YWRtaW46b2ZiaXo=")
                .rootUri("https://localhost:8443/restexample/rest");

        RestClient restClient = restClientBuilder.build();
        RestResponse restResponse = restClient.get("examples/10082").run();
        System.out.println("111 body: " + restResponse.getBody().as(String.class));
    }
}
