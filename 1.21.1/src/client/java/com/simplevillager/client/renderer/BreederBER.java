package com.simplevillager.client.renderer;

import com.simplevillager.blockentity.BreederBlockEntity;
import com.simplevillager.client.BedConfig;
import com.simplevillager.entity.SimpleVillagerEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BedPart;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionf;

public class BreederBER implements BlockEntityRenderer<BreederBlockEntity> {

    @Override
    public void render(BreederBlockEntity blockEntity, float partialTick, PoseStack poseStack, MultiBufferSource buffer, int packedLight, int packedOverlay) {
        if (!RenderConfig.shouldRender(blockEntity, cameraPos())) return;

        Direction facing = Direction.NORTH;
        if (blockEntity.getBlockState().hasProperty(BlockStateProperties.HORIZONTAL_FACING)) {
            facing = blockEntity.getBlockState().getValue(BlockStateProperties.HORIZONTAL_FACING);
        }

        EntityRenderDispatcher dispatcher = Minecraft.getInstance().getEntityRenderDispatcher();

        poseStack.pushPose();

        SimpleVillagerEntity v1 = blockEntity.getVillagerEntity1();
        if (v1 != null) {
            var renderer = dispatcher.getRenderer(v1);
            if (renderer != null) {
                poseStack.pushPose();
                poseStack.translate(0.5d, 0.0625d, 0.5d);
                poseStack.mulPose(new Quaternionf().rotateY((float) Math.toRadians(-facing.toYRot())));
                poseStack.translate(-0.3125d, 0.0d, 0.0d);
                poseStack.mulPose(new Quaternionf().rotateY((float) Math.toRadians(90.0f)));
                poseStack.scale(0.45f, 0.45f, 0.45f);
                syncOldValues(v1);
                renderer.render(v1, 0f, partialTick, poseStack, buffer, packedLight);
                poseStack.popPose();
            }
        }

        SimpleVillagerEntity v2 = blockEntity.getVillagerEntity2();
        if (v2 != null) {
            var renderer = dispatcher.getRenderer(v2);
            if (renderer != null) {
                poseStack.pushPose();
                poseStack.translate(0.5d, 0.0625d, 0.5d);
                poseStack.mulPose(new Quaternionf().rotateY((float) Math.toRadians(-facing.toYRot())));
                poseStack.translate(0.3125d, 0.0d, 0.0d);
                poseStack.mulPose(new Quaternionf().rotateY((float) Math.toRadians(-90.0f)));
                poseStack.scale(0.45f, 0.45f, 0.45f);
                syncOldValues(v2);
                renderer.render(v2, 0f, partialTick, poseStack, buffer, packedLight);
                poseStack.popPose();
            }
        }

        // Bed (center) - 1.21.1 renders the whole bed from a single block state
        BlockState bedState = Blocks.RED_BED.defaultBlockState();
        poseStack.pushPose();
        poseStack.translate(0.5d, 0.0625d, 0.5d);
        poseStack.mulPose(new Quaternionf().rotateY((float) Math.toRadians(-facing.toYRot())));
        poseStack.translate(0.0d, 0.0d, BedConfig.offsetZ);
        poseStack.translate(-0.5d, 0.0d, -0.5d);
        float bedScale = (float) BedConfig.scale;
        poseStack.scale(bedScale, bedScale, bedScale);
        poseStack.translate(BedConfig.translateX, BedConfig.translateY, BedConfig.translateZ);
        poseStack.translate(0.0d, 0.0d, BedConfig.rotateCenterZ);
        poseStack.mulPose(new Quaternionf().rotateY((float) Math.toRadians(BedConfig.rotationY)));
        poseStack.translate(0.0d, 0.0d, -BedConfig.rotateCenterZ);
        Minecraft.getInstance().getBlockRenderer().renderSingleBlock(bedState, poseStack, buffer, packedLight, packedOverlay);
        poseStack.popPose();

        poseStack.popPose();
    }

    private static Vec3 cameraPos() {
        return Minecraft.getInstance().gameRenderer.getMainCamera().getPosition();
    }

    private static void syncOldValues(net.minecraft.world.entity.LivingEntity entity) {
        entity.xo = entity.getX();
        entity.yo = entity.getY();
        entity.zo = entity.getZ();
        entity.yBodyRotO = entity.yBodyRot;
        entity.yHeadRotO = entity.yHeadRot;
    }
}
