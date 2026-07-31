package ar.edu.utn.frc.tup.piii.seeders;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import ar.edu.utn.frc.tup.piii.models.PokemonCard;
import ar.edu.utn.frc.tup.piii.repositories.CardRepository;
import ar.edu.utn.frc.tup.piii.repositories.PlayerRepository;
import ar.edu.utn.frc.tup.piii.services.CardMapper;

/**
 * Seeds initial data on application startup.
 *
 * <p>Card seeding is handled by {@link ar.edu.utn.frc.tup.piii.services.CardApiSyncService}.
 * This seeder only handles player coin migration and dev account grants.
 */
@Component
@Order(1)
public class DataSeeder implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(DataSeeder.class);
    private static final String DEV_COINS_EMAIL = "josepanichelli90@gmail.com";
    private static final int DEV_COINS_AMOUNT = 99999;
    private static final int INITIAL_COINS = 1000;

    private final PlayerRepository playerRepository;
    private final CardRepository cardRepository;

    public DataSeeder(PlayerRepository playerRepository, CardRepository cardRepository) {
        this.playerRepository = playerRepository;
        this.cardRepository = cardRepository;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) throws Exception {
        migrateInitialCoins();
        grantDevCoins();
        migrateGifUrls();
    }

    private void migrateInitialCoins() {
        List<ar.edu.utn.frc.tup.piii.models.Player> toUpdate = playerRepository.findAll()
                .stream()
                .filter(p -> !p.isBot() && p.getCoins() == 0)
                .toList();
        if (!toUpdate.isEmpty()) {
            toUpdate.forEach(p -> p.setCoins(INITIAL_COINS));
            playerRepository.saveAll(toUpdate);
            log.info("Migrated {} coins to {} existing player(s) with 0 balance.",
                    INITIAL_COINS, toUpdate.size());
        }
    }

    private void grantDevCoins() {
        playerRepository.findByEmail(DEV_COINS_EMAIL).ifPresent(player -> {
            player.setCoins(player.getCoins() + DEV_COINS_AMOUNT);
            playerRepository.save(player);
            log.info("Granted {} coins to {} (total: {}).",
                    DEV_COINS_AMOUNT, DEV_COINS_EMAIL, player.getCoins());
        });
    }

    private void migrateGifUrls() {
        List<PokemonCard> toUpdate = cardRepository.findAll().stream()
                .filter(PokemonCard.class::isInstance)
                .map(PokemonCard.class::cast)
                .filter(pc -> pc.getGifUrl() == null)
                .toList();
        if (toUpdate.isEmpty()) {
            return;
        }
        for (PokemonCard pc : toUpdate) {
            pc.setGifUrl(CardMapper.resolveGifUrl(pc.getId(), pc.getName()));
        }
        cardRepository.saveAll(toUpdate);
        log.info("Migrated gif_url for {} pokemon card(s).", toUpdate.size());
    }
}
