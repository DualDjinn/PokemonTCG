package ar.edu.utn.frc.tup.piii.models;

import org.junit.jupiter.api.Test;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import static org.assertj.core.api.Assertions.assertThat;

class PlayerTest {

    private Player createDefault() {
        return new Player("p1", "user1", "Trainer1",
                "e@e.com", "pwd", LocalDate.of(2000, 1, 1),
                0, 0, new ArrayList<>(), new ArrayList<>());
    }

    @Test
    void constructor_Default_InitializesLists() {
        Player player = new Player();
        assertThat(player.getCollection()).isNotNull();
        assertThat(player.getDecks()).isNotNull();
    }

    @Test
    void constructor_Parameterized_SetsFields() {
        List<Card> collection = new ArrayList<>();
        List<Deck> decks = new ArrayList<>();
        Player player = new Player("p1", "testUser", "TestTrainer",
                "test@test.com", "pass123", LocalDate.of(2000, 1, 1),
                0, 0, collection, decks);
        assertThat(player.getId()).isEqualTo("p1");
        assertThat(player.getUsername()).isEqualTo("testUser");
        assertThat(player.getTrainerName()).isEqualTo("TestTrainer");
        assertThat(player.getEmail()).isEqualTo("test@test.com");
        assertThat(player.getPassword()).isEqualTo("pass123");
        assertThat(player.getDateOfBirth()).isEqualTo(LocalDate.of(2000, 1, 1));
        assertThat(player.getExperience()).isZero();
        assertThat(player.getCoins()).isZero();
    }

    @Test
    void constructor_Parameterized_WithNullLists_InitializesEmptyLists() {
        Player player = new Player("p2", "user2", "Trainer2",
                "e@e.com", "pwd", null, 0, 0, null, null);
        assertThat(player.getCollection()).isEmpty();
        assertThat(player.getDecks()).isEmpty();
    }

    @Test
    void addExperience_AddsCorrectly() {
        Player player = new Player();
        player.addExperience(50);
        assertThat(player.getExperience()).isEqualTo(50);
        player.addExperience(30);
        assertThat(player.getExperience()).isEqualTo(80);
    }

    @Test
    void getLevel_WhenNoExperience_Returns1() {
        Player player = new Player();
        assertThat(player.getLevel()).isEqualTo(1);
    }

    @Test
    void getLevel_WhenSomeExperience_ReturnsCorrectLevel() {
        Player player = new Player();
        player.addExperience(0);
        assertThat(player.getLevel()).isEqualTo(1);
        player.addExperience(100);
        assertThat(player.getLevel()).isEqualTo(2);
        player.addExperience(200);
        assertThat(player.getLevel()).isEqualTo(3);
    }

    @Test
    void getLevel_AtExactBoundary() {
        Player player = new Player();
        player.addExperience(99);
        assertThat(player.getLevel()).isEqualTo(1);
        player.addExperience(1);
        assertThat(player.getLevel()).isEqualTo(2);
    }

    @Test
    void getXpForNextLevel_ReturnsCorrectXp() {
        Player player = new Player();
        assertThat(player.getXpForNextLevel()).isEqualTo(100);
        player.addExperience(100);
        assertThat(player.getXpForNextLevel()).isEqualTo(200);
        player.addExperience(200);
        assertThat(player.getXpForNextLevel()).isEqualTo(400);
    }

    @Test
    void getXpIntoCurrentLevel_WhenNoExperience_ReturnsZero() {
        Player player = new Player();
        assertThat(player.getXpIntoCurrentLevel()).isZero();
    }

    @Test
    void getXpIntoCurrentLevel_WithPartialExperience() {
        Player player = new Player();
        player.addExperience(50);
        assertThat(player.getXpIntoCurrentLevel()).isEqualTo(50);
    }

    @Test
    void getXpIntoCurrentLevel_WithExactLevelUp() {
        Player player = new Player();
        player.addExperience(100);
        assertThat(player.getXpIntoCurrentLevel()).isZero();
    }

    @Test
    void getXpIntoCurrentLevel_WithExperienceSpanningLevels() {
        Player player = new Player();
        player.addExperience(150);
        assertThat(player.getXpIntoCurrentLevel()).isEqualTo(50);
    }

    @Test
    void addToCollection_AddsCard() {
        Player player = new Player();
        PokemonCard card = PokemonCard.builder().id("c1").name("Pikachu").build();
        player.addToCollection(card);
        assertThat(player.getCollection()).hasSize(1);
        assertThat(player.getCollection().get(0).getId()).isEqualTo("c1");
    }

