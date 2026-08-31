export type ShopItemType = 'card' | 'splashArt' | 'cardBack' | 'pack';

export interface CartItem {
  id: string;
  name: string;
  imageUrl: string;
  type: ShopItemType;
  rarity: string;
  quantity: number;
  unitPrice: number;
}

const RARITY_PRICES: Record<string, number> = {
  'Common': 50,
  'Uncommon': 150,
  'Rare': 300,
  'Rare Holo': 500,
  'Rare Holo EX': 1200,
  'Rare Ace': 800,
  'Rare Ultra': 4000,
};

export function getPrice(rarity: string): number {
  return RARITY_PRICES[rarity] ?? 100;
}
