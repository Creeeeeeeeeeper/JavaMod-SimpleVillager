package com.simplevillager.client.gui;

import com.simplevillager.SimpleVillagerMod;
import com.simplevillager.gui.BreederContainer;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;

public class BreederScreen extends AbstractContainerScreen<BreederContainer> {

    private static final Identifier BACKGROUND = Identifier.fromNamespaceAndPath(SimpleVillagerMod.MOD_ID, "textures/gui/container/breeder.png");

    public BreederScreen(BreederContainer container, Inventory playerInventory, Component name) {
        super(container, playerInventory, name);
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTicks, int mouseX, int mouseY) {
        renderTransparentBackground(guiGraphics);
        RenderPipeline pipeline = RenderPipelines.GUI_TEXTURED;
        guiGraphics.blit(pipeline, BACKGROUND, this.leftPos, this.topPos, 0, 0, this.imageWidth, this.imageHeight, 256, 256);
        guiGraphics.drawString(this.font, Component.translatable("gui.SimpleVillager.food_items"), this.leftPos + 52, this.topPos + 8, 0x404040, false);
        guiGraphics.drawString(this.font, Component.translatable("gui.SimpleVillager.output"), this.leftPos + 52, this.topPos + 39, 0x404040, false);
        guiGraphics.drawString(this.font, this.playerInventoryTitle, this.leftPos + 8, this.topPos + this.imageHeight - 96 + 2, 0x404040, false);
    }
}
