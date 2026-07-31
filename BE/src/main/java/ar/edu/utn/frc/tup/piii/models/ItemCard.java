package ar.edu.utn.frc.tup.piii.models;

import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

/**
 * Carta de Entrenador del subtipo Item.
 * Ejemplos del JSON: Evosoda, Great Ball, Max Revive, Super Potion, Roller Skates, Red Card.
 */
@SuperBuilder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@DiscriminatorValue("ITEM")
public class ItemCard extends TrainerCard {

    @Override
    public String getSubtype() {
        return "Item";
    }

    @Override
    public String getPlayRule() {
        return "You may play as many Item cards as you like during your turn (before your attack).";
    }
}
