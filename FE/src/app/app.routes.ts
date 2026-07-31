import { Routes } from '@angular/router';
import { authGuard } from './features/game/guards/auth.guard';

export const routes: Routes = [
  {
    path: 'login',
    loadComponent: () => import('./features/game/pages/login-page/login-page').then(m => m.LoginPage)
  },
  {
    path: 'crear-personaje',
    canActivate: [authGuard],
    loadComponent: () => import('./features/game/pages/character-creation-page/character-creation-page').then(m => m.CharacterCreationPage)
  },
  {
    path: 'juego/:location',
    canActivate: [authGuard],
    loadComponent: () => import('./features/game/pages/game-page/game-page').then(m => m.GamePage)
  },
  {
    path: 'pokeshop',
    loadComponent: () => import('./features/pokedex/pages/pokedex-page/pokedex-page').then(m => m.PokedexPage)
  },
  {
    path: 'profile',
    canActivate: [authGuard],
    loadComponent: () => import('./features/profile/pages/player-profile/player-profile.page').then(m => m.PlayerProfilePage)
  },
  {
    path: 'decks',
    canActivate: [authGuard],
    loadComponent: () => import('./features/decks/pages/deck-list/deck-list.page').then(m => m.DeckListPage)
  },
  {
    path: 'decks/:id',
    canActivate: [authGuard],
    loadComponent: () => import('./features/decks/pages/deck-detail/deck-detail.page').then(m => m.DeckDetailPage)
  },
  {
    path: 'batalla/prueba',
    loadComponent: () => import('./features/match/pages/battle/battle.page').then(m => m.BattlePage)
  },
  {
    path: 'batalla/:matchId',
    canActivate: [authGuard],
    loadComponent: () => import('./features/match/pages/battle/battle.page').then(m => m.BattlePage)
  },
  {
    path: 'forgot-password',
    loadComponent: () => import('./features/game/pages/forgot-password-page/forgot-password-page').then(m => m.ForgotPasswordPage)
  },
  {
    path: 'reset-password',
    loadComponent: () => import('./features/game/pages/reset-password-page/reset-password-page').then(m => m.ResetPasswordPage)
  },
  {
    path: 'slot-machine',
    loadComponent: () => import('./features/slot-machine/slot-machine.page').then(m => m.SlotMachinePage)
  },
  {
    path: 'mew',
    loadComponent: () => import('./features/mew/pages/mew-playground/mew-playground.page').then(m => m.MewPlaygroundPage)
  },
  {
    path: 'lobby',
    canActivate: [authGuard],
    loadComponent: () => import('./features/lobby/pages/lobby-page').then(m => m.LobbyPage)
  },
  {
    path: 'payment/success',
    loadComponent: () => import('./features/payment/pages/payment-success/payment-success.page').then(m => m.PaymentSuccessPage)
  },
  {
    path: 'payment/failure',
    loadComponent: () => import('./features/payment/pages/payment-failure/payment-failure.page').then(m => m.PaymentFailurePage)
  },
  {
    path: '',
    redirectTo: 'login',
    pathMatch: 'full'
  },
  {
    path: '**',
    loadComponent: () => import('./features/not-found/pages/not-found-page/not-found.page').then(m => m.NotFoundPage)
  }
];
