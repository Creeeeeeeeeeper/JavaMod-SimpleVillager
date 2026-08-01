package com.simplevillager.events;

import com.simplevillager.datacomponent.VillagerData;
import com.simplevillager.items.ModItems;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

public class VillagerPickup {

    public static void pickUp(Villager villager, Player player) {
        if (!arePickupConditionsMet(villager)) {
            return;
        }
        ItemStack stack = new ItemStack(ModItems.VILLAGER);
        VillagerData.applyToItem(stack, villager);
        if (player.getMainHandItem().isEmpty()) {
            player.setItemInHand(InteractionHand.MAIN_HAND, stack);
            villager.discard();
        } else if (player.getInventory().add(stack)) {
            villager.discard();
        }
    }

    public static boolean arePickupConditionsMet(Villager villager) {
        return villager.isAlive() && !villager.isSleeping();
    }
}
