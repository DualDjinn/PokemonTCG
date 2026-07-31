package ar.edu.utn.frc.tup.piii.services;

import java.io.InputStream;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;

import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Data;

import ar.edu.utn.frc.tup.piii.dtos.player.LoginRequest;
import ar.edu.utn.frc.tup.piii.dtos.player.PlayerRequest;
import ar.edu.utn.frc.tup.piii.dtos.player.PlayerResponse;
import ar.edu.utn.frc.tup.piii.dtos.player.PlayerResponse.DeckSummary;
import ar.edu.utn.frc.tup.piii.dtos.player.UpdateCharacterRequest;
import ar.edu.utn.frc.tup.piii.dtos.player.UpdateTrainerNameRequest;

import ar.edu.utn.frc.tup.piii.models.Card;
import ar.edu.utn.frc.tup.piii.models.Deck;
import ar.edu.utn.frc.tup.piii.models.Player;
import ar.edu.utn.frc.tup.piii.models.PlayerCard;
import ar.edu.utn.frc.tup.piii.repositories.CardBackRepository;
import ar.edu.utn.frc.tup.piii.repositories.CardRepository;
import ar.edu.utn.frc.tup.piii.repositories.PlayerCardRepository;
import ar.edu.utn.frc.tup.piii.repositories.PlayerRepository;

@Service
public class PlayerService {

    @Value("${app.google.client-id:default-client-id}")
    private String googleClientId;

    private final Map<String, String> sessions = new ConcurrentHashMap<>();
    private final PlayerRepository playerRepository;
    private final CardRepository cardRepository;
    private final ObjectMapper objectMapper;
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;
    private final CardBackRepository cardBackRepository;
    private final PlayerCardRepository playerCardRepository;

    public PlayerService(PlayerRepository playerRepository,
                         CardRepository cardRepository,
                         ObjectMapper objectMapper,
                         PasswordEncoder passwordEncoder,
                         EmailService emailService,
                         CardBackRepository cardBackRepository,
                         PlayerCardRepository playerCardRepository) {
        this.playerRepository = playerRepository;
        this.cardRepository = cardRepository;
        this.objectMapper = objectMapper;
        this.passwordEncoder = passwordEncoder;
        this.emailService = emailService;
        this.cardBackRepository = cardBackRepository;
        this.playerCardRepository = playerCardRepository;
    }

    @Transactional
    public PlayerResponse register(PlayerRequest request) {
        if (playerRepository.findByUsername(request.getUsername()).isPresent()) {
            throw new IllegalArgumentException("Username already exists");
        }
        if (playerRepository.findByEmail(request.getEmail()).isPresent()) {
            throw new IllegalArgumentException("Email already exists");
        }

        // Hash password with BCrypt before storing. This is one-way hashing.
        String encodedPassword = passwordEncoder.encode(request.getPassword());

        Player player = new Player(
                UUID.randomUUID().toString(),
                request.getUsername(),
                request.getTrainerName(),
                request.getEmail(),
                encodedPassword, // store encoded password
                request.getDateOfBirth(),
                0,
                1000,
                new ArrayList<>(),
                new ArrayList<>());

        playerRepository.save(player);
        return toResponse(player);
    }

    public PlayerResponse login(LoginRequest request) {
        // Si el username no existe, lanza 401 sin revelar si el usuario esta o no registrado.
        Player player = playerRepository.findByUsername(request.getUsername())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid username or password"));
        // BCrypt re-hashea la contrasena ingresada y la compara con el hash guardado.
        // Nunca descifra: si los hashes no coinciden, lanza 401.
        if (!passwordEncoder.matches(request.getPassword(), player.getPassword())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid username or password");
        }
        return toResponse(player);
    }

    private static final Logger log = LoggerFactory.getLogger(PlayerService.class);

    public String createSession(String playerId) {
        // Invalidar cualquier sesión anterior del mismo jugador
        sessions.entrySet().removeIf(e -> e.getValue().equals(playerId));
        String sessionId = UUID.randomUUID().toString();
        sessions.put(sessionId, playerId);
        return sessionId;
    }

    public Optional<Player> getPlayerBySession(String sessionId) {
        String playerId = sessions.get(sessionId);
        if (playerId == null) return Optional.empty();
        return playerRepository.findById(playerId);
    }

    public void removeSession(String sessionId) {
        sessions.remove(sessionId);
    }

    /**
     * Limpia sesiones que queden huerfanas cada 1 hora.
     */
    @Scheduled(fixedRate = 3600000)
    public void cleanupStaleSessions() {
        int before = sessions.size();
        // Elimina sesiones cuyo playerId ya no existe en la BD
        sessions.values().removeIf(pid -> playerRepository.findById(pid).isEmpty());
        int removed = before - sessions.size();
        if (removed > 0) {
            log.info("Cleaned up {} stale player sessions", removed);
        }
    }

    @Transactional
    public PlayerResponse updateTrainerName(String playerId, UpdateTrainerNameRequest request) {
        Player player = getPlayerOrThrow(playerId);
        player.setTrainerName(request.getTrainerName());
        playerRepository.save(player);
        return toResponse(player);
    }

