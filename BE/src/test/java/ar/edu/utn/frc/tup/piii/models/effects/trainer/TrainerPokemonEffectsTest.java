package ar.edu.utn.frc.tup.piii.models.effects.trainer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import ar.edu.utn.frc.tup.piii.engine.choice.PendingPlayerChoice;
import ar.edu.utn.frc.tup.piii.models.EnergyCard;
import ar.edu.utn.frc.tup.piii.models.EnergyType;
import ar.edu.utn.frc.tup.piii.models.GameContext;
import ar.edu.utn.frc.tup.piii.models.InPlayPokemon;
import ar.edu.utn.frc.tup.piii.models.ItemCard;
import ar.edu.utn.frc.tup.piii.models.PokemonCard;
import ar.edu.utn.frc.tup.piii.models.PokemonStage;

class TrainerPokemonEffectsTest {

    private GameContext ctx;
    private PokemonCard basePokemon;
    private InPlayPokemon attacker;
    private InPlayPokemon defender;

    @BeforeEach
    void setUp() {
        basePokemon = PokemonCard.builder()
                .id("p1").name("TestMon").hp(100)
                .stage(PokemonStage.BASIC).pokemonType(EnergyType.COLORLESS)
                .build();
        attacker = new InPlayPokemon(basePokemon);
        defender = new InPlayPokemon(basePokemon);
        ctx = new GameContext();
        ctx.setAttacker(attacker);
        ctx.setDefender(defender);
        ctx.setAttackerBench(new ArrayList<>());
        ctx.setDefenderBench(new ArrayList<>());
        ctx.setAttackerPlayerId("p1");
    }

    // ───────── Super Potion Effect ─────────

    @Test
    void superPotion_Heals60FromActiveAndDiscardsEnergy() {
        attacker.setDamageTaken(70);
        attacker.getEnergies().add(EnergyCard.builder().id("e1").name("Fire Energy")
                .energyType(EnergyType.FIRE).build());

        new SuperPotionEffect().apply(ctx);

        assertThat(attacker.getDamageTaken()).isEqualTo(10);
        assertThat(attacker.getEnergies()).isEmpty();
    }

    @Test
    void superPotion_PrefersMostDamagedOnBench() {
        attacker.setDamageTaken(30);
        attacker.getEnergies().add(EnergyCard.builder().id("e0").name("Fire Energy")
                .energyType(EnergyType.FIRE).build());

        InPlayPokemon benched = new InPlayPokemon(basePokemon);
        benched.setDamageTaken(80);
        benched.getEnergies().add(EnergyCard.builder().id("e1").name("Water Energy")
                .energyType(EnergyType.WATER).build());
        ctx.getAttackerBench().add(benched);

        new SuperPotionEffect().apply(ctx);

        assertThat(attacker.getDamageTaken()).isEqualTo(30);
        assertThat(benched.getDamageTaken()).isEqualTo(20);
        assertThat(benched.getEnergies()).isEmpty();
    }

    @Test
    void superPotion_NoDamage_NoOp() {
        attacker.getEnergies().add(EnergyCard.builder().id("e1").name("Fire Energy")
                .energyType(EnergyType.FIRE).build());

        new SuperPotionEffect().apply(ctx);

        assertThat(attacker.getDamageTaken()).isZero();
        assertThat(attacker.getEnergies()).hasSize(1);
    }

    @Test
    void superPotion_NoEnergy_NoOp() {
        attacker.setDamageTaken(50);

        new SuperPotionEffect().apply(ctx);

        assertThat(attacker.getDamageTaken()).isEqualTo(50);
    }

    @Test
    void superPotion_HealsExactDamageWhenLessThan60() {
        attacker.setDamageTaken(30);
        attacker.getEnergies().add(EnergyCard.builder().id("e1").name("Fire Energy")
                .energyType(EnergyType.FIRE).build());

        new SuperPotionEffect().apply(ctx);

        assertThat(attacker.getDamageTaken()).isZero();
    }

    // ───────── Evosoda Effect ─────────

    @Test
    void evosoda_EvolvesBenchPokemon() {
        PokemonCard stage1 = PokemonCard.builder()
                .id("evo1").name("Ivysaur").hp(80)
                .stage(PokemonStage.STAGE_1).pokemonType(EnergyType.GRASS)
                .evolvesFrom("Bulbasaur").build();

        PokemonCard bulbasaur = PokemonCard.builder()
                .id("bulba").name("Bulbasaur").hp(60)
                .stage(PokemonStage.BASIC).pokemonType(EnergyType.GRASS).build();
        InPlayPokemon benched = new InPlayPokemon(bulbasaur);
        benched.setDamageTaken(20);
        EnergyCard grass = EnergyCard.builder().id("e1").name("Grass Energy")
                .energyType(EnergyType.GRASS).build();
        benched.getEnergies().add(grass);
        ctx.getAttackerBench().add(benched);

        ctx.getAttackerDeck().add(stage1);

        new EvosodaEffect().apply(ctx);

        assertThat(ctx.getAttackerBench().get(0).getCard().getName()).isEqualTo("Ivysaur");
        assertThat(ctx.getAttackerBench().get(0).getDamageTaken()).isEqualTo(20);
        assertThat(ctx.getAttackerBench().get(0).getEnergies()).containsExactly(grass);
        assertThat(ctx.getAttackerDeck()).isEmpty();
    }

