package org.flashlightdc.flashlight.service;

import org.flashlightdc.flashlight.client.CongressApiClient;
import org.flashlightdc.flashlight.client.VertexAiClient;
import org.flashlightdc.flashlight.dto.BillDetailResponse;
import org.flashlightdc.flashlight.dto.SummaryResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.Optional;

@Service
public class SummarizationService {

    private static final Logger log = LoggerFactory.getLogger(SummarizationService.class);

    private final BillService billService;
    private final CongressApiClient congressApiClient;
    private final VertexAiClient vertexAiClient;

    @Value("${VERTEX_AI_MODEL_NAME:gemini-2.5-flash}")
    private String modelName;

    @Value("${GOOGLE_CLOUD_PROJECT_ID:}")
    private String projectId;

    public SummarizationService(BillService billService, CongressApiClient congressApiClient,
                                VertexAiClient vertexAiClient) {
        this.billService = billService;
        this.congressApiClient = congressApiClient;
        this.vertexAiClient = vertexAiClient;
    }

    private boolean isAiEnabled() {
        return projectId != null && !projectId.isEmpty() && !projectId.equals("your-project-id");
    }

    public Mono<SummaryResponse> summarizeBill(int congress, String type, int number) {
        String billId = String.format("%d-%s-%d", congress, type, number);

        if (!isAiEnabled()) {
            return Mono.just(SummaryResponse.builder()
                    .billId(billId)
                    .status("DISABLED")
                    .summary("AI summarization is not configured.")
                    .build());
        }

        return billService.getBill(congress, type, number)
                .flatMap(billDetail -> {
                    log.debug("Metadata for {}: {}", billId, extractContext(billDetail));

                    Mono<Optional<String>> textMono = congressApiClient
                            .getBillTextVersions(congress, type, number)
                            .map(Optional::of)
                            .defaultIfEmpty(Optional.empty());

                    Mono<Optional<String>> summaryMono = congressApiClient
                            .getBillSummariesText(congress, type, number)
                            .map(Optional::of)
                            .defaultIfEmpty(Optional.empty());

                    return Mono.zip(textMono, summaryMono)
                            .flatMap(tuple -> {
                                Optional<String> billTextOpt = tuple.getT1();
                                Optional<String> crsSummaryOpt = tuple.getT2();

                                if (billTextOpt.isEmpty() && crsSummaryOpt.isEmpty()) {
                                    return Mono.empty();
                                }

                                String billText = billTextOpt.orElse("Not available");
                                String crsSummary = crsSummaryOpt.orElse("Not available");

                                String fullPrompt = String.format(
                                        vertexAiClient.getPromptTemplate(),
                                        billText,
                                        crsSummary
                                );

                                return vertexAiClient.summarizeText(fullPrompt)
                                        .map(rawJson -> SummaryResponse.builder()
                                                .billId(billId)
                                                .summary(rawJson)
                                                .modelUsed(modelName)
                                                .status("SUCCESS")
                                                .format("json")
                                                .build())
                                        .doOnNext(response -> {
                                            if ("SUCCESS".equals(response.getStatus())) {
                                                billService.saveSummary(congress, type,
                                                        String.valueOf(number),
                                                        response.getSummary());
                                            }
                                        });
                            })
                            .switchIfEmpty(Mono.just(SummaryResponse.builder()
                                    .billId(billId)
                                    .status("NO_CONTENT")
                                    .summary("No bill text or CRS summary available for summarization.")
                                    .build()));
                })
                .switchIfEmpty(Mono.just(SummaryResponse.builder()
                        .billId(billId)
                        .status("NO_CONTENT")
                        .summary("Bill not found in database.")
                        .build()))
                .onErrorResume(e -> {
                    log.error("Summarization failed for {}: {}", billId, e.getMessage(), e);
                    return Mono.just(SummaryResponse.builder()
                            .billId(billId)
                            .status("ERROR")
                            .summary("Failed to generate summary: " + e.getMessage())
                            .build());
                });
    }

    /**
     * Blocking wrapper for scheduler use. Takes bill data from DB and
     * re-fetches text/CRS from Congress.gov API for Vertex AI summarization.
     */
    public SummaryResponse summarizeBillBlocking(int congress, String type, String billNumber) {
        if (!isAiEnabled()) {
            return SummaryResponse.builder()
                    .billId(String.format("%d-%s-%s", congress, type, billNumber))
                    .status("DISABLED")
                    .summary("AI summarization is not configured.")
                    .build();
        }
        try {
            int number = Integer.parseInt(billNumber);
            return summarizeBill(congress, type, number).block();
        } catch (NumberFormatException e) {
            log.warn("Cannot summarize bill with non-numeric number: {}-{}-{}", congress, type, billNumber);
            return SummaryResponse.builder()
                    .billId(String.format("%d-%s-%s", congress, type, billNumber))
                    .status("ERROR")
                    .summary("Invalid bill number: " + billNumber)
                    .build();
        }
    }

    private String extractContext(BillDetailResponse response) {
        if (response.bill() == null)
            return "";
        StringBuilder context = new StringBuilder();
        context.append("Title: ").append(response.bill().title()).append("\n");
        context.append("Congress: ").append(response.bill().congress()).append("\n");
        context.append("Type: ").append(response.bill().type()).append("\n");
        context.append("Origin Chamber: ").append(response.bill().originChamber()).append("\n");
        context.append("Introduced Date: ").append(response.bill().introducedDate()).append("\n");

        if (response.bill().latestAction() != null) {
            context.append("Latest Action: ").append(response.bill().latestAction().text()).append("\n");
            context.append("Latest Action Date: ").append(response.bill().latestAction().actionDate()).append("\n");
        }

        if (response.bill().policyArea() != null) {
            context.append("Policy Area: ").append(response.bill().policyArea().name()).append("\n");
        }

        if (response.bill().sponsors() != null && !response.bill().sponsors().isEmpty()) {
            context.append("Sponsors: ");
            StringBuilder sponsorsList = new StringBuilder();
            response.bill().sponsors().forEach(sponsor -> {
                if (sponsor.fullName() != null && !sponsor.fullName().isEmpty()) {
                    sponsorsList.append(sponsor.fullName());
                    if (sponsor.party() != null && !sponsor.party().isEmpty()) {
                        sponsorsList.append(" (").append(sponsor.party()).append(")");
                    }
                    sponsorsList.append(", ");
                }
            });
            if (sponsorsList.length() > 0) {
                sponsorsList.delete(sponsorsList.length() - 2, sponsorsList.length());
                context.append(sponsorsList);
            }
            context.append("\n");
        }

        if (response.bill().constitutionalAuthorityStatementText() != null) {
            context.append("Constitutional Authority Statement: ")
                   .append(response.bill().constitutionalAuthorityStatementText()).append("\n");
        }

        if (response.bill().url() != null) {
            context.append("URL: ").append(response.bill().url()).append("\n");
        }
        return context.toString();
    }
}
