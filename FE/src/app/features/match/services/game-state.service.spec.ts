import { signal } from '@angular/core';
import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting, HttpTestingController } from '@angular/common/http/testing';
import { AuthService } from '../../game/data-access/services/auth.service';
import { PlayerResponse } from '../../game/domain/models/player-response.model';
import { BackendMatchStateDTO, GameEvent, GameStateService, MatchStateView } from './game-state.service';

describe('GameStateService', () => {
  let service: GameStateService;
  let httpTestingController: HttpTestingController;

  const authUser = signal<PlayerResponse | null>({
    id: 'player-1',
    username: 'red',
    trainerName: 'Red',
    email: 'red@kanto.test',
    dateOfBirth: '2000-01-01',
    level: 10,
    experience: 900,
    xpForNextLevel: 1200,
    xpIntoCurrentLevel: 400,
    coins: 0,
    activeDeckId: null,
    collectionCardIds: [],
    decks: [],
  });

  beforeEach(() => {
    authUser.set({
      id: 'player-1',
      username: 'red',
      trainerName: 'Red',
      email: 'red@kanto.test',
      dateOfBirth: '2000-01-01',
      level: 10,
      experience: 900,
      xpForNextLevel: 1200,
      xpIntoCurrentLevel: 400,
      coins: 0,
      activeDeckId: null,
      collectionCardIds: [],
      decks: [],
    });

    TestBed.configureTestingModule({
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        {
          provide: AuthService,
          useValue: {
            currentUser: authUser,
          },
        },
      ],
    });

    service = TestBed.inject(GameStateService);
    httpTestingController = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpTestingController.verify();
  });

  it('should initialize the match state and expose derived signals', () => {
    service.initialize('match-1');

    const request = httpTestingController.expectOne('/api/matches/match-1/state');
    expect(request.request.method).toBe('GET');
    request.flush(createMatchState());

    expect(service.loading()).toBeFalse();
    expect(service.error()).toBeNull();
    expect(service.currentPhase()).toBe('MAIN');
    expect(service.isMyTurn()).toBeTrue();
    expect(service.myHand().map(card => card.name)).toEqual(['Charmander']);
    expect(service.opponentHandCount()).toBe(2);
    expect(service.myActivePokemon()?.name).toBe('Charmander');
    expect(service.opponentBench()[0]?.name).toBe('Squirtle');
    expect(service.myPrizeCount()).toBe(4);
    expect(service.opponentPrizeCount()).toBe(5);
  });

  it('should apply an incoming event as an immutable state patch', () => {
    service.initialize('match-1');
    httpTestingController.expectOne('/api/matches/match-1/state').flush(createMatchState());

    const previousState = service.matchState();
    const event: GameEvent = {
      type: 'ATTACK_RESOLVED',
      matchId: 'match-1',
      payload: {
        state: {
          phase: 'BETWEEN_TURNS',
          activePlayerId: 'player-2',
          myState: {
            playerId: 'player-1',
            hand: [{ id: 'card-3', name: 'Fire Energy' }],
            handCount: 1,
            activePokemon: {
              name: 'Charmeleon',
              currentHp: 70,
              maxHp: 90,
              attachedEnergies: ['Fire', 'Fire'],
              statusConditions: [],
            },
            bench: [],
            prizeCount: 3,
          },
        },
      },
    };

    service.applyEvent(event);

    expect(service.matchState()).not.toBe(previousState);
    expect(service.currentPhase()).toBe('BETWEEN_TURNS');
    expect(service.isMyTurn()).toBeFalse();
    expect(service.myHand().map(card => card.name)).toEqual(['Fire Energy']);
    expect(service.myActivePokemon()?.name).toBe('Charmeleon');
    expect(service.myPrizeCount()).toBe(3);
    expect(service.opponentActivePokemon()?.name).toBe('Bulbasaur');
  });

  it('should derive my and opponent state from players when direct slices are absent', () => {
    service.applyEvent({
      type: 'TURN_STARTED',
      matchId: 'match-2',
      payload: {
        matchState: {
          matchId: 'match-2',
          phase: 'DRAW',
          activePlayerId: 'player-2',
          players: [
            {
              playerId: 'player-1',
              hand: [{ id: 'card-1', name: 'Pidgey' }],
              handCount: 1,
              activePokemon: null,
              bench: [],
              prizeCount: 6,
            },
            {
              playerId: 'player-2',
              hand: [],
              handCount: 4,
              activePokemon: {
                name: 'Totodile',
                currentHp: 50,
                maxHp: 50,
                attachedEnergies: ['Water'],
                statusConditions: [],
              },
              bench: [],
              prizeCount: 6,
            },
          ],
        },
      },
    });

    expect(service.matchId()).toBe('match-2');
    expect(service.currentPhase()).toBe('DRAW');
    expect(service.isMyTurn()).toBeFalse();
    expect(service.myHand()[0]?.name).toBe('Pidgey');
    expect(service.opponentActivePokemon()?.name).toBe('Totodile');
  });

  it('should expose an error when the initial match load fails', () => {
    service.initialize('match-1');

    const request = httpTestingController.expectOne('/api/matches/match-1/state');
    request.flush('error', { status: 500, statusText: 'Server Error' });

    expect(service.loading()).toBeFalse();
    expect(service.matchState()).toBeNull();
    expect(service.error()).toBe('No se pudo cargar el estado inicial de la partida.');
  });

  it('should ignore websocket events without a valid match state patch', () => {
    service.initialize('match-1');
    httpTestingController.expectOne('/api/matches/match-1/state').flush(createMatchState());

    const previousState = service.matchState();

    service.applyEvent({
      type: 'TURN_STARTED',
      matchId: 'match-1',
      payload: { message: 'no-state-here' },
    });

    expect(service.matchState()).toBe(previousState);
    expect(service.currentPhase()).toBe('MAIN');
  });

  it('should expose fallback derived values when no match state exists', () => {
    expect(service.currentPhase()).toBeNull();
    expect(service.isMyTurn()).toBeFalse();
    expect(service.myHand()).toEqual([]);
    expect(service.opponentHandCount()).toBe(0);
    expect(service.myActivePokemon()).toBeNull();
    expect(service.opponentActivePokemon()).toBeNull();
    expect(service.myBench()).toEqual([]);
    expect(service.opponentBench()).toEqual([]);
    expect(service.myPrizeCount()).toBe(0);
    expect(service.opponentPrizeCount()).toBe(0);
  });

  it('should initialize state from a direct event patch when there is no current match', () => {
    authUser.set(null);

    service.applyEvent({
      type: 'MATCH_STARTED',
      payload: {
        phase: 'SETUP',
        activePlayerId: null,
        myState: {
          playerId: 'player-9',
          hand: [],
          handCount: 0,
          activePokemon: null,
          bench: [],
          prizeCount: 6,
        },
      },
    });

    expect(service.matchId()).toBeNull();
    expect(service.matchState()?.matchId).toBe('');
    expect(service.currentPhase()).toBe('SETUP');
  });

  it('should use localPlayerId and player fallbacks when auth user is missing', () => {
    authUser.set(null);

    service.applyEvent({
      type: 'TURN_STARTED',
      matchId: 'match-3',
      payload: {
        matchState: {
          matchId: 'match-3',
          phase: 'MAIN',
          activePlayerId: 'player-3',
          localPlayerId: 'player-3',
          players: [
            {
              playerId: 'player-3',
              hand: [{ id: 'card-1', name: 'Torchic' }],
              handCount: 1,
              activePokemon: null,
              bench: [],
              prizeCount: 6,
            },
          ],
        },
      },
    });

    expect(service.isMyTurn()).toBeTrue();
    expect(service.myHand()[0]?.name).toBe('Torchic');
    expect(service.opponentActivePokemon()).toBeNull();
  });

  it('should keep current player slices when a patch omits optional fields', () => {
    service.initialize('match-1');
    httpTestingController.expectOne('/api/matches/match-1/state').flush(createMatchState());

    service.applyEvent({
      type: 'TURN_STARTED',
      matchId: 'match-1',
      payload: {
        state: {
          myState: {
            playerId: 'player-1',
            hand: undefined,
            handCount: undefined,
            activePokemon: undefined,
            bench: undefined,
            prizeCount: undefined,
          },
        },
      },
    });

    expect(service.myHand().map(card => card.name)).toEqual(['Charmander']);
    expect(service.myActivePokemon()?.name).toBe('Charmander');
    expect(service.myBench()).toEqual([null]);
    expect(service.myPrizeCount()).toBe(4);
  });

  it('should cover normalization helpers with sparse state', () => {
    const normalizedState = service['normalizeMatchState']({
      matchId: '',
      phase: 'MAIN',
      activePlayerId: undefined,
      myState: {
        playerId: 'player-1',
        hand: undefined,
        handCount: undefined,
        activePokemon: {
          name: 'Pikachu',
          currentHp: undefined,
          maxHp: undefined,
          attachedEnergies: undefined,
          statusConditions: undefined,
          toolCardName: undefined,
          attacks: undefined,
        },
        bench: undefined,
        prizeCount: undefined,
      },
    } as unknown as MatchStateView, 'fallback-match');

    expect(normalizedState.matchId).toBe('fallback-match');
    expect(normalizedState.activePlayerId).toBeNull();
    expect(normalizedState.myState?.hand).toEqual([]);
    expect(normalizedState.myState?.handCount).toBe(0);
    expect(normalizedState.myState?.bench).toEqual([]);
    expect(normalizedState.myState?.prizeCount).toBe(0);
    expect(normalizedState.myState?.activePokemon?.currentHp).toBe(0);
    expect(normalizedState.myState?.activePokemon?.maxHp).toBe(0);
    expect(normalizedState.myState?.activePokemon?.attachedEnergies).toEqual([]);
    expect(normalizedState.myState?.activePokemon?.statusConditions).toEqual([]);
    expect(normalizedState.myState?.activePokemon?.toolCardName).toBeNull();
  });

  it('should detect alternative patch shapes', () => {
    service.applyEvent({
      type: 'MATCH_UPDATED',
      matchId: 'match-4',
      payload: {
        matchState: {
          matchId: 'match-4',
          players: [
            {
              playerId: 'player-1',
              hand: [],
              handCount: 0,
              activePokemon: null,
              bench: [],
              prizeCount: 6,
            },
            {
              playerId: 'player-2',
              hand: [],
              handCount: 0,
              activePokemon: null,
              bench: [],
              prizeCount: 6,
            },
          ],
          phase: 'DRAW',
          activePlayerId: 'player-1',
        },
      },
    });

    expect(service.matchId()).toBe('match-4');
    expect(service.currentPhase()).toBe('DRAW');
  });

  it('should resolve player fallbacks and null states through private helpers', () => {
    authUser.set(null);

    service.matchState.set({
      matchId: 'match-5',
      phase: 'MAIN',
      activePlayerId: null,
      myState: {
        playerId: 'fallback-player',
        hand: [],
        handCount: 0,
        activePokemon: null,
        bench: [],
        prizeCount: 6,
        discard: [],
      },
      players: [
        {
          playerId: 'bench-player',
          hand: [],
          handCount: 0,
          activePokemon: null,
          bench: [],
          prizeCount: 6,
          discard: [],
        },
      ],
    });

    expect(service['resolveCurrentPlayerId'](null, service.matchState())).toBe('fallback-player');
    expect(service['resolveMyState']()?.playerId).toBe('fallback-player');
    expect(service['resolveOpponentState']()?.playerId).toBe('bench-player');

    service.matchState.set({ matchId: 'match-6', phase: 'MAIN', activePlayerId: null, players: [] });
    expect(service['resolveCurrentPlayerId'](null, service.matchState())).toBeNull();
    expect(service['resolveMyState']()).toBeNull();
  });

  it('should cover merge helpers for empty, additive, and null branches', () => {
    const currentPlayer = {
      playerId: 'player-1',
      hand: [{ id: 'card-1', name: 'Eevee' }],
      handCount: 1,
      activePokemon: null,
      bench: [],
      prizeCount: 6,
      discard: [],
    };
    const patchPlayer = {
      playerId: 'player-2',
      hand: [],
      handCount: 0,
      activePokemon: null,
      bench: [],
      prizeCount: 5,
      discard: [],
    };

    const unchangedPlayers = service['mergePlayers']([currentPlayer], []);
    const mergedPlayers = service['mergePlayers']([currentPlayer], [patchPlayer]);

    expect(unchangedPlayers[0].playerId).toBe('player-1');
    expect(mergedPlayers.map(player => player.playerId)).toEqual(['player-1', 'player-2']);
    expect(service['mergePlayer'](null, patchPlayer)?.playerId).toBe('player-2');
    expect(service['mergePlayer'](null, null)).toBeNull();
    expect(service['mergePlayer'](currentPlayer, null)?.playerId).toBe('player-1');
  });

  it('should cover match-shape detection and merge fallbacks', () => {
    expect(service['isMatchStateLike']({ players: [] })).toBeTrue();
    expect(service['isMatchStateLike']({ myState: null })).toBeTrue();
    expect(service['isMatchStateLike']({ opponentState: null })).toBeTrue();
    expect(service['extractPatch']({ type: 'X', payload: { foo: 'bar' } })).toBeNull();

    service.matchId.set(null);
    const mergedState = service['mergeMatchState'](
      {
        matchId: 'match-7',
        phase: 'MAIN',
        activePlayerId: 'player-1',
        players: [
          {
            playerId: 'player-1',
            hand: [],
            handCount: 0,
            activePokemon: null,
            bench: [],
            prizeCount: 6,
            discard: [],
          },
        ],
      },
      {
        players: [
          {
            playerId: 'player-2',
            hand: [],
            handCount: 0,
            activePokemon: null,
            bench: [],
            prizeCount: 6,
            discard: [],
          },
        ],
      },
    );

    expect(mergedState.matchId).toBe('match-7');
    expect(mergedState.phase).toBe('MAIN');
    expect(mergedState.activePlayerId).toBe('player-1');
    expect(mergedState.players?.length).toBe(2);
    expect(mergedState.myState).toBeUndefined();
    expect(mergedState.opponentState).toBeUndefined();
  });

  // El endpoint /state devuelve la forma del backend (BackendMatchStateDTO):
  // self/opponent, currentPhase + setupComplete, handSize, remainingPrizes y los
  // Pokémon con attachedEnergyTypes/statuses. El servicio la mapea a MatchStateView.
  function createMatchState(): BackendMatchStateDTO {
    return {
      matchId: 'match-1',
      requestedPlayerId: 'player-1',
      currentTurnPlayerId: 'player-1',
      winnerPlayerId: null,
      currentPhase: 'MAIN',
      waitingForActiveSelection: false,
      finished: false,
      setupComplete: true,
      self: {
        playerId: 'player-1',
        trainerName: 'Red',
        handSize: 1,
        remainingPrizes: 4,
        hand: [{ id: 'card-1', name: 'Charmander', supertype: 'Pokemon' }],
        activePokemon: {
          name: 'Charmander',
          currentHp: 50,
          maxHp: 50,
          attachedEnergyTypes: ['Fire'],
          statuses: [],
        },
        bench: [null],
      },
      opponent: {
        playerId: 'player-2',
        trainerName: 'Blue',
        handSize: 2,
        remainingPrizes: 5,
        hand: [],
        activePokemon: {
          name: 'Bulbasaur',
          currentHp: 40,
          maxHp: 60,
          attachedEnergyTypes: ['Grass'],
          statuses: [],
        },
        bench: [
          {
            name: 'Squirtle',
            currentHp: 50,
            maxHp: 50,
            attachedEnergyTypes: [],
            statuses: [],
          },
        ],
      },
    };
  }
});