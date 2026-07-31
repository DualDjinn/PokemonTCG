import { Injectable, inject, signal } from '@angular/core';
import { AudioSfxChannel } from '../../core/services/audio-sfx-channel.service';

export interface ToastData {
  message: string;
  type: 'success' | 'error' | 'info';
  visible: boolean;
}

@Injectable({ providedIn: 'root' })
export class ToastService {
  readonly toast = signal<ToastData | null>(null);
  private readonly sfx = inject(AudioSfxChannel);
  private timeoutId: ReturnType<typeof setTimeout> | null = null;

  show(message: string, type: 'success' | 'error' | 'info' = 'success', duration = 3000): void {
    this.clear();
    this.toast.set({ message, type, visible: true });
    if (type === 'error') {
      this.sfx.play('Error.mp3');
    }
    this.timeoutId = setTimeout(() => this.clear(), duration);
  }

  clear(): void {
    if (this.timeoutId) {
      clearTimeout(this.timeoutId);
      this.timeoutId = null;
    }
    this.toast.set(null);
  }
}
