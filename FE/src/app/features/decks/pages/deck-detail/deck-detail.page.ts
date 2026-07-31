import { AfterViewInit, ChangeDetectionStrategy, Component, computed, DestroyRef, HostListener, inject, signal } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { ActivatedRoute, Router } from '@angular/router';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { finalize, map, timeout, catchError, of } from 'rxjs';
import { DeckService } from '../../data-access/services/deck.service';
import { CardService } from '../../../pokedex/data-access/services/card.service';
import { AudioMusicChannel } from '../../../../core/services/audio-music-channel.service';
import { AudioSfxChannel } from '../../../../core/services/audio-sfx-channel.service';
import { Card } from '../../../pokedex/domain/models/card.model';
import { DeckDetail, CardSummary } from '../../domain/models/deck.models';
import { TranslateModule, TranslateService } from '@ngx-translate/core';
import { LanguageService } from '../../../../core/services/language.service';
import { PokenavDeviceComponent } from '../../../game/components/pokenav-device/pokenav-device.component';
import { GameOptionsComponent } from '../../../game/components/game-options/game-options';
import { MyText } from '../../../../shared/ui/my-text/my-text';
import { MyIconButton } from '../../../../shared/ui/my-icon-button/my-icon-button';
import { MyButton } from '../../../../shared/ui/my-button/my-button';
import { MyBadge } from '../../../../shared/ui/my-badge/my-badge';
import { MyDivider } from '../../../../shared/ui/my-divider/my-divider';
import { MySkeletonLoader } from '../../../../shared/components/my-skeleton-loader/my-skeleton-loader';
import { MyModal } from '../../../../shared/components/my-modal/my-modal';
import { MyFilterBar } from '../../../../shared/components/my-filter-bar/my-filter-bar';
import { MyCardThumbnail } from '../../../../shared/components/my-card-thumbnail/my-card-thumbnail';
import { MySearchBar } from '../../../../shared/components/my-search-bar/my-search-bar';
import { MyValidationChecklist, ValidationCheck } from '../../../../shared/components/my-validation-checklist/my-validation-checklist';
import { ToastService } from '../../../../shared/services/toast.service';
import { AuthService } from '../../../game/data-access/services/auth.service';
import { DeckCardDetail } from '../../components/deck-card-detail/deck-card-detail';
import { PixelPanelDirective } from '../../../../shared/directives';

interface GroupedCard {
  id: string;
  name: string;
  supertype: string;
  imageUrl: string;
  rarity: string;
  count: number;
}

interface CollectionItem {
  id: string;
  name: string;
  supertype: string;
  imageUrl: string;
  rarity: string;
}

interface CollectionApiItem {
  id?: string;
  name?: string;
  supertype?: string;
  imageUrl?: string;
  images?: { small?: string; large?: string };
  rarity?: string;
}

interface FlyCardData {
  left: number;
  top: number;
  width: number;
  height: number;
  imageUrl: string;
  name: string;
  opacity: number;
  scale: number;
  rotation: number;
}

