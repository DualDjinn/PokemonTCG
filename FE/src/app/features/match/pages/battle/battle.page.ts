import {
  ChangeDetectionStrategy,
  ChangeDetectorRef,
  Component,
  DestroyRef,
  HostListener,
  NgZone,
  computed,
  effect,
  OnDestroy,
  OnInit,
  inject,
  signal,
} from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { fromEvent } from 'rxjs';
import { filter } from 'rxjs/operators';
import { Router, ActivatedRoute } from '@angular/router';
import { TranslateModule, TranslateService } from '@ngx-translate/core';
import { BattleBoardComponent } from '../../components/battle-board/battle-board.component';
import { TableCardsComponent } from '../../components/table-cards/table-cards.component';
import { CardDealingComponent } from '../../components/card-dealing/card-dealing.component';
import { MulliganReDealComponent } from '../../components/mulligan-re-deal/mulligan-re-deal.component';
import { HandCardComponent } from '../../components/hand-card/hand-card.component';

import { DeckCardDetail } from '../../../decks/components/deck-card-detail/deck-card-detail';
import { CardSummary } from '../../../decks/domain/models/deck.models';
import { GameStateService, AbilityView, InPlayPokemonView, MatchCardView, MatchLogEntry } from '../../services/game-state.service';
import { WebSocketService } from '../../services/websocket.service';
import { TurnActionService } from '../../services/turn-action.service';
import { AttackEffectComponent } from '../../components/attack-effect/attack-effect.component';
import { AuthService } from '../../../game/data-access/services/auth.service';
import { MyModal } from '../../../../shared/components/my-modal/my-modal';
import { MyConfirmModal } from '../../../../shared/components/my-confirm-modal/my-confirm-modal';
import { MyButton } from '../../../../shared/ui/my-button/my-button';
import { AudioSfxChannel } from '../../../../core/services/audio-sfx-channel.service';
import { AudioMusicChannel } from '../../../../core/services/audio-music-channel.service';
import { PokenavDeviceComponent } from '../../../game/components/pokenav-device/pokenav-device.component';
import { GameOptionsComponent } from '../../../game/components/game-options/game-options';
import { CoinFlipComponent } from '../../components/coin-flip/coin-flip.component';
import { CardPickerModal } from '../../../../shared/components/card-picker-modal/card-picker-modal';
import { MulliganResolutionModal } from '../../components/mulligan-resolution-modal/mulligan-resolution-modal';
import { ToastService } from '../../../../shared/services/toast.service';
import { getTrainerDescription } from '../../data/trainer-descriptions';
import { GameEventEnvelope } from '../../services/websocket.service';
import { CircleTransitionService } from '../../../../shared/services/circle-transition.service';
import { JudgeWidgetComponent } from '../../../judge/components/judge-widget/judge-widget.component';
import { JudgeMewService } from '../../../judge/services/judge-mew.service';
import { JudgeService } from '../../../judge/services/judge.service';
import { JudgeRulesExplainer } from '../../../judge/services/judge-rules-explainer.service';
import { JudgeQueryCategory } from '../../../judge/models/judge.types';

interface AttackFlyData {
  left: number;
  top: number;
  width: number;
  height: number;
  imageUrl: string;
  name: string;
  opacity: number;
}

