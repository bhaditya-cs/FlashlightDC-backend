package org.flashlightdc.flashlight.service;

import org.flashlightdc.flashlight.client.VertexAiClient;
import org.flashlightdc.flashlight.dto.BillDetailResponse;
import org.flashlightdc.flashlight.dto.SummaryResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Service
public class SummarizationService {

    private final BillService billService;
    private final VertexAiClient vertexAiClient;

    @Value("${VERTEX_AI_MODEL_NAME:gemini-2.5-flash}")
    private String modelName;

    public SummarizationService(BillService billService, VertexAiClient vertexAiClient) {
        this.billService = billService;
        this.vertexAiClient = vertexAiClient;
    }

    public Mono<SummaryResponse> summarizeBill(int congress, String type, int number) {
        return billService.getBill(congress, type, number)
                .flatMap(billDetail -> {
                    String context = extractContext(billDetail);
                    return summarizeRawText(context)
                            .map(summaryResponse -> {
                                summaryResponse.setBillId(String.format("%d-%s-%d", congress, type, number));
                                return summaryResponse;
                            });
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
                        .build());
    }

    private String extractContext(BillDetailResponse response) {
        if (response.bill() == null)
            return "";
        StringBuilder context = new StringBuilder();
        context.append("Title: ").append(response.bill().title()).append("\n");
        // In a real scenario, we might fetch the full text or official summary here
        // For now, we use the title and any existing metadata
        context.append("Congress: ").append(response.bill().congress()).append("\n");
        context.append("Type: ").append(response.bill().type()).append("\n");
        return context.toString();
    }
}
