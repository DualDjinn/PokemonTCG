import { Component, OnInit, OnDestroy, inject, ElementRef, ViewChild, DestroyRef } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { Subject, switchMap, takeUntil } from 'rxjs';
import { PokenavDeviceComponent } from '../../components/pokenav-device/pokenav-device.component';
import { GameOptionsComponent } from '../../components/game-options/game-options';
import { GameCharacterBarComponent } from '../../components/game-character-bar/game-character-bar';
import { NpcInteractionComponent } from '../../components/npc-interaction/npc-interaction';
import { VirtualJoystickComponent } from '../../components/virtual-joystick/virtual-joystick.component';
import { FloatingMenuButtonComponent } from '../../components/floating-menu-button/floating-menu-button.component';
import { GameEngineService } from '../../data-access/services/game-engine.service';
import { QuestService } from '../../data-access/services/quest.service';
import { AudioMasterService } from '../../../../core/services/audio-master.service';
import { AudioMusicChannel } from '../../../../core/services/audio-music-channel.service';
import { StorageService } from '../../../../core/services/storage.service';
import { CircleTransitionService } from '../../../../shared/services/circle-transition.service';
import { GameData } from '../../engine/game-engine';
import { LOCATIONS_CONFIG } from '../../engine/locations.config';
import { COLLISIONS_CIUDAD_PRINCIPAL } from '../../engine/data/collisions-ciudad-principal';
import { COLLISIONS_LAN } from '../../engine/data/collisions-lan';
import { COLLISIONS_PRE_LAN } from '../../engine/data/collisions-pre-lan';
import { COLLISIONS_CHAIR } from '../../engine/data/collisions-chair';
import { COLLISIONS_SLOT_MACHINE } from '../../engine/data/collisions-slot-machine';
import { COLLISIONS_SECRET } from '../../engine/data/collisions-secret';
import { LEAVE_BUILD_CIUDAD_PRINCIPAL, ENTER_PRE_LAN, ENTER_TUTORIAL } from '../../engine/data/builds-ciudad-principal';
import { LEAVE_BUILD_LAN } from '../../engine/data/builds-lan';
import { LEAVE_BUILD_PRE_LAN, ENTER_BUILD_PRE_LAN, ENTER_BUILD_SECRET } from '../../engine/data/builds-pre-lan';
import { LEAVE_BUILD_SECRET } from '../../engine/data/builds-secret';
import { COLLISIONS_TUTORIAL } from '../../engine/data/collisions-tutorial';
import { LEAVE_BUILD_TUTORIAL } from '../../engine/data/builds-tutorial';
import { LocationKey } from '../../domain/models/location.model';

@Component({
  selector: 'app-game-page',
  standalone: true,
  imports: [PokenavDeviceComponent, GameOptionsComponent, GameCharacterBarComponent, NpcInteractionComponent, VirtualJoystickComponent, FloatingMenuButtonComponent],
  template: `
    <app-game-character-bar />
    <div class="transition-overlay" [class.active]="engineService.transitionActive()"></div>
    <canvas #gameCanvas></canvas>
    <app-virtual-joystick />
    <app-floating-menu-button (menuToggle)="onFloatingMenuToggle()" />
    <app-pokenav-device
      [isOpen]="menuOpen && !optionsOpen"
      (resume)="onResume()"
      (showProfile)="onShowProfile()"
      (showOptions)="onShowOptions()"
      (showDecks)="onShowDecks()"
      (exit)="onExit()"
      (resetCharacter)="onResetCharacter()">
    </app-pokenav-device>
    @if (optionsOpen) {
      <app-game-options (back)="onCloseOptions()"></app-game-options>
    }
    <app-npc-interaction />
  `,
  host: {
    '(document:keydown)': 'handleKeydown($event)',
    '(contextmenu)': '$event.preventDefault()',
  },
  styles: [`
    :host { display: block; width: 100%; height: 100vh; overflow: hidden; background: #0a0a1a; user-select: none; }
    canvas { display: block; width: 100%; height: 100vh; }
    .transition-overlay {
      position: fixed; top: 0; left: 0; width: 100%; height: 100%;
      background: #000; z-index: 999; pointer-events: none;
      opacity: 0;
      transition: opacity 250ms ease-in-out;
    }
    .transition-overlay.active {
      opacity: 1;
    }
  `],
})
export class GamePage implements OnDestroy {
  private route = inject(ActivatedRoute);
  private router = inject(Router);
  private destroyRef = inject(DestroyRef);
  private storage = inject(StorageService);
  engineService = inject(GameEngineService);
  private questService = inject(QuestService);
  audioMaster = inject(AudioMasterService);
  audioMusic = inject(AudioMusicChannel);
  private circleTransition = inject(CircleTransitionService);
  private destroy$ = new Subject<void>();

