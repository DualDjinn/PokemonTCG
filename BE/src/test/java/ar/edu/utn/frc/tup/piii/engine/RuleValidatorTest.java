package ar.edu.utn.frc.tup.piii.engine;

import ar.edu.utn.frc.tup.piii.models.Attack;
import ar.edu.utn.frc.tup.piii.models.DamageModifier;
import ar.edu.utn.frc.tup.piii.models.EnergyCard;
import ar.edu.utn.frc.tup.piii.models.EnergyType;
import ar.edu.utn.frc.tup.piii.models.GameContext;
import ar.edu.utn.frc.tup.piii.models.InPlayPokemon;
import ar.edu.utn.frc.tup.piii.models.ItemCard;
import ar.edu.utn.frc.tup.piii.models.Player;
import ar.edu.utn.frc.tup.piii.models.PokemonCard;
import ar.edu.utn.frc.tup.piii.models.PokemonStage;
import ar.edu.utn.frc.tup.piii.models.PokemonToolCard;
import ar.edu.utn.frc.tup.piii.models.StadiumCard;
import ar.edu.utn.frc.tup.piii.models.StatusCondition;
import ar.edu.utn.frc.tup.piii.models.SupporterCard;
import ar.edu.utn.frc.tup.piii.models.TurnPhase;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Unit tests for {@link RuleValidator}.
 */
class RuleValidatorTest {

    private RuleValidator ruleValidator;
    private Player player;
    private GameContext context;

    @BeforeEach
    void setUp() {
        ruleValidator = new RuleValidator();
        player = new Player();
        player.setId("player-1");
        player.setUsername("ash");
        context = new GameContext();
        // Por defecto GameContext arranca en turno de apertura (firstTurn=true), donde
        // no se puede atacar/evolucionar. Estos tests simulan un turno normal; los que
        // verifican la regla de apertura setean firstTurn=true explícitamente.
        context.setFirstTurn(false);
    }

    @Test
    void canPlayBasicPokemon_WhenBenchHasFourPokemonInMainPhase_ReturnsTrue() {
        context.setCurrentPhase(TurnPhase.MAIN);
        context.setAttackerBench(createBench(4));

        boolean canPlay = ruleValidator.canPlayBasicPokemon(player, context);

        assertThat(canPlay).isTrue();
    }

    @Test
    void canPlayBasicPokemon_WhenBenchIsAtExactLimit_ReturnsFalse() {
        context.setCurrentPhase(TurnPhase.MAIN);
        context.setAttackerBench(createBench(5));

        boolean canPlay = ruleValidator.canPlayBasicPokemon(player, context);

        assertThat(canPlay).isFalse();
    }

    @Test
    void canPlayBasicPokemon_WhenPhaseIsNotMain_ReturnsFalse() {
        context.setCurrentPhase(TurnPhase.ATTACK);
        context.setAttackerBench(createBench(2));

        boolean canPlay = ruleValidator.canPlayBasicPokemon(player, context);

        assertThat(canPlay).isFalse();
    }

