package org.flashlightdc.flashlight.controller;

import org.flashlightdc.flashlight.dto.BillListResponse;
import org.flashlightdc.flashlight.dto.PaginationDto;
import org.flashlightdc.flashlight.service.BillService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import reactor.core.publisher.Mono;

import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(BillController.class)
@ActiveProfiles("test")
class BillControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private BillService billService;

    @Test
    void getBills_ShouldReturnOk() throws Exception {
        BillListResponse mockResponse = new BillListResponse(java.util.List.of(), new PaginationDto(0, null, null));
        when(billService.getBills(anyInt(), anyInt(), anyInt()))
                .thenReturn(Mono.just(mockResponse));

        mockMvc.perform(get("/api/bills")
                        .param("congress", "119")
                        .param("limit", "20")
                        .param("offset", "0"))
                .andExpect(status().isOk());
    }
}
