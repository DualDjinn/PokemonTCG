package ar.edu.utn.frc.tup.piii.controllers;

import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import com.fasterxml.jackson.databind.ObjectMapper;

import ar.edu.utn.frc.tup.piii.dtos.slot.SlotSpinRequest;
import ar.edu.utn.frc.tup.piii.dtos.slot.SlotSpinResponse;
import ar.edu.utn.frc.tup.piii.models.Player;
import ar.edu.utn.frc.tup.piii.services.PlayerService;
import ar.edu.utn.frc.tup.piii.services.SlotMachineService;
import jakarta.servlet.http.Cookie;

/**
 * Slice tests for {@link SlotMachineController}. The service and authentication layers are
 * mocked so the controller's mapping, status codes and local error handling are verified
 * in isolation.
 */
@SpringBootTest
class SlotMachineControllerTest {

    private static final String SESSION_ID = "session-123";
    private static final String PLAYER_ID = "p1";

    @Autowired
    private WebApplicationContext webApplicationContext;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private SlotMachineService slotMachineService;

    @MockitoBean
    private PlayerService playerService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();
        Player host = new Player();
        host.setId(PLAYER_ID);
        when(playerService.getPlayerBySession(SESSION_ID)).thenReturn(Optional.of(host));
        when(playerService.getPlayerBySession("bad-session")).thenReturn(Optional.empty());
    }

    private Cookie sessionCookie() {
        return new Cookie("sessionId", SESSION_ID);
    }

    private String body(int bet, int winAmount) throws Exception {
        SlotSpinRequest request = new SlotSpinRequest();
        request.setBet(bet);
        request.setWinAmount(winAmount);
        return objectMapper.writeValueAsString(request);
    }

    @Test
    void spin_Authenticated_Returns200WithRemainingCoins() throws Exception {
        when(slotMachineService.spin(PLAYER_ID, 10, 0)).thenReturn(new SlotSpinResponse(990));

        mockMvc.perform(post("/api/slot-machine/spin")
                        .cookie(sessionCookie())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body(10, 0)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.remainingCoins").value(990));

        verify(slotMachineService).spin(PLAYER_ID, 10, 0);
    }

    @Test
    void spin_InvalidSession_Returns401() throws Exception {
        mockMvc.perform(post("/api/slot-machine/spin")
                        .cookie(new Cookie("sessionId", "bad-session"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body(10, 0)))
                .andExpect(status().isUnauthorized());

        verifyNoInteractions(slotMachineService);
    }

    @Test
    void spin_MissingCookie_Returns4xx() throws Exception {
        mockMvc.perform(post("/api/slot-machine/spin")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body(10, 0)))
                .andExpect(status().is4xxClientError());
    }

    @Test
    void spin_InsufficientCoins_Returns402() throws Exception {
        when(slotMachineService.spin(eq(PLAYER_ID), anyInt(), anyInt()))
                .thenThrow(new IllegalStateException("Insufficient coins"));

        mockMvc.perform(post("/api/slot-machine/spin")
                        .cookie(sessionCookie())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body(50, 0)))
                .andExpect(status().isPaymentRequired())
                .andExpect(jsonPath("$.status").value(402))
                .andExpect(jsonPath("$.message").value("Insufficient coins"));
    }

    @Test
    void spin_IllegalArgument_Returns404() throws Exception {
        when(slotMachineService.spin(eq(PLAYER_ID), anyInt(), anyInt()))
                .thenThrow(new IllegalArgumentException("Player not found"));

        mockMvc.perform(post("/api/slot-machine/spin")
                        .cookie(sessionCookie())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body(10, 0)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.message").value("Player not found"));
    }

    @Test
    void spin_InvalidBet_FailsValidation() throws Exception {
        // bet above @Max(50) is rejected by bean validation before reaching the service.
        mockMvc.perform(post("/api/slot-machine/spin")
                        .cookie(sessionCookie())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body(100, 0)))
                .andExpect(status().is4xxClientError());

        verify(slotMachineService, org.mockito.Mockito.never()).spin(anyString(), anyInt(), anyInt());
    }
}
