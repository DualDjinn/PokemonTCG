package ar.edu.utn.frc.tup.piii.dtos.front;

import java.util.List;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Full Pokémon TCG card representation returned to the frontend.
 * Covers Pokémon, Trainer, and Energy cards from the XY1 set.
 */
@Getter
@Setter
@NoArgsConstructor
public class CardDTO {

    /** Unique card identifier (e.g., "xy1-1"). */
    private String id;

    /** Card name (e.g., "Venusaur-EX"). */
    private String name;

    /** Supertype: "Pokémon", "Trainer", or "Energy". */
    private String supertype;

    /** List of subtypes (e.g., ["Basic", "EX"] or ["Item"]). */
    private List<String> subtypes;

    /** Hit points as a string; only present on Pokémon cards (e.g., "180"). */
    private String hp;

    /** Energy types; only present on Pokémon cards (e.g., ["Grass"]). */
    private List<String> types;

    /** Names of Pokémon this card can evolve into. */
    private List<String> evolvesTo;

    /** Name of the Pokémon this card evolves from; null for Basic Pokémon. */
    private String evolvesFrom;

    /** Descriptive rule texts (e.g., EX rule text). */
    private List<String> rules;

    /** List of attacks; only present on Pokémon cards. */
    private List<AttackDTO> attacks;

    /** List of abilities; only present on certain Pokémon cards. */
    private List<AbilityDTO> abilities;

    /** Weakness entries; only present on Pokémon cards. */
    private List<WeaknessDTO> weaknesses;

    /** Resistance entries; only present on Pokémon cards. */
    private List<ResistanceDTO> resistances;

    /** Retreat cost energy list; only present on Pokémon cards. */
    private List<String> retreatCost;

    /** Total retreat cost as a single integer. */
    private Integer convertedRetreatCost;

    /** Card number within the set (e.g., "1"). */
    private String number;

    /** Rarity label (e.g., "Rare Holo EX", "Common"). */
    private String rarity;

    /** True when the card is a full art variant (XY1: rarity "Rare Ultra"). */
    private boolean isFullArt;

    /** Gif filename for animated Pokémon display (e.g., "venusaur.gif"); null for non-Pokémon cards. */
    private String gifUrl;

    /** Small and large image URLs for this card. */
    private CardImagesDTO images;

    /** Set information this card belongs to. */
    private SetDTO set;
}
