package org.alfresco.ke.common;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.alfresco.ke.config.AppProperties;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;

/**
 * Shared utility to acquire and cache OAuth2 tokens via client credentials.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OAuthTokenManager {

    private final RestTemplate restTemplate;
    private final AppProperties appProperties;

    private static final String GRANT_TYPE = "client_credentials";
    private static final String ACCESS_TOKEN = "access_token";

    // Cache keyed by context (e.g., service or scope)
    private final Map<String, CachedToken> tokenCache = new java.util.concurrent.ConcurrentHashMap<>();

    /**
     * Returns a valid access token from cache or fetches a new one.
     */
    public String getAccessToken(AppProperties.ApiProperties props, String scope, String cacheKey) {
        var cached = tokenCache.get(cacheKey);
        if (cached != null && cached.isValid()) {
            return cached.token();
        }

        // Synchronize to avoid race conditions when multiple threads request the same token
        synchronized (tokenCache) {
            cached = tokenCache.get(cacheKey);
            if (cached != null && cached.isValid()) {
                return cached.token();
            }

            log.debug("Requesting new token for {}", cacheKey);
            String token = requestToken(props, scope);
            cacheToken(cacheKey, token);
            return token;
        }
    }

    /**
     * Removes the token from cache (e.g., after a 401).
     */
    public void invalidate(String cacheKey) {
        tokenCache.remove(cacheKey);
    }

    /**
     * Performs the actual client credentials token request.
     */
    private String requestToken(AppProperties.ApiProperties props, String scope) {
        String tokenUrl = buildUrl(props.getOauthUrl(), "");

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        headers.setAccept(List.of(MediaType.APPLICATION_JSON));

        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("grant_type", GRANT_TYPE);
        form.add("client_id", props.getClientId());
        form.add("client_secret", props.getClientSecret());
        form.add("scope", scope);

        HttpEntity<?> request = new HttpEntity<>(form, headers);

        // Execute request with retry support
        ResponseEntity<Map<String, Object>> response = withRetry(() ->
                restTemplate.exchange(tokenUrl, HttpMethod.POST, request,
                        new ParameterizedTypeReference<>() {}), "token request");

        // Extract token from response body
        return Optional.ofNullable(response.getBody())
                .map(body -> body.get(ACCESS_TOKEN))
                .map(Object::toString)
                .filter(t -> !t.isBlank())
                .orElseThrow(() -> new RuntimeException("Missing access token"));
    }

    /**
     * Caches token with expiration timestamp.
     */
    private void cacheToken(String key, String token) {
        Duration duration = appProperties.getSecurity().getTokenCacheDuration();
        long expiry = System.currentTimeMillis() + duration.toMillis();
        tokenCache.put(key, new CachedToken(token, expiry));
        log.debug("Cached token for key '{}' valid for {} seconds", key, duration.toSeconds());
    }

    /**
     * Safely constructs a URL by removing trailing slashes before appending path.
     */
    private String buildUrl(String base, String path) {
        return base.replaceAll("/+$", "") + path;
    }

    /**
     * Simple retry wrapper for token requests.
     */
    private <T> T withRetry(Supplier<T> supplier, String label) {
        var sec = appProperties.getSecurity();
        int max = sec.getMaxRetries();
        long delay = sec.getRetryDelay().toMillis();

        for (int i = 1; i <= max; i++) {
            try {
                return supplier.get();
            } catch (RestClientException e) {
                log.warn("Attempt {}/{} failed for {}: {}", i, max, label, e.getMessage());
                if (i == max) throw e;
                try {
                    Thread.sleep(delay);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    throw new RuntimeException("Interrupted during retry", ie);
                }
            }
        }
        throw new IllegalStateException("Should not reach here");
    }

    /**
     * Simple token holder with expiry logic.
     */
    private record CachedToken(String token, long expiryMillis) {
        boolean isValid() {
            return System.currentTimeMillis() < expiryMillis;
        }
    }
}