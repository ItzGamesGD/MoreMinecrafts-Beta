package dev.itzgamesgd.moreminecrafts.progression;
import java.lang.reflect.Method;
import java.util.*;
import dev.itzgamesgd.moreminecrafts.enhancement.EnhancementOperation;
import dev.itzgamesgd.moreminecrafts.registry.*;
import net.fabricmc.fabric.api.gametest.v1.*;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.*;
import net.minecraft.world.item.enchantment.*;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.trading.*;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.NbtOps;
import net.minecraft.resources.RegistryOps;

public final class T1ProgressionGameTests implements CustomTestMethodInvoker {
    @GameTest public void itemTransactionsAndPersistence(GameTestHelper ctx){
        var player=ctx.makeMockServerPlayer(net.minecraft.world.level.GameType.SURVIVAL);player.giveExperiencePoints(10000);
        var sword=new ItemStack(Items.DIAMOND_SWORD);int before=RawXp.available(player);
        ctx.assertTrue(T1Operations.purchase(player,sword,"minecraft:sharpness"),"purchase Sharpness I");
        ctx.assertTrue(RawXp.available(player)==before-100,"deduct raw XP, not levels");
        ctx.assertTrue(!T1Operations.purchase(player,sword,"minecraft:mending"),"Mending requires Unbreaking III");
        var book=book(player,"minecraft:sharpness",1);
        ctx.assertTrue(T1Operations.apply(player,sword,book,()->0),"apply compatible book");
        ctx.assertTrue(book.isEmpty()&&T1Operations.option(sword,"sharpness")==5,"book consumed and low level can roll maximum");
        ctx.assertTrue(!T1Operations.apply(player,sword,book(player,"minecraft:sharpness",5),()->0),"cannot overwrite locked option");
        T1Operations.purchase(player,sword,"minecraft:fire_aspect");
        ctx.assertTrue(T1Operations.apply(player,sword,book(player,"minecraft:fire_aspect",1),()->.99),"second independent option");
        var ops=RegistryOps.create(NbtOps.INSTANCE,ctx.getLevel().registryAccess());
        var data=ItemStack.CODEC.encodeStart(ops,sword).getOrThrow();var restored=ItemStack.CODEC.parse(ops,data).getOrThrow();
        ctx.assertTrue(T1Operations.options(restored).equals(T1Operations.options(sword)),"Book Options survive item save/load");
        ctx.assertTrue(!T1Operations.reset(player,sword,"minecraft:sharpness"),"reset cannot omit materials");
        for(Item item:List.of(Items.DIAMOND,ModItems.MYTHRIL,Items.NETHERITE_INGOT,ModItems.ADAMANTIUM_INGOT))player.getInventory().add(new ItemStack(item));
        ctx.assertTrue(T1Operations.reset(player,sword,"minecraft:sharpness"),"reset succeeds with all four");
        ctx.assertTrue(!T1Operations.options(sword).containsKey("minecraft:sharpness")&&T1Operations.option(sword,"fire_aspect")==1,"only selected option cleared");
        for(Item item:List.of(Items.DIAMOND,ModItems.MYTHRIL,Items.NETHERITE_INGOT,ModItems.ADAMANTIUM_INGOT))ctx.assertTrue(player.getInventory().countItem(item)==0,"reset consumes each material");
        ctx.succeed();
    }
    @GameTest public void enchantingMenuKeepsModesAndInventoryStable(GameTestHelper ctx){
        var player=ctx.makeMockServerPlayer(net.minecraft.world.level.GameType.SURVIVAL);
        var menu=new EnchantingProgressionMenu(3,player.getInventory());
        menu.getSlot(0).set(new ItemStack(Items.DIAMOND_SWORD));
        var source=book(player,"minecraft:sharpness",3);
        player.getInventory().setItem(9,source.copy());
        int inventoryX=menu.getSlot(2).x,inventoryY=menu.getSlot(2).y;
        ctx.assertTrue(menu.tree().stream().anyMatch(node->node.id().equals("minecraft:sharpness")),"equipment selects the Sword tree");
        ctx.assertTrue(!menu.getSlot(1).isActive(),"Book input is hidden in Enchantment mode");
        ctx.assertTrue(menu.quickMoveStack(player,2).isEmpty()&&menu.getSlot(1).getItem().isEmpty(),"hidden Book input cannot receive quick-moved books");
        ctx.assertTrue(menu.clickMenuButton(player,1)&&menu.getSlot(1).isActive(),"Book Option mode exposes its compatible book slot");
        ctx.assertTrue(menu.quickMoveStack(player,2).getItem()==Items.ENCHANTED_BOOK,"eligible book moves into the visible input");
        ctx.assertTrue(T1Operations.book(menu.getSlot(1).getItem()).orElseThrow().id().equals("minecraft:sharpness"),"book input contains the compatible source book");
        ctx.assertTrue(menu.getSlot(2).x==inventoryX&&menu.getSlot(2).y==inventoryY,"player inventory placement stays fixed across tabs");
        ctx.succeed();
    }
    @GameTest public void anvilModesAndBookCombination(GameTestHelper ctx){
        var player=ctx.makeMockServerPlayer(net.minecraft.world.level.GameType.SURVIVAL);player.giveExperiencePoints(10000);
        var menu=new AnvilMenu(1,player.getInventory());var modes=(T1AnvilModes)menu;
        menu.getSlot(0).set(new ItemStack(Items.DIAMOND_SWORD));menu.getSlot(1).set(new ItemStack(ModItems.ENHANCEMENT_STONE));menu.createResult();
        ctx.assertTrue(!menu.getSlot(2).hasItem(),"Enhance mode has no result slot item");
        menu.getSlot(1).set(new ItemStack(Items.DIRT));menu.createResult();ctx.assertTrue(!menu.getSlot(2).hasItem(),"Enhance never produces output from unrelated materials");
        modes.moreminecrafts$button(player,1);menu.getSlot(1).set(book(player,"minecraft:sharpness",1));menu.createResult();
        ctx.assertTrue(!menu.getSlot(2).hasItem(),"normal anvil blocks direct book enchantment");
        modes.moreminecrafts$button(player,2);menu.getSlot(0).set(book(player,"minecraft:sharpness",1));menu.getSlot(1).set(book(player,"minecraft:sharpness",1));menu.createResult();
        ctx.assertTrue(T1Operations.book(menu.getSlot(2).getItem()).orElseThrow().level()==2,"equal books combine one level");
        int before=RawXp.available(player);menu.getSlot(2).onTake(player,menu.getSlot(2).getItem().copy());
        ctx.assertTrue(RawXp.available(player)==before-200,"book combination pays raw XP exactly once");
        ctx.assertTrue(menu.getSlot(0).getItem().isEmpty()&&menu.getSlot(1).getItem().isEmpty(),"combination consumes inputs");
        ctx.succeed();
    }

