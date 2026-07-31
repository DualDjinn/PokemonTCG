package ar.edu.utn.frc.tup.piii.seeders;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import ar.edu.utn.frc.tup.piii.models.CardBack;
import ar.edu.utn.frc.tup.piii.models.CardBackOrigin;
import ar.edu.utn.frc.tup.piii.repositories.CardBackRepository;

/**
 * Siembra el catálogo de dorsos de carta con las imágenes del bucket
 * {@code back-card} (subidas por {@link MinioCardImageSeeder}).
 *
 * <p>Distribución de orígenes: 2 de tienda, 2 exclusivos de sobres,
 * 1 por batallas ganadas y 1 que todos los jugadores reciben de entrada
 * (Pixel). El dorso clásico por defecto no se siembra: es
 * {@code activeCardBackId == null}.
 */
@Component
@Order(6)
public class CardBackSeeder implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(CardBackSeeder.class);
    private static final String BUCKET = "back-card";

    private final CardBackRepository cardBackRepository;

    public CardBackSeeder(CardBackRepository cardBackRepository) {
        this.cardBackRepository = cardBackRepository;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (cardBackRepository.count() > 0) {
            migratePixelToDefault();
            migrateMissingPrices();
            log.info("Card backs already seeded, skipping.");
            return;
        }
        List<CardBack> backs = List.of(
                back("bio", "Bio", "backCardBio.png", CardBackOrigin.SHOP, 200, null),
                back("urbano", "Urbano", "backCardUrbano.png", CardBackOrigin.SHOP, 800, null),
                back("mundial-2022", "Mundial 2022", "backCardMundial2022.png", CardBackOrigin.SHOP, 1000, null),
                back("tori", "Tori", "backCardTori.png", CardBackOrigin.SHOP, 500, null),
                back("mundial-2026", "Mundial 2026", "backCardMundial2026.png", CardBackOrigin.SHOP, 5000, null),
                back("pixel", "Pixel", "backCardPixel.png", CardBackOrigin.DEFAULT, 100, null)
        );
        cardBackRepository.saveAll(backs);
        log.info("Seeded {} card backs.", backs.size());
    }

    /**
     * El dorso Pixel pasó de desbloquearse por nivel a ser de todos los
     * jugadores; actualiza bases ya sembradas con el origen viejo.
     */
    private void migratePixelToDefault() {
        cardBackRepository.findById("pixel")
                .filter(back -> back.getOrigin() != CardBackOrigin.DEFAULT)
                .ifPresent(back -> {
                    back.setOrigin(CardBackOrigin.DEFAULT);
                    back.setUnlockValue(null);
                    cardBackRepository.save(back);
                    log.info("Migrated 'pixel' card back to DEFAULT origin.");
                });
    }

    /** Agrega precio y/o corrige origen de los dorsos sembrados con valores viejos. */
    private void migrateMissingPrices() {
        java.util.Map<String, Integer> prices = java.util.Map.of(
                "mundial-2022", 1000,
                "tori", 500,
                "mundial-2026", 5000,
                "pixel", 100
        );
        prices.forEach((id, price) ->
                cardBackRepository.findById(id)
                        .filter(back -> back.getPrice() == null)
                        .ifPresent(back -> {
                            back.setPrice(price);
                            cardBackRepository.save(back);
                            log.info("Set price {} for card back '{}'.", price, id);
                        })
        );
        // Corrige origen de los tres reversos que eran PACK/BATTLE
        java.util.List<String> toShop = java.util.List.of("mundial-2022", "tori", "mundial-2026");
        toShop.forEach(id ->
                cardBackRepository.findById(id)
                        .filter(back -> back.getOrigin() != CardBackOrigin.SHOP)
                        .ifPresent(back -> {
                            back.setOrigin(CardBackOrigin.SHOP);
                            back.setUnlockValue(null);
                            cardBackRepository.save(back);
                            log.info("Migrated card back '{}' to SHOP origin.", id);
                        })
        );
    }

    private CardBack back(String id, String name, String file,
                          CardBackOrigin origin, Integer price, Integer unlockValue) {
        return CardBack.builder()
                .id(id)
                .name(name)
                .imageUrl("/media/" + BUCKET + "/" + file)
                .origin(origin)
                .price(price)
                .unlockValue(unlockValue)
                .build();
    }
}
