package ar.edu.utn.frc.tup.piii.judge.resolvers;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import ar.edu.utn.frc.tup.piii.dtos.engine.GameActionType;
import ar.edu.utn.frc.tup.piii.dtos.engine.MatchStateDTO;
import ar.edu.utn.frc.tup.piii.dtos.engine.MatchStateDTO.AttackViewDTO;
import ar.edu.utn.frc.tup.piii.dtos.engine.MatchStateDTO.InPlayPokemonViewDTO;
import ar.edu.utn.frc.tup.piii.dtos.engine.MatchStateDTO.PlayerBattlefieldDTO;
import ar.edu.utn.frc.tup.piii.dtos.judge.JudgeQueryCategory;
import ar.edu.utn.frc.tup.piii.dtos.judge.JudgeQueryDTO;
import ar.edu.utn.frc.tup.piii.dtos.judge.JudgeResponseDTO;
import ar.edu.utn.frc.tup.piii.judge.events.JudgeEvent;
import ar.edu.utn.frc.tup.piii.judge.events.StatusAppliedEvent;
import ar.edu.utn.frc.tup.piii.judge.templates.JudgeTemplateRegistry;
import ar.edu.utn.frc.tup.piii.models.StatusCondition;
import ar.edu.utn.frc.tup.piii.models.TurnPhase;

/**
 * Unit tests for {@link ActionFeasibilityResolver} (judge category WHY_CANT_I /
 * CURRENT_RESTRICTIONS). Uses a real {@link JudgeTemplateRegistry} (a deterministic,
 * dependency-free collaborator) so the assertions exercise the real Spanish templates.
 *
 * <p>Special focus on the energy-shortfall detection added to the ATTACK branch, which
 * mirrors {@code RuleValidator.canPayAttackCost} over the projected match-state DTO.
 */
class ActionFeasibilityResolverTest {

    private ActionFeasibilityResolver resolver;

    @BeforeEach
    void setUp() {
        JudgeTemplateRegistry templates = new JudgeTemplateRegistry();
        templates.init();
        resolver = new ActionFeasibilityResolver(templates);
    }

    // ───────── supports ─────────

    @Test
    void supports_ReturnsWhyCantI() {
        assertThat(resolver.supports()).isEqualTo(JudgeQueryCategory.WHY_CANT_I);
    }

    // ───────── ATTACK — phase ─────────

    @Test
    void attack_WrongPhase_ReturnsWrongPhaseReason() {
        InPlayPokemonViewDTO active = pokemon("Pikachu", noStatus(),
                List.of(attack("Bolt", List.of("LIGHTNING"))), List.of("LIGHTNING"));
        MatchStateDTO state = stateWith(TurnPhase.MAIN, active);

        JudgeResponseDTO res = resolve(GameActionType.ATTACK, state);

        assertThat(res.getReasons()).isNotEmpty();
        assertThat(res.getSummary()).contains("fase del turno");
        assertThat(res.isHasActiveRestrictions()).isTrue();
    }

    // ───────── ATTACK — special conditions ─────────

    @Test
    void attack_Asleep_ReturnsSleepReason_AndSkipsEnergyCheck() {
        // Asleep + no energy: only the status reason must surface (energy is moot).
        InPlayPokemonViewDTO active = pokemon("Snorlax", Set.of(StatusCondition.ASLEEP),
                List.of(attack("Body Slam", List.of("COLORLESS", "COLORLESS"))), List.of());
        MatchStateDTO state = stateWith(TurnPhase.ATTACK, active);

        JudgeResponseDTO res = resolve(GameActionType.ATTACK, state);

        assertThat(res.getReasons()).hasSize(1);
        assertThat(res.getReasons().get(0)).contains("dormido");
        assertThat(res.getReasons().get(0)).doesNotContain("suficiente energia");
    }

    @Test
    void attack_Paralyzed_ReturnsParalyzedReason() {
        InPlayPokemonViewDTO active = pokemon("Magnemite", Set.of(StatusCondition.PARALYZED),
                List.of(attack("Tackle", List.of("COLORLESS"))), List.of("METAL"));
        MatchStateDTO state = stateWith(TurnPhase.ATTACK, active);

        JudgeResponseDTO res = resolve(GameActionType.ATTACK, state);

        assertThat(res.getReasons().get(0)).contains("paralizado");
    }

    // ───────── ATTACK — energy (the feature under test) ─────────

