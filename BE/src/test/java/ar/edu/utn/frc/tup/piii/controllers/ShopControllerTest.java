package ar.edu.utn.frc.tup.piii.controllers;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

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

import ar.edu.utn.frc.tup.piii.dtos.shop.BuyCartItemRequest;
import ar.edu.utn.frc.tup.piii.dtos.shop.BuyCartRequest;
import ar.edu.utn.frc.tup.piii.dtos.shop.BuyCartResponse;
import ar.edu.utn.frc.tup.piii.dtos.shop.BuySplashArtsRequest;
import ar.edu.utn.frc.tup.piii.models.Player;
import ar.edu.utn.frc.tup.piii.services.PlayerService;
import ar.edu.utn.frc.tup.piii.services.ShopService;
import jakarta.servlet.http.Cookie;

/**
 * Slice tests for {@link ShopController}. The {@link ShopService} and the
 * authentication layer ({@link PlayerService} behind {@code @Authenticated}) are
 * mocked so the controller's request mapping, header binding, status codes and
 * response bodies are verified in isolation.
 *
 * <p>Unlike most controllers, {@code ShopController} maps its own service errors
 * locally: an {@link IllegalArgumentException} becomes {@code 404 NOT_FOUND} and
 * an {@link IllegalStateException} becomes {@code 402 PAYMENT_REQUIRED}. The
 * {@code 401 UNAUTHORIZED} path comes from {@code AuthenticatedPlayerResolver}
 * throwing a {@link ResponseStatusException} that {@code GlobalExceptionHandler}
 * maps to the HTTP status.
 */
@SpringBootTest
class ShopControllerTest {

    private static final String SESSION_ID = "session-123";
    private static final String BAD_SESSION = "bad-session";
    private static final String PLAYER_ID = "p1";
    private static final String IDEMPOTENCY_HEADER = "Idempotency-Key";
    private static final String IDEMPOTENCY_KEY = "idem-key-1";

