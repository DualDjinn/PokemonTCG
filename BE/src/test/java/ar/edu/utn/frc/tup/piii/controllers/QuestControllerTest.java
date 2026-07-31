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

import ar.edu.utn.frc.tup.piii.dtos.player.PlayerRequest;
import jakarta.servlet.http.Cookie;

import java.util.UUID;

import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Tests de integración para {@link QuestController}.
 *
 * <p>Mismo patrón que {@link PackControllerTest}: {@code @SpringBootTest} +
 * MockMvc con jugadores reales registrados en cada test. Cubren el flujo
 * HTTP completo del reclamo de recompensas de NPC: primer reclamo otorga la
 * carta y la persiste en la colección, reclamos posteriores son idempotentes,
 * y el listado de reclamados refleja el estado.
 */
@SpringBootTest
class QuestControllerTest {

    private static final String NPC_WITH_REWARD = "Don Alfajor";
    private static final String REWARD_CARD_ID = "pokesecret-8";

    @Autowired
    private WebApplicationContext webApplicationContext;

    @Autowired
    private ObjectMapper objectMapper;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();
    }

    // ============================ helpers ============================

    /**
     * Registra un jugador nuevo y devuelve su sessionId para auth.
     * Usa un username único por llamada para evitar colisiones entre tests.
     */
    private String registerAndGetSession(String prefix) throws Exception {
        String unique = prefix + "_" + UUID.randomUUID().toString().substring(0, 8);
        PlayerRequest request = new PlayerRequest();
        request.setUsername(unique);
        request.setEmail(unique + "@test.com");
        request.setPassword("pass123");
        request.setTrainerName("T_" + prefix);

        MvcResult result = mockMvc.perform(post("/api/players/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andReturn();

        return extractSessionId(result);
    }

    private String extractSessionId(MvcResult result) {
        String setCookieHeader = result.getResponse().getHeader(HttpHeaders.SET_COOKIE);
        return setCookieHeader.split(";")[0].split("=")[1];
    }

    private Cookie session(String sessionId) {
        return new Cookie("sessionId", sessionId);
    }

    // ==================== GET /api/quests/npc/claimed ====================

    @Test
    void getClaimed_NewPlayer_ReturnsEmptyList() throws Exception {
        String sessionId = registerAndGetSession("quest_empty");

        mockMvc.perform(get("/api/quests/npc/claimed").cookie(session(sessionId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isEmpty());
    }

    @Test
    void getClaimed_NoSession_Returns401() throws Exception {
        mockMvc.perform(get("/api/quests/npc/claimed"))
                .andExpect(status().isUnauthorized());
    }

    // ================= POST /api/quests/npc/{npcId}/claim =================

    @Test
    void claim_FirstTime_GrantsCardWithMinioImage() throws Exception {
        String sessionId = registerAndGetSession("quest_first");

        mockMvc.perform(post("/api/quests/npc/{npcId}/claim", NPC_WITH_REWARD)
                        .cookie(session(sessionId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.npcId", is(NPC_WITH_REWARD)))
                .andExpect(jsonPath("$.alreadyClaimed", is(false)))
                .andExpect(jsonPath("$.card.id", is(REWARD_CARD_ID)))
                .andExpect(jsonPath("$.card.name", is("I've Become So Numb")))
                .andExpect(jsonPath("$.card.imageUrl",
                        is("/media/custom-cards/ive-become-so-numb.png")));
    }

    @Test
    void claim_SecondTime_IsIdempotent() throws Exception {
        String sessionId = registerAndGetSession("quest_repeat");

        mockMvc.perform(post("/api/quests/npc/{npcId}/claim", NPC_WITH_REWARD)
                        .cookie(session(sessionId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.alreadyClaimed", is(false)));

        mockMvc.perform(post("/api/quests/npc/{npcId}/claim", NPC_WITH_REWARD)
                        .cookie(session(sessionId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.alreadyClaimed", is(true)))
                .andExpect(jsonPath("$.card", nullValue()));
    }

    @Test
    void claim_ThenClaimedListContainsNpc() throws Exception {
        String sessionId = registerAndGetSession("quest_list");

        mockMvc.perform(post("/api/quests/npc/{npcId}/claim", NPC_WITH_REWARD)
                        .cookie(session(sessionId)))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/quests/npc/claimed").cookie(session(sessionId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasItem(NPC_WITH_REWARD)));
    }

    @Test
    void claim_AddsCardToPlayerCollection() throws Exception {
        String sessionId = registerAndGetSession("quest_coll");

        mockMvc.perform(post("/api/quests/npc/{npcId}/claim", NPC_WITH_REWARD)
                        .cookie(session(sessionId)))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/players/me/collection")
                        .param("page", "0")
                        .param("size", "200")
                        .cookie(session(sessionId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].id", hasItem(REWARD_CARD_ID)));
    }

    @Test
    void claim_NpcWithoutReward_Returns404() throws Exception {
        String sessionId = registerAndGetSession("quest_404");

        mockMvc.perform(post("/api/quests/npc/{npcId}/claim", "Gandalf Holmes")
                        .cookie(session(sessionId)))
                .andExpect(status().isNotFound());
    }

    @Test
    void claim_NoSession_Returns401() throws Exception {
        mockMvc.perform(post("/api/quests/npc/{npcId}/claim", NPC_WITH_REWARD))
                .andExpect(status().isUnauthorized());
    }
}