@Component({
  selector: 'app-deck-detail-page',
  imports: [
    MyText,
    MyIconButton,
    MyButton,
    MyBadge,
    MyDivider,
    MySkeletonLoader,
    MyModal,
    MyFilterBar,
    MyCardThumbnail,
    MySearchBar,
    MyValidationChecklist,
    DeckCardDetail,
    TranslateModule,
    PixelPanelDirective,
    PokenavDeviceComponent,
    GameOptionsComponent,
  ],
  template: `
    <video class="bg-video" autoplay muted loop playsinline preload="auto" poster="/assets/img/deck-list-bg.jpg">
      <source src="/assets/video/decksVideo.webm" type="video/webm" />
    </video>
    <div class="bg-overlay"></div>
    <div class="max-w-[1400px] mx-auto p-8 flex flex-col h-screen">
      <header class="flex items-center justify-between flex-shrink-0 pb-4 gap-4">
        <div class="flex items-center gap-3">
          <my-icon-button icon="←" variant="pixel" size="small" color="var(--text-secondary)" (clicked)="goBack()"></my-icon-button>
          <div class="flex items-center gap-3">
            <img src="/assets/img/login/pokeball.svg" class="title-pokeball" />
            <my-text [text]="'DECK_DETAIL.TITLE' | translate" tag="h1" size="0.7rem" weight="bold" color="var(--text-primary)" align="left"></my-text>
            <img src="/assets/img/login/pokeball.svg" class="title-pokeball" />
          </div>
          @if (deckDetail(); as deck) {
            <my-text [text]="deck.name" tag="span" size="0.9rem" color="var(--text-secondary)" align="left"></my-text>
          }
        </div>
        <div class="flex items-center gap-4">
          <div class="flex items-center gap-3">
            <my-text [text]="totalCardsLabel()" tag="span" size="0.855rem" weight="bold" color="var(--neon-cyan)" align="left"></my-text>
            @for (check of validationChecks(); track check.label) {
              <my-badge
                [text]="(check.passed ? '✅' : '❌') + ' ' + check.label"
                [color]="check.passed ? 'rgba(34,197,94,0.2)' : 'rgba(239,68,68,0.2)'"
                [textColor]="check.passed ? '#22c55e' : '#ef4444'"
                [borderColor]="check.passed ? '#22c55e' : '#ef4444'"
              ></my-badge>
            }
          </div>
          <div class="flex items-center gap-3">
            <my-button [text]="'DECKS.SAVE' | translate" variant="pixel" size="large" [disabled]="saving()" (clicked)="save()"></my-button>
          </div>
        </div>
      </header>

      <div class="grid grid-cols-[minmax(0,2fr)_minmax(320px,1fr)] gap-6 flex-1 min-h-0">
        <section class="flex flex-col gap-4 overflow-y-auto p-4 min-h-0 scrollbar-none">
          <my-search-bar
            [value]="searchQuery()"
            [placeholder]="'DECK_DETAIL.SEARCH' | translate"
            variant="pixel"
            (valueChange)="searchQuery.set($event)"
          ></my-search-bar>

          @if (loading()) {
            <my-skeleton-loader [lines]="8" height="7rem" gap="0.75rem"></my-skeleton-loader>
          } @else {
            <div class="grid grid-cols-[repeat(auto-fill,minmax(130px,1fr))] gap-3 auto-rows-[1fr] p-2">
              @for (card of groupedCollectionCards(); track card.id) {
                @let isFull = card.supertype !== 'Energy' && card.count <= 0;
                  <button
                    type="button"
                    [class]="'flex flex-col gap-1 p-1.5 border border-[rgba(0,204,170,0.2)] rounded-lg bg-black/30 cursor-pointer font-inherit text-inherit text-left transition-all duration-150 hover:-translate-y-0.5 hover:border-[#00ccaa] hover:shadow-[0_0_10px rgba(0,204,170,0.15)]' + (isFull ? ' opacity-30 grayscale pointer-events-none' : '')"
                    (click)="addToDeck($event, card)"
                    [title]="isFull ? ('DECK_DETAIL.NO_COPIES' | translate) : ('DECK_DETAIL.ADD_TITLE' | translate: {name: card.name})"
                >
                  <div class="card-img-wrapper relative w-full aspect-[2.5/3.5] overflow-hidden rounded bg-black/30 flex items-center justify-center" [class.card-holo]="isHoloCard(card.rarity)">
                    @if (card.imageUrl) {
                      <img [src]="card.imageUrl" [alt]="card.name" loading="lazy" class="w-full h-full object-contain block" (error)="onImgError($event)" />
                    } @else {
                      <div class="w-full h-full flex items-center justify-center text-2xl text-[#00ccaa]">?</div>
                    }
                  </div>
                  <div class="flex flex-col items-center gap-0.5">
                    <my-text [text]="card.name" tag="span" size="0.63rem" align="center"></my-text>
                    <div class="flex items-center gap-1">
                      @if (card.supertype === 'Energy') {
                        <span class="inline-block bg-[rgba(0,204,170,0.15)] text-[#00ccaa] text-xs font-bold px-1.5 py-0.5 rounded-full leading-tight">∞</span>
                      } @else {
                        <span class="inline-block bg-[rgba(0,204,170,0.15)] text-[#00ccaa] text-xs font-bold px-1.5 py-0.5 rounded-full leading-tight">{{ card.count }}</span>
                      }
                      @if (card.inDeck > 0) {
                        <span class="text-xs text-text-secondary">({{ card.inDeck }})</span>
                      }
                    </div>
                  </div>
                </button>
              } @empty {
                <div class="col-span-full py-8">
                  <my-text [text]="'DECK_DETAIL.NO_CARDS' | translate" tag="p" color="var(--text-secondary)" align="center"></my-text>
                </div>
              }
            </div>
          }
        </section>

        <aside appPixelPanel class="flex flex-col gap-3 p-5 min-h-0 overflow-y-auto scrollbar-none">
          @if (deckDetail(); as deck) {
            <div class="flex items-center gap-2 shrink-0">
              <my-text [text]="deck.name" tag="h2" size="1.215rem" weight="bold" color="var(--text-primary)"></my-text>
            </div>

            <div class="flex gap-2 flex-wrap shrink-0">
              @if (deckValid()) {
                <my-badge [text]="'DECKS.VALID' | translate" color="rgba(34,197,94,0.2)" textColor="#22c55e" borderColor="#22c55e"></my-badge>
              } @else {
                <my-badge [text]="'DECKS.INVALID' | translate" color="rgba(239,68,68,0.2)" textColor="#ef4444" borderColor="#ef4444"></my-badge>
              }
            </div>

            <my-divider class="shrink-0"></my-divider>

            <my-validation-checklist
              [title]="'DECKS.REQUIREMENTS' | translate"
              [checks]="validationChecks()"
              class="shrink-0"
            ></my-validation-checklist>

            <my-divider class="shrink-0"></my-divider>

            <div class="flex justify-center">
              <my-filter-bar
                class="shrink-0"
                [options]="supertypeFilterOptions"
                [selected]="supertypeFilter()"
                (selectedChange)="toggleSupertypeFilter($event)"
              ></my-filter-bar>
            </div>

            @if (groupedDeckCards().length > 0) {
              <my-divider class="shrink-0"></my-divider>
              <my-text
                [text]="totalCardsLabel()"
                tag="p"
                size="0.855rem"
                weight="bold"
                color="var(--text-secondary)"
              ></my-text>
              <div class="grid grid-cols-[repeat(auto-fill,minmax(90px,1fr))] gap-2 p-1 scrollbar-none">
                @for (group of groupedDeckCards(); track group.id) {
                  <my-card-thumbnail
                    [imageUrl]="group.imageUrl"
                    [name]="group.name"
                    [count]="group.count"
                    [rarity]="group.rarity"
                    (clicked)="removeFromDeck($event, group.id)"
                  ></my-card-thumbnail>
                }
              </div>
            }
          } @else {
            <my-skeleton-loader class="shrink-0" [lines]="6" height="1rem" gap="0.75rem"></my-skeleton-loader>
          }
        </aside>
      </div>
    </div>

    @if (showExitModal()) {
      <my-modal
        [open]="showExitModal()"
        [title]="'DECK_DETAIL.UNSAVED_CHANGES' | translate"
        variant="pixel"
        color="#ef4444"
        (close)="closeExitModal()"
        width="28rem"
      >
        <div class="flex flex-col gap-4">
          <my-text [text]="'DECK_DETAIL.UNSAVED_DESC' | translate" tag="p" size="0.855rem" color="var(--text-secondary)"></my-text>
          <my-divider></my-divider>
          <div class="flex flex-col gap-2">
            <my-text [text]="'DECK_DETAIL.VALIDATION_STATUS' | translate" tag="p" size="0.765rem" weight="bold" color="var(--text-secondary)"></my-text>
            @for (check of validationChecks(); track check.label) {
              <div class="flex items-center gap-2 text-sm">
                <span>{{ check.passed ? '✅' : '❌' }}</span>
                <span [style.color]="check.passed ? '#22c55e' : '#ef4444'">{{ check.label }}</span>
              </div>
            }
          </div>
          <my-divider></my-divider>
          <div class="flex gap-3 justify-end">
            <my-button [text]="'DECK_DETAIL.CLOSE' | translate" variant="pixel" size="medium" color="var(--text-secondary)" (clicked)="closeExitModal()"></my-button>
            <my-button [text]="'DECK_DETAIL.EXIT_WITHOUT_SAVING' | translate" variant="pixel" size="medium" color="var(--neon-magenta)" (clicked)="goBackWithoutSaving()"></my-button>
            <my-button [text]="'DECK_DETAIL.SAVE_AND_EXIT' | translate" variant="pixel" size="medium" [disabled]="saving()" (clicked)="saveAndGoBack()"></my-button>
          </div>
        </div>
      </my-modal>
    }

    @if (showCardModal()) {
      @if (modalCardData(); as card) {
        <my-modal
          [open]="showCardModal()"
          [title]="card.name"
          variant="pixel"
          [backdropOpacity]="cardDetailBackdropOpacity()"
          [panelOpacity]="cardDetailPanelOpacity()"
          [bodyOverflow]="cardDetailBodyOverflow()"
          (close)="closeCardDetail()"
          width="46rem"
        >
          <div class="card-detail-layout grid grid-cols-2 gap-6 items-start">
            <div class="flex flex-col gap-4 items-center">
              <div class="w-full max-w-[220px]">
                <app-deck-card-detail [card]="card" [count]="card.inDeck" [showCount]="false" />
              </div>
              <div class="flex flex-col gap-2 w-full">
                <div class="flex justify-between items-center">
                  <span class="text-sm text-text-secondary">{{ 'DECK_DETAIL.CAN_ADD' | translate }}</span>
                  <span class="text-sm font-bold text-text-primary">{{ card.supertype === 'Energy' ? '∞' : card.available }}</span>
                </div>
                <div class="flex justify-between items-center">
                  <span class="text-sm text-text-secondary">{{ 'DECK_DETAIL.IN_DECK' | translate }}</span>
                  <span class="text-sm font-bold text-text-primary">{{ card.inDeck }}</span>
                </div>
              </div>
              <my-divider></my-divider>
              <div class="flex items-center gap-4">
                <button type="button" class="w-9 h-9 border border-[#ef4444] rounded-full font-inherit text-lg cursor-pointer transition-all duration-150 hover:bg-[rgba(239,68,68,0.15)] hover:scale-110 hover:shadow-[0_0_10px_rgba(239,68,68,0.3)] disabled:opacity-40 disabled:cursor-not-allowed disabled:hover:scale-100 disabled:hover:shadow-none" [disabled]="card.inDeck <= 0" (click)="removeOneFromDeck(card.id)">−</button>
                <span class="text-xl font-bold text-text-primary min-w-[2rem] text-center">{{ card.inDeck }}</span>
                <button type="button" class="w-9 h-9 border border-[#00ccaa] rounded-full font-inherit text-lg cursor-pointer transition-all duration-150 bg-[#00ccaa] text-[#050508] font-bold shadow-[0_0_8px_rgba(0,204,170,0.4)] hover:scale-110 hover:shadow-[0_0_15px_rgba(0,204,170,0.6)] disabled:opacity-40 disabled:cursor-not-allowed disabled:hover:scale-100 disabled:hover:shadow-none" [disabled]="card.supertype !== 'Energy' && card.inDeck >= 4" (click)="addOneToDeck(card.id)">+</button>
              </div>
              <div class="flex justify-center mt-2">
                <my-button [text]="'DECK_DETAIL.CLOSE' | translate" variant="pixel" size="medium" color="var(--text-secondary)" (clicked)="closeCardDetail()"></my-button>
              </div>
            </div>
            <div class="flex flex-col gap-2 max-h-[60vh] overflow-y-auto pr-1 scrollbar-none">
              @if (selectedCardFull(); as full) {
                @if (full.supertype === 'Pokemon') {
                  <div class="flex items-center gap-3 pb-2">
                    @if (full.hp) {
                      <div class="inline-flex items-center gap-1 text-2xl font-bold text-red-500 bg-red-500/10 border-2 border-red-500/30 rounded-lg px-3 py-1 leading-tight">{{ full.hp }} <span class="text-sm font-normal text-text-secondary">HP</span></div>
                    }
                    @if (full.types?.length) {
                      <div class="flex gap-1 flex-wrap">
                        @for (t of full.types; track $index) {
                          <span class="px-2 py-0.5 rounded-full text-xs font-bold text-white" [style.background]="energyColor(t)" style="text-shadow: 0 1px 2px rgba(0,0,0,0.4)">{{ t }}</span>
                        }
                      </div>
                    }
                  </div>

                  @if (full.evolvesFrom || full.evolvesTo?.length) {
                    <div class="flex gap-2 items-start">
                      <span class="flex-shrink-0 text-base leading-tight w-5 text-center">🔀</span>
                      <div class="flex flex-col gap-1 flex-1 text-sm text-text-primary leading-tight">
                        @if (full.evolvesFrom) {
                          <span>{{ 'DECKS.EVOLVES_FROM' | translate }} <strong>{{ full.evolvesFrom }}</strong></span>
                        }
                        @if (full.evolvesTo?.length) {
                          <span>{{ 'DECKS.EVOLVES_TO' | translate }} <strong>{{ full.evolvesTo!.join(', ') }}</strong></span>
                        }
                      </div>
                    </div>
                  }

                  @if (full.abilities?.length) {
                    <div class="flex gap-2 items-start border-t border-[rgba(0,204,170,0.15)] pt-2">
                      <span class="flex-shrink-0 text-base leading-tight w-5 text-center">⚡</span>
                      <div class="flex flex-col gap-1 flex-1 text-sm text-text-primary leading-tight">
                        <span class="text-xs text-[#00ccaa] font-bold uppercase tracking-[0.04em] mb-0.5">{{ 'DECKS.ABILITIES' | translate }}</span>
                        @for (ability of full.abilities; track ability.name) {
                          <div class="bg-[rgba(0,204,170,0.05)] border border-[rgba(0,204,170,0.15)] rounded p-2 flex flex-col gap-0.5">
                            <span class="text-sm text-text-primary font-bold">{{ ability.name }}</span>
                            <span class="text-xs text-[#00ccaa] font-medium leading-none">{{ ability.type }}</span>
                            <p class="text-xs text-text-secondary leading-tight m-0">{{ ability.text }}</p>
                          </div>
                        }
                      </div>
                    </div>
                  }

                  @if (full.attacks?.length) {
                    <div class="flex gap-2 items-start border-t border-[rgba(0,204,170,0.15)] pt-2">
                      <span class="flex-shrink-0 text-base leading-tight w-5 text-center">💥</span>
                      <div class="flex flex-col gap-1 flex-1 text-sm text-text-primary leading-tight">
                        <span class="text-xs text-[#00ccaa] font-bold uppercase tracking-[0.04em] mb-0.5">{{ 'DECKS.ATTACKS' | translate }}</span>
                        @for (attack of full.attacks; track attack.name) {
                          <div class="bg-[rgba(0,204,170,0.05)] border border-[rgba(0,204,170,0.15)] rounded p-2 flex flex-col gap-0.5">
                            <div class="flex items-center gap-2">
                              @if (attack.cost.length) {
                                <div class="flex gap-1 items-center">
                                  @for (energy of attack.cost; track $index) {
                                    <img [src]="energyIcon(energy)" alt="" class="w-5 h-5 inline-block flex-shrink-0" />
                                  }
                                 </div>
                             }
                             <span class="text-sm font-bold text-text-primary flex-1">{{ attack.name }}</span>
                             @if (attack.damage) {
                               <span class="text-base font-bold text-text-primary whitespace-nowrap">{{ attack.damage }}</span>
                             }
                           </div>
                            @if (attack.text) {
                              <p class="text-xs text-text-secondary leading-tight m-0">{{ attack.text }}</p>
                            }
                          </div>
                        }
                      </div>
                    </div>
                  }

                  <div class="flex gap-2 items-start border-t border-[rgba(0,204,170,0.15)] pt-2">
                    <span class="flex-shrink-0 text-base leading-tight w-5 text-center">🛡️</span>
                    <div class="flex flex-col gap-1 flex-1 text-sm text-text-primary leading-tight">
                      @if (full.weaknesses?.length) {
                        <div class="flex items-center gap-2">
                          <span class="text-xs text-text-secondary min-w-[5rem]">{{ 'DECKS.WEAKNESS' | translate }}</span>
                          <span class="text-sm text-text-primary font-semibold flex items-center gap-1">{{ full.weaknesses![0].type }} {{ full.weaknesses![0].value }}</span>
                        </div>
                      }
                      @if (full.resistances?.length) {
                        <div class="flex items-center gap-2">
                          <span class="text-xs text-text-secondary min-w-[5rem]">{{ 'DECKS.RESISTANCE' | translate }}</span>
                          <span class="text-sm text-text-primary font-semibold flex items-center gap-1">{{ full.resistances![0].type }} {{ full.resistances![0].value }}</span>
                        </div>
                      }
                      @if (full.retreatCost?.length) {
                        <div class="flex items-center gap-2">
                          <span class="text-xs text-text-secondary min-w-[5rem]">{{ 'DECKS.RETREAT' | translate }}</span>
                          <span class="text-sm text-text-primary font-semibold flex items-center gap-1">
                            @for (e of full.retreatCost; track $index) {
                              <img [src]="energyIcon(e)" alt="" class="w-5 h-5 inline-block flex-shrink-0" />
                            }
                          </span>
                        </div>
                      }
                    </div>
                  </div>
                }

                @if (full.supertype !== 'Pokemon') {
                  <div class="flex gap-2 items-start border-t border-[rgba(0,204,170,0.15)] pt-2">
                    <span class="flex-shrink-0 text-base leading-tight w-5 text-center">📋</span>
                    <div class="flex flex-col gap-1 flex-1 text-sm text-text-primary leading-tight">
                      @if (full.subtypes.length) {
                        <div class="flex gap-1 flex-wrap">
                          @for (st of full.subtypes; track $index) {
                            <span class="px-2 py-0.5 bg-[rgba(0,204,170,0.12)] border border-[rgba(0,204,170,0.25)] rounded-full text-xs font-medium text-[#00ccaa]">{{ st }}</span>
                          }
                        </div>
                      }
                      @if (full.rules?.length) {
                        @for (rule of full.rules; track $index) {
                          <p class="text-xs text-text-secondary leading-tight m-0">{{ rule }}</p>
                        }
                      }
                    </div>
                  </div>
                }

                <div class="flex gap-2 items-start border-t border-[rgba(0,204,170,0.15)] pt-2">
                  <span class="flex-shrink-0 text-base leading-tight w-5 text-center">🏷️</span>
                  <div class="flex flex-col gap-0.5 flex-1 text-sm text-text-primary leading-tight">
                    @if (full.rarity) {
                      <div class="flex items-center gap-2">
                        <span class="text-xs text-text-secondary min-w-[4rem]">{{ 'DECKS.RARITY' | translate }}</span>
                        <span class="text-sm text-text-primary font-semibold">{{ full.rarity }}</span>
                      </div>
                    }
                    <div class="flex items-center gap-2">
                      <span class="text-xs text-text-secondary min-w-[4rem]">{{ 'DECKS.SET' | translate }}</span>
                      <span class="text-sm text-text-primary font-semibold">{{ full.set.name }}</span>
                    </div>
                    @if (full.number) {
                      <div class="flex items-center gap-2">
                        <span class="text-xs text-text-secondary min-w-[4rem]">{{ 'DECKS.NUMBER' | translate }}</span>
                        <span class="text-sm text-text-primary font-semibold">{{ full.number }}/{{ full.set.printedTotal }}</span>
                      </div>
                    }
                  </div>
                </div>
              } @else {
                <my-skeleton-loader [lines]="6" height="1rem" gap="0.75rem"></my-skeleton-loader>
              }
            </div>
          </div>
        </my-modal>
      }
    }

    @if (flyCardData(); as fly) {
      <div
        class="fixed z-[2000] pointer-events-none rounded-xl overflow-hidden shadow-[0_0_30px_rgba(0,204,170,0.3),0_8px_32px_rgba(0,0,0,0.5)]"
        [style.left.px]="fly.left"
        [style.top.px]="fly.top"
        [style.width.px]="fly.width"
        [style.height.px]="fly.height"
        [style.opacity]="fly.opacity"
        [style.transform]="'scale(' + fly.scale + ') rotateY(' + fly.rotation + 'deg)'"
        [style.transition]="'left 800ms cubic-bezier(0.23,1,0.32,1), top 800ms cubic-bezier(0.23,1,0.32,1), width 800ms cubic-bezier(0.23,1,0.32,1), height 800ms cubic-bezier(0.23,1,0.32,1), opacity 400ms ease, transform 800ms cubic-bezier(0.23,1,0.32,1)'"
        (transitionend)="onFlyTransitionEnd($event)"
      >
        @if (fly.imageUrl) {
          <img [src]="fly.imageUrl" [alt]="fly.name" class="w-full h-full object-contain block rounded-xl" />
        } @else {
          <div class="w-full h-full flex items-center justify-center text-3xl text-[#00ccaa] bg-gradient-to-br from-[rgba(0,204,170,0.1)] to-[rgba(0,0,0,0.4)] rounded-xl">?</div>
        }
      </div>
    }

    <app-pokenav-device
      [isOpen]="showMenu() && !showOptions()"
      [compact]="true"
      (resume)="showMenu.set(false)"
      (showProfile)="onMenuProfile()"
      (showDecks)="onMenuExit()"
      (showOptions)="showMenu.set(false); showOptions.set(true)"
      (exit)="onMenuExit()"
      (resetCharacter)="showMenu.set(false)"
    ></app-pokenav-device>
    @if (showOptions()) {
      <app-game-options (back)="showOptions.set(false); showMenu.set(true)"></app-game-options>
    }
  `,
  styleUrl: './deck-detail.page.css',
  changeDetection: ChangeDetectionStrategy.OnPush,
  host: {
    '(contextmenu)': '$event.preventDefault()',
  },
})
export class DeckDetailPage implements AfterViewInit {
  private readonly deckService = inject(DeckService);
  private readonly cardService = inject(CardService);
  private readonly http = inject(HttpClient);
  private readonly router = inject(Router);
  private readonly route = inject(ActivatedRoute);
  private readonly destroyRef = inject(DestroyRef);
  private readonly audioMusic = inject(AudioMusicChannel);
  private readonly sfx = inject(AudioSfxChannel);
  private readonly toastService = inject(ToastService);
  private readonly authService = inject(AuthService);
  readonly langService = inject(LanguageService);
  readonly translateService = inject(TranslateService);

