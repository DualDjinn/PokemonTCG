import { Component, AfterViewInit, OnDestroy, inject, ChangeDetectionStrategy, ChangeDetectorRef } from '@angular/core';
import { Router } from '@angular/router';
import { HttpErrorResponse } from '@angular/common/http';
import { AuthService } from '../../data-access/services/auth.service';
import { AuthTransitionService } from '../../data-access/services/auth-transition.service';
import { GameStateService } from '../../data-access/services/game-state.service';
import { FormsModule } from '@angular/forms';
import { TranslateModule, TranslateService } from '@ngx-translate/core';
import { ToastService } from '../../../../shared/services/toast.service';
import { AudioSfxChannel } from '../../../../core/services/audio-sfx-channel.service';
import { AudioService } from '../../data-access/services/audio.service';
import { FullPageDirective } from '../../../../shared/directives';

@Component({
  selector: 'app-login-page',
  standalone: true,
  imports: [FormsModule, TranslateModule],
  hostDirectives: [FullPageDirective],
  templateUrl: './login-page.html',
  styleUrl: './login-page.css',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class LoginPage implements AfterViewInit, OnDestroy {
  private router = inject(Router);
  private authService = inject(AuthService);
  private gameState = inject(GameStateService);
  private cdr = inject(ChangeDetectorRef);
  private toastService = inject(ToastService);
  private translateService = inject(TranslateService);
  private audioService = inject(AudioService);
  private transitionService = inject(AuthTransitionService);

  private readonly MOVE_TO_CENTER_MS = 1000;
  private readonly LOADING_AT_CENTER_MS = 1500;

  isAuthTransitioning = false;
  private sfx = inject(AudioSfxChannel);

  currentPage: 'intro' | 'login' = 'intro';
  activeTab: 'login' | 'register' = 'login';
  isMuted = localStorage.getItem('musicMuted') === 'true';
  isRotating = false;
  slideLeft = false;
  showIntroHint = false;
  keepSession = localStorage.getItem('keepSession') === 'true';

  loginUser = '';
  loginPass = '';
  regUser = '';
  regEmail = '';
  regPass = '';
  regPassVisible = false;
  isLoading = false;
  showPokeballPulse = false;

  constructor() {}

  private progressInterval: any = null;
  private progressStartTime: number | null = null;
  private readonly PROGRESS_DURATION = 800;
  private readonly RING_CIRCUMFERENCE = 283;

  ngAfterViewInit() {
    this.tryPlayVideo();
    if (this.keepSession) {
      this.loginUser = localStorage.getItem('savedUser') || '';
      this.loginPass = localStorage.getItem('savedPass') || '';
      this.cdr.markForCheck();
    }
    this.initGoogleSignIn();
  }

  ngOnDestroy() {
    this.clearProgress();
  }

  private initGoogleSignIn(retries = 8) {
    const google = (window as any)['google'];
    if (google?.accounts?.id) {
      google.accounts.id.initialize({
        client_id: '377262968636-c522jeon23bb74arig40gbgghlnm6q9l.apps.googleusercontent.com',
        callback: (resp: { credential: string }) => this.onGoogleCallback(resp),
        auto_select: false,
        cancel_on_tap_outside: true,
      });
      this.renderGoogleButton('signin_with');
    } else if (retries > 0) {
      setTimeout(() => this.initGoogleSignIn(retries - 1), 400);
    }
  }

  private renderGoogleButton(text: 'signin_with' | 'signup_with') {
    const google = (window as any)['google'];
    const btnEl = document.getElementById('google-signin-btn');
    if (btnEl && google?.accounts?.id) {
      google.accounts.id.renderButton(btnEl, {
        theme: 'filled_black',
        size: 'large',
        text: text,
        shape: 'pill',
        width: 280,
      });
    }
  }

  onGoogleCallback(resp: { credential: string }) {
    this.isLoading = true;
    this.cdr.markForCheck();
    this.startTransition();
    const startedAt = Date.now();

    this.authService.googleLogin(resp.credential).subscribe({
      next: (user) => {
        const minTotal = this.MOVE_TO_CENTER_MS + this.LOADING_AT_CENTER_MS;
        const remaining = Math.max(0, minTotal - (Date.now() - startedAt));
        this.toastService.show(this.translateService.instant('TOAST.LOGIN.GOOGLE_SUCCESS'), 'success');
        this.sfx.play('OperacionExitosa.mp3');
        this.cdr.markForCheck();
        setTimeout(() => {
          this.isLoading = false;
          this.cdr.markForCheck();
          this.sfx.play('BolaAcercandose.mp3');
          this.transitionService.expand(() => {
            if (this.gameState.hasCharacter(user.username)) {
              const saved = this.gameState.getSavedPosition(user.username);
              if (saved) {
                this.router.navigate(['/juego', saved.location], { queryParams: { user: user.username } });
              } else {
                this.router.navigate(['/juego', 'ciudadPrincipal'], { queryParams: { user: user.username } });
              }
            } else {
              this.router.navigate(['/crear-personaje'], { queryParams: { user: user.username } });
            }
          });
        }, remaining);
      },
      error: (err: HttpErrorResponse) => {
        this.transitionService.dismiss();
        this.isAuthTransitioning = false;
        this.isLoading = false;
        const message = err.error?.message || this.translateService.instant('TOAST.LOGIN.GOOGLE_ERROR');
        this.toastService.show(message, 'error');
        this.cdr.markForCheck();
      }
    });
  }

  private tryPlayVideo() {
    const video = document.getElementById('main-video') as HTMLVideoElement;
    if (video) {
      video.muted = this.isMuted;
      video.play().catch(() => {
        video.muted = true;
        this.isMuted = true;
      });
    }
  }

  toggleMute() {
    this.isMuted = !this.isMuted;
    const video = document.getElementById('main-video') as HTMLVideoElement;
    if (video) video.muted = this.isMuted;
    localStorage.setItem('musicMuted', String(this.isMuted));
    this.sfx.play('Mute.mp3');
  }

  revealIntro() {
    if (!this.showIntroHint) {
      this.showIntroHint = true;
      this.cdr.markForCheck();
    }
  }

  onMouseDown(event: MouseEvent) {
    if (this.currentPage !== 'intro') return;
    if (!this.showIntroHint) {
      this.revealIntro();
      return;
    }
    if (event.button === 0) {
      event.preventDefault();
      this.clearProgress();
      this.progressStartTime = Date.now();
      this.progressInterval = setInterval(() => this.updateProgress(), 16);
    }
  }

  onMouseUp(_event: MouseEvent) {
    if (this.currentPage === 'intro') {
      this.clearProgress();
    }
  }

  onSpaceDown(event: KeyboardEvent) {
    if (this.currentPage !== 'intro') return;
    if (!this.showIntroHint) {
      this.revealIntro();
      return;
    }
    if (event.code === 'Space' && !event.repeat) {
      event.preventDefault();
      this.clearProgress();
      this.progressStartTime = Date.now();
      this.progressInterval = setInterval(() => this.updateProgress(), 16);
    }
  }

  onSpaceUp(event: KeyboardEvent) {
    if (event.code === 'Space' && this.currentPage === 'intro') {
      this.clearProgress();
    }
  }

  private updateProgress() {
    const elapsed = Date.now() - this.progressStartTime!;
    const progress = Math.min(elapsed / this.PROGRESS_DURATION, 1);
    const offset = this.RING_CIRCUMFERENCE * (1 - progress);
    const ring = document.getElementById('ring-progress') as HTMLElement;
    const hint = document.querySelector('.hint-text') as HTMLElement;
    if (ring) {
      ring.style.strokeDashoffset = String(offset);
      const r = Math.round(0 + 255 * progress);
      const g = Math.round(170 - 170 * progress);
      const b = Math.round(255 - 255 * progress);
      ring.style.stroke = `rgb(${r}, ${g}, ${b})`;
    }
    if (hint) hint.style.opacity = '0.3';
    if (progress >= 1) {
      this.clearProgress();
      const overlay = document.getElementById('transition-overlay') as HTMLElement;
      if (overlay) overlay.classList.add('active');
      setTimeout(() => {
        this.currentPage = 'login';
        this.cdr.markForCheck();
        if (overlay) overlay.classList.remove('active');
        setTimeout(() => {
          this.showPokeballPulse = true;
          this.cdr.markForCheck();
        }, 2500);
      }, 300);
    }
  }

  private clearProgress() {
    if (this.progressInterval) {
      clearInterval(this.progressInterval);
      this.progressInterval = null;
    }
    const ring = document.getElementById('ring-progress') as HTMLElement;
    if (ring) {
      ring.style.strokeDashoffset = String(this.RING_CIRCUMFERENCE);
      ring.style.stroke = 'rgb(0, 170, 255)';
    }
    const hint = document.querySelector('.hint-text') as HTMLElement;
    if (hint) hint.style.opacity = '1';
    this.progressStartTime = null;
  }

  toggleForm() {
    if (this.isRotating) return;
    this.showPokeballPulse = false;
    this.sfx.play('Click.mp3');
    this.isRotating = true;
    this.slideLeft = !this.slideLeft;
    this.cdr.markForCheck();

    setTimeout(() => {
      this.activeTab = this.slideLeft ? 'register' : 'login';
      if (this.activeTab === 'login') {
        this.regUser = '';
        this.regEmail = '';
        this.regPass = '';
      }
      this.cdr.markForCheck();
    }, 250);

    setTimeout(() => {
      this.isRotating = false;
      this.cdr.markForCheck();
    }, 500);
  }

  onLogin() {
    const username = this.loginUser.trim();
    const password = this.loginPass.trim();
    if (!username || !password) {
      this.toastService.show(this.translateService.instant('TOAST.LOGIN.ALL_FIELDS_REQUIRED'), 'error');
      return;
    }

    this.sfx.play('Click.mp3');
    this.isLoading = true;
    this.cdr.markForCheck();
    this.startTransition();
    const startedAt = Date.now();

    this.authService.login(username, password).subscribe({
      next: (user) => {
        if (this.keepSession) {
          localStorage.setItem('keepSession', 'true');
          localStorage.setItem('savedUser', user.username);
          localStorage.setItem('savedPass', password);
        } else {
          localStorage.removeItem('keepSession');
          localStorage.removeItem('savedUser');
          localStorage.removeItem('savedPass');
        }
        this.loginUser = '';
        this.loginPass = '';
        const minTotal = this.MOVE_TO_CENTER_MS + this.LOADING_AT_CENTER_MS;
        const remaining = Math.max(0, minTotal - (Date.now() - startedAt));
        this.toastService.show(this.translateService.instant('TOAST.LOGIN.LOGIN_SUCCESS'), 'success');
        this.sfx.play('OperacionExitosa.mp3');
        this.cdr.markForCheck();
        setTimeout(() => {
          this.isLoading = false;
          this.cdr.markForCheck();
          this.sfx.play('BolaAcercandose.mp3');
          this.transitionService.expand(() => {
            if (this.gameState.hasCharacter(user.username)) {
              const saved = this.gameState.getSavedPosition(user.username);
              if (saved) {
                this.router.navigate(['/juego', saved.location], { queryParams: { user: user.username } });
              } else {
                this.router.navigate(['/juego', 'ciudadPrincipal'], { queryParams: { user: user.username } });
              }
            } else {
              this.router.navigate(['/crear-personaje'], { queryParams: { user: user.username } });
            }
          });
        }, remaining);
      },
      error: (err: HttpErrorResponse) => {
        this.transitionService.dismiss();
        this.isAuthTransitioning = false;
        this.isLoading = false;
        const message = err.error?.message || this.translateService.instant('TOAST.LOGIN.LOGIN_ERROR');
        this.toastService.show(message, 'error');
        this.cdr.markForCheck();
      }
    });
  }

  onRegister() {
    const regUser = this.regUser.trim();
    const regEmail = this.regEmail.trim();
    const regPass = this.regPass.trim();
    if (!regUser || !regEmail || !regPass) {
      this.toastService.show(this.translateService.instant('TOAST.LOGIN.ALL_FIELDS_REQUIRED'), 'error');
      return;
    }
    if (!this.authService.isEmailValid(regEmail)) {
      this.toastService.show(this.translateService.instant('TOAST.LOGIN.INVALID_EMAIL'), 'error');
      return;
    }

    this.sfx.play('Click.mp3');
    this.isLoading = true;
    this.cdr.markForCheck();
    this.startTransition();
    const startedAt = Date.now();

    this.authService.register(regUser, regEmail, regPass).subscribe({
      next: (newUser) => {
        this.regUser = '';
        this.regEmail = '';
        this.regPass = '';
        const minTotal = this.MOVE_TO_CENTER_MS + this.LOADING_AT_CENTER_MS;
        const remaining = Math.max(0, minTotal - (Date.now() - startedAt));
        this.toastService.show(this.translateService.instant('TOAST.LOGIN.REGISTER_SUCCESS'), 'success');
        this.sfx.play('OperacionExitosa.mp3');
        this.cdr.markForCheck();
        setTimeout(() => {
          this.isLoading = false;
          this.cdr.markForCheck();
          this.sfx.play('BolaAcercandose.mp3');
          this.transitionService.expand(() => {
            this.router.navigate(['/crear-personaje'], { queryParams: { user: newUser.username } });
          });
        }, remaining);
      },
      error: (err: HttpErrorResponse) => {
        this.transitionService.dismiss();
        this.isAuthTransitioning = false;
        this.isLoading = false;
        const message = err.error?.message || this.translateService.instant('TOAST.LOGIN.REGISTER_ERROR');
        this.toastService.show(message, 'error');
        this.cdr.markForCheck();
      }
    });
  }

  onForgotPassword() {
    this.router.navigate(['/forgot-password']);
  }

  private startTransition() {
    const dockEl = document.querySelector('.pokeball-dock') as HTMLElement | null;
    this.isAuthTransitioning = true;
    this.sfx.play('PantallaCarga.mp3');
    this.cdr.markForCheck();
    this.transitionService.show(dockEl);
  }
}
