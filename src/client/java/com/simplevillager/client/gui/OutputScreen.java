package com.simplevillager.client.gui;

import com.simplevillager.SimpleVillagerMod;
import com.simplevillager.gui.OutputContainer;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;

public class OutputScreen extends AbstractContainerScreen<OutputContainer> {

    private static final Identifier BACKGROUND = Identifier.fromNamespaceAndPath(SimpleVillagerMod.MOD_ID, "textures/gui/container/output.png");

    public OutputScreen(OutputContainer container, Inventory playerInventory, Component name) {
        super(container, playerInventory, name, 176, 132);
    }

    @Override
    public void extractContents(GuiGraphicsExtractor guiGraphics, int mouseX, int mouseY, float partialTicks) {
        extractTransparentBackground(guiGraphics);
        RenderPipeline pipeline = RenderPipelines.GUI_TEXTURED;
        guiGraphics.blit(pipeline, BACKGROUND, this.leftPos, this.topPos, 0, 0, this.imageWidth, this.imageHeight, 256, 256);
        super.extractContents(guiGraphics, mouseX, mouseY, partialTicks);
        guiGraphics.text(this.font, this.playerInventoryTitle, this.leftPos + 8, this.topPos + this.imageHeight - 96 + 2, 0x404040, false);
    }
}
