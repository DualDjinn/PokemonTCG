package ar.edu.utn.frc.tup.piii.models;

/**
 * Cómo se adquiere un dorso de carta.
 */
public enum CardBackOrigin {
    /** Se otorga a todos los jugadores desde el inicio. */
    DEFAULT,
    /** Se compra con monedas en la tienda. */
    SHOP,
    /** Aparece como bonus al abrir sobres. */
    PACK,
    /** Se otorga al ganar cierta cantidad de batallas. */
    BATTLE,
    /** Se desbloquea al alcanzar cierto nivel. */
    LEVEL
}
