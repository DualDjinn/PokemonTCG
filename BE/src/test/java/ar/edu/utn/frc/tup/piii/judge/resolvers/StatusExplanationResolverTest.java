package ar.edu.utn.frc.tup.piii.judge.resolvers;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import ar.edu.utn.frc.tup.piii.dtos.engine.MatchStateDTO;
import ar.edu.utn.frc.tup.piii.dtos.engine.MatchStateDTO.InPlayPokemonViewDTO;
import ar.edu.utn.frc.tup.piii.dtos.engine.MatchStateDTO.PlayerBattlefieldDTO;
import ar.edu.utn.frc.tup.piii.dtos.judge.JudgeQueryCategory;
import ar.edu.utn.frc.tup.piii.dtos.judge.JudgeQueryDTO;
import ar.edu.utn.frc.tup.piii.dtos.judge.JudgeResponseDTO;
import ar.edu.utn.frc.tup.piii.judge.events.JudgeEvent;
import ar.edu.utn.frc.tup.piii.judge.events.StatusAppliedEvent;
import ar.edu.utn.frc.tup.piii.judge.templates.JudgeTemplateRegistry;
import ar.edu.utn.frc.tup.piii.models.StatusCondition;

/**
 * Unit tests for {@link StatusExplanationResolver} (judge category EXPLAIN_STATUS).
 */
class StatusExplanationResolverTest {

    private StatusExplanationResolver resolver;

    @BeforeEach
    void setUp() {
        JudgeTemplateRegistry templates = new JudgeTemplateRegistry();
        templates.init();
        resolver = new StatusExplanationResolver(templates);
    }

    @Test
    void supports_ReturnsExplainStatus() {
        assertThat(resolver.supports()).isEqualTo(JudgeQueryCategory.EXPLAIN_STATUS);
    }

    @Test
    void asleepActive_DescribesSleep_AndOffersRemovalHint() {
        MatchStateDTO state = stateWithActive(active("Snorlax", Set.of(StatusCondition.ASLEEP)));

        JudgeResponseDTO res = resolver.resolve(query("es"), state, List.of());

        assertThat(res.getReasons()).anySatisfy(r -> assertThat(r).contains("Dormido"));
        assertThat(res.isHasActiveRestrictions()).isTrue();
        assertThat(res.getSuggestion()).isNotNull().contains("Retirarse");
    }

    @Test
    void noStatuses_SummarizesNone_AndHasNoSuggestion() {
        MatchStateDTO state = stateWithActive(active("Pikachu", Set.of()));

        JudgeResponseDTO res = resolver.resolve(query("es"), state, List.of());

        assertThat(res.getSummary()).contains("no tiene condiciones especiales activas");
        assertThat(res.isHasActiveRestrictions()).isFalse();
        assertThat(res.getSuggestion()).isNull();
    }

    @Test
    void multipleStatuses_DescribesEachCondition() {
        MatchStateDTO state = stateWithActive(
                active("Gengar", Set.of(StatusCondition.POISONED, StatusCondition.CONFUSED)));

        JudgeResponseDTO res = resolver.resolve(query("es"), state, List.of());

        assertThat(res.getReasons()).anySatisfy(r -> assertThat(r).contains("Envenenado"));
        assertThat(res.getReasons()).anySatisfy(r -> assertThat(r).contains("Confundido"));
    }

    @Test
    void statusEventInLog_SummaryUsesAppliedTemplate() {
        MatchStateDTO state = stateWithActive(active("Snorlax", Set.of(StatusCondition.ASLEEP)));
        List<JudgeEvent> log = List.of(statusEvent(StatusCondition.ASLEEP, "Hypnosis"));

        JudgeResponseDTO res = resolver.resolve(query("es"), state, log);

        assertThat(res.getSummary())
                .contains("ahora tiene el estado").contains("Dormido").contains("Hypnosis");
    }

