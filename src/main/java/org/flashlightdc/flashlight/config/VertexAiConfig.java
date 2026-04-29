package org.flashlightdc.flashlight.config;

import com.google.cloud.vertexai.VertexAI;
import com.google.cloud.vertexai.api.GenerationConfig;
import com.google.cloud.vertexai.generativeai.GenerativeModel;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class VertexAiConfig {

    @Value("${GOOGLE_CLOUD_PROJECT_ID:your-project-id}")
    private String projectId;

    @Value("${GOOGLE_CLOUD_LOCATION:us-central1}")
    private String location;

    @Value("${VERTEX_AI_MODEL_NAME:gemini-2.5-flash}")
    private String modelName;

    @Value("${GOOGLE_APPLICATION_CREDENTIALS:}")
    private String credentialsPath;

    @Bean
    public VertexAI vertexAI() throws java.io.IOException {
        VertexAI.Builder builder = new VertexAI.Builder()
                .setProjectId(projectId)
                .setLocation(location);

        if (credentialsPath != null && !credentialsPath.isEmpty()) {
            com.google.auth.oauth2.GoogleCredentials credentials =
                    com.google.auth.oauth2.ServiceAccountCredentials.fromStream(
                                    new java.io.FileInputStream(credentialsPath))
                            .createScoped(java.util.Collections.singletonList("https://www.googleapis.com/auth/cloud-platform"));
            builder.setCredentials(credentials);
        }

        return builder.build();
    }

    @Bean
    public GenerativeModel generativeModel(VertexAI vertexAI) {
        GenerationConfig config = GenerationConfig.newBuilder()
                .setResponseMimeType("application/json")
                .build();
        return new GenerativeModel(modelName, vertexAI)
                .withGenerationConfig(config);
    }
}
