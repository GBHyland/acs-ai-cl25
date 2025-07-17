package org.alfresco.ke.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

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
    private final ServiceConfig dataCuration = new ServiceConfig();

    /**
     * Configuration for the Context Enrichment API.
     */
    private final ServiceConfig contextEnrichment = new ServiceConfig();

    /**
     * Generic service configuration: credentials and endpoints.
     */
    @Data
    public static class ServiceConfig {
        private String clientId;
        private String clientSecret;
        private String apiUrl;
        private String oauthUrl;
    }

}