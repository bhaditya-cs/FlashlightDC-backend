package org.flashlightdc.flashlight.service;

import org.flashlightdc.flashlight.util.RestResponsePage;
import org.springframework.transaction.annotation.Transactional;
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
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.hibernate.Hibernate;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class BillService {

    private final CongressApiClient congressApiClient;
    private final BillRepository billRepository;
    private final SponsorRepository sponsorRepository;
    private final MemberRepository memberRepository;
    private final CosponsorRepository cosponsorRepository;

    public BillService(CongressApiClient congressApiClient,
                       BillRepository billRepository,
                       SponsorRepository sponsorRepository,
                       MemberRepository memberRepository,
                       CosponsorRepository cosponsorRepository) {
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
    @Cacheable(value = "bills")
    public RestResponsePage<BillCacheDto> findByCongressPaginated(Integer congress, Pageable pageable) {
        return RestResponsePage.fromPage(billRepository.findByCongress(congress, pageable)
                .map(this::toCacheDto));
    }
    @Cacheable(value = "billsByPolicyArea")
    public RestResponsePage<BillCacheDto> findByPolicyAreaAndCongress(String policyArea, Integer congress, Pageable pageable) {
        return  RestResponsePage.fromPage(billRepository.findByPolicyAreaAndCongress(policyArea, congress, pageable)
                .map(this::toCacheDto));
    }
    @Cacheable(value = "billsAndSummaryIsNull")
    public RestResponsePage<BillCacheDto> findByCongressAndSummaryIsNull(Integer congress, Pageable pageable) {
        return  RestResponsePage.fromPage(billRepository.findByCongressAndSummaryIsNull(congress, pageable)
                .map(this::toCacheDto));
    }

    public List<Bill> findByCongress(Integer congress) {
        return billRepository.findByCongress(congress);
    }

    @Cacheable(value = "bill", key = "#congress + '-' + #type + '-' + #number")
    @Transactional
    public Optional<BillCacheDto> findByCongressAndTypeAndNumber(Integer congress, String type, String number) {
        return billRepository
                .findByCongressAndBillTypeAndBillNumber(congress, type, number)
                .map(this::toCacheDto);
    }

    @CacheEvict(value = {"bills", "bill", "stats"}, allEntries = true)
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
        bill.setLatestActionDate(dto.latestAction() != null ? dto.latestAction().actionDate() : null);
        bill.setLatestActionText(dto.latestAction() != null ? dto.latestAction().text() : null);

        return billRepository.save(bill);
    }

    @CacheEvict(value = {"bills", "bill", "stats"}, allEntries = true)
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
        bill.setLatestActionDate(dto.latestAction() != null ? dto.latestAction().actionDate() : null);
        bill.setLatestActionText(dto.latestAction() != null ? dto.latestAction().text() : null);
        bill.setPolicyArea(dto.policyArea() != null ? dto.policyArea().name() : null);

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

    public BillStatsResponse getStats(Integer congress) {
        long totalBills = billRepository.countByCongress(congress);

        Map<String, Long> chamberBreakdown = billRepository.countByOriginChamberGrouped(congress)
                .stream()
                .collect(Collectors.toMap(
                        row -> (String) row[0],
                        row -> (Long) row[1]
                ));

        YearMonth currentMonth = YearMonth.now();
        String monthStart = currentMonth.atDay(1).toString();
        String monthEnd = currentMonth.plusMonths(1).atDay(1).toString();
        long billsThisMonth = billRepository.countByCongressAndIntroducedDateBetween(congress, monthStart, monthEnd);

        String threeMonthsAgo = currentMonth.minusMonths(3).atDay(1).toString();
        long recentAmendments = billRepository.countByCongressAndLatestActionDateSince(congress, threeMonthsAgo);

        List<BillStatsResponse.PopularTopic> popularTopics = billRepository.countByPolicyAreaGrouped(congress)
                .stream()
                .map(row -> new BillStatsResponse.PopularTopic((String) row[0], (Long) row[1]))
                .toList();

        return new BillStatsResponse(totalBills, billsThisMonth, recentAmendments, chamberBreakdown, popularTopics);
    }

    public BillDetailDto toDetailDto(Bill bill) {
        List<SponsorDto> sponsors = bill.getSponsors().stream()
                .map(s -> new SponsorDto(
                        s.getMember().getBioguideId(),
                        s.getMember().getName(),
                        s.getMember().getPartyName(),
                        s.getMember().getState(),
                        s.getMember().getDistrict(),
                        s.isByRequest(),
                        s.getMember().getUrl()
                ))
                .toList();

        List<CosponsorDto> cosponsors = bill.getCosponsors().stream()
                .map(c -> new CosponsorDto(
                        c.getMember().getBioguideId(),
                        c.getMember().getName(),
                        c.getMember().getPartyName(),
                        c.getMember().getState(),
                        c.getMember().getDistrict(),
                        c.getSponsoredDate(),
                        c.isOriginal(),
                        c.getMember().getUrl()
                ))
                .toList();

        return new BillDetailDto(
                bill.getCongress(),
                bill.getBillNumber(),
                bill.getBillType(),
                bill.getTitle(),
                bill.getOriginChamber(),
                bill.getIntroducedDate(),
                bill.getLatestActionDate() != null
                        ? new LatestActionDto(bill.getLatestActionDate(), bill.getLatestActionText(), null)
                        : null,
                sponsors,
                cosponsors,
                bill.getPolicyArea() != null ? new PolicyAreaDto(bill.getPolicyArea()) : null,
                null,
                null,
                bill.getUrl()
        );
    }
    public BillCacheDto toCacheDto(Bill bill) {
        Hibernate.initialize(bill.getSponsors());
        bill.getSponsors().forEach(s -> Hibernate.initialize(s.getMember()));
        Hibernate.initialize(bill.getCosponsors());
        bill.getCosponsors().forEach(c -> Hibernate.initialize(c.getMember()));
        return new BillCacheDto(
                bill.getId(),
                bill.getCongress(),
                bill.getBillNumber(),
                bill.getBillType(),
                bill.getTitle(),
                bill.getOriginChamber(),
                bill.getIntroducedDate(),
                bill.getLatestActionDate(),
                bill.getLatestActionText(),
                bill.getPolicyArea(),
                bill.getUrl(),
                bill.getSummary(),
                bill.getSummaryUpdatedAt(),
                bill.getUpdatedAt(),
                bill.getSponsors().stream()
                        .map(s -> new SponsorshipCacheDto(s.getId(), s.isByRequest(), s.getMemberName(), s.getMemberState(), s.getMemberParty(), s.getMemberBioguideId()))
                        .toList(),
                bill.getCosponsors().stream()
                        .map(c -> new CosponsorshipCacheDto(c.getId(), c.isOriginal(), c.getSponsoredDate(), c.getMemberName(), c.getMemberState(), c.getMemberParty(), c.getMemberBioguideId()))
                        .toList()
        );
    }
}