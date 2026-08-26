import { AfterViewInit, ChangeDetectionStrategy, Component, computed, DestroyRef, HostListener, inject, signal } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { finalize, timeout, catchError, of, tap } from 'rxjs';
import { DeckService } from '../../data-access/services/deck.service';
import { CardService } from '../../../pokedex/data-access/services/card.service';
import { AudioMusicChannel } from '../../../../core/services/audio-music-channel.service';
import { AudioSfxChannel } from '../../../../core/services/audio-sfx-channel.service';
import { Card } from '../../../pokedex/domain/models/card.model';
import { DeckListItem, DeckDetail, CardSummary } from '../../domain/models/deck.models';
import { TranslateModule, TranslateService } from '@ngx-translate/core';
import { LanguageService } from '../../../../core/services/language.service';
import { PokenavDeviceComponent } from '../../../game/components/pokenav-device/pokenav-device.component';
import { GameOptionsComponent } from '../../../game/components/game-options/game-options';
import { DeckCard } from '../../components/deck-card/deck-card';
import { DeckCardDetail } from '../../components/deck-card-detail/deck-card-detail';
import { MyText } from '../../../../shared/ui/my-text/my-text';
import { MyInput } from '../../../../shared/ui/my-input/my-input';
import { MyIconButton } from '../../../../shared/ui/my-icon-button/my-icon-button';
import { MyButton } from '../../../../shared/ui/my-button/my-button';
import { MyBadge } from '../../../../shared/ui/my-badge/my-badge';
import { MyDivider } from '../../../../shared/ui/my-divider/my-divider';
import { MyModal } from '../../../../shared/components/my-modal/my-modal';
import { MySkeletonLoader } from '../../../../shared/components/my-skeleton-loader/my-skeleton-loader';
import { MyFilterBar } from '../../../../shared/components/my-filter-bar/my-filter-bar';
import { MyCardThumbnail } from '../../../../shared/components/my-card-thumbnail/my-card-thumbnail';
import { MyConfirmModal } from '../../../../shared/components/my-confirm-modal/my-confirm-modal';
import { MyValidationChecklist, ValidationCheck } from '../../../../shared/components/my-validation-checklist/my-validation-checklist';
import { MySearchBar } from '../../../../shared/components/my-search-bar/my-search-bar';
import { MyPageBackground } from '../../../../shared/components/my-page-background/my-page-background';
import { ToastService } from '../../../../shared/services/toast.service';
import { AuthService } from '../../../game/data-access/services/auth.service';
import { StorageService } from '../../../../core/services/storage.service';
import { CircleTransitionService } from '../../../../shared/services/circle-transition.service';
import { PixelPanelDirective } from '../../../../shared/directives';

type FilterMode = 'ALL' | 'FAVORITES' | 'VALID';

