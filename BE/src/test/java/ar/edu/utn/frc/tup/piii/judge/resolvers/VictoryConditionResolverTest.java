package ar.edu.utn.frc.tup.piii.judge.resolvers;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import ar.edu.utn.frc.tup.piii.dtos.engine.MatchStateDTO;
import ar.edu.utn.frc.tup.piii.dtos.engine.MatchStateDTO.InPlayPokemonViewDTO;
import ar.edu.utn.frc.tup.piii.dtos.engine.MatchStateDTO.PlayerBattlefieldDTO;
import ar.edu.utn.frc.tup.piii.dtos.judge.JudgeQueryCategory;
import ar.edu.utn.frc.tup.piii.dtos.judge.JudgeQueryDTO;
import ar.edu.utn.frc.tup.piii.dtos.judge.JudgeResponseDTO;
import ar.edu.utn.frc.tup.piii.judge.emitter.JudgeEventCodes;
import ar.edu.utn.frc.tup.piii.judge.events.JudgeEvent;
import ar.edu.utn.frc.tup.piii.judge.events.VictoryEvent;
import ar.edu.utn.frc.tup.piii.judge.templates.JudgeTemplateRegistry;

/**
 * Unit tests for {@link VictoryConditionResolver} (judge category VICTORY_CONDITIONS).
 */
class VictoryConditionResolverTest {

    private VictoryConditionResolver resolver;

    @BeforeEach
    void setUp() {
        JudgeTemplateRegistry templates = new JudgeTemplateRegistry();
        templates.init();
        resolver = new VictoryConditionResolver(templates);
    }

    @Test
    void supports_ReturnsVictoryConditions() {
        assertThat(resolver.supports()).isEqualTo(JudgeQueryCategory.VICTORY_CONDITIONS);
    }

    // ───────── ongoing match (es) ─────────

    @Test
    void ongoingMatch_SummarizesRemainingPrizesAndListsConditions() {
        JudgeResponseDTO res = resolver.resolve(query("es"), ongoing(3, 4, 5), List.of());

        assertThat(res.getSummary()).contains("faltan 3");
        assertThat(res.getReasons()).hasSize(3);
        assertThat(res.isHasActiveRestrictions()).isFalse();
    }

    @Test
    void oneRemainingPrize_SuggestsFinishingMove() {
        JudgeResponseDTO res = resolver.resolve(query("es"), ongoing(1, 4, 5), List.of());

        assertThat(res.getSuggestion()).isNotNull().contains("Un KO mas");
    }

    @Test
    void opponentWithEmptyBenchAndActive_SuggestsImmediateKo() {
        JudgeResponseDTO res = resolver.resolve(query("es"), ongoing(2, 4, 0), List.of());

        assertThat(res.getSuggestion()).isNotNull().contains("no tiene Pokemon en el banco");
    }

    @Test
    void noSpecialSituation_HasNoSuggestion() {
        JudgeResponseDTO res = resolver.resolve(query("es"), ongoing(4, 4, 3), List.of());

        assertThat(res.getSuggestion()).isNull();
    }

    // ───────── ongoing match (en) ─────────

    @Test
    void ongoingMatch_English_SummaryAndConditionsInEnglish() {
        JudgeResponseDTO res = resolver.resolve(query("en"), ongoing(3, 4, 5), List.of());

        assertThat(res.getSummary()).contains("You need 3");
        assertThat(res.getReasons()).hasSize(3);
        assertThat(res.getLang()).isEqualTo("en");
    }

    @Test
    void oneRemainingPrize_English_SuggestsFinishingMove() {
        JudgeResponseDTO res = resolver.resolve(query("en"), ongoing(1, 4, 5), List.of());

        assertThat(res.getSuggestion()).isNotNull().contains("One more KO");
    }

    @Test
    void opponentEmptyBench_English_SuggestsImmediateKo() {
        JudgeResponseDTO res = resolver.resolve(query("en"), ongoing(2, 4, 0), List.of());

        assertThat(res.getSuggestion()).isNotNull().contains("Opponent has no bench");
    }

    // ───────── finished match ─────────

    @Test
    void finishedMatch_NoVictoryEvent_DefaultsToPrizesDepleted() {
        JudgeResponseDTO res = resolver.resolve(query("es"), finished(), List.of());

        assertThat(res.getSummary()).contains("gano la partida");
    }

    @Test
    void finishedMatch_DeckOutEvent_UsesDeckOutSummary() {
        JudgeResponseDTO res = resolver.resolve(query("es"), finished(),
                victoryLog(JudgeEventCodes.VICTORY_DECK_OUT));

        assertThat(res.getSummary()).contains("deck-out");
    }

    @Test
    void finishedMatch_NoPokemonRemainingEvent_UsesThatSummary() {
        JudgeResponseDTO res = resolver.resolve(query("es"), finished(),
                victoryLog(JudgeEventCodes.VICTORY_NO_POKEMON_REMAINING));

        assertThat(res.getSummary()).contains("no tiene mas Pokemon");
    }

    @Test
    void finishedMatch_English_ReturnsEnglishSummary() {
        JudgeResponseDTO res = resolver.resolve(query("en"), finished(), List.of());

        assertThat(res.getSummary()).isNotBlank();
        assertThat(res.getLang()).isEqualTo("en");
        assertThat(res.getReasons()).hasSize(1);
    }

    @Test
    void emptyState_NullSides_UseDefaults() {
        JudgeResponseDTO res = resolver.resolve(query("es"), new MatchStateDTO(), List.of());

        assertThat(res.getReasons()).hasSize(3);
        assertThat(res.getSummary()).contains("faltan 0");
        assertThat(res.getSuggestion()).isNull();
    }

    // ───────── helpers ─────────

    private JudgeQueryDTO query(String lang) {
        JudgeQueryDTO q = new JudgeQueryDTO();
        q.setMatchId("m1");
        q.setPlayerId("p1");
        q.setCategory(JudgeQueryCategory.VICTORY_CONDITIONS);
        q.setLang(lang);
        return q;
    }

    /**
     * Builds an ongoing match where the player has {@code selfPrizes} prizes left and the
     * opponent has {@code opponentPrizes}, {@code opponentBenchSize} benched Pokémon and an active.
     */
    private MatchStateDTO ongoing(int selfPrizes, int opponentPrizes, int opponentBenchSize) {
        MatchStateDTO state = new MatchStateDTO();

        PlayerBattlefieldDTO self = new PlayerBattlefieldDTO();
        self.setRemainingPrizes(selfPrizes);
        state.setSelf(self);

        PlayerBattlefieldDTO opponent = new PlayerBattlefieldDTO();
        opponent.setRemainingPrizes(opponentPrizes);
        opponent.setActivePokemon(pokemon("Rival"));
        opponent.setBench(new ArrayList<>());
        for (int i = 0; i < opponentBenchSize; i++) {
            opponent.getBench().add(pokemon("Bench" + i));
        }
        state.setOpponent(opponent);

        return state;
    }

    private MatchStateDTO finished() {
        MatchStateDTO state = ongoing(0, 6, 3);
        state.setFinished(true);
        state.setWinnerPlayerId("p1");
        return state;
    }

    private List<JudgeEvent> victoryLog(String reason) {
        return List.of(VictoryEvent.builder()
                .engineMatchId("engine-1")
                .winnerPlayerId("p1")
                .victoryReason(reason)
                .build());
    }

    private InPlayPokemonViewDTO pokemon(String name) {
        InPlayPokemonViewDTO p = new InPlayPokemonViewDTO();
        p.setName(name);
        return p;
    }
}
