package com.simplevillager.blockentity;

import com.simplevillager.config.ModConfig;
import com.simplevillager.blocks.VillagerBlockBase;
import com.simplevillager.datacomponent.VillagerData;
import com.simplevillager.entity.SimpleVillagerEntity;
import com.simplevillager.items.VillagerItem;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jetbrains.annotations.Nullable;

public class IncubatorBlockEntity extends VillagerBlockEntityBase implements Container, WorldlyContainer {

    private static int incubatorSpeed() { return Math.max(1, ModConfig.server().incubatorSpeed); }

    private ItemStack villager = ItemStack.EMPTY;
    @Nullable
    private SimpleVillagerEntity villagerEntity = null;
    private final SimpleContainer inputInventory = new SimpleContainer(4);
    private final SimpleContainer outputInventory = new SimpleContainer(4);

    public IncubatorBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.INCUBATOR, pos, state);
    }

    // --- Villager management ---

    public ItemStack getVillager() {
        if (villagerEntity != null) {
            VillagerData.applyToItem(villager, villagerEntity);
        }
        return villager;
    }

    public boolean hasVillager() {
        return !villager.isEmpty();
    }

    @Nullable
    public SimpleVillagerEntity getVillagerEntity() {
        if (villagerEntity == null && !villager.isEmpty() && level != null) {
            villagerEntity = VillagerData.createSimpleVillager(villager, level);
        }
        return villagerEntity;
    }

    public void setVillager(ItemStack stack) {
        this.villager = stack;
        this.villagerEntity = stack.isEmpty() ? null : VillagerData.createSimpleVillager(stack, level);
        setChanged();
        syncData();
    }

    public ItemStack removeVillager() {
        ItemStack v = getVillager();
        setVillager(ItemStack.EMPTY);
        return v;
    }

    // --- Inventories ---

    public SimpleContainer getInputInventory() { return inputInventory; }
    public SimpleContainer getOutputInventory() { return outputInventory; }

    // --- Server tick ---

    public static void serverTick(Level level, BlockPos pos, BlockState state, IncubatorBlockEntity entity) {
        if (level.isClientSide()) return;

        // Phase 1: Eject adult villagers to output, pull baby villager from input
        if (!entity.hasVillager()) {
            for (int i = 0; i < entity.inputInventory.getContainerSize(); i++) {
                ItemStack stack = entity.inputInventory.getItem(i);
                if (stack.isEmpty() || !(stack.getItem() instanceof VillagerItem)) continue;

                if (VillagerItem.isBaby(stack)) {
                    ItemStack extracted = stack.copy();
                    extracted.setCount(1);
                    stack.shrink(1);
                    entity.setVillager(extracted);
                    entity.syncData();
                    break;
                } else {
                    // Adult villager: eject to output inventory
                    ItemStack extracted = stack.copy();
                    extracted.setCount(1);
                    stack.shrink(1);
                    for (int j = 0; j < entity.outputInventory.getContainerSize(); j++) {
                        if (entity.outputInventory.getItem(j).isEmpty()) {
                            entity.outputInventory.setItem(j, extracted);
                            entity.setChanged();
                            break;
                        }
                    }
                    entity.syncData();
                }
            }
        }

        // Phase 2: Advance baby villager age
        if (entity.hasVillager()) {
            SimpleVillagerEntity villager = entity.getVillagerEntity();
            if (villager != null) {
                int prevAge = villager.getAge();
                int newAge = prevAge + incubatorSpeed();
                villager.setAge(newAge);

                // Baby -> Adult transition
                if (prevAge < 0 && newAge >= 0) {
                    ItemStack adultVillager = entity.removeVillager();
                    if (!adultVillager.isEmpty()) {
                        // Find empty output slot
                        for (int i = 0; i < entity.outputInventory.getContainerSize(); i++) {
                            if (entity.outputInventory.getItem(i).isEmpty()) {
                                entity.outputInventory.setItem(i, adultVillager);
                                entity.syncData();
                                entity.setChanged();
                                break;
                            }
                        }
                    }
                    return;
                }

                // Random ambient sounds while incubating
                if (level.getRandom().nextInt(40) == 0) {
                    VillagerBlockBase.playVillagerSound(level, pos, SoundEvents.VILLAGER_AMBIENT);
                }

                entity.setChanged();
                if (level.getGameTime() % 20 == 0) {
                    entity.syncData();
                }
            }
        }
    }

    // --- NBT ---

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        if (hasVillager()) {
            output.store("Villager", ItemStack.CODEC, getVillager());
        }

        ValueOutput inputChild = output.child("InputInventory");
        for (int i = 0; i < inputInventory.getContainerSize(); i++) {
            int idx = i;
            ItemStack stack = inputInventory.getItem(idx);
            if (!stack.isEmpty()) {
                inputChild.store("Slot_" + idx, ItemStack.CODEC, stack);
            }
        }

        ValueOutput outputChild = output.child("OutputInventory");
        for (int i = 0; i < outputInventory.getContainerSize(); i++) {
            int idx = i;
            ItemStack stack = outputInventory.getItem(idx);
            if (!stack.isEmpty()) {
                outputChild.store("Slot_" + idx, ItemStack.CODEC, stack);
            }
        }
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        var optVillager = input.read("Villager", ItemStack.CODEC);
        if (optVillager.isPresent()) {
            villager = optVillager.get();
            villagerEntity = null;
        } else {
            villager = ItemStack.EMPTY;
            villagerEntity = null;
        }

        ValueInput inputChild = input.childOrEmpty("InputInventory");
        for (int i = 0; i < inputInventory.getContainerSize(); i++) {
            int idx = i;
            var opt = inputChild.read("Slot_" + idx, ItemStack.CODEC);
            opt.ifPresent(s -> inputInventory.setItem(idx, s));
        }

        ValueInput outputChild = input.childOrEmpty("OutputInventory");
        for (int i = 0; i < outputInventory.getContainerSize(); i++) {
            int idx = i;
            var opt = outputChild.read("Slot_" + idx, ItemStack.CODEC);
            opt.ifPresent(s -> outputInventory.setItem(idx, s));
        }

        super.loadAdditional(input);
    }

    @Override
    public void setRemoved() {
        super.setRemoved();
        villagerEntity = null;
    }

    // --- Container + WorldlyContainer for hopper support ---

    private static final int[] HOPPER_INPUT_SLOTS = {0, 1, 2, 3};
    private static final int[] HOPPER_OUTPUT_SLOTS = {4, 5, 6, 7};

    private int combinedSize() {
        return inputInventory.getContainerSize() + outputInventory.getContainerSize();
    }

    private Container getInventoryForSlot(int slot) {
        if (slot < inputInventory.getContainerSize()) return inputInventory;
        return outputInventory;
    }

    private int getLocalSlot(int slot) {
        if (slot < inputInventory.getContainerSize()) return slot;
        return slot - inputInventory.getContainerSize();
    }

    @Override
    public int getContainerSize() {
        return combinedSize();
    }

    @Override
    public boolean isEmpty() {
        return inputInventory.isEmpty() && outputInventory.isEmpty();
    }

    @Override
    public ItemStack getItem(int slot) {
        return getInventoryForSlot(slot).getItem(getLocalSlot(slot));
    }

    @Override
    public ItemStack removeItem(int slot, int amount) {
        return getInventoryForSlot(slot).removeItem(getLocalSlot(slot), amount);
    }

    @Override
    public ItemStack removeItemNoUpdate(int slot) {
        return getInventoryForSlot(slot).removeItemNoUpdate(getLocalSlot(slot));
    }

    @Override
    public void setItem(int slot, ItemStack stack) {
        getInventoryForSlot(slot).setItem(getLocalSlot(slot), stack);
    }

    @Override
    public boolean stillValid(Player player) {
        return Container.stillValidBlockEntity(this, player);
    }

    @Override
    public void clearContent() {
        inputInventory.clearContent();
        outputInventory.clearContent();
    }

    @Override
    public boolean canPlaceItem(int slot, ItemStack stack) {
        if (slot < inputInventory.getContainerSize()) {
            return stack.getItem() instanceof VillagerItem && VillagerItem.isBaby(stack);
        }
        return false;
    }

    @Override
    public int[] getSlotsForFace(Direction side) {
        if (side == Direction.UP) return HOPPER_INPUT_SLOTS;
        if (side == Direction.DOWN) return HOPPER_OUTPUT_SLOTS;
        return new int[0];
    }

    @Override
    public boolean canPlaceItemThroughFace(int slot, ItemStack stack, Direction side) {
        if (side == Direction.UP) return canPlaceItem(slot, stack);
        return false;
    }

    @Override
    public boolean canTakeItemThroughFace(int slot, ItemStack stack, Direction side) {
        return side == Direction.DOWN && slot >= inputInventory.getContainerSize();
    }
}
