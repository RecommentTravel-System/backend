package org.example.wayveesystem.common.client;

import com.google.common.util.concurrent.RateLimiter;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.example.wayveesystem.common.exception.ErrorCode;
import org.example.wayveesystem.common.exception.ExternalMapServiceException;
import org.example.wayveesystem.dto.request.OverpassRequest;
import org.example.wayveesystem.dto.response.OverpassResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.Semaphore;
import java.util.stream.Collectors;

@Component
@Slf4j
public class OverpassApiClient {

    private final RestClient overpassRestClient;

    @Value("${overpass.api.primary-url}")
    private String primaryUrl;

    @Value("${overpass.api.fallback-url}")
    private String fallbackUrl;

    @Value("${overpass.rate-limit.requests-per-second:1.0}")
    private double requestsPerSecond;

    // MỚI: giới hạn số connection Overpass mở đồng thời (không liên quan tới size của overpassExecutor)
    @Value("${overpass.concurrency.max-parallel-requests:2}")
    private int maxParallelRequests;

    @Value("${overpass.retry.max-attempts:3}")
    private int maxAttempts;

    @Value("${overpass.retry.initial-backoff-ms:1000}")
    private long initialBackoffMs;

    private RateLimiter rateLimiter;

    private Semaphore concurrencyLimiter;// MỚI

    public OverpassApiClient(RestClient overpassRestClient) {
        this.overpassRestClient = overpassRestClient;
    }

    @PostConstruct
    public void init() {
        this.rateLimiter = RateLimiter.create(requestsPerSecond);
        this.concurrencyLimiter = new Semaphore(maxParallelRequests, true); // fair=true để tránh starvation
        log.info("Initialized OverpassApiClient: rate={} req/sec, maxParallel={}",
                requestsPerSecond, maxParallelRequests);
    }



    public OverpassResponse fetchNearbyPois(OverpassRequest request) {
        String query = buildQuery(request);
        return executeWithRetryAndFallback(query);
    }

    /**
     * Fetches a single OSM element by raw Overpass QL query.
     * Used for individual POI lookup by osmId.
     */
    public OverpassResponse fetchSingleElement(String query) {
        return executeWithRetryAndFallback(query);
    }

    private OverpassResponse executeWithRetryAndFallback(String query) {
        try {
            return executeWithRetry(primaryUrl, query);
        } catch (Exception primaryEx) {
            log.warn("Primary Overpass endpoint [{}] failed or exhausted retries: {}. Trying fallback endpoint [{}]...",
                    primaryUrl, primaryEx.getMessage(), fallbackUrl);
            try {
                return executeWithRetry(fallbackUrl, query);
            } catch (Exception fallbackEx) {
                log.error("Fallback Overpass endpoint [{}] also failed: {}", fallbackUrl, fallbackEx.getMessage(), fallbackEx);
                throw new ExternalMapServiceException(ErrorCode.EXTERNAL_MAP_SERVICE_UNAVAILABLE, fallbackEx);
            }
        }
    }

    private OverpassResponse executeWithRetry(String url, String query) {
        long backoffMs = initialBackoffMs;

        for (int attempt = 1; attempt <= maxAttempts; attempt++) {
            rateLimiter.acquire();
            try {
                return callOverpass(url, query);
            } catch (HttpClientErrorException.TooManyRequests ex) {
                log.warn("Overpass API [{}] returned 429 Too Many Requests (attempt {}/{}). Retrying in {}ms...",
                        url, attempt, maxAttempts, backoffMs);
                if (attempt == maxAttempts) {
                    throw ex;
                }
                sleep(backoffMs);
                backoffMs *= 2;
            } catch (ResourceAccessException timeoutEx) {
                log.warn("Overpass API [{}] timeout on attempt {}/{}: {}",
                        url, attempt, maxAttempts, timeoutEx.getMessage());
                if (attempt == maxAttempts) {
                    throw new ExternalMapServiceException(ErrorCode.EXTERNAL_MAP_TIMEOUT, timeoutEx);
                }
                sleep(backoffMs);
                backoffMs *= 2;
            } catch (RestClientException otherEx) {
                log.warn("Overpass API [{}] failed with client error on attempt {}/{}: {}",
                        url, attempt, maxAttempts, otherEx.getMessage());
                throw otherEx;
            }
        }
        throw new ExternalMapServiceException(ErrorCode.EXTERNAL_MAP_SERVICE_UNAVAILABLE);
    }

    private OverpassResponse callOverpass(String url, String query) {
        try {
            concurrencyLimiter.acquire(); // chờ tới khi có slot trống (tối đa 2 request chạy song song)
            try {
                return overpassRestClient.post()
                        .uri(url)
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .body("data=" + URLEncoder.encode(query, StandardCharsets.UTF_8))
                        .retrieve()
                        .body(OverpassResponse.class);
            } finally {
                concurrencyLimiter.release(); // LUÔN trả slot lại, kể cả khi lỗi
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new ExternalMapServiceException(ErrorCode.EXTERNAL_MAP_SERVICE_UNAVAILABLE, e);
        }
    }

    private void sleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("Retry backoff interrupted", e);
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
        return "[out:json][timeout:30];(%s);out center qt 1000;".formatted(filterClauses);
    }
}
