package dev.itzgamesgd.moreminecrafts.progression;

import java.util.List;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.monster.cubemob.Slime;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;

/** Server runtime for Slime research (`v3.research.41`): prevents splitting and awards recursive loot & XP once. */
public final class ResearchSlimeSplitRuntime {
    private ResearchSlimeSplitRuntime() {}

    public static void handleSplitPrevention(Slime slime, ServerPlayer killer) {
        if (!(slime.level() instanceof ServerLevel level) || killer == null) return;
        int parentSize = slime.getSize();
        if (!ResearchSlimeSplitRules.shouldPreventSplit(
                ResearchRewards.level(killer, "v3.research.41", "slime_split_prevention"),
                parentSize, true, slime.isDeadOrDying())) {
            return;
        }
        // Prevent vanilla AbstractCubeMob.remove(KILLED) from spawning split children.
        slime.setSize(1, false);

        List<Integer> descendants = ResearchSlimeSplitRules.descendantSizes(
                parentSize, bound -> level.getRandom().nextInt(bound));
        if (descendants.isEmpty()) return;

        var defaultTable = slime.getType().getDefaultLootTable();
        if (defaultTable.isPresent()) {
            LootTable table = level.getServer().reloadableRegistries().getLootTable(defaultTable.get());
            if (table != LootTable.EMPTY) {
                for (int childSize : descendants) {
                    awardDescendantLoot(level, slime, killer, table, childSize);
                }
            }
        }

        int totalXp = ResearchSlimeSplitRules.totalDescendantXp(descendants);
        if (totalXp > 0) {
            try (var ignored = ResearchXpOrbContext.enter(
                    killer, "ecology_root", "hostile_xp_orbs", "ecology_hostile")) {
                ExperienceOrb.award(level, slime.position(), totalXp);
            }
        }
    }

    private static void awardDescendantLoot(ServerLevel level, Slime parent, ServerPlayer killer,
                                            LootTable table, int childSize) {
        Entity created = parent.getType().create(level, EntitySpawnReason.COMMAND);
        if (!(created instanceof Slime virtualChild)) return;
        virtualChild.setSize(childSize, true);
        virtualChild.setPos(parent.getX(), parent.getY(), parent.getZ());
        LootParams params = new LootParams.Builder(level)
                .withParameter(LootContextParams.THIS_ENTITY, virtualChild)
                .withParameter(LootContextParams.ORIGIN, parent.position())
                .withParameter(LootContextParams.DAMAGE_SOURCE, level.damageSources().playerAttack(killer))
                .withParameter(LootContextParams.ATTACKING_ENTITY, killer)
                .withParameter(LootContextParams.DIRECT_ATTACKING_ENTITY, killer)
                .withParameter(LootContextParams.LAST_DAMAGE_PLAYER, killer)
                .create(LootContextParamSets.ENTITY);
        for (ItemStack drop : table.getRandomItems(params)) {
            if (!drop.isEmpty()) parent.spawnAtLocation(level, drop);
        }
    }
}
