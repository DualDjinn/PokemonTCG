package ar.edu.utn.frc.tup.piii.configs;

import java.time.LocalDateTime;
import java.util.List;

import ar.edu.utn.frc.tup.piii.models.Match;
import ar.edu.utn.frc.tup.piii.models.MatchStatus;
import ar.edu.utn.frc.tup.piii.repositories.MatchRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link DisconnectScheduler}.
 *
 * <p>Cubre el barrido periódico de partidas con jugador desconectado: forfeit al expirar el
 * timeout (con el oponente correcto como ganador), countdown ticks antes de expirar, y las
 * guardas de null (sin desconexión, oponente inexistente).
 */
@ExtendWith(MockitoExtension.class)
class DisconnectSchedulerTest {

    @Mock
    private MatchRepository matchRepository;

    @Mock
    private SimpMessagingTemplate messagingTemplate;

    @Mock
    private ar.edu.utn.frc.tup.piii.services.PlayerStatsService playerStatsService;

    @InjectMocks
    private DisconnectScheduler scheduler;

    private Match match(String id, String p1, String p2) {
        Match m = new Match();
        m.setId(id);
        m.setPlayerOneId(p1);
        m.setPlayerTwoId(p2);
        m.setStatus(MatchStatus.ACTIVE);
        return m;
    }

    private void returnMatches(Match... matches) {
        when(matchRepository.findByStatusInOrderByCreatedAtDesc(anyList())).thenReturn(List.of(matches));
    }

    @Test
    void noDisconnectedPlayer_SkipsMatch() {
        returnMatches(match("m1", "p1", "p2")); // disconnectedPlayerId == null
        scheduler.checkDisconnectedPlayers();

        verify(matchRepository, never()).save(any());
        verifyNoInteractions(messagingTemplate);
    }

    @Test
    void disconnectedAtNull_SkipsMatch() {
        Match m = match("m1", "p1", "p2");
        m.setDisconnectedPlayerId("p1"); // pero disconnectedAt == null
        returnMatches(m);

        scheduler.checkDisconnectedPlayers();

        verify(matchRepository, never()).save(any());
        verifyNoInteractions(messagingTemplate);
    }

    @Test
    void timeoutExpired_PlayerOneDisconnected_ForfeitsWithPlayerTwoAsWinner() {
        Match m = match("m1", "p1", "p2");
        m.setDisconnectedPlayerId("p1");
        m.setDisconnectedAt(LocalDateTime.now().minusSeconds(200));
        returnMatches(m);

        scheduler.checkDisconnectedPlayers();

        assertThat(m.getStatus()).isEqualTo(MatchStatus.FINISHED);
        assertThat(m.getWinnerId()).isEqualTo("p2");
        assertThat(m.getDisconnectedPlayerId()).isNull();
        assertThat(m.getDisconnectedAt()).isNull();
        verify(matchRepository).save(m);
        verify(messagingTemplate).convertAndSend(eq("/topic/matches/m1/state/p1"), any(Object.class));
        verify(messagingTemplate).convertAndSend(eq("/topic/matches/m1/state/p2"), any(Object.class));
    }

    @Test
    void timeoutExpired_PlayerTwoDisconnected_ForfeitsWithPlayerOneAsWinner() {
        Match m = match("m1", "p1", "p2");
        m.setDisconnectedPlayerId("p2");
        m.setDisconnectedAt(LocalDateTime.now().minusSeconds(150));
        returnMatches(m);

        scheduler.checkDisconnectedPlayers();

        assertThat(m.getStatus()).isEqualTo(MatchStatus.FINISHED);
        assertThat(m.getWinnerId()).isEqualTo("p1");
        verify(matchRepository).save(m);
    }

    @Test
    void timeoutExpired_OpponentIdNull_OnlyNotifiesConnectedSide() {
        Match m = match("m1", "p1", null); // playerTwo ausente
        m.setDisconnectedPlayerId("p1");
        m.setDisconnectedAt(LocalDateTime.now().minusSeconds(200));
        returnMatches(m);

        scheduler.checkDisconnectedPlayers();

        // winner = playerTwoId = null; sólo se notifica a p1 (p2 es null -> se omite)
        assertThat(m.getWinnerId()).isNull();
        verify(messagingTemplate).convertAndSend(eq("/topic/matches/m1/state/p1"), any(Object.class));
        verify(messagingTemplate, never()).convertAndSend(eq("/topic/matches/m1/state/null"), any(Object.class));
    }

    @Test
    void notExpired_SendsCountdownTickToOpponent() {
        Match m = match("m1", "p1", "p2");
        m.setDisconnectedPlayerId("p1");
        m.setDisconnectedAt(LocalDateTime.now().minusSeconds(10));
        returnMatches(m);

        scheduler.checkDisconnectedPlayers();

        verify(matchRepository, never()).save(any());
        verify(messagingTemplate).convertAndSend(eq("/topic/matches/m1/state/p2"), any(Object.class));
    }

    @Test
    void timeoutExpired_PlayerOneIdNull_SkipsPlayerOneNotification() {
        Match m = match("m1", null, "p2"); // host ausente; p2 es quien sigue
        m.setDisconnectedPlayerId("p2");
        m.setDisconnectedAt(LocalDateTime.now().minusSeconds(200));
        returnMatches(m);

        scheduler.checkDisconnectedPlayers();

        // winner = playerOneId = null; sólo se notifica a p2 (p1Id null -> se omite)
        assertThat(m.getStatus()).isEqualTo(MatchStatus.FINISHED);
        assertThat(m.getWinnerId()).isNull();
        verify(matchRepository).save(m);
        verify(messagingTemplate).convertAndSend(eq("/topic/matches/m1/state/p2"), any(Object.class));
        verify(messagingTemplate, never()).convertAndSend(eq("/topic/matches/m1/state/null"), any(Object.class));
    }

    @Test
    void notExpired_PlayerTwoDisconnected_TicksPlayerOne() {
        Match m = match("m1", "p1", "p2");
        m.setDisconnectedPlayerId("p2"); // oponente del desconectado = playerOne
        m.setDisconnectedAt(LocalDateTime.now().minusSeconds(10));
        returnMatches(m);

        scheduler.checkDisconnectedPlayers();

        verify(matchRepository, never()).save(any());
        verify(messagingTemplate).convertAndSend(eq("/topic/matches/m1/state/p1"), any(Object.class));
    }

    @Test
    void notExpired_OpponentNull_DoesNotSendTick() {
        Match m = match("m1", "p1", null);
        m.setDisconnectedPlayerId("p1"); // oponente = playerTwoId = null
        m.setDisconnectedAt(LocalDateTime.now().minusSeconds(10));
        returnMatches(m);

        scheduler.checkDisconnectedPlayers();

        verify(matchRepository, never()).save(any());
        verifyNoInteractions(messagingTemplate);
    }
}
