import { Component, inject, ChangeDetectionStrategy, ChangeDetectorRef } from '@angular/core';
import { Router } from '@angular/router';
import { HttpErrorResponse } from '@angular/common/http';
import { FormsModule } from '@angular/forms';
import { TranslateModule, TranslateService } from '@ngx-translate/core';
import { AuthService } from '../../data-access/services/auth.service';
import { ToastService } from '../../../../shared/services/toast.service';
import { AudioSfxChannel } from '../../../../core/services/audio-sfx-channel.service';
import { FullPageDirective } from '../../../../shared/directives';

@Component({
  selector: 'app-forgot-password-page',
  standalone: true,
  imports: [FormsModule, TranslateModule],
  hostDirectives: [FullPageDirective],
  templateUrl: './forgot-password-page.html',
  styleUrl: './forgot-password-page.css',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class ForgotPasswordPage {
  private router = inject(Router);
  private authService = inject(AuthService);
  private toastService = inject(ToastService);
  private translateService = inject(TranslateService);
  private cdr = inject(ChangeDetectorRef);
  private sfx = inject(AudioSfxChannel);

  email = '';
  isLoading = false;
  submitted = false;
  isMuted = true;

  toggleMute() {
    this.isMuted = !this.isMuted;
    const video = document.getElementById('fp-video') as HTMLVideoElement;
    if (video) {
      video.muted = this.isMuted;
      if (!this.isMuted && video.paused) {
        video.play().catch(() => {});
      }
    }
    localStorage.setItem('musicMuted', String(this.isMuted));
    this.sfx.play('Mute.mp3');
    this.cdr.markForCheck();
  }

  onSubmit() {
    const email = this.email.trim();
    if (!email) {
      this.toastService.show(this.translateService.instant('TOAST.FORGOT_PASSWORD.EMAIL_REQUIRED'), 'error');
      return;
    }
    if (!this.authService.isEmailValid(email)) {
      this.toastService.show(this.translateService.instant('TOAST.FORGOT_PASSWORD.INVALID_EMAIL'), 'error');
      return;
    }

    this.isLoading = true;
    this.cdr.markForCheck();

    this.authService.forgotPassword(email).subscribe({
      next: () => {
        this.submitted = true;
        this.isLoading = false;
        this.cdr.markForCheck();
      },
      error: (_err: HttpErrorResponse) => {
        this.submitted = true;
        this.isLoading = false;
        this.cdr.markForCheck();
      }
    });
  }

  goToLogin() {
    this.router.navigate(['/login']);
  }
}
