package ar.edu.utn.frc.tup.piii.controllers;

import ar.edu.utn.frc.tup.piii.configs.SessionHandshakeInterceptor;
import ar.edu.utn.frc.tup.piii.services.MatchService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

/**
 * Unit tests for {@link GameController} — the STOMP-over-WebSocket handlers for
 * the in-match interactive flow. All 7 handlers share the same auth contract:
 * read {@code authenticatedPlayerId} from the WebSocket session attributes
 * (set by {@link SessionHandshakeInterceptor}); if missing, reject the message
 * with {@code IllegalArgumentException}.
 *
 * <p>Coverage strategy: each handler gets a 3-test block — happy path, null
 * session attributes (the interceptor never ran or was bypassed), null player
 * id (session exists but no authenticated user). This prevents a regression
 * where someone removes the auth check from a single handler.
 */
class GameControllerUnitTest {

    private static final String MATCH_ID = "match-1";
    private static final String PLAYER_ID = "player-1";

    private MatchService matchService;
    private SimpMessagingTemplate messagingTemplate;
    private GameController controller;

    @BeforeEach
    void setUp() {
        matchService = mock(MatchService.class);
        messagingTemplate = mock(SimpMessagingTemplate.class);
        controller = new GameController(matchService, messagingTemplate);
    }

    private SimpMessageHeaderAccessor accessorFor(String playerId) {
        SimpMessageHeaderAccessor a = SimpMessageHeaderAccessor.create();
        Map<String, Object> attrs = new HashMap<>();
        attrs.put(SessionHandshakeInterceptor.PLAYER_ID_ATTR, playerId);
        a.setSessionAttributes(attrs);
        return a;
    }

    private SimpMessageHeaderAccessor accessorWithNullSessionAttrs() {
        return SimpMessageHeaderAccessor.create();
    }

    private SimpMessageHeaderAccessor accessorWithEmptyAttrs() {
        SimpMessageHeaderAccessor a = SimpMessageHeaderAccessor.create();
        a.setSessionAttributes(new HashMap<>());
        return a;
    }

    // ---------------------------------------------------------------- coin-toss

    @Nested
    class CoinToss {
        @Test
        void authenticatedPlayer_DelegatesToService() {
            controller.handleCoinToss(MATCH_ID,
                    Map.of("coinSide", "HEADS"),
                    accessorFor(PLAYER_ID));

            verify(matchService).chooseCoinSide(MATCH_ID, PLAYER_ID, "HEADS");
        }

        @Test
        void nullSessionAttrs_ThrowsAndDoesNotCallService() {
            SimpMessageHeaderAccessor accessor = accessorWithNullSessionAttrs();

            assertThatThrownBy(() -> controller.handleCoinToss(MATCH_ID, Map.of(), accessor))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("Not authenticated");
            verifyNoInteractions(matchService);
        }

        @Test
        void nullPlayerIdInAttrs_ThrowsAndDoesNotCallService() {
            SimpMessageHeaderAccessor accessor = accessorWithEmptyAttrs();

            assertThatThrownBy(() -> controller.handleCoinToss(MATCH_ID, Map.of(), accessor))
                    .isInstanceOf(IllegalArgumentException.class);
            verifyNoInteractions(matchService);
        }

        @Test
        void missingCoinSideKey_ForwardsNullToService() {
            // Parity with handleSetup: missing payload keys become nulls passed
            // straight through. If service input validation is later moved to
            // the controller, this test should flip to an exception assertion.
            controller.handleCoinToss(MATCH_ID, Map.of(), accessorFor(PLAYER_ID));

            verify(matchService).chooseCoinSide(MATCH_ID, PLAYER_ID, null);
        }

        @Test
        void emptyStringPlayerId_PassesEmptyStringToService() {
            // Current guard is `playerId == null` — empty string passes through.
            // Mirrors the PaymentController.webhook behavior we documented.
            // To tighten the check, change the controller to also reject blank
            // and update this test to expect IllegalArgumentException.
            controller.handleCoinToss(MATCH_ID,
                    Map.of("coinSide", "HEADS"),
                    accessorFor(""));

            verify(matchService).chooseCoinSide(MATCH_ID, "", "HEADS");
        }
    }