  private readonly deckId = this.route.snapshot.paramMap.get('id')!;

  readonly deckDetail = signal<DeckDetail | null>(null);
  readonly currentCardIds = signal<string[]>([]);
  readonly cardInfoMap = signal<Map<string, CardSummary>>(new Map());
  readonly collectionCounts = signal<Map<string, number>>(new Map());
  readonly loading = signal(true);
  readonly saving = signal(false);
  readonly supertypeOptions: ('Pokemon' | 'Trainer' | 'Energy')[] = ['Pokemon', 'Trainer', 'Energy'];
  readonly supertypeFilterOptions = [
    { label: 'DECK_DETAIL.POKEMON', value: 'Pokemon' },
    { label: 'DECK_DETAIL.TRAINER', value: 'Trainer' },
    { label: 'DECK_DETAIL.ENERGY', value: 'Energy' },
  ];
  readonly searchQuery = signal('');
  readonly supertypeFilter = signal<'ALL' | 'Pokemon' | 'Trainer' | 'Energy'>('ALL');

  readonly originalCardIds = signal<string[]>([]);
  readonly showExitModal = signal(false);
  readonly showCardModal = signal(false);
  readonly showMenu = signal(false);
  readonly showOptions = signal(false);

  @HostListener('document:keydown.escape')
  onEscapeKey(): void {
    if (this.showExitModal() || this.showCardModal()) return;
    if (this.showOptions()) { this.showOptions.set(false); this.showMenu.set(true); return; }
    this.showMenu.update(v => !v);
  }

