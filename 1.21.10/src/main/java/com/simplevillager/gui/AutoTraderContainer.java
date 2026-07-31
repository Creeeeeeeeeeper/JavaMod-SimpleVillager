package com.simplevillager.gui;

import com.simplevillager.blockentity.AutoTraderBlockEntity;
import com.simplevillager.blocks.ModBlocks;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import org.jetbrains.annotations.Nullable;

public class AutoTraderContainer extends AbstractContainerMenu {

    private final AutoTraderBlockEntity trader;
    private boolean locked;
    private final ContainerData tradeData;

    public AutoTraderContainer(int id, Inventory playerInventory, @Nullable AutoTraderBlockEntity trader) {
        super(ModMenus.AUTO_TRADER, id);
        this.trader = trader;
        this.tradeData = new ContainerData() {
            @Override
            public int get(int index) {
                if (index == 0) {
                    return trader != null && trader.isLocked() ? 0 : 1;
                }
                return 0;
            }

            @Override
            public void set(int index, int value) {
                if (index == 0) {
                    locked = value == 0;
                }
            }

            @Override
            public int getCount() {
                return 1;
            }
        };

        if (trader != null) {
            Container tradeGuiInv = trader.getTradeGuiInv();
            Container inputInv = trader.getInputInventory();
            Container outputInv = trader.getOutputInventory();

            // Trade display slots (locked)
            addSlot(new LockedSlot(tradeGuiInv, 0, 36, 21));
            addSlot(new LockedSlot(tradeGuiInv, 1, 62, 21));
            addSlot(new LockedSlot(tradeGuiInv, 2, 120, 21));

            // Input slots
            for (int i = 0; i < 4; i++) {
                addSlot(new Slot(inputInv, i, 53 + (i * 18), 57));
            }

            // Output slots (locked)
            for (int i = 0; i < 4; i++) {
                addSlot(new LockedSlot(outputInv, i, 53 + (i * 18), 88));
            }
        } else {
            SimpleContainer empty = new SimpleContainer(11);
            addSlot(new LockedSlot(empty, 0, 36, 21));
            addSlot(new LockedSlot(empty, 1, 62, 21));
            addSlot(new LockedSlot(empty, 2, 120, 21));
            for (int i = 0; i < 4; i++) {
                addSlot(new Slot(empty, 3 + i, 53 + (i * 18), 57));
            }
            for (int i = 0; i < 4; i++) {
                addSlot(new LockedSlot(empty, 7 + i, 53 + (i * 18), 88));
            }
        }

        // Player inventory
        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 9; j++) {
                addSlot(new Slot(playerInventory, j + (i * 9) + 9, 8 + (j * 18), 84 + (i * 18) + 36));
            }
        }
        for (int k = 0; k < 9; k++) {
            addSlot(new Slot(playerInventory, k, 8 + (k * 18), 142 + 36));
        }

        addDataSlots(this.tradeData);
    }

    public AutoTraderContainer(int id, Inventory playerInventory) {
        this(id, playerInventory, null);
    }

    public boolean isLocked() {
        return locked;
    }

    @Nullable
    public AutoTraderBlockEntity getTrader() {
        return trader;
    }

    @Override
    public ItemStack quickMoveStack(Player playerIn, int index) {
        ItemStack itemstack = ItemStack.EMPTY;
        Slot slot = this.slots.get(index);
        if (slot != null && slot.hasItem()) {
            ItemStack stack = slot.getItem();
            itemstack = stack.copy();
            // 11 = 3 trade + 4 input + 4 output
            if (index < 11) {
                if (!moveItemStackTo(stack, 11, this.slots.size(), true)) {
                    return ItemStack.EMPTY;
                }
            } else if (index >= 11 && index < 11 + 36) {
                // Player inventory -> try input slots (index 3-6)
                if (!moveItemStackTo(stack, 3, 7, false)) {
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

    // --- Locked Slot (cannot be interacted with by player) ---
    public static class LockedSlot extends Slot {
        public LockedSlot(Container container, int index, int x, int y) {
            super(container, index, x, y);
        }

        @Override
        public boolean mayPickup(Player player) {
            return false;
        }

        @Override
        public boolean mayPlace(ItemStack stack) {
            return false;
        }
    }
}
