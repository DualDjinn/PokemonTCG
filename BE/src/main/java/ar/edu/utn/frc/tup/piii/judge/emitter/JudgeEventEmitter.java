package ar.edu.utn.frc.tup.piii.judge.emitter;

import ar.edu.utn.frc.tup.piii.dtos.engine.GameActionType;
import ar.edu.utn.frc.tup.piii.judge.events.ActionBlockedEvent;
import ar.edu.utn.frc.tup.piii.judge.events.DamageResolvedEvent;
import ar.edu.utn.frc.tup.piii.judge.events.KnockoutEvent;
import ar.edu.utn.frc.tup.piii.judge.events.StatusAppliedEvent;
import ar.edu.utn.frc.tup.piii.judge.events.VictoryEvent;
import ar.edu.utn.frc.tup.piii.judge.store.JudgeEventStore;
import ar.edu.utn.frc.tup.piii.models.StatusCondition;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class JudgeEventEmitter {

    private final JudgeEventStore store;

    public JudgeEventEmitter(JudgeEventStore store) {
        this.store = store;
    }

    public void emitActionBlocked(String matchId, int turnNumber, String playerId,
                                    GameActionType action, String targetCardId,
                                    String targetName, List<String> reasons) {
        String code = deriveActionBlockedCode(action, reasons);
        store.record(ActionBlockedEvent.builder()
                .engineMatchId(matchId)
                .turnNumber(turnNumber)
                .actingPlayerId(playerId)
                .timestamp(System.currentTimeMillis())
                .eventCode(code)
                .blockedAction(action)
                .targetCardId(targetCardId)
                .targetPokemonName(targetName)
                .activeReasons(reasons != null ? List.copyOf(reasons) : List.of())
                .build());
    }

    public void emitDamageResolved(String matchId, int turnNumber, String playerId,
                                    String attackerCardId, String attackName,
                                    int baseDamage, int pendingDamageAfterEffect,
                                    boolean weaknessApplied, boolean resistanceApplied,
                                    boolean weaknessIgnored, boolean resistanceIgnored,
                                    int calculatedDamage,
                                    boolean preventedByAllEffects, boolean preventedByAllDamage,
                                    boolean preventedByThreshold, int thresholdValue,
                                    int reductionApplied, int finalDamage) {
        String code = finalDamage <= 0
                ? JudgeEventCodes.DAMAGE_FINAL_ZERO
                : JudgeEventCodes.DAMAGE_FINAL_APPLIED;
        store.record(DamageResolvedEvent.builder()
                .engineMatchId(matchId)
                .turnNumber(turnNumber)
                .actingPlayerId(playerId)
                .timestamp(System.currentTimeMillis())
                .eventCode(code)
                .attackerCardId(attackerCardId)
                .attackName(attackName)
                .baseDamage(baseDamage)
                .pendingDamageAfterEffect(pendingDamageAfterEffect)
                .weaknessApplied(weaknessApplied)
                .resistanceApplied(resistanceApplied)
                .weaknessIgnored(weaknessIgnored)
                .resistanceIgnored(resistanceIgnored)
                .modifiersApplied(List.of())
                .calculatedDamage(calculatedDamage)
                .preventedByAllEffects(preventedByAllEffects)
                .preventedByAllDamage(preventedByAllDamage)
                .preventedByThreshold(preventedByThreshold)
                .thresholdValue(thresholdValue)
                .reductionApplied(reductionApplied)
                .finalDamage(finalDamage)
                .build());
    }

    public void emitStatusApplied(String matchId, int turnNumber, String playerId,
                                    String targetId, String targetName,
                                    StatusCondition status,
                                    String sourceAttackName, String sourceCardId) {
        store.record(StatusAppliedEvent.builder()
                .engineMatchId(matchId)
                .turnNumber(turnNumber)
                .actingPlayerId(playerId)
                .timestamp(System.currentTimeMillis())
                .eventCode(JudgeEventCodes.STATUS_APPLIED)
                .targetPokemonId(targetId)
                .targetPokemonName(targetName)
                .status(status)
                .wasCured(false)
                .cureReason(null)
                .sourceAttackName(sourceAttackName)
                .sourceCardId(sourceCardId)
                .build());
    }

    public void emitStatusCured(String matchId, int turnNumber, String playerId,
                                  String targetId, String targetName,
                                  StatusCondition status, String cureReason) {
        store.record(StatusAppliedEvent.builder()
                .engineMatchId(matchId)
                .turnNumber(turnNumber)
                .actingPlayerId(playerId)
                .timestamp(System.currentTimeMillis())
                .eventCode(JudgeEventCodes.STATUS_CURED)
                .targetPokemonId(targetId)
                .targetPokemonName(targetName)
                .status(status)
                .wasCured(true)
                .cureReason(cureReason)
                .sourceAttackName(null)
                .sourceCardId(null)
                .build());
    }

    public void emitKnockout(String matchId, int turnNumber, String playerId,
                               String knockedId, String knockedName,
                               int damageTaken, int maxHp, int prizesAwarded) {
        store.record(KnockoutEvent.builder()
                .engineMatchId(matchId)
                .turnNumber(turnNumber)
                .actingPlayerId(playerId)
                .timestamp(System.currentTimeMillis())
                .eventCode(JudgeEventCodes.POKEMON_KO_BY_DAMAGE)
                .knockedPokemonId(knockedId)
                .knockedPokemonName(knockedName)
                .damageTaken(damageTaken)
                .maxHp(maxHp)
                .prizesAwarded(prizesAwarded)
                .build());
    }

    public void emitVictory(String matchId, int turnNumber, String playerId,
                              String winnerPlayerId, String victoryReason) {
        store.record(VictoryEvent.builder()
                .engineMatchId(matchId)
                .turnNumber(turnNumber)
                .actingPlayerId(playerId)
                .timestamp(System.currentTimeMillis())
                .eventCode(victoryReason)
                .winnerPlayerId(winnerPlayerId)
                .victoryReason(victoryReason)
                .build());
    }

    private String deriveActionBlockedCode(GameActionType action, List<String> reasons) {
        if (reasons == null || reasons.isEmpty()) {
            return JudgeEventCodes.ACTION_BLOCKED_WRONG_PHASE;
        }
        return reasons.get(0);
    }
}
