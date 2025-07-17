package org.alfresco.ke.datacuration;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;
import java.util.Optional;

/**
 * REST controller to handle data curation upload, status, and result polling.
 */
@RestController
@RequestMapping("/data-curation")
@RequiredArgsConstructor
@Validated
@Slf4j
public class DataCurationController {

    private final DataCurationClient dc;
    private final DataCurationStore store; // in-memory jobId → getUrl

    /**
     * Uploads a file for data curation and initiates the processing pipeline.
     *
     * @param file          the uploaded file (max 5 GB)
     * @param normalization whether normalization is requested
     * @param chunking      whether chunking is requested
     * @param embedding     whether embedding is requested
     * @param jsonSchema    optional JSON schema for validation
     * @return job ID and retrieval URL
     */
    @PostMapping("/upload")
    public ResponseEntity<?> upload(@RequestParam("file") MultipartFile file,
                                    @RequestParam(defaultValue = "false") boolean normalization,
                                    @RequestParam(defaultValue = "false") boolean chunking,
                                    @RequestParam(defaultValue = "false") boolean embedding,
                                    @RequestParam(required = false) String jsonSchema) {
        try {
            if (file.isEmpty()) {
                return ResponseEntity.badRequest().body("No file uploaded");
            }
            if (file.getSize() > 5L * 1024 * 1024 * 1024) {
                return ResponseEntity.status(HttpStatus.PAYLOAD_TOO_LARGE)
                        .body("Max file size is 5 GB");
            }

            String token = dc.getAccessToken();
            Map<String, Object> pre = dc.presign(token, file.getOriginalFilename());

            String putUrl = Optional.ofNullable(pre.get("put_url")).orElse(pre.get("putUrl")).toString();
            String getUrl = Optional.ofNullable(pre.get("get_url")).orElse(pre.get("getUrl")).toString();
            String jobId  = Optional.ofNullable(pre.get("job_id")).orElse(pre.get("jobId")).toString();

            dc.putToS3(putUrl, file.getBytes(), file.getContentType());
            store.save(jobId, getUrl); // persist for polling

            return ResponseEntity.ok(Map.of(
                    "jobId", jobId,
                    "getUrl", getUrl,
                    "status", "UPLOADED"
            ));
        } catch (Exception e) {
            log.error("Upload failed", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(e.getMessage());
        }
    }

    /**
     * Returns the current status of a data curation job.
     *
     * @param jobId the job identifier
     * @return status as reported by the remote API
     */
    @GetMapping("/status/{jobId}")
    public ResponseEntity<?> status(@PathVariable String jobId) {
        return ResponseEntity.ok(dc.status(jobId));
    }

    /**
     * Polls for the final results of a data curation job.
     * Uses cached presigned URL first, then bearer-based fallback.
     *
     * @param jobId the job identifier
     * @return final JSON result or current status
     */
    @GetMapping("/poll_results/{jobId}")
    public ResponseEntity<?> pollResults(@PathVariable String jobId) {

        String getUrl = store.find(jobId);
        if (getUrl == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", "Unknown jobId; upload first"));
        }

        // Try presigned result URL (no auth)
        Map<String, Object> result = dc.getPresignedResults(getUrl);
        if (result != null && !result.containsKey("error")) {
            store.remove(jobId);
            return ResponseEntity.ok(result);
        }

        // Fallback to /results/{id} (requires token)
        result = dc.results(jobId);
        if (result != null && !result.containsKey("error")) {
            store.remove(jobId);
            return ResponseEntity.ok(result);
        }

        // If not ready, return current status
        Map<String, Object> status = dc.status(jobId);
        boolean done = "DONE".equalsIgnoreCase(String.valueOf(status.get("status")));

        return ResponseEntity.ok(Map.of(
                "jobId", jobId,
                "status", done ? "DONE" : "PENDING"
        ));
    }
}