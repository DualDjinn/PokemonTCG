package ar.edu.utn.frc.tup.piii.services;

import ar.edu.utn.frc.tup.piii.models.effects.AbilityRegistry;
import ar.edu.utn.frc.tup.piii.models.effects.CardEffectRegistry;
import ar.edu.utn.frc.tup.piii.dtos.AbilityDTO;
import ar.edu.utn.frc.tup.piii.dtos.AttackDTO;
import ar.edu.utn.frc.tup.piii.dtos.CardApiDTO;
import ar.edu.utn.frc.tup.piii.dtos.WeaknessDTO;
import ar.edu.utn.frc.tup.piii.dtos.ResistanceDTO;
import ar.edu.utn.frc.tup.piii.models.AceSpecCard;
import ar.edu.utn.frc.tup.piii.models.Ability;
import ar.edu.utn.frc.tup.piii.models.AbilityType;
import ar.edu.utn.frc.tup.piii.models.Attack;
import ar.edu.utn.frc.tup.piii.models.Card;
import ar.edu.utn.frc.tup.piii.models.CardRarity;
import ar.edu.utn.frc.tup.piii.models.CollectibleCard;
import ar.edu.utn.frc.tup.piii.models.DamageModifier;
import ar.edu.utn.frc.tup.piii.models.EnergyCard;
import ar.edu.utn.frc.tup.piii.models.EnergyType;
import ar.edu.utn.frc.tup.piii.models.ItemCard;
import ar.edu.utn.frc.tup.piii.models.PokemonCard;
import ar.edu.utn.frc.tup.piii.models.PokemonStage;
import ar.edu.utn.frc.tup.piii.models.PokemonToolCard;
import ar.edu.utn.frc.tup.piii.models.StadiumCard;
import ar.edu.utn.frc.tup.piii.models.SupporterCard;
import ar.edu.utn.frc.tup.piii.models.TrainerCard;
import ar.edu.utn.frc.tup.piii.models.Weakness;
import ar.edu.utn.frc.tup.piii.models.Resistance;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;

/**
 * Maps raw {@link CardApiDTO} objects (from the pokemontcg.io API shape)
 * to domain {@link Card} instances used by the game engine.
 */
@Component
public class CardMapper {

    /**
     * Cards whose abilities at the given indices should be treated as
     * {@link AbilityType#ACTIVATED} (manually usable by the player)
     * instead of the default {@link AbilityType#PASSIVE}.
     */
    private static final Map<String, Set<Integer>> TRIGGERED_ABILITIES = Map.of(
        "xy1-14", Set.of(0),  // Chesnaught — Spiky Shield (on-damaged)
        "xy1-44", Set.of(0)   // Voltorb — Destiny Burst (on-KO)
    );

    private static final Map<String, Set<Integer>> ACTIVATED_ABILITIES = Map.ofEntries(
        Map.entry("xy1-26", Set.of(0)),  // Delphox — Mystical Fire
        Map.entry("xy1-41", Set.of(0)),  // Greninja — Water Shuriken
        Map.entry("xy1-74", Set.of(0)),  // Inkay — Upside-Down Evolution
        Map.entry("xy1-85", Set.of(0)),  // Aegislash — Stance Change
        Map.entry("xy1-86", Set.of(0)),  // Aegislash — Stance Change
        Map.entry("xy1-93", Set.of(0)),  // Aromatisse — Fairy Transfer
        Map.entry("xy1-103", Set.of(0))  // Swellow — Drive Off
    );

    /** Registry used to look up attack-effect strategies by card ID and attack index. */
    private final CardEffectRegistry effectRegistry;

    /** Registry used to look up ability-effect strategies by card ID and ability index. */
    private final AbilityRegistry abilityRegistry;

    /**
     * Constructs the mapper with the required effect registries.
     *
     * @param effectRegistry   the registry that provides {@code AttackEffect} strategies
     * @param abilityRegistry  the registry that provides {@code AbilityEffect} strategies
     */
    public CardMapper(CardEffectRegistry effectRegistry, AbilityRegistry abilityRegistry) {
        this.effectRegistry = effectRegistry;
        this.abilityRegistry = abilityRegistry;
    }

