import { ChangeDetectionStrategy, Component, EventEmitter, forwardRef, Input, Output } from '@angular/core';
import { ControlValueAccessor, NG_VALUE_ACCESSOR } from '@angular/forms';
import { MyText } from '../my-text/my-text';

/**
 * Labeled checkbox with a custom-painted box.
 *
 * The native checkbox is hidden and a styled square is drawn next to
 * the label so colors stay consistent with the rest of the design.
 * Implements ControlValueAccessor so it can bind to Angular forms.
 */
@Component({
  selector: 'my-checkbox',
  standalone: true,
  imports: [MyText],
  templateUrl: './my-checkbox.html',
  styleUrl: './my-checkbox.css',
  changeDetection: ChangeDetectionStrategy.OnPush,
  providers: [
    {
      provide: NG_VALUE_ACCESSOR,
      useExisting: forwardRef(() => MyCheckbox),
      multi: true,
    },
  ],
})
export class MyCheckbox implements ControlValueAccessor {
  @Input() checked = false;
  @Input() label = '';
  @Input() variant: 'solid' | 'pixel' = 'solid';
  @Input() color = 'var(--neon-cyan)';
  @Input() textColor = 'var(--text-primary)';
  @Input() disabled = false;

  @Output() checkedChange = new EventEmitter<boolean>();

  private onChange: (value: boolean) => void = () => {};
  private onTouched: () => void = () => {};

  toggle(): void {
    if (this.disabled) {
      return;
    }
    this.checked = !this.checked;
    this.onChange(this.checked);
    this.checkedChange.emit(this.checked);
  }

  markTouched(): void {
    this.onTouched();
  }

  writeValue(value: boolean): void {
    this.checked = !!value;
  }

  registerOnChange(fn: (value: boolean) => void): void {
    this.onChange = fn;
  }

  registerOnTouched(fn: () => void): void {
    this.onTouched = fn;
  }

  setDisabledState(isDisabled: boolean): void {
    this.disabled = isDisabled;
  }
}
