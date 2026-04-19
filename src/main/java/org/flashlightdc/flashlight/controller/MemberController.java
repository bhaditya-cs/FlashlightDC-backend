package org.flashlightdc.flashlight.controller;

import org.flashlightdc.flashlight.dto.BillListResponse;
import org.flashlightdc.flashlight.dto.MemberDetailResponse;
import org.flashlightdc.flashlight.dto.MemberListResponse;
import org.flashlightdc.flashlight.entity.Member;
import org.flashlightdc.flashlight.service.MemberService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.util.List;

// controller/MemberController.java
@RestController
@RequestMapping("/api/members")
public class MemberController {

    private final MemberService memberService;

    public MemberController(MemberService memberService) {
        this.memberService = memberService;
    }

    // raw API endpoints (debug)
    @GetMapping("/raw")
    public Mono<MemberListResponse> getMembersRaw(
            @RequestParam(defaultValue = "119") int congress,
            @RequestParam(defaultValue = "20") int limit,
            @RequestParam(defaultValue = "0") int offset
    ) {
        return memberService.getMembers(congress, limit, offset);
    }

    // fetch from API and persist
    @PostMapping("/fetch")
    public Mono<ResponseEntity<String>> fetchAndPersistMembers(
            @RequestParam(defaultValue = "119") int congress,
            @RequestParam(defaultValue = "20") int limit,
            @RequestParam(defaultValue = "0") int offset
    ) {
        return memberService.getMembers(congress, limit, offset)
                .map(response -> {
                    response.members().forEach(memberService::saveMember);
                    return ResponseEntity.ok("Persisted " + response.members().size() + " members");
                });
    }

    // read from DB
    @GetMapping
    public ResponseEntity<List<Member>> getMembers(
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
    public ResponseEntity<Member> getMember(@PathVariable String bioguideId) {
        return memberService.findById(bioguideId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
}
