package com.simplevillager.client.renderer;

import com.simplevillager.blockentity.BreederBlockEntity;
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
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BedPart;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionf;

public class BreederBER implements BlockEntityRenderer<BreederBlockEntity, BreederRenderState> {

    private static final BlockDisplayContext DISPLAY_CONTEXT = BlockDisplayContext.create();

    private final BlockModelResolver blockModelResolver;
    private VillagerRenderer villagerRenderer;

    public BreederBER(BlockEntityRendererProvider.Context context) {
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
    public BreederRenderState createRenderState() {
        return new BreederRenderState();
    }

    @Override
    public void extractRenderState(BreederBlockEntity blockEntity, BreederRenderState state, float partialTick, Vec3 cameraPos, ModelFeatureRenderer.CrumblingOverlay overlay) {
        net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState.extractBase(blockEntity, state, overlay);
        state.renderContents = RenderConfig.shouldRender(blockEntity, cameraPos);
        if (!state.renderContents) return;

        int lightCoords = 15728880;
        if (blockEntity.getLevel() != null) {
            BlockPos pos = blockEntity.getBlockPos();
            int blockLight = blockEntity.getLevel().getBrightness(LightLayer.BLOCK, pos);
            int skyLight = blockEntity.getLevel().getBrightness(LightLayer.SKY, pos);
            lightCoords = skyLight << 20 | blockLight << 4;
        }
        state.lightCoords = lightCoords;

        state.facing = Direction.NORTH;
        if (blockEntity.getBlockState().hasProperty(BlockStateProperties.HORIZONTAL_FACING)) {
            state.facing = blockEntity.getBlockState().getValue(BlockStateProperties.HORIZONTAL_FACING);
        }

        state.renderVillager1 = false;
        state.renderVillager2 = false;

        VillagerRenderer vr = getVillagerRenderer();
        if (vr != null) {
            SimpleVillagerEntity villager1 = blockEntity.getVillagerEntity1();
            if (villager1 != null) {
                state.renderVillager1 = true;
                syncOldValues(villager1);
                vr.extractRenderState(villager1, state.villagerRenderState1, partialTick);
                state.villagerRenderState1.lightCoords = lightCoords;
            }
            SimpleVillagerEntity villager2 = blockEntity.getVillagerEntity2();
            if (villager2 != null) {
                state.renderVillager2 = true;
                syncOldValues(villager2);
                vr.extractRenderState(villager2, state.villagerRenderState2, partialTick);
                state.villagerRenderState2.lightCoords = lightCoords;
            }
        }

        BlockState bedFootState = ((Block) Blocks.RED_BED).defaultBlockState();
        BlockState bedHeadState = ((Block) Blocks.RED_BED).defaultBlockState().setValue(BedBlock.PART, BedPart.HEAD);
        this.blockModelResolver.update(state.bedFoot, bedFootState, DISPLAY_CONTEXT);
        this.blockModelResolver.update(state.bedHead, bedHeadState, DISPLAY_CONTEXT);
    }

    @Override
    public void submit(BreederRenderState state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState cameraState) {
        if (!state.renderContents) return;
        VillagerRenderer vr = getVillagerRenderer();
        if (vr == null) return;

        poseStack.pushPose();

        // Villager 1 (left side)
        if (state.renderVillager1) {
            poseStack.pushPose();
            poseStack.translate(0.5d, 0.0625d, 0.5d);
            poseStack.mulPose(new Quaternionf().rotateY((float) Math.toRadians(-state.facing.toYRot())));
            poseStack.translate(-0.3125d, 0.0d, 0.0d);
            poseStack.mulPose(new Quaternionf().rotateY((float) Math.toRadians(90.0f)));
            poseStack.scale(0.45f, 0.45f, 0.45f);
            vr.submit(state.villagerRenderState1, poseStack, collector, cameraState);
            poseStack.popPose();
        }

        // Villager 2 (right side)
        if (state.renderVillager2) {
            poseStack.pushPose();
            poseStack.translate(0.5d, 0.0625d, 0.5d);
            poseStack.mulPose(new Quaternionf().rotateY((float) Math.toRadians(-state.facing.toYRot())));
            poseStack.translate(0.3125d, 0.0d, 0.0d);
            poseStack.mulPose(new Quaternionf().rotateY((float) Math.toRadians(-90.0f)));
            poseStack.scale(0.45f, 0.45f, 0.45f);
            vr.submit(state.villagerRenderState2, poseStack, collector, cameraState);
            poseStack.popPose();
        }

        // Bed (center)
        poseStack.pushPose();
        poseStack.translate(0.5d, 0.0625d, 0.5d);
        poseStack.mulPose(new Quaternionf().rotateY((float) Math.toRadians(-state.facing.toYRot())));
        poseStack.translate(0.0d, 0.0d, 0.1875d);
        poseStack.translate(-0.5d, 0.0d, -0.5d);
        poseStack.scale(0.4f, 0.4f, 0.4f);
        poseStack.translate(0.75d, 0.0d, 0.75d);
        state.bedFoot.submit(poseStack, collector, state.lightCoords, OverlayTexture.NO_OVERLAY, 0);
        poseStack.translate(0.0d, 0.0d, -1.0d);
        state.bedHead.submit(poseStack, collector, state.lightCoords, OverlayTexture.NO_OVERLAY, 0);
        poseStack.popPose();

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
