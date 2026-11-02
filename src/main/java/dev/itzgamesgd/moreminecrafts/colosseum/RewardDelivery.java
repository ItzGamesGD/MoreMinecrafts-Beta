package dev.itzgamesgd.moreminecrafts.colosseum;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

/** Durable, idempotent plan for one shared Trial Key world-entity delivery. */
final class RewardDelivery {
    enum State { PLANNED, INSERTING, DELIVERED, QUARANTINED }
    enum Reconcile { RETRY, CONFIRMED, ALREADY_DELIVERED, QUARANTINE }

    String challengeId;
    String roundId;
    String rewardId;
    String itemId;
    State state = State.PLANNED;
    String entityId;

    RewardDelivery() {}

    private RewardDelivery(UUID challenge, int round, boolean ominous) {
        challengeId = challenge.toString();
        roundId = Integer.toString(round);
        rewardId = sharedKeyId(challenge, round, ominous);
        itemId = ominous ? "minecraft:ominous_trial_key" : "minecraft:trial_key";
    }

    static String sharedKeyId(UUID challenge, int round, boolean ominous) {
        return challenge + ":round:" + round + ":reward:" + (ominous ? "ominous_trial_key" : "trial_key");
    }

    static RewardDelivery sharedKey(UUID challenge, int round, boolean ominous) {
        if (round < 1 || round > 7 || ominous != (round == 7)) throw new IllegalArgumentException("invalid shared key round/kind");
        return new RewardDelivery(challenge, round, ominous);
    }

    boolean matches(UUID challenge, int round, boolean ominous) {
        return challengeId != null && challengeId.equals(challenge.toString())
                && roundId != null && roundId.equals(Integer.toString(round))
                && rewardId != null && rewardId.equals(sharedKeyId(challenge, round, ominous))
                && itemId != null && itemId.equals(ominous ? "minecraft:ominous_trial_key" : "minecraft:trial_key");
    }

    void begin(UUID entity) {
        if (state != State.PLANNED) throw new IllegalStateException("reward delivery is not safely plannable");
        state = State.INSERTING;
        entityId = entity.toString();
    }

    void delivered(UUID entity) {
        if (state != State.INSERTING || entityId == null || !entityId.equals(entity.toString()))
            throw new IllegalStateException("reward receipt does not match its delivery plan");
        state = State.DELIVERED;
    }

    Reconcile reconcile(int matchingEntities) {
        if (matchingEntities < 0) return quarantine();
        if (matchingEntities > 1) return quarantine();
        if (matchingEntities == 1) {
            if (state == State.QUARANTINED) return Reconcile.QUARANTINE;
            state = State.DELIVERED;
            return Reconcile.CONFIRMED;
        }
        if (state == State.PLANNED) return Reconcile.RETRY;
        if (state == State.DELIVERED) return Reconcile.ALREADY_DELIVERED;
        return quarantine();
    }

    private Reconcile quarantine() { state = State.QUARANTINED; return Reconcile.QUARANTINE; }

    static UUID entityUuid(String rewardId) {
        return UUID.nameUUIDFromBytes(("moreminecrafts:st05:" + rewardId).getBytes(StandardCharsets.UTF_8));
    }
}
