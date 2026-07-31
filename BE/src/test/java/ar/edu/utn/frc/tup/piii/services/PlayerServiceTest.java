package ar.edu.utn.frc.tup.piii.services;

import ar.edu.utn.frc.tup.piii.dtos.player.LoginRequest;
import ar.edu.utn.frc.tup.piii.dtos.player.PlayerRequest;
import ar.edu.utn.frc.tup.piii.dtos.player.PlayerResponse;
import ar.edu.utn.frc.tup.piii.dtos.player.UpdateCharacterRequest;
import ar.edu.utn.frc.tup.piii.dtos.player.UpdateTrainerNameRequest;
import ar.edu.utn.frc.tup.piii.models.Attack;
import ar.edu.utn.frc.tup.piii.models.Card;
import ar.edu.utn.frc.tup.piii.models.CardBack;
import ar.edu.utn.frc.tup.piii.models.Deck;
import ar.edu.utn.frc.tup.piii.models.EnergyType;
import ar.edu.utn.frc.tup.piii.models.Player;
import ar.edu.utn.frc.tup.piii.models.PokemonCard;
import ar.edu.utn.frc.tup.piii.models.PokemonStage;
import ar.edu.utn.frc.tup.piii.models.Supertype;
import ar.edu.utn.frc.tup.piii.repositories.CardBackRepository;
import ar.edu.utn.frc.tup.piii.repositories.CardRepository;
import ar.edu.utn.frc.tup.piii.repositories.PlayerCardRepository;
import ar.edu.utn.frc.tup.piii.repositories.PlayerRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedConstruction;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

class PlayerServiceTest {

    private PlayerRepository playerRepository;
    private CardRepository cardRepository;
    private ObjectMapper objectMapper;
    private PlayerService playerService;
    private PasswordEncoder passwordEncoder;
    private EmailService emailService;
    private CardBackRepository cardBackRepository;
    private PlayerCardRepository playerCardRepository;

