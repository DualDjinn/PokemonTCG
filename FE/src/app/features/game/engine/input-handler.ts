export class InputHandler {
  key = {
    w: { pressed: false },
    a: { pressed: false },
    s: { pressed: false },
    d: { pressed: false },
  };
  lastKey = '';
  private keydownHandler!: (e: KeyboardEvent) => void;
  private keyupHandler!: (e: KeyboardEvent) => void;

  setup() {
    this.keydownHandler = (e) => this.handleKeyDown(e);
    this.keyupHandler = (e) => this.handleKeyUp(e);
    window.addEventListener('keydown', this.keydownHandler);
    window.addEventListener('keyup', this.keyupHandler);
  }

  destroy() {
    window.removeEventListener('keydown', this.keydownHandler);
    window.removeEventListener('keyup', this.keyupHandler);
  }

  handleKeyDown(e: KeyboardEvent) {
    const mapped = this.mapKey(e.key);
    if (!mapped) return;
    this.key[mapped].pressed = true;
    this.lastKey = mapped;
  }

  handleKeyUp(e: KeyboardEvent) {
    const mapped = this.mapKey(e.key);
    if (!mapped) return;
    this.key[mapped].pressed = false;
  }

  /**
   * Maps both WASD letters and arrow keys to the four-direction model used by
   * the engine. Returns {@code null} for any other key so callers can ignore it.
   */
  private mapKey(rawKey: string): 'w' | 'a' | 's' | 'd' | null {
    switch (rawKey) {
      case 'w': case 'W': case 'ArrowUp': return 'w';
      case 'a': case 'A': case 'ArrowLeft': return 'a';
      case 's': case 'S': case 'ArrowDown': return 's';
      case 'd': case 'D': case 'ArrowRight': return 'd';
      default: return null;
    }
  }

  pressVirtualKey(dir: 'w' | 'a' | 's' | 'd'): void {
    this.key[dir].pressed = true;
    this.lastKey = dir;
  }

  releaseVirtualKey(dir: 'w' | 'a' | 's' | 'd'): void {
    this.key[dir].pressed = false;
  }

  releaseAllVirtualKeys(): void {
    this.key.w.pressed = false;
    this.key.a.pressed = false;
    this.key.s.pressed = false;
    this.key.d.pressed = false;
  }
}
