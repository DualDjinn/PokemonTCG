package ar.edu.utn.frc.tup.piii.engine;

import ar.edu.utn.frc.tup.piii.models.effects.CardEffectRegistry;
import ar.edu.utn.frc.tup.piii.models.Attack;
import ar.edu.utn.frc.tup.piii.models.AttackEffect;
import ar.edu.utn.frc.tup.piii.models.EnergyType;
import ar.edu.utn.frc.tup.piii.models.GameContext;
import ar.edu.utn.frc.tup.piii.models.InPlayPokemon;
import ar.edu.utn.frc.tup.piii.models.PokemonCard;
import ar.edu.utn.frc.tup.piii.models.PokemonStage;
import ar.edu.utn.frc.tup.piii.models.PokemonToolCard;
import ar.edu.utn.frc.tup.piii.models.Resistance;
import ar.edu.utn.frc.tup.piii.models.Weakness;
import java.util.List;
import java.util.Map;
import java.util.Random;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Unit tests for {@link AttackResolutionPipeline}.
 */
class AttackResolutionPipelineTest {

    private final AttackResolutionPipeline attackResolutionPipeline = new AttackResolutionPipeline(
            new DamageCalculator(),
            new CardEffectRegistry(),
            new PassiveAbilityHandler(),
            new TriggeredAbilityDispatcher()
    );

    @Test
    void resolve_WhenAttackDefinesInlineEffect_AppliesItBeforeDamageCalculation() {
        Attack attack = createAttack(
                "Power Hit",
                30,
                ctx -> ctx.setPendingDamage(ctx.getPendingDamage() + 20)
        );
        InPlayPokemon attacker = createPokemon("attacker", EnergyType.FIRE, List.of(attack), List.of(), List.of());
        InPlayPokemon defender = createPokemon("defender", EnergyType.GRASS, List.of(), List.of(), List.of());
        GameContext context = createContext(attacker, defender);

        int finalDamage = attackResolutionPipeline.resolve(context, attack);

        assertThat(finalDamage).isEqualTo(50);
        assertThat(defender.getDamageTaken()).isEqualTo(50);
    }

    @Test
    void resolve_WhenRegistryContainsAnEffect_UsesCardIdAndAttackIndexToResolveIt() {
        Attack pheromotion = createAttack("Pheromotion", 0, null);
        Attack quickAttack = createAttack("Quick Attack", 10, null);
        InPlayPokemon attacker = createPokemon(
                "xy1-9",
                EnergyType.GRASS,
                List.of(pheromotion, quickAttack),
                List.of(),
                List.of()
        );
        InPlayPokemon defender = createPokemon("defender", EnergyType.WATER, List.of(), List.of(), List.of());
        GameContext context = createContext(attacker, defender);
        context.setRandom(new FixedBooleanRandom(true));

        int finalDamage = attackResolutionPipeline.resolve(context, quickAttack);

        assertThat(finalDamage).isEqualTo(30);
        assertThat(defender.getDamageTaken()).isEqualTo(30);
    }

    @Test
    void resolve_WhenDefenderPreventsAllEffects_SkipsAttackEffectAndAppliesNoDamage() {
        AttackEffect markerEffect = ctx -> {
            ctx.getAttacker().setMetalWallopBonus(99);
            ctx.setPendingDamage(ctx.getPendingDamage() + 40);
        };
        Attack attack = createAttack("Blocked Hit", 30, markerEffect);
        InPlayPokemon attacker = createPokemon("attacker", EnergyType.FIRE, List.of(attack), List.of(), List.of());
        InPlayPokemon defender = createPokemon("defender", EnergyType.GRASS, List.of(), List.of(), List.of());
        defender.setPreventAllEffects(true);
        GameContext context = createContext(attacker, defender);

        int finalDamage = attackResolutionPipeline.resolve(context, attack);

        assertThat(finalDamage).isZero();
        assertThat(defender.getDamageTaken()).isZero();
        assertThat(attacker.getMetalWallopBonus()).isZero();
    }

    @Test
    void resolve_WhenDefenderPreventsAllIncomingDamage_KeepsEffectButAppliesNoDamage() {
        AttackEffect markerEffect = ctx -> {
            ctx.getAttacker().setMetalWallopBonus(40);
            ctx.setPendingDamage(ctx.getPendingDamage() + 30);
        };
        Attack attack = createAttack("Shielded Hit", 20, markerEffect);
        InPlayPokemon attacker = createPokemon("attacker", EnergyType.FIRE, List.of(attack), List.of(), List.of());
        InPlayPokemon defender = createPokemon("defender", EnergyType.GRASS, List.of(), List.of(), List.of());
        defender.setPreventAllIncomingDamage(true);
        GameContext context = createContext(attacker, defender);

        int finalDamage = attackResolutionPipeline.resolve(context, attack);

        assertThat(finalDamage).isZero();
        assertThat(defender.getDamageTaken()).isZero();
        assertThat(attacker.getMetalWallopBonus()).isEqualTo(40);
    }

