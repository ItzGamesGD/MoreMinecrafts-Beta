package dev.itzgamesgd.moreminecrafts.special.elemental;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.MaceItem;
import net.minecraft.world.level.Level;
public final class EarthMaceItem extends MaceItem {
 public EarthMaceItem(Properties p){super(p);}
 @Override public InteractionResult use(Level level,Player player,InteractionHand hand){
  if(level.isClientSide()) return InteractionResult.SUCCESS;
  ItemStack stack=player.getItemInHand(hand);
  if(!(player instanceof ServerPlayer server)||player.getCooldowns().isOnCooldown(stack)) return InteractionResult.FAIL;
  ServerLevel sl=server.level();
  for(LivingEntity e:sl.getEntitiesOfClass(LivingEntity.class,player.getBoundingBox().inflate(3),e->e!=player&&e.isAlive()&&!e.isAlliedTo(player))){e.hurtServer(sl,player.damageSources().playerAttack(player),4);e.push(e.position().subtract(player.position()).normalize().scale(1.2));}
  sl.sendParticles(ParticleTypes.POOF,player.getX(),player.getY(),player.getZ(),30,2,.2,2,.05);
  player.getCooldowns().addCooldown(stack,300); return InteractionResult.SUCCESS_SERVER;
 }
}
