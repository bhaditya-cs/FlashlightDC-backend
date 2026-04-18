package org.flashlightdc.flashlight.service;

import org.flashlightdc.flashlight.client.CongressApiClient;
import org.flashlightdc.flashlight.dto.BillDetailResponse;
import org.flashlightdc.flashlight.dto.BillListResponse;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Service
public class BillService {

    private final CongressApiClient congressApiClient;

    public BillService(CongressApiClient congressApiClient) {
        this.congressApiClient = congressApiClient;
    }

    public Mono<BillListResponse> getBills(int congress, int limit, int offset) {
        return congressApiClient.getBills(congress, limit, offset);
    }

    public Mono<BillDetailResponse> getBill(int congress, String type, int number) {
        return congressApiClient.getBill(congress, type, number);
    }
}
