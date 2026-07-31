import { StorageService } from '../../../../core/services/storage.service';
import { Injectable, NgZone, signal, inject } from '@angular/core';
import { Router } from '@angular/router';
import { GameEngine, GameData } from '../../engine/game-engine';
import { LocationKey } from '../../domain/models/location.model';
import { LOCATIONS_CONFIG, NpcSpawn } from '../../engine/locations.config';
import { AudioMusicChannel } from '../../../../core/services/audio-music-channel.service';
import { ToastService } from '../../../../shared/services/toast.service';
import { AuthService } from './auth.service';
import { CircleTransitionService } from '../../../../shared/services/circle-transition.service';

@Injectable({ providedIn: 'root' })
export class GameEngineService {
  private engine: GameEngine | null = null;
  private currentLocation: LocationKey | null = null;
  readonly transitionActive = signal(false);
  readonly nearNpcIndex = signal<number | null>(null);
  readonly nearNpcPosition = signal<{ x: number; y: number }>({ x: 0, y: 0 });
  readonly interactionOpen = signal(false);
  readonly interactionDialog = signal('');
  readonly interactionNpcName = signal('');
  readonly interactionNpcConfig = signal<NpcSpawn | null>(null);
  readonly nearChair = signal(false);
  readonly slotMachinePosition = signal<{ x: number; y: number }>({ x: 0, y: 0 });
  readonly nearSecret = signal(false);
  readonly secretPosition = signal<{ x: number; y: number }>({ x: 0, y: 0 });
  readonly secretCodeOpen = signal(false);

  private router = inject(Router);
  private ngZone = inject(NgZone);
  private storage = inject(StorageService);
  private audioMusic = inject(AudioMusicChannel);
  private authService = inject(AuthService);
  private toast = inject(ToastService);
  private circleTransition = inject(CircleTransitionService);

  async startGame(
    canvas: HTMLCanvasElement,
    locationKey: LocationKey,
    data: GameData,
    username: string,
    spawnTile?: { col: number; row: number }
  ) {
    this.destroy();

    const ctx = canvas.getContext('2d');
    if (!ctx) return;

    if (!spawnTile) {
      const saved = this.storage.getSavedPosition(username);
      if (saved?.spawnTile) {
        spawnTile = saved.spawnTile;
      }
    }

    const user = this.authService.currentUser();
    let characterKey: string | undefined;
    if (user?.character) {
      characterKey = user.color ? `${user.character}_${user.color}` : user.character;
    } else {
      const localChar = this.storage.getCharacter(username);
      if (localChar?.character) {
        characterKey = localChar.color ? `${localChar.character}_${localChar.color}` : localChar.character;
        if (user) {
          this.authService.updateCharacter(localChar.character, localChar.color ?? null).subscribe();
        }
      }
    }

    this.engine = new GameEngine(locationKey, data, username, this.storage, characterKey, spawnTile);
    this.engine.setNavigator((dest, params) => this.navigateTo(dest, params));
    this.currentLocation = locationKey;
    this.engine.onBeforeLeaveZone = () => {
      if (this.currentLocation === 'tutorial' && (this.authService.currentUser()?.decks?.length ?? 0) === 0) {
        this.toast.show('Completá el tutorial para poder salir', 'info');
        return false;
      }
      return true;
    };
    this.engine.onNearNpcChange = (index) => {
      this.ngZone.run(() => this.nearNpcIndex.set(index));
    };
    this.engine.onNearNpcPositionChange = (pos) => {
      this.ngZone.run(() => this.nearNpcPosition.set({ x: pos.x, y: pos.y }));
    };
    this.engine.onNearChairChange = (near) => {
      this.ngZone.run(() => this.nearChair.set(near));
    };
    this.engine.onSlotMachineScreenPosChange = (pos) => {
      this.ngZone.run(() => this.slotMachinePosition.set({ x: pos.x, y: pos.y }));
    };
    this.engine.onNearSecretChange = (near) => {
      this.ngZone.run(() => this.nearSecret.set(near));
    };
    this.engine.onSecretScreenPosChange = (pos) => {
      this.ngZone.run(() => this.secretPosition.set({ x: pos.x, y: pos.y }));
    };

    canvas.width = window.innerWidth;
    canvas.height = window.innerHeight;

    await this.engine.initialize(canvas, ctx);

    this.transitionActive.set(false);

    const musicMap: Partial<Record<LocationKey, string>> = {
      ciudadPrincipal: 'City.mp3',
      preLan: 'preLan.ogg',
      lan: 'Lan.mp3',
      secret: 'secretLevel.mp3',
      tutorial: 'tutorial.mp3',
    };

    const track = musicMap[locationKey];
    if (track) {
      this.audioMusic.play(track);
    }

    this.ngZone.runOutsideAngular(() => {
      this.engine!.start();
    });
  }

