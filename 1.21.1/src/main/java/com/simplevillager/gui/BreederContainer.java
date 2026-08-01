package com.simplevillager.gui;

import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.entity.npc.Villager;

public class BreederContainer extends AbstractContainerMenu {

    private final Container foodInventory;
    private final Container outputInventory;

    public BreederContainer(int id, Inventory playerInventory, Container foodInventory, Container outputInventory) {
        super(ModMenus.BREEDER, id);
        this.foodInventory = foodInventory;
        this.outputInventory = outputInventory;

        // Food input slots
        for (int i = 0; i < 4; i++) {
            addSlot(new FoodSlot(foodInventory, i, 52 + (i * 18), 20));
        }

        // Output slots (locked)
        for (int i = 0; i < 4; i++) {
            addSlot(new OutputOnlySlot(outputInventory, i, 52 + (i * 18), 51));
        }

        // Player inventory
        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 9; j++) {
                addSlot(new Slot(playerInventory, j + (i * 9) + 9, 8 + (j * 18), 82 + (i * 18)));
            }
        }
        for (int k = 0; k < 9; k++) {
            addSlot(new Slot(playerInventory, k, 8 + (k * 18), 140));
        }
    }

    public BreederContainer(int id, Inventory playerInventory) {
        this(id, playerInventory, new SimpleContainer(4), new SimpleContainer(4));
    }

    @Override
    public ItemStack quickMoveStack(Player playerIn, int index) {
        ItemStack itemstack = ItemStack.EMPTY;
        Slot slot = this.slots.get(index);
        if (slot != null && slot.hasItem()) {
            ItemStack stack = slot.getItem();
            itemstack = stack.copy();
            if (index < 8) {
                if (!moveItemStackTo(stack, 8, this.slots.size(), true)) {
                    return ItemStack.EMPTY;
                }
            } else if (index >= 8) {
                if (!moveItemStackTo(stack, 0, 4, false)) {
                    return ItemStack.EMPTY;
                }
            }
            if (stack.isEmpty()) {
                slot.set(ItemStack.EMPTY);
            } else {
                slot.setChanged();
            }
        }
        return itemstack;
    }

    @Override
    public boolean stillValid(Player player) {
        return true;
    }

    public static class FoodSlot extends Slot {
        public FoodSlot(Container container, int index, int x, int y) {
            super(container, index, x, y);
        }

        @Override
        public boolean mayPlace(ItemStack stack) {
            return Villager.FOOD_POINTS.getOrDefault(stack.getItem(), 0) > 0;
        }
    }

    public static class OutputOnlySlot extends Slot {
        public OutputOnlySlot(Container container, int index, int x, int y) {
            super(container, index, x, y);
        }

        @Override
        public boolean mayPickup(Player player) {
            return true;
        }

        @Override
        public boolean mayPlace(ItemStack stack) {
            return false;
        }
    }
}
