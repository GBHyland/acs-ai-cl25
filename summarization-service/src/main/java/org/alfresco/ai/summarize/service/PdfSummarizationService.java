package org.alfresco.ai.summarize.service;

import lombok.RequiredArgsConstructor;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.document.Document;
import org.springframework.ai.reader.pdf.PagePdfDocumentReader;
import org.springframework.core.io.InputStreamResource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

@Service // Makes this class injectable in other Spring components (e.g., controllers)
@RequiredArgsConstructor // Lombok: generates a constructor with all final fields (chatModel)
public class PdfSummarizationService {

    // LLM model interface injected by Spring (configured in application.yml)
    private final ChatModel chatModel;

    /**
     * Summarizes a PDF file by:
     * 1. Splitting it into pages
     * 2. Summarizing each page separately
     * 3. Producing a global summary from all page summaries
     *
     * @param pdfFile the uploaded PDF to summarize
     * @return a final summary of the entire document
     * @throws IOException if the file can't be read
     */
    public String summarize(MultipartFile pdfFile) throws IOException {
        // Initialize the chat client using the configured model
        ChatClient chatClient = ChatClient.builder(chatModel).build();

        // Extract pages from the PDF using Spring AI's PDF reader
        List<Document> pages = new PagePdfDocumentReader(
                new InputStreamResource(pdfFile.getInputStream()) // Wrap file input as a Spring resource
        ).get(); // Returns a list of Document objects (one per page)

        // Summarize each page using the LLM, producing one summary per page
        List<String> summaries = pages.stream()
                .map(Document::getText) // Get plain text of each page
                .map(content -> chatClient.prompt() // Create a prompt for each page
                        .user("Summarize this page: " + content) // Instruction to the model
                        .call() // Call the LLM
                        .content()) // Extract the result text
                .collect(Collectors.toList());

        // Ask the LLM to summarize the full document using the previous page summaries
        return chatClient.prompt()
                .user("Summarize the whole document: " + String.join("\n", summaries))
                .call()
                .content(); // Return the global summary
    }
}