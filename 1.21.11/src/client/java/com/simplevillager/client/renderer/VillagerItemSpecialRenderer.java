package com.simplevillager.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.serialization.MapCodec;
import com.simplevillager.client.BedConfig;
import com.simplevillager.datacomponent.VillagerData;
import com.simplevillager.entity.SimpleVillagerEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.VillagerRenderer;
import net.minecraft.client.renderer.entity.state.VillagerRenderState;
import net.minecraft.client.renderer.special.SpecialModelRenderer;
import net.minecraft.client.renderer.state.CameraRenderState;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import org.joml.Vector3f;
import org.joml.Vector3fc;

import java.util.function.Consumer;

public class VillagerItemSpecialRenderer implements SpecialModelRenderer<VillagerRenderState> {

    public static final Identifier ID = Identifier.fromNamespaceAndPath("simplevillager", "villager");
    private static final int DEFAULT_LIGHT = 15728880;

    private VillagerRenderer cachedRenderer;

    private VillagerRenderer getRenderer() {
        if (cachedRenderer == null) {
            try {
                EntityRenderDispatcher dispatcher = Minecraft.getInstance().getEntityRenderDispatcher();
                SimpleVillagerEntity temp = new SimpleVillagerEntity(EntityType.VILLAGER, Minecraft.getInstance().level);
                EntityRenderer<? super SimpleVillagerEntity, ?> renderer = dispatcher.getRenderer(temp);
                if (renderer instanceof VillagerRenderer vr) {
                    cachedRenderer = vr;
                }
            } catch (Exception ignored) {
            }
        }
        return cachedRenderer;
    }

    @Override
    public VillagerRenderState extractArgument(ItemStack stack) {
        ClientLevel level = Minecraft.getInstance().level;
        if (level == null) return null;

        SimpleVillagerEntity villager = VillagerData.createSimpleVillager(stack, level);
        if (villager == null) {
            villager = new SimpleVillagerEntity(EntityType.VILLAGER, level);
        }
        villager.setNoAi(true);

        VillagerRenderer renderer = getRenderer();
        if (renderer == null) return null;

        try {
            VillagerRenderState renderState = renderer.createRenderState();
            renderer.extractRenderState(villager, renderState, 0.0f);
            renderState.lightCoords = DEFAULT_LIGHT;
            return renderState;
        } catch (Exception e) {
            return null;
        }
    }

    @Override
    public void submit(VillagerRenderState state, ItemDisplayContext context, PoseStack poseStack, SubmitNodeCollector collector, int light, int overlay, boolean hasFoil, int outlineColor) {
        if (state == null) return;

        VillagerRenderer renderer = getRenderer();
        if (renderer == null) return;

        state.lightCoords = light > 0 ? light : DEFAULT_LIGHT;

        CameraRenderState cameraState = new CameraRenderState();
        if (context == ItemDisplayContext.GROUND) {
            float s = (float) BedConfig.villagerGroundScale;
            float y = (float) BedConfig.villagerGroundY;
            poseStack.pushPose();
            poseStack.translate(0.0f, y, 0.0f);
            poseStack.scale(s, s, s);
            renderer.submit(state, poseStack, collector, cameraState);
            poseStack.popPose();
            return;
        }
        renderer.submit(state, poseStack, collector, cameraState);
    }

    @Override
    public void getExtents(Consumer<Vector3fc> extents) {
        float h = 2.0f;
        float w = 0.5f;
        extents.accept(new Vector3f(-w, 0.0f, -w));
        extents.accept(new Vector3f(w, 0.0f, -w));
        extents.accept(new Vector3f(-w, -h, -w));
        extents.accept(new Vector3f(w, -h, -w));
        extents.accept(new Vector3f(-w, 0.0f, w));
        extents.accept(new Vector3f(w, 0.0f, w));
        extents.accept(new Vector3f(-w, -h, w));
        extents.accept(new Vector3f(w, -h, w));
    }

    public static class Unbaked implements SpecialModelRenderer.Unbaked {
        public static final MapCodec<Unbaked> MAP_CODEC = MapCodec.unit(new Unbaked());

        @Override
        public SpecialModelRenderer<?> bake(SpecialModelRenderer.BakingContext context) {
            return new VillagerItemSpecialRenderer();
        }

        @Override
        public MapCodec<? extends SpecialModelRenderer.Unbaked> type() {
            return MAP_CODEC;
        }
    }
}
