import { Injectable, inject, signal, effect } from '@angular/core';
import { AudioMasterService } from './audio-master.service';

@Injectable({ providedIn: 'root' })
export class AudioSfxChannel {
  private master = inject(AudioMasterService);
  private readonly SFX_BASE = 'assets/audio/';

  readonly gain = signal(1);

  constructor() {
    effect(() => {
      this.master.masterVolume();
      this.master.isMuted();
    });
  }

  setGain(g: number): void {
    this.gain.set(Math.max(0, Math.min(1, g)));
  }

  play(file: string, volume?: number): void {
    const path = `${this.SFX_BASE}${file}`;
    const audio = new Audio(path);
    audio.loop = false;
    audio.volume = volume ?? (this.master.isMuted() ? 0 : this.master.masterVolume() * this.gain());
    audio.play().catch(() => {});
    audio.addEventListener('ended', () => audio.remove());
  }

  playLooped(file: string): { stop: () => void } {
    const path = `${this.SFX_BASE}${file}`;
    const audio = new Audio(path);
    audio.loop = true;
    audio.volume = this.master.isMuted() ? 0 : this.master.masterVolume() * this.gain();
    audio.play().catch(() => {});
    return {
      stop: () => {
        audio.pause();
        audio.remove();
      }
    };
  }
}
