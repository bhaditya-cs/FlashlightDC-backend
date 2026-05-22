package org.flashlightdc.flashlight.service;

import org.springframework.transaction.annotation.Transactional;
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
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
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

    @CacheEvict(value = {"member", "members", "memberBills"}, allEntries = true)
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

    @Cacheable(value = "member", key = "#bioguideId")
    @Transactional
    public Optional<MemberCacheDto> findById(String bioguideId) {
        return memberRepository.findById(bioguideId).map(member -> {
            Hibernate.initialize(member.getTerms());
            Hibernate.initialize(member.getSponsorships());
            Hibernate.initialize(member.getCosponsorships());

            return new MemberCacheDto(
                    member.getBioguideId(),
                    member.getName(),
                    member.getPartyName(),
                    member.getState(),
                    member.getDistrict(),
                    member.getImageUrl(),
                    member.getAttribution(),
                    member.getUrl(),
                    member.getTerms().stream()
                            .map(t -> new TermCacheDto(t.getId(), t.getChamber(), t.getStartYear(), t.getEndYear()))
                            .toList(),
                    member.getSponsorships().stream()
                            .map(s -> new SponsorshipCacheDto(s.getId(), s.isByRequest(), s.getMemberName(), s.getMemberState(), s.getMemberParty(), s.getMemberBioguideId()))
                            .toList(),
                    member.getCosponsorships().stream()
                            .map(c -> new CosponsorshipCacheDto(c.getId(), c.isOriginal(), c.getSponsoredDate(), c.getMemberName(), c.getMemberState(), c.getMemberParty(), c.getMemberBioguideId()))
                            .toList()
            );
        });
    }
    @Cacheable(value = "members", key = "#partyName")
    @Transactional
    public List<MemberCacheDto> findByParty(String partyName) {
        return memberRepository.findByPartyName(partyName).stream()
                .map(member -> {
                    Hibernate.initialize(member.getTerms());
                    Hibernate.initialize(member.getSponsorships());
                    Hibernate.initialize(member.getCosponsorships());
                    return new MemberCacheDto(
                            member.getBioguideId(),
                            member.getName(),
                            member.getPartyName(),
                            member.getState(),
                            member.getDistrict(),
                            member.getImageUrl(),
                            member.getAttribution(),
                            member.getUrl(),
                            member.getTerms().stream()
                                    .map(t -> new TermCacheDto(t.getId(), t.getChamber(), t.getStartYear(), t.getEndYear()))
                                    .toList(),
                            member.getSponsorships().stream()
                                    .map(s -> new SponsorshipCacheDto(s.getId(), s.isByRequest(), s.getMemberName(), s.getMemberState(), s.getMemberParty(), s.getMemberBioguideId()))
                                    .toList(),
                            member.getCosponsorships().stream()
                                    .map(c -> new CosponsorshipCacheDto(c.getId(), c.isOriginal(), c.getSponsoredDate(), c.getMemberName(), c.getMemberState(), c.getMemberParty(), c.getMemberBioguideId()))
                                    .toList()

                    );
                })
                .toList();
    }

    @Cacheable(value = "members", key = "#state")
    @Transactional
    public List<MemberCacheDto> findByState(String state) {
        return memberRepository.findByState(state).stream()
                .map(member -> {
                    Hibernate.initialize(member.getTerms());
                    Hibernate.initialize(member.getSponsorships());
                    Hibernate.initialize(member.getCosponsorships());
                    return new MemberCacheDto(
                            member.getBioguideId(),
                            member.getName(),
                            member.getPartyName(),
                            member.getState(),
                            member.getDistrict(),
                            member.getImageUrl(),
                            member.getAttribution(),
                            member.getUrl(),
                            member.getTerms().stream()
                                    .map(t -> new TermCacheDto(t.getId(), t.getChamber(), t.getStartYear(), t.getEndYear()))
                                    .toList(),
                            member.getSponsorships().stream()
                                    .map(s -> new SponsorshipCacheDto(s.getId(), s.isByRequest(), s.getMemberName(), s.getMemberState(), s.getMemberParty(), s.getMemberBioguideId()))
                                    .toList(),
                            member.getCosponsorships().stream()
                                    .map(c -> new CosponsorshipCacheDto(c.getId(), c.isOriginal(), c.getSponsoredDate(), c.getMemberName(), c.getMemberState(), c.getMemberParty(), c.getMemberBioguideId()))
                                    .toList()
                    );
                })
                .toList();
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
    @Cacheable(value = "members")
    @Transactional
    public List<MemberCacheDto> findAll() {
        return memberRepository.findAll().stream()
                .map(member -> {
                    Hibernate.initialize(member.getTerms());
                    Hibernate.initialize(member.getSponsorships());
                    Hibernate.initialize(member.getCosponsorships());
                    return new MemberCacheDto(
                            member.getBioguideId(),
                            member.getName(),
                            member.getPartyName(),
                            member.getState(),
                            member.getDistrict(),
                            member.getImageUrl(),
                            member.getAttribution(),
                            member.getUrl(),
                            member.getTerms().stream()
                                    .map(t -> new TermCacheDto(t.getId(), t.getChamber(), t.getStartYear(), t.getEndYear()))
                                    .toList(),
                            member.getSponsorships().stream()
                                    .map(s -> new SponsorshipCacheDto(s.getId(), s.isByRequest(), s.getMemberName(), s.getMemberState(), s.getMemberParty(), s.getMemberBioguideId()))
                                    .toList(),
                            member.getCosponsorships().stream()
                                    .map(c -> new CosponsorshipCacheDto(c.getId(), c.isOriginal(), c.getSponsoredDate(), c.getMemberName(), c.getMemberState(), c.getMemberParty(), c.getMemberBioguideId()))
                                    .toList()
                    );
                })
                .toList();
    }
    @Cacheable(value = "members", key = "#partyName + '-' + #state")
    @Transactional
    public List<MemberCacheDto> findByPartyAndState(String partyName, String state) {
        return memberRepository.findByPartyNameAndState(partyName, state).stream()
                .map(member -> {
                    Hibernate.initialize(member.getTerms());
                    Hibernate.initialize(member.getSponsorships());
                    Hibernate.initialize(member.getCosponsorships());
                    return new MemberCacheDto(
                            member.getBioguideId(),
                            member.getName(),
                            member.getPartyName(),
                            member.getState(),
                            member.getDistrict(),
                            member.getImageUrl(),
                            member.getAttribution(),
                            member.getUrl(),
                            member.getTerms().stream()
                                    .map(t -> new TermCacheDto(t.getId(), t.getChamber(), t.getStartYear(), t.getEndYear()))
                                    .toList(),
                            member.getSponsorships().stream()
                                    .map(s -> new SponsorshipCacheDto(s.getId(), s.isByRequest(), s.getMemberName(), s.getMemberState(), s.getMemberParty(), s.getMemberBioguideId()))
                                    .toList(),
                            member.getCosponsorships().stream()
                                    .map(c -> new CosponsorshipCacheDto(c.getId(), c.isOriginal(), c.getSponsoredDate(), c.getMemberName(), c.getMemberState(), c.getMemberParty(), c.getMemberBioguideId()))
                                    .toList()
                    );
                })
                .toList();
    }

    @Transactional
    public Page<Bill> getSponsoredBills(String bioguideId, Pageable pageable) {
        Page<Sponsor> sponsors = sponsorRepository.findByMember_BioguideId(bioguideId, pageable);

        List<Bill> bills = sponsors.getContent().stream()
                .map(sponsor -> {
                    Bill bill = sponsor.getBill();
                    Hibernate.initialize(bill.getSponsors());
                    bill.getSponsors().forEach(s -> Hibernate.initialize(s.getMember()));
                    Hibernate.initialize(bill.getCosponsors());
                    bill.getCosponsors().forEach(c -> Hibernate.initialize(c.getMember()));
                    return bill;
                })
                .toList();

        return new PageImpl<>(bills, pageable, sponsors.getTotalElements());
    }

    public SponsorCountDto getSponsorCount(String bioguideId) {
        Member member = memberRepository.findById(bioguideId).orElse(null);
        if (member == null) {
            return new SponsorCountDto(0, 0);
        }
        return new SponsorCountDto(member.getSponsorships().size(), member.getCosponsorships().size());
    }
}