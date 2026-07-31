package ar.edu.utn.frc.tup.piii.services;

import ar.edu.utn.frc.tup.piii.dtos.CardApiDTO;
import ar.edu.utn.frc.tup.piii.dtos.front.CardDTO;
import ar.edu.utn.frc.tup.piii.dtos.front.CardImagesDTO;
import ar.edu.utn.frc.tup.piii.dtos.front.SetImagesDTO;
import jakarta.annotation.PostConstruct;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;

/**
 * Service that exposes XY1 Pokémon TCG card data (as frontend-safe {@link CardDTO}s)
 * by delegating to {@link CardRegistryService} — the single source of truth for
 * card data loaded from {@code data/db.json}.
 *
 * <p>All 146 cards are converted once at startup and held in memory.</p>
 */
@Service
public class CardService {

    private final CardRegistryService cardRegistry;

    /** In-memory cache of all 146 XY1 cards converted from the registry. */
    private List<CardDTO> cards;

    public CardService(CardRegistryService cardRegistry) {
        this.cardRegistry = cardRegistry;
    }

    @PostConstruct
    public void init() {
        refreshCache();
    }

    /**
     * Rebuilds the in-memory CardDTO cache from the registry's raw cards.
     * Called after API sync operations to reflect updated card data.
     */
    public void refreshCache() {
        this.cards = Collections.unmodifiableList(
            cardRegistry.getRawCards().stream()
                .map(this::toCardDTO)
                .collect(Collectors.toList())
        );
    }

    private CardDTO toCardDTO(CardApiDTO api) {
        CardDTO dto = new CardDTO();
        dto.setId(api.getId());
        dto.setName(api.getName());
        dto.setSupertype(api.getSupertype());
        dto.setSubtypes(api.getSubtypes());
        dto.setHp(api.getHp());
        dto.setTypes(api.getTypes());
        dto.setEvolvesTo(api.getEvolvesTo());
        dto.setEvolvesFrom(api.getEvolvesFrom());
        dto.setRules(api.getRules());
        dto.setNumber(api.getNumber());
        dto.setRarity(api.getRarity());
        dto.setFullArt("Rare Ultra".equals(api.getRarity()));
        if ("Pokémon".equals(api.getSupertype()) || "Pokemon".equals(api.getSupertype())) {
            dto.setGifUrl(CardMapper.resolveGifUrl(api.getId(), api.getName()));
        }
        dto.setConvertedRetreatCost(api.getConvertedRetreatCost());
        dto.setRetreatCost(api.getRetreatCost());
        if (api.getAttacks() != null) {
            dto.setAttacks(api.getAttacks().stream()
                .map(a -> {
                    ar.edu.utn.frc.tup.piii.dtos.front.AttackDTO ad = new ar.edu.utn.frc.tup.piii.dtos.front.AttackDTO();
                    ad.setName(a.getName());
                    ad.setCost(a.getCost());
                    ad.setConvertedEnergyCost(a.getConvertedEnergyCost());
                    ad.setDamage(a.getDamage());
                    ad.setText(a.getText());
                    return ad;
                })
                .collect(Collectors.toList()));
        }
        if (api.getAbilities() != null) {
            dto.setAbilities(api.getAbilities().stream()
                .map(a -> {
                    ar.edu.utn.frc.tup.piii.dtos.front.AbilityDTO ad = new ar.edu.utn.frc.tup.piii.dtos.front.AbilityDTO();
                    ad.setName(a.getName());
                    ad.setText(a.getText());
                    ad.setType(a.getType());
                    return ad;
                })
                .collect(Collectors.toList()));
        }
        if (api.getWeaknesses() != null) {
            dto.setWeaknesses(api.getWeaknesses().stream()
                .map(w -> {
                    ar.edu.utn.frc.tup.piii.dtos.front.WeaknessDTO wd = new ar.edu.utn.frc.tup.piii.dtos.front.WeaknessDTO();
                    wd.setType(w.getType());
                    wd.setValue(w.getValue());
                    return wd;
                })
                .collect(Collectors.toList()));
        }
        if (api.getResistances() != null) {
            dto.setResistances(api.getResistances().stream()
                .map(r -> {
                    ar.edu.utn.frc.tup.piii.dtos.front.ResistanceDTO rd = new ar.edu.utn.frc.tup.piii.dtos.front.ResistanceDTO();
                    rd.setType(r.getType());
                    rd.setValue(r.getValue());
                    return rd;
                })
                .collect(Collectors.toList()));
        }
        if (api.getImages() != null) {
            CardImagesDTO images = new CardImagesDTO();
            images.setSmall(api.getImages().getSmall());
            images.setLarge(api.getImages().getLarge());
            dto.setImages(images);
        }
        if (api.getSet() != null) {
            ar.edu.utn.frc.tup.piii.dtos.front.SetDTO sd = new ar.edu.utn.frc.tup.piii.dtos.front.SetDTO();
            sd.setId(api.getSet().getId());
            sd.setName(api.getSet().getName());
            sd.setSeries(api.getSet().getSeries());
            sd.setPrintedTotal(api.getSet().getPrintedTotal());
            sd.setTotal(api.getSet().getTotal());
            sd.setPtcgoCode(api.getSet().getPtcgoCode());
            sd.setReleaseDate(api.getSet().getReleaseDate());
            if (api.getSet().getImages() != null) {
                SetImagesDTO simg = new SetImagesDTO();
                simg.setSymbol(api.getSet().getImages().getSymbol());
                simg.setLogo(api.getSet().getImages().getLogo());
                sd.setImages(simg);
            }
            dto.setSet(sd);
        }
        return dto;
    }

