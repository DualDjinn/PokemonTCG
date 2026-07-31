export type LocationKey = 'ciudadPrincipal' | 'lan' | 'preLan' | 'secret' | 'tutorial';

export interface ZoneData {
  boundaries: import('../../engine/classes').Boundary[];
  destination: string;
  spawnTileDelta: { dcol: number; drow: number } | null;
}