interface GroupedCard {
  id: string;
  name: string;
  imageUrl: string;
  supertype: string;
  rarity: string;
  count: number;
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
  selector: 'app-deck-list-page',
  standalone: true,
  // TranslateModule agrega el pipe 'translate' al template inline
  imports: [
    TranslateModule,
    DeckCard,
    DeckCardDetail,
    MyText,
    MyInput,
    MyIconButton,
    MyButton,
    MyBadge,
    MyDivider,
    MyModal,
    MySkeletonLoader,
    MyFilterBar,
    MyCardThumbnail,
    MyConfirmModal,
    MyValidationChecklist,
    MySearchBar,
    MyPageBackground,
    PixelPanelDirective,
    PokenavDeviceComponent,
    GameOptionsComponent,
  ],
  template: `
    <my-page-background videoSrc="/assets/video/decksVideo.webm" poster="/assets/img/deck-list-bg.jpg" [overlayOpacity]="0.45" />
    <div class="max-w-[1400px] mx-auto p-8 flex flex-col h-screen">
      <div class="grid grid-cols-[minmax(0,2fr)_minmax(320px,1fr)] gap-6 flex-1 min-h-0">
        <section appPixelPanel="glass" class="flex flex-col gap-4 overflow-y-auto p-6 min-h-0 scrollbar-none">
          <header class="flex flex-col gap-3 flex-shrink-0">
            <div class="flex items-center justify-between">
            <div class="flex items-center gap-3">
              <img src="/assets/img/login/pokeball.svg" class="title-pokeball" />
              <my-text [text]="'DECKS.TITLE' | translate" tag="h1" size="0.7rem" weight="bold" color="var(--text-primary)"></my-text>
              <img src="/assets/img/login/pokeball.svg" class="title-pokeball" />
            </div>
              <!-- Botón "volver al juego" traducido -->
              <my-button [text]="'DECKS.BACK_TO_GAME' | translate" variant="modern" size="small" (clicked)="goBackToGame()"></my-button>
            </div>
            <div class="flex gap-4 items-center flex-wrap">
              <my-search-bar
                [value]="searchQuery()"
                [placeholder]="'DECKS.SEARCH' | translate"
                variant="modern"
                (valueChange)="searchQuery.set($event)"
              ></my-search-bar>
              <my-filter-bar
                [options]="filterOptions"
                [selected]="filterMode()"
                variant="modern"
                color="#0284c7"
                (selectedChange)="setFilterMode($event)"
              ></my-filter-bar>
            </div>
          </header>

          @if (loading()) {
            <my-skeleton-loader [lines]="6" height="5rem" gap="0.75rem"></my-skeleton-loader>
          } @else {
            <div class="grid grid-cols-[repeat(auto-fill,minmax(220px,1fr))] gap-3 auto-rows-[1fr] p-2">
              <app-deck-card
                [isCreateCard]="true"
                [deck]="createCardPlaceholder"
                (create)="openCreateModal()"
              ></app-deck-card>

              @for (deck of filteredDecks(); track deck.id) {
                <app-deck-card
                  [deck]="deck"
                  [selected]="selectedDeck()?.id === deck.id"
                  [isFavorite]="favoriteIds().has(deck.id)"
                  (selectedChange)="selectDeck($event)"
                  (toggleFavorite)="toggleFavorite($event)"
                ></app-deck-card>
              } @empty {
                <div class="col-span-full py-8">
                  <my-text [text]="'DECKS.NO_DECKS' | translate" tag="p" color="#ffffff" align="center"></my-text>
                </div>
              }
            </div>
          }
        </section>

        <aside appPixelPanel="glass" class="flex flex-col gap-3 p-6 min-h-0 overflow-y-auto scrollbar-none">
          @if (selectedDeck(); as deck) {
            <div class="flex items-center gap-2 shrink-0">
              <my-icon-button
                [icon]="favoriteIds().has(deck.id) ? '★' : '☆'"
                [color]="favoriteIds().has(deck.id) ? 'var(--page-teal)' : '#cbd5e1'"
                variant="modern"
                size="small"
                (clicked)="toggleFavorite(deck.id)"
              ></my-icon-button>

              @if (renamingDeckId() === deck.id) {
                <my-input
                  #renameInput
                  [value]="renameValue()"
                  [placeholder]="'DECKS.DECK_NAME_PLACEHOLDER' | translate"
                  variant="modern"
                  (valueChange)="renameValue.set($event)"
                  (keydown.enter)="saveRename()"
                ></my-input>
                <my-button [text]="'DECKS.SAVE' | translate" variant="modern" size="small" (clicked)="saveRename()"></my-button>
                <my-button [text]="'DECKS.CANCEL' | translate" variant="modern" size="small" color="#64748b" (clicked)="cancelRename()"></my-button>
              } @else {
                <my-text [text]="deck.name" tag="h2" size="1.215rem" weight="bold" color="var(--text-primary)"></my-text>
                <my-icon-button icon="&#9998;" variant="modern" size="small" color="#cbd5e1" (clicked)="startRename(deck.id)"></my-icon-button>
              }
            </div>

            <div class="flex gap-2 flex-wrap shrink-0">
              @if (deck.valid) {
                <!-- Badge válido/inválido traducido -->
                <my-badge [text]="'DECKS.VALID' | translate" color="var(--success-color, #22c55e)"></my-badge>
              } @else {
                <my-badge [text]="'DECKS.INVALID' | translate" color="var(--error-color, #ef4444)"></my-badge>
              }
            </div>

            <my-divider class="shrink-0"></my-divider>

            <my-validation-checklist
              [title]="'DECKS.REQUIREMENTS' | translate"
              [checks]="validationChecks()"
              class="shrink-0"
            ></my-validation-checklist>

            <my-divider class="shrink-0"></my-divider>

            <div class="flex flex-col gap-2 shrink-0">
              @if (deck.id === authService.currentUser()?.activeDeckId) {
                <my-button [text]="'DECKS.SELECTED_DECK' | translate" variant="modern" size="large" color="var(--neon-green)" [disabled]="true"></my-button>
              } @else {
                <my-button [text]="'DECKS.SELECT_DECK' | translate" variant="modern" size="large" [disabled]="selectingDeck() || !deck.valid" (clicked)="selectAsActiveDeck(deck.id)"></my-button>
              }
            </div>
            <div class="flex flex-row gap-3 justify-center shrink-0">
              <my-button [text]="'DECKS.EDIT_DECK' | translate" variant="modern" size="large" (clicked)="editDeck(deck.id)"></my-button>
              <my-button [text]="'DECKS.DELETE_DECK' | translate" variant="modern" size="large" color="#ef4444" (clicked)="openDeleteModal(deck)"></my-button>
            </div>

            @if (groupedCards().length > 0) {
              <my-divider class="shrink-0"></my-divider>
              <my-text [text]="('DECKS.CARDS_IN_DECK' | translate) + ' (' + deck.cardCount + ')'" tag="p" size="0.855rem" weight="bold" color="#ffffff"></my-text>
              <div class="grid grid-cols-[repeat(auto-fill,minmax(90px,1fr))] gap-2 p-1 scrollbar-none">
                @for (group of groupedCards(); track group.id) {
                  <my-card-thumbnail
                    [imageUrl]="group.imageUrl"
                    [name]="group.name"
                    [count]="group.count"
                    [rarity]="group.rarity"
                    (clicked)="openCardDetail($event, group, group.count)"
                  ></my-card-thumbnail>
                }
              </div>
            }
          } @else {
            <div class="flex items-center justify-center flex-1 py-8 shrink-0">
              <my-text [text]="'DECKS.SELECT_TO_SEE' | translate" tag="p" color="#ffffff" align="center"></my-text>
            </div>
          }
        </aside>
      </div>
    </div>

    <!-- Modal de crear mazo con título y botones traducidos -->
    <my-modal
      [open]="showCreateModal()"
      [title]="'DECKS.NEW_DECK' | translate"
      variant="glass"
      backdropStyle="blur"
      (close)="closeCreateModal()"
      width="24rem"
    >
      <div class="flex flex-col gap-4">
        <my-input
          [value]="newDeckName()"
          [placeholder]="'DECKS.DECK_NAME_PLACEHOLDER' | translate"
          variant="modern"
          (valueChange)="newDeckName.set($event)"
          (keydown.enter)="createDeck()"
        ></my-input>
          <div class="flex gap-3 justify-end">
            <my-button [text]="'DECKS.CANCEL' | translate" variant="modern" size="medium" color="#64748b" (clicked)="closeCreateModal()"></my-button>
            <my-button [text]="'DECKS.CREATE' | translate" variant="modern" size="medium" [disabled]="creating() || !newDeckName().trim()" (clicked)="createDeck()"></my-button>
          </div>
      </div>
    </my-modal>

    <my-confirm-modal
      [open]="showDeleteModal()"
      [title]="'DECKS.DELETE_DECK' | translate"
      [message]="deleteMessage()"
      [confirmText]="'DECKS.DELETE_DECK' | translate"
      [cancelText]="'DECKS.CANCEL' | translate"
      [destructive]="true"
      [loading]="deleting()"
      variant="glass"
      backdropStyle="blur"
      (confirm)="deleteDeck()"
      (cancel)="closeDeleteModal()"
    ></my-confirm-modal>

    @if (flyCardData(); as fly) {
      <div
        class="fixed z-[2000] pointer-events-none rounded-xl overflow-hidden shadow-[0_0_30px_rgba(0,170,255,0.3),0_8px_32px_rgba(0,0,0,0.5)]"
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

    <my-modal
      [open]="showCardDetailModal()"
      [title]="selectedCardForDetail()?.name ?? ''"
      variant="glass"
      backdropStyle="blur"
      [backdropOpacity]="cardDetailBackdropOpacity()"
      [panelOpacity]="cardDetailPanelOpacity()"
      [entranceAnimated]="false"
      (close)="closeCardDetail()"
      width="46rem"
      [bodyOverflow]="cardDetailBodyOverflow()"
    >
      @if (selectedCardForDetail(); as card) {
        <div class="card-detail-layout grid grid-cols-2 gap-6 items-start">
          <div class="flex items-center justify-center">
            <div class="w-full max-w-[260px]">
              <app-deck-card-detail [card]="card" [count]="cardCountDetail()" />
            </div>
          </div>
          <div class="flex flex-col gap-2 max-h-[60vh] overflow-y-auto pr-1 scrollbar-none">
            @if (selectedCardFull(); as full) {
              @if (full.supertype === 'Pokemon') {
                <div class="flex items-center gap-3 pb-2">
                  @if (full.hp) {
                    <div class="inline-flex items-center gap-1 text-2xl font-bold text-red-500 bg-red-500/10 border-2 border-red-500/30 rounded-lg px-3 py-1 leading-tight">{{ full.hp }} <span class="text-sm font-normal text-[#cbd5e1]">HP</span></div>
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
                          <p class="text-xs text-[#cbd5e1] leading-tight m-0">{{ ability.text }}</p>
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
                            <p class="text-xs text-[#cbd5e1] leading-tight m-0">{{ attack.text }}</p>
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
                        <span class="text-xs text-[#cbd5e1] min-w-[5rem]">{{ 'DECKS.WEAKNESS' | translate }}</span>
                        <span class="text-sm text-text-primary font-semibold flex items-center gap-1">{{ full.weaknesses![0].type }} {{ full.weaknesses![0].value }}</span>
                      </div>
                    }
                    @if (full.resistances?.length) {
                      <div class="flex items-center gap-2">
                        <span class="text-xs text-[#cbd5e1] min-w-[5rem]">{{ 'DECKS.RESISTANCE' | translate }}</span>
                        <span class="text-sm text-text-primary font-semibold flex items-center gap-1">{{ full.resistances![0].type }} {{ full.resistances![0].value }}</span>
                      </div>
                    }
                    @if (full.retreatCost?.length) {
                      <div class="flex items-center gap-2">
                        <span class="text-xs text-[#cbd5e1] min-w-[5rem]">{{ 'DECKS.RETREAT' | translate }}</span>
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
                        <p class="text-xs text-[#cbd5e1] leading-tight m-0">{{ rule }}</p>
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
                      <span class="text-xs text-[#cbd5e1] min-w-[4rem]">{{ 'DECKS.RARITY' | translate }}</span>
                      <span class="text-sm text-text-primary font-semibold">{{ full.rarity }}</span>
                    </div>
                  }
                  <div class="flex items-center gap-2">
                    <span class="text-xs text-[#cbd5e1] min-w-[4rem]">{{ 'DECKS.SET' | translate }}</span>
                    <span class="text-sm text-text-primary font-semibold">{{ full.set.name }}</span>
                  </div>
                  @if (full.number) {
                    <div class="flex items-center gap-2">
                      <span class="text-xs text-[#cbd5e1] min-w-[4rem]">{{ 'DECKS.NUMBER' | translate }}</span>
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
      }
    </my-modal>

    <app-pokenav-device
      [isOpen]="showMenu() && !showOptions()"
      [compact]="true"
      (resume)="showMenu.set(false)"
      (showProfile)="onMenuProfile()"
      (showDecks)="showMenu.set(false)"
      (showOptions)="showMenu.set(false); showOptions.set(true)"
      (exit)="onMenuExit()"
      (resetCharacter)="showMenu.set(false)"
    ></app-pokenav-device>
    @if (showOptions()) {
      <app-game-options (back)="showOptions.set(false); showMenu.set(true)"></app-game-options>
    }
  `,
  styleUrl: './deck-list.page.css',
  changeDetection: ChangeDetectionStrategy.OnPush,
  host: {
    '(contextmenu)': '$event.preventDefault()',
  },
})
export class DeckListPage implements AfterViewInit {
  private readonly deckService = inject(DeckService);
  private readonly cardService = inject(CardService);
  private readonly destroyRef = inject(DestroyRef);
  private readonly router = inject(Router);
  private readonly audioMusic = inject(AudioMusicChannel);
  private readonly sfx = inject(AudioSfxChannel);
  private readonly toastService = inject(ToastService);
  // TranslateService permite traducir strings en el código TypeScript (no solo en templates)
  private readonly translateService = inject(TranslateService);
  private readonly langService = inject(LanguageService);
  readonly authService = inject(AuthService);
  private readonly storageService = inject(StorageService);
  private readonly circleTransition = inject(CircleTransitionService);
  private readonly activatedRoute = inject(ActivatedRoute);
  private cardFlyOriginRect: DOMRect | null = null;

