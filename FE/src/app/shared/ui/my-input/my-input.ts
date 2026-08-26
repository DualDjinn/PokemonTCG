import { ChangeDetectionStrategy, Component, EventEmitter, forwardRef, Input, Output, ChangeDetectorRef, inject } from '@angular/core';
import { ControlValueAccessor, NG_VALUE_ACCESSOR } from '@angular/forms';

/**
 * Reusable text input.
 *
 * Wraps a native input and exposes the color, placeholder and label as
 * inputs so a single component covers every form field in the project.
 * Implements ControlValueAccessor so it can be used with Angular reactive
 * or template-driven forms ([(ngModel)] / formControlName).
 */
export type MyInputType = 'text' | 'password' | 'email' | 'number' | 'search';
export type MyInputVariant = 'solid' | 'pixel' | 'modern';

@Component({
  selector: 'my-input',
  standalone: true,
  templateUrl: './my-input.html',
  styleUrl: './my-input.css',
  changeDetection: ChangeDetectionStrategy.OnPush,
  providers: [
    {
      provide: NG_VALUE_ACCESSOR,
      useExisting: forwardRef(() => MyInput),
      multi: true,
    },
  ],
})
export class MyInput implements ControlValueAccessor {
  private cdr = inject(ChangeDetectorRef);

  @Input() value = '';
  @Input() placeholder = '';
  @Input() type: MyInputType = 'text';
  @Input() variant: MyInputVariant = 'solid';
  @Input() color = 'var(--neon-cyan)';
  @Input() textColor = 'var(--text-primary)';
  @Input() background = 'transparent';
  @Input() disabled = false;
  @Input() autocomplete?: string;

  @Output() valueChange = new EventEmitter<string>();
  @Output('keydown') keydownEvent = new EventEmitter<KeyboardEvent>();

  get resolvedAutocomplete(): string {
    if (this.autocomplete) return this.autocomplete;
    return this.type === 'password' ? 'new-password' : 'off';
  }

  private onChange: (value: string) => void = () => {};
  private onTouched: () => void = () => {};

  onInput(event: Event): void {
    const target = event.target as HTMLInputElement;
    this.value = target.value;
    this.onChange(this.value);
    this.valueChange.emit(this.value);
  }

  onBlur(): void {
    this.onTouched();
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

  setDisabledState(isDisabled: boolean): void {
    this.disabled = isDisabled;
  }
}
