package com.simplevillager.gui;

import com.simplevillager.items.ModItems;
import com.simplevillager.items.VillagerItem;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

public class IncubatorContainer extends AbstractContainerMenu {

    private final Container inputInventory;
    private final Container outputInventory;

    public IncubatorContainer(int id, Inventory playerInventory, Container inputInventory, Container outputInventory) {
        super(ModMenus.INCUBATOR, id);
        this.inputInventory = inputInventory;
        this.outputInventory = outputInventory;

        // 4 input slots (only accepts VillagerItem)
        for (int i = 0; i < 4; i++) {
            addSlot(new IncubatorInputSlot(inputInventory, i, 52 + (i * 18), 20));
        }

        // 4 output slots (locked)
        for (int i = 0; i < 4; i++) {
            addSlot(new OutputContainer.OutputSlot(outputInventory, i, 52 + (i * 18), 51));
        }

        // Player inventory
        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 9; j++) {
                addSlot(new Slot(playerInventory, j + (i * 9) + 9, 8 + (j * 18), 84 + (i * 18)));
            }
        }
        for (int k = 0; k < 9; k++) {
            addSlot(new Slot(playerInventory, k, 8 + (k * 18), 142));
        }
    }

    public IncubatorContainer(int id, Inventory playerInventory) {
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

    public static class IncubatorInputSlot extends Slot {
        public IncubatorInputSlot(Container container, int index, int x, int y) {
            super(container, index, x, y);
        }

        @Override
        public boolean mayPlace(ItemStack stack) {
            return stack.getItem() instanceof VillagerItem && VillagerItem.isBaby(stack);
        }
    }
}
