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
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemInstance;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.NetherWartBlock;
import net.minecraft.world.level.block.PitcherCropBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/** Server-authoritative effects that cannot be expressed by vanilla enchantment components. */
public final class EnchantmentRuntime {
    private static final TagKey<Block> ORES = TagKey.create(Registries.BLOCK, ModIds.id("vein_mining_ores"));
    private static final ThreadLocal<MiningOperation> MINING = new ThreadLocal<>();
    private static final ThreadLocal<Integer> BREAK_DEPTH = ThreadLocal.withInitial(() -> 0);
    private static final ThreadLocal<BlockPos> ACTIVE_DROP = new ThreadLocal<>();
    private static final ThreadLocal<Boolean> CHAIN_BREAK = ThreadLocal.withInitial(() -> false);
    private static final ThreadLocal<Boolean> EXACT_DURABILITY = ThreadLocal.withInitial(() -> false);
    private static final Map<UUID, Boolean> JUMP_HELD = new HashMap<>();
    private static final Map<UUID, Integer> AIR_JUMPS = new HashMap<>();
    private static final Map<UUID, ReplantOperation> REPLANT = new HashMap<>();

    private EnchantmentRuntime() {}

    public static void initialize() {
        PlayerBlockBreakEvents.BEFORE.register(EnchantmentRuntime::beforeBreak);
        PlayerBlockBreakEvents.AFTER.register(EnchantmentRuntime::afterBreak);
        ServerTickEvents.END_SERVER_TICK.register(server -> server.getPlayerList().getPlayers().forEach(EnchantmentRuntime::tickMovement));
    }

    private static boolean beforeBreak(net.minecraft.world.level.Level level, net.minecraft.world.entity.player.Player player,
            BlockPos pos, BlockState state, net.minecraft.world.level.block.entity.BlockEntity ignored) {
        if (!(level instanceof ServerLevel serverLevel) || !(player instanceof ServerPlayer serverPlayer)) return true;
        if (CHAIN_BREAK.get()) return true;
        ItemStack tool = player.getMainHandItem();

        CropInfo crop = cropInfo(pos, state);
        if (crop != null && ModEnchantments.level(level, tool, ModEnchantments.AUTO_REPLANT) > 0) {
            if (!crop.mature()) return false;
            Set<UUID> existing = new HashSet<>();
            serverLevel.getEntitiesOfClass(ItemEntity.class, new AABB(crop.root()).inflate(2.0))
                    .forEach(item -> existing.add(item.getUUID()));
            REPLANT.put(player.getUUID(), new ReplantOperation(crop.root(), crop.seed(), crop.replanted(), existing));
        }

        if (MINING.get() != null) return true;
        int vein = ModEnchantments.level(level, tool, ModEnchantments.VEIN_MINING);
        int lumber = ModEnchantments.level(level, tool, ModEnchantments.LUMBERJACK);
        boolean autoSmelt = ModEnchantments.level(level, tool, ModEnchantments.AUTO_SMELT) > 0
                && ModEnchantments.level(level, tool, Enchantments.SILK_TOUCH) == 0;
        if (vein > 0 && state.is(ORES)) {
            MINING.set(new MiningOperation(tool, tool.getDamageValue(), vein, false,
                    autoSmelt, pos.immutable(), null));
        } else if (lumber > 0 && state.is(net.minecraft.tags.BlockTags.LOGS)) {
            Set<BlockPos> tree = findTree(serverLevel, pos, state);
            if (tree != null) {
                MINING.set(new MiningOperation(tool, tool.getDamageValue(), lumber, true, false,
                        pos.immutable(), tree));
            }
        } else if (autoSmelt && state.is(ORES)) {
            // A level of zero marks an Auto Smelt-only operation: preserve vanilla durability and
            // do not traverse connected blocks, while keeping drop conversion block-scoped.
            MINING.set(new MiningOperation(tool, tool.getDamageValue(), 0, false, true, pos.immutable(), null));
        }
        return true;
    }

    private static void afterBreak(net.minecraft.world.level.Level level, net.minecraft.world.entity.player.Player player,
            BlockPos origin, BlockState broken, net.minecraft.world.level.block.entity.BlockEntity ignored) {
        if (!(level instanceof ServerLevel serverLevel) || !(player instanceof ServerPlayer serverPlayer)) return;
        if (CHAIN_BREAK.get()) return;
        MiningOperation operation = MINING.get();
        if (operation != null && operation.pos().equals(origin) && operation.tool() == player.getMainHandItem()) {
            if (operation.level() == 0) {
                finishReplant(serverLevel, serverPlayer);
                return;
            }
            int additional;
            CHAIN_BREAK.set(true);
            try {
                additional = operation.lumber()
                        ? breakTree(serverPlayer, origin, operation.treeLogs())
                        : breakVein(serverPlayer, origin, broken);
            } finally {
                CHAIN_BREAK.remove();
            }
            operation.tool().setDamageValue(operation.initialDamage());
            int total = additional + 1;
            int cost = operation.lumber()
                    ? EnchantmentMath.lumberDurability(total,
                            ModEnchantments.level(level, operation.tool(), Enchantments.UNBREAKING))
                    : EnchantmentMath.veinDurability(total, operation.level());
            applyExactDurability(operation.tool(), cost, serverPlayer);
        }
        finishReplant(serverLevel, serverPlayer);
    }

