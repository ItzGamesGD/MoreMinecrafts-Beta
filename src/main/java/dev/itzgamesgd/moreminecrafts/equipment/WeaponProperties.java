package dev.itzgamesgd.moreminecrafts.equipment;

import dev.itzgamesgd.moreminecrafts.core.ModIds;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.component.AttackRange;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.minecraft.world.item.component.Weapon;

public final class WeaponProperties {
    private WeaponProperties() {}
    public static Item.Properties create(net.minecraft.resources.ResourceKey<Item> key, float damage,
                                         float attacksPerSecond, float reach) {
        var attributes = ItemAttributeModifiers.builder()
                .add(Attributes.ATTACK_DAMAGE, new AttributeModifier(Item.BASE_ATTACK_DAMAGE_ID,
                        damage - 1.0D, AttributeModifier.Operation.ADD_VALUE), EquipmentSlotGroup.MAINHAND)
                .add(Attributes.ATTACK_SPEED, new AttributeModifier(Item.BASE_ATTACK_SPEED_ID,
                        attacksPerSecond - 4.0D, AttributeModifier.Operation.ADD_VALUE), EquipmentSlotGroup.MAINHAND)
                .build();
        return new Item.Properties().stacksTo(1).enchantable(15).attributes(attributes)
                .component(DataComponents.WEAPON, new Weapon(0))
                .component(DataComponents.ATTACK_RANGE, new AttackRange(0, reach, 0, reach + 1.5F, 0.3F, 1.0F))
                .setId(key);
    }
}
