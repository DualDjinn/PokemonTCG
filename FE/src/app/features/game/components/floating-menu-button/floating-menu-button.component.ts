import { ChangeDetectionStrategy, Component, EventEmitter, Output } from '@angular/core';
import { TranslateModule } from '@ngx-translate/core';

/**
 * Botón flotante con forma de burbuja gris visible solo en dispositivos
 * táctiles (tablets/celulares). Permite abrir el menú del juego sin teclado.
 *
 * <p>Renderiza una burbuja circular con fondo gris claro, borde gris medio
 * y tres líneas blancas (estilo menú hamburguesa). Ubicado en la parte
 * media-baja del costado derecho de la pantalla.
 *
 * <p>La detección de dispositivo táctil usa {@code navigator.maxTouchPoints > 0}
 * (mismo criterio que {@code VirtualJoystickComponent}).
 */
@Component({
  selector: 'app-floating-menu-button',
  standalone: true,
  imports: [TranslateModule],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    @if (isTouchDevice) {
      <button
        class="bubble-menu-btn"
        type="button"
        [attr.aria-label]="'COMMON.OPEN_MENU' | translate"
        (click)="onClick($event)"
      >
        <span class="bubble-menu-btn__icon">
          <span class="bubble-line"></span>
          <span class="bubble-line"></span>
          <span class="bubble-line"></span>
        </span>
      </button>
    }
  `,
  styles: [`
    .bubble-menu-btn {
      position: fixed;
      top: 20%;
      right: 1rem;
      z-index: 850;
      width: 60px;
      height: 60px;
      padding: 0;
      cursor: pointer;
      /* Burbuja translúcida: gris muy suave con casi nada de opacidad */
      background: radial-gradient(
        circle at 35% 30%,
        rgba(232, 232, 232, 0.22) 0%,
        rgba(200, 200, 200, 0.18) 65%,
        rgba(168, 168, 168, 0.22) 100%
      );
      backdrop-filter: blur(4px);
      -webkit-backdrop-filter: blur(4px);
      border: 1.5px solid rgba(144, 144, 144, 0.35);
      border-radius: 50%;
      box-shadow:
        inset -3px -4px 6px rgba(120, 120, 120, 0.15),
        inset 3px 4px 6px rgba(255, 255, 255, 0.25),
        0 4px 10px rgba(0, 0, 0, 0.2);
      display: flex;
      align-items: center;
      justify-content: center;
      transition: transform 0.12s ease-out, box-shadow 0.12s ease-out;
      touch-action: manipulation;
      user-select: none;
    }
    .bubble-menu-btn:active {
      transform: scale(0.93);
      box-shadow:
        inset 3px 4px 6px rgba(120, 120, 120, 0.18),
        inset -3px -4px 6px rgba(255, 255, 255, 0.18),
        0 2px 4px rgba(0, 0, 0, 0.2);
    }

    /* ── Ícono: tres líneas blancas con borde gris ── */
    .bubble-menu-btn__icon {
      display: flex;
      flex-direction: column;
      gap: 5px;
      width: 26px;
    }
    .bubble-line {
      width: 100%;
      height: 4px;
      background: #ffffff;
      border: 1px solid #909090;
      border-radius: 2px;
    }
  `],
})
export class FloatingMenuButtonComponent {
  /** Se emite cuando el jugador toca el botón. */
  @Output() menuToggle = new EventEmitter<void>();

  /** Solo se renderiza el botón si el dispositivo soporta toques. */
  readonly isTouchDevice = navigator.maxTouchPoints > 0;

  onClick(event: MouseEvent): void {
    event.stopPropagation();
    this.menuToggle.emit();
  }
}
