package org.alfresco.ke.contextenrichment;

import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.alfresco.ke.util.ContentTypeUtils;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

/**
 * REST controller to handle file uploads and content processing
 * via the Context Enrichment API.
 */
@RestController
@RequestMapping("/context")
@RequiredArgsConstructor
@Validated
public class ContextEnrichmentController {

    private final ContextEnrichmentClient client;

    /**
     * Returns the list of available processing actions from the API.
     */
    @GetMapping("/available_actions")
    public ResponseEntity<?> availableActions() {
        try {
            String token = client.getAccessToken();
            return ResponseEntity.ok(client.getAvailableActions(token));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(e.getMessage());
        }
    }

    /**
     * Uploads a file, requests a presigned URL, uploads the content,
     * and starts content enrichment with the specified actions.
     *
     * @param file    the uploaded file
     * @param actions list of actions to apply
     * @return a JSON response containing the job ID
     */
    @PostMapping("/upload")
    public ResponseEntity<?> upload(
            @RequestParam("file") MultipartFile file,
            @RequestParam("actions") List<String> actions) {

        try {
            String contentType = ContentTypeUtils.determineContentType(file);
            byte[] content = file.getBytes();

            String token = client.getAccessToken();
            Map<String, Object> presigned = client.getPresignedUrl(token, contentType);

            String url = (String) presigned.get("presignedUrl");
            String key = (String) presigned.get("objectKey");

            client.uploadFileFromMemory(url, content, contentType);
            String jobId = client.processContent(token, key, actions, null, null);

            return ResponseEntity.ok(Map.of("jobId", jobId));

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(e.getMessage());
        }
    }

    /**
     * Retrieves the results for a given job ID.
     *
     * @param jobId the job identifier
     * @return Json information from the Context Enrichment API
     */
    @GetMapping("/results/{jobId}")
    public ResponseEntity<?> results(@PathVariable String jobId) {
        try {
            String token = client.getAccessToken();
            Map<String, Object> status = client.getResults(token, jobId);
            return ResponseEntity.ok(status);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(e.getMessage());
        }
    }
}
