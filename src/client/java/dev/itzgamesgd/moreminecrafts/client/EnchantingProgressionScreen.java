package dev.itzgamesgd.moreminecrafts.client;

import java.util.*;
import dev.itzgamesgd.moreminecrafts.progression.*;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

/** Compact branch rows connected to a common equipment root; each button buys only the next level. */
public final class EnchantingProgressionScreen extends AbstractContainerScreen<EnchantingProgressionMenu> {
    private int page;
    private List<EnchantmentTrees.Node> visible=List.of();
    private final List<Button> rows=new ArrayList<>();
    private Button apply;
    public EnchantingProgressionScreen(EnchantingProgressionMenu menu,Inventory inventory,Component title){
        super(menu,inventory,title,330,284);inventoryLabelX=78;inventoryLabelY=190;
    }
    private void request(int action){minecraft.gameMode.handleInventoryButtonClick(menu.containerId,action);}
    @Override protected void init(){
        super.init();rows.clear();
        addRenderableWidget(Button.builder(Component.translatable("menu.moreminecrafts.enchantment"),b->{page=0;request(0);}).bounds(leftPos+8,topPos+8,154,20).build());
        addRenderableWidget(Button.builder(Component.translatable("menu.moreminecrafts.book_option"),b->{page=0;request(1);}).bounds(leftPos+168,topPos+8,154,20).build());
        for(int i=0;i<6;i++){final int row=i;rows.add(addRenderableWidget(Button.builder(Component.empty(),b->{
            if(row<visible.size()){int index=menu.tree().indexOf(visible.get(row));request((menu.mode()==0?100:200)+index);}
        }).bounds(leftPos+244,topPos+42+i*22,78,20).build()));}
        addRenderableWidget(Button.builder(Component.literal("<"),b->page=Math.max(0,page-1)).bounds(leftPos+36,topPos+174,20,18).build());
        addRenderableWidget(Button.builder(Component.literal(">"),b->page++).bounds(leftPos+60,topPos+174,20,18).build());
        apply=addRenderableWidget(Button.builder(Component.translatable("menu.moreminecrafts.apply_option"),b->request(2)).bounds(leftPos+86,topPos+174,236,18).build());
        refresh();
    }
    @Override protected void containerTick(){super.containerTick();refresh();}
    private void refresh(){
        var all=menu.tree().stream().filter(n->menu.mode()==0||BookOptionRules.retained(n.id())).toList();
        page=Math.clamp(page,0,Math.max(0,(all.size()-1)/6));visible=all.subList(Math.min(page*6,all.size()),Math.min(page*6+6,all.size()));
        var equipment=menu.getSlot(0).getItem();var owned=T1Operations.enchantments(equipment);
        for(int i=0;i<rows.size();i++){
            Button button=rows.get(i);button.visible=i<visible.size();if(!button.visible)continue;
            var node=visible.get(i);var q=EnchantmentTrees.quote(EquipmentTreeResolver.resolve(equipment),owned,node.id());
            if(menu.mode()==0){
                boolean registered=minecraft.level.registryAccess().lookupOrThrow(net.minecraft.core.registries.Registries.ENCHANTMENT).get(net.minecraft.resources.ResourceKey.create(net.minecraft.core.registries.Registries.ENCHANTMENT,net.minecraft.resources.Identifier.parse(node.id()))).isPresent();
                button.setMessage(q.allowed()?Component.literal(q.cost()+" XP"):Component.translatable("menu.moreminecrafts."+q.reason()));
                button.active=q.allowed()&&registered&&RawXp.canPay(minecraft.player,q.cost());
                String requirement=node.prerequisite().isEmpty()?"START":node.prerequisite()+" "+BookOptionRules.roman(node.prerequisiteLevel());
                button.setTooltip(Tooltip.create(Component.literal(requirement+"\n"+q.cost()+" XP"+(q.premium()>0?" (+"+q.premium()+" premium)":"")+(!registered?"\nDeferred function":""))));
            }else {
                button.setMessage(Component.translatable("menu.moreminecrafts.reset"));button.active=T1Operations.options(equipment).containsKey(node.id());
                button.setTooltip(Tooltip.create(Component.translatable("menu.moreminecrafts.reset_materials")));
            }
        }
        apply.visible=menu.mode()==1;apply.active=apply.visible&&T1Operations.canApply(equipment,menu.getSlot(1).getItem())&&RawXp.canPay(minecraft.player,T1Balance.BOOK_OPTION_XP);
        apply.setMessage(Component.translatable("menu.moreminecrafts.apply_option").append(" ("+T1Balance.BOOK_OPTION_XP+" XP)"));
    }
    @Override public void extractBackground(GuiGraphicsExtractor g,int mx,int my,float delta){
        super.extractBackground(g,mx,my,delta);
        int x=leftPos,y=topPos;g.fill(x,y,x+330,y+284,0xFFC6C6C6);g.fill(x+2,y+2,x+328,y+3,0xFFFFFFFF);g.fill(x+327,y+3,x+328,y+282,0xFF555555);
        // Vanilla recessed slots, familiar 9x3 inventory and hotbar placement.
        for(var slot:menu.slots){int sx=x+slot.x-1,sy=y+slot.y-1;g.fill(sx,sy,sx+18,sy+18,0xFF373737);g.fill(sx+1,sy+1,sx+18,sy+18,0xFFFFFFFF);g.fill(sx+1,sy+1,sx+17,sy+17,0xFF8B8B8B);}
        if(!visible.isEmpty()){g.fill(x+33,y+52,x+34,y+52+(visible.size()-1)*22,0xFF555555);for(int i=0;i<visible.size();i++)g.fill(x+33,y+51+i*22,x+39,y+52+i*22,0xFF555555);}
    }
    @Override protected void extractLabels(GuiGraphicsExtractor g,int mx,int my){
        g.text(font,Component.translatable("container.inventory"),78,190,0xFF404040,false);
        if(menu.tree().isEmpty())g.text(font,Component.translatable("menu.moreminecrafts.insert_equipment"),40,48,0xFF404040,false);
        var owned=T1Operations.enchantments(menu.getSlot(0).getItem());var options=T1Operations.options(menu.getSlot(0).getItem());
        for(int i=0;i<visible.size();i++){
            var n=visible.get(i);int current=owned.getOrDefault(n.id(),0);int option=options.getOrDefault(n.id(),0);
            String key="enchantment."+n.id().replace(':','.');Component name=Component.translatable(key);
            String detail=menu.mode()==0?BookOptionRules.roman(current)+" → "+BookOptionRules.roman(Math.min(current+1,n.maximum())):option>0?"(+"+BookOptionRules.roman(option)+")":"(+I) – (+"+BookOptionRules.roman(n.maximum())+")";
            g.text(font,name,40,44+i*22,0xFF404040,false);g.text(font,Component.literal(detail),40,54+i*22,0xFF555555,false);
        }
        g.text(font,Component.literal(RawXp.available(minecraft.player)+" XP"),12,106,0xFF404040,false);
    }
}
