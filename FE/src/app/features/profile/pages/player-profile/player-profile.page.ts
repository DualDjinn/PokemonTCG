import { ChangeDetectionStrategy, Component, DestroyRef, HostListener, AfterViewInit, OnDestroy, computed, inject, signal } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { CardService } from '../../../pokedex/data-access/services/card.service';
import { Card } from '../../../pokedex/domain/models/card.model';
import { ReactiveFormsModule, FormControl, FormGroup, Validators } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { catchError, finalize, forkJoin, map, of, timeout } from 'rxjs';
import { AuthService } from '../../../game/data-access/services/auth.service';
import { CosmeticsService, PlayerCardBacks, PlayerSkin } from '../../data-access/cosmetics.service';
import { DeckSummary, PlayerResponse } from '../../../game/domain/models/player-response.model';
import { GameStateService } from '../../../game/data-access/services/game-state.service';
import { ToastService } from '../../../../shared/services/toast.service';
import { AudioMusicChannel } from '../../../../core/services/audio-music-channel.service';
import { AudioSfxChannel } from '../../../../core/services/audio-sfx-channel.service';
import { StorageService } from '../../../../core/services/storage.service';
import { CircleTransitionService } from '../../../../shared/services/circle-transition.service';
import { MyText } from '../../../../shared/ui/my-text/my-text';
import { MyBadge } from '../../../../shared/ui/my-badge/my-badge';
import { MySkeletonLoader } from '../../../../shared/components/my-skeleton-loader/my-skeleton-loader';
import { MyInput } from '../../../../shared/ui/my-input/my-input';
import { MyButton } from '../../../../shared/ui/my-button/my-button';
import { MyModal } from '../../../../shared/components/my-modal/my-modal';
import { MyFilterBar } from '../../../../shared/components/my-filter-bar/my-filter-bar';
import { MyCardThumbnail } from '../../../../shared/components/my-card-thumbnail/my-card-thumbnail';
import { MyPageBackground } from '../../../../shared/components/my-page-background/my-page-background';
import { TranslateModule, TranslateService } from '@ngx-translate/core';
import { LanguageService } from '../../../../core/services/language.service';
import { PokenavDeviceComponent } from '../../../game/components/pokenav-device/pokenav-device.component';
import { GameOptionsComponent } from '../../../game/components/game-options/game-options';
import { PixelPanelDirective, PixelBorderDirective } from '../../../../shared/directives';

type CollectionSupertypeFilter = 'ALL' | 'Pokemon' | 'Trainer' | 'Energy' | 'Pokesecret';

interface CollectionCard {
  id: string;
  name: string;
  supertype: string;
  rarity: string;
  imageUrl: string;
  isLocked?: boolean;
}

interface CollectionResponse {
  content: CollectionCard[];
  last: boolean;
  totalElements: number;
}

interface DeckListItem extends DeckSummary {
  valid: boolean;
  validationMessage: string | null;
}

interface DeckApiResponse {
  id?: string;
  name?: string;
  cardCount?: number;
  cardsCount?: number;
  totalCards?: number;
  valid?: boolean;
  isValid?: boolean;
  message?: string;
  error?: string;
  errors?: string[];
}

interface CollectionApiResponse {
  id?: string;
  name?: string;
  supertype?: string;
  rarity?: string;
  imageUrl?: string;
  images?: {
    small?: string;
    large?: string;
  };
}

interface CharOption {
  id: string;
  name: string;
  portrait: string;
  hasColor: boolean;
}

