package com.simplevillager.client.gui;

import com.simplevillager.SimpleVillagerMod;
import com.simplevillager.gui.AutoTraderContainer;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Renderable;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;

public class AutoTraderScreen extends AbstractContainerScreen<AutoTraderContainer> {

    public static final Identifier BACKGROUND = Identifier.fromNamespaceAndPath(SimpleVillagerMod.MOD_ID, "textures/gui/container/auto_trader.png");
    public static final Identifier DISCOUNT_STRIKETHROUGH = Identifier.withDefaultNamespace("container/villager/discount_strikethrough");

    public AutoTraderScreen(AutoTraderContainer container, Inventory playerInventory, Component name) {
        super(container, playerInventory, name, 176, 202);
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
            public void extractRenderState(GuiGraphicsExtractor g, int mx, int my, float pt) {
                g.centeredText(AutoTraderScreen.this.font, Component.translatable("gui.simplevillager.input"), AutoTraderScreen.this.leftPos + AutoTraderScreen.this.imageWidth / 2, AutoTraderScreen.this.topPos + 45, 0x404040);
                g.centeredText(AutoTraderScreen.this.font, Component.translatable("gui.simplevillager.output"), AutoTraderScreen.this.leftPos + AutoTraderScreen.this.imageWidth / 2, AutoTraderScreen.this.topPos + 77, 0x404040);
            }
        });
    }

    @Override
    public void extractContents(GuiGraphicsExtractor guiGraphics, int mouseX, int mouseY, float partialTicks) {
        super.extractContents(guiGraphics, mouseX, mouseY, partialTicks);
        ItemStack base = menu.slots.get(0).getItem();
        if (base.isEmpty()) return;
        int baseCount = base.getCount();
        int discounted = menu.getDiscountedCostACount();
        if (discounted <= 0 || baseCount == discounted) return;
        int x = this.leftPos + 36, y = this.topPos + 21;
        String baseStr = String.valueOf(baseCount);
        int baseWidth = this.font.width(baseStr);
        if (baseCount == 1) {
            guiGraphics.text(this.font, baseStr, x + 18 - baseWidth, y + 9, 0xFFFFFFFF, true);
        }
        guiGraphics.blitSprite(RenderPipelines.GUI_TEXTURED, DISCOUNT_STRIKETHROUGH, x + 18 - baseWidth, y + 13, baseWidth, 2);
        guiGraphics.text(this.font, String.valueOf(discounted), x + 18, y + 9, 0xFFFFFFFF, true);
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor guiGraphics, int mouseX, int mouseY, float partialTicks) {
        extractTransparentBackground(guiGraphics);
        RenderPipeline pipeline = RenderPipelines.GUI_TEXTURED;
        guiGraphics.blit(pipeline, BACKGROUND, this.leftPos, this.topPos, 0, 0, this.imageWidth, this.imageHeight, 256, 256);
        if (menu.isLocked()) {
            guiGraphics.blit(pipeline, BACKGROUND, this.leftPos + 83, this.topPos + 19, 176, 0, 28, 21, 256, 256);
        }
    }

    public static class ArrowButton extends Button {
        private final boolean left;

        public ArrowButton(int x, int y, boolean left, OnPress onPress) {
            super(x, y, 16, 20, Component.empty(), onPress, DEFAULT_NARRATION);
            this.left = left;
        }

        @Override
        protected void extractContents(GuiGraphicsExtractor guiGraphics, int mouseX, int mouseY, float partialTicks) {
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

            RenderPipeline pipeline = RenderPipelines.GUI_TEXTURED;
            Identifier texture = left
                    ? Identifier.fromNamespaceAndPath(SimpleVillagerMod.MOD_ID, "textures/gui/arrow_left.png")
                    : Identifier.fromNamespaceAndPath(SimpleVillagerMod.MOD_ID, "textures/gui/arrow_right.png");
            guiGraphics.blit(pipeline, texture, x + 1, y + 2, 0, 0, 14, 16, 16, 16);
        }
    }
}
