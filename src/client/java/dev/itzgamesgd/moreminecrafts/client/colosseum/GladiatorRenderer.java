package dev.itzgamesgd.moreminecrafts.client.colosseum;

import dev.itzgamesgd.moreminecrafts.colosseum.ImmortalGladiator;
import dev.itzgamesgd.moreminecrafts.core.ModIds;
import net.fabricmc.fabric.api.client.rendering.v1.*;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.*;
import net.minecraft.client.model.geom.builders.*;
import net.minecraft.client.renderer.entity.*;
import net.minecraft.client.renderer.entity.state.HumanoidRenderState;
import net.minecraft.resources.Identifier;

/** Dedicated 1.9x armored gladiator: bronze helmet with crimson crest, cuirass, pauldrons, bracers, greaves, dedicated texture; sword and shield scale with the body; no new animation dependency. */
public final class GladiatorRenderer extends HumanoidMobRenderer<ImmortalGladiator,GladiatorRenderer.State,HumanoidModel<GladiatorRenderer.State>> {
    private static final ModelLayerLocation LAYER=new ModelLayerLocation(ModIds.id("immortal_gladiator"),"main");
    public static final class State extends HumanoidRenderState {int pose;boolean phaseTwo,shieldActive;}
    private static final float SCALE=1.9F;
    public GladiatorRenderer(EntityRendererProvider.Context ctx) {super(ctx,new Model(ctx.bakeLayer(LAYER)),.8F);}
    public static void register() {
        ModelLayerRegistry.registerModelLayer(LAYER,()->{
            MeshDefinition mesh=HumanoidModel.createMesh(new CubeDeformation(.25F),0);
            var root=mesh.getRoot();
            // Extra armor parts are children of the animated limbs, so every pose/animation (and all combat code) is untouched. UVs match tools/st05-gladiator/generate_gladiator_texture.py.
            root.getChild("head").addOrReplaceChild("crest",CubeListBuilder.create().texOffs(0,32).addBox(-1,-12,-4,2,5,8),PartPose.ZERO);
            root.getChild("body").addOrReplaceChild("breastplate",CubeListBuilder.create().texOffs(20,32).addBox(-4.5F,0,-2.5F,9,10,5),PartPose.ZERO);
            root.getChild("right_arm").addOrReplaceChild("pauldron",CubeListBuilder.create().texOffs(48,32).addBox(-3.5F,-2.5F,-2.5F,4,3,4,new CubeDeformation(.75F)),PartPose.ZERO);
            root.getChild("left_arm").addOrReplaceChild("pauldron",CubeListBuilder.create().texOffs(48,32).addBox(-.5F,-2.5F,-2.5F,4,3,4,new CubeDeformation(.75F)),PartPose.ZERO);
            root.getChild("right_arm").addOrReplaceChild("bracer",CubeListBuilder.create().texOffs(48,41).addBox(-3F,4.5F,-2F,4,5,4,new CubeDeformation(.55F)),PartPose.ZERO);
            root.getChild("left_arm").addOrReplaceChild("bracer",CubeListBuilder.create().texOffs(48,41).addBox(-1F,4.5F,-2F,4,5,4,new CubeDeformation(.55F)),PartPose.ZERO);
            root.getChild("right_leg").addOrReplaceChild("greave",CubeListBuilder.create().texOffs(48,50).addBox(-2F,6.5F,-2F,4,5,4,new CubeDeformation(.5F)),PartPose.ZERO);
            root.getChild("left_leg").addOrReplaceChild("greave",CubeListBuilder.create().texOffs(48,50).addBox(-2F,6.5F,-2F,4,5,4,new CubeDeformation(.5F)),PartPose.ZERO);
            return LayerDefinition.create(mesh,64,64);
        });
        EntityRendererRegistry.register(ImmortalGladiator.entityType(),GladiatorRenderer::new);
    }
    /** Scaling the whole render (not the mesh) makes the held sword and shield grow with the 1.9x body instead of staying vanilla-sized. */
    @Override protected void scale(State s,com.mojang.blaze3d.vertex.PoseStack stack) {stack.scale(SCALE,SCALE,SCALE);}
    @Override public State createRenderState() {return new State();}
    @Override public void extractRenderState(ImmortalGladiator b,State s,float t) {super.extractRenderState(b,s,t);s.pose=b.attackPose();s.phaseTwo=b.phaseTwo();s.shieldActive=b.shieldActive();}
    @Override public Identifier getTextureLocation(State s) {return ModIds.id("textures/entity/immortal_gladiator.png");}
    private static final class Model extends HumanoidModel<State> {
        Model(ModelPart p) {super(p);}
        @Override public void setupAnim(State s) {
            super.setupAnim(s);
            if(s.pose!=0) {rightArm.xRot=-2.1F;rightArm.zRot=-.3F;body.yRot=.15F;}
            if(s.shieldActive) {leftArm.xRot=-1.0F;leftArm.yRot=.4F;}
        }
    }
}
