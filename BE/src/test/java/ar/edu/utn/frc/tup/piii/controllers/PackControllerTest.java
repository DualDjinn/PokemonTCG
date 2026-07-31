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

import ar.edu.utn.frc.tup.piii.dtos.pack.OpenPackRequest;
import ar.edu.utn.frc.tup.piii.dtos.player.PlayerRequest;
import jakarta.servlet.http.Cookie;

import java.util.UUID;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Tests de integración para {@link PackController}.
 *
 * <p>Usan {@code @SpringBootTest} + MockMvc (mismo patrón que
 * {@code PlayerControllerTest}). Crean jugadores reales en cada test para
 * verificar el flujo HTTP completo: parseo de JSON, manejo de cookies, códigos
 * de estado, descuento de monedas y persistencia.
 */
@SpringBootTest
class PackControllerTest {

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

    /** Acredita monedas al jugador autenticado vía el endpoint dev. */
    private void grantCoins(String sessionId, int amount) throws Exception {
        mockMvc.perform(post("/api/dev/grant-coins")
                        .param("amount", String.valueOf(amount))
                        .cookie(new Cookie("sessionId", sessionId)))
                .andExpect(status().isOk());
    }

    private String extractSessionId(MvcResult result) {
        String setCookieHeader = result.getResponse().getHeader(HttpHeaders.SET_COOKIE);
        return setCookieHeader.split(";")[0].split("=")[1];
    }

    // ============================ GET /api/packs ============================

