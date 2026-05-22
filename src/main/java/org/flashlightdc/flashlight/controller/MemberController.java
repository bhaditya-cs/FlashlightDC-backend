package org.flashlightdc.flashlight.controller;

import org.flashlightdc.flashlight.dto.*;
import org.flashlightdc.flashlight.entity.Bill;
import org.flashlightdc.flashlight.entity.Member;
import org.flashlightdc.flashlight.service.MemberService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.util.List;

// controller/MemberController.java
@RestController
@RequestMapping("/api/members")
public class    MemberController {

    private final MemberService memberService;

    public MemberController(MemberService memberService) {
        this.memberService = memberService;
    }


    // read from DB
    @GetMapping
    public ResponseEntity<List<MemberCacheDto>> getMembers(
            @RequestParam(required = false) String party,
            @RequestParam(required = false) String state
    ) {
        if (party != null && state != null) {
            return ResponseEntity.ok(memberService.findByPartyAndState(party, state));
        } else if (party != null) {
            return ResponseEntity.ok(memberService.findByParty(party));
        } else if (state != null) {
            return ResponseEntity.ok(memberService.findByState(state));
        }
        return ResponseEntity.ok(memberService.findAll());
    }

    @GetMapping("/{bioguideId}")
    public ResponseEntity<MemberCacheDto> getMember(@PathVariable String bioguideId) {
        return memberService.findById(bioguideId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/{bioguideId}/bills")
    public ResponseEntity<Page<Bill>> getSponsoredBills(
            @PathVariable String bioguideId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Pageable pageable = PageRequest.of(page, size);
        return ResponseEntity.ok(memberService.getSponsoredBills(bioguideId, pageable));

    }
    @GetMapping("/{bioguideId}/sponsorCount")
    public ResponseEntity<SponsorCountDto> getSponsorCount (@PathVariable String bioguideId) {
        return ResponseEntity.ok(memberService.getSponsorCount(bioguideId));
    }
}
