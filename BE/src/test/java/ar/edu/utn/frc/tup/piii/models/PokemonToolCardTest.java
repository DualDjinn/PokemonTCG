package ar.edu.utn.frc.tup.piii.models;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class PokemonToolCardTest {

    @Test
    void getSubtype_ReturnsPokemonTool() {
        PokemonToolCard card = PokemonToolCard.builder().id("tool-1").name("Muscle Band").build();
        assertThat(card.getSubtype()).isEqualTo("Pokémon Tool");
    }

    @Test
    void getPlayRule_ReturnsToolRule() {
        PokemonToolCard card = PokemonToolCard.builder().id("tool-1").name("Muscle Band").build();
        assertThat(card.getPlayRule()).contains("Attach a Pokémon Tool");
    }
}
