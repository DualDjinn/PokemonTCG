package ar.edu.utn.frc.tup.piii.engine.choice;

/**
 * Categories of player-input prompts the engine can raise mid-action.
 *
 * <p>The {@link PendingPlayerChoice} carries one of these so the frontend can
 * render the appropriate modal (card picker, energy picker, bench picker,
 * reorder grid, or coin-flip info display).
 */
public enum ChoiceType {
    /** "Look at the top N of your deck and pick up to {@code maxPick} of a given filter." */
    PICK_FROM_DECK_TOP_N,
    /** "Search your deck for up to {@code maxPick} basic energy cards." */
    PICK_BASIC_ENERGIES_FROM_DECK,
    /** "Choose one of your benched Pokémon." */
    PICK_BENCHED_POKEMON,
    /** "Reorder the top N cards of your deck." */
    REORDER_DECK_TOP_N,
    /** Display-only: show a coin flip animation with the predetermined result. */
    COIN_FLIP_INFO,
    /** Player chooses heads or tails before the coin is flipped. */
    CHOOSE_COIN_SIDE,
    /** "Pick a Pokémon from your discard pile." */
    PICK_FROM_DISCARD
}
