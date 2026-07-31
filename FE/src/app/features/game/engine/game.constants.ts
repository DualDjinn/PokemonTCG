/** Shared numeric constants for the game engine. Change here → applies everywhere. */
export const GAME = {
  /** World-to-screen scale factor applied to all sprites and maps. */
  SCALE: 1.5,
  /** Pixels the world moves per input frame. */
  STEP: 6,
  /** Horizontal divisor used to center the player on canvas (canvas.width / PLAYER_X_RATIO). */
  PLAYER_X_RATIO: 2.8,
  /** Vertical divisor used to center the player on canvas (canvas.height / PLAYER_Y_RATIO). */
  PLAYER_Y_RATIO: 1.6,
  /** Number of animation frames in the player sprite sheet. */
  SPRITE_FRAMES: 4,
  /** How many render frames each sprite frame is held before advancing. */
  SPRITE_HOLD: 10,
} as const;
