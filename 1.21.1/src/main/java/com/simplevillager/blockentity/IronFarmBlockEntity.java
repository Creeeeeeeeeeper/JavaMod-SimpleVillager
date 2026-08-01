package com.simplevillager.blockentity;

import com.simplevillager.config.ModConfig;
import com.simplevillager.blocks.VillagerBlockBase;
import com.simplevillager.datacomponent.VillagerData;
import com.simplevillager.entity.SimpleVillagerEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.IronGolem;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

public class IronFarmBlockEntity extends VillagerBlockEntityBase implements Container {

    private static int golemSpawnTime() { return ModConfig.server().golemSpawnTime; }
    private static int golemKillTime() { return golemSpawnTime() + 100; }

    private static final ResourceKey<LootTable> GOLEM_LOOT_TABLE = ResourceKey.create(
            Registries.LOOT_TABLE, ResourceLocation.withDefaultNamespace("entities/iron_golem")
    );

    private ItemStack villager = ItemStack.EMPTY;
    @Nullable
    private SimpleVillagerEntity villagerEntity = null;
    private final SimpleContainer outputInventory = new SimpleContainer(4);
    private long timer = 0;

    public IronFarmBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.IRON_FARM, pos, state);
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

    public SimpleContainer getOutputInventory() {
        return outputInventory;
    }

    public long getTimer() {
        return timer;
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, IronFarmBlockEntity entity) {
        if (level.isClientSide()) return;

        if (entity.hasVillager()) {
            // Random ambient sounds
            if (level.getRandom().nextInt(40) == 0) {
                VillagerBlockBase.playVillagerSound(level, pos, SoundEvents.VILLAGER_AMBIENT);
            }
            if (level.getRandom().nextInt(40) == 0) {
                VillagerBlockBase.playVillagerSound(level, pos, SoundEvents.ZOMBIE_AMBIENT);
            }

            // Advance villager age
            SimpleVillagerEntity villager = entity.getVillagerEntity();
            if (villager != null) {
                int age = villager.getAge();
                if (age < 0) {
                    villager.setAge(age + 1);
                    if (villager.isBaby()) {
                        entity.syncData();
                    }
                }
            }

            // Timer logic
            if (entity.timer == golemSpawnTime()) {
                VillagerBlockBase.playVillagerSound(level, pos, SoundEvents.ZOMBIE_AMBIENT);
                entity.syncData();
            }

            if (entity.timer > golemSpawnTime() && entity.timer < golemKillTime()) {
                if (entity.timer % 20 == 0) {
                    VillagerBlockBase.playVillagerSound(level, pos, SoundEvents.IRON_GOLEM_HURT);
                }
            }

            if (entity.timer >= golemKillTime()) {
                VillagerBlockBase.playVillagerSound(level, pos, SoundEvents.IRON_GOLEM_DEATH);
                entity.generateLoot((ServerLevel) level, pos);
                entity.timer = 0;
                entity.syncData();
            }

            entity.timer++;
            entity.setChanged();
        } else {
            if (entity.timer >= golemSpawnTime()) {
                if (entity.timer % 20 == 0) {
                    VillagerBlockBase.playVillagerSound(level, pos, SoundEvents.IRON_GOLEM_HURT);
                }
                if (entity.timer >= golemKillTime()) {
                    VillagerBlockBase.playVillagerSound(level, pos, SoundEvents.IRON_GOLEM_DEATH);
                    entity.generateLoot((ServerLevel) level, pos);
                    entity.timer = 0;
                    entity.syncData();
                }
                entity.timer++;
                entity.setChanged();
            } else if (entity.timer != 0) {
                entity.timer = 0;
                entity.setChanged();
                entity.syncData();
            }
        }
    }

    private void generateLoot(ServerLevel serverLevel, BlockPos pos) {
        IronGolem golem = new IronGolem(EntityType.IRON_GOLEM, serverLevel);
        LootParams lootParams = new LootParams.Builder(serverLevel)
                .withParameter(LootContextParams.THIS_ENTITY, golem)
                .withParameter(LootContextParams.ORIGIN, new Vec3(pos.getX(), pos.getY(), pos.getZ()))
                .withParameter(LootContextParams.DAMAGE_SOURCE, serverLevel.damageSources().lava())
                .create(LootContextParamSets.ENTITY);

        LootTable lootTable = serverLevel.getServer().reloadableRegistries().getLootTable(GOLEM_LOOT_TABLE);
        lootTable.getRandomItems(lootParams, stack -> {
            outputInventory.addItem(stack);
        });
        setChanged();
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider provider) {
        super.saveAdditional(tag, provider);
        if (hasVillager()) {
            tag.put("Villager", getVillager().saveOptional(provider));
        }
        for (int i = 0; i < outputInventory.getContainerSize(); i++) {
            ItemStack stack = outputInventory.getItem(i);
            if (!stack.isEmpty()) {
                tag.put("Slot_" + i, stack.saveOptional(provider));
            }
        }
        tag.putLong("Timer", timer);
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

        for (int i = 0; i < outputInventory.getContainerSize(); i++) {
            if (tag.contains("Slot_" + i)) {
                outputInventory.setItem(i, ItemStack.parseOptional(provider, tag.getCompound("Slot_" + i)));
            }
        }

        timer = tag.getLong("Timer");
        super.loadAdditional(tag, provider);
    }

    @Override
    public void setRemoved() {
        super.setRemoved();
        villagerEntity = null;
    }

    // Container interface for hopper interaction
    @Override
    public int getContainerSize() {
        return outputInventory.getContainerSize();
    }

    @Override
    public boolean isEmpty() {
        return outputInventory.isEmpty();
    }

    @Override
    public ItemStack getItem(int slot) {
        return outputInventory.getItem(slot);
    }

    @Override
    public ItemStack removeItem(int slot, int amount) {
        return outputInventory.removeItem(slot, amount);
    }

    @Override
    public ItemStack removeItemNoUpdate(int slot) {
        return outputInventory.removeItemNoUpdate(slot);
    }

    @Override
    public void setItem(int slot, ItemStack stack) {
        outputInventory.setItem(slot, stack);
    }

    @Override
    public void setChanged() {
        super.setChanged();
    }

    @Override
    public boolean stillValid(Player player) {
        return outputInventory.stillValid(player);
    }

    @Override
    public void clearContent() {
        outputInventory.clearContent();
    }
}