  onMenuProfile(): void { this.showMenu.set(false); this.router.navigate(['/profile']); }
  onMenuExit(): void { this.showMenu.set(false); this.router.navigate(['/decks']); }
  readonly modalCardId = signal<string | null>(null);
  readonly selectedCardFull = signal<Card | null>(null);
  readonly cardDetailBodyOverflow = signal('auto');
  readonly cardDetailBackdropOpacity = signal(0);
  readonly cardDetailPanelOpacity = signal(1);
  readonly flyCardData = signal<FlyCardData | null>(null);
  readonly flyPhase = signal<'entering' | 'exiting' | null>(null);
  private cardFlyOriginRect: DOMRect | null = null;
  private flyArrived = false;
  private cardDetailLoaded = false;

  readonly hasChanges = computed(() => {
    const current = this.currentCardIds();
    const original = this.originalCardIds();
    if (current.length !== original.length) return true;
    return current.some((id, i) => id !== original[i]);
  });

  readonly modalCardData = computed(() => {
    const cardId = this.modalCardId();
    if (!cardId) return null;
    const info = this.cardInfoMap().get(cardId);
    if (!info) return null;
    const inDeck = this.currentCardIds().filter(id => id === cardId).length;
    if (info.supertype === 'Energy') {
      return { ...info, inDeck, available: Infinity };
    }
    return { ...info, inDeck, available: Math.max(0, 4 - inDeck) };
  });

