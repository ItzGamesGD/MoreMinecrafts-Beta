package dev.itzgamesgd.moreminecrafts.colosseum;

import java.util.EnumSet;
import java.util.Set;

/**
 * The exactly-15 active Q109 baseline roster with provisional threat scores, and the owner-approved R3 role
 * classification. Enum names are persisted; never rename them. The 8 conditional candidates live in
 * {@link ColosseumConditionalCandidate} and can never be allocated by this enum.
 */
public enum ColosseumMob {
    ZOMBIE(3, Role.MELEE),
    BABY_ZOMBIE(2, Role.MELEE),
    HUSK(3, Role.MELEE, Role.SPECIAL),
    SKELETON(4, Role.RANGED),
    BOGGED(4, Role.RANGED, Role.SPECIAL),
    STRAY(5, Role.RANGED, Role.SPECIAL),
    BREEZE(7, Role.RANGED, Role.SPECIAL),
    CAVE_ZOMBIE(3, Role.MELEE),
    CAVE_SKELETON(3, Role.MELEE),
    BANDIT(4, Role.MELEE),
    BANDIT_ARCHER(4, Role.RANGED),
    LARGE_ZOMBIE(6, Role.MELEE),
    LESSER_WITHER_SKELETON(6, Role.MELEE, Role.SPECIAL),
    ARMORED_SKELETON(7, Role.RANGED),
    BANDIT_WARRIOR(7, Role.MELEE);

    public enum Role { MELEE, RANGED, SPECIAL }

    private final int threat;
    private final Set<Role> roles;

    ColosseumMob(int threat, Role first, Role... rest) {
        this.threat = threat;
        this.roles = EnumSet.of(first, rest);
    }

    public int threat() { return threat; }

    public Set<Role> roles() { return roles; }

    public boolean hasRole(Role role) { return roles.contains(role); }

    public boolean isElite() { return threat >= ColosseumRules.ELITE_SCORE; }
}