  readonly createCardPlaceholder: DeckListItem = {
    id: '',
    name: '',
    cardCount: 0,
    valid: false,
    favorite: false,
    validationMessage: null,
  };

  // Las etiquetas de filtro son computed para reaccionar al cambio de idioma
  readonly filterOptions: { label: string; value: FilterMode }[] = [
    { label: 'DECKS.FILTER_ALL', value: 'ALL' },
    { label: 'DECKS.FILTER_FAVORITES', value: 'FAVORITES' },
    { label: 'DECKS.FILTER_VALID', value: 'VALID' },
  ];

  readonly decks = signal<DeckListItem[]>([]);
  readonly deckDetails = signal<Map<string, DeckDetail>>(new Map());
  readonly selectedDeckId = signal<string | null>(null);
  readonly loading = signal(false);
  readonly searchQuery = signal('');
  readonly filterMode = signal<FilterMode>('ALL');
  readonly showCreateModal = signal(false);
  readonly showDeleteModal = signal(false);
  readonly navigating = signal(false);
  readonly showMenu = signal(false);
  readonly showOptions = signal(false);

  @HostListener('document:keydown.escape')
  onEscapeKey(): void {
    if (this.showCreateModal() || this.showDeleteModal() || this.showCardDetailModal()) return;
    if (this.showOptions()) { this.showOptions.set(false); this.showMenu.set(true); return; }
    this.showMenu.update(v => !v);
  }

