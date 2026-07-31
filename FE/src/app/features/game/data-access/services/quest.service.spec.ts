import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting, HttpTestingController } from '@angular/common/http/testing';
import { QuestService } from './quest.service';
import { NpcRewardClaimResponse } from '../../domain/models/npc-reward.model';

describe('QuestService', () => {
  let service: QuestService;
  let httpTestingController: HttpTestingController;

  const claimResponse: NpcRewardClaimResponse = {
    npcId: 'Don Alfajor',
    alreadyClaimed: false,
    card: {
      id: 'pokesecret-8',
      name: "I've Become So Numb",
      supertype: 'COLLECTIBLE',
      imageUrl: 'http://localhost:9000/custom-cards/ive-become-so-numb.png',
      imageUrlLarge: 'http://localhost:9000/custom-cards/ive-become-so-numb.png',
      rarity: 'COMMON',
    },
  };

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()],
    });
    service = TestBed.inject(QuestService);
    httpTestingController = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpTestingController.verify();
  });

  it('starts with no claimed rewards', () => {
    expect(service.isClaimed('Don Alfajor')).toBeFalse();
    expect(service.claimedNpcIds().size).toBe(0);
  });

  it('loadClaimedRewards populates the claimed set', () => {
    service.loadClaimedRewards();

    const req = httpTestingController.expectOne('/api/quests/npc/claimed');
    expect(req.request.method).toBe('GET');
    req.flush(['Don Alfajor']);

    expect(service.isClaimed('Don Alfajor')).toBeTrue();
    expect(service.isClaimed('Sofia')).toBeFalse();
  });

  it('loadClaimedRewards leaves the set empty on error', () => {
    service.loadClaimedRewards();

    httpTestingController
      .expectOne('/api/quests/npc/claimed')
      .flush('boom', { status: 500, statusText: 'Server Error' });

    expect(service.claimedNpcIds().size).toBe(0);
  });

  it('claimNpcReward POSTs to the url-encoded claim endpoint', () => {
    let result: NpcRewardClaimResponse | undefined;
    service.claimNpcReward('Don Alfajor').subscribe(res => (result = res));

    const req = httpTestingController.expectOne('/api/quests/npc/Don%20Alfajor/claim');
    expect(req.request.method).toBe('POST');
    req.flush(claimResponse);

    expect(result).toEqual(claimResponse);
  });

  it('claimNpcReward marks the npc as claimed locally', () => {
    service.claimNpcReward('Don Alfajor').subscribe();
    httpTestingController
      .expectOne('/api/quests/npc/Don%20Alfajor/claim')
      .flush(claimResponse);

    expect(service.isClaimed('Don Alfajor')).toBeTrue();
  });
});
