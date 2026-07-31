import { Injectable, inject } from '@angular/core';
import { GameStateService } from './game-state.service';
import { WebSocketService } from './websocket.service';

/**
 * Tipos de acción de juego que el jugador puede enviar al backend.
 * Reflejan los tipos ya usados en `action-panel.component.ts` (END_TURN, RETREAT,
 * etc.), de modo que conectar ese componente a este servicio no requiera traducción.
 */
export type MatchActionType =
  | 'PLAY_BASIC'
  | 'EVOLVE'
  | 'ATTACH_ENERGY'
  | 'PLAY_ITEM'
  | 'PLAY_SUPPORTER'
  | 'PLAY_STADIUM'
  | 'ATTACH_TOOL'
  | 'ATTACK'
  | 'ADVANCE_TO_ATTACK'
  | 'EXECUTE_ATTACK'
  | 'ACTIVATE_ABILITY'
  | 'RESOLVE_PLAYER_CHOICE'
  | 'RETREAT'
  | 'END_TURN';

/**
 * Mensaje que viaja por STOMP hacia `/app/matches/{matchId}/action`.
 * Contiene el tipo de acción y solo los parámetros que esa acción necesita.
 */
export interface GameActionMessage {
  type: MatchActionType;
  cardId?: string;
  targetCardId?: string;
  targetSlot?: number;
  attackName?: string;
  energyCardIds?: string[];
  chosenCardIds?: string[];
}

/**
 * Payload del setup inicial enviado por STOMP hacia `/app/matches/{matchId}/setup`.
 */
export interface SetupMessage {
  activeCardId: string;
  benchCardIds: string[];
}

/**
 * Centraliza el envío de acciones del jugador durante la partida.
 *
 * Todas las acciones de juego viajan por WebSocket (STOMP), no por HTTP REST,
 * y se publican a través del {@link WebSocketService}. Este servicio construye
 * los mensajes con el formato del protocolo para que los componentes del tablero
 * no necesiten conocerlo.
 *
 * Las respuestas del backend no se devuelven de forma sincrónica desde estos
 * métodos: llegan como eventos WebSocket recibidos en {@link WebSocketService}
 * y procesados por {@link GameStateService}.
 */
@Injectable({ providedIn: 'root' })
export class TurnActionService {
  private readonly webSocketService = inject(WebSocketService);
  private readonly gameStateService = inject(GameStateService);

  /** Juega un Pokémon Básico desde la mano al banco. */
  playBasicPokemon(cardId: string, targetSlot: number): void {
    this.sendAction({ type: 'PLAY_BASIC', cardId, targetSlot });
  }

  /** Evoluciona un Pokémon en juego usando una carta de la mano. */
  evolve(cardId: string, targetCardId: string): void {
    this.sendAction({ type: 'EVOLVE', cardId, targetCardId });
  }

  /** Adjunta una energía de la mano a un Pokémon en juego. */
  attachEnergy(energyCardId: string, targetCardId: string): void {
    this.sendAction({ type: 'ATTACH_ENERGY', cardId: energyCardId, targetCardId });
  }

  /** Juega una carta Item desde la mano. */
  playItem(cardId: string): void {
    this.sendAction({ type: 'PLAY_ITEM', cardId });
  }

  /** Juega una carta Supporter desde la mano. */
  playSupporter(cardId: string): void {
    this.sendAction({ type: 'PLAY_SUPPORTER', cardId });
  }

  /** Juega una carta Stadium desde la mano. */
  playStadium(cardId: string): void {
    this.sendAction({ type: 'PLAY_STADIUM', cardId });
  }

  /** Adjunta una herramienta Pokémon a un Pokémon en juego. */
  attachPokemonTool(cardId: string, targetCardId: string): void {
    this.sendAction({ type: 'ATTACH_TOOL', cardId, targetCardId });
  }

  /** Declara el ataque del Pokémon activo. */
  attack(attackName: string): void {
    this.sendAction({ type: 'ATTACK', attackName });
  }

  /** Transiciona de MAIN phase a ATTACK phase. */
  advanceToAttack(): void {
    this.sendAction({ type: 'ADVANCE_TO_ATTACK' });
  }

  /** Entra en attack phase y ejecuta el ataque en un solo mensaje. */
  executeAttack(attackName: string, energyCardIds?: string[]): void {
    this.sendAction({ type: 'EXECUTE_ATTACK', attackName, energyCardIds });
  }

