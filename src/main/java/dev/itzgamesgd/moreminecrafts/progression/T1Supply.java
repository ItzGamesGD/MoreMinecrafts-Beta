package dev.itzgamesgd.moreminecrafts.progression;
import java.util.*;
import java.util.function.DoubleSupplier;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.item.enchantment.*;
import net.minecraft.world.item.trading.*;

/** Supply policies preserve ordinary equipment bases and remove only random enchantment bypasses. */
public final class T1Supply {
    public static boolean retainNaturalEnchantedCandidate(double roll){
        return roll>=0 && roll<T1Balance.NATURAL_ENCHANTED_KEEP_CHANCE;
    }
    public static boolean retainLibrarianBookCandidate(double roll){
        return roll>=0 && roll<T1Balance.LIBRARIAN_BOOK_OFFER_CHANCE;
    }
    public static ItemStack filterLoot(ItemStack stack,DoubleSupplier rng){
        if(stack.is(Items.ENCHANTED_BOOK))return T1Operations.book(stack).isPresent()&&retainNaturalEnchantedCandidate(rng.getAsDouble())?stack:ItemStack.EMPTY;
        if(!stack.getOrDefault(DataComponents.ENCHANTMENTS,ItemEnchantments.EMPTY).isEmpty()&&!retainNaturalEnchantedCandidate(rng.getAsDouble()))stack.set(DataComponents.ENCHANTMENTS,ItemEnchantments.EMPTY);
        return stack;
    }
    public static MerchantOffer smithOffer(Player registryContext,MerchantOffer old){
        ItemStack result=old.getResult().copyWithCount(1);result.setDamageValue(0);result.set(DataComponents.ENCHANTMENTS,ItemEnchantments.EMPTY);
        return smithOffer(registryContext.level().registryAccess(),old,result);
    }
    public static MerchantOffer smithOffer(net.minecraft.core.RegistryAccess registries,MerchantOffer old,ItemStack result){
        var type=EquipmentTreeResolver.resolve(result);
        String id=switch(type){case HELMET,CHESTPLATE,LEGGINGS,BOOTS->"protection";case PICKAXE,SHOVEL,HOE->"efficiency";case BOW->"power";case CROSSBOW->"quick_charge";case TRIDENT->"impaling";default->"sharpness";};
        var key=net.minecraft.resources.ResourceKey.create(net.minecraft.core.registries.Registries.ENCHANTMENT,net.minecraft.resources.Identifier.withDefaultNamespace(id));
        result.setDamageValue(0);result.set(DataComponents.ENCHANTMENTS,ItemEnchantments.EMPTY);
        var enchantment=registries.lookupOrThrow(net.minecraft.core.registries.Registries.ENCHANTMENT).get(key);
        enchantment.ifPresent(holder->EnchantmentHelper.updateEnchantments(result,e->e.set(holder,2)));
        ItemCost equipment=new ItemCost(result.getItem(),1).withComponents(b->b.expect(DataComponents.DAMAGE,0));
        return new MerchantOffer(equipment,Optional.of(new ItemCost(Items.EMERALD,T1Balance.SMITH_EMERALDS)),result,old.getUses(),old.getMaxUses(),old.getXp(),0);
    }
    public static boolean isSmith(net.minecraft.world.entity.npc.villager.Villager villager){
        var profession=villager.getVillagerData().profession();
        return profession.is(net.minecraft.world.entity.npc.villager.VillagerProfession.ARMORER)||profession.is(net.minecraft.world.entity.npc.villager.VillagerProfession.WEAPONSMITH)||profession.is(net.minecraft.world.entity.npc.villager.VillagerProfession.TOOLSMITH);
    }
    /** Static merchant offers preview a moderate upgrade; actual output carries the submitted item's state. */
    public static Optional<ItemStack> smithUpgrade(ItemStack submitted,ItemStack offered){
        ItemStack result=submitted.copyWithCount(1);
        Map<String,Integer> owned=T1Operations.enchantments(result);
        EnchantmentHelper.getEnchantmentsForCrafting(offered).entrySet().forEach(entry->{
            String id=entry.getKey().unwrapKey().map(key->key.identifier().toString()).orElse("");
            var quote=EnchantmentTrees.quote(EquipmentTreeResolver.resolve(result),owned,id);
            int current=owned.getOrDefault(id,0);
            // A smith cannot add a conflicting or premium-overlap branch to pre-grown gear.
            if(current==0&&(!quote.allowed()||quote.premium()>0))return;
            if(entry.getIntValue()<=current)return;
            EnchantmentHelper.updateEnchantments(result,e->e.set(entry.getKey(),Math.max(current,entry.getIntValue())));
        });
        return T1Operations.enchantments(result).equals(owned) ? Optional.empty() : Optional.of(result);
    }

    /** Makes retained-book prices immune to special prices and vanilla demand modifiers. */
    public static void enforceFixedBookPrices(MerchantOffers offers){
        for(int i=0;i<offers.size();i++){
            MerchantOffer offer=offers.get(i);
            if(!isFixedBookOffer(offer))continue;
            if(offer.getSpecialPriceDiff()!=0||offer.getDemand()!=0||offer.getPriceMultiplier()!=0){
                offers.set(i,new MerchantOffer(offer.getItemCostA(),offer.getItemCostB(),offer.getResult().copy(),
                        offer.getUses(),offer.getMaxUses(),offer.getXp(),0,0));
            }
        }
    }
    public static boolean isFixedBookOffer(MerchantOffer offer){
        return offer.getMaxUses()==T1Balance.LIBRARIAN_MAX_USES
                && offer.getBaseCostA().is(Items.EMERALD)
                && offer.getBaseCostA().getCount()==T1Balance.LIBRARIAN_EMERALDS
                && offer.getCostB().is(Items.BOOK)
                && offer.getResult().is(Items.ENCHANTED_BOOK)
                && T1Operations.book(offer.getResult()).isPresent();
    }
    public static void normalizeTrades(net.minecraft.world.entity.npc.villager.Villager villager,MerchantOffers offers){
        var profession=villager.getVillagerData().profession();
        boolean smith=isSmith(villager);
        for(int i=offers.size()-1;i>=0;i--){
            MerchantOffer offer=offers.get(i);ItemStack result=offer.getResult();
            if(result.is(Items.ENCHANTED_BOOK)){
                if(T1Operations.book(result).isEmpty()){offers.remove(i);continue;}
                // Existing normalized offers are stable across saves, restocks and repeated UI opens.
                if(isFixedBookOffer(offer))continue;
                if(!retainLibrarianBookCandidate(villager.getRandom().nextDouble())){offers.remove(i);continue;}
                offers.set(i,new MerchantOffer(new ItemCost(Items.EMERALD,T1Balance.LIBRARIAN_EMERALDS),Optional.of(new ItemCost(Items.BOOK)),result.copy(),T1Balance.LIBRARIAN_MAX_USES,offer.getXp(),0));
            }else if(smith&&EquipmentTreeResolver.resolve(result)!=EnchantmentTrees.Equipment.NONE){
                if(offer.getBaseCostA().is(result.getItem()))continue;
                offers.set(i,smithOffer(villager.level().registryAccess(),offer,result.copyWithCount(1)));
            }
        }
        enforceFixedBookPrices(offers);
    }
    private T1Supply(){}
}
