package com.simplevillager.client.mixin;

import com.simplevillager.client.SimpleVillagerClient;
import com.simplevillager.client.gui.CycleTradesButton;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.MerchantScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.MerchantMenu;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MerchantScreen.class)
public abstract class MerchantScreenMixin extends AbstractContainerScreen<MerchantMenu> {

    protected MerchantScreenMixin(MerchantMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
    }

    @Inject(method = "init", at = @At("TAIL"))
    private void addCycleTradesButton(CallbackInfo ci) {
        int x = this.leftPos + 107;
        int y = this.topPos + 8;
        CycleTradesButton button = new CycleTradesButton(x, y, btn -> {
            SimpleVillagerClient.sendCycleTrades();
        });
        this.addRenderableWidget(button);
    }
}