    @Test
    void attack_InsufficientTypedEnergy_ReturnsEnergyReasonWithCounts() {
        // Fire Blast needs 2 Fire; only 1 Fire attached → not payable.
        InPlayPokemonViewDTO active = pokemon("Charizard", noStatus(),
                List.of(attack("Fire Blast", List.of("FIRE", "FIRE"))), List.of("FIRE"));
        MatchStateDTO state = stateWith(TurnPhase.ATTACK, active);

        JudgeResponseDTO res = resolve(GameActionType.ATTACK, state);

        assertThat(res.getSummary())
                .contains("suficiente energia")
                .contains("Fire Blast")
                .contains("requiere 2")
                .contains("solo tiene 1");
        assertThat(res.isHasActiveRestrictions()).isTrue();
    }

    @Test
    void attack_EnoughEnergy_NoReasons() {
        InPlayPokemonViewDTO active = pokemon("Charizard", noStatus(),
                List.of(attack("Fire Blast", List.of("FIRE", "FIRE"))), List.of("FIRE", "FIRE"));
        MatchStateDTO state = stateWith(TurnPhase.ATTACK, active);

        JudgeResponseDTO res = resolve(GameActionType.ATTACK, state);

        assertThat(res.getReasons()).isEmpty();
        assertThat(res.isHasActiveRestrictions()).isFalse();
    }

    @Test
    void attack_ColorlessCost_PaidByAnyEnergyType() {
        // 2 Colorless can be paid by any two energies.
        InPlayPokemonViewDTO active = pokemon("Snorlax", noStatus(),
                List.of(attack("Body Slam", List.of("COLORLESS", "COLORLESS"))),
                List.of("WATER", "GRASS"));
        MatchStateDTO state = stateWith(TurnPhase.ATTACK, active);

        JudgeResponseDTO res = resolve(GameActionType.ATTACK, state);

        assertThat(res.getReasons()).isEmpty();
    }

    @Test
    void attack_TypeMismatchEvenWithEnoughTotal_ReturnsEnergyReason() {
        // Ember needs 1 Fire; a single Water does not satisfy the typed requirement.
        InPlayPokemonViewDTO active = pokemon("Vulpix", noStatus(),
                List.of(attack("Ember", List.of("FIRE"))), List.of("WATER"));
        MatchStateDTO state = stateWith(TurnPhase.ATTACK, active);

        JudgeResponseDTO res = resolve(GameActionType.ATTACK, state);

        assertThat(res.getSummary()).contains("suficiente energia");
    }

    @Test
    void attack_NoAttacks_DoesNotReportEnergy() {
        InPlayPokemonViewDTO active = pokemon("Ditto", noStatus(), List.of(), List.of("COLORLESS"));
        MatchStateDTO state = stateWith(TurnPhase.ATTACK, active);

        JudgeResponseDTO res = resolve(GameActionType.ATTACK, state);

        assertThat(res.getReasons()).isEmpty();
    }

    @Test
    void attack_NoneAffordable_PicksCheapestAttackForTheMessage() {
        // Two unpayable attacks; the cheapest (1 energy) drives the "requiere N" message.
        InPlayPokemonViewDTO active = pokemon("Gyarados", noStatus(),
                List.of(attack("Splash", List.of("WATER")),
                        attack("Hyper Beam", List.of("WATER", "WATER", "WATER"))),
                List.of());
        MatchStateDTO state = stateWith(TurnPhase.ATTACK, active);

        JudgeResponseDTO res = resolve(GameActionType.ATTACK, state);

        assertThat(res.getSummary()).contains("requiere 1").contains("Splash");
    }

    // ───────── other actions ─────────

    @Test
    void supporter_WrongPhase_ReturnsWrongPhase() {
        MatchStateDTO state = stateWith(TurnPhase.ATTACK, pokemon("X", noStatus(), List.of(), List.of()));

        JudgeResponseDTO res = resolve(GameActionType.PLAY_SUPPORTER, state);

        assertThat(res.getSummary()).contains("fase del turno");
    }

    @Test
    void attachEnergy_AlreadyAttachedThisTurn_ReturnsQuotaReason() {
        MatchStateDTO state = stateWith(TurnPhase.MAIN, pokemon("X", noStatus(), List.of(), List.of()));
        state.getSelf().setHasAttachedEnergyThisTurn(true);

        JudgeResponseDTO res = resolve(GameActionType.ATTACH_ENERGY, state);

        assertThat(res.getSummary()).contains("energia manual");
    }

