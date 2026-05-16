package org.flashlightdc.flashlight.controller;

import org.flashlightdc.flashlight.dto.BillStatsDto;
import org.flashlightdc.flashlight.dto.CosponsorAveragesDto;
import org.flashlightdc.flashlight.dto.PartyBreakdownDto;
import org.flashlightdc.flashlight.dto.StateLegislationDto;
import org.flashlightdc.flashlight.service.StatsService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/stats")
public class StatsController {

    private final StatsService statsService;

    public StatsController(StatsService statsService) {
        this.statsService = statsService;
    }

    @GetMapping("/bills")
    public ResponseEntity<BillStatsDto> getBillStats(
            @RequestParam(defaultValue = "119") int congress
    ) {
        return ResponseEntity.ok(statsService.getBillStats(congress));
    }

    @GetMapping("/party-breakdown")
    public ResponseEntity<List<PartyBreakdownDto>> getPartyBreakdown(
            @RequestParam(defaultValue = "119") int congress
    ) {
        return ResponseEntity.ok(statsService.getPartyBreakdown(congress));
    }

    @GetMapping("/state-legislation")
    public ResponseEntity<List<StateLegislationDto>> getStateLegislation(
            @RequestParam(defaultValue = "119") int congress
    ) {
        return ResponseEntity.ok(statsService.getStateLegislation(congress));
    }

    @GetMapping("/cosponsor-averages")
    public ResponseEntity<CosponsorAveragesDto> getCosponsorAverages(
            @RequestParam(defaultValue = "119") int congress
    ) {
        return ResponseEntity.ok(statsService.getCosponsorAverages(congress));
    }
}