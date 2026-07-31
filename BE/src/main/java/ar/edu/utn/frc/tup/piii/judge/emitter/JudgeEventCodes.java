package ar.edu.utn.frc.tup.piii.judge.emitter;

public final class JudgeEventCodes {

    public static final String ACTION_BLOCKED_WRONG_PHASE = "ACTION_BLOCKED_WRONG_PHASE";

    public static final String ACTION_BLOCKED_CANT_ATTACK_STATUS_SLEEP =
            "ACTION_BLOCKED_CANT_ATTACK_STATUS_SLEEP";
    public static final String ACTION_BLOCKED_CANT_ATTACK_STATUS_PARALYZED =
            "ACTION_BLOCKED_CANT_ATTACK_STATUS_PARALYZED";
    public static final String ACTION_BLOCKED_CANT_ATTACK_FLAG =
            "ACTION_BLOCKED_CANT_ATTACK_FLAG";
    public static final String ACTION_BLOCKED_CANT_ATTACK_SPECIFIC_NAME =
            "ACTION_BLOCKED_CANT_ATTACK_SPECIFIC_NAME";
    public static final String ACTION_BLOCKED_CANT_ATTACK_ENERGY =
            "ACTION_BLOCKED_CANT_ATTACK_ENERGY";

    public static final String ACTION_BLOCKED_CANT_RETREAT_ENERGY =
            "ACTION_BLOCKED_CANT_RETREAT_ENERGY";
    public static final String ACTION_BLOCKED_CANT_RETREAT_FLAG =
            "ACTION_BLOCKED_CANT_RETREAT_FLAG";
    public static final String ACTION_BLOCKED_CANT_RETREAT_STATUS_PARALYZED =
            "ACTION_BLOCKED_CANT_RETREAT_STATUS_PARALYZED";
    public static final String ACTION_BLOCKED_CANT_RETREAT_STATUS_SLEEP =
            "ACTION_BLOCKED_CANT_RETREAT_STATUS_SLEEP";
    public static final String ACTION_BLOCKED_RETREAT_ALREADY_USED =
            "ACTION_BLOCKED_RETREAT_ALREADY_USED";

    public static final String ACTION_BLOCKED_CANT_PLAY_SUPPORTER_QUOTA =
            "ACTION_BLOCKED_CANT_PLAY_SUPPORTER_QUOTA";
    public static final String ACTION_BLOCKED_CANT_PLAY_SUPPORTER_FLAG =
            "ACTION_BLOCKED_CANT_PLAY_SUPPORTER_FLAG";

    public static final String ACTION_BLOCKED_CANT_EVOLVE_FIRST_TURN =
            "ACTION_BLOCKED_CANT_EVOLVE_FIRST_TURN";
    public static final String ACTION_BLOCKED_CANT_EVOLVE_PLAYED_THIS_TURN =
            "ACTION_BLOCKED_CANT_EVOLVE_PLAYED_THIS_TURN";

    public static final String ACTION_BLOCKED_CANT_ATTACH_ENERGY_QUOTA =
            "ACTION_BLOCKED_CANT_ATTACH_ENERGY_QUOTA";
    public static final String ACTION_BLOCKED_BENCH_FULL = "ACTION_BLOCKED_BENCH_FULL";

    public static final String DAMAGE_FINAL_APPLIED = "DAMAGE_FINAL_APPLIED";
    public static final String DAMAGE_FINAL_ZERO = "DAMAGE_FINAL_ZERO";

    public static final String STATUS_APPLIED = "STATUS_APPLIED";
    public static final String STATUS_CURED = "STATUS_CURED";

    public static final String POKEMON_KO_BY_DAMAGE = "POKEMON_KO_BY_DAMAGE";

    public static final String VICTORY_PRIZES_DEPLETED = "VICTORY_PRIZES_DEPLETED";
    public static final String VICTORY_NO_POKEMON_REMAINING = "VICTORY_NO_POKEMON_REMAINING";
    public static final String VICTORY_DECK_OUT = "VICTORY_DECK_OUT";

    private JudgeEventCodes() {
    }
}
