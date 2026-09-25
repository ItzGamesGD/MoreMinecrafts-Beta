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
public final class DarkEnergyStaffItem extends Item {
 public DarkEnergyStaffItem(Properties p){super(p);}
 @Override public InteractionResult use(Level level,Player player,InteractionHand hand){
  if(level.isClientSide()) return InteractionResult.SUCCESS; ItemStack stack=player.getItemInHand(hand);
  if(!(player instanceof ServerPlayer server)||player.getCooldowns().isOnCooldown(stack)) return InteractionResult.FAIL;
  ServerLevel sl=server.level(); Vec3 look=player.getLookAngle(); LivingEntity best=null; double bestD=144;
  for(LivingEntity e:sl.getEntitiesOfClass(LivingEntity.class,player.getBoundingBox().expandTowards(look.scale(12)).inflate(1),e->e!=player&&e.isAlive()&&!e.isAlliedTo(player))){double d=e.distanceToSqr(player);if(d<bestD){best=e;bestD=d;}}
  if(best==null)return InteractionResult.FAIL; best.hurtServer(sl,player.damageSources().magic(),7); // PROVISIONAL ranged magic
  for(int i=1;i<=12;i++)sl.sendParticles(ParticleTypes.PORTAL,player.getX()+look.x*i,player.getEyeY()+look.y*i,player.getZ()+look.z*i,2,.1,.1,.1,0);
  player.getCooldowns().addCooldown(stack,160);return InteractionResult.SUCCESS_SERVER;
 }
}
