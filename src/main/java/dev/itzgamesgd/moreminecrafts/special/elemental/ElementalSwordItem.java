package dev.itzgamesgd.moreminecrafts.special.elemental;

import dev.itzgamesgd.moreminecrafts.ability.AbilityActivation;
import dev.itzgamesgd.moreminecrafts.ability.ActiveEquipmentAbility;
import dev.itzgamesgd.moreminecrafts.core.ModIds;
import dev.itzgamesgd.moreminecrafts.special.ElementalHitProgress;
import dev.itzgamesgd.moreminecrafts.special.vfx.T2ElementalVfx;
import net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/** Server-authoritative Water and Lightning T2 swords. Balance constants remain provisional. */
public final class ElementalSwordItem extends Item {
    public enum Element { WATER, LIGHTNING }

    private final Element element;

    public ElementalSwordItem(Element element, Properties properties) {
        super(properties);
        this.element = element;
    }

    @Override
    public float getAttackDamageBonus(Entity target, float baseAttackDamage, DamageSource damageSource) {
        float bonus = super.getAttackDamageBonus(target, baseAttackDamage, damageSource);
        if (element == Element.LIGHTNING
                && damageSource.getEntity() instanceof ServerPlayer player
                && player.getMainHandItem().getItem() == this) {
            bonus += ElementalHitProgress.primaryHitBonusDamage(
                    player.getUUID(), ElementalHitProgress.Weapon.LIGHTNING);
        }
        return bonus;
    }

    @Override
    public void hurtEnemy(ItemStack stack, LivingEntity target, LivingEntity attacker) {
        if (attacker instanceof ServerPlayer player) {
            ServerLevel level = player.level();
            if (element == Element.WATER) {
                T2ElementalVfx.waterHit(level, player.getEyePosition(),
                        target.position().add(0.0D, target.getBbHeight() * 0.55D, 0.0D));
            } else {
                T2ElementalVfx.lightningPrimary(level,
                        target.position().add(0.0D, target.getBbHeight() * 0.55D, 0.0D));
                if (ElementalHitProgress.record(player.getUUID(), ElementalHitProgress.Weapon.LIGHTNING)) {
                    T2ElementalVfx.lightningProc(level,
                            target.position().add(0.0D, target.getBbHeight() * 0.6D, 0.0D));
                    level.playSound(null, target.blockPosition(), SoundEvents.LIGHTNING_BOLT_IMPACT,
                            SoundSource.PLAYERS, 0.7F, 1.5F);
                }
            }
        }
        super.hurtEnemy(stack, target, attacker);
    }

    @Override
    public void postHurtEnemy(ItemStack stack, LivingEntity target, LivingEntity attacker) {
        if (element == Element.WATER && !target.isAlive() && attacker instanceof ServerPlayer player) {
            player.heal(2.0F);
        }
        super.postHurtEnemy(stack, target, attacker);
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        if (level.isClientSide()) return InteractionResult.SUCCESS;
        ActiveEquipmentAbility ability = element == Element.WATER ? WaterDiveAbility.INSTANCE : LightningSlash.INSTANCE;
        return player instanceof ServerPlayer server
                && AbilityActivation.tryActivate(server, player.getItemInHand(hand), ability)
                == AbilityActivation.Result.ACTIVATED
                ? InteractionResult.SUCCESS_SERVER : InteractionResult.FAIL;
    }

    private static final class WaterDiveAbility implements ActiveEquipmentAbility {
        private static final WaterDiveAbility INSTANCE = new WaterDiveAbility();
        private static final Identifier ID = ModIds.id("water_dive");
        @Override public Identifier id() { return ID; }
        @Override public int cooldownTicks() { return 240; }
        @Override public boolean activate(ServerPlayer player, ItemStack source) {
            return WaterDiveRuntime.launch(player);
        }
    }

    private static final class LightningSlash implements ActiveEquipmentAbility {
        private static final LightningSlash INSTANCE = new LightningSlash();
        private static final Identifier ID = ModIds.id("lightning_slash");
        @Override public Identifier id() { return ID; }
        @Override public int cooldownTicks() { return 200; }
        @Override public boolean activate(ServerPlayer player, ItemStack source) {
            ServerLevel level = player.level();
            Vec3 facing = player.getLookAngle().normalize();
            Vec3 horizontal = new Vec3(facing.x, 0.0D, facing.z);
            if (horizontal.lengthSqr() > 1.0E-8D) {
                horizontal = horizontal.normalize();
                player.setDeltaMovement(horizontal.x * 0.65D, player.getDeltaMovement().y, horizontal.z * 0.65D);
                player.connection.send(new ClientboundSetEntityMotionPacket(player));
            }
            Vec3 start = player.getEyePosition();
            Vec3 end = start.add(facing.scale(4.0D));
            AABB search = new AABB(start, end).inflate(1.0D);
            for (LivingEntity target : level.getEntitiesOfClass(LivingEntity.class, search,
                    target -> ElementalCombat.validTarget(player, target))) {
                Vec3 offset = target.getBoundingBox().getCenter().subtract(start);
                double along = offset.dot(facing);
                if (along >= 0.0D && along <= 4.0D
                        && offset.subtract(facing.scale(along)).lengthSqr() <= 1.44D) {
                    target.hurtServer(level, player.damageSources().playerAttack(player), 8.0F);
                }
            }
            T2ElementalVfx.lightningSlash(level, start, end, facing);
            level.playSound(null, player.blockPosition(), SoundEvents.LIGHTNING_BOLT_IMPACT,
                    SoundSource.PLAYERS, 0.9F, 1.7F);
            return true;
        }
    }
}
