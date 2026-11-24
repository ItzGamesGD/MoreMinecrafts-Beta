package dev.itzgamesgd.moreminecrafts.pillagerhq;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.longs.LongSet;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;

/**
 * Persistent per-dimension state for all generated ST-03 Pillager Headquarters instances.
 * Guarantees one-time garrison spawning, permanent non-respawn of dead defenders,
 * one-time premium loot rolls (65%/35%/10%), and post-clear village/spawn rules (Option D).
 */
public final class St03HeadquartersData extends SavedData {
    public static final class HqInstance {
        public final long startChunk;
        public final LongSet committedChunks = new LongOpenHashSet();
        public BlockPos origin;
        public BoundingBox box;
        public Rotation rotation = Rotation.NONE;
        public boolean structureCompleted;
        public boolean garrisonInitialized;
        public final Set<Integer> eliminatedSlots = new HashSet<>();
        public boolean cleared;
        public boolean commanderKilled;
        public boolean commanderSpecialAttempted;
        public boolean manorPremiumSuccess;
        public boolean armoryPremiumSuccess;
        public boolean lootInitialized;
        public long lastVillagerDay = -1L;
        public int villagerAttemptsToday;
        public int spawnedVillagers;

        public HqInstance(long startChunk) {
            this.startChunk = startChunk;
        }

        public String id() {
            return "st03_" + Long.toUnsignedString(startChunk, 16);
        }

        public boolean contains(BlockPos pos) {
            return box != null && box.isInside(pos);
        }

        public boolean isNearCompound(BlockPos pos, int marginBlocks) {
            if (box == null) {
                return false;
            }
            return pos.getX() >= box.minX() - marginBlocks && pos.getX() <= box.maxX() + marginBlocks
                    && pos.getZ() >= box.minZ() - marginBlocks && pos.getZ() <= box.maxZ() + marginBlocks
                    && pos.getY() >= box.minY() - 16 && pos.getY() <= box.maxY() + 32;
        }
    }

    private record HqLocation(
            long startChunk,
            List<Long> committedChunks,
            Optional<Long> origin,
            Optional<List<Integer>> box,
            String rotation
    ) {}

    private static final Codec<HqLocation> LOCATION_CODEC = RecordCodecBuilder.create(i -> i.group(
            Codec.LONG.fieldOf("start_chunk").forGetter(HqLocation::startChunk),
            Codec.LONG.listOf().fieldOf("committed_chunks").forGetter(HqLocation::committedChunks),
            Codec.LONG.optionalFieldOf("origin").forGetter(HqLocation::origin),
            Codec.INT.listOf().optionalFieldOf("box").forGetter(HqLocation::box),
            Codec.STRING.fieldOf("rotation").forGetter(HqLocation::rotation)
    ).apply(i, HqLocation::new));

    private record HqStatus(
            boolean structureCompleted,
            boolean garrisonInitialized,
            List<Integer> eliminatedSlots,
            boolean cleared,
            boolean commanderKilled,
            boolean commanderSpecialAttempted,
            boolean manorPremiumSuccess,
            boolean armoryPremiumSuccess,
            boolean lootInitialized
    ) {}

    private static final Codec<HqStatus> STATUS_CODEC = RecordCodecBuilder.create(i -> i.group(
            Codec.BOOL.fieldOf("structure_completed").forGetter(HqStatus::structureCompleted),
            Codec.BOOL.fieldOf("garrison_initialized").forGetter(HqStatus::garrisonInitialized),
            Codec.INT.listOf().fieldOf("eliminated_slots").forGetter(HqStatus::eliminatedSlots),
            Codec.BOOL.fieldOf("cleared").forGetter(HqStatus::cleared),
            Codec.BOOL.fieldOf("commander_killed").forGetter(HqStatus::commanderKilled),
            Codec.BOOL.fieldOf("commander_special_attempted").forGetter(HqStatus::commanderSpecialAttempted),
            Codec.BOOL.fieldOf("manor_premium_success").forGetter(HqStatus::manorPremiumSuccess),
            Codec.BOOL.fieldOf("armory_premium_success").forGetter(HqStatus::armoryPremiumSuccess),
            Codec.BOOL.fieldOf("loot_initialized").forGetter(HqStatus::lootInitialized)
    ).apply(i, HqStatus::new));

    private record HqVillagers(
            long lastVillagerDay,
            int villagerAttemptsToday,
            int spawnedVillagers
    ) {}

    private static final Codec<HqVillagers> VILLAGERS_CODEC = RecordCodecBuilder.create(i -> i.group(
            Codec.LONG.fieldOf("last_villager_day").forGetter(HqVillagers::lastVillagerDay),
            Codec.INT.fieldOf("villager_attempts_today").forGetter(HqVillagers::villagerAttemptsToday),
            Codec.INT.fieldOf("spawned_villagers").forGetter(HqVillagers::spawnedVillagers)
    ).apply(i, HqVillagers::new));

