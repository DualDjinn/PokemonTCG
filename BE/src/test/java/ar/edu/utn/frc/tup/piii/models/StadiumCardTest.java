package ar.edu.utn.frc.tup.piii.models;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class StadiumCardTest {

    @Test
    void getSubtype_ReturnsStadium() {
        StadiumCard card = StadiumCard.builder().id("stad-1").name("Fairy Garden").build();
        assertThat(card.getSubtype()).isEqualTo("Stadium");
    }

    @Test
    void getPlayRule_ReturnsStadiumRule() {
        StadiumCard card = StadiumCard.builder().id("stad-1").name("Fairy Garden").build();
        assertThat(card.getPlayRule()).contains("stays in play");
    }

    private StadiumCard stadium(String id, String name) {
        return StadiumCard.builder().id(id).name(name).build();
    }

    @Test
    void canBePlayed_WithNullContext_ReturnsFalse() {
        assertThat(stadium("stad-1", "Fairy Garden").canBePlayed(null)).isFalse();
    }

    @Test
    void canBePlayed_OutsideMainPhase_ReturnsFalse() {
        GameContext ctx = new GameContext();
        ctx.setCurrentPhase(TurnPhase.ATTACK);
        assertThat(stadium("stad-1", "Fairy Garden").canBePlayed(ctx)).isFalse();
    }

    @Test
    void canBePlayed_WhenNoActiveStadium_ReturnsTrue() {
        GameContext ctx = new GameContext(); // MAIN, sin estadio activo
        assertThat(stadium("stad-1", "Fairy Garden").canBePlayed(ctx)).isTrue();
    }

    @Test
    void canBePlayed_WhenDifferentStadiumActive_ReturnsTrue() {
        GameContext ctx = new GameContext();
        ctx.setActiveStadium(stadium("stad-2", "Shadow Circle"));
        assertThat(stadium("stad-1", "Fairy Garden").canBePlayed(ctx)).isTrue();
    }

    @Test
    void canBePlayed_WhenSameNamedStadiumActive_ReturnsFalse() {
        GameContext ctx = new GameContext();
        ctx.setActiveStadium(stadium("stad-2", "Fairy Garden"));
        assertThat(stadium("stad-1", "Fairy Garden").canBePlayed(ctx)).isFalse();
    }
}
