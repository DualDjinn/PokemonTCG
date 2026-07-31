import { ChangeDetectionStrategy, Component, computed, inject } from '@angular/core';
import { Router } from '@angular/router';
import { TranslateModule } from '@ngx-translate/core';
import { AuthService } from '../../../game/data-access/services/auth.service';
import { AudioSfxChannel } from '../../../../core/services/audio-sfx-channel.service';
import { MyButton } from '../../../../shared/ui/my-button/my-button';
import { MyText } from '../../../../shared/ui/my-text/my-text';
import { PixelPanelDirective } from '../../../../shared/directives';

/**
 * Página 404 - "Not found".
 *
 * <p>Se renderiza cuando el router matchea la ruta wildcard ({@code **}).
 * Reutiliza el pixel design system del proyecto: panel oscuro con bordes pixel,
 * neón cyan, fuente Press Start 2P y my-button. El botón de regreso es
 * inteligente: si hay sesión activa lleva al juego, si no lleva al login.
 */
@Component({
  selector: 'app-not-found-page',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [TranslateModule, MyButton, MyText, PixelPanelDirective],
  styleUrl: './not-found.page.css',
  template: `
    <div class="nf-container">
      <div appPixelPanel class="nf-card p-8 flex flex-col items-center gap-6 max-w-[640px] w-full">
        <img
          src="/assets/img/login/pokeball.svg"
          alt="Pokeball"
          class="nf-logo"
        />

        <div class="nf-404">
          <span class="nf-digit">4</span>
          <div class="nf-zero">
            <img
              src="/assets/img/slot-machine/snorlax.gif"
              alt="Snorlax"
              class="nf-snorlax"
            />
          </div>
          <span class="nf-digit">4</span>
        </div>

        <my-text
          [text]="('NOT_FOUND.MESSAGE_PREFIX' | translate) + ' ' + ('NOT_FOUND.SNORLAX' | translate) + ' ' + ('NOT_FOUND.MESSAGE_SUFFIX' | translate)"
          tag="p"
          size="0.85rem"
          color="var(--text-primary)"
          align="center"
        ></my-text>

        <my-text
          [text]="'NOT_FOUND.OOPS' | translate"
          tag="span"
          size="0.7rem"
          weight="bold"
          color="var(--neon-cyan)"
          [style.letterSpacing]="'0.2em'"
        ></my-text>

        <my-button
          [text]="'NOT_FOUND.GO_BACK' | translate"
          variant="pixel"
          size="medium"
          (clicked)="goBack()"
        ></my-button>
      </div>
    </div>
  `,
})
export class NotFoundPage {
  private readonly router = inject(Router);
  private readonly authService = inject(AuthService);
  private readonly sfx = inject(AudioSfxChannel);

  /** Indica si hay un usuario autenticado en este momento. */
  readonly isLoggedIn = computed(() => this.authService.currentUser() !== null);

  /**
   * Navega al destino más razonable según el estado de sesión:
   * <ul>
   *   <li>Con sesión → ciudad principal del juego.</li>
   *   <li>Sin sesión → login.</li>
   * </ul>
   */
  goBack(): void {
    this.sfx.play('Click.mp3');
    if (this.isLoggedIn()) {
      const user = this.authService.currentUser();
      this.router.navigate(['/juego', 'ciudadPrincipal'], {
        queryParams: user?.username ? { user: user.username } : undefined,
      });
    } else {
      this.router.navigate(['/login']);
    }
  }
}
