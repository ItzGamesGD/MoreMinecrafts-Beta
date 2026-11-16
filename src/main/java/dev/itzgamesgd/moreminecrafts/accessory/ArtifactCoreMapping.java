package dev.itzgamesgd.moreminecrafts.accessory;

import java.util.List;

/** Only accessory identifiers explicitly published in the accessory-sync mapping table are populated. */
public final class ArtifactCoreMapping {
    public record Entry(ArtifactCore core, String accessoryId, String koreanName) {}

    public static final List<Entry> ALL = List.of(
            e(ArtifactCore.EARTHEN, "t1_combo", "전술가의 반지"),
            e(ArtifactCore.EARTHEN, "t1_sprint_acceleration", "질풍의 고리"),
            e(ArtifactCore.EARTHEN, "phantom_mirror", "환영의 거울"),
            e(ArtifactCore.EARTHEN, "soul_chain", "영혼의 사슬"),
            e(ArtifactCore.ABYSSAL, "t1_status_resilience", "정화의 송곳니"),
            e(ArtifactCore.ABYSSAL, "t1_sustained_recovery", "활력의 펜던트"),
            e(ArtifactCore.ABYSSAL, "heart_of_gravity", "중력의 심장"),
            e(ArtifactCore.ABYSSAL, "seed_of_life", "생명의 씨앗"),
            e(ArtifactCore.GLACIAL, "t1_double_jump", "도약의 깃털 팔찌"),
            e(ArtifactCore.GLACIAL, "ring_of_time", "시간의 고리"),
            e(ArtifactCore.GLACIAL, "snow_charm", "설원의 부적"),
            new Entry(ArtifactCore.GLACIAL, null, "깃털 부적"),
            e(ArtifactCore.TIDAL, "deep_sea_pearl", "심해의 진주"),
            e(ArtifactCore.TIDAL, "whisper_of_the_sea", "바다의 속삭임"),
            e(ArtifactCore.BLAZING, "t1_crisis_escape", "충격파의 브로치"),
            e(ArtifactCore.BLAZING, "t1_heavy_buffer", "중갑의 문장"),
            e(ArtifactCore.BLAZING, "star_covenant", "별의 계약"),
            e(ArtifactCore.BLAZING, "blood_covenant", "피의 계약"),
            e(ArtifactCore.BLAZING, "ember_ring", "잿불 반지"),
            e(ArtifactCore.BLAZING, "ring_of_life", "생명의 고리"));

    private ArtifactCoreMapping() {}
    private static Entry e(ArtifactCore core, String id, String name) { return new Entry(core, id, name); }
}
