package ar.edu.utn.frc.tup.piii.models.effects.trainer;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

import java.util.ArrayList;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import ar.edu.utn.frc.tup.piii.models.EnergyType;
import ar.edu.utn.frc.tup.piii.models.GameContext;
import ar.edu.utn.frc.tup.piii.models.InPlayPokemon;
import ar.edu.utn.frc.tup.piii.models.PokemonCard;
import ar.edu.utn.frc.tup.piii.models.PokemonStage;

class TrainerMarkerEffectsTest {

    private GameContext ctx;

    @BeforeEach
    void setUp() {
        PokemonCard base = PokemonCard.builder()
                .id("p1").name("TestMon").hp(100)
                .stage(PokemonStage.BASIC).pokemonType(EnergyType.COLORLESS)
                .build();
        InPlayPokemon attacker = new InPlayPokemon(base);
        InPlayPokemon defender = new InPlayPokemon(base);
        ctx = new GameContext();
        ctx.setAttacker(attacker);
        ctx.setDefender(defender);
        ctx.setAttackerBench(new ArrayList<>());
        ctx.setDefenderBench(new ArrayList<>());
    }

    @Test
    void fairyGarden_Apply_DoesNotThrow() {
        FairyGardenEffect effect = new FairyGardenEffect();
        assertDoesNotThrow(() -> effect.apply(ctx));
    }

    @Test
    void hardCharm_Apply_DoesNotThrow() {
        HardCharmEffect effect = new HardCharmEffect();
        assertDoesNotThrow(() -> effect.apply(ctx));
    }

    @Test
    void muscleBand_Apply_DoesNotThrow() {
        MuscleBandEffect effect = new MuscleBandEffect();
        assertDoesNotThrow(() -> effect.apply(ctx));
    }

    @Test
    void shadowCircle_Apply_DoesNotThrow() {
        ShadowCircleEffect effect = new ShadowCircleEffect();
        assertDoesNotThrow(() -> effect.apply(ctx));
    }
}
