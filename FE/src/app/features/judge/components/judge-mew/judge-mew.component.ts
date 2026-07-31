import {
  ChangeDetectionStrategy,
  Component,
  inject,
} from '@angular/core';
import { JudgeMewService } from '../../services/judge-mew.service';

@Component({
  selector: 'app-judge-mew',
  changeDetection: ChangeDetectionStrategy.OnPush,
  styleUrl: './judge-mew.component.css',
  template: `
    <div
      class="mew-root"
      [attr.data-state]="mewService.state()"
      aria-hidden="true"
    >
      <div class="mew-aura"></div>

      <div class="mew-orbit">
        <span class="orb o1"></span>
        <span class="orb o2"></span>
        <span class="orb o3"></span>
        <span class="orb o4"></span>
        <span class="orb o5"></span>
        <span class="orb o6"></span>
      </div>

      <div class="fx-flash"></div>

      <div class="mew-ground-shadow"></div>

      <div class="mew-float">
        <div class="mew-sway">
          <div class="mew-tilt">
            <div class="mew-breathe">
              <img
                class="mew-sprite"
                src="/assets/img/character/judge-mew/mew1.gif"
                alt="Judge Mew"
                draggable="false"
              />
            </div>
          </div>
        </div>
      </div>

      <div class="fx-stars">
        <span class="st s1" style="--a:0deg">&#11088;</span>
        <span class="st s2" style="--a:90deg">&#10024;</span>
        <span class="st s3" style="--a:180deg">&#11088;</span>
        <span class="st s4" style="--a:270deg">&#10024;</span>
      </div>
    </div>
  `,
})
export class JudgeMewComponent {
  readonly mewService = inject(JudgeMewService);
}