  readonly totalCardsLabel = computed(() => {
    const count = this.currentCardIds().length;
    this.langService.currentLang();
    return this.translateService.instant('DECK_DETAIL.DECK_CARDS') + ' (' + count + ')';
  });

  readonly deckValid = computed(() => {
    const checks = this.validationChecks();
    return checks.every(c => c.passed);
  });

  readonly groupedCollectionCards = computed(() => {
    const cardInfo = this.cardInfoMap();
    const collectionCounts = this.collectionCounts();
    const cardIds = this.currentCardIds();
    const search = this.searchQuery().toLowerCase().trim();
    const filter = this.supertypeFilter();

    const inDeckCounts = new Map<string, number>();
    for (const id of cardIds) {
      inDeckCounts.set(id, (inDeckCounts.get(id) ?? 0) + 1);
    }

    const result: (GroupedCard & { inDeck: number; available: number })[] = [];

    for (const [id, totalCount] of collectionCounts) {
      if (id.startsWith('pokesecret-')) continue;
      const info = cardInfo.get(id);
      if (!info) continue;
      if (filter !== 'ALL' && info.supertype !== filter) continue;
      if (search && !info.name.toLowerCase().includes(search)) continue;

      const inDeck = inDeckCounts.get(id) ?? 0;
      const available = info.supertype === 'Energy' ? Infinity : Math.max(0, 4 - inDeck);
      result.push({ ...info, count: available, available, inDeck });
    }

    result.sort((a, b) => a.name.localeCompare(b.name));
    return result;
  });

