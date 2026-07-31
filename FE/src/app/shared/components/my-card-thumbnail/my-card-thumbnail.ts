import { ChangeDetectionStrategy, Component, EventEmitter, Input, Output } from '@angular/core';
import { MyText } from '../../ui/my-text/my-text';

@Component({
  selector: 'my-card-thumbnail',
  standalone: true,
  imports: [MyText],
  templateUrl: './my-card-thumbnail.html',
  styleUrl: './my-card-thumbnail.css',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class MyCardThumbnail {
  @Input() imageUrl = '';
  @Input() name = '';
  @Input() count = 0;
  @Input() showCount = true;
  @Input() rarity = '';
  @Input() color = 'var(--neon-cyan, #00ccaa)';
  @Input() hoverOverlay = false;
  @Input() showName = true;

  @Output() clicked = new EventEmitter<MouseEvent>();

  get isHolo(): boolean {
    return this.rarity === 'Rare Holo EX' || this.rarity === 'Rare Ultra';
  }

  onClick(event: MouseEvent): void {
    this.clicked.emit(event);
  }

  onImgError(event: Event): void {
    const img = event.target as HTMLImageElement;
    img.style.display = 'none';
  }
}
