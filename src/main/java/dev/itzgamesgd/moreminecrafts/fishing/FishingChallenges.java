package dev.itzgamesgd.moreminecrafts.fishing;
import dev.itzgamesgd.moreminecrafts.network.FishingChallengePayload;
import dev.itzgamesgd.moreminecrafts.registry.ModDataComponents;
import dev.itzgamesgd.moreminecrafts.registry.ModItems;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.entity.projectile.FishingHook;
import net.minecraft.world.item.ItemStack;
import java.util.*;

public final class FishingChallenges {
 private static final Map<UUID,Challenge> ACTIVE=new HashMap<>();
 private FishingChallenges(){}
 public static void initialize(){ServerPlayConnectionEvents.DISCONNECT.register((handler,server)->ACTIVE.remove(handler.player.getUUID()));ServerTickEvents.END_SERVER_TICK.register(server->{long tick=server.getTickCount();for(ServerPlayer p:server.getPlayerList().getPlayers()){Challenge c=ACTIVE.get(p.getUUID());if(c!=null&&(FishingTiming.timedOut(tick-c.startTick())||!p.isAlive()))finish(p,false);}});}
 public static boolean active(ServerPlayer p){return ACTIVE.containsKey(p.getUUID());}
 public static void start(ServerPlayer p,FishingHook hook,ItemStack rod){if(active(p))return;RodEquipment e=rod.getOrDefault(ModDataComponents.ROD_EQUIPMENT,RodEquipment.EMPTY);int depth=measureDepth(hook);ACTIVE.put(p.getUUID(),new Challenge(p.serverLevel().getServer().getTickCount(),e.grade(FishingEquipmentFamily.REEL),e.grade(FishingEquipmentFamily.CRANKBAIT),e.grade(FishingEquipmentFamily.MINNOW),depth,0,e));wear(e);rod.set(ModDataComponents.ROD_EQUIPMENT,new RodEquipment(e.reel(),e.crankbait(),e.minnow()));ServerPlayNetworking.send(p,new FishingChallengePayload(true));}
 public static void confirm(ServerPlayer p){Challenge c=ACTIVE.get(p.getUUID());if(c==null)return;long elapsed=p.serverLevel().getServer().getTickCount()-c.startTick();finish(p,!FishingTiming.timedOut(elapsed)&&FishingTiming.successful(elapsed));}
 private static void finish(ServerPlayer p,boolean success){Challenge c=ACTIVE.remove(p.getUUID());if(c==null)return;if(success){List<FishSpecies> pool=FishingLogic.eligible(c.depth(),c.reel(),c.minnow());if(!pool.isEmpty()){FishingLogic.CatchRoll roll=FishingLogic.select(pool,c.crankbait(),c.luck(),p.getRandom());FishSpecies species=roll.species();int length=species.lengthFromRoll(p.getRandom().nextInt());ItemStack fish=new ItemStack(ModItems.fishItem(species));fish.set(ModDataComponents.FISH_GRADE,roll.grade());fish.set(ModDataComponents.FISH_LENGTH,length);if(!p.getInventory().add(fish))p.drop(fish,false);}}ServerPlayNetworking.send(p,new FishingChallengePayload(false));}
 private static int measureDepth(FishingHook hook){BlockPos.MutableBlockPos pos=hook.blockPosition().mutable();int water=0;for(int i=0;i<20&&hook.level().getFluidState(pos).is(FluidTags.WATER);i++,pos.move(0,-1,0))water++;return water>=12?5:water>=8?4:water>=5?3:water>=3?2:1;}
 private static void wear(RodEquipment e){for(int i=0;i<3;i++){ItemStack s=e.slot(i);if(!s.isEmpty()){s.setDamageValue(s.getDamageValue()+1);if(s.getDamageValue()>=s.getMaxDamage())s.shrink(1);}}}
 public record Challenge(long startTick,int reel,int crankbait,int minnow,int depth,int luck,RodEquipment equipment){}
}
