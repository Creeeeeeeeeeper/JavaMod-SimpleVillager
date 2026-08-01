package com.simplevillager.blockentity;

import com.simplevillager.datacomponent.VillagerData;
import com.simplevillager.entity.SimpleVillagerEntity;
import com.simplevillager.items.VillagerItem;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

public class InventoryViewerBlockEntity extends VillagerBlockEntityBase {

    private ItemStack villager = ItemStack.EMPTY;
    @Nullable
    private SimpleVillagerEntity villagerEntity = null;

    public InventoryViewerBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.INVENTORY_VIEWER, pos, state);
    }

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

    public SimpleContainer getVillagerInventory() {
        SimpleVillagerEntity entity = getVillagerEntity();
        return entity != null ? entity.getInventory() : new SimpleContainer(8);
    }

    public ItemStack getEquipment(EquipmentSlot slot) {
        SimpleVillagerEntity entity = getVillagerEntity();
        return entity != null ? entity.getItemBySlot(slot) : ItemStack.EMPTY;
    }

    public void setEquipment(EquipmentSlot slot, ItemStack stack) {
        SimpleVillagerEntity entity = getVillagerEntity();
        if (entity != null) {
            entity.setItemSlot(slot, stack);
            syncData();
        }
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, InventoryViewerBlockEntity entity) {
        if (level.isClientSide()) return;
        if (!entity.hasVillager()) return;

        if (level.getRandom().nextInt(40) == 0) {
            com.simplevillager.blocks.VillagerBlockBase.playVillagerSound(level, pos, SoundEvents.VILLAGER_AMBIENT);
        }
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider provider) {
        super.saveAdditional(tag, provider);
        if (hasVillager()) {
            tag.put("Villager", getVillager().saveOptional(provider));
        }
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider provider) {
        if (tag.contains("Villager")) {
            villager = ItemStack.parseOptional(provider, tag.getCompound("Villager"));
        } else {
            villager = ItemStack.EMPTY;
        }
        villagerEntity = null;
        super.loadAdditional(tag, provider);
    }

    @Override
    public void setRemoved() {
        super.setRemoved();
        villagerEntity = null;
    }
}
