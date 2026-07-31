package ar.edu.utn.frc.tup.piii.judge.resolvers;

import ar.edu.utn.frc.tup.piii.dtos.engine.MatchStateDTO;
import ar.edu.utn.frc.tup.piii.dtos.judge.JudgeQueryCategory;
import ar.edu.utn.frc.tup.piii.dtos.judge.JudgeQueryDTO;
import ar.edu.utn.frc.tup.piii.dtos.judge.JudgeResponseDTO;
import ar.edu.utn.frc.tup.piii.judge.emitter.JudgeEventCodes;
import ar.edu.utn.frc.tup.piii.judge.events.JudgeEvent;
import ar.edu.utn.frc.tup.piii.judge.events.StatusAppliedEvent;
import ar.edu.utn.frc.tup.piii.judge.templates.JudgeTemplateRegistry;
import ar.edu.utn.frc.tup.piii.models.StatusCondition;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
public class StatusExplanationResolver implements JudgeQueryResolver {

    private static final String RULE_REF_STATUS = "XY1 Rulebook — Special Conditions";

    private final JudgeTemplateRegistry templates;

    public StatusExplanationResolver(JudgeTemplateRegistry templates) {
        this.templates = templates;
    }

    @Override
    public JudgeQueryCategory supports() {
        return JudgeQueryCategory.EXPLAIN_STATUS;
    }

    @Override
    public JudgeResponseDTO resolve(JudgeQueryDTO query, MatchStateDTO matchState,
                                     List<JudgeEvent> log) {
        String lang = effectiveLang(query.getLang());
        MatchStateDTO.InPlayPokemonViewDTO active = matchState.getSelf() != null
                ? matchState.getSelf().getActivePokemon() : null;
        String pokemonName = active != null ? active.getName() : "?";

        Optional<StatusAppliedEvent> lastEvent = findLastStatusEvent(log, query.getTargetPokemonId());

        List<String> reasons = buildReasons(active, lastEvent, lang, pokemonName);
        String summary = buildSummary(active, lastEvent, lang, pokemonName);

        return JudgeResponseDTO.builder()
                .queryId(UUID.randomUUID().toString())
                .category(JudgeQueryCategory.EXPLAIN_STATUS)
                .summary(summary)
                .reasons(reasons)
                .ruleReferences(List.of(RULE_REF_STATUS))
                .suggestion(buildRemovalHint(active, lang))
                .hasActiveRestrictions(hasActiveStatuses(active))
                .turnNumber(log != null ? log.size() : 0)
                .lang(lang)
                .build();
    }

    private List<String> buildReasons(MatchStateDTO.InPlayPokemonViewDTO active,
                                       Optional<StatusAppliedEvent> lastEvent,
                                       String lang, String pokemonName) {
        List<String> reasons = new ArrayList<>();

        if (lastEvent.isPresent()) {
            StatusAppliedEvent event = lastEvent.get();
            String source = event.getSourceAttackName() != null
                    ? event.getSourceAttackName()
                    : (lang.equals("en") ? "end of turn" : "fin de turno");
            reasons.add(templates.resolve(JudgeEventCodes.STATUS_APPLIED, lang,
                    pokemonName, localizeStatus(event.getStatus(), lang), source));
        }

        if (active != null && active.getStatuses() != null) {
            for (StatusCondition status : active.getStatuses()) {
                reasons.add(describeStatus(status, lang, pokemonName));
            }
        }

        if (reasons.isEmpty()) {
            reasons.add(lang.equals("en")
                    ? pokemonName + " has no active special conditions."
                    : pokemonName + " no tiene condiciones especiales activas.");
        }

        return reasons;
    }

