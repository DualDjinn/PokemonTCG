import { Injectable, NgZone, computed, inject, signal } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { finalize } from 'rxjs';
import { AuthService } from '../../game/data-access/services/auth.service';
import { PlayerResponse } from '../../game/domain/models/player-response.model';

export type MatchPhase = 'SETUP' | 'DRAW' | 'MAIN' | 'ATTACK' | 'BETWEEN_TURNS' | string;

export interface MatchCardView {
  id: string;
  name: string;
  supertype?: string;
  subtypes?: string[];
  evolvesFrom?: string | null;
  imageUrl?: string;
  gifUrl?: string | null;
  rarity?: string;
  instanceId?: string;
  hp?: number | null;
  attacks?: MatchAttackView[];
  abilities?: AbilityView[];
  weaknesses?: WeaknessDTO[];
  resistances?: ResistanceDTO[];
  retreatCost?: number | null;
  pokemonType?: string | null;
  rules?: string[];
}

export interface WeaknessDTO {
  type: string;
  value: string;
}

export interface ResistanceDTO {
  type: string;
  reduction: number;
}

export interface MatchAttackView {
  name: string;
  damage?: string;
  cost?: string[];
  description?: string;
}

export interface AbilityView {
  name: string;
  description: string;
  type: string;
}

export interface InPlayPokemonView {
  slot?: number;
  cardId?: string;
  instanceId?: string;
  name: string;
  imageUrl?: string;
  gifUrl?: string | null;
  currentHp: number;
  maxHp: number;
  attachedEnergies: string[];
  attachedEnergyCardIds?: string[];
  attachedEnergyImageUrls?: string[];
  retreatCost?: number;
  statusConditions: string[];
  toolCardName?: string | null;
  toolCardId?: string | null;
  toolCardImageUrl?: string | null;
  toolCardRules?: string[];
  attacks?: MatchAttackView[];
  abilities?: AbilityView[];
  weaknesses?: WeaknessDTO[];
  resistances?: ResistanceDTO[];
  pokemonType?: string | null;
}

export interface MatchPlayerStateView {
  playerId: string;
  trainerName?: string;
  cardBackUrl?: string;
  hand: MatchCardView[];
  prizeCards?: MatchCardView[];
  discard: MatchCardView[];
  handCount: number;
  activePokemon: InPlayPokemonView | null;
  bench: Array<InPlayPokemonView | null>;
  prizeCount: number;
  mulliganCount?: number;
  setupReady?: boolean;
  canRetreat?: boolean;
  hasAttachedEnergyThisTurn?: boolean;
  hasPlayedSupporterThisTurn?: boolean;
  abilitiesUsedThisTurn?: string[];
  pendingPlayerChoice?: PendingPlayerChoiceView | null;
  remainingTimeMs?: number;
}

export type PendingPlayerChoiceType =
  | 'PICK_FROM_DECK_TOP_N'
  | 'PICK_BASIC_ENERGIES_FROM_DECK'
  | 'PICK_BENCHED_POKEMON'
  | 'REORDER_DECK_TOP_N'
  | 'COIN_FLIP_INFO'
  | 'CHOOSE_COIN_SIDE'
  | 'PICK_FROM_DISCARD';

export interface PendingPlayerChoiceView {
  type: PendingPlayerChoiceType;
  options: MatchCardView[];
  minPick: number;
  maxPick: number;
  prompt?: string | null;
  coinFlipHeads?: boolean | null;
}

export interface MatchStateView {
  matchId: string;
  phase: MatchPhase;
  activePlayerId: string | null;
  localPlayerId?: string | null;
  firstTurn?: boolean;
  waitingForActiveSelection?: boolean;
  requiresActiveSelection?: boolean;
  pendingPrizeSelection?: boolean;
  pendingPrizeSelectionPlayerId?: string | null;
  openingPhase?: string | null;
  openingChooserPlayerId?: string | null;
  openingCoinChooserPlayerId?: string | null;
  openingCoinChoice?: string | null;
  openingCoinResult?: string | null;
  openingStarterPlayerId?: string | null;
  mulliganIteration?: number;
  setupComplete?: boolean;
  setupRemainingMs?: number;
  dealt?: boolean;
  matchLog?: MatchLogEntry[];
  currentTurnNumber?: number;
  mulliganPlayerId?: string | null;
  mulliganBeneficiaryPlayerId?: string | null;
  mulliganBenefitCount?: number;
  mulliganResolutionPending?: boolean;
  myState?: MatchPlayerStateView | null;
  opponentState?: MatchPlayerStateView | null;
  players?: MatchPlayerStateView[];
  winnerId?: string | null;
  activeStadium?: MatchCardView | null;
  lastPlayedCard?: MatchCardView | null;
  lastPlayedCardPlayerId?: string | null;
}