  readonly groupedDeckCards = computed(() => {
    const cardInfo = this.cardInfoMap();
    const cardIds = this.currentCardIds();
    const filter = this.supertypeFilter();

    const counts = new Map<string, number>();
    for (const id of cardIds) {
      const info = cardInfo.get(id);
      if (!info) continue;
      if (filter !== 'ALL' && info.supertype !== filter) continue;
      counts.set(id, (counts.get(id) ?? 0) + 1);
    }

    const result: GroupedCard[] = [];
    for (const [id, count] of counts) {
      const info = cardInfo.get(id);
      if (!info) continue;
      result.push({ ...info, count });
    }

    result.sort((a, b) => a.name.localeCompare(b.name));
    return result;
  });

  readonly validationChecks = computed<ValidationCheck[]>(() => {
    const cardIds = this.currentCardIds();
    const cardInfo = this.cardInfoMap();
    this.langService.currentLang();

    const totalCards = cardIds.length;
    const hasPokemon = cardIds.some(id => cardInfo.get(id)?.supertype === 'Pokemon');

    const nonEnergyCounts = new Map<string, number>();
    for (const id of cardIds) {
      const card = cardInfo.get(id);
      if (card && card.supertype !== 'Energy') {
        nonEnergyCounts.set(id, (nonEnergyCounts.get(id) ?? 0) + 1);
      }
    }
    const maxCopies = nonEnergyCounts.size > 0 ? Math.max(0, ...nonEnergyCounts.values()) : 0;
    const noExceedsMax = maxCopies <= 4;

    return [
      { label: this.translateService.instant('DECKS.VALIDATION_60_CARDS'), passed: totalCards === 60 },
      { label: this.translateService.instant('DECKS.VALIDATION_BASIC_POKEMON'), passed: hasPokemon },
      { label: this.translateService.instant('DECKS.VALIDATION_MAX_COPIES'), passed: noExceedsMax },
    ];
  });

  constructor() {
    this.loadData();
  }

  ngAfterViewInit(): void {
    this.audioMusic.play('../audio/MusicaFondoDeckList.mp3');
  }

  goBack(): void {
    this.sfx.play('Click.mp3');
    if (this.hasChanges()) {
      this.showExitModal.set(true);
    } else {
      this.router.navigate(['/decks']);
    }
  }

  goBackWithoutSaving(): void {
    this.sfx.play('Click.mp3');
    this.showExitModal.set(false);
    this.router.navigate(['/decks']);
  }

  toggleSupertypeFilter(type: string): void {
    this.sfx.play('Click.mp3');
    this.supertypeFilter.update(current => current === type ? 'ALL' : type as 'ALL' | 'Pokemon' | 'Trainer' | 'Energy');
  }

