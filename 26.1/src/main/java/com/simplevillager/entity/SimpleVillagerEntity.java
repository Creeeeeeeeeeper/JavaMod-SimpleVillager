package com.simplevillager.entity;

import com.simplevillager.util.NbtHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.core.GlobalPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.TagValueOutput;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import java.lang.reflect.Method;

public class SimpleVillagerEntity extends Villager {

    public SimpleVillagerEntity(EntityType<? extends Villager> type, Level level) {
        super(type, level);
    }

    @Override
    public int getId() {
        try {
            java.lang.reflect.Field idField = net.minecraft.world.entity.Entity.class.getDeclaredField("id");
            idField.setAccessible(true);
            int id = idField.getInt(this);
            if (id == 0) {
                return Integer.MIN_VALUE;
            }
        } catch (Exception e) {
            // fallback
        }
        return super.getId();
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
        try {
            TagValueOutput output = NbtHelper.createValueOutput(villager.registryAccess());
            Method method = net.minecraft.world.entity.Entity.class.getDeclaredMethod("addAdditionalSaveData", ValueOutput.class);
            method.setAccessible(true);
            method.invoke(villager, output);
            return NbtHelper.toTag(output);
        } catch (Exception e) {
            throw new RuntimeException("Failed to save villager data", e);
        }
    }

    public void setupBrainForBlock(Level level, BlockPos blockPos) {
        GlobalPos jobSite = GlobalPos.of(level.dimension(), blockPos);
        this.getBrain().setMemory(MemoryModuleType.JOB_SITE, jobSite);
    }

    public static void loadVillager(Villager villager, CompoundTag tag) {
        try {
            ValueInput input = NbtHelper.createValueInput(villager.registryAccess(), tag);
            Method method = net.minecraft.world.entity.Entity.class.getDeclaredMethod("readAdditionalSaveData", ValueInput.class);
            method.setAccessible(true);
            method.invoke(villager, input);
        } catch (Exception e) {
            throw new RuntimeException("Failed to load villager data", e);
        }
    }
}
