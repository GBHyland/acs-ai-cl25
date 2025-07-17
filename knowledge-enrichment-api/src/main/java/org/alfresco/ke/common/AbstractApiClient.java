package org.alfresco.ke.common;

import lombok.extern.slf4j.Slf4j;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.*;
import org.springframework.web.client.RestClientException;

import java.time.Duration;
import java.util.List;
import java.util.function.Supplier;

@Slf4j
public abstract class AbstractApiClient {

    // Should return max retry attempts, defined by subclass
    protected abstract int maxRetries();

    // Should return delay between retries, defined by subclass
    protected abstract Duration retryDelay();

    // Create HTTP headers that accept JSON responses
    protected HttpHeaders createJsonHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.setAccept(List.of(MediaType.APPLICATION_JSON));
        return headers;
    }

    // Create headers with Bearer token authentication
    protected HttpHeaders createBearerHeaders(String token) {
        HttpHeaders headers = createJsonHeaders();
        headers.setBearerAuth(token);
        return headers;
    }

    // Ensure base URL doesn't end with slash, then append path
    protected String buildUrl(String base, String path) {
        return base.replaceAll("/+$", "") + path;
    }

    // Throw exception if response is not successful (non-2xx)
    protected void validateResponse(ResponseEntity<?> r, String opName) {
        if (!r.getStatusCode().is2xxSuccessful()) {
            throw new RuntimeException(opName + " failed: " + r.getStatusCode());
        }
    }

    // Retry wrapper for executing an API operation with backoff
    protected <T> T executeWithRetry(Supplier<T> operation, String opName) {
        Exception last = null;
        for (int attempt = 1; attempt <= maxRetries(); attempt++) {
            try {
                return operation.get();
            } catch (RestClientException e) {
                last = e;
                log.warn("Attempt {}/{} failed for {}: {}", attempt, maxRetries(), opName, e.getMessage());
                if (attempt < maxRetries()) {
                    try {
                        Thread.sleep(retryDelay().toMillis());
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        throw new RuntimeException("Retry interrupted", ie);
                    }
                }
            }
        }
        throw new RuntimeException(opName + " failed after " + maxRetries() + " attempts", last);
    }
}