  @ViewChild('gameCanvas', { static: true })
  private canvasRef!: ElementRef<HTMLCanvasElement>;

  locationKey = '';
  gameData: GameData | null = null;
  menuOpen = false;
  optionsOpen = false;

  private readonly locationDataMap: Record<string, () => GameData> = {
    ciudadPrincipal: () => ({
      collisions: COLLISIONS_CIUDAD_PRINCIPAL,
      enterBuild: LEAVE_BUILD_CIUDAD_PRINCIPAL,
      leaveBuild: LEAVE_BUILD_CIUDAD_PRINCIPAL,
      enterPreLan: ENTER_PRE_LAN,
      enterTutorial: ENTER_TUTORIAL,
    }),
    lan: () => ({
      collisions: COLLISIONS_LAN,
      leaveBuild: LEAVE_BUILD_LAN,
    }),
    preLan: () => ({
      collisions: COLLISIONS_PRE_LAN,
      collisionsChair: COLLISIONS_CHAIR,
      collisionsSlotMachine: COLLISIONS_SLOT_MACHINE,
      enterBuild: ENTER_BUILD_PRE_LAN,
      leaveBuild: LEAVE_BUILD_PRE_LAN,
      enterBuildSecret: ENTER_BUILD_SECRET,
    }),
    secret: () => ({
      collisions: COLLISIONS_SECRET,
      leaveBuild: LEAVE_BUILD_SECRET,
    }),
    tutorial: () => ({
      collisions: COLLISIONS_TUTORIAL,
      enterBuild: LEAVE_BUILD_TUTORIAL,
      leaveBuild: LEAVE_BUILD_TUTORIAL,
    }),
  };

  ngOnInit() {
    // Precarga qué recompensas de NPC ya reclamó el jugador, para decidir
    // sin esperar a la red si corresponde la animación de carta secreta.
    this.questService.loadClaimedRewards();

    this.route.params.pipe(
      switchMap(params => {
        const loc = params['location'] || 'ciudadPrincipal';
        this.locationKey = loc;
        const dataFactory = this.locationDataMap[loc];
        this.gameData = dataFactory ? dataFactory() : null;
        return this.route.queryParams;
      }),
      takeUntil(this.destroy$),
    ).subscribe(qp => {
      const username = qp['user'] || localStorage.getItem('lastUser') || '';
      if (qp['user']) {
        localStorage.setItem('lastUser', qp['user']);
      }

      const dcol = parseInt(qp['dcol']) || 0;
      const drow = parseInt(qp['drow']) || 0;
      const hasTileDelta = qp['dcol'] !== undefined;
      const col = parseInt(qp['col']) || 0;
      const row = parseInt(qp['row']) || 0;
      const hasDirectTile = qp['col'] !== undefined;

      if (username && this.locationKey && this.gameData) {
        let spawnTile: { col: number; row: number } | undefined;
        if (hasTileDelta) {
          const config = LOCATIONS_CONFIG[this.locationKey];
          spawnTile = {
            col: config.spawnTile.col + dcol,
            row: config.spawnTile.row + drow,
          };
        } else if (hasDirectTile) {
          spawnTile = { col, row };
        }

        this.engineService.startGame(
          this.canvasRef.nativeElement,
          this.locationKey as LocationKey,
          this.gameData,
          username,
          spawnTile,
        );

        if (this.circleTransition.pendingOpen) {
          setTimeout(() => this.circleTransition.open(), 400);
        }
      }
    });
  }

