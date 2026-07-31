package ar.edu.utn.frc.tup.piii.engine.choice;

import ar.edu.utn.frc.tup.piii.models.Card;
import ar.edu.utn.frc.tup.piii.models.PokemonCard;
import ar.edu.utn.frc.tup.piii.models.EnergyType;
import ar.edu.utn.frc.tup.piii.models.PokemonStage;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PendingPlayerChoiceTest {

    private static final String PLAYER_ID = "player-1";
    private static final ChoiceType TYPE = ChoiceType.PICK_FROM_DECK_TOP_N;
    private static final List<Card> OPTIONS = List.of(createCard("c1"), createCard("c2"));
    private static final int MIN_PICK = 1;
    private static final int MAX_PICK = 2;
    private static final String PROMPT = "Choose a card";
    private static final Boolean COIN_FLIP_HEADS = Boolean.TRUE;
    private static final Consumer<List<String>> CALLBACK = ids -> {};

    @Test
    void constructor_WithAllValidParams_SetsFieldsCorrectly() {
        PendingPlayerChoice choice = new PendingPlayerChoice(
                PLAYER_ID, TYPE, OPTIONS, MIN_PICK, MAX_PICK, PROMPT, COIN_FLIP_HEADS, CALLBACK);

        assertThat(choice.getPlayerId()).isEqualTo(PLAYER_ID);
        assertThat(choice.getType()).isEqualTo(TYPE);
        assertThat(choice.getOptions()).containsExactlyElementsOf(OPTIONS);
        assertThat(choice.getMinPick()).isEqualTo(MIN_PICK);
        assertThat(choice.getMaxPick()).isEqualTo(MAX_PICK);
        assertThat(choice.getPrompt()).isEqualTo(PROMPT);
        assertThat(choice.getCoinFlipHeads()).isTrue();
        assertThat(choice.getCallback()).isEqualTo(CALLBACK);
    }

    @Test
    void constructor_WithNullPlayerId_ThrowsNullPointerException() {
        assertThatThrownBy(() -> new PendingPlayerChoice(
                null, TYPE, OPTIONS, MIN_PICK, MAX_PICK, PROMPT, COIN_FLIP_HEADS, CALLBACK))
                .isInstanceOf(NullPointerException.class)
                .hasMessage("playerId must not be null");
    }

    @Test
    void constructor_WithNullType_ThrowsNullPointerException() {
        assertThatThrownBy(() -> new PendingPlayerChoice(
                PLAYER_ID, null, OPTIONS, MIN_PICK, MAX_PICK, PROMPT, COIN_FLIP_HEADS, CALLBACK))
                .isInstanceOf(NullPointerException.class)
                .hasMessage("type must not be null");
    }

    @Test
    void constructor_WithNullCallback_ThrowsNullPointerException() {
        assertThatThrownBy(() -> new PendingPlayerChoice(
                PLAYER_ID, TYPE, OPTIONS, MIN_PICK, MAX_PICK, PROMPT, COIN_FLIP_HEADS, null))
                .isInstanceOf(NullPointerException.class)
                .hasMessage("callback must not be null");
    }

    @Test
    void constructor_WithNullOptions_DefaultsToEmptyList() {
        PendingPlayerChoice choice = new PendingPlayerChoice(
                PLAYER_ID, TYPE, null, MIN_PICK, MAX_PICK, PROMPT, COIN_FLIP_HEADS, CALLBACK);

        assertThat(choice.getOptions()).isEmpty();
    }

    @Test
    void constructor_WithEmptyOptions_StoresEmptyList() {
        PendingPlayerChoice choice = new PendingPlayerChoice(
                PLAYER_ID, TYPE, List.of(), MIN_PICK, MAX_PICK, PROMPT, COIN_FLIP_HEADS, CALLBACK);

        assertThat(choice.getOptions()).isEmpty();
    }

    @Test
    void constructor_WithNullPrompt_StoresNull() {
        PendingPlayerChoice choice = new PendingPlayerChoice(
                PLAYER_ID, TYPE, OPTIONS, MIN_PICK, MAX_PICK, null, COIN_FLIP_HEADS, CALLBACK);

        assertThat(choice.getPrompt()).isNull();
    }

    @Test
    void constructor_WithNullCoinFlipHeads_StoresNull() {
        PendingPlayerChoice choice = new PendingPlayerChoice(
                PLAYER_ID, TYPE, OPTIONS, MIN_PICK, MAX_PICK, PROMPT, null, CALLBACK);

        assertThat(choice.getCoinFlipHeads()).isNull();
    }

    @Test
    void constructor_WithCoinFlipHeadsFalse_StoresFalse() {
        PendingPlayerChoice choice = new PendingPlayerChoice(
                PLAYER_ID, TYPE, OPTIONS, MIN_PICK, MAX_PICK, PROMPT, Boolean.FALSE, CALLBACK);

        assertThat(choice.getCoinFlipHeads()).isFalse();
    }

    @Test
    void constructor_WithEmptyPlayerId_StoresEmptyString() {
        PendingPlayerChoice choice = new PendingPlayerChoice(
                "", TYPE, OPTIONS, MIN_PICK, MAX_PICK, PROMPT, COIN_FLIP_HEADS, CALLBACK);

        assertThat(choice.getPlayerId()).isEmpty();
    }

    @Test
    void constructor_WithMinPickGreaterThanMaxPick_StoresAsIs() {
        PendingPlayerChoice choice = new PendingPlayerChoice(
                PLAYER_ID, TYPE, OPTIONS, 5, 2, PROMPT, COIN_FLIP_HEADS, CALLBACK);

        assertThat(choice.getMinPick()).isEqualTo(5);
        assertThat(choice.getMaxPick()).isEqualTo(2);
    }

    @Test
    void constructor_WithNegativeMinPick_StoresNegative() {
        PendingPlayerChoice choice = new PendingPlayerChoice(
                PLAYER_ID, TYPE, OPTIONS, -1, 2, PROMPT, COIN_FLIP_HEADS, CALLBACK);

        assertThat(choice.getMinPick()).isNegative();
    }

    @Test
    void constructor_WithNegativeMaxPick_StoresNegative() {
        PendingPlayerChoice choice = new PendingPlayerChoice(
                PLAYER_ID, TYPE, OPTIONS, 1, -1, PROMPT, COIN_FLIP_HEADS, CALLBACK);

        assertThat(choice.getMaxPick()).isNegative();
    }

    @Test
    void constructor_WithZeroMinAndMaxPick_StoresZero() {
        PendingPlayerChoice choice = new PendingPlayerChoice(
                PLAYER_ID, TYPE, OPTIONS, 0, 0, PROMPT, COIN_FLIP_HEADS, CALLBACK);

        assertThat(choice.getMinPick()).isZero();
        assertThat(choice.getMaxPick()).isZero();
    }

    @Test
    void constructor_WithAllChoiceTypes_StoresTypeCorrectly() {
        for (ChoiceType ct : ChoiceType.values()) {
            PendingPlayerChoice choice = new PendingPlayerChoice(
                    PLAYER_ID, ct, OPTIONS, MIN_PICK, MAX_PICK, PROMPT, COIN_FLIP_HEADS, CALLBACK);
            assertThat(choice.getType()).isEqualTo(ct);
        }
    }

    @Test
    void constructor_WithCoinFlipInfoTypeAndEmptyOptions_StoresCorrectly() {
        PendingPlayerChoice choice = new PendingPlayerChoice(
                PLAYER_ID, ChoiceType.COIN_FLIP_INFO, List.of(), 0, 0, null, true, CALLBACK);

        assertThat(choice.getType()).isEqualTo(ChoiceType.COIN_FLIP_INFO);
        assertThat(choice.getOptions()).isEmpty();
        assertThat(choice.getMinPick()).isZero();
        assertThat(choice.getMaxPick()).isZero();
        assertThat(choice.getPrompt()).isNull();
        assertThat(choice.getCoinFlipHeads()).isTrue();
    }

    @Test
    void constructor_WithSingleOption_StoresCorrectly() {
        List<Card> singleOption = List.of(createCard("c1"));
        PendingPlayerChoice choice = new PendingPlayerChoice(
                PLAYER_ID, ChoiceType.PICK_BENCHED_POKEMON, singleOption, 1, 1, PROMPT, null, CALLBACK);

        assertThat(choice.getOptions()).hasSize(1);
        assertThat(choice.getOptions().getFirst().getId()).isEqualTo("c1");
    }

    @Test
    void constructor_WithLargeOptionsList_StoresCorrectly() {
        List<Card> manyOptions = new ArrayList<>();
        for (int i = 0; i < 100; i++) {
            manyOptions.add(createCard("card-" + i));
        }
        PendingPlayerChoice choice = new PendingPlayerChoice(
                PLAYER_ID, ChoiceType.REORDER_DECK_TOP_N, manyOptions, 0, 100, "Reorder", null, CALLBACK);

        assertThat(choice.getOptions()).hasSize(100);
        assertThat(choice.getOptions().get(0).getId()).isEqualTo("card-0");
        assertThat(choice.getOptions().get(99).getId()).isEqualTo("card-99");
    }

    @Test
    void getOptions_ReturnsUnmodifiableList() {
        PendingPlayerChoice choice = new PendingPlayerChoice(
                PLAYER_ID, TYPE, OPTIONS, MIN_PICK, MAX_PICK, PROMPT, COIN_FLIP_HEADS, CALLBACK);

        assertThatThrownBy(() -> choice.getOptions().add(createCard("c3")))
                .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void getOptions_ReturnsUnmodifiableList_RemoveThrows() {
        PendingPlayerChoice choice = new PendingPlayerChoice(
                PLAYER_ID, TYPE, OPTIONS, MIN_PICK, MAX_PICK, PROMPT, COIN_FLIP_HEADS, CALLBACK);

        assertThatThrownBy(() -> choice.getOptions().remove(0))
                .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void getOptions_ReturnsUnmodifiableList_ClearThrows() {
        PendingPlayerChoice choice = new PendingPlayerChoice(
                PLAYER_ID, TYPE, OPTIONS, MIN_PICK, MAX_PICK, PROMPT, COIN_FLIP_HEADS, CALLBACK);

        assertThatThrownBy(() -> choice.getOptions().clear())
                .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void constructor_DefensiveCopy_ModifyingOriginalDoesNotAffectChoice() {
        List<Card> mutableOptions = new ArrayList<>(OPTIONS);
        PendingPlayerChoice choice = new PendingPlayerChoice(
                PLAYER_ID, TYPE, mutableOptions, MIN_PICK, MAX_PICK, PROMPT, COIN_FLIP_HEADS, CALLBACK);

        mutableOptions.add(createCard("c3"));

        assertThat(choice.getOptions()).hasSize(2);
    }

    @Test
    void callback_CanBeInvoked() {
        AtomicReference<List<String>> captured = new AtomicReference<>();
        Consumer<List<String>> capturingCallback = captured::set;
        List<String> expectedIds = List.of("c1", "c2");

        PendingPlayerChoice choice = new PendingPlayerChoice(
                PLAYER_ID, TYPE, OPTIONS, MIN_PICK, MAX_PICK, PROMPT, COIN_FLIP_HEADS, capturingCallback);

        choice.getCallback().accept(expectedIds);

        assertThat(captured.get()).isEqualTo(expectedIds);
    }

    @Test
    void callback_WithEmptyList_TriggersCorrectly() {
        AtomicReference<List<String>> captured = new AtomicReference<>();
        Consumer<List<String>> capturingCallback = captured::set;

        PendingPlayerChoice choice = new PendingPlayerChoice(
                PLAYER_ID, ChoiceType.COIN_FLIP_INFO, List.of(), 0, 0, null, true, capturingCallback);

        choice.getCallback().accept(List.of());

        assertThat(captured.get()).isEmpty();
    }

    @Test
    void callback_WithNullList_TriggersCorrectly() {
        AtomicReference<List<String>> captured = new AtomicReference<>();
        Consumer<List<String>> capturingCallback = captured::set;

        PendingPlayerChoice choice = new PendingPlayerChoice(
                PLAYER_ID, TYPE, OPTIONS, MIN_PICK, MAX_PICK, PROMPT, COIN_FLIP_HEADS, capturingCallback);

        choice.getCallback().accept(null);

        assertThat(captured.get()).isNull();
    }

    @Test
    void callback_LambdaExecutesSuccessfully() {
        StringBuilder sb = new StringBuilder();
        Consumer<List<String>> appendingCallback = ids -> sb.append("called");

        PendingPlayerChoice choice = new PendingPlayerChoice(
                PLAYER_ID, TYPE, OPTIONS, MIN_PICK, MAX_PICK, PROMPT, COIN_FLIP_HEADS, appendingCallback);

        choice.getCallback().accept(List.of());

        assertThat(sb.toString()).isEqualTo("called");
    }

    @Test
    void callback_IsTransient() throws Exception {
        PendingPlayerChoice choice = new PendingPlayerChoice(
                PLAYER_ID, TYPE, OPTIONS, MIN_PICK, MAX_PICK, PROMPT, COIN_FLIP_HEADS, CALLBACK);

        java.lang.reflect.Field callbackField = PendingPlayerChoice.class.getDeclaredField("callback");
        assertThat(callbackField.getModifiers())
                .matches(mod -> java.lang.reflect.Modifier.isTransient(mod));
    }

    @Test
    void constructor_WithSameParams_ProducesDistinctInstance() {
        PendingPlayerChoice choice1 = new PendingPlayerChoice(
                PLAYER_ID, TYPE, OPTIONS, MIN_PICK, MAX_PICK, PROMPT, COIN_FLIP_HEADS, ids -> {});
        PendingPlayerChoice choice2 = new PendingPlayerChoice(
                PLAYER_ID, TYPE, OPTIONS, MIN_PICK, MAX_PICK, PROMPT, COIN_FLIP_HEADS, ids -> {});

        assertThat(choice1).isNotSameAs(choice2);
    }

    private static Card createCard(String id) {
        return PokemonCard.builder()
                .id(id)
                .name("Pokemon-" + id)
                .hp(60)
                .stage(PokemonStage.BASIC)
                .pokemonType(EnergyType.COLORLESS)
                .attacks(List.of())
                .abilities(List.of())
                .weaknesses(List.of())
                .resistances(List.of())
                .costToAttack(List.of())
                .retreatCost(0)
                .build();
    }
}