    /**
     * Converts a raw API DTO to the appropriate domain {@link Card} subtype.
     *
     * @param dto the raw card data from pokemontcg.io
     * @return the corresponding domain object
     * @throws IllegalArgumentException if the supertype is not recognised
     */
    public Card toDomain(CardApiDTO dto) {
        return switch (normalize(dto.getSupertype())) {
            case "Pokemon"     -> mapPokemon(dto);
            case "Trainer"     -> mapTrainer(dto);
            case "Energy"      -> mapEnergy(dto);
            case "Collectible" -> mapCollectible(dto);
            default -> throw new IllegalArgumentException("Unknown supertype: " + dto.getSupertype());
        };
    }

    private String normalize(String value) {
        if (value == null) return "";
        return value.replace("é", "e").replace("è", "e").replace("ê", "e");
    }

    // ── Pokemon ──────────────────────────────────────────────────────────────

    private static final Map<String, String> GIF_OVERRIDES_BY_ID = Map.of(
        "xy1-85", "aegislash-blade.gif",
        "xy1-17", "vivillon-highplains.gif"
    );

    private static final Map<String, String> GIF_OVERRIDES_BY_NAME = Map.of(
        "Mr. Mime", "mrmime.gif"
    );

    public static String resolveGifUrl(String cardId, String name) {
        if (GIF_OVERRIDES_BY_ID.containsKey(cardId)) {
            return GIF_OVERRIDES_BY_ID.get(cardId);
        }
        if (GIF_OVERRIDES_BY_NAME.containsKey(name)) {
            return GIF_OVERRIDES_BY_NAME.get(name);
        }
        if (name.startsWith("M ") && name.endsWith("-EX")) {
            String base = name.substring(2, name.length() - 3);
            return base.toLowerCase().replaceAll("[^a-z0-9]", "-")
                .replaceAll("-+", "-").replaceAll("^-|-$", "") + "-mega.gif";
        }
        if (name.endsWith("-EX")) {
            String base = name.substring(0, name.length() - 3);
            return base.toLowerCase().replaceAll("[^a-z0-9]", "-")
                .replaceAll("-+", "-").replaceAll("^-|-$", "") + ".gif";
        }
        return name.toLowerCase().replaceAll("[^a-z0-9]", "-")
            .replaceAll("-+", "-").replaceAll("^-|-$", "") + ".gif";
    }

    private PokemonCard mapPokemon(CardApiDTO dto) {
        return PokemonCard.builder()
            .id(dto.getId())
            .name(dto.getName())
            .setCode(dto.getSet() != null ? dto.getSet().getId() : null)
            .number(dto.getNumber())
            .hp(Integer.parseInt(dto.getHp()))
            .stage(deriveStage(dto.getSubtypes()))
            .isEx(dto.getSubtypes().contains("EX"))
            .isMega(dto.getSubtypes().contains("MEGA"))
            .pokemonType(derivePrimaryType(dto.getTypes()))
            .attacks(mapAttacks(dto.getAttacks(), dto.getId()))
            .abilities(mapAbilities(dto.getAbilities(), dto.getId()))
            .weaknesses(mapWeaknesses(dto.getWeaknesses()))
            .resistances(mapResistances(dto.getResistances()))
            .retreatCost(dto.getRetreatCost() != null ? dto.getRetreatCost().size() : 0)
            .evolvesFrom(dto.getEvolvesFrom())
            .evolvesTo(dto.getEvolvesTo() != null ? dto.getEvolvesTo() : List.of())
            .rules(dto.getRules() != null ? dto.getRules() : List.of())
            .rarity(dto.getRarity() != null ? CardRarity.fromJson(dto.getRarity()) : null)
            .imageUrl(dto.getImages() != null ? dto.getImages().getSmall() : null)
            .imageUrlLarge(dto.getImages() != null ? dto.getImages().getLarge() : null)
            .gifUrl(resolveGifUrl(dto.getId(), dto.getName()))
            .build();
    }

    private PokemonStage deriveStage(List<String> subtypes) {
        if (subtypes.contains("Basic"))   return PokemonStage.BASIC;
        if (subtypes.contains("Stage 1")) return PokemonStage.STAGE_1;
        if (subtypes.contains("Stage 2")) return PokemonStage.STAGE_2;
        if (subtypes.contains("MEGA") || subtypes.contains("Mega")) return PokemonStage.MEGA;
        throw new IllegalStateException("Unknown stage in subtypes: " + subtypes);
    }

