package dev.itzgamesgd.moreminecrafts.special.elemental;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/** Server-authoritative provisional live-test slices for Water and Lightning. */
public final class ElementalSwordItem extends Item {
    public enum Element { WATER, LIGHTNING }
    private final Element element;
    public ElementalSwordItem(Element element, Properties properties){super(properties);this.element=element;}
    @Override public void postHurtEnemy(ItemStack stack,LivingEntity target,LivingEntity attacker){
        if(element==Element.WATER && !target.isAlive() && attacker instanceof ServerPlayer player) player.heal(2.0F); // PROVISIONAL
        super.postHurtEnemy(stack,target,attacker);
    }
    @Override public InteractionResult use(Level level, Player player, InteractionHand hand){
        if(level.isClientSide()) return InteractionResult.SUCCESS;
        if(!(player instanceof ServerPlayer server)||player.getCooldowns().isOnCooldown(player.getItemInHand(hand))) return InteractionResult.FAIL;
        Vec3 look=player.getLookAngle();
        Vec3 center=player.position().add(look.scale(2.0D));
        float damage=element==Element.WATER?7.0F:8.0F; // PROVISIONAL
        ServerLevel serverLevel=server.level();
        for(LivingEntity target:serverLevel.getEntitiesOfClass(LivingEntity.class,player.getBoundingBox().move(look.scale(2)).inflate(1.5),
                e->e!=player&&e.isAlive()&&!e.isAlliedTo(player)&&(!(e instanceof ServerPlayer p)||server.canHarmPlayer(p))))
            target.hurtServer(serverLevel,player.damageSources().playerAttack(player),damage);
        if(element==Element.WATER){
            player.setDeltaMovement(look.x*.8D,.65D,look.z*.8D);
            serverLevel.sendParticles(ParticleTypes.SPLASH,center.x,center.y+1,center.z,30,.5,1,.5,.1);
        } else serverLevel.sendParticles(ParticleTypes.ELECTRIC_SPARK,center.x,center.y+1,center.z,24,.6,.6,.6,.08);
        player.getCooldowns().addCooldown(player.getItemInHand(hand),element==Element.WATER?240:200); // PROVISIONAL
        return InteractionResult.SUCCESS_SERVER;
    }
}