export interface GameEvent {
  type: string;
  matchId?: string;
  payload?: Record<string, unknown> | null;
}

/** Mulligan event from backend */
export interface MatchLogEntry {
  turnNumber: number;
  playerId: string;
  actionType: string;
  description: string;
  timestampMs: number;
}

export interface BackendMulliganStepDTO {
  playerId: string;
  hand: MatchCardView[];
  mulliganCount: number;
}

export interface BackendMulliganEventDTO {
  matchId: string;
  steps: BackendMulliganStepDTO[];
}

/** Backend DTO shapes (from MatchStateDTO) */
export interface BackendMatchStateDTO {
  matchId: string;
  requestedPlayerId: string;
  currentTurnPlayerId: string;
  winnerPlayerId: string | null;
  currentPhase: string | null;
  waitingForActiveSelection: boolean;
  requiresActiveSelection?: boolean;
  pendingPrizeSelection?: boolean;
  pendingPrizeSelectionPlayerId?: string | null;
  finished: boolean;
  firstTurn?: boolean;
  openingPhase?: string | null;
  openingChooserPlayerId?: string | null;
  openingCoinChooserPlayerId?: string | null;
  openingCoinChoice?: string | null;
  openingCoinResult?: string | null;
  openingStarterPlayerId?: string | null;
  mulliganIteration?: number;
  setupComplete?: boolean;
  setupRemainingMs?: number;
  dealt?: boolean;
  matchLog?: MatchLogEntry[];
  currentTurnNumber?: number;
  mulliganPlayerId?: string | null;
  mulliganBeneficiaryPlayerId?: string | null;
  mulliganBenefitCount?: number;
  mulliganResolutionPending?: boolean;
  self: BackendPlayerFieldDTO | null;
  opponent: BackendPlayerFieldDTO | null;
  activeStadium?: MatchCardView | null;
  lastPlayedCard?: MatchCardView | null;
  lastPlayedCardPlayerId?: string | null;
}

export interface BackendPlayerFieldDTO {
  playerId: string;
  trainerName?: string;
  handSize: number;
  remainingPrizes: number;
  mulliganCount?: number;
  setupReady?: boolean;
  remainingTimeMs?: number;
  hasAttachedEnergyThisTurn?: boolean;
  abilitiesUsedThisTurn?: string[];
  pendingPlayerChoice?: BackendPendingPlayerChoiceDTO | null;
  cardBackUrl?: string;
  hand: MatchCardView[];
  prizeCards?: MatchCardView[];
  discard?: MatchCardView[];
  activePokemon: BackendInPlayPokemonDTO | null;
  bench: (BackendInPlayPokemonDTO | null)[];
}

export interface BackendPendingPlayerChoiceDTO {
  type: string;
  options: MatchCardView[];
  minPick: number;
  maxPick: number;
  prompt?: string | null;
  coinFlipHeads?: boolean | null;
}

export interface BackendInPlayPokemonDTO {
  cardId?: string;
  instanceId?: string;
  name: string;
  imageUrl?: string;
  maxHp: number;
  currentHp: number;
  damageTaken?: number;
  attachedEnergyCount?: number;
  attachedEnergyTypes?: string[];
  attachedEnergyCardIds?: string[];
  attachedEnergyImageUrls?: string[];
  retreatCost?: number;
  pokemonType?: string | null;
  knockedOut?: boolean;
  statuses?: string[];
  attacks?: MatchAttackView[];
  abilities?: AbilityView[];
  weaknesses?: WeaknessDTO[];
  resistances?: ResistanceDTO[];
  toolCardName?: string | null;
  toolCardId?: string | null;
  toolCardImageUrl?: string | null;
  toolCardRules?: string[];
  gifUrl?: string | null;
}

