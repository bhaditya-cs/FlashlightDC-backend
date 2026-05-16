package org.flashlightdc.flashlight.controller;

import org.flashlightdc.flashlight.dto.SummaryResponse;
import org.flashlightdc.flashlight.entity.Bill;
import org.flashlightdc.flashlight.service.BillService;
import org.flashlightdc.flashlight.service.SummarizationService;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api/v1/bills")
@CrossOrigin(origins = "${FRONTEND_URL:http://localhost:3000}")
public class SummarizationController {

    private final SummarizationService summarizationService;
    private final BillService billService;

    public SummarizationController(SummarizationService summarizationService,
                                   BillService billService) {
        this.summarizationService = summarizationService;
        this.billService = billService;
    }

    @GetMapping("/{congress}/{type}/{number}/summary")
    public Mono<SummaryResponse> getBillSummary(
            @PathVariable int congress,
            @PathVariable String type,
            @PathVariable int number) {
        return billService.findByCongressAndTypeAndNumber(congress, type, String.valueOf(number))
                .filter(bill -> bill.getSummary() != null && !bill.getSummary().isEmpty())
                .map(bill -> SummaryResponse.builder()
                        .billId(String.format("%d-%s-%d", congress, type, number))
                        .summary(bill.getSummary())
                        .status("SUCCESS")
                        .build())
                .map(Mono::just)
                .orElse(summarizationService.summarizeBill(congress, type, number));
    }
}
