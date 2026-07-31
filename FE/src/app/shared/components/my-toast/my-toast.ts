import { ChangeDetectionStrategy, Component, inject } from '@angular/core';
import { TranslateModule } from '@ngx-translate/core';
import { ToastService } from '../../services/toast.service';

@Component({
  selector: 'my-toast',
  standalone: true,
  imports: [TranslateModule],
  template: `
    @if (toastService.toast(); as toast) {
      <div class="toast-overlay" (click)="toastService.clear()">
        <div class="toast-panel" [class.success]="toast.type === 'success'" [class.error]="toast.type === 'error'" [class.info]="toast.type === 'info'">
          <div class="toast-icon">
            @if (toast.type === 'success') {
              <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round">
                <path d="M22 11.08V12a10 10 0 1 1-5.93-9.14"/>
                <polyline points="22 4 12 14.01 9 11.01"/>
              </svg>
            } @else if (toast.type === 'error') {
              <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round">
                <circle cx="12" cy="12" r="10"/>
                <line x1="15" y1="9" x2="9" y2="15"/>
                <line x1="9" y1="9" x2="15" y2="15"/>
              </svg>
            } @else {
              <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round">
                <circle cx="12" cy="12" r="10"/>
                <line x1="12" y1="16" x2="12" y2="12"/>
                <line x1="12" y1="8" x2="12.01" y2="8"/>
              </svg>
            }
          </div>
          <span class="toast-message">{{ toast.message }}</span>
          <button class="toast-close" (click)="toastService.clear(); $event.stopPropagation()" [attr.aria-label]="'COMMON.CLOSE' | translate">✕</button>
        </div>
      </div>
    }
  `,
  styles: [`
    :host { display: contents; }

    .toast-overlay {
      position: fixed;
      top: 0;
      left: 0;
      width: 100%;
      height: 100%;
      z-index: 9999;
      display: flex;
      justify-content: center;
      pointer-events: auto;
    }

    .toast-overlay::before {
      content: '';
      position: absolute;
      inset: 0;
      pointer-events: none;
    }

    .toast-panel {
      position: fixed;
      top: 1.5rem;
      display: flex;
      align-items: center;
      gap: 0.75rem;
      padding: 0.875rem 1.25rem;
      border: 3px solid;
      background-color: var(--pixel-panel-bg);
      background-image:
        linear-gradient(45deg, rgba(0,170,255,0.04) 25%, transparent 25%),
        linear-gradient(-45deg, rgba(0,170,255,0.04) 25%, transparent 25%);
      background-size: 4px 4px;
      font-family: 'Press Start 2P', monospace;
      font-size: 0.585rem;
      color: var(--text-primary);
      box-shadow: 0 0 20px rgba(0, 204, 170, 0.3);
      animation: toastSlideIn 0.2s steps(4);
      max-width: min(90vw, 32rem);
      cursor: pointer;
      pointer-events: auto;
      text-transform: uppercase;
    }

    .toast-panel.success {
      border-color: var(--pixel-border-top-left) var(--pixel-border-bottom-right) var(--pixel-border-bottom-right) var(--pixel-border-top-left);
      box-shadow: 0 0 20px rgba(0, 204, 170, 0.3);
    }

    .toast-panel.error {
      border-color: var(--pixel-border-top-left) #4e2a2a #4e2a2a var(--pixel-border-top-left);
      box-shadow: 0 0 20px rgba(255, 51, 51, 0.3);
    }

    .toast-panel.info {
      border-color: var(--pixel-border-top-left) #2a3a5e #2a3a5e var(--pixel-border-top-left);
      box-shadow: 0 0 20px rgba(59, 130, 246, 0.3);
    }

    .toast-icon {
      width: 1.5rem;
      height: 1.5rem;
      flex-shrink: 0;
    }

    .toast-panel.success .toast-icon {
      color: #00ccaa;
    }

    .toast-panel.error .toast-icon {
      color: #ff3333;
    }

    .toast-panel.info .toast-icon {
      color: #3b82f6;
    }

    .toast-message {
      flex: 1;
      line-height: 1.4;
    }

    .toast-close {
      flex-shrink: 0;
      width: 1.5rem;
      height: 1.5rem;
      display: flex;
      align-items: center;
      justify-content: center;
      border: 2px solid var(--pixel-border-top-left);
      background: var(--pixel-card-bg);
      color: var(--text-secondary);
      font-family: 'Press Start 2P', monospace;
      font-size: 0.45rem;
      cursor: pointer;
      padding: 0;
      line-height: 1;
    }

    .toast-close:hover {
      background: var(--pixel-border-bottom-right);
      color: var(--text-primary);
    }

    @keyframes toastSlideIn {
      from {
        opacity: 0;
        transform: translateY(-24px);
      }
      to {
        opacity: 1;
        transform: translateY(0);
      }
    }
  `],
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class MyToast {
  readonly toastService = inject(ToastService);
}
