package org.alfresco.ke.services;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.alfresco.ke.common.OAuthTokenManager;
import org.alfresco.ke.config.AppProperties;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Client for the Context-Enrichment API.
 * Provides NLP processing capabilities including entity extraction and summarization.
 */
@Slf4j
@Component
public class ContextEnrichmentClient extends BaseApiClient {

    /** JSON field names that may carry a job/processing identifier. */
    private static final List<String> JOB_ID_FIELDS = List.of("processingId", "jobId", "id");

    public ContextEnrichmentClient(RestTemplate restTemplate,
                                   AppProperties config,
                                   ObjectMapper objectMapper,
                                   OAuthTokenManager tokenManager) {
        super(restTemplate, config, objectMapper, tokenManager);
    }

    @Override
    protected AppProperties.ServiceConfig getServiceConfig() {
        return config.getContextEnrichment();
    }

    @Override
    protected String getServiceName() {
        return "context-enrichment";
    }

    /**
     * Enumerate the NLP actions supported by the backend.
     */
    public List<String> getAvailableActions() {
        String url = buildUrl("/content/process/actions");
        ParameterizedTypeReference<List<String>> responseType = new ParameterizedTypeReference<>() {};

        List<String> result = get(url, responseType);
        return result != null ? result : List.of();
    }

    /**
     * Request a presigned S3 URL for file upload.
     */
    public Map<String, Object> getPresignedUrl(String contentType) {
        URI uri = UriComponentsBuilder
                .fromUriString(buildUrl("/files/upload/presigned-url"))
                .queryParam("contentType", contentType)
                .build().toUri();

        ParameterizedTypeReference<Map<String, Object>> responseType = new ParameterizedTypeReference<>() {};
        return get(uri.toString(), responseType);
    }

    /**
     * Upload file content to S3 via presigned URL.
     */
    public void uploadFileFromMemory(String presignedUrl, byte[] data, String contentType) {
        uploadToPresignedUrl(presignedUrl, data, contentType);
    }

    /**
     * Request processing of an uploaded file with enrichment actions.
     */
    public String processContent(String objectKey, List<String> actions) {
        String url = buildUrl("/content/process");

        Map<String, Object> body = new HashMap<>();
        body.put("objectKeys", List.of(objectKey));
        if (actions != null && !actions.isEmpty()) {
            body.put("actions", actions);
        }

        String response = post(url, body, new ParameterizedTypeReference<String>() {});
        return extractJobId(response);
    }

    /**
     * Retrieve enrichment results for a completed job.
     */
    public Map<String, Object> getResults(String jobId) {
        String url = buildUrl("/content/process/" + jobId + "/results");
        return getJson(url);
    }

    /**
     * Extract job identifier from response body.
     */
    private String extractJobId(String body) {
        if (!body.trim().startsWith("{")) {
            return body.replace("\"", "").replace("'", "");
        }

        try {
            JsonNode json = objectMapper.readTree(body);
            return JOB_ID_FIELDS.stream()
                    .filter(json::hasNonNull)
                    .map(k -> json.get(k).asText())
                    .findFirst()
                    .orElseThrow(() -> new RuntimeException("No job ID found in response: " + body));
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Invalid JSON: " + body, e);
        }
    }
}