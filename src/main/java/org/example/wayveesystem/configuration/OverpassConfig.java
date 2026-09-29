package org.example.wayveesystem.configuration;


import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

@Configuration
public class OverpassConfig {
    @Value("${overpass.api.connect-timeout-ms}")
    int connectTimeoutMs;

    @Value("${overpass.api.read-timeout-ms}")
    int readTimeoutMs;

    @Bean
    RestClient overpassRestClient() {
        var factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(connectTimeoutMs);
        factory.setReadTimeout(readTimeoutMs);
        return RestClient.builder()
                .requestFactory(factory)
                .defaultHeader("User-Agent", "WayveeSystem/1.0 (Contact: admin@wayvee.com)")
                .build();
    }
}
