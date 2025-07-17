package org.alfresco.ke.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.hc.client5.http.config.RequestConfig;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManager;
import org.apache.hc.core5.util.Timeout;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.web.client.DefaultResponseErrorHandler;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;

/**
 * Spring Boot configuration for creating a custom {@link RestTemplate} bean
 * with connection pooling, timeout settings, and enhanced error handling.
 */
@Slf4j
@Configuration
@RequiredArgsConstructor
public class RestTemplateConfig {

    private final AppProperties appProperties;

    /**
     * Creates a {@link RestTemplate} with:
     * <ul>
     *   <li>Custom connection pool</li>
     *   <li>Connection and read timeouts from configuration</li>
     *   <li>Enhanced error logging</li>
     * </ul>
     *
     * @param builder Spring's {@link RestTemplateBuilder}
     * @return configured {@link RestTemplate}
     */
    @Bean
    public RestTemplate restTemplate(RestTemplateBuilder builder) {
        var props = appProperties.getRestTemplate();

        var connectionManager = new PoolingHttpClientConnectionManager();
        connectionManager.setMaxTotal(props.getMaxConnections());
        connectionManager.setDefaultMaxPerRoute(props.getMaxConnectionsPerRoute());

        var requestConfig = RequestConfig.custom()
                .setConnectionRequestTimeout(Timeout.of(props.getConnectTimeout()))
                .setResponseTimeout(Timeout.of(props.getReadTimeout()))
                .build();

        var httpClient = HttpClients.custom()
                .setConnectionManager(connectionManager)
                .setDefaultRequestConfig(requestConfig)
                .build();

        var requestFactory = new HttpComponentsClientHttpRequestFactory(httpClient);

        return builder
                .requestFactory(() -> requestFactory)
                .errorHandler(new EnhancedResponseErrorHandler())
                .build();
    }

    /**
     * Custom error handler that logs error responses before delegating to the default handler.
     */
    private static class EnhancedResponseErrorHandler extends DefaultResponseErrorHandler {
        @Override
        public void handleError(ClientHttpResponse response) throws IOException {
            HttpStatusCode status = response.getStatusCode();
            log.error("HTTP error response: {} {}", status.value(), response.getStatusText());
            super.handleError(response);
        }
    }
}