package ar.edu.utn.frc.tup.piii.controllers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import static org.hamcrest.Matchers.hasSize;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.server.ResponseStatusException;

import ar.edu.utn.frc.tup.piii.dtos.engine.GameActionType;
import ar.edu.utn.frc.tup.piii.dtos.judge.JudgeQueryCategory;
import ar.edu.utn.frc.tup.piii.dtos.judge.JudgeQueryDTO;
import ar.edu.utn.frc.tup.piii.dtos.judge.JudgeResponseDTO;
import ar.edu.utn.frc.tup.piii.models.Player;
import ar.edu.utn.frc.tup.piii.services.JudgeService;
import ar.edu.utn.frc.tup.piii.services.PlayerService;
import jakarta.servlet.http.Cookie;

/**
 * Slice tests for {@link JudgeController}. The {@link JudgeService} and
 * authentication layers are mocked so the controller's request mapping, query
 * DTO assembly, status codes and response body are verified in isolation, with
 * {@code GlobalExceptionHandler} mapping {@link ResponseStatusException} to
 * HTTP statuses.
 */
@SpringBootTest
class JudgeControllerTest {

    private static final String SESSION_ID = "session-123";
    private static final String HOST_ID = "p1";
    private static final String MATCH_ID = "match-1";

    @Autowired
    private WebApplicationContext webApplicationContext;

    @MockitoBean
    private JudgeService judgeService;

    @MockitoBean
    private PlayerService playerService;

    private MockMvc mockMvc;
    private Player host;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();
        host = new Player();
        host.setId(HOST_ID);
        when(playerService.getPlayerBySession(SESSION_ID)).thenReturn(Optional.of(host));
        when(playerService.getPlayerBySession(eq("bad-session"))).thenReturn(Optional.empty());
    }

    private Cookie sessionCookie() {
        return new Cookie("sessionId", SESSION_ID);
    }

    private JudgeResponseDTO sampleResponse() {
        return JudgeResponseDTO.builder()
                .queryId("q-1")
                .category(JudgeQueryCategory.VICTORY_CONDITIONS)
                .summary("You still need 6 prizes")
                .reasons(List.of("No prizes taken yet", "Opponent has 6 Pokemon"))
                .ruleReferences(List.of("RF-05"))
                .suggestion("Knock out the active Pokemon")
                .hasActiveRestrictions(true)
                .turnNumber(3)
                .lang("es")
                .build();
    }

    // ---- query: happy path ----

    @Test
    void query_Authenticated_Returns200WithBody() throws Exception {
        when(judgeService.resolve(any(JudgeQueryDTO.class))).thenReturn(sampleResponse());

        mockMvc.perform(get("/api/matches/{matchId}/judge/query", MATCH_ID)
                        .cookie(sessionCookie())
                        .param("category", "VICTORY_CONDITIONS"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.queryId").value("q-1"))
                .andExpect(jsonPath("$.category").value("VICTORY_CONDITIONS"))
                .andExpect(jsonPath("$.summary").value("You still need 6 prizes"))
                .andExpect(jsonPath("$.reasons", hasSize(2)))
                .andExpect(jsonPath("$.ruleReferences", hasSize(1)))
                .andExpect(jsonPath("$.suggestion").value("Knock out the active Pokemon"))
                .andExpect(jsonPath("$.hasActiveRestrictions").value(true))
                .andExpect(jsonPath("$.turnNumber").value(3))
                .andExpect(jsonPath("$.lang").value("es"));

        // The controller must map authenticated player + path/required params, defaulting lang to "es".
        ArgumentCaptor<JudgeQueryDTO> captor = ArgumentCaptor.forClass(JudgeQueryDTO.class);
        verify(judgeService).resolve(captor.capture());
        JudgeQueryDTO sent = captor.getValue();
        assertEquals(MATCH_ID, sent.getMatchId());
        assertEquals(HOST_ID, sent.getPlayerId());
        assertEquals(JudgeQueryCategory.VICTORY_CONDITIONS, sent.getCategory());
        assertEquals("es", sent.getLang());
    }

    @Test
    void query_AllOptionalParams_ForwardedToService() throws Exception {
        when(judgeService.resolve(any(JudgeQueryDTO.class))).thenReturn(sampleResponse());

        mockMvc.perform(get("/api/matches/{matchId}/judge/query", MATCH_ID)
                        .cookie(sessionCookie())
                        .param("category", "WHY_CANT_I")
                        .param("targetAction", "ATTACK")
                        .param("targetCardId", "xy1-1")
                        .param("targetPokemonId", "poke-9")
                        .param("targetTurnNumber", "7")
                        .param("lang", "en"))
                .andExpect(status().isOk());

        ArgumentCaptor<JudgeQueryDTO> captor = ArgumentCaptor.forClass(JudgeQueryDTO.class);
        verify(judgeService).resolve(captor.capture());
        JudgeQueryDTO sent = captor.getValue();
        assertEquals(MATCH_ID, sent.getMatchId());
        assertEquals(HOST_ID, sent.getPlayerId());
        assertEquals(JudgeQueryCategory.WHY_CANT_I, sent.getCategory());
        assertEquals(GameActionType.ATTACK, sent.getTargetAction());
        assertEquals("xy1-1", sent.getTargetCardId());
        assertEquals("poke-9", sent.getTargetPokemonId());
        assertEquals(Integer.valueOf(7), sent.getTargetTurnNumber());
        assertEquals("en", sent.getLang());
    }

    // ---- query: authentication branches ----

    @Test
    void query_InvalidSession_Returns401() throws Exception {
        mockMvc.perform(get("/api/matches/{matchId}/judge/query", MATCH_ID)
                        .cookie(new Cookie("sessionId", "bad-session"))
                        .param("category", "VICTORY_CONDITIONS"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void query_MissingCookie_Returns4xx() throws Exception {
        mockMvc.perform(get("/api/matches/{matchId}/judge/query", MATCH_ID)
                        .param("category", "VICTORY_CONDITIONS"))
                .andExpect(status().is4xxClientError());
    }

    // ---- query: service-side error branches ----

    @Test
    void query_UnsupportedCategory_Returns400() throws Exception {
        when(judgeService.resolve(any(JudgeQueryDTO.class)))
                .thenThrow(new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Unsupported query category"));

        mockMvc.perform(get("/api/matches/{matchId}/judge/query", MATCH_ID)
                        .cookie(sessionCookie())
                        .param("category", "EXPLAIN_DAMAGE"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void query_PlayerNotMember_Returns403() throws Exception {
        when(judgeService.resolve(any(JudgeQueryDTO.class)))
                .thenThrow(new ResponseStatusException(HttpStatus.FORBIDDEN,
                        "Player does not belong to this match"));

        mockMvc.perform(get("/api/matches/{matchId}/judge/query", MATCH_ID)
                        .cookie(sessionCookie())
                        .param("category", "VICTORY_CONDITIONS"))
                .andExpect(status().isForbidden());
    }

    @Test
    void query_MatchNotFound_Returns404() throws Exception {
        when(judgeService.resolve(any(JudgeQueryDTO.class)))
                .thenThrow(new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Match not found: " + MATCH_ID));

        mockMvc.perform(get("/api/matches/{matchId}/judge/query", MATCH_ID)
                        .cookie(sessionCookie())
                        .param("category", "EXPLAIN_STATUS"))
                .andExpect(status().isNotFound());
    }
}