  onMenuProfile(): void { this.showMenu.set(false); this.router.navigate(['/profile']); }
  onMenuExit(): void { this.showMenu.set(false); this.goBackToGame(); }
  readonly newDeckName = signal('');
  readonly creating = signal(false);
  readonly deleting = signal(false);
  readonly deletingDeck = signal<DeckListItem | null>(null);
  readonly favoriteIds = computed(() => new Set(this.decks().filter(d => d.favorite).map(d => d.id)));
  readonly renamingDeckId = signal<string | null>(null);
  readonly renameValue = signal('');
  readonly selectedCardForDetail = signal<CardSummary | null>(null);
  readonly selectedCardFull = signal<Card | null>(null);
  readonly cardCountDetail = signal(0);
  readonly cardDetailBodyOverflow = signal('auto');
  readonly showCardDetailModal = signal(false);
  readonly cardDetailBackdropOpacity = signal(0);
  readonly cardDetailPanelOpacity = signal(1);
  readonly selectingDeck = signal(false);
  readonly flyCardData = signal<FlyCardData | null>(null);
  readonly flyPhase = signal<'entering' | 'exiting' | null>(null);

  private flyArrived = false;
  private cardDetailLoaded = false;
  private flyTimers: number[] = [];

  private scheduleFlyTimer(fn: () => void, ms: number): void {
    this.flyTimers.push(window.setTimeout(fn, ms));
  }

