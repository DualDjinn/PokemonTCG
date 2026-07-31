import { ChangeDetectionStrategy, Component, inject } from '@angular/core';
import { RouterOutlet } from '@angular/router';
import { GameEngineService } from './features/game/data-access/services/game-engine.service';
import { MyToast } from './shared/components/my-toast/my-toast';

@Component({
  selector: 'app-root',
  imports: [RouterOutlet, MyToast],
  templateUrl: './app.html',
  styleUrl: './app.css',
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class App {
  engineService = inject(GameEngineService);
}