    @Test
    void addDeck_AddsDeck() {
        Player player = new Player();
        Deck deck = new Deck("d1", "MyDeck", new ArrayList<>());
        player.addDeck(deck);
        assertThat(player.getDecks()).hasSize(1);
        assertThat(player.getDecks().get(0).getName()).isEqualTo("MyDeck");
    }

    @Test
    void setters_And_Getters_Work() {
        Player player = new Player();
        player.setId("p99");
        player.setUsername("newUser");
        player.setTrainerName("NewTrainer");
        player.setEmail("new@e.com");
        player.setPassword("newPwd");
        player.setDateOfBirth(LocalDate.of(1999, 12, 31));
        player.setExperience(999);
        List<Card> coll = new ArrayList<>();
        player.setCollection(coll);
        List<Deck> dlist = new ArrayList<>();
        player.setDecks(dlist);

        assertThat(player.getId()).isEqualTo("p99");
        assertThat(player.getUsername()).isEqualTo("newUser");
        assertThat(player.getTrainerName()).isEqualTo("NewTrainer");
        assertThat(player.getEmail()).isEqualTo("new@e.com");
        assertThat(player.getPassword()).isEqualTo("newPwd");
        assertThat(player.getDateOfBirth()).isEqualTo(LocalDate.of(1999, 12, 31));
        assertThat(player.getExperience()).isEqualTo(999);
        assertThat(player.getCollection()).isSameAs(coll);
        assertThat(player.getDecks()).isSameAs(dlist);
    }

    @Test
    void equals_And_HashCode_SameValues_AreEqual() {
        Player a = createDefault();
        Player b = createDefault();
        assertThat(a).isEqualTo(b);
        assertThat(a.hashCode()).isEqualTo(b.hashCode());
    }

    @Test
    void equals_DifferentValues_AreNotEqual() {
        Player a = createDefault();
        Player b = createDefault();
        b.setId("p2");
        assertThat(a).isNotEqualTo(b);
    }

    @Test
    void toString_ContainsFields() {
        Player player = createDefault();
        String str = player.toString();
        assertThat(str).contains("p1", "user1", "Trainer1");
    }

    @Test
    void equals_WithNull_ReturnsFalse() {
        Player player = createDefault();
        assertThat(player).isNotEqualTo(null);
    }

    @Test
    void equals_WithDifferentType_ReturnsFalse() {
        Player player = createDefault();
        assertThat(player).isNotEqualTo("some string");
    }

    @Test
    void equals_SameInstance_ReturnsTrue() {
        Player player = createDefault();
        assertThat(player).isEqualTo(player);
    }

    @Test
    void equals_DifferentUsername_AreNotEqual() {
        Player a = createDefault();
        Player b = createDefault();
        b.setUsername("differentUser");
        assertThat(a).isNotEqualTo(b);
    }

    @Test
    void equals_DifferentEmail_AreNotEqual() {
        Player a = createDefault();
        Player b = createDefault();
        b.setEmail("other@e.com");
        assertThat(a).isNotEqualTo(b);
    }

    @Test
    void equals_DifferentTrainerName_AreNotEqual() {
        Player a = createDefault();
        Player b = createDefault();
        b.setTrainerName("OtherTrainer");
        assertThat(a).isNotEqualTo(b);
    }

    @Test
    void equals_DifferentExperience_AreNotEqual() {
        Player a = createDefault();
        Player b = createDefault();
        b.setExperience(100);
        assertThat(a).isNotEqualTo(b);
    }

    @Test
    void hashCode_IsConsistent() {
        Player player = createDefault();
        int hash = player.hashCode();
        assertThat(player.hashCode()).isEqualTo(hash);
    }

    @Test
    void hashCode_DifferentValues_DifferentHash() {
        Player a = createDefault();
        Player b = createDefault();
        b.setId("different");
        assertThat(a.hashCode()).isNotEqualTo(b.hashCode());
    }

    @Test
    void setCollection_WithNull_DoesNotBreakOtherMethods() {
        Player player = createDefault();
        player.setCollection(null);
        assertThat(player.getCollection()).isNull();
    }

    @Test
    void setDateOfBirth_WithNull() {
        Player player = createDefault();
        player.setDateOfBirth(null);
        assertThat(player.getDateOfBirth()).isNull();
    }

    @Test
    void getXpIntoCurrentLevel_WithZeroExperience() {
        Player player = new Player();
        assertThat(player.getXpIntoCurrentLevel()).isZero();
    }

    @Test
    void addExperience_WithNegative() {
        Player player = new Player();
        player.addExperience(-10);
        assertThat(player.getExperience()).isEqualTo(-10);
        assertThat(player.getLevel()).isEqualTo(1);
    }
}
