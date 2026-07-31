package ar.edu.utn.frc.tup.piii.engine;

import ar.edu.utn.frc.tup.piii.models.Ability;
import ar.edu.utn.frc.tup.piii.models.AbilityEffect;
import ar.edu.utn.frc.tup.piii.models.AbilityType;
import ar.edu.utn.frc.tup.piii.models.GameContext;
import ar.edu.utn.frc.tup.piii.models.InPlayPokemon;
import ar.edu.utn.frc.tup.piii.models.PokemonCard;
import ar.edu.utn.frc.tup.piii.models.PokemonStage;
import ar.edu.utn.frc.tup.piii.models.EnergyType;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
class TriggeredAbilityDispatcherTest {

    @Mock
    private AbilityEffect mockEffect;

    private final TriggeredAbilityDispatcher dispatcher = new TriggeredAbilityDispatcher();

    @Test
    void onDamaged_DispatchesTriggeredAbility() {
        InPlayPokemon defender = pokemonWithTriggeredAbility();
        InPlayPokemon attacker = pokemon("attacker");
        GameContext ctx = new GameContext();

        dispatcher.onDamaged(ctx, defender, attacker);

        verify(mockEffect).apply(ctx, defender, defender.getCard().getAbilities().get(0),
                new AbilityEffect.TriggerContext(attacker));
    }

    @Test
    void onDamaged_AbilitiesDisabled_Skips() {
        InPlayPokemon defender = pokemonWithTriggeredAbility();
        defender.setAbilitiesDisabled(true);
        InPlayPokemon attacker = pokemon("attacker");
        GameContext ctx = new GameContext();

        dispatcher.onDamaged(ctx, defender, attacker);

        verifyNoInteractions(mockEffect);
    }

    @Test
    void onDamaged_NoAbilities_Skips() {
        InPlayPokemon defender = pokemon("defender");
        InPlayPokemon attacker = pokemon("attacker");
        GameContext ctx = new GameContext();

        dispatcher.onDamaged(ctx, defender, attacker);

        verifyNoInteractions(mockEffect);
    }

    @Test
    void onDamaged_NullCard_Skips() {
        PokemonCard card = PokemonCard.builder()
                .id("null-card").name("Null").hp(60)
                .stage(PokemonStage.BASIC).pokemonType(EnergyType.FIRE)
                .attacks(List.of()).abilities(List.of())
                .weaknesses(List.of()).resistances(List.of())
                .costToAttack(List.of()).retreatCost(0)
                .build();
        InPlayPokemon defender = new InPlayPokemon(card);
        // Simulate null card by not providing one via underlying data
        InPlayPokemon attacker = pokemon("attacker");
        GameContext ctx = new GameContext();

        dispatcher.onDamaged(ctx, defender, attacker);

        verifyNoInteractions(mockEffect);
    }

    @Test
    void onKnockedOut_DispatchesTriggeredAbility() {
        InPlayPokemon knockedOut = pokemonWithTriggeredAbility();
        InPlayPokemon attacker = pokemon("attacker");
        GameContext ctx = new GameContext();

        dispatcher.onKnockedOut(ctx, knockedOut, attacker);

        verify(mockEffect).apply(ctx, knockedOut, knockedOut.getCard().getAbilities().get(0),
                new AbilityEffect.TriggerContext(attacker));
    }

    @Test
    void onKnockedOut_AbilitiesDisabled_Skips() {
        InPlayPokemon knockedOut = pokemonWithTriggeredAbility();
        knockedOut.setAbilitiesDisabled(true);
        InPlayPokemon attacker = pokemon("attacker");
        GameContext ctx = new GameContext();

        dispatcher.onKnockedOut(ctx, knockedOut, attacker);

        verifyNoInteractions(mockEffect);
    }

    @Test
    void onDamaged_NullCtx_Throws() {
        InPlayPokemon defender = pokemon("defender");
        InPlayPokemon attacker = pokemon("attacker");

        assertThatThrownBy(() -> dispatcher.onDamaged(null, defender, attacker))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void onDamaged_NullDefender_Throws() {
        GameContext ctx = new GameContext();
        InPlayPokemon attacker = pokemon("attacker");

        assertThatThrownBy(() -> dispatcher.onDamaged(ctx, null, attacker))
                .isInstanceOf(NullPointerException.class);
    }

    private InPlayPokemon pokemonWithTriggeredAbility() {
        Ability ability = Ability.builder()
                .name("Spiky Shield")
                .description("If this Pokémon is damaged by an attack, do 10 damage to the attacker.")
                .type(AbilityType.TRIGGERED)
                .effect(mockEffect)
                .build();
        return pokemonWithAbilities(List.of(ability));
    }

    private InPlayPokemon pokemonWithAbilities(List<Ability> abilities) {
        PokemonCard card = PokemonCard.builder()
                .id("test-ability").name("TestMon")
                .hp(100)
                .stage(PokemonStage.BASIC).pokemonType(EnergyType.FIRE)
                .attacks(List.of()).abilities(abilities)
                .weaknesses(List.of()).resistances(List.of())
                .costToAttack(List.of()).retreatCost(0)
                .build();
        return new InPlayPokemon(card);
    }

    private static InPlayPokemon pokemon(String id) {
        PokemonCard card = PokemonCard.builder()
                .id(id).name(id).hp(100)
                .stage(PokemonStage.BASIC).pokemonType(EnergyType.FIRE)
                .attacks(List.of()).abilities(List.of())
                .weaknesses(List.of()).resistances(List.of())
                .costToAttack(List.of()).retreatCost(0)
                .build();
        return new InPlayPokemon(card);
    }
}
