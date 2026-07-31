package ar.edu.utn.frc.tup.piii.models.effects.trainer;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import ar.edu.utn.frc.tup.piii.models.TrainerEffect;
import ar.edu.utn.frc.tup.piii.models.effects.TrainerEffectRegistry;

class TrainerEffectRegistryTest {

    private TrainerEffectRegistry registry;

    @BeforeEach
    void setUp() {
        registry = new TrainerEffectRegistry();
    }

    @Test
    void registry_All15CardIdsResolveToNonNullEffect() {
        assertThat(registry.getTrainerEffect("xy1-115")).isNotNull();
        assertThat(registry.getTrainerEffect("xy1-116")).isNotNull();
        assertThat(registry.getTrainerEffect("xy1-117")).isNotNull();
        assertThat(registry.getTrainerEffect("xy1-118")).isNotNull();
        assertThat(registry.getTrainerEffect("xy1-119")).isNotNull();
        assertThat(registry.getTrainerEffect("xy1-120")).isNotNull();
        assertThat(registry.getTrainerEffect("xy1-121")).isNotNull();
        assertThat(registry.getTrainerEffect("xy1-122")).isNotNull();
        assertThat(registry.getTrainerEffect("xy1-123")).isNotNull();
        assertThat(registry.getTrainerEffect("xy1-124")).isNotNull();
        assertThat(registry.getTrainerEffect("xy1-125")).isNotNull();
        assertThat(registry.getTrainerEffect("xy1-126")).isNotNull();
        assertThat(registry.getTrainerEffect("xy1-127")).isNotNull();
        assertThat(registry.getTrainerEffect("xy1-128")).isNotNull();
        assertThat(registry.getTrainerEffect("xy1-129")).isNotNull();
    }

    @Test
    void registry_CorrectTypesForEachEffect() {
        assertThat(registry.getTrainerEffect("xy1-115")).isInstanceOf(CassiusEffect.class);
        assertThat(registry.getTrainerEffect("xy1-116")).isInstanceOf(EvosodaEffect.class);
        assertThat(registry.getTrainerEffect("xy1-117")).isInstanceOf(FairyGardenEffect.class);
        assertThat(registry.getTrainerEffect("xy1-118")).isInstanceOf(GreatBallEffect.class);
        assertThat(registry.getTrainerEffect("xy1-119")).isInstanceOf(HardCharmEffect.class);
        assertThat(registry.getTrainerEffect("xy1-120")).isInstanceOf(MaxReviveEffect.class);
        assertThat(registry.getTrainerEffect("xy1-121")).isInstanceOf(MuscleBandEffect.class);
        assertThat(registry.getTrainerEffect("xy1-122")).isInstanceOf(ProfessorSycamoreEffect.class);
        assertThat(registry.getTrainerEffect("xy1-123")).isInstanceOf(ProfessorsLetterEffect.class);
        assertThat(registry.getTrainerEffect("xy1-124")).isInstanceOf(RedCardEffect.class);
        assertThat(registry.getTrainerEffect("xy1-125")).isInstanceOf(RollerSkatesEffect.class);
        assertThat(registry.getTrainerEffect("xy1-126")).isInstanceOf(ShadowCircleEffect.class);
        assertThat(registry.getTrainerEffect("xy1-127")).isInstanceOf(ShaunaEffect.class);
        assertThat(registry.getTrainerEffect("xy1-128")).isInstanceOf(SuperPotionEffect.class);
        assertThat(registry.getTrainerEffect("xy1-129")).isInstanceOf(TeamFlareGruntEffect.class);
    }

    @Test
    void registry_UnknownId_ReturnsNull() {
        assertThat(registry.getTrainerEffect("xy1-999")).isNull();
        assertThat(registry.getTrainerEffect("unknown")).isNull();
    }

    @Test
    void registry_RegisteredKeys_Returns15Entries() {
        assertThat(registry.registeredKeys()).hasSize(15);
    }
}
