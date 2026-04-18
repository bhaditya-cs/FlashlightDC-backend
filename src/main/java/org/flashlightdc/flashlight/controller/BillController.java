package org.flashlightdc.flashlight.controller;

import org.flashlightdc.flashlight.dto.BillDetailResponse;
import org.flashlightdc.flashlight.dto.BillListResponse;
import org.flashlightdc.flashlight.service.BillService;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api/bills")
public class BillController {

    private final BillService billService;

    public BillController(BillService billService) {
        this.billService = billService;
    }

    @GetMapping
    public Mono<BillListResponse> getBills(
            @RequestParam(defaultValue = "119") int congress,
            @RequestParam(defaultValue = "20") int limit,
            @RequestParam(defaultValue = "0") int offset
    ) {
        return billService.getBills(congress, limit, offset);
    }

    @GetMapping("/{congress}/{type}/{number}")
    public Mono<BillDetailResponse> getBill(
            @PathVariable int congress,
            @PathVariable String type,
            @PathVariable int number
    ) {
        return billService.getBill(congress, type, number);
    }
}
