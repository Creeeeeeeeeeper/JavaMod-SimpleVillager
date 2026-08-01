package com.simplevillager.client.gui;

import com.simplevillager.SimpleVillagerMod;
import com.simplevillager.gui.AutoTraderContainer;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Renderable;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

public class AutoTraderScreen extends AbstractContainerScreen<AutoTraderContainer> {

    public static final ResourceLocation BACKGROUND = ResourceLocation.fromNamespaceAndPath(SimpleVillagerMod.MOD_ID, "textures/gui/container/auto_trader.png");

    public AutoTraderScreen(AutoTraderContainer container, Inventory playerInventory, Component name) {
        super(container, playerInventory, name);
        this.imageWidth = 176;
        this.imageHeight = 202;
        this.inventoryLabelY = this.imageHeight - 84;
    }

    @Override
    protected void init() {
        super.init();
        addRenderableWidget(new ArrowButton(this.leftPos + 8, this.topPos + 19, true, button -> {
            com.simplevillager.client.SimpleVillagerClient.sendSelectTrade(false);
        }));
        addRenderableWidget(new ArrowButton(this.leftPos + this.imageWidth - 16 - 8, this.topPos + 19, false, button2 -> {
            com.simplevillager.client.SimpleVillagerClient.sendSelectTrade(true);
        }));
        addRenderableOnly(new Renderable() {
            @Override
            public void render(GuiGraphics g, int mx, int my, float pt) {
                Component inputLabel = Component.translatable("gui.simplevillager.input");
                g.drawString(AutoTraderScreen.this.font, inputLabel, AutoTraderScreen.this.leftPos + AutoTraderScreen.this.imageWidth / 2 - AutoTraderScreen.this.font.width(inputLabel) / 2, AutoTraderScreen.this.topPos + 45, 0x404040, false);
                Component outputLabel = Component.translatable("gui.simplevillager.output");
                g.drawString(AutoTraderScreen.this.font, outputLabel, AutoTraderScreen.this.leftPos + AutoTraderScreen.this.imageWidth / 2 - AutoTraderScreen.this.font.width(outputLabel) / 2, AutoTraderScreen.this.topPos + 77, 0x404040, false);
            }
        });
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTicks, int mouseX, int mouseY) {
                guiGraphics.blit(BACKGROUND, this.leftPos, this.topPos, 0, 0, this.imageWidth, this.imageHeight, 256, 256);
        if (menu.isLocked()) {
            guiGraphics.blit(BACKGROUND, this.leftPos + 83, this.topPos + 19, 176, 0, 28, 21, 256, 256);
        }
    }

    public static class ArrowButton extends Button {
        private final boolean left;

        public ArrowButton(int x, int y, boolean left, OnPress onPress) {
            super(x, y, 16, 20, Component.empty(), onPress, DEFAULT_NARRATION);
            this.left = left;
        }

        @Override
        protected void renderWidget(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTicks) {
            int x = getX(), y = getY(), w = this.width, h = this.height;

            boolean hovered = this.isHovered;
            int face = hovered ? 0xFF606060 : 0xFF404040;
            int light = hovered ? 0xFF888888 : 0xFF555555;
            int dark = hovered ? 0xFF333333 : 0xFF222222;

            guiGraphics.fill(x, y, x + w, y + h, dark);
            guiGraphics.fill(x + 1, y + 1, x + w - 1, y + h - 1, face);
            guiGraphics.fill(x + 1, y + 1, x + w - 1, y + 2, light);
            guiGraphics.fill(x + 1, y + 1, x + 2, y + h - 1, light);
            guiGraphics.fill(x + 2, y + h - 2, x + w - 1, y + h - 1, dark);

                        ResourceLocation texture = left
                    ? ResourceLocation.fromNamespaceAndPath(SimpleVillagerMod.MOD_ID, "textures/gui/arrow_left.png")
                    : ResourceLocation.fromNamespaceAndPath(SimpleVillagerMod.MOD_ID, "textures/gui/arrow_right.png");
            guiGraphics.blit(texture, x + 1, y + 2, 0, 0, 14, 16, 16, 16);
        }
    }
}
