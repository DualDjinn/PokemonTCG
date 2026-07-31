import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting, HttpTestingController } from '@angular/common/http/testing';
import { TranslateModule, TranslateService } from '@ngx-translate/core';
import { PlayerProfilePage } from './player-profile.page';
import esTranslations from '../../../../../assets/i18n/es.json';
import { AuthService } from '../../../game/data-access/services/auth.service';
import { PlayerResponse } from '../../../game/domain/models/player-response.model';
import { ToastService } from '../../../../shared/services/toast.service';
import { GameStateService } from '../../../game/data-access/services/game-state.service';

describe('PlayerProfilePage', () => {
  let httpTestingController: HttpTestingController;
  let authService: AuthService;
  let toastService: ToastService;

  const currentUser: PlayerResponse = {
    id: 'player-1',
    username: 'ash',
    trainerName: 'Ash',
    email: 'ash@kanto.test',
    dateOfBirth: '2000-01-01',
    level: 7,
    experience: 320,
    xpForNextLevel: 500,
    xpIntoCurrentLevel: 120,
    coins: 500,
    activeDeckId: null,
    collectionCardIds: ['card-1', 'card-2'],
    decks: [{ id: 'deck-1', name: 'Liga Indigo', cardCount: 60 }],
  };

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [PlayerProfilePage, TranslateModule.forRoot()],
      providers: [
        provideRouter([{ path: 'decks', children: [] }]),
        provideHttpClient(),
        provideHttpClientTesting(),
      ],
    }).compileComponents();

    httpTestingController = TestBed.inject(HttpTestingController);
    authService = TestBed.inject(AuthService);
    toastService = TestBed.inject(ToastService);
    TestBed.inject(GameStateService);
    const translateService = TestBed.inject(TranslateService);
    translateService.setTranslation('es', esTranslations);
    translateService.use('es');
    authService.currentUser.set(currentUser);
    spyOn(toastService, 'show');
  });

  afterEach(() => {
    httpTestingController.verify();
  });

  it('should load profile data on init', () => {
    const fixture = TestBed.createComponent(PlayerProfilePage);
    const component = fixture.componentInstance;

    flushInitialRequests();
    fixture.detectChanges();

    expect(component.user()?.trainerName).toBe('Ash Ketchum');
    expect(component.decks().length).toBe(1);
    expect(component.collectionCards().length).toBe(2);
    expect(component.displayedCards().map(card => card.name)).toEqual(['Pikachu', 'Potion']);
    expect(fixture.nativeElement.textContent).toContain('Ash Ketchum');
  });

  it('should filter the collection by supertype', () => {
    const fixture = TestBed.createComponent(PlayerProfilePage);
    const component = fixture.componentInstance;

    flushInitialRequests();
    component.setFilter('Trainer');

    const reloadRequest = httpTestingController.expectOne(matchCollectionRequest('0', '200'));
    reloadRequest.flush({
      content: [
        { id: 'card-1', name: 'Pikachu', supertype: 'Pokemon', rarity: 'Common', images: { small: 'pikachu.png' } },
        { id: 'card-2', name: 'Potion', supertype: 'Trainer', rarity: 'Uncommon', imageUrl: 'potion.png' },
      ],
      last: true,
      totalElements: 2,
    });
    fixture.detectChanges();

    expect(component.displayedCards().length).toBe(1);
    expect(component.displayedCards()[0].name).toBe('Potion');
  });

  it('should save the updated trainer name', () => {
    const fixture = TestBed.createComponent(PlayerProfilePage);
    const component = fixture.componentInstance;

    flushInitialRequests();
    component.openNameModal();
    component.trainerNameForm.controls.trainerName.setValue('Misty');
    component.saveNameFromModal();

    const request = httpTestingController.expectOne('/api/players/trainer-name');
    expect(request.request.method).toBe('PUT');
    expect(request.request.body).toEqual({ trainerName: 'Misty' });

    request.flush({
      ...currentUser,
      trainerName: 'Misty',
    });
    fixture.detectChanges();

    expect(component.user()?.trainerName).toBe('Misty');
    expect(component.showNameModal()).toBeFalse();
    expect(toastService.show).toHaveBeenCalledWith('Nombre de entrenador actualizado.', 'success');
  });

  it('should expose fallback labels when no user is available', () => {
    const fixture = TestBed.createComponent(PlayerProfilePage);
    const component = fixture.componentInstance;

    flushInitialRequests();
    component.user.set(null);

    expect(component.trainerName()).toBe('Entrenador sin nombre');
    expect(component.levelLabel()).toBe('Nivel 0');
    expect(component.experienceLabel()).toBe('0 XP');
    expect(component.coinsLabel()).toBe('0');
    expect(component.cardProgressLabel()).toBe('Sin progreso disponible');
    expect(component.cardProgressPercent()).toBe(0);
  });

  it('should reset the form when cancelling trainer name editing', () => {
    const fixture = TestBed.createComponent(PlayerProfilePage);
    const component = fixture.componentInstance;

    flushInitialRequests();
    component.openNameModal();
    component.trainerNameForm.controls.trainerName.setValue('Gary');
    component.closeNameModal();

    expect(component.showNameModal()).toBeFalse();
    expect(component.trainerNameForm.controls.trainerName.value).toBe('Ash Ketchum');
  });

  it('should not submit an invalid trainer name update', () => {
    const fixture = TestBed.createComponent(PlayerProfilePage);
    const component = fixture.componentInstance;

    flushInitialRequests();
    component.openNameModal();
    component.trainerNameForm.controls.trainerName.setValue('');
    component.saveNameFromModal();

    httpTestingController.expectNone('/api/players/trainer-name');
    expect(component.trainerNameForm.invalid).toBeTrue();
  });

  it('should show an error when the profile data cannot be loaded', () => {
    const fixture = TestBed.createComponent(PlayerProfilePage);
    const component = fixture.componentInstance;

    httpTestingController.expectOne('/api/players/me').flush({
      ...currentUser,
      trainerName: 'Ash Ketchum',
    });
    httpTestingController.expectOne(request => request.url === '/api/players/me/collection').flush({
      content: [],
      last: true,
      totalElements: 0,
    });
    httpTestingController.expectOne('/api/decks').flush('error', { status: 500, statusText: 'Server Error' });
    flushCosmeticsRequests();
    fixture.detectChanges();

    expect(component.collectionLoading()).toBeFalse();
    expect(component.decksLoading()).toBeFalse();
    expect(toastService.show).toHaveBeenCalledWith('No se pudo cargar el perfil completo.', 'error');
  });

  it('should show an error when updating the trainer name fails', () => {
    const fixture = TestBed.createComponent(PlayerProfilePage);
    const component = fixture.componentInstance;

    flushInitialRequests();
    component.openNameModal();
    component.trainerNameForm.controls.trainerName.setValue('Brock');
    component.saveNameFromModal();

    const request = httpTestingController.expectOne('/api/players/trainer-name');
    request.flush('error', { status: 500, statusText: 'Server Error' });
    fixture.detectChanges();

    expect(component.showNameModal()).toBeTrue();
    expect(component.savingTrainerName()).toBeFalse();
    expect(toastService.show).toHaveBeenCalledWith('No se pudo actualizar el nombre de entrenador.', 'error');
  });

  it('should load more collection cards and append normalized fallback data', () => {
    const fixture = TestBed.createComponent(PlayerProfilePage);
    const component = fixture.componentInstance;

    flushInitialRequests({
      collection: {
        content: [{ id: 'card-1', name: 'Pikachu', supertype: 'Pokemon', rarity: 'Common', images: { small: 'pikachu.png' } }],
        last: false,
        totalElements: 3,
      },
      decks: [{ id: 'deck-1', name: 'Liga Indigo', cardsCount: 60, isValid: true }],
    });

    component.loadMoreCollection();

    const request = httpTestingController.expectOne(matchCollectionRequest('1', '200'));
    request.flush([
      {
        supertype: 'Energy',
        images: { large: 'energy-large.png' },
      },
      {},
    ]);
    fixture.detectChanges();

    expect(component.collectionCards().length).toBe(3);
    expect(component.collectionCards()[1].imageUrl).toBe('energy-large.png');
    expect(component.collectionCards()[2].name).toBe('Carta sin nombre');
    expect(component.collectionCards()[2].imageUrl).toBe('assets/img/portrait/Red.webp');
    expect(component.hasMoreCollection()).toBeFalse();
  });

  it('should show an error when loading more collection cards fails', () => {
    const fixture = TestBed.createComponent(PlayerProfilePage);
    const component = fixture.componentInstance;

    flushInitialRequests({
      collection: {
        content: [{ id: 'card-1', name: 'Pikachu', supertype: 'Pokemon', rarity: 'Common', images: { small: 'pikachu.png' } }],
        last: false,
        totalElements: 2,
      },
    });

    component.loadMoreCollection();
    httpTestingController.expectOne(matchCollectionRequest('1', '200')).flush('error', { status: 500, statusText: 'Server Error' });

    expect(toastService.show).toHaveBeenCalledWith('No se pudo cargar mas cartas de la coleccion.', 'error');
    expect(component.collectionLoading()).toBeFalse();
  });

  it('should ignore loadMoreCollection when it is already loading or there are no more pages', () => {
    const fixture = TestBed.createComponent(PlayerProfilePage);
    const component = fixture.componentInstance;

    flushInitialRequests();
    component.hasMoreCollection.set(false);
    component.loadMoreCollection();
    httpTestingController.expectNone(matchCollectionRequest('1', '200'));

    component.hasMoreCollection.set(true);
    component.collectionLoading.set(true);
    component.loadMoreCollection();
    httpTestingController.expectNone(matchCollectionRequest('1', '200'));
  });

  it('should normalize collection and deck fallback branches through helper calls', () => {
    const fixture = TestBed.createComponent(PlayerProfilePage);
    const component = fixture.componentInstance;

    flushInitialRequests();

    const normalizedCollection = component['normalizeCollectionResponse']({});
    const normalizedCard = component['normalizeCollectionCard']({ images: { large: 'rare-large.png' } });
    const normalizedDeck = component['normalizeDeck']({ cardsCount: 40, error: 'faltan cartas' });

    expect(normalizedCollection).toEqual({ content: [], last: true, totalElements: 0 });
    expect(normalizedCard.name).toBe('Carta sin nombre');
    expect(normalizedCard.rarity).toBe('Rareza sin informar');
    expect(normalizedCard.imageUrl).toBe('rare-large.png');
    expect(normalizedDeck.cardCount).toBe(40);
    expect(normalizedDeck.valid).toBeFalse();
    expect(normalizedDeck.validationMessage).toBe('faltan cartas');
  });

  it('should normalize supertype values to pokemon by default', () => {
    const fixture = TestBed.createComponent(PlayerProfilePage);
    const component = fixture.componentInstance;

    flushInitialRequests();

    expect(component['normalizeSupertype']('Trainer')).toBe('Trainer');
    expect(component['normalizeSupertype']('Energy')).toBe('Energy');
    expect(component['normalizeSupertype']('AnythingElse')).toBe('Pokemon');
  });

  it('should display coins label from user', () => {
    const fixture = TestBed.createComponent(PlayerProfilePage);
    const component = fixture.componentInstance;

    flushInitialRequests();

    expect(component.coinsLabel()).toBe('500');
  });

  function matchCollectionRequest(page: string, size: string): (request: { url: string; params: { get(name: string): string | null } }) => boolean {
    return request => request.url === '/api/players/me/collection'
      && request.params.get('page') === page
      && request.params.get('size') === size;
  }

  function flushInitialRequests(overrides?: {
    player?: Partial<PlayerResponse>;
    collection?: { content: unknown[]; last?: boolean; totalElements?: number };
    decks?: unknown[];
  }): void {
    const meRequest = httpTestingController.expectOne('/api/players/me');
    expect(meRequest.request.method).toBe('GET');
    meRequest.flush({
      ...currentUser,
      trainerName: 'Ash Ketchum',
      xpIntoCurrentLevel: 250,
      ...overrides?.player,
    });

    const collectionRequest = httpTestingController.expectOne(matchCollectionRequest('0', '200'));
    expect(collectionRequest.request.method).toBe('GET');
    collectionRequest.flush(overrides?.collection ?? {
      content: [
        {
          id: 'card-1',
          name: 'Pikachu',
          supertype: 'Pokemon',
          rarity: 'Common',
          images: { small: 'pikachu.png' },
        },
        {
          id: 'card-2',
          name: 'Potion',
          supertype: 'Trainer',
          rarity: 'Uncommon',
          imageUrl: 'potion.png',
        },
      ],
      last: true,
      totalElements: 2,
    });

    const decksRequest = httpTestingController.expectOne('/api/decks');
    expect(decksRequest.request.method).toBe('GET');
    decksRequest.flush(overrides?.decks ?? [
      {
        id: 'deck-1',
        name: 'Liga Indigo',
        cardCount: 60,
        valid: true,
      },
    ]);

    flushCosmeticsRequests();
  }

  function flushCosmeticsRequests(): void {
    httpTestingController.expectOne('/api/card-backs/me').flush({ activeCardBackId: null, owned: [] });
    httpTestingController.expectOne('/api/skins/me').flush([]);
  }
});
