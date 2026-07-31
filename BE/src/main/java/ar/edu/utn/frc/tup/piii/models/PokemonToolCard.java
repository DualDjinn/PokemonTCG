package ar.edu.utn.frc.tup.piii.models;

import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

/**
 * Carta de Entrenador del subtipo Pokémon Tool.
 * Ejemplos del JSON: Hard Charm, Muscle Band.
 */
@SuperBuilder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@DiscriminatorValue("POKEMON_TOOL")
public class PokemonToolCard extends TrainerCard {

    @Override
    public String getSubtype() {
        return "Pokémon Tool";
    }

    @Override
    public String getPlayRule() {
        return "Attach a Pokémon Tool to 1 of your Pokémon that doesn't already have a Pokémon Tool "
             + "attached to it. You may play as many Item cards as you like during your turn "
             + "(before your attack).";
    }
}
