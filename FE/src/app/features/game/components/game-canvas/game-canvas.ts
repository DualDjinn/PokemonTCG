import { Component, ElementRef, ViewChild, AfterViewInit, OnDestroy, Input, inject } from '@angular/core';
import { GameEngineService } from '../../data-access/services/game-engine.service';
import { GameData } from '../../engine/game-engine';
import { LocationKey } from '../../domain/models/location.model';

@Component({
  selector: 'app-game-canvas',
  standalone: true,
  template: '<canvas #gameCanvas></canvas>',
  styles: [':host { display: block; width: 100%; height: 100%; } canvas { display: block; width: 100%; height: 100%; }'],
})
export class GameCanvasComponent implements AfterViewInit, OnDestroy {
  @ViewChild('gameCanvas') canvasRef!: ElementRef<HTMLCanvasElement>;
  @Input() locationKey!: LocationKey;
  @Input() gameData!: GameData;
  @Input() username!: string;

  private engineService = inject(GameEngineService);

  ngAfterViewInit() {
    const canvas = this.canvasRef.nativeElement;
    this.engineService.startGame(canvas, this.locationKey, this.gameData, this.username);
  }

  ngOnDestroy() {
    this.engineService.destroy();
  }
}
