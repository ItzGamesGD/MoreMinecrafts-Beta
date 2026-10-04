package dev.itzgamesgd.moreminecrafts.client;
import dev.itzgamesgd.moreminecrafts.progression.*;
import dev.itzgamesgd.moreminecrafts.enhancement.EnhancementOperation;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.*;
import net.minecraft.client.gui.screens.inventory.AnvilScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AnvilMenu;

public final class T1AnvilScreen extends AnvilScreen {
    private Button action;
    private final Button[] modes=new Button[3];
    public T1AnvilScreen(AnvilMenu menu,Inventory inventory,Component title){super(menu,inventory,title);}
    private int mode(){return ((T1AnvilModes)menu).moreminecrafts$mode();}
    private void request(int id){minecraft.gameMode.handleInventoryButtonClick(menu.containerId,id);}
    @Override protected void subInit(){
        super.subInit();String[] keys={"enhancement","normal_anvil","book_combine"};
        int[] widths={56,80,112};int x=leftPos-36;
        for(int i=0;i<3;i++){final int n=i;modes[i]=addRenderableWidget(Button.builder(Component.translatable("menu.moreminecrafts."+keys[i]),b->request(n)).bounds(x,topPos-24,widths[i],20).build());x+=widths[i];}
        action=addRenderableWidget(Button.builder(Component.empty(),b->request(3)).bounds(leftPos+59,topPos+20,110,20).build());
    }
    @Override protected void containerTick(){
        super.containerTick();for(var child:children())if(child instanceof EditBox box)box.visible=mode()==1;
        for(int i=0;i<3;i++)modes[i].active=mode()!=i;
        action.visible=mode()!=1;action.active=menu.getSlot(2).hasItem()&&menu.getSlot(2).mayPickup(minecraft.player);
        action.setMessage(Component.translatable("menu.moreminecrafts."+(mode()==0?"enhancement":"book_combine")));
    }
    @Override protected void extractLabels(GuiGraphicsExtractor g,int mx,int my){
        if(mode()==1){super.extractLabels(g,mx,my);return;}
        g.text(font,Component.translatable("container.inventory"),8,72,0xFF404040,false);
        int current=EnhancementOperation.level(menu.getSlot(0).getItem());
        Component info=mode()==0?Component.literal("+"+current+" → +"+Math.min(20,current+1)+"   "+menu.getCost()+" Lv"):Component.literal(menu.getCost()+" XP");
        g.text(font,info,8,6,0xFF404040,false);
    }
}
