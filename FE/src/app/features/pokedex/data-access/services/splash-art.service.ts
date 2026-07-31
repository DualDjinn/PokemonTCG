import { Injectable } from '@angular/core';
import { Observable, of } from 'rxjs';
import { SplashArt, SplashArtFilters } from '../../domain/models/splash-art.model';

/**
 * Originales del set XY1 — cartas EX Rare Ultra (ids 141..146).
 * Imágenes servidas por el CDN oficial de pokemontcg.io.
 */
const ORIGINALS: SplashArt[] = [
  {
    id: 'xy1-141',
    name: 'Venusaur-EX (Full Art)',
    pokemonName: 'Venusaur',
    rarity: 'Rare Ultra',
    imageUrl: 'https://images.pokemontcg.io/xy1/141_hires.png',
    kind: 'original',
  },
  {
    id: 'xy1-142',
    name: 'Blastoise-EX (Full Art)',
    pokemonName: 'Blastoise',
    rarity: 'Rare Ultra',
    imageUrl: 'https://images.pokemontcg.io/xy1/142_hires.png',
    kind: 'original',
  },
  {
    id: 'xy1-143',
    name: 'Emolga-EX (Full Art)',
    pokemonName: 'Emolga',
    rarity: 'Rare Ultra',
    imageUrl: 'https://images.pokemontcg.io/xy1/143_hires.png',
    kind: 'original',
  },
  {
    id: 'xy1-144',
    name: 'Yveltal-EX (Full Art)',
    pokemonName: 'Yveltal',
    rarity: 'Rare Ultra',
    imageUrl: 'https://images.pokemontcg.io/xy1/144_hires.png',
    kind: 'original',
  },
  {
    id: 'xy1-145',
    name: 'Skarmory-EX (Full Art)',
    pokemonName: 'Skarmory',
    rarity: 'Rare Ultra',
    imageUrl: 'https://images.pokemontcg.io/xy1/145_hires.png',
    kind: 'original',
  },
  {
    id: 'xy1-146',
    name: 'Xerneas-EX (Full Art)',
    pokemonName: 'Xerneas',
    rarity: 'Rare Ultra',
    imageUrl: 'https://images.pokemontcg.io/xy1/146_hires.png',
    kind: 'original',
  },
];

/**
 * Alternativos — arte custom servido por el backend en /full-art-pokemon/**
 * (mapeado vía WebConfig.addResourceHandlers).
 */
const ALTERNATIVES: SplashArt[] = [
  {
    id: 'fa-147',
    name: 'Venusaur Alt',
    pokemonName: 'Venusaur',
    rarity: 'Rare Ultra',
    imageUrl: '/full-art-pokemon/147-146-venusaur.png',
    kind: 'alternative',
  },
  {
    id: 'fa-148',
    name: 'Venusaur Alt',
    pokemonName: 'Venusaur',
    rarity: 'Rare Ultra',
    imageUrl: '/full-art-pokemon/148-146-venusaur.png',
    kind: 'alternative',
  },
  {
    id: 'fa-149',
    name: 'Blastoise Alt',
    pokemonName: 'Blastoise',
    rarity: 'Rare Ultra',
    imageUrl: '/full-art-pokemon/149-146-blastoise.png',
    kind: 'alternative',
  },
  {
    id: 'fa-150',
    name: 'Blastoise Alt',
    pokemonName: 'Blastoise',
    rarity: 'Rare Ultra',
    imageUrl: '/full-art-pokemon/150-146-blastoise.png',
    kind: 'alternative',
  },
  {
    id: 'fa-151',
    name: 'Emolga Alt',
    pokemonName: 'Emolga',
    rarity: 'Rare Ultra',
    imageUrl: '/full-art-pokemon/151-146-emolga.png',
    kind: 'alternative',
  },
  {
    id: 'fa-152',
    name: 'Yveltal Alt',
    pokemonName: 'Yveltal',
    rarity: 'Rare Ultra',
    imageUrl: '/full-art-pokemon/152-146-yveltal.png',
    kind: 'alternative',
  },
  {
    id: 'fa-153',
    name: 'Skarmory Alt',
    pokemonName: 'Skarmory',
    rarity: 'Rare Ultra',
    imageUrl: '/full-art-pokemon/153-146-skarmory.png',
    kind: 'alternative',
  },
  {
    id: 'fa-154',
    name: 'Xerneas Alt',
    pokemonName: 'Xerneas',
    rarity: 'Rare Ultra',
    imageUrl: '/full-art-pokemon/154-146-xerneas.png',
    kind: 'alternative',
  },
];

const ALL_FULL_ARTS: SplashArt[] = [...ORIGINALS, ...ALTERNATIVES];

@Injectable({ providedIn: 'root' })
export class SplashArtService {
  getSplashArts(filters?: SplashArtFilters): Observable<SplashArt[]> {
    let results = ALL_FULL_ARTS;

    if (filters?.kind) {
      results = results.filter(sa => sa.kind === filters.kind);
    }

    if (filters?.name?.trim()) {
      const query = filters.name.trim().toLowerCase();
      results = results.filter(sa =>
        sa.name.toLowerCase().includes(query) ||
        sa.pokemonName.toLowerCase().includes(query)
      );
    }

    return of(results);
  }
}
