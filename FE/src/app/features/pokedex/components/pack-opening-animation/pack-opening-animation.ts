import {
  ChangeDetectionStrategy, Component, computed, EventEmitter,
  HostListener, inject, Input, OnChanges, Output, signal,
} from '@angular/core';
import { TranslateModule } from '@ngx-translate/core';
import { MyText } from '../../../../shared/ui/my-text/my-text';
import { MyButton } from '../../../../shared/ui/my-button/my-button';
import { MyBadge } from '../../../../shared/ui/my-badge/my-badge';
import { MyModal } from '../../../../shared/components/my-modal/my-modal';
import { HoloCardDirective } from '../../../../shared/directives/holo-card.directive';
import { DeckCardDetail } from '../../../decks/components/deck-card-detail/deck-card-detail';
import { CardSummary } from '../../../decks/domain/models/deck.models';
import { Card } from '../../domain/models/card.model';
import { OpenedPack, PackCard, PackType } from '../../domain/models/pack.model';
import { CardService } from '../../data-access/services/card.service';
import { PackSfxService } from '../../data-access/services/pack-sfx.service';
import { CosmeticsService, PlayerCardBacks } from '../../../profile/data-access/cosmetics.service';

type Stage = 'closed' | 'opening' | 'burst' | 'reveal' | 'done';
type RarityClass = 'rarity-common' | 'rarity-uncommon' | 'rarity-rare' | 'rarity-holo';

