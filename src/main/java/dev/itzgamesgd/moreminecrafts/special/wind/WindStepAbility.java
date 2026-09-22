package dev.itzgamesgd.moreminecrafts.special.wind;

import dev.itzgamesgd.moreminecrafts.ability.ActiveEquipmentAbility;
import dev.itzgamesgd.moreminecrafts.core.ModIds;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.Identifier;
import net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

public final class WindStepAbility implements ActiveEquipmentAbility {
    public static final WindStepAbility INSTANCE = new WindStepAbility();
    public static final Identifier ID = ModIds.id("wind_step");
    public static final int COOLDOWN_TICKS = 80;
    public static final double IMPULSE = 1.25D;

    private WindStepAbility() {}
    @Override public Identifier id() { return ID; }
    @Override public int cooldownTicks() { return COOLDOWN_TICKS; }

    @Override
    public boolean canActivate(ServerPlayer player, ItemStack source) {
        Vec3 look = player.getLookAngle();
        return !player.isPassenger() && look.x * look.x + look.z * look.z > 1.0E-12D;
    }

    @Override
    public boolean activate(ServerPlayer player, ItemStack source) {
        Vec3 look = player.getLookAngle();
        double[] step = WindCombatMath.horizontalStep(look.x, look.z, IMPULSE);
        if (step[0] == 0.0D && step[1] == 0.0D) return false;
        Vec3 oldVelocity = player.getDeltaMovement();
        player.setDeltaMovement(step[0], oldVelocity.y, step[1]);
        player.connection.send(new ClientboundSetEntityMotionPacket(player));
        ServerLevel level = player.level();
        level.sendParticles(ParticleTypes.CLOUD, player.getX(), player.getY() + 0.3D, player.getZ(),
                12, 0.35D, 0.15D, 0.35D, 0.03D);
        level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.PLAYER_ATTACK_SWEEP,
                SoundSource.PLAYERS, 0.75F, 1.65F);
        return true;
    }
}
