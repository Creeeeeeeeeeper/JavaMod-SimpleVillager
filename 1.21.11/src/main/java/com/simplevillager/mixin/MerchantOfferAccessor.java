package com.simplevillager.mixin;

import net.minecraft.world.item.trading.MerchantOffer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(MerchantOffer.class)
public interface MerchantOfferAccessor {
    @Mutable
    @Accessor("maxUses")
    void SimpleVillager$setMaxUses(int maxUses);
}
