import { Component, OnInit, inject, ChangeDetectionStrategy, ChangeDetectorRef } from '@angular/core';
import { Router, ActivatedRoute } from '@angular/router';
import { HttpErrorResponse } from '@angular/common/http';
import { FormsModule } from '@angular/forms';
import { TranslateModule, TranslateService } from '@ngx-translate/core';
import { AuthService } from '../../data-access/services/auth.service';
import { ToastService } from '../../../../shared/services/toast.service';
import { FullPageDirective } from '../../../../shared/directives';

@Component({
  selector: 'app-reset-password-page',
  standalone: true,
  imports: [FormsModule, TranslateModule],
  hostDirectives: [FullPageDirective],
  templateUrl: './reset-password-page.html',
  styleUrl: './reset-password-page.css',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class ResetPasswordPage implements OnInit {
  private router = inject(Router);
  private route = inject(ActivatedRoute);
  private authService = inject(AuthService);
  private toastService = inject(ToastService);
  private translateService = inject(TranslateService);
  private cdr = inject(ChangeDetectorRef);

  token = '';
  newPassword = '';
  confirmPassword = '';
  isLoading = false;
  success = false;

  ngOnInit() {
    this.token = this.route.snapshot.queryParamMap.get('token') ?? '';
    if (!this.token) {
      this.toastService.show(this.translateService.instant('TOAST.RESET_PASSWORD.INVALID_TOKEN'), 'error');
      this.router.navigate(['/login']);
    }
  }

  onSubmit() {
    const password = this.newPassword.trim();
    const confirm = this.confirmPassword.trim();
    if (!password || !confirm) {
      this.toastService.show(this.translateService.instant('TOAST.RESET_PASSWORD.ALL_FIELDS_REQUIRED'), 'error');
      return;
    }
    if (password !== confirm) {
      this.toastService.show(this.translateService.instant('TOAST.RESET_PASSWORD.PASSWORDS_DONT_MATCH'), 'error');
      return;
    }
    if (password.length < 6) {
      this.toastService.show(this.translateService.instant('TOAST.RESET_PASSWORD.PASSWORD_TOO_SHORT'), 'error');
      return;
    }

    this.isLoading = true;
    this.cdr.markForCheck();

    this.authService.resetPassword(this.token, password).subscribe({
      next: () => {
        this.success = true;
        this.isLoading = false;
        this.toastService.show(this.translateService.instant('TOAST.RESET_PASSWORD.SUCCESS'), 'success');
        this.cdr.markForCheck();
        setTimeout(() => this.router.navigate(['/login']), 2000);
      },
      error: (err: HttpErrorResponse) => {
        this.isLoading = false;
        const message = err.error?.message || this.translateService.instant('TOAST.RESET_PASSWORD.TOKEN_ERROR');
        this.toastService.show(message, 'error');
        this.cdr.markForCheck();
      }
    });
  }

  goToLogin() {
    this.router.navigate(['/login']);
  }
}