  private clearFlyTimers(): void {
    this.flyTimers.forEach(id => clearTimeout(id));
    this.flyTimers = [];
  }

  readonly deleteMessage = computed(() => {
    const name = this.deletingDeck()?.name ?? '';
    this.langService.currentLang();
    const prefix = this.translateService.instant('DECKS.DELETE_CONFIRM');
    const suffix = this.translateService.instant('DECKS.DELETE_CONFIRM_END');
    return `${prefix} "${name}"? ${suffix}`;
  });

  readonly selectedDeck = computed(() => {
    const id = this.selectedDeckId();
    if (!id) return null;
    return this.deckDetails().get(id) ?? null;
  });

  readonly filteredDecks = computed(() => {
    let list = this.decks();
    const query = this.searchQuery().toLowerCase().trim();
    const mode = this.filterMode();
    const favs = this.favoriteIds();

    if (query) {
      list = list.filter(d => d.name.toLowerCase().includes(query));
    }
    if (mode === 'FAVORITES') {
      list = list.filter(d => favs.has(d.id));
    }
    if (mode === 'VALID') {
      list = list.filter(d => d.valid);
    }

    return list;
  });

  readonly groupedCards = computed<GroupedCard[]>(() => {
    const deck = this.selectedDeck();
    if (!deck) return [];
    const map = new Map<string, GroupedCard>();
    for (const card of deck.cards) {
      const existing = map.get(card.id);
      if (existing) {
        existing.count++;
      } else {
        map.set(card.id, { ...card, count: 1 });
      }
    }
    return [...map.values()];
  });

