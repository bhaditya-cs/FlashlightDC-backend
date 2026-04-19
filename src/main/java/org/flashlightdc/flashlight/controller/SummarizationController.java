package org.flashlightdc.flashlight.controller;

import org.flashlightdc.flashlight.dto.SummaryResponse;
import org.flashlightdc.flashlight.service.SummarizationService;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api/v1/bills")
@CrossOrigin(origins = "${FRONTEND_URL:http://localhost:3000}")
public class SummarizationController {

    private final SummarizationService summarizationService;

    public SummarizationController(SummarizationService summarizationService) {
        this.summarizationService = summarizationService;
    }

    @GetMapping("/{congress}/{type}/{number}/summary")
    public Mono<SummaryResponse> getBillSummary(
            @PathVariable int congress,
            @PathVariable String type,
            @PathVariable int number) {
        return summarizationService.summarizeBill(congress, type, number);
    }
}
