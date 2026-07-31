package ar.edu.utn.frc.tup.piii.judge.resolvers;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import ar.edu.utn.frc.tup.piii.dtos.engine.MatchStateDTO;
import ar.edu.utn.frc.tup.piii.dtos.engine.MatchStateDTO.InPlayPokemonViewDTO;
import ar.edu.utn.frc.tup.piii.dtos.engine.MatchStateDTO.PlayerBattlefieldDTO;
import ar.edu.utn.frc.tup.piii.dtos.judge.JudgeQueryCategory;
import ar.edu.utn.frc.tup.piii.dtos.judge.JudgeQueryDTO;
import ar.edu.utn.frc.tup.piii.dtos.judge.JudgeResponseDTO;
import ar.edu.utn.frc.tup.piii.judge.events.DamageResolvedEvent;
import ar.edu.utn.frc.tup.piii.judge.events.DamageResolvedEvent.DamageResolvedEventBuilder;
import ar.edu.utn.frc.tup.piii.judge.events.JudgeEvent;
import ar.edu.utn.frc.tup.piii.judge.store.JudgeEventStore;
import ar.edu.utn.frc.tup.piii.judge.templates.JudgeTemplateRegistry;

/**
 * Unit tests for {@link DamageExplanationResolver} (judge category EXPLAIN_DAMAGE).
 *
 * <p>Most damage events carry a {@code null} engineMatchId so the resolver reads the last
 * damage from the supplied log; one test exercises the event-store path explicitly.
 */
class DamageExplanationResolverTest {

    private JudgeEventStore eventStore;
    private DamageExplanationResolver resolver;

    @BeforeEach
    void setUp() {
        eventStore = mock(JudgeEventStore.class);
        JudgeTemplateRegistry templates = new JudgeTemplateRegistry();
        templates.init();
        resolver = new DamageExplanationResolver(eventStore, templates);
    }

    @Test
    void supports_ReturnsExplainDamage() {
        assertThat(resolver.supports()).isEqualTo(JudgeQueryCategory.EXPLAIN_DAMAGE);
    }

    @Test
    void noDamageEvent_ReturnsNoDataResponse() {
        JudgeResponseDTO res = resolver.resolve(query("es"), matchState(), List.of());

        assertThat(res.getSummary()).contains("Todavia no se registro ningun ataque");
        assertThat(res.getReasons()).isEmpty();
        assertThat(res.isHasActiveRestrictions()).isFalse();
        assertThat(res.getCategory()).isEqualTo(JudgeQueryCategory.EXPLAIN_DAMAGE);
    }

    @Test
    void noDamageEvent_English_ReturnsEnglishNoData() {
        JudgeResponseDTO res = resolver.resolve(query("en"), matchState(), List.of());

        assertThat(res.getSummary()).contains("No attack has been recorded");
        assertThat(res.getLang()).isEqualTo("en");
    }

    @Test
    void damageApplied_BuildsAppliedSummaryWithAttackerAndDefender() {
        JudgeResponseDTO res = resolve("es", base().attackName("Flamethrower")
                .baseDamage(50).pendingDamageAfterEffect(50).calculatedDamage(50).finalDamage(50));

        assertThat(res.getSummary())
                .contains("Charizard").contains("Flamethrower").contains("de dano").contains("Blastoise");
        assertThat(res.isHasActiveRestrictions()).isFalse();
    }

    @Test
    void zeroDamage_BuildsZeroSummary_AndFlagsRestriction() {
        JudgeResponseDTO res = resolve("es", base().attackName("Splash").baseDamage(0).finalDamage(0));

        assertThat(res.getSummary()).contains("no recibio dano");
        assertThat(res.isHasActiveRestrictions()).isTrue();
    }

    @Test
    void baseDifferentFromAfterEffect_IncludesBaseStep() {
        JudgeResponseDTO res = resolve("es", base().baseDamage(50).pendingDamageAfterEffect(30).finalDamage(30));

        assertThat(res.getReasons()).anySatisfy(r -> assertThat(r).contains("Base: 50"));
    }

    @Test
    void weaknessApplied_IncludesWeaknessStep() {
        JudgeResponseDTO res = resolve("es", base()
                .baseDamage(50).pendingDamageAfterEffect(50).weaknessApplied(true)
                .calculatedDamage(100).finalDamage(100));

        assertThat(res.getReasons()).anySatisfy(r -> assertThat(r).contains("Debilidad aplicada"));
    }

    @Test
    void resistanceApplied_IncludesResistanceStep() {
        JudgeResponseDTO res = resolve("es", base()
                .baseDamage(50).pendingDamageAfterEffect(50).resistanceApplied(true)
                .calculatedDamage(30).finalDamage(30));

        assertThat(res.getReasons()).anySatisfy(r -> assertThat(r).contains("Resistencia aplicada"));
    }