    // ------------------------------------------------------------- request-deal

    @Nested
    class RequestDeal {
        @Test
        void authenticatedPlayer_DelegatesToService() {
            controller.handleRequestDeal(MATCH_ID, accessorFor(PLAYER_ID));

            verify(matchService).requestDeal(MATCH_ID, PLAYER_ID);
        }

        @Test
        void nullSessionAttrs_Throws() {
            SimpMessageHeaderAccessor accessor = accessorWithNullSessionAttrs();
            assertThatThrownBy(() -> controller.handleRequestDeal(MATCH_ID, accessor))
                    .isInstanceOf(IllegalArgumentException.class);
            verifyNoInteractions(matchService);
        }

        @Test
        void nullPlayerId_Throws() {
            SimpMessageHeaderAccessor accessor = accessorWithEmptyAttrs();
            assertThatThrownBy(() -> controller.handleRequestDeal(MATCH_ID, accessor))
                    .isInstanceOf(IllegalArgumentException.class);
            verifyNoInteractions(matchService);
        }
    }

    // ----------------------------------------------------------- choose-starter

    @Nested
    class ChooseStarter {
        @Test
        void authenticatedPlayer_DelegatesToService() {
            controller.handleChooseStarter(MATCH_ID,
                    Map.of("starterPlayerId", "opponent-2"),
                    accessorFor(PLAYER_ID));

            verify(matchService).chooseFirstTurn(MATCH_ID, PLAYER_ID, "opponent-2");
        }

        @Test
        void nullSessionAttrs_Throws() {
            assertThatThrownBy(() -> controller.handleChooseStarter(MATCH_ID,
                    Map.of(), accessorWithNullSessionAttrs()))
                    .isInstanceOf(IllegalArgumentException.class);
            verifyNoInteractions(matchService);
        }

        @Test
        void nullPlayerId_Throws() {
            assertThatThrownBy(() -> controller.handleChooseStarter(MATCH_ID,
                    Map.of(), accessorWithEmptyAttrs()))
                    .isInstanceOf(IllegalArgumentException.class);
            verifyNoInteractions(matchService);
        }

        @Test
        void missingStarterPlayerIdKey_ForwardsNullToService() {
            controller.handleChooseStarter(MATCH_ID, Map.of(), accessorFor(PLAYER_ID));

            verify(matchService).chooseFirstTurn(MATCH_ID, PLAYER_ID, null);
        }
    }

    // ------------------------------------------------------------ select-active

    @Nested
    class SelectActive {
        @Test
        void authenticatedPlayer_DelegatesToService() {
            controller.handleSelectActive(MATCH_ID,
                    Map.of("cardId", "xy1-1"),
                    accessorFor(PLAYER_ID));

            verify(matchService).selectActive(MATCH_ID, PLAYER_ID, "xy1-1");
        }

        @Test
        void nullSessionAttrs_Throws() {
            assertThatThrownBy(() -> controller.handleSelectActive(MATCH_ID,
                    Map.of(), accessorWithNullSessionAttrs()))
                    .isInstanceOf(IllegalArgumentException.class);
            verifyNoInteractions(matchService);
        }

        @Test
        void nullPlayerId_Throws() {
            assertThatThrownBy(() -> controller.handleSelectActive(MATCH_ID,
                    Map.of(), accessorWithEmptyAttrs()))
                    .isInstanceOf(IllegalArgumentException.class);
            verifyNoInteractions(matchService);
        }

        @Test
        void missingCardIdKey_ForwardsNullToService() {
            controller.handleSelectActive(MATCH_ID, Map.of(), accessorFor(PLAYER_ID));

            verify(matchService).selectActive(MATCH_ID, PLAYER_ID, null);
        }
    }

    // -------------------------------------------------------------- place-bench

