package ar.edu.utn.frc.tup.piii.controllers;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import ar.edu.utn.frc.tup.piii.configs.SessionHandshakeInterceptor;
import ar.edu.utn.frc.tup.piii.dtos.engine.GameAction;
import ar.edu.utn.frc.tup.piii.dtos.engine.GameActionType;
import ar.edu.utn.frc.tup.piii.services.MatchService;

/**
 * Exhaustive unit tests for {@link GameController} — the STOMP-over-WebSocket
 * handlers for the interactive in-match flow.
 *
 * <p><strong>Why not MockMvc?</strong> Every handler is a STOMP destination
 * ({@code @MessageMapping}), not an HTTP endpoint. {@code MockMvc} drives the
 * Spring MVC {@code DispatcherServlet}, which does not route {@code @MessageMapping}
 * messages, so it cannot invoke these handlers nor exercise their branches. The
 * canonical pattern for WebSocket controllers in this codebase
 * ({@code ChatControllerTest}, {@code GameControllerUnitTest}) is direct method
 * invocation with a hand-built {@link SimpMessageHeaderAccessor} carrying the
 * authenticated player id placed by {@link SessionHandshakeInterceptor}. This
 * class follows that pattern but covers <em>all 14 handlers</em> and every branch
 * (auth guards, ternaries on payload numbers, the {@code handleAction} target/
 * try-catch branches) to drive instruction and branch coverage above 90%.
 *
 * <p>Shared auth contract for every handler: read {@code authenticatedPlayerId}
 * from the session attributes; if the attribute map is {@code null} or the id is
 * {@code null}, reject with {@link IllegalArgumentException} and never touch the
 * service.
 */
class GameControllerTest {

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

    /** Accessor with a populated session carrying the given authenticated id. */
    private SimpMessageHeaderAccessor accessorFor(String playerId) {
        SimpMessageHeaderAccessor accessor = SimpMessageHeaderAccessor.create();
        Map<String, Object> attrs = new HashMap<>();
        attrs.put(SessionHandshakeInterceptor.PLAYER_ID_ATTR, playerId);
        accessor.setSessionAttributes(attrs);
        return accessor;
    }

    /** Accessor whose session attribute map is {@code null} (interceptor never ran). */
    private SimpMessageHeaderAccessor accessorWithNullSessionAttrs() {
        return SimpMessageHeaderAccessor.create();
    }

    /** Accessor with an empty session (present, but no authenticated id). */
    private SimpMessageHeaderAccessor accessorWithEmptyAttrs() {
        SimpMessageHeaderAccessor accessor = SimpMessageHeaderAccessor.create();
        accessor.setSessionAttributes(new HashMap<>());
        return accessor;
    }

    // ----------------------------------------------------------------- coin-toss

    @Nested
    class CoinToss {
        @Test
        void authenticated_DelegatesToService() {
            controller.handleCoinToss(MATCH_ID,
                    Map.of("coinSide", "HEADS"), accessorFor(PLAYER_ID));

            verify(matchService).chooseCoinSide(MATCH_ID, PLAYER_ID, "HEADS");
        }

        @Test
        void missingCoinSideKey_ForwardsNull() {
            controller.handleCoinToss(MATCH_ID, Map.of(), accessorFor(PLAYER_ID));

            verify(matchService).chooseCoinSide(MATCH_ID, PLAYER_ID, null);
        }

        @Test
        void nullSessionAttrs_ThrowsAndSkipsService() {
            assertThatThrownBy(() -> controller.handleCoinToss(MATCH_ID,
                    Map.of(), accessorWithNullSessionAttrs()))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("Not authenticated");
            verifyNoInteractions(matchService);
        }

        @Test
        void nullPlayerId_ThrowsAndSkipsService() {
            assertThatThrownBy(() -> controller.handleCoinToss(MATCH_ID,
                    Map.of(), accessorWithEmptyAttrs()))
                    .isInstanceOf(IllegalArgumentException.class);
            verifyNoInteractions(matchService);
        }
    }

