package dev.itzgamesgd.moreminecrafts.alchemy;

import dev.itzgamesgd.moreminecrafts.core.ModIds;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.item.alchemy.Potion;

public final class ModAlchemy {
    public static final Holder.Reference<MobEffect> SHOCK_ROOT = Registry.registerForHolder(
            BuiltInRegistries.MOB_EFFECT, ModIds.id("shock_root"), new ShockRootMobEffect());
    public static final Holder.Reference<MobEffect> SHOCK = Registry.registerForHolder(
            BuiltInRegistries.MOB_EFFECT, ModIds.id("shock"), new ShockMobEffect());
    public static final Holder.Reference<Potion> SHOCK_POTION = Registry.registerForHolder(
            BuiltInRegistries.POTION,
            ModIds.id("shock"),
            new Potion("shock", new MobEffectInstance(SHOCK, ShockTiming.DURATION_TICKS)));

    private ModAlchemy() {}

    public static void initialize() {
        // Class loading performs the three vanilla registry registrations.
    }
}
