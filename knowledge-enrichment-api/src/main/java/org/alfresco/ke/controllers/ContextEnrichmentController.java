package org.alfresco.ke.controllers;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.alfresco.ke.services.ContextEnrichmentClient;
import org.alfresco.ke.util.ContentTypeUtils;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.Duration;
import java.util.List;
import java.util.Map;

/**
 * Single-endpoint “upload‑and‑wait” context-enrichment controller.
 * <p>
 * Exposes {@code GET /context/available_actions} and {@code POST /context/process}
 */
@RestController
@RequestMapping("/context")
@RequiredArgsConstructor
@Slf4j
public class ContextEnrichmentController {

    /** Maximum polling attempts before giving up. */
    private static final int MAX_ATTEMPTS = 30;
    /** Wait time between polling attempts (2 s). */
    private static final Duration POLL_DELAY = Duration.ofSeconds(2);

    private final ContextEnrichmentClient client;

    /**
     * Lists the processing actions currently exposed by the Context‑Enrichment service.
     *
     * @return array of action names (e.g. {@code TRANSLATE}, {@code SUMMARISE}, ...)
     */
    @GetMapping("/available_actions")
    public ResponseEntity<List<String>> availableActions() {
        return ResponseEntity.ok(client.getAvailableActions());
    }

    /**
     * Streams a file to S3, triggers the Context‑Enrichment job with the requested actions, blocks until the job
     * finishes, and returns the final JSON result.
     *
     * @param file multipart upload from the HTTP request
     * @param actions actions to apply (must be recognised by the service)
     * @return final result map once the job completes successfully
     * @throws IOException if the upload cannot read or transmit bytes
     */
    @PostMapping("/process")
    public ResponseEntity<Map<String, Object>> processFile(
            @RequestParam("file") MultipartFile file,
            @RequestParam("actions") List<String> actions) throws IOException {

        String contentType = ContentTypeUtils.determineContentType(file);

        Map<String, Object> presigned = client.getPresignedUrl(contentType);
        client.uploadFileFromMemory(presigned.get("presignedUrl").toString(), file.getBytes(), contentType);

        String jobId = client.processContent(presigned.get("objectKey").toString(), actions);

        Map<String, Object> result = waitForResult(jobId);
        return ResponseEntity.ok(result);
    }

    /**
     * Polls {@link ContextEnrichmentClient#getResults(String)} until the job is finished or times out.
     * Returns the final JSON payload on success.
     *
     * @param jobId job identifier obtained from {@code processContent}
     * @throws IllegalStateException if the job fails or the timeout is exceeded
     */
    private Map<String, Object> waitForResult(String jobId) {
        for (int attempt = 1; attempt <= MAX_ATTEMPTS; attempt++) {
            sleepSilently();

            try {
                Map<String, Object> resp = client.getResults(jobId);

                boolean inProgress = (boolean) resp.getOrDefault("inProgress", true);
                if (inProgress) continue;

                String status = String.valueOf(resp.getOrDefault("status", "UNKNOWN")).toUpperCase();
                switch (status) {
                    case "SUCCESS" -> { return resp; }
                    case "FAILED", "ERROR" -> throw new IllegalStateException("Job failed: " + status);
                    default -> throw new IllegalStateException("Unexpected job status: " + status);
                }
            } catch (Exception e) {
                // ignore transient errors unless we are on the last attempt
                if (attempt == MAX_ATTEMPTS) throw new IllegalStateException("Unable to obtain results", e);
                log.debug("Polling attempt {} failed: {}", attempt, e.getMessage());
            }
        }
        throw new IllegalStateException("Timed out after %d attempts".formatted(MAX_ATTEMPTS));
    }

    /**
     * Sleeps for {@link #POLL_DELAY} and propagates interruption as an unchecked exception.
     */
    private void sleepSilently() {
        try {
            Thread.sleep(POLL_DELAY.toMillis());
        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Polling interrupted", ie);
        }
    }
}
