package ar.edu.utn.frc.tup.piii.dtos;

import org.junit.jupiter.api.Test;
import java.lang.reflect.Field;
import java.util.List;
import static org.assertj.core.api.Assertions.assertThat;

class ApiDtoTest {

    private void setField(Object target, String name, Object value) throws Exception {
        Field f = target.getClass().getDeclaredField(name);
        f.setAccessible(true);
        f.set(target, value);
    }

    @Test
    void testSupporterApiDTO() throws Exception {
        SupporterApiDTO dto = new SupporterApiDTO();
        setField(dto, "id", "xy1-115");
        setField(dto, "name", "Cassius");
        setField(dto, "supertype", "Trainer");
        setField(dto, "subtypes", List.of("Supporter"));
        setField(dto, "rules", List.of("Rule text"));
        setField(dto, "number", "115");
        setField(dto, "artist", "Masakazu Fukuda");
        setField(dto, "rarity", "Uncommon");

        assertThat(dto.getId()).isEqualTo("xy1-115");
        assertThat(dto.getName()).isEqualTo("Cassius");
        assertThat(dto.getSupertype()).isEqualTo("Trainer");
        assertThat(dto.getSubtypes()).containsExactly("Supporter");
        assertThat(dto.getRules()).containsExactly("Rule text");
        assertThat(dto.getNumber()).isEqualTo("115");
        assertThat(dto.getArtist()).isEqualTo("Masakazu Fukuda");
        assertThat(dto.getRarity()).isEqualTo("Uncommon");
        assertThat(dto.getSet()).isNull();
        assertThat(dto.getLegalities()).isNull();
        assertThat(dto.getImages()).isNull();
    }

    @Test
    void testCardApiDTO() throws Exception {
        CardApiDTO dto = new CardApiDTO();
        setField(dto, "id", "xy1-1");
        setField(dto, "name", "Venusaur-EX");
        setField(dto, "supertype", "Pokémon");
        setField(dto, "subtypes", List.of("Basic", "EX"));
        setField(dto, "hp", "180");
        setField(dto, "types", List.of("Grass"));
        setField(dto, "evolvesTo", List.of("M Venusaur-EX"));
        setField(dto, "rules", List.of("Rule text"));
        setField(dto, "number", "1");
        setField(dto, "artist", "Mitsuhiro Arita");
        setField(dto, "rarity", "Rare");
        setField(dto, "retreatCost", List.of("Colorless", "Colorless"));
        setField(dto, "convertedRetreatCost", 2);

        assertThat(dto.getId()).isEqualTo("xy1-1");
        assertThat(dto.getName()).isEqualTo("Venusaur-EX");
        assertThat(dto.getSupertype()).isEqualTo("Pokémon");
        assertThat(dto.getSubtypes()).containsExactly("Basic", "EX");
        assertThat(dto.getHp()).isEqualTo("180");
        assertThat(dto.getTypes()).containsExactly("Grass");
        assertThat(dto.getEvolvesTo()).containsExactly("M Venusaur-EX");
        assertThat(dto.getRules()).containsExactly("Rule text");
        assertThat(dto.getNumber()).isEqualTo("1");
        assertThat(dto.getArtist()).isEqualTo("Mitsuhiro Arita");
        assertThat(dto.getRarity()).isEqualTo("Rare");
        assertThat(dto.getRetreatCost()).containsExactly("Colorless", "Colorless");
        assertThat(dto.getConvertedRetreatCost()).isEqualTo(2);
    }

    @Test
    void testSetDTO() throws Exception {
        SetDTO dto = new SetDTO();
        setField(dto, "id", "xy1");
        setField(dto, "name", "XY");
        setField(dto, "series", "XY");
        setField(dto, "printedTotal", 146);
        setField(dto, "total", 146);
        setField(dto, "ptcgoCode", "XY");
        setField(dto, "releaseDate", "2014/02/05");
        setField(dto, "updatedAt", "2020/08/14");

        assertThat(dto.getId()).isEqualTo("xy1");
        assertThat(dto.getName()).isEqualTo("XY");
        assertThat(dto.getSeries()).isEqualTo("XY");
        assertThat(dto.getPrintedTotal()).isEqualTo(146);
        assertThat(dto.getTotal()).isEqualTo(146);
        assertThat(dto.getPtcgoCode()).isEqualTo("XY");
        assertThat(dto.getReleaseDate()).isEqualTo("2014/02/05");
        assertThat(dto.getUpdatedAt()).isEqualTo("2020/08/14");
    }

    @Test
    void testImagesDTO() throws Exception {
        ImagesDTO dto = new ImagesDTO();
        setField(dto, "small", "https://images.pokemontcg.io/xy1/1.png");
        setField(dto, "large", "https://images.pokemontcg.io/xy1/1_hires.png");
        setField(dto, "symbol", "https://images.pokemontcg.io/xy1/symbol.png");
        setField(dto, "logo", "https://images.pokemontcg.io/xy1/logo.png");

        assertThat(dto.getSmall()).isEqualTo("https://images.pokemontcg.io/xy1/1.png");
        assertThat(dto.getLarge()).isEqualTo("https://images.pokemontcg.io/xy1/1_hires.png");
        assertThat(dto.getSymbol()).isEqualTo("https://images.pokemontcg.io/xy1/symbol.png");
        assertThat(dto.getLogo()).isEqualTo("https://images.pokemontcg.io/xy1/logo.png");
    }

    @Test
    void testCardImagesDTO() throws Exception {
        CardImagesDTO dto = new CardImagesDTO();
        setField(dto, "small", "small.png");
        setField(dto, "large", "large.png");

        assertThat(dto.getSmall()).isEqualTo("small.png");
        assertThat(dto.getLarge()).isEqualTo("large.png");
    }

    @Test
    void testAbilityDTO() throws Exception {
        AbilityDTO dto = new AbilityDTO();
        setField(dto, "name", "Overgrow");
        setField(dto, "text", "When HP is low, Grass moves do more");
        setField(dto, "type", "Pokémon Ability");

        assertThat(dto.getName()).isEqualTo("Overgrow");
        assertThat(dto.getText()).isEqualTo("When HP is low, Grass moves do more");
        assertThat(dto.getType()).isEqualTo("Pokémon Ability");
    }

    @Test
    void testAttackDTO() throws Exception {
        AttackDTO dto = new AttackDTO();
        setField(dto, "name", "Tackle");
        setField(dto, "cost", List.of("Grass", "Colorless"));
        setField(dto, "convertedEnergyCost", 2);
        setField(dto, "damage", "30");
        setField(dto, "text", "");

        assertThat(dto.getName()).isEqualTo("Tackle");
        assertThat(dto.getCost()).containsExactly("Grass", "Colorless");
        assertThat(dto.getConvertedEnergyCost()).isEqualTo(2);
        assertThat(dto.getDamage()).isEqualTo("30");
        assertThat(dto.getText()).isEqualTo("");
    }

    @Test
    void testLegalitiesDTO() throws Exception {
        LegalitiesDTO dto = new LegalitiesDTO();
        setField(dto, "unlimited", "Legal");
        setField(dto, "standard", "Legal");
        setField(dto, "expanded", "Legal");

        assertThat(dto.getUnlimited()).isEqualTo("Legal");
        assertThat(dto.getStandard()).isEqualTo("Legal");
        assertThat(dto.getExpanded()).isEqualTo("Legal");
    }
}
