package dev.itzgamesgd.moreminecrafts.mixin;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.players.PlayerList;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

/** Saves exactly one player synchronously; {@code saveAll} would rewrite every online player for each redemption. */
@Mixin(PlayerList.class)
public interface PlayerListAccessor {
    @Invoker("save")
    void moreminecrafts$save(ServerPlayer player);
}
