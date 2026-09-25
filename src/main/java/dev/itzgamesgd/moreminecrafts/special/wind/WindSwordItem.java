package dev.itzgamesgd.moreminecrafts.special.wind;

import dev.itzgamesgd.moreminecrafts.ability.AbilityActivation;
import dev.itzgamesgd.moreminecrafts.special.SuccessfulHitCounter;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
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

public final class WindSwordItem extends Item {
    private static final Map<UUID, SuccessfulHitCounter> HITS=new HashMap<>();
    public WindSwordItem(Properties properties){super(properties);}
    @Override public void hurtEnemy(ItemStack stack, LivingEntity primary, LivingEntity attacker) {
        if(attacker instanceof ServerPlayer player){
            var next=HITS.getOrDefault(player.getUUID(),SuccessfulHitCounter.EMPTY).recordSuccessfulHit(10);
            HITS.put(player.getUUID(),next.counter());
            if(next.triggered()) sweep(player,primary);
        }
        super.hurtEnemy(stack,primary,attacker);
    }
    private static void sweep(ServerPlayer player,LivingEntity primary){
        ServerLevel level=player.level();
        for(LivingEntity target:level.getEntitiesOfClass(LivingEntity.class,primary.getBoundingBox().inflate(3),
                e->e!=player&&e!=primary&&e.isAlive()&&!e.isAlliedTo(player)&&(!(e instanceof ServerPlayer p)||player.canHarmPlayer(p)))){
            if(target.hurtServer(level,player.damageSources().playerAttack(player),3.0F))
                level.sendParticles(ParticleTypes.SWEEP_ATTACK,target.getX(),target.getY()+.8,target.getZ(),2,.2,.2,.2,0);
        }
    }
    @Override public InteractionResult use(Level level,Player player,InteractionHand hand){
        if(level.isClientSide()) return InteractionResult.SUCCESS;
        return player instanceof ServerPlayer server&&AbilityActivation.tryActivate(server,player.getItemInHand(hand),WindBladeAbility.INSTANCE)==AbilityActivation.Result.ACTIVATED
                ?InteractionResult.SUCCESS_SERVER:InteractionResult.FAIL;
    }
}