    /**
     * Returns the primary {@link EnergyType} of a Pokemon card (first entry in the types list),
     * or {@code null} for cards with no type information.
     *
     * @param types raw type strings from the API (may be {@code null})
     * @return the primary energy type, or {@code null}
     */
    private EnergyType derivePrimaryType(List<String> types) {
        if (types == null || types.isEmpty()) {
            return null;
        }
        return mapEnergyType(types.get(0));
    }

    private EnergyType mapEnergyType(String type) {
        return switch (type) {
            case "Grass"     -> EnergyType.GRASS;
            case "Fire"      -> EnergyType.FIRE;
            case "Water"     -> EnergyType.WATER;
            case "Lightning" -> EnergyType.LIGHTNING;
            case "Psychic"   -> EnergyType.PSYCHIC;
            case "Fighting"  -> EnergyType.FIGHTING;
            case "Darkness"  -> EnergyType.DARKNESS;
            case "Metal"     -> EnergyType.METAL;
            case "Fairy"     -> EnergyType.FAIRY;
            case "Dragon"    -> EnergyType.DRAGON;
            case "Colorless" -> EnergyType.COLORLESS;
            default -> throw new IllegalArgumentException("Unknown energy type: " + type);
        };
    }

    // ── Attacks ──────────────────────────────────────────────────────────────

    private List<Attack> mapAttacks(List<AttackDTO> dtos, String cardId) {
        if (dtos == null) {
            return new ArrayList<>();
        }
        List<Attack> result = new ArrayList<>();
        for (int i = 0; i < dtos.size(); i++) {
            AttackDTO d = dtos.get(i);
            result.add(Attack.builder()
                .name(d.getName())
                .energyCost(countEnergyCost(d.getCost()))
                .baseDamage(parseDamage(d.getDamage()))
                .damageModifier(parseDamageModifier(d.getDamage()))
                .effectText(d.getText())
                .effect(effectRegistry.getAttackEffect(cardId, i))
                .build());
        }
        return result;
    }

    private Map<EnergyType, Integer> countEnergyCost(List<String> cost) {
        if (cost == null) {
            return Map.of();
        }
        return cost.stream()
            .map(this::mapEnergyType)
            .collect(Collectors.groupingBy(
                Function.identity(),
                Collectors.summingInt(e -> 1)
            ));
    }

    private int parseDamage(String damage) {
        if (damage == null || damage.isBlank()) {
            return 0;
        }
        String digits = damage.replaceAll("[^0-9]", "");
        return digits.isEmpty() ? 0 : Integer.parseInt(digits);
    }

    private DamageModifier parseDamageModifier(String damage) {
        if (damage == null)                           return DamageModifier.NONE;
        if (damage.endsWith("+"))                     return DamageModifier.PLUS;
        if (damage.endsWith("×") || damage.endsWith("x")) return DamageModifier.TIMES;
        if (damage.endsWith("-"))                     return DamageModifier.MINUS;
        return DamageModifier.NONE;
    }

    // ── Abilities ─────────────────────────────────────────────────────────────

    /**
     * Maps the raw ability DTOs from the API to domain {@link Ability} objects.
     *
     * <p>For each ability, the {@code name} and {@code text} from the API are
     * carried over and the {@code type} string (always {@code "Ability"} in
     * the XY1 set) is normalised to {@link AbilityType#PASSIVE} in v1, since
     * only the passive category is wired into the engine at this stage. The
     * resolved {@link ar.edu.utn.frc.tup.piii.models.AbilityEffect} strategy
     * is looked up from the {@link AbilityRegistry} via
     * {@code (cardId, abilityIndex)} and attached as a transient reference on
     * the resulting {@link Ability}.
     *
     * @param dtos   raw ability DTOs from the API (may be {@code null})
     * @param cardId card identifier used to resolve the effect strategy
     * @return list of domain {@link Ability} objects, or an empty list if
     *         {@code dtos} is {@code null}
     */
    private List<Ability> mapAbilities(List<AbilityDTO> dtos, String cardId) {
        if (dtos == null) {
            return new ArrayList<>();
        }
        List<Ability> result = new ArrayList<>();
        for (int i = 0; i < dtos.size(); i++) {
            AbilityDTO d = dtos.get(i);
            result.add(Ability.builder()
                .name(d.getName())
                .description(d.getText())
                .type(deriveAbilityType(d.getType(), cardId, i))
                .effect(abilityRegistry.getAbilityEffect(cardId, i))
                .build());
        }
        return result;
    }

