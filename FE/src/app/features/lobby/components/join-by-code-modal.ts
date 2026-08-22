import { ChangeDetectionStrategy, Component, EventEmitter, Input, Output, inject } from '@angular/core';
import { MyModal } from '../../../shared/components/my-modal/my-modal';
import { MyInput } from '../../../shared/ui/my-input/my-input';
import { MyButton } from '../../../shared/ui/my-button/my-button';
import { MyText } from '../../../shared/ui/my-text/my-text';
import { MatchLobbyService } from '../services/match-lobby.service';
import { AuthService } from '../../game/data-access/services/auth.service';
import { ToastService } from '../../../shared/services/toast.service';
import { TranslateModule, TranslateService } from '@ngx-translate/core';
import { finalize } from 'rxjs';

@Component({
  selector: 'app-join-by-code-modal',
  standalone: true,
  imports: [MyModal, MyInput, MyButton, MyText, TranslateModule],
  template: `
    <my-modal
      [open]="open"
      [title]="'LOBBY.JOIN_BY_CODE_TITLE' | translate"
      (close)="close.emit()"
      width="26rem"
    >
      <div class="flex flex-col gap-4">
        @if (!showPassword) {
          <my-input
            [value]="code"
            [placeholder]="'LOBBY.CODE_ROOM_PLACEHOLDER' | translate"
            variant="modern"
            (valueChange)="code = $event.toUpperCase()"
            (keydown.enter)="onJoin()"
          ></my-input>

          @if (!activeDeckId()) {
            <my-text
              [text]="'LOBBY.NEED_DECK_TO_JOIN' | translate"
              tag="p"
              size="0.765rem"
              color="var(--neon-magenta)"
            ></my-text>
          }

          @if (error) {
            <my-text
              [text]="error"
              tag="p"
              size="0.765rem"
              color="var(--neon-magenta)"
            ></my-text>
          }

          <div class="flex gap-3 justify-end">
            <my-button
              [text]="'LOBBY.CANCEL' | translate"
              variant="modern"
              size="small"
              (clicked)="close.emit()"
              [disabled]="loading"
            ></my-button>
            <my-button
              [text]="'LOBBY.JOIN' | translate"
              variant="modern"
              size="small"
              (clicked)="onJoin()"
              [disabled]="loading || !code.trim() || !activeDeckId()"
            ></my-button>
          </div>
        } @else {
          <p class="font-share-tech text-[0.63rem] text-text-secondary mb-1">{{ 'LOBBY.PASSWORD_DESC' | translate }}</p>
          <my-input
            [value]="passwordValue"
            [placeholder]="'LOBBY.PASSWORD_PLACEHOLDER' | translate"
            type="password"
            variant="modern"
            (valueChange)="passwordValue = $event"
            (keydown.enter)="onConfirmPassword()"
          ></my-input>

          @if (error) {
            <my-text
              [text]="error"
              tag="p"
              size="0.765rem"
              color="var(--neon-magenta)"
            ></my-text>
          }

          <div class="flex gap-3 justify-end mt-2">
            <my-button
              [text]="'LOBBY.CANCEL' | translate"
              variant="modern"
              size="small"
              (clicked)="reset()"
              [disabled]="loading"
            ></my-button>
            <my-button
              [text]="'LOBBY.CONFIRM_UPPER' | translate"
              variant="modern"
              size="small"
              (clicked)="onConfirmPassword()"
              [disabled]="loading || !passwordValue.trim()"
            ></my-button>
          </div>
        }
      </div>
    </my-modal>
  `,
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class JoinByCodeModal {
  @Input() open = false;
  @Output() close = new EventEmitter<void>();
  @Output() matchJoined = new EventEmitter<string>();

  private readonly lobbyService = inject(MatchLobbyService);
  private readonly authService = inject(AuthService);
  private readonly toastService = inject(ToastService);
  private readonly translate = inject(TranslateService);

  code = '';
  showPassword = false;
  passwordValue = '';
  loading = false;
  error = '';

  readonly activeDeckId = () => this.authService.currentUser()?.activeDeckId ?? null;

  onJoin(): void {
    const deckId = this.activeDeckId();
    if (!deckId) return;

    const trimmed = this.code.trim();
    if (!trimmed) return;

    this.loading = true;
    this.error = '';

    this.lobbyService.joinByCode(trimmed, deckId)
      .pipe(finalize(() => this.loading = false))
      .subscribe({
        next: room => {
          this.toastService.show(this.translate.instant('LOBBY.JOINED_ROOM'), 'success');
          this.matchJoined.emit(room.matchId);
        },
        error: err => {
          if (err.status === 401) {
            this.showPassword = true;
            this.error = '';
          } else {
            this.error = this.translate.instant('LOBBY.JOIN_CODE_INVALID');
          }
        },
      });
  }

  onConfirmPassword(): void {
    const deckId = this.activeDeckId();
    if (!deckId) return;

    const trimmed = this.code.trim();
    if (!trimmed) return;

    this.loading = true;
    this.error = '';

    this.lobbyService.joinByCode(trimmed, deckId, this.passwordValue)
      .pipe(finalize(() => this.loading = false))
      .subscribe({
        next: room => {
          this.toastService.show(this.translate.instant('LOBBY.JOINED_ROOM'), 'success');
          this.reset();
          this.matchJoined.emit(room.matchId);
        },
        error: err => {
          if (err.status === 403) {
            this.error = this.translate.instant('LOBBY.TOAST_WRONG_PASSWORD');
          } else {
            this.error = this.translate.instant('LOBBY.JOIN_CODE_INVALID');
          }
        },
      });
  }

  reset(): void {
    this.code = '';
    this.showPassword = false;
    this.passwordValue = '';
    this.error = '';
    this.loading = false;
  }
}
