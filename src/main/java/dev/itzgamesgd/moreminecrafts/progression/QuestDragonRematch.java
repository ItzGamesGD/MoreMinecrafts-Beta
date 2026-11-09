package dev.itzgamesgd.moreminecrafts.progression;

import java.util.UUID;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;

/**
 * Ender Dragon rematch. The dragon's vanilla kill-credit player is recorded when the dragon dies; the credit
 * is granted when {@code EnderDragonFight.setDragonKilled} completes the fight, and only if the fight had
 * already recorded a previous kill <em>before</em> that call mutated it. A first dragon, a dragon with no
 * attributable player, a repeated completion callback and a dragon that is not the fight's own never qualify.
 */
public final class QuestDragonRematch {
    static final String COUNTER = "dragon_rematch_kills";

    private QuestDragonRematch() {}

    public static void initialize() {
        ServerLivingEntityEvents.AFTER_DEATH.register((entity, source) -> {
            if (entity instanceof EnderDragon dragon && dragon.level().getServer() != null)
                dying(dragon, dragon.level().getServer());
        });
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
            if (data(server).takePending(handler.player.getUUID())) award(handler.player);
        });
    }

    private static QuestDragonRematchData data(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(QuestDragonRematchData.TYPE);
    }

    static void dying(EnderDragon dragon, MinecraftServer server) {
        if (dragon.getKillCredit() instanceof ServerPlayer credited)
            data(server).putCandidate(dragon.getUUID(), credited.getUUID());
    }

    /**
     * Called from the HEAD of {@code EnderDragonFight.setDragonKilled}, before it mutates the fight.
     *
     * @param ownDragon        the dying dragon is the fight's own and the fight was not already completed
     * @param previouslyKilled the fight's {@code hasPreviouslyKilledDragon()} before this call
     */
    public static void fightCompleting(EnderDragon dragon, boolean ownDragon, boolean previouslyKilled) {
        MinecraftServer server = dragon.level().getServer();
        if (server == null) return;
        QuestDragonRematchData data = data(server);
        UUID credited = data.takeCandidate(dragon.getUUID());
        if (!ownDragon || !previouslyKilled || credited == null) return;
        ServerPlayer player = server.getPlayerList().getPlayer(credited);
        if (player != null) award(player);
        else data.addPending(credited);
    }

    private static void award(ServerPlayer player) {
        XpQuestTracking.addCounter(player, COUNTER, 1);
        XpQuestTracking.reconcile(player);
    }
}
