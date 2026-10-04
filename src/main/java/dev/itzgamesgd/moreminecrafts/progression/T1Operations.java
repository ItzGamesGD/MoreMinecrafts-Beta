package dev.itzgamesgd.moreminecrafts.progression;

import java.util.*;
import java.util.function.DoubleSupplier;
import dev.itzgamesgd.moreminecrafts.registry.ModDataComponents;
import dev.itzgamesgd.moreminecrafts.registry.ModItems;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.item.enchantment.*;

/** Server-only mutations: all eligibility and payment checks precede RNG and consumption. */
public final class T1Operations {
    public record Book(String id,int level) {}
    public static Map<String,Integer> enchantments(ItemStack stack) {
        Map<String,Integer> result=new HashMap<>();
        EnchantmentHelper.getEnchantmentsForCrafting(stack).entrySet().forEach(e->e.getKey().unwrapKey().ifPresent(k->result.put(k.identifier().toString(),e.getIntValue())));
        return result;
    }
    public static Optional<Book> book(ItemStack stack) {
        if(!stack.is(Items.ENCHANTED_BOOK))return Optional.empty();
        var ench=enchantments(stack);
        if(ench.size()!=1)return Optional.empty();
        var e=ench.entrySet().iterator().next();
        int max=BookOptionRules.MAXIMUMS.getOrDefault(e.getKey(),0);
        return e.getValue()>0 && e.getValue()<=max?Optional.of(new Book(e.getKey(),e.getValue())):Optional.empty();
    }
    public static Optional<Holder.Reference<Enchantment>> enchantment(Player player,String id) {
        return player.level().registryAccess().lookupOrThrow(Registries.ENCHANTMENT).get(ResourceKey.create(Registries.ENCHANTMENT,Identifier.parse(id)));
    }
    public static boolean purchase(Player player,ItemStack stack,String id) {
        if(player.level().isClientSide() || stack.getCount()!=1)return false;
        var quote=EnchantmentTrees.quote(EquipmentTreeResolver.resolve(stack),enchantments(stack),id);
        var holder=enchantment(player,id);
        if(!quote.allowed() || holder.isEmpty() || !RawXp.pay(player,quote.cost()))return false;
        EnchantmentHelper.updateEnchantments(stack,e->e.set(holder.get(),quote.nextLevel()));
        return true;
    }
    public static boolean canApply(ItemStack equipment,ItemStack source) {
        var b=book(source);
        if(equipment.getCount()!=1 || EquipmentTreeResolver.resolve(equipment)==EnchantmentTrees.Equipment.NONE || b.isEmpty())return false;
        return enchantments(equipment).getOrDefault(b.get().id(),0)>0 && !options(equipment).containsKey(b.get().id());
    }
    public static boolean apply(Player player,ItemStack equipment,ItemStack source,DoubleSupplier rng) {
        if(player.level().isClientSide() || !canApply(equipment,source) || !RawXp.canPay(player,T1Balance.BOOK_OPTION_XP))return false;
        Book b=book(source).orElseThrow();int max=BookOptionRules.MAXIMUMS.get(b.id());
        int roll=BookOptionRules.roll(max,b.level(),max,rng);
        Map<String,Integer> options=new HashMap<>(options(equipment));options.put(b.id(),roll);
        RawXp.pay(player,T1Balance.BOOK_OPTION_XP);
        equipment.set(ModDataComponents.BOOK_OPTIONS,Map.copyOf(options));source.shrink(1);return true;
    }
    public static Map<String,Integer> options(ItemStack stack){return stack.getOrDefault(ModDataComponents.BOOK_OPTIONS,Map.of());}
    public static int option(ItemStack stack,String path){String id="minecraft:"+path;return enchantments(stack).getOrDefault(id,0)>0?options(stack).getOrDefault(id,0):0;}
    public static boolean reset(Player player,ItemStack equipment,String id) {
        if(player.level().isClientSide() || !options(equipment).containsKey(id))return false;
        List<Item> materials=List.of(Items.DIAMOND,ModItems.MYTHRIL,Items.NETHERITE_INGOT,ModItems.ADAMANTIUM_INGOT);
        for(Item item:materials)if(player.getInventory().countItem(item)<1)return false;
        for(Item item:materials)for(int i=0;i<player.getInventory().getContainerSize();i++) {
            ItemStack stack=player.getInventory().getItem(i);if(stack.is(item)){stack.shrink(1);break;}
        }
        Map<String,Integer> result=new HashMap<>(options(equipment));result.remove(id);
        if(result.isEmpty())equipment.remove(ModDataComponents.BOOK_OPTIONS);else equipment.set(ModDataComponents.BOOK_OPTIONS,Map.copyOf(result));
        player.getInventory().setChanged();return true;
    }
    public static Optional<ItemStack> combine(Player player,ItemStack a,ItemStack b) {
        var first=book(a);var second=book(b);if(first.isEmpty()||second.isEmpty())return Optional.empty();
        int next=BookOptionRules.combine(first.get().id(),first.get().level(),second.get().id(),second.get().level());
        if(next==0)return Optional.empty();var holder=enchantment(player,first.get().id());if(holder.isEmpty())return Optional.empty();
        ItemStack result=new ItemStack(Items.ENCHANTED_BOOK);EnchantmentHelper.updateEnchantments(result,e->e.set(holder.get(),next));return Optional.of(result);
    }
    public static int combineCost(ItemStack result){return book(result).map(b->b.level()*T1Balance.BOOK_COMBINE_XP_PER_TARGET_LEVEL).orElse(0);}
    private T1Operations(){}
}
