# Plan 01 — Card Effect Self-Registration

**Goal.** Replace the 408-line god-method in `CardEffectRegistry` with a Spring-driven self-registration mechanism. Each card effect (whether a `@Component`-annotated `isolated/` class or a `@Bean` method in a new `@Configuration` recipe class) declares its own `(cardId, attackIndex)` via a `@CardEffect` annotation. The registry becomes a thin discoverer.

**Decision already made (do not relitigate).** Self-registration via Spring + custom annotation. *Not* JSON externalization. Co-locate DSL recipes in a single `@Configuration` (Option B) rather than splitting them into one file per card.

**Public API contract.** `CardEffectRegistry.getAttackEffect(String cardId, int attackIndex)` must keep its current signature and semantics. Callers in the attack pipeline are out of scope.

---

## Conventions

- **Working repo**: `/Users/manusanchez/Desktop/tpi-pokemon-2w2-07/BE`
- **Path correction** (the original brief was wrong): the registry actually lives at `src/main/java/ar/edu/utn/frc/tup/piii/models/effects/CardEffectRegistry.java`, NOT under `helpers/`. The test is at `src/test/java/ar/edu/utn/frc/tup/piii/models/effects/CardEffectRegistryTest.java`. Use these paths.
- **Build & quality gates**: Checkstyle + PMD run on `mvn package`. Every phase must end green.
- **Test command**: `./mvnw test`
- **Full verification**: `./mvnw clean package`
- **Total registrations to preserve**: **160** `register(...)` calls in the current file. The plan must finish with 160 distinct `(cardId, attackIndex)` keys resolvable from the new registry.

---

## Phase 0 — Documentation Discovery (already done — read this section, don't redo)

### Allowed Spring APIs (verified)

| API | Signature | Use |
|---|---|---|
| `ConfigurableListableBeanFactory.findAllAnnotationsOnBean` | `<A extends Annotation> Set<A> findAllAnnotationsOnBean(String beanName, Class<A> annotationType, boolean allowFactoryBeanInit)` | **Primary lookup.** Returns *all* occurrences of a repeatable annotation, works uniformly whether the annotation lives on the bean's class (for `@Component`-annotated `isolated/` effects) or on the `@Bean` factory method (for recipes). |
| `Map<String, T>` autowiring | `@Autowired Map<String, AttackEffect> effects;` | Spring populates with all `AttackEffect` beans keyed by bean name. Use this to iterate, then call `findAllAnnotationsOnBean(name, CardEffect.class, true)` for each. |
| `MergedAnnotations.from(elem, strategy, RepeatableContainers.standardRepeatables()).stream(MyAnno.class)` | static factory | Fallback if `findAllAnnotationsOnBean` proves insufficient. Not expected to be needed. |

**Anti-pattern guard.** Do *not* reflect `bean.getClass().getAnnotation(CardEffect.class)` — for `@Bean`-method-produced beans, the annotation lives on the factory method, not the instance's class. Use the bean factory APIs above.

**Anti-pattern guard.** Do *not* rely on `ApplicationContext.getBeansWithAnnotation` for repeatable annotations — its multi-occurrence behavior is uncertain per Phase 0 research. Use `findAllAnnotationsOnBean(..., true)` instead.

### Local code facts (verified)

- Registry path: `src/main/java/ar/edu/utn/frc/tup/piii/models/effects/CardEffectRegistry.java` (~408 lines, 9 private register methods).
- **Dual-registered `isolated/` classes** (only 4):
  - `EnergyGlideEffect` → `xy1-46:0`, `xy1-143:0`
  - `ElectronCrushEffect` → `xy1-46:1`, `xy1-143:1`
  - `YCycloneEffect` → `xy1-79:1`, `xy1-144:1`
  - `JoustEffect` → `xy1-80:0`, `xy1-145:0`
- **31** `isolated/` classes registered exactly once; **4** registered twice → these 4 need the repeatable annotation.
- **67** DSL-only registrations (`Effects.*` / `Conditions.*` / `Counts.*` only).
- **36** inline `common/*` registrations (parameterized constructors).
- **~57** mixed/composite registrations (DSL wrapping a `common/` instance, or `Effects.seq(new X(), new Y())`).
- **Existing test does sample-lookups only.** It does NOT iterate over all keys. The migration could silently drop registrations without failing tests — this is why **Phase 0 captures a baseline snapshot**.

