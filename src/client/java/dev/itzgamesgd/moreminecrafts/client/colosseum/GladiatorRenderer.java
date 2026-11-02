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

/** Dedicated 1.9x armored humanoid with a crested helmet, sword and shield; no new animation dependency. */
public final class GladiatorRenderer extends HumanoidMobRenderer<ImmortalGladiator,GladiatorRenderer.State,HumanoidModel<GladiatorRenderer.State>> {
    private static final ModelLayerLocation LAYER=new ModelLayerLocation(ModIds.id("immortal_gladiator"),"main");
    public static final class State extends HumanoidRenderState {int pose;boolean phaseTwo,shieldActive;}
    public GladiatorRenderer(EntityRendererProvider.Context ctx) {super(ctx,new Model(ctx.bakeLayer(LAYER)),.8F);}
    public static void register() {
        ModelLayerRegistry.registerModelLayer(LAYER,()->{
            MeshDefinition mesh=HumanoidModel.createMesh(new CubeDeformation(.25F),0);
            var root=mesh.getRoot();
            root.getChild("head").addOrReplaceChild("crest",CubeListBuilder.create().texOffs(0,0).addBox(-1,-12,-4,2,5,8),PartPose.ZERO);
            root.getChild("body").addOrReplaceChild("breastplate",CubeListBuilder.create().texOffs(16,16).addBox(-4.5F,0,-2.5F,9,10,5),PartPose.ZERO);
            root.getChild("right_arm").addOrReplaceChild("pauldron",CubeListBuilder.create().texOffs(40,16).addBox(-3.5F,-2.5F,-2.5F,5,4,5),PartPose.ZERO);
            root.getChild("left_arm").addOrReplaceChild("pauldron",CubeListBuilder.create().texOffs(40,16).addBox(-1.5F,-2.5F,-2.5F,5,4,5),PartPose.ZERO);
            return LayerDefinition.create(mesh,64,64).apply(MeshTransformer.scaling(1.9F));
        });
        EntityRendererRegistry.register(ImmortalGladiator.entityType(),GladiatorRenderer::new);
    }
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
