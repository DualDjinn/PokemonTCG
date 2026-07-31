package ar.edu.utn.frc.tup.piii.models;

import org.junit.jupiter.api.Test;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import static org.assertj.core.api.Assertions.assertThat;

class DeckTest {

    private PokemonCard createBasicPokemon(String id, String name) {
        return PokemonCard.builder()
                .id(id).name(name).hp(60)
                .stage(PokemonStage.BASIC)
                .build();
    }

    private EnergyCard createEnergy() {
        return EnergyCard.builder()
                .id("energy-1").name("Grass Energy")
                .energyType(EnergyType.GRASS)
                .build();
    }

    @Test
    void constructor_Default_InitializesCards() {
        Deck deck = new Deck();
        assertThat(deck.getCards()).isNotNull();
        assertThat(deck.getCards()).isEmpty();
    }

    @Test
    void constructor_Parameterized_SetsFields() {
        List<Card> cards = new ArrayList<>();
        Deck deck = new Deck("d1", "TestDeck", cards);
        assertThat(deck.getId()).isEqualTo("d1");
        assertThat(deck.getName()).isEqualTo("TestDeck");
        assertThat(deck.getCards()).isSameAs(cards);
    }

    @Test
    void constructor_Parameterized_WithNullCards_InitializesEmpty() {
        Deck deck = new Deck("d2", "EmptyDeck", null);
        assertThat(deck.getCards()).isEmpty();
    }

    @Test
    void validate_WhenNot60Cards_ReturnsError() {
        Deck deck = new Deck("d1", "SmallDeck", new ArrayList<>());
        List<String> errors = deck.validate(null, null);
        assertThat(errors).anyMatch(e -> e.contains("exactly 60 cards"));
    }

    @Test
    void validate_WhenNullCards_ReturnsError() {
        Deck deck = new Deck("d2", "NullDeck", null);
        List<String> errors = deck.validate(null, null);
        assertThat(errors).anyMatch(e -> e.contains("exactly 60 cards"));
    }

    @Test
    void validate_WhenNoBasicPokemon_ReturnsError() {
        List<Card> cards = new ArrayList<>();
        for (int i = 0; i < 60; i++) {
            cards.add(createEnergy());
        }
        Deck deck = new Deck("d3", "NoBasic", cards);
        List<String> errors = deck.validate(null, null);
        assertThat(errors).anyMatch(e -> e.contains("Basic Pokémon"));
    }

    @Test
    void validate_WhenMoreThan4NonEnergy_ReturnsError() {
        List<Card> cards = new ArrayList<>();
        PokemonCard basic = createBasicPokemon("p1", "Pikachu");
        cards.add(basic);
        cards.add(basic);
        cards.add(basic);
        cards.add(basic);
        cards.add(basic);
        for (int i = 0; i < 55; i++) {
            cards.add(createEnergy());
        }
        Deck deck = new Deck("d4", "TooMany", List.copyOf(cards));
        List<String> errors = deck.validate(null, null);
        assertThat(errors).anyMatch(e -> e.contains("Max 4 copies"));
    }

    @Test
    void validate_AllowsUpTo4CopiesOfNonEnergy() {
        List<Card> cards = new ArrayList<>();
        PokemonCard basic = createBasicPokemon("p1", "Pikachu");
        // PokemonCard extra = createBasicPokemon("p2", "Charmander");
        cards.add(basic);
        cards.add(basic);
        cards.add(basic);
        cards.add(basic);
        for (int i = 0; i < 56; i++) {
            cards.add(createEnergy());
        }
        Deck deck = new Deck("d4b", "Max4", cards);
        List<String> errors = deck.validate(null, null);
        assertThat(errors).noneMatch(e -> e.contains("Max 4 copies"));
    }

    @Test
    void validate_WhenBannedCards_ReturnsError() {
        List<Card> cards = new ArrayList<>();
        PokemonCard banned = createBasicPokemon("banned-1", "BannedMon");
        cards.add(banned);
        for (int i = 0; i < 59; i++) {
            cards.add(createEnergy());
        }
        Deck deck = new Deck("d5", "Banned", cards);
        List<String> errors = deck.validate(null, Set.of("banned-1"));
        assertThat(errors).anyMatch(e -> e.contains("banned"));
    }