@Component({
  selector: 'app-player-profile-page',
  imports: [ReactiveFormsModule, RouterLink, MyText, MyBadge, MySkeletonLoader, MyInput, MyButton, MyModal, MyFilterBar, MyCardThumbnail, MyPageBackground, TranslateModule, PixelPanelDirective, PixelBorderDirective, PokenavDeviceComponent, GameOptionsComponent],
  template: `
    <my-page-background videoSrc="/assets/video/profileVideo.webm" poster="/assets/img/FondoPerfil.jpg" [overlayOpacity]="0.45" />
    <section class="max-w-[1200px] mx-auto px-8 pb-12 h-screen overflow-y-auto scrollbar-none">
      <div class="mb-4 pt-8">
        <my-button [text]="'PLAYER_PROFILE.BACK' | translate" variant="pixel" size="small" (clicked)="goBackToGame()"></my-button>
      </div>

      <header appPixelPanel class="grid grid-cols-[auto_minmax(0,1fr)_280px] gap-6 p-6 opacity-80">
        <div class="flex flex-col items-center gap-3 p-5 border-4 border-[var(--pixel-border-bottom-right)] border-t-[var(--pixel-border-top-left)] border-l-[var(--pixel-border-top-left)] bg-black/30">
          <img [src]="portraitUrl()" alt="Personaje" class="w-36 h-36 object-contain" />
          <my-text [text]="trainerName()" tag="h2" size="1.125rem" weight="bold" color="var(--text-primary)" align="center"></my-text>
        </div>

        <div class="flex flex-col justify-between h-full">
          <div class="flex flex-row gap-2 items-start">
            <my-button [text]="'PLAYER_PROFILE.CHANGE_NAME' | translate" variant="pixel" size="small" (clicked)="openNameModal()"></my-button>
            <my-button [text]="'PLAYER_PROFILE.CHANGE_CHARACTER' | translate" variant="pixel" size="small" (clicked)="openCharacterModal()"></my-button>
            <my-button [text]="'PLAYER_PROFILE.CHANGE_PASSWORD' | translate" variant="pixel" size="small" (clicked)="openPasswordModal()"></my-button>
          </div>
          <div class="grid grid-cols-3 gap-4 text-center justify-items-center">
            <div>
              <my-text [text]="'PLAYER_PROFILE.LEVEL' | translate" tag="span" size="0.675rem" weight="bold" color="var(--text-secondary)" [style.letterSpacing]="'0.18em'" [style.textTransform]="'uppercase'"></my-text>
              <div class="mt-1"><my-text [text]="levelLabel()" tag="span" weight="bold" size="1.0125rem" color="var(--text-primary)"></my-text></div>
            </div>
            <div>
              <my-text [text]="'PLAYER_PROFILE.EXPERIENCE' | translate" tag="span" size="0.675rem" weight="bold" color="var(--text-secondary)" [style.letterSpacing]="'0.18em'" [style.textTransform]="'uppercase'"></my-text>
              <div class="mt-1"><my-text [text]="experienceLabel()" tag="span" weight="bold" size="1.0125rem" color="var(--text-primary)"></my-text></div>
            </div>
            <div>
              <my-text [text]="'PLAYER_PROFILE.COINS' | translate" tag="span" size="0.675rem" weight="bold" color="var(--text-secondary)" [style.letterSpacing]="'0.18em'" [style.textTransform]="'uppercase'"></my-text>
              <div class="flex items-center justify-center gap-1.5 mt-1">
                <img src="/assets/img/Moneda.png" alt="" class="w-8 h-8 object-contain" />
                <my-text [text]="coinsLabel()" tag="span" weight="bold" size="1.0125rem" color="var(--neon-cyan)"></my-text>
              </div>
            </div>
          </div>
        </div>

        <aside class="grid gap-3 p-5 border-4 border-[var(--pixel-border-bottom-right)] border-t-[var(--pixel-border-top-left)] border-l-[var(--pixel-border-top-left)] bg-black/30">
          <my-text [text]="'PLAYER_PROFILE.COLLECTION_PROGRESS' | translate" tag="span" size="0.675rem" weight="bold" color="var(--text-secondary)" [style.letterSpacing]="'0.18em'" [style.textTransform]="'uppercase'"></my-text>
          <my-text [text]="cardProgressPercent() + '%'" tag="span" weight="bold" size="1.98rem" color="var(--neon-cyan)"></my-text>
          <my-text [text]="cardProgressLabel()" tag="p" size="0.7875rem" color="var(--text-secondary)"></my-text>
        </aside>
      </header>

      <div class="grid grid-cols-[minmax(0,1fr)_minmax(0,1fr)] gap-6 mt-6">
        <section appPixelPanel class="flex flex-col gap-4 p-6 opacity-80">
          <div>
            <my-text [text]="'PLAYER_PROFILE.COSMETICS' | translate" tag="span" size="0.675rem" weight="bold" color="var(--text-secondary)" [style.letterSpacing]="'0.18em'" [style.textTransform]="'uppercase'"></my-text>
            <my-text [text]="'PLAYER_PROFILE.CARD_BACKS' | translate" tag="h2" size="1.125rem" weight="bold" color="var(--text-primary)"></my-text>
          </div>

          @if (cosmeticsLoading()) {
            <my-skeleton-loader [lines]="2" height="8rem" gap="0.75rem"></my-skeleton-loader>
          } @else {
            <div class="grid grid-cols-[repeat(auto-fill,minmax(110px,1fr))] gap-3 p-2">
              <div class="relative flex flex-col items-center gap-1.5 cursor-pointer select-none" (click)="equipCardBack(null)">
                <div class="w-full rounded-lg border-2 flex items-center justify-center transition-all duration-300 bg-gradient-to-br from-[#16213e] to-[#0f3460]"
                     [style.borderColor]="activeCardBackId() === null ? 'var(--neon-cyan)' : 'rgba(255,255,255,0.15)'"
                     [style.boxShadow]="activeCardBackId() === null ? '0 0 12px rgba(0,204,170,0.5)' : 'none'"
                     style="aspect-ratio:63/88">
                  <span class="text-4xl font-bold text-white/25">?</span>
                </div>
                @if (activeCardBackId() === null) {
                  <div class="absolute top-1.5 right-1.5">
                    <my-badge [text]="'PLAYER_PROFILE.EQUIPPED' | translate" color="rgba(0,204,170,0.25)" textColor="#00ccaa" borderColor="rgba(0,204,170,0.5)"></my-badge>
                  </div>
                }
                <my-text [text]="'PLAYER_PROFILE.DEFAULT_BACK' | translate" tag="span" size="0.65rem" color="var(--text-secondary)" align="center"></my-text>
              </div>

              @for (back of ownedBacks(); track back.id) {
                <div class="relative flex flex-col items-center gap-1.5 cursor-pointer select-none" (click)="equipCardBack(back.id)">
                  <img [src]="back.imageUrl" [alt]="back.name" class="w-full rounded-lg border-2 transition-all duration-300 object-cover"
                       [style.borderColor]="activeCardBackId() === back.id ? 'var(--neon-cyan)' : 'rgba(255,255,255,0.15)'"
                       [style.boxShadow]="activeCardBackId() === back.id ? '0 0 12px rgba(0,204,170,0.5)' : 'none'"
                       style="aspect-ratio:63/88" />
                  @if (activeCardBackId() === back.id) {
                    <div class="absolute top-1.5 right-1.5">
                      <my-badge [text]="'PLAYER_PROFILE.EQUIPPED' | translate" color="rgba(0,204,170,0.25)" textColor="#00ccaa" borderColor="rgba(0,204,170,0.5)"></my-badge>
                    </div>
                  }
                  <my-text [text]="back.name" tag="span" size="0.65rem" color="var(--text-secondary)" align="center"></my-text>
                </div>
              }
            </div>
          }
        </section>

        <section appPixelPanel class="flex flex-col gap-4 p-6 opacity-80">
          <div>
            <my-text [text]="'PLAYER_PROFILE.COSMETICS' | translate" tag="span" size="0.675rem" weight="bold" color="var(--text-secondary)" [style.letterSpacing]="'0.18em'" [style.textTransform]="'uppercase'"></my-text>
            <my-text [text]="'PLAYER_PROFILE.FULL_ART' | translate" tag="h2" size="1.125rem" weight="bold" color="var(--text-primary)"></my-text>
          </div>

          @if (cosmeticsLoading()) {
            <my-skeleton-loader [lines]="2" height="8rem" gap="0.75rem"></my-skeleton-loader>
          } @else if (playerSkins().length === 0) {
            <div class="py-8"><my-text [text]="'PLAYER_PROFILE.NO_SKINS' | translate" tag="p" color="var(--text-secondary)" align="center"></my-text></div>
          } @else {
            <div class="grid grid-cols-[repeat(auto-fill,minmax(110px,1fr))] gap-3 p-2">
              @for (skin of playerSkins(); track skin.skinId) {
                <div class="relative flex flex-col items-center gap-1.5 cursor-pointer select-none" (click)="toggleSkin(skin)">
                  <img [src]="skin.imageUrl" [alt]="skin.skinName" class="w-full rounded-lg border-2 transition-all duration-300 object-cover"
                       [style.borderColor]="skin.equipped ? 'var(--neon-cyan)' : 'rgba(255,255,255,0.15)'"
                       [style.boxShadow]="skin.equipped ? '0 0 12px rgba(0,204,170,0.5)' : 'none'"
                       style="aspect-ratio:63/88" />
                  @if (skin.equipped) {
                    <div class="absolute top-1.5 right-1.5">
                      <my-badge [text]="'PLAYER_PROFILE.EQUIPPED' | translate" color="rgba(0,204,170,0.25)" textColor="#00ccaa" borderColor="rgba(0,204,170,0.5)"></my-badge>
                    </div>
                  }
                  <my-text [text]="skin.baseCardName || skin.skinName" tag="span" size="0.65rem" color="var(--text-secondary)" align="center"></my-text>
                </div>
              }
            </div>
          }
        </section>
      </div>

      <div class="grid grid-cols-[minmax(0,1.7fr)_minmax(280px,0.9fr)] gap-6 mt-6 sticky top-10 h-screen overflow-hidden grid-rows-[1fr]">
        <section appPixelPanel class="flex flex-col gap-4 p-6 opacity-80 min-h-0">
          <div class="flex items-center justify-between gap-4 flex-shrink-0">
            <div>
              <my-text [text]="'PLAYER_PROFILE.COLLECTION' | translate" tag="span" size="0.675rem" weight="bold" color="var(--text-secondary)" [style.letterSpacing]="'0.18em'" [style.textTransform]="'uppercase'"></my-text>
              <my-text [text]="'PLAYER_PROFILE.CARDS_OBTAINED' | translate" tag="h2" size="1.125rem" weight="bold" color="var(--text-primary)"></my-text>
            </div>

            <my-filter-bar
              [options]="supertypeFilters"
              [selected]="selectedFilter()"
              (selectedChange)="setFilter($event)"
            ></my-filter-bar>
          </div>

          <div class="overflow-y-auto scrollbar-none flex-1 min-h-0">
          @if (collectionLoading() && displayedCards().length === 0) {
            <my-skeleton-loader [lines]="4" height="10rem" gap="0.75rem"></my-skeleton-loader>
          } @else if (displayedCards().length === 0) {
            <div class="py-8"><my-text [text]="'PLAYER_PROFILE.NO_CARDS_FILTER' | translate" tag="p" color="var(--text-secondary)" align="center"></my-text></div>
          } @else {
            <!-- Layout en 2 columnas cuando hay carta seleccionada: grilla + panel detalle -->
            <div [class.collection-with-detail]="selectedCard()" class="collection-layout">
              <div class="collection-grid">
                @for (card of displayedCards(); track card.id) {
                  @if (card.isLocked) {
                    <div class="relative flex items-center justify-center rounded-lg bg-[#0a0a14] border border-[#00ccaa20] select-none" style="aspect-ratio:63/88">
                      <span class="text-5xl font-bold text-[#00ccaa30]">?</span>
                    </div>
                  } @else {
                    <div
                      class="card-thumb-wrapper"
                      [class.card-thumb-wrapper--selected]="selectedCard()?.id === card.id"
                      (click)="selectCard(card)"
                    >
                      <my-card-thumbnail
                        [imageUrl]="card.imageUrl"
                        [name]="card.name"
                        [rarity]="card.rarity"
                        [showCount]="false"
                        [showName]="false"
                        [hoverOverlay]="true"
                      ></my-card-thumbnail>
                    </div>
                  }
                }
              </div>

              @if (selectedCard(); as sel) {
                <aside class="card-detail-panel" appPixelPanel>
                  <div class="flex items-center justify-between mb-3">
                    <my-text [text]="'PLAYER_PROFILE.DETAIL' | translate" tag="span" size="0.65rem" weight="bold" color="var(--text-secondary)" [style.letterSpacing]="'0.18em'"></my-text>
                    <button type="button" class="close-detail-btn" (click)="closeCardDetail()" [attr.aria-label]="'COMMON.CLOSE' | translate">✕</button>
                  </div>

                  <img [src]="sel.imageUrl" [alt]="sel.name" class="w-full rounded-md mb-3" style="aspect-ratio:63/88; object-fit:contain" />

                  <my-text [text]="sel.name" tag="h3" size="1.2rem" weight="bold" color="var(--text-primary)"></my-text>

                  @if (selectedCardFull(); as full) {
                    <div class="flex items-center gap-2 mt-2 flex-wrap">
                      @if (full.hp) {
                        <div class="inline-flex items-center gap-1 text-base font-bold text-red-500 bg-red-500/10 border border-red-500/30 rounded px-2 py-0.5 leading-tight">
                          {{ full.hp }} <span class="text-[0.7rem] font-normal text-text-secondary">HP</span>
                        </div>
                      }
                      @if (full.types?.length) {
                        @for (t of full.types; track $index) {
                          <span class="px-3 py-0.5 rounded-full text-xs font-bold text-white bg-[var(--neon-cyan,#00aaff)]">{{ t }}</span>
                        }
                      }
                    </div>
                  }
                </aside>
              }
            </div>
          }

          @if (hasMoreCollection() && selectedFilter() !== 'Pokesecret') {
            <div class="flex justify-center flex-shrink-0">
              <my-button
                [text]="collectionLoading() ? ('PLAYER_PROFILE.LOADING' | translate) : ('PLAYER_PROFILE.LOAD_MORE' | translate)"
                variant="pixel"
                size="small"
                [disabled]="collectionLoading()"
                (clicked)="loadMoreCollection()"
              ></my-button>
            </div>
          }
          </div>
        </section>

        <section appPixelPanel class="flex flex-col gap-4 p-6 opacity-80 overflow-y-auto">
          <div class="flex items-center justify-between gap-4 flex-shrink-0">
            <div>
              <my-text [text]="'PLAYER_PROFILE.MY_DECKS' | translate" tag="span" size="0.675rem" weight="bold" color="var(--text-secondary)" [style.letterSpacing]="'0.18em'" [style.textTransform]="'uppercase'"></my-text>
              <my-text [text]="'PLAYER_PROFILE.READY_TO_PLAY' | translate" tag="h2" size="1.125rem" weight="bold" color="var(--text-primary)"></my-text>
            </div>
            <my-button [text]="'PLAYER_PROFILE.GO_TO_DECKS' | translate" variant="pixel" size="small" (clicked)="goToDecks()"></my-button>
          </div>

          @if (decksLoading()) {
            <my-skeleton-loader [lines]="3" height="4.5rem" gap="0.75rem"></my-skeleton-loader>
          } @else if (decks().length === 0) {
            <div class="py-8"><my-text [text]="'PLAYER_PROFILE.NO_DECKS' | translate" tag="p" color="var(--text-secondary)" align="center"></my-text></div>
          } @else {
            <div class="grid gap-3">
              @for (deck of decks(); track deck.id) {
                <a routerLink="/decks" appPixelBorder class="pixel-card-interactive">
                  <div class="grid gap-1">
                    <my-text [text]="deck.name" tag="span" weight="bold" size="0.844rem" color="var(--text-primary)"></my-text>
                    <my-text [text]="deck.cardCount + ' ' + ('PLAYER_PROFILE.CARDS' | translate)" tag="span" size="0.731rem" color="var(--text-secondary)"></my-text>
                  </div>
                  <div class="flex items-center gap-2">
                    @if (deck.valid) {
                      <my-badge [text]="'PLAYER_PROFILE.VALID' | translate" color="rgba(34,197,94,0.2)" textColor="#22c55e" borderColor="rgba(34,197,94,0.4)"></my-badge>
                    } @else {
                      <my-badge [text]="(deck.validationMessage || ('PLAYER_PROFILE.INVALID' | translate))" color="rgba(239,68,68,0.2)" textColor="#ef4444" borderColor="rgba(239,68,68,0.4)"></my-badge>
                    }
                  </div>
                </a>
              }
            </div>
          }
        </section>
      </div>
    </section>

    <my-modal [open]="showNameModal()" [title]="'PLAYER_PROFILE.CHANGE_NAME_TITLE' | translate" variant="pixel" (close)="closeNameModal()" width="28rem">
      <form [formGroup]="trainerNameForm" (ngSubmit)="saveNameFromModal()" class="flex flex-col gap-4">
        <my-input formControlName="trainerName" [placeholder]="'PLAYER_PROFILE.NAME_PLACEHOLDER' | translate" type="text" variant="pixel" color="var(--neon-cyan)"></my-input>
        <div class="flex gap-3 justify-end">
          <my-button [text]="'PLAYER_PROFILE.CANCEL' | translate" variant="pixel" size="small" (clicked)="closeNameModal()" [disabled]="savingTrainerName()"></my-button>
          <my-button [text]="'PLAYER_PROFILE.SAVE' | translate" variant="pixel" size="small" type="submit" [disabled]="savingTrainerName() || trainerNameForm.invalid"></my-button>
        </div>
      </form>
    </my-modal>

    <my-modal [open]="showCharacterModal()" [title]="'PLAYER_PROFILE.SELECT_CHARACTER' | translate" variant="pixel" (close)="closeCharacterModal()" width="32rem">
      <div class="flex flex-col gap-4">
        <div class="grid grid-cols-3 gap-2 justify-items-center">
          @for (char of characterOptions; track char.id) {
            <div class="flex flex-col items-center gap-1 p-3 border-4 cursor-pointer transition-all duration-300 bg-black/30 w-full max-w-[120px]"
                 [style.borderColor]="selectedChar() === char.id ? 'var(--pixel-border-bottom-right)' : 'var(--pixel-border-top-left)'"
                 [style.borderTopColor]="selectedChar() === char.id ? 'var(--pixel-border-hover-top-left)' : 'var(--pixel-border-top-left)'"
                 [style.borderLeftColor]="selectedChar() === char.id ? 'var(--pixel-border-hover-top-left)' : 'var(--pixel-border-top-left)'"
                 [style.boxShadow]="selectedChar() === char.id ? '0 0 15px rgba(0,170,255,0.4)' : 'none'"
                 (click)="selectCharInModal(char.id)">
              <img [src]="char.portrait" [alt]="char.name" class="w-[72px] h-auto rounded">
            </div>
          }
        </div>

        <div class="min-h-[44px] flex items-center justify-center">
          @if (showColorSelector()) {
            <div class="flex gap-6 justify-center min-h-[36px]">
              <div class="w-8 h-8 rounded-full border-2 cursor-pointer transition-all duration-300 hover:scale-110 relative"
                   [class.border-[#00ccaa]]="selectedCharColor() === 'rojo'"
                   [class.shadow-[0_0_12px_#00ccaa]]="selectedCharColor() === 'rojo'"
                   [class.scale-110]="selectedCharColor() === 'rojo'"
                   [class.border-[rgba(0,204,170,0.3)]]="selectedCharColor() !== 'rojo'"
                   style="background: #e03131"
                   (click)="selectColorInModal('rojo')">
                <span class="absolute -bottom-[22px] left-1/2 text-[0.585rem] text-text-secondary whitespace-nowrap -translate-x-1/2" [class.text-[#00ccaa]]="selectedCharColor() === 'rojo'">{{ 'PLAYER_PROFILE.COLOR_RED' | translate }}</span>
              </div>
              <div class="w-8 h-8 rounded-full border-2 cursor-pointer transition-all duration-300 hover:scale-110 relative"
                   [class.border-[#00ccaa]]="selectedCharColor() === 'verde'"
                   [class.shadow-[0_0_12px_#00ccaa]]="selectedCharColor() === 'verde'"
                   [class.scale-110]="selectedCharColor() === 'verde'"
                   [class.border-[rgba(0,204,170,0.3)]]="selectedCharColor() !== 'verde'"
                   style="background: #2f9e44"
                   (click)="selectColorInModal('verde')">
                <span class="absolute -bottom-[22px] left-1/2 text-[0.585rem] text-text-secondary whitespace-nowrap -translate-x-1/2" [class.text-[#00ccaa]]="selectedCharColor() === 'verde'">{{ 'PLAYER_PROFILE.COLOR_GREEN' | translate }}</span>
              </div>
              <div class="w-8 h-8 rounded-full border-2 cursor-pointer transition-all duration-300 hover:scale-110 relative"
                   [class.border-[#00ccaa]]="selectedCharColor() === 'violeta'"
                   [class.shadow-[0_0_12px_#00ccaa]]="selectedCharColor() === 'violeta'"
                   [class.scale-110]="selectedCharColor() === 'violeta'"
                   [class.border-[rgba(0,204,170,0.3)]]="selectedCharColor() !== 'violeta'"
                   style="background: #845ef7"
                   (click)="selectColorInModal('violeta')">
                <span class="absolute -bottom-[22px] left-1/2 text-[0.585rem] text-text-secondary whitespace-nowrap -translate-x-1/2" [class.text-[#00ccaa]]="selectedCharColor() === 'violeta'">{{ 'PLAYER_PROFILE.COLOR_PURPLE' | translate }}</span>
              </div>
            </div>
          }
        </div>

        <div class="flex gap-3 justify-end mt-2">
          <my-button [text]="'PLAYER_PROFILE.CANCEL' | translate" variant="pixel" size="small" (clicked)="closeCharacterModal()"></my-button>
          <my-button [text]="'PLAYER_PROFILE.SAVE' | translate" variant="pixel" size="small" (clicked)="saveCharacterModal()"></my-button>
        </div>
      </div>
    </my-modal>

    <my-modal [open]="showPasswordModal()" [title]="'PLAYER_PROFILE.CHANGE_PASSWORD_TITLE' | translate" variant="pixel" (close)="closePasswordModal()" width="30rem">
      <div class="flex flex-col gap-4">
        @if (!passwordEmailSent()) {
          <my-text [text]="'PLAYER_PROFILE.CHANGE_PASSWORD_DESC' | translate" tag="p" size="0.8rem" color="var(--text-secondary)"></my-text>
          <my-text [text]="userEmail()" tag="p" size="0.85rem" weight="bold" color="var(--neon-cyan)" align="center"></my-text>
          <div class="flex gap-3 justify-end">
            <my-button [text]="'PLAYER_PROFILE.CANCEL' | translate" variant="pixel" size="small" (clicked)="closePasswordModal()" [disabled]="sendingPasswordEmail()"></my-button>
            <my-button [text]="(sendingPasswordEmail() ? 'PLAYER_PROFILE.CHANGE_PASSWORD_SENDING' : 'PLAYER_PROFILE.CHANGE_PASSWORD_SEND') | translate" variant="pixel" size="small" (clicked)="sendPasswordResetEmail()" [disabled]="sendingPasswordEmail()"></my-button>
          </div>
        } @else {
          <my-text [text]="'PLAYER_PROFILE.CHANGE_PASSWORD_SENT' | translate" tag="p" size="0.8rem" color="var(--text-secondary)"></my-text>
          <my-text [text]="userEmail()" tag="p" size="0.85rem" weight="bold" color="var(--neon-cyan)" align="center"></my-text>
          <div class="flex justify-end">
            <my-button [text]="'PLAYER_PROFILE.CHANGE_PASSWORD_OK' | translate" variant="pixel" size="small" (clicked)="closePasswordModal()"></my-button>
          </div>
        }
      </div>
    </my-modal>

    <app-pokenav-device
      [isOpen]="showMenu() && !showOptions()"
      [compact]="true"
      (resume)="showMenu.set(false)"
      (showProfile)="showMenu.set(false)"
      (showDecks)="onMenuDecks()"
      (showOptions)="showMenu.set(false); showOptions.set(true)"
      (exit)="onMenuExit()"
      (resetCharacter)="showMenu.set(false)"
    ></app-pokenav-device>
    @if (showOptions()) {
      <app-game-options (back)="showOptions.set(false); showMenu.set(true)"></app-game-options>
    }
  `,
  styleUrl: './player-profile.page.css',
  changeDetection: ChangeDetectionStrategy.OnPush,
  host: {
    '(contextmenu)': '$event.preventDefault()',
  },
})
export class PlayerProfilePage implements AfterViewInit, OnDestroy {
  private readonly http = inject(HttpClient);
  private readonly authService = inject(AuthService);
  private readonly gameState = inject(GameStateService);
  private readonly destroyRef = inject(DestroyRef);
  private readonly toastService = inject(ToastService);
  private readonly audioMusic = inject(AudioMusicChannel);
  private readonly sfx = inject(AudioSfxChannel);
  private readonly router = inject(Router);
  private readonly storageService = inject(StorageService);
  private readonly circleTransition = inject(CircleTransitionService);
  private readonly translateService = inject(TranslateService);
  private readonly langService = inject(LanguageService);
  private readonly cosmetics = inject(CosmeticsService);
  private readonly cardService = inject(CardService);