type MatchStatePatch = Partial<MatchStateView>;

@Injectable({ providedIn: 'root' })
export class GameStateService {
  private readonly http = inject(HttpClient);
  private readonly ngZone = inject(NgZone);
  private readonly authService = inject(AuthService);

  readonly matchId = signal<string | null>(null);
  readonly matchState = signal<MatchStateView | null>(null);
  readonly loading = signal(false);
  readonly error = signal<string | null>(null);
  readonly actionError = signal<string | null>(null);
  readonly opponentDisconnected = signal<{ secondsRemaining: number; disconnectedPlayerId: string } | null>(null);
  readonly mulliganSteps = signal<BackendMulliganStepDTO[]>([]);

  readonly currentPhase = computed(() => this.matchState()?.phase ?? null);
  readonly isMyTurn = computed(() => {
    const state = this.matchState();
    const currentPlayerId = this.resolveCurrentPlayerId(this.authService.currentUser(), state);

    return !!state && !!currentPlayerId && state.activePlayerId === currentPlayerId;
  });
  readonly myHand = computed(() => this.resolveMyState()?.hand ?? []);
  readonly opponentHandCount = computed(() => this.resolveOpponentState()?.handCount ?? 0);
  readonly myActivePokemon = computed(() => this.resolveMyState()?.activePokemon ?? null);
  readonly opponentActivePokemon = computed(() => this.resolveOpponentState()?.activePokemon ?? null);
  readonly myBench = computed(() => this.resolveMyState()?.bench ?? []);
  readonly opponentBench = computed(() => this.resolveOpponentState()?.bench ?? []);
  readonly myPrizeCount = computed(() => this.resolveMyState()?.prizeCount ?? 0);
  readonly opponentPrizeCount = computed(() => this.resolveOpponentState()?.prizeCount ?? 0);

  readonly activeStadium = computed(() => this.matchState()?.activeStadium ?? null);
  readonly lastPlayedCard = computed(() => this.matchState()?.lastPlayedCard ?? null);
  readonly lastPlayedCardPlayerId = computed(() => this.matchState()?.lastPlayedCardPlayerId ?? null);

  readonly openingPhase = computed(() => this.matchState()?.openingPhase ?? null);
  readonly openingChooserId = computed(() => this.matchState()?.openingChooserPlayerId ?? null);
  readonly openingCoinChooserId = computed(() => this.matchState()?.openingCoinChooserPlayerId ?? null);
  readonly openingCoinChoice = computed(() => this.matchState()?.openingCoinChoice ?? null);
  readonly openingCoinResult = computed(() => this.matchState()?.openingCoinResult ?? null);
  readonly openingStarterId = computed(() => this.matchState()?.openingStarterPlayerId ?? null);
  readonly isOpeningComplete = computed(() => this.openingPhase() === 'COMPLETE');

  readonly isSetupComplete = computed(() => this.matchState()?.setupComplete ?? false);
  readonly mySetupReady = computed(() => this.resolveMyState()?.setupReady ?? false);
  readonly opponentSetupReady = computed(() => this.resolveOpponentState()?.setupReady ?? false);
  readonly isSetupPhase = computed(() => this.currentPhase() === 'SETUP');
  readonly requiresActiveSelection = computed(() => this.matchState()?.phase === 'SETUP' && !this.isSetupComplete());

  readonly isPendingPrizeSelection = computed(() =>
    this.matchState()?.pendingPrizeSelection ?? false);

  readonly isMyPrizeSelection = computed(() => {
    const state = this.matchState();
    const currentPlayerId = this.resolveCurrentPlayerId(this.authService.currentUser(), state);
    return !!state?.pendingPrizeSelectionPlayerId && state.pendingPrizeSelectionPlayerId === currentPlayerId;
  });

  readonly isOpponentPrizeSelection = computed(() => {
    const state = this.matchState();
    const currentPlayerId = this.resolveCurrentPlayerId(this.authService.currentUser(), state);
    return !!state?.pendingPrizeSelectionPlayerId && state.pendingPrizeSelectionPlayerId !== currentPlayerId;
  });

