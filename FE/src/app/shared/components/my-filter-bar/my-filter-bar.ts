import { ChangeDetectionStrategy, Component, EventEmitter, Input, Output } from '@angular/core';
import { TranslateModule } from '@ngx-translate/core';

export interface FilterOption {
  label: string;
  value: string;
}

@Component({
  selector: 'my-filter-bar',
  standalone: true,
  imports: [TranslateModule],
  templateUrl: './my-filter-bar.html',
  styleUrl: './my-filter-bar.css',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class MyFilterBar {
  @Input() options: FilterOption[] = [];
  @Input() selected = '';
  @Input() color = 'var(--neon-cyan, #00ccaa)';
  @Input() translate = true;

  @Output() selectedChange = new EventEmitter<string>();

  btnClass(value: string): string {
    const active = this.selected === value;
    const base =
      'px-2 py-1 border border-dashed border-[var(--filter-color)] rounded-full font-pixel text-[0.5rem] cursor-pointer transition-all duration-150 uppercase tracking-wider hover:opacity-100 hover:bg-[rgba(var(--filter-color-rgb),0.15)] hover:scale-105';
    if (active) {
      return base + ' bg-[var(--filter-color)] text-[#050508] font-bold shadow-[0_0_10px_rgba(var(--filter-color-rgb),0.5)] scale-105';
    }
    return base + ' opacity-60';
  }

  select(value: string): void {
    this.selectedChange.emit(value);
  }
}
