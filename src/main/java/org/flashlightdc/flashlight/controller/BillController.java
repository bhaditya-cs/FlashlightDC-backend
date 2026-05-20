package org.flashlightdc.flashlight.controller;

import org.flashlightdc.flashlight.dto.BillDetailResponse;
import org.flashlightdc.flashlight.dto.BillListResponse;
import org.flashlightdc.flashlight.dto.BillStatsResponse;
import org.flashlightdc.flashlight.entity.Bill;
import org.flashlightdc.flashlight.service.BillService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.util.List;

@RestController
@RequestMapping("/api/bills")
public class BillController {

    private final BillService billService;

    public BillController(BillService billService) {
        this.billService = billService;
    }

    // raw API endpoints (debug)
    @GetMapping("/raw")
    public Mono<BillListResponse> getBillsRaw(
            @RequestParam(defaultValue = "119") int congress,
            @RequestParam(defaultValue = "20") int limit,
            @RequestParam(defaultValue = "0") int offset
    ) {
        return billService.getBills(congress, limit, offset);
    }

    @GetMapping("/raw/{congress}/{type}/{number}")
    public Mono<BillDetailResponse> getBillRaw(
            @PathVariable int congress,
            @PathVariable String type,
            @PathVariable int number
    ) {
        return billService.getBill(congress, type, number);
    }

    // fetch from API and persist
    @PostMapping("/fetch")
    public Mono<ResponseEntity<String>> fetchAndPersistBills(
            @RequestParam(defaultValue = "119") int congress,
            @RequestParam(defaultValue = "20") int limit,
            @RequestParam(defaultValue = "0") int offset
    ) {
        return billService.getBills(congress, limit, offset)
                .map(response -> {
                    response.bills().forEach(billService::saveBill);
                    return ResponseEntity.ok("Persisted " + response.bills().size() + " bills");
                });
    }

    @PostMapping("/fetch/{congress}/{type}/{number}")
    public Mono<ResponseEntity<Bill>> fetchAndPersistBill(
            @PathVariable int congress,
            @PathVariable String type,
            @PathVariable int number
    ) {
        return billService.getBill(congress, type, number)
                .map(response -> ResponseEntity.ok(billService.saveBill(response.bill())));
    }

    // read from DB
    @GetMapping
    public ResponseEntity<Page<Bill>> getBills(
            @RequestParam(defaultValue = "119") int congress,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        Pageable pageable = PageRequest.of(page, size);
        return ResponseEntity.ok(billService.findByCongressPaginated(congress, pageable));
    }

    @GetMapping("/{congress}/{type}/{number}")
    public ResponseEntity<Bill> getBill(
            @PathVariable int congress,
            @PathVariable String type,
            @PathVariable String number
    ) {
        return billService.findByCongressAndTypeAndNumber(congress, type, number)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/policy-area/{policyArea}")
    public ResponseEntity<Page<Bill>> getByPolicyArea(
            @PathVariable String policyArea,
            @RequestParam(defaultValue = "119") int congress,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        Pageable pageable = PageRequest.of(page, size);
        return ResponseEntity.ok(billService.findByPolicyAreaAndCongress(policyArea, congress, pageable));
    }

    @GetMapping("/stats")
    public ResponseEntity<BillStatsResponse> getStats(
            @RequestParam(defaultValue = "119") int congress
    ) {
        return ResponseEntity.ok(billService.getStats(congress));
    }
}