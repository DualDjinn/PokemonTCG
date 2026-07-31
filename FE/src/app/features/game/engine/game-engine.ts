import { Sprite, Boundary } from './classes';
import { InputHandler } from './input-handler';
import { LOCATIONS_CONFIG, LocationConfig } from './locations.config';
import { createMapArray, createBoundaries } from './boundary-factory';
import { rectangularCollision } from './collision-detection';
import { loadPlayerSprites, loadMapImage, loadNpcSprite } from './sprite-loader';
import { AutumnLeavesEffect } from './autumn-leaves';
import { AnimatedDecorationsEffect } from './animated-decorations';
import { GAME } from './game.constants';
import { StorageService } from '../../../core/services/storage.service';
import { User } from '../domain/models/user.model';

const TILE_PX = 28;
const NPC_INTERACTION_RANGE_TILES = 4;

export interface GameData {
  collisions: number[];
  enterBuild?: number[];
  leaveBuild?: number[];
  enterBuildSecret?: number[];
  enterPreLan?: number[];
  enterTutorial?: number[];
  collisionsChair?: number[];
  collisionsSlotMachine?: number[];
}

export class GameEngine {
  locationKey: string;
  config: LocationConfig;
  data: GameData;
  canvas: HTMLCanvasElement | null = null;
  ctx: CanvasRenderingContext2D | null = null;
  player: Sprite | null = null;
  boundaries: Boundary[] = [];
  zones: Array<{ boundaries: Boundary[]; destination: string; spawnTileDelta: { dcol: number; drow: number } | null }> = [];
  background: Sprite | null = null;
  foreground: Sprite | null = null;
  ambientLeaves: AutumnLeavesEffect | null = null;
  animatedDecorations: AnimatedDecorationsEffect | null = null;
  npcs: Sprite[] = [];
  inputHandler: InputHandler | null = null;
  readonly SCALE = GAME.SCALE;
  isRunning = false;
  menuOpen = false;
  interactionMenuOpen = false;
  interactionNpcName = '';
  nearNpcIndex: number | null = null;
  nearNpcPosition: { x: number; y: number } = { x: 0, y: 0 };
  worldOffsetX = 0;
  worldOffsetY = 0;
  private lastNearNpcIndex: number | null = null;
  private lastNotifiedPosX = 0;
  private lastNotifiedPosY = 0;
  onNearNpcChange?: (index: number | null) => void;
  onNearNpcPositionChange?: (pos: { x: number; y: number }) => void;
  onNearChairChange?: (near: boolean) => void;
  onSlotMachineScreenPosChange?: (pos: { x: number; y: number }) => void;
  onNearSecretChange?: (near: boolean) => void;
  onSecretScreenPosChange?: (pos: { x: number; y: number }) => void;
  startOffset: { x: number; y: number } | null = null;
  currentSpawnTile: { col: number; row: number };
  accumulatedTime = 0;
  startTime: number | null = null;
  username: string = '';
  private onNavigate?: (destination: string, params: Record<string, string>) => void;
  onBeforeLeaveZone?: () => boolean;
  private storage: StorageService;
  private animationFrameId: number | null = null;
  chairBoundaries: Boundary[] = [];
  slotMachineTile: { col: number; row: number } | null = null;
  enterSecretTile: { col: number; row: number } | null = null;
  nearChair = false;
  private lastNearChair = false;
  nearSecret = false;
  private lastNearSecret = false;
  private lastSlotScreenX = 0;
  private lastSlotScreenY = 0;
  private lastSecretScreenX = 0;
  private lastSecretScreenY = 0;
  private pokeballImg: HTMLImageElement;

  characterKey: string = 'red_rojo';
  facing: 'up' | 'down' | 'left' | 'right' = 'down';

