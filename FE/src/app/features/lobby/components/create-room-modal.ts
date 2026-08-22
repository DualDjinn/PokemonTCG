import { ChangeDetectionStrategy, Component, EventEmitter, Input, Output, inject } from '@angular/core';
import { TranslateModule, TranslateService } from '@ngx-translate/core';
import { MyModal } from '../../../shared/components/my-modal/my-modal';
import { MyInput } from '../../../shared/ui/my-input/my-input';
import { MyButton } from '../../../shared/ui/my-button/my-button';
import { MyCheckbox } from '../../../shared/ui/my-checkbox/my-checkbox';
import { MyText } from '../../../shared/ui/my-text/my-text';
import { AuthService } from '../../game/data-access/services/auth.service';
import { MatchLobbyService, CreateRoomRequest, LobbyRoom } from '../services/match-lobby.service';
import { ToastService } from '../../../shared/services/toast.service';
import { AudioSfxChannel } from '../../../core/services/audio-sfx-channel.service';
import { finalize } from 'rxjs';

@Component({
  selector: 'app-create-room-modal',
  standalone: true,
  imports: [TranslateModule, MyModal, MyInput, MyButton, MyCheckbox, MyText],
  template: `
    <my-modal
      [open]="open"
      [title]="'CREATE_ROOM_MODAL.TITLE' | translate"
      variant="pixel"
      bodyPadding="p-4 pt-2 pb-2"
      (close)="close.emit()"
      width="28rem"
    >
      <div class="flex flex-col gap-4">
        <my-input
          [value]="roomName"
          [placeholder]="'CREATE_ROOM_MODAL.ROOM_NAME' | translate"
          variant="modern"
          (valueChange)="roomName = $event"
          (keydown.enter)="onCreate()"
        ></my-input>

        <my-input
          [value]="description"
          [placeholder]="'CREATE_ROOM_MODAL.DESCRIPTION' | translate"
          variant="modern"
          (valueChange)="description = $event"
          (keydown.enter)="onCreate()"
        ></my-input>

        <div class="flex items-center gap-3">
          <my-checkbox
            [checked]="isPrivate"
            [label]="'CREATE_ROOM_MODAL.PRIVATE' | translate"
            variant="pixel"
            (checkedChange)="isPrivate = $event"
          ></my-checkbox>
        </div>

        @if (isPrivate) {
          <my-input
            [value]="password"
            [placeholder]="'CREATE_ROOM_MODAL.PASSWORD' | translate"
            type="password"
            variant="modern"
            (valueChange)="password = $event"
            (keydown.enter)="onCreate()"
          ></my-input>
        }

        <div class="flex items-center gap-3">
          <my-checkbox
            [checked]="showInList"
            [label]="'CREATE_ROOM_MODAL.SHOW_IN_LIST' | translate"
            variant="pixel"
            (checkedChange)="showInList = $event"
          ></my-checkbox>
        </div>

        <div class="flex items-center gap-3">
          <my-checkbox
            [checked]="allowSpectators"
            [label]="'CREATE_ROOM_MODAL.ALLOW_SPECTATORS' | translate"
            variant="pixel"
            (checkedChange)="allowSpectators = $event"
          ></my-checkbox>
        </div>

        <my-text
          [text]="'CREATE_ROOM_MODAL.ACTIVE_DECK' | translate:{name: activeDeckName()}"
          tag="p"
          size="0.765rem"
          color="var(--text-secondary)"
        ></my-text>

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
            [text]="'CREATE_ROOM_MODAL.CANCEL' | translate"
            variant="modern"
            size="small"
            (clicked)="close.emit()"
            [disabled]="loading"
          ></my-button>
          <my-button
            [text]="'CREATE_ROOM_MODAL.CREATE' | translate"
            variant="modern"
            size="small"
            (clicked)="onCreate()"
            [disabled]="loading || !activeDeckId()"
          ></my-button>
        </div>
      </div>
    </my-modal>
  `,
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class CreateRoomModal {
  @Input() open = false;
  @Output() close = new EventEmitter<void>();
  @Output() roomCreated = new EventEmitter<LobbyRoom>();

  private readonly lobbyService = inject(MatchLobbyService);
  private readonly authService = inject(AuthService);
  private readonly toastService = inject(ToastService);
  private readonly sfx = inject(AudioSfxChannel);
  private readonly translate = inject(TranslateService);

  roomName = '';
  description = '';
  isPrivate = false;
  password = '';
  showInList = true;
  allowSpectators = false;
  loading = false;
  error = '';

  readonly activeDeckId = () => this.authService.currentUser()?.activeDeckId ?? null;
  readonly activeDeckName = () => {
    const user = this.authService.currentUser();
    const deck = user?.decks?.find(d => d.id === user.activeDeckId);
    return deck?.name ?? this.translate.instant('LOBBY.NO_DECK');
  };

  onCreate(): void {
    this.sfx.play('Click.mp3');
    const deckId = this.activeDeckId();
    if (!deckId) {
      this.error = this.translate.instant('CREATE_ROOM_MODAL.ERROR_NO_DECK');
      return;
    }

    this.loading = true;
    this.error = '';

    const request: CreateRoomRequest = {
      deckId,
      roomName: this.roomName.trim(),
      description: this.description.trim(),
      isPrivate: this.isPrivate,
      showInList: this.showInList,
      password: this.password,
      allowSpectators: this.allowSpectators,
    };

    this.lobbyService.createRoom(request)
      .pipe(finalize(() => this.loading = false))
      .subscribe({
        next: room => {
          this.toastService.show(this.translate.instant('CREATE_ROOM_MODAL.TOAST_CREATED'), 'success');
          this.reset();
          this.roomCreated.emit(room);
        },
        error: () => {
          this.error = this.translate.instant('CREATE_ROOM_MODAL.ERROR_CREATE');
        },
      });
  }

  private reset(): void {
    this.roomName = '';
    this.description = '';
    this.isPrivate = false;
    this.password = '';
    this.showInList = true;
    this.allowSpectators = false;
    this.error = '';
  }
}
