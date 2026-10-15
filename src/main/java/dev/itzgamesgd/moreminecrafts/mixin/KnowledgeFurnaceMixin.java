package dev.itzgamesgd.moreminecrafts.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import dev.itzgamesgd.moreminecrafts.furnace.FurnaceResearchAttribution;
import dev.itzgamesgd.moreminecrafts.furnace.FurnaceResearchState;
import dev.itzgamesgd.moreminecrafts.furnace.FurnaceResearchStateAccess;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import dev.itzgamesgd.moreminecrafts.progression.SmeltingOutputProvenance;
import net.minecraft.core.NonNullList;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.AbstractFurnaceBlockEntity;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Records the exact input/output pair after vanilla has accepted a completed cooking result. */
@Mixin(AbstractFurnaceBlockEntity.class)
abstract class KnowledgeFurnaceMixin implements FurnaceResearchStateAccess {
    @org.spongepowered.asm.mixin.Unique
    private FurnaceResearchState moreminecrafts$researchState = FurnaceResearchState.empty();

    @Override
    public FurnaceResearchState moreminecrafts$getResearchState() {
        return moreminecrafts$researchState;
    }

    @Override
    public void moreminecrafts$setResearchState(FurnaceResearchState state) {
        moreminecrafts$researchState = state == null ? FurnaceResearchState.empty() : state;
        ((AbstractFurnaceBlockEntity) (Object) this).setChanged();
    }

    @Inject(method = "startOpen", at = @At("HEAD"))
    private void moreminecrafts$attributeGuiOpener(Player player, CallbackInfo ci) {
        if (player instanceof ServerPlayer serverPlayer)
            FurnaceResearchAttribution.opened((AbstractFurnaceBlockEntity) (Object) this, serverPlayer);
    }

    @Inject(method = "setItem", at = @At("HEAD"))
    private void moreminecrafts$discardReplacedOutput(int slot, ItemStack stack, CallbackInfo ci) {
        if (slot == 2)
            SmeltingOutputProvenance.discard((AbstractFurnaceBlockEntity) (Object) this);
    }

    @Inject(method = "saveAdditional", at = @At("TAIL"))
    private void moreminecrafts$saveSmeltProvenance(ValueOutput output, CallbackInfo ci) {
        AbstractFurnaceBlockEntity furnace = (AbstractFurnaceBlockEntity) (Object) this;
        if (!moreminecrafts$researchState.equals(FurnaceResearchState.empty())) {
            output.putInt("MoreMinecraftsFurnaceResearchVersion", 1);
            output.store("MoreMinecraftsFurnaceResearchState", FurnaceResearchState.CODEC,
                    moreminecrafts$researchState);
        }
        var pending = SmeltingOutputProvenance.save(furnace);
        if (!pending.isEmpty()) {
            output.putInt("MoreMinecraftsSmeltProvenanceVersion", SmeltingOutputProvenance.DATA_VERSION);
            output.store("MoreMinecraftsSmeltProvenance", SmeltingOutputProvenance.CODEC, pending);
        }
    }

    @Inject(method = "loadAdditional", at = @At("TAIL"))
    private void moreminecrafts$loadSmeltProvenance(ValueInput input, CallbackInfo ci) {
        AbstractFurnaceBlockEntity furnace = (AbstractFurnaceBlockEntity) (Object) this;
        moreminecrafts$researchState = FurnaceResearchState.empty();
        if (input.getIntOr("MoreMinecraftsFurnaceResearchVersion", 0) == 1)
            input.read("MoreMinecraftsFurnaceResearchState", FurnaceResearchState.CODEC)
                    .ifPresent(state -> moreminecrafts$researchState = state);
        if (input.getIntOr("MoreMinecraftsSmeltProvenanceVersion", 0) == SmeltingOutputProvenance.DATA_VERSION)
            input.read("MoreMinecraftsSmeltProvenance", SmeltingOutputProvenance.CODEC)
                    .ifPresent(pending -> SmeltingOutputProvenance.load(furnace, pending));
    }

    @WrapOperation(method = "serverTick",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/block/entity/AbstractFurnaceBlockEntity;burn(Lnet/minecraft/core/NonNullList;Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/item/ItemStack;)V"))
    private static void moreminecrafts$rememberCompletedOutput(NonNullList<ItemStack> items, ItemStack input,
            ItemStack result, Operation<Void> original,
            @com.llamalad7.mixinextras.sugar.Local(argsOnly = true) AbstractFurnaceBlockEntity furnace) {
        ItemStack consumedInput = input.copy();
        ItemStack producedOutput = result.copy();
        original.call(items, input, result);
        SmeltingOutputProvenance.record(furnace, consumedInput, producedOutput);
    }
}
