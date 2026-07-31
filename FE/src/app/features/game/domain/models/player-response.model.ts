export interface PlayerResponse {
  id: string;
  username: string;
  trainerName: string;
  email: string;
  dateOfBirth: string;
  level: number;
  experience: number;
  xpForNextLevel: number;
  xpIntoCurrentLevel: number;
  coins: number;
  activeDeckId: string | null;
  character?: string;
  color?: string;
  collectionCardIds: string[];
  collectionQuantities: Record<string, number>;
  decks: DeckSummary[];
}

export interface DeckSummary {
  id: string;
  name: string;
  cardCount: number;
}