    /**
     * Returns every card in the database.
     *
     * @return unmodifiable list of all {@link CardDTO} objects
     */
    public List<CardDTO> findAll() {
        return cards;
    }

    /**
     * Finds a card by its unique identifier (e.g., {@code "xy1-1"}).
     *
     * @param id the card identifier to search for
     * @return an {@link Optional} containing the matching card, or empty if none found
     */
    public Optional<CardDTO> findById(String id) {
        return cards.stream()
            .filter(c -> id.equals(c.getId()))
            .findFirst();
    }

    /**
     * Returns all cards that match every non-null filter criterion.
     * Filters are combined with logical AND; passing {@code null} for a
     * parameter disables that particular filter.
     *
     * @param name      partial name match (case-insensitive); {@code null} = no filter
     * @param supertype exact supertype match — accepts both {@code "Pokemon"} and {@code "Pokémon"};
     *                  {@code null} = no filter
     * @param type      energy type for Pokémon cards (e.g., {@code "Grass"}); {@code null} = no filter
     * @param rarity    exact rarity match (case-insensitive); {@code null} = no filter
     * @param subtype   subtype match (e.g., {@code "Basic"}, {@code "EX"}); {@code null} = no filter
     * @return list of cards satisfying all provided filters
     */
    public List<CardDTO> findByFilters(String name, String supertype,
                                       String type, String rarity, String subtype) {
        return cards.stream()
            .filter(c -> name == null
                || c.getName().toLowerCase(Locale.ROOT).contains(name.toLowerCase(Locale.ROOT)))
            .filter(c -> supertype == null
                || normalizeText(c.getSupertype()).equals(normalizeText(supertype)))
            .filter(c -> type == null
                || (c.getTypes() != null
                    && c.getTypes().stream().anyMatch(t -> t.equalsIgnoreCase(type))))
            .filter(c -> rarity == null
                || (c.getRarity() != null && c.getRarity().equalsIgnoreCase(rarity)))
            .filter(c -> subtype == null
                || (c.getSubtypes() != null
                    && c.getSubtypes().stream().anyMatch(s -> s.equalsIgnoreCase(subtype))))
            .collect(Collectors.toList());
    }

    /**
     * Normalises a string to lowercase ASCII so that {@code "Pokémon"} and
     * {@code "Pokemon"} compare as equal.
     *
     * @param value raw string to normalise; may be {@code null}
     * @return lowercase de-accented string, or an empty string if the input is {@code null}
     */
    private String normalizeText(String value) {
        if (value == null) {
            return "";
        }
        return value.toLowerCase(Locale.ROOT)
            .replace("é", "e")
            .replace("è", "e")
            .replace("ê", "e");
    }
}
