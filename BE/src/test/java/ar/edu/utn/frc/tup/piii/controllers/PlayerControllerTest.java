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

import ar.edu.utn.frc.tup.piii.dtos.player.AddCardRequest;
import ar.edu.utn.frc.tup.piii.dtos.player.ForgotPasswordRequest;
import ar.edu.utn.frc.tup.piii.dtos.player.GoogleLoginRequest;
import ar.edu.utn.frc.tup.piii.dtos.player.LoginRequest;
import ar.edu.utn.frc.tup.piii.dtos.player.PlayerRequest;
import ar.edu.utn.frc.tup.piii.dtos.player.ResetPasswordRequest;
import ar.edu.utn.frc.tup.piii.dtos.player.UpdateCharacterRequest;
import ar.edu.utn.frc.tup.piii.dtos.player.UpdateTrainerNameRequest;
import jakarta.servlet.http.Cookie;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
class PlayerControllerTest {

    @Autowired
    private WebApplicationContext webApplicationContext;

    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();
    }

    @Test
    void register_Success_Returns201AndCookie() throws Exception {
        PlayerRequest request = new PlayerRequest();
        request.setUsername("regSuccessUser");
        request.setEmail("regsuccess@test.com");
        request.setPassword("pass123");
        request.setTrainerName("TrainerReg");

        mockMvc.perform(post("/api/players/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(header().exists(HttpHeaders.SET_COOKIE))
                .andExpect(jsonPath("$.username", is("regSuccessUser")))
                .andExpect(jsonPath("$.id", not(emptyOrNullString())));
    }

    @Test
    void register_DuplicateUsername_Returns400() throws Exception {
        PlayerRequest request = new PlayerRequest();
        request.setUsername("regDupeUser");
        request.setEmail("regdupe@test.com");
        request.setPassword("pass123");
        request.setTrainerName("TrainerDupe");

        mockMvc.perform(post("/api/players/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/players/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("already exists")));
    }

    @Test
    void register_DuplicateEmail_Returns400() throws Exception {
        PlayerRequest first = new PlayerRequest();
        first.setUsername("regEmail1");
        first.setEmail("dupemail@test.com");
        first.setPassword("pass123");
        first.setTrainerName("Trainer1");

        PlayerRequest second = new PlayerRequest();
        second.setUsername("regEmail2");
        second.setEmail("dupemail@test.com");
        second.setPassword("pass456");
        second.setTrainerName("Trainer2");

        mockMvc.perform(post("/api/players/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(first)))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/players/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(second)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("already exists")));
    }

    @Test
    void login_Success_Returns200AndCookie() throws Exception {
        PlayerRequest regRequest = new PlayerRequest();
        regRequest.setUsername("loginSuccessUser");
        regRequest.setEmail("loginsuccess@test.com");
        regRequest.setPassword("pass123");
        regRequest.setTrainerName("TrainerLogin");

        mockMvc.perform(post("/api/players/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(regRequest)))
                .andExpect(status().isCreated());

        LoginRequest loginRequest = new LoginRequest();
        loginRequest.setUsername("loginSuccessUser");
        loginRequest.setPassword("pass123");

        mockMvc.perform(post("/api/players/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andExpect(header().exists(HttpHeaders.SET_COOKIE))
                .andExpect(jsonPath("$.username", is("loginSuccessUser")));
    }

    @Test
    void login_WrongPassword_Returns401() throws Exception {
        PlayerRequest regRequest = new PlayerRequest();
        regRequest.setUsername("loginWrongUser");
        regRequest.setEmail("loginwrong@test.com");
        regRequest.setPassword("correctPass");
        regRequest.setTrainerName("TrainerWrong");

        mockMvc.perform(post("/api/players/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(regRequest)))
                .andExpect(status().isCreated());

        LoginRequest loginRequest = new LoginRequest();
        loginRequest.setUsername("loginWrongUser");
        loginRequest.setPassword("wrongPass");

        mockMvc.perform(post("/api/players/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message", containsString("Invalid")));
    }

    @Test
    void login_NonExistingUser_Returns401() throws Exception {
        LoginRequest loginRequest = new LoginRequest();
        loginRequest.setUsername("nonexistentUser");
        loginRequest.setPassword("anyPass");

        mockMvc.perform(post("/api/players/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message", containsString("Invalid")));
    }

    @Test
    void getMe_WithValidCookie_ReturnsPlayer() throws Exception {
        PlayerRequest request = new PlayerRequest();
        request.setUsername("getMeValidUser");
        request.setEmail("getmevalid@test.com");
        request.setPassword("pass123");
        request.setTrainerName("TrainerGetMe");

        MvcResult regResult = mockMvc.perform(post("/api/players/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andReturn();

        String setCookieHeader = regResult.getResponse().getHeader(HttpHeaders.SET_COOKIE);
        String sessionId = setCookieHeader.split(";")[0].split("=")[1];

        mockMvc.perform(get("/api/players/me")
                        .cookie(new Cookie("sessionId", sessionId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username", is("getMeValidUser")))
                .andExpect(jsonPath("$.email", is("getmevalid@test.com")));
    }

    @Test
    void getMe_WithoutCookie_Returns4xx() throws Exception {
        mockMvc.perform(get("/api/players/me"))
                .andExpect(status().is4xxClientError());
    }

    @Test
    void getMeDecks_WithValidCookie_ReturnsDecks() throws Exception {
        PlayerRequest request = new PlayerRequest();
        request.setUsername("getMeDecksUser");
        request.setEmail("getmedecks@test.com");
        request.setPassword("pass123");
        request.setTrainerName("TrainerDecks");

        MvcResult regResult = mockMvc.perform(post("/api/players/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andReturn();

        String setCookieHeader = regResult.getResponse().getHeader(HttpHeaders.SET_COOKIE);
        String sessionId = setCookieHeader.split(";")[0].split("=")[1];

        mockMvc.perform(get("/api/players/me/decks")
                        .cookie(new Cookie("sessionId", sessionId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(greaterThan(0))))
                .andExpect(jsonPath("$[*].name", hasItem(containsString("Deck"))));
    }

    @Test
    void getPlayerById_ExistingId_ReturnsPlayer() throws Exception {
        PlayerRequest request = new PlayerRequest();
        request.setUsername("getByIdUser");
        request.setEmail("getbyid@test.com");
        request.setPassword("pass123");
        request.setTrainerName("TrainerById");

        MvcResult regResult = mockMvc.perform(post("/api/players/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andReturn();

        String regBody = regResult.getResponse().getContentAsString();
        String playerId = objectMapper.readTree(regBody).get("id").asText();

        mockMvc.perform(get("/api/players/{id}", playerId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username", is("getByIdUser")));
    }

    @Test
    void getPlayerById_NonExistingId_Returns400() throws Exception {
        mockMvc.perform(get("/api/players/nonexistent"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("not found")));
    }

    @Test
    void updateTrainerName_WithValidCookie_UpdatesName() throws Exception {
        PlayerRequest request = new PlayerRequest();
        request.setUsername("updateTrainerUser");
        request.setEmail("updatetrainer@test.com");
        request.setPassword("pass123");
        request.setTrainerName("OriginalName");

        MvcResult regResult = mockMvc.perform(post("/api/players/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andReturn();

        String setCookieHeader = regResult.getResponse().getHeader(HttpHeaders.SET_COOKIE);
        String sessionId = setCookieHeader.split(";")[0].split("=")[1];

        UpdateTrainerNameRequest updateRequest = new UpdateTrainerNameRequest();
        updateRequest.setTrainerName("NewTrainerName");

        mockMvc.perform(put("/api/players/trainer-name")
                        .cookie(new Cookie("sessionId", sessionId))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.trainerName", is("NewTrainerName")));
    }

    @Test
    void getCollection_WithValidCookie_ReturnsCollection() throws Exception {
        PlayerRequest request = new PlayerRequest();
        request.setUsername("getCollUser");
        request.setEmail("getcoll@test.com");
        request.setPassword("pass123");
        request.setTrainerName("TrainerColl");

        MvcResult regResult = mockMvc.perform(post("/api/players/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andReturn();

        String sessionId = extractSessionId(regResult);

        mockMvc.perform(get("/api/players/me/collection")
                        .cookie(new Cookie("sessionId", sessionId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", is(not(empty()))))
                .andExpect(jsonPath("$[*].id", hasItem(startsWith("xy1-"))));
    }

    @Test
    void getCollection_WithoutCookie_Returns4xx() throws Exception {
        mockMvc.perform(get("/api/players/me/collection"))
                .andExpect(status().is4xxClientError());
    }

    @Test
    void addToCollection_ValidCard_Returns200() throws Exception {
        PlayerRequest request = new PlayerRequest();
        request.setUsername("addCollUser");
        request.setEmail("addcoll@test.com");
        request.setPassword("pass123");
        request.setTrainerName("TrainerAdd");

        MvcResult regResult = mockMvc.perform(post("/api/players/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andReturn();

        String sessionId = extractSessionId(regResult);

        AddCardRequest addRequest = new AddCardRequest();
        addRequest.setCardId("xy1-1");

        mockMvc.perform(post("/api/players/me/collection")
                        .cookie(new Cookie("sessionId", sessionId))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(addRequest)))
                .andExpect(status().isOk());
    }

    @Test
    void addToCollection_NonExistingCard_Returns404() throws Exception {
        PlayerRequest request = new PlayerRequest();
        request.setUsername("addColl404User");
        request.setEmail("addcoll404@test.com");
        request.setPassword("pass123");
        request.setTrainerName("Trainer404");

        MvcResult regResult = mockMvc.perform(post("/api/players/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andReturn();

        String sessionId = extractSessionId(regResult);

        AddCardRequest addRequest = new AddCardRequest();
        addRequest.setCardId("invalid-card-id");

        mockMvc.perform(post("/api/players/me/collection")
                        .cookie(new Cookie("sessionId", sessionId))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(addRequest)))
                .andExpect(status().isNotFound());
    }

    @Test
    void addToCollection_WithoutCookie_Returns4xx() throws Exception {
        AddCardRequest addRequest = new AddCardRequest();
        addRequest.setCardId("xy1-1");

        mockMvc.perform(post("/api/players/me/collection")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(addRequest)))
                .andExpect(status().is4xxClientError());
    }

    @Test
    void removeFromCollection_WithValidCookie_Returns200() throws Exception {
        PlayerRequest request = new PlayerRequest();
        request.setUsername("remCollUser");
        request.setEmail("remcoll@test.com");
        request.setPassword("pass123");
        request.setTrainerName("TrainerRem");

        MvcResult regResult = mockMvc.perform(post("/api/players/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andReturn();

        String sessionId = extractSessionId(regResult);

        mockMvc.perform(delete("/api/players/me/collection/{cardId}", "xy1-1")
                        .cookie(new Cookie("sessionId", sessionId)))
                .andExpect(status().isOk());
    }

    @Test
    void removeFromCollection_WithoutCookie_Returns4xx() throws Exception {
        mockMvc.perform(delete("/api/players/me/collection/{cardId}", "xy1-1"))
                .andExpect(status().is4xxClientError());
    }

    @Test
    void getMeDecks_WithoutCookie_Returns4xx() throws Exception {
        mockMvc.perform(get("/api/players/me/decks"))
                .andExpect(status().is4xxClientError());
    }

    @Test
    void updateTrainerName_WithoutCookie_Returns4xx() throws Exception {
        UpdateTrainerNameRequest updateRequest = new UpdateTrainerNameRequest();
        updateRequest.setTrainerName("NewName");
        mockMvc.perform(put("/api/players/trainer-name")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().is4xxClientError());
    }

    @Test
    void updateTrainerName_WithInvalidSession_Returns401() throws Exception {
        UpdateTrainerNameRequest updateRequest = new UpdateTrainerNameRequest();
        updateRequest.setTrainerName("NewName");
        mockMvc.perform(put("/api/players/trainer-name")
                        .cookie(new Cookie("sessionId", "invalid-session-id"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void getMe_WithInvalidSession_Returns401() throws Exception {
        mockMvc.perform(get("/api/players/me")
                        .cookie(new Cookie("sessionId", "invalid-session-id")))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void getMeDecks_WithInvalidSession_Returns401() throws Exception {
        mockMvc.perform(get("/api/players/me/decks")
                        .cookie(new Cookie("sessionId", "invalid-session-id")))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void getMeCollection_WithInvalidSession_Returns401() throws Exception {
        mockMvc.perform(get("/api/players/me/collection")
                        .cookie(new Cookie("sessionId", "invalid-session-id")))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void addToCollection_WithInvalidSession_Returns401() throws Exception {
        AddCardRequest addRequest = new AddCardRequest();
        addRequest.setCardId("xy1-1");
        mockMvc.perform(post("/api/players/me/collection")
                        .cookie(new Cookie("sessionId", "invalid-session-id"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(addRequest)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void removeFromCollection_WithInvalidSession_Returns401() throws Exception {
        mockMvc.perform(delete("/api/players/me/collection/{cardId}", "xy1-1")
                        .cookie(new Cookie("sessionId", "invalid-session-id")))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void logout_WithValidSession_InvalidatesSession() throws Exception {
        PlayerRequest request = new PlayerRequest();
        request.setUsername("logoutUser");
        request.setEmail("logout@test.com");
        request.setPassword("pass123");
        request.setTrainerName("TrainerLogout");

        MvcResult regResult = mockMvc.perform(post("/api/players/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andReturn();

        String sessionId = extractSessionId(regResult);

        mockMvc.perform(post("/api/players/logout")
                        .cookie(new Cookie("sessionId", sessionId)))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/players/me")
                        .cookie(new Cookie("sessionId", sessionId)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void logout_WithoutCookie_Returns4xx() throws Exception {
        mockMvc.perform(post("/api/players/logout"))
                .andExpect(status().is4xxClientError());
    }

    @Test
    void register_WithInvalidEmail_Returns400() throws Exception {
        PlayerRequest request = new PlayerRequest();
        request.setUsername("badEmailUser");
        request.setEmail("invalid-email");
        request.setPassword("pass123");
        request.setTrainerName("TrainerBadEmail");

        mockMvc.perform(post("/api/players/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void updateCharacter_WithValidCookie_UpdatesCharacter() throws Exception {
        PlayerRequest request = new PlayerRequest();
        request.setUsername("updateCharUser");
        request.setEmail("updatechar@test.com");
        request.setPassword("pass123");
        request.setTrainerName("TrainerChar");

        MvcResult regResult = mockMvc.perform(post("/api/players/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andReturn();

        String sessionId = extractSessionId(regResult);

        UpdateCharacterRequest updateRequest = new UpdateCharacterRequest();
        updateRequest.setCharacter("red");
        updateRequest.setColor("default");

        mockMvc.perform(put("/api/players/character")
                        .cookie(new Cookie("sessionId", sessionId))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.character", is("red")));
    }

    @Test
    void setActiveDeck_WithValidCookie_Returns200() throws Exception {
        PlayerRequest request = new PlayerRequest();
        request.setUsername("setActiveDeckUser");
        request.setEmail("setactivedeck@test.com");
        request.setPassword("pass123");
        request.setTrainerName("TrainerActiveDeck");

        MvcResult regResult = mockMvc.perform(post("/api/players/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andReturn();

        String sessionId = extractSessionId(regResult);

        mockMvc.perform(put("/api/players/me/active-deck")
                        .cookie(new Cookie("sessionId", sessionId))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"deckId\":\"\"}"))
                .andExpect(status().isOk());
    }

    @Test
    void forgotPassword_WithValidEmail_Returns200() throws Exception {
        ForgotPasswordRequest forgotRequest = new ForgotPasswordRequest();
        forgotRequest.setEmail("test@example.com");

        mockMvc.perform(post("/api/players/forgot-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(forgotRequest)))
                .andExpect(status().isOk());
    }

    @Test
    void resetPassword_WithInvalidToken_Returns400() throws Exception {
        ResetPasswordRequest resetRequest = new ResetPasswordRequest();
        resetRequest.setToken("invalid-token");
        resetRequest.setNewPassword("newPass123");

        mockMvc.perform(post("/api/players/reset-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(resetRequest)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void googleLogin_EmptyCredential_Returns400() throws Exception {
        GoogleLoginRequest request = new GoogleLoginRequest();
        request.setCredential("");

        mockMvc.perform(post("/api/players/google-login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    // -----------------------------------------------------------------------
    // Cookie security attributes — regression guard against silently dropping
    // HttpOnly, weakening Max-Age, or scoping the cookie incorrectly.
    // -----------------------------------------------------------------------

    @Test
    void register_SessionCookie_HasSecurityAttributes() throws Exception {
        PlayerRequest request = new PlayerRequest();
        request.setUsername("regCookieAttrsUser");
        request.setEmail("regcookieattrs@test.com");
        request.setPassword("pass123");
        request.setTrainerName("TrainerCookieAttrs");

        MvcResult result = mockMvc.perform(post("/api/players/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andReturn();

        String setCookie = result.getResponse().getHeader(HttpHeaders.SET_COOKIE);
        org.assertj.core.api.Assertions.assertThat(setCookie)
                .as("session cookie must be HttpOnly so XSS cannot read it")
                .containsIgnoringCase("HttpOnly");
        org.assertj.core.api.Assertions.assertThat(setCookie)
                .as("session cookie must be scoped to the whole app")
                .contains("Path=/");
        org.assertj.core.api.Assertions.assertThat(setCookie)
                .as("session cookie must expire in 24h (86400s)")
                .contains("Max-Age=86400");
    }

    @Test
    void login_SessionCookie_HasSecurityAttributes() throws Exception {
        PlayerRequest reg = new PlayerRequest();
        reg.setUsername("loginCookieAttrsUser");
        reg.setEmail("logincookieattrs@test.com");
        reg.setPassword("pass123");
        reg.setTrainerName("TrainerCookieLogin");

        mockMvc.perform(post("/api/players/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(reg)))
                .andExpect(status().isCreated());

        LoginRequest login = new LoginRequest();
        login.setUsername("loginCookieAttrsUser");
        login.setPassword("pass123");

        MvcResult result = mockMvc.perform(post("/api/players/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(login)))
                .andExpect(status().isOk())
                .andReturn();

        String setCookie = result.getResponse().getHeader(HttpHeaders.SET_COOKIE);
        org.assertj.core.api.Assertions.assertThat(setCookie)
                .containsIgnoringCase("HttpOnly")
                .contains("Path=/")
                .contains("Max-Age=86400");
    }

    @Test
    void logout_SessionCookie_ClearsWithMaxAgeZero() throws Exception {
        PlayerRequest request = new PlayerRequest();
        request.setUsername("logoutCookieClearUser");
        request.setEmail("logoutcookieclear@test.com");
        request.setPassword("pass123");
        request.setTrainerName("TrainerLogoutClear");

        MvcResult reg = mockMvc.perform(post("/api/players/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andReturn();

        String sessionId = extractSessionId(reg);

        MvcResult logout = mockMvc.perform(post("/api/players/logout")
                        .cookie(new Cookie("sessionId", sessionId)))
                .andExpect(status().isOk())
                .andReturn();

        String setCookie = logout.getResponse().getHeader(HttpHeaders.SET_COOKIE);
        org.assertj.core.api.Assertions.assertThat(setCookie)
                .as("logout must emit a clearing cookie so the browser drops the session id")
                .isNotNull()
                .contains("sessionId=")
                .contains("Max-Age=0");
    }

    @Test
    void logout_AfterLogout_SessionNoLongerValid() throws Exception {
        PlayerRequest request = new PlayerRequest();
        request.setUsername("logoutTwiceUser");
        request.setEmail("logouttwice@test.com");
        request.setPassword("pass123");
        request.setTrainerName("TrainerLogoutTwice");

        MvcResult reg = mockMvc.perform(post("/api/players/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andReturn();

        String sessionId = extractSessionId(reg);

        mockMvc.perform(post("/api/players/logout")
                        .cookie(new Cookie("sessionId", sessionId)))
                .andExpect(status().isOk());

        // Second use of the same sessionId on a protected endpoint must fail.
        mockMvc.perform(get("/api/players/me")
                        .cookie(new Cookie("sessionId", sessionId)))
                .andExpect(status().isUnauthorized());
    }

    // -----------------------------------------------------------------------
    // forgot-password: must not leak whether an email is registered (email
    // enumeration). Both branches — found and not-found — must return 200.
    // -----------------------------------------------------------------------

    @Test
    void forgotPassword_UnknownEmail_Returns200NotLeakingExistence() throws Exception {
        ForgotPasswordRequest req = new ForgotPasswordRequest();
        req.setEmail("definitely-not-registered@example.com");

        mockMvc.perform(post("/api/players/forgot-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(content().string(""));
    }

    @Test
    void forgotPassword_BlankEmail_Returns400() throws Exception {
        ForgotPasswordRequest req = new ForgotPasswordRequest();
        req.setEmail("   ");

        mockMvc.perform(post("/api/players/forgot-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest());
    }

    // -----------------------------------------------------------------------
    // Collection endpoints — documented idempotency (per Javadoc):
    // "If already present it is silently ignored"
    // "If not present it is silently ignored"
    // -----------------------------------------------------------------------

    @Test
    void addToCollection_DuplicateCard_StillReturns200() throws Exception {
        PlayerRequest request = new PlayerRequest();
        request.setUsername("dupCardUser");
        request.setEmail("dupcard@test.com");
        request.setPassword("pass123");
        request.setTrainerName("TrainerDupCard");

        MvcResult reg = mockMvc.perform(post("/api/players/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andReturn();
        String sessionId = extractSessionId(reg);

        AddCardRequest add = new AddCardRequest();
        add.setCardId("xy1-1");

        mockMvc.perform(post("/api/players/me/collection")
                        .cookie(new Cookie("sessionId", sessionId))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(add)))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/players/me/collection")
                        .cookie(new Cookie("sessionId", sessionId))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(add)))
                .andExpect(status().isOk());
    }

    @Test
    void removeFromCollection_CardNotInCollection_StillReturns200() throws Exception {
        PlayerRequest request = new PlayerRequest();
        request.setUsername("remMissingCardUser");
        request.setEmail("remmissing@test.com");
        request.setPassword("pass123");
        request.setTrainerName("TrainerRemMissing");

        MvcResult reg = mockMvc.perform(post("/api/players/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andReturn();
        String sessionId = extractSessionId(reg);

        mockMvc.perform(delete("/api/players/me/collection/{cardId}", "xy1-146")
                        .cookie(new Cookie("sessionId", sessionId)))
                .andExpect(status().isOk());
    }

    // -----------------------------------------------------------------------
    // Auth checks completeness — updateCharacter and setActiveDeck only had
    // happy-path coverage. Same auth contract as the other authenticated
    // endpoints must hold.
    // -----------------------------------------------------------------------

    @Test
    void updateCharacter_WithoutCookie_Returns4xx() throws Exception {
        UpdateCharacterRequest req = new UpdateCharacterRequest();
        req.setCharacter("red");
        req.setColor("default");

        mockMvc.perform(put("/api/players/character")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().is4xxClientError());
    }

    @Test
    void updateCharacter_WithInvalidSession_Returns401() throws Exception {
        UpdateCharacterRequest req = new UpdateCharacterRequest();
        req.setCharacter("red");
        req.setColor("default");

        mockMvc.perform(put("/api/players/character")
                        .cookie(new Cookie("sessionId", "invalid-session-id"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void setActiveDeck_WithoutCookie_Returns4xx() throws Exception {
        mockMvc.perform(put("/api/players/me/active-deck")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"deckId\":\"\"}"))
                .andExpect(status().is4xxClientError());
    }

    @Test
    void setActiveDeck_WithInvalidSession_Returns401() throws Exception {
        mockMvc.perform(put("/api/players/me/active-deck")
                        .cookie(new Cookie("sessionId", "invalid-session-id"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"deckId\":\"\"}"))
                .andExpect(status().isUnauthorized());
    }

    // -----------------------------------------------------------------------
    // grant-all-cards (admin/dev) — email null / blank / valid branches
    // -----------------------------------------------------------------------

    @Test
    void grantAllCards_BlankEmail_Returns400() throws Exception {
        mockMvc.perform(post("/api/players/grant-all-cards")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"   \"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void grantAllCards_MissingEmail_Returns400() throws Exception {
        mockMvc.perform(post("/api/players/grant-all-cards")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void grantAllCards_ValidEmail_Returns200() throws Exception {
        PlayerRequest request = new PlayerRequest();
        request.setUsername("grantAllUser");
        request.setEmail("grantall@test.com");
        request.setPassword("pass123");
        request.setTrainerName("TrainerGrantAll");
        mockMvc.perform(post("/api/players/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/players/grant-all-cards")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"grantall@test.com\"}"))
                .andExpect(status().isOk());
    }

    // -----------------------------------------------------------------------
    // request-password-change (authenticated self-service reset)
    // -----------------------------------------------------------------------

    @Test
    void requestPasswordChange_Authenticated_Returns200() throws Exception {
        PlayerRequest request = new PlayerRequest();
        request.setUsername("reqPwdChangeUser");
        request.setEmail("reqpwdchange@test.com");
        request.setPassword("pass123");
        request.setTrainerName("TrainerReqPwd");
        MvcResult reg = mockMvc.perform(post("/api/players/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andReturn();
        String sessionId = extractSessionId(reg);

        mockMvc.perform(post("/api/players/me/request-password-change")
                        .cookie(new Cookie("sessionId", sessionId)))
                .andExpect(status().isOk());
    }

    @Test
    void requestPasswordChange_WithoutCookie_Returns4xx() throws Exception {
        mockMvc.perform(post("/api/players/me/request-password-change"))
                .andExpect(status().is4xxClientError());
    }

    private String extractSessionId(MvcResult result) {
        String setCookieHeader = result.getResponse().getHeader(HttpHeaders.SET_COOKIE);
        return setCookieHeader.split(";")[0].split("=")[1];
    }
}
