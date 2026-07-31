package ar.edu.utn.frc.tup.piii.services;

import ar.edu.utn.frc.tup.piii.models.effects.CardEffectRegistry;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import ar.edu.utn.frc.tup.piii.dtos.AttackDTO;
import ar.edu.utn.frc.tup.piii.dtos.CardApiDTO;
import ar.edu.utn.frc.tup.piii.dtos.ResistanceDTO;
import ar.edu.utn.frc.tup.piii.dtos.WeaknessDTO;
import ar.edu.utn.frc.tup.piii.models.AceSpecCard;
import ar.edu.utn.frc.tup.piii.models.AttackEffect;
import ar.edu.utn.frc.tup.piii.models.Card;
import ar.edu.utn.frc.tup.piii.models.DamageModifier;
import ar.edu.utn.frc.tup.piii.models.EnergyCard;
import ar.edu.utn.frc.tup.piii.models.EnergyType;
import ar.edu.utn.frc.tup.piii.models.ItemCard;
import ar.edu.utn.frc.tup.piii.models.PokemonCard;
import ar.edu.utn.frc.tup.piii.models.PokemonStage;
import ar.edu.utn.frc.tup.piii.models.PokemonToolCard;
import ar.edu.utn.frc.tup.piii.models.StadiumCard;
import ar.edu.utn.frc.tup.piii.models.SupporterCard;
import ar.edu.utn.frc.tup.piii.models.Supertype;

