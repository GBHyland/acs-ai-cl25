package org.alfresco.ke.contextenrichment;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
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
import java.time.Duration;
import java.util.*;

/**
 * Client for communicating with the Context Enrichment API.
 * Supports OAuth2 authentication, job initiation, polling, file upload, and available action discovery.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ContextEnrichmentClient extends AbstractApiClient {

    private static final List<String> JOB_ID_FIELDS = List.of("processingId", "jobId", "id");

    private final RestTemplate restTemplate;
    private final AppProperties config;
    private final ObjectMapper objectMapper;
    private final OAuthTokenManager tokenManager;

    @Override
    protected int maxRetries() {
        return config.getSecurity().getMaxRetries();
    }

    @Override
    protected Duration retryDelay() {
        return config.getSecurity().getRetryDelay();
    }

    /**
     * Retrieves and caches the access token for context enrichment API operations.
     */
    public String getAccessToken() {
        return tokenManager.getAccessToken(
                config.getContextEnrichment(),
                "environment_authorization",
                "context-enrichment"
        );
    }

    /**
     * Retrieves the list of available content enrichment actions.
     */
    public List<String> getAvailableActions(String token) {
        String url = buildUrl(config.getContextEnrichment().getApiUrl(), "/content/process/actions");
        return executeWithRetry(() -> {
            ResponseEntity<List<String>> response = restTemplate.exchange(
                    url, HttpMethod.GET, new HttpEntity<>(createBearerHeaders(token)),
                    new ParameterizedTypeReference<>() {});
            return Optional.ofNullable(response.getBody()).orElse(List.of());
        }, "get available actions");
    }

    /**
     * Requests a presigned URL to upload a file with the given content type.
     */
    public Map<String, Object> getPresignedUrl(String token, String contentType) {
        URI uri = UriComponentsBuilder.fromUriString(buildUrl(config.getContextEnrichment().getApiUrl(), "/files/upload/presigned-url"))
                .queryParam("contentType", contentType).build().toUri();
        return executeWithRetry(() -> exchangeMap(uri.toString(), HttpMethod.GET, createBearerHeaders(token)), "get presigned URL");
    }

    /**
     * Uploads file content directly to a presigned URL.
     */
    public void uploadFileFromMemory(String presignedUrl, byte[] data, String contentType) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.parseMediaType(contentType));
            HttpEntity<byte[]> entity = new HttpEntity<>(data, headers);

            restTemplate.exchange(URI.create(presignedUrl), HttpMethod.PUT, entity, Void.class);
            log.debug("Uploaded {} bytes to presigned URL", data.length);
        } catch (HttpClientErrorException e) {
            log.error("Upload failed ({}): {}", e.getStatusCode(), e.getResponseBodyAsString());
            throw new ContextEnrichmentException("Upload failed", e);
        } catch (RestClientException e) {
            log.error("Upload failed: {}", e.getMessage(), e);
            throw new ContextEnrichmentException("Upload failed", e);
        }
    }

    /**
     * Initiates content processing on an uploaded file.
     */
    public String processContent(String token, String objectKey, List<String> actions,
                                 Integer deleteAfterSeconds, Map<String, String> metadata) {
        String url = buildUrl(config.getContextEnrichment().getApiUrl(), "/content/process");

        Map<String, Object> body = new HashMap<>();
        body.put("objectKeys", List.of(objectKey));
        if (actions != null && !actions.isEmpty()) body.put("actions", actions);
        if (metadata != null && !metadata.isEmpty()) body.put("kSimilarMetadata", List.of(metadata));
        if (deleteAfterSeconds != null) body.put("deleteAfterSeconds", deleteAfterSeconds);

        HttpHeaders headers = createBearerHeaders(token);
        headers.setContentType(MediaType.APPLICATION_JSON);

        return executeWithRetry(() -> {
            ResponseEntity<String> response = restTemplate.exchange(
                    url, HttpMethod.POST, new HttpEntity<>(body, headers), String.class);
            try {
                return extractJobId(response.getBody());
            } catch (JsonProcessingException e) {
                throw new RuntimeException(e);
            }
        }, "process content");
    }

    /**
     * Retrieves the results of a specific enrichment job.
     */
    public Map<String, Object> getResults(String token, String jobId) {
        String url = buildUrl(config.getContextEnrichment().getApiUrl(), "/content/process/" + jobId + "/results");
        return executeWithRetry(() -> exchangeMap(url, HttpMethod.GET, createBearerHeaders(token)), "get results");
    }

    // --- Internal helpers ---

    private Map<String, Object> exchangeMap(String url, HttpMethod method, HttpHeaders headers) {
        ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                url, method, new HttpEntity<>(headers),
                new ParameterizedTypeReference<>() {});
        return Optional.ofNullable(response.getBody()).orElseThrow();
    }

    private String extractJobId(String body) throws JsonProcessingException {
        if (body == null || body.isBlank()) {
            throw new ContextEnrichmentException("Empty job response");
        }

        // Handle plain ID string
        if (!body.trim().startsWith("{")) {
            return body.replace("\"", "").replace("'", "");
        }

        // Parse JSON and extract first known job ID field
        JsonNode json = objectMapper.readTree(body);
        return JOB_ID_FIELDS.stream()
                .filter(json::hasNonNull)
                .map(k -> json.get(k).asText())
                .findFirst()
                .orElseThrow(() -> new ContextEnrichmentException("No job ID found in response: " + body));
    }

    /**
     * Runtime exception for context enrichment failures.
     */
    public static class ContextEnrichmentException extends RuntimeException {
        public ContextEnrichmentException(String message) {
            super(message);
        }

        public ContextEnrichmentException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}