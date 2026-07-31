package ar.edu.utn.frc.tup.piii.engine.attackresolution;

import ar.edu.utn.frc.tup.piii.models.Attack;
import ar.edu.utn.frc.tup.piii.models.GameContext;
import ar.edu.utn.frc.tup.piii.models.InPlayPokemon;
import java.util.Objects;

/**
 * Mutable state passed through each attack-resolution handler.
 *
 * <p>The context wraps the oriented {@link GameContext}, the selected {@link Attack} and the
 * already validated attacker/defender pair. Handlers communicate by updating the intermediate and
 * final damage fields instead of re-reading or re-deriving previous stages.
 */
public final class AttackResolutionContext {

    private final GameContext gameContext;
    private final Attack attack;
    private final InPlayPokemon attacker;
    private final InPlayPokemon defender;
    private int calculatedDamage;
    private int finalDamage;

    /**
     * Creates a new per-attack resolution context.
     *
     * @param gameContext oriented match context
     * @param attack selected attack
     * @param attacker attacking Pokémon
     * @param defender defending Pokémon
     */
    public AttackResolutionContext(GameContext gameContext,
                                   Attack attack,
                                   InPlayPokemon attacker,
                                   InPlayPokemon defender) {
        this.gameContext = Objects.requireNonNull(gameContext, "gameContext must not be null");
        this.attack = Objects.requireNonNull(attack, "attack must not be null");
        this.attacker = Objects.requireNonNull(attacker, "attacker must not be null");
        this.defender = Objects.requireNonNull(defender, "defender must not be null");
        this.calculatedDamage = 0;
        this.finalDamage = 0;
    }

    public GameContext getGameContext() {
        return gameContext;
    }

    public Attack getAttack() {
        return attack;
    }

    public InPlayPokemon getAttacker() {
        return attacker;
    }

    public InPlayPokemon getDefender() {
        return defender;
    }

    public int getCalculatedDamage() {
        return calculatedDamage;
    }

    public void setCalculatedDamage(int calculatedDamage) {
        this.calculatedDamage = calculatedDamage;
    }

    public int getFinalDamage() {
        return finalDamage;
    }

    public void setFinalDamage(int finalDamage) {
        this.finalDamage = finalDamage;
    }
}
