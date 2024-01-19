package com.codeages.javaskeletonserver.biz.server.service.impl;

import lombok.extern.slf4j.Slf4j;
import org.apache.http.client.HttpClient;
import org.apache.http.conn.ssl.NoopHostnameVerifier;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.conn.ssl.TrustAllStrategy;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.ssl.SSLContextBuilder;
import org.mitre.dsmiley.httpproxy.ProxyServlet;

import javax.net.ssl.SSLContext;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;

@Slf4j
public class CustomProxyServlet extends ProxyServlet {

    /**
     * 重写HttpClient，跳过ssl认证
     *
     * @return {@link HttpClient}
     */
    @Override
    protected HttpClient createHttpClient() {
        HttpClientBuilder clientBuilder = getHttpClientBuilder()
                .setDefaultRequestConfig(buildRequestConfig())
                .setDefaultSocketConfig(buildSocketConfig());

        clientBuilder.setMaxConnTotal(maxConnections);
        clientBuilder.setMaxConnPerRoute(maxConnections);
        if(! doHandleCompression) {
            clientBuilder.disableContentCompression();
        }
        if (useSystemProperties){
            clientBuilder.useSystemProperties();
        }

        SSLContext sslContext = this.getSslContext();
        SSLConnectionSocketFactory sslSocketFactory = new SSLConnectionSocketFactory(sslContext, NoopHostnameVerifier.INSTANCE);
        clientBuilder.setSSLSocketFactory(sslSocketFactory);
        return super.buildHttpClient(clientBuilder);
    }

    /**
     * 获取sslContext
     * @return {@link SSLContext}
     */
    public SSLContext getSslContext() {
        SSLContext sslContext = null;
        try {
            sslContext = SSLContextBuilder.create()
                                          .loadTrustMaterial(TrustAllStrategy.INSTANCE)
                                          .build();
        } catch (NoSuchAlgorithmException | KeyManagementException | KeyStoreException e) {
            log.error("获取sslContext失败", e);
        }
        return sslContext;
    }

}