    @GameTest public void combineBooksActionButtonDeliversOnePaidResultAndOneAnvilUse(GameTestHelper ctx){
        var player=ctx.makeMockServerPlayer(net.minecraft.world.level.GameType.SURVIVAL);
        player.giveExperiencePoints(10000);
        var pos=ctx.absolutePos(new net.minecraft.core.BlockPos(1,1,1));
        ctx.getLevel().setBlock(pos,net.minecraft.world.level.block.Blocks.ANVIL.defaultBlockState(),3);
        player.setPos(pos.getX()+.5,pos.getY()+1,pos.getZ()+.5);
        var menu=new AnvilMenu(1,player.getInventory(),ContainerLevelAccess.create(ctx.getLevel(),pos));
        var modes=(T1AnvilModes)menu;
        ctx.assertTrue(modes.moreminecrafts$button(player,2),"action 2 selects Combine Books mode");
        menu.getSlot(0).set(book(player,"minecraft:sharpness",2));
        menu.getSlot(1).set(book(player,"minecraft:sharpness",2));
        menu.createResult();
        ctx.assertTrue(T1Operations.book(menu.getSlot(2).getItem()).orElseThrow().level()==3,"equal retained books produce the valid next level");
        int rawXpBefore=RawXp.available(player);
        long wearSeed=anvilWearSeed(player,2);
        player.getRandom().setSeed(wearSeed);

        ctx.assertTrue(modes.moreminecrafts$button(player,3),"action 3 takes the result through the real quick-move path");

        ctx.assertTrue(menu.getSlot(0).getItem().isEmpty()&&menu.getSlot(1).getItem().isEmpty(),"one source book from each input is consumed");
        ctx.assertTrue(menu.getSlot(2).getItem().isEmpty(),"result slot clears after delivery");
        ctx.assertTrue(RawXp.available(player)==rawXpBefore-300,"combination consumes its raw XP formula exactly once");
        ctx.assertTrue(player.getInventory().countItem(Items.ENCHANTED_BOOK)==1,"one upgraded book is delivered with no duplicate");
        ItemStack delivered=ItemStack.EMPTY;
        for(int slot=0;slot<player.getInventory().getContainerSize();slot++){
            ItemStack candidate=player.getInventory().getItem(slot);
            if(candidate.is(Items.ENCHANTED_BOOK)){delivered=candidate;break;}
        }
        ctx.assertTrue(T1Operations.book(delivered).orElseThrow().level()==3,"delivered book has the upgraded level");
        ctx.assertTrue(ctx.getLevel().getBlockState(pos).is(net.minecraft.world.level.block.Blocks.CHIPPED_ANVIL),
                "one successful button operation performs exactly one deterministic Anvil wear roll");
        ctx.succeed();
    }

