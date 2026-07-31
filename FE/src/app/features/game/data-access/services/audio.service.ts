import { Injectable, signal } from '@angular/core';

@Injectable({ providedIn: 'root' })
export class AudioService {
  private audio: HTMLAudioElement | null = null;
  private src: string | null = null;
  readonly volume = signal(0.12);

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
    this.audio.volume = volume;
    this.volume.set(volume);

    this.audio.play().catch(() => {});
  }

  playSfx(src: string, volume = 0.5): void {
    const audio = new Audio(src);
    audio.loop = false;
    audio.volume = volume;
    audio.play().catch(() => {});
    audio.addEventListener('ended', () => audio.remove());
  }

  pause(): void {
    if (this.audio) {
      this.audio.pause();
    }
  }

  setVolume(volume: number): void {
    this.volume.set(volume);
    if (this.audio) {
      this.audio.volume = volume;
    }
  }

  isPlaying(): boolean {
    return this.audio !== null && !this.audio.paused && this.audio.currentTime > 0;
  }
}