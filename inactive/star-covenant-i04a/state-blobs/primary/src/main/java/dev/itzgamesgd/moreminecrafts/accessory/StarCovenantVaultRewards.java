package dev.itzgamesgd.moreminecrafts.accessory;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.itzgamesgd.moreminecrafts.registry.ModItems;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.function.DoubleSupplier;
import net.minecraft.core.UUIDUtil;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;

/**
 * D13-03-I04-A (Q11-Q12): structure-independent Star Covenant reward logic for the Sky Trial Tower top-floor
 * personal vault. A vault is identified only by the stable string pair ({@code towerId}, {@code vaultId}) that the
 * (not yet wired) structure integration supplies, never by coordinates, so moving a vault cannot reset claims.
 * Each (player, tower, vault) is rolled exactly once, hit or miss; the claim is recorded before the roll result is
 * used and persists in world saved data (restart/reconnect safe). The Sky Feather 15% / Gale Shard 5% pools live
 * in the existing sky-islands vault loot table and are not touched here.
 */
public final class StarCovenantVaultRewards {
    public static final double CHANCE = 0.10D;

    private StarCovenantVaultRewards() {}

    public record VaultRef(String towerId, String vaultId) {
        public VaultRef {
            if (towerId == null || towerId.isBlank() || vaultId == null || vaultId.isBlank())
                throw new IllegalArgumentException("tower and vault ids must be non-blank");
        }
    }

    public enum Outcome { ALREADY_CLAIMED, MISS, HIT }

    /** Pure claim ledger: one entry per (player, tower, vault). */
    public static final class Ledger {
        public record Entry(UUID player, String towerId, String vaultId) {}

        private final Set<Entry> entries = new LinkedHashSet<>();

        public boolean tryClaim(UUID player, VaultRef ref) {
            return entries.add(new Entry(player, ref.towerId(), ref.vaultId()));
        }

        public boolean hasClaimed(UUID player, VaultRef ref) {
            return entries.contains(new Entry(player, ref.towerId(), ref.vaultId()));
        }

        public List<Entry> entries() { return new ArrayList<>(entries); }

        public static Ledger of(List<Entry> list) {
            Ledger ledger = new Ledger();
            ledger.entries.addAll(list);
            return ledger;
        }
    }

    /** Claims first (so a repeat can never roll again), then one roll: strictly below 10% is a hit. */
    public static Outcome resolve(Ledger ledger, UUID player, VaultRef ref, DoubleSupplier roll) {
        if (!ledger.tryClaim(player, ref)) return Outcome.ALREADY_CLAIMED;
        return roll.getAsDouble() < CHANCE ? Outcome.HIT : Outcome.MISS;
    }

    /** The reward item for a HIT; delivery to the player is decided by the I04-B integration. */
    public static ItemStack reward() {
        return new ItemStack(ModItems.ACCESSORIES.get(AccessoryDefinition.STAR_COVENANT));
    }

    // ---- persistence ----

    private static final Codec<Ledger.Entry> ENTRY_CODEC = RecordCodecBuilder.create(i -> i.group(
            UUIDUtil.CODEC.fieldOf("player").forGetter(Ledger.Entry::player),
            Codec.STRING.fieldOf("tower").forGetter(Ledger.Entry::towerId),
            Codec.STRING.fieldOf("vault").forGetter(Ledger.Entry::vaultId)).apply(i, Ledger.Entry::new));

    public static final Codec<List<Ledger.Entry>> ENTRIES_CODEC = ENTRY_CODEC.listOf();

    /** World-persistent ledger. */
    public static final class Data extends SavedData {
        private static final Codec<Data> CODEC = ENTRIES_CODEC.xmap(
                list -> new Data(Ledger.of(list)), data -> data.ledger.entries());
        public static final SavedDataType<Data> TYPE = new SavedDataType<>(
                Identifier.parse("moreminecrafts:star_covenant_vault_claims"), Data::new, CODEC, DataFixTypes.LEVEL);

        private final Ledger ledger;

        public Data() { this(new Ledger()); }
        private Data(Ledger ledger) { this.ledger = ledger; }

        public Outcome resolve(UUID player, VaultRef ref, DoubleSupplier roll) {
            Outcome outcome = StarCovenantVaultRewards.resolve(ledger, player, ref, roll);
            if (outcome != Outcome.ALREADY_CLAIMED) setDirty();
            return outcome;
        }

        public static Data get(MinecraftServer server) {
            return server.overworld().getDataStorage().computeIfAbsent(TYPE);
        }
    }
}
