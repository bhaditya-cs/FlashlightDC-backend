package org.flashlightdc.flashlight.service;

import org.flashlightdc.flashlight.client.CongressApiClient;
import org.flashlightdc.flashlight.client.VertexAiClient;
import org.flashlightdc.flashlight.dto.BillDetailResponse;
import org.flashlightdc.flashlight.dto.SummaryResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Service
public class SummarizationService {

    private final BillService billService;
    private final CongressApiClient congressApiClient;
    private final VertexAiClient vertexAiClient;

    @Value("${VERTEX_AI_MODEL_NAME:gemini-2.5-flash}")
    private String modelName;

    public SummarizationService(BillService billService, CongressApiClient congressApiClient,
                                VertexAiClient vertexAiClient) {
        this.billService = billService;
        this.congressApiClient = congressApiClient;
        this.vertexAiClient = vertexAiClient;
    }

    public Mono<SummaryResponse> summarizeBill(int congress, String type, int number) {
        return billService.getBill(congress, type, number)
                .flatMap(billDetail -> {
                    String context = extractContext(billDetail);
                    String billId = String.format("%d-%s-%d", congress, type, number);
                    return congressApiClient.getBillTextVersions(congress, type, number)
                            .map(text -> context + "\n\nFULL TEXT:\n" + text)
                            .switchIfEmpty(congressApiClient.getBillSummariesText(congress, type, number)
                                    .map(summary -> context + "\n\nCRS SUMMARY:\n" + summary))
                            .flatMap(this::summarizeRawText)
                            .map(summaryResponse -> {
                                summaryResponse.setBillId(billId);
                                return summaryResponse;
                            })
                            .switchIfEmpty(Mono.just(SummaryResponse.builder()
                                    .billId(billId)
                                    .status("NO_CONTENT")
                                    .summary("No bill text or CRS summary available for summarization.")
                                    .build()));
                })
                .onErrorResume(e -> Mono.just(SummaryResponse.builder()
                        .billId(String.format("%d-%s-%d", congress, type, number))
                        .status("ERROR")
                        .summary("Failed to generate summary: " + e.getMessage())
                        .build()));
    }

    public Mono<SummaryResponse> summarizeRawText(String text) {
        return vertexAiClient.summarizeText(text)
                .map(summary -> SummaryResponse.builder()
                        .summary(summary)
                        .modelUsed(modelName)
                        .status("SUCCESS")
                        .format("markdown")
                        .build());
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
            // Remove trailing comma and space if any sponsors added
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
