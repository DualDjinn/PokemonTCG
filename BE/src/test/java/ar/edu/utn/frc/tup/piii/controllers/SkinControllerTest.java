package ar.edu.utn.frc.tup.piii.controllers;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import static org.hamcrest.Matchers.hasSize;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.server.ResponseStatusException;

import com.fasterxml.jackson.databind.ObjectMapper;

import ar.edu.utn.frc.tup.piii.dtos.skin.CardSkinDTO;
import ar.edu.utn.frc.tup.piii.dtos.skin.EquipSkinRequest;
import ar.edu.utn.frc.tup.piii.dtos.skin.PlayerSkinDTO;
import ar.edu.utn.frc.tup.piii.models.Player;
import ar.edu.utn.frc.tup.piii.services.PlayerService;
import ar.edu.utn.frc.tup.piii.services.SkinService;
import jakarta.servlet.http.Cookie;

/**
 * Slice tests for {@link SkinController}. The {@link SkinService} and the
 * authentication layer ({@link PlayerService} via {@code AuthenticatedPlayerResolver})
 * are mocked so the controller's request mapping, status codes and response
 * bodies are verified in isolation.
 *
 * <p>Unlike {@code MatchController}, the equip/unequip endpoints translate
 * service exceptions to HTTP statuses inside the controller itself:
 * {@link IllegalArgumentException} maps to {@code 400} and
 * {@link IllegalStateException} maps to {@code 409}, each returning an
 * {@code ErrorApi} body. Authentication failures are raised as
 * {@link ResponseStatusException} ({@code 401}) by the argument resolver.
 */
@SpringBootTest
class SkinControllerTest {

    private static final String SESSION_ID = "session-123";
    private static final String PLAYER_ID = "p1";
    private static final String CARD_ID = "card-1";
    private static final String SKIN_ID = "skin-1";

    @Autowired
    private WebApplicationContext webApplicationContext;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private SkinService skinService;

    @MockitoBean
    private PlayerService playerService;

