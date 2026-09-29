package dev.itzgamesgd.moreminecrafts.enchantment;

import dev.itzgamesgd.moreminecrafts.core.ModIds;
import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.NetherWartBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

/** Server-authoritative effects that cannot be expressed by vanilla enchantment components. */
public final class EnchantmentRuntime {
    private static final TagKey<Block> ORES = TagKey.create(Registries.BLOCK, ModIds.id("vein_mining_ores"));
    private static final ThreadLocal<Boolean> CHAIN_BREAK = ThreadLocal.withInitial(() -> false);
    private static final Map<UUID, Boolean> JUMP_HELD = new HashMap<>();
    private static final Map<UUID, Integer> AIR_JUMPS = new HashMap<>();

    private EnchantmentRuntime() {}

    public static void initialize() {
        PlayerBlockBreakEvents.AFTER.register(EnchantmentRuntime::afterBreak);
        ServerTickEvents.END_SERVER_TICK.register(server -> server.getPlayerList().getPlayers().forEach(EnchantmentRuntime::tickMovement));
    }

    private static void afterBreak(net.minecraft.world.level.Level level, net.minecraft.world.entity.player.Player player,
            BlockPos origin, BlockState broken, net.minecraft.world.level.block.entity.BlockEntity ignored) {
        if (!(level instanceof ServerLevel serverLevel) || !(player instanceof ServerPlayer serverPlayer) || CHAIN_BREAK.get()) return;
        ItemStack tool = player.getMainHandItem();
        int vein = ModEnchantments.level(level, tool, ModEnchantments.VEIN_MINING);
        int lumber = ModEnchantments.level(level, tool, ModEnchantments.LUMBERJACK);
        if (vein > 0 && broken.is(ORES)) {
            int extra = breakConnected(serverPlayer, origin, broken, EnchantmentMath.MAX_VEIN_BLOCKS, Integer.MAX_VALUE);
            chargeDirect(tool, EnchantmentMath.veinExtraDurability(extra, vein));
        } else if (lumber > 0 && broken.is(net.minecraft.tags.BlockTags.LOGS)) {
            int extra = breakConnected(serverPlayer, origin, broken, 2 * 2 * EnchantmentMath.MAX_TREE_HEIGHT, EnchantmentMath.MAX_TREE_HEIGHT);
            int unbreaking = ModEnchantments.level(level, tool, Enchantments.UNBREAKING);
            // The original vanilla log already charged once; charge only the arithmetic remainder.
            chargeDirect(tool, Math.max(0, EnchantmentMath.lumberDurability(extra + 1, unbreaking) - 1));
        }
        replant(serverLevel, serverPlayer, origin, broken, tool);
    }

    private static int breakConnected(ServerPlayer player, BlockPos origin, BlockState target, int limit, int maxHeight) {
        ArrayDeque<BlockPos> queue = new ArrayDeque<>();
        Set<BlockPos> seen = new HashSet<>();
        queue.add(origin);
        int destroyed = 0;
        CHAIN_BREAK.set(true);
        try {
            while (!queue.isEmpty() && destroyed < limit) {
                BlockPos current = queue.removeFirst();
                for (BlockPos next : neighbours(current)) {
                    if (!seen.add(next) || Math.abs(next.getY() - origin.getY()) >= maxHeight) continue;
                    BlockState state = player.level().getBlockState(next);
                    if (state.is(target.getBlock())) {
                        if (player.gameMode.destroyBlock(next)) destroyed++;
                        queue.addLast(next);
                    }
                }
            }
        } finally { CHAIN_BREAK.set(false); }
        return destroyed;
    }

    private static BlockPos[] neighbours(BlockPos p) {
        BlockPos[] out = new BlockPos[26]; int i = 0;
        for (int x=-1;x<=1;x++) for (int y=-1;y<=1;y++) for (int z=-1;z<=1;z++)
            if (x != 0 || y != 0 || z != 0) out[i++] = p.offset(x,y,z);
        return out;
    }

    private static void chargeDirect(ItemStack tool, int amount) {
        if (amount <= 0 || !tool.isDamageableItem()) return;
        tool.setDamageValue(Math.min(tool.getMaxDamage(), tool.getDamageValue() + amount));
    }

    private static void replant(ServerLevel level, ServerPlayer player, BlockPos pos, BlockState broken, ItemStack tool) {
        if (ModEnchantments.level(level, tool, ModEnchantments.AUTO_REPLANT) == 0) return;
        ItemStack seed = ItemStack.EMPTY;
        BlockState replanted = null;
        if (broken.getBlock() instanceof CropBlock crop && crop.isMaxAge(broken)) {
            seed = new ItemStack(crop.asItem()); replanted = crop.defaultBlockState();
        } else if (broken.getBlock() instanceof NetherWartBlock && broken.getValue(NetherWartBlock.AGE) == NetherWartBlock.MAX_AGE) {
            seed = new ItemStack(Items.NETHER_WART); replanted = broken.setValue(NetherWartBlock.AGE, 0);
        }
        if (replanted == null) return;
        int slot = player.getInventory().findSlotMatchingItem(seed);
        if (slot >= 0) {
            player.getInventory().removeItem(slot, 1);
            level.setBlockAndUpdate(pos, replanted);
        }
    }

    private static void tickMovement(ServerPlayer player) {
        CrossbowBarrageRuntime.tick(player.level().getGameTime());
        UUID id = player.getUUID();
        boolean jump = player.getLastClientInput().jump();
        boolean edge = jump && !JUMP_HELD.getOrDefault(id, false);
        JUMP_HELD.put(id, jump);
        if (player.onGround()) AIR_JUMPS.put(id, 0);
        if (!edge) return;
        ItemStack chest = player.getItemBySlot(EquipmentSlot.CHEST);
        if (player.isShiftKeyDown() && ModEnchantments.level(player.level(), chest, ModEnchantments.ELYTRA_LAUNCH) > 0) {
            Vec3 look = player.getLookAngle();
            player.setDeltaMovement(look.x * 0.8, 1.0, look.z * 0.8);
            return;
        }
        ItemStack boots = player.getItemBySlot(EquipmentSlot.FEET);
        if (player.isShiftKeyDown() && player.onGround()
                && ModEnchantments.level(player.level(), boots, ModEnchantments.FEATHER_BOOTS) > 0) {
            Vec3 look = player.getLookAngle();
            player.setDeltaMovement(look.x * 1.0, 0.72, look.z * 1.0);
            return;
        }
        int max = EnchantmentMath.airJumps(ModEnchantments.level(player.level(), boots, ModEnchantments.JET_BOOTS));
        int used = AIR_JUMPS.getOrDefault(id, 0);
        if (!player.onGround() && used < max) {
            Vec3 movement = player.getDeltaMovement();
            player.setDeltaMovement(movement.x, 0.55, movement.z);
            AIR_JUMPS.put(id, used + 1);
        }
    }
}
