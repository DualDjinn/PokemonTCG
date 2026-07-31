package ar.edu.utn.frc.tup.piii.controllers;

import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import ar.edu.utn.frc.tup.piii.configs.Authenticated;
import ar.edu.utn.frc.tup.piii.dtos.player.LoginRequest;
import ar.edu.utn.frc.tup.piii.dtos.player.PlayerRequest;
import ar.edu.utn.frc.tup.piii.dtos.player.PlayerResponse;
import ar.edu.utn.frc.tup.piii.models.Match;
import ar.edu.utn.frc.tup.piii.models.Player;
import ar.edu.utn.frc.tup.piii.repositories.PlayerRepository;
import ar.edu.utn.frc.tup.piii.services.MatchService;
import ar.edu.utn.frc.tup.piii.services.PlayerService;

/**
 * Development-only endpoints for testing. Do NOT deploy to production.
 */
@RestController
@RequestMapping("/api/dev")
public class DevController {

    private static final String TEST_PASSWORD = "test1234";

    private final PlayerService playerService;
    private final MatchService matchService;
    private final PlayerRepository playerRepository;

    public DevController(PlayerService playerService,
                         @Qualifier("matchLobbyService") MatchService matchService,
                         PlayerRepository playerRepository) {
        this.playerService = playerService;
        this.matchService = matchService;
        this.playerRepository = playerRepository;
    }

    /**
     * Adds coins to the authenticated player. Defaults to 1000 if {@code amount}
     * is not specified. Useful for testing the pack shop without buying coins
     * with real money via MercadoPago.
     *
     * <p>Examples:
     * <pre>
     *   POST /api/dev/grant-coins
     *   POST /api/dev/grant-coins?amount=5000
     * </pre>
     */
    @Transactional
    @PostMapping("/grant-coins")
    public ResponseEntity<Map<String, Object>> grantCoins(
            @Authenticated Player authPlayer,
            @RequestParam(defaultValue = "1000") int amount) {

        Player player = playerRepository.findById(authPlayer.getId()).orElseThrow();
        int previous = player.getCoins();
        player.setCoins(previous + amount);
        playerRepository.save(player);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("playerId", player.getId());
        result.put("previousCoins", previous);
        result.put("granted", amount);
        result.put("currentCoins", player.getCoins());
        return ResponseEntity.ok(result);
    }

    /**
     * Creates a test match using the currently logged-in player as player one
     * and a bot opponent as player two. If no session cookie is present, both
     * players are auto-created test accounts.
     */
    @RequestMapping(value = "/test-match", method = {RequestMethod.GET, RequestMethod.POST})
    public ResponseEntity<Map<String, Object>> createTestMatch(
            @CookieValue(value = "sessionId", required = false) String sessionId) {

        PlayerResponse playerOne = resolvePlayerOne(sessionId);
        PlayerResponse playerTwo = getOrCreateTestPlayer("dev_opponent", "Gary", "dev_opponent@test.com");

        String deckOneId = playerOne.getDecks().get(0).getId();
        String deckTwoId = playerTwo.getDecks().get(0).getId();

        Match match = matchService.createMatch(playerOne.getId(), deckOneId,
                "Test Match", false, false);
        match = matchService.joinMatch(match.getId(), playerTwo.getId(), deckTwoId);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("matchId", match.getId());
        result.put("frontendUrl", "/batalla/" + match.getId());
        result.put("playerOneId", playerOne.getId());
        result.put("playerTwoId", playerTwo.getId());

        return ResponseEntity.ok(result);
    }

    private PlayerResponse resolvePlayerOne(String sessionId) {
        if (sessionId != null && !sessionId.isBlank()) {
            Player player = playerService.getPlayerBySession(sessionId).orElse(null);
            if (player != null) {
                return playerService.getPlayerById(player.getId());
            }
        }
        return getOrCreateTestPlayer("dev_host", "Ash", "dev_host@test.com");
    }

    private PlayerResponse getOrCreateTestPlayer(String username,
                                                  String trainerName,
                                                  String email) {
        try {
            PlayerRequest request = new PlayerRequest();
            request.setUsername(username);
            request.setTrainerName(trainerName);
            request.setEmail(email);
            request.setPassword(TEST_PASSWORD);
            request.setDateOfBirth(LocalDate.of(2000, 1, 1));
            return playerService.register(request);
        } catch (IllegalArgumentException ex) {
            LoginRequest login = new LoginRequest();
            login.setUsername(username);
            login.setPassword(TEST_PASSWORD);
            return playerService.login(login);
        }
    }
}