    @GameTest public void enhancementButtonMutatesInputAndSupportsConsecutiveUses(GameTestHelper ctx){
        var player=ctx.makeMockServerPlayer(net.minecraft.world.level.GameType.SURVIVAL);player.giveExperienceLevels(3);
        var pos=ctx.absolutePos(new net.minecraft.core.BlockPos(1,1,1));
        ctx.getLevel().setBlock(pos,net.minecraft.world.level.block.Blocks.ANVIL.defaultBlockState(),3);
        player.setPos(pos.getX()+.5,pos.getY()+1,pos.getZ()+.5);
        var menu=new AnvilMenu(1,player.getInventory(),ContainerLevelAccess.create(ctx.getLevel(),pos));var modes=(T1AnvilModes)menu;
        menu.getSlot(0).set(new ItemStack(Items.DIAMOND_SWORD));
        menu.getSlot(1).set(new ItemStack(ModItems.ENHANCEMENT_STONE,2));
        menu.createResult();
        int levelsBefore=player.experienceLevel;
        player.getRandom().setSeed(anvilWearSeed(player,4));
        ctx.assertTrue(modes.moreminecrafts$button(player,3),"first Enhance click succeeds");
        ctx.assertTrue(EnhancementOperation.level(menu.getSlot(0).getItem())==1,"equipment remains in its input slot at +1");
        ctx.assertTrue(menu.getSlot(1).getItem().getCount()==1,"first click consumes one stone from the input stack");
        ctx.assertTrue(menu.getSlot(2).getItem().isEmpty(),"Enhance mode does not create a result item");
        ctx.assertTrue(player.experienceLevel==levelsBefore-1,"first click consumes exactly one XP level");
        ctx.assertTrue(modes.moreminecrafts$button(player,3),"second Enhance click succeeds without reinsertion");
        ctx.assertTrue(EnhancementOperation.level(menu.getSlot(0).getItem())==2,"same input equipment advances a second step");
        ctx.assertTrue(menu.getSlot(1).getItem().isEmpty(),"second click consumes the remaining stone");
        ctx.assertTrue(player.experienceLevel==levelsBefore-2,"each click consumes one XP level");
        ctx.assertTrue(ctx.getLevel().getBlockState(pos).is(net.minecraft.world.level.block.Blocks.DAMAGED_ANVIL),
                "two successful clicks count as exactly two Anvil uses");

        menu.getSlot(0).getItem().set(ModDataComponents.ENHANCEMENT_LEVEL,20);
        menu.getSlot(1).set(new ItemStack(ModItems.ENHANCEMENT_STONE,1));
        int atMaximum=player.experienceLevel;
        ctx.getLevel().setBlock(pos,net.minecraft.world.level.block.Blocks.ANVIL.defaultBlockState(),3);
        player.getRandom().setSeed(anvilWearSeed(player,1));
        ctx.assertTrue(!modes.moreminecrafts$button(player,3),"+20 rejects another enhancement");
        ctx.assertTrue(menu.getSlot(1).getItem().getCount()==1,"rejected maximum-level action keeps its stone");
        ctx.assertTrue(player.experienceLevel==atMaximum,"rejected maximum-level action keeps XP levels");
        ctx.assertTrue(ctx.getLevel().getBlockState(pos).is(net.minecraft.world.level.block.Blocks.ANVIL),"+20 rejection causes no Anvil wear");

        var poorPlayer=ctx.makeMockServerPlayer(net.minecraft.world.level.GameType.SURVIVAL);
        poorPlayer.setPos(pos.getX()+.5,pos.getY()+1,pos.getZ()+.5);
        poorPlayer.giveExperiencePoints(5);
        var poorMenu=new AnvilMenu(2,poorPlayer.getInventory(),ContainerLevelAccess.create(ctx.getLevel(),pos));var poorModes=(T1AnvilModes)poorMenu;
        poorMenu.getSlot(0).set(new ItemStack(Items.IRON_SWORD));
        poorMenu.getSlot(1).set(new ItemStack(ModItems.ENHANCEMENT_STONE,1));
        int poorRawXp=RawXp.available(poorPlayer);
        poorPlayer.getRandom().setSeed(anvilWearSeed(poorPlayer,1));
        ctx.assertTrue(!poorModes.moreminecrafts$button(poorPlayer,3),"fractional raw XP without a full level rejects the transaction");
        ctx.assertTrue(EnhancementOperation.level(poorMenu.getSlot(0).getItem())==0
                &&poorMenu.getSlot(1).getItem().getCount()==1,"insufficient XP levels preserve equipment and stone");
        ctx.assertTrue(RawXp.available(poorPlayer)==poorRawXp,"insufficient XP leaves raw XP untouched");
        ctx.assertTrue(ctx.getLevel().getBlockState(pos).is(net.minecraft.world.level.block.Blocks.ANVIL),"insufficient XP causes no Anvil wear");

        var stonePoorPlayer=ctx.makeMockServerPlayer(net.minecraft.world.level.GameType.SURVIVAL);stonePoorPlayer.giveExperienceLevels(3);
        stonePoorPlayer.setPos(pos.getX()+.5,pos.getY()+1,pos.getZ()+.5);
        var stonePoorMenu=new AnvilMenu(3,stonePoorPlayer.getInventory(),ContainerLevelAccess.create(ctx.getLevel(),pos));
        var stonePoorModes=(T1AnvilModes)stonePoorMenu;
        stonePoorMenu.getSlot(0).set(new ItemStack(Items.IRON_SWORD));
        stonePoorMenu.getSlot(0).getItem().set(ModDataComponents.ENHANCEMENT_LEVEL,4);
        stonePoorMenu.getSlot(1).set(new ItemStack(ModItems.ENHANCEMENT_STONE,1));
        int stonePoorLevel=stonePoorPlayer.experienceLevel;
        stonePoorPlayer.getRandom().setSeed(anvilWearSeed(stonePoorPlayer,1));
        ctx.assertTrue(!stonePoorModes.moreminecrafts$button(stonePoorPlayer,3),"milestone rejects a short Enhancement Stone stack");
        ctx.assertTrue(EnhancementOperation.level(stonePoorMenu.getSlot(0).getItem())==4
                &&stonePoorMenu.getSlot(1).getItem().getCount()==1&&stonePoorPlayer.experienceLevel==stonePoorLevel,
                "insufficient stones preserve equipment, material and XP levels");
        ctx.assertTrue(ctx.getLevel().getBlockState(pos).is(net.minecraft.world.level.block.Blocks.ANVIL),"insufficient stones cause no Anvil wear");

        var creative=ctx.makeMockServerPlayer(net.minecraft.world.level.GameType.CREATIVE);creative.giveExperienceLevels(2);
        creative.setPos(pos.getX()+.5,pos.getY()+1,pos.getZ()+.5);
        var creativeMenu=new AnvilMenu(4,creative.getInventory(),ContainerLevelAccess.create(ctx.getLevel(),pos));
        var creativeModes=(T1AnvilModes)creativeMenu;
        creativeMenu.getSlot(0).set(new ItemStack(Items.IRON_SWORD));
        creativeMenu.getSlot(1).set(new ItemStack(ModItems.ENHANCEMENT_STONE,1));
        int creativeLevels=creative.experienceLevel;
        creative.getRandom().setSeed(anvilWearSeed(creative,1));
        ctx.assertTrue(creativeModes.moreminecrafts$button(creative,3),"creative Enhancement remains available");
        ctx.assertTrue(creative.experienceLevel==creativeLevels,"creative Enhancement does not spend XP levels");
        ctx.assertTrue(creativeMenu.getSlot(1).getItem().isEmpty(),"creative Enhancement follows existing material-consumption conventions");
        ctx.assertTrue(ctx.getLevel().getBlockState(pos).is(net.minecraft.world.level.block.Blocks.ANVIL),"creative player remains exempt from Anvil wear");
        ctx.succeed();
    }
    @GameTest public void supplyFilteringAndSmithInputs(GameTestHelper ctx){
        var player=ctx.makeMockServerPlayer(net.minecraft.world.level.GameType.SURVIVAL);
        ctx.assertTrue(T1Supply.filterLoot(book(player,"minecraft:looting",1),()->0).isEmpty(),"removed book filtered even at successful rarity roll");
        ctx.assertTrue(T1Supply.filterLoot(book(player,"minecraft:sharpness",1),()->.004999).getItem()==Items.ENCHANTED_BOOK,"0.5% enchanted candidate retained below boundary");
        ctx.assertTrue(T1Supply.filterLoot(book(player,"minecraft:sharpness",1),()->.005).isEmpty(),"candidate at 0.5% boundary is rejected");
        ctx.assertTrue(T1Supply.retainNaturalEnchantedCandidate(.004999)&&!T1Supply.retainNaturalEnchantedCandidate(.005),"natural enchanted candidate retention is 0.005");
        ctx.assertTrue(T1Supply.retainLibrarianBookCandidate(.009999)&&!T1Supply.retainLibrarianBookCandidate(.01),"Librarian retention is 1% of eligible book candidates");
        ctx.assertTrue(T1Balance.LIBRARIAN_MAX_USES==1&&T1Balance.LIBRARIAN_EMERALDS==64,"retained Librarian offer has fixed base price and one use");
        MerchantOffers librarianOffers=new MerchantOffers();
        MerchantOffer librarianBook=new MerchantOffer(new ItemCost(Items.EMERALD,64),Optional.of(new ItemCost(Items.BOOK)),book(player,"minecraft:sharpness",1),1,1,.2f);
        librarianBook.setToOutOfStock();librarianBook.updateDemand();librarianBook.setSpecialPriceDiff(-63);librarianOffers.add(librarianBook);
        T1Supply.enforceFixedBookPrices(librarianOffers);
        MerchantOffer fixedBook=librarianOffers.get(0);
        ctx.assertTrue(fixedBook.getCostA().getCount()==64&&fixedBook.getPriceMultiplier()==0
                &&fixedBook.getDemand()==0&&fixedBook.getSpecialPriceDiff()==0,
                "discount and demand cannot lower the fixed 64-Emerald price");
        fixedBook.addToSpecialPriceDiff(-64);fixedBook.updateDemand();T1Supply.enforceFixedBookPrices(librarianOffers);
        ctx.assertTrue(librarianOffers.get(0).getCostA().getCount()==64,"repeated Hero/reputation/demand updates stay fixed");
        MerchantOffer old=new MerchantOffer(new ItemCost(Items.EMERALD,10),new ItemStack(Items.DIAMOND_SWORD),3,10,.05f);
        MerchantOffer offer=T1Supply.smithOffer(player,old);
        ctx.assertTrue(!offer.satisfiedBy(new ItemStack(Items.EMERALD,64),new ItemStack(Items.EMERALD,64)),"emerald-only bypass closed");
        ItemStack damaged=new ItemStack(Items.DIAMOND_SWORD);damaged.setDamageValue(1);
        ctx.assertTrue(!offer.satisfiedBy(damaged,new ItemStack(Items.EMERALD,64)),"smith requires undamaged equipment");
        ctx.assertTrue(offer.satisfiedBy(new ItemStack(Items.DIAMOND_SWORD),new ItemStack(Items.EMERALD,32)),"smith accepts corresponding intact equipment and emeralds");
        ctx.succeed();
    }
    @GameTest public void bonusHooksAndTooltip(GameTestHelper ctx){
        var player=ctx.makeMockServerPlayer(net.minecraft.world.level.GameType.SURVIVAL);
        ItemStack crossbow=new ItemStack(Items.CROSSBOW);
        EnchantmentHelper.updateEnchantments(crossbow,e->{e.set(T1Operations.enchantment(player,"minecraft:quick_charge").orElseThrow(),3);e.set(T1Operations.enchantment(player,"minecraft:piercing").orElseThrow(),4);});
        float normal=EnchantmentHelper.modifyCrossbowChargingTime(crossbow,player,1.25f);
        int piercing=EnchantmentHelper.getPiercingCount(ctx.getLevel(),crossbow,new ItemStack(Items.ARROW));
        crossbow.set(ModDataComponents.BOOK_OPTIONS,Map.of("minecraft:quick_charge",3,"minecraft:piercing",4));
        ctx.assertTrue(Math.abs(EnchantmentHelper.modifyCrossbowChargingTime(crossbow,player,1.25f)-(normal-.3f))<.0001,"Quick Charge subtracts 0.1 seconds per option");
        ctx.assertTrue(EnchantmentHelper.getPiercingCount(ctx.getLevel(),crossbow,new ItemStack(Items.ARROW))==piercing+4,"Piercing adds entity penetration");
        ItemStack bow=new ItemStack(Items.BOW);
        EnchantmentHelper.updateEnchantments(bow,e->e.set(T1Operations.enchantment(player,"minecraft:power").orElseThrow(),1));
        var arrow=net.minecraft.world.entity.EntityTypes.ARROW.create(ctx.getLevel(),net.minecraft.world.entity.EntitySpawnReason.COMMAND);
        var projectileSource=ctx.getLevel().damageSources().arrow(arrow,player);
        var damageTarget=net.minecraft.world.entity.EntityTypes.ZOMBIE.create(ctx.getLevel(),net.minecraft.world.entity.EntitySpawnReason.COMMAND);
        float bowProjectileBase=EnchantmentHelper.modifyDamage(ctx.getLevel(),bow,damageTarget,projectileSource,20.0f);
        float bowMeleeBase=EnchantmentHelper.modifyDamage(ctx.getLevel(),bow,damageTarget,
                ctx.getLevel().damageSources().playerAttack(player),20.0f);
        bow.set(ModDataComponents.BOOK_OPTIONS,Map.of("minecraft:power",2));
        float bowProjectileDamage=EnchantmentHelper.modifyDamage(ctx.getLevel(),bow,
                net.minecraft.world.entity.EntityTypes.ZOMBIE.create(ctx.getLevel(),net.minecraft.world.entity.EntitySpawnReason.COMMAND),projectileSource,20.0f);
        float bowMeleeDamage=EnchantmentHelper.modifyDamage(ctx.getLevel(),bow,
                net.minecraft.world.entity.EntityTypes.ZOMBIE.create(ctx.getLevel(),net.minecraft.world.entity.EntitySpawnReason.COMMAND),
                ctx.getLevel().damageSources().playerAttack(player),20.0f);
        ctx.assertTrue(Math.abs((bowProjectileDamage-bowProjectileBase)-2.2f)<.001f,"Power Book Option adds 5.5% per level to Bow projectile damage");
        ctx.assertTrue(Math.abs(bowMeleeDamage-bowMeleeBase)<.001f,"holding a Bow does not grant Power Option damage to melee hits");
        ItemStack windBow=new ItemStack(ModItems.WIND_ARCHERS_BOW);
        EnchantmentHelper.updateEnchantments(windBow,e->e.set(T1Operations.enchantment(player,"minecraft:power").orElseThrow(),1));
        float windProjectileBase=EnchantmentHelper.modifyDamage(ctx.getLevel(),windBow,damageTarget,projectileSource,20.0f);
        windBow.set(ModDataComponents.BOOK_OPTIONS,Map.of("minecraft:power",2));
        float windProjectileDamage=EnchantmentHelper.modifyDamage(ctx.getLevel(),windBow,
                damageTarget,projectileSource,20.0f);
        ctx.assertTrue(Math.abs((windProjectileDamage-windProjectileBase)-2.2f)<.001f,"Wind Bow profile receives the same Power projectile bonus");
        ItemStack sword=new ItemStack(Items.DIAMOND_SWORD);
        EnchantmentHelper.updateEnchantments(sword,e->{e.set(T1Operations.enchantment(player,"minecraft:mending").orElseThrow(),1);e.set(T1Operations.enchantment(player,"minecraft:sharpness").orElseThrow(),5);e.set(T1Operations.enchantment(player,"minecraft:fire_aspect").orElseThrow(),2);});
        ctx.assertTrue(EnchantmentHelper.modifyDurabilityToRepairFromXp(ctx.getLevel(),sword,1)==2,"vanilla mending remains 2 durability/XP");
        sword.set(ModDataComponents.BOOK_OPTIONS,Map.of("minecraft:mending",1,"minecraft:sharpness",4,"minecraft:fire_aspect",2));
        ctx.assertTrue(EnchantmentHelper.modifyDurabilityToRepairFromXp(ctx.getLevel(),sword,1)==4,"option mending repairs 4 durability/XP");
        List<net.minecraft.network.chat.Component> lines=new ArrayList<>();
        EnchantmentHelper.getEnchantmentsForCrafting(sword).addToTooltip(Item.TooltipContext.of(ctx.getLevel()),lines::add,TooltipFlag.NORMAL,sword);
        ctx.assertTrue(lines.stream().anyMatch(line->line.getString().contains("(+IV)")),"tooltip adds option next to enchantment");
        var target=net.minecraft.world.entity.EntityTypes.ZOMBIE.create(ctx.getLevel(),net.minecraft.world.entity.EntitySpawnReason.COMMAND);
        new net.minecraft.world.item.enchantment.effects.Ignite(LevelBasedValue.constant(8)).apply(ctx.getLevel(),2,new EnchantedItemInUse(sword,null,null,ignored->{}),target,target.position());
        ctx.assertTrue(target.getRemainingFireTicks()==240,"Fire Aspect adds four seconds to vanilla eight seconds");
        ctx.succeed();
    }
    @GameTest public void insufficientXpAndContextualEquipment(GameTestHelper ctx){
        var player=ctx.makeMockServerPlayer(net.minecraft.world.level.GameType.SURVIVAL);
        ctx.assertTrue(EquipmentTreeResolver.resolve(new ItemStack(Items.NETHERITE_SWORD))==EnchantmentTrees.Equipment.SWORD,"vanilla sword resolves by item category");
        ctx.assertTrue(EquipmentTreeResolver.resolve(new ItemStack(Items.NETHERITE_PICKAXE))==EnchantmentTrees.Equipment.PICKAXE,"vanilla pickaxe resolves by item identity");
        ctx.assertTrue(EquipmentTreeResolver.resolve(new ItemStack(Items.BOW))==EnchantmentTrees.Equipment.BOW,"vanilla bow resolves by item identity");
        ctx.assertTrue(EquipmentTreeResolver.resolve(new ItemStack(Items.DIAMOND_BOOTS))==EnchantmentTrees.Equipment.BOOTS,"vanilla armor resolves by exact slot family");
        ItemStack sword=new ItemStack(Items.DIAMOND_SWORD);
        ctx.assertTrue(!T1Operations.purchase(player,sword,"minecraft:sharpness"),"insufficient XP rejects purchase");
        ctx.assertTrue(T1Operations.enchantments(sword).isEmpty(),"failed purchase does not mutate item");
        var ironDagger=ModItems.DAGGERS.get(dev.itzgamesgd.moreminecrafts.equipment.WeaponBalance.Tier.IRON);
        ctx.assertTrue(EquipmentTreeResolver.resolve(new ItemStack(ironDagger))==EnchantmentTrees.Equipment.SWORD,
                "dagger profile for "+net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(ironDagger)+" is "+EquipmentTreeResolver.resolve(new ItemStack(ironDagger)));
        ctx.assertTrue(new ItemStack(ModItems.DAGGERS.get(dev.itzgamesgd.moreminecrafts.equipment.WeaponBalance.Tier.IRON)).isDamageableItem(),"tiered basic weapons need functional durability branches");
        ctx.assertTrue(dev.itzgamesgd.moreminecrafts.enhancement.EnhancementCatalog.definition(ModItems.DAGGERS.get(dev.itzgamesgd.moreminecrafts.equipment.WeaponBalance.Tier.IRON)).isPresent(),"T1 basic weapon supports parallel Enhancement");
        ctx.assertTrue(EquipmentTreeResolver.resolve(new ItemStack(ModItems.GREATSWORDS.get(dev.itzgamesgd.moreminecrafts.equipment.WeaponBalance.Tier.IRON)))==EnchantmentTrees.Equipment.SWORD,"greatsword uses Sword tree");
        ctx.assertTrue(EquipmentTreeResolver.resolve(new ItemStack(ModItems.FLAME_SWORD))==EnchantmentTrees.Equipment.SWORD,"T2 Flame Sword uses shared Sword profile");
        ctx.assertTrue(EquipmentTreeResolver.resolve(new ItemStack(ModItems.WIND_SWORD))==EnchantmentTrees.Equipment.SWORD,"T2 Wind Sword uses shared Sword profile");
        ctx.assertTrue(EquipmentTreeResolver.resolve(new ItemStack(ModItems.FLOWING_WATER_SWORD))==EnchantmentTrees.Equipment.SWORD,"T2 Water Sword uses shared Sword profile");
        ctx.assertTrue(EquipmentTreeResolver.resolve(new ItemStack(ModItems.LIGHTNING_SWORD))==EnchantmentTrees.Equipment.SWORD,"T2 Lightning Sword uses shared Sword profile");
        ctx.assertTrue(EquipmentTreeResolver.resolve(new ItemStack(ModItems.WIND_ARCHERS_BOW))==EnchantmentTrees.Equipment.BOW,"T2 Wind Bow uses shared Bow profile");
        ctx.assertTrue(EquipmentTreeResolver.resolve(new ItemStack(ModItems.EARTH_MACE))==EnchantmentTrees.Equipment.MACE,"T2 Earth Mace uses shared Mace profile");
        ctx.assertTrue(EquipmentTreeResolver.resolve(new ItemStack(ModItems.DARK_ENERGY_STAFF))==EnchantmentTrees.Equipment.NONE,"deferred Staff profile stays unassigned");
        var menu=new AnvilMenu(1,player.getInventory());((T1AnvilModes)menu).moreminecrafts$button(player,2);
        menu.getSlot(0).set(book(player,"minecraft:sharpness",1));menu.getSlot(1).set(book(player,"minecraft:sharpness",1));menu.createResult();
        ctx.assertTrue(menu.getSlot(2).hasItem()&&!menu.getSlot(2).mayPickup(player),"combination preview requires XP before pickup");
        ctx.succeed();
    }
    @GameTest public void workstationProviderAndEquipmentChanges(GameTestHelper ctx){
        var player=ctx.makeMockServerPlayer(net.minecraft.world.level.GameType.SURVIVAL);
        var pos=ctx.absolutePos(new net.minecraft.core.BlockPos(1,1,1));
        ctx.getLevel().setBlock(pos,net.minecraft.world.level.block.Blocks.ENCHANTING_TABLE.defaultBlockState(),3);
        var provider=ctx.getLevel().getBlockState(pos).getMenuProvider(ctx.getLevel(),pos);
        var container=provider.createMenu(2,player.getInventory(),player);
        ctx.assertTrue(container instanceof EnchantingProgressionMenu,"all enchanting table interactions replace random enchanting");
        var menu=(EnchantingProgressionMenu)container;
        ctx.assertTrue(menu.tree().isEmpty(),"no equipment means no category tree");
        menu.getSlot(0).set(new ItemStack(Items.DIAMOND_SWORD));
        ctx.assertTrue(menu.tree().stream().anyMatch(n->n.id().equals("minecraft:sharpness")),"equipment slot shows sword branches");
        menu.getSlot(0).set(new ItemStack(Items.DIAMOND_BOOTS));
        ctx.assertTrue(menu.tree().stream().noneMatch(n->n.id().equals("minecraft:sharpness")),"changing equipment removes unrelated branches");
        ctx.succeed();
    }
    @GameTest public void normalAnvilCannotMergeProgressionOrConflicts(GameTestHelper ctx){
        var player=ctx.makeMockServerPlayer(net.minecraft.world.level.GameType.SURVIVAL);
        var menu=new AnvilMenu(1,player.getInventory());((T1AnvilModes)menu).moreminecrafts$button(player,1);
        ItemStack first=new ItemStack(Items.CROSSBOW),second=new ItemStack(Items.CROSSBOW);first.setDamageValue(50);
        EnchantmentHelper.updateEnchantments(first,e->e.set(T1Operations.enchantment(player,"moreminecrafts:crossbow_barrage").orElseThrow(),1));
        EnchantmentHelper.updateEnchantments(second,e->e.set(T1Operations.enchantment(player,"minecraft:multishot").orElseThrow(),1));
        menu.getSlot(0).set(first);menu.getSlot(1).set(second);menu.createResult();
        ctx.assertTrue(menu.getSlot(2).hasItem(),"normal anvil still repairs equipment");
        ctx.assertTrue(!T1Operations.enchantments(menu.getSlot(2).getItem()).containsKey("minecraft:multishot"),"repair cannot introduce structural conflict from sacrificial equipment");
        ctx.assertTrue(T1Operations.enchantments(menu.getSlot(2).getItem()).containsKey("moreminecrafts:crossbow_barrage"),"first item's progression is preserved");
        ctx.succeed();
    }