  /** Carta seleccionada de la colección (thumbnail) para el panel lateral. */
  readonly selectedCard = signal<CollectionCard | null>(null);
  /** Detalle completo de la carta seleccionada (HP, ataques, etc.). Se carga via API. */
  readonly selectedCardFull = signal<Card | null>(null);
  /** Indica si el detalle de la carta seleccionada todavía se está cargando. */
  readonly selectedCardLoading = signal(false);

  private readonly collectionPageSize = 200;
  private readonly collectionPage = signal(0);
  private readonly collectionTotal = signal(0);

  readonly user = signal<PlayerResponse | null>(this.authService.currentUser());
  readonly decks = signal<DeckListItem[]>([]);
  readonly collectionCards = signal<CollectionCard[]>([]);
  readonly selectedFilter = signal<CollectionSupertypeFilter>('ALL');
  readonly collectionLoading = signal(false);
  readonly decksLoading = signal(false);
  readonly savingTrainerName = signal(false);
  readonly hasMoreCollection = signal(true);

  readonly cardBacks = signal<PlayerCardBacks | null>(null);
  readonly playerSkins = signal<PlayerSkin[]>([]);
  readonly cosmeticsLoading = signal(false);
  readonly savingCosmetic = signal(false);

  readonly activeCardBackId = computed(() => this.cardBacks()?.activeCardBackId ?? null);
  readonly ownedBacks = computed(() => this.cardBacks()?.owned ?? []);

