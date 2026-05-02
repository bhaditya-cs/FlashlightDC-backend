package org.flashlightdc.flashlight.service;

import jakarta.transaction.Transactional;
import org.flashlightdc.flashlight.client.CongressApiClient;
import org.flashlightdc.flashlight.dto.BillDetailDto;
import org.flashlightdc.flashlight.dto.BillDetailResponse;
import org.flashlightdc.flashlight.dto.BillListResponse;
import org.flashlightdc.flashlight.dto.BillSummaryDto;
import org.flashlightdc.flashlight.dto.SponsorDto;
import org.flashlightdc.flashlight.entity.Bill;
import org.flashlightdc.flashlight.entity.Member;
import org.flashlightdc.flashlight.entity.Sponsor;
import org.flashlightdc.flashlight.repository.BillRepository;
import org.flashlightdc.flashlight.repository.MemberRepository;
import org.flashlightdc.flashlight.repository.SponsorRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class BillService {

    private final CongressApiClient congressApiClient;
    private final BillRepository billRepository;
    private final SponsorRepository sponsorRepository;
    private final MemberRepository memberRepository;

    public BillService(CongressApiClient congressApiClient, BillRepository billRepository,
                       SponsorRepository sponsorRepository, MemberRepository memberRepository) {
        this.congressApiClient = congressApiClient;
        this.billRepository = billRepository;
        this.sponsorRepository = sponsorRepository;
        this.memberRepository = memberRepository;
    }

    public Mono<BillListResponse> getBills(int congress, int limit, int offset) {
        return congressApiClient.getBills(congress, limit, offset);
    }

    public Mono<BillDetailResponse> getBill(int congress, String type, int number) {
        return congressApiClient.getBill(congress, type, number);
    }

    @Transactional
    public Bill saveBill(BillSummaryDto dto) {

        Bill bill = billRepository
                .findByCongressAndBillTypeAndBillNumber(dto.congress(), dto.type(), dto.number())
                .orElse(new Bill());

        bill.setCongress(dto.congress());
        bill.setBillNumber(dto.number());
        bill.setBillType(dto.type());
        bill.setTitle(dto.title());
        bill.setOriginChamber(dto.originChamber());
        bill.setIntroducedDate(dto.introducedDate());
        bill.setUpdatedAt(LocalDateTime.now());

        if (dto.latestAction() != null) {
            bill.setLatestActionDate(dto.latestAction().actionDate());
            bill.setLatestActionText(dto.latestAction().text());
        }

        return billRepository.save(bill);
    }

    @Transactional
    public Bill saveBill(BillDetailDto dto) {
        Bill bill = billRepository
                .findByCongressAndBillTypeAndBillNumber(dto.congress(), dto.type(), dto.number())
                .orElse(new Bill());

        bill.setCongress(dto.congress());
        bill.setBillNumber(dto.number());
        bill.setBillType(dto.type());
        bill.setTitle(dto.title());
        bill.setOriginChamber(dto.originChamber());
        bill.setIntroducedDate(dto.introducedDate());
        bill.setUrl(dto.url());
        bill.setUpdatedAt(LocalDateTime.now());

        if (dto.latestAction() != null) {
            bill.setLatestActionDate(dto.latestAction().actionDate());
            bill.setLatestActionText(dto.latestAction().text());
        }

        if (dto.policyArea() != null) {
            bill.setPolicyArea(dto.policyArea().name());
        }

        bill = billRepository.save(bill);

        if (dto.sponsors() != null) {
            persistSponsors(bill, dto.sponsors());
        }

        return bill;
    }

    private void persistSponsors(Bill bill, List<SponsorDto> sponsors) {
        for (SponsorDto sponsorDto : sponsors) {
            Member member = memberRepository.findById(sponsorDto.bioguideId())
                    .orElseGet(() -> {
                        Member stub = new Member();
                        stub.setBioguideId(sponsorDto.bioguideId());
                        stub.setName(sponsorDto.fullName());
                        stub.setPartyName(sponsorDto.party());
                        stub.setState(sponsorDto.state());
                        stub.setDistrict(sponsorDto.district());
                        stub.setUrl(sponsorDto.url());
                        stub.setUpdatedAt(LocalDateTime.now());
                        return memberRepository.save(stub);
                    });
            if (sponsorRepository.findByBill_IdAndMember_BioguideId(bill.getId(), sponsorDto.bioguideId()).isEmpty()) {
                Sponsor sponsor = new Sponsor();
                sponsor.setBill(bill);
                sponsor.setMember(member);
                sponsor.setByRequest(sponsorDto.isByRequest());
                sponsorRepository.save(sponsor);
            }
        }
    }

    @Transactional
    public void saveSummary(Integer congress, String type, String number, String summary) {
        billRepository.findByCongressAndBillTypeAndBillNumber(congress, type, number)
                .ifPresent(bill -> {
                    bill.setSummary(summary);
                    bill.setSummaryUpdatedAt(LocalDateTime.now());
                    billRepository.save(bill);
                });
    }

    public Page<Bill> findByCongressAndSummaryIsNull(Integer congress, Pageable pageable) {
        return billRepository.findByCongressAndSummaryIsNull(congress, pageable);
    }

    public List<Bill> findByCongress(Integer congress) {
        return billRepository.findByCongress(congress);
    }

    public Page<Bill> findByCongressPaginated(Integer congress, Pageable pageable) {
        return billRepository.findByCongress(congress, pageable);
    }

    public Optional<Bill> findByCongressAndTypeAndNumber(Integer congress, String type, String number) {
        return billRepository.findByCongressAndBillTypeAndBillNumber(congress, type, number);
    }

    public Page<Bill> findByPolicyAreaAndCongress(String policyArea, Integer congress, Pageable pageable) {
        return billRepository.findByPolicyAreaAndCongress(policyArea, congress, pageable);
    }
}
