package dev.itzgamesgd.moreminecrafts.progression;

import java.util.UUID;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.ElderGuardian;
import net.minecraft.world.entity.monster.Guardian;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureStart;

/**
 * Ocean Monument participant evidence. A monument is identified by dimension plus the structure's start chunk.
 * A player is a participant when, while the monument is not yet completed, they (a) damage an Elder Guardian that
 * stands inside that monument's bounds (the damage source resolves projectiles and owned entities to the
 * player), or (b) are inside the bounds and deal damage to, or receive damage from, a Guardian or Elder
 * Guardian. Presence alone is never evidence. At completion every player with Elder Guardian damage, plus every
 * player with combat inside the previous 12,000 game ticks, receives one credit for that monument; players who are
 * offline receive it at their next login.
 */
public final class QuestMonumentParticipation {
    static final long WINDOW_TICKS = 12_000L;
    static final String CLEARED = "set.monuments_cleared";
    private static final int CLEARED_CAP = 3;
    private static final ResourceKey<Structure> MONUMENT = ResourceKey.create(Registries.STRUCTURE,
            Identifier.withDefaultNamespace("monument"));

    private QuestMonumentParticipation() {}

    public static void initialize() {
        ServerLivingEntityEvents.AFTER_DAMAGE.register((victim, source, base, taken, blocked) -> {
            if (!blocked && taken > 0.0F) damaged(victim, source);
        });
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> applyPendingCredits(handler.player));
    }

    /** The monument whose bounds contain {@code pos}, or INVALID. Only the Overworld has monuments. */
    static StructureStart monumentAt(ServerLevel level, BlockPos pos) {
        if (level.dimension() != Level.OVERWORLD) return StructureStart.INVALID_START;
        Holder<Structure> holder = level.registryAccess().lookupOrThrow(Registries.STRUCTURE).get(MONUMENT).orElse(null);
        if (holder == null) return StructureStart.INVALID_START;
        StructureStart start = level.structureManager().getStructureAt(pos, HolderSet.direct(holder));
        return start.isValid() ? start : StructureStart.INVALID_START;
    }

    /** The killing blow is reported from AFTER_DEATH, which runs before AFTER_DAMAGE and before completion. */
    static void finalBlow(ElderGuardian elder, DamageSource source) {
        if (elder.level() instanceof ServerLevel level && source.getEntity() instanceof ServerPlayer attacker)
            playerHitGuardian(level, attacker, elder);
    }

    static void damaged(LivingEntity victim, DamageSource source) {
        if (!(victim.level() instanceof ServerLevel level) || level.dimension() != Level.OVERWORLD) return;
        if (victim instanceof Guardian guardian && source.getEntity() instanceof ServerPlayer attacker)
            playerHitGuardian(level, attacker, guardian);
        else if (victim instanceof ServerPlayer player && source.getEntity() instanceof Guardian)
            record(level, player, monumentAt(level, player.blockPosition()), false);
    }

    private static void playerHitGuardian(ServerLevel level, ServerPlayer attacker, Guardian guardian) {
        StructureStart target = monumentAt(level, guardian.blockPosition());
        if (guardian instanceof ElderGuardian) record(level, attacker, target, true);
        StructureStart own = monumentAt(level, attacker.blockPosition());
        // Combat belongs to the monument the player stands in, unless the guardian is inside a different monument.
        if (own.isValid() && (!target.isValid() || own.getChunkPos().equals(target.getChunkPos())))
            record(level, attacker, own, false);
    }

    private static void record(ServerLevel level, ServerPlayer player, StructureStart monument, boolean elderDamage) {
        if (!monument.isValid()) return;
        data(level).recordParticipation(monument.getChunkPos().pack(), player.getUUID(), level.getGameTime(), elderDamage);
    }

    private static MonumentRewardData data(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(MonumentRewardData.TYPE);
    }

    /** Called when the existing completion path claims a monument (immediately or after a deferred chunk load). */
    static void completed(ServerLevel level, MonumentRewardData rewards, long monumentKey) {
        MinecraftServer server = level.getServer();
        for (UUID participant : rewards.qualifiedParticipants(monumentKey, level.getGameTime(), WINDOW_TICKS)) {
            ServerPlayer player = server.getPlayerList().getPlayer(participant);
            if (player != null) credit(player, monumentKey);
            else rewards.addPendingCredit(participant, monumentKey);
        }
        rewards.clearParticipants(monumentKey);
    }

    static void applyPendingCredits(ServerPlayer player) {
        MinecraftServer server = player.level().getServer();
        if (server == null) return;
        ServerLevel overworld = server.overworld();
        for (long monument : data(overworld).takePendingCredits(player.getUUID())) credit(player, monument);
    }

    /** One credit per distinct monument, however many times completion or login is processed. */
    static void credit(ServerPlayer player, long monumentKey) {
        ResearchProgressState before = KnowledgeRuntime.state(player);
        if (QuestRules.distinctCount(before, CLEARED) >= CLEARED_CAP) return;
        ResearchProgressState next = QuestRules.observeDistinct(before, CLEARED, Level.OVERWORLD.identifier() + ":" + monumentKey);
        if (!next.equals(before)) {
            KnowledgeRuntime.updateState(player, next);
            XpQuestTracking.reconcile(player);
        }
    }
}
