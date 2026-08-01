package com.simplevillager.client.gui;

import com.simplevillager.SimpleVillagerMod;
import com.simplevillager.gui.InventoryViewerContainer;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;

public class InventoryViewerScreen extends AbstractContainerScreen<InventoryViewerContainer> {

    private static final ResourceLocation BACKGROUND = ResourceLocation.fromNamespaceAndPath(SimpleVillagerMod.MOD_ID, "textures/gui/container/villager_inventory.png");

    private static final ResourceLocation[] EMPTY_ARMOR_ICONS = {
            ResourceLocation.withDefaultNamespace("container/slot/boots"),
            ResourceLocation.withDefaultNamespace("container/slot/leggings"),
            ResourceLocation.withDefaultNamespace("container/slot/chestplate"),
            ResourceLocation.withDefaultNamespace("container/slot/helmet")
    };

    public InventoryViewerScreen(InventoryViewerContainer container, Inventory playerInventory, Component name) {
        super(container, playerInventory, name);
        this.imageWidth = 176;
        this.imageHeight = 182;
        this.inventoryLabelY = this.imageHeight - 96 + 3;
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTicks, int mouseX, int mouseY) {
                guiGraphics.blit(BACKGROUND, this.leftPos, this.topPos, 0, 0, this.imageWidth, this.imageHeight, 256, 256);
        Component invLabel = Component.translatable("gui.simplevillager.villager_inventory");
        guiGraphics.drawString(this.font, invLabel, this.leftPos + this.imageWidth / 2 - this.font.width(invLabel) / 2, this.topPos + 9, 0x404040, false);
        Component equipLabel = Component.translatable("gui.simplevillager.villager_equipment");
        guiGraphics.drawString(this.font, equipLabel, this.leftPos + this.imageWidth / 2 - this.font.width(equipLabel) / 2, this.topPos + 58, 0x404040, false);
    }
}
