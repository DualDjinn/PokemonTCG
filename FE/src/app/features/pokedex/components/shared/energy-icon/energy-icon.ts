import { Component, computed, input } from '@angular/core';
import type { ElementType } from '../../../domain/models/element-type.model';

const ENERGY_MAP: Record<string, string> = {
  fire: '/assets/img/energy/fire.png',
  water: '/assets/img/energy/water.png',
  grass: '/assets/img/energy/grass.png',
  lightning: '/assets/img/energy/lightning.png',
  electric: '/assets/img/energy/lightning.png',
  psychic: '/assets/img/energy/psychic.png',
  fighting: '/assets/img/energy/fighting.png',
  darkness: '/assets/img/energy/darkness.png',
  metal: '/assets/img/energy/metal.png',
  fairy: '/assets/img/energy/fairy.png',
  dragon: '/assets/img/energy/dragon.png',
  colorless: '/assets/img/energy/colorless.png',
  normal: '/assets/img/energy/colorless.png',
};

@Component({
  standalone: true,
  selector: 'app-energy-icon',
  templateUrl: './energy-icon.html',
  styleUrl: './energy-icon.scss',
})
export class EnergyIcon {
  readonly type = input.required<string>();
  readonly size = input<number>(20);

  protected readonly src = computed(() => {
    const key = (this.type() || '').toLowerCase();
    return ENERGY_MAP[key] ?? '/assets/img/energy/colorless.png';
  });
}
