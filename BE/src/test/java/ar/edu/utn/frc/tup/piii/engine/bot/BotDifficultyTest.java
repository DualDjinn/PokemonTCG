package ar.edu.utn.frc.tup.piii.engine.bot;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class BotDifficultyTest {

    @Test
    void fromStringParsesValuesCaseInsensitive() {
        assertEquals(BotDifficulty.EASY, BotDifficulty.fromString("easy"));
        assertEquals(BotDifficulty.MEDIUM, BotDifficulty.fromString("MEDIUM"));
        assertEquals(BotDifficulty.HARD, BotDifficulty.fromString("Hard"));
    }

    @Test
    void fromStringDefaultsToEasyWhenInvalid() {
        assertEquals(BotDifficulty.EASY, BotDifficulty.fromString(null));
        assertEquals(BotDifficulty.EASY, BotDifficulty.fromString(""));
        assertEquals(BotDifficulty.EASY, BotDifficulty.fromString("ultra-impossible"));
    }

    @Test
    void botPlayerIdFollowsConvention() {
        assertEquals("bot-easy", BotDifficulty.EASY.botPlayerId());
        assertEquals("bot-medium", BotDifficulty.MEDIUM.botPlayerId());
        assertEquals("bot-hard", BotDifficulty.HARD.botPlayerId());
    }
}