  readonly myPrizeCards = computed(() => this.resolveMyState()?.prizeCards ?? []);
  readonly myCardBackUrl = computed(() => this.resolveMyState()?.cardBackUrl);
  readonly opponentCardBackUrl = computed(() => this.resolveOpponentState()?.cardBackUrl);

  readonly isVictory = computed(() => {
    const state = this.matchState();
    const currentPlayerId = this.resolveCurrentPlayerId(this.authService.currentUser(), state);
    return !!state?.winnerId && state.winnerId === currentPlayerId;
  });

  readonly isDefeat = computed(() => {
    const state = this.matchState();
    const currentPlayerId = this.resolveCurrentPlayerId(this.authService.currentUser(), state);
    return !!state?.winnerId && state.winnerId !== currentPlayerId;
  });

  readonly needsBenchPromotion = computed(() => {
    const state = this.matchState();
    const currentPlayerId = this.resolveCurrentPlayerId(this.authService.currentUser(), state);
    return !!state?.waitingForActiveSelection && !!currentPlayerId
        && !this.isSetupPhase() && state.requiresActiveSelection;
  });

  readonly opponentIsChoosingActive = computed(() => {
    const state = this.matchState();
    const currentPlayerId = this.resolveCurrentPlayerId(this.authService.currentUser(), state);
    return !!state?.waitingForActiveSelection && !!currentPlayerId
        && !this.isSetupPhase() && !state.requiresActiveSelection;
  });

  /** Signal que se activa cuando el oponente está interactuando con su Pokémon. */
  readonly opponentHovering = signal(false);

  readonly isMulliganPlayer = computed(() => {
    const state = this.matchState();
    const currentPlayerId = this.resolveCurrentPlayerId(this.authService.currentUser(), state);
    return !!state?.mulliganPlayerId && state.mulliganPlayerId === currentPlayerId;
  });

  readonly isOpponentMulliganPlayer = computed(() => {
    const state = this.matchState();
    const currentPlayerId = this.resolveCurrentPlayerId(this.authService.currentUser(), state);
    return !!state?.mulliganPlayerId && state.mulliganPlayerId !== currentPlayerId;
  });

  readonly isMulliganBeneficiary = computed(() => {
    const state = this.matchState();
    const currentPlayerId = this.resolveCurrentPlayerId(this.authService.currentUser(), state);
    return !!state?.mulliganBeneficiaryPlayerId && state.mulliganBeneficiaryPlayerId === currentPlayerId;
  });

  readonly mulliganBenefitCount = computed(() => this.matchState()?.mulliganBenefitCount ?? 0);

  readonly isMulliganResolutionPending = computed(() => this.matchState()?.mulliganResolutionPending ?? false);

  /** Optimistic: add a card to hand (used for prize selection). */
  optimisticallyAddToHand(card: MatchCardView): void {
    this.matchState.update(state => {
      if (!state?.myState) return state;
      return {
        ...state,
        myState: {
          ...state.myState,
          hand: [...state.myState.hand, card],
          handCount: state.myState.handCount + 1,
        },
      };
    });
  }

  readonly mulliganIteration = computed(() => this.matchState()?.mulliganIteration ?? 0);

  readonly hasAttachedEnergyThisTurn = computed(() =>
    this.resolveMyState()?.hasAttachedEnergyThisTurn ?? false);

  readonly abilitiesUsedThisTurn = computed(() =>
    this.resolveMyState()?.abilitiesUsedThisTurn ?? []);

  readonly pendingPlayerChoice = computed(() =>
    this.resolveMyState()?.pendingPlayerChoice ?? null);

  readonly myRemainingTimeMs = computed(() =>
    this.resolveMyState()?.remainingTimeMs ?? 0);

  readonly opponentRemainingTimeMs = computed(() =>
    this.resolveOpponentState()?.remainingTimeMs ?? 0);

