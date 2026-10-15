package dev.itzgamesgd.moreminecrafts.mixin;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import dev.itzgamesgd.moreminecrafts.enchantment.EnchantmentRuntime;
import dev.itzgamesgd.moreminecrafts.progression.K5NaturalTreeProvenance;
import dev.itzgamesgd.moreminecrafts.progression.KnowledgeRuntime;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ServerPlayerGameMode;
import net.minecraft.world.level.GameType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Final;

@Mixin(ServerPlayerGameMode.class)
abstract class ServerPlayerGameModeMixin {
    @Shadow @Final protected ServerPlayer player;

    @WrapMethod(method = "destroyBlock")
    private boolean moreminecrafts$scopeBlockBreak(BlockPos pos, Operation<Boolean> original) {
        var brokenState = player.level().getBlockState(pos);
        String blockId = String.valueOf(net.minecraft.core.registries.BuiltInRegistries.BLOCK
                .getKey(brokenState.getBlock()));
        String path = blockId.substring(blockId.indexOf(':') + 1);
        boolean eligibleOre = path.endsWith("_ore");
        // Snapshot before Level.removeBlock invalidates the chunk marker. Vanilla can return
        // true even when removeBlock failed, so verify the postcondition as well.
        boolean eligible = player.gameMode.getGameModeForPlayer() == GameType.SURVIVAL
                && K5NaturalTreeProvenance.hasMarker(player.level(), pos);
        try (var miningContext = dev.itzgamesgd.moreminecrafts.progression.ResearchMiningContext.enter(player);
                var pickaxeDamage = KnowledgeRuntime.beginPickaxeDamageTracking(player)) {
            try (var xpScope = eligibleOre
                    ? dev.itzgamesgd.moreminecrafts.progression.ResearchXpOrbContext.enter(player,
                            "resources_root", "ore_mining_xp", "resources_ore")
                    : dev.itzgamesgd.moreminecrafts.progression.ResearchXpOrbContext.enter(null, "", "", "")) {
                EnchantmentRuntime.enterBlockBreak();
                try {
                    boolean result = original.call(pos);
                    boolean removed = result && player.level().getBlockState(pos).isAir();
                    KnowledgeRuntime.finishPickaxeDamageTracking(player, pickaxeDamage, removed);
                    if (eligible && removed && !K5NaturalTreeProvenance.containsMarker(player.level(), pos))
                        KnowledgeRuntime.recordK5NaturalLogMined(player);
                    return result;
                } finally {
                    EnchantmentRuntime.exitBlockBreak();
                }
            }
        }
    }
}
