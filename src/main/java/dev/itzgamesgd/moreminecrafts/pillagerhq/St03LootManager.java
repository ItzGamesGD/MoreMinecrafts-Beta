package dev.itzgamesgd.moreminecrafts.pillagerhq;

import dev.itzgamesgd.moreminecrafts.pillagerhq.worldgen.St03PlacementManifest;
import dev.itzgamesgd.moreminecrafts.registry.ModItems;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.RandomizableContainerBlockEntity;
import net.minecraft.world.level.levelgen.structure.BoundingBox;

/**
 * Initializes ordinary Option B (abundant resource/food + 5-10% Diamond gear pool) and
 * one-time Option A premium container rewards across ST-03 facilities without refilling.
 */
public final class St03LootManager {
    private St03LootManager() {}

    /** 7.5% Diamond equipment chance inside military/manor ordinary supply rolls (approved 5-10% band). */
    public static final double DIAMOND_GEAR_CHANCE = 0.075D;

    private static final List<Item> DIAMOND_GEAR_POOL = List.of(
            Items.DIAMOND_SWORD,
            Items.DIAMOND_AXE,
            Items.DIAMOND_HELMET,
            Items.DIAMOND_CHESTPLATE,
            Items.DIAMOND_LEGGINGS,
            Items.DIAMOND_BOOTS
    );

    public static void initializeStructureLoot(ServerLevel level, St03HeadquartersData.HqInstance hq) {
        if (hq.lootInitialized || hq.box == null || hq.origin == null) {
            return;
        }
        RandomSource random = RandomSource.create(level.getSeed() ^ hq.startChunk ^ 0x53543033L);
        hq.manorPremiumSuccess = random.nextDouble() < St03PremiumLoot.MANOR_CHANCE;
        hq.armoryPremiumSuccess = random.nextDouble() < St03PremiumLoot.ARMORY_CHANCE;

        List<RandomizableContainerBlockEntity> manorContainers = new ArrayList<>();
        List<RandomizableContainerBlockEntity> armoryContainers = new ArrayList<>();
        List<RandomizableContainerBlockEntity> otherContainers = new ArrayList<>();

        BoundingBox box = hq.box;
        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
        for (int x = box.minX(); x <= box.maxX(); x++) {
            for (int z = box.minZ(); z <= box.maxZ(); z++) {
                if (!level.isLoaded(cursor.set(x, box.minY(), z))) {
                    continue;
                }
                for (int y = box.minY(); y <= box.maxY(); y++) {
                    cursor.set(x, y, z);
                    BlockEntity be = level.getBlockEntity(cursor);
                    if (be instanceof RandomizableContainerBlockEntity container && container.isEmpty()) {
                        String zone = classifyZone(hq.origin, hq.rotation, cursor);
                        switch (zone) {
                            case "MANOR" -> manorContainers.add(container);
                            case "ARMORY" -> armoryContainers.add(container);
                            default -> otherContainers.add(container);
                        }
                    }
                }
            }
        }

        for (RandomizableContainerBlockEntity c : manorContainers) {
            fillOrdinaryMilitary(c, random, true);
        }
        for (RandomizableContainerBlockEntity c : armoryContainers) {
            fillOrdinaryMilitary(c, random, true);
        }
        for (RandomizableContainerBlockEntity c : otherContainers) {
            String zone = classifyZone(hq.origin, hq.rotation, c.getBlockPos());
            if ("FOOD_GENERAL".equals(zone)) {
                fillOrdinaryGeneral(c, random);
            } else {
                fillOrdinaryMilitary(c, random, false);
            }
        }

        if (hq.manorPremiumSuccess && !manorContainers.isEmpty()) {
            // Place in highest-Y manor container (3F rare reward chest per Q72)
            RandomizableContainerBlockEntity top = manorContainers.get(0);
            for (RandomizableContainerBlockEntity c : manorContainers) {
                if (c.getBlockPos().getY() > top.getBlockPos().getY()) {
                    top = c;
                }
            }
            addRandomSlot(top, St03PremiumLoot.createPremiumStack(St03PremiumLoot.Source.MANOR, random::nextInt), random);
        }

        if (hq.armoryPremiumSuccess && !armoryContainers.isEmpty()) {
            RandomizableContainerBlockEntity target = armoryContainers.get(random.nextInt(armoryContainers.size()));
            addRandomSlot(target, St03PremiumLoot.createPremiumStack(St03PremiumLoot.Source.ARMORY, random::nextInt), random);
        }

        hq.lootInitialized = true;
    }

