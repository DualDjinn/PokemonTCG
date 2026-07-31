import { ChangeDetectionStrategy, Component, Input } from '@angular/core';
import { MyText } from '../../ui/my-text/my-text';

export interface ValidationCheck {
  label: string;
  passed: boolean;
}

@Component({
  selector: 'my-validation-checklist',
  standalone: true,
  imports: [MyText],
  templateUrl: './my-validation-checklist.html',
  styleUrl: './my-validation-checklist.css',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class MyValidationChecklist {
  @Input() title = '';
  @Input() checks: ValidationCheck[] = [];
}
