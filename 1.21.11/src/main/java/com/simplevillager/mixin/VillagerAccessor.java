package com.simplevillager.mixin;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(Villager.class)
public interface VillagerAccessor {
    @Accessor("lastRestockGameTime")
    long SimpleVillager$getLastRestockGameTime();

    @Accessor("increaseProfessionLevelOnUpdate")
    boolean SimpleVillager$getIncreaseProfessionLevelOnUpdate();

    @Accessor("increaseProfessionLevelOnUpdate")
    void SimpleVillager$setIncreaseProfessionLevelOnUpdate(boolean value);

    @Invoker("increaseMerchantCareer")
    void SimpleVillager$callIncreaseMerchantCareer(ServerLevel level);

    @Invoker("updateSpecialPrices")
    void SimpleVillager$callUpdateSpecialPrices(Player player);

    @Invoker("shouldIncreaseLevel")
    boolean SimpleVillager$callShouldIncreaseLevel();
}
