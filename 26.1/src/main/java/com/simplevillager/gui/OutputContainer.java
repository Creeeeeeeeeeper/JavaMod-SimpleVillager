package com.simplevillager.gui;

import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

public class OutputContainer extends AbstractContainerMenu {

    private final Container outputInventory;

    public OutputContainer(int id, Inventory playerInventory, Container outputInventory) {
        super(ModMenus.OUTPUT, id);
        this.outputInventory = outputInventory;

        for (int i = 0; i < 4; i++) {
            addSlot(new OutputSlot(outputInventory, i, 52 + (i * 18), 20));
        }

        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 9; j++) {
                addSlot(new Slot(playerInventory, j + (i * 9) + 9, 8 + (j * 18), 51 + (i * 18)));
            }
        }
        for (int k = 0; k < 9; k++) {
            addSlot(new Slot(playerInventory, k, 8 + (k * 18), 109));
        }
    }

    public OutputContainer(int id, Inventory playerInventory) {
        this(id, playerInventory, new SimpleContainer(4));
    }

    @Override
    public ItemStack quickMoveStack(Player playerIn, int index) {
        ItemStack itemstack = ItemStack.EMPTY;
        Slot slot = this.slots.get(index);
        if (slot != null && slot.hasItem()) {
            ItemStack stack = slot.getItem();
            itemstack = stack.copy();
            if (index < 4) {
                if (!moveItemStackTo(stack, 4, this.slots.size(), true)) {
                    return ItemStack.EMPTY;
                }
            } else {
                return ItemStack.EMPTY;
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

    public static class OutputSlot extends Slot {
        public OutputSlot(Container container, int index, int x, int y) {
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
