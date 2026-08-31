import {
  afterNextRender,
  Component,
  viewChild,
  type ElementRef,
} from '@angular/core';
import { NeonBorder } from '../../shared/neon-border/neon-border';

@Component({
  standalone: true,
  selector: 'app-video-section',
  imports: [NeonBorder],
  templateUrl: './video-section.html',
  styleUrl: './video-section.scss',
})
export class VideoSection {
  private readonly video = viewChild<ElementRef<HTMLVideoElement>>('video');

  constructor() {
    afterNextRender(() => {
      const v = this.video()?.nativeElement;
      if (!v) return;
      v.muted = true;
      v.play().catch(() => {});
    });
  }
}
