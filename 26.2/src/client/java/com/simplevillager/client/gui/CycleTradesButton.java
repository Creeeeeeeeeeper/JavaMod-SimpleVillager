package com.simplevillager.client.gui;

import com.simplevillager.SimpleVillagerMod;
import com.simplevillager.mixin.MerchantMenuAccessor;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.screens.inventory.MerchantScreen;
import net.minecraft.client.input.InputWithModifiers;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.inventory.MerchantMenu;

import java.util.function.Consumer;

public class CycleTradesButton extends AbstractButton {

    private static final Identifier ARROW_BUTTON =
            Identifier.fromNamespaceAndPath(SimpleVillagerMod.MOD_ID, "textures/gui/container/arrow_button.png");

    public static final int WIDTH = 18;
    public static final int HEIGHT = 14;

    private final Consumer<CycleTradesButton> onPress;

    public CycleTradesButton(int x, int y, Consumer<CycleTradesButton> onPress) {
        super(x, y, WIDTH, HEIGHT, Component.empty());
        this.onPress = onPress;
    }

    @Override
    public void onPress(InputWithModifiers input) {
        this.onPress.accept(this);
    }

    @Override
    protected void extractContents(GuiGraphicsExtractor guiGraphics, int mouseX, int mouseY, float partialTicks) {
        this.visible = canCycle();

        RenderPipeline pipeline = RenderPipelines.GUI_TEXTURED;
        if (this.isHovered) {
            guiGraphics.blit(pipeline, ARROW_BUTTON, this.getX(), this.getY(), 0f, 14f, 18, 14, 32, 32);
        } else {
            guiGraphics.blit(pipeline, ARROW_BUTTON, this.getX(), this.getY(), 0f, 0f, 18, 14, 32, 32);
        }
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput narrationElementOutput) {
    }

    public boolean canCycle() {
        if (!(this.screen != null && this.screen.getMenu() instanceof MerchantMenu menu)) return false;
        if (!menu.showProgressBar()) return false;
        if (menu.getTraderXp() > 0) return false;
        var tradeContainer = ((MerchantMenuAccessor) menu).SimpleVillager$getTradeContainer();
        if (tradeContainer.getActiveOffer() != null) return false;
        return true;
    }

    private MerchantScreen screen;

    public void setScreen(MerchantScreen screen) {
        this.screen = screen;
    }
}