@Component({
  selector: 'app-battle-page',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [BattleBoardComponent, TableCardsComponent, CardDealingComponent, MulliganReDealComponent, HandCardComponent, DeckCardDetail, MyModal, MyConfirmModal, MyButton, TranslateModule, PokenavDeviceComponent, GameOptionsComponent, CoinFlipComponent, MulliganResolutionModal, CardPickerModal, AttackEffectComponent, JudgeWidgetComponent],
  styleUrl: './battle.page.css',
  template: `
    <div class="battle-page" [class.is-active-extended]="isActiveExtended()">
      <!-- Asistente Mew (juez): a nivel raíz para que NO se recree con los re-renders del
           tablero. Es position:fixed; visible sólo en partidas vs bot (gated por botSessionActive). -->
      <app-judge-widget [matchId]="lobbyMatchId() ?? ''" />

      <!-- Toggle del juez (arriba a la izquierda), sólo en partidas vs bot. -->
      @if (isBotMatch()) {
        <button
          type="button"
          class="judge-toggle"
          [class.judge-toggle--off]="!judgeMew.enabled()"
          (click)="judgeMew.toggleEnabled()"
          [title]="judgeMew.enabled() ? 'Desactivar al juez Mew' : 'Activar al juez Mew'"
        >
          <span class="judge-toggle__dot"></span>
          Juez {{ judgeMew.enabled() ? 'ON' : 'OFF' }}
        </button>
      }

      <!-- Error overlay (always on top when present) -->
      @if (error(); as err) {
        <div class="error-overlay">
          <p>{{ err }}</p>
          <my-button [text]="'MATCH.BACK_TO_LOBBY' | translate" variant="pixel" size="small" (clicked)="goToLobby()" />
        </div>
      }

      <!-- Loading overlay with Pokeball (minimum 10s) -->
      @if (!canShowContent() && !error()) {
        <div class="loading-overlay">
          <img src="/assets/img/login/pokeball.svg" class="pokeball-spinner" alt="" />
          <p class="loading-text">{{ 'OPENING.LOADING' | translate }}</p>
        </div>
      }

      <!-- Content (only when loading is done) -->
      @if (canShowContent() && !error()) {
        <!-- Opening overlay (shown before board) -->
        @if (showOpeningOverlay()) {
          <app-coin-flip
            [playerRole]="isCurrentChooser() ? 'chooser' : 'waiter'"
            [wasCoinChooser]="myPlayerId() === openingCoinChooserId()"
            [canChooseStarter]="isCurrentChooser()"
            [openingState]="openingPhase()"
            [openingCoinResult]="openingCoinResult()"
            [chooserChoice]="openingCoinChoice()"
            (coinSideChosen)="chooseCoinSide($event)"
            (starterChosen)="onStarterChosen($event)"
          />
        }

        <!-- Game board (only when opening is complete) -->
        @if (isOpeningComplete()) {
        <!-- Dealing animation overlay -->
        @if (dealingState() === 'dealing') {
          <app-card-dealing
            [myCards]="myHand()"
            [opponentCount]="opponentHandCount()"
            [cardBackUrl]="gameStateService.myCardBackUrl()"
            (dealt)="onDealtComplete()"
            (firstDealDone)="onFirstDealDone()"
          />
        }
        @if (mulliganDealState() === 'animating') {
          <app-mulligan-re-deal
            [side]="mulliganSide()"
            [iterations]="mulliganIteration()"
            [handSize]="mulliganSide() === 'player' ? myHand().length : opponentHandCount()"
            [cardBackUrl]="mulliganSide() === 'player' ? gameStateService.myCardBackUrl() : gameStateService.opponentCardBackUrl()"
            (done)="onMulliganDealDone()"
          />
        }
        <!-- Setup banner -->
        @if (isSetupPhase() && setupStep()) {
          <div class="setup-banner">
            <span class="setup-banner__text">{{ setupBannerText() }}</span>
            @if (setupCountdownActive()) {
              <span class="setup-banner__timer">{{ setupCountdown() }}s</span>
            }
            @if (setupStep() === 'bench') {
              <my-button
                [text]="'BATTLE.SETUP_READY_BTN' | translate"
                variant="pixel"
                size="small"
                (clicked)="onSetupReady()"
              ></my-button>
            }
          </div>
        }

        <!-- Setup to First Turn transition -->
        @if (setupTransitioning()) {
          <div class="setup-transition-overlay">
            <div class="setup-transition-content">
              <img src="/assets/img/login/pokeball.svg" class="pokeball-spinner" alt="" />
              <p class="setup-transition-text">{{ 'BATTLE.SETUP_STARTING' | translate }}</p>
            </div>
          </div>
        }

        <!-- Mulligan resolution modal -->
        @if (isMulliganResolutionPending() && (isMulliganBeneficiary() || isMulliganPlayer())) {
          <app-mulligan-resolution-modal
            [open]="true"
            [isBeneficiary]="isMulliganBeneficiary()"
            [maxCount]="mulliganBenefitCount()"
            [opponentName]="opponentTrainerName()"
            (countChosen)="onMulliganDrawChosen($event)"
          />
        }

        <!-- Gear icon top-right -->
        <button class="settings-btn" (click)="showMenuModal.set(true)" [attr.aria-label]="'MATCH.MENU_TITLE' | translate">&#9881;</button>

        <!-- Reactions button left -->
        <div class="left-reactions">
          <button class="reactions-circle-btn" (click)="toggleReactions()"><img src="/assets/img/login/pokeball.svg" alt="Reacciones" class="reaction-pokeball" /></button>
          @if (showReactions()) {
            <div class="reactions-menu">
              @for (r of reactions; track $index; let i = $index) {
                <button class="reaction-item" [style.animation-delay]="(i * 80) + 'ms'" (click)="onReaction(r)">{{ r }}</button>
              }
            </div>
          }
        </div>

        <!-- Right aside: end turn button + prize counts -->
        <div class="right-aside">
          <button class="history-toggle-btn" (click)="showHistory.set(!showHistory())"
            [title]="showHistory() ? 'Cerrar historial' : 'Abrir historial'">
            <svg width="22" height="22" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <path d="M9 12h6M9 8h6M9 16h4M5 3h14a2 2 0 012 2v14a2 2 0 01-2 2H5a2 2 0 01-2-2V5a2 2 0 012-2z"/>
            </svg>
          </button>
          <div class="right-aside-row">
            @if (showEndTurnBtn()) {
              <button class="end-turn-btn" (click)="endTurn()">ACABAR<br>EL TURNO</button>
            }
            <div class="prize-stack">
              <div class="timer-box">{{ opponentTimeDisplay() }}</div>
              <div class="prize-box prize-box--opponent">{{ opponentPrizeCount() }}</div>
              <div class="prize-box prize-box--player">{{ myPrizeCount() }}</div>
              <div class="timer-box">{{ myTimeDisplay() }}</div>
            </div>
          </div>
        </div>

        <!-- History aside panel -->
        @if (showHistory()) {
          <div class="history-overlay" (click)="showHistory.set(false)"></div>
          <aside class="history-panel">
            <div class="history-header">
              <span class="history-title">Historial</span>
              <button class="history-close-btn" (click)="showHistory.set(false)">✕</button>
            </div>
            <div class="history-scroll">
              @if (groupedHistory().length === 0) {
                <div class="history-empty">No hay acciones todavía</div>
              }
              @for (turn of groupedHistory(); track turn.turnNumber) {
                <div class="history-turn-divider">
                  Turno {{ turn.turnNumber }} ({{ turn.playerName ?? '—' }})
                </div>
                @if (turn.entries.length === 0) {
                  <div class="history-empty-turn">Sin acciones</div>
                }
                @for (entry of turn.entries; track entry.timestampMs) {
                  <div class="history-entry">{{ entry.description }}</div>
                }
              }
            </div>
          </aside>
        }

        <!-- Main battle field -->
        <div class="battle-field">
          <div class="hand-cards-row hand-cards-row--opponent" [class.hand-cards-row--hidden]="!initialDealDone() || (isOpponentMulliganPlayer() && (dealingState() !== 'done' || mulliganDealState() === 'animating'))">
            <div class="opponent-cards-row">
              @for (card of opponentHandCards(); track card.id) {
                <div class="hand-card-back">
                  @let oppBack = gameStateService.opponentCardBackUrl();
                  @if (oppBack) {
                    <img [src]="oppBack" class="card-back-img w-full h-full" />
                  } @else {
                    <div class="card-back"></div>
                  }
                </div>
              }
            </div>
            @if (opponentHandCards().length > 0) {
              <span class="hand-count-badge">{{ opponentHandCards().length }}</span>
            }
          </div>

          <div class="battle-row battle-row--opponent">
            <div class="side-area side-area--left">
              <div class="side-stack">
                <div class="side-container side-container--opponent">
                  @if (opponentPrizeCount(); as count) {
                    <div class="prize-area" [class.prize-area--hidden]="isSetupPhase() || !prizesDealt()">
                      @for (_ of [1,2,3,4,5,6]; track $index) {
                        <div
                          class="prize-card"
                          [style.transform]="'translateX(' + ($index * 18) + 'px)'"
                        >
                          @let oppBack = gameStateService.opponentCardBackUrl();
                          @if (oppBack) {
                            <img [src]="oppBack" class="card-back-img w-full h-full" />
                          } @else {
                            <div class="card-back"></div>
                          }
                        </div>
                      }
                    </div>
                    <div class="side-counter side-counter--right">{{ count }}</div>
                  }
                </div>
              </div>
            </div>

            <div class="hand-area hand-area--opponent">
              <app-table-cards [cards]="opponentBenchCards()" currentPhase="MAIN" [showCardBacks]="isSetupPhase() && !isSetupComplete()" [cardBackUrl]="gameStateService.opponentCardBackUrl()" [isOpponentZone]="true" [hpMap]="opponentBenchHpMap()" [damageMap]="opponentBenchDamageMap()" [energyMap]="opponentBenchEnergyMap()" (openDetail)="onHandCardOpenDetail($event)" />
            </div>

            <div class="side-area side-area--right">
              <div class="side-stack">
                <div class="side-container side-container--opponent side-container--row">
                  <div class="deck-area">
                    @for (_ of [1,2,3]; track $index) {
                      <div
                        class="deck-card"
                        [style.transform]="'translateX(' + ($index * -2) + 'px) translateY(' + ($index * 2) + 'px) rotateY(' + (($index - 1) * 2) + 'deg)'"
                        [style.zIndex]="3 - $index"
                      >
                        @let oppBack = gameStateService.opponentCardBackUrl();
                        @if (oppBack) {
                          <img [src]="oppBack" class="card-back-img w-full h-full" />
                        } @else {
                          <div class="card-back"></div>
                        }
                      </div>
                    }
                  </div>

                  <div class="discard-area" (click)="onDiscardClick('opponent')" title="Ver descarte">
                    @if (opponentDiscard().length) {
                      @let dc = opponentDiscard()[opponentDiscard().length - 1];
                      <div class="discard-card">
                        @if (dc.imageUrl) {
                          <img [src]="dc.imageUrl" [alt]="dc.name" loading="lazy" class="w-full h-full object-cover block" />
                        } @else {
                          <div class="flex items-center justify-center text-[#8888aa] text-xs">?</div>
                        }
                      </div>
                    } @else {
                      <div class="discard-empty"></div>
                    }
                  </div>
                </div>
                <div class="side-counter side-counter--left">{{ opponentDeckCount() }}</div>
          </div>
</div>
        </div>

        <app-battle-board
            [opponentActive]="displayedOpponentActive()"
            [myActive]="displayedMyActive()"
            [highlightPlayerZone]="(isSetupPhase() && setupStep() === 'active') || activeHighlighted()"
            [canReceive]="hasSelectedBasicForActive()"
            [isDropTarget]="dropTargetZone() === 'active'"
            [showOpponentCardBack]="isSetupPhase() && !isSetupComplete()"
            [opponentCardBackUrl]="gameStateService.opponentCardBackUrl()"
            [playerCardBackUrl]="gameStateService.myCardBackUrl()"
            [opponentHover]="gameStateService.opponentHovering()"
            [attackLunge]="attackLunge()"
            [defenderShake]="defenderShake()"
            [boardBg]="boardBgType()"
            [hidePlayerActive]="retreatingMonKey() !== null"
            [activeStadium]="activeStadium()"
            [trainerDisplayCard]="trainerDisplayCard()"
            [trainerDisplayFading]="trainerDisplayFading()"
            [trainerSlotGlow]="trainerSlotGlow()"
            (openDetail)="onActivePokemonOpenDetail($event)"
            (openToolDetail)="onToolOpenDetail($event)"
            (trainerCardClicked)="onHandCardOpenDetail($event)"
            (zoneClicked)="onPlayerActiveZoneClick()" />

        <div class="battle-row battle-row--player">
            <div class="side-area side-area--left">
              <div class="side-stack">
                <div class="side-container side-container--player">
                  @if (myPrizeCount(); as count) {
                    <div class="prize-area" [class.prize-area--hidden]="isSetupPhase() || !prizesDealt()">
                      @for (_ of [0,1,2,3,4,5]; track $index) {
                        @if ($index < count) {
                          <div
                            class="prize-card"
                            [style.transform]="'translateX(' + ($index * 18) + 'px)'"
                          >
                            @let myBack = gameStateService.myCardBackUrl();
                            @if (myBack) {
                              <img [src]="myBack" class="card-back-img w-full h-full" />
                            } @else {
                              <div class="card-back"></div>
                            }
                          </div>
                        }
                      }
                    </div>
                  }
                </div>
                @if (myPrizeCount(); as count) {
                  <div class="side-counter side-counter--right">{{ count }}</div>
                }
              </div>
            </div>

            <div
              class="hand-area hand-area--player"
              [class.setup-highlight-bench]="isSetupPhase() && setupStep() === 'bench'"
              [class.is-drop-target]="(isSetupPhase() || benchDropGlow() || (draggedCard() && isBasicPokemon(draggedCard()!))) && (benchDropGlow() || dropTargetZone() === 'bench')"
              [class.can-receive]="isSetupPhase() && hasSelectedBasicForBench()"
              (click)="onPlayerBenchZoneClick()"
            >
              <app-table-cards [cards]="myBenchCards()" currentPhase="MAIN" [cardBackUrl]="gameStateService.myCardBackUrl()" [hpMap]="myBenchHpMap()" [damageMap]="myBenchDamageMap()" [hoverEnabled]="myBenchHoverEnabled()" [highlightedCardIds]="benchHighlightedCardIds()" [hoverTargetIds]="hoverTargetIds()" [evolvingCardId]="evolvingCardId()" [energyMap]="myBenchEnergyMap()" (openDetail)="onHandCardOpenDetail($event)" (cardSelected)="onBenchCardSelected($event)" />
            </div>

            <div class="side-area side-area--right">
              <div class="side-stack">
                <div class="side-container side-container--player side-container--row">
                  <div class="deck-area">
                    @for (_ of [1,2,3]; track $index) {
                      <div
                        class="deck-card"
                        [style.transform]="'translateX(' + ($index * -2) + 'px) translateY(' + ($index * 2) + 'px) rotateY(' + (($index - 1) * 2) + 'deg)'"
                        [style.zIndex]="3 - $index"
                      >
                        @let myBack = gameStateService.myCardBackUrl();
                        @if (myBack) {
                          <img [src]="myBack" class="card-back-img w-full h-full" />
                        } @else {
                          <div class="card-back"></div>
                        }
                      </div>
                    }
                  </div>

                  <div class="discard-area" (click)="onDiscardClick('my')" title="Ver descarte">
                    @if (myDiscard().length) {
                      @let dc = myDiscard()[myDiscard().length - 1];
                      <div class="discard-card">
                        @if (dc.imageUrl) {
                          <img [src]="dc.imageUrl" [alt]="dc.name" loading="lazy" class="w-full h-full object-cover block" />
                        } @else {
                          <div class="flex items-center justify-center text-[#8888aa] text-xs">?</div>
                        }
                      </div>
                    } @else {
                      <div class="discard-empty"></div>
                    }
                  </div>
                </div>
                <div class="side-counter side-counter--left">{{ myDeckCount() }}</div>
              </div>
            </div>
          </div>

          <div class="hand-cards-row hand-cards-row--player" [class.hand-cards-row--hidden]="!initialDealDone() || (isMulliganPlayer() && (dealingState() !== 'done' || mulliganDealState() === 'animating'))">
            <div class="player-cards-row">
            @for (group of groupedPlayerHand(); track group.name; let i = $index) {
              <app-hand-card
                [card]="group.cards[0]"
                [count]="group.count"
                [isSelected]="selectedCardId() === group.cards[0].id"
                [showSetupArrow]="playableCardIds().has(group.cards[0].id)"
                [glow]="playableCardIds().has(group.cards[0].id)"
                [style.animation-delay.ms]="i * 80"
                (select)="isSetupPhase() ? onSetupCardClick($event) : onHandCardSelect($event)"
                (openDetail)="onHandCardOpenDetail($event)"
                (dragStarted)="onDragStarted($event)"
                (dragMoved)="onDragMoved($event)"
                (dragEnded)="onDragEnded()"
              />
            }
            </div>
          </div>
        </div>

        <!-- Flying card for slot-to-overlay transition -->
        @if (attackFlyData(); as fly) {
          <div class="attack-fly-card"
            [style.left.px]="fly.left" [style.top.px]="fly.top"
            [style.width.px]="fly.width" [style.height.px]="fly.height"
            [style.opacity]="fly.opacity"
            (transitionend)="onAttackFlyTransitionEnd($event)">
            <img [src]="fly.imageUrl" [alt]="fly.name" />
          </div>
        }

        @let active = myActivePokemon();
        @if (isActiveExtended()) {
          <div class="attack-overlay" (click)="onOverlayClick($event)">
            <div class="attack-overlay__content"
                 (click)="$event.stopPropagation()"
                 [class.attack-overlay__content--visible]="attackOverlayOpen()">
              <div class="attack-overlay__title">Realizar ataque o habilidad</div>
              <div class="attack-overlay__body">
                <div class="attack-overlay__card">
                  <img [src]="active?.imageUrl" [alt]="active?.name" class="attack-overlay__img" />
                  <div class="attack-overlay__name">{{ active?.name }}</div>
                </div>
                <div class="attack-overlay__right">
                  <div class="attack-overlay__buttons">
                    <div class="attack-overlay__section-title">Habilidades</div>
                    @let abilities = activeAbilities();
                    @if (abilities.length > 0) {
                      @for (ability of abilities; track ability.name) {
                        @let abilityKey = (myActivePokemon()?.instanceId ?? '') + ':' + ability.name;
                        @let abilityUsed = gameStateService.abilitiesUsedThisTurn().includes(abilityKey);
                        <div class="attack-row">
                          <button class="attack-btn attack-btn--ability"
                            [disabled]="abilityUsed"
                            [title]="abilityUsed ? ('BATTLE.ABILITY_USED' | translate) : ''"
                            (click)="executeAbility(ability.name)">
                            <span class="attack-btn__name">{{ ability.name }}</span>
                          </button>
                          <button class="attack-info-btn" (click)="showAttackInfo(ability.name, '', [], ability.description, true)" title="Info">?</button>
                        </div>
                      }
                    } @else {
                      <div class="attack-overlay__section-text">No hay habilidad para aplicar</div>
                    }
                    <div class="attack-overlay__section-title">Ataques</div>
                    @for (item of attacksWithDisabled(); track item.attack.name; let i = $index) {
                      <div class="attack-row">
                        <button class="attack-btn"
                          (click)="executeAttack(item.attack.name)">
                          <span class="attack-btn__name">{{ item.attack.name }}</span>
                        </button>
                        <button class="attack-info-btn" (click)="showAttackInfo(item.attack.name, item.attack.damage ?? '', item.attack.cost, item.attack.description ?? '', false)" title="Info">?</button>
                      </div>
                    }
                  </div>
                  <div class="attack-overlay__actions">
                    @let retCost = active?.retreatCost ?? 0;
                    @if (retCost > 0) {
                      <button class="attack-btn attack-btn--retreat" (click)="handleRetreat()">
                        <span class="attack-btn__name">RETIRARSE</span>
                        <span class="attack-btn__cost">
                          @for (_ of [1,2,3,4]; track $index) {
                            @if ($index < retCost) {
                              <img [src]="energyIcon('Colorless')" class="attack-btn__energy-icon" />
                            }
                          }
                        </span>
                      </button>
                    }
                    <button class="attack-btn attack-btn--cancel" (click)="closeAttackOverlay()">Cancelar</button>
                  </div>
                </div>
              </div>
              <!-- Info popup -->
              @if (attackInfoPopup(); as info) {
                <div class="attack-info-popup" (click)="hideAttackInfo()">
                  <div class="attack-info-popup__content" (click)="$event.stopPropagation()">
                    <div class="attack-info-popup__title">{{ info.name }}</div>
                    @if (!info.isAbility) {
                      <div class="attack-info-popup__row">
                        <span class="attack-info-popup__label">Daño:</span>
                        <span class="attack-info-popup__value">{{ info.damage || '—' }}</span>
                      </div>
                      @if (info.cost && info.cost.length) {
                        <div class="attack-info-popup__row">
                          <span class="attack-info-popup__label">Costo:</span>
                          <span class="attack-info-popup__value">
                            @for (e of info.cost; track e) {
                              <img [src]="energyIcon(e)" class="attack-btn__energy-icon" />
                            }
                          </span>
                        </div>
                      }
                    }
                    @if (info.description) {
                      <div class="attack-info-popup__desc">{{ info.description }}</div>
                    }
                    <button class="attack-info-popup__close" (click)="hideAttackInfo()">Cerrar</button>
                  </div>
                </div>
              }
            </div>
          </div>
        }

        <!-- Drag ghost overlay -->
        @if (draggedCard(); as card) {
          <div
            class="drag-ghost"
            [class.is-snapping]="dragGhostSnapping()"
            [style.left.px]="dragPos().x"
            [style.top.px]="dragPos().y"
            [style.--drag-tilt-y]="dragTiltY() + 'deg'"
          >
            @if (card.imageUrl) {
              <img [src]="card.imageUrl" draggable="false" class="drag-ghost__img" />
            } @else {
              <div class="drag-ghost__placeholder">?</div>
            }
          </div>
        }

        <!-- PokeNav menu de batalla -->
        <app-pokenav-device
          [isOpen]="showMenuModal() && !showBattleOptions()"
          [battleCompact]="true"
          (resume)="continueGame()"
          (showOptions)="showMenuModal.set(false); showBattleOptions.set(true)"
          (surrender)="surrender()"
        ></app-pokenav-device>
        @if (showBattleOptions()) {
          <app-game-options (back)="showBattleOptions.set(false); showMenuModal.set(true)"></app-game-options>
        }

        <!-- Card detail modal -->
        <my-modal
          [open]="showCardDetailModal()"
          [title]="selectedCardForDetail()?.name ?? ''"
          variant="pixel"
          width="52rem"
          [background]="'#0d0f1a'"
          (close)="showCardDetailModal.set(false)"
          [bodyOverflow]="'visible'"
        >
          @if (selectedCardForDetail(); as card) {
            <div class="card-detail-layout grid grid-cols-2 gap-6 items-start">
              <div class="flex items-center justify-center">
                <div class="w-full max-w-[260px]">
                  <app-deck-card-detail [card]="$any(card)" [showCount]="false" />
                </div>
              </div>
              <div class="flex flex-col gap-3 text-white text-sm">
                <div><span class="font-bold text-[#00ccaa]">{{ 'MATCH.CARD_NAME' | translate }}</span> {{ card.name }}</div>
                <div><span class="font-bold text-[#00ccaa]">{{ 'MATCH.CARD_TYPE' | translate }}</span> {{ card.supertype }}</div>
                @if (card.rarity) {
                  <div><span class="font-bold text-[#00ccaa]">{{ 'MATCH.CARD_RARITY' | translate }}</span> {{ card.rarity }}</div>
                }
                @if (card.hp != null) {
                  <div>
                    <span class="font-bold text-[#00ccaa]">HP</span>
                    <span class="ml-1">{{ card.hp }}</span>
                  </div>
                }
                @if (card.pokemonType) {
                  <div>
                    <span class="font-bold text-[#00ccaa]">{{ 'MATCH.CARD_TYPE' | translate }}</span>
                    <img [src]="energyIcon(card.pokemonType)" class="inline w-5 h-5 ml-1 align-middle" />
                    {{ card.pokemonType }}
                  </div>
                }
                @if (card.supertype !== 'Pokemon' && card.subtypes?.length) {
                  <div>
                    <span class="font-bold text-[#00ccaa]">Tipo</span>
                    <span class="ml-1">{{ (card.subtypes ?? []).join(', ') }}</span>
                  </div>
                }
                @if (card.rules?.length) {
                  <div class="mt-1">
                    <div class="font-bold text-[#ff8844] mb-1">Efecto</div>
                    @for (rule of card.rules; track $index) {
                      <p class="text-[#ccccee] text-xs leading-relaxed mb-1">{{ rule }}</p>
                    }
                  </div>
                }
                @if (card.abilities?.length) {
                  <div class="mt-1">
                    <div class="font-bold text-[#cc66ff] mb-1">{{ 'MATCH.ABILITIES' | translate }}</div>
                    @for (ab of card.abilities; track ab.name) {
                      <div class="bg-[rgba(204,102,255,0.08)] border border-[rgba(204,102,255,0.2)] rounded p-2 mb-1">
                        <div class="font-semibold text-[#cc66ff] text-xs">{{ ab.name }}</div>
                        <div class="text-gray-300 text-xs mt-0.5">{{ ab.description }}</div>
                      </div>
                    }
                  </div>
                }
                @if (card.attacks?.length) {
                  <div class="mt-1">
                    <div class="font-bold text-[#ff8844] mb-1">{{ 'MATCH.ATTACKS' | translate }}</div>
                    @for (atk of card.attacks; track atk.name) {
                      <div class="bg-[rgba(255,136,68,0.08)] border border-[rgba(255,136,68,0.2)] rounded p-2 mb-1">
                        <div class="flex items-center gap-1 mb-1">
                          @for (e of atk.cost ?? []; track e) {
                            <img [src]="energyIcon(e)" class="w-4 h-4 object-contain" />
                          }
                          <span class="font-semibold text-white text-xs ml-1">{{ atk.name }}</span>
                          @if (atk.damage && atk.damage !== '0') {
                            <span class="text-[#ff8844] text-xs ml-auto font-bold">{{ atk.damage }}</span>
                          }
                        </div>
                        @if (atk.description) {
                          <div class="text-gray-300 text-xs">{{ atk.description }}</div>
                        }
                      </div>
                    }
                  </div>
                }
                @if (card.weaknesses?.length) {
                  <div>
                    <span class="font-bold text-[#ff4444]">{{ 'MATCH.WEAKNESS' | translate }}</span>
                    @for (w of card.weaknesses; track w.type) {
                      <span class="ml-2">
                        <img [src]="energyIcon(w.type)" class="inline w-4 h-4 align-middle" />
                        {{ w.value }}
                      </span>
                    }
                  </div>
                }
                @if (card.resistances?.length) {
                  <div>
                    <span class="font-bold text-[#44aaff]">{{ 'MATCH.RESISTANCE' | translate }}</span>
                    @for (r of card.resistances; track r.type) {
                      <span class="ml-2">
                        <img [src]="energyIcon(r.type)" class="inline w-4 h-4 align-middle" />
                        -{{ r.reduction }}
                      </span>
                    }
                  </div>
                }
                @if (card.retreatCost != null && card.retreatCost > 0) {
                  <div>
                    <span class="font-bold text-[#aaaaaa]">{{ 'MATCH.RETREAT' | translate }}</span>
                    @for (_ of [0,1,2,3,4].slice(0, card.retreatCost); track $index) {
                      <img [src]="energyIcon('Colorless')" class="inline w-4 h-4 ml-1 align-middle" />
                    }
                  </div>
                }
              </div>
            </div>
          }
        </my-modal>

        <!-- Discard pile modal -->
        <my-modal
          [open]="showDiscardModal()"
          [title]="discardModalTitle()"
          variant="pixel"
          width="60rem"
          [background]="'#0d0f1a'"
          (close)="closeDiscardModal()"
        >
          <div class="discard-modal-scroll">
            @if (discardModalCards().length === 0) {
              <p class="text-center text-[#8888aa] italic py-8">No hay cartas en el descarte</p>
            }
            <div class="discard-modal-grid">
              @for (card of discardModalCards(); track card.id; let i = $index) {
                <div class="discard-modal-card" (click)="onHandCardOpenDetail(card)">
                  <img [src]="card.imageUrl" [alt]="card.name" loading="lazy" class="discard-modal-img" />
                  <span class="discard-modal-name">{{ card.name }}</span>
                </div>
              }
            </div>
          </div>
        </my-modal>

        <!-- Prize animation overlay -->
        @if (prizeAnimPhase() === 'animating') {
          <div class="prize-anim-overlay">
            @for (idx of [0,1,2,3,4,5,6,7,8,9,10,11]; track idx) {
              <div
                class="prize-anim-card"
                [attr.data-idx]="idx"
              >
                <div class="card-back"></div>
              </div>
            }
          </div>
        }

        <!-- Turn notification -->
        @if (turnNotification(); as text) {
          <div class="turn-notification">
            <span class="turn-notification__text">{{ text }}</span>
          </div>
        }

        <!-- Damage float -->
        @if (damageFloat(); as df) {
          <div class="damage-float" [style.left.px]="df.x" [style.top.px]="df.y">
            -{{ df.amount }}
          </div>
        }

        <!-- Heal float -->
        @if (healFloat(); as hf) {
          <div class="damage-float heal-float" [style.left.px]="hf.x" [style.top.px]="hf.y">
            +{{ hf.amount }}
          </div>
        }

        <!-- Player-choice picker modal -->
        @let pickerCfg = pickerModalConfig();
        @if (pickerCfg) {
          <app-card-picker-modal
            [open]="true"
            [title]="pickerCfg.title"
            [prompt]="pickerCfg.prompt"
            [cards]="pickerCfg.cards"
            [minPick]="pickerCfg.minPick"
            [maxPick]="pickerCfg.maxPick"
            [allowReorder]="pickerCfg.allowReorder"
            (confirm)="onPlayerChoiceConfirm($event)"
            (cancel)="onPlayerChoiceCancel()" />
        }

        <!-- Coin side choice modal (Roller Skates) -->
        @if (showCoinChoiceModal() && coinChoicePendingType() === 'CHOOSE_COIN_SIDE') {
          <div class="coin-flip-overlay">
            <div class="coin-modal">
              <h3 style="color:#ffcc00;font-family:'Orbitron',monospace;font-size:1rem;margin-bottom:0.75rem">Roller Skates</h3>
              <p style="color:#aaaacc;font-size:0.85rem;margin-bottom:1rem">Elegí cara o cruz</p>
              <div class="flex gap-4 justify-center">
                <button class="picker__btn picker__btn--confirm" (click)="onCoinSideChosen('heads')" style="font-size:0.85rem;padding:0.5rem 1.5rem">🌕 Cara</button>
                <button class="picker__btn picker__btn--confirm" (click)="onCoinSideChosen('tails')" style="font-size:0.85rem;padding:0.5rem 1.5rem">🌑 Cruz</button>
              </div>
            </div>
          </div>
        }

        <!-- Coin-flip overlay -->
        @if (coinFlipOverlay(); as cf) {
          <div class="coin-flip-overlay">
            <div class="coin-flip-result" [class.heads]="cf.heads" [class.tails]="!cf.heads">
              <span class="coin-face">{{ cf.heads ? '🌕' : '🌑' }}</span>
              <span class="coin-label">{{ cf.heads ? ('BATTLE.COIN_HEADS' | translate) : ('BATTLE.COIN_TAILS' | translate) }}</span>
              <span class="coin-reason">{{ (cf.reason === 'burn' ? 'BATTLE.COIN_REASON_BURN' : cf.reason === 'sleep' ? 'BATTLE.COIN_REASON_SLEEP' : 'BATTLE.COIN_REASON_DEFAULT') | translate }}</span>
            </div>
          </div>
        }

        <!-- Attack flash -->
        @if (attackFlash(); as f) {
          <div class="attack-flash"
            [style.left.px]="f.x"
            [style.top.px]="f.y"
            [style.background]="'radial-gradient(circle, rgba(255,255,255,0.8) 0%, rgba(200,200,255,0.3) 50%, transparent 70%)'">
          </div>
        }

        <!-- Efecto de impacto por tipo -->
        @if (attackEffect(); as fx) {
          <div class="attack-fx" [style.left.px]="fx.x" [style.top.px]="fx.y">
            <app-attack-effect [type]="fx.type" [power]="fx.power" />
          </div>
        }

        <!-- Impact particles -->
        @for (p of impactParticles(); track $index) {
          <div class="attack-impact-particle"
            [style.left.px]="p.x"
            [style.top.px]="p.y"
            [style.background]="p.color"
            [style.--tx]="p.tx + 'px'"
            [style.--ty]="p.ty + 'px'"
            [style.animationDelay.ms]="p.delay">
          </div>
        }

        <!-- KO explosion -->
        @if (koExplosion(); as ko) {
          <div class="ko-overlay" [style.left.px]="ko.x" [style.top.px]="ko.y">
            @for (p of koParticles(); track $index) {
              <div class="ko-particle"
                [style.background]="p.color"
                [style.width.px]="p.size"
                [style.height.px]="p.size"
                [style.animationDelay.ms]="p.delay"
                [style.left.px]="p.x"
                [style.top.px]="p.y">
              </div>
            }
          </div>
        }

        <!-- End turn confirmation modal -->
        <my-confirm-modal
          [open]="showEndTurnConfirm()"
          [title]="'MATCH.END_TURN_TITLE' | translate"
          [message]="'MATCH.END_TURN_CONFIRM' | translate"
          [confirmText]="'MATCH.END_TURN_YES' | translate"
          [cancelText]="'MATCH.CANCEL' | translate"
          (confirm)="onConfirmEndTurn()"
          (cancel)="showEndTurnConfirm.set(false)"
        />

        <!-- Attack confirmation modal (non-scaling) -->
        <my-modal
          [open]="showAttackConfirm() && !showEnergyModal()"
          title="Confirmar Ataque"
          variant="pixel"
          width="28rem"
          [background]="'#0d0f1a'"
          (close)="cancelAttack()"
        >
          @let info = pendingAttackInfo();
          @if (info) {
            <div class="flex flex-col items-center gap-4 py-2">
              <div class="text-white font-bold text-lg font-['Orbitron']">{{ info.name }}</div>
              <div class="flex items-center gap-6 text-sm">
                <span class="text-[#ff7755] font-bold font-['Press_Start_2P'] text-xs">{{ info.damage ?? '—' }}</span>
                @if (info.cost && info.cost.length) {
                  <span class="flex gap-2 items-center">
                    @for (e of info.cost; track e) {
                      <img [src]="energyIcon(e)" class="w-5 h-5 object-contain" />
                    }
                  </span>
                }
              </div>
              <div class="flex gap-3 mt-2">
                <my-button text="Atacar" variant="pixel" size="small" (clicked)="confirmAttack()" />
                <my-button text="Cancelar" variant="pixel" size="small" color="var(--neon-magenta)" (clicked)="cancelAttack()" />
              </div>
            </div>
          }
        </my-modal>

        <!-- Flying energy card (for energy selection modal animation) -->
        @if (energyFlyCard(); as efc) {
          <div
            class="energy-fly-card"
            [style.left.px]="efc.left" [style.top.px]="efc.top"
            [style.width.px]="efc.width" [style.height.px]="efc.height"
            [style.opacity]="efc.opacity"
            (transitionend)="onEnergyFlyEnd($event)">
            <img [src]="efc.imageUrl || energyIcon(efc.type)" />
          </div>
        }

        <!-- Energy selection modal (scaling attacks) -->
        <my-modal
          [open]="showEnergyModal()"
          title="Seleccionar Energías"
          variant="pixel"
          width="80rem"
          [background]="'#0d0f1a'"
          (close)="cancelEnergyAttack()"
        >
          @let enerInfo = pendingAttackInfo();
          @if (enerInfo) {
            <div class="flex flex-col gap-3">
              <div class="text-center">
                <span class="text-white font-bold font-['Orbitron'] text-base">{{ enerInfo.name }}</span>
                <span class="text-[#ff7755] font-['Press_Start_2P'] text-xs ml-3">{{ enerInfo.damage ?? '—' }}</span>
              </div>
              <div class="flex gap-6 min-h-[220px]">
                <div class="flex-1 bg-black/30 rounded p-4 border border-white/10 energy-available-panel">
                  <div class="text-[#8888aa] font-['Orbitron'] text-[0.65rem] uppercase tracking-wider mb-3">Disponibles</div>
                  <div class="flex flex-wrap gap-3">
                    @for (group of availableEnergyOptions(); track group.type) {
                      <button class="energy-option-btn energy-card-btn"
                        (click)="selectEnergyFromGroup(group.type, $event)"
                        [title]="group.type">
                        <img [src]="group.imageUrl || energyIcon(group.type)" class="w-full h-full object-cover block" />
                        @if (group.count > 1) {
                          <span class="energy-option-count">{{ group.count }}</span>
                        }
                      </button>
                    }
                    @if (availableEnergyOptions().length === 0) {
                      <span class="text-[#666688] text-xs italic">Sin energías disponibles</span>
                    }
                  </div>
                </div>
                <div class="flex-1 bg-black/30 rounded p-4 border border-white/10">
                  <div class="text-[#8888aa] font-['Orbitron'] text-[0.65rem] uppercase tracking-wider mb-3">Seleccionadas</div>
                  <div class="flex flex-wrap gap-3 min-h-[60px] items-start content-start energy-selected-panel">
                    @for (item of selectedEnergiesList(); track item.idx) {
                      <button class="energy-option-btn energy-card-btn energy-option-btn--selected"
                        (click)="removeEnergyByIndex(item.idx, item.type, item.imageUrl, $event)"
                        [title]="'Quitar ' + item.type">
                        <img [src]="item.imageUrl || energyIcon(item.type)" class="w-full h-full object-cover block" />
                      </button>
                    }
                    @if (selectedEnergiesList().length === 0) {
                      <span class="text-[#666688] text-xs italic">Ninguna seleccionada</span>
                    }
                  </div>
                </div>
              </div>
              <div class="text-center text-xs font-['Orbitron'] text-[#8888aa]">
                Mínimo requerido: <span class="font-bold text-white">{{ minEnergyRequired() }}</span>
                &nbsp;|&nbsp; Seleccionadas: <span class="font-bold" [class.text-green-400]="canConfirmEnergy()" [class.text-red-400]="!canConfirmEnergy()">{{ selectedEnergyIndices().length }}</span>
              </div>
              <div class="flex justify-center gap-3 mt-1">
                <my-button
                  [text]="'Atacar'"
                  variant="pixel"
                  size="small"
                  [disabled]="!canConfirmEnergy()"
                  (clicked)="confirmEnergyAttack()"
                />
                <my-button
                  text="Cancelar"
                  variant="pixel"
                  size="small"
                  color="var(--neon-magenta)"
                  (clicked)="cancelEnergyAttack()"
                />
              </div>
            </div>
          }
        </my-modal>

        <!-- Prize selection modal -->
        @if (showPrizeModal() && prizeCards().length > 0) {
          <div class="prize-select-overlay">
            <div class="prize-select-modal">
              <div class="prize-select-title">Seleccioná una carta de premio</div>
              <div class="prize-select-grid">
                @for (card of prizeCards(); track $index; let i = $index) {
                  <button class="prize-card-btn"
                    (click)="onPrizeCardClick(i, card)"
                    [disabled]="prizeRevealAnim().phase !== 'idle'">
                    @let reveal = prizeRevealAnim();
                    @if (reveal.phase !== 'idle' && reveal.index === i) {
                      <div class="prize-card-inner prize-card-flipped">
                        <img [src]="card.imageUrl" [alt]="card.name" class="prize-card-img" />
                      </div>
                    } @else {
                      <div class="prize-card-inner">
                        <div class="card-back"></div>
                      </div>
                    }
                  </button>
                }
              </div>
              @if (prizeRevealAnim().phase === 'revealed') {
                <div class="prize-reveal-name">{{ prizeRevealAnim().cardName }} añadida a la mano</div>
              }
            </div>
          </div>
        }

        <!-- Retreat confirmation modal -->
        <my-confirm-modal
          [open]="showRetreatConfirm()"
          title="Retirar Pokémon"
          [message]="retreatMessage()"
          confirmText="Sí, retirar"
          cancelText="Cancelar"
          (confirm)="confirmRetreat()"
          (cancel)="cancelRetreat()"
        />

        <!-- Bench promote confirmation modal -->
        <my-confirm-modal
          [open]="showPromoteConfirm()"
          title="Nuevo Pokémon activo"
          [message]="promoteMessage()"
          confirmText="Sí, usar este"
          cancelText="Cancelar"
          (confirm)="confirmPromote()"
          (cancel)="cancelPromote()"
        />

        <!-- Opponent waiting messages -->
        @if (isOpponentPrizeSelection() && !showPrizeModal()) {
          <div class="waiting-overlay">
            <div class="waiting-text">El oponente está seleccionando cartas de premio...</div>
          </div>
        }
        @if (opponentIsChoosingActive()) {
          <div class="waiting-overlay">
            <div class="waiting-text">El oponente está eligiendo un nuevo Pokémon activo...</div>
          </div>
        }
        @if (needsBenchPromotion() && !showPromoteConfirm()) {
          <div class="waiting-overlay">
            <div class="waiting-text" style="color:#66aaff">Seleccioná un Pokémon de la banca para ser el nuevo activo</div>
          </div>
        }

        <!-- Opponent disconnected overlay -->
        @if (gameStateService.opponentDisconnected(); as dc) {
          <div class="disconnect-overlay">
            <div class="disconnect-content">
              <div class="disconnect-icon">🚨</div>
              <div class="disconnect-title">El oponente perdió la conexión</div>
              <div class="disconnect-timer">{{ dc.secondsRemaining }}s</div>
              <div class="disconnect-sub">Se rendirá automáticamente si no reconecta</div>
            </div>
          </div>
        }

        <!-- Victory / Defeat screen -->
        @if (isVictory() || isDefeat()) {
          <div class="result-overlay"
               [class.result-overlay--victory]="isVictory()"
               [class.result-overlay--defeat]="isDefeat()">
            <div class="result-glow"></div>
            <div class="result-particles" aria-hidden="true">
              @for (p of resultParticles; track $index) {
                <span class="result-particle"
                      [style.left.%]="p.left"
                      [style.animation-delay.s]="p.delay"
                      [style.animation-duration.s]="p.duration"
                      [style.width.px]="p.size"
                      [style.height.px]="p.size"
                      [style.--ember-rise.vh]="p.rise"
                      [style.--ember-drift.px]="p.drift"></span>
              }
            </div>
            <div class="result-content">
              <div class="result-title">{{ isVictory() ? '¡VICTORIA!' : 'DERROTA' }}</div>
              <div class="result-stats">
                <div class="stat"><span class="stat-label">Cartas jugadas</span><span class="stat-value">{{ resultStats().cardsPlayed }}</span></div>
                <div class="stat"><span class="stat-label">Carta más jugada</span><span class="stat-value">{{ resultStats().mostPlayedCard }}</span></div>
                <div class="stat"><span class="stat-label">Mazo utilizado</span><span class="stat-value">{{ resultStats().deckUsed }}</span></div>
                <div class="stat"><span class="stat-label">Ataques</span><span class="stat-value">{{ resultStats().attacks }}</span></div>
                <div class="stat"><span class="stat-label">Premios tomados</span><span class="stat-value">{{ resultStats().prizesTaken }}</span></div>
              </div>
              <div class="result-btn">
                <my-button text="Volver al lobby" variant="modern" size="large" (clicked)="goToLobby()" />
              </div>
            </div>
          </div>
        }

      }
      }
    </div>


  `,
})
export class BattlePage implements OnInit, OnDestroy {
  readonly gameStateService = inject(GameStateService);
  private readonly webSocketService = inject(WebSocketService);
  private readonly authService = inject(AuthService);
  private readonly router = inject(Router);
  private readonly route = inject(ActivatedRoute);
  private readonly sfx = inject(AudioSfxChannel);
  private readonly music = inject(AudioMusicChannel);
  private readonly translate = inject(TranslateService);
  private readonly cdr = inject(ChangeDetectorRef);
  private readonly ngZone = inject(NgZone);
  private readonly destroyRef = inject(DestroyRef);
  private readonly circleTransition = inject(CircleTransitionService);

