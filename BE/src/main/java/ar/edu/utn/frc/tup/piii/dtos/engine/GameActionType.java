package ar.edu.utn.frc.tup.piii.dtos.engine;

/**
 * Supported actions that can be submitted to the engine facade.
 *
 * <p>The first group is legal during the main phase ({@link #PLAY_BASIC}, {@link #EVOLVE},
 * {@link #ATTACH_ENERGY}, {@link #PLAY_ITEM}, {@link #PLAY_SUPPORTER} and the
 * {@link #ADVANCE_TO_ATTACK} transition); the second group is legal during the attack phase
 * ({@link #ATTACK}, {@link #END_TURN}).
 */
public enum GameActionType {
    CHOOSE_COIN_SIDE,
    CHOOSE_FIRST_TURN,
    PLAY_BASIC,
    EVOLVE,
    ATTACH_ENERGY,
    PLAY_ITEM,
    PLAY_SUPPORTER,
    PLAY_STADIUM,
    ATTACH_TOOL,
    RETREAT,
    ACTIVATE_ABILITY,
    ADVANCE_TO_ATTACK,
    ATTACK,
    EXECUTE_ATTACK,
    RESOLVE_PLAYER_CHOICE,
    END_TURN
}