    @Autowired
    private WebApplicationContext webApplicationContext;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private ShopService shopService;

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
        when(playerService.getPlayerBySession(eq(BAD_SESSION))).thenReturn(Optional.empty());
    }

    private Cookie sessionCookie() {
        return new Cookie("sessionId", SESSION_ID);
    }

    private BuyCartRequest cartRequest() {
        BuyCartRequest request = new BuyCartRequest();
        request.setItems(List.of(new BuyCartItemRequest("xy1-1", 2)));
        return request;
    }

    private BuySplashArtsRequest splashRequest() {
        BuySplashArtsRequest request = new BuySplashArtsRequest();
        request.setQuantity(3);
        return request;
    }

    // ---- buyCards ----

    @Test
    void buyCards_Authenticated_WithIdempotencyKey_Returns200WithRemainingCoins() throws Exception {
        BuyCartRequest request = cartRequest();
        when(shopService.buyCards(eq(PLAYER_ID), any(BuyCartRequest.class), eq(IDEMPOTENCY_KEY)))
                .thenReturn(new BuyCartResponse(900));

        mockMvc.perform(post("/api/shop/buy")
                        .cookie(sessionCookie())
                        .header(IDEMPOTENCY_HEADER, IDEMPOTENCY_KEY)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.remainingCoins").value(900));

        verify(shopService).buyCards(eq(PLAYER_ID), any(BuyCartRequest.class), eq(IDEMPOTENCY_KEY));
    }

    @Test
    void buyCards_Authenticated_WithoutIdempotencyKey_PassesNullToService() throws Exception {
        BuyCartRequest request = cartRequest();
        when(shopService.buyCards(eq(PLAYER_ID), any(BuyCartRequest.class), isNull()))
                .thenReturn(new BuyCartResponse(1234));

        mockMvc.perform(post("/api/shop/buy")
                        .cookie(sessionCookie())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.remainingCoins").value(1234));

        verify(shopService).buyCards(eq(PLAYER_ID), any(BuyCartRequest.class), isNull());
    }

    @Test
    void buyCards_InvalidSession_Returns401() throws Exception {
        mockMvc.perform(post("/api/shop/buy")
                        .cookie(new Cookie("sessionId", BAD_SESSION))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(cartRequest())))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void buyCards_MissingCookie_Returns401() throws Exception {
        mockMvc.perform(post("/api/shop/buy")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(cartRequest())))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void buyCards_CardNotFound_Returns404() throws Exception {
        when(shopService.buyCards(eq(PLAYER_ID), any(BuyCartRequest.class), any()))
                .thenThrow(new IllegalArgumentException("Card not found: xy1-99"));

        mockMvc.perform(post("/api/shop/buy")
                        .cookie(sessionCookie())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(cartRequest())))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.message").value("Card not found: xy1-99"));
    }

    @Test
    void buyCards_InsufficientCoins_Returns402() throws Exception {
        when(shopService.buyCards(eq(PLAYER_ID), any(BuyCartRequest.class), any()))
                .thenThrow(new IllegalStateException("Insufficient coins"));

        mockMvc.perform(post("/api/shop/buy")
                        .cookie(sessionCookie())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(cartRequest())))
                .andExpect(status().isPaymentRequired())
                .andExpect(jsonPath("$.status").value(402))
                .andExpect(jsonPath("$.message").value("Insufficient coins"));
    }

    // ---- buySplashArts ----

    @Test
    void buySplashArts_Authenticated_WithIdempotencyKey_Returns200WithRemainingCoins() throws Exception {
        BuySplashArtsRequest request = splashRequest();
        when(shopService.buySplashArts(eq(PLAYER_ID), any(BuySplashArtsRequest.class), eq(IDEMPOTENCY_KEY)))
                .thenReturn(new BuyCartResponse(700));

        mockMvc.perform(post("/api/shop/splash-arts/buy")
                        .cookie(sessionCookie())
                        .header(IDEMPOTENCY_HEADER, IDEMPOTENCY_KEY)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.remainingCoins").value(700));

        verify(shopService).buySplashArts(eq(PLAYER_ID), any(BuySplashArtsRequest.class), eq(IDEMPOTENCY_KEY));
    }

    @Test
    void buySplashArts_Authenticated_WithoutIdempotencyKey_PassesNullToService() throws Exception {
        BuySplashArtsRequest request = splashRequest();
        when(shopService.buySplashArts(eq(PLAYER_ID), any(BuySplashArtsRequest.class), isNull()))
                .thenReturn(new BuyCartResponse(555));

        mockMvc.perform(post("/api/shop/splash-arts/buy")
                        .cookie(sessionCookie())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.remainingCoins").value(555));

        verify(shopService).buySplashArts(eq(PLAYER_ID), any(BuySplashArtsRequest.class), isNull());
    }

    @Test
    void buySplashArts_InvalidSession_Returns401() throws Exception {
        mockMvc.perform(post("/api/shop/splash-arts/buy")
                        .cookie(new Cookie("sessionId", BAD_SESSION))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(splashRequest())))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void buySplashArts_PlayerNotFound_Returns404() throws Exception {
        when(shopService.buySplashArts(eq(PLAYER_ID), any(BuySplashArtsRequest.class), any()))
                .thenThrow(new IllegalArgumentException("Player not found"));

        mockMvc.perform(post("/api/shop/splash-arts/buy")
                        .cookie(sessionCookie())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(splashRequest())))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.message").value("Player not found"));
    }

    @Test
    void buySplashArts_InsufficientCoins_Returns402() throws Exception {
        when(shopService.buySplashArts(eq(PLAYER_ID), any(BuySplashArtsRequest.class), any()))
                .thenThrow(new IllegalStateException("Insufficient coins"));

        mockMvc.perform(post("/api/shop/splash-arts/buy")
                        .cookie(sessionCookie())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(splashRequest())))
                .andExpect(status().isPaymentRequired())
                .andExpect(jsonPath("$.status").value(402))
                .andExpect(jsonPath("$.message").value("Insufficient coins"));
    }
}
