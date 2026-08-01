package com.simplevillager.client.renderer;

import com.simplevillager.blockentity.FarmerBlockEntity;
import com.simplevillager.blockentity.IncubatorBlockEntity;
import com.simplevillager.blockentity.InventoryViewerBlockEntity;
import com.simplevillager.entity.SimpleVillagerEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionf;

public class SimpleVillagerBER implements BlockEntityRenderer<BlockEntity> {

    private final float scale;
    private final float offsetX;
    private final float offsetZ;
    private final float yRotOffset;

    public SimpleVillagerBER(float scale, float offsetX, float offsetZ, float yRotOffset) {
        this.scale = scale;
        this.offsetX = offsetX;
        this.offsetZ = offsetZ;
        this.yRotOffset = yRotOffset;
    }

    @Override
    public void render(BlockEntity blockEntity, float partialTick, PoseStack poseStack, MultiBufferSource buffer, int packedLight, int packedOverlay) {
        if (!RenderConfig.shouldRender(blockEntity, cameraPos())) return;
        SimpleVillagerEntity villager = getVillager(blockEntity);
        if (villager == null) return;
        EntityRenderDispatcher dispatcher = Minecraft.getInstance().getEntityRenderDispatcher();
        var renderer = dispatcher.getRenderer(villager);
        if (renderer == null) return;

        Direction facing = getFacing(blockEntity);
        poseStack.pushPose();
        poseStack.translate(0.5f, 0.0625f, 0.5f);
        poseStack.mulPose(new Quaternionf().rotateY((float) Math.toRadians(-facing.toYRot())));
        poseStack.translate(offsetX, 0, offsetZ);
        poseStack.scale(scale, scale, scale);
        if (yRotOffset != 0) {
            poseStack.mulPose(new Quaternionf().rotateY((float) Math.toRadians(yRotOffset)));
        }
        syncOldValues(villager);
        renderer.render(villager, 0f, partialTick, poseStack, buffer, packedLight);
        poseStack.popPose();
    }

    private static Vec3 cameraPos() {
        return Minecraft.getInstance().gameRenderer.getMainCamera().getPosition();
    }

    private SimpleVillagerEntity getVillager(BlockEntity be) {
        if (be instanceof FarmerBlockEntity farmer) return farmer.getVillagerEntity();
        if (be instanceof IncubatorBlockEntity inc) return inc.getVillagerEntity();
        if (be instanceof InventoryViewerBlockEntity viewer) return viewer.getVillagerEntity();
        return null;
    }

    private Direction getFacing(BlockEntity be) {
        if (be.getBlockState().hasProperty(BlockStateProperties.HORIZONTAL_FACING)) {
            return be.getBlockState().getValue(BlockStateProperties.HORIZONTAL_FACING);
        }
        return Direction.NORTH;
    }

    private static void syncOldValues(net.minecraft.world.entity.LivingEntity entity) {
        entity.xo = entity.getX();
        entity.yo = entity.getY();
        entity.zo = entity.getZ();
        entity.yBodyRotO = entity.yBodyRot;
        entity.yHeadRotO = entity.yHeadRot;
    }
}
