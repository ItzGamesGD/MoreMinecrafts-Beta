package dev.itzgamesgd.moreminecrafts.skytower;

import dev.itzgamesgd.moreminecrafts.MoreMinecrafts;
import dev.itzgamesgd.moreminecrafts.accessory.StarCovenantVaultRewards;
import java.util.ArrayList;
import java.util.List;
import java.util.function.DoubleSupplier;
import java.util.function.Supplier;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.VaultBlock;
import net.minecraft.world.level.block.entity.vault.VaultBlockEntity;
import net.minecraft.world.level.block.entity.vault.VaultConfig;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.loot.LootTable;

/**
 * Unified custom Vault rewards framework for the Sky Tower.
 * Scopes custom reward pools to Sky Tower vaults while preserving vanilla Vault
 * interaction mechanics, player eligibility, key consumption, cooldowns, and persistence.
 *
 * Normal Vaults (8 per tower): moreminecrafts:chests/sky_islands/vault
 * Rooftop Special Vault: moreminecrafts:chests/sky_islands/vault_tower_top + Star Covenant roll hook
 */
public final class SkyTowerVaultRewards {
    public static final Identifier NORMAL_VAULT_LOOT = Identifier.parse("moreminecrafts:chests/sky_islands/vault");
    public static final Identifier ROOFTOP_VAULT_LOOT = Identifier.parse("moreminecrafts:chests/sky_islands/vault_tower_top");

    public static final ResourceKey<LootTable> NORMAL_VAULT_LOOT_KEY =
            ResourceKey.create(Registries.LOOT_TABLE, NORMAL_VAULT_LOOT);
    public static final ResourceKey<LootTable> ROOFTOP_VAULT_LOOT_KEY =
            ResourceKey.create(Registries.LOOT_TABLE, ROOFTOP_VAULT_LOOT);

    public enum VaultType {
        NORMAL,
        ROOFTOP
    }

    private static final ThreadLocal<DoubleSupplier> ROLL_OVERRIDE = new ThreadLocal<>();

    private SkyTowerVaultRewards() {}

    /** Executes an action with a deterministic Star Covenant roll supplier (for testing). */
    public static <T> T withRollOverride(DoubleSupplier roll, Supplier<T> action) {
        DoubleSupplier previous = ROLL_OVERRIDE.get();
        ROLL_OVERRIDE.set(roll);
        try {
            return action.get();
        } finally {
            if (previous == null) {
                ROLL_OVERRIDE.remove();
            } else {
                ROLL_OVERRIDE.set(previous);
            }
        }
    }

    /** Executes a runnable with a deterministic Star Covenant roll supplier (for testing). */
    public static void withRollOverride(DoubleSupplier roll, Runnable action) {
        withRollOverride(roll, () -> {
            action.run();
            return null;
        });
    }

    /** Finds the registered Sky Tower site whose bounds contain the given position. */
    public static SkyTowerLayout.Site findSite(ServerLevel level, BlockPos pos) {
        if (level == null || pos == null) return null;
        var data = level.getServer().overworld().getDataStorage().computeIfAbsent(SkyTowerData.TYPE);
        if (data.quarantined() || data.sites().isEmpty()) return null;
        String dim = level.dimension().identifier().toString();
        for (var site : data.sites().values()) {
            if (!site.dimension().equals(dim)) continue;
            int[] b = site.bounds();
            if (pos.getX() >= b[0] && pos.getX() <= b[3]
                    && pos.getY() >= b[1] && pos.getY() <= b[4]
                    && pos.getZ() >= b[2] && pos.getZ() <= b[5]) {
                return site;
            }
        }
        return null;
    }

    /** Returns true if the position corresponds to one of the 8 normal combat vaults of a registered tower. */
    public static boolean isNormalVault(ServerLevel level, BlockPos pos) {
        var site = findSite(level, pos);
        if (site == null) return false;
        var pt = new SkyTowerLayout.Point(pos.getX(), pos.getY(), pos.getZ());
        for (int i = 0; i < 8; i++) {
            if (site.vault(i).equals(pt)) return true;
        }
        return false;
    }

    /** Returns 0..7 if pos matches one of the 8 normal vaults, or -1 otherwise. */
    public static int normalVaultIndex(SkyTowerLayout.Site site, BlockPos pos) {
        if (site == null || pos == null) return -1;
        var pt = new SkyTowerLayout.Point(pos.getX(), pos.getY(), pos.getZ());
        for (int i = 0; i < 8; i++) {
            if (site.vault(i).equals(pt)) return i;
        }
        return -1;
    }

