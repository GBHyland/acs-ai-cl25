package org.alfresco.ke.datacuration;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.alfresco.ke.common.AbstractApiClient;
import org.alfresco.ke.common.OAuthTokenManager;
import org.alfresco.ke.config.AppProperties;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.*;

/**
 * Client for communicating with the Data Curation API.
 * <p>Handles OAuth2 access token acquisition, retry logic, S3 uploads,
 * and result polling via presigned or authenticated endpoints.</p>
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class DataCurationClient extends AbstractApiClient {

    private static final String SCOPE_ENVIRONMENT_AUTHORIZATION = "environment_authorization";

    private final RestTemplate restTemplate;
    private final AppProperties config;
    private final OAuthTokenManager tokenManager;

    private static final RestTemplate RAW_TEMPLATE;
    static {
        RAW_TEMPLATE = new RestTemplate();
        RAW_TEMPLATE.setInterceptors(List.of());
    }

    @Override
    protected int maxRetries() {
        return config.getSecurity().getMaxRetries();
    }

    @Override
    protected Duration retryDelay() {
        return config.getSecurity().getRetryDelay();
    }

    /**
     * Retrieve OAuth2 access token using shared token manager.
     */
    public String getAccessToken() {
        return tokenManager.getAccessToken(
                config.getDataCuration(),
                SCOPE_ENVIRONMENT_AUTHORIZATION,
                "data-curation"
        );
    }

    /**
     * Invalidate cached token to force re-authentication.
     */
    public void invalidateToken() {
        tokenManager.invalidate("data-curation");
    }

    /**
     * Request presigned S3 upload/download URLs from Data Curation API.
     */
    public Map<String, Object> presign(String accessToken, String fileName) {
        String url = buildUrl(config.getDataCuration().getApiUrl(), "/presign");
        return executeWithRetry(() -> {
            HttpHeaders headers = createBearerHeaders(accessToken);
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<Map<String, String>> entity = new HttpEntity<>(Map.of("fileName", fileName), headers);

            ParameterizedTypeReference<Map<String, Object>> typeRef =
                    new ParameterizedTypeReference<Map<String, Object>>() {};

            ResponseEntity<Map<String, Object>> resp = restTemplate.exchange(
                    url, HttpMethod.POST, entity, typeRef);

            validateResponse(resp, "presign request");
            return resp.getBody();
        }, "presign request");
    }


    /**
     * Upload file bytes directly to S3 using presigned URL.
     */
    public void putToS3(String presignedUrl, byte[] bytes, String declaredType) {
        String contentType = Arrays.stream(presignedUrl.split("[?&]"))
                .filter(p -> p.startsWith("content-type="))
                .findFirst()
                .map(p -> URLDecoder.decode(p.substring(13), StandardCharsets.UTF_8))
                .orElse(declaredType);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.parseMediaType(contentType));
        headers.setContentLength(bytes.length);

        URI uri = UriComponentsBuilder.fromUriString(presignedUrl).build(true).toUri();

        executeWithRetry(() -> {
            RAW_TEMPLATE.exchange(uri, HttpMethod.PUT, new HttpEntity<>(bytes, headers), Void.class);
            return null;
        }, "file upload to S3");
    }

    /**
     * General-purpose result retrieval, with or without authentication.
     */
    public Map<String, Object> getResults(String url) {
        return executeWithRetry(() -> {
            boolean needsAuth = needsBearerAuth(url);
            HttpHeaders headers = needsAuth ? createBearerHeaders(getAccessToken()) : createJsonHeaders();
            RestTemplate template = needsAuth ? restTemplate : RAW_TEMPLATE;

            try {
                ParameterizedTypeReference<Map<String, Object>> typeRef =
                        new ParameterizedTypeReference<>() {};

                ResponseEntity<Map<String, Object>> resp = template.exchange(
                        url, HttpMethod.GET, new HttpEntity<>(headers), typeRef);

                return resp.getBody();
            } catch (HttpClientErrorException e) {
                if (e.getStatusCode() == HttpStatus.NOT_FOUND || e.getStatusCode() == HttpStatus.FORBIDDEN)
                    return null;
                if (needsAuth && e.getStatusCode() == HttpStatus.UNAUTHORIZED) {
                    invalidateToken();
                    throw e;
                }
                return Map.of("error", e.getMessage(), "status", e.getStatusCode().value());
            }
        }, "get-results");
    }

    /**
     * Downloads JSON results from S3 via presigned URL.
     */
    public Map<String, Object> getPresignedResults(String presignedUrl) {
        return executeWithRetry(() -> {
            try {
                URI uri = UriComponentsBuilder.fromUriString(presignedUrl).build(true).toUri();
                var resp = RAW_TEMPLATE.exchange(uri, HttpMethod.GET,
                        new HttpEntity<>(createJsonHeaders()), String.class);
                String body = resp.getBody();
                if (body == null || body.isBlank()) return null;
                return new ObjectMapper().readValue(body, new TypeReference<>() {});
            } catch (HttpClientErrorException e) {
                if (List.of(HttpStatus.NOT_FOUND, HttpStatus.FORBIDDEN, HttpStatus.BAD_REQUEST).contains(e.getStatusCode()))
                    return null;
                return Map.of("error", e.getMessage(), "status", e.getStatusCode().value());
            } catch (Exception e) {
                log.warn("Presigned-URL download failed: {}", e.getMessage());
                return null;
            }
        }, "presigned-results");
    }

    /**
     * Convenience wrapper for /status/{jobId}.
     */
    public Map<String, Object> status(String jobId) {
        return getResults(buildUrl(config.getDataCuration().getApiUrl(), "/status/" + jobId));
    }

    /**
     * Convenience wrapper for /results/{jobId}.
     */
    public Map<String, Object> results(String jobId) {
        return getResults(buildUrl(config.getDataCuration().getApiUrl(), "/results/" + jobId));
    }

    /**
     * Attempts to extract a known presigned URL key from a result JSON.
     */
    public Optional<String> extractPresignedUrl(Map<String, Object> json) {
        if (json == null) return Optional.empty();
        for (String key : List.of("get_url", "getUrl", "result_url", "resultUrl")) {
            Object val = json.get(key);
            if (val instanceof String s && s.startsWith("http"))
                return Optional.of(s);
        }
        return Optional.empty();
    }

    /* ──────────────────────── Internal Helpers ──────────────────────── */

    private boolean needsBearerAuth(String url) {
        return url.startsWith(config.getDataCuration().getApiUrl()) && !url.contains("/results/");
    }

}
