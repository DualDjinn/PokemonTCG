# Development Rules

These rules apply to every contributor on this project, on both the **Java** backend and the **TypeScript** frontend. They exist to keep the codebase consistent, readable, and maintainable for the whole team.

> **A note on languages:** Java uses Javadoc (`/** ... */`); TypeScript uses TSDoc/JSDoc (`/** ... */`). The format is the same — the rules below apply to both. When an example is shown in one language, the equivalent rule applies in the other.

---

## 1. Every file must be documented

Every class, interface, enum, type alias, and abstract class must have a doc comment at the top explaining **what it is and what it does**. This applies to all new code and to any existing file you modify.

- **Java:** Javadoc on every class, interface, enum, and abstract class.
- **TypeScript:** TSDoc on every exported class, interface, enum, type alias, and non-trivial exported function.

**What to write:**
- What the class/enum/interface/type represents in the context of the game.
- Any important constraint or rule it enforces.
- If it is part of a design pattern, mention which one and its role (e.g. _"Part of the Strategy pattern for attack effects"_).

**What NOT to write:**
- Do not restate what the code already says. `/** Returns the name. */` on a `getName()` method adds no value.
- Do not describe implementation details that change over time (e.g. _"uses a HashMap internally"_, _"uses a Map<string, Card> internally"_).

```java
// Java — Bad: says nothing useful
/** Card class. */
public abstract class Card { ... }

// Java — Good: explains the concept and its role
/**
 * Immutable base for every card in the game (Pokémon, Trainer, Energy).
 * Subclasses must declare their supertype and whether they can be played
 * in the current game context.
 */
public abstract class Card { ... }
```

```ts
// TypeScript — Bad: says nothing useful
/** Card class. */
export abstract class Card { ... }

// TypeScript — Good: explains the concept and its role
/**
 * Immutable base for every card in the game (Pokémon, Trainer, Energy).
 * Subclasses must declare their supertype and whether they can be played
 * in the current game context.
 */
export abstract class Card { ... }
```

Public methods that are not self-explanatory (especially those with non-obvious parameters or side effects) must also have a doc comment.

---

## 2. Keep it simple — do not over-engineer

Solve the problem at hand. Do not add complexity for problems that do not exist yet.

**Concrete rules:**
- Do not create abstractions, base classes, or utility methods/functions unless at least two different places need them right now.
- Do not add configuration flags, feature toggles, or optional parameters for behavior that is always the same.
- Do not write fallback logic for cases that cannot actually happen given the current game rules.
- If a method/function is under ~20 lines and does one clear thing, it does not need to be broken into smaller pieces.
- If you find yourself thinking _"we might need this later"_, stop — implement it later, when you actually need it.
- **TypeScript specific:** do not type things as `any` to silence the compiler. If a type is genuinely unknown, use `unknown` and narrow it. Do not export types or interfaces "just in case" — keep them local until a second module needs them.

The right question is always: **what is the simplest code that correctly solves this specific problem?**

---

## 3. Use design patterns where they fit naturally

This project has specific patterns assigned to specific responsibilities. Use them. Do not invent a new structure if an existing pattern already fits.

The patterns below apply across the stack: the backend implements the game rules, and the frontend mirrors the relevant ones (State for UI phase, Observer for live WebSocket updates, Facade for the API client).

### Assigned patterns and where they belong

| Pattern | Responsibility in this project |
|---|---|
| **State** | Turn phase transitions (DRAW → MAIN → ATTACK → BETWEEN_TURNS) and match state (WAITING → SETUP → ACTIVE → FINISHED). Each state is a class that knows which transitions are valid. On the frontend, the same model drives which UI is shown for the current phase. |
| **Strategy** | Attack effects and Trainer card effects. Each effect is a class implementing a common interface, registered in `CardEffectRegistry`. |
| **Chain of Responsibility** | The 7-step attack resolution pipeline (RF-01c). Each step is a handler that either processes its responsibility and passes control forward, or stops the chain. |
| **Observer** | WebSocket event notifications. Backend game engine components publish events (knockout, prize taken, status applied); the WebSocket layer forwards them to clients, and the frontend store subscribes to update the UI. |
| **Repository** | All database access for cards, decks, and match state on the backend. No SQL or JPA queries outside a Repository class. On the frontend, the equivalent is the API client layer — no `fetch`/HTTP calls outside it. |
| **Facade** | `GameEngine` is the single entry point for all game actions on the backend. Controllers and WebSocket handlers call the Facade; they never call engine internals directly. The frontend mirrors this with a single API client module — components never call HTTP endpoints directly. |

### When to use a pattern that is not on the list

Use it if — and only if — it genuinely simplifies the code. If you add a pattern, document its role in the file's doc comment. If the pattern makes the code harder to follow than a plain solution, use the plain solution.

### When NOT to use a pattern

Do not force a pattern onto simple logic just to have more patterns. A method that checks three boolean conditions does not need a Strategy. A two-step process does not need a Chain of Responsibility. A React component that renders a static panel does not need an Observer.

---

## 4. One responsibility per class / module

Each class does one thing. A class that validates rules does not also calculate damage. A class that maps DTOs does not also fetch data from the API. A React component that renders the hand does not also talk to the WebSocket.

If you cannot describe what a class or module does in one sentence without using the word "and", it is doing too much — split it.

---

## 5. Names must be descriptive

Names are the primary form of documentation for variables, methods, functions, and parameters.

- Use full words: `retreatCost`, not `rc`.
- Method/function names should read like sentences: `canEvolveFrom(base)`, `isKnockedOut()`.
- Boolean methods/functions start with `is`, `has`, or `can`: `isBasic()`, `hasEnoughEnergy()`, `canBePlayed()`.
- Avoid generic names like `data`, `info`, `manager` unless the class truly manages a lifecycle (like `TurnManager`).
- **Java:** classes `PascalCase`, methods and fields `camelCase`, constants `UPPER_SNAKE_CASE`.
- **TypeScript:** classes, interfaces, types, and enums `PascalCase`; variables and functions `camelCase`; constants `UPPER_SNAKE_CASE`; React components `PascalCase`. Do not prefix interfaces with `I` (`CardEffect`, not `ICardEffect`).

---

## Summary

| Rule | Short version |
|---|---|
| Document every file | Doc comment on every class, interface, enum, and type — explain the "what" and "why", not the "how". |
| Keep it simple | Only write code for problems that exist now. No speculative abstractions. No `any` in TypeScript. |
| Use the right patterns | Apply the six assigned patterns in their designated areas, on both backend and frontend. Add others only when they genuinely help. |
| One responsibility per class | If the description needs "and", split the class or module. |
| Descriptive names | Code should read like prose — no abbreviations, no generic names. Follow Java/TS naming conventions. |