    @Test
    void evosoda_EvolvesActivePokemon() {
        PokemonCard stage1 = PokemonCard.builder()
                .id("evo1").name("Charmeleon").hp(80)
                .stage(PokemonStage.STAGE_1).pokemonType(EnergyType.FIRE)
                .evolvesFrom("Charmander").build();

        PokemonCard charmander = PokemonCard.builder()
                .id("char").name("Charmander").hp(60)
                .stage(PokemonStage.BASIC).pokemonType(EnergyType.FIRE).build();
        ctx.setAttacker(new InPlayPokemon(charmander));

        ctx.getAttackerDeck().add(stage1);

        new EvosodaEffect().apply(ctx);

        assertThat(ctx.getAttacker().getCard().getName()).isEqualTo("Charmeleon");
    }

    @Test
    void evosoda_NoMatchingEvolution_ShufflesAndReturns() {
        PokemonCard stage1 = PokemonCard.builder()
                .id("evo1").name("Ivysaur").hp(80)
                .stage(PokemonStage.STAGE_1).pokemonType(EnergyType.GRASS)
                .evolvesFrom("Oddish").build(); // Wrong evolvesFrom
        ctx.getAttackerDeck().add(stage1);

        new EvosodaEffect().apply(ctx);

        assertThat(ctx.getAttackerDeck()).hasSize(1);
    }

    @Test
    void evosoda_NoEligiblePokemon_ShufflesAndReturns() {
        ctx.getAttackerDeck().add(ItemCard.builder().id("i1").name("Potion").build());

        new EvosodaEffect().apply(ctx);

        assertThat(ctx.getAttackerDeck()).hasSize(1);
    }

    @Test
    void evosoda_EmptyDeck_DoesNotThrow() {
        assertDoesNotThrow(() -> new EvosodaEffect().apply(ctx));
    }

    // ───────── Cassius Effect ─────────

    @Test
    void cassius_SetsPendingChoiceWithBenchedPokemon() {
        PokemonCard bulbasaur = PokemonCard.builder()
                .id("bulba").name("Bulbasaur").hp(60)
                .stage(PokemonStage.BASIC).pokemonType(EnergyType.GRASS).build();
        InPlayPokemon benched = new InPlayPokemon(bulbasaur);
        ctx.getAttackerBench().add(benched);

        new CassiusEffect().apply(ctx);

        PendingPlayerChoice choice = ctx.getPendingPlayerChoice();
        assertThat(choice).isNotNull();
        assertThat(choice.getOptions()).containsExactly(bulbasaur);
    }

    @Test
    void cassius_ShufflesChosenPokemonIntoDeck() {
        String attackerPlayerId = ctx.getAttackerPlayerId();

        PokemonCard bulbasaur = PokemonCard.builder()
                .id("bulba").name("Bulbasaur").hp(60)
                .stage(PokemonStage.BASIC).pokemonType(EnergyType.GRASS).build();
        InPlayPokemon benched = new InPlayPokemon(bulbasaur);
        EnergyCard grass = EnergyCard.builder().id("e1").name("Grass Energy")
                .energyType(EnergyType.GRASS).build();
        benched.getEnergies().add(grass);
        ItemCard tool = ItemCard.builder().id("t1").name("Muscle Band").build();
        benched.setTool(tool);
        ctx.getAttackerBench().add(benched);

        new CassiusEffect().apply(ctx);

        PendingPlayerChoice choice = ctx.getPendingPlayerChoice();
        assertThat(choice).isNotNull();
        choice.getCallback().accept(List.of("bulba"));

        assertThat(ctx.getAttackerDeck()).contains(bulbasaur, grass, tool);
        assertThat(ctx.getAttackerBench().get(0)).isNull();
    }

    @Test
    void cassius_CallbackWithEmptyChoice_NoOp() {
        PokemonCard bulbasaur = PokemonCard.builder()
                .id("bulba").name("Bulbasaur").hp(60)
                .stage(PokemonStage.BASIC).pokemonType(EnergyType.GRASS).build();
        ctx.getAttackerBench().add(new InPlayPokemon(bulbasaur));

        new CassiusEffect().apply(ctx);

        PendingPlayerChoice choice = ctx.getPendingPlayerChoice();
        assertThat(choice).isNotNull();
        assertDoesNotThrow(() -> choice.getCallback().accept(List.of()));
        assertThat(ctx.getAttackerDeck()).isEmpty();
        assertThat(ctx.getAttackerBench().get(0)).isNotNull();
    }

    @Test
    void cassius_EmptyBench_NoOp() {
        new CassiusEffect().apply(ctx);

        assertThat(ctx.getPendingPlayerChoice()).isNull();
    }
}
