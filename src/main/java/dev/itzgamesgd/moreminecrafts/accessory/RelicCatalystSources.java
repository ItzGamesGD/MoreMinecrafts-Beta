package dev.itzgamesgd.moreminecrafts.accessory;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.itzgamesgd.moreminecrafts.core.ModIds;
import dev.itzgamesgd.moreminecrafts.registry.ModItems;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.function.DoubleSupplier;
import java.util.function.IntUnaryOperator;
import net.fabricmc.fabric.api.loot.v3.LootTableEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.util.context.ContextKey;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.structure.PoolElementStructurePiece;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import net.minecraft.world.level.levelgen.structure.StructureStart;
import net.minecraft.world.level.levelgen.structure.pools.SinglePoolElement;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.LootPool;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.entries.LootItem;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import net.minecraft.world.level.storage.loot.providers.number.ints.ConstantValue;
import net.minecraft.world.phys.Vec3;

/**
 * D13-02 supplementary Relic Catalyst sources that are tied to one generated structure instance.
 *
 * <p>Catalyst chest assignment is a pure function of (world seed, structure start chunk, source) and the structure's
 * own template chests, so it is fixed from the moment the structure exists and never re-rolled on chest open,
 * reconnect, or restart. Each chest generates its loot once (vanilla clears the loot table on unpack), and the
 * assignment names at most one chest per structure instance, which gives the "at most one catalyst per structure"
 * guarantee. The catalyst is an <em>extra</em> pool added to the existing vanilla loot table; no existing pool,
 * entry, count, or chance is replaced.
 */
public final class RelicCatalystSources {
    /** Per-chest catalyst chance (D13 Q28/Q33) and the per-structure treasure-room chance (Q34). */
    public static final double CATALYST_CHANCE = 0.05D;

    /** A structure whose catalyst is pre-assigned to at most one of its own chests. */
    public enum Source {
        /** Q33/Q38/Q43: every normal chest rolls 5%; city-wide success is 1-0.95^n; one uniform chest wins. */
        ANCIENT_CITY("minecraft:ancient_city", "minecraft:chests/ancient_city", true),
        /** Q34: only treasure-room core chests; 5% per structure, one uniform core chest wins. */
        BASTION_TREASURE("minecraft:bastion_remnant", "minecraft:chests/bastion_treasure", false);

        private final Identifier structure;
        private final ResourceKey<LootTable> lootTable;
        private final boolean perChestChance;

        Source(String structure, String lootTable, boolean perChestChance) {
            this.structure = Identifier.parse(structure);
            this.lootTable = ResourceKey.create(Registries.LOOT_TABLE, Identifier.parse(lootTable));
            this.perChestChance = perChestChance;
        }

        public Identifier structure() { return structure; }
        public ResourceKey<LootTable> lootTable() { return lootTable; }
    }

    private RelicCatalystSources() {}

    // ---- pure probability model (unit-testable with a controlled random source) ----

    /** Probability that a structure instance carries a catalyst given {@code targets} eligible chests. */
    public static double instanceChance(Source source, int targets) {
        if (targets <= 0) return 0.0D;
        return source.perChestChance ? 1.0D - Math.pow(1.0D - CATALYST_CHANCE, targets) : CATALYST_CHANCE;
    }

    /**
     * Index of the single chest that holds the catalyst, or -1 when this instance has none. Exactly one success roll
     * is consumed, then (on success) exactly one uniform pick.
     */
    public static int select(Source source, int targets, DoubleSupplier roll, IntUnaryOperator uniformPick) {
        if (targets <= 0) return -1;
        if (!(roll.getAsDouble() < instanceChance(source, targets))) return -1;
        int index = uniformPick.applyAsInt(targets);
        return index >= 0 && index < targets ? index : -1;
    }