  readonly showNameModal = signal(false);
  readonly showCharacterModal = signal(false);
  readonly showPasswordModal = signal(false);
  readonly sendingPasswordEmail = signal(false);
  readonly passwordEmailSent = signal(false);
  readonly navigating = signal(false);
  readonly showMenu = signal(false);
  readonly showOptions = signal(false);

  readonly userEmail = computed(() => this.user()?.email ?? '');

  @HostListener('document:keydown.escape')
  onEscapeKey(): void {
    if (this.showNameModal() || this.showCharacterModal() || this.showPasswordModal()) return;
    if (this.showOptions()) { this.showOptions.set(false); this.showMenu.set(true); return; }
    this.showMenu.update(v => !v);
  }

  onMenuDecks(): void { this.showMenu.set(false); this.router.navigate(['/decks']); }
  onMenuExit(): void { this.showMenu.set(false); this.goBackToGame(); }
  readonly selectedChar = signal('red');
  readonly selectedCharColor = signal('rojo');

  readonly COLOR_CHARS = ['red', 'green', 'ruby', 'sapphire'];

  readonly characterOptions: CharOption[] = [
    { id: 'red', name: 'Red', portrait: 'assets/img/portrait/RedPortrait.png', hasColor: true },
    { id: 'green', name: 'Green', portrait: 'assets/img/portrait/GreenPortrait.png', hasColor: true },
    { id: 'ruby', name: 'Ruby', portrait: 'assets/img/portrait/RubyPortrait.png', hasColor: true },
    { id: 'sapphire', name: 'Sapphire', portrait: 'assets/img/portrait/SapphirePortrait.png', hasColor: true },
    { id: 'gary', name: 'Gary', portrait: 'assets/img/portrait/BluePortrait.png', hasColor: false },
    { id: 'prima', name: 'Prima', portrait: 'assets/img/portrait/LoreleiPortrait.png', hasColor: false },
  ];

