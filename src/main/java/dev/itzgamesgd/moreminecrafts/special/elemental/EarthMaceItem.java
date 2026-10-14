package dev.itzgamesgd.moreminecrafts.special.elemental;

import dev.itzgamesgd.moreminecrafts.ability.AbilityActivation;
import dev.itzgamesgd.moreminecrafts.ability.ActiveEquipmentAbility;
import dev.itzgamesgd.moreminecrafts.core.ModIds;
import dev.itzgamesgd.moreminecrafts.special.vfx.T2ElementalVfx;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.MaceItem;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

public final class EarthMaceItem extends MaceItem {
    public EarthMaceItem(Properties properties) { super(properties); }

    @Override
    public void hurtEnemy(ItemStack stack, LivingEntity target, LivingEntity attacker) {
        if (attacker instanceof ServerPlayer player && MaceItem.canSmashAttack(attacker)) {
            EarthGroundAnchor.resolve(player, target)
                    .ifPresent(impact -> EarthSeismicRuntime.start(player, impact));
        }
        super.hurtEnemy(stack, target, attacker);
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        if (level.isClientSide()) return InteractionResult.SUCCESS;
        return player instanceof ServerPlayer server
                && AbilityActivation.tryActivate(server, player.getItemInHand(hand), EarthShatter.INSTANCE)
                == AbilityActivation.Result.ACTIVATED
                ? InteractionResult.SUCCESS_SERVER : InteractionResult.FAIL;
    }

    private static Vec3 horizontalFacing(ServerPlayer player) {
        Vec3 facing = new Vec3(player.getLookAngle().x, 0.0D, player.getLookAngle().z);
        return facing.lengthSqr() < 1.0E-8D ? new Vec3(0.0D, 0.0D, 1.0D) : facing.normalize();
    }

    private static final class EarthShatter implements ActiveEquipmentAbility {
        private static final EarthShatter INSTANCE = new EarthShatter();
        private static final Identifier ID = ModIds.id("earth_shatter");
        @Override public Identifier id() { return ID; }
        @Override public int cooldownTicks() { return 300; }
        @Override public boolean canActivate(ServerPlayer player, ItemStack source) { return player.onGround(); }

        @Override
        public boolean activate(ServerPlayer player, ItemStack source) {
            ServerLevel level = player.level();
            Vec3 facing = horizontalFacing(player);
            for (LivingEntity target : level.getEntitiesOfClass(LivingEntity.class,
                    player.getBoundingBox().expandTowards(facing.scale(8.0D)).inflate(5.0D, 1.5D, 5.0D),
                    target -> ElementalCombat.validTarget(player, target))) {
                double dx = target.getX() - player.getX();
                double dz = target.getZ() - player.getZ();
                if (EarthCombatMath.inForwardCone(facing.x, facing.z, dx, dz, 8.0D, 48.0D)
                        && target.hurtServer(level, player.damageSources().playerAttack(player), 6.0F)) {
                    Vec3 push = new Vec3(dx, 0.0D, dz).normalize().scale(1.25D);
                    target.push(push.x, 0.55D, push.z);
                }
            }
            T2ElementalVfx.earthCone(level, player.position().add(0.0D, 0.15D, 0.0D), facing, 8.0D);
            level.playSound(null, player.blockPosition(), SoundEvents.GENERIC_EXPLODE.value(),
                    SoundSource.PLAYERS, 0.9F, 0.65F);
            return true;
        }
    }
}
