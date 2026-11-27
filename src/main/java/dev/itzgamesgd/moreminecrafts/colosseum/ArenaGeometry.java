package dev.itzgamesgd.moreminecrafts.colosseum;

/**
 * Authoritative arena interior derived from the Stage 0-F construction plan (ArenaDesign): combat floor radius {@value #IN_R} around the fixed centre,
 * from the first air layer above the combat floor up to the total structure height. The wall ring (radius 29 to 34), the four entrance arches,
 * corridors and the natural cave all lie outside this volume, so they never trigger entry. The central crystal is not part of any rule here.
 */
public record ArenaGeometry(int cx, int cz, int floorY) {
    public static final int IN_R = 29, TOTAL_H = 23, EXT_R = 34, SPAWN_MIN_R = 12, SPAWN_MAX_R = 24;

    public boolean contains(int x, int y, int z) {
        long dx = x - cx, dz = z - cz;
        if (dx * dx + dz * dz > (long) IN_R * IN_R) return false;
        return y >= floorY && y < floorY + TOTAL_H;
    }

    public boolean annulus(int x, int z) {
        long dx = x - cx, dz = z - cz, d2 = dx * dx + dz * dz;
        return d2 >= (long) SPAWN_MIN_R * SPAWN_MIN_R && d2 <= (long) SPAWN_MAX_R * SPAWN_MAX_R;
    }
}
