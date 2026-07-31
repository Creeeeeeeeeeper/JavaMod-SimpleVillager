package com.simplevillager.client.gui;

import com.simplevillager.SimpleVillagerMod;
import com.simplevillager.gui.OutputContainer;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;

public class OutputScreen extends AbstractContainerScreen<OutputContainer> {

    private static final Identifier BACKGROUND = Identifier.fromNamespaceAndPath(SimpleVillagerMod.MOD_ID, "textures/gui/container/output.png");

    public OutputScreen(OutputContainer container, Inventory playerInventory, Component name) {
        super(container, playerInventory, name);
        this.inventoryLabelY = this.imageHeight - 94 - 35;
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTicks, int mouseX, int mouseY) {
        renderTransparentBackground(guiGraphics);
        RenderPipeline pipeline = RenderPipelines.GUI_TEXTURED;
        guiGraphics.blit(pipeline, BACKGROUND, this.leftPos, this.topPos, 0, 0, this.imageWidth, this.imageHeight, 256, 256);
    }
}
