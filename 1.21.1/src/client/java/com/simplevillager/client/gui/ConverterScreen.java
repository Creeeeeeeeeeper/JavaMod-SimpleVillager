package com.simplevillager.client.gui;

import com.simplevillager.SimpleVillagerMod;
import com.simplevillager.gui.ConverterContainer;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

public class ConverterScreen extends AbstractContainerScreen<ConverterContainer> {

    private static final ResourceLocation BACKGROUND = ResourceLocation.fromNamespaceAndPath(SimpleVillagerMod.MOD_ID, "textures/gui/container/converter.png");

    public ConverterScreen(ConverterContainer container, Inventory playerInventory, Component name) {
        super(container, playerInventory, name);
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTicks, int mouseX, int mouseY) {
                guiGraphics.blit(BACKGROUND, this.leftPos, this.topPos, 0, 0, this.imageWidth, this.imageHeight, 256, 256);
        Component inputLabel = Component.translatable("gui.simplevillager.input");
        guiGraphics.drawString(this.font, inputLabel, this.leftPos + this.imageWidth / 2 - this.font.width(inputLabel) / 2, this.topPos + 8, 0xFF404040, false);
        Component outputLabel = Component.translatable("gui.simplevillager.output");
        guiGraphics.drawString(this.font, outputLabel, this.leftPos + this.imageWidth / 2 - this.font.width(outputLabel) / 2, this.topPos + 39, 0xFF404040, false);
    }
}
