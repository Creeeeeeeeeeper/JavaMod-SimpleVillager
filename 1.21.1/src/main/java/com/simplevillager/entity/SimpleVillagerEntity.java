package com.simplevillager.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.core.GlobalPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.level.Level;

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
        CompoundTag tag = new CompoundTag();
        this.addAdditionalSaveData(tag);
        return tag;
    }

    public void fromCompoundTag(CompoundTag tag) {
        this.readAdditionalSaveData(tag);
    }

    public static SimpleVillagerEntity fromTag(Level level, CompoundTag tag) {
        SimpleVillagerEntity entity = new SimpleVillagerEntity(EntityType.VILLAGER, level);
        entity.readAdditionalSaveData(tag);
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
        CompoundTag tag = new CompoundTag();
        ((com.simplevillager.mixin.EntityAccessor) villager).SimpleVillager$callAddAdditionalSaveData(tag);
        return tag;
    }

    public void setupBrainForBlock(Level level, BlockPos blockPos) {
        GlobalPos jobSite = GlobalPos.of(level.dimension(), blockPos);
        this.getBrain().setMemory(MemoryModuleType.JOB_SITE, jobSite);
    }

    public static void loadVillager(Villager villager, CompoundTag tag) {
        ((com.simplevillager.mixin.EntityAccessor) villager).SimpleVillager$callReadAdditionalSaveData(tag);
    }
}