    /** Brackets the complete vanilla/Fabric destroy call so cancellation and exceptions cannot leak state. */
    public static void enterBlockBreak() {
        int depth = BREAK_DEPTH.get();
        if (depth == 0) clearBlockBreakState();
        BREAK_DEPTH.set(depth + 1);
    }

    public static void exitBlockBreak() {
        int depth = BREAK_DEPTH.get() - 1;
        if (depth <= 0) {
            BREAK_DEPTH.remove();
            clearBlockBreakState();
        } else {
            BREAK_DEPTH.set(depth);
        }
    }

    private static void clearBlockBreakState() {
        MINING.remove();
        ACTIVE_DROP.remove();
        REPLANT.clear();
    }

    private static int breakVein(ServerPlayer player, BlockPos origin, BlockState target) {
        ArrayDeque<BlockPos> queue = new ArrayDeque<>();
        Set<BlockPos> seen = new HashSet<>();
        Set<ChunkPos> chunks = new HashSet<>();
        queue.add(origin);
        seen.add(origin);
        chunks.add(chunkAt(origin));
        int destroyed = 0;
        while (!queue.isEmpty()) {
            BlockPos current = queue.removeFirst();
            for (BlockPos next : neighbours(current)) {
                if (!seen.add(next)) continue;
                BlockState state = player.level().getBlockState(next);
                if (!state.is(target.getBlock())) continue;
                ChunkPos chunk = chunkAt(next);
                if (!chunks.contains(chunk) && chunks.size() == EnchantmentMath.MAX_VEIN_CHUNKS) continue;
                chunks.add(chunk);
                if (destroyScoped(player, next)) destroyed++;
                queue.addLast(next);
            }
        }
        return destroyed;
    }

    private static Set<BlockPos> findTree(ServerLevel level, BlockPos origin, BlockState target) {
        ArrayDeque<BlockPos> queue = new ArrayDeque<>();
        Set<BlockPos> seen = new HashSet<>();
        Set<BlockPos> logs = new HashSet<>();
        queue.add(origin);
        seen.add(origin);
        logs.add(origin);
        int minX = origin.getX(), maxX = minX, minY = origin.getY(), maxY = minY;
        int minZ = origin.getZ(), maxZ = minZ;
        while (!queue.isEmpty()) {
            BlockPos current = queue.removeFirst();
            for (BlockPos next : neighbours(current)) {
                if (!seen.add(next)) continue;
                BlockState state = level.getBlockState(next);
                if (!state.is(target.getBlock())) continue;
                minX = Math.min(minX, next.getX()); maxX = Math.max(maxX, next.getX());
                minY = Math.min(minY, next.getY()); maxY = Math.max(maxY, next.getY());
                minZ = Math.min(minZ, next.getZ()); maxZ = Math.max(maxZ, next.getZ());
                if (!EnchantmentMath.withinTreeEnvelope(minX, maxX, minY, maxY, minZ, maxZ)) return null;
                logs.add(next);
                queue.addLast(next);
            }
        }

        return logs;
    }

    private static int breakTree(ServerPlayer player, BlockPos origin, Set<BlockPos> logs) {
        int destroyed = 0;
        for (BlockPos log : logs) {
            if (log.equals(origin)) continue;
            if (destroyScoped(player, log)) destroyed++;
        }
        return destroyed;
    }

    private static boolean destroyScoped(ServerPlayer player, BlockPos pos) {
        ACTIVE_DROP.set(pos);
        try {
            return player.gameMode.destroyBlock(pos);
        } finally {
            ACTIVE_DROP.remove();
        }
    }

    private static BlockPos[] neighbours(BlockPos p) {
        BlockPos[] out = new BlockPos[26]; int i = 0;
        for (int x=-1;x<=1;x++) for (int y=-1;y<=1;y++) for (int z=-1;z<=1;z++)
            if (x != 0 || y != 0 || z != 0) out[i++] = p.offset(x,y,z);
        return out;
    }

    private static ChunkPos chunkAt(BlockPos pos) {
        return new ChunkPos(pos.getX() >> 4, pos.getZ() >> 4);
    }

