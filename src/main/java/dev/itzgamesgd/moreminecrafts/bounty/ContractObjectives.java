package dev.itzgamesgd.moreminecrafts.bounty;

import java.util.Map;
import java.util.WeakHashMap;
import java.util.UUID;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.levelgen.structure.StructureStart;

/** Server-authoritative combat and structure-piece entry detection. */
public final class ContractObjectives {
    private static final DamageCredit DAMAGE = new DamageCredit();
    private static final Map<LivingEntity, Float> HEALTH_BEFORE = new WeakHashMap<>();
    private ContractObjectives() {}

    public static void initialize() {
        ServerLivingEntityEvents.ALLOW_DAMAGE.register((entity, source, amount) -> {
            if (!entity.level().isClientSide()) HEALTH_BEFORE.put(entity, entity.getHealth());
            return true;
        });
        ServerLivingEntityEvents.AFTER_DAMAGE.register((entity, source, baseDamage, damageTaken, blocked) -> {
            Float before = HEALTH_BEFORE.remove(entity);
            if (!(entity.level() instanceof ServerLevel level) || before == null || blocked) return;
            if (!(source.getEntity() instanceof ServerPlayer player)) return;
            float actual = Math.min(Math.max(0, before - entity.getHealth()), Math.max(0, damageTaken));
            DAMAGE.record(entity.getUUID(), player.getUUID(), level.getServer().getTickCount(), actual, entity.getMaxHealth());
        });
        ServerLivingEntityEvents.AFTER_DEATH.register((entity, source) -> {
            HEALTH_BEFORE.remove(entity);
            if (!(entity.level() instanceof ServerLevel level)) return;
            UUID finalBlow = source.getEntity() instanceof ServerPlayer player ? player.getUUID() : null;
            String target = level.registryAccess().lookupOrThrow(Registries.ENTITY_TYPE).getKey(entity.getType()).toString();
            for (UUID id : DAMAGE.onDeath(entity.getUUID(), finalBlow, level.getServer().getTickCount(), entity.getMaxHealth())) {
                ServerPlayer player = level.getServer().getPlayerList().getPlayer(id);
                if (player != null) ContractService.advance(player, ContractData.Category.COMBAT, target);
            }
        });
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            int tick = server.getTickCount();
            if (tick % 20 == 0) DAMAGE.expire(tick);
            for (ServerPlayer player : server.getPlayerList().getPlayers()) {
                ContractMaterializer.materialize(player);
                detectStructure(player);
            }
        });
    }

    private static void detectStructure(ServerPlayer player) {
        boolean exploring = false;
        for (int slot = 0; slot < player.getInventory().getContainerSize(); slot++) {
            ContractData data = ContractStacks.read(player.getInventory().getItem(slot));
            if (data == null || data.status() != ContractData.Status.ACCEPTED) continue;
            if (data.objectives().stream().anyMatch(objective -> objective.category() == ContractData.Category.EXPLORATION
                    && objective.progress() < objective.required())) { exploring = true; break; }
        }
        if (!exploring) return;
        ServerLevel level = (ServerLevel) player.level();
        StructureStart start = level.structureManager().getStructureWithPieceAt(player.blockPosition(), holder -> true);
        if (start == StructureStart.INVALID_START || !start.isValid()) return;
        String target = level.registryAccess().lookupOrThrow(Registries.STRUCTURE).getKey(start.getStructure()).toString();
        String instance = level.dimension().identifier() + ":" + start.getChunkPos().pack();
        ContractService.enterStructure(player, target, instance);
    }
}
