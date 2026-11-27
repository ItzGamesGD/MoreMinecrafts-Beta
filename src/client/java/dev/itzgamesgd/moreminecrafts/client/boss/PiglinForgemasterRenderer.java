package dev.itzgamesgd.moreminecrafts.client.boss;

import dev.itzgamesgd.moreminecrafts.boss.PiglinForgemaster;
import dev.itzgamesgd.moreminecrafts.boss.PiglinForgemasterEntities;
import dev.itzgamesgd.moreminecrafts.client.model.BoneAnimations;
import dev.itzgamesgd.moreminecrafts.client.model.BoneModelLoader;
import dev.itzgamesgd.moreminecrafts.core.ModIds;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import net.minecraft.client.animation.KeyframeAnimation;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.AnimationState;

/**
 * Lava Forgemaster renderer. The mesh and every animation clip come from
 * {@code assets/moreminecrafts/entity_models/piglin_forgemaster.json}, converted from
 * {@code tools/bbmodel/source/piglin_forgemaster.bbmodel} by {@code convert_bbmodel.py --animations}.
 */
public final class PiglinForgemasterRenderer
        extends MobRenderer<PiglinForgemaster, PiglinForgemasterRenderer.State, PiglinForgemasterRenderer.Model> {
    public static final Identifier MODEL = ModIds.id("entity_models/piglin_forgemaster.json");
    private static final Identifier TEXTURE = ModIds.id("textures/entity/piglin_forgemaster.png");

    public static final class State extends LivingEntityRenderState {
        final AnimationState[] pose = new AnimationState[PiglinForgemaster.Skill.values().length];
        boolean phaseTwo;
        boolean dying;

        State() {
            for (int i = 0; i < pose.length; i++) pose[i] = new AnimationState();
        }
    }

    public PiglinForgemasterRenderer(EntityRendererProvider.Context ctx) {
        super(ctx, new Model(BoneModelLoader.load(ctx.getResourceManager(), MODEL), BoneAnimations.load(ctx.getResourceManager(), MODEL)), 1.4F);
    }

    public static void register() {
        EntityRendererRegistry.register(PiglinForgemasterEntities.PIGLIN_FORGEMASTER, PiglinForgemasterRenderer::new);
    }

    @Override
    public State createRenderState() {
        return new State();
    }

    @Override
    public void extractRenderState(PiglinForgemaster boss, State state, float partialTick) {
        super.extractRenderState(boss, state, partialTick);
        state.phaseTwo = boss.phaseTwo();
        state.dying = boss.isDeadOrDying();
        for (int i = 0; i < state.pose.length; i++) state.pose[i].copyFrom(boss.poseStates[i]);
    }

    @Override
    public Identifier getTextureLocation(State state) {
        return TEXTURE;
    }

    /** The death clip lays the boss down itself, so the vanilla 90 degree corpse flip is disabled. */
    @Override
    protected float getFlipDegrees() {
        return 0.0F;
    }

    public static final class Model extends EntityModel<State> {
        private final ModelPart head;
        private final KeyframeAnimation idle;
        private final KeyframeAnimation walk;
        private final KeyframeAnimation death;
        private final KeyframeAnimation[] skills = new KeyframeAnimation[PiglinForgemaster.Skill.values().length];

        Model(BoneModelLoader.Loaded loaded, BoneAnimations anims) {
            super(loaded.root());
            this.head = loaded.require("head_bone");
            ModelPart root = loaded.root();
            this.idle = anims.bake("idle", root);
            this.walk = anims.bake("walk", root);
            this.death = anims.bake("death", root);
            for (PiglinForgemaster.Skill skill : PiglinForgemaster.Skill.values()) {
                if (skill != PiglinForgemaster.Skill.NONE) skills[skill.id] = anims.bake(skill.animation, root);
            }
        }

        @Override
        public void setupAnim(State s) {
            super.setupAnim(s);
            head.yRot += s.yRot * ((float) Math.PI / 180.0F);
            head.xRot += s.xRot * ((float) Math.PI / 180.0F);

            if (s.dying) {
                // deathTime counts ticks since death; the clip holds its last pose.
                death.apply((long) (s.deathTime * 50.0F), 1.0F);
                return;
            }
            boolean acting = false;
            float speed = s.phaseTwo ? 1.2F : 1.0F; // matches the server's +20% attack speed
            for (int i = 1; i < skills.length; i++) {
                if (s.pose[i].isStarted()) {
                    acting = true;
                    skills[i].apply(s.pose[i], s.ageInTicks, speed);
                }
            }
            if (!acting) {
                idle.apply((long) (s.ageInTicks * 50.0F), 1.0F);
                walk.applyWalk(s.walkAnimationPos, s.walkAnimationSpeed, 3.0F, 2.5F);
            }
        }
    }
}
