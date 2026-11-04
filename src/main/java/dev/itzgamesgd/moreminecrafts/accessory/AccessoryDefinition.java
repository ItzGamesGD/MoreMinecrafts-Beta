package dev.itzgamesgd.moreminecrafts.accessory;

import java.util.List;

/** Registry identity and finalized category for an adopted accessory. */
public enum AccessoryDefinition {
    SNOW_CHARM("snow_charm", "item.moreminecrafts.snow_charm", AccessoryCategory.EXPLORATION_MOBILITY),
    DEEP_SEA_PEARL("deep_sea_pearl", "item.moreminecrafts.deep_sea_pearl", AccessoryCategory.EXPLORATION_MOBILITY),
    EMBER_RING("ember_ring", "item.moreminecrafts.ember_ring", AccessoryCategory.DEFENSE_SURVIVAL),
    RING_OF_LIFE("ring_of_life", "item.moreminecrafts.ring_of_life", AccessoryCategory.DEFENSE_SURVIVAL),
    WHISPER_OF_THE_SEA("whisper_of_the_sea", "item.moreminecrafts.whisper_of_the_sea", AccessoryCategory.LIFESTYLE_GATHERING),
    STAR_COVENANT("star_covenant", "item.moreminecrafts.star_covenant", AccessoryCategory.COMBAT),
    PHANTOM_MIRROR("phantom_mirror", "item.moreminecrafts.phantom_mirror", AccessoryCategory.COMBAT),
    RING_OF_TIME("ring_of_time", "item.moreminecrafts.ring_of_time", AccessoryCategory.COMBAT),
    BLOOD_COVENANT("blood_covenant", "item.moreminecrafts.blood_covenant", AccessoryCategory.COMBAT),
    SEED_OF_LIFE("seed_of_life", "item.moreminecrafts.seed_of_life", AccessoryCategory.SUPPORT_RECOVERY),
    HEART_OF_GRAVITY("heart_of_gravity", "item.moreminecrafts.heart_of_gravity", AccessoryCategory.COMBAT),
    SOUL_CHAIN("soul_chain", "item.moreminecrafts.soul_chain", AccessoryCategory.COMBAT),
    T1_COMBO("t1_combo", "item.moreminecrafts.t1_combo", AccessoryCategory.COMBAT),
    T1_CRISIS_ESCAPE("t1_crisis_escape", "item.moreminecrafts.t1_crisis_escape", AccessoryCategory.COMBAT),
    T1_HEAVY_BUFFER("t1_heavy_buffer", "item.moreminecrafts.t1_heavy_buffer", AccessoryCategory.DEFENSE_SURVIVAL),
    T1_STATUS_RESILIENCE("t1_status_resilience", "item.moreminecrafts.t1_status_resilience", AccessoryCategory.DEFENSE_SURVIVAL),
    T1_SUSTAINED_RECOVERY("t1_sustained_recovery", "item.moreminecrafts.t1_sustained_recovery", AccessoryCategory.SUPPORT_RECOVERY),
    T1_SPRINT_ACCELERATION("t1_sprint_acceleration", "item.moreminecrafts.t1_sprint_acceleration", AccessoryCategory.EXPLORATION_MOBILITY),
    T1_DOUBLE_JUMP("t1_double_jump", "item.moreminecrafts.t1_double_jump", AccessoryCategory.EXPLORATION_MOBILITY);

    public static final List<AccessoryDefinition> ALL = List.of(values());

    private final String registryId;
    private final String translationKey;
    private final AccessoryCategory category;

    AccessoryDefinition(String registryId, String translationKey, AccessoryCategory category) {
        this.registryId = registryId;
        this.translationKey = translationKey;
        this.category = category;
    }

    /** Stable save identifiers are independent of provisional display names. */
    public boolean isT1() { return registryId.startsWith("t1_"); }
    public int maxEnhancementLevel() { return isT1() ? 5 : 10; }

    public String registryId() { return registryId; }
    public String translationKey() { return translationKey; }
    public AccessoryCategory category() { return category; }
}
