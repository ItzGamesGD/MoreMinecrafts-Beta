package dev.itzgamesgd.moreminecrafts.mixin;
import dev.itzgamesgd.moreminecrafts.progression.T1Supply;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.entity.*;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.util.RandomSource;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(Mob.class)
abstract class T1MobSupplyMixin {
    @Inject(method="populateDefaultEquipmentEnchantments",at=@At("RETURN"))
    private void moreminecrafts$rareEquipment(ServerLevelAccessor level,RandomSource random,DifficultyInstance difficulty,CallbackInfo ci){
        Mob mob=(Mob)(Object)this;for(EquipmentSlot slot:EquipmentSlot.values())T1Supply.filterLoot(mob.getItemBySlot(slot),random::nextDouble);
    }
}
