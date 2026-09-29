package org.example.wayveesystem.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.wayveesystem.service.WikimediaService;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriBuilder;

import java.net.URI;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.Semaphore;
import java.util.function.Function;

@Service
public class WikimediaServiceImpl implements WikimediaService {

    private static final int MAX_RETRIES = 3;
    private static final long MIN_INTERVAL_MS = 300;
    private static final long DEFAULT_BACKOFF_MS = 1000;

    private final RestClient client;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final Semaphore concurrencyLimiter = new Semaphore(1);
    private volatile long lastRequestAtMs = 0L;

    public WikimediaServiceImpl() {
        var factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(2000);
        factory.setReadTimeout(3000);
        this.client = RestClient.builder().baseUrl("https://commons.wikimedia.org/w/api.php")
                .requestFactory(factory).defaultHeader("User-Agent", "WayveeSystem/1.0 (location image resolver)").build();
    }

    @Override
    public String resolveImage(String commonsFile) {
        String title = commonsFile.trim();
        if (!title.regionMatches(true, 0, "File:", 0, 5)) title = "File:" + title;
        final String fileTitle = title;

        JsonNode json = fetchJson(uri -> uri.queryParam("action", "query").queryParam("format", "json")
                .queryParam("prop", "imageinfo").queryParam("titles", fileTitle)
                .queryParam("iiprop", "url").queryParam("iiurlwidth", 800).build());
        return extractFirstImageUrl(json);
    }

    @Override
    public String searchByNameAndLocation(String name, double lat, double lon) {
        JsonNode json = fetchJson(uri -> uri.queryParam("action", "query")
                .queryParam("format", "json")
                .queryParam("generator", "geosearch")
                .queryParam("ggscoord", lat + "|" + lon)
                .queryParam("ggsradius", 200)
                .queryParam("ggsnamespace", 6)
                .queryParam("ggslimit", 5)
                .queryParam("prop", "imageinfo")
                .queryParam("iiprop", "url")
                .queryParam("iiurlwidth", 800)
                .build());
        return extractFirstImageUrl(json);
    }

    private String extractFirstImageUrl(JsonNode json) {
        if (json == null) return null;
        JsonNode pages = json.path("query").path("pages");
        for (JsonNode page : pages) {
            JsonNode info = page.path("imageinfo");
            if (info.isArray() && !info.isEmpty()) {
                String thumb = info.get(0).path("thumburl").asText(null);
                return thumb != null ? thumb : info.get(0).path("url").asText(null);
            }
        }
        return null;
    }

    /**
     * Gọi Wikimedia với: (1) tối đa 1 request đang chạy cùng lúc, (2) khoảng cách
     * tối thiểu giữa 2 request liên tiếp, (3) retry có backoff khi gặp 429.
     */
    private JsonNode fetchJson(Function<UriBuilder, URI> uriBuilderFn) {
        for (int attempt = 1; attempt <= MAX_RETRIES; attempt++) {
            try {
                concurrencyLimiter.acquire();
                try {
                    throttle();
                    String raw = client.get().uri(uriBuilderFn).retrieve().body(String.class);
                    return parse(raw);
                } finally {
                    concurrencyLimiter.release();
                }
            } catch (HttpClientErrorException.TooManyRequests ex) {
                if (attempt == MAX_RETRIES) {
                    throw new RuntimeException("Wikimedia rate limit exceeded after " + MAX_RETRIES + " attempts", ex);
                }
                long waitMs = retryAfterMillis(ex).orElse(DEFAULT_BACKOFF_MS * attempt);
                sleep(waitMs);
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
                throw new RuntimeException("Interrupted while waiting for Wikimedia rate limiter", ex);
            }
        }
        return null;
    }

    private void throttle() {
        synchronized (this) {
            long elapsed = System.currentTimeMillis() - lastRequestAtMs;
            if (elapsed < MIN_INTERVAL_MS) {
                sleep(MIN_INTERVAL_MS - elapsed);
            }
            lastRequestAtMs = System.currentTimeMillis();
        }
    }

    private Optional<Long> retryAfterMillis(HttpClientErrorException.TooManyRequests ex) {
        List<String> values = ex.getResponseHeaders() != null
                ? ex.getResponseHeaders().get("Retry-After") : null;
        if (values == null || values.isEmpty()) return Optional.empty();
        try {
            return Optional.of(Long.parseLong(values.get(0)) * 1000L);
        } catch (NumberFormatException nfe) {
            return Optional.empty();
        }
    }

    private JsonNode parse(String rawJson) {
        try {
            return rawJson == null || rawJson.isBlank() ? null : objectMapper.readTree(rawJson);
        } catch (Exception e) {
            throw new RuntimeException("Failed to parse Wikimedia response: " + e.getMessage(), e);
        }
    }

    private void sleep(long ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
        }
    }
}