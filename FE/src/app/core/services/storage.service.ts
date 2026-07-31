import { Injectable } from '@angular/core';
import { User, SavedPosition, SavedPositionsMap } from '../../features/game/domain/models/user.model';
import { Character } from '../../features/game/domain/models/character.model';

type CharacterMap = Record<string, Character>;

@Injectable({ providedIn: 'root' })
export class StorageService {
  private readonly KEYS = {
    USERS: 'users',
    CHARACTERS: 'characters',
    SAVED_POSITIONS: 'savedPositions',
    LAST_USER: 'lastUser',
  } as const;

  // ── Users ──────────────────────────────────────────────────────────────────

  getUsers(): User[] {
    return this.parse<User[]>(this.KEYS.USERS, []);
  }

  setUsers(users: User[]): void {
    localStorage.setItem(this.KEYS.USERS, JSON.stringify(users));
  }

  // ── Characters ─────────────────────────────────────────────────────────────

  getCharacters(): CharacterMap {
    return this.parse<CharacterMap>(this.KEYS.CHARACTERS, {});
  }

  getCharacter(username: string): Character | null {
    return this.getCharacters()[username] ?? null;
  }

  setCharacter(username: string, character: Character): void {
    const chars = this.getCharacters();
    chars[username] = character;
    localStorage.setItem(this.KEYS.CHARACTERS, JSON.stringify(chars));
  }

  // ── Saved positions ────────────────────────────────────────────────────────

  getSavedPositions(): SavedPositionsMap {
    return this.parse<SavedPositionsMap>(this.KEYS.SAVED_POSITIONS, {});
  }

  getSavedPosition(username: string): SavedPosition | null {
    return this.getSavedPositions()[username] ?? null;
  }

  setSavedPosition(username: string, position: SavedPosition): void {
    const all = this.getSavedPositions();
    all[username] = position;
    localStorage.setItem(this.KEYS.SAVED_POSITIONS, JSON.stringify(all));
  }

  clearSavedPosition(username: string): void {
    const all = this.getSavedPositions();
    delete all[username];
    localStorage.setItem(this.KEYS.SAVED_POSITIONS, JSON.stringify(all));
  }

  // ── Last user ──────────────────────────────────────────────────────────────

  getLastUser(): string | null {
    return localStorage.getItem(this.KEYS.LAST_USER);
  }

  setLastUser(username: string): void {
    localStorage.setItem(this.KEYS.LAST_USER, username);
  }

  // ── Helpers ────────────────────────────────────────────────────────────────

  private parse<T>(key: string, fallback: T): T {
    try {
      const raw = localStorage.getItem(key);
      return raw ? (JSON.parse(raw) as T) : fallback;
    } catch {
      return fallback;
    }
  }
}
