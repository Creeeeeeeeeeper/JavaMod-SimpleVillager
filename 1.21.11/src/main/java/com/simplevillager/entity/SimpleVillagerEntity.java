package com.simplevillager.entity;

import com.simplevillager.util.NbtHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.core.GlobalPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.TagValueOutput;
import net.minecraft.world.level.storage.ValueInput;

public class SimpleVillagerEntity extends Villager {

    public SimpleVillagerEntity(EntityType<? extends Villager> type, Level level) {
        super(type, level);
    }

    @Override
    public int getId() {
        int id = ((com.simplevillager.mixin.EntityAccessor) this).SimpleVillager$getId();
        if (id == 0) {
            return Integer.MIN_VALUE;
        }
        return id;
    }

    public CompoundTag toCompoundTag() {
        TagValueOutput output = NbtHelper.createValueOutput(this.registryAccess());
        this.addAdditionalSaveData(output);
        return NbtHelper.toTag(output);
    }

    public void fromCompoundTag(CompoundTag tag) {
        ValueInput input = NbtHelper.createValueInput(this.registryAccess(), tag);
        this.readAdditionalSaveData(input);
    }

    public static SimpleVillagerEntity fromTag(Level level, CompoundTag tag) {
        SimpleVillagerEntity entity = new SimpleVillagerEntity(EntityType.VILLAGER, level);
        ValueInput input = NbtHelper.createValueInput(level.registryAccess(), tag);
        entity.readAdditionalSaveData(input);
        return entity;
    }

    public void spawnInWorld(ServerLevel level, BlockPos pos) {
        this.setPos(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5);
        level.addFreshEntity(this);
    }

    public void spawnInWorld(ServerLevel level, BlockPos pos, Direction direction) {
        this.setPos(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5);
        this.setYRot(direction.toYRot());
        level.addFreshEntity(this);
    }

    public static CompoundTag saveVillager(Villager villager) {
        TagValueOutput output = NbtHelper.createValueOutput(villager.registryAccess());
        ((com.simplevillager.mixin.EntityAccessor) villager).SimpleVillager$callAddAdditionalSaveData(output);
        return NbtHelper.toTag(output);
    }

    public void setupBrainForBlock(Level level, BlockPos blockPos) {
        GlobalPos jobSite = GlobalPos.of(level.dimension(), blockPos);
        this.getBrain().setMemory(MemoryModuleType.JOB_SITE, jobSite);
    }

    public static void loadVillager(Villager villager, CompoundTag tag) {
        ValueInput input = NbtHelper.createValueInput(villager.registryAccess(), tag);
        ((com.simplevillager.mixin.EntityAccessor) villager).SimpleVillager$callReadAdditionalSaveData(input);
    }
}