  constructor(
    locationKey: string,
    data: GameData,
    username: string,
    storage: StorageService,
    characterKey?: string,
    savedSpawnTile?: { col: number; row: number }
  ) {
    this.locationKey = locationKey;
    this.config = LOCATIONS_CONFIG[locationKey];
    this.data = data;
    this.username = username;
    this.storage = storage;
    this.characterKey = characterKey ?? this.resolveCharacterKey();
    this.currentSpawnTile = savedSpawnTile ?? this.config.spawnTile;

    const savedData = this.storage.getSavedPosition(username);
    if (savedData) {
      if (savedData.playTime) {
        this.accumulatedTime = savedData.playTime;
      }
      if (savedData.facing) {
        this.facing = savedData.facing;
      }
    }
    this.startTime = Date.now();

    this.pokeballImg = new Image();
    this.pokeballImg.src = 'assets/img/login/pokeball.svg';
  }

  private resolveCharacterKey(): string {
    const charData = this.storage.getCharacter(this.username);
    if (charData?.character) {
      return charData.color ? `${charData.character}_${charData.color}` : charData.character;
    }
    return 'red_rojo';
  }

  static offsetFromTile(col: number, row: number, canvasW: number, canvasH: number): { x: number; y: number } {
    const tilePx = TILE_PX * GAME.SCALE;
    return {
      x: canvasW / GAME.PLAYER_X_RATIO - col * tilePx - tilePx / 2,
      y: canvasH / GAME.PLAYER_Y_RATIO - row * tilePx - tilePx / 2,
    };
  }

  setNavigator(nav: (destination: string, params: Record<string, string>) => void) {
    this.onNavigate = nav;
  }

  async initialize(canvas: HTMLCanvasElement, ctx: CanvasRenderingContext2D) {
    this.canvas = canvas;
    this.ctx = ctx;

    this.inputHandler = new InputHandler();
    this.inputHandler.setup();

    this.menuOpen = false;
    this.worldOffsetX = 0;
    this.worldOffsetY = 0;

    this.startOffset = GameEngine.offsetFromTile(
      this.currentSpawnTile.col,
      this.currentSpawnTile.row,
      canvas.width,
      canvas.height,
    );

    const collisionsData = this.data.collisions;
    if (!collisionsData) {
      console.error('No se encontraron datos de colisión');
      return;
    }

    const collisionsMap = createMapArray(collisionsData, this.config.mapGridSize);

    this.boundaries = createBoundaries(
      collisionsMap,
      this.config.collisionSymbol,
      this.startOffset,
      this.SCALE,
      'rgba(255, 0, 0, 0.0)'
    );

    this.createZones();

    if (this.data.collisionsChair) {
      const chairMap = createMapArray(this.data.collisionsChair, this.config.mapGridSize);
      this.chairBoundaries = createBoundaries(chairMap, 46957, this.startOffset, this.SCALE);
    }

    if (this.data.collisionsSlotMachine) {
      const slotMap = createMapArray(this.data.collisionsSlotMachine, this.config.mapGridSize);
      for (let i = 0; i < slotMap.length; i++) {
        for (let j = 0; j < slotMap[i].length; j++) {
          if (slotMap[i][j] === 47167) {
            this.slotMachineTile = { col: j, row: i };
            break;
          }
        }
        if (this.slotMachineTile) break;
      }
    }

    if (this.data.enterBuildSecret) {
      const secretMap = createMapArray(this.data.enterBuildSecret, this.config.mapGridSize);
      for (let i = 0; i < secretMap.length; i++) {
        for (let j = 0; j < secretMap[i].length; j++) {
          if (secretMap[i][j] === 6774) {
            this.enterSecretTile = { col: j, row: i };
            break;
          }
        }
        if (this.enterSecretTile) break;
      }
    }

    // Resolve sprite key from saved character data
    let spriteKey = 'red_rojo';
    const charData = this.storage.getCharacter(this.username);
    if (charData?.character) {
      spriteKey = charData.character;
      if (charData.color) spriteKey += '_' + charData.color;
    }

    const playerSprites = loadPlayerSprites(spriteKey);
    const mapImage = loadMapImage(this.config.mapImage);

    this.player = new Sprite({
      position: {
        x: this.canvas.width / GAME.PLAYER_X_RATIO - (96 / GAME.SPRITE_FRAMES * this.SCALE) / 2,
        y: this.canvas.height / GAME.PLAYER_Y_RATIO - (32 * this.SCALE) / 2,
      },
      cityMap: playerSprites[this.facing],
      frames: { max: GAME.SPRITE_FRAMES, hold: GAME.SPRITE_HOLD },
      scale: 0.5,
      sprites: playerSprites,
    });

    this.background = new Sprite({
      position: { x: this.startOffset!.x, y: this.startOffset!.y },
      cityMap: mapImage,
      scale: this.SCALE,
    });

    if (this.config.foregroundImage) {
      const foregroundImage = loadMapImage(this.config.foregroundImage);
      this.foreground = new Sprite({
        position: { x: this.startOffset!.x, y: this.startOffset!.y },
        cityMap: foregroundImage,
        scale: this.SCALE,
      });
    }

    if (this.config.ambientEffect === 'autumnLeaves') {
      this.ambientLeaves = new AutumnLeavesEffect(canvas.width, canvas.height);
    }

    if (this.config.decorations?.length) {
      this.animatedDecorations = new AnimatedDecorationsEffect(
        this.config.decorations,
        this.startOffset!,
        this.SCALE,
      );
    }

    this.loadNpcs();

    this.isRunning = true;
  }

