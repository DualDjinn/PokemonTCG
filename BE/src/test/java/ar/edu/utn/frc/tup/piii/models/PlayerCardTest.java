package ar.edu.utn.frc.tup.piii.models;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Smoke tests for {@link PlayerCard} entity.
 *
 * <p>Valida getters/setters y la igualdad/hashCode generados por Lombok.
 */
class PlayerCardTest {

    @Test
    void noArgsConstructorCreatesEmptyInstance() {
        PlayerCard pc = new PlayerCard();
        assertThat(pc.getId()).isNull();
        assertThat(pc.getPlayer()).isNull();
        assertThat(pc.getCard()).isNull();
        assertThat(pc.getQuantity()).isZero();
    }

    @Test
    void allArgsConstructorSetsAllFields() {
        Player player = new Player();
        player.setId("p1");

        PokemonCard card = PokemonCard.builder()
                .id("c1").name("Test").hp(60)
                .stage(PokemonStage.BASIC).isEx(false).isMega(false)
                .pokemonType(EnergyType.GRASS)
                .build();

        PlayerCard pc = new PlayerCard(42L, player, card, 5, null);

        assertThat(pc.getId()).isEqualTo(42L);
        assertThat(pc.getPlayer()).isSameAs(player);
        assertThat(pc.getCard()).isSameAs(card);
        assertThat(pc.getQuantity()).isEqualTo(5);
        assertThat(pc.getActiveSkin()).isNull();
    }

    @Test
    void settersUpdateFields() {
        PlayerCard pc = new PlayerCard();
        Player player = new Player();
        player.setId("p1");

        pc.setId(99L);
        pc.setPlayer(player);
        pc.setQuantity(3);

        assertThat(pc.getId()).isEqualTo(99L);
        assertThat(pc.getPlayer().getId()).isEqualTo("p1");
        assertThat(pc.getQuantity()).isEqualTo(3);
    }

    @Test
    void equalsAndHashCodeBasedOnAllFields() {
        Player player = new Player();
        player.setId("p1");

        PlayerCard a = new PlayerCard();
        a.setId(1L);
        a.setPlayer(player);
        a.setQuantity(2);

        PlayerCard b = new PlayerCard();
        b.setId(1L);
        b.setPlayer(player);
        b.setQuantity(2);

        assertThat(a).isEqualTo(b);
        assertThat(a.hashCode()).isEqualTo(b.hashCode());
    }
}
