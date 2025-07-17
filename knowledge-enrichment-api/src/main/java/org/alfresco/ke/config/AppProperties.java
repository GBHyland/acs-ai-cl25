package org.alfresco.ke.config;

import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

/**
 * Main configuration properties bound from application.yml or application.properties using prefix 'app'.
 */
@Configuration
@ConfigurationProperties(prefix = "app")
@Data
public class AppProperties {

    /**
     * Configuration for the Data Curation API.
     */
    private final ApiProperties dataCuration = new ApiProperties();

    /**
     * Configuration for the Context Enrichment API.
     */
    private final ApiProperties contextEnrichment = new ApiProperties();

    /**
     * Common settings for all RestTemplate clients.
     */
    private final RestTemplateProperties restTemplate = new RestTemplateProperties();

    /**
     * Security-related settings such as retry and caching.
     */
    private final SecurityProperties security = new SecurityProperties();

    /**
     * Generic API configuration: credentials and endpoints.
     */
    @Data
    public static class ApiProperties {
        private String clientId;
        private String clientSecret;
        private String apiUrl;
        private String oauthUrl;
    }

    /**
     * RestTemplate tuning options like timeouts and connection pool sizes.
     */
    @Data
    @NoArgsConstructor
    public static class RestTemplateProperties {
        private Duration connectTimeout = Duration.ofSeconds(5);
        private Duration readTimeout = Duration.ofSeconds(30);
        private int maxConnections = 100;
        private int maxConnectionsPerRoute = 20;
    }

    /**
     * Security tuning for retry and token caching behavior.
     */
    @Data
    @NoArgsConstructor
    public static class SecurityProperties {
        private Duration tokenCacheDuration = Duration.ofMinutes(5);
        private Duration retryDelay = Duration.ofSeconds(5);
        private int maxRetries = 3;
    }
}