package dev.itzgamesgd.moreminecrafts.client.model;

import dev.itzgamesgd.moreminecrafts.core.ModIds;
import dev.itzgamesgd.moreminecrafts.mob.bone.BoneMobs;
import dev.itzgamesgd.moreminecrafts.mob.bone.BoneSkillMonster;
import dev.itzgamesgd.moreminecrafts.boss.DeepSeaPoseidon;
import dev.itzgamesgd.moreminecrafts.mob.sky.FloatingJellyfish;
import dev.itzgamesgd.moreminecrafts.mob.sky.SkyWyvern;
import java.util.List;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import net.minecraft.client.animation.KeyframeAnimation;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.AnimationState;
import net.minecraft.world.entity.EntityType;

/**
 * Renderer for {@link BoneSkillMonster}s. Mesh and every clip come from {@code entity_models/<id>.json}
 * ({@code convert_bbmodel.py --animations}). Layers per frame: base loop (time driven), optional locomotion loop
 * (driven by walk speed), the synced skill clip, a short hurt clip, and the death clip which replaces everything.
 */
public final class BoneMobRenderer<T extends BoneSkillMonster> extends MobRenderer<T, BoneMobRenderer.State, BoneMobRenderer.Model> {
    public record Spec(String id, float shadow, String baseLoop, String locomotion, float locoTime, float locoScale,
                       String hurt, String death, boolean translucent, String lookBone) {}

    public static final class State extends LivingEntityRenderState {
        AnimationState[] pose = new AnimationState[0];
        float skillSpeed = 1.0F;
        boolean dying;
        float hurtTicks;
    }

    private final Identifier texture;
    private final boolean translucent;

    public BoneMobRenderer(EntityRendererProvider.Context ctx, Spec spec, List<String> skills) {
        super(ctx, new Model(spec, skills,
                BoneModelLoader.load(ctx.getResourceManager(), ModIds.id("entity_models/" + spec.id() + ".json")),
                BoneAnimations.load(ctx.getResourceManager(), ModIds.id("entity_models/" + spec.id() + ".json"))), spec.shadow());
        this.texture = ModIds.id("textures/entity/" + spec.id() + ".png");
        this.translucent = spec.translucent();
    }

    private static <T extends BoneSkillMonster> void register(EntityType<T> type, Spec spec, List<String> skills) {
        EntityRendererRegistry.register(type, ctx -> new BoneMobRenderer<>(ctx, spec, skills));
    }

    public static void registerAll() {
        register(BoneMobs.DEEP_SEA_POSEIDON, new Spec("deep_sea_poseidon", 0.8F, "idle", "swim", 2.0F, 2.0F, null, "death", false, "head_core_bone"),
                DeepSeaPoseidon.SKILLS);
        register(BoneMobs.SKY_WYVERN, new Spec("sky_wyvern", 1.0F, "fly", null, 1.0F, 1.0F, "hurt", "death", false, "head_bone"),
                SkyWyvern.SKILLS);
        register(BoneMobs.FLOATING_JELLYFISH, new Spec("floating_jellyfish", 0.5F, "idle", null, 1.0F, 1.0F, null, "death", true, null),
                FloatingJellyfish.SKILLS);
    }

    @Override
    public State createRenderState() {
        return new State();
    }

    @Override
    public void extractRenderState(T mob, State state, float partialTick) {
        super.extractRenderState(mob, state, partialTick);
        if (state.pose.length != mob.poseStates.length) {
            state.pose = new AnimationState[mob.poseStates.length];
            for (int i = 0; i < state.pose.length; i++) state.pose[i] = new AnimationState();
        }
        for (int i = 0; i < state.pose.length; i++) state.pose[i].copyFrom(mob.poseStates[i]);
        state.skillSpeed = mob.animationSpeed();
        state.dying = mob.isDeadOrDying();
        state.hurtTicks = mob.hurtTime > 0 ? (10 - mob.hurtTime) + partialTick : -1.0F;
    }

    @Override
    public Identifier getTextureLocation(State state) {
        return texture;
    }

    @Override
    protected RenderType getRenderType(State state, boolean bodyVisible, boolean translucentBody, boolean glowing) {
        if (translucent && bodyVisible) return RenderTypes.entityTranslucent(texture);
        return super.getRenderType(state, bodyVisible, translucentBody, glowing);
    }

    /** Death clips lay the mob down themselves. */
    @Override
    protected float getFlipDegrees() {
        return 0.0F;
    }

    public static final class Model extends EntityModel<State> {
        private final Spec spec;
        private final ModelPart look;
        private final KeyframeAnimation base;
        private final KeyframeAnimation locomotion;
        private final KeyframeAnimation hurt;
        private final KeyframeAnimation death;
        private final KeyframeAnimation[] skills;

        Model(Spec spec, List<String> skillNames, BoneModelLoader.Loaded loaded, BoneAnimations anims) {
            super(loaded.root());
            this.spec = spec;
            ModelPart root = loaded.root();
            this.look = spec.lookBone() != null ? loaded.require(spec.lookBone()) : null;
            this.base = anims.bake(spec.baseLoop(), root);
            this.locomotion = spec.locomotion() != null ? anims.bake(spec.locomotion(), root) : null;
            this.hurt = spec.hurt() != null ? anims.bake(spec.hurt(), root) : null;
            this.death = anims.bake(spec.death(), root);
            this.skills = new KeyframeAnimation[skillNames.size() + 1];
            for (int i = 0; i < skillNames.size(); i++) skills[i + 1] = anims.bake(skillNames.get(i), root);
        }

        @Override
        public void setupAnim(State s) {
            super.setupAnim(s);
            if (s.dying) {
                death.apply((long) (s.deathTime * 50.0F), 1.0F);
                return;
            }
            if (look != null) {
                look.yRot += s.yRot * ((float) Math.PI / 180.0F) * 0.6F;
                look.xRot += s.xRot * ((float) Math.PI / 180.0F) * 0.6F;
            }
            boolean acting = false;
            for (int i = 1; i < skills.length && i < s.pose.length; i++) {
                if (s.pose[i].isStarted()) {
                    acting = true;
                    skills[i].apply(s.pose[i], s.ageInTicks, s.skillSpeed);
                }
            }
            if (!acting) {
                base.apply((long) (s.ageInTicks * 50.0F), 1.0F);
                if (locomotion != null) locomotion.applyWalk(s.walkAnimationPos, s.walkAnimationSpeed, spec.locoTime(), spec.locoScale());
            }
            if (hurt != null && s.hurtTicks >= 0.0F) hurt.apply((long) (s.hurtTicks * 50.0F), 1.0F);
        }
    }
}
