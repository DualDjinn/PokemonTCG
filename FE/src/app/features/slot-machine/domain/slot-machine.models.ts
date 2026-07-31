export interface WinPoint {
  x: number;
  y: number;
}

export interface ActiveWinLine {
  points: string;
  pts: WinPoint[];
  color: string;
}

export interface WinLineDetail {
  lineIndex: number;
  label: string;
  multiplier: number;
  symbolIdx: number;
  isJackpot: boolean;
}

export type MachineState = 'idle' | 'spinning';
export type MessageType = 'win' | 'lose' | 'jackpot' | 'none';
