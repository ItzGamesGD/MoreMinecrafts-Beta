package dev.itzgamesgd.moreminecrafts.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import dev.itzgamesgd.moreminecrafts.progression.BlockEntityResearchAttribution;
import dev.itzgamesgd.moreminecrafts.progression.BrewingOutputProvenance;
import dev.itzgamesgd.moreminecrafts.progression.BrewingResearchRuntime;
import dev.itzgamesgd.moreminecrafts.progression.BrewingResearchStateAccess;
import net.minecraft.core.BlockPos;
import net.minecraft.core.NonNullList;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BrewingStandBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Captures output provenance and applies `.59`/`.60`/`.66`/`.69` brewing research effects. */
@Mixin(BrewingStandBlockEntity.class)
abstract class KnowledgeBrewingMixin implements BrewingResearchStateAccess {
    @Shadow private int brewTime;
    @Shadow private int fuel;

    @Unique
    private BlockEntityResearchAttribution.State moreminecrafts$researchAttribution =
            BlockEntityResearchAttribution.State.empty();
    @Unique
    private double moreminecrafts$brewProgressRemainder;
    @Unique
    private int moreminecrafts$fuelBeforeTick;
    @Unique
    private int moreminecrafts$powderBeforeTick;
    @Unique
    private boolean moreminecrafts$autoProcessEnabled;
    @Unique
    private NonNullList<ItemStack> moreminecrafts$inputQueue;
    @Unique
    private NonNullList<ItemStack> moreminecrafts$outputBuffer;

    @Override
    public BlockEntityResearchAttribution.State moreminecrafts$getResearchAttribution() {
        if (moreminecrafts$researchAttribution == null) {
            moreminecrafts$researchAttribution = BlockEntityResearchAttribution.State.empty();
        }
        return moreminecrafts$researchAttribution;
    }

    @Override
    public void moreminecrafts$setResearchAttribution(BlockEntityResearchAttribution.State state) {
        moreminecrafts$researchAttribution = state == null ? BlockEntityResearchAttribution.State.empty() : state;
        ((BrewingStandBlockEntity) (Object) this).setChanged();
    }

    @Override
    public double moreminecrafts$getBrewProgressRemainder() {
        return moreminecrafts$brewProgressRemainder;
    }

    @Override
    public void moreminecrafts$setBrewProgressRemainder(double remainder) {
        moreminecrafts$brewProgressRemainder = Double.isFinite(remainder) && remainder >= 0.0D && remainder < 1.0D
                ? remainder : 0.0D;
    }

    @Override
    public boolean moreminecrafts$isAutoProcessEnabled() {
        return moreminecrafts$autoProcessEnabled;
    }

    @Override
    public void moreminecrafts$setAutoProcessEnabled(boolean enabled) {
        moreminecrafts$autoProcessEnabled = enabled;
        ((BrewingStandBlockEntity) (Object) this).setChanged();
    }

    @Override
    public NonNullList<ItemStack> moreminecrafts$getInputQueue() {
        if (moreminecrafts$inputQueue == null) {
            moreminecrafts$inputQueue = NonNullList.withSize(AUTO_QUEUE_SIZE, ItemStack.EMPTY);
        }
        return moreminecrafts$inputQueue;
    }

    @Override
    public NonNullList<ItemStack> moreminecrafts$getOutputBuffer() {
        if (moreminecrafts$outputBuffer == null) {
            moreminecrafts$outputBuffer = NonNullList.withSize(AUTO_QUEUE_SIZE, ItemStack.EMPTY);
        }
        return moreminecrafts$outputBuffer;
    }

    @Inject(method = "serverTick", at = @At("HEAD"))
    private static void moreminecrafts$reconcileAndAccelerateBrew(ServerLevel level, BlockPos pos, BlockState state,
            BrewingStandBlockEntity stand, CallbackInfo ci) {
        BrewingOutputProvenance.reconcile(stand);
        KnowledgeBrewingMixin self = (KnowledgeBrewingMixin) (Object) stand;
        self.moreminecrafts$fuelBeforeTick = self.fuel;
        self.moreminecrafts$powderBeforeTick = stand.getItem(4).getCount();
        self.brewTime = BrewingResearchRuntime.accelerateBrewTime(level, stand, self.brewTime);
    }

    @Inject(method = "serverTick", at = @At("RETURN"))
    private static void moreminecrafts$applyAdvancedBrewingFuel(ServerLevel level, BlockPos pos, BlockState state,
            BrewingStandBlockEntity stand, CallbackInfo ci) {
        KnowledgeBrewingMixin self = (KnowledgeBrewingMixin) (Object) stand;
        if (self.moreminecrafts$fuelBeforeTick <= 0 && (self.fuel == 20 || self.fuel == 19)
                && stand.getItem(4).getCount() < self.moreminecrafts$powderBeforeTick) {
            int updatedFuel = BrewingResearchRuntime.fuelUsesPerBlazePowder(level, stand);
            int consumedOnStart = 20 - self.fuel;
            int targetFuel = Math.max(0, updatedFuel - consumedOnStart);
            if (targetFuel != self.fuel) {
                self.fuel = targetFuel;
                stand.setChanged();
            }
        }
        BrewingResearchRuntime.tickAutoProcess(level, stand, self.brewTime);
    }