    private static String classifyZone(BlockPos origin, Rotation rotation, BlockPos worldPos) {
        // Inverse-rotate worldPos into q=0 local [0..111] to classify facility
        int dx = worldPos.getX() - origin.getX();
        int dz = worldPos.getZ() - origin.getZ();
        int max = 111;
        int lx;
        int lz;
        switch (rotation) {
            case CLOCKWISE_90 -> { lx = dz; lz = max - dx; }
            case CLOCKWISE_180 -> { lx = max - dx; lz = max - dz; }
            case COUNTERCLOCKWISE_90 -> { lx = max - dz; lz = dx; }
            default -> { lx = dx; lz = dz; }
        }
        if (lx >= 48 && lx <= 64 && lz >= 11 && lz <= 25) return "MANOR";
        if (lx >= 12 && lx <= 28 && lz >= 39 && lz <= 51) return "ARMORY";
        if ((lx >= 75 && lx <= 87 && lz >= 53 && lz <= 67) || (lx >= 92 && lx <= 100 && lz >= 50 && lz <= 58)) {
            return "FOOD_GENERAL";
        }
        return "MILITARY";
    }

    private static void fillOrdinaryMilitary(RandomizableContainerBlockEntity container, RandomSource random, boolean highTier) {
        addRandomSlot(container, new ItemStack(Items.ARROW, 16 + random.nextInt(25)), random);
        addRandomSlot(container, new ItemStack(Items.IRON_INGOT, 6 + random.nextInt(13)), random);
        addRandomSlot(container, new ItemStack(Items.BREAD, 4 + random.nextInt(7)), random);
        if (random.nextDouble() < 0.55) {
            addRandomSlot(container, new ItemStack(Items.CROSSBOW), random);
        }
        if (random.nextDouble() < 0.45) {
            addRandomSlot(container, new ItemStack(Items.EMERALD, highTier ? 5 + random.nextInt(10) : 2 + random.nextInt(6)), random);
        }
        if (random.nextDouble() < 0.35) {
            addRandomSlot(container, new ItemStack(ModItems.ENHANCEMENT_STONE, 2 + random.nextInt(4)), random);
        }
        if (random.nextDouble() < 0.30) {
            addRandomSlot(container, new ItemStack(Items.COOKED_BEEF, 3 + random.nextInt(6)), random);
        }
        if (random.nextDouble() < DIAMOND_GEAR_CHANCE) {
            Item diamondItem = DIAMOND_GEAR_POOL.get(random.nextInt(DIAMOND_GEAR_POOL.size()));
            addRandomSlot(container, new ItemStack(diamondItem), random);
        }
    }

    private static void fillOrdinaryGeneral(RandomizableContainerBlockEntity container, RandomSource random) {
        addRandomSlot(container, new ItemStack(Items.BREAD, 6 + random.nextInt(10)), random);
        addRandomSlot(container, new ItemStack(Items.COOKED_BEEF, 4 + random.nextInt(8)), random);
        addRandomSlot(container, new ItemStack(Items.COAL, 8 + random.nextInt(13)), random);
        if (random.nextDouble() < 0.65) {
            addRandomSlot(container, new ItemStack(Items.LEATHER, 4 + random.nextInt(8)), random);
        }
        if (random.nextDouble() < 0.50) {
            addRandomSlot(container, new ItemStack(Items.EMERALD, 3 + random.nextInt(6)), random);
        }
        if (random.nextDouble() < 0.40) {
            addRandomSlot(container, new ItemStack(Items.GOLD_INGOT, 3 + random.nextInt(7)), random);
        }
        if (random.nextDouble() < DIAMOND_GEAR_CHANCE * 0.5) {
            Item diamondItem = DIAMOND_GEAR_POOL.get(random.nextInt(DIAMOND_GEAR_POOL.size()));
            addRandomSlot(container, new ItemStack(diamondItem), random);
        }
    }

    private static void addRandomSlot(RandomizableContainerBlockEntity container, ItemStack stack, RandomSource random) {
        if (stack.isEmpty()) {
            return;
        }
        int size = container.getContainerSize();
        for (int attempt = 0; attempt < 12; attempt++) {
            int slot = random.nextInt(size);
            if (container.getItem(slot).isEmpty()) {
                container.setItem(slot, stack);
                return;
            }
        }
        for (int i = 0; i < size; i++) {
            if (container.getItem(i).isEmpty()) {
                container.setItem(i, stack);
                return;
            }
        }
    }
}