  // ── Juez Mew (asistente) — sólo en partidas vs bot ───────────────────────
  readonly judgeMew = inject(JudgeMewService);
  private readonly judgeService = inject(JudgeService);
  private readonly rulesExplainer = inject(JudgeRulesExplainer);

  /** Una partida es vs bot cuando el id del oponente sigue la convención `bot-*`. */
  readonly isBotMatch = computed(() => this.opponentPlayerId().startsWith('bot-'));

  /** El juez asiste cuando: es vs bot, está habilitado (toggle) y la sesión está activa. */
  readonly judgeActive = computed(() =>
    this.isBotMatch() && this.judgeMew.enabled() && this.judgeMew.botSessionActive());

  private judgeStarted = false;
  private judgeResultDone = false;
  private judgeTipDone = false;
  private judgePrevMyTurn = false;
  private lastJudgeError: string | null = null;

  /**
   * Arranca la sesión del juez al entrar a una batalla vs bot. NO depende del toggle
   * `enabled`: la sesión queda activa y el toggle sólo controla la visibilidad, para que
   * activar/desactivar el juez a mitad de partida funcione al instante.
   */
  private readonly judgeStartEffect = effect(() => {
    if (this.isBotMatch() && this.canShowContent() && !this.judgeStarted) {
      this.judgeStarted = true;
      this.judgeMew.startBotSession();
    }
  });

  /** El juez reacciona al desenlace de la partida (festeja o consuela). */
  private readonly judgeResultEffect = effect(() => {
    const winnerId = this.gameStateService.matchState()?.winnerId;
    if (!winnerId || !this.judgeActive() || this.judgeResultDone) return;
    this.judgeResultDone = true;
    if (winnerId === this.myPlayerId()) {
      this.judgeMew.triggerCelebrate();
      this.judgeMew.showSpeech('¡Felicitaciones, entrenador! Ganaste la batalla. 🎉', 9000);
    } else {
      this.judgeMew.triggerRejected();
      this.judgeMew.showSpeech('Esta vez no se dio... ¡La próxima la ganás! 💪', 9000);
    }
  });

  /** Teletransporte al iniciar el turno del jugador + un consejo del BE la 1ª vez. */
  private readonly judgeTurnEffect = effect(() => {
    const myTurn = this.gameStateService.isMyTurn();
    const phase = this.gameStateService.currentPhase();
    if (this.judgeActive() && myTurn && !this.judgePrevMyTurn) {
      this.judgeMew.triggerTeleport();
      if (!this.judgeTipDone && phase === 'MAIN') {
        this.judgeTipDone = true;
        this.requestJudgeTip();
      }
    }
    this.judgePrevMyTurn = myTurn;
  });

  /** Función de asistente: explica (en el idioma del juego) por qué se rechazó una acción. */
  private readonly judgeErrorEffect = effect(() => {
    const err = this.gameStateService.actionError();
    if (this.judgeActive() && err && err !== this.lastJudgeError) {
      this.judgeMew.triggerRejected();
      this.judgeMew.showSpeech(this.rulesExplainer.explain(err), 9000);
    }
    this.lastJudgeError = err;
  });

