package dev.itzgamesgd.moreminecrafts.accessory;

/** The five biome affinity cores specified in the November 13 accessory authority. */
public enum ArtifactCore {
    EARTHEN("earthen_relic_core", "대지의 유물핵", "Earthen Relic Core"),
    ABYSSAL("abyssal_relic_core", "심연의 유물핵", "Abyssal Relic Core"),
    GLACIAL("glacial_relic_core", "한기의 유물핵", "Glacial Relic Core"),
    TIDAL("tidal_relic_core", "해류의 유물핵", "Tidal Relic Core"),
    BLAZING("blazing_relic_core", "작열의 유물핵", "Blazing Relic Core");

    private final String id;
    private final String koreanName;
    private final String englishName;

    ArtifactCore(String id, String koreanName, String englishName) {
        this.id = id;
        this.koreanName = koreanName;
        this.englishName = englishName;
    }

    public String id() { return id; }
    public String koreanName() { return koreanName; }
    public String englishName() { return englishName; }
}
