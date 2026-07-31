import { Directive, ElementRef, HostListener, inject, input } from '@angular/core';

/**
 * Efecto "carta holográfica" que sigue al cursor.
 *
 * Publica en el host las custom properties que el CSS del consumidor decide
 * cómo usar (no aplica estilos por sí mismo):
 *  - `--mx` / `--my`: posición del cursor dentro del elemento (en %).
 *  - `--tilt-x` / `--tilt-y`: inclinación 3D sugerida (en deg, clamp ±tiltMax).
 *
 * Al salir el cursor, las propiedades se limpian para que el elemento vuelva
 * a su estado de reposo (o a su animación idle si la define el CSS).
 */
@Directive({
  selector: '[holoCard]',
  standalone: true,
})
export class HoloCardDirective {
  /** Inclinación máxima en grados hacia cada lado. */
  readonly tiltMax = input(8);

  private readonly el = inject<ElementRef<HTMLElement>>(ElementRef);

  @HostListener('mousemove', ['$event'])
  onMouseMove(event: MouseEvent): void {
    const host = this.el.nativeElement;
    const rect = host.getBoundingClientRect();
    if (rect.width === 0 || rect.height === 0) return;

    const px = Math.min(1, Math.max(0, (event.clientX - rect.left) / rect.width));
    const py = Math.min(1, Math.max(0, (event.clientY - rect.top) / rect.height));
    const max = this.tiltMax();

    host.style.setProperty('--mx', `${(px * 100).toFixed(1)}%`);
    host.style.setProperty('--my', `${(py * 100).toFixed(1)}%`);
    host.style.setProperty('--tilt-y', `${((px - 0.5) * 2 * max).toFixed(2)}deg`);
    host.style.setProperty('--tilt-x', `${(-(py - 0.5) * 2 * max).toFixed(2)}deg`);
  }

  @HostListener('mouseleave')
  onMouseLeave(): void {
    const style = this.el.nativeElement.style;
    style.removeProperty('--mx');
    style.removeProperty('--my');
    style.removeProperty('--tilt-x');
    style.removeProperty('--tilt-y');
  }
}
