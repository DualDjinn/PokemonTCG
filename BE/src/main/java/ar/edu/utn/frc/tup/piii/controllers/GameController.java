package ar.edu.utn.frc.tup.piii.controllers;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;
import ar.edu.utn.frc.tup.piii.configs.SessionHandshakeInterceptor;
import ar.edu.utn.frc.tup.piii.dtos.engine.GameAction;
import ar.edu.utn.frc.tup.piii.dtos.engine.GameActionType;
import ar.edu.utn.frc.tup.piii.services.MatchService;

@Controller
public class GameController {

    private final MatchService matchService;
    private final SimpMessagingTemplate messagingTemplate;

    public GameController(MatchService matchService, SimpMessagingTemplate messagingTemplate) {
        this.matchService = matchService;
        this.messagingTemplate = messagingTemplate;
    }

    @MessageMapping("/matches/{matchId}/coin-toss")
    public void handleCoinToss(
            @DestinationVariable String matchId,
            @Payload Map<String, String> payload,
            SimpMessageHeaderAccessor headerAccessor) {
        Map<String, Object> sessionAttrs = headerAccessor.getSessionAttributes();
        String playerId = sessionAttrs == null ? null
                : (String) sessionAttrs.get(SessionHandshakeInterceptor.PLAYER_ID_ATTR);
        if (playerId == null) {
            throw new IllegalArgumentException("Not authenticated");
        }

        matchService.chooseCoinSide(matchId, playerId, payload.get("coinSide"));
    }

    @MessageMapping("/matches/{matchId}/request-deal")
    public void handleRequestDeal(
            @DestinationVariable String matchId,
            SimpMessageHeaderAccessor headerAccessor) {
        Map<String, Object> sessionAttrs = headerAccessor.getSessionAttributes();
        String playerId = sessionAttrs == null ? null
                : (String) sessionAttrs.get(SessionHandshakeInterceptor.PLAYER_ID_ATTR);
        if (playerId == null) {
            throw new IllegalArgumentException("Not authenticated");
        }

        matchService.requestDeal(matchId, playerId);
    }

    @MessageMapping("/matches/{matchId}/choose-starter")
    public void handleChooseStarter(
            @DestinationVariable String matchId,
            @Payload Map<String, String> payload,
            SimpMessageHeaderAccessor headerAccessor) {
        Map<String, Object> sessionAttrs = headerAccessor.getSessionAttributes();
        String playerId = sessionAttrs == null ? null
                : (String) sessionAttrs.get(SessionHandshakeInterceptor.PLAYER_ID_ATTR);
        if (playerId == null) {
            throw new IllegalArgumentException("Not authenticated");
        }

        matchService.chooseFirstTurn(matchId, playerId, payload.get("starterPlayerId"));
    }

    @MessageMapping("/matches/{matchId}/select-active")
    public void handleSelectActive(
            @DestinationVariable String matchId,
            @Payload Map<String, String> payload,
            SimpMessageHeaderAccessor headerAccessor) {
        Map<String, Object> sessionAttrs = headerAccessor.getSessionAttributes();
        String playerId = sessionAttrs == null ? null
                : (String) sessionAttrs.get(SessionHandshakeInterceptor.PLAYER_ID_ATTR);
        if (playerId == null) {
            throw new IllegalArgumentException("Not authenticated");
        }

        matchService.selectActive(matchId, playerId, payload.get("cardId"));
    }

    @MessageMapping("/matches/{matchId}/place-bench")
    public void handlePlaceBench(
            @DestinationVariable String matchId,
            @Payload Map<String, String> payload,
            SimpMessageHeaderAccessor headerAccessor) {
        Map<String, Object> sessionAttrs = headerAccessor.getSessionAttributes();
        String playerId = sessionAttrs == null ? null
                : (String) sessionAttrs.get(SessionHandshakeInterceptor.PLAYER_ID_ATTR);
        if (playerId == null) {
            throw new IllegalArgumentException("Not authenticated");
        }

        matchService.placeBench(matchId, playerId, payload.get("cardId"));
    }

    @MessageMapping("/matches/{matchId}/setup-ready")
    public void handleSetupReady(
            @DestinationVariable String matchId,
            SimpMessageHeaderAccessor headerAccessor) {
        Map<String, Object> sessionAttrs = headerAccessor.getSessionAttributes();
        String playerId = sessionAttrs == null ? null
                : (String) sessionAttrs.get(SessionHandshakeInterceptor.PLAYER_ID_ATTR);
        if (playerId == null) {
            throw new IllegalArgumentException("Not authenticated");
        }

        matchService.confirmSetup(matchId, playerId);
    }

    @MessageMapping("/matches/{matchId}/setup")
    public void handleSetup(
            @DestinationVariable String matchId,
            @Payload Map<String, Object> payload,
            SimpMessageHeaderAccessor headerAccessor) {
        Map<String, Object> sessionAttrs = headerAccessor.getSessionAttributes();
        String playerId = sessionAttrs == null ? null
                : (String) sessionAttrs.get(SessionHandshakeInterceptor.PLAYER_ID_ATTR);
        if (playerId == null) {
            throw new IllegalArgumentException("Not authenticated");
        }

        String activeCardId = (String) payload.get("activeCardId");
        @SuppressWarnings("unchecked")
        List<String> benchCardIds = (List<String>) payload.get("benchCardIds");
        matchService.processSetup(matchId, playerId, activeCardId, benchCardIds);
    }

