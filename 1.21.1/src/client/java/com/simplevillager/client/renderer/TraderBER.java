package com.simplevillager.client.renderer;

import com.simplevillager.blockentity.WorkstationBlockEntity;
import com.simplevillager.entity.SimpleVillagerEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionf;

public class TraderBER implements BlockEntityRenderer<BlockEntity> {

    @Override
    public void render(BlockEntity blockEntity, float partialTick, PoseStack poseStack, MultiBufferSource buffer, int packedLight, int packedOverlay) {
        if (!RenderConfig.shouldRender(blockEntity, cameraPos())) return;
        if (!(blockEntity instanceof WorkstationBlockEntity wbe)) return;

        Direction facing = Direction.NORTH;
        if (blockEntity.getBlockState().hasProperty(BlockStateProperties.HORIZONTAL_FACING)) {
            facing = blockEntity.getBlockState().getValue(BlockStateProperties.HORIZONTAL_FACING);
        }

        poseStack.pushPose();

        SimpleVillagerEntity villager = wbe.getVillagerEntity();
        if (villager != null) {
            EntityRenderDispatcher dispatcher = Minecraft.getInstance().getEntityRenderDispatcher();
            var renderer = dispatcher.getRenderer(villager);
            if (renderer != null) {
                poseStack.pushPose();
                poseStack.translate(0.5f, 0.0625f, 0.5f);
                poseStack.mulPose(new Quaternionf().rotateY((float) Math.toRadians(-facing.toYRot())));
                poseStack.translate(0.0f, 0.0f, -0.2f);
                poseStack.scale(0.45f, 0.45f, 0.45f);
                syncOldValues(villager);
                renderer.render(villager, 0f, partialTick, poseStack, buffer, packedLight);
                poseStack.popPose();
            }
        }

        Block workstation = wbe.getWorkstation();
        if (workstation != null && workstation != Blocks.AIR) {
            poseStack.pushPose();
            poseStack.translate(0.5f, 0.0625f, 0.5f);
            poseStack.mulPose(new Quaternionf().rotateY((float) Math.toRadians(-facing.toYRot())));
            poseStack.translate(0.0f, 0.0f, 0.15f);
            poseStack.translate(-0.5f, 0.0f, -0.5f);
            poseStack.scale(0.4f, 0.4f, 0.4f);
            poseStack.translate(0.75f, 0.0f, 0.75f);
            Minecraft.getInstance().getBlockRenderer().renderSingleBlock(workstation.defaultBlockState(), poseStack, buffer, packedLight, packedOverlay);
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
