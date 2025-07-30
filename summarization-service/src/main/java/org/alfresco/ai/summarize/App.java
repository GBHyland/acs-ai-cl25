package org.alfresco.ai.summarize;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication // Marks this class as the main configuration and bootstrap class
public class App {

    /**
     * Main method — entry point for the Spring Boot application.
     *
     * @param args command-line arguments passed to the app (if any)
     */
    public static void main(String[] args) {
        // Bootstraps the Spring Boot application, starting the embedded server and scanning components
        SpringApplication.run(App.class, args);
    }
}