  /** Determina el paso actual del setup: 'active' (colocar activo), 'bench' (colocar banca), 'waiting' (esperando oponente), o null. */
  readonly setupStep = computed(() => {
    if (!this.isSetupPhase()) {
      return null;
    }
    if (this.mySetupReady()) {
      return 'waiting' as const;
    }
    if (!this.myActivePokemon()) {
      return 'active' as const;
    }
    return 'bench' as const;
  });

  /**
   * Actualiza optimistamente el estado local: mueve una carta de la mano al Pokémon activo.
   * Se usa para feedback visual inmediato antes de que llegue la confirmación del backend.
   */
  optimisticallyMoveToActive(cardId: string): void {
    this.matchState.update(state => {
      if (!state?.myState) return state;
      const hand = state.myState.hand;
      const card = hand.find(c => c.id === cardId);
      if (!card) return state;

      const newHand = hand.filter(c => c.id !== cardId);
      const activePokemon: InPlayPokemonView = {
        cardId: card.id,
        name: card.name,
        imageUrl: card.imageUrl,
        gifUrl: card.gifUrl ?? null,
        currentHp: 0,
        maxHp: 0,
        attachedEnergies: [],
        statusConditions: [],
      };

      return {
        ...state,
        myState: {
          ...state.myState,
          hand: newHand,
          handCount: newHand.length,
          activePokemon,
        },
      };
    });
  }

  /**
   * Actualiza optimistamente el estado local: mueve una carta de la mano a la banca.
   */
  optimisticallyMoveToBench(cardId: string): void {
    this.matchState.update(state => {
      if (!state?.myState) return state;
      const hand = state.myState.hand;
      const card = hand.find(c => c.id === cardId);
      if (!card) return state;

      const newHand = hand.filter(c => c.id !== cardId);
      const benchPokemon: InPlayPokemonView = {
        cardId: card.id,
        name: card.name,
        imageUrl: card.imageUrl,
        gifUrl: card.gifUrl ?? null,
        currentHp: 0,
        maxHp: 0,
        attachedEnergies: [],
        statusConditions: [],
      };

      const newBench = [...(state.myState.bench ?? []), benchPokemon];

      return {
        ...state,
        myState: {
          ...state.myState,
          hand: newHand,
          handCount: newHand.length,
          bench: newBench,
        },
      };
    });
  }

  private fetchRetryTimeout: ReturnType<typeof setTimeout> | null = null;

  initialize(matchId: string): void {
    if (this.fetchRetryTimeout) {
      clearTimeout(this.fetchRetryTimeout);
      this.fetchRetryTimeout = null;
    }
    this.reset();
    this.matchId.set(matchId);
    this.loading.set(true);

    this.fetchStateWithRetry(matchId, 3);
  }

  reset(): void {
    if (this.fetchRetryTimeout) {
      clearTimeout(this.fetchRetryTimeout);
      this.fetchRetryTimeout = null;
    }
    this.matchId.set(null);
    this.matchState.set(null);
    this.loading.set(false);
    this.error.set(null);
    this.mulliganSteps.set([]);
    this.opponentDisconnected.set(null);
  }

  private fetchStateWithRetry(matchId: string, retriesLeft: number): void {
    this.http.get<BackendMatchStateDTO>(`/api/matches/${matchId}/state`)
      .pipe(finalize(() => {
        if (retriesLeft <= 0 && this.loading()) {
          this.loading.set(false);
        }
      }))
      .subscribe({
        next: dto => {
          this.fetchRetryTimeout = null;
          this.loading.set(false);
          this.processRawState(dto, matchId);
        },
        error: (err) => {
          if (retriesLeft > 0 && err.status === 404) {
            this.fetchRetryTimeout = setTimeout(() => this.fetchStateWithRetry(matchId, retriesLeft - 1), 1000);
          } else {
            this.fetchRetryTimeout = null;
            this.loading.set(false);
            this.error.set('No se pudo cargar el estado inicial de la partida.');
            this.matchState.set(null);
          }
        }
      });
  }

  processRawState(dto: BackendMatchStateDTO, fallbackId: string): void {
    this.matchId.set(dto.matchId || fallbackId);
    this.matchState.set(this.toMatchStateView(dto, dto.matchId || fallbackId));
  }

