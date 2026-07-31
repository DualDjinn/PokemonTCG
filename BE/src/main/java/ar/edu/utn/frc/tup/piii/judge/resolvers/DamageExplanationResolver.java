package ar.edu.utn.frc.tup.piii.judge.resolvers;

import ar.edu.utn.frc.tup.piii.dtos.engine.MatchStateDTO;
import ar.edu.utn.frc.tup.piii.dtos.judge.JudgeQueryCategory;
import ar.edu.utn.frc.tup.piii.dtos.judge.JudgeQueryDTO;
import ar.edu.utn.frc.tup.piii.dtos.judge.JudgeResponseDTO;
import ar.edu.utn.frc.tup.piii.judge.emitter.JudgeEventCodes;
import ar.edu.utn.frc.tup.piii.judge.events.DamageResolvedEvent;
import ar.edu.utn.frc.tup.piii.judge.events.JudgeEvent;
import ar.edu.utn.frc.tup.piii.judge.store.JudgeEventStore;
import ar.edu.utn.frc.tup.piii.judge.templates.JudgeTemplateRegistry;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
public class DamageExplanationResolver implements JudgeQueryResolver {

    private static final String RULE_REF_DAMAGE = "XY1 Rulebook — Damage Calculation";

    private final JudgeEventStore eventStore;
    private final JudgeTemplateRegistry templates;

    public DamageExplanationResolver(JudgeEventStore eventStore, JudgeTemplateRegistry templates) {
        this.eventStore = eventStore;
        this.templates = templates;
    }

    @Override
    public JudgeQueryCategory supports() {
        return JudgeQueryCategory.EXPLAIN_DAMAGE;
    }

    @Override
    public JudgeResponseDTO resolve(JudgeQueryDTO query, MatchStateDTO matchState,
                                     List<JudgeEvent> log) {
        String lang = effectiveLang(query.getLang());
        String engineMatchId = deriveEngineMatchId(log);

        Optional<DamageResolvedEvent> lastDamage = engineMatchId != null
                ? eventStore.getLastDamageEvent(engineMatchId)
                : findLastDamageInLog(log);

        if (lastDamage.isEmpty()) {
            return noDataResponse(query, lang, log != null ? log.size() : 0);
        }

        DamageResolvedEvent event = lastDamage.get();
        String attackerName = resolveAttackerName(matchState, event);
        String defenderName = resolveDefenderName(matchState);

        List<String> reasons = buildSteps(event, attackerName, defenderName, lang);
        String summary = buildSummary(event, attackerName, defenderName, lang);

        return JudgeResponseDTO.builder()
                .queryId(UUID.randomUUID().toString())
                .category(JudgeQueryCategory.EXPLAIN_DAMAGE)
                .summary(summary)
                .reasons(reasons)
                .ruleReferences(List.of(RULE_REF_DAMAGE))
                .suggestion(null)
                .hasActiveRestrictions(event.getFinalDamage() == 0)
                .turnNumber(log != null ? log.size() : 0)
                .lang(lang)
                .build();
    }

