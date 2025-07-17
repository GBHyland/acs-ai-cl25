package org.alfresco.ke.services;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.alfresco.ke.common.OAuthTokenManager;
import org.alfresco.ke.config.AppProperties;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.util.HashMap;
import java.util.Map;

/**
 * Client for the Data-Curation API.
 * Provides data processing and curation capabilities.
 */
@Component
@Slf4j
public class DataCurationClient extends BaseApiClient {

    public DataCurationClient(RestTemplate restTemplate,
                              AppProperties config,
                              ObjectMapper objectMapper,
                              OAuthTokenManager tokenManager) {
        super(restTemplate, config, objectMapper, tokenManager);
    }

    @Override
    protected AppProperties.ServiceConfig getServiceConfig() {
        return config.getDataCuration();
    }

    @Override
    protected String getServiceName() {
        return "data-curation";
    }

    /**
     * Request a presigned S3 upload URL with metadata.
     */
    public Map<String, Object> presign(String fileName, Map<String, Object> options) {
        String url = buildUrl("/presign");

        Map<String, Object> body = new HashMap<>(options);
        body.put("fileName", fileName);

        ParameterizedTypeReference<Map<String, Object>> type = new ParameterizedTypeReference<>() {};
        return post(url, body, type);
    }

    /**
     * Upload binary data to presigned S3 URL.
     */
    public void putToS3(String presignedUrl, byte[] bytes, String fallbackContentType) {
        String contentType = extractContentType(presignedUrl).orElse(fallbackContentType);
        uploadToPresignedUrl(presignedUrl, bytes, contentType);
    }

    /**
     * Poll for job status.
     */
    public Map<String, Object> status(String jobId) {
        return getJson(buildUrl("/status/" + jobId));
    }

    /**
     * Retrieve job results.
     */
    public Map<String, Object> results(String jobId) {
        return getJson(buildUrl("/results/" + jobId));
    }

    /**
     * Download and parse results from presigned S3 URL.
     */
    public Map<String, Object> getPresignedResults(String presignedUrl) {
        URI uri = UriComponentsBuilder.fromUriString(presignedUrl).build(true).toUri();

        String json = restTemplate.exchange(
                        uri,
                        HttpMethod.GET,
                        new HttpEntity<>(jsonHeaders()),
                        String.class)
                .getBody();

        return parseJsonSafely(json);
    }

}