    @Transactional
    public PlayerResponse updateCharacter(String playerId, UpdateCharacterRequest request) {
        Player player = getPlayerOrThrow(playerId);
        player.setCharacter(request.getCharacter());
        player.setColor(request.getColor());
        playerRepository.save(player);
        return toResponse(player);
    }

    public PlayerResponse getPlayerById(String playerId) {
        Player player = getPlayerOrThrow(playerId);
        return toResponse(player);
    }

    @Transactional
    public PlayerResponse setActiveDeck(String playerId, String deckId) {
        Player player = getPlayerOrThrow(playerId);
        if (deckId != null && !deckId.isEmpty()) {
            Deck deck = player.getDecks().stream()
                    .filter(d -> d.getId().equals(deckId))
                    .findFirst()
                    .orElseThrow(() -> new IllegalArgumentException("Deck not found"));
            if (!deck.isValid(player, Set.of())) {
                throw new IllegalArgumentException("Cannot select an invalid deck");
            }
        }
        player.setActiveDeckId(deckId);
        playerRepository.save(player);
        return toResponse(player);
    }

    @Transactional
    public PlayerResponse googleLogin(String credential) {
        GoogleIdTokenVerifier verifier = new GoogleIdTokenVerifier.Builder(
                new NetHttpTransport(), GsonFactory.getDefaultInstance())
                .setAudience(Collections.singletonList(googleClientId))
                .build();

        GoogleIdToken idToken;
        try {
            idToken = verifier.verify(credential);
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Token de Google inválido");
        }

        if (idToken == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Token de Google inválido");
        }

        GoogleIdToken.Payload payload = idToken.getPayload();
        String email = payload.getEmail();
        String googleName = (String) payload.get("name");

        Optional<Player> existing = playerRepository.findByEmail(email);
        if (existing.isPresent()) {
            return toResponse(existing.get());
        }

        String baseUsername = email.split("@")[0].replaceAll("[^a-zA-Z0-9_]", "");
        String username = baseUsername;
        int counter = 1;
        while (playerRepository.findByUsername(username).isPresent()) {
            username = baseUsername + counter++;
        }

        String trainerName = (googleName != null && !googleName.isBlank()) ? googleName : username;
        String randomPassword = passwordEncoder.encode(UUID.randomUUID().toString());

        Player player = new Player(
                UUID.randomUUID().toString(),
                username,
                trainerName,
                email,
                randomPassword,
                null,
                0,
                1000,
                new ArrayList<>(),
                new ArrayList<>());

        playerRepository.save(player);
        return toResponse(player);
    }

    /**
     * Otorga el mazo inicial elegido por el jugador tras el tutorial.
     * Solo puede reclamarse una vez: si el jugador ya tiene mazos, rechaza.
     */
    @Transactional
    public PlayerResponse claimStarterDeck(String playerId, String deckName) {
        Player player = playerRepository.findById(playerId)
                .orElseThrow(() -> new IllegalArgumentException("Player not found"));

        if (player.getDecks() != null && !player.getDecks().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Starter deck already claimed");
        }

        List<Deck> allDecks = createInitialDecks();
        Deck chosen = allDecks.stream()
                .filter(d -> d.getName().equals(deckName))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Invalid deck name: " + deckName));

        player.addDeck(chosen);
        for (Card card : chosen.getCards()) {
            if (!player.getCollection().contains(card)) {
                player.addToCollection(card);
            }
        }
        playerRepository.save(player);

        for (Card card : chosen.getCards()) {
            PlayerCard pc = playerCardRepository
                    .findByPlayer_IdAndCard_Id(playerId, card.getId())
                    .orElseGet(() -> {
                        PlayerCard fresh = new PlayerCard();
                        fresh.setPlayer(player);
                        fresh.setCard(card);
                        fresh.setQuantity(0);
                        return fresh;
                    });
            pc.setQuantity(pc.getQuantity() + 1);
            playerCardRepository.save(pc);
        }

        return toResponse(player);
    }

