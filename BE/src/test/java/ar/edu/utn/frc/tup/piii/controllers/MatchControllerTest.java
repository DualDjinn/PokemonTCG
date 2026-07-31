package ar.edu.utn.frc.tup.piii.controllers;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import static org.hamcrest.Matchers.hasSize;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.server.ResponseStatusException;

import com.fasterxml.jackson.databind.ObjectMapper;

import ar.edu.utn.frc.tup.piii.dtos.engine.ChooseCoinSideRequest;
import ar.edu.utn.frc.tup.piii.dtos.engine.ChooseFirstTurnRequest;
import ar.edu.utn.frc.tup.piii.dtos.engine.CreateMatchRequest;
import ar.edu.utn.frc.tup.piii.dtos.engine.CreateMatchResponse;
import ar.edu.utn.frc.tup.piii.dtos.engine.GameAction;
import ar.edu.utn.frc.tup.piii.dtos.engine.JoinByCodeRequest;
import ar.edu.utn.frc.tup.piii.dtos.engine.JoinMatchRequest;
import ar.edu.utn.frc.tup.piii.dtos.engine.MatchStateDTO;
import ar.edu.utn.frc.tup.piii.dtos.engine.SetReadyRequest;
import ar.edu.utn.frc.tup.piii.models.Match;
import ar.edu.utn.frc.tup.piii.models.MatchStatus;
import ar.edu.utn.frc.tup.piii.models.Player;
import ar.edu.utn.frc.tup.piii.services.MatchService;
import ar.edu.utn.frc.tup.piii.services.PlayerService;
import jakarta.servlet.http.Cookie;

/**
 * Slice tests for {@link MatchController}. The service and authentication layers
 * are mocked so the controller's request mapping, status codes and response
 * bodies are verified in isolation, with {@code GlobalExceptionHandler} mapping
 * {@link ResponseStatusException} to HTTP statuses.
 */
@SpringBootTest
class MatchControllerTest {

    private static final String SESSION_ID = "session-123";
    private static final String HOST_ID = "host-1";
    private static final String MATCH_ID = "match-1";
    private static final String JOIN_CODE = "ABC123";

