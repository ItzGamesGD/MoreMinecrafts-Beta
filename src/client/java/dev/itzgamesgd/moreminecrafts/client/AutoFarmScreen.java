package dev.itzgamesgd.moreminecrafts.client;

import dev.itzgamesgd.moreminecrafts.progression.AutoFarmMenu;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

/** Minimal, model-free GUI: two visible 5x5 grids, no extra scripted controls. */
public final class AutoFarmScreen extends AbstractContainerScreen<AutoFarmMenu> {
    public AutoFarmScreen(AutoFarmMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title, 222, 220);
        titleLabelX=10; titleLabelY=6; inventoryLabelX=35; inventoryLabelY=123;
    }
    @Override public void extractBackground(GuiGraphicsExtractor graphics,int mouseX,int mouseY,float delta) {
        super.extractBackground(graphics,mouseX,mouseY,delta);
        int x=leftPos,y=topPos;
        graphics.fill(x,y,x+imageWidth,y+imageHeight,0xEE1C262C);
        for(int i=0;i<50;i++){
            int local=i%25,cx=x+(i<25?10:122)+(local%5)*18,cy=y+18+(local/5)*18;
            graphics.fill(cx-1,cy-1,cx+17,cy+17,0xFF56656D);
            graphics.fill(cx,cy,cx+16,cy+16,0xFF192027);
        }
        for(int i=0;i<36;i++){
            int cx=x+35+(i%9)*18;
            int cy=y+(i<27?136+(i/9)*18:194);
            graphics.fill(cx-1,cy-1,cx+17,cy+17,0xFF56656D);
            graphics.fill(cx,cy,cx+16,cy+16,0xFF192027);
        }
    }
    @Override protected void extractLabels(GuiGraphicsExtractor graphics,int mouseX,int mouseY) {
        graphics.text(font,title,10,6,0xFFFFFFFF,true);
        graphics.text(font,Component.translatable("menu.moreminecrafts.auto_farm.seeds"),10,110,0xFFE4F1F0,false);
        graphics.text(font,Component.translatable("menu.moreminecrafts.auto_farm.output"),122,110,0xFFE4F1F0,false);
        graphics.text(font,Component.translatable("container.inventory"),inventoryLabelX,inventoryLabelY,0xFFE4F1F0,false);
    }
}