  handleKeydown(event: KeyboardEvent) {
    if (event.key === 'Escape') {
      if (this.optionsOpen) {
        this.onCloseOptions();
      } else if (this.engineService.interactionOpen()) {
        this.engineService.closeInteraction();
      } else if (this.engineService.interactionDialog()) {
        return;
      } else {
        this.toggleMenu();
      }
      return;
    }

    // Tecla "E": abrir la conversación con el NPC cercano, la máquina secreta
    // o la Slot Machine si no hay otra UI activa.
    if (event.key === 'e' || event.key === 'E') {
      const hasNearbyNpc = this.engineService.nearNpcIndex() !== null;
      const nearSecret = this.engineService.nearSecret();
      const nearChair = this.engineService.nearChair();
      const interactionAlreadyOpen = this.engineService.interactionOpen();
      const dialogOpen = !!this.engineService.interactionDialog();
      const secretCodeOpen = this.engineService.secretCodeOpen();
      if (!interactionAlreadyOpen
              && !dialogOpen
              && !secretCodeOpen
              && !this.menuOpen
              && !this.optionsOpen) {
        if (hasNearbyNpc) {
          this.engineService.openInteraction();
        } else if (nearSecret) {
          event.stopImmediatePropagation();
          this.engineService.openSecretCodeMenu();
        } else if (nearChair) {
          event.stopImmediatePropagation();
          this.engineService.saveGame();
          this.circleTransition.close();
          this.router.navigate(['/slot-machine']);
        }
      }
    }
  }

  private toggleMenu() {
    this.optionsOpen = false;
    this.menuOpen = this.engineService.toggleMenu();
  }

  /**
   * Atajo táctil: el botón flotante usa la misma lógica que Escape, pero también
   * cierra una interacción NPC o el panel de opciones si están abiertos.
   */
  onFloatingMenuToggle(): void {
    if (this.optionsOpen) {
      this.onCloseOptions();
      return;
    }
    if (this.engineService.interactionOpen()) {
      this.engineService.closeInteraction();
      return;
    }
    this.toggleMenu();
  }

  onResume() {
    this.menuOpen = false;
    this.engineService.toggleMenu();
  }

  onShowOptions() {
    this.menuOpen = false;
    this.optionsOpen = true;
  }

  onCloseOptions() {
    this.optionsOpen = false;
    this.menuOpen = true;
  }

  async onResetCharacter(): Promise<void> {
    this.optionsOpen = false;
    this.menuOpen = false;

    const username = this.engineService.getUsername();
    await this.engineService.animatePlayerFall();
    this.storage.clearSavedPosition(username);

    const currentLocation: LocationKey = (this.locationKey as LocationKey) || 'ciudadPrincipal';
    const data = this.locationDataMap[currentLocation]?.();
    if (!data) return;

    this.engineService.destroy();
    await this.engineService.startGame(
      this.canvasRef.nativeElement,
      currentLocation,
      data,
      username,
      currentLocation === 'tutorial' ? { col: 33, row: 29 } : undefined,
    );

    setTimeout(() => this.circleTransition.open(), 200);
  }

  onShowProfile() {
    this.engineService.navigateToProfile();
  }

  onShowDecks() {
    this.engineService.navigateToDecks();
  }

  onExit() {
    this.engineService.saveGame();
    this.engineService.exitToLogin();
  }

  ngOnDestroy() {
    this.destroy$.next();
    this.destroy$.complete();
    this.engineService.destroy();
  }
}
