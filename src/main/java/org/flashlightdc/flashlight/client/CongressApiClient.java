package org.flashlightdc.flashlight.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.flashlightdc.flashlight.dto.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.util.UriComponentsBuilder;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;

import java.time.Duration;

// client/CongressApiClient.java
@Component
public class CongressApiClient {

    private final WebClient webClient;

    @Value("${congress.api.key}")
    private String apiKey;

    public CongressApiClient(WebClient congressWebClient) {
        this.webClient = congressWebClient;
    }

    public Mono<BillListResponse> getBills(int congress, int limit, int offset) {
        return webClient.get()
                .uri(u -> u.path("/bill/{congress}")
                        .queryParam("limit", limit)
                        .queryParam("offset", offset)
                        .build(congress))
                .retrieve()
                .onStatus(HttpStatusCode::is4xxClientError, r ->
                        Mono.error(new CongressApiException("Client error: " + r.statusCode())))
                .onStatus(HttpStatusCode::is5xxServerError, r ->
                        Mono.error(new CongressApiException("Congress API unavailable")))
                .bodyToMono(BillListResponse.class);
    }

    public Mono<BillDetailResponse> getBill(int congress, String type, int number) {
        return webClient.get()
                .uri(u -> u.path("/bill/{congress}/{type}/{number}")
                        .build(congress, type.toLowerCase(), number))
                .retrieve()
                .onStatus(HttpStatusCode::is4xxClientError, r ->
                        Mono.error(new CongressApiException("Bill not found")))
                .bodyToMono(String.class)
                .doOnNext(raw -> System.out.println("RAW: " + raw))
                .map(raw -> {
                    try {
                        return new ObjectMapper().readValue(raw, BillDetailResponse.class);
                    } catch (Exception e) {
                        throw new RuntimeException("Deserialization failed", e);
                    }
                });
    }

    public Mono<MemberListResponse> getMembers(int congress, int limit, int offset) {
        return webClient.get()
                .uri(u -> u.path("/member/congress/{congress}")
                        .queryParam("limit", limit)
                        .queryParam("offset", offset)
                        .build(congress))
                .retrieve()
                .onStatus(HttpStatusCode::is4xxClientError, r ->
                        Mono.error(new CongressApiException("Client error: " + r.statusCode())))
                .bodyToMono(String.class)
                .doOnNext(raw -> System.out.println("RAW: " + raw))
                .map(raw -> {
                    try {
                        return new ObjectMapper().readValue(raw, MemberListResponse.class);
                    } catch (Exception e) {
                        throw new RuntimeException("Deserialization failed", e);
                    }
                });
    }

    public Mono<MemberDetailResponse> getMember(String id) {
        return webClient.get()
            .uri(u -> u.path("/member/congress/{id}")
                    .queryParam("format", "json")
                    .build(id))
            .retrieve()
            .onStatus(HttpStatusCode::is4xxClientError, r ->
                    Mono.error(new CongressApiException("Client error: " + r.statusCode())))
                .bodyToMono(String.class)
                .doOnNext(raw -> System.out.println("RAW: " + raw))
                .map(raw -> {
                    try {
                        return new ObjectMapper().readValue(raw, MemberDetailResponse.class);
                    } catch (Exception e) {
                        throw new RuntimeException("Deserialization failed", e);
                    }
                });
    }

    public Mono<String> getBillTextVersions(int congress, String type, int number) {
        return webClient.get()
                .uri(u -> u.path("/bill/{congress}/{type}/{number}/text")
                        .build(congress, type.toLowerCase(), number))
                .retrieve()
                .onStatus(HttpStatusCode::is4xxClientError, r ->
                        Mono.error(new CongressApiException("Bill text not found")))
                .bodyToMono(String.class)
                .map(raw -> {
                    try {
                        return new ObjectMapper().readValue(raw, BillTextResponse.class);
                    } catch (Exception e) {
                        throw new RuntimeException("Deserialization failed", e);
                    }
                })
                .flatMap(response -> {
                    if (response.textVersions() == null || response.textVersions().isEmpty()) {
                        return Mono.empty();
                    }
                    for (TextVersionDto version : response.textVersions()) {
                        if (version.formats() != null) {
                            for (TextFormatDto format : version.formats()) {
                                if (format.type() != null && format.type().contains("Text")
                                        && format.url() != null) {
                                    String fullUrl = UriComponentsBuilder.fromHttpUrl(format.url())
                                            .queryParam("api_key", apiKey)
                                            .build()
                                            .toUriString();
                                    return WebClient.create()
                                            .get()
                                            .uri(fullUrl)
                                            .retrieve()
                                            .bodyToMono(String.class);
                                }
                            }
                        }
                    }
                    return Mono.empty();
                });
    }

    public Mono<String> getBillSummariesText(int congress, String type, int number) {
        return webClient.get()
                .uri(u -> u.path("/bill/{congress}/{type}/{number}/summaries")
                        .build(congress, type.toLowerCase(), number))
                .retrieve()
                .onStatus(HttpStatusCode::is4xxClientError, r ->
                        Mono.error(new CongressApiException("Bill summaries not found")))
                .bodyToMono(String.class)
                .map(raw -> {
                    try {
                        return new ObjectMapper().readValue(raw, BillSummariesResponse.class);
                    } catch (Exception e) {
                        throw new RuntimeException("Deserialization failed", e);
                    }
                })
                .flatMap(response -> {
                    if (response.summaries() != null && !response.summaries().isEmpty()) {
                        String text = response.summaries().get(0).text();
                        if (text != null && !text.isBlank()) {
                            return Mono.just(text);
                        }
                    }
                    return Mono.empty();
                });
    }

    public Mono<CosponsorListResponse> getCosponsors(int congress, String type, int number) {
        return webClient.get()
                .uri(u -> u.path("/bill/{congress}/{type}/{number}/cosponsors")
                        .build(congress, type.toLowerCase(), number))
                .retrieve()
                .onStatus(status -> status.value() == 429, r ->
                        Mono.error(new CongressApiException("Rate limited")))
                .onStatus(HttpStatusCode::is4xxClientError, r ->
                        Mono.error(new CongressApiException("Cosponsors not found")))
                .onStatus(HttpStatusCode::is5xxServerError, r ->
                        Mono.error(new CongressApiException("Congress API unavailable")))
                .bodyToMono(String.class)
                .retryWhen(Retry.backoff(3, Duration.ofSeconds(30))
                        .filter(e -> e instanceof CongressApiException &&
                                e.getMessage().equals("Rate limited"))
                        )
                .map(raw -> {
                    try {
                        System.out.println(raw);
                        return new ObjectMapper().readValue(raw, CosponsorListResponse.class);
                    } catch (Exception e) {
                        throw new RuntimeException("Deserialization failed", e);
                    }
                });
    }
}