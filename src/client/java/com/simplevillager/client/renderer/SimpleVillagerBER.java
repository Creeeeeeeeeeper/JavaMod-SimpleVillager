package com.simplevillager.client.renderer;

import com.simplevillager.blockentity.*;
import com.simplevillager.entity.SimpleVillagerEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionf;

public class SimpleVillagerBER implements BlockEntityRenderer<BlockEntity, VillagerBERState> {

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
    public VillagerBERState createRenderState() {
        return new VillagerBERState();
    }

    @Override
    public void extractRenderState(BlockEntity blockEntity, VillagerBERState state, float partialTick, Vec3 cameraPos, net.minecraft.client.renderer.feature.ModelFeatureRenderer.CrumblingOverlay overlay) {
        net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState.extractBase(blockEntity, state, overlay);
        state.clear();

        int lightCoords = 15728880;
        if (blockEntity.getLevel() != null) {
            BlockPos pos = blockEntity.getBlockPos();
            int blockLight = blockEntity.getLevel().getBrightness(LightLayer.BLOCK, pos);
            int skyLight = blockEntity.getLevel().getBrightness(LightLayer.SKY, pos);
            lightCoords = skyLight << 20 | blockLight << 4;
        }

        SimpleVillagerEntity villager = getVillager(blockEntity);
        if (villager == null) return;

        state.facing = getFacing(blockEntity);

        EntityRenderDispatcher dispatcher = Minecraft.getInstance().getEntityRenderDispatcher();
        syncOldValues(villager);
        EntityRenderState renderState = dispatcher.extractEntity(villager, partialTick);
        renderState.lightCoords = lightCoords;
        state.addEntity(renderState, 0, 0, 0, scale, yRotOffset);
    }

    @Override
    public void submit(VillagerBERState state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState cameraState) {
        EntityRenderDispatcher dispatcher = Minecraft.getInstance().getEntityRenderDispatcher();
        for (VillagerBERState.EntityData entity : state.entities) {
            poseStack.pushPose();
            poseStack.translate(0.5f, 0.0625f, 0.5f);
            poseStack.mulPose(new Quaternionf().rotateY((float) Math.toRadians(-state.facing.toYRot())));
            poseStack.translate(entity.offsetX(), entity.offsetY(), entity.offsetZ());
            poseStack.scale(entity.scale(), entity.scale(), entity.scale());
            if (entity.yRotOffset() != 0) {
                poseStack.mulPose(new Quaternionf().rotateY((float) Math.toRadians(entity.yRotOffset())));
            }
            dispatcher.submit(entity.renderState(), cameraState, 0, 0, 0, poseStack, collector);
            poseStack.popPose();
        }
    }

    private SimpleVillagerEntity getVillager(BlockEntity be) {
        if (be instanceof FarmerBlockEntity farmer) return farmer.getVillagerEntity();
        if (be instanceof IncubatorBlockEntity inc) return inc.getVillagerEntity();
        if (be instanceof InventoryViewerBlockEntity viewer) return viewer.getVillagerEntity();
        return null;
    }

    private Direction getFacing(BlockEntity be) {
        if (be.getBlockState().hasProperty(net.minecraft.world.level.block.state.properties.BlockStateProperties.HORIZONTAL_FACING)) {
            return be.getBlockState().getValue(net.minecraft.world.level.block.state.properties.BlockStateProperties.HORIZONTAL_FACING);
        }
        return Direction.NORTH;
    }

    private static void syncOldValues(LivingEntity entity) {
        entity.xo = entity.getX();
        entity.yo = entity.getY();
        entity.zo = entity.getZ();
        entity.yBodyRotO = entity.yBodyRot;
        entity.yHeadRotO = entity.yHeadRot;
    }
}
