package ar.edu.utn.frc.tup.piii.engine.attackresolution;

import ar.edu.utn.frc.tup.piii.engine.TriggeredAbilityDispatcher;
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
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
class ApplyDamageHandlerTest {

    @Mock
    private TriggeredAbilityDispatcher mockDispatcher;

    @Test
    void positiveDamage_AddsToDefender() {
        InPlayPokemon defender = pokemon("defender");
        ApplyDamageHandler handler = new ApplyDamageHandler(mockDispatcher, null);
        AttackResolutionContext ctx = context(50, defender);

        handler.handle(ctx);

        assertThat(defender.getDamageTaken()).isEqualTo(50);
    }

    @Test
    void zeroDamage_DoesNotApply() {
        InPlayPokemon defender = pokemon("defender");
        ApplyDamageHandler handler = new ApplyDamageHandler(mockDispatcher, null);
        AttackResolutionContext ctx = context(0, defender);

        handler.handle(ctx);

        assertThat(defender.getDamageTaken()).isZero();
    }

    @Test
    void callsOnDamagedOnDispatcher() {
        InPlayPokemon defender = pokemon("defender");
        ApplyDamageHandler handler = new ApplyDamageHandler(mockDispatcher, null);
        AttackResolutionContext ctx = context(50, defender);

        handler.handle(ctx);

        verify(mockDispatcher).onDamaged(ctx.getGameContext(), defender, ctx.getAttacker());
    }

    @Test
    void zeroDamage_DoesNotCallOnDamaged() {
        InPlayPokemon defender = pokemon("defender");
        ApplyDamageHandler handler = new ApplyDamageHandler(mockDispatcher, null);
        AttackResolutionContext ctx = context(0, defender);

        handler.handle(ctx);

        verifyNoInteractions(mockDispatcher);
    }

    @Test
    void chain_CallsNextHandler() {
        InPlayPokemon defender = pokemon("defender");
        AtomicBoolean nextCalled = new AtomicBoolean(false);
        AttackResolutionHandler nextHandler = context -> nextCalled.set(true);
        ApplyDamageHandler chained = new ApplyDamageHandler(mockDispatcher, nextHandler);

        chained.handle(context(10, defender));

        assertThat(nextCalled).isTrue();
    }

    private AttackResolutionContext context(int finalDamage, InPlayPokemon defender) {
        GameContext gc = new GameContext();
        InPlayPokemon attacker = pokemon("attacker");
        gc.setAttacker(attacker);
        gc.setDefender(defender);
        Attack atk = Attack.builder()
                .name("Tackle").energyCost(Map.of()).baseDamage(40).build();
        AttackResolutionContext ctx = new AttackResolutionContext(gc, atk, attacker, defender);
        ctx.setFinalDamage(finalDamage);
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
