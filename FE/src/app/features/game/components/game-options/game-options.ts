import { Component, EventEmitter, Output, inject } from '@angular/core';
import { TranslateModule } from '@ngx-translate/core';
import { AudioMasterService } from '../../../../core/services/audio-master.service';
import { AudioMusicChannel } from '../../../../core/services/audio-music-channel.service';
import { AudioSfxChannel } from '../../../../core/services/audio-sfx-channel.service';
import { LanguageService, AppLang } from '../../../../core/services/language.service';
import { MyButton } from '../../../../shared/ui/my-button/my-button';
import { MyText } from '../../../../shared/ui/my-text/my-text';

@Component({
  selector: 'app-game-options',
  standalone: true,
  imports: [TranslateModule, MyButton, MyText],
  template: `
    <div id="game-options">
      <div class="options-content p-10 min-w-[620px] text-center">
        <div class="flex items-center justify-center gap-3 mb-6">
          <img src="/assets/img/login/pokeball.svg" class="title-pokeball" />
          <my-text [text]="'GAME_OPTIONS.TITLE' | translate" tag="h2" size="1.08rem" weight="bold" color="var(--neon-cyan)" align="center"></my-text>
          <img src="/assets/img/login/pokeball.svg" class="title-pokeball" />
        </div>
        <div class="options-columns">
          <div class="options-section">
            <my-text [text]="'GAME_OPTIONS.AUDIO' | translate" tag="p" size="0.6rem" color="var(--text-secondary)" align="left"></my-text>
            <div class="section-divider"></div>
            <div class="volume-control">
              <label class="volume-label">{{ 'GAME_OPTIONS.MASTER_VOLUME' | translate }}</label>
              <div class="volume-row">
                <span class="vol-icon">&#x1F507;</span>
                <input type="range" class="volume-slider" min="0" max="1" step="0.05"
                       [value]="audioMaster.masterVolume()" (input)="onMasterVolumeChange($event)">
                <span class="vol-icon">&#x1F50A;</span>
              </div>
              <div class="volume-pct">{{ (audioMaster.masterVolume() * 100).toFixed(0) }}%</div>
            </div>
            <div class="volume-control">
              <label class="volume-label">{{ 'GAME_OPTIONS.MUSIC' | translate }}</label>
              <div class="volume-row">
                <span class="vol-icon">&#x1F507;</span>
                <input type="range" class="volume-slider" min="0" max="1" step="0.05"
                       [value]="audioMusic.gain()" (input)="onMusicGainChange($event)">
                <span class="vol-icon">&#x1F50A;</span>
              </div>
              <div class="volume-pct">{{ (audioMusic.gain() * 100).toFixed(0) }}%</div>
            </div>
            <div class="volume-control">
              <label class="volume-label">{{ 'GAME_OPTIONS.EFFECTS' | translate }}</label>
              <div class="volume-row">
                <span class="vol-icon">&#x1F507;</span>
                <input type="range" class="volume-slider" min="0" max="1" step="0.05"
                       [value]="sfx.gain()" (input)="onSfxGainChange($event)">
                <span class="vol-icon">&#x1F50A;</span>
              </div>
              <div class="volume-pct">{{ (sfx.gain() * 100).toFixed(0) }}%</div>
            </div>
          </div>
          <div class="options-section">
            <my-text [text]="'GAME_OPTIONS.LANGUAGE' | translate" tag="p" size="0.6rem" color="var(--text-secondary)" align="left"></my-text>
            <div class="section-divider"></div>
            <!-- La lista de idiomas se genera dinámicamente desde el registry de fábricas -->
            @for (lang of availableLanguages; track lang.code) {
              <label class="radio-option">
                <input type="radio" name="language" [value]="lang.code"
                       [checked]="langService.currentLang() === lang.code"
                       (change)="onLanguageChange(lang.code)">
                <span class="radio-label">{{ lang.translateKey | translate }}</span>
              </label>
            }
          </div>
        </div>
        <div class="mt-6">
          <my-button [text]="'GAME_OPTIONS.BACK' | translate" variant="pixel" size="large" (clicked)="onBack()"></my-button>
        </div>
      </div>
    </div>
  `,
  styles: [`
    #game-options {
      position: fixed;
      top: 0;
      left: 0;
      width: 100%;
      height: 100%;
      background: rgba(0, 0, 0, 0.85);
      display: flex;
      align-items: center;
      justify-content: center;
      z-index: 1000;
    }
    .options-content {
      background-color: var(--pixel-panel-bg);
      background-image:
        linear-gradient(45deg, rgba(0,170,255,0.04) 25%, transparent 25%),
        linear-gradient(-45deg, rgba(0,170,255,0.04) 25%, transparent 25%);
      background-size: 4px 4px;
      border: 4px solid;
      border-color: var(--pixel-border-top-left) var(--pixel-border-bottom-right) var(--pixel-border-bottom-right) var(--pixel-border-top-left);
      border-radius: 0;
      padding: 2.5rem;
      min-width: 620px;
      box-shadow: 0 0 30px rgba(0,170,255,0.2);
      text-align: center;
    }
    .options-columns {
      display: flex;
      gap: 1.5rem;
      justify-content: center;
      margin-top: 1.5rem;
    }
    .options-section {
      flex: 1;
      min-width: 220px;
    }
    .section-divider {
      border-bottom: 2px solid rgba(0,170,255,0.15);
      margin-bottom: 1rem;
      margin-top: 0.5rem;
    }
    .volume-control {
      margin-bottom: 1rem;
      padding: 0.8rem;
      background: var(--pixel-card-bg);
      border: 2px solid;
      border-color: var(--pixel-border-top-left) var(--pixel-border-bottom-right) var(--pixel-border-bottom-right) var(--pixel-border-top-left);
      border-radius: 0;
    }
    .volume-label {
      font-family: 'Press Start 2P', monospace;
      font-size: 0.5rem;
      color: var(--text-secondary);
      display: block;
      margin-bottom: 0.5rem;
    }
    .volume-row {
      display: flex;
      align-items: center;
      gap: 0.5rem;
    }
    .vol-icon {
      font-size: 0.85rem;
      color: var(--text-secondary);
      display: flex;
      align-items: center;
    }
    .volume-slider {
      flex: 1;
      height: 6px;
      -webkit-appearance: none;
      appearance: none;
      background: var(--pixel-input-bg);
      border: none;
      border-radius: 0;
      outline: none;
      cursor: pointer;
    }
    .volume-slider::-webkit-slider-thumb {
      -webkit-appearance: none;
      width: 12px;
      height: 12px;
      border-radius: 0;
      background: var(--neon-cyan);
      cursor: pointer;
      box-shadow:
        0 -1px 0 0 var(--pixel-border-top-left),
        0 1px 0 0 var(--pixel-border-hover-bottom-right),
        1px 0 0 0 var(--pixel-border-hover-bottom-right),
        -1px 0 0 0 var(--pixel-border-top-left);
    }
    .volume-slider::-moz-range-thumb {
      width: 12px;
      height: 12px;
      border-radius: 0;
      background: var(--neon-cyan);
      cursor: pointer;
      border: none;
      box-shadow:
        0 -1px 0 0 var(--pixel-border-top-left),
        0 1px 0 0 var(--pixel-border-hover-bottom-right),
        1px 0 0 0 var(--pixel-border-hover-bottom-right),
        -1px 0 0 0 var(--pixel-border-top-left);
    }
    .volume-pct {
      font-family: 'Press Start 2P', monospace;
      font-size: 0.45rem;
      color: var(--neon-cyan);
      margin-top: 0.3rem;
      text-align: right;
    }
    .radio-option {
      display: flex;
      align-items: center;
      gap: 0.65rem;
      padding: 0.7rem 0.8rem;
      margin-bottom: 0.6rem;
      background: var(--pixel-card-bg);
      border: 2px solid;
      border-color: var(--pixel-border-top-left) var(--pixel-border-bottom-right) var(--pixel-border-bottom-right) var(--pixel-border-top-left);
      border-radius: 0;
      cursor: pointer;
      transition: all 0.2s;
    }
    .radio-option:hover {
      background: var(--pixel-hover-bg);
      border-color: var(--pixel-border-hover-top-left) var(--pixel-border-hover-bottom-right) var(--pixel-border-hover-bottom-right) var(--pixel-border-hover-top-left);
    }
    .radio-option input[type="radio"] {
      appearance: none;
      -webkit-appearance: none;
      width: 14px;
      height: 14px;
      border: 2px solid var(--neon-cyan);
      border-radius: 0;
      outline: none;
      cursor: pointer;
      position: relative;
      flex-shrink: 0;
      background: var(--pixel-input-bg);
      box-shadow:
        0 -1px 0 0 #050510,
        0 1px 0 0 var(--pixel-border-bottom-right),
        1px 0 0 0 var(--pixel-border-bottom-right),
        -1px 0 0 0 #050510;
    }
    .radio-option input[type="radio"]:checked::after {
      content: '';
      position: absolute;
      top: 2px;
      left: 2px;
      width: 6px;
      height: 6px;
      background: var(--neon-cyan);
      box-shadow: 0 0 6px rgba(0,170,255,0.6);
    }
    .radio-label {
      font-family: 'Press Start 2P', monospace;
      font-size: 0.55rem;
      color: var(--text-primary);
    }
    .title-pokeball {
      width: 22px;
      height: 22px;
    }
  `],
})
export class GameOptionsComponent {
  @Output() back = new EventEmitter<void>();

  audioMaster = inject(AudioMasterService);
  audioMusic = inject(AudioMusicChannel);
  sfx = inject(AudioSfxChannel);
  langService = inject(LanguageService);

  // Lista de idiomas obtenida del registry de fábricas (se calcula una sola vez)
  readonly availableLanguages = this.langService.getAvailableLanguages();

  onBack() { this.sfx.play('Click.mp3'); this.back.emit(); }

  onMasterVolumeChange(event: Event) {
    this.audioMaster.setMasterVolume(parseFloat((event.target as HTMLInputElement).value));
  }

  onMusicGainChange(event: Event) {
    this.audioMusic.setGain(parseFloat((event.target as HTMLInputElement).value));
  }

  onSfxGainChange(event: Event) {
    this.sfx.setGain(parseFloat((event.target as HTMLInputElement).value));
  }

  onLanguageChange(lang: AppLang) {
    this.langService.setLanguage(lang);
  }
}
