package dev.itzgamesgd.moreminecrafts.ability;

/** The only information a client is allowed to provide for an equipment skill request. */
public enum EquipmentSkillInput {
    PRIMARY,
    SECONDARY;

    public static EquipmentSkillInput fromNetworkId(int id) {
        return id >= 0 && id < values().length ? values()[id] : null;
    }
}