    private List<String> buildSteps(DamageResolvedEvent event, String attackerName,
                                     String defenderName, String lang) {
        List<String> steps = new ArrayList<>();

        if (event.getBaseDamage() != event.getPendingDamageAfterEffect()) {
            steps.add(format(lang,
                    "Base: {0} → Tras efecto: {1}",
                    "Base: {0} → After effect: {1}",
                    event.getBaseDamage(), event.getPendingDamageAfterEffect()));
        }

        if (event.isWeaknessIgnored()) {
            steps.add(lang.equals("en")
                    ? "Weakness was ignored by an attack effect."
                    : "La debilidad fue ignorada por un efecto del ataque.");
        } else if (event.isWeaknessApplied()) {
            steps.add(format(lang,
                    "Debilidad aplicada: {0} × 2 = {1}",
                    "Weakness applied: {0} × 2 = {1}",
                    event.getPendingDamageAfterEffect(), event.getCalculatedDamage()));
        }

        if (event.isResistanceIgnored()) {
            steps.add(lang.equals("en")
                    ? "Resistance was ignored by an attack effect."
                    : "La resistencia fue ignorada por un efecto del ataque.");
        } else if (event.isResistanceApplied()) {
            steps.add(format(lang,
                    "Resistencia aplicada: {0} − 20 = {1}",
                    "Resistance applied: {0} − 20 = {1}",
                    event.getCalculatedDamage() + 20, event.getCalculatedDamage()));
        }

        if (event.isPreventedByAllEffects()) {
            steps.add(lang.equals("en")
                    ? "All damage and effects prevented (preventAllEffects active)."
                    : "Todo el daño y los efectos fueron prevenidos (preventAllEffects activo).");
        } else if (event.isPreventedByAllDamage()) {
            steps.add(lang.equals("en")
                    ? "All incoming damage prevented (King's Shield or similar)."
                    : "Todo el daño entrante fue prevenido (King's Shield o similar).");
        } else if (event.isPreventedByThreshold()) {
            steps.add(format(lang,
                    "Daño cancelado: era {0}, no supera el umbral de protección {1}.",
                    "Damage cancelled: was {0}, does not exceed protection threshold {1}.",
                    event.getCalculatedDamage(), event.getThresholdValue()));
        } else if (event.getReductionApplied() > 0) {
            steps.add(format(lang,
                    "Reducción de daño aplicada: {0} − {1} = {2}.",
                    "Damage reduction applied: {0} − {1} = {2}.",
                    event.getCalculatedDamage(), event.getReductionApplied(), event.getFinalDamage()));
        }

        return steps;
    }

    private String buildSummary(DamageResolvedEvent event, String attackerName,
                                 String defenderName, String lang) {
        if (event.getFinalDamage() <= 0) {
            return templates.resolve(JudgeEventCodes.DAMAGE_FINAL_ZERO, lang,
                    attackerName, event.getAttackName(), defenderName);
        }
        return templates.resolve(JudgeEventCodes.DAMAGE_FINAL_APPLIED, lang,
                attackerName, event.getAttackName(), event.getFinalDamage(), defenderName,
                event.getBaseDamage(), event.getPendingDamageAfterEffect(), event.getCalculatedDamage());
    }

    private JudgeResponseDTO noDataResponse(JudgeQueryDTO query, String lang, int turnNumber) {
        String msg = lang.equals("en")
                ? "No attack has been recorded for this match yet."
                : "Todavia no se registro ningun ataque en esta partida.";
        return JudgeResponseDTO.builder()
                .queryId(UUID.randomUUID().toString())
                .category(JudgeQueryCategory.EXPLAIN_DAMAGE)
                .summary(msg)
                .reasons(List.of())
                .ruleReferences(List.of(RULE_REF_DAMAGE))
                .suggestion(null)
                .hasActiveRestrictions(false)
                .turnNumber(turnNumber)
                .lang(lang)
                .build();
    }

    private String resolveAttackerName(MatchStateDTO matchState, DamageResolvedEvent event) {
        if (matchState.getSelf() != null && matchState.getSelf().getActivePokemon() != null) {
            return matchState.getSelf().getActivePokemon().getName();
        }
        return event.getAttackerCardId();
    }

    private String resolveDefenderName(MatchStateDTO matchState) {
        if (matchState.getOpponent() != null
                && matchState.getOpponent().getActivePokemon() != null) {
            return matchState.getOpponent().getActivePokemon().getName();
        }
        return "?";
    }

    private String deriveEngineMatchId(List<JudgeEvent> log) {
        if (log == null || log.isEmpty()) {
            return null;
        }
        return log.get(0).getEngineMatchId();
    }

    private Optional<DamageResolvedEvent> findLastDamageInLog(List<JudgeEvent> log) {
        if (log == null) {
            return Optional.empty();
        }
        DamageResolvedEvent last = null;
        for (JudgeEvent event : log) {
            if (event instanceof DamageResolvedEvent d) {
                last = d;
            }
        }
        return Optional.ofNullable(last);
    }

    private String format(String lang, String esTemplate, String enTemplate, Object... params) {
        String template = lang.equals("en") ? enTemplate : esTemplate;
        for (int i = 0; i < params.length; i++) {
            template = template.replace("{" + i + "}", String.valueOf(params[i]));
        }
        return template;
    }

    private String effectiveLang(String lang) {
        return "en".equals(lang) ? "en" : "es";
    }
}
