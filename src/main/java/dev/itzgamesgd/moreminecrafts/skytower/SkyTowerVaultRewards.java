package dev.itzgamesgd.moreminecrafts.skytower;

import dev.itzgamesgd.moreminecrafts.MoreMinecrafts;
import dev.itzgamesgd.moreminecrafts.accessory.StarCovenantVaultRewards;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.entity.vault.VaultBlockEntity;
import net.minecraft.world.level.block.entity.vault.VaultConfig;
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

    private SkyTowerVaultRewards() {}

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

    /**
     * Returns true if pos is the rooftop special vault.
     * In the baseline candidate geometry, no rooftop vault is placed in NBT pending owner decision on container/coords.
     */
    public static boolean isRooftopVault(ServerLevel level, BlockPos pos) {
        // Reserved for owner-approved rooftop vault coordinates.
        return false;
    }

    /** Returns true if pos is any Sky Tower vault (normal or rooftop). */
    public static boolean isSkyTowerVault(ServerLevel level, BlockPos pos) {
        return isNormalVault(level, pos) || isRooftopVault(level, pos);
    }

    /** Configures all 8 normal vaults for the given site in the world. */
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
     * Called when a player successfully receives rewards from a Sky Tower vault.
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
            resolveRooftopStarCovenant(player, level, site);
        }
    }

    /** Visual/audio hook for future special rooftop presentation. */
    public static void playRooftopPresentation(ServerLevel level, BlockPos pos) {
        // Deferred presentation hook (sound/particles) per approved scope
    }

    /** Resolves the personal 10% Star Covenant roll for rooftop vault rewards. */
    public static void resolveRooftopStarCovenant(ServerPlayer player, ServerLevel level, SkyTowerLayout.Site site) {
        var data = StarCovenantVaultRewards.Data.get(level.getServer());
        var ref = new StarCovenantVaultRewards.VaultRef(site.id(), "rooftop");
        var outcome = data.resolve(player.getUUID(), ref, () -> level.random.nextDouble());
        if (outcome == StarCovenantVaultRewards.Outcome.HIT) {
            ItemStack reward = StarCovenantVaultRewards.reward();
            if (!player.getInventory().add(reward)) {
                player.drop(reward, false);
            }
        }
    }
}
