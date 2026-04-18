package org.flashlightdc.flashlight.service;

import org.flashlightdc.flashlight.client.CongressApiClient;
import org.flashlightdc.flashlight.dto.BillListResponse;
import org.flashlightdc.flashlight.dto.BillDetailResponse;
import org.flashlightdc.flashlight.dto.PaginationDto;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BillServiceTest {

    @Mock
    private CongressApiClient congressApiClient;

    @InjectMocks
    private BillService billService;

    @Test
    void getBills_ShouldReturnBills() {
        BillListResponse mockResponse = new BillListResponse(java.util.List.of(), new PaginationDto(0, null, null));
        when(congressApiClient.getBills(anyInt(), anyInt(), anyInt()))
                .thenReturn(Mono.just(mockResponse));

        Mono<BillListResponse> result = billService.getBills(119, 20, 0);

        StepVerifier.create(result)
                .expectNext(mockResponse)
                .verifyComplete();
    }

    @Test
    void getBill_ShouldReturnBillDetail() {
        BillDetailResponse mockResponse = new BillDetailResponse(null);
        when(congressApiClient.getBill(anyInt(), anyString(), anyInt()))
                .thenReturn(Mono.just(mockResponse));

        Mono<BillDetailResponse> result = billService.getBill(119, "hr", 1);

        StepVerifier.create(result)
                .expectNext(mockResponse)
                .verifyComplete();
    }
}
