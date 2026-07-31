package ar.edu.utn.frc.tup.piii.controllers;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
class CardControllerTest {

    @Autowired
    private WebApplicationContext webApplicationContext;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();
    }

    // --- GET /cards (sin filtros) ---

    @Test
    void getCards_WithoutFilters_ReturnsAllCards() throws Exception {
        mockMvc.perform(get("/cards"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(greaterThan(0))));
    }

    // --- GET /cards con filtro name ---

    @Test
    void getCards_WithNameFilter_ReturnsFiltered() throws Exception {
        mockMvc.perform(get("/cards").param("name", "Venusaur"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(greaterThan(0))))
                .andExpect(jsonPath("$[*].name",
                        everyItem(containsStringIgnoringCase("Venusaur"))));
    }

    @Test
    void getCards_WithNameFilter_NoMatch_ReturnsEmpty() throws Exception {
        mockMvc.perform(get("/cards").param("name", "ThisCardDoesNotExistXYZ"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    void getCards_WithNameFilter_PartialMatch_ReturnsFiltered() throws Exception {
        mockMvc.perform(get("/cards").param("name", "saur"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(greaterThan(0))))
                .andExpect(jsonPath("$[*].name",
                        everyItem(containsStringIgnoringCase("saur"))));
    }

    // --- GET /cards con filtro supertype ---

    @Test
    void getCards_WithSupertypeFilterPokemon_ReturnsFiltered() throws Exception {
        mockMvc.perform(get("/cards").param("supertype", "Pokémon"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(greaterThan(0))));
    }

    @Test
    void getCards_WithSupertypeFilterTrainer_ReturnsFiltered() throws Exception {
        mockMvc.perform(get("/cards").param("supertype", "Trainer"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(greaterThan(0))));
    }

    @Test
    void getCards_WithSupertypeFilterEnergy_ReturnsFiltered() throws Exception {
        mockMvc.perform(get("/cards").param("supertype", "Energy"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(greaterThan(0))));
    }

    @Test
    void getCards_WithSupertypeFilter_NoMatch_ReturnsEmpty() throws Exception {
        mockMvc.perform(get("/cards").param("supertype", "NonExistentSupertype"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
    }

    // --- GET /cards con filtro type ---

    @Test
    void getCards_WithTypeFilter_ReturnsFiltered() throws Exception {
        mockMvc.perform(get("/cards").param("type", "Fire"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(greaterThan(0))));
    }

    @Test
    void getCards_WithTypeFilter_NoMatch_ReturnsEmpty() throws Exception {
        mockMvc.perform(get("/cards").param("type", "NonExistentType"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
    }

    // --- GET /cards con filtro rarity ---

    @Test
    void getCards_WithRarityFilter_ReturnsFiltered() throws Exception {
        mockMvc.perform(get("/cards").param("rarity", "Common"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(greaterThan(0))));
    }

    @Test
    void getCards_WithRarityFilter_NoMatch_ReturnsEmpty() throws Exception {
        mockMvc.perform(get("/cards").param("rarity", "NonExistentRarity"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
    }

    // --- GET /cards con filtro subtype ---

    @Test
    void getCards_WithSubtypeFilter_ReturnsFiltered() throws Exception {
        mockMvc.perform(get("/cards").param("subtype", "Basic"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(greaterThan(0))));
    }

    @Test
    void getCards_WithSubtypeFilter_NoMatch_ReturnsEmpty() throws Exception {
        mockMvc.perform(get("/cards").param("subtype", "NonExistentSubtype"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
    }

    // --- GET /cards con filtros combinados ---

    @Test
    void getCards_WithTypeAndRarityFilter_ReturnsFiltered() throws Exception {
        mockMvc.perform(get("/cards")
                        .param("type", "Grass")
                        .param("rarity", "Rare Holo EX"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(greaterThan(0))));
    }

    @Test
    void getCards_WithMultipleFilters_ReturnsFiltered() throws Exception {
        mockMvc.perform(get("/cards")
                        .param("supertype", "Pokémon")
                        .param("type", "Grass")
                        .param("subtype", "Basic"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(greaterThanOrEqualTo(0))));
    }

    @Test
    void getCards_WithAllFilters_NoMatch_ReturnsEmpty() throws Exception {
        mockMvc.perform(get("/cards")
                        .param("name", "Pikachu")
                        .param("supertype", "Trainer")
                        .param("type", "Fire")
                        .param("rarity", "Common")
                        .param("subtype", "EX"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
    }

    // --- GET /cards/{id} ---

    @Test
    void getCardById_ExistingId_ReturnsCard() throws Exception {
        mockMvc.perform(get("/cards/xy1-1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is("xy1-1")))
                .andExpect(jsonPath("$.name", not(emptyOrNullString())));
    }

    @Test
    void getCardById_NonExistingId_Returns404() throws Exception {
        mockMvc.perform(get("/cards/nonexistent"))
                .andExpect(status().isNotFound());
    }

    @Test
    void getCardById_AnotherExistingId_ReturnsCorrectCard() throws Exception {
        mockMvc.perform(get("/cards/xy1-2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is("xy1-2")));
    }
}
