package org.flashlightdc.flashlight.service;

import io.github.cdimascio.dotenv.Dotenv;
import org.flashlightdc.flashlight.client.VertexAiClient;
import org.flashlightdc.flashlight.dto.SummaryResponse;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
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
    private VertexAiClient vertexAiClient;

    @BeforeAll
    static void setup() {
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
        Path inputPath = Paths.get("src/test/resources/samples/bill_sample.txt");
        String billText = Files.readString(inputPath);

        String fullPrompt = String.format(
                vertexAiClient.getPromptTemplate(),
                billText,
                "Not available"
        );

        vertexAiClient.summarizeText(fullPrompt)
                .as(StepVerifier::create)
                .assertNext(rawJson -> {
                    assert rawJson != null;
                    assert rawJson.contains("bill_number");
                    assert rawJson.contains("sections");
                    assert rawJson.contains("tldr");
                    assert rawJson.contains("hook");

                    try {
                        Path outputPath = Paths.get("src/test/resources/output/bill_summary_output.json");
                        Files.createDirectories(outputPath.getParent());
                        Files.writeString(outputPath, rawJson);
                        System.out.println("Summary written to: " + outputPath.toAbsolutePath());
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                })
                .verifyComplete();
    }
}
