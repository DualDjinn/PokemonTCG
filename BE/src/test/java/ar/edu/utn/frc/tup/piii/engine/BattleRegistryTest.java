package ar.edu.utn.frc.tup.piii.engine;

import ar.edu.utn.frc.tup.piii.models.Player;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class BattleRegistryTest {

    private BattleRegistry battleRegistry;
    private Player playerOne;
    private Player playerTwo;

    @BeforeEach
    void setUp() {
        battleRegistry = new BattleRegistry();
        playerOne = new Player();
        playerOne.setId("p1");
        playerOne.setUsername("ash");

        playerTwo = new Player();
        playerTwo.setId("p2");
        playerTwo.setUsername("misty");
    }

    @Test
    void create_ReturnsNewBattleWithPlayers() {
        Battle match = battleRegistry.create(playerOne, playerTwo);

        assertThat(match).isNotNull();
        assertThat(match.getId()).isNotNull();
        assertThat(match.getPlayerOne()).isEqualTo(playerOne);
        assertThat(match.getPlayerTwo()).isEqualTo(playerTwo);
    }

    @Test
    void saveAndGetBattleOrThrow_StoresAndRetrievesBattle() {
        Battle match = battleRegistry.create(playerOne, playerTwo);
        battleRegistry.save(match);

        Battle retrieved = battleRegistry.getBattleOrThrow(match.getId());
        assertThat(retrieved).isSameAs(match);
    }

    @Test
    void getBattleOrThrow_NonExistingBattle_ThrowsException() {
        assertThatThrownBy(() -> battleRegistry.getBattleOrThrow("nonexistent"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Battle not found");
    }

    @Test
    void getBattleOrThrow_NullId_ThrowsException() {
        assertThatThrownBy(() -> battleRegistry.getBattleOrThrow(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("must not be blank");
    }

    @Test
    void getBattleOrThrow_BlankId_ThrowsException() {
        assertThatThrownBy(() -> battleRegistry.getBattleOrThrow("   "))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("must not be blank");
    }

    @Test
    void save_NullBattle_ThrowsNullPointerException() {
        assertThatThrownBy(() -> battleRegistry.save(null))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("must not be null");
    }

    @Test
    void create_GeneratesUniqueIds() {
        Battle match1 = battleRegistry.create(playerOne, playerTwo);
        Battle match2 = battleRegistry.create(playerOne, playerTwo);

        assertThat(match1.getId()).isNotEqualTo(match2.getId());
    }
}
