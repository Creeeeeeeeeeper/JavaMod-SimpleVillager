package com.simplevillager.client.renderer;

import com.simplevillager.blockentity.FarmerBlockEntity;
import com.simplevillager.entity.SimpleVillagerEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionf;

public class FarmerBER implements BlockEntityRenderer<FarmerBlockEntity> {

    @Override
    public void render(FarmerBlockEntity blockEntity, float partialTick, PoseStack poseStack, MultiBufferSource buffer, int packedLight, int packedOverlay) {
        if (!RenderConfig.shouldRender(blockEntity, cameraPos())) return;

        Direction facing = Direction.NORTH;
        if (blockEntity.getBlockState().hasProperty(BlockStateProperties.HORIZONTAL_FACING)) {
            facing = blockEntity.getBlockState().getValue(BlockStateProperties.HORIZONTAL_FACING);
        }

        poseStack.pushPose();

        SimpleVillagerEntity v = blockEntity.getVillagerEntity();
        if (v != null) {
            EntityRenderDispatcher dispatcher = Minecraft.getInstance().getEntityRenderDispatcher();
            var renderer = dispatcher.getRenderer(v);
            if (renderer != null) {
                poseStack.pushPose();
                poseStack.translate(0.5d, 0.0625d, 0.5d);
                poseStack.mulPose(new Quaternionf().rotateY((float) Math.toRadians(-facing.toYRot())));
                poseStack.translate(0.0d, 0.0d, -0.25d);
                poseStack.scale(0.45f, 0.45f, 0.45f);
                syncOldValues(v);
                renderer.render(v, 0f, partialTick, poseStack, buffer, packedLight);
                poseStack.popPose();
            }
        }

        BlockState crop = blockEntity.getCrop();
        if (crop != null) {
            poseStack.pushPose();
            poseStack.translate(0.5d, 0.0625d, 0.5d);
            poseStack.mulPose(new Quaternionf().rotateY((float) Math.toRadians(-facing.toYRot())));
            poseStack.translate(0.0d, 0.0d, 0.125d);
            poseStack.translate(-0.5d, 0.0d, -0.5d);
            poseStack.scale(0.45f, 0.45f, 0.45f);
            poseStack.translate(0.6111111111111112d, 0.0d, 0.6111111111111112d);
            Minecraft.getInstance().getBlockRenderer().renderSingleBlock(crop, poseStack, buffer, packedLight, packedOverlay);
            poseStack.popPose();
        }

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