  private readonly PORTRAIT_MAP: Record<string, string> = {
    red: 'assets/img/portrait/RedPortrait.png',
    green: 'assets/img/portrait/GreenPortrait.png',
    ruby: 'assets/img/portrait/RubyPortrait.png',
    sapphire: 'assets/img/portrait/SapphirePortrait.png',
    gary: 'assets/img/portrait/BluePortrait.png',
    prima: 'assets/img/portrait/LoreleiPortrait.png',
  };

  readonly supertypeFilters = [
    { label: 'PLAYER_PROFILE.FILTER_ALL', value: 'ALL' as const },
    { label: 'PLAYER_PROFILE.FILTER_POKEMON', value: 'Pokemon' as const },
    { label: 'PLAYER_PROFILE.FILTER_TRAINER', value: 'Trainer' as const },
    { label: 'PLAYER_PROFILE.FILTER_ENERGY', value: 'Energy' as const },
    { label: 'PLAYER_PROFILE.FILTER_POKESECRET', value: 'Pokesecret' as const },
  ];

  readonly trainerNameForm = new FormGroup({
    trainerName: new FormControl('', { nonNullable: true, validators: [Validators.required, Validators.minLength(2)] })
  });

  readonly displayedCards = computed(() => {
    const filter = this.selectedFilter();
    if (filter === 'ALL') {
      return this.collectionCards();
    }
    if (filter === 'Pokesecret') {
      const owned = this.collectionCards().filter(card => card.id.startsWith('pokesecret-'));
      const ownedIds = new Set(owned.map(c => c.id));
      const placeholders: CollectionCard[] = this.POKESECRET_IDS
        .filter(id => !ownedIds.has(id))
        .map(id => ({ id, name: '???', supertype: 'Trainer', rarity: '???', imageUrl: '', isLocked: true }));
      return [...owned, ...placeholders];
    }

    return this.collectionCards().filter(card => card.supertype === filter);
  });

