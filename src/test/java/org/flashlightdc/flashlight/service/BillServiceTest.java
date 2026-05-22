package org.flashlightdc.flashlight.service;

import org.flashlightdc.flashlight.client.CongressApiClient;
import org.flashlightdc.flashlight.dto.*;

import org.flashlightdc.flashlight.entity.Bill;
import org.flashlightdc.flashlight.entity.Member;
import org.flashlightdc.flashlight.entity.Sponsor;
import org.flashlightdc.flashlight.repository.BillRepository;
import org.flashlightdc.flashlight.repository.MemberRepository;
import org.flashlightdc.flashlight.repository.SponsorRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BillServiceTest {

    @Mock
    private CongressApiClient congressApiClient;

    @Mock
    private BillRepository billRepository;

    @Mock
    private SponsorRepository sponsorRepository;

    @Mock
    private MemberRepository memberRepository;

    @InjectMocks
    private BillService billService;

    private BillListResponse mockListResponse;
    private BillDetailResponse mockDetailResponse;
    private BillSummaryDto mockSummaryDto;
    private BillDetailDto mockDetailDto;
    private Bill mockBill;

    @BeforeEach
    void setUp() {
        mockSummaryDto = new BillSummaryDto(
                119, "1", "hr", "Test Bill", "House",
                "2024-01-01",
                new LatestActionDto("2024-01-02", "Referred to committee", null),
                List.of(),
                "https://api.congress.gov/v3/bill/119/hr/1"
        );

        mockDetailDto = new BillDetailDto(
                119, "1", "hr", "Test Bill", "House",
                "2024-01-01",
                new LatestActionDto("2024-01-02", "Referred to committee", null),
                List.of(), null,
                new PolicyAreaDto("Healthcare"),
                null, null,
                "https://api.congress.gov/v3/bill/119/hr/1"
        );

        mockListResponse = new BillListResponse(
                List.of(mockSummaryDto),
                new PaginationDto(1, null, null)
        );

        mockDetailResponse = new BillDetailResponse(mockDetailDto);

        mockBill = new Bill();
        mockBill.setCongress(119);
        mockBill.setBillNumber("1");
        mockBill.setBillType("hr");
        mockBill.setTitle("Test Bill");
        mockBill.setOriginChamber("House");
        mockBill.setPolicyArea("Healthcare");
        mockBill.setIntroducedDate("2024-01-01");
        mockBill.setLatestActionDate("2024-01-02");
        mockBill.setLatestActionText("Referred to committee");
    }

    // raw API fetch
    @Test
    void getBills_ShouldReturnBills() {
        when(congressApiClient.getBills(anyInt(), anyInt(), anyInt()))
                .thenReturn(Mono.just(mockListResponse));

        StepVerifier.create(billService.getBills(119, 20, 0))
                .expectNext(mockListResponse)
                .verifyComplete();
    }

    @Test
    void getBills_WhenApiError_ShouldPropagateError() {
        when(congressApiClient.getBills(anyInt(), anyInt(), anyInt()))
                .thenReturn(Mono.error(new RuntimeException("Congress API unavailable")));

        StepVerifier.create(billService.getBills(119, 20, 0))
                .expectErrorMessage("Congress API unavailable")
                .verify();
    }

    @Test
    void getBill_ShouldReturnBillDetail() {
        when(congressApiClient.getBill(anyInt(), anyString(), anyInt()))
                .thenReturn(Mono.just(mockDetailResponse));

        StepVerifier.create(billService.getBill(119, "hr", 1))
                .expectNext(mockDetailResponse)
                .verifyComplete();
    }

    @Test
    void getBill_WhenNotFound_ShouldPropagateError() {
        when(congressApiClient.getBill(anyInt(), anyString(), anyInt()))
                .thenReturn(Mono.error(new RuntimeException("Bill not found")));

        StepVerifier.create(billService.getBill(119, "hr", 9999))
                .expectErrorMessage("Bill not found")
                .verify();
    }

    // save from summary DTO
    @Test
    void saveBill_FromSummaryDto_WhenNew_ShouldInsert() {
        when(billRepository.findByCongressAndBillTypeAndBillNumber(119, "hr", "1"))
                .thenReturn(Optional.empty());
        when(billRepository.save(any(Bill.class)))
                .thenReturn(mockBill);

        Bill result = billService.saveBill(mockSummaryDto);

        assertThat(result.getTitle()).isEqualTo("Test Bill");
        assertThat(result.getCongress()).isEqualTo(119);
        verify(billRepository, times(1)).save(any(Bill.class));
    }

    @Test
    void saveBill_FromSummaryDto_WhenExists_ShouldUpdate() {
        when(billRepository.findByCongressAndBillTypeAndBillNumber(119, "hr", "1"))
                .thenReturn(Optional.of(mockBill));
        when(billRepository.save(any(Bill.class)))
                .thenReturn(mockBill);

        Bill result = billService.saveBill(mockSummaryDto);

        assertThat(result).isNotNull();
        verify(billRepository, times(1)).save(any(Bill.class));
        verify(billRepository, never()).findById(any());
    }

    // save from detail DTO
    @Test
    void saveBill_FromDetailDto_WhenNew_ShouldInsert() {
        when(billRepository.findByCongressAndBillTypeAndBillNumber(119, "hr", "1"))
                .thenReturn(Optional.empty());
        when(billRepository.save(any(Bill.class)))
                .thenReturn(mockBill);

        Bill result = billService.saveBill(mockDetailDto);

        assertThat(result.getPolicyArea()).isEqualTo("Healthcare");
        verify(billRepository, times(1)).save(any(Bill.class));
    }

    @Test
    void saveBill_FromDetailDto_WhenExists_ShouldUpdate() {
        when(billRepository.findByCongressAndBillTypeAndBillNumber(119, "hr", "1"))
                .thenReturn(Optional.of(mockBill));
        when(billRepository.save(any(Bill.class)))
                .thenReturn(mockBill);

        Bill result = billService.saveBill(mockDetailDto);

        assertThat(result).isNotNull();
        verify(billRepository, times(1)).save(any(Bill.class));
    }

    @Test
    void saveBill_FromDetailDto_ShouldPersistSponsors() {
        SponsorDto sponsorDto = new SponsorDto(
                "W000790", "Warnock, Raphael G.", "Democratic",
                "Georgia", null, false,
                "https://api.congress.gov/v3/member/W000790"
        );
        BillDetailDto dtoWithSponsors = new BillDetailDto(
                119, "1", "hr", "Test Bill", "House",
                "2024-01-01",
                new LatestActionDto("2024-01-02", "Referred to committee", null),
                List.of(sponsorDto), null,
                new PolicyAreaDto("Healthcare"),
                null, null,
                "https://api.congress.gov/v3/bill/119/hr/1"
        );

        when(billRepository.findByCongressAndBillTypeAndBillNumber(119, "hr", "1"))
                .thenReturn(Optional.empty());
        when(billRepository.save(any(Bill.class)))
                .thenReturn(mockBill);
        when(memberRepository.findById("W000790"))
                .thenReturn(Optional.empty());
        when(memberRepository.save(any(Member.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(sponsorRepository.save(any(Sponsor.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        Bill result = billService.saveBill(dtoWithSponsors);

        assertThat(result).isNotNull();
        verify(memberRepository, times(1)).findById("W000790");
        verify(memberRepository, times(1)).save(any(Member.class));
        verify(sponsorRepository, times(1)).save(any(Sponsor.class));
    }

    @Test
    void saveSummary_ShouldPersistSummaryText() {
        when(billRepository.findByCongressAndBillTypeAndBillNumber(119, "hr", "1"))
                .thenReturn(Optional.of(mockBill));

        billService.saveSummary(119, "hr", "1", "Summary text");

        assertThat(mockBill.getSummary()).isEqualTo("Summary text");
        assertThat(mockBill.getSummaryUpdatedAt()).isNotNull();
        verify(billRepository, times(1)).save(mockBill);
    }

    @Test
    void saveSummary_WhenBillNotFound_ShouldNotThrow() {
        when(billRepository.findByCongressAndBillTypeAndBillNumber(119, "hr", "1"))
                .thenReturn(Optional.empty());

        billService.saveSummary(119, "hr", "1", "Summary text");

        verify(billRepository, never()).save(any(Bill.class));
    }

    @Test
    void findByCongressAndSummaryIsNull_ShouldReturnPage() {
        Pageable pageable = PageRequest.of(0, 20);
        when(billRepository.findByCongressAndSummaryIsNull(119, pageable))
                .thenReturn(new PageImpl<>(List.of(mockBill), pageable, 1));

        Page<BillCacheDto> result = billService.findByCongressAndSummaryIsNull(119, pageable);

        assertThat(result.getTotalElements()).isEqualTo(1);
    }

    @Test
    void saveBill_WithNullLatestAction_ShouldNotThrow() {
        BillSummaryDto dtoWithNullAction = new BillSummaryDto(
                119, "2", "hr", "Another Bill", "House",
                "2024-01-01", null, List.of(),
                "https://api.congress.gov/v3/bill/119/hr/2"
        );

        when(billRepository.findByCongressAndBillTypeAndBillNumber(119, "hr", "2"))
                .thenReturn(Optional.empty());
        when(billRepository.save(any(Bill.class)))
                .thenReturn(mockBill);

        Bill result = billService.saveBill(dtoWithNullAction);
        assertThat(result).isNotNull();
    }

    @Test
    void saveBill_WithNullPolicyArea_ShouldNotThrow() {
        BillDetailDto dtoWithNullPolicy = new BillDetailDto(
                119, "2", "hr", "Another Bill", "House",
                "2024-01-01", null, List.of(), null, null, null, null,
                "https://api.congress.gov/v3/bill/119/hr/2"
        );

        when(billRepository.findByCongressAndBillTypeAndBillNumber(119, "hr", "2"))
                .thenReturn(Optional.empty());
        when(billRepository.save(any(Bill.class)))
                .thenReturn(mockBill);

        Bill result = billService.saveBill(dtoWithNullPolicy);
        assertThat(result).isNotNull();
    }

    // DB reads
    @Test
    void findByCongress_ShouldReturnBills() {
        when(billRepository.findByCongress(119))
                .thenReturn(List.of(mockBill));

        List<Bill> result = billService.findByCongress(119);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getCongress()).isEqualTo(119);
    }

    @Test
    void findByCongress_WhenNone_ShouldReturnEmpty() {
        when(billRepository.findByCongress(anyInt()))
                .thenReturn(List.of());

        List<Bill> result = billService.findByCongress(999);

        assertThat(result).isEmpty();
    }

    @Test
    void findByCongressPaginated_ShouldReturnPage() {
        Pageable pageable = PageRequest.of(0, 20);
        when(billRepository.findByCongress(119, pageable))
                .thenReturn(new PageImpl<>(List.of(mockBill), pageable, 1));

        Page<BillCacheDto> result = billService.findByCongressPaginated(119, pageable);

        assertThat(result.getTotalElements()).isEqualTo(1);
        assertThat(result.getContent()).hasSize(1);
    }

    @Test
    void findByCongressAndTypeAndNumber_WhenExists_ShouldReturnBill() {
        when(billRepository.findByCongressAndBillTypeAndBillNumber(119, "hr", "1"))
                .thenReturn(Optional.of(mockBill));

        Optional<BillCacheDto> result = billService.findByCongressAndTypeAndNumber(119, "hr", "1");

        assertThat(result).isPresent();
        assertThat(result.get().billNumber()).isEqualTo("1");
    }

    @Test
    void findByCongressAndTypeAndNumber_WhenNotFound_ShouldReturnEmpty() {
        when(billRepository.findByCongressAndBillTypeAndBillNumber(anyInt(), anyString(), anyString()))
                .thenReturn(Optional.empty());

        Optional<BillCacheDto> result = billService.findByCongressAndTypeAndNumber(119, "hr", "9999");

        assertThat(result).isEmpty();
    }

    @Test
    void findByPolicyAreaAndCongress_ShouldReturnPage() {
        Pageable pageable = PageRequest.of(0, 20);
        when(billRepository.findByPolicyAreaAndCongress("Healthcare", 119, pageable))
                .thenReturn(new PageImpl<>(List.of(mockBill), pageable, 1));

        Page<BillCacheDto> result = billService.findByPolicyAreaAndCongress("Healthcare", 119, pageable);

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).policyArea()).isEqualTo("Healthcare");
    }
}