package dev.itzgamesgd.moreminecrafts.boss;

import com.mojang.math.Transformation;
import dev.itzgamesgd.moreminecrafts.core.ModIds;
import dev.itzgamesgd.moreminecrafts.mixin.DisplayAccessor;
import net.minecraft.core.component.DataComponents;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Brightness;
import net.minecraft.world.entity.Display;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionf;
import org.joml.Vector3f;

/**
 * Temporary, never-saved item display used for the Stone Eruption telegraph (magic circle) and the risen
 * inscribed statue. Purely visual: it has no collision, edits no blocks and removes itself after its lifetime,
 * so a chunk save or restart can never leave a stale telegraph in the world.
 */
public final class TombVfxDisplay extends Display.ItemDisplay {
    private static final int RISE_TICKS = 5;
    private static final float STATUE_SCALE = (float) (TombGuardianTuning.STATUE_HEIGHT / 2.0D);
    private static final float CIRCLE_SCALE = (float) (TombGuardianTuning.STATUE_RADIUS * 2.0D);

    private boolean statue;
    private int lifetime = 1;
    private int age;

    public TombVfxDisplay(EntityType<?> type, Level level) {
        super(type, level);
    }

    static TombVfxDisplay circle(ServerLevel level, Vec3 point, int lifetime) {
        TombVfxDisplay display = create(level, point, "tomb_magic_circle", lifetime, false);
        if (display == null) return null;
        display.accessor().moreminecrafts$setBrightnessOverride(Brightness.FULL_BRIGHT);
        display.accessor().moreminecrafts$setTransformation(display.circleTransform(0.0F));
        level.addFreshEntity(display);
        return display;
    }

    static TombVfxDisplay statue(ServerLevel level, Vec3 point, int lifetime) {
        TombVfxDisplay display = create(level, point, "tomb_statue", lifetime, true);
        if (display == null) return null;
        display.accessor().moreminecrafts$setTransformation(statueTransform(true));
        level.addFreshEntity(display);
        return display;
    }

    private static TombVfxDisplay create(ServerLevel level, Vec3 point, String model, int lifetime, boolean statue) {
        TombVfxDisplay display = TombGuardianEntities.TOMB_VFX.create(level, EntitySpawnReason.TRIGGERED);
        if (display == null) return null;
        display.statue = statue;
        display.lifetime = Math.max(1, lifetime);
        display.setPos(point.x, point.y, point.z);
        ItemStack stack = new ItemStack(Items.PAPER);
        stack.set(DataComponents.ITEM_MODEL, ModIds.id(model));
        display.getSlot(0).set(stack);
        display.accessor().moreminecrafts$setViewRange(2.0F);
        return display;
    }

    private DisplayAccessor accessor() { return (DisplayAccessor) (Object) this; }

    public boolean isStatue() { return statue; }

    public int lifetime() { return lifetime; }

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide()) return;
        age++;
        if (age >= lifetime) {
            discard();
            return;
        }
        if (statue) {
            // Rise out of the floor right after spawning and sink back just before removal.
            if (age == 1 || age == lifetime - RISE_TICKS) {
                accessor().moreminecrafts$setTransformationInterpolationDelay(0);
                accessor().moreminecrafts$setTransformationInterpolationDuration(RISE_TICKS);
                accessor().moreminecrafts$setTransformation(statueTransform(age != 1));
            }
        } else if (age % 5 == 0) {
            // Slow spin keeps the glyph ring readable as a moving magic circle, not a static floor decal.
            accessor().moreminecrafts$setTransformationInterpolationDelay(0);
            accessor().moreminecrafts$setTransformationInterpolationDuration(5);
            accessor().moreminecrafts$setTransformation(circleTransform(age * 0.06F));
        }
    }

    private Transformation circleTransform(float angle) {
        // The circle model is a flat 16x16 plate; item rendering centres the model on the entity.
        return new Transformation(new Vector3f(0.0F, CIRCLE_SCALE * 0.5F + 0.03F, 0.0F),
                new Quaternionf().rotationY(angle), new Vector3f(CIRCLE_SCALE, CIRCLE_SCALE, CIRCLE_SCALE), null);
    }

    private static Transformation statueTransform(boolean buried) {
        // The statue model spans 32 units (two blocks) upward; scaled so its base sits on the floor when risen.
        float base = STATUE_SCALE * 0.5F;
        float y = buried ? base - (float) TombGuardianTuning.STATUE_HEIGHT - 0.2F : base;
        return new Transformation(new Vector3f(0.0F, y, 0.0F), null,
                new Vector3f(STATUE_SCALE, STATUE_SCALE, STATUE_SCALE), null);
    }

    @Override
    public boolean shouldBeSaved() { return false; }
}