  readonly showColorSelector = computed(() => this.COLOR_CHARS.includes(this.selectedChar()));

  readonly characterData = computed(() => {
    const user = this.user();
    if (user?.character) {
      return { character: user.character, color: user.color ?? null };
    }
    if (user) {
      return this.gameState.getCharacter(user.username);
    }
    return null;
  });

  readonly portraitUrl = computed(() => {
    const char = this.characterData();
    if (!char?.character) {
      return 'assets/img/portrait/RedPortrait.png';
    }
    return this.PORTRAIT_MAP[char.character] ?? 'assets/img/portrait/RedPortrait.png';
  });

  readonly trainerName = computed(() => {
    this.langService.currentLang();
    return this.user()?.trainerName || this.translateService.instant('PLAYER_PROFILE.TRAINER_UNKNOWN');
  });
  readonly levelLabel = computed(() => {
    this.langService.currentLang();
    return this.translateService.instant('PLAYER_PROFILE.LEVEL') + ' ' + (this.user()?.level ?? 0);
  });
  readonly experienceLabel = computed(() => `${this.user()?.experience ?? 0} XP`);
  readonly coinsLabel = computed(() => `${this.user()?.coins ?? 0}`);
  private readonly TOTAL_COLLECTIBLE_CARDS = 146;
  private readonly POKESECRET_IDS = Array.from({ length: 16 }, (_, i) => `pokesecret-${i + 1}`);

  readonly cardProgressLabel = computed(() => {
    this.langService.currentLang();
    const ids = this.user()?.collectionCardIds;
    if (!ids) {
      return this.translateService.instant('PLAYER_PROFILE.PROGRESS_UNKNOWN');
    }

    return `${ids.length}/${this.TOTAL_COLLECTIBLE_CARDS} ${this.translateService.instant('PLAYER_PROFILE.CARDS_UNLOCKED')}`;
  });
  readonly cardProgressPercent = computed(() => {
    const ids = this.user()?.collectionCardIds;
    if (!ids || ids.length === 0) {
      return 0;
    }

    return Math.min(100, Math.round((ids.length / this.TOTAL_COLLECTIBLE_CARDS) * 100));
  });

  constructor() {
    this.loadProfileData();
    this.loadCosmetics();
  }

  equipCardBack(cardBackId: string | null): void {
    if (this.savingCosmetic() || this.activeCardBackId() === cardBackId) {
      return;
    }

    this.sfx.play('Click.mp3');
    this.savingCosmetic.set(true);
    this.cosmetics.equipCardBack(cardBackId)
      .pipe(
        finalize(() => this.savingCosmetic.set(false)),
        takeUntilDestroyed(this.destroyRef)
      )
      .subscribe({
        next: backs => {
          this.cardBacks.set(backs);
          this.toastService.show(this.translateService.instant('PLAYER_PROFILE.BACK_EQUIPPED'), 'success');
          this.sfx.play('OperacionExitosa.mp3');
        },
        error: () => {
          this.toastService.show(this.translateService.instant('PLAYER_PROFILE.COSMETIC_ERROR'), 'error');
        }
      });
  }