    /**
     * Resolves the domain {@link AbilityType} from the API's {@code type} label.
     *
     * <p>The XY1 set exposes the constant string {@code "Ability"} on every
     * ability, with no further sub-classification. v1 treats every ability
     * as {@link AbilityType#PASSIVE} since the engine only honours that
     * category. Future versions will introduce a per-card override path
     * (e.g. {@code derived} via card-id lookup) for {@code ACTIVATED} and
     * {@code TRIGGERED} abilities.
     *
     * @param apiType raw {@code type} string from the API (may be {@code null})
     * @return the resolved ability type, defaulting to {@link AbilityType#PASSIVE}
     */
    private AbilityType deriveAbilityType(String apiType, String cardId, int abilityIndex) {
        if (cardId != null) {
            Set<Integer> triggered = TRIGGERED_ABILITIES.get(cardId);
            if (triggered != null && triggered.contains(abilityIndex)) {
                return AbilityType.TRIGGERED;
            }
            Set<Integer> activated = ACTIVATED_ABILITIES.get(cardId);
            if (activated != null && activated.contains(abilityIndex)) {
                return AbilityType.ACTIVATED;
            }
        }
        return AbilityType.PASSIVE;
    }

    // ── Weaknesses / Resistances ─────────────────────────────────────────────

    /**
     * Maps the raw weakness DTOs to domain {@link Weakness} objects.
     *
     * <p>The raw {@code value} string (e.g. {@code "×2"}) is stored as-is on the
     * domain object; the numeric multiplier is parsed lazily by {@link Weakness#applyTo(int)}.
     *
     * @param dtos raw weakness DTOs from the API (may be {@code null})
     * @return list of domain {@link Weakness} objects, or an empty list if {@code dtos} is null
     */
    private List<Weakness> mapWeaknesses(List<WeaknessDTO> dtos) {
        if (dtos == null) {
            return new ArrayList<>();
        }
        return dtos.stream()
            .map(d -> new Weakness(mapEnergyType(d.getType()), d.getValue()))
            .collect(Collectors.toList());
    }

    /**
     * Maps the raw resistance DTOs to domain {@link Resistance} objects.
     *
     * <p>The API returns the reduction as a signed string (e.g. {@code "-20"});
     * we parse the absolute numeric value into the domain object's {@code reduction}
     * field, which {@link Resistance#applyTo(int)} subtracts from the incoming damage.
     *
     * @param dtos raw resistance DTOs from the API (may be {@code null})
     * @return list of domain {@link Resistance} objects, or an empty list if {@code dtos} is null
     */
    private List<Resistance> mapResistances(List<ResistanceDTO> dtos) {
        if (dtos == null) {
            return new ArrayList<>();
        }
        return dtos.stream()
            .map(d -> new Resistance(mapEnergyType(d.getType()), parseResistanceValue(d.getValue())))
            .collect(Collectors.toList());
    }

    private int parseResistanceValue(String value) {
        if (value == null || value.isBlank()) {
            return 0;
        }
        String digits = value.replaceAll("[^0-9]", "");
        return digits.isEmpty() ? 0 : Integer.parseInt(digits);
    }

    // ── Trainer / Energy (stubs) ─────────────────────────────────────────────

    /**
     * Maps a raw Trainer {@link CardApiDTO} to the matching {@link TrainerCard} subclass
     * (Item, Supporter, Stadium, Pokémon Tool, or ACE SPEC).
     *
     * <p>The concrete subtype is resolved from {@code dto.subtypes}. ACE SPEC is
     * checked before Item because ACE SPEC cards typically also carry the "Item"
     * subtype and must be promoted to {@link AceSpecCard}.
     *
     * @param dto raw card data
     * @return the corresponding {@link TrainerCard} subclass
     * @throws IllegalStateException if no known trainer subtype is present
     */
    private Card mapTrainer(CardApiDTO dto) {
        return selectTrainerBuilder(dto.getSubtypes())
            .id(dto.getId())
            .name(dto.getName())
            .setCode(dto.getSet() != null ? dto.getSet().getId() : null)
            .number(dto.getNumber())
            .rules(dto.getRules() != null ? dto.getRules() : List.of())
            .artist(dto.getArtist())
            .rarity(dto.getRarity() != null ? CardRarity.fromJson(dto.getRarity()) : null)
            .imageUrl(dto.getImages() != null ? dto.getImages().getSmall() : null)
            .imageUrlLarge(dto.getImages() != null ? dto.getImages().getLarge() : null)
            .build();
    }

