package dev.itzgamesgd.moreminecrafts.pyramid;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;

/**
 * Per-dimension persistent royal chest state, keyed by stable {@code structureInstanceId} + role. Coordinates only
 * locate a chest; they are never the identity, so repair, NBT replacement, chunk reload and restart of the same
 * instance keep results and remaining items.
 *
 * <p>Atomicity: a chest's generated contents <em>are</em> its state. "Generated" and the item list are written in one
 * saved-data record, so there is no separate claim flag that could be saved without the rewards (lost) or the rewards
 * without the flag (duplicated). Generation is also a pure function of (world seed, instance id, role), so an
 * unsaved generation that is repeated after a crash yields the identical result - never a different draw.
 */
public final class RoyalChestData extends SavedData {
    public record StackSave(int slot, String item, int count) {}

    public record ChestSave(String instance, RoyalChestRole role, List<StackSave> stacks) {}

    public record InstanceSave(String id, long tombAnchor, long spawn, long primary, long secondary, Optional<RoyalChestRole> catalyst) {}

    private record Saved(List<InstanceSave> instances, List<ChestSave> chests) {}

    private static final Codec<RoyalChestRole> ROLE = Codec.STRING.xmap(RoyalChestRole::valueOf, RoyalChestRole::name);
    private static final Codec<StackSave> STACK = RecordCodecBuilder.create(i -> i.group(
            Codec.INT.fieldOf("slot").forGetter(StackSave::slot),
            Codec.STRING.fieldOf("item").forGetter(StackSave::item),
            Codec.INT.fieldOf("count").forGetter(StackSave::count)).apply(i, StackSave::new));
    private static final Codec<ChestSave> CHEST = RecordCodecBuilder.create(i -> i.group(
            Codec.STRING.fieldOf("instance").forGetter(ChestSave::instance),
            ROLE.fieldOf("role").forGetter(ChestSave::role),
            STACK.listOf().fieldOf("stacks").forGetter(ChestSave::stacks)).apply(i, ChestSave::new));
    private static final Codec<InstanceSave> INSTANCE = RecordCodecBuilder.create(i -> i.group(
            Codec.STRING.fieldOf("id").forGetter(InstanceSave::id),
            Codec.LONG.fieldOf("tomb_anchor").forGetter(InstanceSave::tombAnchor),
            Codec.LONG.fieldOf("spawn").forGetter(InstanceSave::spawn),
            Codec.LONG.fieldOf("primary").forGetter(InstanceSave::primary),
            Codec.LONG.fieldOf("secondary").forGetter(InstanceSave::secondary),
            ROLE.optionalFieldOf("catalyst").forGetter(InstanceSave::catalyst)).apply(i, InstanceSave::new));
    private static final Codec<Saved> SAVED = RecordCodecBuilder.create(i -> i.group(
            INSTANCE.listOf().optionalFieldOf("instances", List.of()).forGetter(Saved::instances),
            CHEST.listOf().optionalFieldOf("chests", List.of()).forGetter(Saved::chests)).apply(i, Saved::new));
    private static final Codec<RoyalChestData> CODEC = SAVED.xmap(RoyalChestData::fromSaved, RoyalChestData::toSaved);

    public static final SavedDataType<RoyalChestData> TYPE = new SavedDataType<>(
            Identifier.parse("moreminecrafts:royal_chest_rewards"), RoyalChestData::new, CODEC, DataFixTypes.LEVEL);

    /** A registered pyramid instance and where its two chests currently are. */
    public record Instance(String id, BlockPos tombAnchor, BlockPos spawn, BlockPos primary, BlockPos secondary,
                           Optional<RoyalChestRole> catalyst) {
        public BlockPos chestPos(RoyalChestRole role) { return role == RoyalChestRole.PRIMARY ? primary : secondary; }
    }

    /** A chest slot-set that has been generated; absent from the map until its first valid opening. */
    public static final class Chest {
        final RoyalChestContainer container;
        Chest(RoyalChestContainer container) { this.container = container; }
        public RoyalChestContainer container() { return container; }
    }

    /** Where a position points. */
    public record Link(Instance instance, RoyalChestRole role) {}

    private final Map<String, Instance> instances = new LinkedHashMap<>();
    private final Map<Long, Link> byPos = new LinkedHashMap<>();
    private final Map<String, Map<RoyalChestRole, Chest>> chests = new LinkedHashMap<>();

    public RoyalChestData() {}

    // ---- queries ----

    public Optional<Link> linkAt(BlockPos pos) { return Optional.ofNullable(byPos.get(pos.asLong())); }

    public Optional<Instance> instance(String id) { return Optional.ofNullable(instances.get(id)); }

    public Optional<Chest> chest(String instanceId, RoyalChestRole role) {
        Map<RoyalChestRole, Chest> byRole = chests.get(instanceId);
        return byRole == null ? Optional.empty() : Optional.ofNullable(byRole.get(role));
    }

    public boolean isGenerated(String instanceId, RoyalChestRole role) { return chest(instanceId, role).isPresent(); }

