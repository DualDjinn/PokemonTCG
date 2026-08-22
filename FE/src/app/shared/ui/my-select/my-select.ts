import { ChangeDetectionStrategy, Component, EventEmitter, Input, Output, forwardRef } from '@angular/core';
import { ControlValueAccessor, NG_VALUE_ACCESSOR } from '@angular/forms';

export interface MySelectOption {
  label: string;
  value: string;
}

@Component({
  selector: 'my-select',
  standalone: true,
  templateUrl: './my-select.html',
  styleUrl: './my-select.css',
  changeDetection: ChangeDetectionStrategy.OnPush,
  providers: [
    {
      provide: NG_VALUE_ACCESSOR,
      useExisting: forwardRef(() => MySelect),
      multi: true,
    },
  ],
})
export class MySelect implements ControlValueAccessor {
  @Input() options: MySelectOption[] = [];
  @Input() value = '';
  @Input() placeholder = '';
  @Input() variant: 'solid' | 'pixel' | 'modern' = 'solid';
  @Input() color = 'var(--neon-cyan)';
  @Input() textColor = 'var(--text-primary)';
  @Input() background = 'var(--bg-secondary)';
  @Input() disabled = false;

  @Output() valueChange = new EventEmitter<string>();

  private onChange: (value: string) => void = () => {};
  private onTouched: () => void = () => {};

  onSelect(event: Event): void {
    const target = event.target as HTMLSelectElement;
    this.value = target.value;
    this.onChange(this.value);
    this.valueChange.emit(this.value);
  }

  onBlur(): void {
    this.onTouched();
  }

  writeValue(value: string): void {
    this.value = value ?? '';
  }

  registerOnChange(fn: (value: string) => void): void {
    this.onChange = fn;
  }

  registerOnTouched(fn: () => void): void {
    this.onTouched = fn;
  }

  setDisabledState(isDisabled: boolean): void {
    this.disabled = isDisabled;
  }
}
