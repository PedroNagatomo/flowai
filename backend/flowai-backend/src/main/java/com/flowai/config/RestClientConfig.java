package com.flowai.config;

import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

import java.time.Duration;

@Configuration
public class RestClientConfig {

    @Bean
    public RestClient.Builder restClientBuilder(RestTemplateBuilder builder) {
        return RestClient.builder(
                builder
                        .setConnectTimeout(Duration.ofSeconds(10))
                        .setReadTimeout(Duration.ofSeconds(60))
                        .build()
        );
    }
}