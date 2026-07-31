import { Injectable } from '@angular/core';

@Injectable({ providedIn: 'root' })
export class AuthTransitionService {
  private overlay: HTMLElement | null = null;
  private hiddenDockEl: HTMLElement | null = null;
  private originalVisibility = '';

  show(dockEl?: HTMLElement | null): void {
    if (this.overlay) return;

    let dockRect: DOMRect | null = null;
    if (dockEl) {
      dockRect = dockEl.getBoundingClientRect();
    }

    const dockWidth = dockRect && dockRect.width > 0 ? dockRect.width : 300;
    const loadingSize = Math.round(dockWidth * 0.7225);

    const overlay = document.createElement('div');
    overlay.className = 'auth-transition-overlay';

    if (dockRect) {
      const dockCenterX = dockRect.left + dockRect.width / 2;
      const dockCenterY = dockRect.top + dockRect.height / 2;
      const viewportCenterX = window.innerWidth / 2;
      const viewportCenterY = window.innerHeight / 2;
      const offsetX = dockCenterX - viewportCenterX;
      const offsetY = dockCenterY - viewportCenterY;
      const initialScale = dockWidth / loadingSize;
      overlay.style.setProperty('--auth-tx', `${offsetX}px`);
      overlay.style.setProperty('--auth-ty', `${offsetY}px`);
      overlay.style.setProperty('--auth-initial-scale', String(initialScale));
    }

    overlay.innerHTML = `
      <div class="auth-transition-pokeball-scaler" style="width:${loadingSize}px;height:${loadingSize}px;">
        <img src="assets/img/login/pokeball.svg" class="auth-transition-pokeball-spinner" alt="" />
      </div>
      <div class="auth-transition-text">Cargando...</div>
    `;
    document.body.appendChild(overlay);

    if (dockEl) {
      this.hiddenDockEl = dockEl;
      this.originalVisibility = dockEl.style.visibility;
      dockEl.style.visibility = 'hidden';
    }

    void overlay.offsetWidth;
    overlay.classList.add('active');
    this.overlay = overlay;
  }

  expand(onPeak?: () => void): Promise<void> {
    this.hiddenDockEl = null;
    return new Promise((resolve) => {
      const overlay = this.overlay;
      if (!overlay) {
        onPeak?.();
        resolve();
        return;
      }
      overlay.classList.add('expanding');
      const peakAt = 700;
      const fadeOutAt = 1300;
      setTimeout(() => onPeak?.(), peakAt);
      setTimeout(() => {
        this.dismiss();
        resolve();
      }, fadeOutAt);
    });
  }

  dismiss(): void {
    if (this.overlay) {
      this.overlay.remove();
      this.overlay = null;
    }
    if (this.hiddenDockEl) {
      this.hiddenDockEl.style.visibility = this.originalVisibility;
      this.hiddenDockEl = null;
      this.originalVisibility = '';
    }
  }
}
