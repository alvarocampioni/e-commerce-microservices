package com.ms.comment_service.config;

import com.ms.comment_service.client.ProductClient;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.support.RestClientAdapter;
import org.springframework.web.service.invoker.HttpServiceProxyFactory;

@Service
public class RestClientConfig {

    @Bean
    public ProductClient productClient() {
        RestClient client = RestClient.builder()
                .baseUrl("http://product-service:8080")
                .build();
        RestClientAdapter adapter = RestClientAdapter.create(client);
        HttpServiceProxyFactory httpServiceProxyFactory = HttpServiceProxyFactory.builderFor(adapter).build();
        return httpServiceProxyFactory.createClient(ProductClient.class);
    }
}
