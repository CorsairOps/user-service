package com.corsairops.corsairopsuserservice.config;

import com.corsairops.corsairopsuserservice.client.AuthServiceClient;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.support.WebClientAdapter;
import org.springframework.web.service.invoker.HttpServiceProxyFactory;

@Configuration
@RequiredArgsConstructor
public class WebClientConfig {
    private final AuthServiceProperties authServiceProperties;


    @Bean
    public AuthServiceClient authServiceClient() {
        WebClient webClient = WebClient.builder().baseUrl(authServiceProperties.getUrl()).build();
        WebClientAdapter adapter = WebClientAdapter.create(webClient);
        HttpServiceProxyFactory factory = HttpServiceProxyFactory.builderFor(adapter).build();
        return factory.createClient(AuthServiceClient.class);
    }
}