  toggleSkin(skin: PlayerSkin): void {
    if (this.savingCosmetic()) {
      return;
    }

    this.sfx.play('Click.mp3');
    this.savingCosmetic.set(true);
    const request = skin.equipped
      ? this.cosmetics.unequipSkin(skin.baseCardId)
      : this.cosmetics.equipSkin(skin.baseCardId, skin.skinId);

    request
      .pipe(
        finalize(() => this.savingCosmetic.set(false)),
        takeUntilDestroyed(this.destroyRef)
      )
      .subscribe({
        next: () => {
          // Una sola skin activa por carta base: equipar una desactiva las demás.
          this.playerSkins.update(skins => skins.map(s => {
            if (s.baseCardId !== skin.baseCardId) {
              return s;
            }
            return { ...s, equipped: !skin.equipped && s.skinId === skin.skinId };
          }));
          const key = skin.equipped ? 'PLAYER_PROFILE.SKIN_UNEQUIPPED' : 'PLAYER_PROFILE.SKIN_EQUIPPED';
          this.toastService.show(this.translateService.instant(key), 'success');
          this.sfx.play('OperacionExitosa.mp3');
        },
        error: () => {
          this.toastService.show(this.translateService.instant('PLAYER_PROFILE.COSMETIC_ERROR'), 'error');
        }
      });
  }

  private loadCosmetics(): void {
    this.cosmeticsLoading.set(true);

    forkJoin({
      backs: this.cosmetics.getPlayerCardBacks().pipe(catchError(() => of(null))),
      skins: this.cosmetics.getPlayerSkins().pipe(catchError(() => of([] as PlayerSkin[]))),
    })
      .pipe(
        finalize(() => this.cosmeticsLoading.set(false)),
        takeUntilDestroyed(this.destroyRef)
      )
      .subscribe(({ backs, skins }) => {
        this.cardBacks.set(backs);
        this.playerSkins.set(skins);
      });
  }

  ngAfterViewInit(): void {
    this.audioMusic.play('profile.opus');
  }

  ngOnDestroy(): void {
    this.audioMusic.stop();
  }

  setFilter(filter: string): void {
    if (this.selectedFilter() === filter) {
      return;
    }

    this.sfx.play('Click.mp3');
    this.selectedFilter.set(filter as CollectionSupertypeFilter);
    this.collectionPage.set(0);
    this.collectionCards.set([]);
    this.hasMoreCollection.set(true);
    this.loadCollectionPage();
    // Cerrar el panel de detalle al cambiar de filtro: la carta seleccionada
    // podría no aparecer ya en la grilla filtrada.
    this.closeCardDetail();
  }

  /**
   * Marca la carta como seleccionada y dispara la carga del detalle completo
   * (HP, ataques, habilidades, etc.) desde {@code /cards/:id}.
   */
  selectCard(card: CollectionCard): void {
    if (this.selectedCard()?.id === card.id) {
      // Re-click sobre la misma carta cierra el panel.
      this.closeCardDetail();
      return;
    }
    this.sfx.play('Click.mp3');
    this.selectedCard.set(card);
    this.selectedCardFull.set(null);
    this.selectedCardLoading.set(true);
    this.cardService.getCardById(card.id)
      .pipe(
        timeout(3000),
        catchError(() => of(null)),
        takeUntilDestroyed(this.destroyRef),
      )
      .subscribe({
        next: full => {
          if (full && this.selectedCard()?.id === card.id) {
            this.selectedCardFull.set(full);
          }
          this.selectedCardLoading.set(false);
        },
      });
  }

  /** Cierra el panel lateral de detalle. */
  closeCardDetail(): void {
    if (this.selectedCard()) {
      this.sfx.play('Click.mp3');
    }
    this.selectedCard.set(null);
    this.selectedCardFull.set(null);
    this.selectedCardLoading.set(false);
  }

  openNameModal(): void {
    this.sfx.play('Click.mp3');
    this.trainerNameForm.controls.trainerName.setValue(this.trainerName());
    this.trainerNameForm.markAsPristine();
    this.showNameModal.set(true);
  }

  closeNameModal(): void {
    this.sfx.play('Click.mp3');
    this.showNameModal.set(false);
    this.trainerNameForm.reset({ trainerName: this.trainerName() });
  }

  saveNameFromModal(): void {
    if (this.trainerNameForm.invalid || this.savingTrainerName()) {
      this.trainerNameForm.markAllAsTouched();
      return;
    }

    this.sfx.play('Click.mp3');
    const trainerName = this.trainerNameForm.controls.trainerName.getRawValue().trim();
    this.savingTrainerName.set(true);

    this.authService.updateTrainerName(trainerName)
      .pipe(
        finalize(() => this.savingTrainerName.set(false)),
        takeUntilDestroyed(this.destroyRef)
      )
      .subscribe({
        next: user => {
          this.user.set(user);
          this.showNameModal.set(false);
          this.toastService.show(this.translateService.instant('PLAYER_PROFILE.NAME_UPDATED'), 'success');
          this.sfx.play('OperacionExitosa.mp3');
        },
        error: () => {
          this.toastService.show(this.translateService.instant('PLAYER_PROFILE.NAME_UPDATE_ERROR'), 'error');
        }
      });
  }

  openCharacterModal(): void {
    this.sfx.play('Click.mp3');
    const charData = this.characterData();
    this.selectedChar.set(charData?.character ?? 'red');
    this.selectedCharColor.set(charData?.color ?? 'rojo');
    this.showCharacterModal.set(true);
  }

  closeCharacterModal(): void {
    this.sfx.play('Click.mp3');
    this.showCharacterModal.set(false);
  }

  selectCharInModal(id: string): void {
    this.sfx.play('Click.mp3');
    this.selectedChar.set(id);
  }

  selectColorInModal(color: string): void {
    this.sfx.play('Click.mp3');
    this.selectedCharColor.set(color);
  }

