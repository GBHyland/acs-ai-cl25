package org.alfresco.ke.controllers;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.alfresco.ke.services.DataCurationClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.Duration;
import java.util.Map;

/**
 * Single-endpoint “upload‑and‑wait” data‑curation controller.
 * <p>
 * Exposes {@code POST /data-curation/process} that:
 * <ol>
 *   <li>Requests a presigned upload & result URL from the data‑curation service</li>
 *   <li>Streams the file to S3</li>
 *   <li>Polls the job status until completion (or timeout/failure)</li>
 *   <li>Returns the final JSON result to the caller</li>
 * </ol>
 */
@RestController
@RequestMapping("/data-curation")
@RequiredArgsConstructor
@Slf4j
public class DataCurationController {

    /** Maximum number of polling attempts before timing out. */
    private static final int MAX_ATTEMPTS = 60;
    /** Delay between polling attempts (5 s). */
    private static final Duration POLL_DELAY = Duration.ofSeconds(5);

    private final DataCurationClient dc;

    /**
     * Uploads a file and blocks until the data‑curation pipeline finishes, then returns its result.
     *
     * @param file           multipart file uploaded by the client
     * @param normalization  whether to run textual normalisation
     * @param chunking       whether to chunk the document
     * @param embedding      whether to generate embeddings
     * @param jsonSchema     Possible values: [MDAST, FULL, PIPELINE]
     * @return final result map produced by the data‑curation service
     * @throws IOException if the upload fails to read the file or transmit bytes
     */
    @PostMapping("/process")
    public ResponseEntity<Map<String, Object>> process(
            @RequestParam("file") MultipartFile file,
            @RequestParam(defaultValue = "false") boolean normalization,
            @RequestParam(defaultValue = "false") boolean chunking,
            @RequestParam(defaultValue = "false") boolean embedding,
            @RequestParam(defaultValue = "MDAST") String jsonSchema) throws IOException {

        var presigned = dc.presign(
                file.getOriginalFilename(),
                Map.of("normalization", normalization,
                        "chunking", chunking,
                        "embedding", embedding,
                        "json_schema", jsonSchema));

        dc.putToS3(presigned.get("put_url").toString(), file.getBytes(), file.getContentType());

        var result = waitForResult(
                (String) presigned.get("job_id"),
                (String) presigned.get("get_url"));

        return ResponseEntity.ok(result);
    }

    /**
     * Polls the job status until it is {@code DONE}, {@code FAILED}, {@code ERROR}, or the timeout is reached.
     *
     * @param jobId  identifier returned by the presign call
     * @param getUrl presigned GET URL for retrieving the result object
     * @return result map once the job is complete and the payload is available
     * @throws IllegalStateException if the job fails or times out
     */
    private Map<String, Object> waitForResult(String jobId, String getUrl) {
        for (int attempt = 1; attempt <= MAX_ATTEMPTS; attempt++) {
            sleepSilently();

            String status = String.valueOf(dc.status(jobId).get("status")).toUpperCase();
            switch (status) {
                case "DONE"   -> { return fetchResults(jobId, getUrl); }
                case "FAILED", "ERROR" -> throw new IllegalStateException("Job failed: " + status);
                default -> { /* keep polling */ }
            }
        }
        throw new IllegalStateException("Timed out after %d attempts".formatted(MAX_ATTEMPTS));
    }

    /**
     * Attempts to fetch job results via the presigned {@code getUrl}; falls back to an authenticated call.
     *
     * @param jobId  job identifier
     * @param getUrl presigned URL shared by the service for anonymous download
     * @return non‑null, error‑free result map
     * @throws IllegalStateException if no usable result can be obtained
     */
    private Map<String, Object> fetchResults(String jobId, String getUrl) {
        var viaUrl = dc.getPresignedResults(getUrl);
        if (viaUrl != null && !viaUrl.containsKey("error")) return viaUrl;

        var viaApi = dc.results(jobId);
        if (viaApi != null && !viaApi.containsKey("error")) return viaApi;

        throw new IllegalStateException("Unable to fetch results for job %s".formatted(jobId));
    }

    /**
     * Sleeps for {@link #POLL_DELAY} while converting {@link InterruptedException} into an unchecked exception.
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