    @Autowired
    private WebApplicationContext webApplicationContext;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private MatchService matchService;

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
        when(matchService.toCreateMatchResponse(any())).thenAnswer(inv -> {
            Match m = inv.getArgument(0);
            return responseFromMatch(m);
        });
    }

    private Cookie sessionCookie() {
        return new Cookie("sessionId", SESSION_ID);
    }

    private Match lobby(MatchStatus status) {
        Match match = new Match(MATCH_ID, HOST_ID, "deck-1", JOIN_CODE,
                null, false, false);
        match.setStatus(status);
        return match;
    }

    private CreateMatchResponse responseFromMatch(Match match) {
        CreateMatchResponse r = new CreateMatchResponse();
        r.setMatchId(match.getId());
        r.setJoinCode(match.getJoinCode());
        r.setStatus(match.getStatus());
        return r;
    }

    // ---- createMatch ----

    @Test
    void createMatch_Authenticated_Returns201WithResponseBody() throws Exception {
        CreateMatchRequest request = new CreateMatchRequest();
        request.setDeckId("deck-1");
        when(matchService.createMatch(eq(HOST_ID), eq("deck-1"), isNull(), isNull(),
                eq(false), eq(true), isNull(), eq(false))).thenReturn(lobby(MatchStatus.WAITING));

        mockMvc.perform(post("/api/matches")
                        .cookie(sessionCookie())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.matchId").value(MATCH_ID))
                .andExpect(jsonPath("$.joinCode").value(JOIN_CODE))
                .andExpect(jsonPath("$.status").value("WAITING"));

        verify(matchService).createMatch(eq(HOST_ID), eq("deck-1"), isNull(), isNull(),
                eq(false), eq(true), isNull(), eq(false));
    }

    @Test
    void createMatch_InvalidSession_Returns401() throws Exception {
        CreateMatchRequest request = new CreateMatchRequest();
        request.setDeckId("deck-1");

        mockMvc.perform(post("/api/matches")
                        .cookie(new Cookie("sessionId", "bad-session"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void createMatch_MissingCookie_Returns4xx() throws Exception {
        CreateMatchRequest request = new CreateMatchRequest();
        request.setDeckId("deck-1");

        mockMvc.perform(post("/api/matches")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().is4xxClientError());
    }

    @Test
    void createMatch_DeckNotFound_Returns404() throws Exception {
        CreateMatchRequest request = new CreateMatchRequest();
        request.setDeckId("missing-deck");
        when(matchService.createMatch(eq(HOST_ID), eq("missing-deck"), isNull(), isNull(),
                eq(false), eq(true), isNull(), eq(false)))
                .thenThrow(new ResponseStatusException(HttpStatus.NOT_FOUND, "Deck not found"));

        mockMvc.perform(post("/api/matches")
                        .cookie(sessionCookie())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound());
    }

    // ---- joinMatch ----

    @Test
    void joinMatch_Authenticated_Returns200WithResponseBody() throws Exception {
        JoinMatchRequest request = new JoinMatchRequest();
        request.setDeckId("deck-2");
        when(matchService.joinMatch(eq(MATCH_ID), eq(HOST_ID), eq("deck-2"), isNull()))
                .thenReturn(lobby(MatchStatus.SETUP));

        mockMvc.perform(post("/api/matches/{matchId}/join", MATCH_ID)
                        .cookie(sessionCookie())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.matchId").value(MATCH_ID))
                .andExpect(jsonPath("$.status").value("SETUP"));

        verify(matchService).joinMatch(eq(MATCH_ID), eq(HOST_ID), eq("deck-2"), isNull());
    }

    @Test
    void joinMatch_InvalidSession_Returns401() throws Exception {
        JoinMatchRequest request = new JoinMatchRequest();
        request.setDeckId("deck-2");

        mockMvc.perform(post("/api/matches/{matchId}/join", MATCH_ID)
                        .cookie(new Cookie("sessionId", "bad-session"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void joinMatch_NotWaiting_Returns409() throws Exception {
        JoinMatchRequest request = new JoinMatchRequest();
        request.setDeckId("deck-2");
        when(matchService.joinMatch(eq(MATCH_ID), eq(HOST_ID), anyString(), isNull()))
                .thenThrow(new ResponseStatusException(HttpStatus.CONFLICT, "Match is not open to join"));

        mockMvc.perform(post("/api/matches/{matchId}/join", MATCH_ID)
                        .cookie(sessionCookie())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict());
    }

    @Test
    void joinMatch_InvalidDeck_Returns400() throws Exception {
        JoinMatchRequest request = new JoinMatchRequest();
        request.setDeckId("deck-2");
        when(matchService.joinMatch(eq(MATCH_ID), eq(HOST_ID), anyString(), isNull()))
                .thenThrow(new ResponseStatusException(HttpStatus.BAD_REQUEST, "Submitted deck is not valid"));

        mockMvc.perform(post("/api/matches/{matchId}/join", MATCH_ID)
                        .cookie(sessionCookie())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    // ---- getMatchState ----

    @Test
    void getMatchState_Authenticated_Returns200WithState() throws Exception {
        MatchStateDTO state = new MatchStateDTO();
        state.setMatchId(MATCH_ID);
        state.setRequestedPlayerId(HOST_ID);
        when(matchService.getMatchState(MATCH_ID, HOST_ID)).thenReturn(state);

        mockMvc.perform(get("/api/matches/{matchId}/state", MATCH_ID)
                        .cookie(sessionCookie()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.matchId").value(MATCH_ID))
                .andExpect(jsonPath("$.requestedPlayerId").value(HOST_ID));

        verify(matchService).getMatchState(MATCH_ID, HOST_ID);
    }

    @Test
    void getMatchState_InvalidSession_Returns401() throws Exception {
        mockMvc.perform(get("/api/matches/{matchId}/state", MATCH_ID)
                        .cookie(new Cookie("sessionId", "bad-session")))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void getMatchState_NotMember_Returns403() throws Exception {
        when(matchService.getMatchState(MATCH_ID, HOST_ID))
                .thenThrow(new ResponseStatusException(HttpStatus.FORBIDDEN,
                        "Player does not belong to this match"));

        mockMvc.perform(get("/api/matches/{matchId}/state", MATCH_ID)
                        .cookie(sessionCookie()))
                .andExpect(status().isForbidden());
    }

    @Test
    void getMatchState_NotStarted_Returns404() throws Exception {
        when(matchService.getMatchState(MATCH_ID, HOST_ID))
                .thenThrow(new ResponseStatusException(HttpStatus.NOT_FOUND, "Match has not started yet"));

        mockMvc.perform(get("/api/matches/{matchId}/state", MATCH_ID)
                        .cookie(sessionCookie()))
                .andExpect(status().isNotFound());
    }

    // ---- submitAction ----

    @Test
    void submitAction_Authenticated_DelegatesAndReturnsState() throws Exception {
        GameAction action = GameAction.endTurn();
        MatchStateDTO state = new MatchStateDTO();
        state.setMatchId(MATCH_ID);
        when(matchService.getMatchState(MATCH_ID, HOST_ID)).thenReturn(state);

        mockMvc.perform(post("/api/matches/{matchId}/actions", MATCH_ID)
                        .cookie(sessionCookie())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(action)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.matchId").value(MATCH_ID));

        verify(matchService).submitAction(eq(MATCH_ID), eq(HOST_ID), any(GameAction.class));
        verify(matchService).getMatchState(MATCH_ID, HOST_ID);
    }

    @Test
    void submitAction_InvalidSession_Returns401() throws Exception {
        GameAction action = GameAction.endTurn();

        mockMvc.perform(post("/api/matches/{matchId}/actions", MATCH_ID)
                        .cookie(new Cookie("sessionId", "bad-session"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(action)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void submitAction_IllegalAction_Returns400() throws Exception {
        GameAction action = GameAction.attack("Thunder Shock");
        doThrow(new ResponseStatusException(HttpStatus.BAD_REQUEST, "Illegal action"))
                .when(matchService).submitAction(eq(MATCH_ID), eq(HOST_ID), any(GameAction.class));

        mockMvc.perform(post("/api/matches/{matchId}/actions", MATCH_ID)
                        .cookie(sessionCookie())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(action)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void submitAction_NotMember_Returns403() throws Exception {
        GameAction action = GameAction.endTurn();
        doThrow(new ResponseStatusException(HttpStatus.FORBIDDEN, "Player does not belong to this match"))
                .when(matchService).submitAction(eq(MATCH_ID), eq(HOST_ID), any(GameAction.class));

        mockMvc.perform(post("/api/matches/{matchId}/actions", MATCH_ID)
                        .cookie(sessionCookie())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(action)))
                .andExpect(status().isForbidden());
    }

    // ---- listLobby ----

    @Test
    void listLobby_Returns200WithList() throws Exception {
        when(matchService.listLobby()).thenReturn(List.of(responseFromMatch(lobby(MatchStatus.WAITING))));

        mockMvc.perform(get("/api/matches/lobby"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)));

        verify(matchService).listLobby();
    }

    // ---- joinByCode ----

    @Test
    void joinByCode_Authenticated_Returns200() throws Exception {
        JoinByCodeRequest request = new JoinByCodeRequest();
        request.setCode(JOIN_CODE);
        request.setDeckId("deck-2");
        when(matchService.joinByCode(eq(JOIN_CODE), eq(HOST_ID), eq("deck-2"), isNull()))
                .thenReturn(responseFromMatch(lobby(MatchStatus.WAITING)));

        mockMvc.perform(post("/api/matches/join-by-code")
                        .cookie(sessionCookie())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.matchId").value(MATCH_ID));

        verify(matchService).joinByCode(eq(JOIN_CODE), eq(HOST_ID), eq("deck-2"), isNull());
    }

    @Test
    void joinByCode_InvalidSession_Returns401() throws Exception {
        JoinByCodeRequest request = new JoinByCodeRequest();
        request.setCode(JOIN_CODE);
        request.setDeckId("deck-2");

        mockMvc.perform(post("/api/matches/join-by-code")
                        .cookie(new Cookie("sessionId", "bad-session"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());
    }

    // ---- getLobbyState ----

    @Test
    void getLobbyState_Authenticated_Returns200() throws Exception {
        when(matchService.getLobbyState(MATCH_ID, HOST_ID))
                .thenReturn(responseFromMatch(lobby(MatchStatus.WAITING)));

        mockMvc.perform(get("/api/matches/{matchId}/lobby-state", MATCH_ID)
                        .cookie(sessionCookie()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.matchId").value(MATCH_ID));

        verify(matchService).getLobbyState(MATCH_ID, HOST_ID);
    }

    @Test
    void getLobbyState_InvalidSession_Returns401() throws Exception {
        mockMvc.perform(get("/api/matches/{matchId}/lobby-state", MATCH_ID)
                        .cookie(new Cookie("sessionId", "bad-session")))
                .andExpect(status().isUnauthorized());
    }

    // ---- setReady ----

    @Test
    void setReady_Authenticated_Returns200() throws Exception {
        SetReadyRequest request = new SetReadyRequest();
        request.setReady(true);
        when(matchService.setReady(MATCH_ID, HOST_ID, true))
                .thenReturn(responseFromMatch(lobby(MatchStatus.WAITING)));

        mockMvc.perform(post("/api/matches/{matchId}/ready", MATCH_ID)
                        .cookie(sessionCookie())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.matchId").value(MATCH_ID));

        verify(matchService).setReady(MATCH_ID, HOST_ID, true);
    }

    @Test
    void setReady_InvalidSession_Returns401() throws Exception {
        SetReadyRequest request = new SetReadyRequest();
        request.setReady(true);

        mockMvc.perform(post("/api/matches/{matchId}/ready", MATCH_ID)
                        .cookie(new Cookie("sessionId", "bad-session"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());
    }

    // ---- startMatch ----

    @Test
    void startMatch_Authenticated_Returns200() throws Exception {
        when(matchService.startMatch(MATCH_ID, HOST_ID))
                .thenReturn(responseFromMatch(lobby(MatchStatus.SETUP)));

        mockMvc.perform(post("/api/matches/{matchId}/start", MATCH_ID)
                        .cookie(sessionCookie()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.matchId").value(MATCH_ID));

        verify(matchService).startMatch(MATCH_ID, HOST_ID);
    }

    @Test
    void startMatch_InvalidSession_Returns401() throws Exception {
        mockMvc.perform(post("/api/matches/{matchId}/start", MATCH_ID)
                        .cookie(new Cookie("sessionId", "bad-session")))
                .andExpect(status().isUnauthorized());
    }

    // ---- cancelMatch ----

    @Test
    void cancelMatch_Authenticated_Returns204() throws Exception {
        mockMvc.perform(post("/api/matches/{matchId}/cancel", MATCH_ID)
                        .cookie(sessionCookie()))
                .andExpect(status().isNoContent());

        verify(matchService).cancelMatch(MATCH_ID, HOST_ID);
    }

    @Test
    void cancelMatch_InvalidSession_Returns401() throws Exception {
        mockMvc.perform(post("/api/matches/{matchId}/cancel", MATCH_ID)
                        .cookie(new Cookie("sessionId", "bad-session")))
                .andExpect(status().isUnauthorized());
    }

    // ---- opening flow: coin side & first turn (previously uncovered) ----

    @Test
    void chooseCoinSide_Authenticated_Returns200WithState() throws Exception {
        ChooseCoinSideRequest req = new ChooseCoinSideRequest();
        req.setCoinSide("HEADS");
        MatchStateDTO state = new MatchStateDTO();
        state.setMatchId(MATCH_ID);
        when(matchService.chooseCoinSide(MATCH_ID, HOST_ID, "HEADS")).thenReturn(state);

        mockMvc.perform(post("/api/matches/{matchId}/opening/coin-side", MATCH_ID)
                        .cookie(sessionCookie())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.matchId").value(MATCH_ID));

        verify(matchService).chooseCoinSide(MATCH_ID, HOST_ID, "HEADS");
    }

    @Test
    void chooseCoinSide_InvalidSession_Returns401() throws Exception {
        ChooseCoinSideRequest req = new ChooseCoinSideRequest();
        req.setCoinSide("HEADS");

        mockMvc.perform(post("/api/matches/{matchId}/opening/coin-side", MATCH_ID)
                        .cookie(new Cookie("sessionId", "bad-session"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void chooseCoinSide_ServiceRejectsSide_Returns400() throws Exception {
        ChooseCoinSideRequest req = new ChooseCoinSideRequest();
        req.setCoinSide("SIDEWAYS");
        when(matchService.chooseCoinSide(MATCH_ID, HOST_ID, "SIDEWAYS"))
                .thenThrow(new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid coin side"));

        mockMvc.perform(post("/api/matches/{matchId}/opening/coin-side", MATCH_ID)
                        .cookie(sessionCookie())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void chooseFirstTurn_Authenticated_Returns200WithState() throws Exception {
        ChooseFirstTurnRequest req = new ChooseFirstTurnRequest();
        req.setStarterPlayerId(HOST_ID);
        MatchStateDTO state = new MatchStateDTO();
        state.setMatchId(MATCH_ID);
        when(matchService.chooseFirstTurn(MATCH_ID, HOST_ID, HOST_ID)).thenReturn(state);

        mockMvc.perform(post("/api/matches/{matchId}/opening/starter", MATCH_ID)
                        .cookie(sessionCookie())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.matchId").value(MATCH_ID));

        verify(matchService).chooseFirstTurn(MATCH_ID, HOST_ID, HOST_ID);
    }

    @Test
    void chooseFirstTurn_InvalidSession_Returns401() throws Exception {
        ChooseFirstTurnRequest req = new ChooseFirstTurnRequest();
        req.setStarterPlayerId(HOST_ID);

        mockMvc.perform(post("/api/matches/{matchId}/opening/starter", MATCH_ID)
                        .cookie(new Cookie("sessionId", "bad-session"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void chooseFirstTurn_LoserPicksWinnerOnly_Returns403() throws Exception {
        ChooseFirstTurnRequest req = new ChooseFirstTurnRequest();
        req.setStarterPlayerId(HOST_ID);
        when(matchService.chooseFirstTurn(MATCH_ID, HOST_ID, HOST_ID))
                .thenThrow(new ResponseStatusException(HttpStatus.FORBIDDEN,
                        "Only the toss winner may choose the starter"));

        mockMvc.perform(post("/api/matches/{matchId}/opening/starter", MATCH_ID)
                        .cookie(sessionCookie())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isForbidden());
    }

    @Test
    void coinTossFlat_DelegatesToChooseCoinSide() throws Exception {
        ChooseCoinSideRequest req = new ChooseCoinSideRequest();
        req.setCoinSide("TAILS");
        MatchStateDTO state = new MatchStateDTO();
        state.setMatchId(MATCH_ID);
        when(matchService.chooseCoinSide(MATCH_ID, HOST_ID, "TAILS")).thenReturn(state);

        mockMvc.perform(post("/api/matches/{matchId}/coin-toss", MATCH_ID)
                        .cookie(sessionCookie())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.matchId").value(MATCH_ID));

        // The flat alias must hit the same service method as the canonical route.
        verify(matchService).chooseCoinSide(MATCH_ID, HOST_ID, "TAILS");
    }

    @Test
    void chooseStarterFlat_DelegatesToChooseFirstTurn() throws Exception {
        ChooseFirstTurnRequest req = new ChooseFirstTurnRequest();
        req.setStarterPlayerId(HOST_ID);
        MatchStateDTO state = new MatchStateDTO();
        state.setMatchId(MATCH_ID);
        when(matchService.chooseFirstTurn(MATCH_ID, HOST_ID, HOST_ID)).thenReturn(state);

        mockMvc.perform(post("/api/matches/{matchId}/choose-starter", MATCH_ID)
                        .cookie(sessionCookie())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.matchId").value(MATCH_ID));

        verify(matchService).chooseFirstTurn(MATCH_ID, HOST_ID, HOST_ID);
    }

    // ---- createMatch flag combinations (previously only defaults were tested) ----

    @Test
    void createMatch_PrivateWithPassword_ForwardsAllFlags() throws Exception {
        CreateMatchRequest request = new CreateMatchRequest();
        request.setDeckId("deck-1");
        request.setRoomName("Secret Lab");
        request.setDescription("Friends only");
        request.setPrivate(true);
        request.setShowInList(false);
        request.setPassword("hunter2");
        request.setAllowSpectators(true);

        when(matchService.createMatch(
                eq(HOST_ID), eq("deck-1"),
                eq("Secret Lab"), eq("Friends only"),
                eq(true), eq(false), eq("hunter2"), eq(true)))
                .thenReturn(lobby(MatchStatus.WAITING));

        mockMvc.perform(post("/api/matches")
                        .cookie(sessionCookie())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());

        verify(matchService).createMatch(
                eq(HOST_ID), eq("deck-1"),
                eq("Secret Lab"), eq("Friends only"),
                eq(true), eq(false), eq("hunter2"), eq(true));
    }

    // ---- listLobby edge case ----

    @Test
    void listLobby_NoOpenMatches_ReturnsEmptyArray() throws Exception {
        when(matchService.listLobby()).thenReturn(List.of());

        mockMvc.perform(get("/api/matches/lobby"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
    }

    // ---- submitAction additional edge case ----

    @Test
    void submitAction_MatchNotFound_Returns404() throws Exception {
        GameAction action = GameAction.endTurn();
        doThrow(new ResponseStatusException(HttpStatus.NOT_FOUND, "Match not found"))
                .when(matchService).submitAction(eq(MATCH_ID), eq(HOST_ID), any(GameAction.class));

        mockMvc.perform(post("/api/matches/{matchId}/actions", MATCH_ID)
                        .cookie(sessionCookie())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(action)))
                .andExpect(status().isNotFound());
    }

    // ---- createMatch: bot branch ----

    @Test
    void createMatch_BotMatch_DelegatesToCreateBotMatch() throws Exception {
        CreateMatchRequest request = new CreateMatchRequest();
        request.setDeckId("deck-1");
        request.setBotMatch(true);
        request.setBotDifficulty("EASY");
        when(matchService.createBotMatch(HOST_ID, "deck-1", "EASY"))
                .thenReturn(responseFromMatch(lobby(MatchStatus.SETUP)));

        mockMvc.perform(post("/api/matches")
                        .cookie(sessionCookie())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.matchId").value(MATCH_ID));

        verify(matchService).createBotMatch(HOST_ID, "deck-1", "EASY");
    }

    // ---- reconnect ----

    @Test
    void reconnect_Authenticated_Returns200WithState() throws Exception {
        MatchStateDTO state = new MatchStateDTO();
        state.setMatchId(MATCH_ID);
        when(matchService.reconnectPlayer(MATCH_ID, HOST_ID)).thenReturn(state);

        mockMvc.perform(get("/api/matches/{matchId}/reconnect", MATCH_ID)
                        .cookie(sessionCookie()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.matchId").value(MATCH_ID));

        verify(matchService).reconnectPlayer(MATCH_ID, HOST_ID);
    }

    @Test
    void reconnect_InvalidSession_Returns401() throws Exception {
        mockMvc.perform(get("/api/matches/{matchId}/reconnect", MATCH_ID)
                        .cookie(new Cookie("sessionId", "bad-session")))
                .andExpect(status().isUnauthorized());
    }

    // ---- getPendingMatches ----

    @Test
    void getPendingMatches_Authenticated_Returns200WithList() throws Exception {
        when(matchService.getPendingMatches(HOST_ID))
                .thenReturn(List.of(responseFromMatch(lobby(MatchStatus.ACTIVE))));

        mockMvc.perform(get("/api/matches/pending-matches")
                        .cookie(sessionCookie()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)));

        verify(matchService).getPendingMatches(HOST_ID);
    }

    @Test
    void getPendingMatches_InvalidSession_Returns401() throws Exception {
        mockMvc.perform(get("/api/matches/pending-matches")
                        .cookie(new Cookie("sessionId", "bad-session")))
                .andExpect(status().isUnauthorized());
    }

    // ---- countdown ----

    @Test
    void startCountdown_Authenticated_Returns200() throws Exception {
        mockMvc.perform(post("/api/matches/{matchId}/start-countdown", MATCH_ID)
                        .cookie(sessionCookie())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"phase\":\"SETUP\",\"totalSeconds\":30}"))
                .andExpect(status().isOk());

        verify(matchService).startCountdown(MATCH_ID, HOST_ID, "SETUP", 30);
    }

    @Test
    void cancelCountdown_Authenticated_Returns200() throws Exception {
        mockMvc.perform(post("/api/matches/{matchId}/cancel-countdown", MATCH_ID)
                        .cookie(sessionCookie()))
                .andExpect(status().isOk());

        verify(matchService).cancelCountdown(MATCH_ID, HOST_ID);
    }

    // ---- surrender ----

    @Test
    void surrender_Authenticated_Returns204() throws Exception {
        mockMvc.perform(post("/api/matches/{matchId}/surrender", MATCH_ID)
                        .cookie(sessionCookie()))
                .andExpect(status().isNoContent());

        verify(matchService).surrender(MATCH_ID, HOST_ID);
    }

    @Test
    void surrender_InvalidSession_Returns401() throws Exception {
        mockMvc.perform(post("/api/matches/{matchId}/surrender", MATCH_ID)
                        .cookie(new Cookie("sessionId", "bad-session")))
                .andExpect(status().isUnauthorized());
    }

    // ---- admin cleanup (no auth) ----

    @Test
    void adminCleanup_Returns200AndRunsBothCleanups() throws Exception {
        mockMvc.perform(post("/api/matches/admin/cleanup"))
                .andExpect(status().isOk());

        verify(matchService).cleanupOrphanRooms();
        verify(matchService).cleanupOrphanBattles();
    }
}
