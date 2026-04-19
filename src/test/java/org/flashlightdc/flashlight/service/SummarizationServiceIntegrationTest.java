package org.flashlightdc.flashlight.service;

import io.github.cdimascio.dotenv.Dotenv;
import org.flashlightdc.flashlight.dto.SummaryResponse;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.ClassPathResource;
import org.springframework.test.context.ActiveProfiles;
import reactor.test.StepVerifier;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@SpringBootTest
@ActiveProfiles("test")
public class SummarizationServiceIntegrationTest {

    @Autowired
    private SummarizationService summarizationService;

    @BeforeAll
    static void setup() {
        // Load .env variables into System properties for local testing
        Dotenv dotenv = Dotenv.configure()
                .ignoreIfMissing()
                .load();
        dotenv.entries().forEach(entry -> {
            if (System.getProperty(entry.getKey()) == null && System.getenv(entry.getKey()) == null) {
                System.setProperty(entry.getKey(), entry.getValue());
            }
        });
    }

    @Test
    void testSummarizeFromFile() throws IOException {
        // 1. Read the example bill text file
        Path inputPath = Paths.get("src/test/resources/samples/bill_sample.txt");
        String billText = Files.readString(inputPath);

        // 2. Call the AI service
        summarizationService.summarizeRawText(billText)
                .as(StepVerifier::create)
                .assertNext(response -> {
                    assert response != null;
                    assert "SUCCESS".equals(response.getStatus());
                    
                    // 3. Write output to a text file
                    try {
                        Path outputPath = Paths.get("src/test/resources/output/bill_summary_output.txt");
                        Files.createDirectories(outputPath.getParent());
                        Files.writeString(outputPath, response.getSummary());
                        System.out.println("Summary written to: " + outputPath.toAbsolutePath());
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                })
                .verifyComplete();
    }
}
