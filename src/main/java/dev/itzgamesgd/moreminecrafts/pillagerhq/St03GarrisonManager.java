package dev.itzgamesgd.moreminecrafts.pillagerhq;

import dev.itzgamesgd.moreminecrafts.mob.CustomMobCatalog;
import java.util.List;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.illager.Vindicator;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.block.Rotation;

/**
 * Handles one-time proximity-triggered ST-03 garrison spawning, dedicated variant equipment,
 * Commander tuning (40 HP, +25% melee damage, vanilla follow_range preserved),
 * persistent survival, permanent non-respawn of eliminated slots, and 10% Commander special drop.
 */
public final class St03GarrisonManager {
    private St03GarrisonManager() {}

    public static final int PROXIMITY_MARGIN_BLOCKS = 32;
    public static final String TAG_HQ_PREFIX = "st03_hq:";
    public static final String TAG_SLOT_PREFIX = "st03_slot:";
    public static final String TAG_COMMANDER = "st03_commander";
    public static final String TAG_PRISONER = "st03_prisoner";
    private static final Identifier COMMANDER_DAMAGE_MOD_ID = Identifier.fromNamespaceAndPath("moreminecrafts", "st03_commander_damage");

    private static boolean eventsRegistered;

    public static void initialize() {
        if (eventsRegistered) {
            return;
        }
        eventsRegistered = true;
        ServerLivingEntityEvents.AFTER_DEATH.register((entity, source) -> {
            if (entity.level() instanceof ServerLevel level) {
                onLivingEntityDeath(level, entity);
            }
        });
    }

    public static void tick(ServerLevel level, St03HeadquartersData data) {
        List<ServerPlayer> players = level.players();
        if (players.isEmpty()) {
            return;
        }
        for (St03HeadquartersData.HqInstance hq : data.instances().values()) {
            if (!hq.structureCompleted || hq.garrisonInitialized || hq.box == null || hq.origin == null) {
                continue;
            }
            for (ServerPlayer player : players) {
                if (!player.isSpectator() && hq.isNearCompound(player.blockPosition(), PROXIMITY_MARGIN_BLOCKS)) {
                    spawnInitialGarrison(level, hq, data);
                    break;
                }
            }
        }
    }

    public static void spawnInitialGarrison(ServerLevel level, St03HeadquartersData.HqInstance hq, St03HeadquartersData data) {
        if (hq.garrisonInitialized || hq.origin == null) {
            return;
        }
        hq.garrisonInitialized = true;
        Rotation rot = hq.rotation;
        for (St03GarrisonSpec.SpawnSlot slot : St03GarrisonSpec.SLOTS) {
            if (hq.eliminatedSlots.contains(slot.index())) {
                continue;
            }
            BlockPos pos = slot.worldPos(hq.origin, rot);
            spawnSlotMob(level, hq, slot, pos);
        }
        for (St03GarrisonSpec.PrisonerSlot prisoner : St03GarrisonSpec.PRISONER_SLOTS) {
            spawnPrisoner(level, hq, prisoner.worldPos(hq.origin, rot));
        }
        if (hq.eliminatedSlots.size() >= St03GarrisonSpec.SLOTS.size()) {
            hq.cleared = true;
        }
        data.setDirty();
    }

    private static void spawnPrisoner(ServerLevel level, St03HeadquartersData.HqInstance hq, BlockPos pos) {
        Villager villager = EntityType.VILLAGER.create(level, EntitySpawnReason.STRUCTURE);
        if (villager == null) {
            return;
        }
        villager.snapTo(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, level.getRandom().nextFloat() * 360.0F, 0.0F);
        villager.setPersistenceRequired();
        villager.addTag(TAG_HQ_PREFIX + hq.id());
        villager.addTag(TAG_PRISONER);
        level.addFreshEntity(villager);
    }