    // -------------------------------------------------------------- request-deal

    @Nested
    class RequestDeal {
        @Test
        void authenticated_DelegatesToService() {
            controller.handleRequestDeal(MATCH_ID, accessorFor(PLAYER_ID));

            verify(matchService).requestDeal(MATCH_ID, PLAYER_ID);
        }

        @Test
        void nullSessionAttrs_Throws() {
            assertThatThrownBy(() -> controller.handleRequestDeal(MATCH_ID,
                    accessorWithNullSessionAttrs()))
                    .isInstanceOf(IllegalArgumentException.class);
            verifyNoInteractions(matchService);
        }

        @Test
        void nullPlayerId_Throws() {
            assertThatThrownBy(() -> controller.handleRequestDeal(MATCH_ID,
                    accessorWithEmptyAttrs()))
                    .isInstanceOf(IllegalArgumentException.class);
            verifyNoInteractions(matchService);
        }
    }

    // ------------------------------------------------------------ choose-starter

    @Nested
    class ChooseStarter {
        @Test
        void authenticated_DelegatesToService() {
            controller.handleChooseStarter(MATCH_ID,
                    Map.of("starterPlayerId", "opponent-2"), accessorFor(PLAYER_ID));

            verify(matchService).chooseFirstTurn(MATCH_ID, PLAYER_ID, "opponent-2");
        }

        @Test
        void missingStarterKey_ForwardsNull() {
            controller.handleChooseStarter(MATCH_ID, Map.of(), accessorFor(PLAYER_ID));

            verify(matchService).chooseFirstTurn(MATCH_ID, PLAYER_ID, null);
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
    }

    // ------------------------------------------------------------- select-active

    @Nested
    class SelectActive {
        @Test
        void authenticated_DelegatesToService() {
            controller.handleSelectActive(MATCH_ID,
                    Map.of("cardId", "xy1-1"), accessorFor(PLAYER_ID));

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
    }

    // --------------------------------------------------------------- place-bench

    @Nested
    class PlaceBench {
        @Test
        void authenticated_DelegatesToService() {
            controller.handlePlaceBench(MATCH_ID,
                    Map.of("cardId", "xy1-5"), accessorFor(PLAYER_ID));

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
    }

    // --------------------------------------------------------------- setup-ready

