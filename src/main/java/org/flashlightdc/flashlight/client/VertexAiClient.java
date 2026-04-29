package org.flashlightdc.flashlight.client;

import com.google.cloud.vertexai.generativeai.GenerativeModel;
import com.google.cloud.vertexai.generativeai.ResponseHandler;
import com.google.cloud.vertexai.api.GenerateContentResponse;
import jakarta.annotation.PostConstruct;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

@Component
public class VertexAiClient {

    private final GenerativeModel generativeModel;
    private String promptTemplate;

    public VertexAiClient(GenerativeModel generativeModel) {
        this.generativeModel = generativeModel;
    }

    @PostConstruct
    void loadPromptTemplate() throws IOException {
        ClassPathResource resource = new ClassPathResource("prompts/BillSummarization.txt");
        promptTemplate = new String(resource.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
    }

    public String getPromptTemplate() {
        return promptTemplate;
    }

    public Mono<String> summarizeText(String fullPrompt) {
        return Mono.fromCallable(() -> {
            GenerateContentResponse response = generativeModel.generateContent(fullPrompt);
            return ResponseHandler.getText(response);
        }).subscribeOn(Schedulers.boundedElastic());
    }
}
