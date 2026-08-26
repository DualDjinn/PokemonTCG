package ar.edu.utn.frc.tup.piii.services;

import ar.edu.utn.frc.tup.piii.dtos.engine.PlayerStatsResponse;
import ar.edu.utn.frc.tup.piii.models.Deck;
import ar.edu.utn.frc.tup.piii.models.Match;
import ar.edu.utn.frc.tup.piii.models.Player;
import ar.edu.utn.frc.tup.piii.models.PlayerStats;
import ar.edu.utn.frc.tup.piii.repositories.PlayerRepository;
import ar.edu.utn.frc.tup.piii.repositories.PlayerStatsRepository;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Tracks and aggregates per-player match statistics.
 *
 * <p>Called from the game flow (card plays, match finishing) and queried by the
 * profile endpoint.
 */
@Service
public class PlayerStatsService {

    private final PlayerStatsRepository statsRepository;
    private final PlayerRepository playerRepository;
    private final CardRegistryService cardRegistryService;

    public PlayerStatsService(PlayerStatsRepository statsRepository,
                              PlayerRepository playerRepository,
                              CardRegistryService cardRegistryService) {
        this.statsRepository = statsRepository;
        this.playerRepository = playerRepository;
        this.cardRegistryService = cardRegistryService;
    }

    @Transactional
    public void recordCardPlayed(String playerId, String cardId) {
        if (playerId == null || cardId == null) {
            return;
        }
        PlayerStats stats = statsRepository.findById(playerId).orElseGet(() -> new PlayerStats(playerId));
        stats.getCardPlays().merge(cardId, 1L, Long::sum);
        statsRepository.save(stats);
    }

    @Transactional
    public void recordFinishedMatch(Match match) {
        LocalDateTime startedAt = match.getStartedAt() != null ? match.getStartedAt() : match.getCreatedAt();
        long durationMs = startedAt == null
                ? 0L
                : Math.max(0L, Duration.between(startedAt, LocalDateTime.now()).toMillis());
        recordPlayer(match.getPlayerOneId(), match.getDeckOneId(), match.getWinnerId(), durationMs);
        if (match.getPlayerTwoId() != null) {
            recordPlayer(match.getPlayerTwoId(), match.getDeckTwoId(), match.getWinnerId(), durationMs);
        }
    }

    private void recordPlayer(String playerId, String deckId, String winnerId, long durationMs) {
        if (playerId == null) {
            return;
        }
        PlayerStats stats = statsRepository.findById(playerId).orElseGet(() -> new PlayerStats(playerId));
        stats.setMatchesPlayed(stats.getMatchesPlayed() + 1);
        if (winnerId == null) {
            stats.setMatchesTied(stats.getMatchesTied() + 1);
        } else if (winnerId.equals(playerId)) {
            stats.setMatchesWon(stats.getMatchesWon() + 1);
        } else {
            stats.setMatchesLost(stats.getMatchesLost() + 1);
        }
        stats.setTotalMatchTimeMs(stats.getTotalMatchTimeMs() + durationMs);
        if (deckId != null) {
            stats.getDeckUsage().merge(deckId, 1L, Long::sum);
        }
        statsRepository.save(stats);
    }

    @Transactional(readOnly = true)
    public PlayerStatsResponse getStats(String playerId) {
        PlayerStats stats = statsRepository.findById(playerId).orElseGet(() -> new PlayerStats(playerId));
        return new PlayerStatsResponse(
                stats.getMatchesPlayed(),
                stats.getMatchesWon(),
                stats.getMatchesLost(),
                stats.getMatchesTied(),
                stats.getTotalMatchTimeMs(),
                mostPlayedCardName(stats),
                mostUsedDeckName(playerId, stats));
    }

    private String mostPlayedCardName(PlayerStats stats) {
        String bestId = null;
        long best = 0;
        for (Map.Entry<String, Long> e : stats.getCardPlays().entrySet()) {
            if (e.getValue() > best) {
                best = e.getValue();
                bestId = e.getKey();
            }
        }
        if (bestId == null) {
            return null;
        }
        final String resultId = bestId;
        return cardRegistryService.findById(resultId)
                .map(card -> card.getName())
                .orElse(resultId);
    }

    private String mostUsedDeckName(String playerId, PlayerStats stats) {
        String bestId = null;
        long best = 0;
        for (Map.Entry<String, Long> e : stats.getDeckUsage().entrySet()) {
            if (e.getValue() > best) {
                best = e.getValue();
                bestId = e.getKey();
            }
        }
        if (bestId == null) {
            return null;
        }
        final String resultId = bestId;
        return playerRepository.findByIdWithDecks(playerId)
                .map(Player::getDecks)
                .flatMap(decks -> decks.stream()
                        .filter(d -> d.getId().equals(resultId))
                        .map(Deck::getName)
                        .findFirst())
                .orElse(resultId);
    }
}