    @Nested
    class SetupReady {
        @Test
        void authenticated_DelegatesToService() {
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

    // --------------------------------------------------------------------- setup

    @Nested
    class Setup {
        @Test
        void authenticated_DelegatesWithActiveAndBench() {
            Map<String, Object> payload = new HashMap<>();
            payload.put("activeCardId", "xy1-1");
            payload.put("benchCardIds", List.of("xy1-2", "xy1-3"));

            controller.handleSetup(MATCH_ID, payload, accessorFor(PLAYER_ID));

            verify(matchService).processSetup(MATCH_ID, PLAYER_ID, "xy1-1",
                    List.of("xy1-2", "xy1-3"));
        }

        @Test
        void missingFields_ForwardsNulls() {
            controller.handleSetup(MATCH_ID, new HashMap<>(), accessorFor(PLAYER_ID));

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

    // ------------------------------------------------------------- mulligan-draw

    @Nested
    class MulliganDraw {
        @Test
        void countAsNumber_ForwardsIntValue() {
            controller.handleMulliganDraw(MATCH_ID,
                    Map.of("count", 3), accessorFor(PLAYER_ID));

            verify(matchService).resolveMulliganDraw(MATCH_ID, PLAYER_ID, 3);
        }

        @Test
        void missingCount_DefaultsToZero() {
            // countObj is null -> not a Number -> falls back to 0.
            controller.handleMulliganDraw(MATCH_ID, new HashMap<>(), accessorFor(PLAYER_ID));

            verify(matchService).resolveMulliganDraw(MATCH_ID, PLAYER_ID, 0);
        }

        @Test
        void nonNumericCount_DefaultsToZero() {
            // A String value is not a Number -> 0 branch.
            controller.handleMulliganDraw(MATCH_ID,
                    Map.of("count", "two"), accessorFor(PLAYER_ID));

            verify(matchService).resolveMulliganDraw(MATCH_ID, PLAYER_ID, 0);
        }

        @Test
        void nullSessionAttrs_Throws() {
            assertThatThrownBy(() -> controller.handleMulliganDraw(MATCH_ID,
                    new HashMap<>(), accessorWithNullSessionAttrs()))
                    .isInstanceOf(IllegalArgumentException.class);
            verifyNoInteractions(matchService);
        }

        @Test
        void nullPlayerId_Throws() {
            assertThatThrownBy(() -> controller.handleMulliganDraw(MATCH_ID,
                    new HashMap<>(), accessorWithEmptyAttrs()))
                    .isInstanceOf(IllegalArgumentException.class);
            verifyNoInteractions(matchService);
        }
    }

    // -------------------------------------------------------------------- action

    @Nested
    class Action {
        private Map<String, Object> actionPayload(String type) {
            Map<String, Object> payload = new HashMap<>();
            payload.put("type", type);
            return payload;
        }

        @Test
        void authenticated_NoTarget_DelegatesToService() {
            Map<String, Object> payload = actionPayload("END_TURN");

            controller.handleAction(MATCH_ID, payload, accessorFor(PLAYER_ID));

            ArgumentCaptor<GameAction> captor = ArgumentCaptor.forClass(GameAction.class);
            verify(matchService).submitAction(eq(MATCH_ID), eq(PLAYER_ID), captor.capture());
            GameAction sent = captor.getValue();
            assertThat(sent.getType()).isEqualTo(GameActionType.END_TURN);
            assertThat(sent.getTargetCardId()).isNull();
        }

        @Test
        void authenticated_WithAllFields_PopulatesAndForwardsAction() {
            Map<String, Object> payload = actionPayload("EXECUTE_ATTACK");
            payload.put("cardId", "xy1-1");
            payload.put("attackName", "Thunder Shock");
            payload.put("targetCardId", "xy1-9");
            payload.put("energyCardIds", new ArrayList<>(List.of("e1", "e2")));

            controller.handleAction(MATCH_ID, payload, accessorFor(PLAYER_ID));

            ArgumentCaptor<GameAction> captor = ArgumentCaptor.forClass(GameAction.class);
            verify(matchService).submitAction(eq(MATCH_ID), eq(PLAYER_ID), captor.capture());
            GameAction sent = captor.getValue();
            assertThat(sent.getType()).isEqualTo(GameActionType.EXECUTE_ATTACK);
            assertThat(sent.getCardId()).isEqualTo("xy1-1");
            assertThat(sent.getAttackName()).isEqualTo("Thunder Shock");
            assertThat(sent.getTargetCardId()).isEqualTo("xy1-9");
            assertThat(sent.getEnergyCardIds()).containsExactly("e1", "e2");
        }

        @Test
        void targetCardIdNonString_IsStringified() {
            // payload.get("targetCardId") != null branch with a non-String value;
            // the controller calls toString() on it.
            Map<String, Object> payload = actionPayload("ATTACK");
            payload.put("targetCardId", 42);

            controller.handleAction(MATCH_ID, payload, accessorFor(PLAYER_ID));

            ArgumentCaptor<GameAction> captor = ArgumentCaptor.forClass(GameAction.class);
            verify(matchService).submitAction(eq(MATCH_ID), eq(PLAYER_ID), captor.capture());
            assertThat(captor.getValue().getTargetCardId()).isEqualTo("42");
        }

        @Test
        void serviceThrows_SendsActionErrorToPlayerTopic() {
            Map<String, Object> payload = actionPayload("ATTACK");
            doThrow(new RuntimeException("Illegal action"))
                    .when(matchService).submitAction(eq(MATCH_ID), eq(PLAYER_ID), any(GameAction.class));

            // Must not propagate: the controller swallows the throwable and notifies the client.
            controller.handleAction(MATCH_ID, payload, accessorFor(PLAYER_ID));

            ArgumentCaptor<Object> payloadCaptor = ArgumentCaptor.forClass(Object.class);
            verify(messagingTemplate).convertAndSend(
                    eq("/topic/matches/" + MATCH_ID + "/state/" + PLAYER_ID),
                    payloadCaptor.capture());

            Object errorPayload = payloadCaptor.getValue();
            assertThat(errorPayload).isInstanceOf(Map.class);
            @SuppressWarnings("unchecked")
            Map<String, Object> error = (Map<String, Object>) errorPayload;
            assertThat(error.get("type")).isEqualTo("ACTION_ERROR");
            @SuppressWarnings("unchecked")
            Map<String, Object> inner = (Map<String, Object>) error.get("payload");
            assertThat(inner.get("message")).isEqualTo("Illegal action");
        }

        @Test
        void serviceSucceeds_DoesNotSendErrorPayload() {
            controller.handleAction(MATCH_ID, actionPayload("END_TURN"), accessorFor(PLAYER_ID));

            verify(matchService).submitAction(eq(MATCH_ID), eq(PLAYER_ID), any(GameAction.class));
            verify(messagingTemplate, never()).convertAndSend(any(String.class), any(Object.class));
        }

        @Test
        void nullSessionAttrs_Throws() {
            assertThatThrownBy(() -> controller.handleAction(MATCH_ID,
                    actionPayload("END_TURN"), accessorWithNullSessionAttrs()))
                    .isInstanceOf(IllegalArgumentException.class);
            verifyNoInteractions(matchService);
        }

        @Test
        void nullPlayerId_Throws() {
            assertThatThrownBy(() -> controller.handleAction(MATCH_ID,
                    actionPayload("END_TURN"), accessorWithEmptyAttrs()))
                    .isInstanceOf(IllegalArgumentException.class);
            verifyNoInteractions(matchService);
        }
    }

    // -------------------------------------------------------------- select-prize

    @Nested
    class SelectPrize {
        @Test
        void prizeIndexAsNumber_ForwardsIntValue() {
            controller.handleSelectPrize(MATCH_ID,
                    Map.of("prizeIndex", 2), accessorFor(PLAYER_ID));

            verify(matchService).selectPrize(MATCH_ID, PLAYER_ID, 2);
        }

        @Test
        void missingPrizeIndex_DefaultsToZero() {
            controller.handleSelectPrize(MATCH_ID, new HashMap<>(), accessorFor(PLAYER_ID));

            verify(matchService).selectPrize(MATCH_ID, PLAYER_ID, 0);
        }

        @Test
        void nonNumericPrizeIndex_DefaultsToZero() {
            controller.handleSelectPrize(MATCH_ID,
                    Map.of("prizeIndex", "first"), accessorFor(PLAYER_ID));

            verify(matchService).selectPrize(MATCH_ID, PLAYER_ID, 0);
        }

        @Test
        void nullSessionAttrs_Throws() {
            assertThatThrownBy(() -> controller.handleSelectPrize(MATCH_ID,
                    new HashMap<>(), accessorWithNullSessionAttrs()))
                    .isInstanceOf(IllegalArgumentException.class);
            verifyNoInteractions(matchService);
        }

        @Test
        void nullPlayerId_Throws() {
            assertThatThrownBy(() -> controller.handleSelectPrize(MATCH_ID,
                    new HashMap<>(), accessorWithEmptyAttrs()))
                    .isInstanceOf(IllegalArgumentException.class);
            verifyNoInteractions(matchService);
        }
    }

    // ------------------------------------------------------------- promote-bench

    @Nested
    class PromoteBench {
        @Test
        void benchSlotAsNumber_ForwardsIntValue() {
            controller.handlePromoteBench(MATCH_ID,
                    Map.of("benchSlot", 1), accessorFor(PLAYER_ID));

            verify(matchService).promoteFromBench(MATCH_ID, PLAYER_ID, 1);
        }

        @Test
        void missingBenchSlot_DefaultsToZero() {
            controller.handlePromoteBench(MATCH_ID, new HashMap<>(), accessorFor(PLAYER_ID));

            verify(matchService).promoteFromBench(MATCH_ID, PLAYER_ID, 0);
        }

        @Test
        void nonNumericBenchSlot_DefaultsToZero() {
            controller.handlePromoteBench(MATCH_ID,
                    Map.of("benchSlot", "left"), accessorFor(PLAYER_ID));

            verify(matchService).promoteFromBench(MATCH_ID, PLAYER_ID, 0);
        }

        @Test
        void nullSessionAttrs_Throws() {
            assertThatThrownBy(() -> controller.handlePromoteBench(MATCH_ID,
                    new HashMap<>(), accessorWithNullSessionAttrs()))
                    .isInstanceOf(IllegalArgumentException.class);
            verifyNoInteractions(matchService);
        }

        @Test
        void nullPlayerId_Throws() {
            assertThatThrownBy(() -> controller.handlePromoteBench(MATCH_ID,
                    new HashMap<>(), accessorWithEmptyAttrs()))
                    .isInstanceOf(IllegalArgumentException.class);
            verifyNoInteractions(matchService);
        }
    }

    // ----------------------------------------------------------------- surrender

    @Nested
    class Surrender {
        @Test
        void authenticated_DelegatesToService() {
            controller.handleSurrender(MATCH_ID, accessorFor(PLAYER_ID));

            verify(matchService).surrender(MATCH_ID, PLAYER_ID);
        }

        @Test
        void nullSessionAttrs_Throws() {
            assertThatThrownBy(() -> controller.handleSurrender(MATCH_ID,
                    accessorWithNullSessionAttrs()))
                    .isInstanceOf(IllegalArgumentException.class);
            verifyNoInteractions(matchService);
        }

        @Test
        void nullPlayerId_Throws() {
            assertThatThrownBy(() -> controller.handleSurrender(MATCH_ID,
                    accessorWithEmptyAttrs()))
                    .isInstanceOf(IllegalArgumentException.class);
            verifyNoInteractions(matchService);
        }
    }

    // ------------------------------------------------------------- pokemon-hover

    @Nested
    class PokemonHover {
        @Test
        void authenticated_DelegatesToService() {
            controller.handlePokemonHover(MATCH_ID, accessorFor(PLAYER_ID));

            verify(matchService).notifyPokemonHover(MATCH_ID, PLAYER_ID);
        }

        @Test
        void nullSessionAttrs_Throws() {
            assertThatThrownBy(() -> controller.handlePokemonHover(MATCH_ID,
                    accessorWithNullSessionAttrs()))
                    .isInstanceOf(IllegalArgumentException.class);
            verifyNoInteractions(matchService);
        }

        @Test
        void nullPlayerId_Throws() {
            assertThatThrownBy(() -> controller.handlePokemonHover(MATCH_ID,
                    accessorWithEmptyAttrs()))
                    .isInstanceOf(IllegalArgumentException.class);
            verifyNoInteractions(matchService);
        }
    }

    // ----------------------------------------------------------- pokemon-unhover

    @Nested
    class PokemonUnhover {
        @Test
        void authenticated_DelegatesToService() {
            controller.handlePokemonUnhover(MATCH_ID, accessorFor(PLAYER_ID));

            verify(matchService).notifyPokemonUnhover(MATCH_ID, PLAYER_ID);
        }

        @Test
        void nullSessionAttrs_Throws() {
            assertThatThrownBy(() -> controller.handlePokemonUnhover(MATCH_ID,
                    accessorWithNullSessionAttrs()))
                    .isInstanceOf(IllegalArgumentException.class);
            verifyNoInteractions(matchService);
        }

        @Test
        void nullPlayerId_Throws() {
            assertThatThrownBy(() -> controller.handlePokemonUnhover(MATCH_ID,
                    accessorWithEmptyAttrs()))
                    .isInstanceOf(IllegalArgumentException.class);
            verifyNoInteractions(matchService);
        }
    }
}
