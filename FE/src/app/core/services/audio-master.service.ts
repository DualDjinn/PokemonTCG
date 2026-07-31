import { Injectable, signal } from '@angular/core';

@Injectable({ providedIn: 'root' })
export class AudioMasterService {
  readonly masterVolume = signal(0.5);
  readonly isMuted = signal(false);
  private readonly STORAGE_KEY = 'audioMasterVolume';
  private readonly MUTED_KEY = 'audioMasterMuted';

  constructor() {
    const saved = localStorage.getItem(this.STORAGE_KEY);
    if (saved !== null) this.masterVolume.set(parseFloat(saved));
    const muted = localStorage.getItem(this.MUTED_KEY);
    if (muted !== null) this.isMuted.set(muted === 'true');
  }

  setMasterVolume(vol: number): void {
    const clamped = Math.max(0, Math.min(1, vol));
    this.masterVolume.set(clamped);
    localStorage.setItem(this.STORAGE_KEY, String(clamped));
  }

  toggleMute(): void {
    this.isMuted.update(v => !v);
    localStorage.setItem(this.MUTED_KEY, String(this.isMuted()));
  }
}