---

## Phase 1 — Capture verification baseline

**Goal.** Pin down the exact set of `(cardId, attackIndex)` keys the registry resolves *today*, so later phases can prove zero regression.

### What to implement

1. **Add a baseline-capture test** `src/test/java/ar/edu/utn/frc/tup/piii/models/effects/CardEffectRegistryBaselineTest.java`.
   - Inject the `CardEffectRegistry` via `@SpringBootTest`.
   - Define a constant `List<String> EXPECTED_KEYS` (160 entries: every `"cardId:attackIndex"` currently in `CardEffectRegistry`). Generate this list by parsing the current registry file — see verification step below for the one-liner that produces it.
   - For each expected key, assert `registry.getAttackEffect(cardId, idx) != null`.
   - Add an upper-bound assertion: the registry must resolve *only* those 160 keys (iterate via reflection of the private `registry` map, or expose a package-private `keys()` method just for testing — choose the cleanest option; recommend **package-private `Set<String> registeredKeys()` on `CardEffectRegistry`** so the test is clean and the method is reusable in later phases).

2. **Add the `registeredKeys()` accessor** to `CardEffectRegistry` (one method, ~3 lines, returns an unmodifiable copy of the map's key set). Document it as "test/debug only, not part of the runtime contract."

### How to verify

```bash
# Generate the EXPECTED_KEYS list from the current registry. Paste into the test.
cd /Users/manusanchez/Desktop/tpi-pokemon-2w2-07/BE
grep -oE 'register\("xy1-[0-9]+", *[0-9]+' src/main/java/ar/edu/utn/frc/tup/piii/models/effects/CardEffectRegistry.java \
  | sed -E 's/register\("(xy1-[0-9]+)", *([0-9]+)/\1:\2/' \
  | sort -u | wc -l
# Expected output: 160
```

Then:
```bash
./mvnw test -Dtest=CardEffectRegistryBaselineTest
```
Must pass against unchanged registry. This locks in the contract.

### Decision points

- **Expose `registeredKeys()` or use reflection?** → Recommend exposing the accessor. Reflection-in-tests is fragile and Checkstyle/PMD may flag it.

### Files touched
- `src/main/java/ar/edu/utn/frc/tup/piii/models/effects/CardEffectRegistry.java` (add `registeredKeys()`)
- `src/test/java/ar/edu/utn/frc/tup/piii/models/effects/CardEffectRegistryBaselineTest.java` (new)

---

## Phase 2 — Introduce the `@CardEffect` annotation

**Goal.** Land the annotation types. No behavior change.

### What to implement

Create `src/main/java/ar/edu/utn/frc/tup/piii/models/effects/CardEffect.java`:

```java
@Target({ ElementType.TYPE, ElementType.METHOD })
@Retention(RetentionPolicy.RUNTIME)
@Repeatable(CardEffects.class)
@Documented
public @interface CardEffect {
    String cardId();
    int attackIndex();
}
```

Create `src/main/java/ar/edu/utn/frc/tup/piii/models/effects/CardEffects.java`:

```java
@Target({ ElementType.TYPE, ElementType.METHOD })
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface CardEffects {
    CardEffect[] value();
}
```

### Critical decisions

- **Do NOT meta-annotate `@CardEffect` with `@Component`.** Spring component scanning of meta-annotated repeatable annotations is fragile (the container `@CardEffects` would also need to be a `@Component`, which gets weird). Instead: authors of `isolated/` classes will put `@Component` *and* `@CardEffect(...)` on the class explicitly. This is two annotations, but it's unambiguous and avoids meta-annotation pitfalls.
- **Allow both `ElementType.TYPE` and `ElementType.METHOD`** so the same annotation works for `@Component` classes (TYPE) and `@Bean` methods in `CardEffectRecipes` (METHOD).
- **Validation of `cardId` format** (e.g. matches `^xy[0-9]+-[0-9]+$`) — *out of scope* for this phase. Could be added later via a `BeanFactoryPostProcessor`. Don't gold-plate.

### Verification

```bash
./mvnw clean compile   # must succeed; nothing yet uses the annotation
./mvnw test            # baseline test must still pass
```

### Files touched
- `src/main/java/ar/edu/utn/frc/tup/piii/models/effects/CardEffect.java` (new)
- `src/main/java/ar/edu/utn/frc/tup/piii/models/effects/CardEffects.java` (new)

---

## Phase 3 — Migrate `isolated/` classes to self-registration

**Goal.** Move every `isolated/` class registration out of `CardEffectRegistry` and onto the class itself.

### What to implement

For each of the **31 single-registered** classes in `registerPhaseBExistingIsolated()` (lines 308–346 of the registry):

1. Open the class file under `models/effects/isolated/`.
2. Add `@Component` (Spring) and `@CardEffect(cardId = "xy1-XX", attackIndex = N)` at TYPE level. Example for `HardenEffect`:
   ```java
   @Component
   @CardEffect(cardId = "xy1-4", attackIndex = 0)
   public class HardenEffect implements AttackEffect { ... }
   ```
3. Delete the corresponding `register(...)` line from `CardEffectRegistry`.

For each of the **4 dual-registered** classes (`EnergyGlideEffect`, `ElectronCrushEffect`, `YCycloneEffect`, `JoustEffect`), use the repeatable annotation:

```java
@Component
@CardEffect(cardId = "xy1-79", attackIndex = 1)
@CardEffect(cardId = "xy1-144", attackIndex = 1)
public class YCycloneEffect implements AttackEffect { ... }
```

(Java compiles two `@CardEffect`s into a `@CardEffects` container automatically thanks to `@Repeatable`.)

There may be additional `isolated/` migrations in `registerPhaseCMigratedFromIsolated()` (lines 355–397) — handle those identically.

### Critical decisions

- **One class, multiple keys** → use the repeatable annotation, not separate instances. Spring still creates one bean; `findAllAnnotationsOnBean` returns both `@CardEffect` occurrences.
- **Bean naming** → let Spring default to the class name (e.g. `hardenEffect`). Do not force `@Component("xy1-4:0")` because colons in bean names are syntactically fine but cause grief in qualifier resolution. The registry will key off the annotation, not the bean name.
- **What if an isolated class is no longer referenced by `CardEffectRegistry` after migration?** → That's the success condition. The `register(...)` line is deleted; the class itself stays.
- **Order of migration** → Do classes alphabetically to make review diffs reviewable; commit in batches of ~10. Each batch must keep tests green.

### Anti-pattern guards

- Don't add `@CardEffect` to abstract bases (e.g. `AbstractAttackResolutionHandler` in a different package — not relevant here, but the principle stands).
- Don't add `@Component` to a class that's not meant to be a Spring bean. Only the 35 `isolated/` effect classes that currently appear in the registry get it.
- Don't change the class's `apply(GameContext)` body. This is pure annotation work.

### Verification

After each batch of ~10 migrations:
```bash
./mvnw test -Dtest=CardEffectRegistryBaselineTest
./mvnw test -Dtest=CardEffectRegistryTest
```
Both must pass. Baseline test will start failing as you delete `register(...)` lines — that's expected, but **only after Phase 5 wires up the discovery side**. During Phase 3, keep both the annotation AND the `register(...)` call temporarily; do not delete `register(...)` calls yet.

> **Important sequencing**: Phase 3 *adds* `@Component` + `@CardEffect` to the 35 classes but **does NOT delete the `register(...)` lines**. Deletion happens in Phase 5, after the discoverer is wired. This way Phase 3 ends with everything still green.

### Files touched
- `src/main/java/ar/edu/utn/frc/tup/piii/models/effects/isolated/*.java` (35 files, ~2 line additions each)
- `src/main/java/ar/edu/utn/frc/tup/piii/models/effects/CardEffectRegistry.java` — **no deletions yet** in this phase

---

## Phase 4 — Build `CardEffectRecipes` `@Configuration`

**Goal.** Land the new home for the ~125 non-isolated registrations (DSL-only + inline `common/` + composites) as `@Bean` methods with `@CardEffect` annotations. Still no deletions from the registry.

### What to implement

Create `src/main/java/ar/edu/utn/frc/tup/piii/configs/CardEffectRecipes.java`:

```java
@Configuration(proxyBeanMethods = false)
public class CardEffectRecipes {

    // === Status applies ===
    @Bean @CardEffect(cardId = "xy1-1", attackIndex = 0)
    AttackEffect venusaurExPoisonPowder() {
        return Effects.applyStatus(StatusCondition.POISONED);
    }

    @Bean @CardEffect(cardId = "xy1-5", attackIndex = 0)
    AttackEffect beedrillPoisonJab() {
        return Effects.applyStatus(StatusCondition.POISONED);
    }

    // === Inline common/ effects ===
    @Bean @CardEffect(cardId = "xy1-7", attackIndex = 1)
    AttackEffect machPunchSpread() {
        return new SpreadBenchDamageEffect(10, 1);
    }

    // === Composites ===
    @Bean @CardEffect(cardId = "xy1-29", attackIndex = 0)
    AttackEffect rapidSpinSwitch() {
        return Effects.seq(new SwitchSelfEffect(), new SwitchOpponentEffect());
    }

    // ... ~125 more
}
```

### Critical decisions

- **`proxyBeanMethods = false`** — these `@Bean` methods don't call each other and don't need CGLIB enhancement. Skipping the proxy is the Spring Boot 4 idiomatic choice and avoids one of the gotchas flagged in Phase 0.
- **Method naming** — use a comment-friendly camelCase name describing the card + attack (e.g. `venusaurExPoisonPowder`, `quickAttack`, `crisisVine`). Bean name defaults to the method name; do NOT use `xy1-9:1` as a method name (colon is illegal in Java identifiers, dashes are illegal too).
- **Section comments** — preserve the section banners from the original registry (`// ── Status conditions ──`, `// ── Self-healing ──`, etc.) inside `CardEffectRecipes` so readability is at least as good as today's god-method.
- **Migration order** — copy the *entire body* of each of the 9 private `registerXxx` methods into `CardEffectRecipes`, one section at a time. Each `register("xy1-X", N, <expr>)` line becomes:
  ```java
  @Bean @CardEffect(cardId = "xy1-X", attackIndex = N)
  AttackEffect descriptiveName() { return <expr>; }
  ```
- **Duplicate-key check at startup** — out of scope for this phase. The discoverer in Phase 5 will fail-fast on duplicate keys; that's enough.
- **`isolated/` classes are NOT touched again here.** They were handled in Phase 3 with `@Component`. The recipe config is *only* for the DSL + inline-common + composite cases.

### Anti-pattern guards

- Do **not** copy the `register(...)` body verbatim — it's a method call, not a returnable expression. Convert each `register("id", N, expr)` to a `@Bean` method returning `expr`.
- Do **not** make `CardEffectRecipes` extend or reference `CardEffectRegistry`. They are separate beans.
- Do **not** instantiate `isolated/` classes via `new` inside recipe methods if the class already has `@Component` from Phase 3 — that would create two beans of the same class. For composites that need an `isolated/` instance (rare; verify by grepping), use constructor injection or `@Bean`-method parameter injection.

### Verification

```bash
./mvnw clean compile         # compile the new config + 125 @Bean methods
./mvnw test                  # all tests still pass
# Count the @Bean methods carrying @CardEffect:
grep -c "@CardEffect" src/main/java/ar/edu/utn/frc/tup/piii/configs/CardEffectRecipes.java
# Expected: 125 (160 total − 35 isolated/ classes)
```

### Files touched
- `src/main/java/ar/edu/utn/frc/tup/piii/configs/CardEffectRecipes.java` (new, ~250 lines including comments)
- `src/main/java/ar/edu/utn/frc/tup/piii/models/effects/CardEffectRegistry.java` — **no deletions yet**

---

## Phase 5 — Rewrite `CardEffectRegistry` as a discoverer

**Goal.** Replace the constructor + 9 private register methods with one discovery loop. Delete every `register(...)` call.

### What to implement

Replace the body of `CardEffectRegistry` with approximately:

```java
@Component
public class CardEffectRegistry {

    private final Map<String, AttackEffect> registry = new HashMap<>();

    public CardEffectRegistry(
            Map<String, AttackEffect> effectsByBeanName,
            ConfigurableListableBeanFactory beanFactory) {

        effectsByBeanName.forEach((beanName, effect) -> {
            Set<CardEffect> annotations = beanFactory.findAllAnnotationsOnBean(
                    beanName, CardEffect.class, true);
            for (CardEffect ann : annotations) {
                String key = ann.cardId() + ":" + ann.attackIndex();
                AttackEffect previous = registry.putIfAbsent(key, effect);
                if (previous != null) {
                    throw new IllegalStateException(
                        "Duplicate @CardEffect registration for " + key
                        + " (bean " + beanName + ")");
                }
            }
        });

        if (registry.isEmpty()) {
            throw new IllegalStateException(
                "No @CardEffect beans discovered — refusing to start with empty registry");
        }
    }

    public AttackEffect getAttackEffect(String cardId, int attackIndex) {
        return registry.get(cardId + ":" + attackIndex);
    }

    /** Test/debug accessor — not part of the runtime contract. */
    Set<String> registeredKeys() {
        return Collections.unmodifiableSet(registry.keySet());
    }
}
```

Then **delete** all 9 private `registerXxx` methods and their imports.

### Critical decisions

- **Why `findAllAnnotationsOnBean` not `getAnnotation`?** Repeatable support + factory-method support, in one call. See Phase 0 doc table.
- **Why fail-fast on empty registry?** Catches "you broke component scanning" early instead of silently shipping a broken match.
- **Why fail-fast on duplicate keys?** Today the registry's `put` would silently overwrite. After this refactor, a typo putting the same key on two beans should be a build-time-ish failure.
- **Constructor injection of `ConfigurableListableBeanFactory`** — this is the documented way to access annotation lookup. Spring will inject it. Do NOT use `@Autowired ApplicationContext` and cast.

### Anti-pattern guards

- Do not catch and log the `IllegalStateException`s. Let them propagate; Spring will fail the context startup, which is exactly what you want.
- Do not invoke `findAnnotationOnBean` (singular) — it loses repeats.
- Do not iterate via `applicationContext.getBeansWithAnnotation(CardEffect.class)` — multi-occurrence semantics not verified per Phase 0.

### Verification

```bash
./mvnw clean package
```
Must succeed (compile + Checkstyle + PMD + all tests). In particular:
- `CardEffectRegistryBaselineTest` must still pass — proving all 160 keys are resolvable through the new discovery path.
- `CardEffectRegistryTest` must still pass — proving the behavior of each tested effect is unchanged.
- Manual key-count check:
  ```bash
  # After this phase, the registry file should be ~40 lines, no register() calls:
  grep -c "register(" src/main/java/ar/edu/utn/frc/tup/piii/models/effects/CardEffectRegistry.java
  # Expected: 0
  wc -l src/main/java/ar/edu/utn/frc/tup/piii/models/effects/CardEffectRegistry.java
  # Expected: under 60
  ```

### Files touched
- `src/main/java/ar/edu/utn/frc/tup/piii/models/effects/CardEffectRegistry.java` (now ~40 lines)

---

## Phase 6 — Final verification & cleanup

**Goal.** Prove zero regression. Tidy up imports. Land docs.

### What to do

1. **Full build & test cycle:**
   ```bash
   ./mvnw clean package
   ```
   Must be green including Checkstyle and PMD.

2. **Baseline key-set equivalence:**
   The Phase 1 `CardEffectRegistryBaselineTest` re-runs and confirms:
   - All 160 original keys still resolve.
   - The new registry resolves *exactly* those 160 keys (no extras, no missing).

3. **Behavioral equivalence:**
   `CardEffectRegistryTest`'s ~30 sample-lookup tests still pass.

4. **Sanity-check the registry size:**
   ```bash
   wc -l src/main/java/ar/edu/utn/frc/tup/piii/models/effects/CardEffectRegistry.java
   # Expected: ~40 lines (from ~408)
   ```

5. **Confirm annotation coverage:**
   ```bash
   # Count @CardEffect occurrences across the codebase.
   grep -rE '@CardEffect[^s]' src/main/java | wc -l
   # Expected: 160 (one per (cardId, attackIndex) across isolated/ + recipes config)
   #   — except dual-registered isolated classes count twice in source if you used
   #     the repeatable annotation in stacked form. Adjust expectation if so:
   #     35 single-registered isolated classes + 4 dual (2 each = 8) + 125 recipes
   #     = 35 + 8 + 125 = 168 occurrences in source, but 160 distinct keys.
   ```

6. **Update Javadoc** on `CardEffectRegistry` to describe the discovery mechanism (one paragraph). Mention `@CardEffect` and `CardEffectRecipes` so future contributors find them.

7. **Update CLAUDE.md** to mention the new extension pattern under the "Card → Domain mapping" section:
   ```
   New card effects: implement AttackEffect, annotate the class with
   @Component + @CardEffect(cardId, attackIndex), OR add a @Bean method
   in configs/CardEffectRecipes annotated the same way. The
   CardEffectRegistry discovers them at startup.
   ```

### Anti-pattern checks (run as greps)

```bash
# 1. No surviving register() calls in CardEffectRegistry:
grep -nE 'register\("xy1-' src/main/java/ar/edu/utn/frc/tup/piii/models/effects/CardEffectRegistry.java
# Expected: no matches.

# 2. No stale imports in CardEffectRegistry:
grep -nE 'import .*effects\.(common|isolated|dsl)\.' \
  src/main/java/ar/edu/utn/frc/tup/piii/models/effects/CardEffectRegistry.java
# Expected: no matches (the registry no longer references effect impls directly).

# 3. No bean.getClass().getAnnotation(CardEffect.class) anywhere:
grep -rn 'getClass().getAnnotation(CardEffect' src/main/java
# Expected: no matches (would be the wrong lookup; we use findAllAnnotationsOnBean).
```

### Done criteria

- [ ] `./mvnw clean package` green.
- [ ] `CardEffectRegistryBaselineTest` passes — 160 keys, exactly.
- [ ] `CardEffectRegistryTest` passes — all sample lookups unchanged.
- [ ] `CardEffectRegistry.java` is ≤ 60 lines.
- [ ] `CardEffectRecipes.java` exists with ~125 `@Bean @CardEffect` methods.
- [ ] All 35 `isolated/` classes that were referenced by the old registry now carry `@Component` + `@CardEffect(...)` (singular or stacked).
- [ ] Anti-pattern greps all return empty.
- [ ] CLAUDE.md updated.

---

## Out of scope (do NOT do as part of this plan)

- Abilities (`Ability` model) — separate future plan, same pattern with `@AbilityEffect`.
- Trainer cards (`TrainerCard.canBePlayed` throwing) — separate future plan.
- Moving effect data to JSON / external API.
- Touching `GameContext`, `DamageCalculator`, `AttackResolutionPipeline`, or any non-registry consumer.
- Adding new card effects beyond what's already registered.
- Validating `cardId` format at boot (could be a follow-up; don't gold-plate here).

---

## Rollback plan

Each phase is a separate commit. If Phase 5 fails for unexpected reasons, revert that commit — the codebase still works because Phases 3 and 4 left all the `register(...)` calls intact. The old registry constructor co-exists with the annotations until Phase 5 deletes it.

If Phase 3 batches go bad, revert the batch commit — `@Component` + `@CardEffect` are inert additions until the registry reads them in Phase 5.

---

## Effort estimate

| Phase | Estimated effort | Risk |
|---|---|---|
| 1 — Baseline | 30 min | Low |
| 2 — Annotation | 15 min | Low |
| 3 — Migrate isolated/ | 1–2 hours (35 files, mechanical) | Low — mostly find-and-add |
| 4 — Build recipes config | 2–3 hours (125 @Bean methods) | Medium — copy/paste fatigue, naming |
| 5 — Discoverer | 30 min | Medium — Spring API correctness |
| 6 — Verification | 30 min | Low |

**Total: ~5–7 hours of focused work.**