    @MessageMapping("/matches/{matchId}/mulligan-draw")
    public void handleMulliganDraw(
            @DestinationVariable String matchId,
            @Payload Map<String, Object> payload,
            SimpMessageHeaderAccessor headerAccessor) {
        Map<String, Object> sessionAttrs = headerAccessor.getSessionAttributes();
        String playerId = sessionAttrs == null ? null
                : (String) sessionAttrs.get(SessionHandshakeInterceptor.PLAYER_ID_ATTR);
        if (playerId == null) {
            throw new IllegalArgumentException("Not authenticated");
        }

        Object countObj = payload.get("count");
        int count = countObj instanceof Number n ? n.intValue() : 0;
        matchService.resolveMulliganDraw(matchId, playerId, count);
    }

    @MessageMapping("/matches/{matchId}/action")
    public void handleAction(
            @DestinationVariable String matchId,
            @Payload Map<String, Object> payload,
            SimpMessageHeaderAccessor headerAccessor) {
        Map<String, Object> sessionAttrs = headerAccessor.getSessionAttributes();
        String playerId = sessionAttrs == null ? null
                : (String) sessionAttrs.get(SessionHandshakeInterceptor.PLAYER_ID_ATTR);
        if (playerId == null) {
            throw new IllegalArgumentException("Not authenticated");
        }

        GameAction action = new GameAction();
        action.setType(GameActionType.valueOf((String) payload.get("type")));
        action.setCardId((String) payload.get("cardId"));
        action.setAttackName((String) payload.get("attackName"));
        if (payload.get("targetCardId") != null) {
            action.setTargetCardId(payload.get("targetCardId").toString());
        }
        @SuppressWarnings("unchecked")
        List<String> energyCardIds = (List<String>) payload.get("energyCardIds");
        action.setEnergyCardIds(energyCardIds);
        @SuppressWarnings("unchecked")
        List<String> chosenCardIds = (List<String>) payload.get("chosenCardIds");
        action.setChosenCardIds(chosenCardIds);
        try {
            matchService.submitAction(matchId, playerId, action);
        } catch (Throwable e) {
            Map<String, Object> errorPayload = new HashMap<>();
            errorPayload.put("type", "ACTION_ERROR");
            Map<String, Object> inner = new HashMap<>();
            inner.put("message", e.getMessage());
            errorPayload.put("payload", inner);
            messagingTemplate.convertAndSend(
                "/topic/matches/" + matchId + "/state/" + playerId,
                (Object) errorPayload
            );
        }
    }

    @MessageMapping("/matches/{matchId}/select-prize")
    public void handleSelectPrize(
            @DestinationVariable String matchId,
            @Payload Map<String, Object> payload,
            SimpMessageHeaderAccessor headerAccessor) {
        Map<String, Object> sessionAttrs = headerAccessor.getSessionAttributes();
        String playerId = sessionAttrs == null ? null
                : (String) sessionAttrs.get(SessionHandshakeInterceptor.PLAYER_ID_ATTR);
        if (playerId == null) {
            throw new IllegalArgumentException("Not authenticated");
        }

        Object idxObj = payload.get("prizeIndex");
        int prizeIndex = idxObj instanceof Number n ? n.intValue() : 0;
        matchService.selectPrize(matchId, playerId, prizeIndex);
    }

    @MessageMapping("/matches/{matchId}/promote-bench")
    public void handlePromoteBench(
            @DestinationVariable String matchId,
            @Payload Map<String, Object> payload,
            SimpMessageHeaderAccessor headerAccessor) {
        Map<String, Object> sessionAttrs = headerAccessor.getSessionAttributes();
        String playerId = sessionAttrs == null ? null
                : (String) sessionAttrs.get(SessionHandshakeInterceptor.PLAYER_ID_ATTR);
        if (playerId == null) {
            throw new IllegalArgumentException("Not authenticated");
        }

        Object slotObj = payload.get("benchSlot");
        int benchSlot = slotObj instanceof Number n ? n.intValue() : 0;
        matchService.promoteFromBench(matchId, playerId, benchSlot);
    }

    @MessageMapping("/matches/{matchId}/surrender")
    public void handleSurrender(
            @DestinationVariable String matchId,
            SimpMessageHeaderAccessor headerAccessor) {
        Map<String, Object> sessionAttrs = headerAccessor.getSessionAttributes();
        String playerId = sessionAttrs == null ? null
                : (String) sessionAttrs.get(SessionHandshakeInterceptor.PLAYER_ID_ATTR);
        if (playerId == null) {
            throw new IllegalArgumentException("Not authenticated");
        }

        matchService.surrender(matchId, playerId);
    }

    @MessageMapping("/matches/{matchId}/pokemon-hover")
    public void handlePokemonHover(
            @DestinationVariable String matchId,
            SimpMessageHeaderAccessor headerAccessor) {
        Map<String, Object> sessionAttrs = headerAccessor.getSessionAttributes();
        String playerId = sessionAttrs == null ? null
                : (String) sessionAttrs.get(SessionHandshakeInterceptor.PLAYER_ID_ATTR);
        if (playerId == null) {
            throw new IllegalArgumentException("Not authenticated");
        }

        matchService.notifyPokemonHover(matchId, playerId);
    }

    @MessageMapping("/matches/{matchId}/pokemon-unhover")
    public void handlePokemonUnhover(
            @DestinationVariable String matchId,
            SimpMessageHeaderAccessor headerAccessor) {
        Map<String, Object> sessionAttrs = headerAccessor.getSessionAttributes();
        String playerId = sessionAttrs == null ? null
                : (String) sessionAttrs.get(SessionHandshakeInterceptor.PLAYER_ID_ATTR);
        if (playerId == null) {
            throw new IllegalArgumentException("Not authenticated");
        }

        matchService.notifyPokemonUnhover(matchId, playerId);
    }
}
