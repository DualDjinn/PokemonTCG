import { Injectable, inject } from '@angular/core';
import { Character } from '../../domain/models/character.model';
import { SavedPosition } from '../../domain/models/user.model';
import { StorageService } from '../../../../core/services/storage.service';

@Injectable({ providedIn: 'root' })
export class GameStateService {
  private storage = inject(StorageService);

  getCharacter(username: string): Character | null {
    return this.storage.getCharacter(username);
  }

  saveCharacter(username: string, character: Character): void {
    this.storage.setCharacter(username, character);
  }

  getSavedPosition(username: string): SavedPosition | null {
    return this.storage.getSavedPosition(username);
  }

  savePosition(username: string, position: SavedPosition): void {
    this.storage.setSavedPosition(username, position);
  }

  getSpriteKey(username: string): string {
    const char = this.getCharacter(username);
    if (!char) return 'red_rojo';
    let key = char.character;
    if (char.color) key += '_' + char.color;
    return key;
  }

  clearSavedPosition(username: string): void {
    this.storage.clearSavedPosition(username);
  }

  hasCharacter(username: string): boolean {
    return this.getCharacter(username) !== null;
  }
}
