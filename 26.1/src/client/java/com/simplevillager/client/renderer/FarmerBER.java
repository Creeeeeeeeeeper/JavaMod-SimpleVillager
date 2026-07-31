package com.simplevillager.client.renderer;

import com.simplevillager.blockentity.FarmerBlockEntity;
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
import net.minecraft.core.Direction;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionf;

public class FarmerBER implements BlockEntityRenderer<FarmerBlockEntity, FarmerRenderState> {

    private static final BlockDisplayContext DISPLAY_CONTEXT = BlockDisplayContext.create();

    private final BlockModelResolver blockModelResolver;
    private VillagerRenderer villagerRenderer;

    public FarmerBER(BlockEntityRendererProvider.Context context) {
        this.blockModelResolver = context.blockModelResolver();
    }

    private VillagerRenderer getVillagerRenderer() {
        if (villagerRenderer == null) {
            try {
                var temp = new SimpleVillagerEntity(net.minecraft.world.entity.EntityType.VILLAGER, Minecraft.getInstance().level);
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
    public FarmerRenderState createRenderState() {
        return new FarmerRenderState();
    }

    @Override
    public void extractRenderState(FarmerBlockEntity blockEntity, FarmerRenderState state, float partialTick, Vec3 cameraPos, ModelFeatureRenderer.CrumblingOverlay overlay) {
        net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState.extractBase(blockEntity, state, overlay);
        state.renderContents = RenderConfig.shouldRender(blockEntity, cameraPos);
        if (!state.renderContents) return;

        int lightCoords = 15728880;
        if (blockEntity.getLevel() != null) {
            var pos = blockEntity.getBlockPos();
            int blockLight = blockEntity.getLevel().getBrightness(LightLayer.BLOCK, pos);
            int skyLight = blockEntity.getLevel().getBrightness(LightLayer.SKY, pos);
            lightCoords = skyLight << 20 | blockLight << 4;
        }
        state.lightCoords = lightCoords;

        state.facing = Direction.NORTH;
        if (blockEntity.getBlockState().hasProperty(BlockStateProperties.HORIZONTAL_FACING)) {
            state.facing = blockEntity.getBlockState().getValue(BlockStateProperties.HORIZONTAL_FACING);
        }

        state.renderVillager = false;
        VillagerRenderer vr = getVillagerRenderer();
        if (vr != null) {
            SimpleVillagerEntity v = blockEntity.getVillagerEntity();
            if (v != null) {
                state.renderVillager = true;
                syncOldValues(v);
                vr.extractRenderState(v, state.villagerRenderState, partialTick);
                state.villagerRenderState.lightCoords = lightCoords;
            }
        }

        BlockState cropState = blockEntity.getCrop();
        if (cropState != null) {
            this.blockModelResolver.update(state.crop, cropState, DISPLAY_CONTEXT);
        } else {
            state.crop.clear();
        }
    }

    @Override
    public void submit(FarmerRenderState state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState cameraState) {
        if (!state.renderContents) return;
        VillagerRenderer vr = getVillagerRenderer();

        poseStack.pushPose();

        // Villager (back side, facing direction)
        if (state.renderVillager && vr != null) {
            poseStack.pushPose();
            poseStack.translate(0.5d, 0.0625d, 0.5d);
            poseStack.mulPose(new Quaternionf().rotateY((float) Math.toRadians(-state.facing.toYRot())));
            poseStack.translate(0.0d, 0.0d, -0.25d);
            poseStack.scale(0.45f, 0.45f, 0.45f);
            vr.submit(state.villagerRenderState, poseStack, collector, cameraState);
            poseStack.popPose();
        }

        // Crop (on farmland floor)
        if (!state.crop.isEmpty()) {
            poseStack.pushPose();
            poseStack.translate(0.5d, 0.0625d, 0.5d);
            poseStack.mulPose(new Quaternionf().rotateY((float) Math.toRadians(-state.facing.toYRot())));
            poseStack.translate(0.0d, 0.0d, 0.125d);
            poseStack.translate(-0.5d, 0.0d, -0.5d);
            poseStack.scale(0.45f, 0.45f, 0.45f);
            poseStack.translate(0.6111111111111112d, 0.0d, 0.6111111111111112d);
            state.crop.submit(poseStack, collector, state.lightCoords, OverlayTexture.NO_OVERLAY, 0);
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
