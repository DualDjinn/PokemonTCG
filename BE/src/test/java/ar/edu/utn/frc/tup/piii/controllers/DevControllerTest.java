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

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
class DevControllerTest {

    @Autowired
    private WebApplicationContext webApplicationContext;

    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();
    }

    // ==================== grant-coins ====================

    @Test
    void grantCoins_WithValidSession_DefaultAmount_ReturnsOk() throws Exception {
        MvcResult regResult = registerUser("grantCoinsUser", "grantcoins@test.com");
        String sessionId = extractSessionId(regResult);

        mockMvc.perform(post("/api/dev/grant-coins")
                        .cookie(new Cookie("sessionId", sessionId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.playerId", not(emptyOrNullString())))
                .andExpect(jsonPath("$.granted", is(1000)))
                .andExpect(jsonPath("$.currentCoins",
                        is(greaterThanOrEqualTo(1000))));
    }

    @Test
    void grantCoins_WithCustomAmount_ReturnsOk() throws Exception {
        MvcResult regResult = registerUser("grantCustomUser", "grantcustom@test.com");
        String sessionId = extractSessionId(regResult);

        mockMvc.perform(post("/api/dev/grant-coins")
                        .cookie(new Cookie("sessionId", sessionId))
                        .param("amount", "5000"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.granted", is(5000)))
                .andExpect(jsonPath("$.currentCoins",
                        is(greaterThanOrEqualTo(5000))));
    }

    @Test
    void grantCoins_AccumulatesCoins() throws Exception {
        MvcResult regResult = registerUser("grantAccumUser", "grantaccum@test.com");
        String sessionId = extractSessionId(regResult);

        MvcResult firstGrant = mockMvc.perform(post("/api/dev/grant-coins")
                        .cookie(new Cookie("sessionId", sessionId))
                        .param("amount", "100"))
                .andExpect(status().isOk())
                .andReturn();

        int afterFirst = objectMapper.readTree(
                firstGrant.getResponse().getContentAsString())
                .get("currentCoins").asInt();

        mockMvc.perform(post("/api/dev/grant-coins")
                        .cookie(new Cookie("sessionId", sessionId))
                        .param("amount", "200"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.previousCoins", is(afterFirst)))
                .andExpect(jsonPath("$.granted", is(200)))
                .andExpect(jsonPath("$.currentCoins", is(afterFirst + 200)));
    }

    @Test
    void grantCoins_WithoutSession_Returns4xx() throws Exception {
        mockMvc.perform(post("/api/dev/grant-coins"))
                .andExpect(status().is4xxClientError());
    }

    @Test
    void grantCoins_WithInvalidSession_Returns401() throws Exception {
        mockMvc.perform(post("/api/dev/grant-coins")
                        .cookie(new Cookie("sessionId", "invalid-session-id")))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void grantCoins_ZeroAmount_ReturnsOk() throws Exception {
        MvcResult regResult = registerUser("grantZeroUser", "grantzero@test.com");
        String sessionId = extractSessionId(regResult);

        mockMvc.perform(post("/api/dev/grant-coins")
                        .cookie(new Cookie("sessionId", sessionId))
                        .param("amount", "0"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.granted", is(0)));
    }

    // ==================== test-match ====================

    @Test
    void createTestMatch_WithValidSession_ReturnsMatchInfo() throws Exception {
        MvcResult regResult = registerUser("testMatchUser", "testmatch@test.com");
        String sessionId = extractSessionId(regResult);

        mockMvc.perform(post("/api/dev/test-match")
                        .cookie(new Cookie("sessionId", sessionId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.matchId", not(emptyOrNullString())))
                .andExpect(jsonPath("$.frontendUrl", startsWith("/batalla/")))
                .andExpect(jsonPath("$.playerOneId", not(emptyOrNullString())))
                .andExpect(jsonPath("$.playerTwoId", not(emptyOrNullString())));
    }

    @Test
    void createTestMatch_WithoutSession_CreatesTestAccounts() throws Exception {
        mockMvc.perform(post("/api/dev/test-match"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.matchId", not(emptyOrNullString())))
                .andExpect(jsonPath("$.frontendUrl", startsWith("/batalla/")))
                .andExpect(jsonPath("$.playerOneId", not(emptyOrNullString())))
                .andExpect(jsonPath("$.playerTwoId", not(emptyOrNullString())));
    }

    @Test
    void createTestMatch_GetMethod_AlsoWorks() throws Exception {
        MvcResult regResult = registerUser("testMatchGetUser", "testmatchget@test.com");
        String sessionId = extractSessionId(regResult);

        mockMvc.perform(get("/api/dev/test-match")
                        .cookie(new Cookie("sessionId", sessionId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.matchId", not(emptyOrNullString())));
    }

    @Test
    void createTestMatch_WithInvalidSession_FallsBackToTestAccount() throws Exception {
        mockMvc.perform(post("/api/dev/test-match")
                        .cookie(new Cookie("sessionId", "invalid-session-id")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.matchId", not(emptyOrNullString())));
    }

    @Test
    void createTestMatch_WithBlankSessionId_FallsBackToTestAccount() throws Exception {
        mockMvc.perform(post("/api/dev/test-match")
                        .cookie(new Cookie("sessionId", "   ")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.matchId", not(emptyOrNullString())));
    }

    @Test
    void createTestMatch_PlayerOneAndTwoAreDifferent() throws Exception {
        MvcResult regResult = registerUser("testDiffUser", "testdiff@test.com");
        String sessionId = extractSessionId(regResult);

        MvcResult matchResult = mockMvc.perform(post("/api/dev/test-match")
                        .cookie(new Cookie("sessionId", sessionId)))
                .andExpect(status().isOk())
                .andReturn();

        String json = matchResult.getResponse().getContentAsString();
        String p1 = objectMapper.readTree(json).get("playerOneId").asText();
        String p2 = objectMapper.readTree(json).get("playerTwoId").asText();

        assert !p1.equals(p2) : "Player one and two should be different";
    }

    // ==================== helpers ====================

    private MvcResult registerUser(String username, String email) throws Exception {
        PlayerRequest request = new PlayerRequest();
        request.setUsername(username);
        request.setEmail(email);
        request.setPassword("pass123");
        request.setTrainerName("Trainer" + username);

        return mockMvc.perform(post("/api/players/register")
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