  processMulliganEvent(dto: BackendMulliganEventDTO): void {
    this.mulliganSteps.set(dto.steps ?? []);
  }

  private toMatchStateView(dto: BackendMatchStateDTO, fallbackId: string): MatchStateView {
    const phase: MatchPhase = !dto.setupComplete
      ? 'SETUP'
      : (dto.currentPhase ?? null) as MatchPhase;

    return {
      matchId: dto.matchId || fallbackId,
      phase,
      activePlayerId: dto.currentTurnPlayerId ?? null,
      localPlayerId: dto.requestedPlayerId ?? null,
      openingPhase: dto.openingPhase ?? null,
      openingChooserPlayerId: dto.openingChooserPlayerId ?? null,
      openingCoinChooserPlayerId: dto.openingCoinChooserPlayerId ?? null,
      openingCoinChoice: dto.openingCoinChoice ?? null,
      openingCoinResult: dto.openingCoinResult ?? null,
      openingStarterPlayerId: dto.openingStarterPlayerId ?? null,
      mulliganIteration: dto.mulliganIteration ?? 0,
      firstTurn: dto.firstTurn ?? false,
      waitingForActiveSelection: dto.waitingForActiveSelection ?? false,
      requiresActiveSelection: dto.requiresActiveSelection ?? false,
      pendingPrizeSelection: dto.pendingPrizeSelection ?? false,
      pendingPrizeSelectionPlayerId: dto.pendingPrizeSelectionPlayerId ?? null,
      setupComplete: dto.setupComplete ?? false,
      setupRemainingMs: dto.setupRemainingMs ?? 0,
      dealt: dto.dealt ?? false,
      matchLog: dto.matchLog ?? [],
      currentTurnNumber: dto.currentTurnNumber ?? 0,
      mulliganPlayerId: dto.mulliganPlayerId ?? null,
      mulliganBeneficiaryPlayerId: dto.mulliganBeneficiaryPlayerId ?? null,
      mulliganBenefitCount: dto.mulliganBenefitCount ?? 0,
      mulliganResolutionPending: dto.mulliganResolutionPending ?? false,
      winnerId: dto.winnerPlayerId ?? null,
      activeStadium: dto.activeStadium ?? null,
      lastPlayedCard: dto.lastPlayedCard ?? null,
      lastPlayedCardPlayerId: dto.lastPlayedCardPlayerId ?? null,
      myState: dto.self ? this.toPlayerState(dto.self) : undefined,
      opponentState: dto.opponent ? this.toPlayerState(dto.opponent) : undefined,
      players: [dto.self, dto.opponent]
        // != null descarta tanto null como undefined (campo ausente) para no
        // pasarle un undefined a toPlayerState y evitar el NPE en field.playerId.
        .filter((p): p is BackendPlayerFieldDTO => p != null)
        .map(p => this.toPlayerState(p)),
    };
  }

  private toPlayerState(field: BackendPlayerFieldDTO): MatchPlayerStateView {
    return {
      playerId: field.playerId,
      trainerName: field.trainerName,
      hand: field.hand ?? [],
      prizeCards: field.prizeCards ?? [],
      discard: field.discard ?? [],
      cardBackUrl: field.cardBackUrl,
      handCount: field.handSize ?? 0,
      mulliganCount: field.mulliganCount ?? 0,
      setupReady: field.setupReady ?? false,
      hasAttachedEnergyThisTurn: field.hasAttachedEnergyThisTurn ?? false,
      abilitiesUsedThisTurn: field.abilitiesUsedThisTurn ?? [],
      pendingPlayerChoice: field.pendingPlayerChoice
        ? {
            type: field.pendingPlayerChoice.type as PendingPlayerChoiceType,
            options: field.pendingPlayerChoice.options ?? [],
            minPick: field.pendingPlayerChoice.minPick,
            maxPick: field.pendingPlayerChoice.maxPick,
            prompt: field.pendingPlayerChoice.prompt ?? null,
            coinFlipHeads: field.pendingPlayerChoice.coinFlipHeads ?? null,
          }
        : null,
      remainingTimeMs: field.remainingTimeMs,
      activePokemon: field.activePokemon ? this.toInPlayPokemon(field.activePokemon) : null,
      bench: (field.bench ?? []).map(p => p ? this.toInPlayPokemon(p) : null),
      prizeCount: field.remainingPrizes ?? 0,
    };
  }

