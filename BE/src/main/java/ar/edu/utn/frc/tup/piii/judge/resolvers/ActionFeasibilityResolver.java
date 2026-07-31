package ar.edu.utn.frc.tup.piii.judge.resolvers;

import ar.edu.utn.frc.tup.piii.dtos.engine.GameActionType;
import ar.edu.utn.frc.tup.piii.dtos.engine.MatchStateDTO;
import ar.edu.utn.frc.tup.piii.dtos.judge.JudgeQueryCategory;
import ar.edu.utn.frc.tup.piii.dtos.judge.JudgeQueryDTO;
import ar.edu.utn.frc.tup.piii.dtos.judge.JudgeResponseDTO;
import ar.edu.utn.frc.tup.piii.judge.emitter.JudgeEventCodes;
import ar.edu.utn.frc.tup.piii.judge.events.JudgeEvent;
import ar.edu.utn.frc.tup.piii.judge.templates.JudgeTemplateRegistry;
import ar.edu.utn.frc.tup.piii.models.StatusCondition;
import ar.edu.utn.frc.tup.piii.models.TurnPhase;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
public class ActionFeasibilityResolver implements JudgeQueryResolver {

    private static final String RULE_REF_XY1 = "XY1 Rulebook";

    private final JudgeTemplateRegistry templates;

    public ActionFeasibilityResolver(JudgeTemplateRegistry templates) {
        this.templates = templates;
    }

    private int extractTurnNumber(MatchStateDTO matchState, List<JudgeEvent> log) {
        return log != null ? log.size() : 0;
    }

    @Override
    public JudgeQueryCategory supports() {
        return JudgeQueryCategory.WHY_CANT_I;
    }

    @Override
    public JudgeResponseDTO resolve(JudgeQueryDTO query, MatchStateDTO matchState,
                                     List<JudgeEvent> log) {
        String lang = effectiveLang(query.getLang());
        MatchStateDTO.InPlayPokemonViewDTO active = matchState.getSelf() != null
                ? matchState.getSelf().getActivePokemon() : null;
        String pokemonName = active != null ? active.getName() : "?";

        List<String> reasons = collectBlockReasons(query, matchState, active, lang, pokemonName);

        String summary = reasons.isEmpty()
                ? templates.resolve(JudgeEventCodes.ACTION_BLOCKED_WRONG_PHASE, lang, pokemonName)
                : reasons.get(0);

        return JudgeResponseDTO.builder()
                .queryId(UUID.randomUUID().toString())
                .category(query.getCategory() != null ? query.getCategory() : JudgeQueryCategory.WHY_CANT_I)
                .summary(summary)
                .reasons(reasons)
                .ruleReferences(List.of(RULE_REF_XY1))
                .suggestion(buildSuggestion(query.getTargetAction(), matchState, lang))
                .hasActiveRestrictions(!reasons.isEmpty())
                .turnNumber(log != null ? log.size() : 0)
                .lang(lang)
                .build();
    }

    private List<String> collectBlockReasons(JudgeQueryDTO query, MatchStateDTO matchState,
                                              MatchStateDTO.InPlayPokemonViewDTO active,
                                              String lang, String pokemonName) {
        GameActionType action = query.getTargetAction();
        if (action == null) {
            return listCurrentRestrictions(active, matchState, lang, pokemonName);
        }
        return switch (action) {
            case ATTACK -> attackBlockReasons(active, matchState, lang, pokemonName);
            case PLAY_SUPPORTER -> supporterBlockReasons(matchState, lang);
            case ATTACH_ENERGY -> energyBlockReasons(matchState, lang);
            case EVOLVE -> evolveBlockReasons(matchState, lang, pokemonName);
            case PLAY_BASIC -> playBasicBlockReasons(matchState, lang);
            default -> List.of(templates.resolve(
                    JudgeEventCodes.ACTION_BLOCKED_WRONG_PHASE, lang, action.name()));
        };
    }

    private List<String> attackBlockReasons(MatchStateDTO.InPlayPokemonViewDTO active,
                                             MatchStateDTO matchState, String lang,
                                             String pokemonName) {
        List<String> reasons = new ArrayList<>();
        if (matchState.getCurrentPhase() != TurnPhase.ATTACK) {
            reasons.add(templates.resolve(JudgeEventCodes.ACTION_BLOCKED_WRONG_PHASE, lang,
                    pokemonName));
        }
        boolean statusBlocked = false;
        if (active != null) {
            if (hasStatus(active, StatusCondition.ASLEEP)) {
                reasons.add(templates.resolve(
                        JudgeEventCodes.ACTION_BLOCKED_CANT_ATTACK_STATUS_SLEEP, lang, pokemonName));
                statusBlocked = true;
            }
            if (hasStatus(active, StatusCondition.PARALYZED)) {
                reasons.add(templates.resolve(
                        JudgeEventCodes.ACTION_BLOCKED_CANT_ATTACK_STATUS_PARALYZED, lang,
                        pokemonName));
                statusBlocked = true;
            }
        }
        // Energía insuficiente: si no está bloqueado por estado y no puede pagar
        // ningún ataque, informa cuánta energía le falta.
        if (active != null && !statusBlocked) {
            String energyReason = energyShortfallReason(active, lang, pokemonName);
            if (energyReason != null) {
                reasons.add(energyReason);
            }
        }
        return reasons;
    }

