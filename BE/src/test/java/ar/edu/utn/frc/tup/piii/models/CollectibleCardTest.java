package ar.edu.utn.frc.tup.piii.models;

import org.junit.jupiter.api.Test;
import java.util.List;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link CollectibleCard}.
 *
 * <p>Cubre el override de {@link CollectibleCard#getSupertype()} (siempre
 * {@link Supertype#COLLECTIBLE}), el de {@link CollectibleCard#canBePlayed(GameContext)}
 * (nunca jugable) y los accesores de {@code rules}/{@code artist}.
 */
class CollectibleCardTest {

    private CollectibleCard build() {
        return CollectibleCard.builder()
                .id("col-1").name("Promo Pikachu")
                .setCode("xy1").number("RC1")
                .build();
    }

    @Test
    void getSupertype_ReturnsCollectible() {
        assertThat(build().getSupertype()).isEqualTo(Supertype.COLLECTIBLE);
    }

    @Test
    void canBePlayed_WithNullContext_ReturnsFalse() {
        assertThat(build().canBePlayed(null)).isFalse();
    }

    @Test
    void canBePlayed_WithMainPhaseContext_ReturnsFalse() {
        GameContext ctx = new GameContext();
        ctx.setCurrentPhase(TurnPhase.MAIN);
        assertThat(build().canBePlayed(ctx)).isFalse();
    }

    @Test
    void rulesAndArtist_GettersAndSetters() {
        CollectibleCard card = CollectibleCard.builder()
                .id("col-2").name("Energy Promo")
                .rules(List.of("Collectible only"))
                .artist("Ken Sugimori")
                .build();

        assertThat(card.getRules()).containsExactly("Collectible only");
        assertThat(card.getArtist()).isEqualTo("Ken Sugimori");

        card.setRules(List.of("Updated rule"));
        card.setArtist("Mitsuhiro Arita");
        assertThat(card.getRules()).containsExactly("Updated rule");
        assertThat(card.getArtist()).isEqualTo("Mitsuhiro Arita");
    }

    @Test
    void inheritedCardFields_AreAccessible() {
        CollectibleCard card = build();
        assertThat(card.getId()).isEqualTo("col-1");
        assertThat(card.getName()).isEqualTo("Promo Pikachu");
        assertThat(card.getSetCode()).isEqualTo("xy1");
        assertThat(card.getNumber()).isEqualTo("RC1");
    }
}
