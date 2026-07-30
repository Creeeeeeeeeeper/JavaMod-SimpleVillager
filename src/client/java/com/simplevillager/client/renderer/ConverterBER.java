package com.simplevillager.client.renderer;

import com.simplevillager.blockentity.ConverterBlockEntity;
import com.simplevillager.entity.SimpleVillagerEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.zombie.ZombieVillager;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionf;
import java.util.concurrent.atomic.AtomicInteger;

public class ConverterBER implements BlockEntityRenderer<ConverterBlockEntity, VillagerBERState> {

    private static final AtomicInteger TEMP_ENTITY_ID = new AtomicInteger(-2000);
    private static java.lang.reflect.Field entity_idField;

    // Phase constants must match ConverterBlockEntity
    private static final int ZOMBIFY_TIME = 100;
    private static final int CURE_TIME = 1900;

    private ZombieVillager cachedZombieVillager;
    private ZombieVillager cachedZombie;

    private static int assignTempId(Entity entity) {
        try {
            if (entity_idField == null) {
                entity_idField = Entity.class.getDeclaredField("id");
                entity_idField.setAccessible(true);
            }
            int id = TEMP_ENTITY_ID.getAndDecrement();
            entity_idField.setInt(entity, id);
            return id;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public VillagerBERState createRenderState() {
        return new VillagerBERState();
    }

    @Override
    public void extractRenderState(ConverterBlockEntity blockEntity, VillagerBERState state, float partialTick, Vec3 cameraPos, net.minecraft.client.renderer.feature.ModelFeatureRenderer.CrumblingOverlay overlay) {
        net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState.extractBase(blockEntity, state, overlay);
        state.clear();

        int lightCoords = 15728880;
        if (blockEntity.getLevel() != null) {
            BlockPos pos = blockEntity.getBlockPos();
            int blockLight = blockEntity.getLevel().getBrightness(LightLayer.BLOCK, pos);
            int skyLight = blockEntity.getLevel().getBrightness(LightLayer.SKY, pos);
            lightCoords = skyLight << 20 | blockLight << 4;
        }

        Direction facing = Direction.NORTH;
        if (blockEntity.getBlockState().hasProperty(net.minecraft.world.level.block.state.properties.BlockStateProperties.HORIZONTAL_FACING)) {
            facing = blockEntity.getBlockState().getValue(net.minecraft.world.level.block.state.properties.BlockStateProperties.HORIZONTAL_FACING);
        }
        state.facing = facing;
        state.timer = blockEntity.getTimer();

        SimpleVillagerEntity villager = blockEntity.getVillagerEntity();
        EntityRenderDispatcher dispatcher = Minecraft.getInstance().getEntityRenderDispatcher();
        net.minecraft.world.level.Level level = blockEntity.getLevel();
        if (level == null) return;

        // Always show zombie on right (cached to prevent flickering)
        if (cachedZombie == null) {
            cachedZombie = EntityTypes.ZOMBIE_VILLAGER.create(level, EntitySpawnReason.NATURAL);
            if (cachedZombie != null) {
                cachedZombie.setNoAi(true);
                assignTempId(cachedZombie);
            }
        }
        if (cachedZombie != null) {
            if (villager != null) {
                cachedZombie.setBaby(villager.isBaby());
            }
            syncOldValues(cachedZombie);
            EntityRenderState zombieState = dispatcher.extractEntity(cachedZombie, partialTick);
            zombieState.lightCoords = lightCoords;
            state.addEntity(zombieState, 0.3125f, 0, 0, 0.4f, -90);
        }

        if (villager != null) {
            long timer = blockEntity.getTimer();

            if (timer >= ZOMBIFY_TIME && timer < CURE_TIME) {
                // Phase 2: zombie villager on left
                if (cachedZombieVillager == null) {
                    cachedZombieVillager = EntityTypes.ZOMBIE_VILLAGER.create(level, EntitySpawnReason.NATURAL);
                    if (cachedZombieVillager != null) {
                        cachedZombieVillager.setNoAi(true);
                        assignTempId(cachedZombieVillager);
                    }
                }
                if (cachedZombieVillager != null) {
                    cachedZombieVillager.setBaby(villager.isBaby());
                    syncOldValues(cachedZombieVillager);
                    EntityRenderState zombieVillagerState = dispatcher.extractEntity(cachedZombieVillager, partialTick);
                    zombieVillagerState.lightCoords = lightCoords;
                    state.addEntity(zombieVillagerState, -0.3125f, 0, 0, 0.4f, 90);
                }
            } else {
                // Phase 1 & 3: villager on left
                syncOldValues(villager);
                EntityRenderState villagerState = dispatcher.extractEntity(villager, partialTick);
                villagerState.lightCoords = lightCoords;
                state.addEntity(villagerState, -0.3125f, 0, 0, 0.4f, 90);
            }
        }
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

    private static void syncOldValues(LivingEntity entity) {
        entity.xo = entity.getX();
        entity.yo = entity.getY();
        entity.zo = entity.getZ();
        entity.yBodyRotO = entity.yBodyRot;
        entity.yHeadRotO = entity.yHeadRot;
    }
}