  loadNpcs() {
    const npcSpawns = this.config.npcs;
    if (!npcSpawns) return;

    this.npcs = npcSpawns.map((npc) => {
      const npcImage = loadNpcSprite(npc.name);
      const tilePx = TILE_PX * this.SCALE;
      return new Sprite({
        position: {
          x: npc.col * tilePx + this.startOffset!.x,
          y: npc.row * tilePx + this.startOffset!.y,
        },
        cityMap: npcImage,
        frames: { max: 1, hold: 10 },
        scale: 0.5,
      });
    });
  }

  checkNpcProximity() {
    if (!this.player || !this.player.width || !this.npcs.length) return;

    const pCenterX = this.canvas!.width / GAME.PLAYER_X_RATIO;
    const pCenterY = this.canvas!.height / GAME.PLAYER_Y_RATIO;
    const tilePx = TILE_PX * this.SCALE;

    let nearestIdx: number | null = null;

    for (let i = 0; i < this.npcs.length; i++) {
      const npc = this.npcs[i];
      if (!npc.width) continue;

      const nCenterX = npc.position.x + npc.width / 2;
      const nCenterY = npc.position.y + npc.height / 2;
      const dx = pCenterX - nCenterX;
      const dy = pCenterY - nCenterY;

      if (Math.abs(dx) < tilePx * NPC_INTERACTION_RANGE_TILES && Math.abs(dy) < tilePx * NPC_INTERACTION_RANGE_TILES) {
        nearestIdx = i;
        break;
      }
    }

    if (nearestIdx !== null) {
      const npcCfg = this.config.npcs![nearestIdx];
      const newX = npcCfg.col * tilePx + this.startOffset!.x + this.worldOffsetX;
      const newY = npcCfg.row * tilePx + this.startOffset!.y + this.worldOffsetY;

      this.nearNpcPosition.x = newX;
      this.nearNpcPosition.y = newY;

      if (newX !== this.lastNotifiedPosX || newY !== this.lastNotifiedPosY) {
        this.lastNotifiedPosX = newX;
        this.lastNotifiedPosY = newY;
        this.onNearNpcPositionChange?.({ x: newX, y: newY });
      }
    }

    if (nearestIdx !== this.lastNearNpcIndex) {
      this.lastNearNpcIndex = nearestIdx;
      this.nearNpcIndex = nearestIdx;
      this.onNearNpcChange?.(nearestIdx);
    }
  }

