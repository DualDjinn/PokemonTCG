package ar.edu.utn.frc.tup.piii.dtos.player;

import org.junit.jupiter.api.Test;
import java.time.LocalDate;
import java.util.List;
import static org.assertj.core.api.Assertions.assertThat;

class PlayerResponseTest {

    @Test
    void testEqualsAndHashCode_SameValues() {
        PlayerResponse r1 = new PlayerResponse();
        r1.setId("1");
        r1.setUsername("ash");
        r1.setTrainerName("Ash Ketchum");
        r1.setEmail("ash@pokemon.com");
        r1.setDateOfBirth(LocalDate.of(2000, 1, 1));
        r1.setLevel(5);
        r1.setExperience(100);
        r1.setXpForNextLevel(200);
        r1.setXpIntoCurrentLevel(100);
        r1.setCollectionCardIds(List.of("c1", "c2"));

        PlayerResponse r2 = new PlayerResponse();
        r2.setId("1");
        r2.setUsername("ash");
        r2.setTrainerName("Ash Ketchum");
        r2.setEmail("ash@pokemon.com");
        r2.setDateOfBirth(LocalDate.of(2000, 1, 1));
        r2.setLevel(5);
        r2.setExperience(100);
        r2.setXpForNextLevel(200);
        r2.setXpIntoCurrentLevel(100);
        r2.setCollectionCardIds(List.of("c1", "c2"));

        assertThat(r1).isEqualTo(r2);
        assertThat(r1.hashCode()).isEqualTo(r2.hashCode());
    }

    @Test
    void testEquals_DifferentValues() {
        PlayerResponse r1 = new PlayerResponse();
        r1.setId("1");
        r1.setUsername("ash");

        PlayerResponse r2 = new PlayerResponse();
        r2.setId("2");
        r2.setUsername("misty");

        assertThat(r1).isNotEqualTo(r2);
    }

    @Test
    void testEquals_NullObject() {
        PlayerResponse r = new PlayerResponse();
        assertThat(r.equals(null)).isFalse();
    }

    @Test
    void testHashCode_Consistency() {
        PlayerResponse r = new PlayerResponse();
        r.setId("1");
        int hash = r.hashCode();
        assertThat(r.hashCode()).isEqualTo(hash);
    }

    @Test
    void testSettersAndGetters() {
        PlayerResponse r = new PlayerResponse();
        r.setId("1");
        r.setUsername("ash");
        r.setTrainerName("Ash");
        r.setEmail("ash@test.com");
        r.setDateOfBirth(LocalDate.of(2000, 5, 10));
        r.setLevel(10);
        r.setExperience(500);
        r.setXpForNextLevel(1000);
        r.setXpIntoCurrentLevel(500);
        r.setCollectionCardIds(List.of("c1"));

        assertThat(r.getId()).isEqualTo("1");
        assertThat(r.getUsername()).isEqualTo("ash");
        assertThat(r.getTrainerName()).isEqualTo("Ash");
        assertThat(r.getEmail()).isEqualTo("ash@test.com");
        assertThat(r.getDateOfBirth()).isEqualTo(LocalDate.of(2000, 5, 10));
        assertThat(r.getLevel()).isEqualTo(10);
        assertThat(r.getExperience()).isEqualTo(500);
        assertThat(r.getXpForNextLevel()).isEqualTo(1000);
        assertThat(r.getXpIntoCurrentLevel()).isEqualTo(500);
        assertThat(r.getCollectionCardIds()).containsExactly("c1");
    }

    @Test
    void testNoArgsConstructor() {
        PlayerResponse r = new PlayerResponse();
        assertThat(r).isNotNull();
    }

    @Test
    void testDeckSummary_EqualsAndHashCode() {
        PlayerResponse.DeckSummary d1 = new PlayerResponse.DeckSummary();
        d1.setId("d1");
        d1.setName("My Deck");
        d1.setCardCount(60);

        PlayerResponse.DeckSummary d2 = new PlayerResponse.DeckSummary();
        d2.setId("d1");
        d2.setName("My Deck");
        d2.setCardCount(60);

        assertThat(d1).isEqualTo(d2);
        assertThat(d1.hashCode()).isEqualTo(d2.hashCode());
    }

    @Test
    void testDeckSummary_NotEquals() {
        PlayerResponse.DeckSummary d1 = new PlayerResponse.DeckSummary();
        d1.setId("d1");

        PlayerResponse.DeckSummary d2 = new PlayerResponse.DeckSummary();
        d2.setId("d2");

        assertThat(d1).isNotEqualTo(d2);
    }

    @Test
    void testDeckSummary_SettersAndGetters() {
        PlayerResponse.DeckSummary d = new PlayerResponse.DeckSummary();
        d.setId("deck1");
        d.setName("Starter");
        d.setCardCount(60);

        assertThat(d.getId()).isEqualTo("deck1");
        assertThat(d.getName()).isEqualTo("Starter");
        assertThat(d.getCardCount()).isEqualTo(60);
    }

    @Test
    void testDeckSummary_NoArgsConstructor() {
        assertThat(new PlayerResponse.DeckSummary()).isNotNull();
    }
}
