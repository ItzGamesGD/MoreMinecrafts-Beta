package dev.itzgamesgd.moreminecrafts.progression;

/** Data for one non-repeatable quest; no schedule or reset behavior is represented. */
public record QuestDefinition(String id, String category, String titleKey, String descriptionKey,
                              String objectiveKey, int objectiveTarget, int rewardRawXp) {
    public QuestDefinition {
        if (id == null || !id.matches("[a-z0-9_./-]+") || objectiveTarget <= 0 || rewardRawXp < 0)
            throw new IllegalArgumentException("Invalid quest definition");
    }
}