    private static void applyExactDurability(ItemStack tool, int amount, ServerPlayer player) {
        if (amount <= 0 || !tool.isDamageableItem()) return;
        EXACT_DURABILITY.set(true);
        try {
            tool.hurtAndBreak(amount, player, EquipmentSlot.MAINHAND);
        } finally {
            EXACT_DURABILITY.remove();
        }
    }

    /** Called by the ItemStack mixin so operation durability uses one deterministic aggregate charge. */
    public static boolean suppressVanillaDurability() {
        MiningOperation operation = MINING.get();
        return operation != null && operation.level() > 0 && !EXACT_DURABILITY.get();
    }

    /** Called by the ItemStack mixin to bypass per-point Unbreaking rolls for the aggregate charge. */
    public static boolean usesExactDurability() {
        return EXACT_DURABILITY.get();
    }

    /** Converts the result of the exact block loot invocation currently being mined. */
    public static ItemStack autoSmeltDrop(ServerLevel level, BlockPos pos, ItemInstance tool, ItemStack stack) {
        MiningOperation operation = MINING.get();
        BlockPos active = ACTIVE_DROP.get() == null ? operation == null ? null : operation.pos() : ACTIVE_DROP.get();
        if (operation == null || !operation.autoSmelt() || !pos.equals(active)
                || operation.tool() != tool || stack.isEmpty()) return stack;
        var input = new SingleRecipeInput(stack);
        return level.recipeAccess().getRecipeFor(RecipeType.SMELTING, input, level).map(holder -> {
            ItemStack result = holder.value().assemble(input);
            if (!result.isEmpty()) {
                result.setCount(result.getCount() * stack.getCount());
                return result;
            }
            return stack;
        }).orElse(stack);
    }

    private static CropInfo cropInfo(BlockPos pos, BlockState state) {
        Block block = state.getBlock();
        if (block instanceof CropBlock crop) {
            Item seed = block == Blocks.WHEAT ? Items.WHEAT_SEEDS
                    : block == Blocks.BEETROOTS ? Items.BEETROOT_SEEDS
                    : block == Blocks.CARROTS ? Items.CARROT
                    : block == Blocks.POTATOES ? Items.POTATO
                    : block == Blocks.TORCHFLOWER_CROP ? Items.TORCHFLOWER_SEEDS : null;
            return seed == null ? null : new CropInfo(pos, seed, crop.defaultBlockState(), crop.isMaxAge(state));
        }
        if (block instanceof NetherWartBlock) {
            return new CropInfo(pos, Items.NETHER_WART, state.setValue(NetherWartBlock.AGE, 0),
                    state.getValue(NetherWartBlock.AGE) == NetherWartBlock.MAX_AGE);
        }
        if (block instanceof PitcherCropBlock) {
            BlockPos root = state.getValue(PitcherCropBlock.HALF) == DoubleBlockHalf.UPPER ? pos.below() : pos;
            return new CropInfo(root, Items.PITCHER_POD, Blocks.PITCHER_CROP.defaultBlockState(),
                    state.getValue(PitcherCropBlock.AGE) == PitcherCropBlock.MAX_AGE);
        }
        return null; // Cocoa is intentionally excluded.
    }

    private static void finishReplant(ServerLevel level, ServerPlayer player) {
        ReplantOperation operation = REPLANT.remove(player.getUUID());
        if (operation == null) return;
        ItemStack wanted = new ItemStack(operation.seed());
        for (ItemEntity item : level.getEntitiesOfClass(ItemEntity.class, new AABB(operation.pos()).inflate(2.0))) {
            if (!operation.existingDrops().contains(item.getUUID()) && ItemStack.isSameItemSameComponents(item.getItem(), wanted)) {
                item.getItem().shrink(1);
                if (item.getItem().isEmpty()) item.discard();
                level.setBlockAndUpdate(operation.pos(), operation.state());
                return;
            }
        }
        int slot = player.getInventory().findSlotMatchingItem(wanted);
        if (slot >= 0) {
            player.getInventory().removeItem(slot, 1);
            level.setBlockAndUpdate(operation.pos(), operation.state());
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
        if (player.isShiftKeyDown() && player.onGround()
                && ModEnchantments.level(player.level(), chest, ModEnchantments.ELYTRA_LAUNCH) > 0) {
            Vec3 look = player.getLookAngle();
            player.setDeltaMovement(look.x * 0.8, 1.0, look.z * 0.8);
            player.startFallFlying();
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

    private record MiningOperation(ItemStack tool, int initialDamage, int level, boolean lumber, boolean autoSmelt,
                                   BlockPos pos, Set<BlockPos> treeLogs) {}
    private record CropInfo(BlockPos root, Item seed, BlockState replanted, boolean mature) {}
    private record ReplantOperation(BlockPos pos, Item seed, BlockState state, Set<UUID> existingDrops) {}
}