  openInteraction() {
    if (this.nearNpcIndex === null) return;
    const npcSpawns = this.config.npcs;
    if (!npcSpawns) return;
    this.interactionNpcName = npcSpawns[this.nearNpcIndex].name;
    this.interactionMenuOpen = true;
    this.isRunning = false;
  }

  closeInteraction() {
    this.interactionMenuOpen = false;
    this.interactionNpcName = '';
    this.isRunning = true;
    this.start();
  }

  createZones() {
    for (const zoneConfig of this.config.zones) {
      const dataKey = zoneConfig.dataKey as 'enterBuild' | 'leaveBuild' | 'enterBuildSecret' ?? (zoneConfig.type === 'enter' ? 'enterBuild' : 'leaveBuild');
      const zoneData = this.data[dataKey];
      if (!zoneData) continue;

      const zoneMap = createMapArray(zoneData, this.config.mapGridSize);
      const zoneBoundaries = createBoundaries(zoneMap, zoneConfig.symbol, this.startOffset!, this.SCALE, 'rgba(0, 255, 0, 0.0)');

      this.zones.push({
        boundaries: zoneBoundaries,
        destination: zoneConfig.destination,
        spawnTileDelta: zoneConfig.spawnTileDelta || null,
      });
    }
  }

  checkChairProximity() {
    if (!this.player || !this.chairBoundaries.length) return;

    this.nearChair = false;
    for (const boundary of this.chairBoundaries) {
      if (rectangularCollision({ rectangle1: this.player, rectangle2: boundary })) {
        this.nearChair = true;
        break;
      }
    }

    if (this.nearChair !== this.lastNearChair) {
      this.lastNearChair = this.nearChair;
      this.onNearChairChange?.(this.nearChair);
    }
  }

  updateSlotMachineScreenPos() {
    if (!this.slotMachineTile) return;
    const tilePx = TILE_PX * this.SCALE;
    const x = this.slotMachineTile.col * tilePx + this.startOffset!.x + this.worldOffsetX;
    const y = this.slotMachineTile.row * tilePx + this.startOffset!.y + this.worldOffsetY;

    if (x !== this.lastSlotScreenX || y !== this.lastSlotScreenY) {
      this.lastSlotScreenX = x;
      this.lastSlotScreenY = y;
      this.onSlotMachineScreenPosChange?.({ x, y });
    }
  }

  checkSecretProximity() {
    if (!this.player || !this.player.width || !this.enterSecretTile) return;

    const pCenterX = this.canvas!.width / GAME.PLAYER_X_RATIO;
    const pCenterY = this.canvas!.height / GAME.PLAYER_Y_RATIO;
    const tilePx = TILE_PX * this.SCALE;
    const tCenterX = this.enterSecretTile.col * tilePx + this.startOffset!.x + this.worldOffsetX + tilePx / 2;
    const tCenterY = this.enterSecretTile.row * tilePx + this.startOffset!.y + this.worldOffsetY + tilePx / 2;
    const dx = pCenterX - tCenterX;
    const dy = pCenterY - tCenterY;

    this.nearSecret = Math.abs(dx) <= tilePx * 4 && Math.abs(dy) <= tilePx * 4;

    if (this.nearSecret !== this.lastNearSecret) {
      this.lastNearSecret = this.nearSecret;
      this.onNearSecretChange?.(this.nearSecret);
    }
  }

  updateSecretScreenPos() {
    if (!this.enterSecretTile) return;
    const tilePx = TILE_PX * this.SCALE;
    const x = this.enterSecretTile.col * tilePx + this.startOffset!.x + this.worldOffsetX;
    const y = this.enterSecretTile.row * tilePx + this.startOffset!.y + this.worldOffsetY;

    if (x !== this.lastSecretScreenX || y !== this.lastSecretScreenY) {
      this.lastSecretScreenX = x;
      this.lastSecretScreenY = y;
      this.onSecretScreenPosChange?.({ x, y });
    }
  }

