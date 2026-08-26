import { ChangeDetectionStrategy, Component, EventEmitter, forwardRef, Input, Output, ChangeDetectorRef, inject } from '@angular/core';
import { ControlValueAccessor, NG_VALUE_ACCESSOR } from '@angular/forms';
import { MyInput } from '../../ui/my-input/my-input';
import { MyIconButton } from '../../ui/my-icon-button/my-icon-button';

@Component({
  selector: 'my-search-bar',
  standalone: true,
  imports: [MyInput, MyIconButton],
  templateUrl: './my-search-bar.html',
  styleUrl: './my-search-bar.css',
  changeDetection: ChangeDetectionStrategy.OnPush,
  providers: [
    {
      provide: NG_VALUE_ACCESSOR,
      useExisting: forwardRef(() => MySearchBar),
      multi: true,
    },
  ],
})
export class MySearchBar implements ControlValueAccessor {
  private cdr = inject(ChangeDetectorRef);

  @Input() value = '';
  @Input() placeholder = 'Search...';
  @Input() variant: 'solid' | 'pixel' | 'modern' = 'solid';
  @Input() color = 'var(--neon-cyan)';
  @Input() textColor = 'var(--text-primary)';
  @Input() background = 'transparent';
  @Input() showClear = true;

  @Output() valueChange = new EventEmitter<string>();
  @Output() cleared = new EventEmitter<void>();

  private onChange: (value: string) => void = () => {};
  private onTouched: () => void = () => {};

  onValueChange(val: string): void {
    this.value = val;
    this.onChange(this.value);
    this.valueChange.emit(this.value);
  }

  clear(): void {
    this.value = '';
    this.onChange(this.value);
    this.valueChange.emit(this.value);
    this.cleared.emit();
  }

  writeValue(value: string): void {
    this.value = value ?? '';
    this.cdr.markForCheck();
  }

  registerOnChange(fn: (value: string) => void): void {
    this.onChange = fn;
  }

  registerOnTouched(fn: () => void): void {
    this.onTouched = fn;
  }
}
