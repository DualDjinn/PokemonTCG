# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

Spring Boot 4.0 / Java 21 backend for a Pokémon TCG digital game (TPI — Programación III, UTN FRC).
The game engine must enforce the official XY1 rulebook exclusively on the backend; the frontend is a pure presentation layer.
Card data comes from the `pokemontcg.io` v2 API (set `xy1`, 146 cards), cached locally in `src/main/resources/data/db.json`.

## Commands

```bash
# Run the application
./mvnw spring-boot:run

# Build (runs Checkstyle + PMD + tests)
./mvnw clean package

# Run all tests
./mvnw test

# Run a single test class
./mvnw test -Dtest=PingControllerTest

# Run a single test method
./mvnw test -Dtest=PingControllerTest#pingTest

# Skip code quality checks during build (use sparingly)
./mvnw clean package -Dcheckstyle.skip=true -Dpmd.skip=true

# Generate Javadoc
./mvnw javadoc:javadoc
```

Swagger UI is available at `http://localhost:8080/swagger-ui.html` when the app is running.

## Code Quality

Checkstyle and PMD rules live in `.code_quality/checkstyle_rules.xml` and `.code_quality/pmd_rules.xml`. Both run automatically on `mvn package`. All new code must pass both checks before a PR can be merged.

## Architecture

### Package layout

```
ar.edu.utn.frc.tup.piii
├── configs/       Spring @Configuration beans (ModelMapper, ObjectMapper, SpringDoc)
├── controllers/   REST controllers — thin; delegate to services
├── dtos/          Raw API DTOs (pokemontcg.io response shape) + common ErrorApi
├── helpers/       CardMapper, CardEffectRegistry
└── models/        Domain model (card hierarchy, game state)
```

### Domain model — card hierarchy

`Card` is the abstract base (`@SuperBuilder`, immutable `@Getter`). All card types extend it:

- **`PokemonCard`** — HP, `PokemonStage` (BASIC/STAGE_1/STAGE_2/MEGA), `isEx`/`isMega` flags, list of `Attack`, list of `Ability`, list of `Weakness`/`Resistance`, `retreatCost` (int, not a list), `evolvesFrom`/`evolvesTo`. `getPrizeValue()` returns 2 for EX/MEGA, 1 otherwise.
- **`EnergyCard`** — `EnergyType`, rules list.
- **`TrainerCard`** (abstract) — subclasses: `ItemCard`, `AceSpecCard`, `StadiumCard`, `SupporterCard`, `PokemonToolCard`. Each implements `getSubtype()` and `getPlayRule()`. Note: `TrainerCard` does **not** extend `Card`; it is a parallel hierarchy.

### Card → Domain mapping

`CardMapper` (`helpers/`) converts `CardApiDTO` (raw pokemontcg.io JSON) to domain objects. It dispatches on `supertype` (`"Pokemon"` / `"Trainer"` / `"Energy"`). Attack energy costs are stored as `Map<EnergyType, Integer>`. Damage strings like `"30+"` are parsed into `baseDamage` (int) + `DamageModifier` enum (NONE / PLUS / TIMES / MINUS). `CardEffectRegistry` resolves a `Strategy`-based `AttackEffect` by card ID + attack index — **this is where attack logic implementations will be registered**.

### Game engine (to be built)

Per the project requirements, the engine must be split into independent, testable components:

| Component | Responsibility |
|---|---|
| `RuleValidator` | Validates legality of any action before execution (≥90% test coverage required) |
| `DamageCalculator` | Base damage → weakness (×2) → resistance (−20, min 0) → modifiers (≥90% coverage) |
| `StatusEffectManager` | Between-turn processing in order: Poisoned → Burned → Asleep → Paralyzed (≥90% coverage) |
| `VictoryConditionChecker` | Prize cards taken, knockout total, deck-out, Sudden Death |
| `TurnManager` | Turn phase FSM: DRAW → MAIN → ATTACK → BETWEEN_TURNS |

The engine must be fully isolated from transport (REST / WebSocket) — no Spring HTTP types inside the engine. Expose it via a `GameEngine` **Facade**.

### Required design patterns

| Pattern | Where |
|---|---|
| **State** | Turn phases (DRAW/MAIN/ATTACK/BETWEEN_TURNS) and match states (WAITING/SETUP/ACTIVE/FINISHED) |
| **Strategy** | Attack effects (`AttackEffect`) and Trainer card effects |
| **Chain of Responsibility** | 7-step attack resolution pipeline (RF-01c) |
| **Observer** | WebSocket event dispatch (knockout, prize taken, status applied, turn end) |
| **Repository** | Data access for cards, decks, and match state |
| **Facade** | `GameEngine` entry point |

### Data persistence

Currently uses H2 in-memory DB (development only). Production will require PostgreSQL or MySQL. The full match state must be persisted after every relevant action so a disconnected player can reconnect and resume. The action log is append-only and immutable.

### Security rules (enforced in backend, not frontend)

- Never send a player's hand cards to the opponent — only the count.
- Deck order and Prize card contents stay hidden until rules require reveal.
- All move validation lives in the backend; frontend validation is complementary only.

## Key enums

- `EnergyType` — GRASS, FIRE, WATER, LIGHTNING, PSYCHIC, FIGHTING, DARKNESS, METAL, FAIRY, DRAGON, COLORLESS
- `PokemonStage` — BASIC, STAGE_1, STAGE_2, MEGA
- `DamageModifier` — NONE, PLUS, TIMES, MINUS
- `Supertype` — POKEMON, TRAINER, ENERGY
- `CardRarity` — values map from pokemontcg.io rarity strings via `@JsonValue`
