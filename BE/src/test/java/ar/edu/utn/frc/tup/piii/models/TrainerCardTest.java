package ar.edu.utn.frc.tup.piii.models;

import org.junit.jupiter.api.Test;
import java.util.List;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TrainerCardTest {

    @Test
    void getSupertype_ReturnsTrainer() {
        ItemCard card = ItemCard.builder().id("item-1").name("Potion").build();
        assertThat(card.getSupertype()).isEqualTo(Supertype.TRAINER);
    }

    @Test
    void getRules_ReturnsUnmodifiableList() {
        ItemCard card = ItemCard.builder()
                .id("item-1").name("Potion")
                .rules(List.of("Heal 30 damage"))
                .build();
        assertThat(card.getRules()).containsExactly("Heal 30 damage");
    }

    @Test
    void getRules_WhenNull_ReturnsEmptyList() {
        ItemCard card = ItemCard.builder().id("item-1").name("Potion").build();
        assertThat(card.getRules()).isEmpty();
    }

    @Test
    void getRules_ReturnsUnmodifiable() {
        ItemCard card = ItemCard.builder()
                .id("item-1").name("Potion")
                .rules(List.of("Rule"))
                .build();
        assertThatThrownBy(() -> card.getRules().add("new"))
                .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void getArtist_ReturnsArtist() {
        ItemCard card = ItemCard.builder()
                .id("item-1").name("Potion")
                .artist("Ken Sugimori")
                .build();
        assertThat(card.getArtist()).isEqualTo("Ken Sugimori");
    }

    @Test
    void getArtist_WhenNull_ReturnsNull() {
        ItemCard card = ItemCard.builder().id("item-1").name("Potion").build();
        assertThat(card.getArtist()).isNull();
    }

    @Test
    void canBePlayed_WithNullContext_ReturnsFalse() {
        ItemCard card = ItemCard.builder().id("item-1").name("Potion").build();
        assertThat(card.canBePlayed(null)).isFalse();
    }

    @Test
    void canBePlayed_DuringMainPhase_ReturnsTrue() {
        ItemCard card = ItemCard.builder().id("item-1").name("Potion").build();
        GameContext ctx = new GameContext();
        ctx.setCurrentPhase(TurnPhase.MAIN);
        assertThat(card.canBePlayed(ctx)).isTrue();
    }

    @Test
    void canBePlayed_OutsideMainPhase_ReturnsFalse() {
        ItemCard card = ItemCard.builder().id("item-1").name("Potion").build();
        GameContext ctx = new GameContext();
        ctx.setCurrentPhase(TurnPhase.ATTACK);
        assertThat(card.canBePlayed(ctx)).isFalse();
    }

    @Test
    void toString_ContainsNameAndSubtype() {
        ItemCard card = ItemCard.builder().id("item-1").name("Potion").build();
        String str = card.toString();
        assertThat(str).contains("Potion", "Item");
    }

    @Test
    void builder_InheritsCardFields() {
        SupporterCard card = SupporterCard.builder()
                .id("sup-1").name("Professor Sycamore")
                .setCode("xy1").number("10")
                .rarity(CardRarity.UNCOMMON)
                .imageUrl("http://example.com/small.png")
                .imageUrlLarge("http://example.com/large.png")
                .rules(List.of("Discard hand, draw 7"))
                .artist("Ken Sugimori")
                .build();

        assertThat(card.getId()).isEqualTo("sup-1");
        assertThat(card.getName()).isEqualTo("Professor Sycamore");
        assertThat(card.getSetCode()).isEqualTo("xy1");
        assertThat(card.getNumber()).isEqualTo("10");
        assertThat(card.getRarity()).isEqualTo(CardRarity.UNCOMMON);
        assertThat(card.getImageUrl()).isEqualTo("http://example.com/small.png");
        assertThat(card.getImageUrlLarge()).isEqualTo("http://example.com/large.png");
        assertThat(card.getRules()).containsExactly("Discard hand, draw 7");
        assertThat(card.getArtist()).isEqualTo("Ken Sugimori");
    }
}
