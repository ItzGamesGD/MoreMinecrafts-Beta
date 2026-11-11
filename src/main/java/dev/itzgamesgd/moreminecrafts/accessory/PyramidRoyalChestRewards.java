package dev.itzgamesgd.moreminecrafts.accessory;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.itzgamesgd.moreminecrafts.core.ModIds;
import dev.itzgamesgd.moreminecrafts.registry.ModItems;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.DoubleSupplier;
import java.util.function.IntUnaryOperator;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.LootPool;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.entries.LootItem;
import net.minecraft.world.level.storage.loot.functions.LootItemConditionalFunction;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import net.minecraft.world.level.storage.loot.predicates.LootItemRandomChanceCondition;
import net.minecraft.world.level.storage.loot.providers.number.ints.ConstantValue;
import net.minecraft.world.phys.Vec3;

/**
 * D13-03-I05 (Q25-Q43): structure-independent Pyramid royal-chest accessory subsystem. Nothing here is attached to
 * any loot table until the I06 integration calls {@link #addPoolTo} for each designated royal chest table and
 * registers a {@link ChestIdResolver}; with no resolver the pool never fires.
 *
 * <p>Mechanics: one additive pool per royal chest table: (1) a first-evaluation gate that claims the chest's stable
 * id in persistent saved data <em>before</em> any roll, so each chest is decided exactly once (hit or miss) even across
 * restarts; (2) a fixed 20% {@code random_chance} (no luck, difficulty or per-player input); (3) two equal-weight
 * entries (Seed of Life / Ring of Time, count 1), so exactly one accessory on success. The pool is evaluated by
 * vanilla loot generation (slot assignment is vanilla's), which happens once when the chest's loot table is first
 * unpacked by any access (GUI or hopper) and never again. The success effect is a loot function on the entries, so
 * it runs only when an accessory was actually produced.
 */
public final class PyramidRoyalChestRewards {
    public static final double CHANCE = 0.20D;
    public static final List<AccessoryDefinition> ITEMS = List.of(AccessoryDefinition.SEED_OF_LIFE, AccessoryDefinition.RING_OF_TIME);
    public static final double EFFECT_RADIUS = 16.0D;

    private PyramidRoyalChestRewards() {}

    // ---- pure model ----

    /** One chance roll, then (on success only) one uniform pick among the two equal-weight items. */
    public static Optional<AccessoryDefinition> draw(DoubleSupplier chanceRoll, IntUnaryOperator uniformPick) {
        if (!(chanceRoll.getAsDouble() < CHANCE)) return Optional.empty();
        int index = uniformPick.applyAsInt(ITEMS.size());
        return index >= 0 && index < ITEMS.size() ? Optional.of(ITEMS.get(index)) : Optional.empty();
    }

    /** Chests whose loot has been decided. Claiming happens before rolling, so a chest can never be decided twice. */
    public static final class Ledger {
        private final Set<String> decided = new LinkedHashSet<>();
        public boolean claim(String chestId) { return decided.add(chestId); }
        public boolean isDecided(String chestId) { return decided.contains(chestId); }
        public List<String> ids() { return List.copyOf(decided); }
        public static Ledger of(List<String> ids) { Ledger l = new Ledger(); l.decided.addAll(ids); return l; }
    }

    /** Whether a point at squared distance {@code distSq} from the chest gets the shared effect. */
    public static boolean inEffectRange(double distSq) { return distSq <= EFFECT_RADIUS * EFFECT_RADIUS; }

    // ---- I06 integration interface ----

    /** Supplied by I06: a stable, coordinate-independent id for the designated royal chest at this position, if any. */
    @FunctionalInterface
    public interface ChestIdResolver {
        Optional<String> idAt(ServerLevel level, BlockPos pos);
    }

    private static volatile ChestIdResolver resolver = (level, pos) -> Optional.empty();

    public static void setResolver(ChestIdResolver newResolver) { resolver = newResolver; }

