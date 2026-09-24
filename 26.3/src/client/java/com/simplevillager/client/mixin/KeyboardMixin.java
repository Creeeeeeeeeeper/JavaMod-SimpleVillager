package com.simplevillager.client.mixin;

import com.simplevillager.client.SimpleVillagerClient;
import net.minecraft.client.KeyboardHandler;
import net.minecraft.client.input.KeyEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(KeyboardHandler.class)
public class KeyboardMixin {

    @Inject(method = "keyPress", at = @At("HEAD"))
    private void onKey(long handle, int action, KeyEvent event, CallbackInfo ci) {
        if (action != 1) return;
        SimpleVillagerClient.onCycleTradesKeyPressed(event);
    }
}