    @Nested
    class PlaceBench {
        @Test
        void authenticatedPlayer_DelegatesToService() {
            controller.handlePlaceBench(MATCH_ID,
                    Map.of("cardId", "xy1-5"),
                    accessorFor(PLAYER_ID));

            verify(matchService).placeBench(MATCH_ID, PLAYER_ID, "xy1-5");
        }

        @Test
        void nullSessionAttrs_Throws() {
            assertThatThrownBy(() -> controller.handlePlaceBench(MATCH_ID,
                    Map.of(), accessorWithNullSessionAttrs()))
                    .isInstanceOf(IllegalArgumentException.class);
            verifyNoInteractions(matchService);
        }

        @Test
        void nullPlayerId_Throws() {
            assertThatThrownBy(() -> controller.handlePlaceBench(MATCH_ID,
                    Map.of(), accessorWithEmptyAttrs()))
                    .isInstanceOf(IllegalArgumentException.class);
            verifyNoInteractions(matchService);
        }

        @Test
        void missingCardIdKey_ForwardsNullToService() {
            controller.handlePlaceBench(MATCH_ID, Map.of(), accessorFor(PLAYER_ID));

            verify(matchService).placeBench(MATCH_ID, PLAYER_ID, null);
        }
    }

    // ------------------------------------------------------------- setup-ready

    @Nested
    class SetupReady {
        @Test
        void authenticatedPlayer_DelegatesToService() {
            controller.handleSetupReady(MATCH_ID, accessorFor(PLAYER_ID));

            verify(matchService).confirmSetup(MATCH_ID, PLAYER_ID);
        }

        @Test
        void nullSessionAttrs_Throws() {
            assertThatThrownBy(() -> controller.handleSetupReady(MATCH_ID,
                    accessorWithNullSessionAttrs()))
                    .isInstanceOf(IllegalArgumentException.class);
            verifyNoInteractions(matchService);
        }

        @Test
        void nullPlayerId_Throws() {
            assertThatThrownBy(() -> controller.handleSetupReady(MATCH_ID,
                    accessorWithEmptyAttrs()))
                    .isInstanceOf(IllegalArgumentException.class);
            verifyNoInteractions(matchService);
        }
    }

    // -------------------------------------------------------------------- setup

    @Nested
    class Setup {
        @Test
        void authenticatedPlayer_DelegatesWithActiveAndBenchCards() {
            Map<String, Object> payload = new HashMap<>();
            payload.put("activeCardId", "xy1-1");
            payload.put("benchCardIds", List.of("xy1-2", "xy1-3", "xy1-4"));

            controller.handleSetup(MATCH_ID, payload, accessorFor(PLAYER_ID));

            verify(matchService).processSetup(MATCH_ID, PLAYER_ID, "xy1-1",
                    List.of("xy1-2", "xy1-3", "xy1-4"));
        }

        @Test
        void nullPayloadFields_PassesNullsThrough() {
            // The controller does not validate the payload — missing fields become
            // nulls forwarded to the service. Pin this so any future validation
            // change surfaces here.
            Map<String, Object> payload = new HashMap<>();
            // no activeCardId, no benchCardIds

            controller.handleSetup(MATCH_ID, payload, accessorFor(PLAYER_ID));

            verify(matchService).processSetup(MATCH_ID, PLAYER_ID, null, null);
        }

        @Test
        void nullSessionAttrs_Throws() {
            assertThatThrownBy(() -> controller.handleSetup(MATCH_ID,
                    new HashMap<>(), accessorWithNullSessionAttrs()))
                    .isInstanceOf(IllegalArgumentException.class);
            verifyNoInteractions(matchService);
        }

        @Test
        void nullPlayerId_Throws() {
            assertThatThrownBy(() -> controller.handleSetup(MATCH_ID,
                    new HashMap<>(), accessorWithEmptyAttrs()))
                    .isInstanceOf(IllegalArgumentException.class);
            verifyNoInteractions(matchService);
        }
    }
}
