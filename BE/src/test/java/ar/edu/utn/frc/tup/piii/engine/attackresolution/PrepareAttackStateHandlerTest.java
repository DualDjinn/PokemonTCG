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
import java.util.function.IntUnaryOperator;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PrepareAttackStateHandlerTest {

    private final PrepareAttackStateHandler handler = new PrepareAttackStateHandler(null);

    @Test
    void doHandle_SetsPendingDamageFromAttack() {
        GameContext ctx = baseContext();
        Attack attack = attack(60);
        AttackResolutionContext resolution = resolution(ctx, attack);

        handler.handle(resolution);

        assertThat(ctx.getPendingDamage()).isEqualTo(60);
    }

    @Test
    void doHandle_ResetsIgnoreWeakness() {
        GameContext ctx = baseContext();
        ctx.setIgnoreWeakness(true);
        AttackResolutionContext resolution = resolution(ctx, attack(40));

        handler.handle(resolution);

        assertThat(ctx.isIgnoreWeakness()).isFalse();
    }

    @Test
    void doHandle_ResetsIgnoreResistance() {
        GameContext ctx = baseContext();
        ctx.setIgnoreResistance(true);
        AttackResolutionContext resolution = resolution(ctx, attack(40));

        handler.handle(resolution);

        assertThat(ctx.isIgnoreResistance()).isFalse();
    }

    @Test
    void doHandle_ClearsDamageCalculationModifiers() {
        GameContext ctx = baseContext();
        ctx.addDamageCalculationModifier(d -> d + 10);
        ctx.addDamageCalculationModifier(d -> d * 2);
        AttackResolutionContext resolution = resolution(ctx, attack(40));

        handler.handle(resolution);

        assertThat(ctx.getDamageCalculationModifiers()).isEmpty();
    }

    @Test
    void doHandle_ZeroesCalculatedAndFinalDamage() {
        GameContext ctx = baseContext();
        AttackResolutionContext resolution = resolution(ctx, attack(40));
        resolution.setCalculatedDamage(999);
        resolution.setFinalDamage(999);

        handler.handle(resolution);

        assertThat(resolution.getCalculatedDamage()).isZero();
        assertThat(resolution.getFinalDamage()).isZero();
    }

    @Test
    void chain_CallsNextHandler() {
        AtomicBoolean nextCalled = new AtomicBoolean(false);
        AttackResolutionHandler nextHandler = context -> nextCalled.set(true);
        PrepareAttackStateHandler chained = new PrepareAttackStateHandler(nextHandler);

        chained.handle(resolution(baseContext(), attack(40)));

        assertThat(nextCalled).isTrue();
    }

    @Test
    void handle_NullContext_Throws() {
        assertThatThrownBy(() -> handler.handle(null))
                .isInstanceOf(NullPointerException.class);
    }

    private static GameContext baseContext() {
        GameContext ctx = new GameContext();
        InPlayPokemon p1 = pokemon("attacker");
        InPlayPokemon p2 = pokemon("defender");
        ctx.setAttacker(p1);
        ctx.setDefender(p2);
        return ctx;
    }

    private static AttackResolutionContext resolution(GameContext ctx, Attack attack) {
        return new AttackResolutionContext(ctx, attack, ctx.getAttacker(), ctx.getDefender());
    }

    private static Attack attack(int baseDamage) {
        return Attack.builder()
                .name("Test Attack")
                .energyCost(Map.of())
                .baseDamage(baseDamage)
                .build();
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
