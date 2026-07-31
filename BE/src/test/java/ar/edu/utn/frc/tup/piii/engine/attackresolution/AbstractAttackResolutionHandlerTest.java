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
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AbstractAttackResolutionHandlerTest {

    private final AttackResolutionHandler next = context -> {
        // no-op next handler
    };

    @Test
    void handle_CallsDoHandle() {
        AtomicBoolean called = new AtomicBoolean(false);
        AbstractAttackResolutionHandler handler = new AbstractAttackResolutionHandler(null) {
            @Override
            protected void doHandle(AttackResolutionContext context) {
                called.set(true);
            }
        };
        handler.handle(context());
        assertThat(called).isTrue();
    }

    @Test
    void handle_NullContext_Throws() {
        AbstractAttackResolutionHandler handler = new AbstractAttackResolutionHandler(null) {
            @Override
            protected void doHandle(AttackResolutionContext context) {
            }
        };
        assertThatThrownBy(() -> handler.handle(null))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void handle_WithNext_CallsNextHandler() {
        AtomicBoolean nextCalled = new AtomicBoolean(false);
        AttackResolutionHandler nextHandler = context -> nextCalled.set(true);
        AbstractAttackResolutionHandler handler = new AbstractAttackResolutionHandler(nextHandler) {
            @Override
            protected void doHandle(AttackResolutionContext context) {
            }
        };
        handler.handle(context());
        assertThat(nextCalled).isTrue();
    }

    @Test
    void handle_WithNullNext_DoesNotThrow() {
        AbstractAttackResolutionHandler handler = new AbstractAttackResolutionHandler(null) {
            @Override
            protected void doHandle(AttackResolutionContext context) {
            }
        };
        handler.handle(context());
    }

    private static AttackResolutionContext context() {
        GameContext gc = new GameContext();
        Attack atk = Attack.builder()
                .name("Tackle").energyCost(Map.of()).baseDamage(40).build();
        InPlayPokemon p1 = pokemon("a");
        InPlayPokemon p2 = pokemon("b");
        gc.setAttacker(p1);
        gc.setDefender(p2);
        return new AttackResolutionContext(gc, atk, p1, p2);
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
