package dev.itzgamesgd.moreminecrafts.mixin;

import dev.itzgamesgd.moreminecrafts.progression.BlockEntityResearchAttribution;
import dev.itzgamesgd.moreminecrafts.progression.HopperFilterAccess;
import dev.itzgamesgd.moreminecrafts.progression.HopperFilterState;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Container;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.HopperBlockEntity;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Persists `hopper_filter` rules/attribution and gates hopper item intake. */
@Mixin(HopperBlockEntity.class)
abstract class ResearchHopperFilterMixin implements HopperFilterAccess {
    @Unique
    private HopperFilterState moreminecrafts$filterState = HopperFilterState.emptyWhitelist();
    @Unique
    private BlockEntityResearchAttribution.State moreminecrafts$researchAttribution =
            BlockEntityResearchAttribution.State.empty();

    @Override
    public HopperFilterState moreminecrafts$getFilterState() {
        return moreminecrafts$filterState;
    }

    @Override
    public void moreminecrafts$setFilterState(HopperFilterState state) {
        moreminecrafts$filterState = state == null ? HopperFilterState.emptyWhitelist() : state;
        ((HopperBlockEntity) (Object) this).setChanged();
    }

    @Override
    public BlockEntityResearchAttribution.State moreminecrafts$getResearchAttribution() {
        return moreminecrafts$researchAttribution;
    }

    @Override
    public void moreminecrafts$setResearchAttribution(BlockEntityResearchAttribution.State state) {
        moreminecrafts$researchAttribution = state == null ? BlockEntityResearchAttribution.State.empty() : state;
        ((HopperBlockEntity) (Object) this).setChanged();
    }

    @Override
    public boolean moreminecrafts$isFilterActive(ServerLevel level) {
        if (level == null || level.getServer() == null) return false;
        if (moreminecrafts$filterState.entries().isEmpty() && moreminecrafts$filterState.whitelist()) {
            return false; // Unconfigured default hopper behaves like vanilla until entries or blacklist mode is set.
        }
        return BlockEntityResearchAttribution.resolveLevel(
                level.getServer(), moreminecrafts$researchAttribution, "hopper_filter") > 0;
    }

    @Inject(method = "saveAdditional", at = @At("TAIL"))
    private void moreminecrafts$saveHopperFilter(ValueOutput output, CallbackInfo ci) {
        BlockEntityResearchAttribution.save(output, "MoreMinecraftsHopperAttribution",
                moreminecrafts$researchAttribution);
        output.store("MoreMinecraftsHopperFilter", HopperFilterState.CODEC, moreminecrafts$filterState);
    }

    @Inject(method = "loadAdditional", at = @At("TAIL"))
    private void moreminecrafts$loadHopperFilter(ValueInput input, CallbackInfo ci) {
        moreminecrafts$researchAttribution = BlockEntityResearchAttribution.load(
                input, "MoreMinecraftsHopperAttribution");
        moreminecrafts$filterState = input.read("MoreMinecraftsHopperFilter", HopperFilterState.CODEC)
                .orElse(HopperFilterState.emptyWhitelist());
    }

    @Inject(method = "addItem(Lnet/minecraft/world/Container;Lnet/minecraft/world/Container;Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/core/Direction;)Lnet/minecraft/world/item/ItemStack;",
            at = @At("HEAD"), cancellable = true)
    private static void moreminecrafts$filterContainerIntake(Container source, Container destination, ItemStack stack,
                                                             Direction direction, CallbackInfoReturnable<ItemStack> cir) {
        if (destination instanceof HopperBlockEntity hopper
                && hopper instanceof HopperFilterAccess access
                && hopper.getLevel() instanceof ServerLevel level
                && access.moreminecrafts$isFilterActive(level)
                && !access.moreminecrafts$getFilterState().allows(stack)) {
            cir.setReturnValue(stack);
        }
    }

    @Inject(method = "addItem(Lnet/minecraft/world/Container;Lnet/minecraft/world/entity/item/ItemEntity;)Z",
            at = @At("HEAD"), cancellable = true)
    private static void moreminecrafts$filterEntityIntake(Container destination, ItemEntity entity,
                                                          CallbackInfoReturnable<Boolean> cir) {
        if (destination instanceof HopperBlockEntity hopper
                && hopper instanceof HopperFilterAccess access
                && hopper.getLevel() instanceof ServerLevel level
                && access.moreminecrafts$isFilterActive(level)
                && !access.moreminecrafts$getFilterState().allows(entity.getItem())) {
            cir.setReturnValue(false);
        }
    }
}
