package org.flashlightdc.flashlight.controller;

import org.flashlightdc.flashlight.dto.*;
import org.flashlightdc.flashlight.entity.Member;
import org.flashlightdc.flashlight.service.MemberService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Mono;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;

@WebFluxTest(MemberController.class)
@ActiveProfiles("test")
class MemberControllerTest {

    @Autowired
    private WebTestClient webTestClient;

    @MockitoBean
    private MemberService memberService;

    private MemberListResponse mockListResponse;
    private Member mockMember;
    private MemberCacheDto mockMemberCacheDto;
    @BeforeEach
    void setUp() {
        MemberDto mockMemberDto = new MemberDto(
                "W000790",
                "Warnock, Raphael G.",
                "Democratic",
                "Georgia",
                null,
                List.of(new TermDto("Senate", 2021, 0)),
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

        mockMemberCacheDto = new MemberCacheDto(
                mockMember.getBioguideId(),
                mockMember.getName(),
                mockMember.getPartyName(),
                mockMember.getState(),
                "1",
                "1",
                "",
                "",
                Collections.emptyList(),
                Collections.emptyList(),
                Collections.emptyList()
        );
    }

    // raw API endpoints
    @Test
    void getMembersRaw_ShouldReturnOk() {
        when(memberService.getMembers(anyInt(), anyInt(), anyInt()))
                .thenReturn(Mono.just(mockListResponse));

        webTestClient.get()
                .uri("/api/members/raw?congress=119&limit=20&offset=0")
                .exchange()
                .expectStatus().isOk();
    }

    @Test
    void getMembersRaw_DefaultParams_ShouldReturnOk() {
        when(memberService.getMembers(anyInt(), anyInt(), anyInt()))
                .thenReturn(Mono.just(mockListResponse));

        webTestClient.get()
                .uri("/api/members/raw")
                .exchange()
                .expectStatus().isOk();
    }

    @Test
    void getMembersRaw_WhenApiError_ShouldReturnServerError() {
        when(memberService.getMembers(anyInt(), anyInt(), anyInt()))
                .thenReturn(Mono.error(new RuntimeException("Congress API unavailable")));

        webTestClient.get()
                .uri("/api/members/raw")
                .exchange()
                .expectStatus().is5xxServerError();
    }

    // fetch and persist
    @Test
    void fetchAndPersistMembers_ShouldReturnOkWithCount() {
        when(memberService.getMembers(anyInt(), anyInt(), anyInt()))
                .thenReturn(Mono.just(mockListResponse));
        when(memberService.saveMember(any(MemberDto.class)))
                .thenReturn(mockMember);

        webTestClient.post()
                .uri("/api/members/fetch?congress=119&limit=20&offset=0")
                .exchange()
                .expectStatus().isOk()
                .expectBody(String.class)
                .isEqualTo("Persisted 1 members");
    }

    @Test
    void fetchAndPersistMembers_WhenApiError_ShouldReturnServerError() {
        when(memberService.getMembers(anyInt(), anyInt(), anyInt()))
                .thenReturn(Mono.error(new RuntimeException("Congress API unavailable")));

        webTestClient.post()
                .uri("/api/members/fetch")
                .exchange()
                .expectStatus().is5xxServerError();
    }

    @Test
    void fetchAndPersistMembers_WhenEmptyList_ShouldReturnZeroCount() {
        when(memberService.getMembers(anyInt(), anyInt(), anyInt()))
                .thenReturn(Mono.just(new MemberListResponse(
                        List.of(),
                        new PaginationDto(0, null, null)
                )));

        webTestClient.post()
                .uri("/api/members/fetch")
                .exchange()
                .expectStatus().isOk()
                .expectBody(String.class)
                .isEqualTo("Persisted 0 members");
    }

    // DB read endpoints
    @Test
    void getMembers_NoParams_ShouldReturnAll() {
        when(memberService.findAll())
                .thenReturn(List.of(mockMemberCacheDto));

        webTestClient.get()
                .uri("/api/members")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$").isArray()
                .jsonPath("$.length()").isEqualTo(1);
    }

    @Test
    void getMembers_FilterByParty_ShouldReturnFiltered() {
        when(memberService.findByParty("Democratic"))
                .thenReturn(List.of(mockMemberCacheDto));

        webTestClient.get()
                .uri("/api/members?party=Democratic")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$").isArray()
                .jsonPath("$.length()").isEqualTo(1);
    }

    @Test
    void getMembers_FilterByState_ShouldReturnFiltered() {
        when(memberService.findByState("Georgia"))
                .thenReturn(List.of(mockMemberCacheDto));

        webTestClient.get()
                .uri("/api/members?state=Georgia")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$").isArray()
                .jsonPath("$.length()").isEqualTo(1);
    }

    @Test
    void getMembers_FilterByPartyAndState_ShouldReturnFiltered() {
        when(memberService.findByPartyAndState("Democratic", "Georgia"))
                .thenReturn(List.of(mockMemberCacheDto));

        webTestClient.get()
                .uri("/api/members?party=Democratic&state=Georgia")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$").isArray()
                .jsonPath("$.length()").isEqualTo(1);
    }

    @Test
    void getMembers_WhenNoneFound_ShouldReturnEmptyArray() {
        when(memberService.findAll())
                .thenReturn(List.of());

        webTestClient.get()
                .uri("/api/members")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$").isArray()
                .jsonPath("$.length()").isEqualTo(0);
    }

    @Test
    void getMember_WhenExists_ShouldReturnMember() {
        when(memberService.findById("W000790"))
                .thenReturn(Optional.of(mockMemberCacheDto));

        webTestClient.get()
                .uri("/api/members/W000790")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.bioguideId").isEqualTo("W000790")
                .jsonPath("$.partyName").isEqualTo("Democratic");
    }

    @Test
    void getMember_WhenNotFound_ShouldReturn404() {
        when(memberService.findById(anyString()))
                .thenReturn(Optional.empty());

        webTestClient.get()
                .uri("/api/members/UNKNOWN")
                .exchange()
                .expectStatus().isNotFound();
    }

    @Test
    void getMember_WhenStateFilterEmpty_ShouldReturnEmptyArray() {
        when(memberService.findByState("Wyoming"))
                .thenReturn(List.of());

        webTestClient.get()
                .uri("/api/members?state=Wyoming")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.length()").isEqualTo(0);
    }
}