  addToDeck(event: MouseEvent, card: GroupedCard & { inDeck: number; available: number }): void {
    const isFull = card.supertype !== 'Energy' && card.count <= 0;
    if (isFull) return;
    this.sfx.play('Click.mp3');
    this.openCardDetail(event, card.id, card);
  }

  removeFromDeck(event: MouseEvent, cardId: string): void {
    this.sfx.play('Click.mp3');
    const info = this.cardInfoMap().get(cardId);
    if (!info) return;
    this.openCardDetail(event, cardId, info);
  }

  save(): void {
    this.sfx.play('Click.mp3');
    this.saveInternal(false);
  }

  saveAndGoBack(): void {
    this.sfx.play('Click.mp3');
    this.saveInternal(true);
  }

  private saveInternal(navigateBack: boolean): void {
    if (this.saving()) return;
    this.saving.set(true);

    this.deckService.updateDeck(this.deckId, this.currentCardIds())
      .pipe(
        finalize(() => this.saving.set(false)),
        takeUntilDestroyed(this.destroyRef),
      )
      .subscribe({
        next: () => {
          this.originalCardIds.set([...this.currentCardIds()]);
          this.authService.getCurrentPlayer().pipe(takeUntilDestroyed(this.destroyRef)).subscribe();
          this.toastService.show(this.translateService.instant('DECK_DETAIL.SAVED_TOAST'), 'success');
          this.sfx.play('OperacionExitosa.mp3');
          if (navigateBack) {
            setTimeout(() => this.router.navigate(['/decks']), 500);
          }
        },
        error: () => {
          this.toastService.show(this.translateService.instant('DECK_DETAIL.SAVE_ERROR'), 'error');
        },
      });
  }

  closeExitModal(): void {
    this.sfx.play('Click.mp3');
    this.showExitModal.set(false);
  }

  closeCardDetail(): void {
    this.sfx.play('Click.mp3');
    const origin = this.cardFlyOriginRect;
    const card = this.modalCardData();
    if (!origin || !card) {
      this.resetCardDetailState();
      return;
    }

    const cardWrapper = document.querySelector('.card-detail-layout .card-3d-wrapper') as HTMLElement;
    const currentRect = cardWrapper?.getBoundingClientRect();

    this.showCardModal.set(false);
    this.cardDetailBackdropOpacity.set(0);
    this.cardDetailPanelOpacity.set(0);

    if (currentRect) {
      this.flyCardData.set({
        left: currentRect.left,
        top: currentRect.top,
        width: currentRect.width,
        height: currentRect.height,
        imageUrl: card.imageUrl,
        name: card.name,
        opacity: 1,
        scale: 1,
        rotation: 0,
      });

      this.flyPhase.set('exiting');

      setTimeout(() => {
        requestAnimationFrame(() => {
          this.flyCardData.set({
            left: origin.left,
            top: origin.top,
            width: origin.width,
            height: origin.height,
            imageUrl: card.imageUrl,
            name: card.name,
            opacity: 1,
            scale: 1.08,
            rotation: -720,
          });
        });
      });
    } else {
      this.resetCardDetailState();
    }
  }

  addOneToDeck(cardId: string): void {
    if (cardId.startsWith('pokesecret-')) return;
    const info = this.cardInfoMap().get(cardId);
    if (!info) return;

    const inDeckCount = this.currentCardIds().filter(id => id === cardId).length;

    if (info.supertype !== 'Energy') {
      if (inDeckCount >= 4) return;
    }

    this.sfx.play('AQCarta.mp3');
    this.currentCardIds.update(ids => [...ids, cardId]);
  }

  removeOneFromDeck(cardId: string): void {
    this.currentCardIds.update(ids => {
      const index = ids.indexOf(cardId);
      if (index === -1) return ids;
      const next = [...ids];
      next.splice(index, 1);
      return next;
    });
    this.sfx.play('AQCarta.mp3');
  }

  private openCardDetail(event: MouseEvent, cardId: string, card: CardSummary): void {
    const target = event.currentTarget as HTMLElement;
    const imgWrapper = target.querySelector('.card-img-wrapper');
    const originRect = (imgWrapper ?? target).getBoundingClientRect();

    this.flyArrived = false;
    this.cardDetailLoaded = false;
    this.cardFlyOriginRect = originRect;
    this.cardDetailBodyOverflow.set('visible');

    this.modalCardId.set(cardId);
    this.selectedCardFull.set(null);
    this.showCardModal.set(true);
    this.cardDetailBackdropOpacity.set(0);
    this.cardDetailPanelOpacity.set(0);
    this.loadCardDetail(cardId);

    setTimeout(() => {
      const cardWrapper = document.querySelector('.card-detail-layout .card-3d-wrapper') as HTMLElement;
      if (!cardWrapper) {
        this.cardDetailBackdropOpacity.set(0.85);
        return;
      }

      const targetRect = cardWrapper.getBoundingClientRect();

      this.flyCardData.set({
        left: originRect.left,
        top: originRect.top,
        width: originRect.width,
        height: originRect.height,
        imageUrl: card.imageUrl,
        name: card.name,
        opacity: 1,
        scale: 1.08,
        rotation: 0,
      });

      this.flyPhase.set('entering');

      requestAnimationFrame(() => {
        this.flyCardData.set({
          left: targetRect.left,
          top: targetRect.top,
          width: targetRect.width,
          height: targetRect.height,
          imageUrl: card.imageUrl,
          name: card.name,
          opacity: 1,
          scale: 1,
          rotation: 720,
        });
      });
    });
  }

