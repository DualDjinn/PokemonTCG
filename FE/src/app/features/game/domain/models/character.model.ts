export interface Character {
  name: string;
  character: string;
  color?: string;
}

export interface CharacterSpriteMap {
  [key: string]: {
    down: string;
    up: string;
    left: string;
    right: string;
  };
}
