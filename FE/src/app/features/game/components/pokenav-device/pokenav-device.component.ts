import { Component, OnInit, OnDestroy, inject, signal, computed, effect, ChangeDetectionStrategy } from '@angular/core';
import { input, output } from '@angular/core';
import { TranslateModule } from '@ngx-translate/core';
import { AudioSfxChannel } from '../../../../core/services/audio-sfx-channel.service';

interface MenuItem {
  id: number;
  number: number;
  icon: string;
  iconBg: string;
  titleKey: string;
  descKey: string;
  action: 'resume' | 'profile' | 'decks' | 'options' | 'reset' | 'exit' | 'surrender';
}

@Component({
  selector: 'app-pokenav-device',
  standalone: true,
  imports: [TranslateModule],
  changeDetection: ChangeDetectionStrategy.OnPush,
  templateUrl: './pokenav-device.component.html',
  styleUrls: ['./pokenav-device.component.css']
})
export class PokenavDeviceComponent implements OnInit, OnDestroy {
  isOpen = input(false);
  /**
   * Modo reducido para secciones fuera del juego (perfil, mazos): muestra
   * una sola columna con tres opciones (Continuar, Opciones, Salir) en lugar
   * del menu completo de seis acciones en dos columnas.
   */
  compact = input(false);
  /**
   * Modo reducido para batalla: muestra solo Continuar, Opciones, Rendirse
   * en lugar del menu completo.
   */
  battleCompact = input(false);
  resume = output();
  showProfile = output();
  showOptions = output();
  showDecks = output();
  exit = output();
  resetCharacter = output();
  surrender = output();

  private sfx = inject(AudioSfxChannel);

  selectedIndex = signal(0);
  currentTime = signal('00:00');
  showExitConfirm = signal(false);
  showResetConfirm = signal(false);
  showSurrenderConfirm = signal(false);

  menuItems: MenuItem[] = [
    { id: 0, number: 1, icon: '▶️',  iconBg: 'rgba(20,140,40,0.25)',  titleKey: 'GAME_MENU.CONTINUE',        descKey: 'GAME_MENU.CONTINUE_DESC', action: 'resume' },
    { id: 1, number: 2, icon: '🎖️', iconBg: 'rgba(200,120,0,0.25)',  titleKey: 'GAME_MENU.PROFILE',         descKey: 'GAME_MENU.PROFILE_DESC',  action: 'profile' },
    { id: 2, number: 3, icon: '🃏',  iconBg: 'rgba(20,80,200,0.25)',  titleKey: 'GAME_MENU.DECKS',           descKey: 'GAME_MENU.DECKS_DESC',    action: 'decks' },
    { id: 3, number: 4, icon: '⚙️',  iconBg: 'rgba(60,80,120,0.25)', titleKey: 'GAME_MENU.OPTIONS',          descKey: 'GAME_MENU.OPTIONS_DESC',  action: 'options' },
    { id: 4, number: 5, icon: '🔄',  iconBg: 'rgba(200,30,30,0.25)',  titleKey: 'GAME_MENU.RESET_CHARACTER', descKey: 'GAME_MENU.RESET_DESC',    action: 'reset' },
    { id: 5, number: 6, icon: '🚪',  iconBg: 'rgba(180,80,0,0.25)',  titleKey: 'GAME_MENU.EXIT',            descKey: 'GAME_MENU.EXIT_DESC',     action: 'exit' },
    { id: 6, number: 7, icon: '🏳️',  iconBg: 'rgba(200,30,30,0.25)',  titleKey: 'GAME_MENU.SURRENDER',       descKey: 'GAME_MENU.SURRENDER_DESC', action: 'surrender' },
  ];

  /** Acciones visibles en modo compacto, en orden: Continuar, Opciones, Salir. */
  private readonly compactActions: ReadonlyArray<MenuItem['action']> = ['resume', 'options', 'exit'];
  /** Acciones visibles en modo batalla: Continuar, Opciones, Rendirse. */
  private readonly battleCompactActions: ReadonlyArray<MenuItem['action']> = ['resume', 'options', 'surrender'];

  /**
   * Items que se renderizan: el set completo, solo Continuar/Opciones/Salir
   * cuando {@link compact} esta activo, o Continuar/Opciones/Rendirse cuando
   * {@link battleCompact} esta activo.
   */
  displayedItems = computed<MenuItem[]>(() => {
    if (this.battleCompact()) {
      return this.battleCompactActions.map(action => this.menuItems.find(item => item.action === action)!);
    }
    if (this.compact()) {
      return this.compactActions.map(action => this.menuItems.find(item => item.action === action)!);
    }
    return this.menuItems.filter(i => i.action !== 'surrender');
  });

  speakerDots: number[] = Array(12).fill(0);

  private clockInterval?: ReturnType<typeof setInterval>;

  constructor() {
    effect(() => {
      if (!this.isOpen()) {
        this.showExitConfirm.set(false);
        this.showResetConfirm.set(false);
        this.showSurrenderConfirm.set(false);
        this.selectedIndex.set(0);
      }
    });
  }

  ngOnInit(): void {
    this.updateClock();
    this.clockInterval = setInterval(() => this.updateClock(), 60000);
  }

  ngOnDestroy(): void {
    if (this.clockInterval) {
      clearInterval(this.clockInterval);
    }
  }

  selectItem(index: number): void {
    this.sfx.play('Click.mp3');
    this.selectedIndex.set(index);

    const item = this.displayedItems()[index];
    if (!item) return;

    switch (item.action) {
      case 'resume':
        this.resume.emit();
        break;
      case 'profile':
        this.showProfile.emit();
        break;
      case 'decks':
        this.showDecks.emit();
        break;
      case 'options':
        this.showOptions.emit();
        break;
      case 'reset':
        this.showResetConfirm.set(true);
        break;
      case 'exit':
        this.showExitConfirm.set(true);
        break;
      case 'surrender':
        this.showSurrenderConfirm.set(true);
        break;
    }
  }

  onConfirmExit(): void {
    this.exit.emit();
  }

  onCancelExit(): void {
    this.sfx.play('Click.mp3');
    this.showExitConfirm.set(false);
  }

  onResetYes(): void {
    this.sfx.play('Click.mp3');
    this.showResetConfirm.set(false);
    this.resetCharacter.emit();
  }

  onResetNo(): void {
    this.sfx.play('Click.mp3');
    this.showResetConfirm.set(false);
  }

  onConfirmSurrender(): void {
    this.surrender.emit();
  }

  onCancelSurrender(): void {
    this.sfx.play('Click.mp3');
    this.showSurrenderConfirm.set(false);
  }

  trackById(_index: number, item: MenuItem): number {
    return item.id;
  }

  private updateClock(): void {
    const now = new Date();
    const h = now.getHours().toString().padStart(2, '0');
    const m = now.getMinutes().toString().padStart(2, '0');
    this.currentTime.set(`${h}:${m}`);
  }
}
