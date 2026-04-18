package org.flashlightdc.flashlight.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.client.ClientRequest;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.util.UriComponentsBuilder;

// config/WebClientConfig.java
@Configuration
public class WebClientConfig {

    @Value("${congress.api.key}")
    private String apiKey;

    @Value("${congress.api.base-url:https://api.congress.gov/v3}")
    private String baseUrl;

    @Bean
    public WebClient congressWebClient() {
        return WebClient.builder()
                .baseUrl(baseUrl)
                .defaultHeader(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
                .filter((request, next) -> {
                    ClientRequest modified = ClientRequest.from(request)
                            .url(UriComponentsBuilder.fromUri(request.url())
                                    .queryParam("api_key", apiKey)
                                    .queryParam("format", "json")
                                    .build().toUri())
                            .build();
                    return next.exchange(modified);
                })
                .codecs(c -> c.defaultCodecs().maxInMemorySize(2 * 1024 * 1024))
                .build();
    }
}