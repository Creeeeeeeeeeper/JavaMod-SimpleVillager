package com.simplevillager.client.renderer;

import com.simplevillager.blockentity.ConverterBlockEntity;
import com.simplevillager.entity.SimpleVillagerEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.entity.monster.ZombieVillager;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionf;

import java.util.concurrent.atomic.AtomicInteger;

public class ConverterBER implements BlockEntityRenderer<ConverterBlockEntity> {

    private static final AtomicInteger TEMP_ENTITY_ID = new AtomicInteger(-2000);

    // Phase constants must match ConverterBlockEntity
    private static final int ZOMBIFY_TIME = 100;
    private static final int CURE_TIME = 1900;

    private ZombieVillager cachedZombieVillager;
    private Zombie cachedZombie;

    private static int assignTempId(Entity entity) {
        int id = TEMP_ENTITY_ID.getAndDecrement();
        entity.setId(id);
        return id;
    }

    @Override
    public void render(ConverterBlockEntity blockEntity, float partialTick, PoseStack poseStack, MultiBufferSource buffer, int packedLight, int packedOverlay) {
        if (!RenderConfig.shouldRender(blockEntity, cameraPos())) return;
        Level level = blockEntity.getLevel();
        if (level == null) return;

        Direction facing = Direction.NORTH;
        if (blockEntity.getBlockState().hasProperty(BlockStateProperties.HORIZONTAL_FACING)) {
            facing = blockEntity.getBlockState().getValue(BlockStateProperties.HORIZONTAL_FACING);
        }

        SimpleVillagerEntity villager = blockEntity.getVillagerEntity();
        long timer = blockEntity.getTimer();
        EntityRenderDispatcher dispatcher = Minecraft.getInstance().getEntityRenderDispatcher();

        poseStack.pushPose();

        // Always show zombie on right (cached to prevent flickering)
        if (cachedZombie == null) {
            cachedZombie = EntityType.ZOMBIE.create(level);
            if (cachedZombie != null) {
                cachedZombie.setNoAi(true);
                assignTempId(cachedZombie);
            }
        }
        if (cachedZombie != null) {
            if (villager != null) {
                cachedZombie.setBaby(villager.isBaby());
            }
            poseStack.pushPose();
            poseStack.translate(0.5f, 0.0625f, 0.5f);
            poseStack.mulPose(new Quaternionf().rotateY((float) Math.toRadians(-facing.toYRot())));
            poseStack.translate(0.3125f, 0, 0);
            poseStack.scale(0.4f, 0.4f, 0.4f);
            poseStack.mulPose(new Quaternionf().rotateY((float) Math.toRadians(-90.0f)));
            syncOldValues(cachedZombie);
            var renderer = dispatcher.getRenderer(cachedZombie);
            if (renderer != null) {
                renderer.render(cachedZombie, 0f, partialTick, poseStack, buffer, packedLight);
            }
            poseStack.popPose();
        }

        if (villager != null) {
            if (timer >= ZOMBIFY_TIME && timer < CURE_TIME) {
                // Phase 2: zombie villager on left
                if (cachedZombieVillager == null) {
                    cachedZombieVillager = EntityType.ZOMBIE_VILLAGER.create(level);
                    if (cachedZombieVillager != null) {
                        cachedZombieVillager.setNoAi(true);
                        assignTempId(cachedZombieVillager);
                    }
                }
                if (cachedZombieVillager != null) {
                    cachedZombieVillager.setBaby(villager.isBaby());
                    poseStack.pushPose();
                    poseStack.translate(0.5f, 0.0625f, 0.5f);
                    poseStack.mulPose(new Quaternionf().rotateY((float) Math.toRadians(-facing.toYRot())));
                    poseStack.translate(-0.3125f, 0, 0);
                    poseStack.scale(0.4f, 0.4f, 0.4f);
                    poseStack.mulPose(new Quaternionf().rotateY((float) Math.toRadians(90.0f)));
                    syncOldValues(cachedZombieVillager);
                    var renderer = dispatcher.getRenderer(cachedZombieVillager);
                    if (renderer != null) {
                        renderer.render(cachedZombieVillager, 0f, partialTick, poseStack, buffer, packedLight);
                    }
                    poseStack.popPose();
                }
            } else {
                // Phase 1 & 3: villager on left
                poseStack.pushPose();
                poseStack.translate(0.5f, 0.0625f, 0.5f);
                poseStack.mulPose(new Quaternionf().rotateY((float) Math.toRadians(-facing.toYRot())));
                poseStack.translate(-0.3125f, 0, 0);
                poseStack.scale(0.4f, 0.4f, 0.4f);
                poseStack.mulPose(new Quaternionf().rotateY((float) Math.toRadians(90.0f)));
                syncOldValues(villager);
                var renderer = dispatcher.getRenderer(villager);
                if (renderer != null) {
                    renderer.render(villager, 0f, partialTick, poseStack, buffer, packedLight);
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
