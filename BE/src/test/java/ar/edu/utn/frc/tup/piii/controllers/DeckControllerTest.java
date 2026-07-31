package ar.edu.utn.frc.tup.piii.controllers;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import ar.edu.utn.frc.tup.piii.dtos.player.CreateDeckRequest;
import ar.edu.utn.frc.tup.piii.dtos.player.PlayerRequest;
import ar.edu.utn.frc.tup.piii.dtos.player.UpdateDeckNameRequest;
import ar.edu.utn.frc.tup.piii.dtos.player.UpdateDeckRequest;
import jakarta.servlet.http.Cookie;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
class DeckControllerTest {

    @Autowired
    private WebApplicationContext webApplicationContext;

    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();
    }

    // ==================== listDecks ====================

    @Test
    void listDecks_WithValidCookie_ReturnsDecks() throws Exception {
        MvcResult regResult = registerUser("listDecksUser", "listdecks@test.com");
        String sessionId = extractSessionId(regResult);

        mockMvc.perform(get("/api/decks")
                        .cookie(new Cookie("sessionId", sessionId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(greaterThanOrEqualTo(1))));
    }

    @Test
    void listDecks_WithoutCookie_Returns4xx() throws Exception {
        mockMvc.perform(get("/api/decks"))
                .andExpect(status().is4xxClientError());
    }

    @Test
    void listDecks_WithInvalidSession_Returns401() throws Exception {
        mockMvc.perform(get("/api/decks")
                        .cookie(new Cookie("sessionId", "invalid-session-id")))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message", containsString("Not authenticated")));
    }

    // ==================== getDeck ====================

    @Test
    void getDeck_WithValidCookie_ReturnsDeck() throws Exception {
        MvcResult regResult = registerUser("getDeckUser", "getdeck@test.com");
        String sessionId = extractSessionId(regResult);

        MvcResult decksResult = mockMvc.perform(get("/api/players/me/decks")
                        .cookie(new Cookie("sessionId", sessionId)))
                .andExpect(status().isOk())
                .andReturn();

        String decksJson = decksResult.getResponse().getContentAsString();
        String deckId = objectMapper.readTree(decksJson).get(0).get("id").asText();

        mockMvc.perform(get("/api/decks/{deckId}", deckId)
                        .cookie(new Cookie("sessionId", sessionId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(deckId)))
                .andExpect(jsonPath("$.name", not(emptyOrNullString())));
    }

    @Test
    void getDeck_WithoutCookie_Returns4xx() throws Exception {
        mockMvc.perform(get("/api/decks/some-deck-id"))
                .andExpect(status().is4xxClientError());
    }

    @Test
    void getDeck_WithInvalidSession_Returns401() throws Exception {
        mockMvc.perform(get("/api/decks/some-deck-id")
                        .cookie(new Cookie("sessionId", "invalid-session-id")))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message", containsString("Not authenticated")));
    }

    @Test
    void getDeck_NonExistingDeck_Returns400() throws Exception {
        MvcResult regResult = registerUser("getDeck404User", "getdeck404@test.com");
        String sessionId = extractSessionId(regResult);

        mockMvc.perform(get("/api/decks/nonexistent-deck-id")
                        .cookie(new Cookie("sessionId", sessionId)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("not found")));
    }

    // ==================== updateDeck ====================

    @Test
    void updateDeck_WithValidCookie_ReturnsUpdatedDeck() throws Exception {
        MvcResult regResult = registerUser("updateDeckUser", "updatedeck@test.com");
        String sessionId = extractSessionId(regResult);

        MvcResult decksResult = mockMvc.perform(get("/api/players/me/decks")
                        .cookie(new Cookie("sessionId", sessionId)))
                .andExpect(status().isOk())
                .andReturn();

        String decksJson = decksResult.getResponse().getContentAsString();
        String deckId = objectMapper.readTree(decksJson).get(0).get("id").asText();

        UpdateDeckRequest updateRequest = new UpdateDeckRequest();
        updateRequest.setCardIds(java.util.List.of("xy1-1", "xy1-2", "xy1-3"));

        mockMvc.perform(put("/api/decks/{deckId}", deckId)
                        .cookie(new Cookie("sessionId", sessionId))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(deckId)))
                .andExpect(jsonPath("$.cards", hasSize(3)));
    }

    @Test
    void updateDeck_WithEmptyCardList_ReturnsUpdatedDeck() throws Exception {
        MvcResult regResult = registerUser("updateEmptyUser", "updateempty@test.com");
        String sessionId = extractSessionId(regResult);

        MvcResult decksResult = mockMvc.perform(get("/api/players/me/decks")
                        .cookie(new Cookie("sessionId", sessionId)))
                .andExpect(status().isOk())
                .andReturn();

        String decksJson = decksResult.getResponse().getContentAsString();
        String deckId = objectMapper.readTree(decksJson).get(0).get("id").asText();

        UpdateDeckRequest updateRequest = new UpdateDeckRequest();
        updateRequest.setCardIds(java.util.List.of());

        mockMvc.perform(put("/api/decks/{deckId}", deckId)
                        .cookie(new Cookie("sessionId", sessionId))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(deckId)))
                .andExpect(jsonPath("$.cards", hasSize(0)));
    }

    @Test
    void updateDeck_WithoutCookie_Returns4xx() throws Exception {
        UpdateDeckRequest updateRequest = new UpdateDeckRequest();
        updateRequest.setCardIds(java.util.List.of("xy1-1"));

        mockMvc.perform(put("/api/decks/some-deck-id")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().is4xxClientError());
    }

    @Test
    void updateDeck_WithInvalidSession_Returns401() throws Exception {
        UpdateDeckRequest updateRequest = new UpdateDeckRequest();
        updateRequest.setCardIds(java.util.List.of("xy1-1"));

        mockMvc.perform(put("/api/decks/some-deck-id")
                        .cookie(new Cookie("sessionId", "invalid-session-id"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message", containsString("Not authenticated")));
    }

    @Test
    void updateDeck_NonExistingDeck_ReturnsError() throws Exception {
        MvcResult regResult = registerUser("updateNonExUser", "updatenex@test.com");
        String sessionId = extractSessionId(regResult);

        UpdateDeckRequest updateRequest = new UpdateDeckRequest();
        updateRequest.setCardIds(java.util.List.of("xy1-1"));

        mockMvc.perform(put("/api/decks/nonexistent-deck-id", "nonexistent")
                        .cookie(new Cookie("sessionId", sessionId))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().is4xxClientError());
    }

    // ==================== createDeck ====================

    @Test
    void createDeck_WithValidCookie_ReturnsCreated() throws Exception {
        MvcResult regResult = registerUser("createDeckUser", "createdeck@test.com");
        String sessionId = extractSessionId(regResult);

        CreateDeckRequest request = new CreateDeckRequest();
        request.setName("My New Deck");

        mockMvc.perform(post("/api/decks")
                        .cookie(new Cookie("sessionId", sessionId))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id", not(emptyOrNullString())))
                .andExpect(jsonPath("$.name", is("My New Deck")))
                .andExpect(jsonPath("$.cardCount", is(0)));
    }

    @Test
    void createDeck_WithoutCookie_Returns4xx() throws Exception {
        CreateDeckRequest request = new CreateDeckRequest();
        request.setName("Should Fail");

        mockMvc.perform(post("/api/decks")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().is4xxClientError());
    }

    @Test
    void createDeck_WithInvalidSession_Returns401() throws Exception {
        CreateDeckRequest request = new CreateDeckRequest();
        request.setName("Should Fail");

        mockMvc.perform(post("/api/decks")
                        .cookie(new Cookie("sessionId", "invalid-session-id"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message", containsString("Not authenticated")));
    }

    @Test
    void createDeck_VerifyAppearsInList() throws Exception {
        MvcResult regResult = registerUser("createListUser", "createlist@test.com");
        String sessionId = extractSessionId(regResult);

        CreateDeckRequest request = new CreateDeckRequest();
        request.setName("Deck Visible In List");

        mockMvc.perform(post("/api/decks")
                        .cookie(new Cookie("sessionId", sessionId))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/api/decks")
                        .cookie(new Cookie("sessionId", sessionId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.name == 'Deck Visible In List')]", hasSize(1)));
    }

    // ==================== deleteDeck ====================

    @Test
    void deleteDeck_WithValidCookie_Returns204() throws Exception {
        MvcResult regResult = registerUser("deleteDeckUser", "deletedeck@test.com");
        String sessionId = extractSessionId(regResult);

        MvcResult decksResult = mockMvc.perform(get("/api/players/me/decks")
                        .cookie(new Cookie("sessionId", sessionId)))
                .andExpect(status().isOk())
                .andReturn();

        String decksJson = decksResult.getResponse().getContentAsString();
        String deckId = objectMapper.readTree(decksJson).get(0).get("id").asText();

        mockMvc.perform(delete("/api/decks/{deckId}", deckId)
                        .cookie(new Cookie("sessionId", sessionId)))
                .andExpect(status().isNoContent());
    }

    @Test
    void deleteDeck_WithoutCookie_Returns4xx() throws Exception {
        mockMvc.perform(delete("/api/decks/some-deck-id"))
                .andExpect(status().is4xxClientError());
    }

    @Test
    void deleteDeck_WithInvalidSession_Returns401() throws Exception {
        mockMvc.perform(delete("/api/decks/some-deck-id")
                        .cookie(new Cookie("sessionId", "invalid-session-id")))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message", containsString("Not authenticated")));
    }

    @Test
    void deleteDeck_NonExistingDeck_Returns400() throws Exception {
        MvcResult regResult = registerUser("deleteDeck404User", "deletedeck404@test.com");
        String sessionId = extractSessionId(regResult);

        mockMvc.perform(delete("/api/decks/nonexistent-deck-id")
                        .cookie(new Cookie("sessionId", sessionId)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("not found")));
    }

    @Test
    void deleteDeck_ThenGetReturnsError() throws Exception {
        MvcResult regResult = registerUser("deleteVerifyUser", "deleteverify@test.com");
        String sessionId = extractSessionId(regResult);

        CreateDeckRequest createReq = new CreateDeckRequest();
        createReq.setName("To Be Deleted");

        MvcResult createResult = mockMvc.perform(post("/api/decks")
                        .cookie(new Cookie("sessionId", sessionId))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createReq)))
                .andExpect(status().isCreated())
                .andReturn();

        String deckId = objectMapper.readTree(
                createResult.getResponse().getContentAsString()).get("id").asText();

        mockMvc.perform(delete("/api/decks/{deckId}", deckId)
                        .cookie(new Cookie("sessionId", sessionId)))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/decks/{deckId}", deckId)
                        .cookie(new Cookie("sessionId", sessionId)))
                .andExpect(status().isBadRequest());
    }

    // ==================== renameDeck ====================

    @Test
    void renameDeck_WithValidCookie_ReturnsOk() throws Exception {
        MvcResult regResult = registerUser("renameDeckUser", "renamedeck@test.com");
        String sessionId = extractSessionId(regResult);

        MvcResult decksResult = mockMvc.perform(get("/api/players/me/decks")
                        .cookie(new Cookie("sessionId", sessionId)))
                .andExpect(status().isOk())
                .andReturn();

        String decksJson = decksResult.getResponse().getContentAsString();
        String deckId = objectMapper.readTree(decksJson).get(0).get("id").asText();

        UpdateDeckNameRequest request = new UpdateDeckNameRequest();
        request.setName("Renamed Deck");

        mockMvc.perform(patch("/api/decks/{deckId}/name", deckId)
                        .cookie(new Cookie("sessionId", sessionId))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name", is("Renamed Deck")));
    }

    @Test
    void renameDeck_WithoutCookie_Returns4xx() throws Exception {
        UpdateDeckNameRequest request = new UpdateDeckNameRequest();
        request.setName("Should Not Work");

        mockMvc.perform(patch("/api/decks/some-deck-id/name")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().is4xxClientError());
    }

    @Test
    void renameDeck_WithInvalidSession_Returns401() throws Exception {
        UpdateDeckNameRequest request = new UpdateDeckNameRequest();
        request.setName("Should Not Work");

        mockMvc.perform(patch("/api/decks/some-deck-id/name")
                        .cookie(new Cookie("sessionId", "invalid-session-id"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message", containsString("Not authenticated")));
    }

    @Test
    void renameDeck_NonExistingDeck_ReturnsError() throws Exception {
        MvcResult regResult = registerUser("renameNexUser", "renamenex@test.com");
        String sessionId = extractSessionId(regResult);

        UpdateDeckNameRequest request = new UpdateDeckNameRequest();
        request.setName("Nope");

        mockMvc.perform(patch("/api/decks/nonexistent-deck-id/name")
                        .cookie(new Cookie("sessionId", sessionId))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("not found")));
    }

    // ==================== toggleFavorite ====================

    @Test
    void toggleFavorite_WithValidCookie_ReturnsOk() throws Exception {
        MvcResult regResult = registerUser("favDeckUser", "favdeck@test.com");
        String sessionId = extractSessionId(regResult);

        MvcResult decksResult = mockMvc.perform(get("/api/players/me/decks")
                        .cookie(new Cookie("sessionId", sessionId)))
                .andExpect(status().isOk())
                .andReturn();

        String decksJson = decksResult.getResponse().getContentAsString();
        String deckId = objectMapper.readTree(decksJson).get(0).get("id").asText();

        mockMvc.perform(put("/api/decks/{deckId}/favorite", deckId)
                        .cookie(new Cookie("sessionId", sessionId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.favorite", is(true)));

        mockMvc.perform(put("/api/decks/{deckId}/favorite", deckId)
                        .cookie(new Cookie("sessionId", sessionId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.favorite", is(false)));
    }

    @Test
    void toggleFavorite_WithoutCookie_Returns4xx() throws Exception {
        mockMvc.perform(put("/api/decks/some-deck-id/favorite"))
                .andExpect(status().is4xxClientError());
    }

    @Test
    void toggleFavorite_WithInvalidSession_Returns401() throws Exception {
        mockMvc.perform(put("/api/decks/some-deck-id/favorite")
                        .cookie(new Cookie("sessionId", "invalid-session-id")))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message", containsString("Not authenticated")));
    }

    @Test
    void toggleFavorite_NonExistingDeck_ReturnsError() throws Exception {
        MvcResult regResult = registerUser("favNexUser", "favnex@test.com");
        String sessionId = extractSessionId(regResult);

        mockMvc.perform(put("/api/decks/nonexistent-deck-id/favorite")
                        .cookie(new Cookie("sessionId", sessionId)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("not found")));
    }

    // ==================== validateDeck ====================

    @Test
    void validateDeck_WithValidCookie_ReturnsValidationResult() throws Exception {
        MvcResult regResult = registerUser("validateDeckUser", "validatedeck@test.com");
        String sessionId = extractSessionId(regResult);

        MvcResult decksResult = mockMvc.perform(get("/api/players/me/decks")
                        .cookie(new Cookie("sessionId", sessionId)))
                .andExpect(status().isOk())
                .andReturn();

        String decksJson = decksResult.getResponse().getContentAsString();
        String deckId = objectMapper.readTree(decksJson).get(0).get("id").asText();

        mockMvc.perform(get("/api/decks/{deckId}/validate", deckId)
                        .cookie(new Cookie("sessionId", sessionId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.valid").exists())
                .andExpect(jsonPath("$.errors").exists());
    }

    @Test
    void validateDeck_WithoutCookie_Returns4xx() throws Exception {
        mockMvc.perform(get("/api/decks/some-deck-id/validate"))
                .andExpect(status().is4xxClientError());
    }

    @Test
    void validateDeck_WithInvalidSession_Returns401() throws Exception {
        mockMvc.perform(get("/api/decks/some-deck-id/validate")
                        .cookie(new Cookie("sessionId", "invalid-session-id")))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message", containsString("Not authenticated")));
    }

    @Test
    void validateDeck_NonExistingDeck_ReturnsError() throws Exception {
        MvcResult regResult = registerUser("valNexUser", "valnex@test.com");
        String sessionId = extractSessionId(regResult);

        mockMvc.perform(get("/api/decks/nonexistent-deck-id/validate")
                        .cookie(new Cookie("sessionId", sessionId)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("not found")));
    }

    @Test
    void validateDeck_EmptyDeck_ReturnsInvalid() throws Exception {
        MvcResult regResult = registerUser("valEmptyUser", "valempty@test.com");
        String sessionId = extractSessionId(regResult);

        CreateDeckRequest createReq = new CreateDeckRequest();
        createReq.setName("Empty Validation Deck");

        MvcResult createResult = mockMvc.perform(post("/api/decks")
                        .cookie(new Cookie("sessionId", sessionId))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createReq)))
                .andExpect(status().isCreated())
                .andReturn();

        String deckId = objectMapper.readTree(
                createResult.getResponse().getContentAsString()).get("id").asText();

        mockMvc.perform(get("/api/decks/{deckId}/validate", deckId)
                        .cookie(new Cookie("sessionId", sessionId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.valid", is(false)))
                .andExpect(jsonPath("$.errors", hasSize(greaterThan(0))));
    }

    // ==================== helpers ====================

    private MvcResult registerUser(String username, String email) throws Exception {
        PlayerRequest request = new PlayerRequest();
        request.setUsername(username);
        request.setEmail(email);
        request.setPassword("pass123");
        request.setTrainerName("Trainer" + username);

        return mockMvc.perform(
                        org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                                .post("/api/players/register")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andReturn();
    }

    private String extractSessionId(MvcResult result) {
        String setCookieHeader = result.getResponse().getHeader(HttpHeaders.SET_COOKIE);
        return setCookieHeader.split(";")[0].split("=")[1];
    }
}
