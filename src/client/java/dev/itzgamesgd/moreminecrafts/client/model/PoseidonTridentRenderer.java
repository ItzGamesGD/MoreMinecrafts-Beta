package dev.itzgamesgd.moreminecrafts.client.model;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import dev.itzgamesgd.moreminecrafts.boss.PoseidonTrident;
import dev.itzgamesgd.moreminecrafts.core.ModIds;
import java.util.Map;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;
import net.minecraft.world.phys.Vec3;

/**
 * Draws the thrown Poseidon trident with the trident mesh taken out of the boss model itself, aligned to the
 * projectile's direction of travel (prongs first going out, hand-end first coming back) and rolling about its shaft.
 * Rendered full-bright: the document calls it a glowing trident.
 */
public final class PoseidonTridentRenderer extends EntityRenderer<PoseidonTrident, PoseidonTridentRenderer.State> {
    private static final Identifier TEXTURE = ModIds.id("textures/entity/deep_sea_poseidon.png");
    /** Distance in pixels from the shaft pivot (butt) to the grip, so the entity position is the grip. */
    private static final float GRIP = 11.0F;
    private static final int FULL_BRIGHT = 0xF000F0;

    public static final class State extends EntityRenderState {
        Vec3 direction = new Vec3(0, 0, 1);
        boolean returning;
    }

    private final EntityModel<State> model;

    public PoseidonTridentRenderer(EntityRendererProvider.Context ctx) {
        super(ctx);
        BoneModelLoader.Loaded loaded = BoneModelLoader.load(ctx.getResourceManager(), ModIds.id("entity_models/deep_sea_poseidon.json"));
        ModelPart shaft = loaded.require("trident_shaft_bone");
        shaft.setInitialPose(PartPose.ZERO);
        shaft.resetPose();
        this.model = new EntityModel<>(new ModelPart(java.util.List.of(), Map.of("trident", shaft))) {};
    }

    @Override
    public State createRenderState() {
        return new State();
    }

    @Override
    public void extractRenderState(PoseidonTrident entity, State state, float partialTick) {
        super.extractRenderState(entity, state, partialTick);
        state.direction = entity.clientDirection;
        state.returning = entity.returning();
    }

    @Override
    public void submit(State state, PoseStack pose, SubmitNodeCollector collector, CameraRenderState camera) {
        Vec3 d = state.returning ? state.direction.scale(-1) : state.direction; // prongs lead out, trail back
        double horizontal = Math.sqrt(d.x * d.x + d.z * d.z);
        float yaw = (float) Math.toDegrees(Math.atan2(d.x, d.z));
        float tilt = (float) Math.toDegrees(Math.atan2(horizontal, d.y)); // 0 = prongs up, 90 = level
        pose.pushPose();
        pose.rotateDegrees(Axis.YP, yaw);
        pose.rotateDegrees(Axis.XP, tilt);
        pose.rotateDegrees(Axis.YP, state.ageInTicks * 40.0F); // roll about the shaft
        pose.translate(0.0F, -GRIP / 16.0F, 0.0F);
        pose.scale(-1.0F, -1.0F, 1.0F);
        collector.submitModel(model, state, pose, RenderTypes.entityCutout(TEXTURE), FULL_BRIGHT, OverlayTexture.NO_OVERLAY, 0);
        pose.popPose();
        super.submit(state, pose, collector, camera);
    }
}
