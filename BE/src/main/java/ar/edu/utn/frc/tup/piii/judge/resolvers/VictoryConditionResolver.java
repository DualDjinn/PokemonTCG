package ar.edu.utn.frc.tup.piii.judge.resolvers;

import ar.edu.utn.frc.tup.piii.dtos.engine.MatchStateDTO;
import ar.edu.utn.frc.tup.piii.dtos.judge.JudgeQueryCategory;
import ar.edu.utn.frc.tup.piii.dtos.judge.JudgeQueryDTO;
import ar.edu.utn.frc.tup.piii.dtos.judge.JudgeResponseDTO;
import ar.edu.utn.frc.tup.piii.judge.emitter.JudgeEventCodes;
import ar.edu.utn.frc.tup.piii.judge.events.JudgeEvent;
import ar.edu.utn.frc.tup.piii.judge.events.VictoryEvent;
import ar.edu.utn.frc.tup.piii.judge.templates.JudgeTemplateRegistry;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
public class VictoryConditionResolver implements JudgeQueryResolver {

    private static final String RULE_REF_VICTORY = "XY1 Rulebook — Winning the Game";

    private final JudgeTemplateRegistry templates;

    public VictoryConditionResolver(JudgeTemplateRegistry templates) {
        this.templates = templates;
    }

    @Override
    public JudgeQueryCategory supports() {
        return JudgeQueryCategory.VICTORY_CONDITIONS;
    }

    @Override
    public JudgeResponseDTO resolve(JudgeQueryDTO query, MatchStateDTO matchState,
                                     List<JudgeEvent> log) {
        String lang = effectiveLang(query.getLang());

        if (matchState.isFinished()) {
            return finishedMatchResponse(query, matchState, log, lang);
        }

        List<String> reasons = buildConditions(matchState, lang);
        String summary = buildSummary(matchState, lang);

        return JudgeResponseDTO.builder()
                .queryId(UUID.randomUUID().toString())
                .category(JudgeQueryCategory.VICTORY_CONDITIONS)
                .summary(summary)
                .reasons(reasons)
                .ruleReferences(List.of(RULE_REF_VICTORY))
                .suggestion(buildNextStep(matchState, lang))
                .hasActiveRestrictions(false)
                .turnNumber(log != null ? log.size() : 0)
                .lang(lang)
                .build();
    }

    private List<String> buildConditions(MatchStateDTO matchState, String lang) {
        List<String> conditions = new ArrayList<>();
        MatchStateDTO.PlayerBattlefieldDTO self = matchState.getSelf();
        MatchStateDTO.PlayerBattlefieldDTO opponent = matchState.getOpponent();

        int selfPrizes = self != null ? self.getRemainingPrizes() : 0;
        int opponentPrizes = opponent != null ? opponent.getRemainingPrizes() : 0;
        int opponentBench = opponent != null && opponent.getBench() != null
                ? opponent.getBench().size() : 0;
        boolean opponentHasActive = opponent != null && opponent.getActivePokemon() != null;

        conditions.add(format(lang,
                "Tus premios restantes: {0}. Premios del rival: {1}.",
                "Your remaining prizes: {0}. Opponent's prizes: {1}.",
                selfPrizes, opponentPrizes));

        conditions.add(format(lang,
                "Banco del rival: {0} Pokemon. Activo del rival: {1}.",
                "Opponent's bench: {0} Pokemon. Opponent's active: {1}.",
                opponentBench,
                opponentHasActive && opponent != null ? (opponent.getActivePokemon().getName()) : "none"));

        conditions.add(format(lang,
                "Cómo ganar: (1) tomar todos tus premios, (2) KO rival sin reemplazo, (3) rival sin cartas.",
                "How to win: (1) take all your prize cards, (2) KO opponent with no replacement, "
                        + "(3) opponent cannot draw at start of their turn."));

        return conditions;
    }

    private String buildSummary(MatchStateDTO matchState, String lang) {
        MatchStateDTO.PlayerBattlefieldDTO self = matchState.getSelf();
        int prizes = self != null ? self.getRemainingPrizes() : 0;
        return format(lang,
                "Te faltan {0} premio(s) para ganar.",
                "You need {0} more prize card(s) to win.",
                prizes);
    }

    private String buildNextStep(MatchStateDTO matchState, String lang) {
        MatchStateDTO.PlayerBattlefieldDTO self = matchState.getSelf();
        MatchStateDTO.PlayerBattlefieldDTO opponent = matchState.getOpponent();
        int selfPrizes = self != null ? self.getRemainingPrizes() : 0;
        int opponentBench = opponent != null && opponent.getBench() != null
                ? opponent.getBench().size() : 99;

        if (selfPrizes == 1) {
            return lang.equals("en")
                    ? "One more KO and you win by taking your last prize!"
                    : "Un KO mas y ganas tomando tu ultimo premio.";
        }
        if (opponentBench == 0 && opponent != null && opponent.getActivePokemon() != null) {
            return lang.equals("en")
                    ? "Opponent has no bench Pokemon — a KO would win the match immediately!"
                    : "El rival no tiene Pokemon en el banco. Un KO gana la partida de inmediato.";
        }
        return null;
    }

    private JudgeResponseDTO finishedMatchResponse(JudgeQueryDTO query, MatchStateDTO matchState,
                                                     List<JudgeEvent> log, String lang) {
        String winnerName = matchState.getWinnerPlayerId() != null
                ? matchState.getWinnerPlayerId() : "?";
        String reason = findVictoryReason(log);
        String summary = buildVictorySummary(reason, winnerName, lang);

        return JudgeResponseDTO.builder()
                .queryId(UUID.randomUUID().toString())
                .category(JudgeQueryCategory.VICTORY_CONDITIONS)
                .summary(summary)
                .reasons(List.of(summary))
                .ruleReferences(List.of(RULE_REF_VICTORY))
                .suggestion(null)
                .hasActiveRestrictions(false)
                .turnNumber(log != null ? log.size() : 0)
                .lang(lang)
                .build();
    }

    private String buildVictorySummary(String reason, String winnerName, String lang) {
        if (reason == null) {
            return templates.resolve(JudgeEventCodes.VICTORY_PRIZES_DEPLETED, lang, winnerName);
        }
        return switch (reason) {
            case JudgeEventCodes.VICTORY_DECK_OUT ->
                    templates.resolve(JudgeEventCodes.VICTORY_DECK_OUT, lang, winnerName);
            case JudgeEventCodes.VICTORY_NO_POKEMON_REMAINING ->
                    templates.resolve(JudgeEventCodes.VICTORY_NO_POKEMON_REMAINING, lang, winnerName);
            default -> templates.resolve(JudgeEventCodes.VICTORY_PRIZES_DEPLETED, lang, winnerName);
        };
    }

    private String findVictoryReason(List<JudgeEvent> log) {
        if (log == null) {
            return null;
        }
        for (int i = log.size() - 1; i >= 0; i--) {
            if (log.get(i) instanceof VictoryEvent v) {
                return v.getVictoryReason();
            }
        }
        return null;
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