  /** Consulta proactiva al juez del BE: muestra las restricciones del turno actual. */
  private requestJudgeTip(): void {
    const matchId = this.lobbyMatchId();
    if (!matchId) return;
    const lang: 'es' | 'en' = this.translate.currentLang === 'en' ? 'en' : 'es';
    this.judgeMew.triggerThinking();
    this.judgeService.query(matchId, { category: JudgeQueryCategory.CURRENT_RESTRICTIONS, lang })
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: res => {
          this.judgeMew.triggerApproved();
          this.judgeMew.showSpeech(res.suggestion ?? res.summary, 9000);
        },
        error: () => this.judgeMew.backToIdle(),
      });
  }

  private timers: ReturnType<typeof setTimeout>[] = [];

  private addTimer(id: ReturnType<typeof setTimeout>): void {
    this.timers.push(id);
  }

  private clearAllTimers(): void {
    for (const id of this.timers) {
      clearTimeout(id);
    }
    this.timers = [];
  }

  readonly loading = this.gameStateService.loading;
  private readonly minLoadingElapsed = signal(false);
  private readonly MIN_LOADING_MS = 10000;
  readonly canShowContent = computed(() => !this.loading() && this.minLoadingElapsed());
  readonly error = this.gameStateService.error;

  readonly dealingState = signal<'idle' | 'requested' | 'dealing' | 'done'>('idle');
  readonly mulliganDealState = signal<'idle' | 'animating' | 'done'>('idle');

  readonly selectedCardId = signal<string | null>(null);
  readonly selectedCard = signal<MatchCardView | null>(null);
  readonly selectedCardForDetail = signal<MatchCardView | null>(null);
  readonly showCardDetailModal = signal(false);

  readonly myHand = this.gameStateService.myHand;
  readonly myPrizeCount = this.gameStateService.myPrizeCount;
  readonly opponentPrizeCount = this.gameStateService.opponentPrizeCount;
  readonly myActivePokemon = this.gameStateService.myActivePokemon;
  readonly opponentActivePokemon = this.gameStateService.opponentActivePokemon;
  readonly displayedOpponentActive = signal<InPlayPokemonView | null>(null);
  readonly displayedMyActive = signal<InPlayPokemonView | null>(null);

  readonly boardBgType = computed(() => {
    const my = this.displayedMyActive();
    if (my?.pokemonType) return my.pokemonType;
    const oppVisible = !this.isSetupPhase() || this.gameStateService.isSetupComplete();
    if (oppVisible) {
      const opp = this.opponentActivePokemon();
      if (opp?.pokemonType) return opp.pokemonType;
    }
    return null;
  });

  readonly opponentHandCount = this.gameStateService.opponentHandCount;

  readonly myMulliganCount = computed(() =>
    this.gameStateService.matchState()?.myState?.mulliganCount ?? 0);

  readonly opponentMulliganCount = computed(() =>
    this.gameStateService.matchState()?.opponentState?.mulliganCount ?? 0);

  readonly opponentHandCards = computed<MatchCardView[]>(() =>
    Array.from({ length: this.gameStateService.opponentHandCount() }, (_, i) => ({
      id: `opp-${i}`,
      name: '',
    }))
  );

  readonly myTableCards = signal<MatchCardView[]>([]);
  readonly opponentTableCards = signal<MatchCardView[]>([]);

  // Placement animation
  readonly placementAnim = signal<{ startX: number; startY: number; endX: number; endY: number; isBack: boolean; imageUrl?: string } | null>(null);
  readonly placementAnimActive = signal(false);

  // Retreat / promote card fly animation (DOM-managed, no signal)

  private benchKey(p: InPlayPokemonView): string {
    return p.instanceId ?? p.cardId ?? `bench-${p.name}`;
  }

  readonly myBenchCards = computed<MatchCardView[]>(() =>
    this.gameStateService.myBench()
      .filter((p): p is NonNullable<typeof p> => p !== null)
      .map(p => ({
        id: this.benchKey(p),
        name: p.name,
        instanceId: p.instanceId,
        imageUrl: p.imageUrl,
        supertype: 'Pokemon',
        subtypes: ['Basic'],
      }))
  );

  readonly myBenchHpMap = computed(() => {
    const map = new Map<string, number>();
    for (const p of this.gameStateService.myBench()) {
      if (p && (p.cardId || p.instanceId)) {
        map.set(this.benchKey(p), p.maxHp);
      }
    }
    return map;
  });

  readonly opponentBenchHpMap = computed(() => {
    const map = new Map<string, number>();
    for (const p of this.gameStateService.opponentBench()) {
      if (p && (p.cardId || p.instanceId)) {
        map.set(this.benchKey(p), p.maxHp);
      }
    }
    return map;
  });

  readonly isActiveExtended = signal(false);
  readonly attackOverlayOpen = signal(false);
  readonly attackFlyData = signal<AttackFlyData | null>(null);
  readonly attackFlyPhase = signal<'entering' | 'returning' | null>(null);
  private attackFlyOriginRect: DOMRect | null = null;

  readonly attackInfoPopup = signal<{
    name: string; damage: string; cost?: string[]; description: string; isAbility: boolean;
  } | null>(null);

  showAttackInfo(name: string, damage: string, cost: string[] | undefined, description: string, isAbility: boolean): void {
    this.attackInfoPopup.set({ name, damage, cost, description, isAbility });
  }

  hideAttackInfo(): void {
    this.attackInfoPopup.set(null);
  }

  // Confirm/energy selection modals for attacks
  readonly showAttackConfirm = signal(false);
  readonly showEnergyModal = signal(false);
  readonly pendingAttackName = signal<string | null>(null);
  readonly selectedEnergyIndices = signal<number[]>([]);

  readonly availableEnergyOptions = signal<Array<{ type: string; count: number; cardIds: string[]; imageUrl: string }>>([]);

  private recalculateEnergyOptions(): void {
    const active = this.myActivePokemon();
    if (!active) {
      this.availableEnergyOptions.set([]);
      return;
    }
    const used = new Set(this.selectedEnergyIndices());
    const typeMap = new Map<string, { count: number; cardIds: string[]; imageUrl: string }>();
    const types = active.attachedEnergies ?? [];
    const ids = active.attachedEnergyCardIds ?? [];
    const imgs = active.attachedEnergyImageUrls ?? [];
    for (let i = 0; i < types.length; i++) {
      if (used.has(i)) continue;
      const entry = typeMap.get(types[i]);
      if (entry) {
        entry.count++;
        entry.cardIds.push(ids[i]);
      } else {
        typeMap.set(types[i], { count: 1, cardIds: [ids[i]], imageUrl: imgs[i] ?? '' });
      }
    }
    const result: Array<{ type: string; count: number; cardIds: string[]; imageUrl: string }> = [];
    for (const [type, info] of typeMap) {
      result.push({ type, count: info.count, cardIds: info.cardIds, imageUrl: info.imageUrl });
    }
    this.availableEnergyOptions.set(result);
  }

  readonly selectedEnergiesList = computed(() => {
    const active = this.myActivePokemon();
    if (!active) return [];
    const selected = this.selectedEnergyIndices();
    const types = active.attachedEnergies ?? [];
    const ids = active.attachedEnergyCardIds ?? [];
    const imgs = active.attachedEnergyImageUrls ?? [];
    return selected.map(idx => ({
      idx,
      cardId: ids[idx] ?? '',
      type: types[idx] ?? 'Colorless',
      imageUrl: imgs[idx] ?? '',
    }));
  });

  readonly minEnergyRequired = computed(() => {
    const name = this.pendingAttackName();
    if (!name) return 0;
    const attack = this.myActivePokemon()?.attacks?.find(a => a.name === name);
    return attack?.cost?.length ?? 0;
  });

  readonly canConfirmEnergy = computed(() => {
    return this.selectedEnergyIndices().length >= this.minEnergyRequired();
  });

  readonly pendingAttackInfo = computed(() => {
    const name = this.pendingAttackName();
    if (!name) return null;
    const attack = this.myActivePokemon()?.attacks?.find(a => a.name === name);
    return attack ?? null;
  });

  readonly energyFlyCard = signal<{
    left: number; top: number; width: number; height: number;
    type: string; cardId: string; imageUrl: string; opacity: number;
  } | null>(null);

  readonly isFirstTurn = computed(() =>
    this.gameStateService.matchState()?.firstTurn ?? false);

  // Prize / KO / Victory signals
  readonly showPrizeModal = signal(false);
  readonly prizeCards = computed(() => this.gameStateService.myPrizeCards());
  readonly isMyPrizeSelection = computed(() => this.gameStateService.isMyPrizeSelection());
  readonly isOpponentPrizeSelection = computed(() => this.gameStateService.isOpponentPrizeSelection());
  readonly isVictory = computed(() => this.gameStateService.isVictory());
  readonly isDefeat = computed(() => this.gameStateService.isDefeat());
  readonly needsBenchPromotion = computed(() => this.gameStateService.needsBenchPromotion());
  readonly opponentIsChoosingActive = computed(() => this.gameStateService.opponentIsChoosingActive());

  readonly resultStats = computed(() => {
    const st = this.gameStateService.matchState();
    const myId = this.myPlayerId();
    const log = st?.matchLog ?? [];
    const myEntries = log.filter(e => e.playerId === myId);
    const playTypes = new Set([
      'PLAY_BASIC', 'EVOLVE', 'PLAY_ITEM', 'PLAY_SUPPORTER',
      'PLAY_TOOL', 'PLAY_STADIUM', 'ATTACH_ENERGY',
    ]);
    const played = myEntries.filter(e => playTypes.has(e.actionType));
    const counts = new Map<string, number>();
    for (const e of played) {
      const name = e.description || '—';
      counts.set(name, (counts.get(name) ?? 0) + 1);
    }
    let mostPlayed = '—';
    let mostCount = 0;
    for (const [name, c] of counts) {
      if (c > mostCount) {
        mostCount = c;
        mostPlayed = name;
      }
    }
    const user = this.authService.currentUser();
    const deck = user?.decks?.find(d => d.id === user.activeDeckId);
    return {
      cardsPlayed: played.length,
      mostPlayedCard: mostPlayed,
      attacks: myEntries.filter(e => e.actionType === 'ATTACK').length,
      prizesTaken: myEntries.filter(e => e.actionType === 'TAKE_PRIZE').length,
      deckUsed: deck?.name ?? 'Mazo',
    };
  });

  readonly resultParticles = Array.from({ length: 28 }, () => ({
    left: Math.random() * 100,
    delay: Math.random() * 5,
    duration: 5 + Math.random() * 4,
    rise: 30 + Math.random() * 55,
    drift: (Math.random() > 0.5 ? 1 : -1) * (10 + Math.random() * 30),
    size: 4 + Math.random() * 3,
  }));

  readonly playableCardIds = signal<Set<string>>(new Set());

  readonly prizeRevealAnim = signal<{
    index: number;
    cardName: string;
    imageUrl: string;
    phase: 'idle' | 'revealing' | 'revealed' | 'adding';
  }>({ index: -1, cardName: '', imageUrl: '', phase: 'idle' });

  /** Retraso para modales post-KO (esperar a que terminen las animaciones). */
  readonly delayedPrizeSelection = signal(false);
  readonly prizeRequestPending = signal(false);
  readonly retreatingMonKey = signal<string | null>(null);
  readonly processingAttack = signal(false);
  readonly showHistory = signal(false);
  readonly matchHistory = signal<MatchLogEntry[]>([]);
  private lastOpponentAnimData: { x: number; y: number; type: string | undefined; hp: number } | null = null;
  private lastMyAnimData: { x: number; y: number; type: string | undefined; hp: number } | null = null;

  readonly evolvesFrom = signal<string | null>(null);

  // Damage / heal float effects
  readonly damageFloat = signal<{ amount: number; x: number; y: number } | null>(null);
  readonly healFloat = signal<{ amount: number; x: number; y: number } | null>(null);

  // Coin-flip overlay (shown when backend sends a COIN_FLIP event)
  readonly coinFlipOverlay = signal<{ heads: boolean; reason: string } | null>(null);
  readonly koExplosion = signal<{ x: number; y: number; type: string } | null>(null);
  readonly koParticles = signal<Array<{ x: number; y: number; dx: number; dy: number; color: string; delay: number; size: number }>>([]);
  private prevOpponentHp = 0;

  private readonly ATTACK_TYPE_COLORS: Record<string, string[]> = {
    Fire: ['#ff4400', '#ff8800', '#ffcc00', '#ff2200'],
    Water: ['#0088ff', '#44aaff', '#00ccff', '#0066cc'],
    Grass: ['#44cc44', '#88ee44', '#22aa22', '#66ff44'],
    Lightning: ['#ffdd00', '#ffee44', '#ffcc00', '#ffff66'],
    Psychic: ['#cc44ff', '#ee88ff', '#aa22dd', '#ff66ff'],
    Fighting: ['#cc8844', '#aa6633', '#eeaa55', '#884422'],
    Darkness: ['#442266', '#664488', '#8855aa', '#331155'],
    Metal: ['#aaaacc', '#ccccdd', '#8888aa', '#ddddee'],
    Fairy: ['#ff88cc', '#ffaadd', '#ff66bb', '#ffccee'],
  };
  private readonly DEFAULT_KO_COLORS = ['#cccccc', '#eeeeee', '#aaaaaa', '#ffffff'];

  getKoColors(type: string | undefined): string[] {
    return this.ATTACK_TYPE_COLORS[type ?? ''] ?? this.DEFAULT_KO_COLORS;
  }

  readonly evolvingCardId = signal<string | null>(null);
  private prevBenchBySlot = new Map<number, string>();

  private readonly evolutionFlashEffect = effect(() => {
    const bench = this.gameStateService.myBench();
    const currentBySlot = new Map<number, string>();

    for (let i = 0; i < bench.length; i++) {
      const p = bench[i];
      if (p && (p.cardId || p.instanceId)) {
        currentBySlot.set(i, this.benchKey(p));
      }
    }

    let evolved: string | null = null;
    for (const [slot, key] of currentBySlot) {
      const prevKey = this.prevBenchBySlot.get(slot);
      if (prevKey && prevKey !== key) {
        evolved = key;
        break;
      }
    }

    if (evolved && this.prevBenchBySlot.size > 0 && !this.isSetupPhase()) {
      this.evolvingCardId.set(evolved);
      this.addTimer(setTimeout(() => this.evolvingCardId.set(null), 800));
    }

    this.prevBenchBySlot = currentBySlot;
  });

  readonly activeAttacks = computed(() => this.myActivePokemon()?.attacks ?? []);
  readonly activeAbilities = computed<AbilityView[]>(() =>
    this.myActivePokemon()?.abilities?.filter(a => a.type === 'ACTIVATED') ?? []);

  canPayEnergyCost(cost: string[], attached: string[]): boolean {
    const upperCost = cost.map(e => e.toUpperCase());
    const remaining = [...attached.map(e => e.toUpperCase())];
    for (const required of upperCost.filter(e => e !== 'COLORLESS')) {
      const idx = remaining.indexOf(required);
      if (idx === -1) return false;
      remaining.splice(idx, 1);
    }
    const colorlessNeeded = upperCost.filter(e => e === 'COLORLESS').length;
    return remaining.length >= colorlessNeeded;
  }

  energyIcon(type: string): string {
    const name = type.charAt(0).toUpperCase() + type.slice(1).toLowerCase();
    return `/assets/energies/${name}.png`;
  }

  energyColor(type: string): string {
    const colors: Record<string, string> = {
      COLORLESS: '#a8a878', FIRE: '#f08030', WATER: '#6890f0',
      LIGHTNING: '#f8d030', GRASS: '#78c850', PSYCHIC: '#f85888',
      FIGHTING: '#c03028', DARKNESS: '#705848', METAL: '#b8b8d0',
      DRAGON: '#7038f8', FAIRY: '#ee99ac',
    };
    return colors[type.toUpperCase()] ?? '#a8a878';
  }

  readonly attacksWithDisabled = computed(() =>
    this.activeAttacks().map(attack => ({
      attack,
      disabled: !this.canPayEnergyCost(attack.cost ?? [], this.myActivePokemon()?.attachedEnergies ?? []),
    })),
  );

  onActivePokemonClick(): void {
    if (!this.gameStateService.isMyTurn() || this.gameStateService.currentPhase() === 'SETUP') return;
    if (this.gameStateService.currentPhase() !== 'MAIN') return;

    if (this.attackOverlayOpen() || this.isActiveExtended()) {
      this.closeAttackOverlay();
      return;
    }

    const active = this.myActivePokemon();
    if (!active) return;

    // If a matching evolution card is selected in hand, evolve the active instead of opening attacks.
    const handCard = this.selectedCard();
    if (handCard && this.isEvolutionCard(handCard) && this.canEvolveInto(active.name, handCard)) {
      this.turnActionService.evolve(handCard.id, active.instanceId ?? active.cardId ?? '');
      this.sfx.play('PonerCarta.mp3');
      this.selectedCardId.set(null);
      this.selectedCard.set(null);
      return;
    }

    const slotCard = document.querySelector('.board-zone--player .slot-card') as HTMLElement;
    if (!slotCard) return;

    this.attackFlyOriginRect = slotCard.getBoundingClientRect();

    this.isActiveExtended.set(true);
    this.attackOverlayOpen.set(false);
    this.cdr.markForCheck();

    const matchId = this.gameStateService.matchId();
    if (matchId) {
      this.webSocketService.publish(`/app/matches/${matchId}/pokemon-hover`, {});
    }

    this.addTimer(setTimeout(() => {
      const targetEl = document.querySelector('.attack-overlay__img') as HTMLElement;
      if (!targetEl || !this.attackFlyOriginRect) {
        this.attackOverlayOpen.set(true);
        return;
      }

      const targetRect = targetEl.getBoundingClientRect();
      const origin = this.attackFlyOriginRect;

      this.attackFlyData.set({
        left: origin.left,
        top: origin.top,
        width: origin.width,
        height: origin.height,
        imageUrl: active.imageUrl ?? '',
        name: active.name,
        opacity: 1,
      });
      this.attackFlyPhase.set('entering');

      requestAnimationFrame(() => {
        this.attackFlyData.set({
          left: targetRect.left,
          top: targetRect.top,
          width: targetRect.width,
          height: targetRect.height,
          imageUrl: active.imageUrl ?? '',
          name: active.name,
          opacity: 1,
        });
      });
    }));
  }

  onOverlayClick(event: MouseEvent): void {
    const target = event.target as HTMLElement;
    if (!target.closest('.attack-overlay__content')) {
      this.closeAttackOverlay();
    }
  }

  closeAttackOverlay(): void {
    this.showAttackConfirm.set(false);
    this.showEnergyModal.set(false);
    this.pendingAttackName.set(null);
    this.selectedEnergyIndices.set([]);
    this.recalculateEnergyOptions();
    this.energyFlyCard.set(null);

    const hoverMatchId = this.gameStateService.matchId();
    if (hoverMatchId) {
      this.webSocketService.publish(`/app/matches/${hoverMatchId}/pokemon-unhover`, {});
    }

    if (!this.isActiveExtended()) return;

    const origin = this.attackFlyOriginRect;
    const active = this.myActivePokemon();
    if (!origin || !active) {
      this.attackOverlayOpen.set(false);
      this.attackFlyData.set(null);
      this.isActiveExtended.set(false);
      this.attackFlyPhase.set(null);
      this.attackFlyOriginRect = null;
      this.cdr.markForCheck();
      return;
    }

    this.attackOverlayOpen.set(false);
    this.cdr.markForCheck();

    this.addTimer(setTimeout(() => {
      const imgEl = document.querySelector('.attack-overlay__img') as HTMLElement;
      const originRect = this.attackFlyOriginRect;
      if (!imgEl || !originRect) {
        this.attackFlyData.set(null);
        this.isActiveExtended.set(false);
        this.attackFlyPhase.set(null);
        this.attackFlyOriginRect = null;
        this.cdr.markForCheck();
        return;
      }

      const currentRect = imgEl.getBoundingClientRect();

      this.attackFlyPhase.set('returning');
      this.attackFlyData.set({
        left: currentRect.left,
        top: currentRect.top,
        width: currentRect.width,
        height: currentRect.height,
        imageUrl: active.imageUrl ?? '',
        name: active.name,
        opacity: 1,
      });

      requestAnimationFrame(() => {
        this.attackFlyData.set({
          left: originRect.left,
          top: originRect.top,
          width: originRect.width,
          height: originRect.height,
          imageUrl: active.imageUrl ?? '',
          name: active.name,
          opacity: 1,
        });
      });
    }));
  }

  onAttackFlyTransitionEnd(event: TransitionEvent): void {
    if (event.propertyName !== 'left') return;

    const phase = this.attackFlyPhase();
    this.attackFlyPhase.set(null);

    if (phase === 'entering') {
      this.attackOverlayOpen.set(true);
      this.attackFlyData.set(null);
      this.cdr.markForCheck();
    } else if (phase === 'returning') {
      this.isActiveExtended.set(false);
      this.attackFlyData.set(null);
      this.attackFlyOriginRect = null;
      this.cdr.markForCheck();
    }
  }

  private readonly toastService = inject(ToastService);

  /** Check if an attack has scaling damage (10×, 30+, etc). */
  private isScalingDamage(attackName: string): boolean {
    const attack = this.myActivePokemon()?.attacks?.find(a => a.name === attackName);
    if (!attack?.damage) return false;
    return attack.damage.includes('×') || attack.damage.includes('+');
  }

  private autoSelectEnergyIds(cost: string[], types: string[], ids: string[]): string[] {
    const used = new Set<number>();
    const selected: string[] = [];
    for (const required of cost.filter(e => e !== 'Colorless')) {
      const idx = types.findIndex((t, i) => t === required && !used.has(i));
      if (idx !== -1) {
        used.add(idx);
        selected.push(ids[idx]);
      }
    }
    const colorlessNeeded = cost.filter(e => e === 'Colorless').length;
    for (let i = 0; i < types.length && selected.length < cost.length; i++) {
      if (!used.has(i)) {
        used.add(i);
        selected.push(ids[i]);
      }
    }
    return selected;
  }

  confirmAttack(): void {
    const active = this.myActivePokemon();
    const name = this.pendingAttackName();
    if (!active || !name) return;
    const cost = active.attacks?.find(a => a.name === name)?.cost ?? [];
    const types = active.attachedEnergies ?? [];
    const ids = active.attachedEnergyCardIds ?? [];
    const selectedIds = this.autoSelectEnergyIds(cost, types, ids);
    this.closeAttackOverlay();
    this.showAttackConfirm.set(false);
    this.pendingAttackName.set(null);
    this.processingAttack.set(true);
    this.storeOpponentAnimData();
    const power = parseInt(active.attacks?.find(a => a.name === name)?.damage ?? '0', 10) || 0;
    this.addTimer(setTimeout(() => {
      this.turnActionService.executeAttack(name, selectedIds);
    }, 500));
    this.addTimer(setTimeout(() => this.processingAttack.set(false), 3000));
  }

  private storeOpponentAnimData(): void {
    const opp = this.opponentActivePokemon();
    if (!opp) return;
    const slotEl = document.querySelector('.board-zone--opponent .slot-card') as HTMLElement;
    const rect = slotEl?.getBoundingClientRect();
    this.lastOpponentAnimData = {
      x: rect ? rect.left + rect.width / 2 : 0,
      y: rect ? rect.top : 0,
      type: opp.pokemonType ?? undefined,
      hp: opp.currentHp,
    };
  }

  private storeMyAnimData(): void {
    const my = this.myActivePokemon();
    if (!my) return;
    const slotEl = document.querySelector('.board-zone--player .slot-card') as HTMLElement;
    const rect = slotEl?.getBoundingClientRect();
    this.lastMyAnimData = {
      x: rect ? rect.left + rect.width / 2 : 0,
      y: rect ? rect.top : 0,
      type: my.pokemonType ?? undefined,
      hp: my.currentHp,
    };
  }

  cancelAttack(): void {
    this.showAttackConfirm.set(false);
    this.pendingAttackName.set(null);
  }

  toggleEnergyIndex(idx: number): void {
    const current = this.selectedEnergyIndices();
    const pos = current.indexOf(idx);
    if (pos !== -1) {
      this.selectedEnergyIndices.set([...current.slice(0, pos), ...current.slice(pos + 1)]);
    } else {
      this.selectedEnergyIndices.set([...current, idx]);
    }
    this.recalculateEnergyOptions();
  }

  selectEnergyFromGroup(groupType: string, event: Event): void {
    const active = this.myActivePokemon();
    const types = active?.attachedEnergies ?? [];
    if (!active) return;
    const used = new Set(this.selectedEnergyIndices());
    let targetIdx = -1;
    for (let i = 0; i < types.length; i++) {
      if (types[i] === groupType && !used.has(i)) {
        targetIdx = i;
        break;
      }
    }
    if (targetIdx === -1) return;

    const ids = active.attachedEnergyCardIds ?? [];
    const imgs = active.attachedEnergyImageUrls ?? [];

    this.toggleEnergyIndex(targetIdx);
    this.cdr.markForCheck();

    const btnEl = (event.currentTarget as HTMLElement)?.closest('button');
    const srcRect = btnEl?.getBoundingClientRect();
    if (!srcRect) return;

    const selPanel = document.querySelector('.energy-selected-panel');
    const selCount = this.selectedEnergyIndices().length;
    const slotSize = 65;
    const pads = 16;
    const perRow = Math.floor((selPanel?.clientWidth ?? 300) / slotSize);
    const col = (selCount - 1) % perRow;
    const row = Math.floor((selCount - 1) / perRow);
    const panelRect = selPanel?.getBoundingClientRect();
    if (!panelRect) return;

    const targetLeft = panelRect.left + pads + col * slotSize;
    const targetTop = panelRect.top + 32 + row * slotSize;

    this.energyFlyCard.set({
      left: srcRect.left, top: srcRect.top,
      width: 55, height: 77,
      type: groupType, cardId: ids[targetIdx], imageUrl: imgs[targetIdx] || '', opacity: 1,
    });

    setTimeout(() => {
      this.energyFlyCard.set({
        left: targetLeft, top: targetTop,
        width: 55, height: 77,
        type: groupType, cardId: ids[targetIdx], imageUrl: imgs[targetIdx] || '', opacity: 1,
      });
    }, 20);
  }

  removeEnergyByIndex(idx: number, type: string, imageUrl: string, event: Event): void {
    this.toggleEnergyIndex(idx);
    this.cdr.markForCheck();

    const btnEl = (event.currentTarget as HTMLElement)?.closest('button');
    const srcRect = btnEl?.getBoundingClientRect();
    if (!srcRect) return;

    const availPanel = document.querySelector('.energy-available-panel');
    const availRect = availPanel?.getBoundingClientRect();
    if (!availRect) return;

    const targetLeft = availRect.left + 16;
    const targetTop = availRect.top + 32;

    this.energyFlyCard.set({
      left: srcRect.left, top: srcRect.top,
      width: 55, height: 77,
      type, cardId: `idx-${idx}`, imageUrl: imageUrl || '', opacity: 1,
    });

    setTimeout(() => {
      this.energyFlyCard.set({
        left: targetLeft, top: targetTop,
        width: 55, height: 77,
        type, cardId: `idx-${idx}`, imageUrl: imageUrl || '', opacity: 1,
      });
    }, 20);
  }

  onEnergyFlyEnd(event: TransitionEvent): void {
    if (event.propertyName !== 'left') return;
    this.energyFlyCard.set(null);
  }

  onPrizeCardClick(index: number, card: MatchCardView): void {
    if (this.prizeRevealAnim().phase !== 'idle') return;

    this.prizeRevealAnim.set({ index, cardName: card.name, imageUrl: card.imageUrl ?? '', phase: 'revealing' });

    this.addTimer(setTimeout(() => {
      this.prizeRevealAnim.set({ index, cardName: card.name, imageUrl: card.imageUrl ?? '', phase: 'revealed' });
    }, 600));

    const flyDelay = setTimeout(() => {
      // Capture rect from the side prize area (not the modal)
      const sidePrizeEls = document.querySelectorAll('.side-area--left .prize-card');
      const lastPrizeEl = sidePrizeEls[sidePrizeEls.length - 1] as HTMLElement;
      const prizeRect = lastPrizeEl?.getBoundingClientRect();

      this.prizeRevealAnim.set({ index, cardName: card.name, imageUrl: card.imageUrl ?? '', phase: 'adding' });
      this.showPrizeModal.set(false);
      this.prizeRequestPending.set(true);

      const inner = setTimeout(() => {
        this.triggerCardFlyFromRect(prizeRect, '.hand-cards-row--player', card.imageUrl ?? '', () => {
          this.gameStateService.optimisticallyAddToHand(card);
          const matchId = this.gameStateService.matchId();
          if (matchId) {
            this.webSocketService.publish(`/app/matches/${matchId}/select-prize`, { prizeIndex: index });
          }
          this.addTimer(setTimeout(() => {
            this.prizeRequestPending.set(false);
            this.prizeRevealAnim.set({ index: -1, cardName: '', imageUrl: '', phase: 'idle' });
          }, 500));
        });
      }, 100);
      this.addTimer(inner);
    }, 2000);
    this.addTimer(flyDelay);
  }

  readonly showRetreatConfirm = signal(false);
  readonly retreatMessage = signal('');

  readonly showPromoteConfirm = signal(false);
  readonly pendingPromoteSlot = signal<number | null>(null);
  readonly promoteMessage = signal('');

  onBenchCardForPromote(benchSlot: number, monName: string): void {
    this.pendingPromoteSlot.set(benchSlot);
    this.promoteMessage.set(`¿Usar ${monName} como nuevo Pokémon activo?`);
    this.showPromoteConfirm.set(true);
  }

  confirmPromote(): void {
    const slot = this.pendingPromoteSlot();
    if (slot === null) return;
    this.showPromoteConfirm.set(false);
    this.pendingPromoteSlot.set(null);
    const benchMon = this.gameStateService.myBench()[slot];
    const imageUrl = benchMon?.imageUrl ?? '';
    const fromSel = '.hand-area--player';
    const toSel = '.battle-board .board-zone--player';
    this.triggerCardFly(fromSel, toSel, imageUrl, () => {
      const matchId = this.gameStateService.matchId();
      if (matchId) {
        this.webSocketService.publish(`/app/matches/${matchId}/promote-bench`, { benchSlot: slot });
      }
    });
  }

  cancelPromote(): void {
    this.showPromoteConfirm.set(false);
    this.pendingPromoteSlot.set(null);
  }

  confirmEnergyAttack(): void {
    const name = this.pendingAttackName();
    if (!name) return;
    const active = this.myActivePokemon();
    const ids = active?.attachedEnergyCardIds ?? [];
    const selectedCardIds = this.selectedEnergyIndices().map(idx => ids[idx]);
    this.closeAttackOverlay();
    this.showEnergyModal.set(false);
    this.pendingAttackName.set(null);
    this.selectedEnergyIndices.set([]);
    this.processingAttack.set(true);
    this.storeOpponentAnimData();
    const power = parseInt(active?.attacks?.find(a => a.name === name)?.damage ?? '0', 10) || 0;
    this.addTimer(setTimeout(() => {
      this.turnActionService.executeAttack(name, selectedCardIds);
    }, 500));
    this.addTimer(setTimeout(() => this.processingAttack.set(false), 3000));
  }

  cancelEnergyAttack(): void {
    this.showEnergyModal.set(false);
    this.pendingAttackName.set(null);
    this.selectedEnergyIndices.set([]);
    this.recalculateEnergyOptions();
  }

  executeAttack(attackName: string): void {
    if (this.isFirstTurn()) {
      this.toastService.show('No puedes atacar en el primer turno', 'error');
      return;
    }

    const active = this.myActivePokemon();
    const attack = active?.attacks?.find(a => a.name === attackName);
    if (!attack) return;

    const enough = this.canPayEnergyCost(attack.cost ?? [], active?.attachedEnergies ?? []);
    if (!enough) {
      this.toastService.show('¡Energía insuficiente para este ataque!', 'error');
      return;
    }

    this.pendingAttackName.set(attackName);

    if (!this.isScalingDamage(attackName)) {
      this.showAttackConfirm.set(true);
      return;
    }

    this.selectedEnergyIndices.set([]);
    this.recalculateEnergyOptions();
    this.showEnergyModal.set(true);
  }

  executeAbility(abilityName: string): void {
    const active = this.myActivePokemon();
    const instanceId = active?.instanceId;
    if (!instanceId) {
      this.toastService.show('No hay Pokémon activo', 'error');
      return;
    }
    this.turnActionService.activateAbility(instanceId, abilityName);
  }

  // ---- Player-choice modal wiring ----

  readonly pickerModalConfig = computed(() => {
    const choice = this.gameStateService.pendingPlayerChoice();
    if (!choice) return null;
    if (choice.type === 'COIN_FLIP_INFO' || choice.type === 'CHOOSE_COIN_SIDE') return null;
    const titleKey = this.titleKeyForChoice(choice.type);
    return {
      title: this.translate.instant(titleKey),
      prompt: choice.prompt ?? null,
      cards: choice.options,
      minPick: choice.minPick,
      maxPick: choice.maxPick,
      allowReorder: choice.type === 'REORDER_DECK_TOP_N',
    };
  });

  private titleKeyForChoice(type: string): string {
    switch (type) {
      case 'PICK_FROM_DECK_TOP_N': return 'BATTLE.CHOOSE_POKEMON';
      case 'PICK_BASIC_ENERGIES_FROM_DECK': return 'BATTLE.CHOOSE_ENERGIES';
      case 'PICK_BENCHED_POKEMON': return 'BATTLE.CHOOSE_BENCHED_POKEMON';
      case 'REORDER_DECK_TOP_N': return 'BATTLE.REORDER_CARDS';
      case 'PICK_FROM_DISCARD': return 'BATTLE.CHOOSE_POKEMON';
      default: return 'BATTLE.CHOOSE_POKEMON';
    }
  }

  private readonly coinChoiceEffect = effect(() => {
    const choice = this.gameStateService.pendingPlayerChoice();
    if (choice?.type === 'CHOOSE_COIN_SIDE') {
      this.coinChoicePendingType.set('CHOOSE_COIN_SIDE');
      this.showCoinChoiceModal.set(true);
    }
  });

  private readonly autoResolveCoinFlipEffect = effect(() => {
    const choice = this.gameStateService.pendingPlayerChoice();
    if (choice?.type === 'COIN_FLIP_INFO') {
      // Let the existing coin-flip overlay (from CoinFlipGameEvent) play, then auto-acknowledge.
      this.addTimer(setTimeout(() => {
        this.turnActionService.resolvePlayerChoice([]);
      }, 2500));
    }
  });

  onPlayerChoiceConfirm(chosen: string[]): void {
    this.turnActionService.resolvePlayerChoice(chosen);
  }

  onPlayerChoiceCancel(): void {
    this.turnActionService.resolvePlayerChoice([]);
  }

  onCoinSideChosen(side: 'heads' | 'tails'): void {
    this.showCoinChoiceModal.set(false);
    this.coinChoicePendingType.set('');
    this.turnActionService.resolvePlayerChoice([side]);
  }

  handleRetreat(): void {
    const active = this.myActivePokemon();
    if (!active) return;
    const bench = this.gameStateService.myBench();
    const benchCount = bench.filter(Boolean).length;
    if (benchCount < 1) {
      this.toastService.show('No hay Pokémon en banca para reemplazar al activo', 'error');
      return;
    }
    if ((active.attachedEnergies?.length ?? 0) < (active.retreatCost ?? 0)) {
      this.toastService.show('Energía insuficiente para retirarse', 'error');
      return;
    }
    this.retreatMessage.set(`¿Retirar a ${active.name}?`);
    this.showRetreatConfirm.set(true);
  }

  confirmRetreat(): void {
    const active = this.myActivePokemon();
    if (!active) return;
    this.showRetreatConfirm.set(false);
    this.closeAttackOverlay();
    this.retreatingMonKey.set(this.benchKey(active));
    this.addTimer(setTimeout(() => {
      this.triggerCardFly('.board-zone--player', '.hand-area--player', active.imageUrl ?? '', () => {
        this.turnActionService.retreat(0);
      });
    }, 800));
  }

  cancelRetreat(): void {
    this.showRetreatConfirm.set(false);
    this.retreatingMonKey.set(null);
  }

  readonly myBenchDamageMap = computed(() => {
    const map = new Map<string, number>();
    for (const p of this.gameStateService.myBench()) {
      if (p && (p.cardId || p.instanceId)) {
        map.set(this.benchKey(p), Math.max(0, p.maxHp - p.currentHp));
      }
    }
    return map;
  });

  readonly opponentBenchDamageMap = computed(() => {
    const map = new Map<string, number>();
    for (const p of this.gameStateService.opponentBench()) {
      if (p && (p.cardId || p.instanceId)) {
        map.set(this.benchKey(p), Math.max(0, p.maxHp - p.currentHp));
      }
    }
    return map;
  });

  readonly opponentBenchEnergyMap = computed(() => {
    const map = new Map<string, string[]>();
    for (const p of this.gameStateService.opponentBench()) {
      if (p && (p.cardId || p.instanceId)) map.set(this.benchKey(p), p.attachedEnergies);
    }
    return map;
  });

  readonly opponentBenchCards = computed<MatchCardView[]>(() => {
    const hideCards = this.isSetupPhase() && !this.isSetupComplete();
    return this.gameStateService.opponentBench()
      .filter((p): p is NonNullable<typeof p> => p !== null)
      .map(p => ({
        id: this.benchKey(p),
        name: p.name,
        instanceId: p.instanceId,
        imageUrl: hideCards ? undefined : p.imageUrl,
        supertype: 'Pokemon',
        subtypes: ['Basic'],
      }));
  });

  readonly myDeckCount = computed(() => {
    const hand = this.gameStateService.myHand().length;
    const active = this.gameStateService.myActivePokemon() ? 1 : 0;
    const bench = this.gameStateService.myBench().filter(Boolean).length;
    const prizes = this.gameStateService.myPrizeCount();
    const discard = this.myDiscard().length;
    return 60 - hand - active - bench - prizes - discard;
  });
  readonly opponentDeckCount = computed(() => {
    const hand = this.gameStateService.opponentHandCount();
    const active = this.gameStateService.opponentActivePokemon() ? 1 : 0;
    const bench = this.gameStateService.opponentBench().filter(Boolean).length;
    const prizes = this.gameStateService.opponentPrizeCount();
    const discard = this.opponentDiscard().length;
    return 60 - hand - active - bench - prizes - discard;
  });
  readonly myDiscard = signal<MatchCardView[]>([]);
  readonly opponentDiscard = signal<MatchCardView[]>([]);
  readonly showDiscardModal = signal(false);
  readonly discardModalCards = signal<MatchCardView[]>([]);
  readonly discardModalTitle = signal('');
  readonly showCoinChoiceModal = signal(false);
  readonly coinChoicePendingType = signal<string>('');

  readonly activeStadium = this.gameStateService.activeStadium;
  readonly trainerDisplayCard = signal<{ card: MatchCardView; playerId: string } | null>(null);
  readonly trainerDisplayFading = signal(false);
  private trainerDisplayTimer: ReturnType<typeof setTimeout> | null = null;

  showTrainerCard(card: MatchCardView, playerId: string): void {
    if (this.trainerDisplayTimer) {
      clearTimeout(this.trainerDisplayTimer);
    }
    this.trainerDisplayFading.set(false);
    this.trainerDisplayCard.set({ card, playerId });
    this.trainerDisplayTimer = setTimeout(() => {
      this.trainerDisplayFading.set(true);
      const fadeTimer = setTimeout(() => {
        this.trainerDisplayCard.set(null);
        this.trainerDisplayFading.set(false);
      }, 500);
      this.addTimer(fadeTimer);
    }, 4500);
    this.addTimer(this.trainerDisplayTimer);
  }

  readonly showMenuModal = signal(false);
  readonly showReactions = signal(false);
  readonly showBattleOptions = signal(false);
  readonly reactions = ['\uD83D\uDC4D', '\uD83D\uDC4E', '\uD83D\uDE04', '\uD83D\uDD25', '\uD83D\uDCAA', '\uD83C\uDF89'];

  readonly openingActionLoading = signal(false);
  readonly lobbyMatchId = signal<string | null>(null);
  readonly setupTransitioning = signal(false);
  readonly prizesDealt = signal(false);
  readonly prizeAnimPhase = signal<'idle' | 'animating' | 'done'>('idle');
  readonly turnNotification = signal<string | null>(null);
  readonly showEndTurnConfirm = signal(false);
  readonly selectedBenchCardId = signal<string | null>(null);
  readonly initialDealDone = signal(false);
  private prevTurnPlayerId: string | null = null;
  private musicFadeStarted = false;
  private readonly turnActionService = inject(TurnActionService);

  readonly setupStep = this.gameStateService.setupStep;

  readonly setupCountdown = signal(40);
  private setupTimer: ReturnType<typeof setInterval> | null = null;
  private setupDeadlineAt = 0;
  private setupStartSent = false;
  readonly setupCountdownActive = signal(false);

  readonly setupBannerText = computed(() => {
    const step = this.setupStep();
    if (step === 'active') return this.translate.instant('BATTLE.SETUP_PLACE_ACTIVE');
    if (step === 'bench') return this.translate.instant('BATTLE.SETUP_PLACE_BENCH');
    if (step === 'waiting') return this.translate.instant('BATTLE.SETUP_WAITING_OPPONENT');
    return '';
  });

  readonly canConfirmSetup = computed(() => this.setupStep() === 'bench' || this.setupStep() === 'active');

  readonly hasSelectedBasicForActive = computed(() => {
    if (this.setupStep() !== 'active') return false;
    const selectedId = this.selectedCardId();
    if (!selectedId) return false;
    const card = this.myHand().find(c => c.id === selectedId);
    return !!card && this.isBasicPokemon(card);
  });

  readonly hasSelectedBasicForBench = computed(() => {
    if (this.setupStep() !== 'bench') return false;
    const selectedId = this.selectedCardId();
    if (!selectedId) return false;
    const card = this.myHand().find(c => c.id === selectedId);
    return !!card && this.isBasicPokemon(card);
  });

  canPlayCardDuringTurn(card: MatchCardView): boolean {
    if (this.isSetupPhase()) {
      return this.isBasicPokemon(card) && !this.mySetupReady();
    }
    if (this.gameStateService.isMyTurn() && this.gameStateService.currentPhase() === 'MAIN') {
      if (this.isBasicPokemon(card)) {
        const bench = this.gameStateService.myBench().filter(Boolean);
        return bench.length < 5;
      }
      if (this.isEvolutionCard(card)) {
        const active = this.myActivePokemon();
        if (active && this.canEvolveInto(active.name, card)) return true;
        return this.gameStateService.myBench().some(p => p && this.canEvolveInto(p.name, card));
      }
      if (this.isEnergyCard(card) && !this.gameStateService.hasAttachedEnergyThisTurn()) {
        return this.hasMatchingEnergyTarget(card);
      }
      if ((card.supertype ?? '').toLowerCase() === 'trainer') {
        return true;
      }
    }
    return false;
  }

  private computePlayableIds(): Set<string> {
    const set = new Set<string>();
    if (this.isSetupPhase() && this.mySetupReady()) return set;
    if (!this.isSetupPhase() && !(this.gameStateService.isMyTurn() && this.gameStateService.currentPhase() === 'MAIN')) return set;
    for (const card of this.myHand()) {
      if (this.canPlayCardDuringTurn(card)) set.add(card.id);
    }
    return set;
  }

  isEnergyCard(card: MatchCardView): boolean {
    return (card.supertype ?? '').toLowerCase() === 'energy';
  }

  isEvolutionCard(card: MatchCardView): boolean {
    const subs = card.subtypes ?? [];
    return subs.some(s => s === 'Stage 1' || s === 'Stage 2');
  }

  canEvolveInto(benchName: string, handCard: MatchCardView): boolean {
    return handCard.evolvesFrom === benchName;
  }

  private getBenchSlot(cardId: string): number {
    for (const p of this.gameStateService.myBench()) {
      if (p && p.cardId === cardId) {
        return p.slot ?? 0;
      }
    }
    return 0;
  }

  private hasMatchingEnergyTarget(energyCard: MatchCardView): boolean {
    const energyName = this.resolveEnergyName(energyCard).toUpperCase();
    const active = this.gameStateService.myActivePokemon();
    const bench = this.gameStateService.myBench();
    if (active && this.pokemonMatchesEnergy(active, energyName)) return true;
    return bench.some(p => p !== null && this.pokemonMatchesEnergy(p!, energyName));
  }

  private pokemonMatchesEnergy(pokemon: InPlayPokemonView, energyName: string): boolean {
    const pkmEnergyType = this.resolvePokemonEnergyType(pokemon);
    return pkmEnergyType === energyName;
  }

  private resolvePokemonEnergyType(pokemon: InPlayPokemonView): string {
    return (pokemon.pokemonType ?? '').toUpperCase();
  }

  private readonly dealRequestEffect = effect(() => {
    const matchId = this.lobbyMatchId();
    if (!matchId) return;
    if (!this.isOpeningComplete()) return;
    if (this.dealingState() !== 'idle') return;

    let alreadyDealt = false;
    try {
      alreadyDealt = localStorage.getItem('matchDealt_' + matchId) === '1';
    } catch {}
    if (alreadyDealt) {
      this.dealingState.set('done');
      this.initialDealDone.set(true);
      return;
    }

    if (this.myHand().length > 0) {
      this.dealingState.set('dealing');
      return;
    }

    this.dealingState.set('requested');
    this.webSocketService.publish(`/app/matches/${matchId}/request-deal`, {});
  });

  /** Dispara la animaciÃ³n de re-deal por mulligan despuÃ©s del reparto inicial. */
  private readonly mulliganDealEffect = effect(() => {
    if (!this.lobbyMatchId()) return;
    if (this.dealingState() !== 'done') return;
    if (!this.isMulliganPlayer() && !this.isOpponentMulliganPlayer()) return;
    if (this.mulliganIteration() <= 0) return;
    if (this.mulliganDealState() !== 'idle') return;

    this.mulliganDealState.set('animating');
  });

  /** Hace fadeOut de la mÃºsica de cuenta regresiva cuando aparece el overlay de la moneda. */
  private readonly openingMusicFadeEffect = effect(() => {
    if (this.canShowContent() && this.showOpeningOverlay() && !this.musicFadeStarted) {
      this.musicFadeStarted = true;
      this.music.fadeOut(3000);
    }
  });

  /** Avisa al backend que el setup ya es accionable (reparto + mulligans terminados). */
  private readonly setupStartEffect = effect(() => {
    const inSetup = this.isSetupPhase();
    const dealDone = this.dealingState() === 'done';
    const mulliganDone = this.mulliganDealState() !== 'animating';
    const step = this.setupStep();
    const ready = this.mySetupReady();
    if (inSetup && dealDone && mulliganDone && (step === 'active' || step === 'bench') && !ready && !this.setupStartSent) {
      this.setupStartSent = true;
      this.turnActionService.startSetupCountdown();
    }
  });

  /** Timer de 40s para el setup (activo/banca). Compartido entre ambos pasos. */
  private readonly setupCountdownEffect = effect(() => {
    const step = this.setupStep();
    const ready = this.mySetupReady();
    const inSetup = this.isSetupPhase();
    const dealDone = this.dealingState() === 'done';
    const mulliganDone = this.mulliganDealState() !== 'animating';
    const remainingMs = this.gameStateService.matchState()?.setupRemainingMs ?? 0;
    if (inSetup && dealDone && mulliganDone && (step === 'active' || step === 'bench') && !ready && remainingMs > 0) {
      this.ensureSetupCountdown(remainingMs);
    } else {
      this.stopSetupCountdown();
    }
  });

  /** Si no quedan cartas para poner en banca, se activa "Listo" directamente. */
  private readonly benchAutoReadyEffect = effect(() => {
    if (this.setupStep() !== 'bench') return;
    if (this.mySetupReady()) return;
    const hasBenchable = this.myHand().some(c => this.isBasicPokemon(c));
    const benchFull = this.gameStateService.myBench().filter(Boolean).length >= 5;
    if (!hasBenchable || benchFull) {
      this.onSetupReady();
    }
  });

  /** Detecta cuando el setup termina para animar los premios y mostrar el turno. */
  private readonly setupTransitionEffect = effect(() => {
    const wasSetup = this.isSetupPhase();
    const setupComplete = this.gameStateService.isSetupComplete();

    if (!wasSetup && setupComplete && this.prizeAnimPhase() === 'idle') {
      this.triggerPrizeAnimation();
    }
  });

  /** Abre modal de selección de premio cuando el backend lo indica. */
  private readonly prizeSelectionEffect = effect(() => {
    if (this.isMyPrizeSelection() && this.prizeRevealAnim().phase === 'idle' && !this.prizeRequestPending()) {
      this.delayedPrizeSelection.set(true);
      this.addTimer(setTimeout(() => {
        this.delayedPrizeSelection.set(false);
        this.showPrizeModal.set(true);
        this.prizeRevealAnim.set({ index: -1, cardName: '', imageUrl: '', phase: 'idle' });
      }, 4000));
    }
    if (!this.isMyPrizeSelection() && this.showPrizeModal()) {
      this.showPrizeModal.set(false);
      this.prizeRequestPending.set(false);
      this.prizeRevealAnim.set({ index: -1, cardName: '', imageUrl: '', phase: 'idle' });
    }
  });

  /** Sincroniza los signals de descarte con el estado del juego. */
  private readonly discardSyncEffect = effect(() => {
    const st = this.gameStateService.matchState();
    this.myDiscard.set(st?.myState?.discard ?? []);
    this.opponentDiscard.set(st?.opponentState?.discard ?? []);
  });

  /** Sincroniza el historial de la partida desde el estado. */
  private readonly historySyncEffect = effect(() => {
    const st = this.gameStateService.matchState();
    const log = st?.matchLog ?? [];
    const currentLen = this.matchHistory().length;
    if (log.length > currentLen) {
      const newEntries = log.slice(currentLen);
      this.matchHistory.update(h => [...h, ...newEntries]);
    }
  });

  readonly groupedHistory = computed(() => {
    const maxTurn = this.gameStateService.matchState()?.currentTurnNumber ?? 0;
    const log = this.matchHistory();
    const st = this.gameStateService.matchState();
    const myId = this.myPlayerId();
    const myName = st?.myState?.trainerName;
    const oppName = st?.opponentState?.trainerName;
    const groups: { turnNumber: number; entries: MatchLogEntry[]; playerName?: string }[] = [];
    for (let t = 1; t <= maxTurn; t++) {
      const turnEntries = log.filter(e => e.turnNumber === t);
      const firstEntry = turnEntries[0];
      let playerName: string | undefined;
      if (firstEntry) {
        playerName = firstEntry.playerId === myId
          ? (myName ?? 'Tú')
          : (oppName ?? 'Rival');
      }
      groups.push({ turnNumber: t, entries: turnEntries, playerName });
    }
    return groups;
  });

  /** Resetea retreatingMonKey cuando ya no se necesita promoción de banca. */
  private readonly retreatResetEffect = effect(() => {
    if (!this.gameStateService.needsBenchPromotion()) {
      this.retreatingMonKey.set(null);
    }
  });

  /**
   * Muestra un toast cuando el backend rechaza una acción. En partidas vs bot con
   * el juez activo NO se muestra el toast: el aviso lo dice Mew (ver judgeErrorEffect).
   */
  private readonly actionErrorEffect = effect(() => {
    const msg = this.gameStateService.actionError();
    if (msg) {
      if (!this.judgeActive()) {
        this.toastService.show(msg, 'error');
      }
      this.gameStateService.actionError.set(null);
    }
  });

  /** Sincroniza el overlay de desconexión y fuerza detección de cambios con OnPush. */
  private readonly opponentDisconnectedEffect = effect(() => {
    this.gameStateService.opponentDisconnected();
    this.cdr.markForCheck();
  });

  private opponentDisplayTimer: ReturnType<typeof setTimeout> | null = null;
  private myDisplayTimer: ReturnType<typeof setTimeout> | null = null;

  /** Retiene la imagen del oponente visible por ~3s tras KO para que las animaciones se vean. */
  private readonly opponentDisplayEffect = effect(() => {
    const mon = this.opponentActivePokemon();
    if (mon) {
      this.displayedOpponentActive.set(mon);
      if (this.opponentDisplayTimer) {
        clearTimeout(this.opponentDisplayTimer);
        this.opponentDisplayTimer = null;
      }
    } else if (this.prevOpponentHp !== 0 && !this.opponentDisplayTimer) {
      this.opponentDisplayTimer = setTimeout(() => {
        this.displayedOpponentActive.set(null);
        this.opponentDisplayTimer = null;
      }, 3000);
      this.addTimer(this.opponentDisplayTimer);
    }
  });

  /** Retiene la imagen de nuestro activo visible por ~3s tras KO. */
  private readonly myDisplayEffect = effect(() => {
    const mon = this.myActivePokemon();
    if (mon) {
      this.displayedMyActive.set(mon);
      if (this.myDisplayTimer) {
        clearTimeout(this.myDisplayTimer);
        this.myDisplayTimer = null;
      }
    } else if (this.prevMyHp !== 0 && !this.myDisplayTimer) {
      this.myDisplayTimer = setTimeout(() => {
        this.displayedMyActive.set(null);
        this.myDisplayTimer = null;
      }, 3000);
      this.addTimer(this.myDisplayTimer);
    }
  });

  /** Muestra flotante de daño cuando el oponente recibe daño (con delay). */
  private readonly damageFloatEffect = effect(() => {
    const mon = this.opponentActivePokemon();
    const curr = mon?.currentHp ?? 0;
    if (!this.isSetupComplete() || this.attackOverlayOpen()) return;

    // Detect damage or KO
    const isKo = this.prevOpponentHp !== 0 && (!mon && curr === 0);
    if (this.prevOpponentHp !== 0 && (curr < this.prevOpponentHp || isKo)) {
      this.processingAttack.set(true);
      this.addTimer(setTimeout(() => this.processingAttack.set(false), 3000));
      const dmg = this.prevOpponentHp - curr;
      const animData = mon
        ? (() => {
            const slotEl = document.querySelector('.board-zone--opponent .slot-card') as HTMLElement;
            const rect = slotEl?.getBoundingClientRect();
            return rect ? { x: rect.left + rect.width / 2, y: rect.top, type: mon.pokemonType ?? undefined } : null;
          })()
        : this.lastOpponentAnimData;

      if (animData) {
          this.addTimer(setTimeout(() => {
            const atkType = this.myActivePokemon()?.pokemonType ?? animData.type;
            // El efecto del jugador ya lo dispara confirmAttack (optimista); acá solo
            // mostramos el número de daño real + KO para evitar el doble movimiento.
            this.damageFloat.set({ amount: dmg, x: animData.x, y: animData.y });
            if (curr <= 0 || isKo) {
              this.addTimer(setTimeout(() => {
                this.triggerKoExplosion(animData.x, animData.y, atkType);
              }, 800));
            }
            this.addTimer(setTimeout(() => this.damageFloat.set(null), 2000));
          }, 500));
      }
    }
    if (mon) {
      this.prevOpponentHp = curr;
    } else if (this.prevOpponentHp !== 0) {
      this.addTimer(setTimeout(() => {
        this.prevOpponentHp = 0;
        this.lastOpponentAnimData = null;
      }, 3000));
    }
  });

  private prevMyHp = 0;

  /** Muestra flotante de daño cuando nuestro Pokémon recibe daño (oponente ataca). */
  private readonly myDamageEffect = effect(() => {
    const mon = this.myActivePokemon();
    const curr = mon?.currentHp ?? 0;
    if (!this.isSetupComplete()) return;

    // Detect damage or KO on our side
    const isKo = this.prevMyHp !== 0 && (!mon && curr === 0);
    if (this.prevMyHp !== 0 && (curr < this.prevMyHp || isKo)) {
      this.processingAttack.set(true);
      this.addTimer(setTimeout(() => this.processingAttack.set(false), 3000));
      const dmg = this.prevMyHp - curr;
      const animData = mon
        ? (() => {
            this.storeMyAnimData();
            const slotEl = document.querySelector('.board-zone--player .slot-card') as HTMLElement;
            const rect = slotEl?.getBoundingClientRect();
            return rect ? { x: rect.left + rect.width / 2, y: rect.top, type: mon.pokemonType ?? undefined } : null;
          })()
        : this.lastMyAnimData;

      if (animData) {
        this.addTimer(setTimeout(() => {
          const atkType = this.opponentActivePokemon()?.pokemonType ?? animData.type;
          this.triggerAttackEffects('opponent', atkType);
          this.damageFloat.set({ amount: dmg, x: animData.x, y: animData.y });
          if (curr <= 0 || isKo) {
            this.addTimer(setTimeout(() => {
              this.triggerKoExplosion(animData.x, animData.y, atkType);
            }, 800));
          }
          this.addTimer(setTimeout(() => this.damageFloat.set(null), 2000));
        }, 500));
      }
    }
    if (mon) {
      this.prevMyHp = curr;
    } else if (this.prevMyHp !== 0) {
      this.addTimer(setTimeout(() => {
        this.prevMyHp = 0;
        this.lastMyAnimData = null;
      }, 3000));
    }
  });

  /** Muestra flotante verde cuando el HP de nuestro Pokémon activo sube (curación). */
  private readonly myHealEffect = effect(() => {
    const mon = this.myActivePokemon();
    if (!mon || !this.isSetupComplete()) return;
    const curr = mon.currentHp;
    if (this.prevMyHp !== 0 && curr > this.prevMyHp) {
      const healed = curr - this.prevMyHp;
      const slotEl = document.querySelector('.board-zone--player .slot-card') as HTMLElement;
      const rect = slotEl?.getBoundingClientRect();
      if (rect) {
        this.addTimer(setTimeout(() => {
          this.healFloat.set({ amount: healed, x: rect.left + rect.width / 2, y: rect.top });
          this.addTimer(setTimeout(() => this.healFloat.set(null), 2000));
        }, 300));
      }
    }
  });

  /** Reacts to granular game events from the backend event channel. */
  private readonly gameEventEffect = effect(() => {
    const envelope = this.webSocketService.lastGameEvent();
    if (!envelope) return;

    this.handleGameEvent(envelope);
  });

  private handleGameEvent(envelope: GameEventEnvelope): void {
    const payload = envelope.payload as Record<string, unknown>;

    switch (envelope.type) {
      case 'COIN_FLIP': {
        const heads = payload['heads'] as boolean;
        const reason = payload['reason'] as string;
        this.coinFlipOverlay.set({ heads, reason });
        this.addTimer(setTimeout(() => this.coinFlipOverlay.set(null), 2500));
        break;
      }
      case 'STATUS_APPLIED': {
        const pokemonName = payload['pokemonName'] as string;
        const condition = payload['condition'] as string;
        const conditionLabel = this.statusLabel(condition);
        this.toastService.show(`${pokemonName} quedó ${conditionLabel}`, 'info');
        break;
      }
      case 'TRAINER_EFFECT': {
        const cardId = payload['cardId'] as string;
        const opponentId = this.opponentPlayerId();
        // Show feedback when the *opponent* played a trainer (our own play already shows a toast in tryPlayTrainerCard)
        const desc = getTrainerDescription(cardId);
        if (desc && opponentId) {
          this.toastService.show(`Rival: ${desc}`, 'info');
        }
        break;
      }
      case 'ABILITY_ACTIVATED': {
        const cardName = payload['cardName'] as string;
        const abilityName = payload['abilityName'] as string;
        this.toastService.show(`${cardName}: ${abilityName}`, 'info');
        break;
      }
      default:
        break;
    }
  }

  private statusLabel(condition: string): string {
    const key = `BATTLE.STATUS_${condition}`;
    const translated = this.translate.instant(key);
    return translated !== key ? translated : condition;
  }

  // Hover/unhover controlado por eventos OPPONENT_HOVER / OPPONENT_UNHOVER desde el backend

  /** Señal para la embestida de ataque: 'player' | 'opponent' | null. */
  readonly attackLunge = signal<'player' | 'opponent' | null>(null);
  readonly defenderShake = signal<'player' | 'opponent' | null>(null);

  /** Señal para el flash de impacto. */
  readonly attackFlash = signal<{ x: number; y: number } | null>(null);

  /** Efecto de impacto por tipo (posicionado sobre el defensor). */
  readonly attackEffect = signal<{ type: string | null; x: number; y: number; power: number } | null>(null);

  /** Partículas de impacto. */
  readonly impactParticles = signal<Array<{ x: number; y: number; tx: number; ty: number; color: string; delay: number }>>([]);

  private readonly ATTACK_COLORS: Record<string, string> = {
    Fire: '#ff4400', Water: '#0088ff', Grass: '#44cc44',
    Lightning: '#ffdd00', Psychic: '#cc44ff', Fighting: '#cc8844',
    Darkness: '#442266', Metal: '#aaaacc', Fairy: '#ff88cc',
    Dragon: '#cccccc', Colorless: '#cccccc',
  };

  private triggerAttackEffects(side: 'player' | 'opponent', atkType: string | undefined, power = 0): void {
    const defSide = side === 'player' ? 'opponent' : 'player';

    this.attackLunge.set(side);
    this.defenderShake.set(defSide);

    const defSelector = side === 'player' ? '.board-zone--opponent' : '.board-zone--player';
    const defEl = document.querySelector(`${defSelector} .slot-card`) as HTMLElement;
    const defRect = defEl?.getBoundingClientRect();

    // 500ms: impacto (flash + partículas + daño)
    this.addTimer(setTimeout(() => {
      if (defRect) {
        const cx = defRect.left + defRect.width / 2;
        const cy = defRect.top + defRect.height / 2;
        const color = this.ATTACK_COLORS[atkType ?? ''] ?? '#cccccc';

        this.attackFlash.set({ x: cx, y: cy });

        // Efecto de impacto vectorial por tipo (sobre el flash de partículas existente).
        this.attackEffect.set({ type: atkType ?? null, x: cx, y: cy, power });

        const parts = [];
        for (let i = 0; i < 6; i++) {
          const angle = (Math.PI * 2 * i) / 6 + Math.random() * 0.3;
          const dist = 40 + Math.random() * 60;
          parts.push({
            x: cx, y: cy,
            tx: Math.cos(angle) * dist,
            ty: Math.sin(angle) * dist,
            color,
            delay: Math.random() * 50,
          });
        }
        this.impactParticles.set(parts);
      }
    }, 500));

    // 1200ms: limpiar todo
    this.addTimer(setTimeout(() => {
      this.attackLunge.set(null);
      this.defenderShake.set(null);
      this.attackFlash.set(null);
      this.attackEffect.set(null);
      this.impactParticles.set([]);
    }, 1200));
  }

  private triggerKoExplosion(cx: number, cy: number, type: string | undefined): void {
    const colors = this.getKoColors(type);
    const particles = [];
    for (let i = 0; i < 24; i++) {
      const angle = (Math.PI * 2 * i) / 24 + (Math.random() - 0.5) * 0.5;
      const dist = 40 + Math.random() * 80;
      particles.push({
        x: Math.cos(angle) * dist,
        y: Math.sin(angle) * dist,
        dx: 0, dy: 0,
        color: colors[i % colors.length],
        delay: Math.random() * 100,
        size: 4 + Math.random() * 8,
      });
    }
    this.koExplosion.set({ x: cx, y: cy, type: type ?? '' });
    this.koParticles.set(particles);
    this.addTimer(setTimeout(() => {
      this.koExplosion.set(null);
      this.koParticles.set([]);
    }, 1500));
  }

  private pendingTurnText: string | null = null;

  /** Muestra banner de turno cada vez que cambia el jugador activo. */
  private readonly playableEffect = effect(() => {
    this.gameStateService.currentPhase();
    this.gameStateService.isMyTurn();
    this.myHand();
    this.gameStateService.myBench();
    this.gameStateService.hasAttachedEnergyThisTurn();
    this.isSetupPhase();
    this.mySetupReady();
    this.cdr.markForCheck();
    this.playableCardIds.set(this.computePlayableIds());
  });

  private readonly turnChangeEffect = effect(() => {
    const currentId = this.gameStateService.matchState()?.activePlayerId;
    if (!currentId || !this.isSetupComplete() || this.dealingState() !== 'done') return;
    if (currentId === this.prevTurnPlayerId) return;

    const myId = this.myPlayerId();
    const turnText = currentId === myId
      ? this.translate.instant('MATCH.TURN_YOURS')
      : this.translate.instant('MATCH.TURN_OPPONENT', { name: this.opponentTrainerName().toUpperCase() });

    if (this.attackLunge() !== null || this.processingAttack()) {
      this.pendingTurnText = turnText;
      this.prevTurnPlayerId = currentId;
      return;
    }
    this.prevTurnPlayerId = currentId;
    this.pendingTurnText = null;
    this.turnNotification.set(turnText);
    this.addTimer(setTimeout(() => this.turnNotification.set(null), 2000));
  });

  private showPendingTurn(): void {
    if (!this.pendingTurnText) return;
    this.turnNotification.set(this.pendingTurnText);
    this.pendingTurnText = null;
    this.addTimer(setTimeout(() => this.turnNotification.set(null), 2000));
  }

  /** Efecto que muestra el turno pendiente cuando terminan las animaciones de ataque. */
  private readonly pendingTurnEffect = effect(() => {
    this.processingAttack();
    if (this.attackLunge() === null && !this.processingAttack() && this.pendingTurnText) {
      this.showPendingTurn();
    }
  });

  /** Anima las 12 cartas de premio (6 por lado) desde cada mazo a su zona de premios. */
  private triggerPrizeAnimation(): void {
    this.prizeAnimPhase.set('animating');

    const oppDeckEl = document.querySelector('.battle-row--opponent .deck-area') as HTMLElement;
    const playerDeckEl = document.querySelector('.battle-row--player .deck-area') as HTMLElement;
    const oppPrizeSlots = document.querySelectorAll('.battle-row--opponent .prize-card');
    const playerPrizeSlots = document.querySelectorAll('.battle-row--player .prize-card');

    if (!oppDeckEl || !playerDeckEl || oppPrizeSlots.length === 0 || playerPrizeSlots.length === 0) {
      this.finishPrizeAnimation();
      return;
    }

    const oppDeckRect = oppDeckEl.getBoundingClientRect();
    const playerDeckRect = playerDeckEl.getBoundingClientRect();
    const cardW = 80;
    const cardH = 112;

    interface PrizeCardAnim {
      startX: number; startY: number;
      endX: number; endY: number;
      delay: number;
    }

    const allCards: PrizeCardAnim[] = [];

    // Opponent prize cards (6)
    oppPrizeSlots.forEach((slot) => {
      const r = slot.getBoundingClientRect();
      allCards.push({
        startX: oppDeckRect.left + oppDeckRect.width / 2 - cardW / 2,
        startY: oppDeckRect.top + oppDeckRect.height / 2 - cardH / 2,
        endX: r.left,
        endY: r.top,
        delay: 0,
      });
    });

    // Player prize cards (6)
    playerPrizeSlots.forEach((slot) => {
      const r = slot.getBoundingClientRect();
      allCards.push({
        startX: playerDeckRect.left + playerDeckRect.width / 2 - cardW / 2,
        startY: playerDeckRect.top + playerDeckRect.height / 2 - cardH / 2,
        endX: r.left,
        endY: r.top,
        delay: 600,
      });
    });

    // Initialize all cards at start positions (no transition)
    allCards.forEach((card, i) => {
      const el = document.querySelector(`.prize-anim-card[data-idx="${i}"]`) as HTMLElement;
      if (el) {
        el.style.transition = 'none';
        el.style.left = card.startX + 'px';
        el.style.top = card.startY + 'px';
      }
    });

    // Force layout, then animate to end positions with staggered delay
    requestAnimationFrame(() => {
      allCards.forEach((card, i) => {
        const t = setTimeout(() => {
          const el = document.querySelector(`.prize-anim-card[data-idx="${i}"]`) as HTMLElement;
          if (el) {
            el.style.transition = 'left 0.4s cubic-bezier(0.34, 1.56, 0.64, 1), top 0.4s cubic-bezier(0.34, 1.56, 0.64, 1)';
            el.style.left = card.endX + 'px';
            el.style.top = card.endY + 'px';
          }
        }, card.delay + i * 150);
        this.addTimer(t);
      });
    });

    const totalDuration = 600 + 12 * 150 + 500;
    this.addTimer(setTimeout(() => this.finishPrizeAnimation(), totalDuration));
  }

  private finishPrizeAnimation(): void {
    this.prizeAnimPhase.set('done');
    this.prizesDealt.set(true);
    this.setupTransitioning.set(true);

    this.addTimer(setTimeout(() => {
      this.setupTransitioning.set(false);
    }, 3000));
  }

  private prevOpponentBenchCount = 0;
  private prevOpponentActiveCount = 0;
  private prevOpponentActiveId: string | null = null;
  private prevOpponentActiveImageUrl = '';

  /** Detecta cartas nuevas del oponente en bench/active durante setup y las anima desde el mazo. */
  private readonly opponentPlacementEffect = effect(() => {
    if (!this.isSetupPhase()) {
      this.prevOpponentBenchCount = this.gameStateService.opponentBench().length;
      this.prevOpponentActiveCount = this.opponentActivePokemon() ? 1 : 0;
      return;
    }

    const bench = this.gameStateService.opponentBench();
    if (bench.length > this.prevOpponentBenchCount) {
      this.triggerPlacementAnim(true, 'bench');
    }
    this.prevOpponentBenchCount = bench.length;

    const active = this.opponentActivePokemon();
    if (active && this.prevOpponentActiveCount === 0) {
      this.triggerPlacementAnim(true, 'active');
    }
    this.prevOpponentActiveCount = active ? 1 : 0;
  });

  /** Detecta retirada/promoción del oponente y anima el movimiento de cartas. */
  private readonly opponentBattleEffect = effect(() => {
    if (this.isSetupPhase()) {
      this.prevOpponentActiveId = this.displayedOpponentActive()?.cardId ?? null;
      return;
    }
    const active = this.displayedOpponentActive();
    const activeId = active?.cardId ?? null;

    if (this.prevOpponentActiveId && !activeId) {
      this.triggerCardFly('.board-zone--opponent', '.hand-area--opponent', this.prevOpponentActiveImageUrl, () => {});
    } else if (!this.prevOpponentActiveId && activeId) {
      this.triggerCardFly('.hand-area--opponent', '.board-zone--opponent', active?.imageUrl ?? '', () => {});
    }

    this.prevOpponentActiveId = activeId;
    this.prevOpponentActiveImageUrl = active?.imageUrl ?? '';
  });

  private lastDisplayedPlayedCardId: string | null = null;

  private readonly opponentPlayedCardEffect = effect(() => {
    const lastPlayed = this.gameStateService.lastPlayedCard();
    const lastPlayerId = this.gameStateService.lastPlayedCardPlayerId();
    if (lastPlayed && lastPlayerId && lastPlayerId !== this.myPlayerId() && lastPlayed.id !== this.lastDisplayedPlayedCardId) {
      const isStadium = (lastPlayed.subtypes ?? []).map(s => s.toLowerCase()).includes('stadium');
      if (isStadium) return;
      this.lastDisplayedPlayedCardId = lastPlayed.id;
      this.showTrainerCard(lastPlayed, lastPlayerId);
    }
    if (!lastPlayed) {
      this.lastDisplayedPlayedCardId = null;
    }
  });

  /** Anima una carta desde su origen (mazo/hand) hasta el slot destino. */
  private triggerPlacementAnim(isOpponent: boolean, slot: 'bench' | 'active', cardImageUrl?: string): void {
    const sourceEl = isOpponent
      ? document.querySelector('.side-area--right .deck-area') as HTMLElement
      : document.querySelector('.hand-cards-row--player') as HTMLElement;

    let targetEl: HTMLElement | null = null;
    if (isOpponent) {
      targetEl = slot === 'active'
        ? document.querySelector('.board-zone--opponent') as HTMLElement
        : document.querySelector('.hand-area--opponent') as HTMLElement;
    } else {
      targetEl = slot === 'active'
        ? document.querySelector('.board-zone--player') as HTMLElement
        : document.querySelector('.hand-area--player') as HTMLElement;
    }

    if (!sourceEl || !targetEl) return;

    const sRect = sourceEl.getBoundingClientRect();
    const tRect = targetEl.getBoundingClientRect();
    const cardW = 80;
    const cardH = 112;

    this.placementAnim.set({
      isBack: isOpponent,
      imageUrl: isOpponent ? undefined : cardImageUrl,
      startX: sRect.left + sRect.width / 2 - cardW / 2,
      startY: sRect.top + sRect.height / 2 - cardH / 2,
      endX: tRect.left + tRect.width / 2 - cardW / 2,
      endY: tRect.top + tRect.height / 2 - cardH / 2,
    });

    this.addTimer(setTimeout(() => {
      this.placementAnimActive.set(true);
    }, 20));

    this.addTimer(setTimeout(() => {
      this.placementAnim.set(null);
      this.placementAnimActive.set(false);
    }, 500));
  }

  /** Anima una carta volando desde un origen hasta un destino usando manipulación DOM directa. */
  private triggerCardFly(fromSelector: string, toSelector: string, imageUrl: string, onComplete: () => void): void {
    const fromEl = document.querySelector(fromSelector) as HTMLElement;
    const toEl = document.querySelector(toSelector) as HTMLElement;
    if (!fromEl || !toEl || !imageUrl) {
      onComplete();
      return;
    }
    const fRect = fromEl.getBoundingClientRect();
    const tRect = toEl.getBoundingClientRect();
    const cardW = 105;
    const cardH = 147;
    const startX = fRect.left + fRect.width / 2 - cardW / 2;
    const startY = fRect.top + fRect.height / 2 - cardH / 2;
    const endX = tRect.left + tRect.width / 2 - cardW / 2;
    const endY = tRect.top + tRect.height / 2 - cardH / 2;

    const flyEl = document.createElement('div');
    flyEl.className = 'retreat-fly-card';
    flyEl.style.cssText = `position:fixed;left:${startX}px;top:${startY}px;width:${cardW}px;height:${cardH}px;z-index:3500;pointer-events:none;overflow:hidden;border-radius:8px;box-shadow:0 0 30px rgba(0,170,255,0.6)`;

    const img = document.createElement('img');
    img.src = imageUrl;
    img.style.cssText = 'width:100%;height:100%;object-fit:cover;display:block;border-radius:6px';
    flyEl.appendChild(img);
    document.body.appendChild(flyEl);

    // Force reflow, then animate
    flyEl.getBoundingClientRect();
    flyEl.style.transition = 'left 450ms cubic-bezier(0.23,1,0.32,1), top 450ms cubic-bezier(0.23,1,0.32,1)';
    flyEl.style.left = endX + 'px';
    flyEl.style.top = endY + 'px';

    this.addTimer(setTimeout(() => {
      flyEl.remove();
      onComplete();
    }, 500));
  }

  /** Anima una carta volando desde un DOMRect de origen hasta un selector destino. */
  private triggerCardFlyFromRect(fromRect: DOMRect | undefined, toSelector: string, imageUrl: string, onComplete: () => void): void {
    const toEl = document.querySelector(toSelector) as HTMLElement;
    if (!fromRect || !toEl || !imageUrl) {
      onComplete();
      return;
    }
    const tRect = toEl.getBoundingClientRect();
    const cardW = 105;
    const cardH = 147;
    const startX = fromRect.left + fromRect.width / 2 - cardW / 2;
    const startY = fromRect.top + fromRect.height / 2 - cardH / 2;
    const endX = tRect.left + tRect.width / 2 - cardW / 2;
    const endY = tRect.top + tRect.height / 2 - cardH / 2;

    const flyEl = document.createElement('div');
    flyEl.className = 'retreat-fly-card';
    flyEl.style.cssText = `position:fixed;left:${startX}px;top:${startY}px;width:${cardW}px;height:${cardH}px;z-index:3500;pointer-events:none;overflow:hidden;border-radius:8px;box-shadow:0 0 30px rgba(0,170,255,0.6)`;
    const img = document.createElement('img');
    img.src = imageUrl;
    img.style.cssText = 'width:100%;height:100%;object-fit:cover;display:block;border-radius:6px';
    flyEl.appendChild(img);
    document.body.appendChild(flyEl);

    flyEl.getBoundingClientRect();
    flyEl.style.transition = 'left 450ms cubic-bezier(0.23,1,0.32,1), top 450ms cubic-bezier(0.23,1,0.32,1)';
    flyEl.style.left = endX + 'px';
    flyEl.style.top = endY + 'px';

    this.addTimer(setTimeout(() => {
      flyEl.remove();
      onComplete();
    }, 500));
  }

  onDealtComplete(): void {
    this.dealingState.set('done');
    const matchId = this.lobbyMatchId() || this.gameStateService.matchId();
    if (matchId) {
      try {
        localStorage.setItem('matchDealt_' + matchId, '1');
      } catch {}
    }
  }

  onFirstDealDone(): void {
    this.initialDealDone.set(true);
  }

  onMulliganDealDone(): void {
    this.mulliganDealState.set('done');
  }

  /** Determina si una carta es un PokÃ©mon BÃ¡sico elegible para setup.
   * Normaliza acentos y mayÃºsculas porque el backend envÃ­a 'Pokemon' y 'Basic'. */
  isBasicPokemon(card: MatchCardView): boolean {
    const supertype = (card.supertype ?? '').toLowerCase().normalize('NFD').replace(/[\u0300-\u036f]/g, '');
    if (supertype !== 'pokemon') return false;
    const subtypes = card.subtypes ?? [];
    return subtypes.length === 0 || subtypes.some(s => s.toLowerCase() === 'basic');
  }

  /** EnvÃ­a la selecciÃ³n de PokÃ©mon Activo al backend. */
  onSelectActive(card: MatchCardView): void {
    if (!this.isBasicPokemon(card)) return;
    this.turnActionService.selectActive(card.id);
  }

  /** EnvÃ­a la colocaciÃ³n de un PokÃ©mon en banca al backend. */
  onPlaceBench(card: MatchCardView): void {
    if (!this.isBasicPokemon(card)) return;
    this.turnActionService.placeBench(card.id);
  }

  /** Confirma que el jugador terminÃ³ de colocar sus PokÃ©mon. */
  onSetupReady(): void {
    this.turnActionService.confirmSetupReady();
  }

  private ensureSetupCountdown(remainingMs: number): void {
    this.setupDeadlineAt = Date.now() + remainingMs;
    const secs = Math.max(0, Math.ceil(remainingMs / 1000));
    this.setupCountdown.set(secs);
    this.setupCountdownActive.set(true);
    if (secs <= 0) {
      this.stopSetupCountdown();
      this.onSetupCountdownExpired();
      return;
    }
    if (this.setupTimer) return;
    this.setupTimer = setInterval(() => {
      const remain = this.setupDeadlineAt - Date.now();
      const next = Math.max(0, Math.ceil(remain / 1000));
      this.setupCountdown.set(next);
      if (next <= 0) {
        this.stopSetupCountdown();
        this.onSetupCountdownExpired();
      }
    }, 1000);
  }

  private stopSetupCountdown(): void {
    this.setupCountdownActive.set(false);
    if (this.setupTimer) {
      clearInterval(this.setupTimer);
      this.setupTimer = null;
    }
  }

  private onSetupCountdownExpired(): void {
    if (this.setupStep() === 'active') {
      const card = this.myHand().find(c => this.isBasicPokemon(c));
      if (card) {
        this.onSelectActive(card);
      }
    }
    this.turnActionService.confirmSetupReady();
  }

  /** Durante setup, clickear una carta la selecciona visualmente.
   * El envÃ­o al backend ocurre al hacer click en la zona o soltar el drag. */
  onSetupCardClick(card: MatchCardView): void {
    const step = this.setupStep();
    if (!step || step === 'waiting') return;
    if (!this.isBasicPokemon(card)) return;

    // Guardar carta completa para usar al hacer click en zona (puede ya no estar en mano por optimismo)
    this.selectedCardId.set(card.id);
    this.selectedCard.set(card);
  }

  /** Click en la zona del PokÃ©mon Activo para colocar carta o extender ataque. */
  onPlayerActiveZoneClick(): void {
    const card = this.selectedCard();
    if (card) {
      if (this.setupStep() === 'active' && this.isBasicPokemon(card)) {
        this.triggerPlacementAnim(false, 'active', card.imageUrl);
        this.onSelectActive(card);
        this.sfx.play('PonerCarta.mp3');
        this.selectedCardId.set(null);
        this.selectedCard.set(null);
        return;
      }
      if (this.gameStateService.isMyTurn() && this.gameStateService.currentPhase() === 'MAIN') {
        if (this.isEnergyCard(card)) {
          const act = this.myActivePokemon();
          this.turnActionService.attachEnergy(card.id, act?.instanceId ?? act?.cardId ?? '');
          this.sfx.play('PonerCarta.mp3');
          this.selectedCardId.set(null);
          this.selectedCard.set(null);
          return;
        }
      }
    }
    if (this.pendingToolCardId()) {
      const act = this.myActivePokemon();
      if (act) {
        this.attachToolToTarget(act.instanceId ?? act.cardId ?? '');
        return;
      }
    }
    this.onActivePokemonClick();
  }

  /** Click en la zona del Bench para colocar la carta seleccionada. */
  onPlayerBenchZoneClick(): void {
    const card = this.selectedCard();
    if (!card) return;

    if (this.setupStep() === 'bench' && this.isBasicPokemon(card)) {
      this.triggerPlacementAnim(false, 'bench', card.imageUrl);
      this.onPlaceBench(card);
      this.sfx.play('PonerCarta.mp3');
      this.selectedCardId.set(null);
      this.selectedCard.set(null);
      return;
    }

    if (this.gameStateService.isMyTurn() && this.gameStateService.currentPhase() === 'MAIN') {
      if (this.isBasicPokemon(card)) {
        this.turnActionService.playBasicPokemon(card.id, 0);
        this.sfx.play('PonerCarta.mp3');
        this.selectedCardId.set(null);
        this.selectedCard.set(null);
      } else if (this.isEnergyCard(card)) {
        this.turnActionService.attachEnergy(card.id, this.selectedBenchCardId() ?? '');
        this.sfx.play('PonerCarta.mp3');
        this.selectedCardId.set(null);
        this.selectedCard.set(null);
      }
    }
  }

  readonly dropTargetZone = signal<'active' | 'bench' | 'trainer' | null>(null);
  readonly dropTargetCardId = signal<string | null>(null);
  readonly dropTargetInstanceId = signal<string | null>(null);
  private trainerPlaying = false;

  /** Detecta si el cursor está sobre una zona válida durante drag. */
  private checkDropZone(clientX: number, clientY: number): void {
    const isMain = this.gameStateService.isMyTurn() && this.gameStateService.currentPhase() === 'MAIN';
    if (!this.isSetupPhase() && !isMain) {
      this.dropTargetZone.set(null);
      this.dropTargetCardId.set(null);
      this.dropTargetInstanceId.set(null);
      return;
    }

    const elements = document.elementsFromPoint(clientX, clientY);
    const isOverActive = elements.some(el => el.closest('.board-zone--player'));
    const isOverBench = elements.some(el => el.closest('.hand-area--player'));
    const isOverTrainerSlot = elements.some(el => el.closest('[data-drop-zone="trainer"]'));

    const benchCardEl = elements.reduce<Element | null>(
      (found, el) => found ?? el.closest('.table-cards-card'), null);
    const benchCardId = benchCardEl?.getAttribute('data-card-id') ?? null;
    const benchInstanceId = benchCardEl?.getAttribute('data-card-instance-id') ?? null;

    if (this.isSetupPhase()) {
      if (this.setupStep() === 'active' && isOverActive) {
        this.dropTargetZone.set('active');
      } else if (this.setupStep() === 'bench' && isOverBench) {
        this.dropTargetZone.set('bench');
      } else {
        this.dropTargetZone.set(null);
        this.dropTargetCardId.set(null);
      }
      this.dropTargetCardId.set(null);
      return;
    }

    if (isMain && isOverTrainerSlot) {
      this.dropTargetZone.set('trainer');
      this.dropTargetCardId.set(null);
      this.dropTargetInstanceId.set(null);
    } else if (isMain && isOverActive) {
      this.dropTargetZone.set('active');
      this.dropTargetCardId.set(null);
      this.dropTargetInstanceId.set(this.myActivePokemon()?.instanceId ?? null);
    } else if (isMain && isOverBench && benchCardId) {
      this.dropTargetZone.set('bench');
      this.dropTargetCardId.set(benchCardId);
      this.dropTargetInstanceId.set(benchInstanceId);
    } else if (isMain && isOverBench) {
      this.dropTargetZone.set('bench');
      this.dropTargetCardId.set(null);
      this.dropTargetInstanceId.set(null);
    } else {
      this.dropTargetZone.set(null);
      this.dropTargetCardId.set(null);
      this.dropTargetInstanceId.set(null);
    }
  }

  readonly myPlayerId = computed(() => this.authService.currentUser()?.id ?? '');
  readonly opponentPlayerId = computed(() => {
    const state = this.gameStateService.matchState();
    const myId = this.myPlayerId();
    if (!state?.players) return '';
    const opponent = state.players.find(p => p.playerId !== myId);
    return opponent?.playerId ?? '';
  });
  readonly myTrainerName = computed(() => {
    const state = this.gameStateService.matchState();
    const myId = this.myPlayerId();
    const me = state?.players?.find(p => p.playerId === myId);
    return me?.trainerName ?? this.authService.currentUser()?.trainerName ?? 'TÃº';
  });
  readonly opponentTrainerName = computed(() => {
    const state = this.gameStateService.matchState();
    const oppId = this.opponentPlayerId();
    const opp = state?.players?.find(p => p.playerId === oppId);
    return opp?.trainerName ?? 'Oponente';
  });

  readonly openingPhase = this.gameStateService.openingPhase;
  readonly openingCoinChooserId = this.gameStateService.openingCoinChooserId;
  readonly openingCoinResult = this.gameStateService.openingCoinResult;
  readonly openingCoinChoice = this.gameStateService.openingCoinChoice;

  readonly showOpeningOverlay = computed(() => {
    const phase = this.gameStateService.openingPhase();
    return phase !== null && phase !== 'COMPLETE';
  });

  readonly isOpeningComplete = this.gameStateService.isOpeningComplete;
  readonly isSetupPhase = this.gameStateService.isSetupPhase;
  readonly isSetupComplete = this.gameStateService.isSetupComplete;
  readonly mySetupReady = this.gameStateService.mySetupReady;

  readonly isMulliganPlayer = this.gameStateService.isMulliganPlayer;
  readonly isOpponentMulliganPlayer = this.gameStateService.isOpponentMulliganPlayer;
  readonly mulliganSide = computed<'player' | 'opponent'>(() => this.isMulliganPlayer() ? 'player' : 'opponent');
  readonly isMulliganBeneficiary = this.gameStateService.isMulliganBeneficiary;
  readonly mulliganBenefitCount = this.gameStateService.mulliganBenefitCount;
  readonly isMulliganResolutionPending = this.gameStateService.isMulliganResolutionPending;

  readonly mulliganIteration = this.gameStateService.mulliganIteration;

  readonly currentPhase = this.gameStateService.currentPhase;

  readonly showEndTurnBtn = computed(() =>
    (this.gameStateService.isMyTurn() || this.processingAttack())
    && this.isSetupComplete()
    && this.dealingState() === 'done'
    && (this.currentPhase() === 'MAIN' || this.currentPhase() === 'ATTACK')
  );

  readonly myTimeLeftSec = signal(2340);
  readonly opponentTimeLeftSec = signal(2340);

  readonly myTimeDisplay = computed(() => this.formatTime(this.myTimeLeftSec()));
  readonly opponentTimeDisplay = computed(() => this.formatTime(this.opponentTimeLeftSec()));

  private timerInterval: ReturnType<typeof setInterval> | null = null;

  private formatTime(totalSec: number): string {
    const m = Math.floor(totalSec / 60);
    const s = totalSec % 60;
    return `${String(m).padStart(2, '0')}:${String(s).padStart(2, '0')}`;
  }

  readonly isCurrentChooser = computed(() => {
    const myId = this.myPlayerId();
    const chooser = this.gameStateService.openingChooserId();
    return !!myId && myId === chooser;
  });

  readonly chooserWonCoin = computed(() => {
    const choice = this.openingCoinChoice();
    const result = this.openingCoinResult();
    return !!choice && !!result && choice === result;
  });

  readonly groupedPlayerHand = computed(() => {
    const cards = this.gameStateService.myHand();
    const groups = new Map<string, { name: string; cards: MatchCardView[]; count: number; imageUrl?: string }>();
    for (const card of cards) {
      const key = card.name || card.id;
      const existing = groups.get(key);
      if (existing) { existing.cards.push(card); existing.count++; }
      else { groups.set(key, { name: card.name, cards: [card], count: 1, imageUrl: card.imageUrl }); }
    }
    return [...groups.values()];
  });

  // ===== Drag ghost =====

  readonly draggedCard = signal<MatchCardView | null>(null);
  readonly dragPos = signal({ x: 0, y: 0 });
  readonly dragTiltY = signal(0);
  readonly dragGhostSnapping = signal(false);
  private dragOffset = { x: 0, y: 0 };
  private dragSnapTarget = { x: 0, y: 0 };

  onHandCardSelect(card: MatchCardView): void {
    const wasSelected = this.selectedCardId() === card.id;

    if (wasSelected && this.gameStateService.isMyTurn() && this.gameStateService.currentPhase() === 'MAIN') {
      if (this.tryPlayTrainerCard(card)) return;
    }

    this.selectedCardId.update(current => current === card.id ? null : card.id);
    this.selectedCard.set(this.selectedCardId() ? card : null);
    this.selectedBenchCardId.set(null);
  }

  private tryPlayTrainerCard(card: MatchCardView): boolean {
    if (this.trainerPlaying) return false;
    if ((card.supertype ?? '').toLowerCase() !== 'trainer') return false;
    const subtypes = (card.subtypes ?? []).map(s => s.toLowerCase());

    if (subtypes.some(s => s.includes('tool'))) {
      this.pendingToolCardId.set(card.id);
      this.pendingToolCard.set(card);
      this.selectedCardId.set(null);
      this.selectedCard.set(null);
      this.toastService.show('Seleccioná un Pokémon para adjuntar la herramienta', 'info');
      return true;
    }

    if (subtypes.includes('item')) {
      this.trainerPlaying = true;
      this.turnActionService.playItem(card.id);
      this.deferTrainerAnimation(card);
      this.sfx.play('PonerCarta.mp3');
      this.selectedCardId.set(null);
      this.selectedCard.set(null);
      const desc = getTrainerDescription(card.id);
      if (desc) this.toastService.show(desc, 'info');
      setTimeout(() => this.trainerPlaying = false, 500);
      return true;
    }
    if (subtypes.includes('supporter')) {
      this.trainerPlaying = true;
      this.turnActionService.playSupporter(card.id);
      this.deferTrainerAnimation(card);
      this.sfx.play('PonerCarta.mp3');
      this.selectedCardId.set(null);
      this.selectedCard.set(null);
      const desc = getTrainerDescription(card.id);
      if (desc) this.toastService.show(desc, 'info');
      setTimeout(() => this.trainerPlaying = false, 500);
      return true;
    }
    if (subtypes.includes('stadium')) {
      this.trainerPlaying = true;
      this.turnActionService.playStadium(card.id);
      this.sfx.play('PonerCarta.mp3');
      this.selectedCardId.set(null);
      this.selectedCard.set(null);
      const desc = getTrainerDescription(card.id);
      if (desc) this.toastService.show(desc, 'info');
      setTimeout(() => this.trainerPlaying = false, 500);
      return true;
    }
    return false;
  }

  /**
   * Schedules the trainer-card animation after a short delay.
   */
  private pendingTrainerAnimationTimer: ReturnType<typeof setTimeout> | null = null;
  private deferTrainerAnimation(card: MatchCardView): void {
    if (this.pendingTrainerAnimationTimer) {
      clearTimeout(this.pendingTrainerAnimationTimer);
    }
    const errorBefore = this.gameStateService.actionError();
    this.pendingTrainerAnimationTimer = setTimeout(() => {
      this.pendingTrainerAnimationTimer = null;
      // Only show the animation if no new ACTION_ERROR arrived since the play was attempted.
      if (this.gameStateService.actionError() === errorBefore) {
        this.showTrainerCard(card, this.myPlayerId());
      }
    }, 200);
    this.addTimer(this.pendingTrainerAnimationTimer);
  }

  readonly pendingToolCardId = signal<string | null>(null);
  readonly pendingToolCard = signal<MatchCardView | null>(null);
  readonly trainerSlotGlow = computed(() => {
    if (!this.gameStateService.isMyTurn()) return false;
    if (this.gameStateService.currentPhase() !== 'MAIN') return false;
    if (this.pendingToolCardId() !== null) return false;
    const checkCard = (c: MatchCardView | null) => {
      if (!c) return false;
      if ((c.supertype ?? '').toLowerCase() !== 'trainer') return false;
      const subtypes = (c.subtypes ?? []).map(s => s.toLowerCase());
      if (subtypes.some(s => s.includes('tool'))) return false;
      if (subtypes.includes('supporter') && this.gameStateService.matchState()?.myState?.hasPlayedSupporterThisTurn) return false;
      return true;
    };
    return checkCard(this.draggedCard()) || checkCard(this.selectedCard());
  });

  private attachToolToTarget(targetId: string): void {
    const toolId = this.pendingToolCardId();
    const toolCard = this.pendingToolCard();
    if (!toolId) return;
    this.turnActionService.attachPokemonTool(toolId, targetId);
    if (toolCard) {
      this.showTrainerCard(toolCard, this.myPlayerId());
      const desc = getTrainerDescription(toolId);
      if (desc) this.toastService.show(desc, 'info');
    }
    this.sfx.play('PonerCarta.mp3');
    this.pendingToolCardId.set(null);
    this.pendingToolCard.set(null);
  }

  readonly myBenchHoverEnabled = computed(() => {
    if (this.isSetupPhase()) return this.setupStep() === 'bench';
    if (this.gameStateService.needsBenchPromotion()) return true;
    if (this.gameStateService.isMyTurn() && this.gameStateService.currentPhase() === 'MAIN') {
      const c = this.draggedCard() || this.selectedCard();
      if (!c) return false;
      if (this.isBasicPokemon(c)) return true;
      if (this.isEnergyCard(c) && !this.gameStateService.hasAttachedEnergyThisTurn()) return true;
      if (this.isEvolutionCard(c)) {
        const active = this.myActivePokemon();
        if (active && this.canEvolveInto(active.name, c)) return true;
        return this.gameStateService.myBench().some(p => p && this.canEvolveInto(p.name, c));
      }
    }
    return false;
  });

  readonly myBenchEnergyMap = computed(() => {
    const map = new Map<string, string[]>();
    for (const p of this.gameStateService.myBench()) {
      if (p && (p.cardId || p.instanceId)) map.set(this.benchKey(p), p.attachedEnergies);
    }
    return map;
  });

  readonly benchHighlightedCardIds = computed(() => {
    const ids = new Set<string>();
    if (this.dropTargetCardId()) ids.add(this.dropTargetCardId()!);
    if (this.gameStateService.needsBenchPromotion()) {
      const retreating = this.retreatingMonKey();
      for (const p of this.gameStateService.myBench()) {
        if (p && this.benchKey(p) !== retreating) ids.add(this.benchKey(p));
      }
      return ids;
    }
    const handCard = this.selectedCard();
    const dragCard = this.draggedCard();
    const card = handCard ?? dragCard;
    if (!card) return ids;
    if (this.isEvolutionCard(card)) {
      for (const p of this.gameStateService.myBench()) {
        if (p && this.canEvolveInto(p.name, card)) {
          ids.add(this.benchKey(p));
        }
      }
    }
    if (this.isEnergyCard(card) && !this.gameStateService.hasAttachedEnergyThisTurn()) {
      const en = this.resolveEnergyName(card).toUpperCase();
      for (const p of this.gameStateService.myBench()) {
        if (p?.cardId && this.pokemonMatchesEnergy(p, en)) {
          ids.add(this.benchKey(p));
        }
      }
      const a = this.myActivePokemon();
      if (a?.cardId && this.pokemonMatchesEnergy(a, en)) {
        // active highlighted separately via activeHighlighted
      }
    }
    return ids;
  });

  readonly benchDropGlow = computed(() => {
    if (this.isSetupPhase()) return false;
    const card = this.selectedCard() ?? this.draggedCard();
    if (!card) return false;
    if (!this.gameStateService.isMyTurn() || this.gameStateService.currentPhase() !== 'MAIN') return false;
    const bench = this.gameStateService.myBench().filter(Boolean);
    return this.isBasicPokemon(card) && bench.length < 5;
  });

  readonly hoverTargetIds = computed(() => {
    const ids = new Set<string>();
    const card = this.selectedCard() ?? this.draggedCard();
    if (!card) return ids;
    const dropId = this.dropTargetCardId();
    if (!dropId) return ids;

    const isBasic = this.isBasicPokemon(card);
    const isEvolution = this.isEvolutionCard(card);
    const isEnergy = this.isEnergyCard(card) && !this.gameStateService.hasAttachedEnergyThisTurn();

    for (const p of this.gameStateService.myBench()) {
      if (!p) continue;
      const key = this.benchKey(p);
      if (key === dropId) {
        if (isEvolution && this.canEvolveInto(p.name, card)) ids.add(key);
        if (isEnergy && p.cardId && this.pokemonMatchesEnergy(p, this.resolveEnergyName(card).toUpperCase())) ids.add(key);
        if (isBasic) ids.add(key); // any bench target is valid for basic
      }
    }
    // Also check active Pokemon for energy
    if (isEnergy) {
      const a = this.myActivePokemon();
      if (a?.cardId && this.pokemonMatchesEnergy(a, this.resolveEnergyName(card).toUpperCase())) {
        // Active gets highlight via activeHighlighted
      }
    }
    return ids;
  });

  readonly activeHighlighted = computed(() => {
    if (this.isSetupPhase()) return false;
    const card = this.selectedCard() ?? this.draggedCard();
    if (!card) return false;
    if (this.isEnergyCard(card) && !this.gameStateService.hasAttachedEnergyThisTurn()) {
      const a = this.myActivePokemon();
      return !!(a?.cardId && this.pokemonMatchesEnergy(a, this.resolveEnergyName(card).toUpperCase()));
    }
    if (this.isEvolutionCard(card)) {
      const a = this.myActivePokemon();
      return !!(a && this.canEvolveInto(a.name, card));
    }
    return false;
  });

  private resolveEnergyName(card: MatchCardView): string {
    return card.name.replace(' Energy', '');
  }

  onBenchCardSelected(card: MatchCardView): void {
    if (this.gameStateService.needsBenchPromotion()) {
      if (card.id === this.retreatingMonKey()) {
        this.toastService.show('Ese Pokémon no puede reemplazarse a sí mismo', 'info');
        return;
      }
      const bench = this.gameStateService.myBench();
      const idx = bench.findIndex(p => p && this.benchKey(p) === card.id);
      if (idx !== -1) {
        this.onBenchCardForPromote(idx, card.name);
      }
      return;
    }
    if (this.gameStateService.isMyTurn() && this.gameStateService.currentPhase() === 'MAIN') {
      if (this.pendingToolCardId()) {
        this.attachToolToTarget(card.instanceId ?? card.id);
        return;
      }
      const handCard = this.selectedCard();
      if (handCard && this.isEvolutionCard(handCard) && this.canEvolveInto(card.name, handCard)) {
        this.turnActionService.evolve(handCard.id, card.instanceId ?? card.id);
        this.sfx.play('PonerCarta.mp3');
        this.selectedCardId.set(null);
        this.selectedCard.set(null);
        return;
      }
      this.selectedBenchCardId.set(card.instanceId ?? card.id);
    }
  }

  onHandCardOpenDetail(card: MatchCardView): void {
    this.selectedCardForDetail.set({
      ...card,
      supertype: card.supertype ?? 'Unknown',
      imageUrl: card.imageUrl ?? '',
      rarity: card.rarity ?? 'COMMON',
    });
    this.showCardDetailModal.set(true);
  }

  onActivePokemonOpenDetail(pokemon: InPlayPokemonView): void {
    this.selectedCardForDetail.set({
      id: pokemon.cardId ?? pokemon.name,
      name: pokemon.name,
      supertype: 'Pokemon',
      imageUrl: pokemon.imageUrl ?? '',
      rarity: 'COMMON',
      hp: pokemon.maxHp,
      attacks: pokemon.attacks,
      abilities: pokemon.abilities,
      weaknesses: pokemon.weaknesses,
      resistances: pokemon.resistances,
      retreatCost: pokemon.retreatCost,
      pokemonType: pokemon.pokemonType,
    });
    this.showCardDetailModal.set(true);
  }

  onToolOpenDetail(tool: { name: string; imageUrl: string; rules?: string[] }): void {
    this.selectedCardForDetail.set({
      id: tool.name,
      name: tool.name,
      supertype: 'Trainer',
      imageUrl: tool.imageUrl,
      subtypes: ['Pokémon Tool'],
      rules: tool.rules ?? [],
    });
    this.showCardDetailModal.set(true);
  }

  onDiscardClick(side: 'my' | 'opponent'): void {
    const cards = side === 'my' ? this.myDiscard() : this.opponentDiscard();
    this.discardModalCards.set(cards);
    this.discardModalTitle.set(side === 'my' ? 'Mi descarte' : 'Descarte del rival');
    this.showDiscardModal.set(true);
  }

  closeDiscardModal(): void {
    this.showDiscardModal.set(false);
    this.discardModalCards.set([]);
  }

  onDragStarted(event: { card: MatchCardView; clientX: number; clientY: number; offsetX: number; offsetY: number }): void {
    this.dragOffset = { x: event.offsetX, y: event.offsetY };
    this.dragSnapTarget = { x: event.clientX - event.offsetX, y: event.clientY - event.offsetY };
    this.draggedCard.set(event.card);
    this.dragPos.set({ x: event.clientX - event.offsetX, y: event.clientY - event.offsetY });
    this.dragTiltY.set(0);
    this.dragGhostSnapping.set(false);
  }

  onDragMoved(event: { clientX: number; clientY: number }): void {
    const posX = event.clientX - this.dragOffset.x;
    const posY = event.clientY - this.dragOffset.y;
    this.dragPos.set({ x: posX, y: posY });

    const maxTiltY = 15;
    this.dragTiltY.set(Math.max(-maxTiltY, Math.min(maxTiltY, posY * 0.12)));

    // Detectar si estamos sobre una zona vÃ¡lida durante setup
    this.checkDropZone(event.clientX, event.clientY);
  }

  onDragEnded(): void {
    const dropZone = this.dropTargetZone();
    const draggedCard = this.draggedCard();
    let dropSuccess = false;

    if (dropZone && draggedCard) {
      // Setup phase: solo Basic al activo o banca
      if (this.isSetupPhase()) {
        if (this.isBasicPokemon(draggedCard)) {
          if (dropZone === 'active' && this.setupStep() === 'active') {
            this.triggerPlacementAnim(false, 'active', draggedCard.imageUrl);
            this.onSelectActive(draggedCard);
            dropSuccess = true;
          } else if (dropZone === 'bench' && this.setupStep() === 'bench') {
            this.triggerPlacementAnim(false, 'bench', draggedCard.imageUrl);
            this.onPlaceBench(draggedCard);
            dropSuccess = true;
          }
        }
      }
      // MAIN phase: Energia → activo/banca, Basic → banca
      else if (this.gameStateService.isMyTurn() && this.gameStateService.currentPhase() === 'MAIN') {
        if (this.isEnergyCard(draggedCard) && dropZone === 'active') {
          const act = this.myActivePokemon();
          this.turnActionService.attachEnergy(draggedCard.id, act?.instanceId ?? act?.cardId ?? '');
          dropSuccess = true;
        } else if (this.isEvolutionCard(draggedCard) && (dropZone === 'bench' || dropZone === 'active')) {
          const targetId = this.dropTargetInstanceId() ?? this.dropTargetCardId();
          if (targetId) {
            this.turnActionService.evolve(draggedCard.id, targetId);
            dropSuccess = true;
          }
        } else if (this.isEnergyCard(draggedCard) && dropZone === 'bench') {
          const targetId = this.dropTargetInstanceId() ?? this.dropTargetCardId();
          if (targetId) {
            this.turnActionService.attachEnergy(draggedCard.id, targetId);
            dropSuccess = true;
          }
        } else if (this.isBasicPokemon(draggedCard) && dropZone === 'bench') {
          this.turnActionService.playBasicPokemon(draggedCard.id, 0);
          dropSuccess = true;
        } else if ((draggedCard.supertype ?? '').toLowerCase() === 'trainer') {
          const subtypes = (draggedCard.subtypes ?? []).map(s => s.toLowerCase());
          if (subtypes.some(s => s.includes('tool'))) {
            // Tool dropped on bench or active — set pending tool then attach if we have a target
            const targetId = this.dropTargetInstanceId() ?? this.dropTargetCardId();
            if (targetId) {
              this.pendingToolCardId.set(draggedCard.id);
              this.pendingToolCard.set(draggedCard);
              this.attachToolToTarget(targetId);
              dropSuccess = true;
            } else {
              // No specific target — enter pending-tool selection mode
              this.pendingToolCardId.set(draggedCard.id);
              this.pendingToolCard.set(draggedCard);
              this.toastService.show('Seleccioná un Pokémon para adjuntar la herramienta', 'info');
              dropSuccess = true;
            }
          } else {
            dropSuccess = this.tryPlayTrainerCard(draggedCard);
          }
        }
      }
    }

    if (dropSuccess) {
      this.sfx.play('PonerCarta.mp3');
    }

    this.dropTargetZone.set(null);

    if (!dropSuccess) {
      this.dragGhostSnapping.set(true);
      this.addTimer(setTimeout(() => {
        this.dragPos.set(this.dragSnapTarget);
      }, 0));
      this.addTimer(setTimeout(() => {
        this.draggedCard.set(null);
        this.dragGhostSnapping.set(false);
      }, 400));
    } else {
      this.draggedCard.set(null);
    }
  }

  @HostListener('document:mousedown', ['$event'])
  onDocumentMouseDown(event: MouseEvent): void {
    if (!this.selectedCardId()) return;
    const target = event.target as HTMLElement;
    if (target.closest('app-hand-card') || target.closest('app-battle-board') || target.closest('app-table-cards') || target.closest('.hand-area--player') || target.closest('.drag-ghost') || target.closest('.attack-panel')) {
      return;
    }
    this.selectedCardId.set(null);
    this.selectedCard.set(null);
    if (this.isActiveExtended()) {
      this.closeAttackOverlay();
    }
  }

  ngOnInit(): void {
    if (this.circleTransition.pendingOpen) {
      this.circleTransition.open();
    }
    if (this.route.snapshot.queryParams['reconnect']) {
      this.music.fadeOut(300, false);
    }

    fromEvent<KeyboardEvent>(document, 'keydown').pipe(
      filter(e => e.key === 'Escape'),
      takeUntilDestroyed(this.destroyRef)
    ).subscribe(() => {
      this.ngZone.run(() => {
        if (this.showCardDetailModal()) return;
        if (this.showBattleOptions()) { this.showBattleOptions.set(false); this.showMenuModal.set(true); }
        else if (this.showMenuModal()) { this.showMenuModal.set(false); }
        else if (this.isActiveExtended()) { this.closeAttackOverlay(); }
        else { this.showMenuModal.set(true); }
        this.cdr.markForCheck();
      });
    });

    this.route.paramMap.subscribe(params => {
      const matchId = params.get('matchId');
      if (matchId) {
        this.lobbyMatchId.set(matchId);
        const user = this.authService.currentUser();
        this.gameStateService.initialize(matchId);
        if (user?.id) this.webSocketService.connect(matchId, user.id);
      }
    });

    this.addTimer(setTimeout(() => {
      this.minLoadingElapsed.set(true);
    }, this.MIN_LOADING_MS));

    this.timerInterval = setInterval(() => {
      if (!this.gameStateService.isSetupComplete()) return;
      const isMyTurn = this.gameStateService.isMyTurn();
      if (isMyTurn) {
        this.myTimeLeftSec.update(v => Math.max(0, v - 1));
      } else {
        this.opponentTimeLeftSec.update(v => Math.max(0, v - 1));
      }
    }, 1000);
  }

  ngOnDestroy(): void {
    localStorage.removeItem('activeMatchId');
    localStorage.removeItem('activeMatchHost');
    this.judgeMew.endBotSession();
    this.stopSetupCountdown();
    if (this.timerInterval) {
      clearInterval(this.timerInterval);
      this.timerInterval = null;
    }
    this.clearAllTimers();
    this.music.stop();
    this.webSocketService.disconnect();
    this.gameStateService.reset();
  }

  chooseCoinSide(side: 'heads' | 'tails'): void {
    this.sfx.play('Click.mp3');
    this.openingActionLoading.set(true);
    const matchId = this.lobbyMatchId();
    if (!matchId) {
      this.openingActionLoading.set(false);
      return;
    }
    this.webSocketService.publish(
      `/app/matches/${matchId}/coin-toss`,
      { coinSide: side }
    );
    // Simulate loading for UX until next state arrives
    this.addTimer(setTimeout(() => this.openingActionLoading.set(false), 800));
  }

  chooseStarter(starterId: string): void {
    this.sfx.play('Click.mp3');
    this.openingActionLoading.set(true);
    const matchId = this.lobbyMatchId();
    if (!matchId) {
      this.openingActionLoading.set(false);
      return;
    }
    this.webSocketService.publish(
      `/app/matches/${matchId}/choose-starter`,
      { starterPlayerId: starterId }
    );
    this.addTimer(setTimeout(() => this.openingActionLoading.set(false), 800));
  }

  onStarterChosen(who: 'me' | 'opponent'): void {
    const id = who === 'me' ? this.myPlayerId() : this.opponentPlayerId();
    this.chooseStarter(id);
  }

  onMulliganDrawChosen(count: number): void {
    this.turnActionService.resolveMulliganDraw(count);
  }

  toggleReactions(): void { this.showReactions.update(v => !v); }
  onReaction(r: string): void { console.log('Reaction:', r); this.showReactions.set(false); }
  endTurn(): void { this.showEndTurnConfirm.set(true); }

  onConfirmEndTurn(): void {
    this.showEndTurnConfirm.set(false);
    this.turnActionService.passTurn();
  }
  continueGame(): void { this.showMenuModal.set(false); }
  private surrendering = false;

  surrender(): void {
    this.showMenuModal.set(false);
    const matchId = this.gameStateService.matchId();
    if (!matchId || this.surrendering) return;
    this.surrendering = true;
    this.webSocketService.publish(`/app/matches/${matchId}/surrender`, {});
  }
  goToLobby(): void { this.router.navigate(['/lobby']); }
}
