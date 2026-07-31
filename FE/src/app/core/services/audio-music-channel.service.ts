import { Injectable, inject, signal, effect } from '@angular/core';
import { AudioMasterService } from './audio-master.service';

@Injectable({ providedIn: 'root' })
export class AudioMusicChannel {
  private master = inject(AudioMasterService);
  private audio: HTMLAudioElement | null = null;
  private currentTrack: string | null = null;
  private pendingTrack: string | null = null;
  private unlocked = false;
  private readonly MUSIC_BASE = 'assets/music/';

  readonly gain = signal(0.8);

  constructor() {
    effect(() => {
      this.master.masterVolume();
      this.master.isMuted();
      this.applyVolume();
    });
    const unlock = () => {
      this.unlocked = true;
      this.resumePending();
      document.removeEventListener('click', unlock);
      document.removeEventListener('keydown', unlock);
    };
    document.addEventListener('click', unlock);
    document.addEventListener('keydown', unlock);
  }

  setGain(g: number): void {
    this.gain.set(Math.max(0, Math.min(1, g)));
    this.applyVolume();
  }

  play(track: string): void {
    const path = `${this.MUSIC_BASE}${track}`;
    if (this.currentTrack === path && this.audio) return;
    this.stop();
    this.currentTrack = path;
    this.audio = new Audio(path);
    this.audio.loop = true;
    this.applyVolume();
    this.audio.play().catch(() => {
      this.pendingTrack = track;
      if (this.unlocked) {
        this.resumePending();
      }
    });
  }

  stop(): void {
    if (this.audio) {
      this.audio.pause();
      this.audio = null;
    }
    this.currentTrack = null;
    this.pendingTrack = null;
  }

  fadeOut(durationMs: number, keepAlive = false): void {
    if (!this.audio) return;
    const initialVolume = this.audio.volume;
    const steps = Math.floor(durationMs / 50);
    const stepSize = initialVolume / steps;
    let currentStep = 0;
    const interval = setInterval(() => {
      currentStep++;
      if (currentStep >= steps || !this.audio) {
        clearInterval(interval);
        if (!keepAlive) this.stop();
      } else {
        this.audio.volume = Math.max(0, initialVolume - stepSize * currentStep);
      }
    }, 50);
  }

  fadeIn(durationMs: number, track?: string): void {
    if (track) {
      const path = `${this.MUSIC_BASE}${track}`;
      if (this.currentTrack === path && this.audio) return;
      this.stop();
      this.currentTrack = path;
      this.audio = new Audio(path);
      this.audio.loop = true;
      this.audio.volume = 0;
      this.audio.play().catch(() => {});
    }
    if (!this.audio) return;
    const targetVolume = this.master.isMuted() ? 0 : this.master.masterVolume() * this.gain();
    const steps = Math.floor(durationMs / 50);
    const stepSize = targetVolume / steps;
    let currentStep = 0;
    const interval = setInterval(() => {
      currentStep++;
      if (currentStep >= steps || !this.audio) {
        clearInterval(interval);
        if (this.audio) this.audio.volume = targetVolume;
      } else {
        this.audio.volume = Math.min(targetVolume, stepSize * currentStep);
      }
    }, 50);
  }

  private getTargetVolume(): number {
    return this.master.isMuted() ? 0 : this.master.masterVolume() * this.gain();
  }

  resumePending(): void {
    if (this.pendingTrack && this.audio && this.audio.paused) {
      this.applyVolume();
      this.audio.play().catch(() => {});
      this.pendingTrack = null;
    }
  }

  playOnce(track: string): void {
    const path = `${this.MUSIC_BASE}${track}`;
    this.stop();
    this.currentTrack = path;
    this.audio = new Audio(path);
    this.audio.loop = false;
    this.applyVolume();
    this.audio.play().catch(() => {});
  }

  getCurrentTrack(): string | null {
    if (!this.currentTrack) return null;
    return this.currentTrack.replace(this.MUSIC_BASE, '');
  }

  private applyVolume(): void {
    if (!this.audio) return;
    this.audio.volume = this.master.isMuted() ? 0 : this.master.masterVolume() * this.gain();
  }
}
