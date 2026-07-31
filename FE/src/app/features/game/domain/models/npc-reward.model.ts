/**
 * Espejo de los DTOs de sidequests del backend (`/api/quests`).
 */

/** Campos de CardSummaryDTO que usa el front para la revelación. */
export interface NpcRewardCard {
  id: string;
  name: string;
  supertype: string;
  imageUrl: string;
  imageUrlLarge: string;
  rarity: string | null;
}

export interface NpcRewardClaimResponse {
  npcId: string;
  /** true si la recompensa ya se había reclamado antes: no animar. */
  alreadyClaimed: boolean;
  /** Carta otorgada; null cuando alreadyClaimed es true. */
  card: NpcRewardCard | null;
}