/**
 * Unit tests for {@link CardMapper}.
 *
 * <p>The mapper's only collaborator is {@link CardEffectRegistry}, which is mocked
 * to verify the (cardId, attackIndex) pair passed in and to control the resolved
 * {@link AttackEffect}. The input DTOs are also mocked because they expose only
 * Lombok-generated getters (no setters or builders).
 *
 * <p>Tests are organised by feature using {@link Nested} classes and target every
 * branch of the mapper to keep line/branch coverage above 90 %.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class CardMapperTest {

    @Mock
    private CardEffectRegistry effectRegistry;

    @Mock
    private ar.edu.utn.frc.tup.piii.models.effects.AbilityRegistry abilityRegistry;

    private CardMapper mapper;

    @BeforeEach
    void setUp() {
        mapper = new CardMapper(effectRegistry, abilityRegistry);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // toDomain() — supertype routing
    // ─────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("toDomain() — routing por supertype")
    class ToDomainRoutingTests {

        @Test
        @DisplayName("'Pokémon' → PokemonCard")
        void routesToPokemon() {
            CardApiDTO dto = pokemonDtoMinimal("xy1-1", "Charmander", "60", List.of("Basic"));
            Card result = mapper.toDomain(dto);
            assertThat(result).isInstanceOf(PokemonCard.class);
        }

        @Test
        @DisplayName("'Trainer' → TrainerCard subclass")
        void routesToTrainer() {
            CardApiDTO dto = trainerDto("xy1-100", "Potion", List.of("Item"));
            Card result = mapper.toDomain(dto);
            assertThat(result).isInstanceOf(ItemCard.class);
        }

        @Test
        @DisplayName("'Energy' → EnergyCard")
        void routesToEnergy() {
            CardApiDTO dto = energyDto("xy1-132", "Grass Energy", List.of("Grass"));
            Card result = mapper.toDomain(dto);
            assertThat(result).isInstanceOf(EnergyCard.class);
        }

        @Test
        @DisplayName("supertype desconocido → IllegalArgumentException")
        void unknownSupertypeThrows() {
            CardApiDTO dto = mock(CardApiDTO.class);
            when(dto.getSupertype()).thenReturn("Wizard");

            assertThatThrownBy(() -> mapper.toDomain(dto))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Unknown supertype")
                    .hasMessageContaining("Wizard");
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Pokémon mapping — happy path + null-branch fallbacks
    // ─────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("mapPokemon() — caso completo y fallbacks")
    class PokemonMappingTests {

        @Test
        @DisplayName("EX Basic con todos los campos poblados")
        void mapsFullEXPokemon() {
            AttackDTO atk = attackDto("Poison Powder", List.of("Grass", "Colorless", "Colorless"),
                    "60", "Opponent is now Poisoned.");
            WeaknessDTO wk = weaknessDto("Fire", "×2");
            ResistanceDTO rs = resistanceDto("Water", "-20");

            CardApiDTO dto = mock(CardApiDTO.class);
            when(dto.getSupertype()).thenReturn("Pokémon");
            when(dto.getId()).thenReturn("xy1-1");
            when(dto.getName()).thenReturn("Venusaur-EX");
            when(dto.getHp()).thenReturn("180");
            when(dto.getSubtypes()).thenReturn(List.of("Basic", "EX"));
            when(dto.getTypes()).thenReturn(List.of("Grass"));
            when(dto.getAttacks()).thenReturn(List.of(atk));
            when(dto.getWeaknesses()).thenReturn(List.of(wk));
            when(dto.getResistances()).thenReturn(List.of(rs));
            when(dto.getRetreatCost()).thenReturn(List.of("Colorless", "Colorless", "Colorless", "Colorless"));
            when(dto.getEvolvesFrom()).thenReturn(null);
            when(dto.getEvolvesTo()).thenReturn(List.of("M Venusaur-EX"));
            when(dto.getRules()).thenReturn(List.of("Pokémon-EX rule"));

            Card result = mapper.toDomain(dto);

            assertThat(result).isInstanceOf(PokemonCard.class);
            PokemonCard p = (PokemonCard) result;
            assertThat(p.getId()).isEqualTo("xy1-1");
            assertThat(p.getName()).isEqualTo("Venusaur-EX");
            assertThat(p.getHp()).isEqualTo(180);
            assertThat(p.getStage()).isEqualTo(PokemonStage.BASIC);
            assertThat(p.isEx()).isTrue();
            assertThat(p.isMega()).isFalse();
            assertThat(p.getPokemonType()).isEqualTo(EnergyType.GRASS);
            assertThat(p.getAttacks()).hasSize(1);
            assertThat(p.getWeaknesses()).hasSize(1);
            assertThat(p.getResistances()).hasSize(1);
            assertThat(p.getRetreatCost()).isEqualTo(4);
            assertThat(p.getEvolvesFrom()).isNull();
            assertThat(p.getEvolvesTo()).containsExactly("M Venusaur-EX");
            assertThat(p.getRules()).containsExactly("Pokémon-EX rule");
            assertThat(p.getSupertype()).isEqualTo(Supertype.POKEMON);
        }

        @Test
        @DisplayName("MEGA Pokémon → isMega=true, isEx=false (sólo el flag MEGA)")
        void detectsMegaFlag() {
            CardApiDTO dto = pokemonDtoMinimal("xy1-2", "M Venusaur-EX", "230", List.of("Mega", "MEGA"));
            PokemonCard p = (PokemonCard) mapper.toDomain(dto);
            assertThat(p.isMega()).isTrue();
            assertThat(p.isEx()).isFalse();
            assertThat(p.getStage()).isEqualTo(PokemonStage.MEGA);
        }

        @Test
        @DisplayName("retreatCost=null → 0 (no se cae)")
        void retreatCostNullDefaultsToZero() {
            CardApiDTO dto = pokemonDtoMinimal("xy1-x", "X", "60", List.of("Basic"));
            when(dto.getRetreatCost()).thenReturn(null);
            PokemonCard p = (PokemonCard) mapper.toDomain(dto);
            assertThat(p.getRetreatCost()).isEqualTo(0);
        }

        @Test
        @DisplayName("evolvesTo=null y rules=null → listas vacías (no NPE)")
        void nullEvolvesToAndRulesDefaultToEmpty() {
            CardApiDTO dto = pokemonDtoMinimal("xy1-x", "X", "60", List.of("Basic"));
            when(dto.getEvolvesTo()).thenReturn(null);
            when(dto.getRules()).thenReturn(null);
            PokemonCard p = (PokemonCard) mapper.toDomain(dto);
            assertThat(p.getEvolvesTo()).isEmpty();
            assertThat(p.getRules()).isEmpty();
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Abilities mapping — registry interaction and field propagation
    // ─────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("mapAbilities() — mapea name/text/type y consulta registry por índice")
    class AbilitiesMappingTests {

        @Test
        @DisplayName("abilities=null → lista vacía y registry nunca se invoca")
        void nullAbilitiesYieldsEmptyList() {
            CardApiDTO dto = pokemonDtoMinimal("xy1-x", "X", "60", List.of("Basic"));
            when(dto.getAbilities()).thenReturn(null);

            PokemonCard p = (PokemonCard) mapper.toDomain(dto);

            assertThat(p.getAbilities()).isEmpty();
            org.mockito.Mockito.verify(abilityRegistry, org.mockito.Mockito.never())
                    .getAbilityEffect(org.mockito.ArgumentMatchers.anyString(),
                            org.mockito.ArgumentMatchers.anyInt());
        }

        @Test
        @DisplayName("Mapea name/text y propaga type PASSIVE (v1)")
        void mapsAbilityFields() {
            ar.edu.utn.frc.tup.piii.dtos.AbilityDTO ability = abilityDto(
                    "Chlorophyll", "Your Grass Pokémon's attacks do 10 more damage.", "Ability");
            ar.edu.utn.frc.tup.piii.models.AbilityEffect noOp =
                    ar.edu.utn.frc.tup.piii.models.effects.AbilityRegistry.NO_OP;
            when(abilityRegistry.getAbilityEffect("xy1-1", 0)).thenReturn(noOp);

            CardApiDTO dto = pokemonDtoMinimal("xy1-1", "Venusaur-EX", "180", List.of("Basic"));
            when(dto.getAbilities()).thenReturn(java.util.Collections.singletonList(ability));

            PokemonCard p = (PokemonCard) mapper.toDomain(dto);

            assertThat(p.getAbilities()).hasSize(1);
            assertThat(p.getAbilities().get(0).getName()).isEqualTo("Chlorophyll");
            assertThat(p.getAbilities().get(0).getDescription())
                    .isEqualTo("Your Grass Pokémon's attacks do 10 more damage.");
            assertThat(p.getAbilities().get(0).getType())
                    .isEqualTo(ar.edu.utn.frc.tup.piii.models.AbilityType.PASSIVE);
            org.mockito.Mockito.verify(abilityRegistry)
                    .getAbilityEffect("xy1-1", 0);
        }

        @Test
        @DisplayName("Múltiples abilities: cada uno consulta registry con su índice")
        void registryIsQueriedPerAbilityIndex() {
            ar.edu.utn.frc.tup.piii.models.AbilityEffect effect0 =
                    ar.edu.utn.frc.tup.piii.models.effects.AbilityRegistry.NO_OP;
            ar.edu.utn.frc.tup.piii.models.AbilityEffect effect1 = (ctx, holder, a) -> { };
            when(abilityRegistry.getAbilityEffect("xy1-1", 0)).thenReturn(effect0);
            when(abilityRegistry.getAbilityEffect("xy1-1", 1)).thenReturn(effect1);

            ar.edu.utn.frc.tup.piii.dtos.AbilityDTO a0 = abilityDto("A0", "t0", "Ability");
            ar.edu.utn.frc.tup.piii.dtos.AbilityDTO a1 = abilityDto("A1", "t1", "Ability");

            CardApiDTO dto = pokemonDtoMinimal("xy1-1", "X", "60", List.of("Basic"));
            when(dto.getAbilities()).thenReturn(List.of(a0, a1));

            PokemonCard p = (PokemonCard) mapper.toDomain(dto);

            assertThat(p.getAbilities()).hasSize(2);
            assertThat(p.getAbilities().get(0).getName()).isEqualTo("A0");
            assertThat(p.getAbilities().get(1).getName()).isEqualTo("A1");
            org.mockito.Mockito.verify(abilityRegistry).getAbilityEffect("xy1-1", 0);
            org.mockito.Mockito.verify(abilityRegistry).getAbilityEffect("xy1-1", 1);
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // PokemonStage derivation
    // ─────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("deriveStage() — todas las etapas y caso desconocido")
    class PokemonStageTests {

        @Test
        @DisplayName("'Basic' → BASIC")
        void basicStage() {
            CardApiDTO dto = pokemonDtoMinimal("id", "n", "60", List.of("Basic"));
            assertThat(((PokemonCard) mapper.toDomain(dto)).getStage()).isEqualTo(PokemonStage.BASIC);
        }

        @Test
        @DisplayName("'Stage 1' → STAGE_1")
        void stage1() {
            CardApiDTO dto = pokemonDtoMinimal("id", "n", "60", List.of("Stage 1"));
            assertThat(((PokemonCard) mapper.toDomain(dto)).getStage()).isEqualTo(PokemonStage.STAGE_1);
        }

        @Test
        @DisplayName("'Stage 2' → STAGE_2")
        void stage2() {
            CardApiDTO dto = pokemonDtoMinimal("id", "n", "60", List.of("Stage 2"));
            assertThat(((PokemonCard) mapper.toDomain(dto)).getStage()).isEqualTo(PokemonStage.STAGE_2);
        }

        @Test
        @DisplayName("'Mega' → MEGA")
        void megaStage() {
            CardApiDTO dto = pokemonDtoMinimal("id", "n", "60", List.of("Mega"));
            assertThat(((PokemonCard) mapper.toDomain(dto)).getStage()).isEqualTo(PokemonStage.MEGA);
        }

        @Test
        @DisplayName("Subtype sin etapa conocida → IllegalStateException")
        void unknownStageThrows() {
            CardApiDTO dto = pokemonDtoMinimal("id", "n", "60", List.of("Wildcard"));
            assertThatThrownBy(() -> mapper.toDomain(dto))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("Unknown stage");
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // EnergyType mapping — los 11 enums + tipo desconocido
    // ─────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("mapEnergyType() — todos los tipos vía derivePrimaryType")
    class EnergyTypeMappingTests {

        @Test
        @DisplayName("types=null → pokemonType=null")
        void nullTypesYieldNull() {
            CardApiDTO dto = pokemonDtoMinimal("id", "n", "60", List.of("Basic"));
            when(dto.getTypes()).thenReturn(null);
            PokemonCard p = (PokemonCard) mapper.toDomain(dto);
            assertThat(p.getPokemonType()).isNull();
        }

        @Test
        @DisplayName("types=[] → pokemonType=null")
        void emptyTypesYieldNull() {
            CardApiDTO dto = pokemonDtoMinimal("id", "n", "60", List.of("Basic"));
            when(dto.getTypes()).thenReturn(List.of());
            PokemonCard p = (PokemonCard) mapper.toDomain(dto);
            assertThat(p.getPokemonType()).isNull();
        }

        @Test
        @DisplayName("Todos los nombres de energía mapean correctamente")
        void allKnownEnergyTypes() {
            assertEnergyMapping("Grass", EnergyType.GRASS);
            assertEnergyMapping("Fire", EnergyType.FIRE);
            assertEnergyMapping("Water", EnergyType.WATER);
            assertEnergyMapping("Lightning", EnergyType.LIGHTNING);
            assertEnergyMapping("Psychic", EnergyType.PSYCHIC);
            assertEnergyMapping("Fighting", EnergyType.FIGHTING);
            assertEnergyMapping("Darkness", EnergyType.DARKNESS);
            assertEnergyMapping("Metal", EnergyType.METAL);
            assertEnergyMapping("Fairy", EnergyType.FAIRY);
            assertEnergyMapping("Dragon", EnergyType.DRAGON);
            assertEnergyMapping("Colorless", EnergyType.COLORLESS);
        }

        @Test
        @DisplayName("Nombre de energía desconocido → IllegalArgumentException")
        void unknownEnergyTypeThrows() {
            CardApiDTO dto = pokemonDtoMinimal("id", "n", "60", List.of("Basic"));
            when(dto.getTypes()).thenReturn(List.of("Plasma"));

            assertThatThrownBy(() -> mapper.toDomain(dto))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Unknown energy type")
                    .hasMessageContaining("Plasma");
        }

        private void assertEnergyMapping(String typeName, EnergyType expected) {
            CardApiDTO dto = pokemonDtoMinimal("id", "n", "60", List.of("Basic"));
            when(dto.getTypes()).thenReturn(List.of(typeName));
            PokemonCard p = (PokemonCard) mapper.toDomain(dto);
            assertThat(p.getPokemonType())
                    .as("Mapping for '%s'", typeName)
                    .isEqualTo(expected);
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Attacks mapping + registry interaction + damage parsing
    // ─────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("mapAttacks() — incluye registry interaction y parseo de daño")
    class AttacksMappingTests {

        @Test
        @DisplayName("attacks=null → lista vacía y registry nunca se invoca")
        void nullAttacksYieldsEmptyList() {
            CardApiDTO dto = pokemonDtoMinimal("xy1-x", "X", "60", List.of("Basic"));
            when(dto.getAttacks()).thenReturn(null);

            PokemonCard p = (PokemonCard) mapper.toDomain(dto);

            assertThat(p.getAttacks()).isEmpty();
            verify(effectRegistry, never()).getAttackEffect(anyStringMatcher(), anyIntMatcher());
        }

        @Test
        @DisplayName("Múltiples ataques: cada uno pide al registry con su índice")
        void registryIsQueriedPerAttackIndex() {
            AttackEffect effect0 = ctx -> { };
            AttackEffect effect1 = ctx -> { };
            when(effectRegistry.getAttackEffect("xy1-1", 0)).thenReturn(effect0);
            when(effectRegistry.getAttackEffect("xy1-1", 1)).thenReturn(effect1);

            AttackDTO a0 = attackDto("A0", List.of("Grass"), "10", "t0");
            AttackDTO a1 = attackDto("A1", List.of("Fire"), "20", "t1");

            CardApiDTO dto = pokemonDtoMinimal("xy1-1", "X", "60", List.of("Basic"));
            when(dto.getAttacks()).thenReturn(List.of(a0, a1));

            PokemonCard p = (PokemonCard) mapper.toDomain(dto);

            assertThat(p.getAttacks()).hasSize(2);
            assertThat(p.getAttacks().get(0).getEffect()).isSameAs(effect0);
            assertThat(p.getAttacks().get(1).getEffect()).isSameAs(effect1);
            verify(effectRegistry, times(1)).getAttackEffect("xy1-1", 0);
            verify(effectRegistry, times(1)).getAttackEffect("xy1-1", 1);
        }

        @Test
        @DisplayName("countEnergyCost() — agrupa repetidos por tipo")
        void energyCostGroupsByType() {
            AttackDTO a = attackDto("Mix", List.of("Grass", "Colorless", "Colorless"), "30", "t");
            CardApiDTO dto = pokemonDtoMinimal("xy1-x", "X", "60", List.of("Basic"));
            when(dto.getAttacks()).thenReturn(List.of(a));

            PokemonCard p = (PokemonCard) mapper.toDomain(dto);
            assertThat(p.getAttacks().get(0).getEnergyCost())
                    .containsEntry(EnergyType.GRASS, 1)
                    .containsEntry(EnergyType.COLORLESS, 2)
                    .hasSize(2);
        }

        @Test
        @DisplayName("countEnergyCost() — cost=null → mapa vacío")
        void energyCostNullYieldsEmptyMap() {
            AttackDTO a = attackDto("Free", null, "30", "t");
            CardApiDTO dto = pokemonDtoMinimal("xy1-x", "X", "60", List.of("Basic"));
            when(dto.getAttacks()).thenReturn(List.of(a));

            PokemonCard p = (PokemonCard) mapper.toDomain(dto);
            assertThat(p.getAttacks().get(0).getEnergyCost()).isEmpty();
        }

        @Test
        @DisplayName("parseDamage() — damage 'null' → 0, blank → 0, plus → 30, plain → 60")
        void parseDamageVariants() {
            assertDamageParsed(null, 0, DamageModifier.NONE);
            assertDamageParsed("", 0, DamageModifier.NONE);
            assertDamageParsed("60", 60, DamageModifier.NONE);
            assertDamageParsed("30+", 30, DamageModifier.PLUS);
            assertDamageParsed("20×", 20, DamageModifier.TIMES);
            assertDamageParsed("20x", 20, DamageModifier.TIMES);
            assertDamageParsed("40-", 40, DamageModifier.MINUS);
        }

        @Test
        @DisplayName("parseDamage() — string solo con símbolo → 0")
        void parseDamageOnlySymbol() {
            assertDamageParsed("+", 0, DamageModifier.PLUS);
        }

        private void assertDamageParsed(String raw, int expectedBase, DamageModifier expectedMod) {
            AttackDTO a = attackDto("Atk", List.of("Grass"), raw, "t");
            CardApiDTO dto = pokemonDtoMinimal("xy1-x", "X", "60", List.of("Basic"));
            when(dto.getAttacks()).thenReturn(List.of(a));

            PokemonCard p = (PokemonCard) mapper.toDomain(dto);

            assertThat(p.getAttacks().get(0).getBaseDamage())
                    .as("baseDamage parsed from '%s'", raw)
                    .isEqualTo(expectedBase);
            assertThat(p.getAttacks().get(0).getDamageModifier())
                    .as("modifier parsed from '%s'", raw)
                    .isEqualTo(expectedMod);
        }

        // local matchers reused inside this nested class
        private String anyStringMatcher() { return org.mockito.ArgumentMatchers.anyString(); }
        private int    anyIntMatcher()    { return org.mockito.ArgumentMatchers.anyInt(); }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Weaknesses
    // ─────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("mapWeaknesses() — null y valores poblados")
    class WeaknessesMappingTests {

        @Test
        @DisplayName("weaknesses=null → lista vacía")
        void nullYieldsEmpty() {
            CardApiDTO dto = pokemonDtoMinimal("id", "n", "60", List.of("Basic"));
            when(dto.getWeaknesses()).thenReturn(null);
            PokemonCard p = (PokemonCard) mapper.toDomain(dto);
            assertThat(p.getWeaknesses()).isEmpty();
        }

        @Test
        @DisplayName("Varias debilidades: guarda type+value crudo")
        void mapsMultipleWeaknesses() {
            WeaknessDTO w1 = weaknessDto("Fire", "×2");
            WeaknessDTO w2 = weaknessDto("Psychic", "×2");
            CardApiDTO dto = pokemonDtoMinimal("id", "n", "60", List.of("Basic"));
            when(dto.getWeaknesses()).thenReturn(List.of(w1, w2));

            PokemonCard p = (PokemonCard) mapper.toDomain(dto);

            assertThat(p.getWeaknesses()).hasSize(2);
            assertThat(p.getWeaknesses().get(0).getType()).isEqualTo(EnergyType.FIRE);
            assertThat(p.getWeaknesses().get(0).getValue()).isEqualTo("×2");
            assertThat(p.getWeaknesses().get(1).getType()).isEqualTo(EnergyType.PSYCHIC);
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Resistances
    // ─────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("mapResistances() — null y parseo del valor numérico")
    class ResistancesMappingTests {

        @Test
        @DisplayName("resistances=null → lista vacía")
        void nullYieldsEmpty() {
            CardApiDTO dto = pokemonDtoMinimal("id", "n", "60", List.of("Basic"));
            when(dto.getResistances()).thenReturn(null);
            PokemonCard p = (PokemonCard) mapper.toDomain(dto);
            assertThat(p.getResistances()).isEmpty();
        }

        @Test
        @DisplayName("'-20' → reduction=20")
        void parsesNegativeStringValue() {
            ResistanceDTO r = resistanceDto("Water", "-20");
            CardApiDTO dto = pokemonDtoMinimal("id", "n", "60", List.of("Basic"));
            when(dto.getResistances()).thenReturn(List.of(r));

            PokemonCard p = (PokemonCard) mapper.toDomain(dto);
            assertThat(p.getResistances()).hasSize(1);
            assertThat(p.getResistances().get(0).getType()).isEqualTo(EnergyType.WATER);
            assertThat(p.getResistances().get(0).getReduction()).isEqualTo(20);
        }

        @Test
        @DisplayName("value=null → reduction=0 (no NPE)")
        void nullValueYieldsZero() {
            ResistanceDTO r = resistanceDto("Water", null);
            CardApiDTO dto = pokemonDtoMinimal("id", "n", "60", List.of("Basic"));
            when(dto.getResistances()).thenReturn(List.of(r));

            PokemonCard p = (PokemonCard) mapper.toDomain(dto);
            assertThat(p.getResistances().get(0).getReduction()).isEqualTo(0);
        }

        @Test
        @DisplayName("value blank → reduction=0")
        void blankValueYieldsZero() {
            ResistanceDTO r = resistanceDto("Water", "   ");
            CardApiDTO dto = pokemonDtoMinimal("id", "n", "60", List.of("Basic"));
            when(dto.getResistances()).thenReturn(List.of(r));

            PokemonCard p = (PokemonCard) mapper.toDomain(dto);
            assertThat(p.getResistances().get(0).getReduction()).isEqualTo(0);
        }

        @Test
        @DisplayName("value sin dígitos → reduction=0")
        void noDigitsYieldsZero() {
            ResistanceDTO r = resistanceDto("Water", "---");
            CardApiDTO dto = pokemonDtoMinimal("id", "n", "60", List.of("Basic"));
            when(dto.getResistances()).thenReturn(List.of(r));

            PokemonCard p = (PokemonCard) mapper.toDomain(dto);
            assertThat(p.getResistances().get(0).getReduction()).isEqualTo(0);
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Trainer mapping
    // ─────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("mapTrainer() — todos los subtipos")
    class TrainerMappingTests {

        @Test
        @DisplayName("'Item' → ItemCard")
        void mapsItem() {
            Card c = mapper.toDomain(trainerDto("xy1-100", "Potion", List.of("Item")));
            assertThat(c).isInstanceOf(ItemCard.class);
            assertThat(c.getId()).isEqualTo("xy1-100");
            assertThat(c.getName()).isEqualTo("Potion");
        }

        @Test
        @DisplayName("'Supporter' → SupporterCard")
        void mapsSupporter() {
            Card c = mapper.toDomain(trainerDto("xy1-122", "Professor Sycamore", List.of("Supporter")));
            assertThat(c).isInstanceOf(SupporterCard.class);
        }

        @Test
        @DisplayName("'Stadium' → StadiumCard")
        void mapsStadium() {
            Card c = mapper.toDomain(trainerDto("xy1-140", "Fairy Garden", List.of("Stadium")));
            assertThat(c).isInstanceOf(StadiumCard.class);
        }

        @Test
        @DisplayName("'Pokémon Tool' → PokemonToolCard")
        void mapsPokemonTool() {
            Card c = mapper.toDomain(trainerDto("xy1-141", "Muscle Band", List.of("Pokémon Tool")));
            assertThat(c).isInstanceOf(PokemonToolCard.class);
        }

        @Test
        @DisplayName("'ACE SPEC' (precedencia sobre Item) → AceSpecCard")
        void aceSpecHasPrecedenceOverItem() {
            Card c = mapper.toDomain(trainerDto("xy1-200", "Computer Search", List.of("Item", "ACE SPEC")));
            assertThat(c).isInstanceOf(AceSpecCard.class);
        }

        @Test
        @DisplayName("rules=null → lista vacía")
        void nullRulesDefaultsToEmpty() {
            CardApiDTO dto = trainerDto("xy1-100", "Potion", List.of("Item"));
            when(dto.getRules()).thenReturn(null);
            ItemCard item = (ItemCard) mapper.toDomain(dto);
            assertThat(item.getRules()).isEmpty();
        }

        @Test
        @DisplayName("subtypes=null → IllegalStateException")
        void nullSubtypesThrows() {
            CardApiDTO dto = trainerDto("xy1-x", "X", null);
            assertThatThrownBy(() -> mapper.toDomain(dto))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("no subtypes");
        }

        @Test
        @DisplayName("subtypes sin ningún tipo conocido → IllegalStateException")
        void unknownSubtypeThrows() {
            CardApiDTO dto = trainerDto("xy1-x", "X", List.of("Mystery"));
            assertThatThrownBy(() -> mapper.toDomain(dto))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("Unknown trainer subtype");
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Energy mapping
    // ─────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("mapEnergy() — basic vs special + fallbacks")
    class EnergyMappingTests {

        @Test
        @DisplayName("Basic Energy con types poblados → usa el campo types")
        void basicEnergyUsesTypesField() {
            CardApiDTO dto = energyDto("xy1-132", "Grass Energy", List.of("Grass"));
            EnergyCard e = (EnergyCard) mapper.toDomain(dto);
            assertThat(e.getEnergyType()).isEqualTo(EnergyType.GRASS);
            assertThat(e.getName()).isEqualTo("Grass Energy");
        }

        @Test
        @DisplayName("Special Energy sin types → fallback al nombre")
        void specialEnergyFallsBackToName() {
            CardApiDTO dto = energyDto("xy1-130", "Double Colorless Energy", null);
            EnergyCard e = (EnergyCard) mapper.toDomain(dto);
            assertThat(e.getEnergyType()).isEqualTo(EnergyType.COLORLESS);
        }

        @Test
        @DisplayName("types=[] (vacío) → cae al fallback del nombre")
        void emptyTypesFallsBackToName() {
            CardApiDTO dto = energyDto("xy1-200", "Fire Splash Energy", List.of());
            EnergyCard e = (EnergyCard) mapper.toDomain(dto);
            assertThat(e.getEnergyType()).isEqualTo(EnergyType.FIRE);
        }

        @Test
        @DisplayName("Sin types y nombre sin palabra clave → energyType=null")
        void unresolvableEnergyYieldsNullType() {
            CardApiDTO dto = energyDto("xy1-x", "Mystery Source", null);
            EnergyCard e = (EnergyCard) mapper.toDomain(dto);
            assertThat(e.getEnergyType()).isNull();
        }

        @Test
        @DisplayName("Sin types y nombre=null → energyType=null")
        void nullNameYieldsNullType() {
            CardApiDTO dto = mock(CardApiDTO.class);
            when(dto.getSupertype()).thenReturn("Energy");
            when(dto.getId()).thenReturn("xy1-x");
            when(dto.getName()).thenReturn(null);
            when(dto.getTypes()).thenReturn(null);
            when(dto.getRules()).thenReturn(null);

            EnergyCard e = (EnergyCard) mapper.toDomain(dto);
            assertThat(e.getEnergyType()).isNull();
            assertThat(e.getRules()).isEmpty();
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Edge cases — entradas inusuales, nulls anidados, prioridades
    // ─────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("Edge cases — entradas nulas, valores límite y prioridades")
    class EdgeCasesTests {

        // ── 1. null supertype en el switch principal ────────────────────────
        @Test
        @DisplayName("supertype=null → IllegalArgumentException (normalize lo mapea al default)")
        void nullSupertypeThrowsIllegalArgument() {
            CardApiDTO dto = mock(CardApiDTO.class);
            when(dto.getSupertype()).thenReturn(null);

            assertThatThrownBy(() -> mapper.toDomain(dto))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Unknown supertype");
        }

        // ── 2. hp inválido en Pokémon ───────────────────────────────────────
        @Test
        @DisplayName("hp='abc' → NumberFormatException")
        void invalidHpStringThrows() {
            CardApiDTO dto = pokemonDtoMinimal("xy1-x", "X", "abc", List.of("Basic"));
            assertThatThrownBy(() -> mapper.toDomain(dto))
                    .isInstanceOf(NumberFormatException.class);
        }

        @Test
        @DisplayName("hp=null → NumberFormatException (parseInt no acepta null)")
        void nullHpThrows() {
            CardApiDTO dto = pokemonDtoMinimal("xy1-x", "X", null, List.of("Basic"));
            assertThatThrownBy(() -> mapper.toDomain(dto))
                    .isInstanceOf(NumberFormatException.class);
        }

        // ── 3. null anidado en Weakness ─────────────────────────────────────
        @Test
        @DisplayName("Weakness con type=null → NPE en switch de mapEnergyType")
        void weaknessWithNullTypeThrowsNPE() {
            WeaknessDTO bad = weaknessDto(null, "×2");
            CardApiDTO dto = pokemonDtoMinimal("id", "n", "60", List.of("Basic"));
            when(dto.getWeaknesses()).thenReturn(List.of(bad));

            assertThatThrownBy(() -> mapper.toDomain(dto))
                    .isInstanceOf(NullPointerException.class);
        }

        // ── 4. Energy type desconocido en cost de ataque ────────────────────
        @Test
        @DisplayName("Attack cost con energía desconocida → IllegalArgumentException")
        void attackCostWithUnknownEnergyThrows() {
            AttackDTO atk = attackDto("Plasma Blast", List.of("Plasma"), "30", "t");
            CardApiDTO dto = pokemonDtoMinimal("xy1-x", "X", "60", List.of("Basic"));
            when(dto.getAttacks()).thenReturn(List.of(atk));

            assertThatThrownBy(() -> mapper.toDomain(dto))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Unknown energy type");
        }

        // ── 5. Múltiples stages: el primero del código gana ─────────────────
        @Test
        @DisplayName("subtypes=['Stage 1','Basic'] → BASIC gana (orden de chequeo en código)")
        void multipleStagesPicksFirstMatchInCodeOrder() {
            // Aunque el JSON pase "Stage 1" antes que "Basic", deriveStage chequea
            // primero "Basic" en su cadena de if's, por lo tanto BASIC gana.
            CardApiDTO dto = pokemonDtoMinimal("id", "n", "60", List.of("Stage 1", "Basic"));
            PokemonCard p = (PokemonCard) mapper.toDomain(dto);
            assertThat(p.getStage()).isEqualTo(PokemonStage.BASIC);
        }

        // ── 6. Trainer con subtypes vacío (distinto de null) ────────────────
        @Test
        @DisplayName("Trainer con subtypes=[] (vacío) → IllegalStateException 'Unknown trainer subtype'")
        void emptyTrainerSubtypesThrows() {
            CardApiDTO dto = trainerDto("xy1-x", "X", List.of());
            assertThatThrownBy(() -> mapper.toDomain(dto))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("Unknown trainer subtype");
        }

        // ── 7. Prioridad de subtipos Trainer ────────────────────────────────
        @Test
        @DisplayName("ACE SPEC > Pokémon Tool > Stadium > Supporter > Item")
        void aceSpecHasHighestPriority() {
            CardApiDTO dto = trainerDto("xy1-x", "ChaosCard",
                    List.of("Item", "Supporter", "Stadium", "Pokémon Tool", "ACE SPEC"));
            assertThat(mapper.toDomain(dto)).isInstanceOf(AceSpecCard.class);
        }

        @Test
        @DisplayName("Sin ACE SPEC, Pokémon Tool gana sobre Stadium/Supporter/Item")
        void pokemonToolHasPrecedenceOverLowerSubtypes() {
            CardApiDTO dto = trainerDto("xy1-x", "X",
                    List.of("Item", "Supporter", "Stadium", "Pokémon Tool"));
            assertThat(mapper.toDomain(dto)).isInstanceOf(PokemonToolCard.class);
        }

        @Test
        @DisplayName("Sólo Stadium + Supporter + Item → Stadium gana")
        void stadiumHasPrecedenceOverSupporterAndItem() {
            CardApiDTO dto = trainerDto("xy1-x", "X", List.of("Item", "Supporter", "Stadium"));
            assertThat(mapper.toDomain(dto)).isInstanceOf(StadiumCard.class);
        }

        @Test
        @DisplayName("Supporter + Item → Supporter gana")
        void supporterHasPrecedenceOverItem() {
            CardApiDTO dto = trainerDto("xy1-x", "X", List.of("Item", "Supporter"));
            assertThat(mapper.toDomain(dto)).isInstanceOf(SupporterCard.class);
        }

        // ── 8. Energy con nombre que contiene varios keywords ───────────────
        @Test
        @DisplayName("Energy 'Fire and Water Energy' → Fire (primer keyword del array)")
        void energyNameWithMultipleKeywordsPicksFirstInKeywordOrder() {
            // El array de keywords es: Grass, Fire, Water, Lightning, ...
            // El nombre contiene Fire y Water — Fire aparece primero en el array.
            CardApiDTO dto = energyDto("xy1-x", "Fire and Water Energy", null);
            EnergyCard e = (EnergyCard) mapper.toDomain(dto);
            assertThat(e.getEnergyType()).isEqualTo(EnergyType.FIRE);
        }

        // ── 9. Case sensitivity ─────────────────────────────────────────────
        @Test
        @DisplayName("Energy con nombre en mayúsculas → no matchea (case-sensitive) → null")
        void energyNameMatchingIsCaseSensitive() {
            CardApiDTO dto = energyDto("xy1-x", "FIRE ENERGY", null);
            EnergyCard e = (EnergyCard) mapper.toDomain(dto);
            assertThat(e.getEnergyType()).isNull();
        }

        @Test
        @DisplayName("types=['grass'] (lowercase) → IllegalArgumentException")
        void energyTypeStringIsCaseSensitive() {
            CardApiDTO dto = pokemonDtoMinimal("id", "n", "60", List.of("Basic"));
            when(dto.getTypes()).thenReturn(List.of("grass"));
            assertThatThrownBy(() -> mapper.toDomain(dto))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Unknown energy type");
        }

        // ── 10. Registry devuelve null para un ataque existente ─────────────
        @Test
        @DisplayName("Registry devuelve null → el Attack se construye con effect=null")
        void registryReturningNullPropagatesAsNullEffect() {
            when(effectRegistry.getAttackEffect("xy1-x", 0)).thenReturn(null);

            AttackDTO a = attackDto("Mysterious Move", List.of("Grass"), "10", "no effect known");
            CardApiDTO dto = pokemonDtoMinimal("xy1-x", "X", "60", List.of("Basic"));
            when(dto.getAttacks()).thenReturn(List.of(a));

            PokemonCard p = (PokemonCard) mapper.toDomain(dto);
            assertThat(p.getAttacks()).hasSize(1);
            assertThat(p.getAttacks().get(0).getEffect()).isNull();
            assertThat(p.getAttacks().get(0).getName()).isEqualTo("Mysterious Move");
        }

        // ── 11. Lista de attacks vacía (no null) ────────────────────────────
        @Test
        @DisplayName("attacks=[] (vacío) → resultado vacío y registry nunca llamado")
        void emptyAttacksListYieldsEmptyResultAndNoRegistryCalls() {
            CardApiDTO dto = pokemonDtoMinimal("xy1-x", "X", "60", List.of("Basic"));
            when(dto.getAttacks()).thenReturn(List.of());

            PokemonCard p = (PokemonCard) mapper.toDomain(dto);

            assertThat(p.getAttacks()).isEmpty();
            verify(effectRegistry, never()).getAttackEffect(
                    org.mockito.ArgumentMatchers.anyString(),
                    org.mockito.ArgumentMatchers.anyInt());
        }

        // ── 12. Energy rules=null ───────────────────────────────────────────
        @Test
        @DisplayName("Energy con rules=null → lista vacía (no NPE)")
        void energyNullRulesDefaultsToEmpty() {
            CardApiDTO dto = energyDto("xy1-132", "Grass Energy", List.of("Grass"));
            when(dto.getRules()).thenReturn(null);
            EnergyCard e = (EnergyCard) mapper.toDomain(dto);
            assertThat(e.getRules()).isEmpty();
        }

        // ── 13. parseDamage con dígitos embebidos en texto ──────────────────
        @Test
        @DisplayName("damage='abc12def' → extrae sólo dígitos → 12 (modifier NONE)")
        void parseDamageExtractsDigitsFromMixedString() {
            AttackDTO a = attackDto("Weird", List.of("Grass"), "abc12def", "t");
            CardApiDTO dto = pokemonDtoMinimal("xy1-x", "X", "60", List.of("Basic"));
            when(dto.getAttacks()).thenReturn(List.of(a));

            PokemonCard p = (PokemonCard) mapper.toDomain(dto);
            assertThat(p.getAttacks().get(0).getBaseDamage()).isEqualTo(12);
            assertThat(p.getAttacks().get(0).getDamageModifier()).isEqualTo(DamageModifier.NONE);
        }

        // ── 14. parseDamageModifier con string vacío ────────────────────────
        @Test
        @DisplayName("damage='' (string vacío, no null) → baseDamage=0, modifier=NONE")
        void parseDamageEmptyStringYieldsZeroAndNone() {
            AttackDTO a = attackDto("Empty", List.of("Grass"), "", "t");
            CardApiDTO dto = pokemonDtoMinimal("xy1-x", "X", "60", List.of("Basic"));
            when(dto.getAttacks()).thenReturn(List.of(a));

            PokemonCard p = (PokemonCard) mapper.toDomain(dto);
            assertThat(p.getAttacks().get(0).getBaseDamage()).isEqualTo(0);
            assertThat(p.getAttacks().get(0).getDamageModifier()).isEqualTo(DamageModifier.NONE);
        }

        // ── 15. Múltiples weaknesses y resistances simultáneamente ──────────
        @Test
        @DisplayName("Pokémon con varias weaknesses y resistances mantiene todas")
        void multipleWeaknessesAndResistancesPreserved() {
            // Mocks creados como variables locales para evitar el clásico
            // UnfinishedStubbing de Mockito al anidar mocks dentro de un thenReturn().
            WeaknessDTO w1 = weaknessDto("Fire", "×2");
            WeaknessDTO w2 = weaknessDto("Lightning", "×2");
            ResistanceDTO r1 = resistanceDto("Water", "-20");
            ResistanceDTO r2 = resistanceDto("Metal", "-30");

            CardApiDTO dto = pokemonDtoMinimal("id", "n", "60", List.of("Basic"));
            when(dto.getWeaknesses()).thenReturn(List.of(w1, w2));
            when(dto.getResistances()).thenReturn(List.of(r1, r2));

            PokemonCard p = (PokemonCard) mapper.toDomain(dto);
            assertThat(p.getWeaknesses()).hasSize(2);
            assertThat(p.getResistances()).hasSize(2);
            assertThat(p.getResistances().get(1).getReduction()).isEqualTo(30);
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Test fixture helpers
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Builds a minimal but valid Pokémon mock DTO with all required getters stubbed.
     * Individual tests may override specific getters after creation.
     */
    private CardApiDTO pokemonDtoMinimal(String id, String name, String hp, List<String> subtypes) {
        CardApiDTO dto = mock(CardApiDTO.class);
        lenient().when(dto.getSupertype()).thenReturn("Pokémon");
        lenient().when(dto.getId()).thenReturn(id);
        lenient().when(dto.getName()).thenReturn(name);
        lenient().when(dto.getHp()).thenReturn(hp);
        lenient().when(dto.getSubtypes()).thenReturn(subtypes);
        lenient().when(dto.getTypes()).thenReturn(List.of());
        lenient().when(dto.getAttacks()).thenReturn(List.of());
        lenient().when(dto.getWeaknesses()).thenReturn(List.of());
        lenient().when(dto.getResistances()).thenReturn(List.of());
        lenient().when(dto.getRetreatCost()).thenReturn(List.of());
        lenient().when(dto.getEvolvesFrom()).thenReturn(null);
        lenient().when(dto.getEvolvesTo()).thenReturn(List.of());
        lenient().when(dto.getRules()).thenReturn(List.of());
        return dto;
    }

    /** Builds a Trainer DTO mock with the given subtypes list (may be null). */
    private CardApiDTO trainerDto(String id, String name, List<String> subtypes) {
        CardApiDTO dto = mock(CardApiDTO.class);
        lenient().when(dto.getSupertype()).thenReturn("Trainer");
        lenient().when(dto.getId()).thenReturn(id);
        lenient().when(dto.getName()).thenReturn(name);
        lenient().when(dto.getSubtypes()).thenReturn(subtypes);
        lenient().when(dto.getRules()).thenReturn(List.of());
        lenient().when(dto.getArtist()).thenReturn("Test Artist");
        return dto;
    }

    /** Builds an Energy DTO mock; pass {@code null} types for Special Energy. */
    private CardApiDTO energyDto(String id, String name, List<String> types) {
        CardApiDTO dto = mock(CardApiDTO.class);
        lenient().when(dto.getSupertype()).thenReturn("Energy");
        lenient().when(dto.getId()).thenReturn(id);
        lenient().when(dto.getName()).thenReturn(name);
        lenient().when(dto.getTypes()).thenReturn(types);
        lenient().when(dto.getRules()).thenReturn(List.of());
        return dto;
    }

    /** Builds an AttackDTO mock; pass {@code null} cost to test the empty-cost branch. */
    private AttackDTO attackDto(String name, List<String> cost, String damage, String text) {
        AttackDTO a = mock(AttackDTO.class);
        lenient().when(a.getName()).thenReturn(name);
        lenient().when(a.getCost()).thenReturn(cost);
        lenient().when(a.getDamage()).thenReturn(damage);
        lenient().when(a.getText()).thenReturn(text);
        return a;
    }

    private WeaknessDTO weaknessDto(String type, String value) {
        WeaknessDTO w = mock(WeaknessDTO.class);
        lenient().when(w.getType()).thenReturn(type);
        lenient().when(w.getValue()).thenReturn(value);
        return w;
    }

    private ResistanceDTO resistanceDto(String type, String value) {
        ResistanceDTO r = mock(ResistanceDTO.class);
        lenient().when(r.getType()).thenReturn(type);
        lenient().when(r.getValue()).thenReturn(value);
        return r;
    }

    private ar.edu.utn.frc.tup.piii.dtos.AbilityDTO abilityDto(String name, String text, String type) {
        ar.edu.utn.frc.tup.piii.dtos.AbilityDTO a = mock(ar.edu.utn.frc.tup.piii.dtos.AbilityDTO.class);
        lenient().when(a.getName()).thenReturn(name);
        lenient().when(a.getText()).thenReturn(text);
        lenient().when(a.getType()).thenReturn(type);
        return a;
    }
}
