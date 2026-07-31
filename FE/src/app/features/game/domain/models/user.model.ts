export interface User {
  username: string;
  email: string;
  password: string;
}

export interface SavedPosition {
  page: string;
  location: string;
  spawnTile: { col: number; row: number };
  playTime: number;
  facing?: 'up' | 'down' | 'left' | 'right';
}

export interface SavedPositionsMap {
  [username: string]: SavedPosition;
}
