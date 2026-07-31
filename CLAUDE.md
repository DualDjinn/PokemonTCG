# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Layout

Monorepo with two sub-projects:
- `BE/` — Spring Boot 4.0 / Java 21 backend (see `BE/CLAUDE.md` for deep backend detail)
- `FE/` — Angular 20 frontend (standalone components, Tailwind CSS)

---

## Commands

### Backend (`BE/`)

```bash
./mvnw spring-boot:run                                        # Run the app (port 8080)
./mvnw clean package                                          # Build + Checkstyle + PMD + tests
./mvnw test                                                   # Run all tests
./mvnw test -Dtest=PaymentServiceTest                         # Run a single test class
./mvnw test -Dtest=PaymentServiceTest#creditCoinsOnApproval   # Run a single test method
./mvnw clean package -Dcheckstyle.skip=true -Dpmd.skip=true  # Build skipping quality checks
```

Swagger UI is available at `http://localhost:8080/swagger-ui.html` when running.

Tests use H2 in-memory DB (`application-test.properties`). Dev/prod use MySQL on port 3400.

### Frontend (`FE/`)

```bash
npm start       # Dev server at http://localhost:4200 (proxies /api, /cards, /ws to :8080)
npm run build   # Production build
npm test        # Karma + Jasmine
```

### Infrastructure

```bash
# From BE/
docker-compose up   # MySQL (port 3400) + MinIO (ports 9000/9001)
```

---

## Architecture

### Auth

Session-cookie based. `PlayerController` issues an `JSESSIONID` cookie on `POST /api/players/login` or `/register`. All protected backend endpoints use the `@Authenticated` custom annotation — `AuthenticatedPlayerResolver` resolves the current `Player` by reading the session. The FE `session.interceptor.ts` attaches `withCredentials: true` to every HTTP request and redirects to `/login` on 401.

Frontend routes protected by `authGuard` (`features/game/guards/auth.guard.ts`); the guard checks `AuthService.currentUser()` signal, which is bootstrapped from `localStorage` and refreshed via `GET /api/players/me`.

### Coins Economy

`Player.coins` is persisted in the `player` table. Three paths modify it:
1. **Pack purchase** — `PackService.openPack()` deducts coins atomically (`@Transactional`).
2. **MercadoPago payment** — `PaymentService.handleWebhook()` credits coins on IPN approval. The amount is derived server-side by `calculateCoinsForAmount(totalAmount)` mapping: `$1→100`, `$2→500`, `$3→1000`, `$4→5000`. The frontend passes `coinsToAdd` in the request but the backend ignores it and recalculates from the total.
3. **DataSeeder** — on every app startup, `DataSeeder.grantDevCoins()` adds `99,999` coins to the player with email `stefano.pzt@gmail.com` (dev-only convenience). This is independent of payments and affects only that one hardcoded address.

### Payment Flow (Adapter Pattern)

`PaymentService` (client) → `PaymentGateway` interface (port) → `MercadoPagoAdapter` (adapter). Swapping providers only requires a new `PaymentGateway` implementation — `PaymentService` never changes. The `Purchase` entity tracks state across the async webhook lifecycle (`approved`/`rejected` flags set by IPN).

### Game Engine

Fully isolated from Spring (no HTTP types inside). Exposed via `GameEngine` facade. Uses:
- **State** — `TurnPhase` FSM (`DrawState`, `MainState`, `AttackState`, `BetweenTurnsState`)
- **Strategy** — `AttackEffect` per card ID + attack index, registered in `CardEffectRegistry`
- **Chain of Responsibility** — `AttackResolutionPipeline` (7 handlers)
- **Observer** — `MatchEventPublisher` dispatches to WebSocket via STOMP

### WebSocket

STOMP + SockJS endpoint at `/ws`. `SessionHandshakeInterceptor` bridges the HTTP session cookie to the WebSocket handshake so the same `Player` identity is available in `ChatController` and `GameController`.

### Card Data

146 cards from the XY1 set (pokemontcg.io v2 API) are cached in `BE/src/main/resources/data/db.json`. `CardRegistryService` loads them into memory at startup; `DataSeeder` persists them to the DB once. Cards are never fetched from the external API at runtime.

### Frontend Structure

Feature-driven: each feature owns its own pages, components, services, and domain models.

| Feature | Route(s) | Purpose |
|---|---|---|
| `game` | `/login`, `/juego/:location`, `/crear-personaje` | Auth + 2D game canvas (Three.js) |
| `lobby` | `/lobby` | Multiplayer matchmaking + chat |
| `match` | `/batalla/:matchId` | Card game battle board |
| `pokedex` | `/pokeshop` | Card/pack/coins marketplace |
| `decks` | `/decks`, `/decks/:id` | Deck builder |
| `profile` | `/profile` | Player stats + collection |
| `slot-machine` | `/slot-machine` | Mini-game (xoshiro256++ PRNG) |
| `payment` | `/payment/success`, `/payment/failure` | MercadoPago redirect pages |

Shared UI primitives (`MyButton`, `MyText`, `MyInput`, etc.) live in `shared/ui/`. Cross-feature services (`ToastService`, `CircleTransitionService`) are in `shared/services/`. Core infrastructure (`StorageService`, `LanguageService`, `session.interceptor`) is in `core/`.

### i18n

`@ngx-translate` with 4 languages: Spanish (default), English, Italian, Portuguese. Translation files: `FE/src/assets/i18n/{es,en,it,pt}.json`. Active language stored in `localStorage` as `app_language`. When adding new keys, update all 4 files.

### Storage

| Environment | Database | Config file |
|---|---|---|
| dev | MySQL 8 on port 3400, db `pokemon_tcg_dev`, user `pokemon_dev` | `application-dev.properties` |
| prod | MySQL 8 on port 3400, db `pokemon_tcg_prod` | `application-prod.properties` |
| test | H2 in-memory | `application-test.properties` |

MinIO object storage (ports 9000/9001) holds custom card images; credentials default to `admin`/`admin123456` locally.
