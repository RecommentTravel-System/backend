package org.example.wayveesystem.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.example.wayveesystem.service.MapillaryService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.http.client.SimpleClientHttpRequestFactory;

@Service
@Slf4j
public class MapillaryServiceImpl implements MapillaryService {
    private final RestClient client;
    private final String accessToken;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public MapillaryServiceImpl(@Value("${mapillary.access-token}") String accessToken) {
        this.accessToken = accessToken;
        if (accessToken == null || accessToken.isBlank()) {
            log.warn("Mapillary is disabled because MAPILLARY_ACCESS_TOKEN is blank");
        }
        var factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(2000);
        factory.setReadTimeout(3000);
        this.client = RestClient.builder().baseUrl("https://graph.mapillary.com").requestFactory(factory).build();
    }

    @Override
    public String resolveImage(String mapillaryId) {
        if (accessToken == null || accessToken.isBlank()) return null;
        String id = mapillaryId.trim();
        try {
            var uri = java.net.URI.create(id);
            String query = uri.getRawQuery();
            if (query != null) {
                for (String parameter : query.split("&")) {
                    String[] pair = parameter.split("=", 2);
                    if (pair.length == 2 && (pair[0].equals("pKey") || pair[0].equals("key"))) {
                        id = java.net.URLDecoder.decode(pair[1], java.nio.charset.StandardCharsets.UTF_8);
                        break;
                    }
                }
            }
            if (id.equals(mapillaryId.trim())) {
                String path = uri.getPath();
                if (path != null && !path.isBlank()) id = path.substring(path.lastIndexOf('/') + 1);
            }
        } catch (IllegalArgumentException ignored) {
            // OSM may contain the raw image key instead of a Mapillary URL.
        }
        final String imageId = id;

        String raw = client.get().uri(uri -> uri.path("/" + imageId).queryParam("fields", "thumb_1024_url")
                        .queryParam("access_token", accessToken).build())
                .retrieve().body(String.class);
        JsonNode json = parse(raw);
        return json == null ? null : json.path("thumb_1024_url").asText(null);
    }

    @Override
    public String searchNearby(double lat, double lon) {
        if (accessToken == null || accessToken.isBlank()) return null;
        double delta = 0.0005; // ~50m quanh điểm
        String bbox = (lon - delta) + "," + (lat - delta) + "," + (lon + delta) + "," + (lat + delta);

        String raw = client.get().uri(uri -> uri.path("/images")
                        .queryParam("access_token", accessToken)
                        .queryParam("fields", "thumb_1024_url")
                        .queryParam("bbox", bbox)
                        .queryParam("limit", 1)
                        .build())
                .retrieve().body(String.class);
        JsonNode json = parse(raw);
        if (json == null) return null;
        JsonNode data = json.path("data");
        return (data.isArray() && !data.isEmpty()) ? data.get(0).path("thumb_1024_url").asText(null) : null;
    }

    private JsonNode parse(String rawJson) {
        try {
            return rawJson == null || rawJson.isBlank() ? null : objectMapper.readTree(rawJson);
        } catch (Exception e) {
            throw new RuntimeException("Failed to parse Mapillary response: " + e.getMessage(), e);
        }
    }
}