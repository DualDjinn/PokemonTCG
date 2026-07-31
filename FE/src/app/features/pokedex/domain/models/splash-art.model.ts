/**
 * Tipo de full-art:
 *  - `original`     → carta oficial del set (xy1-141 a xy1-146, EX Rare Ultra).
 *  - `alternative`  → arte alternativo custom (carpeta full-art-pokemons del BE).
 */
export type FullArtKind = 'original' | 'alternative';

export interface SplashArt {
  id: string;
  name: string;
  imageUrl: string;
  pokemonName: string;
  rarity: string;
  kind: FullArtKind;
}

export interface SplashArtFilters {
  name?: string;
  kind?: FullArtKind;
}
