import { Injectable, signal, inject, effect } from '@angular/core';
import { AudioMasterService } from './audio-master.service';

@Injectable({ providedIn: 'root' })
export class AudioService {
  private master = inject(AudioMasterService);
  private audio: HTMLAudioElement | null = null;
  private src: string | null = null;
  private baseVolume = 0.12;
  readonly volume = signal(0.12);

  constructor() {
    effect(() => {
      this.master.masterVolume();
      this.master.isMuted();
      if (this.audio) {
        this.audio.volume = this.applyMasterVolume(this.baseVolume);
      }
    });
  }

  private applyMasterVolume(vol: number): number {
    return this.master.isMuted() ? 0 : this.master.masterVolume() * vol;
  }

  play(src: string, volume = 0.12): void {
    if (this.audio && this.src === src && !this.audio.paused) return;

    if (this.audio && this.src !== src) {
      this.audio.pause();
      this.audio.src = src;
    }

    if (!this.audio) {
      this.audio = new Audio(src);
      this.audio.loop = true;
    }

    this.src = src;
    this.baseVolume = volume;
    this.audio.volume = this.applyMasterVolume(volume);
    this.volume.set(volume);

    this.audio.play().catch(() => {});
  }

  playSfx(src: string, volume = 0.5): void {
    const audio = new Audio(src);
    audio.loop = false;
    audio.volume = this.applyMasterVolume(volume);
    audio.play().catch(() => {});
    audio.addEventListener('ended', () => audio.remove());
  }

  pause(): void {
    if (this.audio) {
      this.audio.pause();
    }
  }

  setVolume(volume: number): void {
    this.baseVolume = volume;
    this.volume.set(volume);
    if (this.audio) {
      this.audio.volume = this.applyMasterVolume(volume);
    }
  }

  isPlaying(): boolean {
    return this.audio !== null && !this.audio.paused && this.audio.currentTime > 0;
  }
}
