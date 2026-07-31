package ar.edu.utn.frc.tup.piii.models;

/**
 * Special conditions a Pokémon can be affected by during battle.
 *
 * <p>A Pokémon may hold multiple conditions simultaneously only when a card text
 * explicitly grants both (e.g. Crisis Vine applies PARALYZED and POISONED at once).
 * Normally applying a new condition replaces any existing one.
 */
public enum StatusCondition {
    POISONED,
    BURNED,
    PARALYZED,
    ASLEEP,
    CONFUSED
}
