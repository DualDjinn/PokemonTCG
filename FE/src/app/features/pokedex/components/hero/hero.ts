import { Component, input, output } from '@angular/core';
import { VideoSection } from './video-section/video-section';
import { BentoGrid } from './bento-grid/bento-grid';
import { OffersMarquee } from '../shared/offers-marquee/offers-marquee';
import { SobresSection } from '../sections/sobres-section/sobres-section';
import { DecksSection } from '../sections/decks-section/decks-section';
import { SinglesSection } from '../sections/singles-section/singles-section';
import { AccesoriosSection } from '../sections/accesorios-section/accesorios-section';
import { OpenedPack } from '../../domain/models/pack.model';
import { Card } from '../../domain/models/card.model';
import { SplashArt } from '../../domain/models/splash-art.model';
import { CardBack } from '../../../profile/data-access/cosmetics.service';

@Component({
  standalone: true,
  selector: 'app-shop-hero',
  imports: [
    VideoSection,
    BentoGrid,
    OffersMarquee,
    SobresSection,
    DecksSection,
    SinglesSection,
    AccesoriosSection,
  ],
  templateUrl: './hero.html',
  styleUrl: './hero.scss',
})
export class ShopHero {
  readonly activeSection = input<'sobres' | 'decks' | 'singles' | 'accesorios' | null>(null);

  readonly sectionChange = output<'sobres' | 'decks' | 'singles' | 'accesorios' | null>();
  readonly packOpened = output<OpenedPack>();
  readonly cardDetail = output<Card>();
  readonly splashDetail = output<SplashArt>();
  readonly cardBackDetail = output<CardBack>();

  protected onSectionChange(section: 'sobres' | 'decks' | 'singles' | 'accesorios' | null): void {
    this.sectionChange.emit(section);
  }
}