  drawPokeSignal(ctx: CanvasRenderingContext2D, tile: { col: number; row: number }, color: string) {
    const tilePx = TILE_PX * this.SCALE;
    const centerX = tile.col * tilePx + this.startOffset!.x + this.worldOffsetX + tilePx / 2;
    const baseY = tile.row * tilePx + this.startOffset!.y + this.worldOffsetY - 20;

    const bounce = Math.sin(Date.now() * 0.006) * 6;
    const arrowY = baseY + bounce;

    const arrowWidth = 39;
    const arrowHeight = 45;

    ctx.save();
    ctx.fillStyle = color;
    ctx.shadowColor = color;
    ctx.shadowBlur = 12;

    ctx.beginPath();
    ctx.moveTo(centerX, arrowY + arrowHeight);
    ctx.lineTo(centerX - arrowWidth / 2, arrowY + 4);
    ctx.lineTo(centerX - arrowWidth / 2, arrowY);
    ctx.lineTo(centerX + arrowWidth / 2, arrowY);
    ctx.lineTo(centerX + arrowWidth / 2, arrowY + 4);
    ctx.closePath();
    ctx.fill();

    ctx.restore();

    const pokeballSize = 39;
    const pokeballY = arrowY - pokeballSize / 2;
    // Defensivo: si la imagen no terminó de cargar o quedó en estado `broken`,
    // saltamos el draw para no romper el game loop entero.
    if (this.pokeballImg.complete && this.pokeballImg.naturalWidth > 0) {
      ctx.drawImage(this.pokeballImg, centerX - pokeballSize / 2, pokeballY, pokeballSize, pokeballSize);
    }
  }

  start() {
    if (this.animationFrameId !== null) {
      cancelAnimationFrame(this.animationFrameId);
      this.animationFrameId = null;
    }
    this.animate();
  }

  animate = () => {
    if (!this.isRunning) return;
    this.animationFrameId = window.requestAnimationFrame(this.animate);

    this.background?.draw(this.ctx!);
    this.boundaries.forEach((b) => b.draw(this.ctx!));

    for (const zone of this.zones) {
      zone.boundaries.forEach((b) => b.draw(this.ctx!));
    }

    // Decoraciones animadas (fuente, flores): se dibujan ANTES de NPCs y
    // jugador, así el jugador y los NPCs pasan por delante en lugar de
    // quedar tapados por la decoración.
    this.animatedDecorations?.render(this.ctx!);

    for (const npc of this.npcs) {
      npc.draw(this.ctx!);
    }

    this.player?.draw(this.ctx!);

    if (this.foreground) {
      this.foreground.draw(this.ctx!);
    }

    // Las hojas van por encima del foreground: están en el aire.
    this.ambientLeaves?.render(this.ctx!, this.canvas!.width, this.canvas!.height);

    this.checkChairProximity();
    this.updateSlotMachineScreenPos();
    if (this.slotMachineTile) {
      this.drawPokeSignal(this.ctx!, this.slotMachineTile, '#ff2020');
    }

    this.checkSecretProximity();
    this.updateSecretScreenPos();
    if (this.enterSecretTile) {
      this.drawPokeSignal(this.ctx!, this.enterSecretTile, '#9b59b6');
    }

    this.checkNpcProximity();
    this.checkZoneCollisions();
    this.handleMovement();
  };

  checkZoneCollisions() {
    for (const zone of this.zones) {
      for (const boundary of zone.boundaries) {
        if (this.player && rectangularCollision({ rectangle1: this.player, rectangle2: boundary })) {
          const params: Record<string, string> = { user: this.username };
          if (zone.spawnTileDelta) {
            params['dcol'] = String(zone.spawnTileDelta.dcol);
            params['drow'] = String(zone.spawnTileDelta.drow);
          }
          if (this.onBeforeLeaveZone && !this.onBeforeLeaveZone()) {
            return;
          }
          this.isRunning = false;
          this.onNavigate?.(zone.destination, params);
          return;
        }
      }
    }
  }