    @Test
    void playBasic_BenchFull_ReturnsBenchFull() {
        MatchStateDTO state = stateWith(TurnPhase.MAIN, pokemon("X", noStatus(), List.of(), List.of()));
        List<InPlayPokemonViewDTO> bench = new ArrayList<>();
        for (int i = 0; i < 5; i++) {
            bench.add(pokemon("B" + i, noStatus(), List.of(), List.of()));
        }
        state.getSelf().setBench(bench);

        JudgeResponseDTO res = resolve(GameActionType.PLAY_BASIC, state);

        assertThat(res.getSummary()).contains("banco esta lleno");
    }

    @Test
    void evolve_ReturnsPlayedThisTurnReason() {
        MatchStateDTO state = stateWith(TurnPhase.MAIN, pokemon("Charmander", noStatus(), List.of(), List.of()));

        JudgeResponseDTO res = resolve(GameActionType.EVOLVE, state);

        assertThat(res.getSummary()).contains("entro en juego este turno");
    }

    // ───────── CURRENT_RESTRICTIONS (no target action) ─────────

    @Test
    void currentRestrictions_AsleepActive_ListsRestrictions() {
        InPlayPokemonViewDTO active = pokemon("Snorlax", Set.of(StatusCondition.ASLEEP), List.of(), List.of());
        MatchStateDTO state = stateWith(TurnPhase.MAIN, active);

        JudgeQueryDTO q = query(JudgeQueryCategory.CURRENT_RESTRICTIONS, null);
        JudgeResponseDTO res = resolver.resolve(q, state, List.of());

        assertThat(res.getReasons()).anySatisfy(r -> assertThat(r).contains("dormido"));
        assertThat(res.isHasActiveRestrictions()).isTrue();
    }

    @Test
    void currentRestrictions_NoActive_ReturnsEmpty() {
        MatchStateDTO state = stateWith(TurnPhase.MAIN, null);

        JudgeQueryDTO q = query(JudgeQueryCategory.CURRENT_RESTRICTIONS, null);
        JudgeResponseDTO res = resolver.resolve(q, state, List.of());

        assertThat(res.getReasons()).isEmpty();
        assertThat(res.isHasActiveRestrictions()).isFalse();
    }

    // ───────── suggestion ─────────

    @Test
    void suggestion_AttackInMainPhase_SuggestsAdvancing() {
        MatchStateDTO state = stateWith(TurnPhase.MAIN, pokemon("X", noStatus(), List.of(), List.of()));

        JudgeResponseDTO res = resolve(GameActionType.ATTACK, state);

        assertThat(res.getSuggestion()).isNotNull().contains("fase de ataque");
    }

    @Test
    void suggestion_AttackInAttackPhase_IsNull() {
        InPlayPokemonViewDTO active = pokemon("X", noStatus(),
                List.of(attack("A", List.of("COLORLESS"))), List.of("FIRE"));
        MatchStateDTO state = stateWith(TurnPhase.ATTACK, active);

        JudgeResponseDTO res = resolve(GameActionType.ATTACK, state);

        assertThat(res.getSuggestion()).isNull();
    }

    // ───────── metadata ─────────

    @Test
    void response_CarriesCategoryRuleRefAndTurnNumberFromLogSize() {
        MatchStateDTO state = stateWith(TurnPhase.MAIN, pokemon("X", noStatus(), List.of(), List.of()));
        List<JudgeEvent> log = List.of(statusEvent(), statusEvent());

        JudgeResponseDTO res = resolver.resolve(query(JudgeQueryCategory.WHY_CANT_I, GameActionType.ATTACK),
                state, log);

        assertThat(res.getCategory()).isEqualTo(JudgeQueryCategory.WHY_CANT_I);
        assertThat(res.getRuleReferences()).contains("XY1 Rulebook");
        assertThat(res.getTurnNumber()).isEqualTo(2);
        assertThat(res.getLang()).isEqualTo("es");
        assertThat(res.getQueryId()).isNotBlank();
    }

    // ───────── english + remaining branches ─────────

    @Test
    void attack_MainPhase_EnglishSuggestionIsAdvanceToAttack() {
        MatchStateDTO state = stateWith(TurnPhase.MAIN, pokemon("X", noStatus(), List.of(), List.of()));
        JudgeQueryDTO q = query(JudgeQueryCategory.WHY_CANT_I, GameActionType.ATTACK);
        q.setLang("en");

        JudgeResponseDTO res = resolver.resolve(q, state, List.of());

        assertThat(res.getSuggestion()).isNotNull().contains("attack phase");
        assertThat(res.getLang()).isEqualTo("en");
    }

