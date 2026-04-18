package org.flashlightdc.flashlight.service;

import org.flashlightdc.flashlight.client.CongressApiClient;
import org.flashlightdc.flashlight.dto.MemberDetailResponse;
import org.flashlightdc.flashlight.dto.MemberListResponse;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Service
public class MemberService {
    private final CongressApiClient congressApiClient;

    public MemberService(CongressApiClient congressApiClient) {
        this.congressApiClient = congressApiClient;
    }

    public Mono<MemberListResponse> getMembers(int congress, int limit, int offset) {
        return congressApiClient.getMembers(congress, limit, offset);
    }

    public Mono<MemberDetailResponse> getMember(String id) {
        return congressApiClient.getMember(id);
    }
}
