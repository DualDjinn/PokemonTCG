import {
  afterNextRender,
  Component,
  input,
  viewChild,
  type ElementRef,
  type OnDestroy,
} from '@angular/core';

@Component({
  standalone: true,
  selector: 'app-sparkle-text',
  imports: [],
  templateUrl: './sparkle-text.html',
  styleUrl: './sparkle-text.scss',
})
export class SparkleText implements OnDestroy {
  readonly text = input('reverseHolo.');

  private readonly root = viewChild<ElementRef<HTMLAnchorElement>>('root');
  private intervalId: ReturnType<typeof setInterval> | null = null;

  constructor() {
    afterNextRender(() => {
      const el = this.root()?.nativeElement;
      if (!el) {
        return;
      }
      this.startSparkles();
      el.addEventListener('mouseenter', () => this.popRandomStar());
    });
  }

  ngOnDestroy(): void {
    if (this.intervalId) {
      clearInterval(this.intervalId);
      this.intervalId = null;
    }
  }

  private startSparkles(): void {
    this.popRandomStar();
    this.intervalId = setInterval(() => {
      this.popRandomStar();
    }, 650);
  }

  private popRandomStar(): void {
    const svgs = this.root()?.nativeElement.querySelectorAll<SVGSVGElement>('.sparkle-star');
    if (!svgs || svgs.length === 0) {
      return;
    }
    const svg = svgs[Math.floor(Math.random() * svgs.length)];
    const scale = 1.1 + Math.random() * 0.6;
    const rot = Math.random() * 180;

    svg.animate(
      [
        { transform: 'translate(-50%, -50%) rotate(0deg) scale(0)', opacity: 0 },
        { transform: `translate(-50%, -50%) rotate(${rot * 0.5}deg) scale(${scale})`, opacity: 1, offset: 0.4 },
        { transform: `translate(-50%, -50%) rotate(${rot}deg) scale(0)`, opacity: 0, offset: 1 },
      ],
      {
        duration: 750 + Math.random() * 300,
        easing: 'cubic-bezier(0.22, 1, 0.36, 1)',
      },
    );
  }
}
