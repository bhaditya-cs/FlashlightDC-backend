package org.flashlightdc.flashlight.controller;

import org.flashlightdc.flashlight.dto.*;
import org.flashlightdc.flashlight.entity.Bill;
import org.flashlightdc.flashlight.service.BillService;
import org.flashlightdc.flashlight.util.RestResponsePage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@WebFluxTest(BillController.class)
@ActiveProfiles("test")
class BillControllerTest {

    @Autowired
    private WebTestClient webTestClient;

    @MockitoBean
    private BillService billService;

    private BillListResponse mockListResponse;
    private BillDetailResponse mockDetailResponse;
    private Bill mockBill;
    private BillCacheDto mockBillCacheDto;

    @BeforeEach
    void setUp() {
        mockListResponse = new BillListResponse(
                List.of(),
                new PaginationDto(0, null, null)
        );

        mockDetailResponse = new BillDetailResponse(
                new BillDetailDto(
                        119, "1", "hr", "Test Bill", "House",
                        "2024-01-01", new LatestActionDto("2024-01-02", "Referred to committee", null),
                        List.of(), null, new PolicyAreaDto("Healthcare"), null, null,
                        "https://api.congress.gov/v3/bill/119/hr/1"
                )
        );

        mockBill = new Bill();
        mockBill.setCongress(119);
        mockBill.setBillNumber("1");
        mockBill.setBillType("hr");
        mockBill.setTitle("Test Bill");
        mockBill.setOriginChamber("House");
        mockBill.setPolicyArea("Healthcare");

        mockBillCacheDto =billService.toCacheDto(mockBill);
    }



    // DB read endpoints
    @Test
    void getBillsFromDb_ShouldReturnPage() {
        when(billService.findByCongressPaginated(anyInt(), any()))
                .thenReturn(new RestResponsePage<BillCacheDto>(List.of(mockBillCacheDto), PageRequest.of(0, 20), 1));
        webTestClient.get()
                .uri("/api/bills?congress=119&page=0&size=20")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.content").isArray()
                .jsonPath("$.totalElements").isEqualTo(1);
    }

    @Test
    void getBillFromDb_WhenExists_ShouldReturnBill() {
        when(billService.findByCongressAndTypeAndNumber(anyInt(), anyString(), anyString()))
                .thenReturn(Optional.of(mockBillCacheDto));

        webTestClient.get()
                .uri("/api/bills/119/hr/1")
                .exchange()
                .expectStatus().isOk();
    }

    @Test
    void getBillFromDb_WhenNotFound_ShouldReturn404() {
        when(billService.findByCongressAndTypeAndNumber(anyInt(), anyString(), anyString()))
                .thenReturn(Optional.empty());

        webTestClient.get()
                .uri("/api/bills/119/hr/9999")
                .exchange()
                .expectStatus().isNotFound();
    }

    @Test
    void getByPolicyArea_ShouldReturnBills() {
        when(billService.findByPolicyAreaAndCongress(anyString(), anyInt(), any(Pageable.class)))
                .thenReturn(new RestResponsePage<BillCacheDto>(List.of(mockBillCacheDto), PageRequest.of(0, 20), 1));

        webTestClient.get()
                .uri("/api/bills/policy-area/Healthcare?congress=119&page=0&size=20")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.content").isArray()
                .jsonPath("$.totalElements").isEqualTo(1);
    }

    @Test
    void getByPolicyArea_WhenNoneFound_ShouldReturnEmptyArray() {
        when(billService.findByPolicyAreaAndCongress(anyString(), anyInt(), any(Pageable.class)))
                .thenReturn(new RestResponsePage<>(List.of(), PageRequest.of(0, 20), 0));

        webTestClient.get()
                .uri("/api/bills/policy-area/Unknown?congress=119&page=0&size=20")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.content").isArray()
                .jsonPath("$.content.length()").isEqualTo(0);
    }
}