    @Test
    void weaknessIgnored_NotedInSteps() {
        JudgeResponseDTO res = resolve("es", base().weaknessIgnored(true).finalDamage(40));

        assertThat(res.getReasons()).anySatisfy(r -> assertThat(r).contains("debilidad fue ignorada"));
    }

    @Test
    void resistanceIgnored_NotedInSteps() {
        JudgeResponseDTO res = resolve("es", base().resistanceIgnored(true).finalDamage(40));

        assertThat(res.getReasons()).anySatisfy(r -> assertThat(r).contains("resistencia fue ignorada"));
    }

    @Test
    void preventedByAllEffects_NotedInSteps() {
        JudgeResponseDTO res = resolve("es", base().preventedByAllEffects(true).finalDamage(0));

        assertThat(res.getReasons()).anySatisfy(r -> assertThat(r).contains("preventAllEffects"));
    }

    @Test
    void preventedByAllDamage_NotedInSteps() {
        JudgeResponseDTO res = resolve("es", base().preventedByAllDamage(true).finalDamage(0));

        assertThat(res.getReasons()).anySatisfy(r -> assertThat(r).contains("King's Shield"));
    }

    @Test
    void preventedByThreshold_NotedInSteps() {
        JudgeResponseDTO res = resolve("es", base()
                .preventedByThreshold(true).calculatedDamage(20).thresholdValue(30).finalDamage(0));

        assertThat(res.getReasons()).anySatisfy(r -> assertThat(r).contains("no supera el umbral"));
    }

    @Test
    void reductionApplied_NotedInSteps() {
        JudgeResponseDTO res = resolve("es", base()
                .reductionApplied(10).calculatedDamage(50).finalDamage(40));

        assertThat(res.getReasons()).anySatisfy(r -> assertThat(r).contains("aplicada: 50"));
    }

    @Test
    void englishLang_BuildsStepsInEnglish() {
        JudgeResponseDTO res = resolve("en", base()
                .baseDamage(50).pendingDamageAfterEffect(50).weaknessApplied(true)
                .calculatedDamage(100).finalDamage(100));

        assertThat(res.getReasons()).anySatisfy(r -> assertThat(r).contains("Weakness applied"));
        assertThat(res.getSummary()).isNotBlank();
    }

    @Test
    void damageReadFromEventStore_WhenEngineMatchIdPresent() {
        DamageResolvedEvent event = DamageResolvedEvent.builder()
                .engineMatchId("engine-1").attackName("Tackle")
                .baseDamage(20).pendingDamageAfterEffect(20).calculatedDamage(20).finalDamage(20)
                .build();
        when(eventStore.getLastDamageEvent("engine-1")).thenReturn(Optional.of(event));

        JudgeResponseDTO res = resolver.resolve(query("es"), matchState(), List.of(event));

        assertThat(res.getSummary()).contains("de dano").contains("Tackle");
    }

    @Test
    void damageEvent_NoMatchStateSides_FallsBackToCardIdAndUnknownDefender() {
        DamageResolvedEvent event = DamageResolvedEvent.builder()
                .engineMatchId(null).attackerCardId("xy1-7").attackName("Slash")
                .baseDamage(30).pendingDamageAfterEffect(30).calculatedDamage(30).finalDamage(30)
                .build();

        JudgeResponseDTO res = resolver.resolve(query("es"), new MatchStateDTO(), List.<JudgeEvent>of(event));

        assertThat(res.getSummary()).contains("xy1-7");
    }

    // ───────── helpers ─────────

    private JudgeResponseDTO resolve(String lang, DamageResolvedEventBuilder<?, ?> builder) {
        DamageResolvedEvent event = builder.engineMatchId(null).build();
        return resolver.resolve(query(lang), matchState(), List.<JudgeEvent>of(event));
    }

    /** A baseline damage event with a non-null attackName and final damage > 0 by default. */
    private DamageResolvedEventBuilder<?, ?> base() {
        return DamageResolvedEvent.builder().attackName("Flamethrower").finalDamage(40);
    }

    private JudgeQueryDTO query(String lang) {
        JudgeQueryDTO q = new JudgeQueryDTO();
        q.setMatchId("m1");
        q.setPlayerId("p1");
        q.setCategory(JudgeQueryCategory.EXPLAIN_DAMAGE);
        q.setLang(lang);
        return q;
    }

    private MatchStateDTO matchState() {
        MatchStateDTO state = new MatchStateDTO();
        state.setSelf(side("Charizard"));
        state.setOpponent(side("Blastoise"));
        return state;
    }

    private PlayerBattlefieldDTO side(String activeName) {
        PlayerBattlefieldDTO side = new PlayerBattlefieldDTO();
        InPlayPokemonViewDTO active = new InPlayPokemonViewDTO();
        active.setName(activeName);
        side.setActivePokemon(active);
        return side;
    }
}
