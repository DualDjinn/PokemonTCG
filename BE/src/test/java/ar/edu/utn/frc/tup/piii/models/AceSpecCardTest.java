package ar.edu.utn.frc.tup.piii.models;

import org.junit.jupiter.api.Test;
import java.util.List;
import static org.assertj.core.api.Assertions.assertThat;

class AceSpecCardTest {

    @Test
    void getSubtype_ReturnsItemAceSpec() {
        AceSpecCard card = AceSpecCard.builder().id("ace-1").name("Computer Search").build();
        assertThat(card.getSubtype()).isEqualTo("Item (ACE SPEC)");
    }

    @Test
    void getPlayRule_ExtendsItemRule() {
        AceSpecCard card = AceSpecCard.builder().id("ace-1").name("Computer Search").build();
        assertThat(card.getPlayRule()).contains("only 1 ACE SPEC card in your deck");
    }

    @Test
    void builder_WithAllFields() {
        AceSpecCard card = AceSpecCard.builder()
                .id("ace-2").name("Gold Potion")
                .setCode("xy1").number("99")
                .rarity(CardRarity.ACE_SPEC_RARE)
                .imageUrl("http://example.com/small.png")
                .imageUrlLarge("http://example.com/large.png")
                .rules(List.of("Heal all damage"))
                .artist("Ken Sugimori")
                .build();

        assertThat(card.getId()).isEqualTo("ace-2");
        assertThat(card.getName()).isEqualTo("Gold Potion");
        assertThat(card.getSetCode()).isEqualTo("xy1");
        assertThat(card.getSubtype()).isEqualTo("Item (ACE SPEC)");
        assertThat(card.getRules()).containsExactly("Heal all damage");
        assertThat(card.getArtist()).isEqualTo("Ken Sugimori");
    }

    @Test
    void supertype_IsTrainer() {
        AceSpecCard card = AceSpecCard.builder().id("ace-1").name("Computer Search").build();
        assertThat(card.getSupertype()).isEqualTo(Supertype.TRAINER);
    }

    @Test
    void getRules_ReturnsRules() {
        AceSpecCard card = AceSpecCard.builder()
                .id("ace-1").name("Computer Search")
                .rules(List.of("Search your deck"))
                .build();
        assertThat(card.getRules()).containsExactly("Search your deck");
    }

    @Test
    void noArgsConstructor_CreatesInstance() {
        AceSpecCard card = new AceSpecCard();
        assertThat(card).isNotNull();
        assertThat(card.getSupertype()).isEqualTo(Supertype.TRAINER);
    }
}
