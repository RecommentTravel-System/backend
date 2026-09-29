package org.example.wayveesystem.configuration;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

@Configuration
public class NominatimConfig {

    @Value("${nominatim.api.connect-timeout-ms:5000}")
    private int connectTimeoutMs;

    @Value("${nominatim.api.read-timeout-ms:8000}")
    private int readTimeoutMs;

    @Bean
    public RestClient nominatimRestClient() {
        var factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(connectTimeoutMs);
        factory.setReadTimeout(readTimeoutMs);

        return RestClient.builder()
                .baseUrl("https://nominatim.openstreetmap.org")
                .requestFactory(factory)
                .defaultHeader("User-Agent", "Wayvee/1.0 (student project)")
                .build();
    }
}
