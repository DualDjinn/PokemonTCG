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

interface PlayerStatsResponse {
  matchesPlayed: number;
  matchesWon: number;
  matchesLost: number;
  matchesTied: number;
  totalMatchTimeMs: number;
  mostPlayedCard: string | null;
  mostUsedDeck: string | null;
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
  selector: 'app-player-profile-page',
  imports: [ReactiveFormsModule, RouterLink, MyText, MyBadge, MySkeletonLoader, MyInput, MyButton, MyModal, MyFilterBar, MyCardThumbnail, MyPageBackground, TranslateModule, PixelPanelDirective, PixelBorderDirective, PokenavDeviceComponent, GameOptionsComponent],
  template: `
    <my-page-background videoSrc="/assets/video/profileVideo.webm" poster="/assets/img/FondoPerfil.jpg" [overlayOpacity]="0.45" />
    <section class="max-w-[1200px] mx-auto px-8 pb-12 h-screen overflow-y-auto scrollbar-none">
      <div class="mb-4 pt-8">
        <my-button [text]="'PLAYER_PROFILE.BACK' | translate" variant="modern" size="small" (clicked)="goBackToGame()"></my-button>
      </div>

      <header appPixelPanel="glass" class="grid grid-cols-[auto_minmax(0,1fr)_280px] gap-6 p-6">
        <div class="flex flex-col items-center gap-3 p-5 rounded-2xl border border-white/20 bg-white/5">
          <img [src]="portraitUrl()" alt="Personaje" class="w-36 h-36 object-contain" />
          <my-text [text]="trainerName()" tag="h2" size="1.125rem" weight="bold" color="var(--text-primary)" align="center"></my-text>
        </div>

        <div class="flex flex-col justify-between h-full">
          <div class="flex flex-row gap-2 items-start">
            <my-button [text]="'PLAYER_PROFILE.CHANGE_NAME' | translate" variant="modern" size="small" (clicked)="openNameModal()"></my-button>
            <my-button [text]="'PLAYER_PROFILE.CHANGE_CHARACTER' | translate" variant="modern" size="small" (clicked)="openCharacterModal()"></my-button>
            <my-button [text]="'PLAYER_PROFILE.CHANGE_PASSWORD' | translate" variant="modern" size="small" (clicked)="openPasswordModal()"></my-button>
          </div>
          <div class="mt-2">
            <my-button [text]="'PLAYER_PROFILE.SHOW_STATS' | translate" variant="modern" size="small" (clicked)="openStatsModal()"></my-button>
          </div>
          <div class="grid grid-cols-3 gap-4 text-center justify-items-center">
            <div>
              <my-text [text]="'PLAYER_PROFILE.LEVEL' | translate" tag="span" size="0.675rem" weight="bold" color="#ffffff" [style.letterSpacing]="'0.18em'" [style.textTransform]="'uppercase'"></my-text>
              <div class="mt-1"><my-text [text]="levelLabel()" tag="span" weight="bold" size="1.0125rem" color="var(--text-primary)"></my-text></div>
            </div>
            <div>
              <my-text [text]="'PLAYER_PROFILE.EXPERIENCE' | translate" tag="span" size="0.675rem" weight="bold" color="#ffffff" [style.letterSpacing]="'0.18em'" [style.textTransform]="'uppercase'"></my-text>
              <div class="mt-1"><my-text [text]="experienceLabel()" tag="span" weight="bold" size="1.0125rem" color="var(--text-primary)"></my-text></div>
            </div>
            <div>
              <my-text [text]="'PLAYER_PROFILE.COINS' | translate" tag="span" size="0.675rem" weight="bold" color="#ffffff" [style.letterSpacing]="'0.18em'" [style.textTransform]="'uppercase'"></my-text>
              <div class="flex items-center justify-center gap-1.5 mt-1">
                <img src="/assets/img/Moneda.png" alt="" class="w-8 h-8 object-contain" />
                <my-text [text]="coinsLabel()" tag="span" weight="bold" size="1.0125rem" color="var(--neon-cyan)"></my-text>
              </div>
            </div>
          </div>
        </div>

        <aside class="grid gap-3 p-5 rounded-2xl border border-white/20 bg-white/5">
          <my-text [text]="'PLAYER_PROFILE.COLLECTION_PROGRESS' | translate" tag="span" size="0.675rem" weight="bold" color="#ffffff" [style.letterSpacing]="'0.18em'" [style.textTransform]="'uppercase'"></my-text>
          <my-text [text]="cardProgressPercent() + '%'" tag="span" weight="bold" size="1.98rem" color="var(--neon-cyan)"></my-text>
          <my-text [text]="cardProgressLabel()" tag="p" size="0.7875rem" color="#ffffff"></my-text>
        </aside>
      </header>

      <div class="grid grid-cols-[minmax(0,1fr)_minmax(0,1fr)] gap-6 mt-6">
        <section appPixelPanel="glass" class="flex flex-col gap-4 p-6">
          <div>
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
                <my-text [text]="'PLAYER_PROFILE.DEFAULT_BACK' | translate" tag="span" size="0.65rem" color="#ffffff" align="center"></my-text>
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
                  <my-text [text]="back.name" tag="span" size="0.65rem" color="#ffffff" align="center"></my-text>
                </div>
              }
            </div>
          }
        </section>

        <section appPixelPanel="glass" class="flex flex-col gap-4 p-6">
          <div>
            <my-text [text]="'PLAYER_PROFILE.FULL_ART' | translate" tag="h2" size="1.125rem" weight="bold" color="var(--text-primary)"></my-text>
          </div>

          @if (cosmeticsLoading()) {
            <my-skeleton-loader [lines]="2" height="8rem" gap="0.75rem"></my-skeleton-loader>
          } @else if (playerSkins().length === 0) {
            <div class="py-8"><my-text [text]="'PLAYER_PROFILE.NO_SKINS' | translate" tag="p" color="#ffffff" align="center"></my-text></div>
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
                  <my-text [text]="skin.baseCardName || skin.skinName" tag="span" size="0.65rem" color="#ffffff" align="center"></my-text>
                </div>
              }
            </div>
          }
        </section>
      </div>

      <div class="grid grid-cols-[minmax(0,1.7fr)_minmax(280px,0.9fr)] gap-6 mt-6 sticky top-10 h-screen overflow-hidden grid-rows-[1fr]">
        <section appPixelPanel="glass" class="flex flex-col gap-4 p-6 min-h-0">
          <div class="flex items-center justify-between gap-4 flex-shrink-0">
            <div>
              <my-text [text]="'PLAYER_PROFILE.CARDS_OBTAINED' | translate" tag="h2" size="1.125rem" weight="bold" color="var(--text-primary)"></my-text>
            </div>

            <my-filter-bar
              [options]="supertypeFilters"
              [selected]="selectedFilter()"
              variant="modern"
              color="#0284c7"
              (selectedChange)="setFilter($event)"
            ></my-filter-bar>
          </div>

          <div class="overflow-y-auto scrollbar-none flex-1 min-h-0">
          @if (collectionLoading() && displayedCards().length === 0) {
            <my-skeleton-loader [lines]="4" height="10rem" gap="0.75rem"></my-skeleton-loader>
          } @else if (displayedCards().length === 0) {
            <div class="py-8"><my-text [text]="'PLAYER_PROFILE.NO_CARDS_FILTER' | translate" tag="p" color="#ffffff" align="center"></my-text></div>
          } @else {
            <div class="collection-layout">
              <div class="collection-grid">
                @for (card of displayedCards(); track card.id) {
                  @if (card.isLocked) {
                    <div class="relative flex items-center justify-center rounded-lg bg-[#0a0a14] border border-[#00ccaa20] select-none" style="aspect-ratio:63/88">
                      <span class="text-5xl font-bold text-[#00ccaa30]">?</span>
                    </div>
                  } @else {
                    <div
                      class="card-thumb-wrapper"
                      (click)="openCardDetail($event, card)"
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
            </div>
          }

          @if (hasMoreCollection() && selectedFilter() !== 'Pokesecret') {
            <div class="flex justify-center flex-shrink-0">
              <my-button
                [text]="collectionLoading() ? ('PLAYER_PROFILE.LOADING' | translate) : ('PLAYER_PROFILE.LOAD_MORE' | translate)"
                variant="modern"
                size="small"
                [disabled]="collectionLoading()"
                (clicked)="loadMoreCollection()"
              ></my-button>
            </div>
          }
          </div>
        </section>

        <section appPixelPanel="glass" class="flex flex-col gap-4 p-6 overflow-y-auto">
          <div class="flex items-center justify-between gap-4 flex-shrink-0">
            <div>
              <my-text [text]="'PLAYER_PROFILE.READY_TO_PLAY' | translate" tag="h2" size="1.125rem" weight="bold" color="var(--text-primary)"></my-text>
            </div>
            <my-button [text]="'PLAYER_PROFILE.GO_TO_DECKS' | translate" variant="modern" size="small" (clicked)="goToDecks()"></my-button>
          </div>

          @if (decksLoading()) {
            <my-skeleton-loader [lines]="3" height="4.5rem" gap="0.75rem"></my-skeleton-loader>
          } @else if (decks().length === 0) {
            <div class="py-8"><my-text [text]="'PLAYER_PROFILE.NO_DECKS' | translate" tag="p" color="#ffffff" align="center"></my-text></div>
          } @else {
            <div class="grid gap-3">
              @for (deck of decks(); track deck.id) {
                <a routerLink="/decks" appPixelBorder class="pixel-card-interactive">
                  <div class="grid gap-1">
                    <my-text [text]="deck.name" tag="span" weight="bold" size="0.844rem" color="var(--text-primary)"></my-text>
                    <my-text [text]="deck.cardCount + ' ' + ('PLAYER_PROFILE.CARDS' | translate)" tag="span" size="0.731rem" color="#ffffff"></my-text>
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

    <my-modal [open]="showNameModal()" [title]="'PLAYER_PROFILE.CHANGE_NAME_TITLE' | translate" variant="glass" backdropStyle="blur" (close)="closeNameModal()" width="28rem">
      <form [formGroup]="trainerNameForm" (ngSubmit)="saveNameFromModal()" class="flex flex-col gap-4">
        <my-input formControlName="trainerName" [placeholder]="'PLAYER_PROFILE.NAME_PLACEHOLDER' | translate" type="text" variant="modern" color="var(--neon-cyan)"></my-input>
        <div class="flex gap-3 justify-end">
          <my-button [text]="'PLAYER_PROFILE.CANCEL' | translate" variant="modern" size="small" (clicked)="closeNameModal()" [disabled]="savingTrainerName()"></my-button>
          <my-button [text]="'PLAYER_PROFILE.SAVE' | translate" variant="modern" size="small" type="submit" [disabled]="savingTrainerName() || trainerNameForm.invalid"></my-button>
        </div>
      </form>
    </my-modal>

    <my-modal [open]="showStatsModal()" [title]="'PLAYER_PROFILE.STATS_TITLE' | translate" variant="glass" backdropStyle="blur" (close)="closeStatsModal()" width="30rem">
      @if (statsLoading()) {
        <div class="py-8 flex justify-center">
          <div class="w-8 h-8 border-4 border-white/10 border-t-[var(--neon-cyan)] rounded-full animate-spin"></div>
        </div>
      } @else if (playerStats(); as st) {
        <div class="flex flex-col gap-2.5">
          <div class="flex items-center justify-between gap-3 rounded-xl border border-white/15 px-3 py-2" style="background: rgba(255,255,255,0.06);">
            <span class="font-orbitron text-[0.7rem] text-[#ffffff]">Nivel</span>
            <div class="flex flex-col items-end gap-1">
              <span class="font-orbitron text-[0.85rem] font-bold text-[var(--text-primary)]">{{ user()?.level ?? 0 }}</span>
              <div class="w-28 h-1.5 rounded-full overflow-hidden" style="background: rgba(255,255,255,0.12);">
                <div class="h-full rounded-full bg-gradient-to-r from-neon-cyan to-neon-magenta transition-all duration-300" [style.width.%]="xpIntoCurrentPercent()"></div>
              </div>
              <span class="font-share-tech text-[0.6rem] text-[#ffffff]">{{ user()?.xpIntoCurrentLevel ?? 0 }} / {{ user()?.xpForNextLevel ?? 0 }} XP</span>
            </div>
          </div>
          <div class="flex items-center justify-between rounded-xl border border-white/15 px-3 py-2" style="background: rgba(255,255,255,0.06);">
            <span class="font-orbitron text-[0.7rem] text-[#ffffff]">Partidas jugadas</span>
            <span class="font-orbitron text-[0.85rem] font-bold">{{ st.matchesPlayed }}</span>
          </div>
          <div class="flex items-center justify-between rounded-xl border border-white/15 px-3 py-2" style="background: rgba(255,255,255,0.06);">
            <span class="font-orbitron text-[0.7rem] text-[#ffffff]">Partidas ganadas</span>
            <span class="font-orbitron text-[0.85rem] font-bold text-[#4ade80]">{{ st.matchesWon }}</span>
          </div>
          <div class="flex items-center justify-between rounded-xl border border-white/15 px-3 py-2" style="background: rgba(255,255,255,0.06);">
            <span class="font-orbitron text-[0.7rem] text-[#ffffff]">Partidas perdidas</span>
            <span class="font-orbitron text-[0.85rem] font-bold text-[#f87171]">{{ st.matchesLost }}</span>
          </div>
          <div class="flex items-center justify-between rounded-xl border border-white/15 px-3 py-2" style="background: rgba(255,255,255,0.06);">
            <span class="font-orbitron text-[0.7rem] text-[#ffffff]">Partidas empatadas</span>
            <span class="font-orbitron text-[0.85rem] font-bold text-[#fbbf24]">{{ st.matchesTied }}</span>
          </div>
          <div class="flex items-center justify-between rounded-xl border border-white/15 px-3 py-2" style="background: rgba(255,255,255,0.06);">
            <span class="font-orbitron text-[0.7rem] text-[#ffffff]">Tiempo jugado</span>
            <span class="font-orbitron text-[0.85rem] font-bold">{{ formatMatchTime(st.totalMatchTimeMs) }}</span>
          </div>
          <div class="flex items-center justify-between rounded-xl border border-white/15 px-3 py-2" style="background: rgba(255,255,255,0.06);">
            <span class="font-orbitron text-[0.7rem] text-[#ffffff]">Carta más jugada</span>
            <span class="font-orbitron text-[0.85rem] font-bold">{{ st.mostPlayedCard || '—' }}</span>
          </div>
          <div class="flex items-center justify-between rounded-xl border border-white/15 px-3 py-2" style="background: rgba(255,255,255,0.06);">
            <span class="font-orbitron text-[0.7rem] text-[#ffffff]">Mazo actual</span>
            <span class="font-orbitron text-[0.85rem] font-bold">{{ currentDeckName() }}</span>
          </div>
          <div class="flex items-center justify-between rounded-xl border border-white/15 px-3 py-2" style="background: rgba(255,255,255,0.06);">
            <span class="font-orbitron text-[0.7rem] text-[#ffffff]">Mazo más utilizado</span>
            <span class="font-orbitron text-[0.85rem] font-bold">{{ st.mostUsedDeck || '—' }}</span>
          </div>
          <div class="flex items-center justify-between rounded-xl border border-white/15 px-3 py-2" style="background: rgba(255,255,255,0.06);">
            <span class="font-orbitron text-[0.7rem] text-[#ffffff]">Mazos construidos</span>
            <span class="font-orbitron text-[0.85rem] font-bold">{{ user()?.decks?.length ?? 0 }}</span>
          </div>
          <div class="flex items-center justify-between rounded-xl border border-white/15 px-3 py-2" style="background: rgba(255,255,255,0.06);">
            <span class="font-orbitron text-[0.7rem] text-[#ffffff]">Colección</span>
            <span class="font-orbitron text-[0.85rem] font-bold">{{ user()?.collectionCardIds?.length ?? 0 }} / {{ TOTAL_COLLECTIBLE_CARDS }}</span>
          </div>
          <div class="flex items-center justify-between rounded-xl border border-white/15 px-3 py-2" style="background: rgba(255,255,255,0.06);">
            <span class="font-orbitron text-[0.7rem] text-[#ffffff]">% colección</span>
            <span class="font-orbitron text-[0.85rem] font-bold">{{ cardProgressPercent() }}%</span>
          </div>
          <div class="flex items-center justify-between rounded-xl border border-white/15 px-3 py-2" style="background: rgba(255,255,255,0.06);">
            <span class="font-orbitron text-[0.7rem] text-[#ffffff]">Monedas</span>
            <span class="font-orbitron text-[0.85rem] font-bold text-[var(--neon-cyan)]">{{ coinsLabel() }}</span>
          </div>
          <div class="flex justify-end mt-1">
            <my-button [text]="'PLAYER_PROFILE.CANCEL' | translate" variant="modern" size="small" (clicked)="closeStatsModal()"></my-button>
          </div>
        </div>
      }
    </my-modal>

    <my-modal [open]="showCharacterModal()" [title]="'PLAYER_PROFILE.SELECT_CHARACTER' | translate" variant="glass" backdropStyle="blur" (close)="closeCharacterModal()" width="32rem">
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
                <span class="absolute -bottom-[22px] left-1/2 text-[0.585rem] text-[#ffffff] whitespace-nowrap -translate-x-1/2" [class.text-[#00ccaa]]="selectedCharColor() === 'rojo'">{{ 'PLAYER_PROFILE.COLOR_RED' | translate }}</span>
              </div>
              <div class="w-8 h-8 rounded-full border-2 cursor-pointer transition-all duration-300 hover:scale-110 relative"
                   [class.border-[#00ccaa]]="selectedCharColor() === 'verde'"
                   [class.shadow-[0_0_12px_#00ccaa]]="selectedCharColor() === 'verde'"
                   [class.scale-110]="selectedCharColor() === 'verde'"
                   [class.border-[rgba(0,204,170,0.3)]]="selectedCharColor() !== 'verde'"
                   style="background: #2f9e44"
                   (click)="selectColorInModal('verde')">
                <span class="absolute -bottom-[22px] left-1/2 text-[0.585rem] text-[#ffffff] whitespace-nowrap -translate-x-1/2" [class.text-[#00ccaa]]="selectedCharColor() === 'verde'">{{ 'PLAYER_PROFILE.COLOR_GREEN' | translate }}</span>
              </div>
              <div class="w-8 h-8 rounded-full border-2 cursor-pointer transition-all duration-300 hover:scale-110 relative"
                   [class.border-[#00ccaa]]="selectedCharColor() === 'violeta'"
                   [class.shadow-[0_0_12px_#00ccaa]]="selectedCharColor() === 'violeta'"
                   [class.scale-110]="selectedCharColor() === 'violeta'"
                   [class.border-[rgba(0,204,170,0.3)]]="selectedCharColor() !== 'violeta'"
                   style="background: #845ef7"
                   (click)="selectColorInModal('violeta')">
                <span class="absolute -bottom-[22px] left-1/2 text-[0.585rem] text-[#ffffff] whitespace-nowrap -translate-x-1/2" [class.text-[#00ccaa]]="selectedCharColor() === 'violeta'">{{ 'PLAYER_PROFILE.COLOR_PURPLE' | translate }}</span>
              </div>
            </div>
          }
        </div>

        <div class="flex gap-3 justify-end mt-2">
          <my-button [text]="'PLAYER_PROFILE.CANCEL' | translate" variant="modern" size="small" (clicked)="closeCharacterModal()"></my-button>
          <my-button [text]="'PLAYER_PROFILE.SAVE' | translate" variant="modern" size="small" (clicked)="saveCharacterModal()"></my-button>
        </div>
      </div>
    </my-modal>

    <my-modal [open]="showPasswordModal()" [title]="'PLAYER_PROFILE.CHANGE_PASSWORD_TITLE' | translate" variant="glass" backdropStyle="blur" (close)="closePasswordModal()" width="30rem">
      <div class="flex flex-col gap-4">
        @if (!passwordEmailSent()) {
          <my-text [text]="'PLAYER_PROFILE.CHANGE_PASSWORD_DESC' | translate" tag="p" size="0.8rem" color="#ffffff"></my-text>
          <my-text [text]="userEmail()" tag="p" size="0.85rem" weight="bold" color="var(--neon-cyan)" align="center"></my-text>
          <div class="flex gap-3 justify-end">
            <my-button [text]="'PLAYER_PROFILE.CANCEL' | translate" variant="modern" size="small" (clicked)="closePasswordModal()" [disabled]="sendingPasswordEmail()"></my-button>
            <my-button [text]="(sendingPasswordEmail() ? 'PLAYER_PROFILE.CHANGE_PASSWORD_SENDING' : 'PLAYER_PROFILE.CHANGE_PASSWORD_SEND') | translate" variant="modern" size="small" (clicked)="sendPasswordResetEmail()" [disabled]="sendingPasswordEmail()"></my-button>
          </div>
        } @else {
          <my-text [text]="'PLAYER_PROFILE.CHANGE_PASSWORD_SENT' | translate" tag="p" size="0.8rem" color="#ffffff"></my-text>
          <my-text [text]="userEmail()" tag="p" size="0.85rem" weight="bold" color="var(--neon-cyan)" align="center"></my-text>
          <div class="flex justify-end">
            <my-button [text]="'PLAYER_PROFILE.CHANGE_PASSWORD_OK' | translate" variant="modern" size="small" (clicked)="closePasswordModal()"></my-button>
          </div>
        }
      </div>
    </my-modal>

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
      [title]="selectedCard()?.name ?? ''"
      variant="glass"
      backdropStyle="blur"
      [backdropOpacity]="cardDetailBackdropOpacity()"
      [panelOpacity]="cardDetailPanelOpacity()"
      [entranceAnimated]="false"
      (close)="closeCardDetail()"
      width="46rem"
      [bodyOverflow]="cardDetailBodyOverflow()"
    >
      @if (selectedCard(); as sel) {
        <div class="card-detail-layout grid grid-cols-2 gap-6 items-start">
          <div class="flex items-center justify-center">
            <div class="card-3d-wrapper w-full max-w-[260px]">
              @if (sel.imageUrl) {
                <img [src]="sel.imageUrl" [alt]="sel.name" class="w-full rounded-lg" style="aspect-ratio:63/88; object-fit:contain" />
              } @else {
                <div class="w-full rounded-lg flex items-center justify-center text-4xl text-[#00ccaa] bg-gradient-to-br from-[rgba(0,204,170,0.1)] to-[rgba(0,0,0,0.4)]" style="aspect-ratio:63/88">?</div>
              }
            </div>
          </div>
          <div class="flex flex-col gap-3 max-h-[60vh] overflow-y-auto pr-1 scrollbar-none">
            @if (selectedCardLoading()) {
              <div class="py-6 text-center text-[#cbd5e1] font-share-tech text-xs">Cargando detalle…</div>
            } @else if (selectedCardFull(); as full) {
              <div>
                <my-text [text]="sel.name" tag="h3" size="1.2rem" weight="bold" color="var(--text-primary)"></my-text>
                <div class="flex items-center gap-2 mt-2 flex-wrap">
                  @if (full.hp) {
                    <div class="inline-flex items-center gap-1 text-base font-bold text-red-500 bg-red-500/10 border border-red-500/30 rounded px-2 py-0.5 leading-tight">
                      {{ full.hp }} <span class="text-[0.7rem] font-normal text-[#ffffff]">HP</span>
                    </div>
                  }
                  @if (full.types?.length) {
                    @for (t of full.types; track $index) {
                      <span class="px-3 py-0.5 rounded-full text-xs font-bold text-white bg-[var(--neon-cyan,#00aaff)]">{{ t }}</span>
                    }
                  }
                </div>
              </div>

              @if (full.attacks?.length) {
                <div>
                  <span class="font-orbitron text-[0.7rem] text-[#ffffff] uppercase tracking-wider">Ataques</span>
                  @for (atk of full.attacks; track $index) {
                    <div class="mt-1 rounded-lg border border-white/15 px-3 py-2" style="background: rgba(255,255,255,0.06);">
                      <div class="flex items-center justify-between gap-2">
                        <span class="font-orbitron text-[0.8rem] font-bold">{{ atk.name }}</span>
                        <span class="font-orbitron text-[0.8rem] font-bold text-red-400">{{ atk.damage || '—' }}</span>
                      </div>
                    </div>
                  }
                </div>
              }

              @if (full.abilities?.length) {
                <div>
                  <span class="font-orbitron text-[0.7rem] text-[#ffffff] uppercase tracking-wider">Habilidad</span>
                  @for (ab of full.abilities; track $index) {
                    <div class="mt-1 rounded-lg border border-white/15 px-3 py-2" style="background: rgba(255,255,255,0.06);">
                      <div class="font-orbitron text-[0.8rem] font-bold">{{ ab.name }}</div>
                      <div class="text-[0.72rem] text-[#cbd5e1]">{{ ab.text }}</div>
                    </div>
                  }
                </div>
              }
            }
          </div>
        </div>
      }
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

  /** Carta seleccionada de la colección (thumbnail) para el modal. */
  readonly selectedCard = signal<CollectionCard | null>(null);
  /** Detalle completo de la carta seleccionada (HP, ataques, etc.). Se carga via API. */
  readonly selectedCardFull = signal<Card | null>(null);
  /** Indica si el detalle de la carta seleccionada todavía se está cargando. */
  readonly selectedCardLoading = signal(false);

  readonly flyCardData = signal<FlyCardData | null>(null);
  readonly flyPhase = signal<'entering' | 'exiting' | null>(null);
  private cardFlyOriginRect: { left: number; top: number; width: number; height: number } | null = null;
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
  readonly showCardDetailModal = signal(false);
  readonly cardDetailBackdropOpacity = signal(0);
  readonly cardDetailPanelOpacity = signal(1);
  readonly cardDetailBodyOverflow = signal('auto');

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
  readonly showStatsModal = signal(false);
  readonly statsLoading = signal(false);
  readonly playerStats = signal<PlayerStatsResponse | null>(null);

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
  readonly TOTAL_COLLECTIBLE_CARDS = 146;
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
    // Cerrar el detalle al cambiar de filtro: la carta seleccionada
    // podría no aparecer ya en la grilla filtrada.
    this.resetCardDetailState();
  }

  /**
   * Abre el modal de detalle de la carta con la animación de "carta voladora"
   * (misma mecánica que DeckListPage): la carta clickeada vuela hacia el modal,
   * que aparece con backdrop blur y fade.
   */
  openCardDetail(event: MouseEvent, card: CollectionCard): void {
    this.sfx.play('Click.mp3');
    const target = event.currentTarget as HTMLElement;
    const originRect = target.getBoundingClientRect();

    this.clearFlyTimers();
    this.flyPhase.set(null);
    this.flyArrived = false;
    this.cardDetailLoaded = false;
    this.flyCardData.set(null);
    this.cardFlyOriginRect = originRect;
    this.cardDetailBodyOverflow.set('visible');

    this.selectedCard.set(card);
    this.selectedCardFull.set(null);
    this.selectedCardLoading.set(true);
    this.showCardDetailModal.set(true);
    this.cardDetailBackdropOpacity.set(0);
    this.cardDetailPanelOpacity.set(0);

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
            this.cardDetailLoaded = true;
            this.showModalAndFadePortal();
          }
          this.selectedCardLoading.set(false);
        },
      });

    setTimeout(() => {
      const cardWrapper = document.querySelector('.card-detail-layout .card-3d-wrapper') as HTMLElement;
      if (!cardWrapper) {
        this.cardDetailBackdropOpacity.set(0.85);
        this.cardDetailPanelOpacity.set(1);
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

  /** Cierra el modal volando la carta de vuelta a su posición original. */
  closeCardDetail(): void {
    this.sfx.play('Click.mp3');
    const origin = this.cardFlyOriginRect;
    const card = this.selectedCard();
    if (!origin || !card) {
      this.resetCardDetailState();
      return;
    }

    const cardWrapper = document.querySelector('.card-detail-layout .card-3d-wrapper') as HTMLElement;
    const currentRect = cardWrapper?.getBoundingClientRect();

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
    this.selectedCard.set(null);
    this.selectedCardFull.set(null);
    this.selectedCardLoading.set(false);
    this.showCardDetailModal.set(false);
    this.cardDetailBackdropOpacity.set(0);
    this.cardDetailPanelOpacity.set(0);
    this.cardDetailBodyOverflow.set('auto');
    this.flyPhase.set(null);
    this.flyCardData.update(d => (d ? { ...d, opacity: 0 } : null));
    this.scheduleFlyTimer(() => this.flyCardData.set(null), 350);
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

  openStatsModal(): void {
    this.sfx.play('Click.mp3');
    this.showStatsModal.set(true);
    this.statsLoading.set(true);
    this.http.get<PlayerStatsResponse>('/api/players/me/stats').subscribe({
      next: s => {
        this.playerStats.set(s);
        this.statsLoading.set(false);
      },
      error: () => {
        this.playerStats.set(null);
        this.statsLoading.set(false);
      },
    });
  }

  closeStatsModal(): void {
    this.sfx.play('Click.mp3');
    this.showStatsModal.set(false);
  }

  formatMatchTime(ms: number): string {
    const totalSeconds = Math.floor(ms / 1000);
    const h = Math.floor(totalSeconds / 3600);
    const m = Math.floor((totalSeconds % 3600) / 60);
    const s = totalSeconds % 60;
    if (h > 0) return `${h}h ${m}m`;
    if (m > 0) return `${m}m ${s}s`;
    return `${s}s`;
  }

  readonly currentDeckName = computed(() => {
    const u = this.user();
    return u?.decks?.find(d => d.id === u.activeDeckId)?.name ?? '—';
  });

  readonly xpIntoCurrentPercent = computed(() => {
    const u = this.user();
    const total = u?.xpForNextLevel ?? 1;
    const into = u?.xpIntoCurrentLevel ?? 0;
    return Math.min(100, Math.max(0, Math.round((into / total) * 100)));
  });

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
