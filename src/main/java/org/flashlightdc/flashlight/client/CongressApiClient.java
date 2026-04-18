package org.flashlightdc.flashlight.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.flashlightdc.flashlight.dto.BillDetailResponse;
import org.flashlightdc.flashlight.dto.BillListResponse;
import org.flashlightdc.flashlight.dto.MemberDetailResponse;
import org.flashlightdc.flashlight.dto.MemberListResponse;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

// client/CongressApiClient.java
@Component
public class CongressApiClient {

    private final WebClient webClient;

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
}