package com.hs.config;

import okhttp3.OkHttpClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.OkHttp3ClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

import java.util.concurrent.TimeUnit;

/**
 * FileName: RestTemplateConfig
 * Author:   HS
 * Date:     2020/4/14 23:39
 * Description:
 */
@Configuration
public class RestTemplateConfig {
//    @Bean
//    public RestTemplate restTemplate(ClientHttpRequestFactory factory) {
//        return new RestTemplate(factory);
//    }

    @Bean
    public RestTemplate restTemplate() {
        return new RestTemplate(getOK());
    }

    private OkHttp3ClientHttpRequestFactory getOK() {

        OkHttpClient build = new OkHttpClient().newBuilder()
                .connectTimeout(15, TimeUnit.SECONDS)
                .readTimeout(15, TimeUnit.SECONDS)
                .writeTimeout(15, TimeUnit.SECONDS)
//                .addInterceptor(new LogInterceptor())
//                .addInterceptor(new TokenInterceptor())
                .sslSocketFactory(SSLSocketClient.getSSLSocketFactory())//配置
                .hostnameVerifier(SSLSocketClient.getHostnameVerifier())//配置
                .build();
        OkHttp3ClientHttpRequestFactory a = new OkHttp3ClientHttpRequestFactory(build);
        return a;
    }
//    @Bean
//    public ClientHttpRequestFactory simpleClientHttpRequestFactory() {
//        com.httpclient.SSL factory = new com.httpclient.SSL();
//        factory.setReadTimeout(5000);
//        factory.setConnectTimeout(15000);
//        return factory;
//    }



}