  /** Retira el Pokémon activo y pone otro del banco como activo. */
  retreat(newActiveSlot: number): void {
    this.sendAction({ type: 'RETREAT', targetSlot: newActiveSlot });
  }

  /** Termina el turno sin atacar. */
  passTurn(): void {
    this.sendAction({ type: 'END_TURN' });
  }

  /** Activa una habilidad "Una vez por turno" de un Pokémon en juego. */
  activateAbility(instanceId: string, abilityName: string): void {
    this.sendAction({ type: 'ACTIVATE_ABILITY', cardId: instanceId, attackName: abilityName });
  }

  /** Resuelve un prompt de elección de cartas iniciado por el backend. */
  resolvePlayerChoice(chosenCardIds: string[]): void {
    this.sendAction({ type: 'RESOLVE_PLAYER_CHOICE', chosenCardIds });
  }

  /** Envía el setup inicial de Pokémon (activo + banco) hacia el endpoint de setup. */
  confirmSetup(activeCardId: string, benchCardIds: string[]): void {
    const matchId = this.requireMatchId();
    if (!matchId) {
      return;
    }

    const message: SetupMessage = { activeCardId, benchCardIds };
    this.webSocketService.publish(`/app/matches/${matchId}/setup`, message);
  }

  /** Selecciona un Pokémon Básico de la mano como Pokémon activo. */
  selectActive(cardId: string): void {
    const matchId = this.requireMatchId();
    if (!matchId) {
      console.warn('[TurnActionService] selectActive: no hay matchId');
      return;
    }
    console.log(`[TurnActionService] Enviando select-active: matchId=${matchId}, cardId=${cardId}`);
    // Feedback visual inmediato (optimista) antes de la confirmación del backend
    this.gameStateService.optimisticallyMoveToActive(cardId);
    this.webSocketService.publish(`/app/matches/${matchId}/select-active`, { cardId });
  }

  /** Coloca un Pokémon Básico de la mano en la banca. */
  placeBench(cardId: string): void {
    const matchId = this.requireMatchId();
    if (!matchId) {
      console.warn('[TurnActionService] placeBench: no hay matchId');
      return;
    }
    console.log(`[TurnActionService] Enviando place-bench: matchId=${matchId}, cardId=${cardId}`);
    // Feedback visual inmediato (optimista) antes de la confirmación del backend
    this.gameStateService.optimisticallyMoveToBench(cardId);
    this.webSocketService.publish(`/app/matches/${matchId}/place-bench`, { cardId });
  }

  /** Confirma que el jugador terminó de colocar sus Pokémon (activo + banca). */
  confirmSetupReady(): void {
    const matchId = this.requireMatchId();
    if (!matchId) {
      console.warn('[TurnActionService] confirmSetupReady: no hay matchId');
      return;
    }
    console.log(`[TurnActionService] Enviando setup-ready: matchId=${matchId}`);
    // Feedback visual inmediato (optimista)
    this.gameStateService.matchState.update(state => {
      if (!state?.myState) return state;
      return {
        ...state,
        myState: {
          ...state.myState,
          setupReady: true,
        },
      };
    });
    this.webSocketService.publish(`/app/matches/${matchId}/setup-ready`, {});
  }

  /** Envía la decisión de cuántas cartas robar por los mulligans del oponente. */
  resolveMulliganDraw(count: number): void {
    const matchId = this.requireMatchId();
    if (!matchId) {
      console.warn('[TurnActionService] resolveMulliganDraw: no hay matchId');
      return;
    }
    console.log(`[TurnActionService] Enviando mulligan-draw: matchId=${matchId}, count=${count}`);
    this.webSocketService.publish(`/app/matches/${matchId}/mulligan-draw`, { count });
  }

  /** Publica una acción de juego hacia el endpoint de acciones de la partida. */
  private sendAction(message: GameActionMessage): void {
    const matchId = this.requireMatchId();
    if (!matchId) {
      return;
    }

    this.webSocketService.publish(`/app/matches/${matchId}/action`, message);
  }

  /** Devuelve el matchId actual o `null` (y avisa) si no hay partida activa. */
  private requireMatchId(): string | null {
    const matchId = this.gameStateService.matchId();
    if (!matchId) {
      console.warn('[TurnActionService] No hay partida activa: acción descartada.');
      return null;
    }
    return matchId;
  }
}
