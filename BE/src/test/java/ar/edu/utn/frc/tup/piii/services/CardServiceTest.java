package ar.edu.utn.frc.tup.piii.services;

import ar.edu.utn.frc.tup.piii.dtos.front.CardDTO;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class CardServiceTest {

    @Autowired
    private CardService cardService;

    @Test
    void findAll_ReturnsAllCards() {
        List<CardDTO> cards = cardService.findAll();
        assertThat(cards).hasSize(157);
    }

    @Test
    void findAll_ReturnsUnmodifiableList() {
        List<CardDTO> cards = cardService.findAll();
        assertThat(cards).isUnmodifiable();
    }

    @Test
    void findById_ExistingId_ReturnsCard() {
        Optional<CardDTO> result = cardService.findById("xy1-1");
        assertThat(result).isPresent();
        assertThat(result.get().getId()).isEqualTo("xy1-1");
    }

    @Test
    void findById_NonExistingId_ReturnsEmpty() {
        Optional<CardDTO> result = cardService.findById("nonexistent");
        assertThat(result).isEmpty();
    }

    @Test
    void findByFilters_ByName_CaseInsensitive() {
        List<CardDTO> result = cardService.findByFilters("saur", null, null, null, null);
        assertThat(result).isNotEmpty();
        assertThat(result).allMatch(c -> c.getName().toLowerCase().contains("saur"));
    }

    @Test
    void findByFilters_ByName_AccentInsensitive() {
        List<CardDTO> withAccent = cardService.findByFilters(null, "Pokémon", null, null, null);
        List<CardDTO> withoutAccent = cardService.findByFilters(null, "Pokemon", null, null, null);
        assertThat(withAccent).hasSameSizeAs(withoutAccent);
        assertThat(withAccent).containsExactlyInAnyOrderElementsOf(withoutAccent);
    }

    @Test
    void findByFilters_NoMatch_ReturnsEmpty() {
        List<CardDTO> result = cardService.findByFilters("ThisCardDoesNotExistXYZ", null, null, null, null);
        assertThat(result).isEmpty();
    }

    @Test
    void findByFilters_ByType_ReturnsCards() {
        List<CardDTO> result = cardService.findByFilters(null, null, "Grass", null, null);
        assertThat(result).isNotEmpty();
        assertThat(result).allMatch(c -> c.getTypes() != null
                && c.getTypes().stream().anyMatch(t -> t.equalsIgnoreCase("Grass")));
    }

    @Test
    void findByFilters_ByRarity_ReturnsCards() {
        List<CardDTO> result = cardService.findByFilters(null, null, null, "Rare Holo", null);
        assertThat(result).isNotEmpty();
        assertThat(result).allMatch(c -> "Rare Holo".equalsIgnoreCase(c.getRarity()));
    }

    @Test
    void findByFilters_BySubtype_ReturnsCards() {
        List<CardDTO> result = cardService.findByFilters(null, null, null, null, "Basic");
        assertThat(result).isNotEmpty();
        assertThat(result).allMatch(c -> c.getSubtypes() != null
                && c.getSubtypes().stream().anyMatch(s -> s.equalsIgnoreCase("Basic")));
    }

    @Test
    void findByFilters_AllFiltersCombined_ReturnsMatching() {
        List<CardDTO> result = cardService.findByFilters(null, "Pokemon", "Grass", null, "Basic");
        assertThat(result).isNotEmpty();
    }

    @Test
    void findByFilters_RarityNoMatch_ReturnsEmpty() {
        List<CardDTO> result = cardService.findByFilters(null, null, null, "NonExistentRarity", null);
        assertThat(result).isEmpty();
    }

    @Test
    void findByFilters_TypeNoMatch_ReturnsEmpty() {
        List<CardDTO> result = cardService.findByFilters(null, null, "NonExistentType", null, null);
        assertThat(result).isEmpty();
    }

    @Test
    void findByFilters_SubtypeNoMatch_ReturnsEmpty() {
        List<CardDTO> result = cardService.findByFilters(null, null, null, null, "NonExistentSubtype");
        assertThat(result).isEmpty();
    }

    @Test
    void findByFilters_SupertypeFilter_ReturnsOnlyMatching() {
        List<CardDTO> pokemon = cardService.findByFilters(null, "Pokemon", null, null, null);
        List<CardDTO> trainer = cardService.findByFilters(null, "Trainer", null, null, null);
        List<CardDTO> energy = cardService.findByFilters(null, "Energy", null, null, null);
        assertThat(pokemon).isNotEmpty();
        assertThat(trainer).isNotEmpty();
        assertThat(energy).isNotEmpty();
        List<CardDTO> collectible = cardService.findByFilters(null, "Collectible", null, null, null);
        assertThat(pokemon.size() + trainer.size() + energy.size() + collectible.size()).isEqualTo(157);
    }

    @Test
    void findByFilters_NullFilters_ReturnsAllCards() {
        List<CardDTO> result = cardService.findByFilters(null, null, null, null, null);
        assertThat(result).hasSize(157);
    }

}
