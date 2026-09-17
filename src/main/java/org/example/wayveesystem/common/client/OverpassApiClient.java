package org.example.wayveesystem.common.client;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.wayveesystem.common.exception.ErrorCode;
import org.example.wayveesystem.common.exception.ExternalMapServiceException;
import org.example.wayveesystem.dto.request.OverpassRequest;
import org.example.wayveesystem.dto.response.OverpassResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
@Slf4j
public class OverpassApiClient {

    private final RestClient overpassRestClient;

    @Value("${overpass.api.primary-url}")
    private String primaryUrl;

    @Value("${overpass.api.fallback-url}")
    private String fallbackUrl;

    public OverpassResponse fetchNearbyPois(OverpassRequest request) {
        String query = buildQuery(request);
        try {
            return callOverpass(primaryUrl, query);
        } catch (RestClientException primaryEx) {
            log.warn("Primary Overpass endpoint failed, trying fallback", primaryEx);
            try {
                return callOverpass(fallbackUrl, query);
            } catch (RestClientException fallbackEx) {
                log.error("Fallback Overpass endpoint also failed", fallbackEx);
                throw new ExternalMapServiceException(ErrorCode.EXTERNAL_MAP_SERVICE_UNAVAILABLE, fallbackEx);
            }
        }
    }

    private OverpassResponse callOverpass(String url, String query) {
        try {
            return overpassRestClient.post()
                    .uri(url)
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .body("data=" + URLEncoder.encode(query, StandardCharsets.UTF_8))
                    .retrieve()
                    .body(OverpassResponse.class);
        } catch (ResourceAccessException timeoutEx) {
            throw new ExternalMapServiceException(ErrorCode.EXTERNAL_MAP_TIMEOUT, timeoutEx);
        }
    }

    private String buildQuery(OverpassRequest request) {
        String filterClauses = request.osmFilters().stream()
                .map(f -> {
                    if (f.startsWith("node[") || f.startsWith("way[")) {
                        return f;
                    }
                    return String.format(java.util.Locale.US, "node[\"%s\"](around:%d,%.6f,%.6f);", f, request.radiusMeters(), request.lat(), request.lng());
                })
                .collect(Collectors.joining());
        return "[out:json][timeout:30];(%s);out center qt;".formatted(filterClauses);
    }
}