  saveCharacterModal(): void {
    this.sfx.play('Click.mp3');
    const character = this.selectedChar();
    const color = this.COLOR_CHARS.includes(character) ? this.selectedCharColor() : null;

    this.authService.updateCharacter(character, color)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: user => {
          this.user.set(user);
          const username = user.username;
          this.gameState.saveCharacter(username, {
            name: user.trainerName,
            character,
            color: color ?? undefined,
          });
          this.showCharacterModal.set(false);
          this.toastService.show(this.translateService.instant('PLAYER_PROFILE.CHARACTER_UPDATED'), 'success');
          this.sfx.play('OperacionExitosa.mp3');
        },
        error: () => {
          this.toastService.show(this.translateService.instant('PLAYER_PROFILE.CHARACTER_UPDATE_ERROR'), 'error');
        }
      });
  }

  openPasswordModal(): void {
    this.sfx.play('Click.mp3');
    this.passwordEmailSent.set(false);
    this.showPasswordModal.set(true);
  }

  closePasswordModal(): void {
    this.sfx.play('Click.mp3');
    this.showPasswordModal.set(false);
  }

  /**
   * Dispara el envío del email de cambio de contraseña al email del jugador
   * autenticado. Reutiliza el mismo flujo de token que "recuperar contraseña".
   */
  sendPasswordResetEmail(): void {
    if (this.sendingPasswordEmail()) {
      return;
    }

    this.sfx.play('Click.mp3');
    this.sendingPasswordEmail.set(true);

    this.authService.requestPasswordChange()
      .pipe(
        finalize(() => this.sendingPasswordEmail.set(false)),
        takeUntilDestroyed(this.destroyRef)
      )
      .subscribe({
        next: () => {
          this.passwordEmailSent.set(true);
          this.sfx.play('OperacionExitosa.mp3');
        },
        error: () => {
          this.toastService.show(this.translateService.instant('PLAYER_PROFILE.CHANGE_PASSWORD_ERROR'), 'error');
        }
      });
  }

  async goBackToGame(): Promise<void> {
    if (this.navigating()) return;
    this.navigating.set(true);
    this.sfx.play('Click.mp3');
    const user = this.authService.currentUser();
    if (!user) return;
    const saved = this.storageService.getSavedPosition(user.username);
    const location = saved?.location ?? 'ciudadPrincipal';
    await this.circleTransition.close();
    this.router.navigate(['/juego', location], { queryParams: { user: user.username } });
  }

  goToDecks(): void {
    this.sfx.play('Click.mp3');
    this.router.navigate(['/decks']);
  }

  loadMoreCollection(): void {
    this.sfx.play('Click.mp3');
    if (this.collectionLoading() || !this.hasMoreCollection()) {
      return;
    }

    this.collectionPage.update(page => page + 1);
    this.loadCollectionPage();
  }

  private loadProfileData(): void {
    this.collectionLoading.set(true);
    this.decksLoading.set(true);

    forkJoin({
      player: this.authService.getCurrentPlayer(),
      collection: this.fetchCollectionPage(0),
      decks: this.fetchDecks()
    })
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: ({ player, collection, decks }) => {
          this.user.set(player);
          this.collectionPage.set(0);
          this.collectionCards.set(collection.content);
          this.collectionTotal.set(collection.totalElements);
          this.hasMoreCollection.set(!collection.last);
          this.decks.set(decks);
          this.collectionLoading.set(false);
          this.decksLoading.set(false);
          if (this.circleTransition.pendingOpen) {
            setTimeout(() => this.circleTransition.open());
          }
        },
        error: () => {
          this.collectionLoading.set(false);
          this.decksLoading.set(false);
          this.toastService.show(this.translateService.instant('PLAYER_PROFILE.PROFILE_LOAD_ERROR'), 'error');
        }
      });
  }

  private loadCollectionPage(): void {
    this.collectionLoading.set(true);

    this.fetchCollectionPage(this.collectionPage())
      .pipe(
        finalize(() => this.collectionLoading.set(false)),
        takeUntilDestroyed(this.destroyRef)
      )
      .subscribe({
        next: collection => {
          this.collectionCards.update(cards => [...cards, ...collection.content]);
          this.collectionTotal.set(collection.totalElements);
          this.hasMoreCollection.set(!collection.last);
        },
        error: () => {
          this.collectionPage.update(page => Math.max(0, page - 1));
          this.toastService.show(this.translateService.instant('PLAYER_PROFILE.COLLECTION_LOAD_ERROR'), 'error');
        }
      });
  }

  private fetchCollectionPage(page: number) {
    const params = new HttpParams()
      .set('page', page)
      .set('size', this.collectionPageSize);

    return this.http.get<CollectionApiResponse[] | { content?: CollectionApiResponse[]; last?: boolean; totalElements?: number }>(
      '/api/players/me/collection',
      { params }
    ).pipe(map(response => this.normalizeCollectionResponse(response)));
  }

  private fetchDecks() {
    return this.http.get<DeckApiResponse[]>('/api/decks').pipe(
      map(decks => decks.map(deck => this.normalizeDeck(deck)))
    );
  }

  private normalizeCollectionResponse(
    response: CollectionApiResponse[] | { content?: CollectionApiResponse[]; last?: boolean; totalElements?: number }
  ): CollectionResponse {
    if (Array.isArray(response)) {
      return {
        content: response.map(card => this.normalizeCollectionCard(card)),
        last: response.length < this.collectionPageSize,
        totalElements: response.length
      };
    }

    const content = (response.content ?? []).map(card => this.normalizeCollectionCard(card));

    return {
      content,
      last: response.last ?? content.length < this.collectionPageSize,
      totalElements: response.totalElements ?? content.length,
    };
  }

  private normalizeCollectionCard(card: CollectionApiResponse): CollectionCard {
    return {
      id: card.id ?? crypto.randomUUID(),
      name: card.name ?? this.translateService.instant('PLAYER_PROFILE.CARD_UNKNOWN'),
      supertype: this.normalizeSupertype(card.supertype),
      rarity: card.rarity ?? this.translateService.instant('PLAYER_PROFILE.RARITY_UNKNOWN'),
      imageUrl: card.imageUrl ?? card.images?.small ?? card.images?.large ?? 'assets/img/portrait/Red.webp',
    };
  }

  private normalizeDeck(deck: DeckApiResponse): DeckListItem {
    const errors = deck.errors ?? [];
    const validationMessage = errors[0] ?? deck.message ?? deck.error ?? null;

    return {
      id: deck.id ?? crypto.randomUUID(),
      name: deck.name ?? this.translateService.instant('PLAYER_PROFILE.DECK_UNKNOWN'),
      cardCount: deck.cardCount ?? deck.cardsCount ?? deck.totalCards ?? 0,
      valid: deck.valid ?? deck.isValid ?? false,
      validationMessage,
    };
  }

  private normalizeSupertype(supertype: string | undefined): 'Pokemon' | 'Trainer' | 'Energy' {
    const s = supertype?.toUpperCase();
    if (s === 'TRAINER') return 'Trainer';
    if (s === 'ENERGY') return 'Energy';
    return 'Pokemon';
  }
}