  handleMovement() {
    if (!this.inputHandler || !this.player) return;
    const input = this.inputHandler;

    if (input.key.w.pressed && input.lastKey === 'w') {
      this.player.moving = true;
      this.player.setSprite(this.player.sprites!.up);
      this.facing = 'up';
      if (this.canMoveUp()) this.moveAll(0, GAME.STEP);
    } else if (input.key.a.pressed && input.lastKey === 'a') {
      this.player.moving = true;
      this.player.setSprite(this.player.sprites!.left);
      this.facing = 'left';
      if (this.canMoveLeft()) this.moveAll(GAME.STEP, 0);
    } else if (input.key.s.pressed && input.lastKey === 's') {
      this.player.moving = true;
      this.player.setSprite(this.player.sprites!.down);
      this.facing = 'down';
      if (this.canMoveDown()) this.moveAll(0, -GAME.STEP);
    } else if (input.key.d.pressed && input.lastKey === 'd') {
      this.player.moving = true;
      this.player.setSprite(this.player.sprites!.right);
      this.facing = 'right';
      if (this.canMoveRight()) this.moveAll(-GAME.STEP, 0);
    } else {
      this.player.moving = false;
    }
  }

  canMoveUp() {
    return this.checkCollision(0, GAME.STEP);
  }
  canMoveDown() {
    return this.checkCollision(0, -GAME.STEP);
  }
  canMoveLeft() {
    return this.checkCollision(GAME.STEP, 0);
  }
  canMoveRight() {
    return this.checkCollision(-GAME.STEP, 0);
  }

  private checkCollision(offsetX: number, offsetY: number): boolean {
    if (!this.player) return false;
    for (const boundary of this.boundaries) {
      if (rectangularCollision({
        rectangle1: this.player,
        rectangle2: {
          ...boundary,
          position: {
            x: boundary.position.x + offsetX,
            y: boundary.position.y + offsetY,
          },
        },
      })) {
        return false;
      }
    }
    return true;
  }

  moveAll(x: number, y: number) {
    this.worldOffsetX += x;
    this.worldOffsetY += y;

    const movables: Array<{ position: { x: number; y: number } }> = [];
    if (this.background) movables.push(this.background);
    movables.push(...this.boundaries);
    for (const z of this.zones) movables.push(...z.boundaries);
    if (this.foreground) movables.push(this.foreground);
    movables.push(...this.npcs);
    movables.push(...this.chairBoundaries);

    movables.forEach((m) => {
      m.position.x += x;
      m.position.y += y;
    });

    // Las hojas acompañan el mundo para sentirse ancladas al mapa.
    this.ambientLeaves?.shift(x, y);
    // Decoraciones quedan pegadas al tile cuando el jugador se mueve.
    this.animatedDecorations?.shift(x, y);
  }

  private scheduleFallThud(delayMs: number): void {
    try {
      const ac = new AudioContext();
      const schedule = () => {
        const t = ac.currentTime + delayMs / 1000;
        const osc = ac.createOscillator();
        const gain = ac.createGain();
        osc.connect(gain);
        gain.connect(ac.destination);
        osc.type = 'sine';
        osc.frequency.setValueAtTime(120, t);
        osc.frequency.exponentialRampToValueAtTime(30, t + 0.18);
        gain.gain.setValueAtTime(0.6, t);
        gain.gain.exponentialRampToValueAtTime(0.001, t + 0.22);
        osc.start(t);
        osc.stop(t + 0.22);
        osc.onended = () => ac.close();
      };
      if (ac.state === 'suspended') { ac.resume().then(schedule); } else { schedule(); }
    } catch { /* silently ignore */ }
  }

