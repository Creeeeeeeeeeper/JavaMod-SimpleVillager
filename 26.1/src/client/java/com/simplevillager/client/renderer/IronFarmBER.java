package com.simplevillager.client.renderer;

import com.simplevillager.blockentity.IronFarmBlockEntity;
import com.simplevillager.entity.SimpleVillagerEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.entity.state.IronGolemRenderState;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.golem.IronGolem;
import net.minecraft.world.entity.monster.zombie.Zombie;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionf;
import java.util.concurrent.atomic.AtomicInteger;

public class IronFarmBER implements BlockEntityRenderer<IronFarmBlockEntity, VillagerBERState> {

    private static final AtomicInteger TEMP_ENTITY_ID = new AtomicInteger(-1000);
    private static java.lang.reflect.Field entity_idField;

    private Zombie cachedZombie;
    private IronGolem cachedGolem;

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
    public void extractRenderState(IronFarmBlockEntity blockEntity, VillagerBERState state, float partialTick, Vec3 cameraPos, net.minecraft.client.renderer.feature.ModelFeatureRenderer.CrumblingOverlay overlay) {
        net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState.extractBase(blockEntity, state, overlay);
        state.clear();

        state.renderContents = RenderConfig.shouldRender(blockEntity, cameraPos);
        if (!state.renderContents) return;

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

        Level level = blockEntity.getLevel();
        if (level == null) return;

        EntityRenderDispatcher dispatcher = Minecraft.getInstance().getEntityRenderDispatcher();
        long timer = blockEntity.getTimer();

        // Villager (back-left)
        SimpleVillagerEntity villager = blockEntity.getVillagerEntity();
        if (villager != null) {
            syncOldValues(villager);
            EntityRenderState villagerState = dispatcher.extractEntity(villager, partialTick);
            villagerState.lightCoords = lightCoords;
            state.addEntity(villagerState, -0.3125f, 0, -0.3125f, 0.3f, 90);
        }

        // Zombie (back-right, cached)
        if (cachedZombie == null) {
            cachedZombie = EntityType.ZOMBIE.create(level, EntitySpawnReason.NATURAL);
            if (cachedZombie != null) {
                cachedZombie.setNoAi(true);
                assignTempId(cachedZombie);
            }
        }
        if (cachedZombie != null) {
            syncOldValues(cachedZombie);
            EntityRenderState zombieState = dispatcher.extractEntity(cachedZombie, partialTick);
            zombieState.lightCoords = lightCoords;
            state.addEntity(zombieState, 0.3125f, 0, -0.3125f, 0.3f, -90);
        }

        // Iron Golem (cached, front lava area, only during hurt phase)
        // Timer is synced from server via sendBlockUpdated at golem spawn/kill times
        try {
            if (cachedGolem == null) {
                cachedGolem = EntityType.IRON_GOLEM.create(level, EntitySpawnReason.NATURAL);
                if (cachedGolem != null) {
                    cachedGolem.setNoAi(true);
                    assignTempId(cachedGolem);
                }
            }
            long golemSpawn = 1100;
            long golemKill = 1200;
            if (timer >= golemSpawn && timer < golemKill && cachedGolem != null) {
                syncOldValues(cachedGolem);
                EntityRenderState golemState = dispatcher.extractEntity(cachedGolem, partialTick);
                golemState.lightCoords = lightCoords;
                if (golemState instanceof IronGolemRenderState ironGolemState) {
                    ironGolemState.hasRedOverlay = (level.getGameTime() / 10) % 2 == 0;
                }
                state.addEntity(golemState, 0, -0.0625f, 0.22f, 0.28f, 0);
            }
        } catch (Exception e) {
            org.slf4j.LoggerFactory.getLogger("simplevillager").error("IronFarm golem render error", e);
        }
    }

    @Override
    public void submit(VillagerBERState state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState cameraState) {
        if (!state.renderContents) return;
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
