package ar.edu.utn.frc.tup.piii.controllers;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import ar.edu.utn.frc.tup.piii.dtos.engine.MatchStateDTO;
import ar.edu.utn.frc.tup.piii.dtos.player.PlayerRequest;
import ar.edu.utn.frc.tup.piii.services.MatchService;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * End-to-end integration test that exercises the full match lifecycle:
 * register two players → use starter decks → create match → join →
 * ready up → start → coin toss → setup → play one turn.
 *
 * <p>Runs against the full Spring context with H2 in-memory DB.
 * Cards and starter decks are auto-seeded by {@code DataSeeder}.
 * The lobby lifecycle (create, join, ready, start) uses REST endpoints via
 * MockMvc. The in-game setup actions (coin toss, select active, confirm
 * setup) are WebSocket-only in the controller layer, so this test calls
 * {@link MatchService} directly for those steps. The final turn action
 * goes through the REST {@code POST /actions} endpoint.
 */
@SpringBootTest
class MatchE2ETest {

    @Autowired
    private WebApplicationContext webApplicationContext;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private MatchService matchService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();
    }

    @Test
    void fullMatchLifecycle_CreateDeck_JoinMatch_PlayOneTurn() throws Exception {
        // ── 1. Register two players (each gets 3 starter decks with 60 cards) ──
        PlayerSession hostSession = registerPlayer("host");
        PlayerSession guestSession = registerPlayer("guest");

        // ── 2. Get a valid starter deck for each player ──
        String hostDeckId = getFirstValidDeckId(hostSession);
        String guestDeckId = getFirstValidDeckId(guestSession);

        // ── 3. Host creates a match (REST) ──
        String createMatchBody = objectMapper.writeValueAsString(
                java.util.Map.of(
                        "deckId", hostDeckId,
                        "roomName", "E2E Test Room",
                        "isPrivate", false,
                        "showInList", true,
                        "allowSpectators", false,
                        "isBotMatch", false
                ));

        MvcResult createResult = mockMvc.perform(post("/api/matches")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createMatchBody)
                        .cookie(hostSession.cookie))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.matchId", not(emptyOrNullString())))
                .andExpect(jsonPath("$.status", is("WAITING")))
                .andReturn();

        JsonNode createResponse = objectMapper.readTree(
                createResult.getResponse().getContentAsString());
        String matchId = createResponse.get("matchId").asText();

        // ── 4. Guest joins the match (REST) ──
        String joinBody = objectMapper.writeValueAsString(
                java.util.Map.of("deckId", guestDeckId));

        mockMvc.perform(post("/api/matches/" + matchId + "/join")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(joinBody)
                        .cookie(guestSession.cookie))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("WAITING")));

        // ── 5. Both players set ready (REST) ──
        String readyBody = objectMapper.writeValueAsString(
                java.util.Map.of("ready", true));

        mockMvc.perform(post("/api/matches/" + matchId + "/ready")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(readyBody)
                        .cookie(hostSession.cookie))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/matches/" + matchId + "/ready")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(readyBody)
                        .cookie(guestSession.cookie))
                .andExpect(status().isOk());

        // ── 6. Host starts the match (REST) → transitions to SETUP ──
        mockMvc.perform(post("/api/matches/" + matchId + "/start")
                        .cookie(hostSession.cookie))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("SETUP")));

        // ── 7. Complete the opening flow (coin toss + choose starter) via service ──
        MatchStateDTO state = matchService.getMatchState(matchId, hostSession.playerId);

        // Coin toss: the designated chooser picks a side
        if (state.getOpeningCoinChooserPlayerId() != null) {
            matchService.chooseCoinSide(matchId,
                    state.getOpeningCoinChooserPlayerId(), "HEADS");
            state = matchService.getMatchState(matchId, hostSession.playerId);
        }

        // Choose who goes first: after coin toss (phase = COIN_RESULT), the
        // winner picks who starts
        if (state.getOpeningChooserPlayerId() != null
                && "COIN_RESULT".equals(state.getOpeningPhase())) {
            String chooserId = state.getOpeningChooserPlayerId();
            matchService.chooseFirstTurn(matchId, chooserId, chooserId);
            state = matchService.getMatchState(matchId, hostSession.playerId);
        }

        // ── 8. Setup phase: select active Pokemon and confirm ──
        // After chooseFirstTurn, dealCards runs → hands are populated
        setupPlayerViaService(matchId, hostSession.playerId);
        setupPlayerViaService(matchId, guestSession.playerId);

        // ── 9. Handle mulligan if needed ──
        state = matchService.getMatchState(matchId, hostSession.playerId);
        if (state.isMulliganResolutionPending()) {
            String beneficiaryId = state.getMulliganBeneficiaryPlayerId();
            if (beneficiaryId != null) {
                matchService.resolveMulliganDraw(matchId, beneficiaryId, 0);
            }
        }

        // ── 10. Match should now be in an active game phase ──
        state = matchService.getMatchState(matchId, hostSession.playerId);
        assertNotNull(state.getCurrentPhase(),
                "Match should have a current phase after setup completes");
        assertFalse(state.isFinished(), "Match should not be finished");

        // ── 11. Play one turn via REST: the current player ends their turn ──
        String currentTurnPlayerId = state.getCurrentTurnPlayerId();
        assertNotNull(currentTurnPlayerId, "There should be a current turn player");

        PlayerSession currentPlayer = currentTurnPlayerId.equals(hostSession.playerId)
                ? hostSession : guestSession;

        String endTurnBody = objectMapper.writeValueAsString(
                java.util.Map.of("type", "END_TURN"));

        MvcResult turnResult = mockMvc.perform(post("/api/matches/" + matchId + "/actions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(endTurnBody)
                        .cookie(currentPlayer.cookie))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode afterTurn = objectMapper.readTree(
                turnResult.getResponse().getContentAsString());

        // The turn should have advanced to the other player
        String newTurnPlayerId = afterTurn.get("currentTurnPlayerId").asText();
        assertNotEquals(currentTurnPlayerId, newTurnPlayerId,
                "After END_TURN the turn should pass to the other player");
        assertFalse(afterTurn.get("finished").asBoolean(),
                "Match should still be active after one turn");
    }

    // ─── helpers ────────────────────────────────────────────────────────────

    private PlayerSession registerPlayer(String prefix) throws Exception {
        String unique = prefix + "_" + UUID.randomUUID().toString().substring(0, 8);
        PlayerRequest request = new PlayerRequest();
        request.setUsername(unique);
        request.setEmail(unique + "@e2etest.com");
        request.setPassword("pass123");
        request.setTrainerName("Trainer_" + prefix);

        MvcResult result = mockMvc.perform(post("/api/players/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andReturn();

        String setCookieHeader = result.getResponse().getHeader(HttpHeaders.SET_COOKIE);
        String sessionId = setCookieHeader.split(";")[0].split("=")[1];
        JsonNode body = objectMapper.readTree(result.getResponse().getContentAsString());
        String playerId = body.get("id").asText();

        return new PlayerSession(playerId, sessionId, new Cookie("sessionId", sessionId));
    }

    private String getFirstValidDeckId(PlayerSession session) throws Exception {
        MvcResult result = mockMvc.perform(get("/api/decks")
                        .cookie(session.cookie))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", not(empty())))
                .andReturn();

        JsonNode decks = objectMapper.readTree(result.getResponse().getContentAsString());

        for (JsonNode deck : decks) {
            if (deck.get("cardCount").asInt() == 60) {
                return deck.get("id").asText();
            }
        }

        return decks.get(0).get("id").asText();
    }

    private void setupPlayerViaService(String matchId, String playerId) {
        MatchStateDTO state = matchService.getMatchState(matchId, playerId);
        MatchStateDTO.PlayerBattlefieldDTO self = state.getSelf();

        if (self.isSetupReady()) {
            return;
        }

        // Find the first Basic Pokemon in hand
        String basicCardId = null;
        if (self.getHand() != null) {
            for (MatchStateDTO.CardViewDTO card : self.getHand()) {
                if (card.getSubtypes() != null && card.getSubtypes().contains("Basic")) {
                    basicCardId = card.getId();
                    break;
                }
            }
        }

        assertNotNull(basicCardId,
                "Player " + playerId + " should have at least one Basic Pokemon in hand"
                        + " (hand size: " + (self.getHand() != null ? self.getHand().size() : "null")
                        + ", opening phase: " + state.getOpeningPhase()
                        + ", setup complete: " + state.isSetupComplete() + ")");

        matchService.selectActive(matchId, playerId, basicCardId);
        matchService.confirmSetup(matchId, playerId);
    }

    private record PlayerSession(String playerId, String sessionId, Cookie cookie) {
    }
}