    private String buildSummary(MatchStateDTO.InPlayPokemonViewDTO active,
                                 Optional<StatusAppliedEvent> lastEvent,
                                 String lang, String pokemonName) {
        if (!hasActiveStatuses(active)) {
            return lang.equals("en")
                    ? pokemonName + " has no active special conditions."
                    : pokemonName + " no tiene condiciones especiales activas.";
        }
        if (lastEvent.isPresent()) {
            StatusAppliedEvent event = lastEvent.get();
            String source = event.getSourceAttackName() != null
                    ? event.getSourceAttackName()
                    : (lang.equals("en") ? "end of turn" : "fin de turno");
            return templates.resolve(JudgeEventCodes.STATUS_APPLIED, lang,
                    pokemonName, localizeStatus(event.getStatus(), lang), source);
        }
        if (active != null && active.getStatuses() != null && !active.getStatuses().isEmpty()) {
            StatusCondition first = active.getStatuses().iterator().next();
            return lang.equals("en")
                    ? pokemonName + " currently has: " + localizeStatus(first, lang)
                    : pokemonName + " tiene actualmente: " + localizeStatus(first, lang);
        }
        return lang.equals("en") ? "No status information available." : "Sin informacion de estado.";
    }

    private String describeStatus(StatusCondition status, String lang, String pokemonName) {
        return switch (status) {
            case ASLEEP -> lang.equals("en")
                    ? pokemonName + " is Asleep: cannot attack or retreat. Flip coin at EOT to wake up."
                    : pokemonName + " esta Dormido: no puede atacar ni retirarse. Lanza moneda al final del turno para despertar.";
            case PARALYZED -> lang.equals("en")
                    ? pokemonName + " is Paralyzed: cannot attack or retreat. Removed at end of this turn."
                    : pokemonName + " esta Paralizado: no puede atacar ni retirarse. Se quita al final de este turno.";
            case POISONED -> lang.equals("en")
                    ? pokemonName + " is Poisoned: takes 10 damage at end of each turn."
                    : pokemonName + " esta Envenenado: recibe 10 de daño al final de cada turno.";
            case BURNED -> lang.equals("en")
                    ? pokemonName + " is Burned: flip coin at EOT — tails = 20 damage, then Burn is removed."
                    : pokemonName + " esta Quemado: lanza moneda al final del turno — cruz = 20 daño, luego se quita.";
            case CONFUSED -> lang.equals("en")
                    ? pokemonName + " is Confused: before attacking, flip coin — tails = 30 self-damage, attack fails."
                    : pokemonName + " esta Confundido: antes de atacar, lanza moneda — cruz = 30 de daño propio, el ataque falla.";
        };
    }

    private String buildRemovalHint(MatchStateDTO.InPlayPokemonViewDTO active, String lang) {
        if (!hasActiveStatuses(active)) {
            return null;
        }
        return lang.equals("en")
                ? "Retreating removes all special conditions from the retreating Pokemon."
                : "Retirarse elimina todas las condiciones especiales del Pokemon que se retira.";
    }

    private Optional<StatusAppliedEvent> findLastStatusEvent(List<JudgeEvent> log,
                                                               String targetPokemonId) {
        if (log == null || log.isEmpty()) {
            return Optional.empty();
        }
        StatusAppliedEvent last = null;
        for (JudgeEvent event : log) {
            if (event instanceof StatusAppliedEvent s && !s.isWasCured()) {
                if (targetPokemonId == null
                        || targetPokemonId.equals(s.getTargetPokemonId())) {
                    last = s;
                }
            }
        }
        return Optional.ofNullable(last);
    }

    private boolean hasActiveStatuses(MatchStateDTO.InPlayPokemonViewDTO active) {
        return active != null && active.getStatuses() != null && !active.getStatuses().isEmpty();
    }

    private String localizeStatus(StatusCondition status, String lang) {
        if (lang.equals("en")) {
            return switch (status) {
                case ASLEEP -> "Asleep";
                case PARALYZED -> "Paralyzed";
                case POISONED -> "Poisoned";
                case BURNED -> "Burned";
                case CONFUSED -> "Confused";
            };
        }
        return switch (status) {
            case ASLEEP -> "Dormido";
            case PARALYZED -> "Paralizado";
            case POISONED -> "Envenenado";
            case BURNED -> "Quemado";
            case CONFUSED -> "Confundido";
        };
    }

    private String effectiveLang(String lang) {
        return "en".equals(lang) ? "en" : "es";
    }
}