  onFlyTransitionEnd(event: TransitionEvent): void {
    if (event.propertyName !== 'left') return;

    const phase = this.flyPhase();
    this.flyPhase.set(null);

    if (phase === 'entering') {
      this.flyArrived = true;
      this.showModalAndFadePortal();
    } else if (phase === 'exiting') {
      this.resetCardDetailState();
    }
  }

  private showModalAndFadePortal(): void {
    if (!this.flyArrived || !this.cardDetailLoaded) return;

    this.cardDetailBackdropOpacity.set(0.85);
    this.cardDetailPanelOpacity.set(1);
    this.flyCardData.update(d => d ? { ...d, opacity: 0 } : null);
    setTimeout(() => this.flyCardData.set(null), 350);
  }

  private resetCardDetailState(): void {
    this.flyArrived = false;
    this.cardDetailLoaded = false;
    this.cardFlyOriginRect = null;
    this.selectedCardFull.set(null);
    this.modalCardId.set(null);
    this.showCardModal.set(false);
    this.cardDetailBackdropOpacity.set(0);
    this.cardDetailPanelOpacity.set(0);
    this.flyCardData.set(null);
    this.flyPhase.set(null);
    this.cardDetailBodyOverflow.set('auto');
  }

  private loadCardDetail(cardId: string): void {
    this.cardService.getCardById(cardId)
      .pipe(
        timeout(3000),
        catchError(() => of(null)),
        takeUntilDestroyed(this.destroyRef),
      )
      .subscribe({
        next: full => {
          if (full) this.selectedCardFull.set(full);
          this.cardDetailLoaded = true;
          this.showModalAndFadePortal();
        },
      });
  }

  onImgError(event: Event): void {
    const img = event.target as HTMLImageElement;
    img.style.display = 'none';
    const wrapper = img.parentElement;
    if (wrapper) {
      const placeholder = document.createElement('div');
      placeholder.className = 'card-placeholder';
      placeholder.textContent = '?';
      wrapper.appendChild(placeholder);
    }
  }

  isHoloCard(rarity: string): boolean {
    return rarity === 'Rare Holo EX' || rarity === 'Rare Ultra';
  }


  private loadData(): void {
    const deckId = this.deckId;
    if (!deckId) {
      this.router.navigate(['/decks']);
      return;
    }

    this.loading.set(true);

    this.deckService.getDeck(deckId)
      .pipe(
        takeUntilDestroyed(this.destroyRef),
      )
      .subscribe({
        next: deck => {
          this.deckDetail.set(deck);
          const cardIds = deck.cards.map(c => c.id);
          this.currentCardIds.set(cardIds);
          this.originalCardIds.set([...cardIds]);

          const infoMap = new Map<string, CardSummary>();
          for (const card of deck.cards) {
            if (!infoMap.has(card.id)) {
              infoMap.set(card.id, card);
            }
          }
          this.cardInfoMap.set(infoMap);

          this.loadCollection();
        },
        error: () => {
          this.loading.set(false);
          this.router.navigate(['/decks']);
        },
      });
  }

  private loadCollection(): void {
    const params = new HttpParams()
      .set('page', 0)
      .set('size', 200);

    this.http.get<CollectionApiItem[] | { content?: CollectionApiItem[]; last?: boolean; totalElements?: number }>(
      '/api/players/me/collection',
      { params },
    )
      .pipe(
        map(response => this.normalizeCollectionResponse(response)),
        finalize(() => this.loading.set(false)),
        takeUntilDestroyed(this.destroyRef),
      )
      .subscribe({
        next: items => {
          const counts = new Map<string, number>();
          const infoMap = new Map(this.cardInfoMap());

          for (const item of items) {
            counts.set(item.id, (counts.get(item.id) ?? 0) + 1);
            if (!infoMap.has(item.id)) {
              infoMap.set(item.id, {
                id: item.id,
                name: item.name,
                supertype: item.supertype,
                imageUrl: item.imageUrl,
                rarity: item.rarity,
              });
            }
          }

          this.collectionCounts.set(counts);
          this.cardInfoMap.set(infoMap);
        },
        error: () => {
          this.collectionCounts.set(new Map());
        },
      });
  }

  private normalizeCollectionResponse(
    response: CollectionApiItem[] | { content?: CollectionApiItem[]; last?: boolean; totalElements?: number },
  ): CollectionItem[] {
    if (Array.isArray(response)) {
      return response.map(item => this.normalizeCollectionItem(item));
    }

    const content = (response.content ?? []).map(item => this.normalizeCollectionItem(item));
    return content;
  }

  private normalizeCollectionItem(item: CollectionApiItem): CollectionItem {
    return {
      id: item.id ?? crypto.randomUUID(),
      name: item.name ?? 'Carta sin nombre',
      supertype: this.normalizeSupertype(item.supertype),
      imageUrl: item.imageUrl ?? item.images?.small ?? item.images?.large ?? '',
      rarity: item.rarity ?? '',
    };
  }

  private normalizeSupertype(supertype: string | undefined): string {
    if (!supertype) return 'Pokemon';
    const upper = supertype.toUpperCase();
    if (upper === 'POKEMON') return 'Pokemon';
    if (upper === 'TRAINER') return 'Trainer';
    if (upper === 'ENERGY') return 'Energy';
    return 'Pokemon';
  }

  energyColor(type: string): string {
    const colors: Record<string, string> = {
      Colorless: '#a8a878',
      Fire: '#f08030',
      Water: '#6890f0',
      Lightning: '#f8d030',
      Grass: '#78c850',
      Psychic: '#f85888',
      Fighting: '#c03028',
      Darkness: '#705848',
      Metal: '#b8b8d0',
      Dragon: '#7038f8',
      Fairy: '#ee99ac',
    };
    return colors[type] ?? '#a8a878';
  }

  energyIcon(type: string): string {
    return `/assets/energies/${type}.png`;
  }


}
