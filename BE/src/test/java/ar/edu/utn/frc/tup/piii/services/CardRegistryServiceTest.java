package ar.edu.utn.frc.tup.piii.services;

import ar.edu.utn.frc.tup.piii.models.Card;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.Collection;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

@SpringBootTest
class CardRegistryServiceTest {

    @Autowired
    private CardRegistryService cardRegistry;

    @Test
    void getAll_ReturnsAllCards() {
        Collection<Card> cards = cardRegistry.getAll();
        assertThat(cards).hasSize(157);
    }

    @Test
    void getAll_ReturnsAllCardsWithNonNullIds() {
        Collection<Card> cards = cardRegistry.getAll();
        assertThat(cards).allMatch(c -> c.getId() != null);
    }

    @Test
    void getAll_ReturnsUnmodifiableCollection() {
        Collection<Card> cards = cardRegistry.getAll();
        assertThrows(UnsupportedOperationException.class, () -> cards.clear());
    }

    @Test
    void getById_ExistingId_ReturnsCard() {
        Card card = cardRegistry.getById("xy1-1");
        assertThat(card).isNotNull();
        assertThat(card.getId()).isEqualTo("xy1-1");
    }

    @Test
    void getById_AnotherExistingId_ReturnsCard() {
        Card card = cardRegistry.getById("xy1-100");
        assertThat(card).isNotNull();
        assertThat(card.getId()).isEqualTo("xy1-100");
    }

    @Test
    void getById_LastExistingId_ReturnsCard() {
        Card card = cardRegistry.getById("xy1-140");
        assertThat(card).isNotNull();
        assertThat(card.getId()).isEqualTo("xy1-140");
    }

    @Test
    void getById_NonExistingId_ThrowsIllegalArgumentException() {
        assertThrows(IllegalArgumentException.class,
            () -> cardRegistry.getById("nonexistent"));
    }

    @Test
    void getById_NonExistingId_ErrorMessageContainsId() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
            () -> cardRegistry.getById("bad-id"));
        assertThat(ex.getMessage()).contains("bad-id");
    }

    @Test
    void getById_EmptyId_ThrowsIllegalArgumentException() {
        assertThrows(IllegalArgumentException.class,
            () -> cardRegistry.getById(""));
    }

    @Test
    void getById_NullId_ThrowsIllegalArgumentException() {
        assertThrows(IllegalArgumentException.class,
            () -> cardRegistry.getById(null));
    }

    @Test
    void findById_ExistingId_ReturnsOptionalWithCard() {
        Optional<Card> result = cardRegistry.findById("xy1-1");
        assertThat(result).isPresent();
        assertThat(result.get().getId()).isEqualTo("xy1-1");
    }

    @Test
    void findById_AnotherExistingId_ReturnsOptionalWithCard() {
        Optional<Card> result = cardRegistry.findById("xy1-50");
        assertThat(result).isPresent();
        assertThat(result.get().getId()).isEqualTo("xy1-50");
    }

    @Test
    void findById_NonExistingId_ReturnsEmpty() {
        Optional<Card> result = cardRegistry.findById("nonexistent");
        assertThat(result).isEmpty();
    }

    @Test
    void findById_EmptyId_ReturnsEmpty() {
        Optional<Card> result = cardRegistry.findById("");
        assertThat(result).isEmpty();
    }

    @Test
    void findById_NullId_ReturnsEmpty() {
        Optional<Card> result = cardRegistry.findById(null);
        assertThat(result).isEmpty();
    }

    @Test
    void referentialIdentity_SameId_ReturnsSameReference() {
        Card first = cardRegistry.getById("xy1-1");
        Card second = cardRegistry.getById("xy1-1");
        assertThat(first).isSameAs(second);
    }

    @Test
    void referentialIdentity_DifferentCardIds_DifferentReferences() {
        Card first = cardRegistry.getById("xy1-1");
        Card second = cardRegistry.getById("xy1-2");
        assertThat(first).isNotSameAs(second);
    }

    @Test
    void referentialIdentity_RepeatedCallsAcrossMethods_ReturnsSameReference() {
        Card fromGetById = cardRegistry.getById("xy1-10");
        Card fromFindById = cardRegistry.findById("xy1-10").orElseThrow();
        assertThat(fromGetById).isSameAs(fromFindById);
    }

    @Test
    void getById_MultipleCallsAllReturnSameInstance() {
        Card[] references = new Card[5];
        for (int i = 0; i < references.length; i++) {
            references[i] = cardRegistry.getById("xy1-25");
        }
        for (int i = 1; i < references.length; i++) {
            assertThat(references[i]).isSameAs(references[0]);
        }
    }

    @Test
    void getAll_ContainsAllIds() {
        Collection<Card> cards = cardRegistry.getAll();
        assertThat(cards)
            .extracting(Card::getId)
            .contains("xy1-1", "xy1-50", "xy1-100", "xy1-140");
    }

    @Test
    void getById_PokemonCard_ReturnsCardWithCorrectSupertype() {
        Card card = cardRegistry.getById("xy1-1");
        assertThat(card.getSupertype().name()).isEqualTo("POKEMON");
    }

    @Test
    void findById_MultipleLookups_ConsistentResults() {
        for (int i = 1; i <= 140; i++) {
            String id = "xy1-" + i;
            Optional<Card> result = cardRegistry.findById(id);
            assertThat(result).as("Card %s should exist", id).isPresent();
        }
        // xy1-141 to xy1-146 are Rare Ultra (full-art skins), excluded from registry
        for (int i = 141; i <= 146; i++) {
            String id = "xy1-" + i;
            Optional<Card> result = cardRegistry.findById(id);
            assertThat(result).as("Card %s should be excluded (Rare Ultra)", id).isEmpty();
        }
    }

    @Test
    void getById_RegistryContainsMultipleSupertypes() {
        boolean hasPokemon = false;
        boolean hasTrainer = false;
        boolean hasEnergy = false;
        for (Card card : cardRegistry.getAll()) {
            switch (card.getSupertype()) {
                case POKEMON -> hasPokemon = true;
                case TRAINER -> hasTrainer = true;
                case ENERGY -> hasEnergy = true;
                case COLLECTIBLE -> { }
            }
        }
        assertThat(hasPokemon).isTrue();
        assertThat(hasTrainer).isTrue();
        assertThat(hasEnergy).isTrue();
    }
}