    public int instanceCount() { return instances.size(); }

    // ---- mutations ----

    /**
     * Registers an instance, or relocates the chests of an existing one (repair / NBT replacement) while keeping its
     * catalyst assignment and chest contents. Returns empty when a position already belongs to another instance.
     */
    Optional<Instance> register(String id, BlockPos tombAnchor, BlockPos spawn, BlockPos primary, BlockPos secondary,
                                Optional<RoyalChestRole> freshCatalyst) {
        if (primary.equals(secondary)) return Optional.empty();
        for (BlockPos pos : List.of(primary, secondary)) {
            Link owner = byPos.get(pos.asLong());
            if (owner != null && !owner.instance().id().equals(id)) return Optional.empty();
        }
        Instance existing = instances.get(id);
        Optional<RoyalChestRole> catalyst = existing != null ? existing.catalyst() : freshCatalyst;
        if (existing != null) {
            byPos.remove(existing.primary().asLong());
            byPos.remove(existing.secondary().asLong());
        }
        Instance instance = new Instance(id, tombAnchor, spawn, primary, secondary, catalyst);
        instances.put(id, instance);
        byPos.put(primary.asLong(), new Link(instance, RoyalChestRole.PRIMARY));
        byPos.put(secondary.asLong(), new Link(instance, RoyalChestRole.SECONDARY));
        setDirty();
        return Optional.of(instance);
    }

    /** Removes an instance and everything generated for it (administrative / test cleanup only). */
    boolean discard(String id) {
        Instance instance = instances.remove(id);
        if (instance == null) return false;
        byPos.remove(instance.primary().asLong());
        byPos.remove(instance.secondary().asLong());
        chests.remove(id);
        setDirty();
        return true;
    }

    /** Creates the chest's one shared container with its generated stacks, atomically with the "generated" state. */
    Chest install(String instanceId, RoyalChestRole role, List<RoyalChestRewardModel.Stack> stacks) {
        RoyalChestContainer container = new RoyalChestContainer(this::setDirty);
        for (int slot = 0; slot < stacks.size(); slot++) {
            RoyalChestRewardModel.Stack stack = stacks.get(slot);
            container.fill(slot, toStack(stack.itemId(), stack.count()));
        }
        Chest chest = new Chest(container);
        chests.computeIfAbsent(instanceId, k -> new EnumMap<>(RoyalChestRole.class)).put(role, chest);
        setDirty();
        return chest;
    }

    static ItemStack toStack(String itemId, int count) {
        Item item = BuiltInRegistries.ITEM.getValue(Identifier.parse(itemId));
        return new ItemStack(item, count);
    }

    // ---- codec bridge ----

    private static RoyalChestData fromSaved(Saved saved) {
        RoyalChestData data = new RoyalChestData();
        for (InstanceSave s : saved.instances()) {
            Instance instance = new Instance(s.id(), BlockPos.of(s.tombAnchor()), BlockPos.of(s.spawn()),
                    BlockPos.of(s.primary()), BlockPos.of(s.secondary()), s.catalyst());
            data.instances.put(instance.id(), instance);
            data.byPos.put(instance.primary().asLong(), new Link(instance, RoyalChestRole.PRIMARY));
            data.byPos.put(instance.secondary().asLong(), new Link(instance, RoyalChestRole.SECONDARY));
        }
        for (ChestSave c : saved.chests()) {
            RoyalChestContainer container = new RoyalChestContainer(data::setDirty);
            for (StackSave s : c.stacks()) {
                if (s.slot() < 0 || s.slot() >= RoyalChestRewardModel.SLOTS || s.count() <= 0) continue;
                Identifier id = Identifier.tryParse(s.item());
                if (id == null || !BuiltInRegistries.ITEM.containsKey(id)) continue;
                container.fill(s.slot(), toStack(s.item(), s.count()));
            }
            data.chests.computeIfAbsent(c.instance(), k -> new EnumMap<>(RoyalChestRole.class)).put(c.role(), new Chest(container));
        }
        return data;
    }

    private Saved toSaved() {
        List<InstanceSave> savedInstances = new ArrayList<>();
        for (Instance i : instances.values())
            savedInstances.add(new InstanceSave(i.id(), i.tombAnchor().asLong(), i.spawn().asLong(), i.primary().asLong(),
                    i.secondary().asLong(), i.catalyst()));
        List<ChestSave> savedChests = new ArrayList<>();
        chests.forEach((instanceId, byRole) -> byRole.forEach((role, chest) -> {
            List<StackSave> stacks = new ArrayList<>();
            for (int slot = 0; slot < chest.container.getContainerSize(); slot++) {
                ItemStack stack = chest.container.getItem(slot);
                if (!stack.isEmpty())
                    stacks.add(new StackSave(slot, BuiltInRegistries.ITEM.getKey(stack.getItem()).toString(), stack.getCount()));
            }
            savedChests.add(new ChestSave(instanceId, role, stacks));
        }));
        return new Saved(savedInstances, savedChests);
    }
}
