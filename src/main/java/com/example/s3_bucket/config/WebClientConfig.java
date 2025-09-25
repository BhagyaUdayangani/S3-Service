package com.example.s3_bucket.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.ExchangeStrategies;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
public class WebClientConfig {

    @Value("${service.base.url.user}")
    private String userBaseUrl;

    @Value("${service.base.url.search}")
    private String searchBaseUrl;

    @Value("${service.base.url.feed}")
    private String feedBaseUrl;

    @Value("${service.base.url.reel}")
    private String reelBaseUrl;

    @Value("${service.base.url.trick}")
    private String trickBaseUrl;

    @Value("${service.base.url.images}")
    private String imagesBaseUrl;

    private ExchangeStrategies createLargeBufferStrategy() {
        int size = 16 * 1024 * 1024; // 16 MB
        return ExchangeStrategies.builder()
                .codecs(configurer -> configurer
                        .defaultCodecs()
                        .maxInMemorySize(size))
                .build();
    }

    @Bean(name = "userWebClient")
    public WebClient userWebClient() {
        return WebClient.builder()
                .baseUrl(userBaseUrl)
                .exchangeStrategies(createLargeBufferStrategy())
                .build();
    }

    @Bean(name = "searchWebClient")
    public WebClient searchWebClient() {
        return WebClient.builder()
                .baseUrl(searchBaseUrl)
                .exchangeStrategies(createLargeBufferStrategy())
                .build();
    }

    @Bean(name = "feedWebClient")
    public WebClient feedWebClient() {
        return WebClient.builder()
                .baseUrl(feedBaseUrl)
                .exchangeStrategies(createLargeBufferStrategy())
                .build();
    }

    @Bean(name = "reelWebClient")
    public WebClient reelWebClient() {
        return WebClient.builder()
                .baseUrl(reelBaseUrl)
                .exchangeStrategies(createLargeBufferStrategy())
                .build();
    }

    @Bean(name = "trickWebClient")
    public WebClient trickWebClient() {
        return WebClient.builder()
                .baseUrl(trickBaseUrl)
                .exchangeStrategies(createLargeBufferStrategy())
                .build();
    }

    @Bean(name = "imagesWebClient")
    public WebClient imagesWebClient() {
        return WebClient.builder()
                .baseUrl(imagesBaseUrl)
                .exchangeStrategies(createLargeBufferStrategy())
                .build();
    }
}