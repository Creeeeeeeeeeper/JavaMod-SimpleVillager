package com.simplevillager.client.gui;

import com.simplevillager.SimpleVillagerMod;
import com.simplevillager.gui.InventoryViewerContainer;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;

public class InventoryViewerScreen extends AbstractContainerScreen<InventoryViewerContainer> {

    private static final Identifier BACKGROUND = Identifier.fromNamespaceAndPath(SimpleVillagerMod.MOD_ID, "textures/gui/container/villager_inventory.png");

    private static final Identifier[] EMPTY_ARMOR_ICONS = {
            Identifier.withDefaultNamespace("container/slot/boots"),
            Identifier.withDefaultNamespace("container/slot/leggings"),
            Identifier.withDefaultNamespace("container/slot/chestplate"),
            Identifier.withDefaultNamespace("container/slot/helmet")
    };

    public InventoryViewerScreen(InventoryViewerContainer container, Inventory playerInventory, Component name) {
        super(container, playerInventory, name);
        this.imageWidth = 176;
        this.imageHeight = 182;
        this.inventoryLabelY = this.imageHeight - 96 + 3;
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTicks, int mouseX, int mouseY) {
        renderTransparentBackground(guiGraphics);
        RenderPipeline pipeline = RenderPipelines.GUI_TEXTURED;
        guiGraphics.blit(pipeline, BACKGROUND, this.leftPos, this.topPos, 0, 0, this.imageWidth, this.imageHeight, 256, 256);
        renderArmorSlotIcons(guiGraphics);
        guiGraphics.drawString(this.font, Component.translatable("gui.simplevillager.villager_inventory"), this.leftPos + 9, this.topPos + 9, 0xFF404040, false);
        guiGraphics.drawString(this.font, Component.translatable("gui.simplevillager.villager_equipment"), this.leftPos + 58, this.topPos + 58, 0xFF404040, false);
    }

    private void renderArmorSlotIcons(GuiGraphics guiGraphics) {
        RenderPipeline pipeline = RenderPipelines.GUI_TEXTURED;
        for (int i = 0; i < 4; i++) {
            int slotIndex = 8 + i;
            if (slotIndex < this.menu.slots.size()) {
                Slot slot = this.menu.getSlot(slotIndex);
                if (slot.getItem().isEmpty() && slot.isActive()) {
                    Identifier icon = slot.getNoItemIcon();
                    if (icon != null) {
                        guiGraphics.blitSprite(pipeline, icon, this.leftPos + slot.x, this.topPos + slot.y, 16, 16);
                    }
                }
            }
        }
    }
}
