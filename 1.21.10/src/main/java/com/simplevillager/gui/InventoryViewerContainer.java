package com.simplevillager.gui;

import com.simplevillager.blockentity.InventoryViewerBlockEntity;
import com.simplevillager.entity.SimpleVillagerEntity;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

public class InventoryViewerContainer extends AbstractContainerMenu {

    @Nullable
    private final InventoryViewerBlockEntity blockEntity;

    private static final EquipmentSlot[] DISPLAY_ARMOR = {
            EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET
    };

    private static final ResourceLocation[] EMPTY_SLOT_TEXTURES = {
            ResourceLocation.withDefaultNamespace("container/slot/boots"),
            ResourceLocation.withDefaultNamespace("container/slot/leggings"),
            ResourceLocation.withDefaultNamespace("container/slot/chestplate"),
            ResourceLocation.withDefaultNamespace("container/slot/helmet")
    };

    public InventoryViewerContainer(int id, Inventory playerInventory, @Nullable InventoryViewerBlockEntity blockEntity) {
        super(ModMenus.INVENTORY_VIEWER, id);
        this.blockEntity = blockEntity;

        SimpleContainer villagerInv = blockEntity != null ? blockEntity.getVillagerInventory() : new SimpleContainer(8);
        for (int i = 0; i < 4; i++) {
            addSlot(new Slot(villagerInv, i, 52 + (i * 18), 20));
        }
        for (int i = 0; i < 4; i++) {
            addSlot(new Slot(villagerInv, i + 4, 52 + (i * 18), 38));
        }

        EquipmentContainer equipInv = blockEntity != null ? new EquipmentContainer(blockEntity) : null;
        for (int i = 0; i < 4; i++) {
            EquipmentSlot es = DISPLAY_ARMOR[i];
            addSlot(new ArmorDisplaySlot(equipInv, es, 52 + (i * 18), 69, blockEntity));
        }

        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 9; j++) {
                addSlot(new Slot(playerInventory, j + (i * 9) + 9, 8 + (j * 18), 100 + (i * 18)));
            }
        }
        for (int k = 0; k < 9; k++) {
            addSlot(new Slot(playerInventory, k, 8 + (k * 18), 158));
        }
    }

    public InventoryViewerContainer(int id, Inventory playerInventory) {
        this(id, playerInventory, null);
    }

    @Nullable
    public InventoryViewerBlockEntity getBlockEntity() {
        return blockEntity;
    }

    @Override
    public ItemStack quickMoveStack(Player playerIn, int index) {
        ItemStack itemstack = ItemStack.EMPTY;
        Slot slot = this.slots.get(index);
        if (slot != null && slot.hasItem()) {
            ItemStack stack = slot.getItem();
            itemstack = stack.copy();
            int inventorySize = 12;
            if (index < inventorySize) {
                if (!moveItemStackTo(stack, inventorySize, this.slots.size(), true)) {
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
        if (blockEntity == null) return true;
        return !blockEntity.isRemoved() && player.distanceToSqr(blockEntity.getBlockPos().getX() + 0.5, blockEntity.getBlockPos().getY() + 0.5, blockEntity.getBlockPos().getZ() + 0.5) <= 64.0;
    }

    private static class ArmorDisplaySlot extends Slot {
        private final EquipmentSlot equipmentSlot;
        @Nullable
        private final InventoryViewerBlockEntity blockEntity;

        ArmorDisplaySlot(@Nullable EquipmentContainer container, EquipmentSlot equipmentSlot, int x, int y, @Nullable InventoryViewerBlockEntity blockEntity) {
            super(container != null ? container : new SimpleContainer(4), equipmentSlot.getIndex(), x, y);
            this.equipmentSlot = equipmentSlot;
            this.blockEntity = blockEntity;
        }

        @Override
        public int getMaxStackSize() {
            return 1;
        }

        @Override
        public boolean mayPlace(ItemStack stack) {
            if (blockEntity != null) {
                SimpleVillagerEntity villager = blockEntity.getVillagerEntity();
                if (villager != null) {
                    return villager.getEquipmentSlotForItem(stack) == equipmentSlot;
                }
            }
            return false;
        }

        @Override
        public boolean mayPickup(Player player) {
            return true;
        }

        @Override
        public ResourceLocation getNoItemIcon() {
            return EMPTY_SLOT_TEXTURES[equipmentSlot.getIndex()];
        }
    }

    private static class EquipmentContainer implements Container {
        private final InventoryViewerBlockEntity blockEntity;

        EquipmentContainer(InventoryViewerBlockEntity blockEntity) {
            this.blockEntity = blockEntity;
        }

        private static EquipmentSlot slotByIndex(int index) {
            return switch (index) {
                case 0 -> EquipmentSlot.FEET;
                case 1 -> EquipmentSlot.LEGS;
                case 2 -> EquipmentSlot.CHEST;
                case 3 -> EquipmentSlot.HEAD;
                default -> EquipmentSlot.MAINHAND;
            };
        }

        @Override
        public int getContainerSize() {
            return 4;
        }

        @Override
        public boolean isEmpty() {
            for (int i = 0; i < 4; i++) {
                if (!getItem(i).isEmpty()) return false;
            }
            return true;
        }

        @Override
        public ItemStack getItem(int slot) {
            return blockEntity.getEquipment(slotByIndex(slot));
        }

        @Override
        public ItemStack removeItem(int slot, int amount) {
            ItemStack stack = getItem(slot);
            if (stack.isEmpty()) return ItemStack.EMPTY;
            ItemStack result = stack.split(amount);
            if (!result.isEmpty()) {
                blockEntity.setEquipment(slotByIndex(slot), stack);
            }
            return result;
        }

        @Override
        public ItemStack removeItemNoUpdate(int slot) {
            ItemStack stack = getItem(slot);
            blockEntity.setEquipment(slotByIndex(slot), ItemStack.EMPTY);
            return stack;
        }

        @Override
        public void setItem(int slot, ItemStack stack) {
            blockEntity.setEquipment(slotByIndex(slot), stack);
        }

        @Override
        public void setChanged() {
        }

        @Override
        public boolean stillValid(Player player) {
            return true;
        }

        @Override
        public void clearContent() {
            for (int i = 0; i < 4; i++) {
                blockEntity.setEquipment(slotByIndex(i), ItemStack.EMPTY);
            }
        }
    }
}