    /** Returns true if pos is the rooftop special vault of a registered tower. */
    public static boolean isRooftopVault(ServerLevel level, BlockPos pos) {
        if (level == null || pos == null) return false;
        var site = findSite(level, pos);
        if (site == null) return false;
        var pt = site.rooftopVault();
        return pos.getX() == pt.x() && pos.getY() == pt.y() && pos.getZ() == pt.z();
    }

    /** Returns true if pos is any Sky Tower vault (normal or rooftop). */
    public static boolean isSkyTowerVault(ServerLevel level, BlockPos pos) {
        return isNormalVault(level, pos) || isRooftopVault(level, pos);
    }

    /**
     * Determines the horizontal facing direction for the rooftop vault based on site rotation.
     * Local facing is NORTH; rotated clockwise by rotation * 90 degrees.
     */
    public static Direction rooftopVaultFacing(int rotation) {
        return switch (rotation) {
            case 1 -> Direction.EAST;
            case 2 -> Direction.SOUTH;
            case 3 -> Direction.WEST;
            default -> Direction.NORTH;
        };
    }

    /**
     * Places and configures the rooftop special vault if the position is air,
     * or ensures configuration if already a vault.
     * Idempotent; never overwrites unrelated blocks and never resets player eligibility.
     */
    public static boolean placeAndConfigureRooftopVault(ServerLevel level, SkyTowerLayout.Site site) {
        if (level == null || site == null) return false;
        var pt = site.rooftopVault();
        var pos = new BlockPos(pt.x(), pt.y(), pt.z());
        if (!level.hasChunkAt(pos)) return false;

        BlockState current = level.getBlockState(pos);
        if (current.is(Blocks.VAULT)) {
            var be = level.getBlockEntity(pos);
            if (be instanceof VaultBlockEntity vault) {
                configureRooftopVault(vault, new ItemStack(Items.TRIAL_KEY));
                return true;
            }
            return false;
        }

        if (current.isAir()) {
            Direction facing = rooftopVaultFacing(site.rotation());
            BlockState vaultState = Blocks.VAULT.defaultBlockState()
                    .setValue(VaultBlock.FACING, facing)
                    .setValue(VaultBlock.OMINOUS, false);
            level.setBlock(pos, vaultState, 3);
            var be = level.getBlockEntity(pos);
            if (be instanceof VaultBlockEntity vault) {
                configureRooftopVault(vault, new ItemStack(Items.TRIAL_KEY));
                return true;
            }
        }
        return false;
    }

    /** Configures all 8 normal vaults and the rooftop special vault for the given site in the world. */
    public static int configureSiteVaults(ServerLevel level, SkyTowerLayout.Site site) {
        if (level == null || site == null) return 0;
        int configured = 0;
        for (int i = 0; i < 8; i++) {
            var pt = site.vault(i);
            var pos = new BlockPos(pt.x(), pt.y(), pt.z());
            if (level.hasChunkAt(pos)) {
                var be = level.getBlockEntity(pos);
                if (be instanceof VaultBlockEntity vault) {
                    configureNormalVault(vault);
                    configured++;
                }
            }
        }
        if (placeAndConfigureRooftopVault(level, site)) {
            configured++;
        }
        return configured;
    }

    /** Configures a normal vault block entity with the approved Sky Tower loot table. */
    public static void configureNormalVault(VaultBlockEntity vault) {
        if (vault == null) return;
        VaultConfig cur = vault.getConfig();
        if (!NORMAL_VAULT_LOOT_KEY.equals(cur.lootTable())) {
            vault.setConfig(new VaultConfig(NORMAL_VAULT_LOOT_KEY,
                    cur.activationRange(), cur.deactivationRange(),
                    cur.keyItem(), cur.overrideLootTableToDisplay()));
            vault.setChanged();
        }
    }

    /** Configures a rooftop vault block entity with the approved rooftop loot table. */
    public static void configureRooftopVault(VaultBlockEntity vault, ItemStack keyItem) {
        if (vault == null) return;
        VaultConfig cur = vault.getConfig();
        ItemStack key = keyItem != null ? keyItem : cur.keyItem();
        vault.setConfig(new VaultConfig(ROOFTOP_VAULT_LOOT_KEY,
                cur.activationRange(), cur.deactivationRange(),
                key, cur.overrideLootTableToDisplay()));
        vault.setChanged();
    }