    /** Adds the independent accessory pool; existing pools (royal rewards, catalyst) are neither read nor changed. */
    public static void addPoolTo(LootTable.Builder table) {
        LootPool.Builder pool = LootPool.lootPool().setRolls(Holder.direct(new ConstantValue(1)))
                .when(() -> FirstEvaluation.INSTANCE)
                .when(LootItemRandomChanceCondition.randomChance((float) CHANCE));
        for (AccessoryDefinition definition : ITEMS)
            pool.add(LootItem.lootTableItem(ModItems.ACCESSORIES.get(definition)).setWeight(1)
                    .apply(GeneratedEffect.builder()));
        table.withPool(pool);
    }

    // ---- loot condition: claim chest id on first evaluation ----

    public record FirstEvaluation() implements LootItemCondition {
        static final FirstEvaluation INSTANCE = new FirstEvaluation();
        public static final MapCodec<FirstEvaluation> CODEC = MapCodec.unit(INSTANCE);

        @Override public MapCodec<FirstEvaluation> codec() { return CODEC; }

        @Override public Set<net.minecraft.util.context.ContextKey<?>> getReferencedContextParams() {
            return Set.of(LootContextParams.ORIGIN);
        }

        @Override public boolean test(LootContext context) {
            Vec3 origin = context.getOptional(LootContextParams.ORIGIN);
            if (origin == null) return false;
            ServerLevel level = context.getLevel();
            Optional<String> id = resolver.idAt(level, BlockPos.containing(origin));
            return id.isPresent() && Data.get(level).claim(id.get());
        }
    }

    // ---- loot function: success effect, runs only when an accessory is actually generated ----

    public static final class GeneratedEffect extends LootItemConditionalFunction {
        public static final MapCodec<GeneratedEffect> CODEC = RecordCodecBuilder.mapCodec(
                i -> commonFields(i).apply(i, GeneratedEffect::new));

        private GeneratedEffect(Optional<Holder<LootItemCondition>> conditions) { super(conditions); }

        static LootItemConditionalFunction.Builder<?> builder() { return simpleBuilder(GeneratedEffect::new); }

        @Override public MapCodec<GeneratedEffect> codec() { return CODEC; }

        @Override protected ItemStack run(ItemStack stack, LootContext context) {
            Vec3 origin = context.getOptional(LootContextParams.ORIGIN);
            if (origin != null) play(context.getLevel(), origin);
            return stack;
        }
    }

    static void play(ServerLevel level, Vec3 at) {
        for (ServerPlayer player : level.players()) {
            if (!inEffectRange(player.position().distanceToSqr(at))) continue;
            level.sendParticles(player, ParticleTypes.END_ROD, false, false, at.x, at.y + 0.5D, at.z, 12, 0.3D, 0.3D, 0.3D, 0.02D);
        }
        // Vanilla distance attenuation: volume 1.0 is audible within 16 blocks.
        level.playSound(null, BlockPos.containing(at), SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.BLOCKS, 1.0F, 1.0F);
    }

    public static void initialize() {
        Registry.register(BuiltInRegistries.LOOT_CONDITION_TYPE, ModIds.id("pyramid_royal_first_evaluation"), FirstEvaluation.CODEC);
        Registry.register(BuiltInRegistries.LOOT_FUNCTION_TYPE, ModIds.id("pyramid_royal_generated_effect"), GeneratedEffect.CODEC);
    }

    // ---- persistence ----

    public static final Codec<List<String>> IDS_CODEC = Codec.STRING.listOf();

    public static final class Data extends SavedData {
        private static final Codec<Data> CODEC = IDS_CODEC.xmap(ids -> new Data(Ledger.of(ids)), d -> d.ledger.ids());
        public static final SavedDataType<Data> TYPE = new SavedDataType<>(
                Identifier.parse("moreminecrafts:pyramid_royal_chest_decisions"), Data::new, CODEC, DataFixTypes.LEVEL);

        private final Ledger ledger;
        public Data() { this(new Ledger()); }
        private Data(Ledger ledger) { this.ledger = ledger; }

        boolean claim(String chestId) {
            boolean first = ledger.claim(chestId);
            if (first) setDirty();
            return first;
        }

        static Data get(ServerLevel level) {
            return level.getServer().overworld().getDataStorage().computeIfAbsent(TYPE);
        }
    }
}