    private record HqSave(
            HqLocation location,
            HqStatus status,
            HqVillagers villagers
    ) {}

    private static final Codec<HqSave> HQ_CODEC = RecordCodecBuilder.create(i -> i.group(
            LOCATION_CODEC.fieldOf("location").forGetter(HqSave::location),
            STATUS_CODEC.fieldOf("status").forGetter(HqSave::status),
            VILLAGERS_CODEC.fieldOf("villagers").forGetter(HqSave::villagers)
    ).apply(i, HqSave::new));

    private static final Codec<St03HeadquartersData> CODEC = HQ_CODEC.listOf()
            .optionalFieldOf("headquarters", List.of())
            .codec()
            .xmap(St03HeadquartersData::fromSaved, St03HeadquartersData::toSaved);

    public static final SavedDataType<St03HeadquartersData> TYPE = new SavedDataType<>(
            Identifier.parse("moreminecrafts:st03_pillager_headquarters"),
            St03HeadquartersData::new,
            CODEC,
            DataFixTypes.LEVEL
    );

    private final Map<Long, HqInstance> instances = new LinkedHashMap<>();

    public St03HeadquartersData() {}

    public static St03HeadquartersData get(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(TYPE);
    }

    public HqInstance getOrCreate(long startChunk) {
        return instances.computeIfAbsent(startChunk, HqInstance::new);
    }

    public Optional<HqInstance> findById(String id) {
        for (HqInstance inst : instances.values()) {
            if (inst.id().equals(id)) {
                return Optional.of(inst);
            }
        }
        return Optional.empty();
    }

    public Map<Long, HqInstance> instances() {
        return instances;
    }

    private static St03HeadquartersData fromSaved(List<HqSave> list) {
        St03HeadquartersData data = new St03HeadquartersData();
        for (HqSave s : list) {
            HqLocation loc = s.location();
            HqStatus stat = s.status();
            HqVillagers vil = s.villagers();
            HqInstance inst = new HqInstance(loc.startChunk());
            loc.committedChunks().forEach(inst.committedChunks::add);
            inst.origin = loc.origin().map(BlockPos::of).orElse(null);
            inst.box = loc.box().filter(b -> b.size() == 6)
                    .map(b -> new BoundingBox(b.get(0), b.get(1), b.get(2), b.get(3), b.get(4), b.get(5)))
                    .orElse(null);
            inst.rotation = Rotation.valueOf(loc.rotation());
            inst.structureCompleted = stat.structureCompleted();
            inst.garrisonInitialized = stat.garrisonInitialized();
            inst.eliminatedSlots.addAll(stat.eliminatedSlots());
            inst.cleared = stat.cleared();
            inst.commanderKilled = stat.commanderKilled();
            inst.commanderSpecialAttempted = stat.commanderSpecialAttempted();
            inst.manorPremiumSuccess = stat.manorPremiumSuccess();
            inst.armoryPremiumSuccess = stat.armoryPremiumSuccess();
            inst.lootInitialized = stat.lootInitialized();
            inst.lastVillagerDay = vil.lastVillagerDay();
            inst.villagerAttemptsToday = vil.villagerAttemptsToday();
            inst.spawnedVillagers = vil.spawnedVillagers();
            data.instances.put(inst.startChunk, inst);
        }
        return data;
    }

    private List<HqSave> toSaved() {
        List<HqSave> list = new ArrayList<>(instances.size());
        for (HqInstance inst : instances.values()) {
            List<Long> chunks = new ArrayList<>(inst.committedChunks.size());
            inst.committedChunks.forEach(chunks::add);
            Optional<List<Integer>> boxList = inst.box == null
                    ? Optional.empty()
                    : Optional.of(List.of(inst.box.minX(), inst.box.minY(), inst.box.minZ(),
                    inst.box.maxX(), inst.box.maxY(), inst.box.maxZ()));
            HqLocation loc = new HqLocation(
                    inst.startChunk,
                    chunks,
                    Optional.ofNullable(inst.origin).map(BlockPos::asLong),
                    boxList,
                    inst.rotation.name()
            );
            HqStatus stat = new HqStatus(
                    inst.structureCompleted,
                    inst.garrisonInitialized,
                    new ArrayList<>(inst.eliminatedSlots),
                    inst.cleared,
                    inst.commanderKilled,
                    inst.commanderSpecialAttempted,
                    inst.manorPremiumSuccess,
                    inst.armoryPremiumSuccess,
                    inst.lootInitialized
            );
            HqVillagers vil = new HqVillagers(
                    inst.lastVillagerDay,
                    inst.villagerAttemptsToday,
                    inst.spawnedVillagers
            );
            list.add(new HqSave(loc, stat, vil));
        }
        return list;
    }
}
