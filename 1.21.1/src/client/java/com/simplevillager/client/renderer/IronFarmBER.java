package com.simplevillager.client.renderer;

import com.simplevillager.blockentity.IronFarmBlockEntity;
import com.simplevillager.entity.SimpleVillagerEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.IronGolem;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionf;

import java.util.concurrent.atomic.AtomicInteger;

public class IronFarmBER implements BlockEntityRenderer<IronFarmBlockEntity> {

    private static final AtomicInteger TEMP_ENTITY_ID = new AtomicInteger(-1000);

    private Zombie cachedZombie;
    private IronGolem cachedGolem;

    private static int assignTempId(Entity entity) {
        int id = TEMP_ENTITY_ID.getAndDecrement();
        entity.setId(id);
        return id;
    }

    @Override
    public void render(IronFarmBlockEntity blockEntity, float partialTick, PoseStack poseStack, MultiBufferSource buffer, int packedLight, int packedOverlay) {
        if (!RenderConfig.shouldRender(blockEntity, cameraPos())) return;
        Level level = blockEntity.getLevel();
        if (level == null) return;

        Direction facing = Direction.NORTH;
        if (blockEntity.getBlockState().hasProperty(BlockStateProperties.HORIZONTAL_FACING)) {
            facing = blockEntity.getBlockState().getValue(BlockStateProperties.HORIZONTAL_FACING);
        }

        long timer = blockEntity.getTimer();
        EntityRenderDispatcher dispatcher = Minecraft.getInstance().getEntityRenderDispatcher();

        poseStack.pushPose();

        // Villager (back-left)
        SimpleVillagerEntity villager = blockEntity.getVillagerEntity();
        if (villager != null) {
            poseStack.pushPose();
            poseStack.translate(0.5f, 0.0625f, 0.5f);
            poseStack.mulPose(new Quaternionf().rotateY((float) Math.toRadians(-facing.toYRot())));
            poseStack.translate(-0.3125f, 0, -0.3125f);
            poseStack.scale(0.3f, 0.3f, 0.3f);
            poseStack.mulPose(new Quaternionf().rotateY((float) Math.toRadians(90.0f)));
            syncOldValues(villager);
            var renderer = dispatcher.getRenderer(villager);
            if (renderer != null) {
                renderer.render(villager, 0f, partialTick, poseStack, buffer, packedLight);
            }
            poseStack.popPose();
        }

        // Zombie (back-right, cached)
        if (cachedZombie == null) {
            cachedZombie = EntityType.ZOMBIE.create(level);
            if (cachedZombie != null) {
                cachedZombie.setNoAi(true);
                assignTempId(cachedZombie);
            }
        }
        if (cachedZombie != null) {
            poseStack.pushPose();
            poseStack.translate(0.5f, 0.0625f, 0.5f);
            poseStack.mulPose(new Quaternionf().rotateY((float) Math.toRadians(-facing.toYRot())));
            poseStack.translate(0.3125f, 0, -0.3125f);
            poseStack.scale(0.3f, 0.3f, 0.3f);
            poseStack.mulPose(new Quaternionf().rotateY((float) Math.toRadians(-90.0f)));
            syncOldValues(cachedZombie);
            var renderer = dispatcher.getRenderer(cachedZombie);
            if (renderer != null) {
                renderer.render(cachedZombie, 0f, partialTick, poseStack, buffer, packedLight);
            }
            poseStack.popPose();
        }

        // Iron Golem (cached, front lava area, only during hurt phase)
        long golemSpawn = 1100;
        long golemKill = 1200;
        if (timer >= golemSpawn && timer < golemKill) {
            if (cachedGolem == null) {
                cachedGolem = EntityType.IRON_GOLEM.create(level);
                if (cachedGolem != null) {
                    cachedGolem.setNoAi(true);
                    assignTempId(cachedGolem);
                }
            }
            if (cachedGolem != null) {
                poseStack.pushPose();
                poseStack.translate(0.5f, 0.0625f, 0.5f);
                poseStack.mulPose(new Quaternionf().rotateY((float) Math.toRadians(-facing.toYRot())));
                poseStack.translate(0, -0.0625f, 0.22f);
                poseStack.scale(0.28f, 0.28f, 0.28f);
                syncOldValues(cachedGolem);
                var renderer = dispatcher.getRenderer(cachedGolem);
                if (renderer != null) {
                    renderer.render(cachedGolem, 0f, partialTick, poseStack, buffer, packedLight);
                }
                poseStack.popPose();
            }
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