    @Test
    void canPlayBasicPokemon_WhenPlayerIsNull_ThrowsNullPointerException() {
        assertThatThrownBy(() -> ruleValidator.canPlayBasicPokemon(null, context))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void canPlayBasicPokemon_WhenContextIsNull_ThrowsNullPointerException() {
        assertThatThrownBy(() -> ruleValidator.canPlayBasicPokemon(player, null))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void canPlayBasicPokemon_WhenBenchTrackerIsMissing_ReturnsTrue() {
        context.setCurrentPhase(TurnPhase.MAIN);
        context.setAttackerBench(null);

        boolean canPlay = ruleValidator.canPlayBasicPokemon(player, context);

        assertThat(canPlay).isTrue();
    }

    @Test
    void canEvolve_WhenEvolutionIsValidAndTimingRulesPass_ReturnsTrue() {
        PokemonCard base = createPokemonCard("Bulbasaur", PokemonStage.BASIC, null, 1);
        PokemonCard evolution = createPokemonCard("Ivysaur", PokemonStage.STAGE_1,
                "Bulbasaur", 2);
        context.setFirstTurn(false);
        context.setAttackerPokemonPlayedThisTurn(new ArrayList<>());

        boolean canEvolve = ruleValidator.canEvolve(base, evolution, context);

        assertThat(canEvolve).isTrue();
    }

    @Test
    void canEvolve_WhenEvolutionPathDoesNotMatch_ReturnsFalse() {
        PokemonCard base = createPokemonCard("Bulbasaur", PokemonStage.BASIC, null, 1);
        PokemonCard evolution = createPokemonCard("Charmeleon", PokemonStage.STAGE_1,
                "Charmander", 1);

        boolean canEvolve = ruleValidator.canEvolve(base, evolution, context);

        assertThat(canEvolve).isFalse();
    }

    @Test
    void canEvolve_WhenItIsFirstTurn_ReturnsFalse() {
        PokemonCard base = createPokemonCard("Bulbasaur", PokemonStage.BASIC, null, 1);
        PokemonCard evolution = createPokemonCard("Ivysaur", PokemonStage.STAGE_1,
                "Bulbasaur", 2);
        context.setFirstTurn(true);

        boolean canEvolve = ruleValidator.canEvolve(base, evolution, context);

        assertThat(canEvolve).isFalse();
    }

    @Test
    void canEvolve_WhenBasePokemonWasPlayedThisTurn_ReturnsFalse() {
        PokemonCard base = createPokemonCard("Bulbasaur", PokemonStage.BASIC, null, 1);
        PokemonCard evolution = createPokemonCard("Ivysaur", PokemonStage.STAGE_1,
                "Bulbasaur", 2);
        context.getAttackerPokemonPlayedThisTurn().add(base);

        boolean canEvolve = ruleValidator.canEvolve(base, evolution, context);

        assertThat(canEvolve).isFalse();
    }

    @Test
    void canEvolve_WhenPlayedThisTurnTrackerIsMissing_ReturnsTrue() {
        PokemonCard base = createPokemonCard("Bulbasaur", PokemonStage.BASIC, null, 1);
        PokemonCard evolution = createPokemonCard("Ivysaur", PokemonStage.STAGE_1,
                "Bulbasaur", 2);
        context.setAttackerPokemonPlayedThisTurn(null);

        boolean canEvolve = ruleValidator.canEvolve(base, evolution, context);

        assertThat(canEvolve).isTrue();
    }

    @Test
    void canAttachEnergy_WhenMainPhaseAndAttachmentAvailable_ReturnsTrue() {
        InPlayPokemon target = createInPlayPokemon("Pikachu", 0);
        context.setCurrentPhase(TurnPhase.MAIN);
        context.setAttackerEnergyAttachmentsThisTurn(0);

        boolean canAttach = ruleValidator.canAttachEnergy(player, createEnergy(EnergyType.LIGHTNING),
                target, context);

        assertThat(canAttach).isTrue();
    }

    @Test
    void canAttachEnergy_WhenStandardQuotaIsAlreadyUsed_ReturnsFalse() {
        InPlayPokemon target = createInPlayPokemon("Pikachu", 0);
        context.setCurrentPhase(TurnPhase.MAIN);
        context.setAttackerEnergyAttachmentsThisTurn(1);

        boolean canAttach = ruleValidator.canAttachEnergy(player, createEnergy(EnergyType.LIGHTNING),
                target, context);

        assertThat(canAttach).isFalse();
    }

    @Test
    void canAttachEnergy_WhenSpecialEffectGrantsExtraAttachment_ReturnsTrue() {
        InPlayPokemon target = createInPlayPokemon("Pikachu", 0);
        context.setCurrentPhase(TurnPhase.MAIN);
        context.setAttackerEnergyAttachmentsThisTurn(1);
        context.setAttackerAdditionalEnergyAttachments(1);

        boolean canAttach = ruleValidator.canAttachEnergy(player, createEnergy(EnergyType.LIGHTNING),
                target, context);

        assertThat(canAttach).isTrue();
    }

    @Test
    void canAttachEnergy_WhenPhaseIsNotMain_ReturnsFalse() {
        InPlayPokemon target = createInPlayPokemon("Pikachu", 0);
        context.setCurrentPhase(TurnPhase.ATTACK);

        boolean canAttach = ruleValidator.canAttachEnergy(player, createEnergy(EnergyType.LIGHTNING),
                target, context);

        assertThat(canAttach).isFalse();
    }

    @Test
    void canAttack_WhenPhaseAndEnergyRequirementsMatchExactly_ReturnsTrue() {
        InPlayPokemon attacker = createInPlayPokemon("Charizard", 1,
                EnergyType.FIRE, EnergyType.WATER);
        Attack attack = createAttack("Flare Shot", Map.of(
                EnergyType.FIRE, 1,
                EnergyType.COLORLESS, 1
        ));
        context.setCurrentPhase(TurnPhase.ATTACK);

        boolean canAttack = ruleValidator.canAttack(attacker, attack, context);

        assertThat(canAttack).isTrue();
    }

    @Test
    void canAttack_WhenSpecificTypedEnergyIsMissing_ReturnsFalse() {
        InPlayPokemon attacker = createInPlayPokemon("Charizard", 1, EnergyType.WATER);
        Attack attack = createAttack("Flare Shot", Map.of(EnergyType.FIRE, 1));
        context.setCurrentPhase(TurnPhase.ATTACK);

        boolean canAttack = ruleValidator.canAttack(attacker, attack, context);

        assertThat(canAttack).isFalse();
    }

    @Test
    void canAttack_WhenTotalEnergyIsInsufficientForColorlessRequirement_ReturnsFalse() {
        InPlayPokemon attacker = createInPlayPokemon("Charizard", 1, EnergyType.FIRE);
        Attack attack = createAttack("Big Blaze", Map.of(
                EnergyType.FIRE, 1,
                EnergyType.COLORLESS, 1
        ));
        context.setCurrentPhase(TurnPhase.ATTACK);

        boolean canAttack = ruleValidator.canAttack(attacker, attack, context);

        assertThat(canAttack).isFalse();
    }

    @Test
    void canAttack_WhenAttackHasNoEnergyCost_ReturnsTrue() {
        InPlayPokemon attacker = createInPlayPokemon("Mew", 0);
        Attack attack = createAttack("Free Shot", Map.of());
        context.setCurrentPhase(TurnPhase.ATTACK);

        boolean canAttack = ruleValidator.canAttack(attacker, attack, context);

        assertThat(canAttack).isTrue();
    }

    @Test
    void canAttack_WhenPokemonIsAsleep_ReturnsFalse() {
        InPlayPokemon attacker = createInPlayPokemon("Snorlax", 1, EnergyType.COLORLESS);
        attacker.applyStatus(StatusCondition.ASLEEP);
        Attack attack = createAttack("Roll Over", Map.of(EnergyType.COLORLESS, 1));
        context.setCurrentPhase(TurnPhase.ATTACK);

        boolean canAttack = ruleValidator.canAttack(attacker, attack, context);

        assertThat(canAttack).isFalse();
    }

    @Test
    void canAttack_WhenPokemonIsParalyzed_ReturnsFalse() {
        InPlayPokemon attacker = createInPlayPokemon("Raichu", 1, EnergyType.LIGHTNING);
        attacker.applyStatus(StatusCondition.PARALYZED);
        Attack attack = createAttack("Thunder", Map.of(EnergyType.LIGHTNING, 1));
        context.setCurrentPhase(TurnPhase.ATTACK);

        boolean canAttack = ruleValidator.canAttack(attacker, attack, context);

        assertThat(canAttack).isFalse();
    }

    @Test
    void canAttack_WhenSpecialRestrictionBlocksSpecificAttack_ReturnsFalse() {
        InPlayPokemon attacker = createInPlayPokemon("Yveltal", 1,
                EnergyType.DARKNESS, EnergyType.COLORLESS, EnergyType.COLORLESS);
        attacker.setCantUseAttackName("X Blast");
        Attack attack = createAttack("X Blast", Map.of(
                EnergyType.DARKNESS, 1,
                EnergyType.COLORLESS, 2
        ));
        context.setCurrentPhase(TurnPhase.ATTACK);

        boolean canAttack = ruleValidator.canAttack(attacker, attack, context);

        assertThat(canAttack).isFalse();
    }

    @Test
    void canAttack_WhenCardEffectBlocksAllAttacks_ReturnsFalse() {
        InPlayPokemon attacker = createInPlayPokemon("Machamp", 1, EnergyType.FIGHTING);
        attacker.setCantAttack(true);
        Attack attack = createAttack("Knock Back", Map.of(EnergyType.FIGHTING, 1));
        context.setCurrentPhase(TurnPhase.ATTACK);

        boolean canAttack = ruleValidator.canAttack(attacker, attack, context);

        assertThat(canAttack).isFalse();
    }

    @Test
    void canAttack_WhenPhaseIsNotAttack_ReturnsFalse() {
        InPlayPokemon attacker = createInPlayPokemon("Charizard", 1,
                EnergyType.FIRE, EnergyType.COLORLESS);
        Attack attack = createAttack("Flare Shot", Map.of(EnergyType.FIRE, 1));
        context.setCurrentPhase(TurnPhase.MAIN);

        boolean canAttack = ruleValidator.canAttack(attacker, attack, context);

        assertThat(canAttack).isFalse();
    }

    @Test
    void canRetreat_WhenAttachedEnergyMatchesExactRetreatCost_ReturnsTrue() {
        InPlayPokemon pokemon = createInPlayPokemon("Blastoise", 2,
                EnergyType.WATER, EnergyType.WATER);

        boolean canRetreat = ruleValidator.canRetreat(pokemon, context);

        assertThat(canRetreat).isTrue();
    }

    @Test
    void canRetreat_WhenAttachedEnergyIsInsufficient_ReturnsFalse() {
        InPlayPokemon pokemon = createInPlayPokemon("Blastoise", 2, EnergyType.WATER);

        boolean canRetreat = ruleValidator.canRetreat(pokemon, context);

        assertThat(canRetreat).isFalse();
    }

    @Test
    void canRetreat_WhenCardEffectPreventsRetreat_ReturnsFalse() {
        InPlayPokemon pokemon = createInPlayPokemon("Blastoise", 1, EnergyType.WATER);
        pokemon.setCantRetreat(true);

        boolean canRetreat = ruleValidator.canRetreat(pokemon, context);

        assertThat(canRetreat).isFalse();
    }

    @Test
    void canRetreat_WhenImmobilizingStatusIsActive_ReturnsFalse() {
        InPlayPokemon pokemon = createInPlayPokemon("Blastoise", 1, EnergyType.WATER);
        pokemon.applyStatus(StatusCondition.PARALYZED);

        boolean canRetreat = ruleValidator.canRetreat(pokemon, context);

        assertThat(canRetreat).isFalse();
    }

    @Test
    void canRetreat_WhenPokemonIsAsleep_ReturnsFalse() {
        InPlayPokemon pokemon = createInPlayPokemon("Blastoise", 1, EnergyType.WATER);
        pokemon.applyStatus(StatusCondition.ASLEEP);

        boolean canRetreat = ruleValidator.canRetreat(pokemon, context);

        assertThat(canRetreat).isFalse();
    }

    @Test
    void canUseSupporterCard_WhenQuotaAndTimingAllowIt_ReturnsTrue() {
        context.setCurrentPhase(TurnPhase.MAIN);
        context.setAttackerSupportersPlayedThisTurn(0);

        boolean canUseSupporter = ruleValidator.canUseSupporterCard(player, createSupporter(),
                context);

        assertThat(canUseSupporter).isTrue();
    }

    @Test
    void canUseSupporterCard_WhenExactQuotaWasReached_ReturnsFalse() {
        context.setCurrentPhase(TurnPhase.MAIN);
        context.setAttackerSupportersPlayedThisTurn(1);

        boolean canUseSupporter = ruleValidator.canUseSupporterCard(player, createSupporter(),
                context);

        assertThat(canUseSupporter).isFalse();
    }

    @Test
    void canUseSupporterCard_WhenEffectPreventsSupporters_ReturnsFalse() {
        context.setCurrentPhase(TurnPhase.MAIN);
        context.setAttackerCantPlaySupporters(true);

        boolean canUseSupporter = ruleValidator.canUseSupporterCard(player, createSupporter(),
                context);

        assertThat(canUseSupporter).isFalse();
    }

    @Test
    void canUseSupporterCard_WhenPhaseIsNotMain_ReturnsFalse() {
        context.setCurrentPhase(TurnPhase.ATTACK);

        boolean canUseSupporter = ruleValidator.canUseSupporterCard(player, createSupporter(),
                context);

        assertThat(canUseSupporter).isFalse();
    }

    /**
     * Creates a deterministic bench state with the requested number of Pokémon.
     */
    private List<InPlayPokemon> createBench(int size) {
        List<InPlayPokemon> bench = new ArrayList<>();
        for (int i = 0; i < size; i++) {
            bench.add(createInPlayPokemon("BenchMon-" + i, 1));
        }
        return bench;
    }

    /**
     * Creates an in-play Pokémon and attaches the provided energies.
     */
    private InPlayPokemon createInPlayPokemon(String name,
                                              int retreatCost,
                                              EnergyType... attachedEnergies) {
        InPlayPokemon pokemon = new InPlayPokemon(createPokemonCard(name, PokemonStage.BASIC,
                null, retreatCost));
        for (EnergyType energyType : attachedEnergies) {
            pokemon.getEnergies().add(createEnergy(energyType));
        }
        return pokemon;
    }

    /**
     * Creates card metadata tailored to the validation scenario under test.
     */
    private PokemonCard createPokemonCard(String name,
                                          PokemonStage stage,
                                          String evolvesFrom,
                                          int retreatCost) {
        return PokemonCard.builder()
                .id(name + "-" + stage)
                .name(name)
                .hp(100)
                .stage(stage)
                .pokemonType(EnergyType.GRASS)
                .costToAttack(List.of())
                .attacks(List.of())
                .abilities(List.of())
                .weaknesses(List.of())
                .resistances(List.of())
                .retreatCost(retreatCost)
                .evolvesFrom(evolvesFrom)
                .build();
    }

    /**
     * Creates a simple Energy card for resource-based validations.
     */
    private EnergyCard createEnergy(EnergyType energyType) {
        return EnergyCard.builder()
                .id("energy-" + energyType)
                .name(energyType.name())
                .energyType(energyType)
                .build();
    }

    /**
     * Creates an attack with a deterministic cost map.
     */
    private Attack createAttack(String name, Map<EnergyType, Integer> energyCost) {
        return Attack.builder()
                .name(name)
                .energyCost(energyCost)
                .baseDamage(60)
                .damageModifier(DamageModifier.NONE)
                .build();
    }

    /**
     * Creates a Supporter card stub for turn-quota validations.
     */
    private SupporterCard createSupporter() {
        return SupporterCard.builder()
                .id("supporter-1")
                .name("Professor Sycamore")
                .build();
    }

    // ───────── canAttack first-turn ─────────

    @Test
    void canAttack_WhenFirstTurn_ReturnsFalse() {
        context.setCurrentPhase(TurnPhase.ATTACK);
        context.setFirstTurn(true);
        InPlayPokemon pokemon = createInPlayPokemon("Attacker", 1, EnergyType.FIRE, EnergyType.FIRE);
        Attack attack = createAttack("atk", Map.of(EnergyType.COLORLESS, 1));

        boolean canAttack = ruleValidator.canAttack(pokemon, attack, context);
        assertThat(canAttack).isFalse();
    }

    // ───────── canRetreat edge cases ─────────

    @Test
    void canRetreat_WhenZeroRetreatCost_ReturnsTrue() {
        context.setCurrentPhase(TurnPhase.MAIN);
        PokemonCard card = PokemonCard.builder().id("p-zero").name("ZeroMon").hp(60)
                .stage(PokemonStage.BASIC).pokemonType(EnergyType.COLORLESS)
                .retreatCost(0).build();
        InPlayPokemon pokemon = new InPlayPokemon(card);

        boolean canRetreat = ruleValidator.canRetreat(pokemon, context);
        assertThat(canRetreat).isTrue();
    }

    @Test
    void canRetreat_FairyGardenWithFairyEnergy_ReturnsTrue() {
        context.setCurrentPhase(TurnPhase.MAIN);
        PokemonCard card = PokemonCard.builder().id("p1").name("HeavyMon").hp(100)
                .stage(PokemonStage.BASIC).pokemonType(EnergyType.COLORLESS)
                .retreatCost(3).build();
        InPlayPokemon pokemon = new InPlayPokemon(card);
        pokemon.getEnergies().add(EnergyCard.builder().id("f1").name("Fairy Energy")
                .energyType(EnergyType.FAIRY).build());
        context.setActiveStadium(StadiumCard.builder().id("xy1-117")
                .name("Fairy Garden").build());

        boolean canRetreat = ruleValidator.canRetreat(pokemon, context);
        assertThat(canRetreat).isTrue();
    }

    @Test
    void canRetreat_FairyGardenWithoutFairyEnergy_StillNeedsCost() {
        context.setCurrentPhase(TurnPhase.MAIN);
        PokemonCard card = PokemonCard.builder().id("p1").name("HeavyMon").hp(100)
                .stage(PokemonStage.BASIC).pokemonType(EnergyType.COLORLESS)
                .retreatCost(3).build();
        InPlayPokemon pokemon = new InPlayPokemon(card);
        pokemon.getEnergies().add(EnergyCard.builder().id("f1").name("Fire Energy")
                .energyType(EnergyType.FIRE).build());
        context.setActiveStadium(StadiumCard.builder().id("xy1-117")
                .name("Fairy Garden").build());

        boolean canRetreat = ruleValidator.canRetreat(pokemon, context);
        assertThat(canRetreat).isFalse();
    }

    // ───────── canPlayItem ─────────

    @Test
    void canPlayItem_MainPhase_ReturnsTrue() {
        context.setCurrentPhase(TurnPhase.MAIN);
        ItemCard item = ItemCard.builder().id("i1").name("Potion").build();

        boolean canPlay = ruleValidator.canPlayItem(player, item, context);
        assertThat(canPlay).isTrue();
    }

    @Test
    void canPlayItem_NotMainPhase_ReturnsFalse() {
        context.setCurrentPhase(TurnPhase.ATTACK);
        ItemCard item = ItemCard.builder().id("i1").name("Potion").build();

        boolean canPlay = ruleValidator.canPlayItem(player, item, context);
        assertThat(canPlay).isFalse();
    }

    @Test
    void canPlayItem_NullPlayer_Throws() {
        assertThatThrownBy(() -> ruleValidator.canPlayItem(null, ItemCard.builder().id("i1").build(), context))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void canPlayItem_NullCard_Throws() {
        assertThatThrownBy(() -> ruleValidator.canPlayItem(player, null, context))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void canPlayItem_NullContext_Throws() {
        assertThatThrownBy(() -> ruleValidator.canPlayItem(player, ItemCard.builder().id("i1").build(), null))
                .isInstanceOf(NullPointerException.class);
    }

    // ───────── canPlayStadium ─────────

    @Test
    void canPlayStadium_MainPhaseNoStadium_ReturnsTrue() {
        context.setCurrentPhase(TurnPhase.MAIN);
        StadiumCard stadium = StadiumCard.builder().id("s1").name("Fairy Garden").build();

        boolean canPlay = ruleValidator.canPlayStadium(player, stadium, context);
        assertThat(canPlay).isTrue();
    }

    @Test
    void canPlayStadium_MainPhaseDifferentStadium_ReturnsTrue() {
        context.setCurrentPhase(TurnPhase.MAIN);
        context.setActiveStadium(StadiumCard.builder().id("s-old").name("Shadow Circle").build());
        StadiumCard stadium = StadiumCard.builder().id("s1").name("Fairy Garden").build();

        boolean canPlay = ruleValidator.canPlayStadium(player, stadium, context);
        assertThat(canPlay).isTrue();
    }

    @Test
    void canPlayStadium_SameNameAlreadyActive_ReturnsFalse() {
        context.setCurrentPhase(TurnPhase.MAIN);
        context.setActiveStadium(StadiumCard.builder().id("s-old").name("Fairy Garden").build());
        StadiumCard stadium = StadiumCard.builder().id("s1").name("Fairy Garden").build();

        boolean canPlay = ruleValidator.canPlayStadium(player, stadium, context);
        assertThat(canPlay).isFalse();
    }

    @Test
    void canPlayStadium_NotMainPhase_ReturnsFalse() {
        context.setCurrentPhase(TurnPhase.ATTACK);
        StadiumCard stadium = StadiumCard.builder().id("s1").name("Fairy Garden").build();

        boolean canPlay = ruleValidator.canPlayStadium(player, stadium, context);
        assertThat(canPlay).isFalse();
    }

    @Test
    void canPlayStadium_NullPlayer_Throws() {
        StadiumCard s = StadiumCard.builder().id("s1").build();
        assertThatThrownBy(() -> ruleValidator.canPlayStadium(null, s, context))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void canPlayStadium_NullCard_Throws() {
        assertThatThrownBy(() -> ruleValidator.canPlayStadium(player, null, context))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void canPlayStadium_NullContext_Throws() {
        StadiumCard s = StadiumCard.builder().id("s1").build();
        assertThatThrownBy(() -> ruleValidator.canPlayStadium(player, s, null))
                .isInstanceOf(NullPointerException.class);
    }

    // ───────── canAttachTool ─────────

    @Test
    void canAttachTool_MainPhaseNoExistingTool_ReturnsTrue() {
        context.setCurrentPhase(TurnPhase.MAIN);
        PokemonToolCard tool = PokemonToolCard.builder().id("t1").name("Muscle Band").build();
        PokemonCard card = PokemonCard.builder().id("p1").name("TestMon").hp(100)
                .stage(PokemonStage.BASIC).pokemonType(EnergyType.COLORLESS).build();
        InPlayPokemon pokemon = new InPlayPokemon(card);

        boolean canAttach = ruleValidator.canAttachTool(player, tool, pokemon, context);
        assertThat(canAttach).isTrue();
    }

    @Test
    void canAttachTool_AlreadyHasTool_ReturnsFalse() {
        context.setCurrentPhase(TurnPhase.MAIN);
        PokemonToolCard tool = PokemonToolCard.builder().id("t1").name("Muscle Band").build();
        PokemonCard card = PokemonCard.builder().id("p1").name("TestMon").hp(100)
                .stage(PokemonStage.BASIC).pokemonType(EnergyType.COLORLESS).build();
        InPlayPokemon pokemon = new InPlayPokemon(card);
        pokemon.setTool(PokemonToolCard.builder().id("t-old").name("Hard Charm").build());

        boolean canAttach = ruleValidator.canAttachTool(player, tool, pokemon, context);
        assertThat(canAttach).isFalse();
    }

    @Test
    void canAttachTool_NotMainPhase_ReturnsFalse() {
        context.setCurrentPhase(TurnPhase.ATTACK);
        PokemonToolCard tool = PokemonToolCard.builder().id("t1").name("Muscle Band").build();
        PokemonCard card = PokemonCard.builder().id("p1").name("TestMon").hp(100)
                .stage(PokemonStage.BASIC).pokemonType(EnergyType.COLORLESS).build();
        InPlayPokemon pokemon = new InPlayPokemon(card);

        boolean canAttach = ruleValidator.canAttachTool(player, tool, pokemon, context);
        assertThat(canAttach).isFalse();
    }

    @Test
    void canAttachTool_NullPlayer_Throws() {
        PokemonToolCard tool = PokemonToolCard.builder().id("t1").build();
        InPlayPokemon pokemon = new InPlayPokemon(PokemonCard.builder().id("p1").hp(60)
                .stage(PokemonStage.BASIC).pokemonType(EnergyType.COLORLESS).build());
        assertThatThrownBy(() -> ruleValidator.canAttachTool(null, tool, pokemon, context))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void canAttachTool_NullCard_Throws() {
        InPlayPokemon pokemon = new InPlayPokemon(PokemonCard.builder().id("p1").hp(60)
                .stage(PokemonStage.BASIC).pokemonType(EnergyType.COLORLESS).build());
        assertThatThrownBy(() -> ruleValidator.canAttachTool(player, null, pokemon, context))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void canAttachTool_NullPokemon_Throws() {
        PokemonToolCard tool = PokemonToolCard.builder().id("t1").build();
        assertThatThrownBy(() -> ruleValidator.canAttachTool(player, tool, null, context))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void canAttachTool_NullContext_Throws() {
        PokemonToolCard tool = PokemonToolCard.builder().id("t1").build();
        InPlayPokemon pokemon = new InPlayPokemon(PokemonCard.builder().id("p1").hp(60)
                .stage(PokemonStage.BASIC).pokemonType(EnergyType.COLORLESS).build());
        assertThatThrownBy(() -> ruleValidator.canAttachTool(player, tool, pokemon, null))
                .isInstanceOf(NullPointerException.class);
    }

    // ───────── Null guard tests for existing methods ─────────

    @Test
    void canEvolve_NullBase_Throws() {
        PokemonCard evolution = PokemonCard.builder().id("e1").name("Ivysaur").hp(80)
                .stage(PokemonStage.STAGE_1).evolvesFrom("Bulbasaur").build();
        assertThatThrownBy(() -> ruleValidator.canEvolve(null, evolution, context))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void canEvolve_NullEvolution_Throws() {
        PokemonCard base = PokemonCard.builder().id("b1").name("Bulbasaur").hp(60)
                .stage(PokemonStage.BASIC).build();
        assertThatThrownBy(() -> ruleValidator.canEvolve(base, null, context))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void canEvolve_NullContext_Throws() {
        PokemonCard base = PokemonCard.builder().id("b1").name("Bulbasaur").hp(60)
                .stage(PokemonStage.BASIC).build();
        PokemonCard evolution = PokemonCard.builder().id("e1").name("Ivysaur").hp(80)
                .stage(PokemonStage.STAGE_1).evolvesFrom("Bulbasaur").build();
        assertThatThrownBy(() -> ruleValidator.canEvolve(base, evolution, null))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void canAttachEnergy_NullPlayer_Throws() {
        EnergyCard energy = EnergyCard.builder().id("e1").energyType(EnergyType.FIRE).build();
        InPlayPokemon target = createInPlayPokemon("Target", 1);
        assertThatThrownBy(() -> ruleValidator.canAttachEnergy(null, energy, target, context))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void canAttachEnergy_NullEnergyCard_Throws() {
        InPlayPokemon target = createInPlayPokemon("Target", 1);
        assertThatThrownBy(() -> ruleValidator.canAttachEnergy(player, null, target, context))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void canAttachEnergy_NullTarget_Throws() {
        EnergyCard energy = EnergyCard.builder().id("e1").energyType(EnergyType.FIRE).build();
        assertThatThrownBy(() -> ruleValidator.canAttachEnergy(player, energy, null, context))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void canAttachEnergy_NullContext_Throws() {
        EnergyCard energy = EnergyCard.builder().id("e1").energyType(EnergyType.FIRE).build();
        InPlayPokemon target = createInPlayPokemon("Target", 1);
        assertThatThrownBy(() -> ruleValidator.canAttachEnergy(player, energy, target, null))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void canAttack_NullAttacker_Throws() {
        Attack attack = createAttack("atk", Map.of());
        assertThatThrownBy(() -> ruleValidator.canAttack(null, attack, context))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void canAttack_NullAttack_Throws() {
        InPlayPokemon attacker = createInPlayPokemon("Attacker", 1, EnergyType.FIRE, EnergyType.FIRE);
        assertThatThrownBy(() -> ruleValidator.canAttack(attacker, null, context))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void canAttack_NullContext_Throws() {
        InPlayPokemon attacker = createInPlayPokemon("Attacker", 1, EnergyType.FIRE, EnergyType.FIRE);
        Attack attack = createAttack("atk", Map.of());
        assertThatThrownBy(() -> ruleValidator.canAttack(attacker, attack, null))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void canRetreat_NullPokemon_Throws() {
        assertThatThrownBy(() -> ruleValidator.canRetreat(null, context))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void canRetreat_NullContext_Throws() {
        InPlayPokemon pokemon = createInPlayPokemon("Target", 1);
        assertThatThrownBy(() -> ruleValidator.canRetreat(pokemon, null))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void canUseSupporterCard_NullPlayer_Throws() {
        SupporterCard s = createSupporter();
        assertThatThrownBy(() -> ruleValidator.canUseSupporterCard(null, s, context))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void canUseSupporterCard_NullCard_Throws() {
        assertThatThrownBy(() -> ruleValidator.canUseSupporterCard(player, null, context))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void canUseSupporterCard_NullContext_Throws() {
        SupporterCard s = createSupporter();
        assertThatThrownBy(() -> ruleValidator.canUseSupporterCard(player, s, null))
                .isInstanceOf(NullPointerException.class);
    }

    // ───────── countNonNull edge case ─────────

    @Test
    void canPlayBasicPokemon_WhenBenchHasMixedNulls_CountsOnlyNonNull() {
        context.setCurrentPhase(TurnPhase.MAIN);
        List<InPlayPokemon> bench = new ArrayList<>();
        bench.add(new InPlayPokemon(PokemonCard.builder().id("b1").hp(60)
                .stage(PokemonStage.BASIC).pokemonType(EnergyType.COLORLESS).build()));
        bench.add(null);
        bench.add(new InPlayPokemon(PokemonCard.builder().id("b2").hp(60)
                .stage(PokemonStage.BASIC).pokemonType(EnergyType.COLORLESS).build()));
        bench.add(null);
        // 2 real pokemon + 2 nulls = 2 counted, still under 5
        context.setAttackerBench(bench);

        boolean canPlay = ruleValidator.canPlayBasicPokemon(player, context);
        assertThat(canPlay).isTrue();
    }

    // ───────── hasEnergyAttachmentAvailable with negative ─────────

    @Test
    void canAttachEnergy_WhenUsedIsNegative_StillAllows() {
        context.setCurrentPhase(TurnPhase.MAIN);
        context.setAttackerEnergyAttachmentsThisTurn(-1);
        EnergyCard energy = EnergyCard.builder().id("e1").energyType(EnergyType.FIRE).build();
        InPlayPokemon target = createInPlayPokemon("Target", 1);

        boolean canAttach = ruleValidator.canAttachEnergy(player, energy, target, context);
        assertThat(canAttach).isTrue();
    }
}
