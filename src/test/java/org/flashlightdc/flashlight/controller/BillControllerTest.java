package org.flashlightdc.flashlight.controller;

import org.flashlightdc.flashlight.dto.*;
import org.flashlightdc.flashlight.entity.Bill;
import org.flashlightdc.flashlight.service.BillService;
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
    }

    // raw API endpoints
    @Test
    void getBillsRaw_ShouldReturnOk() {
        when(billService.getBills(anyInt(), anyInt(), anyInt()))
                .thenReturn(Mono.just(mockListResponse));

        webTestClient.get()
                .uri("/api/bills/raw?congress=119&limit=20&offset=0")
                .exchange()
                .expectStatus().isOk();
    }

    @Test
    void getBillsRaw_DefaultParams_ShouldReturnOk() {
        when(billService.getBills(anyInt(), anyInt(), anyInt()))
                .thenReturn(Mono.just(mockListResponse));

        webTestClient.get()
                .uri("/api/bills/raw")
                .exchange()
                .expectStatus().isOk();
    }

    @Test
    void getBillRaw_ShouldReturnOk() {
        when(billService.getBill(anyInt(), anyString(), anyInt()))
                .thenReturn(Mono.just(mockDetailResponse));

        webTestClient.get()
                .uri("/api/bills/raw/119/hr/1")
                .exchange()
                .expectStatus().isOk();
    }

    @Test
    void getBillRaw_WhenApiError_ShouldReturnServerError() {
        when(billService.getBill(anyInt(), anyString(), anyInt()))
                .thenReturn(Mono.error(new RuntimeException("Congress API unavailable")));

        webTestClient.get()
                .uri("/api/bills/raw/119/hr/1")
                .exchange()
                .expectStatus().is5xxServerError();
    }

    // fetch and persist endpoints
    @Test
    void fetchAndPersistBills_ShouldReturnOkWithCount() {
        BillListResponse responseWithBills = new BillListResponse(
                List.of(
                        new BillSummaryDto(119, "1", "hr", "Test Bill", "House",
                                "2024-01-01", new LatestActionDto("2024-01-02", "Referred", null),
                                List.of(), "https://api.congress.gov/v3/bill/119/hr/1")
                ),
                new PaginationDto(1, null, null)
        );

        when(billService.getBills(anyInt(), anyInt(), anyInt()))
                .thenReturn(Mono.just(responseWithBills));
        when(billService.saveBill(any(BillSummaryDto.class)))
                .thenReturn(mockBill);

        webTestClient.post()
                .uri("/api/bills/fetch?congress=119&limit=20&offset=0")
                .exchange()
                .expectStatus().isOk()
                .expectBody(String.class)
                .isEqualTo("Persisted 1 bills");
    }

    @Test
    void fetchAndPersistBill_ShouldReturnPersistedBill() {
        when(billService.getBill(anyInt(), anyString(), anyInt()))
                .thenReturn(Mono.just(mockDetailResponse));
        when(billService.saveBill(any(BillDetailDto.class)))
                .thenReturn(mockBill);

        webTestClient.post()
                .uri("/api/bills/fetch/119/hr/1")
                .exchange()
                .expectStatus().isOk();
    }

    // DB read endpoints
    @Test
    void getBillsFromDb_ShouldReturnPage() {
        when(billService.findByCongressPaginated(anyInt(), any()))
                .thenReturn(new PageImpl<>(List.of(mockBill), PageRequest.of(0, 20), 1));

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
                .thenReturn(Optional.of(mockBill));

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
                .thenReturn(new PageImpl<>(List.of(mockBill), PageRequest.of(0, 20), 1));

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
                .thenReturn(new PageImpl<>(List.of(), PageRequest.of(0, 20), 0));

        webTestClient.get()
                .uri("/api/bills/policy-area/Unknown?congress=119&page=0&size=20")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.content").isArray()
                .jsonPath("$.content.length()").isEqualTo(0);
    }
}