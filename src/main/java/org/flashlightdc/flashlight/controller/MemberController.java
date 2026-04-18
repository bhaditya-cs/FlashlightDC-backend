package org.flashlightdc.flashlight.controller;

import org.flashlightdc.flashlight.dto.BillListResponse;
import org.flashlightdc.flashlight.dto.MemberDetailResponse;
import org.flashlightdc.flashlight.dto.MemberListResponse;
import org.flashlightdc.flashlight.service.MemberService;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api/members")
public class MemberController {
    private final MemberService memberService;

    public MemberController(MemberService memberService) {
        this.memberService = memberService;
    }

    @GetMapping
    public Mono<MemberListResponse> getMembers(
            @RequestParam(defaultValue = "119") int congress,
            @RequestParam(defaultValue = "20") int limit,
            @RequestParam(defaultValue = "0") int offset
    ) {
        return memberService.getMembers(congress, limit, offset);
    }

    @GetMapping("/{id}")
    public Mono<MemberDetailResponse> getMember(@PathVariable String id) {
        return memberService.getMember(id);
    }
}