  private navigateTo(destination: string, params: Record<string, string>) {
    const locKey = destination.replace('.html', '').replace('indexCiudadPrincipal', 'ciudadPrincipal').replace('index', '');

    this.ngZone.run(() => {
      this.transitionActive.set(true);

      setTimeout(() => {
        this.router.navigate(['/juego', locKey], { queryParams: params });
        //this.appRef.tick();
      }, 250);
    });
  }

  animatePlayerFall(): Promise<void> {
    return this.engine ? this.engine.animatePlayerFall() : Promise.resolve();
  }

  saveGame(): string | null {
    return this.engine?.saveGame() || null;
  }

  getPlayTime(): string {
    return this.engine?.getPlayTime() || '00:00:00';
  }

  getUsername(): string {
    return this.engine?.getUsername() || '';
  }

  getUserEmail(): string {
    return this.engine?.getUserEmail() || '';
  }

  toggleMenu(): boolean {
    if (!this.engine) return false;
    if (this.engine.menuOpen) {
      this.engine.menuOpen = false;
      this.engine.isRunning = true;
      this.engine.start();
      return false;
    } else {
      this.engine.isRunning = false;
      this.engine.menuOpen = true;
      return true;
    }
  }

  async navigateToProfile(): Promise<void> {
    this.saveGame();
    await this.circleTransition.close();
    this.destroy();
    this.ngZone.run(() => this.router.navigate(['/profile']));
  }

  async navigateToDecks(): Promise<void> {
    this.saveGame();
    await this.circleTransition.close();
    this.destroy();
    this.ngZone.run(() => this.router.navigate(['/decks']));
  }

  exitToLogin(): void {
    this.destroy();
    this.authService.logout();
    this.ngZone.run(() => this.router.navigateByUrl('/login', { replaceUrl: true }));
  }

  getNearNpcPosition(): { x: number; y: number } {
    return this.nearNpcPosition();
  }

  openInteraction() {
    this.ngZone.run(() => {
      const idx = this.nearNpcIndex();
      const config = this.engine?.config;
      const npcCfg = idx !== null && config?.npcs ? config.npcs[idx] : null;
      this.interactionNpcConfig.set(npcCfg);
      this.interactionOpen.set(true);
      this.engine?.openInteraction();
    });
  }

  closeInteraction() {
    this.interactionOpen.set(false);
    this.interactionDialog.set('');
    this.interactionNpcName.set('');
    this.interactionNpcConfig.set(null);
    this.engine?.closeInteraction();
  }

  showDialog(text: string) {
    this.interactionDialog.set(text);
    this.interactionOpen.set(false);
  }

  hideDialog() {
    this.closeInteraction();
  }

  pressKey(dir: 'w' | 'a' | 's' | 'd'): void {
    this.engine?.inputHandler?.pressVirtualKey(dir);
  }

  releaseKey(dir: 'w' | 'a' | 's' | 'd'): void {
    this.engine?.inputHandler?.releaseVirtualKey(dir);
  }

  releaseAllKeys(): void {
    this.engine?.inputHandler?.releaseAllVirtualKeys();
  }

  destroy(): void {
    this.audioMusic.stop();
    this.nearNpcIndex.set(null);
    this.interactionOpen.set(false);
    this.interactionDialog.set('');
    this.interactionNpcName.set('');
    this.interactionNpcConfig.set(null);
    this.nearChair.set(false);
    this.slotMachinePosition.set({ x: 0, y: 0 });
    this.nearSecret.set(false);
    this.secretPosition.set({ x: 0, y: 0 });
    this.secretCodeOpen.set(false);
    if (this.engine) {
      this.engine.stop();
      this.engine = null;
    }
  }

  openSecretCodeMenu() {
    this.ngZone.run(() => {
      this.secretCodeOpen.set(true);
      if (this.engine) {
        this.engine.isRunning = false;
      }
    });
  }

  closeSecretCodeMenu() {
    this.secretCodeOpen.set(false);
    if (this.engine) {
      this.engine.isRunning = true;
      this.engine.start();
    }
  }
}
