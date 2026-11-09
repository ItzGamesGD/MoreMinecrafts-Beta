package dev.itzgamesgd.moreminecrafts.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import dev.itzgamesgd.moreminecrafts.furnace.FurnaceResearchAttribution;
import dev.itzgamesgd.moreminecrafts.furnace.FurnaceResearchState;
import dev.itzgamesgd.moreminecrafts.furnace.FurnaceResearchStateAccess;
import dev.itzgamesgd.moreminecrafts.furnace.FurnaceResearchRuntime;
import dev.itzgamesgd.moreminecrafts.progression.SmeltingOutputProvenance;
import net.minecraft.core.NonNullList;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.AbstractFurnaceBlockEntity;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Shadow;

/** Records the exact input/output pair after vanilla has accepted a completed cooking result. */
@Mixin(AbstractFurnaceBlockEntity.class)
abstract class KnowledgeFurnaceMixin implements FurnaceResearchStateAccess {
    @Shadow private int cookingTimer;
    @Shadow private int cookingTotalTime;
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

    @Inject(method = "setItem", at = @At("HEAD"))
    private void moreminecrafts$discardReplacedOutput(int slot, ItemStack stack, CallbackInfo ci) {
        if (slot == 2)
            SmeltingOutputProvenance.discard((AbstractFurnaceBlockEntity) (Object) this);
    }

    @Inject(method = "saveAdditional", at = @At("TAIL"))
    private void moreminecrafts$saveSmeltProvenance(ValueOutput output, CallbackInfo ci) {
        AbstractFurnaceBlockEntity furnace = (AbstractFurnaceBlockEntity) (Object) this;
        if (!moreminecrafts$researchState.equals(FurnaceResearchState.empty())) {
            output.putInt("MoreMinecraftsFurnaceResearchVersion", 2);
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
        int version = input.getIntOr("MoreMinecraftsFurnaceResearchVersion", 0);
        if (version == 1 || version == 2)
            input.read("MoreMinecraftsFurnaceResearchState", FurnaceResearchState.CODEC)
                    .ifPresent(state -> moreminecrafts$researchState = state);
        if (input.getIntOr("MoreMinecraftsSmeltProvenanceVersion", 0) == SmeltingOutputProvenance.DATA_VERSION)
            input.read("MoreMinecraftsSmeltProvenance", SmeltingOutputProvenance.CODEC)
                    .ifPresent(pending -> SmeltingOutputProvenance.load(furnace, pending));
    }

    @WrapOperation(method = "serverTick",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/block/entity/AbstractFurnaceBlockEntity;getBurnDuration(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/item/ItemStack;)I"))
    private static int moreminecrafts$saveFuel(AbstractFurnaceBlockEntity receiver, ServerLevel level, ItemStack fuel,
            Operation<Integer> original,
            @com.llamalad7.mixinextras.sugar.Local(argsOnly = true) AbstractFurnaceBlockEntity furnace) {
        int base = original.call(receiver, level, fuel);
        return FurnaceResearchRuntime.fuelDuration((ServerLevel) furnace.getLevel(), furnace, base);
    }

    @Inject(method = "serverTick", at = @At(value = "FIELD",
            target = "Lnet/minecraft/world/level/block/entity/AbstractFurnaceBlockEntity;cookingTimer:I",
            opcode = Opcodes.PUTFIELD, ordinal = 1, shift = At.Shift.AFTER))
    private static void moreminecrafts$accelerateCooking(ServerLevel level, BlockPos pos, BlockState blockState,
            AbstractFurnaceBlockEntity furnace, CallbackInfo ci) {
        KnowledgeFurnaceMixin self = (KnowledgeFurnaceMixin) (Object) furnace;
        self.cookingTimer = FurnaceResearchRuntime.cookingTimer(level, furnace,
                self.cookingTimer, self.cookingTotalTime);
    }

    @WrapOperation(method = "serverTick",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/block/entity/AbstractFurnaceBlockEntity;canBurn(Lnet/minecraft/core/NonNullList;ILnet/minecraft/world/item/ItemStack;)Z"))
    private static boolean moreminecrafts$reserveBonusSpace(NonNullList<ItemStack> items, int maxStack,
            ItemStack result, Operation<Boolean> original,
            @com.llamalad7.mixinextras.sugar.Local(argsOnly = true) AbstractFurnaceBlockEntity furnace) {
        return original.call(items, maxStack, result)
                && FurnaceResearchRuntime.canAcceptPossibleBonus((ServerLevel) furnace.getLevel(),
                        furnace, items, result, maxStack);
    }

    @WrapOperation(method = "serverTick",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/block/entity/AbstractFurnaceBlockEntity;burn(Lnet/minecraft/core/NonNullList;Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/item/ItemStack;)V"))
    private static void moreminecrafts$rememberCompletedOutput(NonNullList<ItemStack> items, ItemStack input,
            ItemStack result, Operation<Void> original,
            @com.llamalad7.mixinextras.sugar.Local(argsOnly = true) AbstractFurnaceBlockEntity furnace) {
        ItemStack consumedInput = input.copy();
        ItemStack producedOutput = FurnaceResearchRuntime.completedOutput(
                (ServerLevel) furnace.getLevel(), furnace, input, result);
        original.call(items, input, producedOutput);
        SmeltingOutputProvenance.record(furnace, consumedInput, producedOutput);
    }
}