    @Test
    void resolve_WhenCalculatedDamageIsAtOrBelowThreshold_PreventsAllDamage() {
        Attack attack = createAttack("Hardened Hit", 60, null);
        InPlayPokemon attacker = createPokemon("attacker", EnergyType.FIRE, List.of(attack), List.of(), List.of());
        InPlayPokemon defender = createPokemon("defender", EnergyType.GRASS, List.of(), List.of(), List.of());
        defender.setPreventIncomingDamageThreshold(60);
        GameContext context = createContext(attacker, defender);

        int finalDamage = attackResolutionPipeline.resolve(context, attack);

        assertThat(finalDamage).isZero();
        assertThat(defender.getDamageTaken()).isZero();
    }

    @Test
    void resolve_WhenDefenderHasIncomingReduction_SubtractsItAfterDamageCalculation() {
        Attack attack = createAttack("Moonblast", 70, null);
        InPlayPokemon attacker = createPokemon("attacker", EnergyType.FIRE, List.of(attack), List.of(), List.of());
        InPlayPokemon defender = createPokemon("defender", EnergyType.GRASS, List.of(), List.of(), List.of());
        defender.setIncomingDamageReduction(20);
        GameContext context = createContext(attacker, defender);

        int finalDamage = attackResolutionPipeline.resolve(context, attack);

        assertThat(finalDamage).isEqualTo(50);
        assertThat(defender.getDamageTaken()).isEqualTo(50);
    }

    @Test
    void resolve_WhenPreviousAttackStateExists_ClearsItBeforeResolvingCurrentAttack() {
        Attack attack = createAttack("Clean Hit", 30, null);
        InPlayPokemon attacker = createPokemon("attacker", EnergyType.FIRE, List.of(attack), List.of(), List.of());
        InPlayPokemon defender = createPokemon(
                "defender",
                EnergyType.GRASS,
                List.of(),
                List.of(new Weakness(EnergyType.FIRE, "x2")),
                List.of()
        );
        GameContext context = createContext(attacker, defender);
        context.setPendingDamage(999);
        context.setIgnoreWeakness(true);
        context.setIgnoreResistance(true);
        context.addDamageCalculationModifier(currentDamage -> currentDamage + 100);

        int finalDamage = attackResolutionPipeline.resolve(context, attack);

        assertThat(finalDamage).isEqualTo(60);
        assertThat(defender.getDamageTaken()).isEqualTo(60);
        assertThat(context.isIgnoreWeakness()).isFalse();
        assertThat(context.isIgnoreResistance()).isFalse();
        assertThat(context.getDamageCalculationModifiers()).isEmpty();
    }

    @Test
    void resolve_WhenAttackerIsMissing_ThrowsMeaningfulError() {
        Attack attack = createAttack("Hit", 20, null);
        GameContext context = new GameContext();
        context.setDefender(createPokemon("defender", EnergyType.WATER, List.of(), List.of(), List.of()));

        assertThatThrownBy(() -> attackResolutionPipeline.resolve(context, attack))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Attacker must not be null");
    }

    @Test
    void resolve_WhenDefenderIsMissing_ThrowsMeaningfulError() {
        Attack attack = createAttack("Hit", 20, null);
        GameContext context = new GameContext();
        context.setAttacker(createPokemon("attacker", EnergyType.FIRE, List.of(attack), List.of(), List.of()));

        assertThatThrownBy(() -> attackResolutionPipeline.resolve(context, attack))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Defender must not be null");
    }

    // ───────── tool-based damage modifiers (CalculateDamageHandler) ─────────

    @Test
    void resolve_WhenAttackerHasMuscleBand_AddsTwentyDamage() {
        Attack attack = createAttack("Tackle", 30, null);
        InPlayPokemon attacker = createPokemon("attacker", EnergyType.FIRE, List.of(attack), List.of(), List.of());
        attacker.setTool(PokemonToolCard.builder().id("xy1-121").name("Muscle Band").build());
        InPlayPokemon defender = createPokemon("defender", EnergyType.GRASS, List.of(), List.of(), List.of());
        GameContext context = createContext(attacker, defender);

        int finalDamage = attackResolutionPipeline.resolve(context, attack);

        assertThat(finalDamage).isEqualTo(50);
    }

