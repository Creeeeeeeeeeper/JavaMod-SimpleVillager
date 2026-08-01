package com.simplevillager.blockentity;

import com.simplevillager.config.ModConfig;
import com.simplevillager.blocks.VillagerBlockBase;
import com.simplevillager.datacomponent.VillagerData;
import com.simplevillager.entity.SimpleVillagerEntity;
import com.simplevillager.items.VillagerItem;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import org.jetbrains.annotations.Nullable;

public class IncubatorBlockEntity extends VillagerBlockEntityBase {

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
        if (villager.isEmpty()) return null;
        if (villagerEntity == null && level != null) {
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
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider provider) {
        super.saveAdditional(tag, provider);
        if (hasVillager()) {
            tag.put("Villager", getVillager().saveOptional(provider));
        }

        CompoundTag inputChild = new CompoundTag();
        for (int i = 0; i < inputInventory.getContainerSize(); i++) {
            ItemStack stack = inputInventory.getItem(i);
            if (!stack.isEmpty()) {
                inputChild.put("Slot_" + i, stack.saveOptional(provider));
            }
        }
        tag.put("InputInventory", inputChild);

        CompoundTag outputChild = new CompoundTag();
        for (int i = 0; i < outputInventory.getContainerSize(); i++) {
            ItemStack stack = outputInventory.getItem(i);
            if (!stack.isEmpty()) {
                outputChild.put("Slot_" + i, stack.saveOptional(provider));
            }
        }
        tag.put("OutputInventory", outputChild);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider provider) {
        if (tag.contains("Villager")) {
            villager = ItemStack.parseOptional(provider, tag.getCompound("Villager"));
            villagerEntity = null;
        } else {
            villager = ItemStack.EMPTY;
            villagerEntity = null;
        }

        CompoundTag inputChild = tag.getCompound("InputInventory");
        for (int i = 0; i < inputInventory.getContainerSize(); i++) {
            if (inputChild.contains("Slot_" + i)) {
                inputInventory.setItem(i, ItemStack.parseOptional(provider, inputChild.getCompound("Slot_" + i)));
            }
        }

        CompoundTag outputChild = tag.getCompound("OutputInventory");
        for (int i = 0; i < outputInventory.getContainerSize(); i++) {
            if (outputChild.contains("Slot_" + i)) {
                outputInventory.setItem(i, ItemStack.parseOptional(provider, outputChild.getCompound("Slot_" + i)));
            }
        }

        super.loadAdditional(tag, provider);
    }

    @Override
    public void setRemoved() {
        super.setRemoved();
        villagerEntity = null;
    }
}