    @Test
    void validate_WhenBannedSetIsEmpty_ReturnsNoBannedError() {
        List<Card> cards = new ArrayList<>();
        cards.add(createBasicPokemon("p1", "Pikachu"));
        for (int i = 0; i < 59; i++) {
            cards.add(createEnergy());
        }
        Deck deck = new Deck("d5b", "NoBanned", cards);
        List<String> errors = deck.validate(null, Set.of());
        assertThat(errors).noneMatch(e -> e.contains("banned"));
    }

    @Test
    void validate_WhenCardsNotOwned_ReturnsError() {
        List<Card> cards = new ArrayList<>();
        PokemonCard owned = createBasicPokemon("p1", "Pikachu");
        // PokemonCard notOwned = createBasicPokemon("p2", "Charizard");
        cards.add(owned);
        for (int i = 0; i < 59; i++) {
            cards.add(createEnergy());
        }
        Deck deck = new Deck("d6", "NotOwned", List.copyOf(cards));
        Player owner = new Player();
        List<Card> collection = new ArrayList<>();
        collection.add(owned);
        owner.setCollection(collection);
        List<String> errors = deck.validate(owner, null);
        assertThat(errors).anyMatch(e -> e.contains("not owned"));
    }

    @Test
    void validate_WhenOwnerHasNullCollection_SkipsOwnershipCheck() {
        List<Card> cards = new ArrayList<>();
        cards.add(createBasicPokemon("p1", "Pikachu"));
        for (int i = 0; i < 59; i++) {
            cards.add(createEnergy());
        }
        Deck deck = new Deck("d6b", "NoOwnerColl", cards);
        Player owner = new Player();
        owner.setCollection(null);
        List<String> errors = deck.validate(owner, null);
        assertThat(errors).noneMatch(e -> e.contains("not owned"));
    }

    @Test
    void validate_WhenValid_ReturnsNoErrors() {
        List<Card> cards = new ArrayList<>();
        PokemonCard basic = createBasicPokemon("p1", "Pikachu");
        cards.add(basic);
        for (int i = 0; i < 59; i++) {
            cards.add(createEnergy());
        }
        Deck deck = new Deck("d7", "ValidDeck", cards);
        List<String> errors = deck.validate(null, null);
        assertThat(errors).isEmpty();
    }

    @Test
    void isValid_ReturnsTrueWhenValid() {
        List<Card> cards = new ArrayList<>();
        cards.add(createBasicPokemon("p1", "Pikachu"));
        for (int i = 0; i < 59; i++) {
            cards.add(createEnergy());
        }
        Deck deck = new Deck("d8", "Valid", cards);
        assertThat(deck.isValid(null, null)).isTrue();
    }

    @Test
    void isValid_ReturnsFalseWhenInvalid() {
        Deck deck = new Deck("d9", "Invalid", new ArrayList<>());
        assertThat(deck.isValid(null, null)).isFalse();
    }

    @Test
    void setters_And_Getters_Work() {
        Deck deck = new Deck();
        deck.setId("d99");
        deck.setName("TestDeck");
        List<Card> cards = new ArrayList<>();
        deck.setCards(cards);
        assertThat(deck.getId()).isEqualTo("d99");
        assertThat(deck.getName()).isEqualTo("TestDeck");
        assertThat(deck.getCards()).isSameAs(cards);
    }

    @Test
    void equals_And_HashCode_SameValues_AreEqual() {
        List<Card> cards = new ArrayList<>();
        Deck a = new Deck("d1", "Deck", cards);
        Deck b = new Deck("d1", "Deck", cards);
        assertThat(a).isEqualTo(b);
        assertThat(a.hashCode()).isEqualTo(b.hashCode());
    }

    @Test
    void equals_DifferentIds_AreNotEqual() {
        Deck a = new Deck("d1", "Deck", new ArrayList<>());
        Deck b = new Deck("d2", "Deck", new ArrayList<>());
        assertThat(a).isNotEqualTo(b);
    }

    @Test
    void validate_WithMoreThan60Cards_ReturnsError() {
        List<Card> cards = new ArrayList<>();
        for (int i = 0; i < 61; i++) {
            cards.add(createEnergy());
        }
        Deck deck = new Deck("d10", "TooBig", cards);
        List<String> errors = deck.validate(null, null);
        assertThat(errors).anyMatch(e -> e.contains("exactly 60 cards"));
    }