@Component({
  selector: 'app-pack-opening-animation',
  standalone: true,
  imports: [TranslateModule, MyText, MyButton, DeckCardDetail, HoloCardDirective],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    @if (openedSig(); as op) {
      <div class="pkanim-overlay"
           [class]="'pkanim-overlay ' + bestRevealedRarity()"
           (mousemove)="onMouseMove($event)"
           (mouseup)="onMouseUp()">

        @if (stage() === 'closed' || stage() === 'opening' || stage() === 'burst') {
          <div class="pkanim-stage-drag" [class.bursting]="stage() === 'burst'">
            @if (stage() === 'closed') {
              <my-text
                [text]="'PACKS_ANIM.DRAG_HINT' | translate" tag="p"
                size="0.72rem" color="white" align="center" />
            }

            <div [class]="'pack-wrapper pack-wrapper--' + packType().toLowerCase()
                   + (stage() === 'opening' ? ' opening' : '')
                   + (stage() === 'burst' ? ' discarding' : '')">

              <!-- PNG del sobre cuando está cerrado -->
              @if (stage() === 'closed') {
                <div class="pack-img-wrap">
                  <img [src]="packClosedImg()" class="pack-img" [alt]="packType()" />
                  <div class="pack-drag-area"
                       [class.dragging]="dragging()"
                       (mousedown)="onMouseDown($event)"
                       (touchstart)="onTouchStart($event)">
                    <div class="pack-strip-corner">
                      <span class="pack-strip-arrow">↘</span>
                    </div>
                  </div>
                </div>
              }

              <!-- Efecto de apertura CSS: sobre se rasga lateral -->
              @if (stage() === 'opening' || stage() === 'burst') {
                <div [class]="'pack-tear-wrap tear-' + tearDirection()">
                  <img [src]="packClosedImg()" class="pack-img pack-img--body" [alt]="packType()" />
                  <img [src]="packClosedImg()" class="pack-img pack-img--flap" [alt]="packType()" />
                  <div class="pack-tear-line"></div>
                  <div class="pack-burst-light"></div>
                </div>
              }

              <!-- FX al abrir: luz, anillos, partículas, sparkles -->
              @if (stage() === 'opening' || stage() === 'burst') {
                <div class="golden-light"></div>
                <div class="golden-rings"></div>

                @for (p of particles; track $index) {
                  <div class="gold-particle"
                       [style.--dx.px]="p.dx"
                       [style.--dy.px]="p.dy"
                       [style.--scale]="p.scale"
                       [style.--delay.ms]="p.delay"></div>
                }

                @for (s of sparkles; track $index) {
                  <div class="sparkle"
                       [style.--sx.px]="s.x"
                       [style.--sy.px]="s.y"
                       [style.--sdelay.ms]="s.delay"></div>
                }
              }
            </div>
          </div>
        }

        @if (stage() === 'burst' || stage() === 'reveal' || stage() === 'done') {
          <div class="pkanim-stage-reveal">
            @if (stage() !== 'burst') {
              <div class="reveal-hint">
                <my-text
                  [text]="(stage() === 'reveal'
                    ? 'PACKS_ANIM.CLICK_TO_REVEAL'
                    : 'PACKS_ANIM.ALL_REVEALED') | translate"
                  tag="p" size="0.81rem" color="var(--neon-cyan)" align="center" />
              </div>
            }

            @if (holoFlipping() !== null) {
              <div class="holo-dimmer"></div>
            }

            <div class="cards-row" [class.holo-ceremony]="holoFlipping() !== null">
              @for (card of op.cards; track i; let i = $index) {
                <div class="card-slot">
                  <div class="card-flip-wrapper"
                       holoCard
                       [tiltMax]="tiltFor(card)"
                       [class]="'card-flip-wrapper ' + rarityClass(card.rarity)
                         + (revealed()[i] ? ' flipped' : '')
                         + (stage() === 'burst' ? ' erupting' : '')
                         + (holoFlipping() !== null && holoFlipping() !== i ? ' dimmed' : '')"
                       [style.--from-x.px]="eruptFromX(i, op.cards.length)"
                       [style.--erupt-delay.ms]="i * 130"
                       (click)="onCardClick(i)">

                    @if (stage() === 'burst') {
                      <div class="erupt-trail"></div>
                    }

                    @if (revealed()[i] && (rarityClass(card.rarity) === 'rarity-rare' || rarityClass(card.rarity) === 'rarity-holo')) {
                      <div class="shockwave"></div>
                    }

                    @if (revealed()[i] && rarityClass(card.rarity) === 'rarity-holo') {
                      <div class="holo-flash"></div>
                    }

                    <!-- Dorso de la carta: imagen fondo según tipo de sobre o reverso activo -->
                    <div class="card-face card-back">
                      <img [src]="cardBackImg()" class="card-back-img" alt="card back" (error)="$any($event.target).src = '/assets/img/card-backs/backCardPixel.png'" />
                      <div class="card-back-glint"></div>
                    </div>

                    <div class="card-face card-front">
                      <img [src]="card.imageUrl" [alt]="card.name" />
                      @if (rarityClass(card.rarity) === 'rarity-rare' || rarityClass(card.rarity) === 'rarity-holo') {
                        <div class="foil-layer"></div>
                        <div class="sheen-layer"></div>
                      }
                      @if (rarityClass(card.rarity) === 'rarity-holo') {
                        <div class="glitter-layer"></div>
                      }
                    </div>

                    @if (revealed()[i] && (rarityClass(card.rarity) === 'rarity-rare' || rarityClass(card.rarity) === 'rarity-holo')) {
                      @for (rs of revealSparkles; track $index) {
                        <div class="reveal-sparkle"
                             [style.--rsx.px]="rs.x"
                             [style.--rsy.px]="rs.y"
                             [style.--rsdelay.ms]="rs.delay"></div>
                      }
                    }

                    @if (revealed()[i] && (rarityClass(card.rarity) === 'rarity-rare' || rarityClass(card.rarity) === 'rarity-holo')) {
                      @for (c of confettiPieces; track $index) {
                        <div class="confetti"
                             [style.--cdx.px]="c.dx"
                             [style.--cdy.px]="c.dy"
                             [style.--crot.deg]="c.rot"
                             [style.--ccolor]="c.color"
                             [style.--cdelay.ms]="c.delay"></div>
                      }
                    }
                  </div>
                </div>
              }
            </div>

            @if (stage() === 'done') {
              <div class="done-actions">
                <my-button
                  [text]="'PACKS_ANIM.CONTINUE' | translate"
                  variant="pixel" size="medium"
                  color="var(--neon-cyan)"
                  (clicked)="close()" />
              </div>
            }
          </div>
        }

        @if (selectedCard(); as sc) {
          <div
            class="fixed inset-0 z-[2500] flex items-center justify-center p-4 bg-black/80 backdrop-blur-sm animate-[fadeIn_0.2s_ease-out]"
            (click)="closeDetail()"
          >
            <div
              class="relative w-full max-w-2xl bg-[#14132b] border border-white/15 rounded-3xl p-6 shadow-2xl overflow-hidden max-h-[90vh] flex flex-col"
              (click)="$event.stopPropagation()"
            >
              <!-- Header -->
              <div class="flex items-center justify-between pb-4 border-b border-white/10 mb-4">
                <h3 class="font-rajdhani font-bold text-xl text-[#edebff] uppercase tracking-wider">
                  {{ sc.name }}
                </h3>
                <button
                  type="button"
                  class="w-8 h-8 rounded-full border border-white/10 bg-white/5 text-[#9c98c7] hover:text-[#edebff] hover:border-white/30 flex items-center justify-center text-sm transition-all cursor-pointer"
                  (click)="closeDetail()"
                  aria-label="Cerrar detalle"
                >
                  ✕
                </button>
              </div>

              <!-- Body: Two-column layout with 3D card tilt & Stats -->
              <div class="flex flex-col md:flex-row gap-6">
                <div class="w-full md:w-[220px] flex-shrink-0 flex items-center justify-center">
                  <app-deck-card-detail [card]="toCardSummary(sc)" [showCount]="false" />
                </div>

                <div class="flex-1 flex flex-col gap-3 min-w-0 overflow-y-auto max-h-[55vh] pr-1">
                  @if (selectedCardInfo(); as info) {
                    <!-- Badges -->
                    <div class="flex items-center gap-2 flex-wrap">
                      <span class="px-2.5 py-1 rounded-full text-xs font-bold font-rajdhani uppercase tracking-wider bg-[#00aaff]/20 text-[#00aaff] border border-[#00aaff]/30">
                        {{ info.supertype }}
                      </span>
                      @if (info.hp) {
                        <span class="px-2.5 py-1 rounded-full text-xs font-bold font-rajdhani uppercase tracking-wider bg-[#ff3366]/20 text-[#ff3366] border border-[#ff3366]/30">
                          {{ info.hp }} HP
                        </span>
                      }
                      <span class="px-2.5 py-1 rounded-full text-xs font-bold font-rajdhani uppercase tracking-wider bg-white/5 text-[#9c98c7] border border-white/10">
                        {{ info.rarity }}
                      </span>
                    </div>

                    <!-- Subtypes -->
                    @if (info.subtypes && info.subtypes.length > 0) {
                      <p class="text-xs text-[#9c98c7] font-sans">{{ info.subtypes.join(' · ') }}</p>
                    }

                    <!-- Abilities -->
                    @if (info.abilities && info.abilities.length > 0) {
                      <div class="flex flex-col gap-1 mt-1">
                        <span class="text-xs font-rajdhani font-bold text-[#c9ff3d] uppercase tracking-wider">Habilidades</span>
                        @for (a of info.abilities; track a.name) {
                          <div class="p-2 rounded-lg bg-[#0b0a1f] border border-white/10 text-xs">
                            <strong class="text-[#edebff]">{{ a.name }}:</strong>
                            <span class="text-[#9c98c7] ml-1">{{ a.text }}</span>
                          </div>
                        }
                      </div>
                    }

                    <!-- Attacks -->
                    @if (info.attacks && info.attacks.length > 0) {
                      <div class="flex flex-col gap-1 mt-1">
                        <span class="text-xs font-rajdhani font-bold text-[#c9ff3d] uppercase tracking-wider">Ataques</span>
                        @for (a of info.attacks; track a.name) {
                          <div class="p-2 rounded-lg bg-[#0b0a1f] border border-white/10 text-xs">
                            <div class="flex items-center justify-between gap-2">
                              <strong class="text-[#edebff]">{{ a.name }}</strong>
                              @if (a.damage) {
                                <span class="font-jetbrains font-bold text-[#ff3366]">{{ a.damage }} DMG</span>
                              }
                            </div>
                            @if (a.text) {
                              <p class="text-[#9c98c7] mt-0.5 text-[11px]">{{ a.text }}</p>
                            }
                          </div>
                        }
                      </div>
                    }
                  } @else {
                    <!-- Loading skeleton or basic info while fetching -->
                    <div class="flex items-center gap-2 flex-wrap">
                      <span class="px-2.5 py-1 rounded-full text-xs font-bold font-rajdhani uppercase tracking-wider bg-[#00aaff]/20 text-[#00aaff] border border-[#00aaff]/30">
                        {{ sc.supertype || 'Pokémon' }}
                      </span>
                      <span class="px-2.5 py-1 rounded-full text-xs font-bold font-rajdhani uppercase tracking-wider bg-white/5 text-[#9c98c7] border border-white/10">
                        {{ sc.rarity }}
                      </span>
                    </div>
                    <p class="text-xs text-[#9c98c7] font-sans">Cargando detalles...</p>
                  }

                  <!-- Footer -->
                  <div class="pt-4 border-t border-white/10 flex items-center justify-between mt-auto">
                    <span class="text-xs font-rajdhani font-bold text-[#c9ff3d] uppercase tracking-wider flex items-center gap-1.5">
                      <span>✓</span> En tu colección
                    </span>
                    <button
                      type="button"
                      class="px-5 py-2.5 rounded-xl font-rajdhani font-bold text-sm uppercase tracking-wider bg-white/10 hover:bg-[#c9ff3d] text-[#edebff] hover:text-[#0b0a1f] transition-all cursor-pointer"
                      (click)="closeDetail()"
                    >
                      Cerrar
                    </button>
                  </div>
                </div>
              </div>
            </div>
          </div>
        }
      </div>
    }
  `,
  styles: [`
    .pkanim-overlay {
      position: fixed; inset: 0; z-index: 2000;
      background: radial-gradient(ellipse at center,
        rgba(20,20,50,0.95) 0%, rgba(0,0,0,0.96) 70%);
      display: flex; align-items: center; justify-content: center;
      user-select: none; overflow: hidden;
      transition: background 1.2s ease-out;
    }
    .pkanim-overlay.rarity-uncommon {
      background: radial-gradient(ellipse at center,
        rgba(20,60,90,0.95) 0%, rgba(0,10,20,0.97) 70%);
    }
    .pkanim-overlay.rarity-rare,
    .pkanim-overlay.rarity-holo {
      background: radial-gradient(ellipse at center,
        rgba(70,50,20,0.95) 0%, rgba(20,10,0,0.97) 70%);
    }

    /* ===== Stage drag ===== */
    .pkanim-stage-drag {
      display: flex; flex-direction: column; align-items: center; gap: 1.5rem;
      animation: fade-in 0.3s ease-out;
    }
    .pkanim-stage-drag.bursting {
      position: absolute; inset: 0;
      justify-content: center;
      pointer-events: none;
      z-index: 1;
    }
    @keyframes fade-in { from { opacity: 0; } to { opacity: 1; } }

    /* ===== Pack wrapper ===== */
    .pack-wrapper {
      position: relative;
      width: 320px; height: 440px;
      filter: drop-shadow(0 14px 28px rgba(0,0,0,0.7));
      display: flex; align-items: center; justify-content: center;
    }
    .pack-wrapper.opening { animation: pack-blast 0.6s ease-out; }
    @keyframes pack-blast {
      0%   { transform: scale(1); }
      30%  { transform: scale(1.08); }
      100% { transform: scale(1); }
    }
    .pack-wrapper.discarding {
      animation:
        pack-spasm 0.3s ease-out,
        pack-discard 0.75s cubic-bezier(0.55, 0, 0.85, 0.36) 0.28s forwards;
    }
    @keyframes pack-spasm {
      0%   { transform: scale(1); }
      45%  { transform: scale(1.12) rotate(1.5deg); }
      100% { transform: scale(1.04); }
    }
    @keyframes pack-discard {
      0%   { transform: scale(1.04) translateY(0) rotate(0deg); opacity: 1; }
      100% { transform: scale(0.85) translateY(72vh) rotate(-32deg); opacity: 0; }
    }

    /* ===== Imagen del sobre ===== */
    .pack-img-wrap {
      position: relative;
      display: inline-block;
    }

    .pack-img {
      display: block;
      height: 380px;
      width: auto;
      object-fit: contain;
      z-index: 1;
    }

    /* ===== Efecto de apertura CSS (reemplaza GIF) ===== */
    .pack-tear-wrap {
      position: relative;
      display: inline-block;
    }

    /* Cuerpo: sobre sin la solapa (clip desde el corte hacia abajo) */
    .pack-img--body {
      display: block;
      height: 380px;
      width: auto;
      clip-path: polygon(0 14%, 100% 14%, 100% 100%, 0 100%);
      animation: body-settle 0.4s ease-out forwards;
    }
    @keyframes body-settle {
      from { transform: translateY(0); }
      to   { transform: translateY(5px); }
    }

    /* Solapa: top 14% que vuela lateralmente */
    .pack-img--flap {
      position: absolute;
      top: 0; left: 0;
      height: 380px;
      width: auto;
      clip-path: polygon(0 0, 100% 0, 100% 14%, 0 14%);
    }

    /* Drag hacia la derecha → solapa sale por la derecha */
    .tear-right .pack-img--flap {
      animation: flap-right 0.65s cubic-bezier(0.4, 0, 0.2, 1) forwards;
    }
    @keyframes flap-right {
      0%   { transform: translate(0,   0)   rotate(0deg);  opacity: 1; }
      35%  { transform: translate(35px, -10px) rotate(12deg); opacity: 0.85; }
      100% { transform: translate(200px,-45px) rotate(28deg); opacity: 0; }
    }

    /* Drag hacia la izquierda → solapa sale por la izquierda */
    .tear-left .pack-img--flap {
      animation: flap-left 0.65s cubic-bezier(0.4, 0, 0.2, 1) forwards;
    }
    @keyframes flap-left {
      0%   { transform: translate(0,    0)    rotate(0deg);   opacity: 1; }
      35%  { transform: translate(-35px,-10px) rotate(-12deg); opacity: 0.85; }
      100% { transform: translate(-200px,-45px) rotate(-28deg); opacity: 0; }
    }

    /* Línea de corte: destello luminoso en el punto de rasgado */
    .pack-tear-line {
      position: absolute;
      left: -10%; right: -10%;
      top: calc(14% - 2px);
      height: 4px;
      background: linear-gradient(90deg,
        transparent 0%,
        rgba(255,240,200,0.6) 8%,
        rgba(255,255,255,1)  50%,
        rgba(255,240,200,0.6) 92%,
        transparent 100%);
      border-radius: 2px;
      box-shadow:
        0 0 8px  rgba(255,255,200,0.9),
        0 0 20px rgba(255,215,0,0.7),
        0 0 40px rgba(255,180,0,0.4);
      pointer-events: none;
      animation: tear-flash 1.3s ease-out forwards;
    }
    @keyframes tear-flash {
      0%   { opacity: 0; transform: scaleX(0.15); }
      18%  { opacity: 1; transform: scaleX(1); }
      60%  { opacity: 0.85; }
      100% { opacity: 0; }
    }

    /* Luz que emana de la apertura hacia arriba */
    .pack-burst-light {
      position: absolute;
      left: 50%;
      top: 14%;
      transform: translateX(-50%);
      width: 130%;
      height: 230px;
      background: radial-gradient(ellipse 70% 90% at 50% 0%,
        rgba(255,248,200,1)   0%,
        rgba(255,215,0,0.7)  28%,
        rgba(255,160,20,0.25) 60%,
        transparent          82%);
      filter: blur(14px);
      pointer-events: none;
      transform-origin: 50% 0%;
      animation: burst-rise 1.6s ease-out forwards;
    }
    @keyframes burst-rise {
      0%   { opacity: 0;   transform: translateX(-50%) scaleY(0.05); }
      18%  { opacity: 1; }
      50%  { transform: translateX(-50%) scaleY(1); }
      100% { opacity: 0;   transform: translateX(-50%) scaleY(1.5) translateY(-18px); }
    }

    /* ===== Área de drag (transparent, solo interacción) ===== */
    .pack-drag-area {
      position: absolute;
      top: 0; right: 0;
      width: 100%; height: 100%;
      z-index: 4;
      cursor: grab;
    }
    .pack-drag-area.dragging { cursor: grabbing; }

    /* Esquina sup-der "grabbable" con efecto de pliegue */
    .pack-strip-corner {
      position: absolute; top: 0; right: 0;
      width: 48px; height: 48px;
      background: linear-gradient(225deg,
        rgba(255,255,255,0.6) 0%,
        rgba(255,255,255,0.25) 45%,
        transparent 55%);
      clip-path: polygon(0 0, 100% 0, 100% 100%);
      border-top-right-radius: 8px;
      pointer-events: none;
      filter: drop-shadow(-2px 2px 3px rgba(0,0,0,0.45));
      z-index: 2;
    }
    .pack-strip-arrow {
      position: absolute;
      top: 6px; right: 8px;
      font-size: 1.2rem;
      color: rgba(255,255,255,0.95);
      text-shadow: 0 0 6px rgba(0,0,0,0.8);
      animation: grip-pulse 1.3s ease-in-out infinite;
      pointer-events: none;
    }
    @keyframes grip-pulse {
      0%, 100% { transform: translate(0, 0) scale(1);      opacity: 0.75; }
      50%      { transform: translate(4px, 4px) scale(1.2); opacity: 1; }
    }

    /* ===== Luz dorada y anillos ===== */
    .golden-light {
      position: absolute;
      top: 50%; left: 50%;
      width: 460px; height: 460px;
      transform: translate(-50%, -50%);
      background: radial-gradient(circle,
        rgba(255,235,150,1) 0%,
        rgba(255,215,0,0.85) 18%,
        rgba(255,200,50,0.45) 40%,
        rgba(255,180,40,0.18) 60%,
        transparent 78%);
      filter: blur(12px);
      animation: golden-pulse 1.2s ease-in-out infinite;
      z-index: -1;
      pointer-events: none;
    }
    @keyframes golden-pulse {
      0%, 100% { opacity: 0.8; transform: translate(-50%, -50%) scale(0.85); }
      50%      { opacity: 1;   transform: translate(-50%, -50%) scale(1.2); }
    }
    .golden-rings {
      position: absolute;
      top: 50%; left: 50%;
      width: 240px; height: 240px;
      transform: translate(-50%, -50%);
      border: 3px solid rgba(255,215,0,0.65);
      border-radius: 50%;
      z-index: -1; pointer-events: none;
      animation: rings-expand 1.4s ease-out infinite;
    }
    @keyframes rings-expand {
      0%   { transform: translate(-50%, -50%) scale(0.3); opacity: 1; border-width: 4px; }
      100% { transform: translate(-50%, -50%) scale(2.4); opacity: 0; border-width: 1px; }
    }

    /* ===== Partículas doradas ===== */
    .gold-particle {
      position: absolute;
      top: 50%; left: 50%;
      width: 8px; height: 8px;
      background: radial-gradient(circle, #ffd700 0%, #ff8c00 100%);
      border-radius: 50%;
      box-shadow: 0 0 10px #ffd700;
      transform: translate(-50%, -50%);
      animation: particle-fly 1.5s ease-out var(--delay) forwards;
      opacity: 0;
      z-index: 3;
      pointer-events: none;
    }
    @keyframes particle-fly {
      0%   { transform: translate(-50%, -50%) scale(0); opacity: 0; }
      20%  { opacity: 1; }
      100% { transform: translate(calc(-50% + var(--dx)), calc(-50% + var(--dy))) scale(var(--scale)); opacity: 0; }
    }

    .sparkle {
      position: absolute;
      top: 50%; left: 50%;
      width: 6px; height: 6px;
      background: white; border-radius: 50%;
      box-shadow: 0 0 8px white;
      transform: translate(calc(-50% + var(--sx)), calc(-50% + var(--sy)));
      animation: sparkle-blink 1.2s ease-in-out var(--sdelay) infinite;
      z-index: 3;
      pointer-events: none;
    }
    @keyframes sparkle-blink {
      0%, 100% { opacity: 0; transform: translate(calc(-50% + var(--sx)), calc(-50% + var(--sy))) scale(0.4); }
      50%      { opacity: 1; transform: translate(calc(-50% + var(--sx)), calc(-50% + var(--sy))) scale(1.4); }
    }

    /* ===== Stage reveal ===== */
    .pkanim-stage-reveal {
      position: absolute; inset: 0;
      display: flex; flex-direction: column;
      align-items: center; justify-content: center;
      z-index: 2;
    }
    .reveal-hint {
      position: absolute;
      top: 7vh; left: 50%; transform: translateX(-50%);
      animation: fade-in 0.4s ease-out;
    }
    .done-actions {
      position: absolute;
      bottom: 7vh; left: 50%; transform: translateX(-50%);
      animation: fade-in 0.4s ease-out;
    }
    .cards-row {
      display: flex; gap: 3.5rem;
      position: relative;
    }

    .card-slot {
      position: relative;
      width: 230px; height: 322px;
      perspective: 1400px;
    }

    .card-flip-wrapper {
      width: 230px; height: 322px;
      position: relative;
      cursor: pointer;
      transform-style: preserve-3d;
      transform:
        translateY(var(--lift, 0px))
        scale(var(--scale, 1))
        rotateX(var(--tilt-x-eff, 0deg))
        rotateY(var(--tilt-y-eff, 0deg));
      transition: transform 0.28s cubic-bezier(0.34, 1.56, 0.64, 1), filter 0.35s ease-out;
      z-index: 2;
    }
    .card-flip-wrapper:hover { z-index: 6; }
    .card-flip-wrapper.flipped {
      --tilt-x-eff: var(--tilt-x, 0deg);
      --tilt-y-eff: var(--tilt-y, 0deg);
    }

    /* ===== Erupción ===== */
    .card-flip-wrapper.erupting {
      animation: card-erupt 1.05s cubic-bezier(0.22, 1, 0.36, 1) var(--erupt-delay, 0ms) both;
      pointer-events: none;
    }
    @keyframes card-erupt {
      0% {
        transform: translate(var(--from-x, 0px), 140px) scale(0.16) rotateZ(-10deg);
        opacity: 0;
      }
      10% { opacity: 1; }
      45% {
        transform: translate(calc(var(--from-x, 0px) * 0.42), -165px) scale(0.85) rotateZ(7deg);
      }
      72% {
        transform: translate(calc(var(--from-x, 0px) * 0.06), -14px) scale(1.05) rotateZ(-2deg);
      }
      86% { transform: translate(0px, 10px) scale(0.98) rotateZ(0.6deg); }
      100% { transform: translate(0px, 0px) scale(1) rotateZ(0deg); opacity: 1; }
    }
    .erupt-trail {
      position: absolute;
      left: 50%; top: 55%;
      width: 30px; height: 170px;
      transform: translateX(-50%);
      background: linear-gradient(180deg,
        transparent 0%,
        rgba(255,215,0,0.55) 55%,
        rgba(255,235,150,0.8) 100%);
      border-radius: 50%;
      filter: blur(9px);
      animation: trail-fade 1.0s ease-out var(--erupt-delay, 0ms) both;
      pointer-events: none;
      z-index: -1;
    }
    @keyframes trail-fade {
      0%   { opacity: 0; transform: translateX(-50%) scaleY(0.4); }
      35%  { opacity: 1; transform: translateX(-50%) scaleY(1.25); }
      100% { opacity: 0; transform: translateX(-50%) scaleY(0.7); }
    }

    /* ===== Caras de la carta ===== */
    .card-face {
      position: absolute; inset: 0;
      backface-visibility: hidden;
      border-radius: 14px;
      overflow: hidden;
      transition:
        transform var(--flip-dur, 0.55s) cubic-bezier(0.4, 0, 0.2, 1),
        box-shadow 0.3s ease-out;
      box-shadow: 0 10px 28px rgba(0,0,0,0.55);
    }

    /* Dorso: imagen fondo_* del tipo de sobre */
    .card-back {
      border: 3px solid rgba(80,80,160,0.6);
      display: flex; align-items: center; justify-content: center;
      overflow: hidden;
      background: #1a1a3e;
    }
    .card-back-img {
      width: 100%; height: 100%;
      object-fit: cover;
      display: block;
    }
    .card-back-glint {
      position: absolute; top: -100%; left: -50%;
      width: 200%; height: 300%;
      background: linear-gradient(110deg,
        transparent 40%,
        rgba(0,170,255,0.22) 50%,
        transparent 60%);
      animation: back-glint 3.5s ease-in-out infinite;
      pointer-events: none;
    }
    @keyframes back-glint {
      0%   { transform: translateY(0); }
      100% { transform: translateY(50%); }
    }

    .card-front {
      transform: rotateY(180deg);
      background: white;
      display: flex; flex-direction: column;
    }
    .card-front img {
      width: 100%; height: 100%; object-fit: cover;
    }
    .card-front .card-name {
      position: absolute; bottom: 0; left: 0; right: 0;
      padding: 0.5rem; text-align: center;
      background: rgba(0,0,0,0.78);
      color: white; font-size: 0.7rem;
      font-family: 'Press Start 2P', monospace;
    }
    .card-flip-wrapper.flipped .card-back  { transform: rotateY(180deg); }
    .card-flip-wrapper.flipped .card-front { transform: rotateY(0deg); }

    /* ===== Flips por rareza ===== */
    .rarity-uncommon.flipped .card-back {
      animation: flip-back-uncommon 0.7s cubic-bezier(0.34, 1.56, 0.64, 1) forwards;
    }
    .rarity-uncommon.flipped .card-front {
      animation: flip-front-uncommon 0.7s cubic-bezier(0.34, 1.56, 0.64, 1) forwards;
    }
    @keyframes flip-front-uncommon {
      0%   { transform: rotateY(180deg); }
      72%  { transform: rotateY(-16deg); }
      100% { transform: rotateY(0deg); }
    }
    @keyframes flip-back-uncommon {
      0%   { transform: rotateY(0deg); }
      72%  { transform: rotateY(196deg); }
      100% { transform: rotateY(180deg); }
    }
    .rarity-rare.flipped {
      animation: rare-suspense 0.38s ease-in;
    }
    @keyframes rare-suspense {
      0%   { transform: translateY(0);    filter: drop-shadow(0 0 0 rgba(255,215,0,0)); }
      30%  { transform: translateY(-12px) rotate(-1.2deg); filter: drop-shadow(0 0 18px rgba(255,215,0,0.9)); }
      60%  { transform: translateY(-10px) rotate(1.4deg);  filter: drop-shadow(0 0 26px rgba(255,215,0,1)); }
      100% { transform: translateY(0);    filter: drop-shadow(0 0 10px rgba(255,215,0,0.5)); }
    }
    .rarity-rare.flipped .card-back {
      animation: flip-back-rare 0.8s cubic-bezier(0.45, 0, 0.2, 1) 0.34s both;
    }
    .rarity-rare.flipped .card-front {
      animation: flip-front-rare 0.8s cubic-bezier(0.45, 0, 0.2, 1) 0.34s both;
    }
    @keyframes flip-front-rare {
      0%   { transform: rotateY(540deg); }
      100% { transform: rotateY(0deg); }
    }
    @keyframes flip-back-rare {
      0%   { transform: rotateY(720deg); }
      100% { transform: rotateY(180deg); }
    }
    .card-flip-wrapper.rarity-rare .shockwave { animation-delay: 1.0s; }
    .card-flip-wrapper.rarity-rare .confetti  { animation-delay: calc(var(--cdelay) + 1000ms); }

    .rarity-holo.flipped {
      animation: holo-rise 1.9s cubic-bezier(0.3, 0, 0.3, 1);
      z-index: 9;
    }
    @keyframes holo-rise {
      0%   { transform: translateY(0) scale(1); }
      16%  { transform: translateY(-36px) scale(1.18); }
      80%  { transform: translateY(-36px) scale(1.18); }
      100% { transform: translateY(0) scale(1); }
    }
    .rarity-holo.flipped .card-back {
      animation: flip-back-holo 1.15s cubic-bezier(0.6, 0, 0.22, 1) 0.4s both;
    }
    .rarity-holo.flipped .card-front {
      animation: flip-front-holo 1.15s cubic-bezier(0.6, 0, 0.22, 1) 0.4s both;
    }
    @keyframes flip-front-holo {
      0%   { transform: rotateY(540deg); filter: brightness(1); }
      55%  { filter: brightness(1.9); }
      100% { transform: rotateY(0deg); filter: brightness(1); }
    }
    @keyframes flip-back-holo {
      0%   { transform: rotateY(720deg); }
      100% { transform: rotateY(180deg); }
    }
    .holo-flash {
      position: absolute; inset: -40%;
      background: radial-gradient(circle,
        rgba(255,255,255,0.95) 0%, rgba(255,255,255,0.4) 40%, transparent 70%);
      opacity: 0;
      animation: holo-flash-pop 0.5s ease-out 1.0s;
      pointer-events: none;
      z-index: 2;
    }
    @keyframes holo-flash-pop {
      0%   { opacity: 0; }
      30%  { opacity: 1; }
      100% { opacity: 0; }
    }
    .card-flip-wrapper.rarity-holo .shockwave {
      border-color: transparent;
      box-shadow:
        0 0 0 4px rgba(255,80,200,0.85),
        0 0 0 10px rgba(80,150,255,0.5),
        0 0 36px rgba(255,255,255,0.85);
      animation-delay: 1.4s;
    }
    .card-flip-wrapper.rarity-holo .confetti { animation-delay: calc(var(--cdelay) + 1400ms); }

    .holo-dimmer {
      position: absolute; inset: 0;
      background: rgba(2, 2, 14, 0.6);
      animation: fade-in 0.35s ease-out;
      pointer-events: none;
      z-index: 5;
    }
    .cards-row.holo-ceremony .card-flip-wrapper.dimmed {
      --scale: 0.92;
      filter: brightness(0.45) saturate(0.7);
    }

    /* ===== Hover ===== */
    .card-flip-wrapper:not(.flipped):hover { --lift: -14px; --scale: 1.15; }
    .card-flip-wrapper:not(.flipped):hover .card-back {
      box-shadow: 0 0 30px rgba(0,170,255,0.85),
                  0 18px 40px rgba(0,0,0,0.6);
    }
    .card-flip-wrapper.flipped:hover { --scale: 1.05; }
    .card-flip-wrapper.flipped { cursor: zoom-in; }

    /* ===== Glow del dorso según rareza ===== */
    .card-flip-wrapper.rarity-rare:not(.flipped) .card-back {
      border-color: #8a4fd0;
      animation: back-glow-purple 1.8s ease-in-out infinite;
    }
    .card-flip-wrapper.rarity-holo:not(.flipped) .card-back {
      border-color: #d4a017;
      animation: back-glow-gold 1.8s ease-in-out infinite;
    }
    @keyframes back-glow-purple {
      0%, 100% {
        box-shadow: 0 0 14px 3px rgba(160,70,255,0.5),
                    0 10px 28px rgba(0,0,0,0.55);
      }
      50% {
        box-shadow: 0 0 30px 9px rgba(180,95,255,0.9),
                    0 10px 28px rgba(0,0,0,0.55);
      }
    }
    @keyframes back-glow-gold {
      0%, 100% {
        box-shadow: 0 0 14px 3px rgba(255,200,60,0.5),
                    0 10px 28px rgba(0,0,0,0.55);
      }
      50% {
        box-shadow: 0 0 32px 10px rgba(255,215,80,0.95),
                    0 10px 28px rgba(0,0,0,0.55);
      }
    }

    /* ===== Foil holográfico interactivo ===== */
    .card-flip-wrapper { --foil-opacity: 0; --sheen-opacity: 0; }
    .card-flip-wrapper.rarity-rare { --foil-opacity: 0.6;  --sheen-opacity: 0.45; }
    .card-flip-wrapper.rarity-holo { --foil-opacity: 0.85; --sheen-opacity: 0.5; }

    .foil-layer, .sheen-layer, .glitter-layer {
      position: absolute; inset: 0;
      pointer-events: none;
      opacity: 0;
      transition: opacity 0.5s ease-out;
    }
    .foil-layer {
      background-image: repeating-linear-gradient(115deg,
        rgba(255,80,200,0.28) 0%,
        rgba(80,150,255,0.28) 9%,
        rgba(80,255,180,0.28) 18%,
        rgba(255,220,80,0.28) 27%,
        rgba(255,80,200,0.28) 36%);
      background-size: 320% 320%;
      background-position: var(--mx, 50%) var(--my, 40%);
      mix-blend-mode: color-dodge;
    }
    .card-flip-wrapper.rarity-rare .foil-layer {
      background-image: repeating-linear-gradient(115deg,
        rgba(255,215,0,0.34) 0%,
        rgba(255,255,255,0.12) 8%,
        rgba(255,160,40,0.3) 16%,
        rgba(255,215,0,0.34) 24%);
    }
    .sheen-layer {
      background: radial-gradient(circle at var(--mx, 50%) var(--my, 35%),
        rgba(255,255,255,0.5) 0%,
        rgba(255,255,255,0.14) 28%,
        transparent 60%);
      mix-blend-mode: overlay;
    }
    .glitter-layer {
      background-image:
        radial-gradient(rgba(255,255,255,0.85) 0.8px, transparent 1.8px),
        radial-gradient(rgba(255,255,255,0.5) 1px, transparent 2.2px);
      background-size: 15px 15px, 23px 23px;
      background-position: var(--mx, 50%) var(--my, 40%), calc(var(--mx, 50%) * -0.6) calc(var(--my, 40%) * -0.6);
      mix-blend-mode: screen;
    }
    .card-flip-wrapper.flipped .foil-layer    { opacity: var(--foil-opacity); }
    .card-flip-wrapper.flipped .sheen-layer   { opacity: var(--sheen-opacity); }
    .card-flip-wrapper.flipped .glitter-layer { opacity: 0.55; }
    .card-flip-wrapper.flipped:not(:hover) .foil-layer {
      animation: foil-drift 7s ease-in-out infinite;
    }
    @keyframes foil-drift {
      0%, 100% { background-position: 32% 36%; }
      50%      { background-position: 68% 60%; }
    }

    /* ===== Shockwave ===== */
    .shockwave {
      position: absolute; inset: 0;
      border-radius: 18px;
      border: 4px solid rgba(255,215,0,0.95);
      opacity: 0;
      animation: shockwave-expand 0.8s ease-out forwards;
      pointer-events: none;
      z-index: 1;
    }
    @keyframes shockwave-expand {
      0%   { transform: scale(0.6); opacity: 1; border-width: 6px; }
      100% { transform: scale(3);   opacity: 0; border-width: 1px; }
    }

    /* ===== Sparkles orbitando ===== */
    .reveal-sparkle {
      position: absolute;
      top: 50%; left: 50%;
      width: 8px; height: 8px;
      background: white; border-radius: 50%;
      box-shadow: 0 0 10px #fff, 0 0 18px #ffd700;
      transform: translate(calc(-50% + var(--rsx)), calc(-50% + var(--rsy)));
      animation: reveal-sparkle 1.5s ease-out var(--rsdelay) infinite;
      pointer-events: none;
    }
    .card-flip-wrapper.rarity-holo .reveal-sparkle {
      box-shadow: 0 0 12px #fff, 0 0 22px #ff50c8, 0 0 32px #ffd700;
    }
    @keyframes reveal-sparkle {
      0%, 100% { opacity: 0; transform: translate(calc(-50% + var(--rsx)), calc(-50% + var(--rsy))) scale(0.3); }
      50%      { opacity: 1; transform: translate(calc(-50% + var(--rsx)), calc(-50% + var(--rsy))) scale(1.3); }
    }

    /* ===== Confetti ===== */
    .confetti {
      position: absolute;
      top: 50%; left: 50%;
      width: 8px; height: 12px;
      background: var(--ccolor);
      transform: translate(-50%, -50%);
      animation: confetti-fall 1.8s ease-out var(--cdelay) forwards;
      opacity: 0;
      pointer-events: none;
      clip-path: polygon(50% 0, 100% 38%, 82% 100%, 18% 100%, 0 38%);
    }
    @keyframes confetti-fall {
      0%   { transform: translate(-50%, -50%) rotate(0); opacity: 1; }
      100% {
        transform:
          translate(calc(-50% + var(--cdx)), calc(-50% + var(--cdy)))
          rotate(var(--crot));
        opacity: 0;
      }
    }

    /* Pantallas angostas */
    @media (max-width: 980px) {
      .cards-row { gap: 1.5rem; }
      .card-slot, .card-flip-wrapper { width: 170px; height: 238px; }
    }
  `],
})
export class PackOpeningAnimationComponent implements OnChanges {
  private readonly packSfx = inject(PackSfxService);
  private readonly cardService = inject(CardService);
  private readonly cosmetics = inject(CosmeticsService);

  @Input() opened: OpenedPack | null = null;
  @Output() readonly closed = new EventEmitter<void>();

  readonly openedSig = signal<OpenedPack | null>(null);

  readonly stage = signal<Stage>('closed');
  readonly selectedCard = signal<PackCard | null>(null);
  readonly selectedCardInfo = signal<Card | null>(null);
  readonly holoFlipping = signal<number | null>(null);

  readonly typeColorMap: { [key: string]: string | undefined } = {
    grass: '#4ade80',
    fire: '#f97316',
    water: '#38bdf8',
    lightning: '#facc15',
    psychic: '#e879f9',
    fighting: '#a16207',
    darkness: '#374151',
    metal: '#9ca3af',
    fairy: '#f472b6',
    dragon: '#7c3aed',
    colorless: '#d1d5db',
  };

  readonly peelAngle = signal<number>(0);
  readonly dragging = signal<boolean>(false);
  readonly revealed = signal<boolean[]>([]);
  readonly tearDirection = signal<'left' | 'right'>('right');

  private readonly playerCardBacks = signal<PlayerCardBacks | null>(null);

  private dragStartX = 0;
  private dragStartY = 0;
  private lastDx = 0;
  private static readonly SEG_COUNT = 6;
  private static readonly OPEN_THRESHOLD = 70;
  private static readonly MAX_ANGLE = 130;
  private static readonly DEG_PER_PX = 0.9;

  readonly particles = Array.from({ length: 16 }, () => ({
    dx: (Math.random() - 0.5) * 360,
    dy: -(120 + Math.random() * 280),
    scale: 0.4 + Math.random() * 0.8,
    delay: Math.random() * 600,
  }));

  readonly sparkles = Array.from({ length: 12 }, () => ({
    x: (Math.random() - 0.5) * 320,
    y: (Math.random() - 0.5) * 280,
    delay: Math.random() * 1000,
  }));

  readonly revealSparkles = Array.from({ length: 8 }, () => ({
    x: (Math.random() - 0.5) * 220,
    y: (Math.random() - 0.5) * 280,
    delay: Math.random() * 1000,
  }));

  readonly confettiPieces = Array.from({ length: 24 }, () => {
    const colors = ['#ffd700', '#ff50c8', '#50c8ff', '#a0ff50', '#ff8c50', '#fff'];
    return {
      dx: (Math.random() - 0.5) * 360,
      dy: 80 + Math.random() * 280,
      rot: (Math.random() - 0.5) * 720,
      color: colors[Math.floor(Math.random() * colors.length)],
      delay: Math.random() * 400,
    };
  });

  packType = computed<PackType>(() => this.openedSig()?.packType ?? 'BRONZE');

  private readonly packAssetName = computed(() => {
    const names: Record<PackType, string> = {
      BRONZE: 'base', SILVER: 'plata', GOLD: 'oro', DIAMOND: 'diamante',
    };
    return names[this.packType()];
  });

  packClosedImg = computed(() => `/assets/animation_store/${this.packAssetName()}.png`);
  packGifImg    = computed(() => `/assets/animation_store/sobre_${this.packAssetName()}.gif`);
  cardBackImg = computed(() => {
    const backs = this.playerCardBacks();
    if (backs?.activeCardBackId) {
      const active = backs.owned.find(b => b.id === backs.activeCardBackId);
      if (active?.imageUrl) return active.imageUrl;
    }
    return '/assets/img/card-backs/backCardPixel.png';
  });

  segAngle = computed(() => this.peelAngle() / PackOpeningAnimationComponent.SEG_COUNT);

  bestRevealedRarity = computed<RarityClass>(() => {
    const op = this.openedSig();
    const revealed = this.revealed();
    if (!op) return 'rarity-common';
    const order: Record<RarityClass, number> = {
      'rarity-common': 0, 'rarity-uncommon': 1, 'rarity-rare': 2, 'rarity-holo': 3
    };
    let best: RarityClass = 'rarity-common';
    op.cards.forEach((c, i) => {
      if (revealed[i]) {
        const cls = this.rarityClass(c.rarity);
        if (order[cls] > order[best]) best = cls;
      }
    });
    return best;
  });

  ngOnChanges(): void {
    this.openedSig.set(this.opened);
    if (this.opened) {
      this.stage.set('closed');
      this.peelAngle.set(0);
      this.dragging.set(false);
      this.lastDx = 0;
      this.revealed.set(this.opened.cards.map(() => false));
      this.selectedCard.set(null);
      this.selectedCardInfo.set(null);
      this.holoFlipping.set(null);
      this.cosmetics.getPlayerCardBacks().subscribe({
        next: backs => this.playerCardBacks.set(backs),
        error: () => {},
      });
    }
  }

  private dragToAngle(dx: number, dy: number): number {
    const magnitude = Math.sqrt(dx * dx + dy * dy);
    return Math.max(0, Math.min(
      PackOpeningAnimationComponent.MAX_ANGLE,
      magnitude * PackOpeningAnimationComponent.DEG_PER_PX,
    ));
  }

  private static readonly CARD_PITCH = 286;

  eruptFromX(index: number, total: number): number {
    const centerIdx = (total - 1) / 2;
    return -(index - centerIdx) * PackOpeningAnimationComponent.CARD_PITCH;
  }

  tiltFor(card: PackCard): number {
    switch (this.rarityClass(card.rarity)) {
      case 'rarity-holo': return 10;
      case 'rarity-rare': return 8;
      default: return 0;
    }
  }

  toCardSummary(card: PackCard): CardSummary {
    return {
      id: card.id,
      name: card.name,
      supertype: card.supertype,
      imageUrl: card.imageUrl,
      imageUrlLarge: card.imageUrlLarge,
      rarity: card.rarity ?? 'COMMON',
    };
  }

  rarityClass(rarity: string | null): RarityClass {
    if (!rarity) return 'rarity-common';
    switch (rarity) {
      case 'COMMON':   return 'rarity-common';
      case 'UNCOMMON': return 'rarity-uncommon';
      case 'RARE':     return 'rarity-rare';
      case 'RARE_HOLO':
      case 'RARE_HOLO_EX':
      case 'RARE_ULTRA':
      case 'ACE_SPEC_RARE':
        return 'rarity-holo';
      default: return 'rarity-common';
    }
  }

  onMouseDown(event: MouseEvent): void {
    if (this.stage() !== 'closed') return;
    this.dragging.set(true);
    this.dragStartX = event.clientX;
    this.dragStartY = event.clientY;
    event.preventDefault();
  }

  onMouseMove(event: MouseEvent): void {
    if (!this.dragging()) return;
    const dx = event.clientX - this.dragStartX;
    const dy = event.clientY - this.dragStartY;
    this.lastDx = dx;
    const angle = this.dragToAngle(dx, dy);
    if (angle !== this.peelAngle()) {
      this.peelAngle.set(angle);
      this.packSfx.tear(Math.min(1, angle / PackOpeningAnimationComponent.MAX_ANGLE));
    }
  }

  onMouseUp(): void {
    if (!this.dragging()) return;
    this.dragging.set(false);
    if (this.peelAngle() >= PackOpeningAnimationComponent.OPEN_THRESHOLD) {
      this.tearDirection.set(this.lastDx >= 0 ? 'right' : 'left');
      this.stage.set('opening');
      this.packSfx.popGolden();
      setTimeout(() => this.stage.set('burst'), 700);
      setTimeout(() => this.stage.set('reveal'), 700 + 1450);
    } else {
      this.peelAngle.set(0);
    }
  }

  onTouchStart(event: TouchEvent): void {
    if (this.stage() !== 'closed' || !event.touches.length) return;
    this.dragging.set(true);
    this.dragStartX = event.touches[0].clientX;
    this.dragStartY = event.touches[0].clientY;
  }

  @HostListener('touchmove', ['$event'])
  onTouchMove(event: TouchEvent): void {
    if (!this.dragging() || !event.touches.length) return;
    const dx = event.touches[0].clientX - this.dragStartX;
    const dy = event.touches[0].clientY - this.dragStartY;
    this.lastDx = dx;
    const angle = this.dragToAngle(dx, dy);
    if (angle !== this.peelAngle()) {
      this.peelAngle.set(angle);
      this.packSfx.tear(Math.min(1, angle / PackOpeningAnimationComponent.MAX_ANGLE));
    }
  }

  @HostListener('touchend')
  onTouchEnd(): void {
    this.onMouseUp();
  }

  onCardClick(index: number): void {
    if (this.revealed()[index]) {
      if (this.stage() === 'reveal' || this.stage() === 'done') {
        const card = this.openedSig()?.cards[index];
        if (card) {
          this.selectedCard.set(card);
          this.selectedCardInfo.set(null);
          this.cardService.getCardById(card.id).subscribe({
            next: info => this.selectedCardInfo.set(info),
            error: () => {},
          });
        }
      }
      return;
    }
    this.reveal(index);
  }

  closeDetail(): void {
    this.selectedCard.set(null);
    this.selectedCardInfo.set(null);
  }

  reveal(index: number): void {
    if (this.stage() !== 'reveal' || this.revealed()[index]) return;
    const next = [...this.revealed()];
    next[index] = true;
    this.revealed.set(next);

    const cards = this.openedSig()?.cards ?? [];
    const cls = this.rarityClass(cards[index]?.rarity ?? null);
    if (cls === 'rarity-rare' || cls === 'rarity-holo') {
      this.packSfx.rareFanfare();
    } else {
      this.packSfx.flip();
    }

    if (cls === 'rarity-holo') {
      this.holoFlipping.set(index);
      setTimeout(() => this.holoFlipping.set(null), 1900);
    }

    if (next.every(Boolean)) {
      setTimeout(() => this.stage.set('done'), cls === 'rarity-holo' ? 2000 : 900);
    }
  }

  close(): void {
    this.closed.emit();
  }
}