    // La anotación @Transactional garantiza que el guardado del token y el envío del email sean consistentes
    @Transactional
    public void requestPasswordReset(String email) {
        // Busca el jugador por email; si no existe, no hace nada para no revelar si el email está registrado
        playerRepository.findByEmail(email).ifPresent(player -> {
            // Genera un token UUID aleatorio e impredecible para identificar esta solicitud de recuperación
            String token = UUID.randomUUID().toString();
            // Almacena el token en el jugador para poder validarlo cuando llegue el formulario de reset
            player.setResetToken(token);
            // El token expira en 1 hora desde su creación para limitar la ventana de uso fraudulento
            player.setResetTokenExpiry(LocalDateTime.now().plusHours(1));
            // Persiste el token y su expiración en la base de datos antes de enviar el email
            playerRepository.save(player);
            // Registra un hook de sincronización para ejecutar código DESPUÉS del commit de la transacción
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                // Se ejecuta solo si la transacción se confirmó exitosamente (el token ya está guardado en BD)
                public void afterCommit() {
                    // Envía el email con el enlace de recuperación que incluye el token generado
                    emailService.sendPasswordResetEmail(email, token);
                }
            });
        });
    }

    // @Transactional asegura que el cambio de contraseña y la limpieza del token sean atómicos
    @Transactional
    public void confirmPasswordReset(String token, String newPassword) {
        // Busca el jugador que tiene exactamente ese token de restablecimiento en la base de datos
        Player player = playerRepository.findByResetToken(token)
                // Si el token no existe en ningún jugador, lanza error 400 con mensaje genérico
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Token inválido o expirado"));
        // Verifica que la fecha de expiración del token no haya pasado
        if (player.getResetTokenExpiry() == null || player.getResetTokenExpiry().isBefore(LocalDateTime.now())) {
            // Si el token expiró, lanza error 400; usa el mismo mensaje para no dar pistas de ataque
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Token inválido o expirado");
        }
        // Hashea la nueva contraseña con BCrypt antes de guardarla; nunca se guarda en texto plano
        player.setPassword(passwordEncoder.encode(newPassword));
        // Invalida el token para que no pueda reutilizarse en un segundo intento de restablecimiento
        player.setResetToken(null);
        // Limpia la fecha de expiración ya que el token fue consumido exitosamente
        player.setResetTokenExpiry(null);
        // Persiste la nueva contraseña y el token nulo en la base de datos
        playerRepository.save(player);
    }

    private Player getPlayerOrThrow(String playerId) {
        return playerRepository.findById(playerId)
                .orElseThrow(() -> new IllegalArgumentException("Player not found"));
    }

    private List<Deck> createInitialDecks() {
        List<Deck> decks = new ArrayList<>();
        List<InitialDeckSeed> seed;

        try {
            InputStream is = getClass().getClassLoader()
                    .getResourceAsStream("data/initial-decks.json");
            if (is == null) {
                return decks;
            }
            seed = objectMapper.readValue(is, new TypeReference<List<InitialDeckSeed>>() {});
        } catch (Exception e) {
            return decks;
        }

        for (InitialDeckSeed deckSeed : seed) {
            List<Card> deckCards = new ArrayList<>();
            for (InitialCardSeed cardSeed : deckSeed.getCards()) {
                List<Card> cards = cardRepository.findByName(cardSeed.getName());
                if (!cards.isEmpty()) {
                    for (int i = 0; i < cardSeed.getCount(); i++) {
                        deckCards.add(cards.get(0));
                    }
                }
            }
            Deck deck = new Deck(UUID.randomUUID().toString(), deckSeed.getName(), deckCards);
            decks.add(deck);
        }

        return decks;
    }

    private PlayerResponse toResponse(Player player) {
        PlayerResponse response = new PlayerResponse();
        response.setId(player.getId());
        response.setUsername(player.getUsername());
        response.setTrainerName(player.getTrainerName());
        response.setEmail(player.getEmail());
        response.setDateOfBirth(player.getDateOfBirth());
        response.setLevel(player.getLevel());
        response.setExperience(player.getExperience());
        response.setXpForNextLevel(player.getXpForNextLevel());
        response.setXpIntoCurrentLevel(player.getXpIntoCurrentLevel());
        response.setCoins(player.getCoins());
        response.setActiveDeckId(player.getActiveDeckId());
        response.setActiveCardBackId(player.getActiveCardBackId());
        if (player.getActiveCardBackId() != null) {
            cardBackRepository.findById(player.getActiveCardBackId())
                    .ifPresent(back -> response.setActiveCardBackUrl(back.getImageUrl()));
        }
        response.setCharacter(player.getCharacter());
        response.setColor(player.getColor());

        List<String> collectionCardIds = new ArrayList<>();
        if (player.getCollection() != null) {
            for (Card card : player.getCollection()) {
                collectionCardIds.add(card.getId());
            }
        }
        response.setCollectionCardIds(collectionCardIds);

        Map<String, Integer> quantities = new java.util.HashMap<>();
        List<PlayerCard> playerCards = playerCardRepository.findByPlayer_Id(player.getId());
        for (PlayerCard pc : playerCards) {
            quantities.put(pc.getCard().getId(), pc.getQuantity());
        }
        for (Card card : player.getCollection()) {
            quantities.putIfAbsent(card.getId(), 1);
        }
        response.setCollectionQuantities(quantities);

        List<DeckSummary> summaries = new ArrayList<>();
        if (player.getDecks() != null) {
            for (Deck deck : player.getDecks()) {
                DeckSummary summary = new DeckSummary();
                summary.setId(deck.getId());
                summary.setName(deck.getName());
                summary.setCardCount(deck.getCards() != null ? deck.getCards().size() : 0);
                summaries.add(summary);
            }
        }
        response.setDecks(summaries);

        return response;
    }

    @Data
    private static class InitialDeckSeed {
        private String name;
        private List<InitialCardSeed> cards;
    }

    @Data
    private static class InitialCardSeed {
        private String name;
        private int count;
    }
}
