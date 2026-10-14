package dev.itzgamesgd.moreminecrafts.enhancement;

/** A deterministic milestone hook; profiles may expose any natural number of hooks. */
public record MilestoneEffect(Kind kind, int firstLevel, int interval) {
    public enum Kind {
        MACE_SMASH_SCALING_SEAM,
        BOW_DRAW_SPEED_CANDIDATE,
        CROSSBOW_RELOAD_SPEED_CANDIDATE,
        TOOL_DURABILITY_CANDIDATE,
        TOOL_UTILITY_CANDIDATE,
        ARMOR_UTILITY_CANDIDATE
    }

    public boolean activeAt(int level) {
        return level >= firstLevel && (level - firstLevel) % interval == 0;
    }
}
