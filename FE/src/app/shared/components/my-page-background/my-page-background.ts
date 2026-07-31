import { ChangeDetectionStrategy, Component, Input } from '@angular/core';

/**
 * Fondo de página compartido: video a pantalla completa con un overlay oscuro
 * por encima para que el contenido siga siendo legible.
 *
 * Reemplaza el patrón .bg-video / .bg-overlay que cada página duplicaba en su
 * propio CSS (Perfil, Mazos, Lobby). El nivel de oscurecimiento se ajusta con
 * `overlayOpacity` (Perfil usa 0.45, el resto 0.65).
 */
@Component({
  selector: 'my-page-background',
  standalone: true,
  templateUrl: './my-page-background.html',
  styleUrl: './my-page-background.css',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class MyPageBackground {
  @Input({ required: true }) videoSrc = '';
  @Input() poster = '';
  @Input() overlayOpacity = 0.65;
}
