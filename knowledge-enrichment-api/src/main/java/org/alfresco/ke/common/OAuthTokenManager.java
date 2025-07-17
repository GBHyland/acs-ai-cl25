package org.alfresco.ke.common;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.alfresco.ke.config.AppProperties;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages OAuth2 tokens via client credentials flow with simple caching.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OAuthTokenManager {

    private final RestTemplate restTemplate;
    private final Map<String, CachedToken> tokenCache = new ConcurrentHashMap<>();

    /**
     * Gets a valid access token from cache or fetches a new one.
     */
    public String getAccessToken(AppProperties.ServiceConfig props, String scope, String cacheKey) {
        CachedToken cached = tokenCache.get(cacheKey);
        if (cached != null && cached.isValid()) {
            return cached.token();
        }
        return requestToken(props, scope);
    }

    /**
     * Requests a new OAuth2 access token using client credentials flow.
     *
     * @param props API properties containing OAuth URL, client ID and secret
     * @param scope OAuth scope to request for the token
     * @return the access token string
     */
    private String requestToken(AppProperties.ServiceConfig props, String scope) {
        String tokenUrl = props.getOauthUrl() + "/connect/token";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        headers.setAccept(List.of(MediaType.APPLICATION_JSON));

        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("grant_type", "client_credentials");
        form.add("client_id", props.getClientId());
        form.add("client_secret", props.getClientSecret());
        form.add("scope", scope);

        HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(form, headers);

        ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                tokenUrl, HttpMethod.POST, request, new ParameterizedTypeReference<>() {}
        );

        String token = (String) response.getBody().get("access_token");
        Number expiresIn = (Number) response.getBody().getOrDefault("expires_in", 900);
        cacheToken(props.getClientId(), token, expiresIn.longValue());

        return token;
    }

    /**
     * Stores the token in cache with calculated expiration time.
     *
     * @param key cache key to store the token under
     * @param token the access token to cache
     * @param expiresInSeconds number of seconds for expiration
     */
    private void cacheToken(String key, String token, long expiresInSeconds) {
        long expiryMillis = System.currentTimeMillis() + (expiresInSeconds * 1000);
        tokenCache.put(key, new CachedToken(token, expiryMillis));
    }
    /**
     * Holds a cached token with its expiration timestamp.
     *
     * @param token the OAuth2 access token
     * @param expiryMillis timestamp in milliseconds when token expires
     */
    private record CachedToken(String token, long expiryMillis) {
        boolean isValid() {
            return System.currentTimeMillis() < expiryMillis;
        }
    }
}