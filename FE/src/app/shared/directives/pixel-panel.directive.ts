import { Directive, ElementRef, Input, OnInit, Renderer2 } from '@angular/core';

export type PixelPanelVariant = 'panel' | 'modal' | 'card' | 'none';

@Directive({
  selector: '[appPixelPanel]',
  standalone: true,
})
export class PixelPanelDirective implements OnInit {
  @Input('appPixelPanel') variant: PixelPanelVariant | '' = 'panel';
  @Input() pixelGlow = true;

  constructor(
    private el: ElementRef<HTMLElement>,
    private renderer: Renderer2,
  ) {}

  ngOnInit(): void {
    this.applyStyles();
  }

  private applyStyles(): void {
    const resolved = this.variant === '' ? 'panel' : this.variant;
    if (resolved === 'none') return;

    const style = this.el.nativeElement.style;

    if (resolved === 'card') {
      style.backgroundColor = 'var(--pixel-card-bg)';
    } else {
      style.backgroundColor = 'var(--pixel-panel-bg)';
      style.backgroundImage = [
        'linear-gradient(45deg, var(--pixel-panel-pattern-color) 25%, transparent 25%)',
        'linear-gradient(-45deg, var(--pixel-panel-pattern-color) 25%, transparent 25%)',
      ].join(', ');
      style.backgroundSize = 'var(--pixel-panel-pattern-size) var(--pixel-panel-pattern-size)';
    }

    style.border = resolved === 'card' ? '2px solid' : '3px solid';
    style.borderColor =
      'var(--pixel-border-top-left) var(--pixel-border-bottom-right) var(--pixel-border-bottom-right) var(--pixel-border-top-left)';
    style.borderRadius = '0';

    if (this.pixelGlow && resolved === 'panel') {
      style.boxShadow = '0 0 var(--pixel-panel-glow-size) var(--pixel-panel-glow-color)';
    }
  }
}
