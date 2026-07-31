package ar.edu.utn.frc.tup.piii.engine.attackresolution;

import ar.edu.utn.frc.tup.piii.models.Attack;
import ar.edu.utn.frc.tup.piii.models.GameContext;
import ar.edu.utn.frc.tup.piii.models.InPlayPokemon;
import ar.edu.utn.frc.tup.piii.models.PokemonCard;
import ar.edu.utn.frc.tup.piii.models.PokemonStage;
import ar.edu.utn.frc.tup.piii.models.EnergyType;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ApplyDefenderProtectionHandlerTest {

    private final ApplyDefenderProtectionHandler handler = new ApplyDefenderProtectionHandler(null);

    @Test
    void preventAllEffects_SetsFinalDamageToZero() {
        InPlayPokemon defender = pokemon("defender");
        defender.setPreventAllEffects(true);
        AttackResolutionContext ctx = context(50, defender);

        handler.handle(ctx);

        assertThat(ctx.getFinalDamage()).isZero();
    }

    @Test
    void preventAllIncomingDamage_SetsFinalDamageToZero() {
        InPlayPokemon defender = pokemon("defender");
        defender.setPreventAllIncomingDamage(true);
        AttackResolutionContext ctx = context(50, defender);

        handler.handle(ctx);

        assertThat(ctx.getFinalDamage()).isZero();
    }

    @Test
    void preventByThreshold_WhenAtOrBelow_ZeroesDamage() {
        InPlayPokemon defender = pokemon("defender");
        defender.setPreventIncomingDamageThreshold(60);
        AttackResolutionContext ctx = context(50, defender);

        handler.handle(ctx);

        assertThat(ctx.getFinalDamage()).isZero();
    }

    @Test
    void preventByThreshold_WhenAbove_DoesNotPrevent() {
        InPlayPokemon defender = pokemon("defender");
        defender.setPreventIncomingDamageThreshold(60);
        AttackResolutionContext ctx = context(70, defender);

        handler.handle(ctx);

        assertThat(ctx.getFinalDamage()).isEqualTo(70);
    }

    @Test
    void reduceIncomingDamage_SubtractsReduction() {
        InPlayPokemon defender = pokemon("defender");
        defender.setIncomingDamageReduction(20);
        AttackResolutionContext ctx = context(50, defender);

        handler.handle(ctx);

        assertThat(ctx.getFinalDamage()).isEqualTo(30);
    }

    @Test
    void reduceIncomingDamage_ClampsToZero() {
        InPlayPokemon defender = pokemon("defender");
        defender.setIncomingDamageReduction(20);
        AttackResolutionContext ctx = context(10, defender);

        handler.handle(ctx);

        assertThat(ctx.getFinalDamage()).isZero();
    }

    @Test
    void noProtection_PassesCalculatedThrough() {
        InPlayPokemon defender = pokemon("defender");
        AttackResolutionContext ctx = context(45, defender);

        handler.handle(ctx);

        assertThat(ctx.getFinalDamage()).isEqualTo(45);
    }

    @Test
    void chain_CallsNextHandler() {
        AtomicBoolean nextCalled = new AtomicBoolean(false);
        AttackResolutionHandler nextHandler = context -> nextCalled.set(true);
        ApplyDefenderProtectionHandler chained = new ApplyDefenderProtectionHandler(nextHandler);

        chained.handle(context(40, pokemon("d")));

        assertThat(nextCalled).isTrue();
    }

    private static AttackResolutionContext context(int calculatedDamage, InPlayPokemon defender) {
        GameContext gc = new GameContext();
        InPlayPokemon attacker = pokemon("attacker");
        gc.setAttacker(attacker);
        gc.setDefender(defender);
        Attack atk = Attack.builder()
                .name("Tackle").energyCost(Map.of()).baseDamage(calculatedDamage).build();
        AttackResolutionContext ctx = new AttackResolutionContext(gc, atk, attacker, defender);
        ctx.setCalculatedDamage(calculatedDamage);
        return ctx;
    }

    private static InPlayPokemon pokemon(String id) {
        PokemonCard card = PokemonCard.builder()
                .id(id).name(id).hp(100)
                .stage(PokemonStage.BASIC).pokemonType(EnergyType.FIRE)
                .attacks(List.of()).abilities(List.of())
                .weaknesses(List.of()).resistances(List.of())
                .costToAttack(List.of()).retreatCost(0)
                .build();
        return new InPlayPokemon(card);
    }
}
