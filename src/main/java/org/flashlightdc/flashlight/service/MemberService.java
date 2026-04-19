package org.flashlightdc.flashlight.service;

import jakarta.transaction.Transactional;
import org.flashlightdc.flashlight.client.CongressApiClient;
import org.flashlightdc.flashlight.dto.MemberDetailResponse;
import org.flashlightdc.flashlight.dto.MemberDto;
import org.flashlightdc.flashlight.dto.MemberListResponse;
import org.flashlightdc.flashlight.dto.TermDto;
import org.flashlightdc.flashlight.entity.Member;
import org.flashlightdc.flashlight.entity.Term;
import org.flashlightdc.flashlight.repository.MemberRepository;
import org.flashlightdc.flashlight.repository.TermRepository;
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

    public MemberService(CongressApiClient congressApiClient,
                         MemberRepository memberRepository,
                         TermRepository termRepository) {
        this.congressApiClient = congressApiClient;
        this.memberRepository = memberRepository;
        this.termRepository = termRepository;
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

    public List<Member> findAll() {
        return memberRepository.findAll();
    }

    public List<Member> findByPartyAndState(String partyName, String state) {
        return memberRepository.findByPartyNameAndState(partyName, state);
    }
}
