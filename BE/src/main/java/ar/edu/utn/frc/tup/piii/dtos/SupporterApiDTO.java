package ar.edu.utn.frc.tup.piii.dtos;

import java.util.List;

import lombok.Getter;

/**
 * DTO that represents a Supporter Trainer card as returned by the pokemontcg.io v2 API.
 *
 * <p>Supporter cards are a subtype of Trainer. They have an immediate effect when played
 * and are then discarded. Only one Supporter may be played per turn.
 *
 * <p>Pricing fields (tcgplayer, cardmarket) are intentionally omitted because they carry
 * no game-logic relevance. Pokemon-specific fields (hp, attacks, weaknesses, etc.) are
 * also absent since Supporters never have them.
 *
 * <p>Example cards: Cassius (xy1-115), Professor Sycamore (xy1-122), Shauna (xy1-127).
 */
@Getter
public class SupporterApiDTO {

    /** Unique card identifier, e.g. {@code "xy1-115"}. */
    private String id;

    /** Card name, e.g. {@code "Professor Sycamore"}. */
    private String name;

    /** Always {@code "Trainer"} for this DTO. */
    private String supertype;

    /** Always contains {@code ["Supporter"]} for this DTO. */
    private List<String> subtypes;

    /**
     * Game rules printed on the card.
     * The first entry is the card's effect; the last entry is always the generic
     * Supporter reminder: "You may play only 1 Supporter card during your turn
     * (before your attack)."
     */
    private List<String> rules;

    /** Set information (id, name, series, release date, legalities, images). */
    private SetDTO set;

    /** Card number within the set, e.g. {@code "115"}. */
    private String number;

    /** Illustrator credit, e.g. {@code "Masakazu Fukuda"}. */
    private String artist;

    /** Print rarity, e.g. {@code "Uncommon"}. */
    private String rarity;

    /** Format legalities for this printing (unlimited, standard, expanded). */
    private LegalitiesDTO legalities;

    /** Card image URLs: {@code small} for thumbnails, {@code large} for high-res display. */
    private ImagesDTO images;
}
