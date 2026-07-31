package ar.edu.utn.frc.tup.piii.models;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class SupporterCardTest {

    @Test
    void getSubtype_ReturnsSupporter() {
        SupporterCard card = SupporterCard.builder().id("sup-1").name("Shauna").build();
        assertThat(card.getSubtype()).isEqualTo("Supporter");
    }

    @Test
    void getPlayRule_ReturnsSupporterRule() {
        SupporterCard card = SupporterCard.builder().id("sup-1").name("Shauna").build();
        assertThat(card.getPlayRule()).contains("only 1 Supporter card");
    }

    private SupporterCard supporter() {
        return SupporterCard.builder().id("sup-1").name("Shauna").build();
    }

    @Test
    void canBePlayed_OutsideMainPhase_ReturnsFalse() {
        GameContext ctx = new GameContext();
        ctx.setCurrentPhase(TurnPhase.ATTACK);
        assertThat(supporter().canBePlayed(ctx)).isFalse();
    }

    @Test
    void canBePlayed_DuringMainPhaseWithNoSupporterPlayed_ReturnsTrue() {
        GameContext ctx = new GameContext(); // MAIN, sin supporters jugados
        assertThat(supporter().canBePlayed(ctx)).isTrue();
    }

    @Test
    void canBePlayed_WhenSupportersBlocked_ReturnsFalse() {
        GameContext ctx = new GameContext();
        ctx.setAttackerCantPlaySupporters(true);
        assertThat(supporter().canBePlayed(ctx)).isFalse();
    }

    @Test
    void canBePlayed_WhenSupporterAlreadyPlayedThisTurn_ReturnsFalse() {
        GameContext ctx = new GameContext();
        ctx.setAttackerSupportersPlayedThisTurn(1);
        assertThat(supporter().canBePlayed(ctx)).isFalse();
    }
}
