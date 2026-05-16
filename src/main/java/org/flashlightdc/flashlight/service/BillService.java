package org.flashlightdc.flashlight.service;

import jakarta.transaction.Transactional;
import org.flashlightdc.flashlight.client.CongressApiClient;
import org.flashlightdc.flashlight.dto.*;
import org.flashlightdc.flashlight.entity.Bill;
import org.flashlightdc.flashlight.entity.Cosponsor;
import org.flashlightdc.flashlight.entity.Member;
import org.flashlightdc.flashlight.entity.Sponsor;
import org.flashlightdc.flashlight.repository.BillRepository;
import org.flashlightdc.flashlight.repository.CosponsorRepository;
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
    private final CosponsorRepository cosponsorRepository;

    public BillService(CongressApiClient congressApiClient, BillRepository billRepository,
                       SponsorRepository sponsorRepository, MemberRepository memberRepository, CosponsorRepository cosponsorRepository) {
        this.congressApiClient = congressApiClient;
        this.billRepository = billRepository;
        this.sponsorRepository = sponsorRepository;
        this.memberRepository = memberRepository;
        this.cosponsorRepository = cosponsorRepository;
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
            sponsorRepository.deleteByBillId(bill.getId());
            persistSponsors(bill, dto.sponsors());
        }

        if (dto.cosponsors() != null) {
            cosponsorRepository.deleteByBillId(bill.getId());
            persistCosponsors(bill, dto.cosponsors());
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
    private void persistCosponsors(Bill bill, List<CosponsorDto> cosponsors) {
        for (CosponsorDto cosponsorDto : cosponsors) {
            Member member = memberRepository.findById(cosponsorDto.bioguideId())
                    .orElseGet(() -> {
                        Member stub = new Member();
                        stub.setBioguideId(cosponsorDto.bioguideId());
                        stub.setName(cosponsorDto.fullName());
                        stub.setPartyName(cosponsorDto.party());
                        stub.setState(cosponsorDto.state());
                        stub.setDistrict(cosponsorDto.district());
                        stub.setUrl(cosponsorDto.url());
                        stub.setUpdatedAt(LocalDateTime.now());
                        return memberRepository.save(stub);
                    });
            if (cosponsorRepository.findByBill_IdAndMember_BioguideId(bill.getId(), cosponsorDto.bioguideId()).isEmpty()) {
                Cosponsor cosponsor = new Cosponsor();
                cosponsor.setBill(bill);
                cosponsor.setMember(member);
                cosponsorRepository.save(cosponsor);
            }
        }
    }

    @Transactional
    public void saveCosponsors(int congress, String type, String number, List<CosponsorDto> cosponsors) {
        billRepository.findByCongressAndBillTypeAndBillNumber(congress, type, number)
                .ifPresent(bill -> {
                    cosponsorRepository.deleteAll(cosponsorRepository.findByBill_Id(bill.getId()));
                    List<Cosponsor> entities = cosponsors.stream()
                            .map(c -> {
                                Cosponsor cosponsor = new Cosponsor();
                                cosponsor.setBill(bill);
                                cosponsor.setSponsoredDate(c.sponsorshipDate());
                                cosponsor.setOriginal(c.isOriginalCosponsor());
                                memberRepository.findById(c.bioguideId()).ifPresent(cosponsor::setMember);
                                return cosponsor;
                            })
                            .filter(c -> c.getMember() != null)
                            .toList();
                    cosponsorRepository.saveAll(entities);
                });
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