    @Test
    void statusEventWithoutSource_UsesEndOfTurnAsSource() {
        MatchStateDTO state = stateWithActive(active("Muk", Set.of(StatusCondition.POISONED)));
        List<JudgeEvent> log = List.of(statusEvent(StatusCondition.POISONED, null));

        JudgeResponseDTO res = resolver.resolve(query("es"), state, log);

        assertThat(res.getSummary()).contains("fin de turno");
    }

    @Test
    void noActivePokemon_ReportsNoConditions() {
        MatchStateDTO state = stateWithActive(null);

        JudgeResponseDTO res = resolver.resolve(query("es"), state, List.of());

        assertThat(res.getSummary()).contains("no tiene condiciones especiales activas");
        assertThat(res.isHasActiveRestrictions()).isFalse();
    }

    @Test
    void englishActive_DescribesInEnglish_AndRemovalHint() {
        MatchStateDTO state = stateWithActive(active("Charmander", Set.of(StatusCondition.BURNED)));

        JudgeResponseDTO res = resolver.resolve(query("en"), state, List.of());

        assertThat(res.getReasons()).anySatisfy(r -> assertThat(r).contains("Burned"));
        assertThat(res.getSuggestion()).isNotNull().contains("Retreating");
    }

    /** Covers every {@code describeStatus} / {@code localizeStatus} arm in both languages. */
    @Test
    void allConditionsBothLanguages_ProduceReasonsAndSummary() {
        for (String lang : List.of("es", "en")) {
            for (StatusCondition condition : StatusCondition.values()) {
                MatchStateDTO state = stateWithActive(active("Mon", Set.of(condition)));
                List<JudgeEvent> log = List.of(statusEvent(condition, "Move"));

                JudgeResponseDTO res = resolver.resolve(query(lang), state, log);

                assertThat(res.getReasons()).as("%s/%s reasons", lang, condition).isNotEmpty();
                assertThat(res.getSummary()).as("%s/%s summary", lang, condition).isNotBlank();
                assertThat(res.isHasActiveRestrictions()).isTrue();
            }
        }
    }

    @Test
    void statusEvent_DifferentTargetPokemon_IsIgnored() {
        MatchStateDTO state = stateWithActive(active("Snorlax", Set.of(StatusCondition.ASLEEP)));
        JudgeQueryDTO q = query("es");
        q.setTargetPokemonId("snorlax-1");
        List<JudgeEvent> log = List.of(StatusAppliedEvent.builder()
                .engineMatchId("engine-1").status(StatusCondition.PARALYZED)
                .targetPokemonId("other-mon").sourceAttackName("Move").wasCured(false).build());

        JudgeResponseDTO res = resolver.resolve(q, state, log);

        // The event targets a different Pokémon → summary falls back to the active status.
        assertThat(res.getSummary()).contains("tiene actualmente");
    }

    // ───────── helpers ─────────

    private JudgeQueryDTO query(String lang) {
        JudgeQueryDTO q = new JudgeQueryDTO();
        q.setMatchId("m1");
        q.setPlayerId("p1");
        q.setCategory(JudgeQueryCategory.EXPLAIN_STATUS);
        q.setLang(lang);
        return q;
    }

    private JudgeEvent statusEvent(StatusCondition condition, String source) {
        return StatusAppliedEvent.builder()
                .engineMatchId("engine-1")
                .targetPokemonName("Mon")
                .status(condition)
                .sourceAttackName(source)
                .wasCured(false)
                .build();
    }

    private MatchStateDTO stateWithActive(InPlayPokemonViewDTO active) {
        MatchStateDTO state = new MatchStateDTO();
        PlayerBattlefieldDTO self = new PlayerBattlefieldDTO();
        self.setActivePokemon(active);
        state.setSelf(self);
        return state;
    }

    private InPlayPokemonViewDTO active(String name, Set<StatusCondition> statuses) {
        InPlayPokemonViewDTO p = new InPlayPokemonViewDTO();
        p.setName(name);
        p.setStatuses(statuses);
        return p;
    }
}