    private static void spawnSlotMob(ServerLevel level, St03HeadquartersData.HqInstance hq,
                                     St03GarrisonSpec.SpawnSlot slot, BlockPos pos) {
        Mob mob = switch (slot.role()) {
            case BANDIT -> CustomMobCatalog.BANDIT.create(level, EntitySpawnReason.STRUCTURE);
            case BANDIT_WARRIOR -> CustomMobCatalog.BANDIT_WARRIOR.create(level, EntitySpawnReason.STRUCTURE);
            case BANDIT_ARCHER -> CustomMobCatalog.BANDIT_ARCHER.create(level, EntitySpawnReason.STRUCTURE);
            case WANDERING_RAIDER -> CustomMobCatalog.WANDERING_RAIDER.create(level, EntitySpawnReason.STRUCTURE);
            case RAVAGER -> EntityType.RAVAGER.create(level, EntitySpawnReason.STRUCTURE);
            case PILLAGER_COMMANDER -> CustomMobCatalog.PILLAGER_COMMANDER.create(level, EntitySpawnReason.STRUCTURE);
            case PILLAGER_BEASTMASTER -> CustomMobCatalog.PILLAGER_BEASTMASTER.create(level, EntitySpawnReason.STRUCTURE);
            case ARMORY_GUARD -> CustomMobCatalog.ARMORY_GUARD.create(level, EntitySpawnReason.STRUCTURE);
            case PILLAGER_DRILLMASTER -> CustomMobCatalog.PILLAGER_DRILLMASTER.create(level, EntitySpawnReason.STRUCTURE);
        };
        if (mob == null) {
            return;
        }
        mob.snapTo(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, level.getRandom().nextFloat() * 360.0F, 0.0F);
        mob.setPersistenceRequired();
        mob.addTag(TAG_HQ_PREFIX + hq.id());
        mob.addTag(TAG_SLOT_PREFIX + slot.index());

        switch (slot.role()) {
            case BANDIT -> mob.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.IRON_SWORD));
            case BANDIT_ARCHER -> mob.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.BOW));
            case BANDIT_WARRIOR -> mob.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.IRON_AXE));
            case WANDERING_RAIDER, PILLAGER_BEASTMASTER -> mob.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.CROSSBOW));
            case PILLAGER_COMMANDER -> {
                if (mob instanceof Vindicator commander) {
                    configureCommander(level, commander);
                }
            }
            case ARMORY_GUARD -> {
                mob.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.IRON_SWORD));
                mob.setItemSlot(EquipmentSlot.OFFHAND, new ItemStack(Items.SHIELD));
            }
            case PILLAGER_DRILLMASTER -> {
                ItemStack crossbow = new ItemStack(Items.CROSSBOW);
                var quickCharge = level.registryAccess().lookupOrThrow(Registries.ENCHANTMENT)
                        .getOrThrow(Enchantments.QUICK_CHARGE);
                crossbow.enchant(quickCharge, 1);
                mob.setItemSlot(EquipmentSlot.MAINHAND, crossbow);
            }
            case RAVAGER -> {}
        }

        // Preserve vanilla follow_range and AI attributes strictly as-is.
        level.addFreshEntity(mob);
    }

    /**
     * Approved Commander configuration:
     * - 40 HP (20 hearts)
     * - +25% fixed melee attack damage over same-difficulty vanilla Vindicator
     * - Iron Axe with Unbreaking I (glint)
     * - Vanilla Vindicator AI and follow_range (12) strictly preserved
     */
    private static void configureCommander(ServerLevel level, Vindicator commander) {
        commander.addTag(TAG_COMMANDER);
        commander.setCustomName(Component.translatable("entity.moreminecrafts.pillager_commander"));
        commander.setCustomNameVisible(true);

        ItemStack axe = new ItemStack(Items.IRON_AXE);
        var unbreaking = level.registryAccess().lookupOrThrow(Registries.ENCHANTMENT)
                .getOrThrow(Enchantments.UNBREAKING);
        axe.enchant(unbreaking, 1);
        commander.setItemSlot(EquipmentSlot.MAINHAND, axe);
        commander.setDropChance(EquipmentSlot.MAINHAND, 0.0F);

        AttributeInstance maxHealth = commander.getAttribute(Attributes.MAX_HEALTH);
        if (maxHealth != null) {
            maxHealth.setBaseValue(40.0D);
        }
        commander.setHealth(40.0F);

        AttributeInstance attackDamage = commander.getAttribute(Attributes.ATTACK_DAMAGE);
        if (attackDamage != null && !attackDamage.hasModifier(COMMANDER_DAMAGE_MOD_ID)) {
            attackDamage.addPermanentModifier(new AttributeModifier(
                    COMMANDER_DAMAGE_MOD_ID, 0.25D, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
        }
    }

    private static void onLivingEntityDeath(ServerLevel level, LivingEntity entity) {
        String hqId = null;
        int slotIdx = -1;
        boolean isCommander = entity.getTags().contains(TAG_COMMANDER);
        for (String tag : entity.getTags()) {
            if (tag.startsWith(TAG_HQ_PREFIX)) {
                hqId = tag.substring(TAG_HQ_PREFIX.length());
            } else if (tag.startsWith(TAG_SLOT_PREFIX)) {
                try {
                    slotIdx = Integer.parseInt(tag.substring(TAG_SLOT_PREFIX.length()));
                } catch (NumberFormatException ignored) {
                }
            }
        }
        if (hqId == null || slotIdx < 0) {
            return;
        }

        St03HeadquartersData data = St03HeadquartersData.get(level);
        var optHq = data.findById(hqId);
        if (optHq.isEmpty()) {
            return;
        }
        St03HeadquartersData.HqInstance hq = optHq.get();
        hq.eliminatedSlots.add(slotIdx);

        if (isCommander && !hq.commanderKilled) {
            hq.commanderKilled = true;
            if (!hq.commanderSpecialAttempted) {
                hq.commanderSpecialAttempted = true;
                // Ordinary rare drop + independent 10% special roll
                entity.spawnAtLocation(level, new ItemStack(Items.EMERALD, 3 + level.getRandom().nextInt(6)));
                if (level.getRandom().nextDouble() < St03PremiumLoot.COMMANDER_CHANCE) {
                    ItemStack special = St03PremiumLoot.createPremiumStack(
                            St03PremiumLoot.Source.COMMANDER, level.getRandom()::nextInt);
                    entity.spawnAtLocation(level, special);
                }
            }
        }

        if (!hq.cleared && hq.eliminatedSlots.size() >= St03GarrisonSpec.SLOTS.size()) {
            hq.cleared = true;
        }
        data.setDirty();
    }
}