  readonly validationChecks = computed<ValidationCheck[]>(() => {
    const deck = this.selectedDeck();
    if (!deck) return [];
    this.langService.currentLang();

    const cardCount = deck.cardCount;
    const hasPokemon = deck.cards.some(c => c.supertype === 'Pokemon');

    const cardCounts = new Map<string, number>();
    for (const card of deck.cards) {
      if (card.supertype === 'Energy') continue;
      cardCounts.set(card.id, (cardCounts.get(card.id) ?? 0) + 1);
    }
    const maxCopies = cardCounts.size > 0 ? Math.max(0, ...cardCounts.values()) : 0;
    const noExceedsMaxCopies = maxCopies <= 4;

    return [
      { label: this.translateService.instant('DECKS.VALIDATION_60_CARDS'), passed: cardCount === 60 },
      { label: this.translateService.instant('DECKS.VALIDATION_BASIC_POKEMON'), passed: hasPokemon },
      { label: this.translateService.instant('DECKS.VALIDATION_MAX_COPIES'), passed: noExceedsMaxCopies },
    ];
  });

  private loadDecks(): void {
    this.loading.set(true);
    this.deckService.getDecks()
      .pipe(
        finalize(() => this.loading.set(false)),
        takeUntilDestroyed(this.destroyRef),
      )
      .subscribe({
        next: ({ items, details }) => {
          this.decks.set(items);
          const detailMap = new Map<string, DeckDetail>();
          for (const detail of details) {
            detailMap.set(detail.id, detail);
          }
          this.deckDetails.set(detailMap);
          if (this.circleTransition.pendingOpen) {
            setTimeout(() => this.circleTransition.open());
          }
        },
        error: () => {
          this.decks.set([]);
          this.deckDetails.set(new Map());
        },
      });
  }

  setFilterMode(mode: string): void {
    this.sfx.play('Click.mp3');
    this.filterMode.set(mode as FilterMode);
  }

  selectDeck(id: string): void {
    this.sfx.play('Click.mp3');
    if (this.renamingDeckId()) {
      this.renamingDeckId.set(null);
    }
    this.selectedDeckId.set(id);
  }

  async goBackToGame(): Promise<void> {
    if (this.navigating()) return;
    this.navigating.set(true);
    this.sfx.play('Click.mp3');
    const from = this.activatedRoute.snapshot.queryParamMap.get('from');
    if (from === 'lobby') {
      this.router.navigate(['/lobby']);
      return;
    }
    const user = this.authService.currentUser();
    if (!user) return;
    const saved = this.storageService.getSavedPosition(user.username);
    const location = saved?.location ?? 'ciudadPrincipal';
    await this.circleTransition.close();
    this.router.navigate(['/juego', location], { queryParams: { user: user.username } });
  }

