export interface Position {
  x: number;
  y: number;
}

export interface SpriteFrames {
  max: number;
  hold: number;
  val: number;
  elapsed: number;
}

export class Sprite {
  position: Position;
  cityMap: HTMLImageElement;
  frames: SpriteFrames;
  scale: number;
  moving: boolean = false;
  sprites?: { down: HTMLImageElement; up: HTMLImageElement; left: HTMLImageElement; right: HTMLImageElement };
  frameWidth: number = 0;
  frameHeight: number = 0;
  width: number = 0;
  height: number = 0;

  constructor({position, cityMap, frames = { max: 1, hold: 10 }, scale = 1, sprites}: {
    position: Position;
    cityMap: HTMLImageElement;
    frames?: { max: number; hold: number };
    scale?: number;
    sprites?: { down: HTMLImageElement; up: HTMLImageElement; left: HTMLImageElement; right: HTMLImageElement };
  }) {
    this.position = position;
    this.cityMap = cityMap;
    this.frames = { ...frames, val: 0, elapsed: 0 };
    this.scale = scale;
    this.sprites = sprites;

    this.cityMap.onload = () => this.calculateDimensions();
    if (this.cityMap.complete) this.calculateDimensions();
  }

  private calculateDimensions() {
    this.frameWidth = this.cityMap.width / this.frames.max;
    this.frameHeight = this.cityMap.height;
    this.width = this.frameWidth * this.scale;
    this.height = this.frameHeight * this.scale;
  }

  setSprite(image: HTMLImageElement) {
    if (this.cityMap === image) return;
    this.cityMap = image;
    if (this.cityMap.complete) {
      this.calculateDimensions();
    } else {
      this.cityMap.onload = () => this.calculateDimensions();
    }
  }

  draw(ctx: CanvasRenderingContext2D) {
    if (!this.frameWidth) return;

    ctx.drawImage(
      this.cityMap,
      this.frames.val * this.frameWidth,
      0,
      this.frameWidth,
      this.frameHeight,
      this.position.x,
      this.position.y,
      this.width,
      this.height
    );

    if (!this.moving) return;
    if (this.frames.max > 1) this.frames.elapsed++;

    if (this.frames.elapsed % this.frames.hold === 0) {
      this.frames.val = this.frames.val < this.frames.max - 1 ? this.frames.val + 1 : 0;
    }
  }
}

export class Boundary {
  static width = 28;
  static height = 28;
  position: Position;
  width: number;
  height: number;
  color: string;

  constructor({position, scale = 1, color = 'rgba(255, 0, 0, 0.0)'}: { position: Position; scale?: number; color?: string }) {
    this.position = position;
    this.width = Boundary.width * scale;
    this.height = Boundary.height * scale;
    this.color = color;
  }

  draw(ctx: CanvasRenderingContext2D) {
    ctx.fillStyle = this.color;
    ctx.fillRect(this.position.x, this.position.y, this.width, this.height);
  }
}