    @Inject(method = "saveAdditional", at = @At("TAIL"))
    private void moreminecrafts$saveBrewProvenance(ValueOutput output, CallbackInfo ci) {
        BrewingStandBlockEntity stand = (BrewingStandBlockEntity) (Object) this;
        BlockEntityResearchAttribution.save(output, "MoreMinecraftsBrewResearchAttribution",
                moreminecrafts$getResearchAttribution());
        if (moreminecrafts$brewProgressRemainder > 0.0D) {
            output.putDouble("MoreMinecraftsBrewProgressRemainder", moreminecrafts$brewProgressRemainder);
        }
        if (moreminecrafts$autoProcessEnabled) {
            output.putBoolean("MoreMinecraftsBrewAutoProcess", true);
        }
        var inputQueue = moreminecrafts$getInputQueue();
        if (inputQueue.stream().anyMatch(s -> !s.isEmpty())) {
            output.store("MoreMinecraftsBrewInputQueue", ItemStack.OPTIONAL_CODEC.listOf(),
                    java.util.List.copyOf(inputQueue));
        }
        var outputBuffer = moreminecrafts$getOutputBuffer();
        if (outputBuffer.stream().anyMatch(s -> !s.isEmpty())) {
            output.store("MoreMinecraftsBrewOutputBuffer", ItemStack.OPTIONAL_CODEC.listOf(),
                    java.util.List.copyOf(outputBuffer));
        }
        var pending = BrewingOutputProvenance.save(stand);
        if (!pending.isEmpty()) {
            output.putInt("MoreMinecraftsBrewProvenanceVersion", BrewingOutputProvenance.DATA_VERSION);
            output.store("MoreMinecraftsBrewProvenance", BrewingOutputProvenance.CODEC, pending);
        }
    }

    @Inject(method = "loadAdditional", at = @At("TAIL"))
    private void moreminecrafts$loadBrewProvenance(ValueInput input, CallbackInfo ci) {
        BrewingStandBlockEntity stand = (BrewingStandBlockEntity) (Object) this;
        moreminecrafts$researchAttribution = BlockEntityResearchAttribution.load(
                input, "MoreMinecraftsBrewResearchAttribution");
        double loadedRemainder = input.getDoubleOr("MoreMinecraftsBrewProgressRemainder", 0.0D);
        moreminecrafts$brewProgressRemainder = Double.isFinite(loadedRemainder)
                && loadedRemainder >= 0.0D && loadedRemainder < 1.0D ? loadedRemainder : 0.0D;
        moreminecrafts$autoProcessEnabled = input.getBooleanOr("MoreMinecraftsBrewAutoProcess", false);
        var inputQueue = moreminecrafts$getInputQueue();
        inputQueue.clear();
        input.read("MoreMinecraftsBrewInputQueue", ItemStack.OPTIONAL_CODEC.listOf()).ifPresent(list -> {
            for (int i = 0; i < Math.min(AUTO_QUEUE_SIZE, list.size()); i++) {
                inputQueue.set(i, list.get(i).copy());
            }
        });
        var outputBuffer = moreminecrafts$getOutputBuffer();
        outputBuffer.clear();
        input.read("MoreMinecraftsBrewOutputBuffer", ItemStack.OPTIONAL_CODEC.listOf()).ifPresent(list -> {
            for (int i = 0; i < Math.min(AUTO_QUEUE_SIZE, list.size()); i++) {
                outputBuffer.set(i, list.get(i).copy());
            }
        });
        if (input.getIntOr("MoreMinecraftsBrewProvenanceVersion", 0) == BrewingOutputProvenance.DATA_VERSION)
            input.read("MoreMinecraftsBrewProvenance", BrewingOutputProvenance.CODEC)
                    .ifPresent(pending -> BrewingOutputProvenance.load(stand, pending));
    }

    @WrapOperation(method = "serverTick", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/level/block/entity/BrewingStandBlockEntity;doBrew(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/entity/BrewingStandBlockEntity;)V"))
    private static void moreminecrafts$recordSuccessfulBrew(ServerLevel level, BlockPos pos,
            BrewingStandBlockEntity stand, Operation<Void> original) {
        ItemStack[] before = new ItemStack[3];
        for (int slot = 0; slot < 3; slot++) before[slot] = stand.getItem(slot).copy();
        ItemStack ingredientBefore = stand.getItem(3).copy();
        original.call(level, pos, stand);
        BrewingResearchRuntime.adjustBrewedPotions(level, stand, ingredientBefore, before);
        BrewingResearchRuntime.tryPreserveCatalyst(level, stand, ingredientBefore);
        ItemStack[] after = new ItemStack[3];
        for (int slot = 0; slot < 3; slot++) after[slot] = stand.getItem(slot).copy();
        BrewingOutputProvenance.completed(level, pos, stand, before, after);
    }
}
