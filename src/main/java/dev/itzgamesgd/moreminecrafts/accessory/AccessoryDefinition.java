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
    SOUL_CHAIN("soul_chain", "item.moreminecrafts.soul_chain", AccessoryCategory.COMBAT);

    public static final List<AccessoryDefinition> ALL = List.of(values());

    private final String registryId;
    private final String translationKey;
    private final AccessoryCategory category;

    AccessoryDefinition(String registryId, String translationKey, AccessoryCategory category) {
        this.registryId = registryId;
        this.translationKey = translationKey;
        this.category = category;
    }

    public String registryId() { return registryId; }
    public String translationKey() { return translationKey; }
    public AccessoryCategory category() { return category; }
}
