package com.simplevillager.client.renderer;

import com.simplevillager.config.ModConfig;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.Vec3;

public final class RenderConfig {
    private RenderConfig() {
    }

    public static boolean shouldRender(BlockEntity blockEntity, Vec3 cameraPos) {
        if (!ModConfig.client().renderBlockContents) return false;
        BlockPos pos = blockEntity.getBlockPos();
        double dist = cameraPos.distanceToSqr(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5);
        int range = ModConfig.client().blockRenderDistance;
        return dist <= (double) range * range;
    }
}
