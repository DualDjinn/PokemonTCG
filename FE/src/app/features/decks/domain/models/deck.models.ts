export interface CardSummary {
  id: string;
  name: string;
  supertype: string;
  imageUrl: string;
  imageUrlLarge?: string;
  rarity: string;
}

export interface DeckListItem {
  id: string;
  name: string;
  cardCount: number;
  valid: boolean;
  favorite: boolean;
  validationMessage: string | null;
}

export interface DeckDetail {
  id: string;
  name: string;
  cardCount: number;
  cards: CardSummary[];
  valid: boolean;
  favorite: boolean;
  validationMessage: string | null;
}

export interface CreateDeckRequest {
  name: string;
}

export interface UpdateDeckRequest {
  cardIds: string[];
}

export interface DeckApiDTO {
  id?: string;
  name?: string;
  cardCount?: number;
  cards?: CardSummaryApiDTO[];
  valid?: boolean;
  isValid?: boolean;
  favorite?: boolean;
  message?: string;
  error?: string;
  errors?: string[];
}

export interface CardSummaryApiDTO {
  id?: string;
  name?: string;
  supertype?: string;
  imageUrl?: string;
  images?: {
    small?: string;
    large?: string;
  };
  rarity?: string;
}