    @BeforeEach
    void setUp() {
        playerRepository = mock(PlayerRepository.class);
        cardRepository = mock(CardRepository.class);
        objectMapper = new ObjectMapper();
        passwordEncoder = new BCryptPasswordEncoder();
        emailService = mock(EmailService.class);
        cardBackRepository = mock(CardBackRepository.class);
        playerCardRepository = mock(PlayerCardRepository.class);
        playerService = new PlayerService(playerRepository, cardRepository, objectMapper, passwordEncoder, emailService,
                cardBackRepository, playerCardRepository);

        when(cardRepository.findByName(anyString())).thenReturn(Collections.emptyList());
        when(playerRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    void register_Success_CreatesPlayer() {
        when(playerRepository.findByUsername("newUser")).thenReturn(Optional.empty());
        when(playerRepository.findByEmail("newuser@test.com")).thenReturn(Optional.empty());

        PlayerRequest request = new PlayerRequest();
        request.setUsername("newUser");
        request.setEmail("newuser@test.com");
        request.setPassword("pass123");
        request.setTrainerName("TrainerNew");
        request.setDateOfBirth(LocalDate.of(2000, 1, 1));

        PlayerResponse response = playerService.register(request);

        assertThat(response).isNotNull();
        assertThat(response.getUsername()).isEqualTo("newUser");
        assertThat(response.getEmail()).isEqualTo("newuser@test.com");
        assertThat(response.getId()).isNotNull();

        verify(playerRepository).save(any(Player.class));
    }

    @Test
    void register_DuplicateUsername_ThrowsException() {
        when(playerRepository.findByUsername("dupeUser")).thenReturn(Optional.of(new Player()));

        PlayerRequest request = new PlayerRequest();
        request.setUsername("dupeUser");
        request.setEmail("dupe@test.com");
        request.setPassword("pass123");

        assertThatThrownBy(() -> playerService.register(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Username already exists");

        verify(playerRepository, never()).save(any());
    }

    @Test
    void register_DuplicateEmail_ThrowsException() {
        when(playerRepository.findByUsername("uniqueUser")).thenReturn(Optional.empty());
        when(playerRepository.findByEmail("dupemail@test.com")).thenReturn(Optional.of(new Player()));

        PlayerRequest request = new PlayerRequest();
        request.setUsername("uniqueUser");
        request.setEmail("dupemail@test.com");
        request.setPassword("pass123");

        assertThatThrownBy(() -> playerService.register(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Email already exists");

        verify(playerRepository, never()).save(any());
    }

    @Test
    void login_Success_ReturnsPlayer() {
        Player player = new Player();
        player.setId("player1");
        player.setUsername("loginUser");
        // store encoded password so passwordEncoder.matches(...) returns true
        player.setPassword(passwordEncoder.encode("correctPass"));
        player.setEmail("login@test.com");
        player.setDecks(new ArrayList<>());
        player.setCollection(new ArrayList<>());

        when(playerRepository.findByUsername("loginUser")).thenReturn(Optional.of(player));

        LoginRequest request = new LoginRequest();
        request.setUsername("loginUser");
        request.setPassword("correctPass");

        PlayerResponse response = playerService.login(request);

        assertThat(response).isNotNull();
        assertThat(response.getUsername()).isEqualTo("loginUser");
    }

    @Test
    void login_WrongPassword_ThrowsException() {
        Player player = new Player();
        player.setId("player2");
        player.setUsername("loginWrong");
        // store encoded password for the expected correct password
        player.setPassword(passwordEncoder.encode("correctPass"));
        player.setEmail("loginwrong@test.com");

        when(playerRepository.findByUsername("loginWrong")).thenReturn(Optional.of(player));

        LoginRequest request = new LoginRequest();
        request.setUsername("loginWrong");
        request.setPassword("wrongPass");

        assertThatThrownBy(() -> playerService.login(request))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Invalid username or password");
    }

    @Test
    void login_NonExistingUser_ThrowsException() {
        when(playerRepository.findByUsername("nobody")).thenReturn(Optional.empty());

        LoginRequest request = new LoginRequest();
        request.setUsername("nobody");
        request.setPassword("anyPass");

        assertThatThrownBy(() -> playerService.login(request))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Invalid username or password");
    }

    @Test
    void createSession_ReturnsSessionId() {
        String sessionId = playerService.createSession("player123");
        assertThat(sessionId).isNotNull();
    }

    @Test
    void getPlayerBySession_ExistingSession_ReturnsPlayer() {
        String playerId = "player456";
        String sessionId = playerService.createSession(playerId);

        Player player = new Player();
        player.setId(playerId);
        player.setUsername("sessionUser");

        when(playerRepository.findById(playerId)).thenReturn(Optional.of(player));

        Optional<Player> result = playerService.getPlayerBySession(sessionId);

        assertThat(result).isPresent();
        assertThat(result.get().getId()).isEqualTo(playerId);
    }

    @Test
    void getPlayerBySession_InvalidSession_ReturnsEmpty() {
        Optional<Player> result = playerService.getPlayerBySession("invalid-session-id");

        assertThat(result).isEmpty();
    }

    @Test
    void removeSession_RemovesExistingSession() {
        String playerId = "player-remove-session";
        String sessionId = playerService.createSession(playerId);

        playerService.removeSession(sessionId);

        Optional<Player> result = playerService.getPlayerBySession(sessionId);
        assertThat(result).isEmpty();
    }

    @Test
    void removeSession_NonExistingSession_DoesNothing() {
        playerService.removeSession("non-existing-session-id");
    }

    @Test
    void updateTrainerName_Success_UpdatesName() {
        String playerId = "player789";
        Player player = new Player();
        player.setId(playerId);
        player.setUsername("updateUser");
        player.setTrainerName("OldName");
        player.setDecks(new ArrayList<>());
        player.setCollection(new ArrayList<>());

        when(playerRepository.findById(playerId)).thenReturn(Optional.of(player));

        UpdateTrainerNameRequest request = new UpdateTrainerNameRequest();
        request.setTrainerName("NewTrainerName");

        PlayerResponse response = playerService.updateTrainerName(playerId, request);

        assertThat(response.getTrainerName()).isEqualTo("NewTrainerName");
        verify(playerRepository).save(player);
    }

    @Test
    void updateTrainerName_PlayerNotFound_ThrowsException() {
        when(playerRepository.findById("nonexistent")).thenReturn(Optional.empty());

        UpdateTrainerNameRequest request = new UpdateTrainerNameRequest();
        request.setTrainerName("AnyName");

        assertThatThrownBy(() -> playerService.updateTrainerName("nonexistent", request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Player not found");
    }

    @Test
    void getPlayerById_ExistingId_ReturnsPlayer() {
        String playerId = "player101";
        Player player = new Player();
        player.setId(playerId);
        player.setUsername("getById");
        player.setDecks(new ArrayList<>());
        player.setCollection(new ArrayList<>());

        when(playerRepository.findById(playerId)).thenReturn(Optional.of(player));

        PlayerResponse response = playerService.getPlayerById(playerId);

        assertThat(response).isNotNull();
        assertThat(response.getId()).isEqualTo(playerId);
    }

    @Test
    void getPlayerById_NonExistingId_ThrowsException() {
        when(playerRepository.findById("unknown")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> playerService.getPlayerById("unknown"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Player not found");
    }



    // ───────── setActiveDeck ─────────

    @Test
    void setActiveDeck_SetsActiveDeckId() {
        String playerId = "p-active";
        Deck deck = createValidDeck("d1");
        Player player = new Player();
        player.setId(playerId);
        player.setCollection(new ArrayList<>(deck.getCards()));
        player.setDecks(new ArrayList<>(List.of(deck)));

        when(playerRepository.findById(playerId)).thenReturn(Optional.of(player));

        PlayerResponse result = playerService.setActiveDeck(playerId, "d1");

        assertThat(result.getActiveDeckId()).isEqualTo("d1");
        verify(playerRepository).save(player);
    }

    @Test
    void setActiveDeck_ClearsActiveDeckWithNull() {
        String playerId = "p-clear";
        Player player = new Player();
        player.setId(playerId);
        player.setDecks(new ArrayList<>());

        when(playerRepository.findById(playerId)).thenReturn(Optional.of(player));

        playerService.setActiveDeck(playerId, null);

        verify(playerRepository).save(player);
    }

    @Test
    void setActiveDeck_PlayerNotFound_Throws() {
        when(playerRepository.findById("ghost")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> playerService.setActiveDeck("ghost", "d1"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Player not found");
    }

    // ───────── updateCharacter ─────────

    @Test
    void updateCharacter_Success() {
        String playerId = "p-char";
        Player player = new Player();
        player.setId(playerId);
        player.setDecks(new ArrayList<>());
        player.setCollection(new ArrayList<>());

        when(playerRepository.findById(playerId)).thenReturn(Optional.of(player));

        UpdateCharacterRequest request = new UpdateCharacterRequest();
        request.setCharacter("hero");
        request.setColor("red");
        PlayerResponse result = playerService.updateCharacter(playerId, request);

        verify(playerRepository).save(player);
        assertThat(result.getCharacter()).isEqualTo("hero");
        assertThat(result.getColor()).isEqualTo("red");
    }

    @Test
    void updateCharacter_PlayerNotFound_Throws() {
        when(playerRepository.findById("ghost")).thenReturn(Optional.empty());

        UpdateCharacterRequest request = new UpdateCharacterRequest();
        assertThatThrownBy(() -> playerService.updateCharacter("ghost", request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Player not found");
    }

    // ───────── confirmPasswordReset ─────────

    @Test
    void confirmPasswordReset_ValidToken_UpdatesPassword() {
        Player player = new Player();
        player.setId("p-confirm");
        player.setResetToken("valid-token");
        player.setResetTokenExpiry(LocalDateTime.now().plusHours(1));

        when(playerRepository.findByResetToken("valid-token")).thenReturn(Optional.of(player));

        playerService.confirmPasswordReset("valid-token", "newPass123");

        assertThat(player.getResetToken()).isNull();
        assertThat(player.getResetTokenExpiry()).isNull();
        assertThat(passwordEncoder.matches("newPass123", player.getPassword())).isTrue();
        verify(playerRepository).save(player);
    }

    @Test
    void confirmPasswordReset_InvalidToken_Throws400() {
        when(playerRepository.findByResetToken("bad-token")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> playerService.confirmPasswordReset("bad-token", "newPass"))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Token inválido");
    }

    @Test
    void confirmPasswordReset_ExpiredToken_Throws400() {
        Player player = new Player();
        player.setId("p-expired");
        player.setResetToken("expired-token");
        player.setResetTokenExpiry(LocalDateTime.now().minusHours(1));

        when(playerRepository.findByResetToken("expired-token")).thenReturn(Optional.of(player));

        assertThatThrownBy(() -> playerService.confirmPasswordReset("expired-token", "newPass"))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Token inválido");
    }

    @Test
    void confirmPasswordReset_NullExpiry_Throws400() {
        Player player = new Player();
        player.setId("p-null-exp");
        player.setResetToken("token-no-expiry");
        player.setResetTokenExpiry(null);

        when(playerRepository.findByResetToken("token-no-expiry")).thenReturn(Optional.of(player));

        assertThatThrownBy(() -> playerService.confirmPasswordReset("token-no-expiry", "newPass"))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Token inválido");
    }

    // ───────── createInitialDecks error paths ─────────

    @Test
    void register_CreatesPlayerWithoutDecks() {
        when(playerRepository.findByUsername("noDecksUser")).thenReturn(Optional.empty());
        when(playerRepository.findByEmail("nodecks@test.com")).thenReturn(Optional.empty());

        PlayerRequest request = new PlayerRequest();
        request.setUsername("noDecksUser");
        request.setEmail("nodecks@test.com");
        request.setPassword("pass123");
        request.setTrainerName("NoDecks");
        request.setDateOfBirth(LocalDate.of(2000, 1, 1));

        PlayerResponse response = playerService.register(request);

        assertThat(response).isNotNull();
        assertThat(response.getUsername()).isEqualTo("noDecksUser");
        assertThat(response.getDecks()).isEmpty();
        verify(playerRepository).save(any(Player.class));
    }

    // ───────── requestPasswordReset ─────────

    @Test
    void requestPasswordReset_PlayerExists_SavesTokenAndSendsEmailAfterCommit() {
        Player player = new Player();
        player.setId("p-reset");
        player.setEmail("reset@test.com");
        when(playerRepository.findByEmail("reset@test.com")).thenReturn(Optional.of(player));

        TransactionSynchronizationManager.initSynchronization();
        try {
            playerService.requestPasswordReset("reset@test.com");

            assertThat(player.getResetToken()).isNotNull();
            assertThat(player.getResetTokenExpiry()).isAfter(LocalDateTime.now());

            // Fire the after-commit hooks that the service registered.
            for (TransactionSynchronization sync : TransactionSynchronizationManager.getSynchronizations()) {
                sync.afterCommit();
            }
        } finally {
            TransactionSynchronizationManager.clearSynchronization();
        }

        verify(playerRepository).save(player);
        verify(emailService).sendPasswordResetEmail(anyString(), anyString());
    }

    @Test
    void requestPasswordReset_PlayerNotFound_DoesNothing() {
        when(playerRepository.findByEmail("nobody@test.com")).thenReturn(Optional.empty());

        playerService.requestPasswordReset("nobody@test.com");

        verify(playerRepository, never()).save(any());
        verify(emailService, never()).sendPasswordResetEmail(anyString(), anyString());
    }

    // ───────── cleanupStaleSessions ─────────

    @Test
    void cleanupStaleSessions_RemovesSessionsForMissingPlayers() {
        String aliveSession = playerService.createSession("alive");
        String staleSession = playerService.createSession("stale");
        when(playerRepository.findById("alive")).thenReturn(Optional.of(new Player()));
        when(playerRepository.findById("stale")).thenReturn(Optional.empty());

        playerService.cleanupStaleSessions();

        assertThat(playerService.getPlayerBySession(staleSession)).isEmpty();
        assertThat(playerService.getPlayerBySession(aliveSession)).isPresent();
    }

    // ───────── toResponse activeCardBack ─────────

    @Test
    void getPlayerById_WithActiveCardBack_SetsCardBackUrl() {
        Player player = new Player();
        player.setId("p-back");
        player.setDecks(new ArrayList<>());
        player.setCollection(new ArrayList<>());
        player.setActiveCardBackId("cb1");

        CardBack back = mock(CardBack.class);
        when(back.getImageUrl()).thenReturn("back-url");
        when(cardBackRepository.findById("cb1")).thenReturn(Optional.of(back));
        when(playerRepository.findById("p-back")).thenReturn(Optional.of(player));

        PlayerResponse response = playerService.getPlayerById("p-back");

        assertThat(response.getActiveCardBackId()).isEqualTo("cb1");
        assertThat(response.getActiveCardBackUrl()).isEqualTo("back-url");
    }

    // ───────── claimStarterDeck ─────────

    @Test
    void claimStarterDeck_ValidName_GrantsDeckAndCards() {
        Player player = new Player();
        player.setId("p-starter");
        player.setCoins(1000);
        player.setCollection(new ArrayList<>());
        player.setDecks(new ArrayList<>());
        when(playerRepository.findById("p-starter")).thenReturn(Optional.of(player));
        Card card = mockCard("xy1-1", "Bulbasaur", "img.jpg");
        when(cardRepository.findByName(anyString())).thenReturn(List.of(card));
        when(playerCardRepository.findByPlayer_IdAndCard_Id(anyString(), anyString()))
                .thenReturn(Optional.empty());

        PlayerResponse response = playerService.claimStarterDeck("p-starter", "Chespin Deck");

        assertThat(response.getDecks()).isNotEmpty();
        assertThat(response.getDecks().get(0).getName()).isEqualTo("Chespin Deck");
        assertThat(response.getCollectionCardIds()).isNotEmpty();
    }

    @Test
    void claimStarterDeck_AlreadyHasDecks_ThrowsConflict() {
        Player player = new Player();
        player.setId("p-starter");
        player.setDecks(new ArrayList<>(List.of(new Deck("d1", "Old", new ArrayList<>()))));
        when(playerRepository.findById("p-starter")).thenReturn(Optional.of(player));

        assertThatThrownBy(() -> playerService.claimStarterDeck("p-starter", "Chespin Deck"))
                .isInstanceOf(ResponseStatusException.class)
                .extracting(e -> ((ResponseStatusException) e).getStatusCode())
                .isEqualTo(HttpStatus.CONFLICT);
    }

    @Test
    void claimStarterDeck_InvalidName_Throws() {
        Player player = new Player();
        player.setId("p-starter");
        player.setDecks(new ArrayList<>());
        when(playerRepository.findById("p-starter")).thenReturn(Optional.of(player));

        assertThatThrownBy(() -> playerService.claimStarterDeck("p-starter", "Invalid Deck"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Invalid deck name");
    }

    // ───────── googleLogin ─────────

    private GoogleIdToken googleTokenWith(String email, String name) {
        GoogleIdToken.Payload payload = new GoogleIdToken.Payload();
        payload.setEmail(email);
        if (name != null) {
            payload.set("name", name);
        }
        GoogleIdToken idToken = mock(GoogleIdToken.class);
        when(idToken.getPayload()).thenReturn(payload);
        return idToken;
    }

    @Test
    void googleLogin_ExistingPlayer_ReturnsExistingWithoutSaving() {
        GoogleIdToken idToken = googleTokenWith("g@test.com", "Google User");
        Player existing = new Player();
        existing.setId("g-existing");
        existing.setUsername("gexisting");
        existing.setEmail("g@test.com");
        existing.setDecks(new ArrayList<>());
        existing.setCollection(new ArrayList<>());
        when(playerRepository.findByEmail("g@test.com")).thenReturn(Optional.of(existing));

        try (MockedConstruction<GoogleIdTokenVerifier> mc = mockConstruction(GoogleIdTokenVerifier.class,
                (verifier, ctx) -> when(verifier.verify(anyString())).thenReturn(idToken))) {
            PlayerResponse response = playerService.googleLogin("any-credential");

            assertThat(response.getEmail()).isEqualTo("g@test.com");
            assertThat(response.getUsername()).isEqualTo("gexisting");
        }
        verify(playerRepository, never()).save(any());
    }

    @Test
    void googleLogin_NewPlayer_CreatesWithDedupedUsername() {
        GoogleIdToken idToken = googleTokenWith("g@test.com", "Google User");
        when(playerRepository.findByEmail("g@test.com")).thenReturn(Optional.empty());
        when(playerRepository.findByUsername("g")).thenReturn(Optional.of(new Player()));
        when(playerRepository.findByUsername("g1")).thenReturn(Optional.empty());

        try (MockedConstruction<GoogleIdTokenVerifier> mc = mockConstruction(GoogleIdTokenVerifier.class,
                (verifier, ctx) -> when(verifier.verify(anyString())).thenReturn(idToken))) {
            PlayerResponse response = playerService.googleLogin("any-credential");

            assertThat(response.getUsername()).isEqualTo("g1");
            assertThat(response.getTrainerName()).isEqualTo("Google User");
            assertThat(response.getEmail()).isEqualTo("g@test.com");
        }
        verify(playerRepository).save(any(Player.class));
    }

    @Test
    void googleLogin_NewPlayerNoName_UsesUsernameAsTrainerName() {
        GoogleIdToken idToken = googleTokenWith("solo@test.com", null);
        when(playerRepository.findByEmail("solo@test.com")).thenReturn(Optional.empty());
        when(playerRepository.findByUsername("solo")).thenReturn(Optional.empty());

        try (MockedConstruction<GoogleIdTokenVerifier> mc = mockConstruction(GoogleIdTokenVerifier.class,
                (verifier, ctx) -> when(verifier.verify(anyString())).thenReturn(idToken))) {
            PlayerResponse response = playerService.googleLogin("any-credential");

            assertThat(response.getUsername()).isEqualTo("solo");
            assertThat(response.getTrainerName()).isEqualTo("solo");
        }
    }

    @Test
    void googleLogin_NullToken_ThrowsUnauthorized() {
        try (MockedConstruction<GoogleIdTokenVerifier> mc = mockConstruction(GoogleIdTokenVerifier.class,
                (verifier, ctx) -> when(verifier.verify(anyString())).thenReturn(null))) {
            assertThatThrownBy(() -> playerService.googleLogin("bad"))
                    .isInstanceOf(ResponseStatusException.class)
                    .hasMessageContaining("Token de Google inválido");
        }
    }

    @Test
    void googleLogin_VerifyThrows_ThrowsUnauthorized() {
        try (MockedConstruction<GoogleIdTokenVerifier> mc = mockConstruction(GoogleIdTokenVerifier.class,
                (verifier, ctx) -> when(verifier.verify(anyString())).thenThrow(new RuntimeException("bad token")))) {
            assertThatThrownBy(() -> playerService.googleLogin("bad"))
                    .isInstanceOf(ResponseStatusException.class)
                    .hasMessageContaining("Token de Google inválido");
        }
    }

    private Card mockCard(String id, String name, String imageUrl) {
        Card card = mock(Card.class);
        when(card.getId()).thenReturn(id);
        when(card.getName()).thenReturn(name);
        when(card.getImageUrl()).thenReturn(imageUrl);
        when(card.getImageUrlLarge()).thenReturn(imageUrl);
        when(card.getSupertype()).thenReturn(Supertype.POKEMON);
        return card;
    }

    private Deck createValidDeck(String deckId) {
        List<Card> cards = new ArrayList<>();
        Attack attack = Attack.builder().name("Tackle").baseDamage(10).build();
        for (int i = 0; i < 60; i++) {
            cards.add(PokemonCard.builder()
                    .id(deckId + "-card-" + i)
                    .name("Mon " + i)
                    .hp(60)
                    .stage(PokemonStage.BASIC)
                    .pokemonType(EnergyType.COLORLESS)
                    .attacks(List.of(attack))
                    .abilities(List.of())
                    .weaknesses(List.of())
                    .resistances(List.of())
                    .costToAttack(List.of())
                    .retreatCost(0)
                    .build());
        }
        return new Deck(deckId, "Valid Deck", cards);
    }
}