    /**
     * Si el Pokémon activo no puede pagar el costo de NINGUNO de sus ataques, devuelve
     * el mensaje de energía insuficiente (para el ataque más barato); en caso contrario
     * {@code null}. Replica {@code RuleValidator.canPayAttackCost} sobre la vista DTO.
     */
    private String energyShortfallReason(MatchStateDTO.InPlayPokemonViewDTO active,
                                         String lang, String pokemonName) {
        List<MatchStateDTO.AttackViewDTO> attacks = active.getAttacks();
        if (attacks == null || attacks.isEmpty()) {
            return null;
        }
        List<String> attached = active.getAttachedEnergyTypes();
        MatchStateDTO.AttackViewDTO cheapest = null;
        for (MatchStateDTO.AttackViewDTO attack : attacks) {
            if (canPayAttack(attached, attack.getCost())) {
                return null; // puede pagar al menos un ataque
            }
            if (cheapest == null || costSize(attack) < costSize(cheapest)) {
                cheapest = attack;
            }
        }
        return templates.resolve(JudgeEventCodes.ACTION_BLOCKED_CANT_ATTACK_ENERGY, lang,
                pokemonName, cheapest.getName(), costSize(cheapest), active.getAttachedEnergyCount());
    }

    /**
     * Determina si las energías adjuntas alcanzan para pagar el costo del ataque,
     * respetando los requisitos por tipo y el total (Colorless = cualquier tipo).
     */
    private boolean canPayAttack(List<String> attached, List<String> cost) {
        if (cost == null || cost.isEmpty()) {
            return true;
        }
        List<String> have = attached != null ? attached : List.of();
        Map<String, Integer> required = new HashMap<>();
        int totalRequired = 0;
        for (String type : cost) {
            if (type == null) {
                continue;
            }
            totalRequired++;
            if (!"COLORLESS".equals(type)) {
                required.merge(type, 1, Integer::sum);
            }
        }
        Map<String, Integer> available = new HashMap<>();
        for (String type : have) {
            if (type != null) {
                available.merge(type, 1, Integer::sum);
            }
        }
        for (Map.Entry<String, Integer> entry : required.entrySet()) {
            if (available.getOrDefault(entry.getKey(), 0) < entry.getValue()) {
                return false;
            }
        }
        return have.size() >= totalRequired;
    }

    private int costSize(MatchStateDTO.AttackViewDTO attack) {
        return attack.getCost() == null ? 0 : attack.getCost().size();
    }

    private List<String> supporterBlockReasons(MatchStateDTO matchState, String lang) {
        List<String> reasons = new ArrayList<>();
        if (matchState.getCurrentPhase() != TurnPhase.MAIN) {
            reasons.add(templates.resolve(JudgeEventCodes.ACTION_BLOCKED_WRONG_PHASE, lang, "Supporter"));
        }
        return reasons;
    }

    private List<String> energyBlockReasons(MatchStateDTO matchState, String lang) {
        List<String> reasons = new ArrayList<>();
        if (matchState.getCurrentPhase() != TurnPhase.MAIN) {
            reasons.add(templates.resolve(
                    JudgeEventCodes.ACTION_BLOCKED_WRONG_PHASE, lang, "Energy"));
        } else if (matchState.getSelf() != null && matchState.getSelf().isHasAttachedEnergyThisTurn()) {
            reasons.add(templates.resolve(
                    JudgeEventCodes.ACTION_BLOCKED_CANT_ATTACH_ENERGY_QUOTA, lang));
        }
        return reasons;
    }

    private List<String> evolveBlockReasons(MatchStateDTO matchState, String lang,
                                             String pokemonName) {
        List<String> reasons = new ArrayList<>();
        reasons.add(templates.resolve(
                JudgeEventCodes.ACTION_BLOCKED_CANT_EVOLVE_PLAYED_THIS_TURN, lang, pokemonName));
        return reasons;
    }

    private List<String> playBasicBlockReasons(MatchStateDTO matchState, String lang) {
        List<String> reasons = new ArrayList<>();
        int benchSize = matchState.getSelf() != null && matchState.getSelf().getBench() != null
                ? matchState.getSelf().getBench().size() : 0;
        if (benchSize >= 5) {
            reasons.add(templates.resolve(JudgeEventCodes.ACTION_BLOCKED_BENCH_FULL, lang));
        } else {
            reasons.add(templates.resolve(JudgeEventCodes.ACTION_BLOCKED_WRONG_PHASE, lang,
                    "Pokemon Basico"));
        }
        return reasons;
    }

    private List<String> listCurrentRestrictions(MatchStateDTO.InPlayPokemonViewDTO active,
                                                   MatchStateDTO matchState, String lang,
                                                   String pokemonName) {
        if (active == null) {
            return Collections.emptyList();
        }
        List<String> restrictions = new ArrayList<>();
        if (hasStatus(active, StatusCondition.ASLEEP)) {
            restrictions.add(templates.resolve(
                    JudgeEventCodes.ACTION_BLOCKED_CANT_ATTACK_STATUS_SLEEP, lang, pokemonName));
        }
        if (hasStatus(active, StatusCondition.PARALYZED)) {
            restrictions.add(templates.resolve(
                    JudgeEventCodes.ACTION_BLOCKED_CANT_ATTACK_STATUS_PARALYZED, lang, pokemonName));
        }
        return restrictions;
    }

    private String buildSuggestion(GameActionType action, MatchStateDTO matchState, String lang) {
        if (action == null) {
            return null;
        }
        TurnPhase phase = matchState.getCurrentPhase();
        return switch (action) {
            case ATTACK -> phase == TurnPhase.MAIN
                    ? (lang.equals("en") ? "Advance to the attack phase first."
                    : "Primero avanza a la fase de ataque.")
                    : null;
            default -> null;
        };
    }

    private boolean hasStatus(MatchStateDTO.InPlayPokemonViewDTO pokemon, StatusCondition status) {
        return pokemon.getStatuses() != null && pokemon.getStatuses().contains(status);
    }

    private String effectiveLang(String lang) {
        return "en".equals(lang) ? "en" : "es";
    }
}
