import { Component, OnInit, inject, ChangeDetectionStrategy, ChangeDetectorRef } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { HttpErrorResponse } from '@angular/common/http';
import { AuthService } from '../../data-access/services/auth.service';
import { GameStateService } from '../../data-access/services/game-state.service';
import { NgClass } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { TranslateModule, TranslateService } from '@ngx-translate/core';
import { ToastService } from '../../../../shared/services/toast.service';
import { AudioSfxChannel } from '../../../../core/services/audio-sfx-channel.service';
import { switchMap } from 'rxjs/operators';
import { FullPageDirective } from '../../../../shared/directives';

interface CharOption {
  id: string;
  name: string;
  portrait: string;
  hasColor: boolean;
}

@Component({
  selector: 'app-character-creation-page',
  standalone: true,
  imports: [ FormsModule, TranslateModule, NgClass],
  hostDirectives: [FullPageDirective],
  templateUrl: './character-creation-page.html',
  styleUrl: './character-creation-page.css',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class CharacterCreationPage implements OnInit {
  private route = inject(ActivatedRoute);
  private router = inject(Router);
  private authService = inject(AuthService);
  private gameState = inject(GameStateService);
  private cdr = inject(ChangeDetectorRef);
  private toastService = inject(ToastService);
  private translateService = inject(TranslateService);
  private sfx = inject(AudioSfxChannel);

  username = '';
  selectedCharacter = 'red';
  selectedColor = 'rojo';
  characterName = '';
  isLoading = false;

  readonly COLOR_CHARS = ['red', 'green', 'ruby', 'sapphire'];

  readonly characters: CharOption[] = [
    { id: 'red', name: 'Red', portrait: 'assets/img/portrait/RedPortrait.png', hasColor: true },
    { id: 'green', name: 'Green', portrait: 'assets/img/portrait/GreenPortrait.png', hasColor: true },
    { id: 'ruby', name: 'Ruby', portrait: 'assets/img/portrait/RubyPortrait.png', hasColor: true },
    { id: 'sapphire', name: 'Sapphire', portrait: 'assets/img/portrait/SapphirePortrait.png', hasColor: true },
    { id: 'gary', name: 'Gary', portrait: 'assets/img/portrait/BluePortrait.png', hasColor: false },
    { id: 'prima', name: 'Prima', portrait: 'assets/img/portrait/LoreleiPortrait.png', hasColor: false },
  ];

  ngOnInit() {
    this.route.queryParams.subscribe(params => {
      this.username = params['user'];
      if (!this.username) {
        this.router.navigate(['/login']);
      }
    });
  }

  selectCharacter(id: string) {
    this.sfx.play('Click.mp3');
    this.selectedCharacter = id;
  }

  selectColor(color: string) {
    this.sfx.play('Click.mp3');
    this.selectedColor = color;
  }

  get showColorSelector(): boolean {
    return this.COLOR_CHARS.includes(this.selectedCharacter);
  }

  get nameLength(): number {
    return this.characterName.length;
  }

  onSubmit() {
    const name = this.characterName.trim();
    if (!name) {
      this.toastService.show(this.translateService.instant('TOAST.CHARACTER_CREATION.NAME_REQUIRED'), 'error');
      return;
    }
    if (name.length < 2) {
      this.toastService.show(this.translateService.instant('TOAST.CHARACTER_CREATION.NAME_TOO_SHORT'), 'error');
      return;
    }

    this.sfx.play('Click.mp3');
    this.isLoading = true;
    this.cdr.markForCheck();

    const color = this.showColorSelector ? this.selectedColor : null;

    this.authService.updateTrainerName(name).pipe(
      switchMap(() => this.authService.updateCharacter(this.selectedCharacter, color)),
    ).subscribe({
      next: () => {
        const charData: any = { name, character: this.selectedCharacter };
        if (this.showColorSelector) {
          charData.color = this.selectedColor;
        }

        this.gameState.saveCharacter(this.username, charData);
        this.isLoading = false;
        this.toastService.show(this.translateService.instant('TOAST.CHARACTER_CREATION.SUCCESS'), 'success');
        this.sfx.play('OperacionExitosa.mp3');
        this.cdr.markForCheck();

        setTimeout(() => {
          this.router.navigate(['/juego', 'tutorial'], { queryParams: { user: this.username, col: 33, row: 29 } });
        }, 500);
      },
      error: (err: HttpErrorResponse) => {
        this.isLoading = false;
        const message = err.error?.message || this.translateService.instant('TOAST.CHARACTER_CREATION.ERROR');
        this.toastService.show(message, 'error');
        this.cdr.markForCheck();
      }
    });
  }
}