  private toInPlayPokemon(dto: BackendInPlayPokemonDTO): InPlayPokemonView {
    return {
      cardId: dto.cardId,
      instanceId: dto.instanceId,
      name: dto.name,
      imageUrl: dto.imageUrl,
      gifUrl: dto.gifUrl ?? null,
      currentHp: dto.currentHp ?? 0,
      maxHp: dto.maxHp ?? 0,
      attachedEnergies: dto.attachedEnergyTypes ?? [],
      attachedEnergyCardIds: dto.attachedEnergyCardIds ?? [],
      attachedEnergyImageUrls: dto.attachedEnergyImageUrls ?? [],
      retreatCost: dto.retreatCost ?? 0,
      statusConditions: dto.statuses ?? [],
      toolCardName: dto.toolCardName ?? null,
      toolCardId: dto.toolCardId ?? null,
      toolCardImageUrl: dto.toolCardImageUrl ?? null,
      toolCardRules: dto.toolCardRules ?? [],
      pokemonType: dto.pokemonType ?? null,
      attacks: dto.attacks ?? [],
      abilities: dto.abilities ?? [],
      weaknesses: dto.weaknesses ?? [],
      resistances: dto.resistances ?? [],
    };
  }

  applyEvent(event: GameEvent): void {
    if (event.matchId) {
      this.matchId.set(event.matchId);
    }

    if (event.type === 'OPPONENT_HOVER') {
      this.ngZone.run(() => this.opponentHovering.set(true));
      return;
    }
    if (event.type === 'OPPONENT_UNHOVER') {
      this.ngZone.run(() => this.opponentHovering.set(false));
      return;
    }

    const patch = this.extractPatch(event);
    if (!patch) {
      return;
    }

    this.matchState.update(currentState => {
      if (!currentState) {
        return this.normalizeMatchState(patch as MatchStateView, this.matchId() ?? event.matchId ?? '');
      }

      return this.mergeMatchState(currentState, patch);
    });
  }

  private resolveMyState(): MatchPlayerStateView | null {
    const state = this.matchState();
    if (!state) {
      return null;
    }

    if (state.myState) {
      return state.myState;
    }

    const currentPlayerId = this.resolveCurrentPlayerId(this.authService.currentUser(), state);
    const player = state.players?.find(entry => entry.playerId === currentPlayerId) ?? state.players?.[0] ?? null;

    return player ? this.normalizePlayerState(player) : null;
  }

  private resolveOpponentState(): MatchPlayerStateView | null {
    const state = this.matchState();
    if (!state) {
      return null;
    }

    if (state.opponentState) {
      return state.opponentState;
    }

    const currentPlayerId = this.resolveCurrentPlayerId(this.authService.currentUser(), state);
    const opponent = state.players?.find(entry => entry.playerId !== currentPlayerId) ?? state.players?.[1] ?? null;

    return opponent ? this.normalizePlayerState(opponent) : null;
  }

  private resolveCurrentPlayerId(user: PlayerResponse | null, state: MatchStateView | null): string | null {
    return user?.id ?? state?.localPlayerId ?? state?.myState?.playerId ?? null;
  }

  private extractPatch(event: GameEvent): MatchStatePatch | null {
    const payload = event.payload;
    if (!payload) {
      return null;
    }

    const directState = payload['matchState'];
    if (this.isMatchStateLike(directState)) {
      return directState;
    }

    const statePatch = payload['state'];
    if (this.isMatchStatePatchLike(statePatch)) {
      return statePatch;
    }

    if (this.isMatchStatePatchLike(payload)) {
      return payload;
    }

    return null;
  }

  private isMatchStateLike(value: unknown): value is MatchStateView {
    if (!value || typeof value !== 'object') {
      return false;
    }

    return 'phase' in value || 'players' in value || 'myState' in value || 'opponentState' in value;
  }

