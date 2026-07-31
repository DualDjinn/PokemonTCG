package ar.edu.utn.frc.tup.piii.models;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDateTime;

import org.junit.jupiter.api.Test;

/**
 * Unit tests for the {@link Match} lobby entity.
 */
class MatchTest {

    @Test
    void convenienceConstructor_InitializesWaitingStateAndTimestamp() {
        LocalDateTime before = LocalDateTime.now();

        Match match = new Match("match-1", "host-1", "deck-1", "ABC123");

        assertThat(match.getId()).isEqualTo("match-1");
        assertThat(match.getPlayerOneId()).isEqualTo("host-1");
        assertThat(match.getDeckOneId()).isEqualTo("deck-1");
        assertThat(match.getJoinCode()).isEqualTo("ABC123");
        assertThat(match.getStatus()).isEqualTo(MatchStatus.WAITING);
        assertThat(match.getCreatedAt()).isNotNull();
        assertThat(match.getCreatedAt()).isAfterOrEqualTo(before);
        assertThat(match.getPlayerTwoId()).isNull();
        assertThat(match.getDeckTwoId()).isNull();
        assertThat(match.getWinnerId()).isNull();
        assertThat(match.getEngineMatchId()).isNull();
    }

    @Test
    void noArgsConstructor_LeavesFieldsUnset() {
        Match match = new Match();

        assertThat(match.getId()).isNull();
        assertThat(match.getStatus()).isNull();
        assertThat(match.getCreatedAt()).isNull();
    }

    @Test
    void containsPlayer_TrueForHostAndJoiner_FalseForOthersAndNull() {
        Match match = new Match("match-1", "host-1", "deck-1", "ABC123");
        match.setPlayerTwoId("joiner-1");

        assertThat(match.containsPlayer("host-1")).isTrue();
        assertThat(match.containsPlayer("joiner-1")).isTrue();
        assertThat(match.containsPlayer("stranger")).isFalse();
        assertThat(match.containsPlayer(null)).isFalse();
    }

    @Test
    void containsPlayer_WhenJoinerNotSet_OnlyMatchesHost() {
        Match match = new Match("match-1", "host-1", "deck-1", "ABC123");

        assertThat(match.containsPlayer("host-1")).isTrue();
        assertThat(match.containsPlayer("joiner-1")).isFalse();
    }

    private Match createDefault() {
        Match match = new Match();
        match.setId("match-1");
        match.setPlayerOneId("host-1");
        match.setPlayerTwoId("joiner-1");
        match.setDeckOneId("deck-1");
        match.setDeckTwoId("deck-2");
        match.setStatus(MatchStatus.ACTIVE);
        match.setWinnerId("host-1");
        match.setCreatedAt(LocalDateTime.of(2026, 1, 1, 12, 0));
        match.setJoinCode("ABC123");
        match.setEngineMatchId("engine-1");
        return match;
    }

    @Test
    void settersAndGetters_RoundTripEveryField() {
        Match match = createDefault();

        assertThat(match.getId()).isEqualTo("match-1");
        assertThat(match.getPlayerOneId()).isEqualTo("host-1");
        assertThat(match.getPlayerTwoId()).isEqualTo("joiner-1");
        assertThat(match.getDeckOneId()).isEqualTo("deck-1");
        assertThat(match.getDeckTwoId()).isEqualTo("deck-2");
        assertThat(match.getStatus()).isEqualTo(MatchStatus.ACTIVE);
        assertThat(match.getWinnerId()).isEqualTo("host-1");
        assertThat(match.getCreatedAt()).isEqualTo(LocalDateTime.of(2026, 1, 1, 12, 0));
        assertThat(match.getJoinCode()).isEqualTo("ABC123");
        assertThat(match.getEngineMatchId()).isEqualTo("engine-1");
    }

    @Test
    void equalsAndHashCode_SameValues_AreEqual() {
        Match a = createDefault();
        Match b = createDefault();

        assertThat(a).isEqualTo(b);
        assertThat(a.hashCode()).isEqualTo(b.hashCode());
    }

    @Test
    void equals_SameInstance_ReturnsTrue() {
        Match match = createDefault();
        assertThat(match).isEqualTo(match);
    }

    @Test
    void equals_WithNull_ReturnsFalse() {
        assertThat(createDefault()).isNotEqualTo(null);
    }

    @Test
    void equals_WithDifferentType_ReturnsFalse() {
        assertThat(createDefault()).isNotEqualTo("not a match");
    }

    @Test
    void equals_DifferentId_AreNotEqual() {
        Match a = createDefault();
        Match b = createDefault();
        b.setId("match-2");
        assertThat(a).isNotEqualTo(b);
    }

    @Test
    void equals_DifferentStatus_AreNotEqual() {
        Match a = createDefault();
        Match b = createDefault();
        b.setStatus(MatchStatus.FINISHED);
        assertThat(a).isNotEqualTo(b);
    }

    @Test
    void equals_DifferentJoinCode_AreNotEqual() {
        Match a = createDefault();
        Match b = createDefault();
        b.setJoinCode("ZZZ999");
        assertThat(a).isNotEqualTo(b);
    }

    @Test
    void equals_DifferentEngineMatchId_AreNotEqual() {
        Match a = createDefault();
        Match b = createDefault();
        b.setEngineMatchId("engine-2");
        assertThat(a).isNotEqualTo(b);
    }

    @Test
    void hashCode_IsConsistent() {
        Match match = createDefault();
        int hash = match.hashCode();
        assertThat(match.hashCode()).isEqualTo(hash);
    }

    @Test
    void toString_ContainsKeyFields() {
        String str = createDefault().toString();
        assertThat(str).contains("match-1", "host-1", "ACTIVE", "ABC123");
    }
}
