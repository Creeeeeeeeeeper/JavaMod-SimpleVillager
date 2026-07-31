package com.simplevillager.client.renderer;

import com.simplevillager.blockentity.WorkstationBlockEntity;
import com.simplevillager.entity.SimpleVillagerEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.block.BlockModelResolver;
import net.minecraft.client.renderer.block.model.BlockDisplayContext;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.entity.VillagerRenderer;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionf;

public class TraderBER implements BlockEntityRenderer<BlockEntity, TraderRenderState> {

    private static final BlockDisplayContext DISPLAY_CONTEXT = BlockDisplayContext.create();

    private final BlockModelResolver blockModelResolver;
    private VillagerRenderer villagerRenderer;

    public TraderBER(BlockEntityRendererProvider.Context context) {
        this.blockModelResolver = context.blockModelResolver();
    }

    private VillagerRenderer getVillagerRenderer() {
        if (villagerRenderer == null) {
            try {
                var temp = new SimpleVillagerEntity(net.minecraft.world.entity.EntityTypes.VILLAGER, Minecraft.getInstance().level);
                var renderer = Minecraft.getInstance().getEntityRenderDispatcher().getRenderer(temp);
                if (renderer instanceof VillagerRenderer vr) {
                    villagerRenderer = vr;
                }
            } catch (Exception ignored) {
            }
        }
        return villagerRenderer;
    }

    @Override
    public TraderRenderState createRenderState() {
        return new TraderRenderState();
    }

    @Override
    public void extractRenderState(BlockEntity blockEntity, TraderRenderState state, float partialTick, Vec3 cameraPos, ModelFeatureRenderer.CrumblingOverlay overlay) {
        net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState.extractBase(blockEntity, state, overlay);
        state.renderVillager = false;
        state.villagerRenderState = null;
        state.renderContents = RenderConfig.shouldRender(blockEntity, cameraPos);
        if (!state.renderContents) return;

        int lightCoords = 15728880;
        if (blockEntity.getLevel() != null) {
            BlockPos pos = blockEntity.getBlockPos();
            int blockLight = blockEntity.getLevel().getBrightness(LightLayer.BLOCK, pos);
            int skyLight = blockEntity.getLevel().getBrightness(LightLayer.SKY, pos);
            lightCoords = skyLight << 20 | blockLight << 4;
        }
        state.worldLight = lightCoords;

        if (blockEntity instanceof WorkstationBlockEntity wbe) {
            SimpleVillagerEntity villager = wbe.getVillagerEntity();
            if (villager != null && getVillagerRenderer() != null) {
                state.renderVillager = true;
                state.villagerRenderState = getVillagerRenderer().createRenderState();
                syncOldValues(villager);
                getVillagerRenderer().extractRenderState(villager, state.villagerRenderState, partialTick);
                state.villagerRenderState.lightCoords = state.worldLight;
            }

            state.workstation.clear();
            if (wbe.hasWorkstation()) {
                Block workstation = wbe.getWorkstation();
                if (workstation != null && workstation != Blocks.AIR) {
                    blockModelResolver.update(state.workstation, workstation.defaultBlockState(), DISPLAY_CONTEXT);
                }
            }
        }

        state.facing = Direction.NORTH;
        if (blockEntity.getBlockState().hasProperty(BlockStateProperties.HORIZONTAL_FACING)) {
            state.facing = blockEntity.getBlockState().getValue(BlockStateProperties.HORIZONTAL_FACING);
        }
    }

    @Override
    public void submit(TraderRenderState state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState cameraState) {
        if (!state.renderContents) return;
        poseStack.pushPose();

        if (state.renderVillager && state.villagerRenderState != null) {
            poseStack.pushPose();
            poseStack.translate(0.5f, 0.0625f, 0.5f);
            poseStack.mulPose(new Quaternionf().rotateY((float) Math.toRadians(-state.facing.toYRot())));
            poseStack.translate(0.0f, 0.0f, -0.2f);
            poseStack.scale(0.45f, 0.45f, 0.45f);
            getVillagerRenderer().submit(state.villagerRenderState, poseStack, collector, cameraState);
            poseStack.popPose();
        }

        if (!state.workstation.isEmpty()) {
            poseStack.pushPose();
            poseStack.translate(0.5f, 0.0625f, 0.5f);
            poseStack.mulPose(new Quaternionf().rotateY((float) Math.toRadians(-state.facing.toYRot())));
            poseStack.translate(0.0f, 0.0f, 0.15f);
            poseStack.translate(-0.5f, 0.0f, -0.5f);
            poseStack.scale(0.4f, 0.4f, 0.4f);
            poseStack.translate(0.75f, 0.0f, 0.75f);
            state.workstation.submit(poseStack, collector, state.worldLight, OverlayTexture.NO_OVERLAY, 0);
            poseStack.popPose();
        }

        poseStack.popPose();
    }

    private static void syncOldValues(net.minecraft.world.entity.LivingEntity entity) {
        entity.xo = entity.getX();
        entity.yo = entity.getY();
        entity.zo = entity.getZ();
        entity.yBodyRotO = entity.yBodyRot;
        entity.yHeadRotO = entity.yHeadRot;
    }
}
