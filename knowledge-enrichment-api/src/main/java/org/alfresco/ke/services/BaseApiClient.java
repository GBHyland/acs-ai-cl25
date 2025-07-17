package org.alfresco.ke.services;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.alfresco.ke.common.OAuthTokenManager;
import org.alfresco.ke.config.AppProperties;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.*;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Base class for API clients providing common HTTP operations and token management.
 */
@Slf4j
@RequiredArgsConstructor
public abstract class BaseApiClient {

    protected final RestTemplate restTemplate;
    protected final AppProperties config;
    protected final ObjectMapper objectMapper;
    protected final OAuthTokenManager tokenManager;

    /**
     * Get the service-specific configuration.
     */
    protected abstract AppProperties.ServiceConfig getServiceConfig();

    /**
     * Get the service name for token management.
     */
    protected abstract String getServiceName();

    /**
     * Acquire a short-lived OAuth2 bearer token for the service.
     */
    public String getAccessToken() {
        return tokenManager.getAccessToken(
                getServiceConfig(),
                "environment_authorization",
                getServiceName()
        );
    }

    /**
     * Build an absolute API URL by appending a path to the base URL from configuration.
     */
    protected String buildUrl(String path) {
        return UriComponentsBuilder
                .fromUriString(getServiceConfig().getApiUrl())
                .path(path)
                .toUriString();
    }

    /**
     * Create JSON headers with bearer token authentication.
     */
    protected HttpHeaders bearerHeaders() {
        HttpHeaders headers = jsonHeaders();
        headers.setBearerAuth(getAccessToken());
        return headers;
    }

    /**
     * Return headers that accept JSON responses only.
     */
    protected HttpHeaders jsonHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.setAccept(List.of(MediaType.APPLICATION_JSON));
        return headers;
    }

    /**
     * Perform a GET request expecting a JSON response and return it as a Map.
     */
    protected Map<String, Object> getJson(String url) {
        return getJson(url, true);
    }

    /**
     * Perform a GET request expecting a JSON response.
     *
     * @param url the URL to request
     * @param requiresAuth whether the request needs bearer token authentication
     */
    protected Map<String, Object> getJson(String url, boolean requiresAuth) {
        HttpHeaders headers = requiresAuth ? bearerHeaders() : jsonHeaders();
        ParameterizedTypeReference<Map<String, Object>> type = new ParameterizedTypeReference<>() {};

        return restTemplate.exchange(
                        url,
                        HttpMethod.GET,
                        new HttpEntity<>(headers),
                        type)
                .getBody();
    }

    /**
     * Perform a GET request expecting a typed response.
     */
    protected <T> T get(String url, ParameterizedTypeReference<T> responseType) {
        return restTemplate.exchange(
                        url,
                        HttpMethod.GET,
                        new HttpEntity<>(bearerHeaders()),
                        responseType)
                .getBody();
    }

    /**
     * Perform a POST request with JSON body.
     */
    protected <T> T post(String url, Object body, ParameterizedTypeReference<T> responseType) {
        HttpHeaders headers = bearerHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        return restTemplate.exchange(
                        url,
                        HttpMethod.POST,
                        new HttpEntity<>(body, headers),
                        responseType)
                .getBody();
    }

    /**
     * Upload binary data to a presigned URL.
     */
    protected void uploadToPresignedUrl(String presignedUrl, byte[] data, String contentType) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.parseMediaType(contentType));
        headers.setContentLength(data.length);

        URI uri = UriComponentsBuilder.fromUriString(presignedUrl).build(true).toUri();

        restTemplate.exchange(
                uri,
                HttpMethod.PUT,
                new HttpEntity<>(data, headers),
                Void.class);
    }

    /**
     * Extract content-type from a presigned URL query parameter.
     */
    protected Optional<String> extractContentType(String url) {
        return Arrays.stream(url.split("[?&]"))
                .filter(p -> p.startsWith("content-type="))
                .findFirst()
                .map(p -> URLDecoder.decode(p.substring(13), StandardCharsets.UTF_8));
    }

    /**
     * Parse JSON string into a Map, returning null if parsing fails.
     */
    protected Map<String, Object> parseJsonSafely(String json) {
        if (json == null || json.isBlank()) return null;

        try {
            return objectMapper.readValue(json, new TypeReference<>() {});
        } catch (Exception e) {
            log.debug("Failed to parse JSON: {}", e.getMessage());
            return null;
        }
    }
}