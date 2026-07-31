package ar.edu.utn.frc.tup.piii.engine.turnstates;

import ar.edu.utn.frc.tup.piii.dtos.engine.GameAction;
import ar.edu.utn.frc.tup.piii.dtos.engine.GameActionType;
import ar.edu.utn.frc.tup.piii.engine.StatusEffectManager;
import ar.edu.utn.frc.tup.piii.models.GameContext;
import ar.edu.utn.frc.tup.piii.models.InPlayPokemon;
import ar.edu.utn.frc.tup.piii.models.TurnPhase;
import java.util.Objects;
import java.util.Set;

/**
 * State object for the between-turns phase.
 *
 * <p>This is a transient phase entered once the active player ends their turn. On
 * {@link #enter(GameContext)} it resolves the end-of-turn special conditions of both active Pokémon
 * via the {@link StatusEffectManager} in the XY1 order (Poisoned → Burned → Asleep → Paralyzed). The
 * remaining between-turns responsibilities that require match-level state — knockout resolution,
 * victory evaluation and rotating the active player into the next draw phase — stay in the turn
 * manager because they reach beyond a single {@link GameContext}. No player action is accepted while
 * the turn rests in this phase.
 */
public final class BetweenTurnsPhaseState implements TurnPhaseState {

    private final StatusEffectManager statusEffectManager;

    /**
     * Creates the between-turns state with the status effect collaborator it delegates to.
     *
     * @param statusEffectManager manager that applies end-of-turn special conditions; must not be
     *                            {@code null}
     */
    public BetweenTurnsPhaseState(StatusEffectManager statusEffectManager) {
        this.statusEffectManager = Objects.requireNonNull(statusEffectManager,
                "statusEffectManager must not be null");
    }

    @Override
    public TurnPhase getPhase() {
        return TurnPhase.BETWEEN_TURNS;
    }

    @Override
    public Set<GameActionType> getValidActions() {
        return Set.of();
    }

    @Override
    public void enter(GameContext context) {
        Objects.requireNonNull(context, "context must not be null");
        processStatuses(context.getAttacker(), context);
        processStatuses(context.getDefender(), context);
    }

    @Override
    public void exit(GameContext context) {
        Objects.requireNonNull(context, "context must not be null");
    }

    @Override
    public void handle(GameAction action, TurnPhaseActionContext context) {
        throw new IllegalArgumentException("No actions can be submitted during the between-turns phase");
    }

    private void processStatuses(InPlayPokemon pokemon, GameContext context) {
        if (pokemon != null) {
            statusEffectManager.processEndOfTurnEffects(pokemon, context);
        }
    }
}
