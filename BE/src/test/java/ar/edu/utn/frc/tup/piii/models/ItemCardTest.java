package ar.edu.utn.frc.tup.piii.models;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class ItemCardTest {

    @Test
    void getSubtype_ReturnsItem() {
        ItemCard card = ItemCard.builder().id("item-1").name("Potion").build();
        assertThat(card.getSubtype()).isEqualTo("Item");
    }

    @Test
    void getPlayRule_ReturnsItemRule() {
        ItemCard card = ItemCard.builder().id("item-1").name("Potion").build();
        assertThat(card.getPlayRule()).contains("play as many Item cards");
    }
}