    /** Deterministic per-instance random source (stable across saves, restarts and reconnects). */
    public static RandomSource assignmentRandom(long worldSeed, ChunkPos start, Source source) {
        long mixed = worldSeed ^ (start.pack() * 0x9E3779B97F4A7C15L) ^ ((long) source.name().hashCode() << 17);
        mixed = (mixed ^ (mixed >>> 30)) * 0xBF58476D1CE4E5B9L;
        mixed = (mixed ^ (mixed >>> 27)) * 0x94D049BB133111EBL;
        return RandomSource.create(mixed ^ (mixed >>> 31));
    }

    /** Index into {@code sortedTargets} of the assigned chest, -1 for none. */
    public static int assignedIndex(long worldSeed, ChunkPos start, Source source, int targets) {
        RandomSource random = assignmentRandom(worldSeed, start, source);
        return select(source, targets, random::nextDouble, random::nextInt);
    }

    // ---- world-dependent resolution ----

    /** The structure instance's eligible chests in a stable order, from its own template data. */
    public static List<BlockPos> targetChests(ServerLevel level, StructureStart start, Source source) {
        List<BlockPos> result = new ArrayList<>();
        String wanted = source.lootTable.identifier().toString();
        for (StructurePiece piece : start.getPieces()) {
            if (!(piece instanceof PoolElementStructurePiece pooled) || !(pooled.getElement() instanceof SinglePoolElement single))
                continue;
            StructureTemplate template = level.getServer().getStructureTemplateManager().getOrCreate(single.getTemplateLocation());
            StructurePlaceSettings settings = new StructurePlaceSettings().setRotation(pooled.getRotation());
            for (StructureTemplate.StructureBlockInfo info : template.filterBlocks(pooled.getPosition(), settings, Blocks.CHEST)) {
                if (info.nbt() != null && wanted.equals(info.nbt().getStringOr("LootTable", ""))) result.add(info.pos());
            }
        }
        result.sort(Comparator.comparingLong(BlockPos::asLong));
        return result.stream().distinct().toList();
    }

    /** Whether the chest at {@code pos} is the one chest of its structure instance assigned the catalyst. */
    public static boolean isAssignedChest(ServerLevel level, BlockPos pos, Source source) {
        StructureStart start = level.structureManager().getStructureWithPieceAt(pos, holder ->
                holder.unwrapKey().map(key -> key.identifier().equals(source.structure)).orElse(false));
        if (start == StructureStart.INVALID_START || !start.isValid()) return false;
        List<BlockPos> targets = targetChests(level, start, source);
        int index = assignedIndex(level.getSeed(), start.getChunkPos(), source, targets.size());
        return index >= 0 && targets.get(index).equals(pos);
    }

    // ---- loot integration ----

    /** Loot condition true only for the structure-assigned chest; evaluated once when that chest generates loot. */
    public record AssignedChestCondition(Source source) implements LootItemCondition {
        public static final MapCodec<AssignedChestCondition> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
                com.mojang.serialization.Codec.STRING.xmap(Source::valueOf, Source::name).fieldOf("source")
                        .forGetter(AssignedChestCondition::source)).apply(instance, AssignedChestCondition::new));

        @Override public MapCodec<AssignedChestCondition> codec() { return CODEC; }

        @Override public Set<ContextKey<?>> getReferencedContextParams() { return Set.of(LootContextParams.ORIGIN); }

        @Override public boolean test(LootContext context) {
            Vec3 origin = context.getOptional(LootContextParams.ORIGIN);
            return origin != null && isAssignedChest(context.getLevel(), BlockPos.containing(origin), source);
        }
    }

    public static void initialize() {
        Registry.register(BuiltInRegistries.LOOT_CONDITION_TYPE, ModIds.id("relic_catalyst_assigned_chest"), AssignedChestCondition.CODEC);
        LootTableEvents.MODIFY.register((key, table, lootSource, registries) -> {
            for (Source source : Source.values()) {
                if (key.equals(source.lootTable)) table.withPool(LootPool.lootPool()
                        .setRolls(Holder.direct(new ConstantValue(1)))
                        .add(LootItem.lootTableItem(ModItems.RELIC_CATALYST))
                        .when(() -> new AssignedChestCondition(source)));
            }
        });
    }
}
