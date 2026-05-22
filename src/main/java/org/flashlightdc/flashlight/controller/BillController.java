package org.flashlightdc.flashlight.controller;

import org.flashlightdc.flashlight.dto.BillCacheDto;
import org.flashlightdc.flashlight.dto.BillDetailResponse;
import org.flashlightdc.flashlight.dto.BillListResponse;
import org.flashlightdc.flashlight.dto.BillStatsResponse;
import org.flashlightdc.flashlight.entity.Bill;
import org.flashlightdc.flashlight.service.BillService;
import org.flashlightdc.flashlight.util.RestResponsePage;
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




    // read from DB
    @GetMapping
    public ResponseEntity<RestResponsePage<BillCacheDto>> getBills(
            @RequestParam(defaultValue = "119") int congress,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        Pageable pageable = PageRequest.of(page, size);
        return ResponseEntity.ok(billService.findByCongressPaginated(congress, pageable));
    }

    @GetMapping("/{congress}/{type}/{number}")
    public ResponseEntity<BillCacheDto> getBill(
            @PathVariable int congress,
            @PathVariable String type,
            @PathVariable String number
    ) {
        return billService.findByCongressAndTypeAndNumber(congress, type, number)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/policy-area/{policyArea}")
    public ResponseEntity<RestResponsePage<BillCacheDto>> getByPolicyArea(
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