    @Test
    void validate_WhenBannedSetNonEmptyButNoMatch_SkipsBannedError() {
        List<Card> cards = new ArrayList<>();
        cards.add(createBasicPokemon("p1", "Pikachu"));
        for (int i = 0; i < 59; i++) {
            cards.add(createEnergy());
        }
        Deck deck = new Deck("d11", "NoBannedMatch", cards);
        List<String> errors = deck.validate(null, Set.of("some-other-ban"));
        assertThat(errors).noneMatch(e -> e.contains("banned"));
    }

    @Test
    void validate_WhenAllCardsOwned_SkipsOwnershipError() {
        List<Card> cards = new ArrayList<>();
        PokemonCard basic = createBasicPokemon("p1", "Pikachu");
        cards.add(basic);
        for (int i = 0; i < 59; i++) {
            cards.add(createEnergy());
        }
        Deck deck = new Deck("d12", "AllOwned", List.copyOf(cards));
        Player owner = new Player();
        owner.setCollection(new ArrayList<>(cards));
        List<String> errors = deck.validate(owner, null);
        assertThat(errors).noneMatch(e -> e.contains("not owned"));
    }

    @Test
    void validate_WithEnergyCardsMoreThan4Copies_NoMaxCopyError() {
        List<Card> cards = new ArrayList<>();
        cards.add(createBasicPokemon("p1", "Pikachu"));
        EnergyCard sameEnergy = EnergyCard.builder()
                .id("energy-same").name("Same Energy")
                .energyType(EnergyType.GRASS)
                .build();
        for (int i = 0; i < 59; i++) {
            cards.add(sameEnergy);
        }
        Deck deck = new Deck("d13", "ManyEnergy", cards);
        List<String> errors = deck.validate(null, null);
        assertThat(errors).noneMatch(e -> e.contains("Max 4 copies"));
    }

    @Test
    void toString_ContainsFields() {
        Deck deck = new Deck("d1", "MyDeck", new ArrayList<>());
        assertThat(deck.toString()).contains("d1", "MyDeck");
    }

    @Test
    void validate_WhenCardsExplicitlyNull_ReturnsErrorWithZeroCount() {
        // El constructor nunca deja cards == null, pero el setter de @Data sí.
        // Cubre la rama cards == null en el if y en el ternario del mensaje.
        Deck deck = new Deck();
        deck.setCards(null);
        List<String> errors = deck.validate(null, null);
        assertThat(errors).anyMatch(e -> e.contains("exactly 60 cards"));
        assertThat(errors).anyMatch(e -> e.contains("has 0"));
    }

    @Test
    void validate_WithNonBasicPokemonPresent_StillFindsBasic() {
        // Cubre la rama 'instanceof PokemonCard && isBasic() == false':
        // un Pokémon de Stage 1 evaluado antes de encontrar el básico.
        List<Card> cards = new ArrayList<>();
        cards.add(PokemonCard.builder()
                .id("stage1").name("Ivysaur").hp(90)
                .stage(PokemonStage.STAGE_1)
                .build());
        cards.add(createBasicPokemon("p1", "Bulbasaur"));
        for (int i = 0; i < 58; i++) {
            cards.add(createEnergy());
        }
        Deck deck = new Deck("d14", "WithStage1", cards);
        List<String> errors = deck.validate(null, null);
        assertThat(errors).noneMatch(e -> e.contains("Basic Pokémon"));
    }

    @Test
    void validate_WithOnlyNonBasicPokemon_ReturnsBasicError() {
        // Stage 1 presente pero ningún básico: la rama isBasic()==false es la única
        // para los Pokémon y debe reportar falta de básico.
        List<Card> cards = new ArrayList<>();
        cards.add(PokemonCard.builder()
                .id("stage1").name("Ivysaur").hp(90)
                .stage(PokemonStage.STAGE_1)
                .build());
        for (int i = 0; i < 59; i++) {
            cards.add(createEnergy());
        }
        Deck deck = new Deck("d15", "NoBasicStage1", cards);
        List<String> errors = deck.validate(null, null);
        assertThat(errors).anyMatch(e -> e.contains("Basic Pokémon"));
    }
}
