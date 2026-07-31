import { Component, inject, OnDestroy } from '@angular/core';
import { GameEngineService } from '../../data-access/services/game-engine.service';

const OUTER_R = 60;   // radio círculo exterior en px
const INNER_R = 25;   // radio knob interior en px
const DEAD_ZONE = 15; // desplazamiento mínimo para activar dirección
const EDGE_MARGIN = 8; // margen desde el borde de pantalla

@Component({
  selector: 'app-virtual-joystick',
  standalone: true,
  template: `
    @if (isTouchDevice) {
      <div class="joystick-zone"
           (touchstart)="onTouchStart($event)"
           (touchmove)="onTouchMove($event)"
           (touchend)="onTouchEnd($event)"
           (touchcancel)="onTouchEnd($event)">
        @if (active) {
          <div class="joystick-outer"
               [style.left.px]="centerX"
               [style.top.px]="centerY">
            <div class="joystick-inner"
                 [style.transform]="'translate(' + knobDX + 'px, ' + knobDY + 'px)'">
            </div>
          </div>
        }
      </div>
    }
  `,
  styles: [`
    .joystick-zone {
      position: fixed;
      inset: 0;
      z-index: 50;
      touch-action: none;
    }

    .joystick-outer {
      position: fixed;
      width: 120px;
      height: 120px;
      border-radius: 50%;
      background: rgba(0, 170, 255, 0.12);
      border: 2px solid rgba(0, 170, 255, 0.55);
      box-shadow: 0 0 24px rgba(0, 170, 255, 0.25);
      transform: translate(-50%, -50%);
      pointer-events: none;
      user-select: none;
    }

    .joystick-inner {
      position: absolute;
      width: 50px;
      height: 50px;
      border-radius: 50%;
      background: rgba(0, 170, 255, 0.65);
      box-shadow: 0 0 14px rgba(0, 170, 255, 0.9);
      top: calc(50% - 25px);
      left: calc(50% - 25px);
    }
  `],
})
export class VirtualJoystickComponent implements OnDestroy {
  private readonly engineService = inject(GameEngineService);

  readonly isTouchDevice = navigator.maxTouchPoints > 0;

  protected active = false;
  protected centerX = 0;
  protected centerY = 0;
  protected knobDX = 0;
  protected knobDY = 0;

  private touchId: number | null = null;
  private currentDir: 'w' | 'a' | 's' | 'd' | null = null;

  onTouchStart(e: TouchEvent): void {
    if (this.touchId !== null) return;
    const touch = e.changedTouches[0];
    this.touchId = touch.identifier;

    const min = OUTER_R + EDGE_MARGIN;
    this.centerX = Math.max(min, Math.min(window.innerWidth - min, touch.clientX));
    this.centerY = Math.max(min, Math.min(window.innerHeight - min, touch.clientY));
    this.knobDX = 0;
    this.knobDY = 0;
    this.active = true;
  }

  onTouchMove(e: TouchEvent): void {
    const touch = Array.from(e.changedTouches).find(t => t.identifier === this.touchId);
    if (!touch) return;

    const dx = touch.clientX - this.centerX;
    const dy = touch.clientY - this.centerY;
    const dist = Math.hypot(dx, dy);

    if (dist <= OUTER_R) {
      this.knobDX = dx;
      this.knobDY = dy;
    } else {
      const angle = Math.atan2(dy, dx);
      this.knobDX = Math.cos(angle) * OUTER_R;
      this.knobDY = Math.sin(angle) * OUTER_R;
    }

    if (dist >= DEAD_ZONE) {
      const newDir: 'w' | 'a' | 's' | 'd' =
        Math.abs(dx) >= Math.abs(dy)
          ? (dx > 0 ? 'd' : 'a')
          : (dy > 0 ? 's' : 'w');

      if (newDir !== this.currentDir) {
        if (this.currentDir) this.engineService.releaseKey(this.currentDir);
        this.currentDir = newDir;
        this.engineService.pressKey(newDir);
      }
    } else if (this.currentDir) {
      this.engineService.releaseKey(this.currentDir);
      this.currentDir = null;
    }
  }

  onTouchEnd(e: TouchEvent): void {
    const touch = Array.from(e.changedTouches).find(t => t.identifier === this.touchId);
    if (!touch) return;

    this.touchId = null;
    this.active = false;
    this.knobDX = 0;
    this.knobDY = 0;

    if (this.currentDir) {
      this.engineService.releaseKey(this.currentDir);
      this.currentDir = null;
    }
  }

  ngOnDestroy(): void {
    if (this.currentDir) {
      this.engineService.releaseKey(this.currentDir);
    }
  }
}
