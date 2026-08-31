import { Component, computed, input } from '@angular/core';

@Component({
  standalone: true,
  selector: 'app-shiny-title',
  templateUrl: './shiny-title.html',
  styleUrl: './shiny-title.scss',
})
export class ShinyTitle {
  readonly text = input.required<string>();
  readonly shineColor = input('#c9ff3d');
  readonly textColor = input('#edebff');
  readonly speed = input(2);
  readonly fontSize = input('1rem');
  readonly fontWeight = input('700');

  protected readonly style = computed(() => ({
    fontSize: this.fontSize(),
    fontWeight: this.fontWeight(),
  }));
}
