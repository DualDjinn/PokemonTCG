/**
 * Tipos del dominio de la tienda de sobres. Espejan los DTOs del backend
 * (PackInfoDTO, OpenedPackResponse, CardSummaryDTO).
 */

export type PackType = 'BRONZE' | 'SILVER' | 'GOLD' | 'DIAMOND';

export type RarityBucket = 'HIGH' | 'MEDIUM' | 'LOW';

export interface PackInfo {
  type: PackType;
  price: number;
  cardsPerPack: number;
  /** Probabilidades por bucket. Claves: "HIGH" | "MEDIUM" | "LOW". */
  probabilities: Record<RarityBucket, number>;
}

export interface PackCard {
  id: string;
  name: string;
  supertype: string;
  imageUrl: string;
  imageUrlLarge?: string;
  rarity: string | null;
}

export interface OpenedPack {
  packType: PackType;
  cards: PackCard[];
  remainingCoins: number;
}
