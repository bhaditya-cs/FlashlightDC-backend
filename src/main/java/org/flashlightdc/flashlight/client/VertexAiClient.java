package org.flashlightdc.flashlight.client;

import com.google.cloud.vertexai.generativeai.GenerativeModel;
import com.google.cloud.vertexai.generativeai.ResponseHandler;
import com.google.cloud.vertexai.api.GenerateContentResponse;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

@Component
public class VertexAiClient {

    private final GenerativeModel generativeModel;

    public VertexAiClient(GenerativeModel generativeModel) {
        this.generativeModel = generativeModel;
    }

    public Mono<String> summarizeText(String text) {
        return Mono.fromCallable(() -> {
            String prompt = "Summarize the following congressional bill text in a concise, " +
                    "easy-to-understand way for a general audience. Focus on the main impact and " +
                    "any major changes it proposes. Please format the summary using Markdown with bullet points, headings, and bold text where appropriate:\n\n" + text;

            GenerateContentResponse response = generativeModel.generateContent(prompt);
            return ResponseHandler.getText(response);
        }).subscribeOn(Schedulers.boundedElastic());
    }
}
