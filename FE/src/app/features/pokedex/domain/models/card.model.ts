export interface CardImages {
  small: string;
  large: string;
}

export interface SetImages {
  symbol: string;
  logo: string;
}

export interface CardSet {
  id: string;
  name: string;
  series: string;
  printedTotal: number;
  total: number;
  ptcgoCode: string;
  releaseDate: string;
  images: SetImages;
}

export interface Attack {
  name: string;
  cost: string[];
  convertedEnergyCost: number;
  damage: string;
  text: string;
}

export interface Ability {
  name: string;
  text: string;
  type: string;
}

export interface Weakness {
  type: string;
  value: string;
}

export interface Resistance {
  type: string;
  value: string;
}

export interface Card {
  id: string;
  name: string;
  supertype: string;
  subtypes: string[];
  hp: string | null;
  types: string[] | null;
  evolvesTo: string[] | null;
  evolvesFrom: string | null;
  rules: string[] | null;
  attacks: Attack[] | null;
  abilities: Ability[] | null;
  weaknesses: Weakness[] | null;
  resistances: Resistance[] | null;
  retreatCost: string[] | null;
  convertedRetreatCost: number | null;
  number: string;
  rarity: string;
  gifUrl: string | null;
  images: CardImages;
  set: CardSet;
}

export interface CardFilters {
  name?: string;
  supertype?: string;
  type?: string;
  rarity?: string;
  subtype?: string;
}