    @GameTest public void normalAnvilRepairAndRenamePreservesProgressedItemState(GameTestHelper ctx){
        var player=ctx.makeMockServerPlayer(net.minecraft.world.level.GameType.SURVIVAL);
        var menu=new AnvilMenu(1,player.getInventory());
        ((T1AnvilModes)menu).moreminecrafts$button(player,1);
        ItemStack first=new ItemStack(Items.DIAMOND_SWORD);first.setDamageValue(800);
        first.set(ModDataComponents.ENHANCEMENT_LEVEL,7);
        first.set(ModDataComponents.BOOK_OPTIONS,Map.of("minecraft:sharpness",4));
        first.set(net.minecraft.core.component.DataComponents.CUSTOM_NAME,net.minecraft.network.chat.Component.literal("Old name"));
        EnchantmentHelper.updateEnchantments(first,e->e.set(T1Operations.enchantment(player,"minecraft:sharpness").orElseThrow(),3));
        ItemStack sacrifice=new ItemStack(Items.DIAMOND_SWORD);sacrifice.setDamageValue(1300);
        EnchantmentHelper.updateEnchantments(sacrifice,e->e.set(T1Operations.enchantment(player,"minecraft:smite").orElseThrow(),5));
        menu.getSlot(0).set(first);menu.getSlot(1).set(sacrifice);menu.setItemName("Reforged Blade");menu.createResult();
        ItemStack output=menu.getSlot(2).getItem();

        ctx.assertTrue(!output.isEmpty()&&output.getDamageValue()<first.getDamageValue(),"ordinary Anvil repair succeeds");
        ctx.assertTrue(output.getOrDefault(ModDataComponents.ENHANCEMENT_LEVEL,0)==7,"Enhancement level survives repair and rename");
        ctx.assertTrue(T1Operations.options(output).equals(Map.of("minecraft:sharpness",4)),"Book Options survive repair and rename");
        ctx.assertTrue(T1Operations.enchantments(output).getOrDefault("minecraft:sharpness",0)==3,"first item's authorized enchantment survives");
        ctx.assertTrue(!T1Operations.enchantments(output).containsKey("minecraft:smite"),"sacrificial equipment enchantments do not transfer");
        ctx.assertTrue(output.get(net.minecraft.core.component.DataComponents.CUSTOM_NAME).getString().equals("Reforged Blade"),"requested rename becomes the output's custom name");
        ctx.succeed();
    }
    @GameTest public void smithUpgradePreservesSubmittedProgress(GameTestHelper ctx){
        var player=ctx.makeMockServerPlayer(net.minecraft.world.level.GameType.SURVIVAL);
        var villager=net.minecraft.world.entity.EntityTypes.VILLAGER.create(ctx.getLevel(),net.minecraft.world.entity.EntitySpawnReason.COMMAND);
        villager.setVillagerData(villager.getVillagerData().withProfession(ctx.getLevel().registryAccess().lookupOrThrow(net.minecraft.core.registries.Registries.VILLAGER_PROFESSION).getOrThrow(net.minecraft.world.entity.npc.villager.VillagerProfession.WEAPONSMITH)));
        var offer=T1Supply.smithOffer(player,new MerchantOffer(new ItemCost(Items.EMERALD,32),new ItemStack(Items.DIAMOND_SWORD),3,10,0));
        MerchantOffers offers=villager.getOffers();offers.clear();offers.add(offer);
        ItemStack submitted=new ItemStack(Items.DIAMOND_SWORD);submitted.set(ModDataComponents.ENHANCEMENT_LEVEL,20);
        EnchantmentHelper.updateEnchantments(submitted,e->e.set(T1Operations.enchantment(player,"minecraft:sharpness").orElseThrow(),1));
        submitted.set(ModDataComponents.BOOK_OPTIONS,Map.of("minecraft:sharpness",4));
        MerchantContainer container=new MerchantContainer(villager);container.setItem(0,submitted);container.setItem(1,new ItemStack(Items.EMERALD,32));
        ItemStack output=container.getItem(2);
        ctx.assertTrue(output.getOrDefault(ModDataComponents.ENHANCEMENT_LEVEL,0)==20,"smith preserves Enhancement");
        ctx.assertTrue(T1Operations.option(output,"sharpness")==4,"smith preserves Book Options");
        ctx.assertTrue(T1Operations.enchantments(output).get("minecraft:sharpness")==2,"valid smith upgrade raises inferior Sharpness");
        ctx.succeed();
    }
    @GameTest public void smithRejectsEqualAndSuperiorNoOpOffers(GameTestHelper ctx){
        var player=ctx.makeMockServerPlayer(net.minecraft.world.level.GameType.SURVIVAL);
        var villager=net.minecraft.world.entity.EntityTypes.VILLAGER.create(ctx.getLevel(),net.minecraft.world.entity.EntitySpawnReason.COMMAND);
        villager.setVillagerData(villager.getVillagerData().withProfession(ctx.getLevel().registryAccess().lookupOrThrow(net.minecraft.core.registries.Registries.VILLAGER_PROFESSION).getOrThrow(net.minecraft.world.entity.npc.villager.VillagerProfession.WEAPONSMITH)));
        villager.getOffers().clear();
        var offer=T1Supply.smithOffer(ctx.getLevel().registryAccess(),
                new MerchantOffer(new ItemCost(Items.EMERALD,32),new ItemStack(Items.DIAMOND_SWORD),3,10,0),
                new ItemStack(Items.DIAMOND_SWORD));
        villager.getOffers().add(offer);
        for(int level:new int[]{2,5}){
            ItemStack submitted=new ItemStack(Items.DIAMOND_SWORD);
            EnchantmentHelper.updateEnchantments(submitted,e->e.set(T1Operations.enchantment(player,"minecraft:sharpness").orElseThrow(),level));
            MerchantContainer container=new MerchantContainer(villager);
            container.setItem(0,submitted);container.setItem(1,new ItemStack(Items.EMERALD,32));
            ctx.assertTrue(container.getItem(2).isEmpty(),"equal/superior offer is hidden");
            ctx.assertTrue(container.getItem(0).getCount()==1&&container.getItem(1).getCount()==32,"rejected no-op consumes neither equipment nor emeralds");
        }
        ctx.succeed();
    }
    @GameTest public void bookCombinationIsOneAnvilUse(GameTestHelper ctx){
        var player=ctx.makeMockServerPlayer(net.minecraft.world.level.GameType.SURVIVAL);player.giveExperiencePoints(10000);
        var pos=ctx.absolutePos(new net.minecraft.core.BlockPos(1,1,1));
        ctx.getLevel().setBlock(pos,net.minecraft.world.level.block.Blocks.ANVIL.defaultBlockState(),3);
        player.setPos(pos.getX()+.5,pos.getY()+1,pos.getZ()+.5);
        var access=ContainerLevelAccess.create(ctx.getLevel(),pos);
        var menu=new AnvilMenu(1,player.getInventory(),access);
        ctx.assertTrue(((T1AnvilModes)menu).moreminecrafts$button(player,2),"in-range player selects Book Combination mode");
        ctx.assertTrue(((T1AnvilModes)menu).moreminecrafts$mode()==2,"Book Combination mode is active");
        menu.getSlot(0).set(book(player,"minecraft:sharpness",1));menu.getSlot(1).set(book(player,"minecraft:sharpness",1));menu.createResult();
        int before=RawXp.available(player);
        menu.getSlot(2).onTake(player,menu.getSlot(2).getItem().copy());
        ctx.assertTrue(RawXp.available(player)==before-200,"custom combination deducts raw XP once: before="+before+" after="+RawXp.available(player)+" cost="+menu.getCost());
        ctx.assertTrue(menu.getSlot(0).getItem().isEmpty()&&menu.getSlot(1).getItem().isEmpty(),"each source book is consumed once");
        var state=ctx.getLevel().getBlockState(pos);
        ctx.assertTrue(state.is(net.minecraft.world.level.block.Blocks.ANVIL)
                ||state.is(net.minecraft.world.level.block.Blocks.CHIPPED_ANVIL)
                ||state.is(net.minecraft.world.level.block.Blocks.DAMAGED_ANVIL)
                ||state.isAir(),"successful combination applies vanilla anvil degradation/destruction path");
        ctx.succeed();
    }
    @GameTest public void normalRepairIgnoresSacrificialEnchantmentFees(GameTestHelper ctx){
        var player=ctx.makeMockServerPlayer(net.minecraft.world.level.GameType.SURVIVAL);
        var menu=new AnvilMenu(1,player.getInventory());((T1AnvilModes)menu).moreminecrafts$button(player,1);
        ItemStack first=new ItemStack(Items.DIAMOND_SWORD),second=new ItemStack(Items.DIAMOND_SWORD);first.setDamageValue(100);
        EnchantmentHelper.updateEnchantments(first,e->e.set(T1Operations.enchantment(player,"minecraft:sharpness").orElseThrow(),5));
        EnchantmentHelper.updateEnchantments(second,e->e.set(T1Operations.enchantment(player,"minecraft:smite").orElseThrow(),5));
        menu.getSlot(0).set(first);menu.getSlot(1).set(second);menu.createResult();
        ctx.assertTrue(menu.getSlot(2).hasItem(),"conflicting sacrifice enchantments do not block ordinary repair");
        ctx.assertTrue(menu.getCost()<=3,"repair does not charge for enchantments never transferred");
        ctx.assertTrue(T1Operations.enchantments(second).containsKey("minecraft:smite"),"calculation does not mutate sacrificial input");
        ctx.succeed();
    }
    @GameTest public void knowledgeRepairCounterTracksSuccessfulDamagedAnvilOutput(GameTestHelper ctx){
        var player=ctx.makeMockServerPlayerInLevel();
        var menu=new AnvilMenu(1,player.getInventory());
        ((T1AnvilModes)menu).moreminecrafts$button(player,1);
        ItemStack first=new ItemStack(Items.IRON_SWORD),second=new ItemStack(Items.IRON_SWORD);
        first.setDamageValue(100);second.setDamageValue(100);
        menu.getSlot(0).set(first);menu.getSlot(1).set(second);menu.createResult();
        ctx.assertTrue(menu.getSlot(2).hasItem(),"damaged swords produce a repair output");
        menu.getSlot(2).onTake(player,menu.getSlot(2).getItem().copy());
        ctx.assertTrue(KnowledgeRuntime.state(player).conditionProgress("anvil_repairs")==1,
                "taking a successful repaired output increments the repair counter once");
        ctx.succeed();
    }
    private static ItemStack book(net.minecraft.world.entity.player.Player player,String id,int level){
        var result=new ItemStack(Items.ENCHANTED_BOOK);EnchantmentHelper.updateEnchantments(result,e->e.set(T1Operations.enchantment(player,id).orElseThrow(),level));return result;
    }
    private static long anvilWearSeed(net.minecraft.world.entity.player.Player player,int consecutiveWearRolls){
        var random=player.getRandom();
        for(long seed=0;seed<1_000_000;seed++){
            random.setSeed(seed);
            boolean qualifies=true;
            for(int roll=0;roll<consecutiveWearRolls;roll++)qualifies&=random.nextDouble()<.12;
            if(qualifies){random.setSeed(seed);return seed;}
        }
        throw new AssertionError("Could not find deterministic Anvil wear seed");
    }
    @Override public void invokeTestMethod(GameTestHelper context,Method method)throws ReflectiveOperationException{method.invoke(this,context);}
}
