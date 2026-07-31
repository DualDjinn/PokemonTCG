import { Injectable, inject } from '@angular/core';
import { AudioSfxChannel } from '../../../../core/services/audio-sfx-channel.service';
import { AudioMasterService } from '../../../../core/services/audio-master.service';

/**
 * SFX específicos de la animación de apertura de sobres.
 *
 * <p>Combina assets existentes ({@code Plop.mp3}, {@code Click.mp3}) con sonidos
 * generados procedualmente con Web Audio API (rasgado de papel, tono dorado,
 * arpegio de rare). No requiere agregar archivos al repo.
 */
@Injectable({ providedIn: 'root' })
export class PackSfxService {
  private readonly sfx = inject(AudioSfxChannel);
  private readonly master = inject(AudioMasterService);

  private audioCtx: AudioContext | null = null;
  private lastTearAt = 0;

  private ctx(): AudioContext {
    if (!this.audioCtx) {
      const Ctor = window.AudioContext ?? (window as unknown as { webkitAudioContext: typeof AudioContext }).webkitAudioContext;
      this.audioCtx = new Ctor();
    }
    return this.audioCtx;
  }

  private masterVolume(): number {
    return this.master.isMuted() ? 0 : this.master.masterVolume() * this.sfx.gain();
  }

  /**
   * Rasgado de papel sintético (white noise filtrado con envelope decay).
   * Throttled a 80ms para evitar saturación al dispararse en cada mousemove.
   */
  tear(intensity = 1): void {
    const now = performance.now();
    if (now - this.lastTearAt < 80) return;
    this.lastTearAt = now;

    const vol = this.masterVolume();
    if (vol <= 0) return;

    const ctx = this.ctx();
    const duration = 0.18 + intensity * 0.18;
    const bufferSize = Math.floor(ctx.sampleRate * duration);
    const buffer = ctx.createBuffer(1, bufferSize, ctx.sampleRate);
    const data = buffer.getChannelData(0);
    for (let i = 0; i < bufferSize; i++) {
      data[i] = (Math.random() * 2 - 1) * (1 - i / bufferSize);
    }

    const noise = ctx.createBufferSource();
    noise.buffer = buffer;

    const hp = ctx.createBiquadFilter();
    hp.type = 'highpass';
    hp.frequency.value = 1800 + intensity * 1800;

    const gain = ctx.createGain();
    gain.gain.value = 0.45 * intensity * vol;

    noise.connect(hp);
    hp.connect(gain);
    gain.connect(ctx.destination);
    noise.start();
  }

  /**
   * "Pop" al abrir + sine sweep ascendente que evoca el tono dorado de la luz.
   */
  popGolden(): void {
    const vol = this.masterVolume();
    if (vol <= 0) return;

    this.sfx.play('Plop.mp3', vol * 0.7);

    const ctx = this.ctx();
    const t0 = ctx.currentTime;

    const osc = ctx.createOscillator();
    osc.type = 'sine';
    osc.frequency.setValueAtTime(440, t0);
    osc.frequency.exponentialRampToValueAtTime(1320, t0 + 0.5);

    const gain = ctx.createGain();
    gain.gain.setValueAtTime(0, t0);
    gain.gain.linearRampToValueAtTime(0.28 * vol, t0 + 0.05);
    gain.gain.exponentialRampToValueAtTime(0.001, t0 + 0.7);

    osc.connect(gain);
    gain.connect(ctx.destination);
    osc.start(t0);
    osc.stop(t0 + 0.75);
  }

  /** Click al hacer flip de una carta. */
  flip(): void {
    this.sfx.play('Click.mp3', this.masterVolume() * 0.55);
  }

  /**
   * Arpegio mayor ascendente C-E-G-C (do mayor) para destacar el revelado de
   * una carta rara. Mucho más mágico que un simple click.
   */
  rareFanfare(): void {
    const vol = this.masterVolume();
    if (vol <= 0) return;

    const ctx = this.ctx();
    const t0 = ctx.currentTime;
    const notes = [523.25, 659.25, 783.99, 1046.5]; // C5 E5 G5 C6

    notes.forEach((freq, i) => {
      const osc = ctx.createOscillator();
      osc.type = 'triangle';
      osc.frequency.value = freq;

      const gain = ctx.createGain();
      const t = t0 + i * 0.07;
      gain.gain.setValueAtTime(0, t);
      gain.gain.linearRampToValueAtTime(0.18 * vol, t + 0.02);
      gain.gain.exponentialRampToValueAtTime(0.001, t + 0.6);

      osc.connect(gain);
      gain.connect(ctx.destination);
      osc.start(t);
      osc.stop(t + 0.65);
    });
  }
}
