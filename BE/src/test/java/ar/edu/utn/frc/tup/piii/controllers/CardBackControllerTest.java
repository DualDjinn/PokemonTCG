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

import ar.edu.utn.frc.tup.piii.dtos.cardback.EquipCardBackRequest;
import ar.edu.utn.frc.tup.piii.dtos.cardback.PurchaseCardBackRequest;
import ar.edu.utn.frc.tup.piii.dtos.pack.OpenPackRequest;
import ar.edu.utn.frc.tup.piii.dtos.player.PlayerRequest;
import jakarta.servlet.http.Cookie;

import java.util.UUID;

import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Tests de integración para {@link CardBackController}: catálogo, compra con
 * monedas reales, equipado y restauración del dorso por defecto.
 */
@SpringBootTest
class CardBackControllerTest {

    private static final String SHOP_BACK = "bio";
    private static final String OTHER_SHOP_BACK = "urbano";
    private static final String NON_SHOP_BACK = "pixel";

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

        String setCookieHeader = result.getResponse().getHeader(HttpHeaders.SET_COOKIE);
        return setCookieHeader.split(";")[0].split("=")[1];
    }

    private void grantCoins(String sessionId, int amount) throws Exception {
        mockMvc.perform(post("/api/dev/grant-coins")
                        .param("amount", String.valueOf(amount))
                        .cookie(session(sessionId)))
                .andExpect(status().isOk());
    }

    /** Drena los 1000 iniciales abriendo dos sobres GOLD (500 c/u) → saldo 0. */
    private void drainCoins(String sessionId) throws Exception {
        for (int i = 0; i < 2; i++) {
            mockMvc.perform(post("/api/packs/open")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(new OpenPackRequest("GOLD")))
                            .cookie(session(sessionId)))
                    .andExpect(status().isOk());
        }
    }

    private Cookie session(String sessionId) {
        return new Cookie("sessionId", sessionId);
    }

    private String purchaseBody(String backId) throws Exception {
        return objectMapper.writeValueAsString(new PurchaseCardBackRequest(backId));
    }

    private String equipBody(String backId) throws Exception {
        return objectMapper.writeValueAsString(new EquipCardBackRequest(backId));
    }

    // ============================ catálogo ============================

    @Test
    void getCatalog_ReturnsSeededBacks() throws Exception {
        mockMvc.perform(get("/api/card-backs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(greaterThanOrEqualTo(6))))
                .andExpect(jsonPath("$[*].id", hasItem(SHOP_BACK)))
                .andExpect(jsonPath("$[*].origin", hasItem("SHOP")))
                .andExpect(jsonPath("$[*].origin", hasItem("DEFAULT")));
    }

    // ============================ /me ============================

    @Test
    void getPlayerBacks_NewPlayer_HasDefaultGrantAndClassicEquipped() throws Exception {
        String sessionId = registerAndGetSession("cb_new");

        mockMvc.perform(get("/api/card-backs/me").cookie(session(sessionId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.activeCardBackId", nullValue()))
                // Pixel se otorga a todos los jugadores apenas consultan sus dorsos.
                .andExpect(jsonPath("$.owned[*].id", hasItem("pixel")));
    }

    @Test
    void getPlayerBacks_NoSession_Returns401() throws Exception {
        mockMvc.perform(get("/api/card-backs/me"))
                .andExpect(status().isUnauthorized());
    }

    // ============================ compra ============================

    @Test
    void purchase_WithCoins_GrantsBack() throws Exception {
        String sessionId = registerAndGetSession("cb_buy");
        grantCoins(sessionId, 1000);

        mockMvc.perform(post("/api/card-backs/me/purchase")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(purchaseBody(SHOP_BACK))
                        .cookie(session(sessionId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.owned[*].id", hasItem(SHOP_BACK)));
    }

    @Test
    void purchase_Twice_SecondReturns409() throws Exception {
        String sessionId = registerAndGetSession("cb_dup");
        grantCoins(sessionId, 5000);

        mockMvc.perform(post("/api/card-backs/me/purchase")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(purchaseBody(SHOP_BACK))
                        .cookie(session(sessionId)))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/card-backs/me/purchase")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(purchaseBody(SHOP_BACK))
                        .cookie(session(sessionId)))
                .andExpect(status().isConflict());
    }

    @Test
    void purchase_WithoutCoins_Returns409() throws Exception {
        String sessionId = registerAndGetSession("cb_poor");
        drainCoins(sessionId); // el jugador nuevo arranca con 1000; lo dejamos en 0

        mockMvc.perform(post("/api/card-backs/me/purchase")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(purchaseBody(SHOP_BACK))
                        .cookie(session(sessionId)))
                .andExpect(status().isConflict());
    }

    @Test
    void purchase_NonShopBack_Returns409() throws Exception {
        String sessionId = registerAndGetSession("cb_nonshop");
        grantCoins(sessionId, 5000);

        // "pixel" es origen DEFAULT (no está a la venta): comprarlo debe dar 409.
        mockMvc.perform(post("/api/card-backs/me/purchase")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(purchaseBody(NON_SHOP_BACK))
                        .cookie(session(sessionId)))
                .andExpect(status().isConflict());
    }

    @Test
    void purchase_UnknownBack_Returns400() throws Exception {
        String sessionId = registerAndGetSession("cb_404");

        mockMvc.perform(post("/api/card-backs/me/purchase")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(purchaseBody("no-existe"))
                        .cookie(session(sessionId)))
                .andExpect(status().isBadRequest());
    }

    // ============================ equipar ============================

    @Test
    void equip_OwnedBack_ThenRestoreDefault() throws Exception {
        String sessionId = registerAndGetSession("cb_equip");
        grantCoins(sessionId, 1000);

        mockMvc.perform(post("/api/card-backs/me/purchase")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(purchaseBody(SHOP_BACK))
                        .cookie(session(sessionId)))
                .andExpect(status().isOk());

        mockMvc.perform(put("/api/card-backs/me/active")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(equipBody(SHOP_BACK))
                        .cookie(session(sessionId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.activeCardBackId", is(SHOP_BACK)));

        // El PlayerResponse también expone el dorso activo (para el front)
        mockMvc.perform(get("/api/players/me").cookie(session(sessionId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.activeCardBackId", is(SHOP_BACK)));

        mockMvc.perform(put("/api/card-backs/me/active")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(equipBody(null))
                        .cookie(session(sessionId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.activeCardBackId", nullValue()));
    }

    @Test
    void equip_UnownedBack_Returns409() throws Exception {
        String sessionId = registerAndGetSession("cb_steal");

        mockMvc.perform(put("/api/card-backs/me/active")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(equipBody(OTHER_SHOP_BACK))
                        .cookie(session(sessionId)))
                .andExpect(status().isConflict());
    }
}
