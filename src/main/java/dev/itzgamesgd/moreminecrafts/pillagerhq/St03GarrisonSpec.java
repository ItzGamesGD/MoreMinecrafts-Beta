package dev.itzgamesgd.moreminecrafts.pillagerhq;

import dev.itzgamesgd.moreminecrafts.pillagerhq.worldgen.St03PlacementManifest;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Rotation;

/**
 * Fixed 40-defender nominal garrison roster inside the 112x112 q=0 compound:
 * 35 existing mod/vanilla roster (BANDIT x9, BANDIT_WARRIOR x8, BANDIT_ARCHER x12, WANDERING_RAIDER x5, RAVAGER x1)
 * + 5 dedicated ST-03 resource variants (PILLAGER_COMMANDER x1, PILLAGER_BEASTMASTER x1, ARMORY_GUARD x2, PILLAGER_DRILLMASTER x1),
 * plus 2 non-combat Villager prisoners in F09 (outside the 40-defender elimination cap).
 */
public final class St03GarrisonSpec {
    private St03GarrisonSpec() {}

    public enum Role {
        BANDIT,
        BANDIT_WARRIOR,
        BANDIT_ARCHER,
        WANDERING_RAIDER,
        RAVAGER,
        PILLAGER_COMMANDER,
        PILLAGER_BEASTMASTER,
        ARMORY_GUARD,
        PILLAGER_DRILLMASTER
    }

    public record SpawnSlot(int index, Role role, int lx, int ly, int lz) {
        public BlockPos worldPos(BlockPos origin, Rotation rotation) {
            int[] xz = St03PlacementManifest.rotateLocalIn112(lx, lz, rotation);
            return origin.offset(xz[0], ly, xz[1]);
        }
    }

    public record PrisonerSlot(int lx, int ly, int lz) {
        public BlockPos worldPos(BlockPos origin, Rotation rotation) {
            int[] xz = St03PlacementManifest.rotateLocalIn112(lx, lz, rotation);
            return origin.offset(xz[0], ly, xz[1]);
        }
    }

    /** 2 non-combat Villager prisoners in F09 Prison (outside the 40-defender cap). */
    public static final List<PrisonerSlot> PRISONER_SLOTS = List.of(
            new PrisonerSlot(95, 1, 70),
            new PrisonerSlot(99, 1, 70)
    );

    public static final List<SpawnSlot> SLOTS = List.of(
            // Corner watchtowers (4): 4x Bandit Archer
            new SpawnSlot(0, Role.BANDIT_ARCHER, 6, 17, 6),
            new SpawnSlot(1, Role.BANDIT_ARCHER, 105, 17, 6),
            new SpawnSlot(2, Role.BANDIT_ARCHER, 105, 17, 105),
            new SpawnSlot(3, Role.BANDIT_ARCHER, 6, 17, 105),

            // Central plaza & South Gate (3): 2x Wandering Raider, 1x Bandit Warrior
            new SpawnSlot(4, Role.WANDERING_RAIDER, 53, 1, 104),
            new SpawnSlot(5, Role.BANDIT_WARRIOR, 58, 1, 104),
            new SpawnSlot(6, Role.WANDERING_RAIDER, 55, 1, 60),

            // F02-F04 Barracks (3 buildings, 9): 3x Bandit, 3x Bandit Warrior, 3x Bandit Archer
            new SpawnSlot(7, Role.BANDIT, 50, 1, 32),
            new SpawnSlot(8, Role.BANDIT_WARRIOR, 55, 1, 35),
            new SpawnSlot(9, Role.BANDIT_ARCHER, 61, 1, 32),
            new SpawnSlot(10, Role.BANDIT, 30, 1, 29),
            new SpawnSlot(11, Role.BANDIT_WARRIOR, 81, 1, 44),
            new SpawnSlot(12, Role.BANDIT_ARCHER, 81, 1, 29),
            new SpawnSlot(13, Role.BANDIT, 18, 1, 14),
            new SpawnSlot(14, Role.BANDIT_WARRIOR, 95, 1, 43),
            new SpawnSlot(15, Role.BANDIT_ARCHER, 93, 1, 14),

            // F07-F08 Warehouses & outbuildings (5): 3x Wandering Raider, 2x Bandit
            new SpawnSlot(16, Role.WANDERING_RAIDER, 17, 1, 85),
            new SpawnSlot(17, Role.WANDERING_RAIDER, 81, 1, 60),
            new SpawnSlot(18, Role.WANDERING_RAIDER, 96, 1, 54),
            new SpawnSlot(19, Role.BANDIT, 32, 1, 83),
            new SpawnSlot(20, Role.BANDIT, 103, 1, 55),

            // F09 Prison (2): 1x Bandit Warrior, 1x Bandit (+ 2 Villager prisoners in PRISONER_SLOTS)
            new SpawnSlot(21, Role.BANDIT_WARRIOR, 97, 1, 68),
            new SpawnSlot(22, Role.BANDIT, 97, 1, 65),

            // F10 Ravager stable (4): 1x Ravager (confirmed), 1x Pillager Beastmaster, 2x Bandit
            new SpawnSlot(23, Role.RAVAGER, 85, 1, 85),
            new SpawnSlot(24, Role.PILLAGER_BEASTMASTER, 82, 1, 85),
            new SpawnSlot(25, Role.BANDIT, 88, 1, 85),
            new SpawnSlot(26, Role.BANDIT, 85, 1, 82),

            // F05 Armory (3): 2x Armory Guard, 1x Bandit Archer
            new SpawnSlot(27, Role.ARMORY_GUARD, 20, 1, 45),
            new SpawnSlot(28, Role.ARMORY_GUARD, 22, 1, 45),
            new SpawnSlot(29, Role.BANDIT_ARCHER, 20, 1, 58),

            // F06 Training yard (4): 1x Pillager Drillmaster, 1x Bandit, 2x Bandit Archer
            new SpawnSlot(30, Role.PILLAGER_DRILLMASTER, 30, 1, 58),
            new SpawnSlot(31, Role.BANDIT, 22, 1, 66),
            new SpawnSlot(32, Role.BANDIT_ARCHER, 24, 1, 62),
            new SpawnSlot(33, Role.BANDIT_ARCHER, 28, 1, 62),

            // F01 Commander manor 1F (4): 2x Bandit Warrior, 2x Bandit Archer
            new SpawnSlot(34, Role.BANDIT_WARRIOR, 53, 1, 16),
            new SpawnSlot(35, Role.BANDIT_WARRIOR, 59, 1, 16),
            new SpawnSlot(36, Role.BANDIT_ARCHER, 54, 1, 20),
            new SpawnSlot(37, Role.BANDIT_ARCHER, 58, 1, 20),

            // F01 Commander manor 2F (2): 1x Pillager Commander, 1x Bandit Warrior
            new SpawnSlot(38, Role.BANDIT_WARRIOR, 56, 7, 18),
            new SpawnSlot(39, Role.PILLAGER_COMMANDER, 56, 7, 14)
    );
}