    /**
     * Resolves the authoritative VaultConfig for a vault at pos.
     * If the vault belongs to a Sky Tower, ensures the correct loot table is used.
     * Otherwise returns current untouched.
     */
    public static VaultConfig resolveVaultConfig(ServerLevel level, BlockPos pos, VaultConfig current) {
        if (level == null || pos == null || current == null) return current;
        if (isNormalVault(level, pos)) {
            if (!NORMAL_VAULT_LOOT_KEY.equals(current.lootTable())) {
                VaultConfig updated = new VaultConfig(NORMAL_VAULT_LOOT_KEY,
                        current.activationRange(), current.deactivationRange(),
                        current.keyItem(), current.overrideLootTableToDisplay());
                var be = level.getBlockEntity(pos);
                if (be instanceof VaultBlockEntity vault) {
                    vault.setConfig(updated);
                    vault.setChanged();
                }
                return updated;
            }
            return current;
        }
        if (isRooftopVault(level, pos)) {
            if (!ROOFTOP_VAULT_LOOT_KEY.equals(current.lootTable())) {
                VaultConfig updated = new VaultConfig(ROOFTOP_VAULT_LOOT_KEY,
                        current.activationRange(), current.deactivationRange(),
                        current.keyItem(), current.overrideLootTableToDisplay());
                var be = level.getBlockEntity(pos);
                if (be instanceof VaultBlockEntity vault) {
                    vault.setConfig(updated);
                    vault.setChanged();
                }
                return updated;
            }
            return current;
        }
        return current;
    }

    /**
     * Appends the personal Star Covenant reward (if the 10% one-time roll hits) into the
     * vanilla Vault's ejection list so that the Vault visibly ejects it upward with
     * standard vanilla timing, velocity, sound, particles, and serverData NBT persistence.
     */
    public static List<ItemStack> appendSpecialRewards(
            ServerPlayer player, ServerLevel level, BlockPos pos, List<ItemStack> itemsToEject) {
        DoubleSupplier override = ROLL_OVERRIDE.get();
        DoubleSupplier roll = override != null ? override : () -> level.getRandom().nextDouble();
        return appendSpecialRewards(player, level, pos, itemsToEject, roll);
    }

    /**
     * Appends the personal Star Covenant reward using the supplied roll source when pos is a
     * registered rooftop vault and itemsToEject is non-empty (ensuring failed/empty unlocks
     * never consume eligibility).
     */
    public static List<ItemStack> appendSpecialRewards(
            ServerPlayer player, ServerLevel level, BlockPos pos, List<ItemStack> itemsToEject, DoubleSupplier roll) {
        if (player == null || level == null || pos == null || itemsToEject == null || itemsToEject.isEmpty()) {
            return itemsToEject;
        }
        if (!isRooftopVault(level, pos)) {
            return itemsToEject;
        }
        var site = findSite(level, pos);
        if (site == null) {
            return itemsToEject;
        }
        return appendRooftopStarCovenantForSite(
                StarCovenantVaultRewards.Data.get(level.getServer()),
                player.getUUID(),
                site,
                itemsToEject,
                roll,
                StarCovenantVaultRewards::reward);
    }

    /**
     * Pure helper for appending a rooftop special reward to an ejection list when the
     * player's one-time 10% Star Covenant roll for the site hits.
     */
    public static List<ItemStack> appendRooftopStarCovenantForSite(
            StarCovenantVaultRewards.Data data,
            java.util.UUID playerId,
            SkyTowerLayout.Site site,
            List<ItemStack> itemsToEject,
            DoubleSupplier roll,
            Supplier<ItemStack> rewardFactory) {
        if (data == null || playerId == null || site == null || itemsToEject == null || itemsToEject.isEmpty()) {
            return itemsToEject;
        }
        var ref = new StarCovenantVaultRewards.VaultRef(site.id(), "rooftop");
        var outcome = data.resolve(playerId, ref, roll);
        if (outcome == StarCovenantVaultRewards.Outcome.HIT) {
            List<ItemStack> updated = new ArrayList<>(itemsToEject.size() + 1);
            updated.addAll(itemsToEject);
            updated.add(rewardFactory.get());
            return updated;
        }
        return itemsToEject;
    }

    /**
     * Called when a player is recorded as rewarded by a Sky Tower vault.
     * Note: Rooftop Star Covenant reward resolution and ejection-queue insertion happen
     * during resolveItemsToEject via {@link #appendSpecialRewards} so that the Vault's
     * vanilla unlock() sequence receives and persists the complete item list.
     */
    public static void onVaultRewarded(ServerPlayer player, ServerLevel level, BlockPos pos, VaultConfig config) {
        if (player == null || level == null || pos == null) return;
        var site = findSite(level, pos);
        if (site == null) return;

        if (isNormalVault(level, pos)) {
            MoreMinecrafts.LOGGER.debug("Sky Tower normal vault rewarded player {} at {}",
                    player.getName().getString(), pos);
        } else if (isRooftopVault(level, pos)) {
            playRooftopPresentation(level, pos);
        }
    }

    /** Visual/audio hook for future special rooftop presentation. */
    public static void playRooftopPresentation(ServerLevel level, BlockPos pos) {
        // Rooftop special reward uses vanilla Vault upward item ejection, sound, and particles.
    }
}