    @Test
    void resolve_WhenDefenderHasHardCharm_SubtractsTwentyDamage() {
        Attack attack = createAttack("Tackle", 30, null);
        InPlayPokemon attacker = createPokemon("attacker", EnergyType.FIRE, List.of(attack), List.of(), List.of());
        InPlayPokemon defender = createPokemon("defender", EnergyType.GRASS, List.of(), List.of(), List.of());
        defender.setTool(PokemonToolCard.builder().id("xy1-119").name("Hard Charm").build());
        GameContext context = createContext(attacker, defender);

        int finalDamage = attackResolutionPipeline.resolve(context, attack);

        assertThat(finalDamage).isEqualTo(10);
    }

    @Test
    void resolve_WhenToolsAreNotDamageModifiers_DamageUnchanged() {
        Attack attack = createAttack("Tackle", 30, null);
        InPlayPokemon attacker = createPokemon("attacker", EnergyType.FIRE, List.of(attack), List.of(), List.of());
        attacker.setTool(PokemonToolCard.builder().id("xy1-200").name("Other Tool").build());
        InPlayPokemon defender = createPokemon("defender", EnergyType.GRASS, List.of(), List.of(), List.of());
        defender.setTool(PokemonToolCard.builder().id("xy1-201").name("Another Tool").build());
        GameContext context = createContext(attacker, defender);

        int finalDamage = attackResolutionPipeline.resolve(context, attack);

        assertThat(finalDamage).isEqualTo(30);
    }

    // ───────── no effect registered (ApplyAttackEffectHandler) ─────────

    @Test
    void resolve_WhenNoInlineEffectAndNoneRegistered_AppliesOnlyDamage() {
        Attack attack = createAttack("Plain Hit", 30, null);
        InPlayPokemon attacker = createPokemon(
                "unregistered-card", EnergyType.FIRE, List.of(attack), List.of(), List.of());
        InPlayPokemon defender = createPokemon("defender", EnergyType.GRASS, List.of(), List.of(), List.of());
        GameContext context = createContext(attacker, defender);

        int finalDamage = attackResolutionPipeline.resolve(context, attack);

        assertThat(finalDamage).isEqualTo(30);
        assertThat(defender.getDamageTaken()).isEqualTo(30);
    }

    @Test
    void resolve_WhenAttackNotInAttackerCardList_ResolvesNoEffect() {
        Attack listedAttack = createAttack("Listed", 10, null);
        Attack standaloneAttack = createAttack("Standalone", 40, null);
        InPlayPokemon attacker = createPokemon(
                "card-with-other-attacks", EnergyType.FIRE, List.of(listedAttack), List.of(), List.of());
        InPlayPokemon defender = createPokemon("defender", EnergyType.GRASS, List.of(), List.of(), List.of());
        GameContext context = createContext(attacker, defender);

        int finalDamage = attackResolutionPipeline.resolve(context, standaloneAttack);

        assertThat(finalDamage).isEqualTo(40);
    }

    // ───────── prevention threshold not met (ApplyDefenderProtectionHandler) ─────────

    @Test
    void resolve_WhenDamageAboveThreshold_NotPrevented() {
        Attack attack = createAttack("Big Hit", 60, null);
        InPlayPokemon attacker = createPokemon("attacker", EnergyType.FIRE, List.of(attack), List.of(), List.of());
        InPlayPokemon defender = createPokemon("defender", EnergyType.GRASS, List.of(), List.of(), List.of());
        defender.setPreventIncomingDamageThreshold(20);
        GameContext context = createContext(attacker, defender);

        int finalDamage = attackResolutionPipeline.resolve(context, attack);

        assertThat(finalDamage).isEqualTo(60);
        assertThat(defender.getDamageTaken()).isEqualTo(60);
    }

    private GameContext createContext(InPlayPokemon attacker, InPlayPokemon defender) {
        GameContext context = new GameContext();
        context.setAttacker(attacker);
        context.setDefender(defender);
        return context;
    }

    private Attack createAttack(String name, int baseDamage, AttackEffect effect) {
        return Attack.builder()
                .name(name)
                .energyCost(Map.of())
                .baseDamage(baseDamage)
                .effect(effect)
                .build();
    }

    private InPlayPokemon createPokemon(String id,
                                        EnergyType type,
                                        List<Attack> attacks,
                                        List<Weakness> weaknesses,
                                        List<Resistance> resistances) {
        PokemonCard card = PokemonCard.builder()
                .id(id)
                .name(id)
                .hp(100)
                .stage(PokemonStage.BASIC)
                .pokemonType(type)
                .costToAttack(List.of())
                .attacks(attacks)
                .abilities(List.of())
                .weaknesses(weaknesses)
                .resistances(resistances)
                .retreatCost(0)
                .build();
        return new InPlayPokemon(card);
    }

    /**
     * Deterministic {@link Random} for single-coin attack effects.
     */
    private static final class FixedBooleanRandom extends Random {

        private final boolean result;

        private FixedBooleanRandom(boolean result) {
            this.result = result;
        }

        @Override
        public boolean nextBoolean() {
            return result;
        }
    }
}