    private MockMvc mockMvc;
    private Player host;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();
        host = new Player();
        host.setId(PLAYER_ID);
        when(playerService.getPlayerBySession(SESSION_ID)).thenReturn(Optional.of(host));
        when(playerService.getPlayerBySession("bad-session")).thenReturn(Optional.empty());
    }

    private Cookie sessionCookie() {
        return new Cookie("sessionId", SESSION_ID);
    }

    private CardSkinDTO sampleCardSkin() {
        return CardSkinDTO.builder()
                .id(SKIN_ID)
                .name("Shiny Pikachu")
                .baseCardId(CARD_ID)
                .origin("PACK")
                .imageUrl("http://img/small.png")
                .imageUrlLarge("http://img/large.png")
                .tag("shiny")
                .build();
    }

    private PlayerSkinDTO samplePlayerSkin(boolean equipped) {
        return PlayerSkinDTO.builder()
                .skinId(SKIN_ID)
                .skinName("Shiny Pikachu")
                .baseCardId(CARD_ID)
                .baseCardName("Pikachu")
                .origin("PACK")
                .imageUrl("http://img/small.png")
                .imageUrlLarge("http://img/large.png")
                .quantity(2)
                .equipped(equipped)
                .build();
    }

    private EquipSkinRequest equipRequest(String cardId, String skinId) {
        EquipSkinRequest request = new EquipSkinRequest();
        request.setCardId(cardId);
        request.setSkinId(skinId);
        return request;
    }

    // ---- getAllSkins (GET /api/skins, no auth) ----

    @Test
    void getAllSkins_Returns200WithCatalog() throws Exception {
        when(skinService.getAllSkins()).thenReturn(List.of(sampleCardSkin()));

        mockMvc.perform(get("/api/skins"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].id").value(SKIN_ID))
                .andExpect(jsonPath("$[0].baseCardId").value(CARD_ID))
                .andExpect(jsonPath("$[0].origin").value("PACK"));

        verify(skinService).getAllSkins();
    }

    @Test
    void getAllSkins_EmptyCatalog_ReturnsEmptyArray() throws Exception {
        when(skinService.getAllSkins()).thenReturn(List.of());

        mockMvc.perform(get("/api/skins"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));

        verify(skinService).getAllSkins();
    }

    // ---- getSkinsForCard (GET /api/skins/card/{cardId}, no auth) ----

    @Test
    void getSkinsForCard_Returns200WithSkins() throws Exception {
        when(skinService.getSkinsForCard(CARD_ID)).thenReturn(List.of(sampleCardSkin()));

        mockMvc.perform(get("/api/skins/card/{cardId}", CARD_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].id").value(SKIN_ID))
                .andExpect(jsonPath("$[0].baseCardId").value(CARD_ID));

        verify(skinService).getSkinsForCard(CARD_ID);
    }

    // ---- getPlayerSkins (GET /api/skins/me, auth required) ----

    @Test
    void getPlayerSkins_Authenticated_Returns200WithPlayerSkins() throws Exception {
        when(skinService.getPlayerSkins(PLAYER_ID)).thenReturn(List.of(samplePlayerSkin(true)));

        mockMvc.perform(get("/api/skins/me")
                        .cookie(sessionCookie()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].skinId").value(SKIN_ID))
                .andExpect(jsonPath("$[0].baseCardId").value(CARD_ID))
                .andExpect(jsonPath("$[0].equipped").value(true));

        verify(skinService).getPlayerSkins(PLAYER_ID);
    }

    @Test
    void getPlayerSkins_InvalidSession_Returns401() throws Exception {
        mockMvc.perform(get("/api/skins/me")
                        .cookie(new Cookie("sessionId", "bad-session")))
                .andExpect(status().isUnauthorized());

        verifyNoInteractions(skinService);
    }

    @Test
    void getPlayerSkins_MissingCookie_Returns401() throws Exception {
        mockMvc.perform(get("/api/skins/me"))
                .andExpect(status().isUnauthorized());

        verifyNoInteractions(skinService);
    }

    // ---- equipSkin (POST /api/skins/me/equip, auth required) ----

    @Test
    void equipSkin_Authenticated_Returns200AndDelegates() throws Exception {
        doNothing().when(skinService).equipSkin(PLAYER_ID, CARD_ID, SKIN_ID);

        mockMvc.perform(post("/api/skins/me/equip")
                        .cookie(sessionCookie())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(equipRequest(CARD_ID, SKIN_ID))))
                .andExpect(status().isOk());

        verify(skinService).equipSkin(PLAYER_ID, CARD_ID, SKIN_ID);
    }

    @Test
    void equipSkin_InvalidSession_Returns401() throws Exception {
        mockMvc.perform(post("/api/skins/me/equip")
                        .cookie(new Cookie("sessionId", "bad-session"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(equipRequest(CARD_ID, SKIN_ID))))
                .andExpect(status().isUnauthorized());

        verifyNoInteractions(skinService);
    }

    @Test
    void equipSkin_InvalidCardOrSkin_Returns400WithErrorBody() throws Exception {
        doThrow(new IllegalArgumentException("Skin not found: " + SKIN_ID))
                .when(skinService).equipSkin(PLAYER_ID, CARD_ID, SKIN_ID);

        mockMvc.perform(post("/api/skins/me/equip")
                        .cookie(sessionCookie())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(equipRequest(CARD_ID, SKIN_ID))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("Bad Request"))
                .andExpect(jsonPath("$.message").value("Skin not found: " + SKIN_ID));

        verify(skinService).equipSkin(PLAYER_ID, CARD_ID, SKIN_ID);
    }

    @Test
    void equipSkin_PlayerDoesNotOwnSkin_Returns409WithErrorBody() throws Exception {
        doThrow(new IllegalStateException("Player does not own skin: " + SKIN_ID))
                .when(skinService).equipSkin(PLAYER_ID, CARD_ID, SKIN_ID);

        mockMvc.perform(post("/api/skins/me/equip")
                        .cookie(sessionCookie())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(equipRequest(CARD_ID, SKIN_ID))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.error").value("Conflict"))
                .andExpect(jsonPath("$.message").value("Player does not own skin: " + SKIN_ID));

        verify(skinService).equipSkin(PLAYER_ID, CARD_ID, SKIN_ID);
    }

    // ---- unequipSkin (POST /api/skins/me/unequip, auth required) ----

    @Test
    void unequipSkin_Authenticated_Returns200AndDelegates() throws Exception {
        doNothing().when(skinService).unequipSkin(PLAYER_ID, CARD_ID);

        mockMvc.perform(post("/api/skins/me/unequip")
                        .cookie(sessionCookie())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(equipRequest(CARD_ID, null))))
                .andExpect(status().isOk());

        verify(skinService).unequipSkin(PLAYER_ID, CARD_ID);
    }

    @Test
    void unequipSkin_InvalidSession_Returns401() throws Exception {
        mockMvc.perform(post("/api/skins/me/unequip")
                        .cookie(new Cookie("sessionId", "bad-session"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(equipRequest(CARD_ID, null))))
                .andExpect(status().isUnauthorized());

        verifyNoInteractions(skinService);
    }

    @Test
    void unequipSkin_PlayerDoesNotOwnCard_Returns400WithErrorBody() throws Exception {
        doThrow(new IllegalArgumentException("Player does not own card: " + CARD_ID))
                .when(skinService).unequipSkin(PLAYER_ID, CARD_ID);

        mockMvc.perform(post("/api/skins/me/unequip")
                        .cookie(sessionCookie())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(equipRequest(CARD_ID, null))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("Bad Request"))
                .andExpect(jsonPath("$.message").value("Player does not own card: " + CARD_ID));

        verify(skinService).unequipSkin(PLAYER_ID, CARD_ID);
    }

    // ---- anyString() guard to document the unequip path ignores skinId ----

    @Test
    void unequipSkin_IgnoresSkinIdInBody_OnlyUsesCardId() throws Exception {
        doNothing().when(skinService).unequipSkin(anyString(), anyString());

        mockMvc.perform(post("/api/skins/me/unequip")
                        .cookie(sessionCookie())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(equipRequest(CARD_ID, "ignored-skin"))))
                .andExpect(status().isOk());

        verify(skinService).unequipSkin(PLAYER_ID, CARD_ID);
    }
}
