package org.flashlightdc.flashlight.service;

import jakarta.transaction.Transactional;
import org.flashlightdc.flashlight.client.CongressApiClient;
import org.flashlightdc.flashlight.dto.*;
import org.flashlightdc.flashlight.entity.Bill;
import org.flashlightdc.flashlight.entity.Member;
import org.flashlightdc.flashlight.entity.Sponsor;
import org.flashlightdc.flashlight.entity.Term;
import org.flashlightdc.flashlight.repository.MemberRepository;
import org.flashlightdc.flashlight.repository.SponsorRepository;
import org.flashlightdc.flashlight.repository.TermRepository;
import org.hibernate.Hibernate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class MemberService {
    private final CongressApiClient congressApiClient;
    private final MemberRepository memberRepository;
    private final TermRepository termRepository;
    private final BillService billService;
    private final SponsorRepository sponsorRepository;
    public MemberService(CongressApiClient congressApiClient,
                         MemberRepository memberRepository,
                         TermRepository termRepository,
                         BillService billService,
                         SponsorRepository sponsorRepository) {
        this.congressApiClient = congressApiClient;
        this.memberRepository = memberRepository;
        this.termRepository = termRepository;
        this.billService = billService;
        this.sponsorRepository = sponsorRepository;
    }

    public Mono<MemberListResponse> getMembers(int congress, int limit, int offset) {
        return congressApiClient.getMembers(congress, limit, offset);
    }

    public Mono<MemberDetailResponse> getMember(String id) {
        return congressApiClient.getMember(id);
    }

    @Transactional
    public Member saveMember(MemberDto dto) {
        Member member = memberRepository.findById(dto.bioguideId())
                .orElse(new Member());

        member.setBioguideId(dto.bioguideId());
        member.setName(dto.name());
        member.setPartyName(dto.partyName());
        member.setState(dto.state());
        member.setDistrict(dto.district());
        member.setUrl(dto.url());
        member.setUpdatedAt(LocalDateTime.now());

        if (dto.depiction() != null) {
            member.setImageUrl(dto.depiction().imageUrl());
            member.setAttribution(dto.depiction().attribution());
        }

        Member saved = memberRepository.save(member);
        saveTerms(saved, dto.terms());
        return saved;
    }


    public Optional<Member> findById(String bioguideId) {
        return memberRepository.findById(bioguideId);
    }

    public List<Member> findByParty(String partyName) {
        return memberRepository.findByPartyName(partyName);
    }

    public List<Member> findByState(String state) {
        return memberRepository.findByState(state);
    }

    private void saveTerms(Member member, List<TermDto> terms) {
        if (terms == null) return;
        termRepository.deleteAll(termRepository.findByMember_BioguideId(member.getBioguideId()));
        List<Term> termEntities = terms.stream()
                .map(t -> {
                    Term term = new Term();
                    term.setMember(member);
                    term.setChamber(t.chamber());
                    term.setStartYear(t.startYear());
                    term.setEndYear(t.endYear());
                    return term;
                })
                .toList();
        termRepository.saveAll(termEntities);
    }

    public Member saveMember(MemberDetailResponse detail) {
        return saveMember(detail.member);
    }

    public Page<Member> findAllPaginated(Pageable pageable) {
        return memberRepository.findAll(pageable);
    }

    public List<Member> findAll() {
        return memberRepository.findAll();
    }

    public List<Member> findByPartyAndState(String partyName, String state) {
        return memberRepository.findByPartyNameAndState(partyName, state);
    }

    @Transactional
    public List<BillDetailDto> getSponsoredBills(String bioguideId) {
        return sponsorRepository.findByMember_BioguideId(bioguideId).stream()
                .map(sponsor -> {
                    Bill bill = sponsor.getBill();
                    Hibernate.initialize(bill.getSponsors());
                    bill.getSponsors().forEach(s -> Hibernate.initialize(s.getMember()));
                    Hibernate.initialize(bill.getCosponsors());
                    bill.getCosponsors().forEach(c -> Hibernate.initialize(c.getMember()));
                    return mapBillToDto(bill);
                })
                .toList();
    }

    private BillDetailDto mapBillToDto(Bill bill) {
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
}