    /*
    *Builder polimórfico TrainerCard.TrainerCardBuilder<?, ?>
    *@SuperBuilder de Lombok genera builders compatibles entre toda la jerarquía,
    * evitando un switch con cinco bloques duplicados.
     */

    private TrainerCard.TrainerCardBuilder<?, ?> selectTrainerBuilder(List<String> subtypes) {
        if (subtypes == null) {
            throw new IllegalStateException("Trainer card has no subtypes");
        }
        if (subtypes.contains("ACE SPEC"))     return AceSpecCard.builder();
        if (subtypes.contains("Pokémon Tool") || subtypes.contains("Pokemon Tool")) return PokemonToolCard.builder();
        if (subtypes.contains("Stadium"))      return StadiumCard.builder();
        if (subtypes.contains("Supporter"))    return SupporterCard.builder();
        if (subtypes.contains("Item"))         return ItemCard.builder();
        throw new IllegalStateException("Unknown trainer subtype in subtypes: " + subtypes);
    }

    /**
     * Maps a raw Energy {@link CardApiDTO} to an {@link EnergyCard} domain object.
     *
     * <p>The energy type is resolved by {@link #deriveEnergyType(List, String)},
     * which prefers the API's {@code types} array (Basic Energy) and falls back
     * to scanning the card name (Special Energy such as "Double Colorless Energy").
     *
     * @param dto raw card data
     * @return the corresponding {@link EnergyCard}
     */
    private Card mapEnergy(CardApiDTO dto) {
        return EnergyCard.builder()
            .id(dto.getId())
            .name(dto.getName())
            .setCode(dto.getSet() != null ? dto.getSet().getId() : null)
            .number(dto.getNumber())
            .energyType(deriveEnergyType(dto.getTypes(), dto.getName()))
            .rarity(dto.getRarity() != null ? CardRarity.fromJson(dto.getRarity()) : null)
            .rules(dto.getRules() != null ? dto.getRules() : List.of())
            .imageUrl(dto.getImages() != null ? dto.getImages().getSmall() : null)
            .imageUrlLarge(dto.getImages() != null ? dto.getImages().getLarge() : null)
            .build();
    }

    /**
     * Resolves the {@link EnergyType} for an Energy card.
     *
     * <p>Basic Energy cards expose their type via the API's {@code types} array.
     * Special Energy cards (e.g. "Double Colorless Energy") often omit that field,
     * so this falls back to scanning the card name for a known energy keyword.
     *
     * @param types raw {@code types} array from the API (may be {@code null})
     * @param name  the card name, used as fallback for Special Energy
     * @return the resolved {@link EnergyType}, or {@code null} if it cannot be derived
     */
    private EnergyType deriveEnergyType(List<String> types, String name) {
        if (types != null && !types.isEmpty()) {
            return mapEnergyType(types.get(0));
        }
        if (name != null) {
            for (String keyword : List.of("Grass", "Fire", "Water", "Lightning", "Psychic",
                                          "Fighting", "Darkness", "Metal", "Fairy",
                                          "Dragon", "Colorless")) {
                if (name.contains(keyword)) {
                    return mapEnergyType(keyword);
                }
            }
        }
        return null;
    }

    private Card mapCollectible(CardApiDTO dto) {
        return CollectibleCard.builder()
            .id(dto.getId())
            .name(dto.getName())
            .setCode(dto.getSet() != null ? dto.getSet().getId() : null)
            .number(dto.getNumber())
            .rules(dto.getRules() != null ? dto.getRules() : List.of())
            .artist(dto.getArtist())
            .rarity(dto.getRarity() != null ? CardRarity.fromJson(dto.getRarity()) : null)
            .imageUrl(dto.getImages() != null ? dto.getImages().getSmall() : null)
            .imageUrlLarge(dto.getImages() != null ? dto.getImages().getLarge() : null)
            .build();
    }
}
