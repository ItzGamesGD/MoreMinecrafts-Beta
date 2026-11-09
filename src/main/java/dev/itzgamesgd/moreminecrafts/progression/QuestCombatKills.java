package dev.itzgamesgd.moreminecrafts.progression;

import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.UUID;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.TagKey;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.arrow.AbstractArrow;
import net.minecraft.world.item.BowItem;
import net.minecraft.world.item.CrossbowItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/**
 * Live kill evidence for the event-based combat quests. Credit is the victim's own vanilla
 * {@link LivingEntity#getKillCredit()} player, exactly the player vanilla gives the kill statistic and
 * loot to. A kill is processed once per victim; weapon quests additionally need that same player to have
 * delivered the final blow personally (a projectile, a pet, fire or the environment never qualifies).
 */
public final class QuestCombatKills {
    static final String DAGGER = "kills_dagger";
    static final String GREATSWORD = "kills_greatsword";
    static final String NETHERITE_DAGGER = "kills_netherite_dagger";
    static final String NETHERITE_GREATSWORD = "kills_netherite_greatsword";
    static final String BANDIT = "kills_bandit";
    static final String BANDIT_ARCHER = "kills_bandit_archer";
    static final String BANDIT_WARRIOR = "kills_bandit_warrior";
    static final String LONG_SHOT = "kills_long_shot";
    static final String MACE_SMASH = "kills_mace_smash";
    static final double LONG_SHOT_DISTANCE = 30.0D;
    private static final TagKey<Item> DAGGERS = TagKey.create(Registries.ITEM, Identifier.parse("moreminecrafts:daggers"));
    private static final TagKey<Item> GREATSWORDS = TagKey.create(Registries.ITEM, Identifier.parse("moreminecrafts:greatswords"));
    private static final int RECENT_LIMIT = 512;
    /** Victims already processed; guards repeated AFTER_DEATH delivery. Server thread only. */
    private static final Set<UUID> RECENT = new LinkedHashSet<>();

    private QuestCombatKills() {}

    public static void initialize() {
        ServerLivingEntityEvents.AFTER_DEATH.register(QuestCombatKills::died);
    }

    static void died(LivingEntity victim, DamageSource source) {
        if (victim instanceof Player || victim.level().isClientSide()) return;
        if (victim.getType().getCategory() != MobCategory.MONSTER) return;
        if (!(victim.getKillCredit() instanceof ServerPlayer player)) return;
        if (!firstTime(victim.getUUID())) return;
        ResearchProgressState before = KnowledgeRuntime.state(player);
        ResearchProgressState next = before;
        Identifier type = BuiltInRegistries.ENTITY_TYPE.getKey(victim.getType());
        if (type.getNamespace().equals("moreminecrafts")) {
            switch (type.getPath()) {
                case "bandit" -> next = QuestRules.addCounter(next, BANDIT, 1);
                case "bandit_archer" -> next = QuestRules.addCounter(next, BANDIT_ARCHER, 1);
                case "bandit_warrior" -> next = QuestRules.addCounter(next, BANDIT_WARRIOR, 1);
                default -> { }
            }
        }
        boolean personal = source.getEntity() == player && source.getDirectEntity() == player;
        ItemStack weapon = source.getWeaponItem();
        if (personal && weapon != null && !weapon.isEmpty()) {
            Identifier item = BuiltInRegistries.ITEM.getKey(weapon.getItem());
            if (weapon.is(DAGGERS)) next = QuestRules.addCounter(next, DAGGER, 1);
            if (weapon.is(GREATSWORDS)) next = QuestRules.addCounter(next, GREATSWORD, 1);
            if (item.equals(Identifier.parse("moreminecrafts:netherite_dagger")))
                next = QuestRules.addCounter(next, NETHERITE_DAGGER, 1);
            if (item.equals(Identifier.parse("moreminecrafts:netherite_greatsword")))
                next = QuestRules.addCounter(next, NETHERITE_GREATSWORD, 1);
        }
        if (personal && source.is(DamageTypes.MACE_SMASH)) next = QuestRules.addCounter(next, MACE_SMASH, 1);
        if (source.getEntity() == player && source.getDirectEntity() instanceof AbstractArrow arrow
                && arrow.getOwner() == player && weapon != null
                && (weapon.getItem() instanceof BowItem || weapon.getItem() instanceof CrossbowItem)
                && player.level() == victim.level() && player.distanceTo(victim) >= LONG_SHOT_DISTANCE)
            next = QuestRules.addCounter(next, LONG_SHOT, 1);
        if (!next.equals(before)) {
            KnowledgeRuntime.updateState(player, next);
            XpQuestTracking.reconcile(player);
        }
    }

    private static boolean firstTime(UUID victim) {
        if (!RECENT.add(victim)) return false;
        if (RECENT.size() > RECENT_LIMIT) {
            Iterator<UUID> oldest = RECENT.iterator();
            oldest.next();
            oldest.remove();
        }
        return true;
    }
}