    @Test
    void unknownAction_FallsBackToWrongPhaseReason() {
        MatchStateDTO state = stateWith(TurnPhase.MAIN, pokemon("X", noStatus(), List.of(), List.of()));

        JudgeResponseDTO res = resolve(GameActionType.RETREAT, state);

        assertThat(res.getReasons()).isNotEmpty();
        assertThat(res.getSummary()).contains("fase del turno");
    }

    @Test
    void playBasic_BenchNotFull_ReturnsWrongPhaseReason() {
        MatchStateDTO state = stateWith(TurnPhase.ATTACK, pokemon("X", noStatus(), List.of(), List.of()));
        state.getSelf().setBench(List.of(pokemon("B", noStatus(), List.of(), List.of())));

        JudgeResponseDTO res = resolve(GameActionType.PLAY_BASIC, state);

        assertThat(res.getSummary()).contains("fase del turno");
    }

    @Test
    void attachEnergy_WrongPhase_ReturnsWrongPhaseReason() {
        MatchStateDTO state = stateWith(TurnPhase.ATTACK, pokemon("X", noStatus(), List.of(), List.of()));

        JudgeResponseDTO res = resolve(GameActionType.ATTACH_ENERGY, state);

        assertThat(res.getSummary()).contains("fase del turno");
    }

    @Test
    void supporter_InMainPhase_HasNoBlockingReasons() {
        MatchStateDTO state = stateWith(TurnPhase.MAIN, pokemon("X", noStatus(), List.of(), List.of()));

        JudgeResponseDTO res = resolve(GameActionType.PLAY_SUPPORTER, state);

        assertThat(res.getReasons()).isEmpty();
        assertThat(res.isHasActiveRestrictions()).isFalse();
    }

    // ───────── null / incomplete state ─────────

    @Test
    void attack_NullActivePokemon_OnlyReportsPhase() {
        MatchStateDTO state = stateWith(TurnPhase.MAIN, null);

        JudgeResponseDTO res = resolve(GameActionType.ATTACK, state);

        assertThat(res.getReasons()).anySatisfy(r -> assertThat(r).contains("fase del turno"));
    }

    @Test
    void attachEnergy_NullSelfInMainPhase_HasNoReasons() {
        MatchStateDTO state = new MatchStateDTO();
        state.setCurrentPhase(TurnPhase.MAIN);

        JudgeResponseDTO res = resolve(GameActionType.ATTACH_ENERGY, state);

        assertThat(res.getReasons()).isEmpty();
    }

    // ───────── helpers ─────────

    private JudgeResponseDTO resolve(GameActionType action, MatchStateDTO state) {
        return resolver.resolve(query(JudgeQueryCategory.WHY_CANT_I, action), state, List.of());
    }

    private JudgeQueryDTO query(JudgeQueryCategory category, GameActionType action) {
        JudgeQueryDTO q = new JudgeQueryDTO();
        q.setMatchId("m1");
        q.setPlayerId("p1");
        q.setCategory(category);
        q.setTargetAction(action);
        q.setLang("es");
        return q;
    }

    private MatchStateDTO stateWith(TurnPhase phase, InPlayPokemonViewDTO active) {
        MatchStateDTO state = new MatchStateDTO();
        state.setCurrentPhase(phase);
        PlayerBattlefieldDTO self = new PlayerBattlefieldDTO();
        self.setActivePokemon(active);
        state.setSelf(self);
        return state;
    }

    private InPlayPokemonViewDTO pokemon(String name, Set<StatusCondition> statuses,
                                         List<AttackViewDTO> attacks, List<String> attachedEnergyTypes) {
        InPlayPokemonViewDTO p = new InPlayPokemonViewDTO();
        p.setName(name);
        p.setStatuses(statuses);
        p.setAttacks(attacks);
        p.setAttachedEnergyTypes(attachedEnergyTypes);
        p.setAttachedEnergyCount(attachedEnergyTypes == null ? 0 : attachedEnergyTypes.size());
        return p;
    }

    private AttackViewDTO attack(String name, List<String> cost) {
        return AttackViewDTO.builder().name(name).cost(cost).build();
    }

    private Set<StatusCondition> noStatus() {
        return Set.of();
    }

    private JudgeEvent statusEvent() {
        return StatusAppliedEvent.builder()
                .engineMatchId("engine-1")
                .status(StatusCondition.ASLEEP)
                .build();
    }
}