  private isMatchStatePatchLike(value: unknown): value is MatchStatePatch {
    if (!value || typeof value !== 'object') {
      return false;
    }

    return 'phase' in value || 'players' in value || 'myState' in value || 'opponentState' in value || 'activePlayerId' in value;
  }

  private mergeMatchState(current: MatchStateView, patch: MatchStatePatch): MatchStateView {
    const mergedPlayers = this.mergePlayers(current.players ?? [], patch.players ?? []);
    const nextState: MatchStateView = {
      ...current,
      ...patch,
      matchId: patch.matchId ?? current.matchId,
      activePlayerId: patch.activePlayerId ?? current.activePlayerId,
      phase: patch.phase ?? current.phase,
      myState: this.mergePlayer(current.myState ?? null, patch.myState ?? null),
      opponentState: this.mergePlayer(current.opponentState ?? null, patch.opponentState ?? null),
      players: mergedPlayers.length > 0 ? mergedPlayers : current.players,
    };

    return this.normalizeMatchState(nextState, this.matchId() ?? nextState.matchId);
  }

  private mergePlayers(currentPlayers: MatchPlayerStateView[], patchPlayers: MatchPlayerStateView[]): MatchPlayerStateView[] {
    if (patchPlayers.length === 0) {
      return currentPlayers.map(player => this.normalizePlayerState(player));
    }

    const playersById = new Map(currentPlayers.map(player => [player.playerId, player]));

    for (const playerPatch of patchPlayers) {
      const existing = playersById.get(playerPatch.playerId) ?? null;
      playersById.set(playerPatch.playerId, this.mergePlayer(existing, playerPatch) ?? this.normalizePlayerState(playerPatch));
    }

    return Array.from(playersById.values()).map(player => this.normalizePlayerState(player));
  }

  private mergePlayer(
    current: MatchPlayerStateView | null,
    patch: MatchPlayerStateView | null | undefined,
  ): MatchPlayerStateView | null {
    if (!patch) {
      return current ? this.normalizePlayerState(current) : null;
    }

    if (!current) {
      return this.normalizePlayerState(patch);
    }

    return this.normalizePlayerState({
      ...current,
      ...patch,
      hand: patch.hand ?? current.hand,
      discard: patch.discard ?? current.discard,
      handCount: patch.handCount ?? current.handCount,
      setupReady: patch.setupReady ?? current.setupReady,
      activePokemon: patch.activePokemon ?? current.activePokemon,
      bench: patch.bench ?? current.bench,
      prizeCount: patch.prizeCount ?? current.prizeCount,
    });
  }

  private normalizeMatchState(state: MatchStateView, fallbackMatchId: string): MatchStateView {
    return {
      ...state,
      matchId: state.matchId || fallbackMatchId,
      activePlayerId: state.activePlayerId ?? null,
      localPlayerId: state.localPlayerId ?? null,
      myState: state.myState ? this.normalizePlayerState(state.myState) : undefined,
      opponentState: state.opponentState ? this.normalizePlayerState(state.opponentState) : undefined,
      players: state.players?.map(player => this.normalizePlayerState(player)) ?? undefined,
      winnerId: state.winnerId ?? null,
    };
  }

  private normalizePlayerState(player: MatchPlayerStateView): MatchPlayerStateView {
    return {
      ...player,
      trainerName: player.trainerName,
      hand: player.hand ?? [],
      discard: player.discard ?? [],
      handCount: player.handCount ?? player.hand?.length ?? 0,
      setupReady: player.setupReady ?? false,
      activePokemon: player.activePokemon ? this.normalizePokemon(player.activePokemon) : null,
      bench: (player.bench ?? []).map(pokemon => pokemon ? this.normalizePokemon(pokemon) : null),
      prizeCount: player.prizeCount ?? 0,
    };
  }

  private normalizePokemon(pokemon: InPlayPokemonView): InPlayPokemonView {
    return {
      ...pokemon,
      currentHp: pokemon.currentHp ?? 0,
      maxHp: pokemon.maxHp ?? 0,
      attachedEnergies: pokemon.attachedEnergies ?? [],
      statusConditions: pokemon.statusConditions ?? [],
      attacks: pokemon.attacks ?? [],
      toolCardName: pokemon.toolCardName ?? null,
    };
  }
}
