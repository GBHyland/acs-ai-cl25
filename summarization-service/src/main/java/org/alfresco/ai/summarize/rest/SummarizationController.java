package org.alfresco.ai.summarize.rest;

import lombok.RequiredArgsConstructor;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import org.alfresco.ai.summarize.service.PdfSummarizationService;

import java.io.IOException;

@RestController // Marks this class as a REST controller (JSON responses by default)
@RequestMapping("/api/summarize") // Base URL for all endpoints in this controller
@RequiredArgsConstructor // Lombok: generates a constructor for 'service' (final field)
public class SummarizationController {

    // Injected service that performs the actual summarization logic
    private final PdfSummarizationService service;

    /**
     * HTTP POST endpoint that receives a PDF file and returns its summary.
     *
     * @param file the uploaded PDF file (multipart/form-data)
     * @return a plain text summary of the PDF, generated via LLM
     * @throws IOException if reading the file fails
     */
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE) // Accepts file uploads
    public ResponseEntity<String> summarizePdf(@RequestParam("file") MultipartFile file) throws IOException {
        // Delegate to the service and return the result in a 200 OK response
        return ResponseEntity.ok(service.summarize(file));
    }
}