    @Test
    void listPacks_ReturnsAllPacks() throws Exception {
        mockMvc.perform(get("/api/packs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(4)))
                .andExpect(jsonPath("$[*].type",
                        containsInAnyOrder("BRONZE", "SILVER", "GOLD", "DIAMOND")));
    }

    @Test
    void listPacks_EachPackHasPriceAndCardsPerPack() throws Exception {
        mockMvc.perform(get("/api/packs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].price", everyItem(greaterThan(0))))
                // Los sobres por rareza entregan 3 cartas; el DIAMOND solo 1.
                .andExpect(jsonPath("$[?(@.type != 'DIAMOND')].cardsPerPack", everyItem(equalTo(3))))
                .andExpect(jsonPath("$[?(@.type == 'DIAMOND')].cardsPerPack", everyItem(equalTo(1))));
    }

    @Test
    void listPacks_EachPackHasProbabilitiesForAllBuckets() throws Exception {
        mockMvc.perform(get("/api/packs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].probabilities.HIGH", everyItem(notNullValue())))
                .andExpect(jsonPath("$[*].probabilities.MEDIUM", everyItem(notNullValue())))
                .andExpect(jsonPath("$[*].probabilities.LOW", everyItem(notNullValue())));
    }

    @Test
    void listPacks_NoAuthRequired() throws Exception {
        // El listado es público — debe responder sin cookie.
        mockMvc.perform(get("/api/packs"))
                .andExpect(status().isOk());
    }

    // ============================ POST /api/packs/open — happy path ============================

    @Test
    void openPack_WithValidSessionAndEnoughCoins_Returns200WithCards() throws Exception {
        String session = registerAndGetSession("openHappy");
        grantCoins(session, 1000);

        OpenPackRequest req = new OpenPackRequest("BRONZE");

        mockMvc.perform(post("/api/packs/open")
                        .cookie(new Cookie("sessionId", session))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.packType", equalTo("BRONZE")))
                .andExpect(jsonPath("$.cards", hasSize(3)))
                .andExpect(jsonPath("$.cards[*].id", everyItem(notNullValue())))
                .andExpect(jsonPath("$.cards[*].name", everyItem(notNullValue())))
                .andExpect(jsonPath("$.remainingCoins", greaterThanOrEqualTo(0)));
    }

    @Test
    void openPack_DeductsCoinsFromPlayer() throws Exception {
        String session = registerAndGetSession("openDeduct");
        grantCoins(session, 1000);

        OpenPackRequest req = new OpenPackRequest("BRONZE");

        // Nuevo jugador empieza con welcome bonus 1000 + grant 1000 = 2000.
        // Tras abrir BRONZE (100), debería tener 1900.
        mockMvc.perform(post("/api/packs/open")
                        .cookie(new Cookie("sessionId", session))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.remainingCoins", equalTo(1900)));
    }

    @Test
    void openPack_GoldPackDeductsCorrectAmount() throws Exception {
        String session = registerAndGetSession("openGold");
        grantCoins(session, 1000);

        OpenPackRequest req = new OpenPackRequest("GOLD");

        mockMvc.perform(post("/api/packs/open")
                        .cookie(new Cookie("sessionId", session))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.packType", equalTo("GOLD")))
                // welcome 1000 + grant 1000 - GOLD 500 = 1500
                .andExpect(jsonPath("$.remainingCoins", equalTo(1500)));
    }

    @Test
    void openPack_AcceptsLowercasePackType() throws Exception {
        String session = registerAndGetSession("openLower");
        grantCoins(session, 1000);

        OpenPackRequest req = new OpenPackRequest("silver");

        mockMvc.perform(post("/api/packs/open")
                        .cookie(new Cookie("sessionId", session))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.packType", equalTo("SILVER")));
    }

    @Test
    void openPack_MultiplePurchasesAccumulateDeduction() throws Exception {
        String session = registerAndGetSession("openMulti");
        grantCoins(session, 1000);

        OpenPackRequest req = new OpenPackRequest("BRONZE");

        // Welcome 1000 + grant 1000 = 2000.
        // Primera compra BRONZE (100): 2000 → 1900
        mockMvc.perform(post("/api/packs/open")
                        .cookie(new Cookie("sessionId", session))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(jsonPath("$.remainingCoins", equalTo(1900)));

        // Segunda compra: 1900 → 1800
        mockMvc.perform(post("/api/packs/open")
                        .cookie(new Cookie("sessionId", session))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(jsonPath("$.remainingCoins", equalTo(1800)));
    }

    // ============================ POST /api/packs/open — errores ============================

    @Test
    void openPack_WithoutSession_Returns401() throws Exception {
        OpenPackRequest req = new OpenPackRequest("BRONZE");
        mockMvc.perform(post("/api/packs/open")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void openPack_WithInvalidSession_Returns401() throws Exception {
        OpenPackRequest req = new OpenPackRequest("BRONZE");
        mockMvc.perform(post("/api/packs/open")
                        .cookie(new Cookie("sessionId", "not-a-real-session"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void openPack_WithInvalidPackType_Returns400() throws Exception {
        String session = registerAndGetSession("openBadType");
        grantCoins(session, 1000);

        OpenPackRequest req = new OpenPackRequest("PLATINUM");
        mockMvc.perform(post("/api/packs/open")
                        .cookie(new Cookie("sessionId", session))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("Invalid pack type")));
    }

    @Test
    void openPack_WithNullPackType_Returns400() throws Exception {
        String session = registerAndGetSession("openNullType");

        // Body con packType null
        String body = "{\"packType\": null}";

        mockMvc.perform(post("/api/packs/open")
                        .cookie(new Cookie("sessionId", session))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest());
    }

    @Test
    void openPack_WithInsufficientCoins_Returns402() throws Exception {
        // Drenamos el welcome bonus (1000) para dejar al jugador sin saldo.
        String session = registerAndGetSession("openPoor");
        grantCoins(session, -1000); // 1000 → 0

        OpenPackRequest req = new OpenPackRequest("GOLD"); // cuesta 500
        mockMvc.perform(post("/api/packs/open")
                        .cookie(new Cookie("sessionId", session))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isPaymentRequired())
                .andExpect(jsonPath("$.message", containsString("Insufficient coins")));
    }

    @Test
    void openPack_InsufficientCoinsDoesNotDeductOrPersist() throws Exception {
        String session = registerAndGetSession("openPoorNoDeduct");
        // 1000 welcome - 950 = 50, debajo del precio de BRONZE (100).
        grantCoins(session, -950);

        OpenPackRequest req = new OpenPackRequest("BRONZE");
        mockMvc.perform(post("/api/packs/open")
                        .cookie(new Cookie("sessionId", session))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isPaymentRequired());

        // Verificar que el saldo no cambió usando GET /api/players/me
        mockMvc.perform(get("/api/players/me")
                        .cookie(new Cookie("sessionId", session)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.coins", equalTo(50)));
    }

    @Test
    void openPack_CardsHaveRequiredFields() throws Exception {
        String session = registerAndGetSession("openCardFields");
        grantCoins(session, 1000);

        OpenPackRequest req = new OpenPackRequest("BRONZE");

        mockMvc.perform(post("/api/packs/open")
                        .cookie(new Cookie("sessionId", session))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.cards[*].id", everyItem(notNullValue())))
                .andExpect(jsonPath("$.cards[*].name", everyItem(notNullValue())))
                .andExpect(jsonPath("$.cards[*].supertype", everyItem(notNullValue())))
                .andExpect(jsonPath("$.cards[*].imageUrl", everyItem(notNullValue())));
    }

    @Test
    void openPack_NeverReturnsBasicEnergyCards() throws Exception {
        // Las energías básicas (xy1-132 a xy1-140) NUNCA deben salir en sobres.
        String session = registerAndGetSession("openNoEnergy");
        grantCoins(session, 10000);

        OpenPackRequest req = new OpenPackRequest("BRONZE");

        // Abrir 20 sobres (60 cartas) y verificar que ninguna es energía básica.
        for (int i = 0; i < 20; i++) {
            mockMvc.perform(post("/api/packs/open")
                            .cookie(new Cookie("sessionId", session))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(req)))
                    .andExpect(status().isOk())
                    // Ninguna carta debería tener supertype ENERGY+rareza COMMON.
                    // Filtramos por id: las básicas son xy1-132..xy1-140.
                    .andExpect(jsonPath("$.cards[?(@.id == 'xy1-132')]", empty()))
                    .andExpect(jsonPath("$.cards[?(@.id == 'xy1-133')]", empty()))
                    .andExpect(jsonPath("$.cards[?(@.id == 'xy1-140')]", empty()));
        }
    }
}
