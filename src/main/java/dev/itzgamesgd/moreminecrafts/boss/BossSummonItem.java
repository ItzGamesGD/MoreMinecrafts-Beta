package dev.itzgamesgd.moreminecrafts.boss;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/**
 * Consumable boss summon. Using it spawns one boss 8 blocks ahead of the player and destroys the item.
 * Refused while a living instance of the same boss already exists; no cooldown.
 */
public final class BossSummonItem extends Item {
    private final EntityType<? extends LivingEntity> bossType;

    public BossSummonItem(EntityType<? extends LivingEntity> bossType, Properties properties) {
        super(properties);
        this.bossType = bossType;
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        if (level.isClientSide() || !(level instanceof ServerLevel serverLevel)) return InteractionResult.SUCCESS;
        if (!serverLevel.getEntities(bossType, LivingEntity::isAlive).isEmpty()) {
            player.sendSystemMessage(Component.literal("This boss is already alive."));
            return InteractionResult.FAIL;
        }
        LivingEntity boss = bossType.create(serverLevel, EntitySpawnReason.COMMAND);
        if (boss == null) return InteractionResult.FAIL;
        double yaw = Math.toRadians(player.getYRot());
        double x = player.getX() - Math.sin(yaw) * 8.0;
        double z = player.getZ() + Math.cos(yaw) * 8.0;
        boss.snapTo(x, player.getY(), z, player.getYRot() + 180.0F, 0.0F);
        serverLevel.addFreshEntity(boss);
        ItemStack stack = player.getItemInHand(hand);
        stack.shrink(1);
        return InteractionResult.SUCCESS_SERVER;
    }
}
