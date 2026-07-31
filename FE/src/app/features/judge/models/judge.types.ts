export enum JudgeQueryCategory {
  WHY_CANT_I = 'WHY_CANT_I',
  EXPLAIN_DAMAGE = 'EXPLAIN_DAMAGE',
  EXPLAIN_STATUS = 'EXPLAIN_STATUS',
  VICTORY_CONDITIONS = 'VICTORY_CONDITIONS',
  CURRENT_RESTRICTIONS = 'CURRENT_RESTRICTIONS',
}

export enum GameActionType {
  PLAY_BASIC = 'PLAY_BASIC',
  EVOLVE = 'EVOLVE',
  ATTACH_ENERGY = 'ATTACH_ENERGY',
  PLAY_ITEM = 'PLAY_ITEM',
  PLAY_SUPPORTER = 'PLAY_SUPPORTER',
  RETREAT = 'RETREAT',
  ADVANCE_TO_ATTACK = 'ADVANCE_TO_ATTACK',
  ATTACK = 'ATTACK',
  END_TURN = 'END_TURN',
}

export interface JudgeResponse {
  queryId: string;
  category: JudgeQueryCategory;
  summary: string;
  reasons: string[];
  ruleReferences: string[];
  suggestion: string | null;
  hasActiveRestrictions: boolean;
  turnNumber: number;
  lang: 'es' | 'en';
}

export interface JudgeQueryParams {
  category: JudgeQueryCategory;
  targetAction?: GameActionType;
  targetCardId?: string;
  targetPokemonId?: string;
  targetTurnNumber?: number;
  lang?: 'es' | 'en';
}

export type JudgeMewState = 'idle' | 'thinking' | 'approved' | 'rejected' | 'teleport' | 'celebrate';
