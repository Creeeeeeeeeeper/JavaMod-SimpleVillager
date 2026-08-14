package com.simplevillager.client.gui;

import com.simplevillager.SimpleVillagerMod;
import com.simplevillager.gui.BreederContainer;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

public class BreederScreen extends AbstractContainerScreen<BreederContainer> {

    private static final ResourceLocation BACKGROUND = ResourceLocation.fromNamespaceAndPath(SimpleVillagerMod.MOD_ID, "textures/gui/container/breeder.png");

    public BreederScreen(BreederContainer container, Inventory playerInventory, Component name) {
        super(container, playerInventory, name);
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTicks, int mouseX, int mouseY) {
                guiGraphics.blit(BACKGROUND, this.leftPos, this.topPos, 0, 0, this.imageWidth, this.imageHeight, 256, 256);
        Component foodLabel = Component.translatable("gui.simplevillager.food_items");
        guiGraphics.drawString(this.font, foodLabel, this.leftPos + this.imageWidth / 2 - this.font.width(foodLabel) / 2, this.topPos + 8, 0xFF404040, false);
        Component outputLabel = Component.translatable("gui.simplevillager.output");
        guiGraphics.drawString(this.font, outputLabel, this.leftPos + this.imageWidth / 2 - this.font.width(outputLabel) / 2, this.topPos + 39, 0xFF404040, false);
    }
}
