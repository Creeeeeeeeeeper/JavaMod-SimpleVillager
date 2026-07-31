package com.simplevillager.blockentity;

import com.simplevillager.entity.SimpleVillagerEntity;
import net.minecraft.world.level.block.Block;

public interface WorkstationBlockEntity {
    SimpleVillagerEntity getVillagerEntity();
    Block getWorkstation();
    boolean hasWorkstation();
}