  selectAsActiveDeck(deckId: string): void {
    this.sfx.play('Click.mp3');
    this.selectingDeck.set(true);
    this.authService.setActiveDeck(deckId).pipe(
      finalize(() => this.selectingDeck.set(false)),
      takeUntilDestroyed(this.destroyRef),
    ).subscribe({
      next: () => {
        this.toastService.show(this.translateService.instant('DECKS.DECK_SELECTED_TOAST'), 'success');
        this.sfx.play('OperacionExitosa.mp3');
      },
    });
  }

  editDeck(id: string): void {
    this.sfx.play('Click.mp3');
    this.router.navigate(['/decks', id]);
  }

  toggleFavorite(id: string): void {
    this.sfx.play('Click.mp3');
    this.deckService.toggleFavorite(id)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: updated => {
          this.decks.update(list =>
            list.map(d => (d.id === id ? updated : d))
          );
          this.deckDetails.update(map => {
            const next = new Map(map);
            const detail = next.get(id);
            if (detail) {
              next.set(id, { ...detail, favorite: updated.favorite });
            }
            return next;
          });
        },
      });
  }

  startRename(id: string): void {
    this.sfx.play('Click.mp3');
    const deck = this.deckDetails().get(id);
    this.renameValue.set(deck?.name ?? '');
    this.renamingDeckId.set(id);
  }

  saveRename(): void {
    this.sfx.play('Click.mp3');
    const id = this.renamingDeckId();
    if (!id) return;

    const newName = this.renameValue().trim();
    if (!newName) return;

    this.deckService.renameDeck(id, newName)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: updated => {
          this.decks.update(list =>
            list.map(d => (d.id === id ? { ...d, name: updated.name } : d))
          );
          this.deckDetails.update(map => {
            const next = new Map(map);
            const detail = next.get(id);
            if (detail) {
              next.set(id, { ...detail, name: updated.name });
            }
            return next;
          });
          this.renamingDeckId.set(null);
        },
      });
  }

  cancelRename(): void {
    this.sfx.play('Click.mp3');
    this.renamingDeckId.set(null);
    this.renameValue.set('');
  }

  openCreateModal(): void {
    this.sfx.play('Click.mp3');
    this.newDeckName.set('');
    this.showCreateModal.set(true);
  }

  closeCreateModal(): void {
    this.sfx.play('Click.mp3');
    this.showCreateModal.set(false);
    this.newDeckName.set('');
  }

  createDeck(): void {
    this.sfx.play('Click.mp3');
    const name = this.newDeckName().trim();
    if (!name || this.creating()) return;

    this.creating.set(true);
    this.deckService.createDeck(name)
      .pipe(
        finalize(() => this.creating.set(false)),
        takeUntilDestroyed(this.destroyRef),
      )
      .subscribe({
        next: newDeck => {
          this.decks.update(list => [...list, newDeck]);
          this.deckDetails.update(map => {
            const next = new Map(map);
            next.set(newDeck.id, {
              id: newDeck.id,
              name: newDeck.name,
              cardCount: newDeck.cardCount,
              cards: [],
              valid: newDeck.valid,
              favorite: newDeck.favorite,
              validationMessage: newDeck.validationMessage,
            });
            return next;
          });
          this.selectedDeckId.set(newDeck.id);
          this.toastService.show(this.translateService.instant('DECKS.DECK_CREATED_TOAST'), 'success');
          this.sfx.play('OperacionExitosa.mp3');
          this.closeCreateModal();
        },
      });
  }

  openDeleteModal(deck: DeckListItem): void {
    this.sfx.play('Click.mp3');
    this.deletingDeck.set(deck);
    this.showDeleteModal.set(true);
  }

  closeDeleteModal(): void {
    this.sfx.play('Click.mp3');
    this.showDeleteModal.set(false);
    this.deletingDeck.set(null);
  }

  deleteDeck(): void {
    this.sfx.play('Click.mp3');
    const deck = this.deletingDeck();
    if (!deck || this.deleting()) return;

    this.deleting.set(true);
    this.deckService.deleteDeck(deck.id)
      .pipe(
        finalize(() => this.deleting.set(false)),
        takeUntilDestroyed(this.destroyRef),
      )
      .subscribe({
        next: () => {
          this.decks.update(list => list.filter(d => d.id !== deck.id));
          this.deckDetails.update(map => {
            const next = new Map(map);
            next.delete(deck.id);
            return next;
          });
          if (this.selectedDeckId() === deck.id) {
            this.selectedDeckId.set(null);
          }
          this.toastService.show(this.translateService.instant('DECKS.DECK_DELETED_TOAST'), 'success');
          this.sfx.play('OperacionExitosa.mp3');
          this.closeDeleteModal();
        },
      });
  }

  constructor() {
    this.loadDecks();
  }

  ngAfterViewInit(): void {
    this.audioMusic.play('../audio/MusicaFondoDeckList.mp3');
  }

  openCardDetail(event: MouseEvent, card: CardSummary, count: number = 0): void {
    this.sfx.play('Click.mp3');
    const target = event.currentTarget as HTMLElement;
    const imgWrapper = target.querySelector('.card-img-wrapper');
    const originRect = (imgWrapper ?? target).getBoundingClientRect();

    this.clearFlyTimers();
    this.flyPhase.set(null);
    this.flyArrived = false;
    this.cardDetailLoaded = false;
    this.flyCardData.set(null);
    this.cardFlyOriginRect = originRect;
    this.cardDetailBodyOverflow.set('visible');

    // Render modal invisible (backdrop + panel opacity 0)
    this.selectedCardForDetail.set(card);
    this.selectedCardFull.set(null);
    this.cardCountDetail.set(count);
    this.showCardDetailModal.set(true);
    this.cardDetailBackdropOpacity.set(0);
    this.cardDetailPanelOpacity.set(0);
    this.loadCardDetail(card.id);

    // Wait for Angular change detection to render modal, then measure
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

      this.scheduleFlyTimer(() => {
        if (this.flyPhase() === 'entering' && !this.flyArrived) {
          this.flyArrived = true;
          this.showModalAndFadePortal();
        }
      }, 900);

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
    this.flyCardData.set(null);
  }

  private loadCardDetail(cardId: string): void {
    this.cardService.getCardById(cardId).pipe(
      takeUntilDestroyed(this.destroyRef),
      timeout(3000),
      catchError(() => of(null)),
    ).subscribe({
      next: full => {
        if (full) { this.selectedCardFull.set(full); }
        this.cardDetailLoaded = true;
        this.showModalAndFadePortal();
      },
    });
  }

  closeCardDetail(): void {
    this.sfx.play('Click.mp3');
    const origin = this.cardFlyOriginRect;
    const card = this.selectedCardForDetail();
    if (!origin || !card) {
      this.resetCardDetailState();
      return;
    }

    // Measure card position in modal before closing
    const cardWrapper = document.querySelector('.card-detail-layout .card-3d-wrapper') as HTMLElement;
    const currentRect = cardWrapper?.getBoundingClientRect();

    // Close modal
    this.showCardDetailModal.set(false);
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

      this.scheduleFlyTimer(() => {
        if (this.flyPhase() !== 'exiting') return;
        requestAnimationFrame(() => {
          if (this.flyPhase() !== 'exiting') return;
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
      }, 0);
    } else {
      this.resetCardDetailState();
    }
  }

  private resetCardDetailState(): void {
    this.clearFlyTimers();
    this.flyArrived = false;
    this.cardDetailLoaded = false;
    this.cardFlyOriginRect = null;
    this.selectedCardForDetail.set(null);
    this.selectedCardFull.set(null);
    this.cardCountDetail.set(0);
    this.showCardDetailModal.set(false);
    this.cardDetailBackdropOpacity.set(0);
    this.cardDetailPanelOpacity.set(0);
    this.flyCardData.set(null);
    this.flyPhase.set(null);
    this.cardDetailBodyOverflow.set('auto');
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
