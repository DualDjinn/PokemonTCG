package ar.edu.utn.frc.tup.piii.services;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import ar.edu.utn.frc.tup.piii.dtos.player.CardSummaryDTO;
import ar.edu.utn.frc.tup.piii.dtos.quest.NpcRewardClaimResponse;
import ar.edu.utn.frc.tup.piii.models.Player;
import ar.edu.utn.frc.tup.piii.models.PlayerNpcReward;
import ar.edu.utn.frc.tup.piii.repositories.PlayerNpcRewardRepository;

/**
 * Sidequests de NPCs: cartas secretas que un NPC regala la primera vez
 * que el jugador habla con él.
 *
 * <p>El mapeo NPC→carta vive acá (lado servidor) para que el cliente no
 * pueda reclamar cartas arbitrarias; el front solo indica con qué NPC
 * habló. El reclamo es idempotente gracias al registro
 * {@link PlayerNpcReward} con unicidad (jugador, NPC).
 */
@Service
public class QuestService {

    /**
     * NPCs que entregan una carta secreta. La clave es el nombre del NPC tal
     * como figura en la configuración de mapas del frontend
     * ({@code locations.config.ts}); ambos lados deben mantenerse en sync.
     */
    private static final Map<String, String> NPC_REWARDS = Map.of(
            "Don Alfajor", "pokesecret-8",
            "Santoro", "pokesecret-3",
            "Sofia", "pokesecret-9",
            "Ramiro", "pokesecret-1",
            "Hernan", "pokesecret-2",
            "Juanse", "pokesecret-7",
            "Profesor Oak", "pokesecret-10",
            "Johto", "pokesecret-6",
            "Fabio y Joaco", "pokesecret-4",
            "Fabio y Joaco-2", "pokesecret-5"
    );

    private final PlayerNpcRewardRepository playerNpcRewardRepository;
    private final CollectionService collectionService;

    public QuestService(PlayerNpcRewardRepository playerNpcRewardRepository,
                        CollectionService collectionService) {
        this.playerNpcRewardRepository = playerNpcRewardRepository;
        this.collectionService = collectionService;
    }

    /** Indica si el NPC tiene una recompensa configurada. */
    public boolean hasReward(String npcId) {
        return NPC_REWARDS.containsKey(npcId);
    }

    /** Ids de NPCs cuya recompensa el jugador ya reclamó. */
    public List<String> getClaimedNpcIds(String playerId) {
        return playerNpcRewardRepository.findByPlayer_Id(playerId).stream()
                .map(PlayerNpcReward::getNpcId)
                .toList();
    }

    /**
     * Reclama la recompensa del NPC para el jugador.
     *
     * <p>La primera vez otorga la carta (vía {@link CollectionService}) y
     * registra el reclamo; las siguientes devuelve {@code alreadyClaimed}
     * sin tocar la colección.
     *
     * @throws IllegalArgumentException si el NPC no tiene recompensa configurada
     */
    @Transactional
    public NpcRewardClaimResponse claimReward(Player player, String npcId) {
        String cardId = NPC_REWARDS.get(npcId);
        if (cardId == null) {
            throw new IllegalArgumentException("NPC has no reward: " + npcId);
        }

        if (playerNpcRewardRepository.existsByPlayer_IdAndNpcId(player.getId(), npcId)) {
            return new NpcRewardClaimResponse(npcId, true, null);
        }

        collectionService.addToCollection(player.getId(), cardId);

        PlayerNpcReward reward = new PlayerNpcReward();
        reward.setPlayer(player);
        reward.setNpcId(npcId);
        reward.setCardId(cardId);
        reward.setClaimedAt(LocalDateTime.now());
        playerNpcRewardRepository.save(reward);

        CardSummaryDTO card = collectionService.getCardSummary(player.getId(), cardId);
        return new NpcRewardClaimResponse(npcId, false, card);
    }
}