  animatePlayerFall(): Promise<void> {
    return new Promise(resolve => {
      if (!this.canvas || !this.ctx || !this.player) { resolve(); return; }

      // Schedule sound synchronously here — still within the user-gesture call stack
      this.scheduleFallThud(700);

      if (this.animationFrameId !== null) {
        cancelAnimationFrame(this.animationFrameId);
        this.animationFrameId = null;
      }
      this.isRunning = false;

      const ctx = this.ctx;
      const player = this.player;
      const background = this.background;
      const foreground = this.foreground;
      const npcs = this.npcs;
      const canvas = this.canvas;

      const FALL_MS = 700;
      const FADE_MS = 600;
      const start = performance.now();

      const pivotX = player.position.x + player.width / 2;
      const pivotY = player.position.y + player.height;

      const drawPlayer = (angle: number) => {
        ctx.save();
        ctx.translate(pivotX, pivotY);
        ctx.rotate(angle);
        ctx.drawImage(
          player.cityMap,
          player.frames.val * player.frameWidth, 0,
          player.frameWidth, player.frameHeight,
          -player.width / 2, -player.height,
          player.width, player.height,
        );
        ctx.restore();
      };

      const loop = (now: number) => {
        const elapsed = now - start;
        background?.draw(ctx);
        for (const npc of npcs) npc.draw(ctx);

        if (elapsed < FALL_MS) {
          const t = elapsed / FALL_MS;
          const ease = 1 - Math.pow(1 - t, 2);
          drawPlayer((Math.PI / 2) * ease);
          foreground?.draw(ctx);
          requestAnimationFrame(loop);
        } else if (elapsed < FALL_MS + FADE_MS) {
          drawPlayer(Math.PI / 2);
          foreground?.draw(ctx);
          const fadeAlpha = (elapsed - FALL_MS) / FADE_MS;
          ctx.fillStyle = `rgba(0,0,0,${fadeAlpha})`;
          ctx.fillRect(0, 0, canvas.width, canvas.height);
          requestAnimationFrame(loop);
        } else {
          ctx.fillStyle = '#000';
          ctx.fillRect(0, 0, canvas.width, canvas.height);
          resolve();
        }
      };

      requestAnimationFrame(loop);
    });
  }

  stop() {
    this.isRunning = false;
    if (this.animationFrameId !== null) {
      cancelAnimationFrame(this.animationFrameId);
      this.animationFrameId = null;
    }
    this.inputHandler?.destroy();
  }

  saveGame(): string {
    if (!this.background || !this.username) return '';
    const tilePx = TILE_PX * GAME.SCALE;
    const currentCol = Math.floor(this.currentSpawnTile.col - this.worldOffsetX / tilePx);
    const currentRow = Math.floor(this.currentSpawnTile.row - this.worldOffsetY / tilePx);
    const currentPlayTime = this.accumulatedTime + (Date.now() - this.startTime!);
    this.storage.setSavedPosition(this.username, {
      page: this.locationKey,
      location: this.locationKey,
      spawnTile: { col: currentCol, row: currentRow },
      playTime: currentPlayTime,
      facing: this.facing,
    });
    return '¡Partida guardada!';
  }

  getPlayTime(): string {
    const currentPlayTime = this.accumulatedTime + (Date.now() - this.startTime!);
    const hours = Math.floor(currentPlayTime / 3600000);
    const minutes = Math.floor((currentPlayTime % 3600000) / 60000);
    const seconds = Math.floor((currentPlayTime % 60000) / 1000);
    return `${hours.toString().padStart(2, '0')}:${minutes.toString().padStart(2, '0')}:${seconds.toString().padStart(2, '0')}`;
  }

  getUsername(): string {
    return this.username;
  }

  getUserEmail(): string {
    const users = this.storage.getUsers();
    return users.find((u: User) => u.username === this.username)?.email ?? '';
  }
}
