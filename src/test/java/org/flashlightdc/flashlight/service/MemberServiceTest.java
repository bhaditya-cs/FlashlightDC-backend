package org.flashlightdc.flashlight.service;

import org.flashlightdc.flashlight.client.CongressApiClient;
import org.flashlightdc.flashlight.dto.*;
import org.flashlightdc.flashlight.entity.Member;
import org.flashlightdc.flashlight.entity.Term;
import org.flashlightdc.flashlight.repository.MemberRepository;
import org.flashlightdc.flashlight.repository.TermRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MemberServiceTest {

    @Mock
    private CongressApiClient congressApiClient;

    @Mock
    private MemberRepository memberRepository;

    @Mock
    private TermRepository termRepository;

    @InjectMocks
    private MemberService memberService;

    private MemberListResponse mockListResponse;
    private MemberDto mockMemberDto;
    private Member mockMember;
    private List<TermDto> mockTerms;

    @BeforeEach
    void setUp() {
        mockTerms = List.of(
                new TermDto("House of Representatives", 2021, 0),
                new TermDto("Senate", 2023, 0)
        );

        mockMemberDto = new MemberDto(
                "W000790",
                "Warnock, Raphael G.",
                "Democratic",
                "Georgia",
                null,
                mockTerms,
                new DepictionDto("https://congress.gov/img/member/w000790_200.jpg", "Image courtesy of the Member"),
                "https://api.congress.gov/v3/member/W000790"
        );

        mockListResponse = new MemberListResponse(
                List.of(mockMemberDto),
                new PaginationDto(1, null, null)
        );

        mockMember = new Member();
        mockMember.setBioguideId("W000790");
        mockMember.setName("Warnock, Raphael G.");
        mockMember.setPartyName("Democratic");
        mockMember.setState("Georgia");
        mockMember.setUrl("https://api.congress.gov/v3/member/W000790");
    }

    // raw API fetch
    @Test
    void getMembers_ShouldReturnMembers() {
        when(congressApiClient.getMembers(anyInt(), anyInt(), anyInt()))
                .thenReturn(Mono.just(mockListResponse));

        StepVerifier.create(memberService.getMembers(119, 20, 0))
                .expectNext(mockListResponse)
                .verifyComplete();
    }

    @Test
    void getMembers_WhenApiError_ShouldPropagateError() {
        when(congressApiClient.getMembers(anyInt(), anyInt(), anyInt()))
                .thenReturn(Mono.error(new RuntimeException("Congress API unavailable")));

        StepVerifier.create(memberService.getMembers(119, 20, 0))
                .expectErrorMessage("Congress API unavailable")
                .verify();
    }

    // save
    @Test
    void saveMember_WhenNew_ShouldInsert() {
        when(memberRepository.findById("W000790"))
                .thenReturn(Optional.empty());
        when(memberRepository.save(any(Member.class)))
                .thenReturn(mockMember);
        when(termRepository.findByMember_BioguideId("W000790"))
                .thenReturn(List.of());

        Member result = memberService.saveMember(mockMemberDto);

        assertThat(result.getBioguideId()).isEqualTo("W000790");
        assertThat(result.getPartyName()).isEqualTo("Democratic");
        verify(memberRepository, times(1)).save(any(Member.class));
        verify(termRepository, times(1)).saveAll(anyList());
    }

    @Test
    void saveMember_WhenExists_ShouldUpdate() {
        when(memberRepository.findById("W000790"))
                .thenReturn(Optional.of(mockMember));
        when(memberRepository.save(any(Member.class)))
                .thenReturn(mockMember);
        when(termRepository.findByMember_BioguideId("W000790"))
                .thenReturn(List.of());

        Member result = memberService.saveMember(mockMemberDto);

        assertThat(result).isNotNull();
        verify(memberRepository, times(1)).save(any(Member.class));
    }

    @Test
    void saveMember_FromDetailResponse_ShouldDelegateToMemberDto() {
        MemberDetailResponse detailResponse = new MemberDetailResponse();
        detailResponse.member = mockMemberDto;

        when(memberRepository.findById("W000790"))
                .thenReturn(Optional.empty());
        when(memberRepository.save(any(Member.class)))
                .thenReturn(mockMember);
        when(termRepository.findByMember_BioguideId("W000790"))
                .thenReturn(List.of());

        Member result = memberService.saveMember(detailResponse);

        assertThat(result.getBioguideId()).isEqualTo("W000790");
        verify(memberRepository, times(1)).save(any(Member.class));
    }

    @Test
    void saveMember_ShouldSaveTerms() {
        when(memberRepository.findById("W000790"))
                .thenReturn(Optional.empty());
        when(memberRepository.save(any(Member.class)))
                .thenReturn(mockMember);
        when(termRepository.findByMember_BioguideId("W000790"))
                .thenReturn(List.of());

        memberService.saveMember(mockMemberDto);

        verify(termRepository, times(1)).deleteAll(anyList());
        verify(termRepository, times(1)).saveAll(argThat(terms ->
                ((List<Term>) terms).size() == 2
        ));
    }

    @Test
    void saveMember_WithNullDepiction_ShouldNotThrow() {
        MemberDto dtoWithNullDepiction = new MemberDto(
                "W000790", "Warnock, Raphael G.", "Democratic",
                "Georgia", null, mockTerms, null,
                "https://api.congress.gov/v3/member/W000790"
        );

        when(memberRepository.findById("W000790"))
                .thenReturn(Optional.empty());
        when(memberRepository.save(any(Member.class)))
                .thenReturn(mockMember);
        when(termRepository.findByMember_BioguideId("W000790"))
                .thenReturn(List.of());

        Member result = memberService.saveMember(dtoWithNullDepiction);
        assertThat(result).isNotNull();
    }

    @Test
    void saveMember_WithNullTerms_ShouldNotThrow() {
        MemberDto dtoWithNullTerms = new MemberDto(
                "W000790", "Warnock, Raphael G.", "Democratic",
                "Georgia", null, null, null,
                "https://api.congress.gov/v3/member/W000790"
        );

        when(memberRepository.findById("W000790"))
                .thenReturn(Optional.empty());
        when(memberRepository.save(any(Member.class)))
                .thenReturn(mockMember);

        Member result = memberService.saveMember(dtoWithNullTerms);

        assertThat(result).isNotNull();
        verify(termRepository, never()).saveAll(anyList());
    }

    // DB reads
    @Test
    void findById_WhenExists_ShouldReturnMember() {
        when(memberRepository.findById("W000790"))
                .thenReturn(Optional.of(mockMember));

        Optional<Member> result = memberService.findById("W000790");

        assertThat(result).isPresent();
        assertThat(result.get().getBioguideId()).isEqualTo("W000790");
    }

    @Test
    void findById_WhenNotFound_ShouldReturnEmpty() {
        when(memberRepository.findById(anyString()))
                .thenReturn(Optional.empty());

        Optional<Member> result = memberService.findById("UNKNOWN");

        assertThat(result).isEmpty();
    }

    @Test
    void findByParty_ShouldReturnMembers() {
        when(memberRepository.findByPartyName("Democratic"))
                .thenReturn(List.of(mockMember));

        List<Member> result = memberService.findByParty("Democratic");

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getPartyName()).isEqualTo("Democratic");
    }

    @Test
    void findByParty_WhenNone_ShouldReturnEmpty() {
        when(memberRepository.findByPartyName(anyString()))
                .thenReturn(List.of());

        List<Member> result = memberService.findByParty("Independent");

        assertThat(result).isEmpty();
    }

    @Test
    void findByState_ShouldReturnMembers() {
        when(memberRepository.findByState("Georgia"))
                .thenReturn(List.of(mockMember));

        List<Member> result = memberService.findByState("Georgia");

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getState()).isEqualTo("Georgia");
    }

    @Test
    void findByPartyAndState_ShouldReturnMembers() {
        when(memberRepository.findByPartyNameAndState("Democratic", "Georgia"))
                .thenReturn(List.of(mockMember));

        List<Member> result = memberService.findByPartyAndState("Democratic", "Georgia");

        assertThat(result).hasSize(1);
    }

    @Test
    void findAll_ShouldReturnAllMembers() {
        when(memberRepository.findAll())
                .thenReturn(List.of(mockMember));

        List<Member> result = memberService.findAll();

        assertThat(result).hasSize(1);
    }
}