package ar.edu.utn.frc.tup.piii.services;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import ar.edu.utn.frc.tup.piii.dtos.skin.CardSkinDTO;
import ar.edu.utn.frc.tup.piii.dtos.skin.PlayerSkinDTO;
import ar.edu.utn.frc.tup.piii.models.CardSkin;
import ar.edu.utn.frc.tup.piii.models.PlayerCard;
import ar.edu.utn.frc.tup.piii.models.PlayerSkin;
import ar.edu.utn.frc.tup.piii.repositories.CardSkinRepository;
import ar.edu.utn.frc.tup.piii.repositories.PlayerCardRepository;
import ar.edu.utn.frc.tup.piii.repositories.PlayerSkinRepository;

/**
 * Lógica de negocio para skins de cartas.
 */
@Service
public class SkinService {

    private final CardSkinRepository cardSkinRepository;
    private final PlayerSkinRepository playerSkinRepository;
    private final PlayerCardRepository playerCardRepository;

    public SkinService(CardSkinRepository cardSkinRepository,
                       PlayerSkinRepository playerSkinRepository,
                       PlayerCardRepository playerCardRepository) {
        this.cardSkinRepository = cardSkinRepository;
        this.playerSkinRepository = playerSkinRepository;
        this.playerCardRepository = playerCardRepository;
    }

    /**
     * Catálogo completo de skins disponibles.
     */
    public List<CardSkinDTO> getAllSkins() {
        return cardSkinRepository.findAll().stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    /**
     * Skins disponibles para una carta base.
     */
    public List<CardSkinDTO> getSkinsForCard(String baseCardId) {
        return cardSkinRepository.findByBaseCard_Id(baseCardId).stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    /**
     * Skins que posee un jugador, indicando cuáles están equipadas en su
     * carta base (estado que vive en {@code PlayerCard.activeSkin}).
     */
    public List<PlayerSkinDTO> getPlayerSkins(String playerId) {
        Set<String> equippedSkinIds = playerCardRepository.findByPlayer_Id(playerId).stream()
                .map(PlayerCard::getActiveSkin)
                .filter(Objects::nonNull)
                .map(CardSkin::getId)
                .collect(Collectors.toSet());

        return playerSkinRepository.findByPlayer_Id(playerId).stream()
                .map(ps -> toPlayerDTO(ps, equippedSkinIds.contains(ps.getSkin().getId())))
                .collect(Collectors.toList());
    }

    /**
     * Equipa una skin en una carta del jugador.
     */
    @Transactional
    public void equipSkin(String playerId, String cardId, String skinId) {
        CardSkin skin = cardSkinRepository.findById(skinId)
                .orElseThrow(() -> new IllegalArgumentException("Skin not found: " + skinId));

        if (!skin.getBaseCard().getId().equals(cardId)) {
            throw new IllegalArgumentException(
                    "Skin " + skinId + " does not apply to card " + cardId);
        }

        playerSkinRepository.findByPlayer_IdAndSkin_Id(playerId, skinId)
                .orElseThrow(() -> new IllegalStateException("Player does not own skin: " + skinId));

        PlayerCard playerCard = playerCardRepository
                .findByPlayer_IdAndCard_Id(playerId, cardId)
                .orElseThrow(() -> new IllegalArgumentException("Player does not own card: " + cardId));

        playerCard.setActiveSkin(skin);
        playerCardRepository.save(playerCard);
    }

    /**
     * Quita la skin activa de una carta del jugador.
     */
    @Transactional
    public void unequipSkin(String playerId, String cardId) {
        PlayerCard playerCard = playerCardRepository
                .findByPlayer_IdAndCard_Id(playerId, cardId)
                .orElseThrow(() -> new IllegalArgumentException("Player does not own card: " + cardId));

        playerCard.setActiveSkin(null);
        playerCardRepository.save(playerCard);
    }

    /**
     * Otorga una skin a un jugador (usado por packs, tienda, recompensas).
     */
    @Transactional
    public void grantSkin(String playerId, String skinId, int quantity,
                          ar.edu.utn.frc.tup.piii.models.Player player) {
        CardSkin skin = cardSkinRepository.findById(skinId)
                .orElseThrow(() -> new IllegalArgumentException("Skin not found: " + skinId));

        PlayerSkin ps = playerSkinRepository
                .findByPlayer_IdAndSkin_Id(playerId, skinId)
                .orElseGet(() -> {
                    PlayerSkin fresh = new PlayerSkin();
                    fresh.setPlayer(player);
                    fresh.setSkin(skin);
                    fresh.setQuantity(0);
                    fresh.setAcquiredAt(LocalDateTime.now());
                    return fresh;
                });
        ps.setQuantity(ps.getQuantity() + quantity);
        playerSkinRepository.save(ps);
    }

    private CardSkinDTO toDTO(CardSkin skin) {
        return CardSkinDTO.builder()
                .id(skin.getId())
                .name(skin.getName())
                .baseCardId(skin.getBaseCard().getId())
                .origin(skin.getOrigin().name())
                .imageUrl(skin.getImageUrl())
                .imageUrlLarge(skin.getImageUrlLarge())
                .tag(skin.getSkinTag())
                .build();
    }

    private PlayerSkinDTO toPlayerDTO(PlayerSkin ps, boolean equipped) {
        CardSkin skin = ps.getSkin();
        return PlayerSkinDTO.builder()
                .skinId(skin.getId())
                .skinName(skin.getName())
                .baseCardId(skin.getBaseCard().getId())
                .baseCardName(skin.getBaseCard().getName())
                .origin(skin.getOrigin().name())
                .imageUrl(skin.getImageUrl())
                .imageUrlLarge(skin.getImageUrlLarge())
                .quantity(ps.getQuantity())
                .equipped(equipped)
                .build();
    }
}
