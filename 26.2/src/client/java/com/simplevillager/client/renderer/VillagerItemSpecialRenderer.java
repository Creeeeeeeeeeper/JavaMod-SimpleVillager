package com.simplevillager.client.renderer;

import com.simplevillager.entity.SimpleVillagerEntity;
import com.simplevillager.items.ModItems;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.serialization.MapCodec;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.VillagerRenderer;
import net.minecraft.client.renderer.entity.state.VillagerRenderState;
import net.minecraft.client.renderer.special.SpecialModelRenderer;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.item.ItemStack;
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
                SimpleVillagerEntity temp = new SimpleVillagerEntity(EntityTypes.VILLAGER, Minecraft.getInstance().level);
                EntityRenderer<?, ?> renderer = dispatcher.getRenderer(temp);
                if (renderer instanceof VillagerRenderer vr) {
                    cachedRenderer = vr;
                }
            } catch (Exception e) {
                // fallback
            }
        }
        return cachedRenderer;
    }

    @Override
    public VillagerRenderState extractArgument(ItemStack stack) {
        ClientLevel level = Minecraft.getInstance().level;
        if (level == null) return null;

        SimpleVillagerEntity villager = com.simplevillager.datacomponent.VillagerData.createSimpleVillager(stack, level);
        if (villager == null) {
            villager = new SimpleVillagerEntity(EntityTypes.VILLAGER, level);
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
    public void submit(VillagerRenderState state, PoseStack poseStack, SubmitNodeCollector collector, int light, int overlay, boolean hasFoil, int outlineColor) {
        if (state == null) return;

        VillagerRenderer renderer = getRenderer();
        if (renderer == null) return;

        state.lightCoords = light > 0 ? light : DEFAULT_LIGHT;

        CameraRenderState cameraState = new CameraRenderState();
        renderer.submit(state, poseStack, collector, cameraState);
    }

    @Override
    public void getExtents(Consumer<Vector3fc> extents) {
    }

    public record Unbaked() implements SpecialModelRenderer.Unbaked<VillagerRenderState> {
        public static final MapCodec<Unbaked> MAP_CODEC = MapCodec.unit(new Unbaked());

        @Override
        public SpecialModelRenderer<VillagerRenderState> bake(BakingContext context) {
            return new VillagerItemSpecialRenderer();
        }

        @Override
        public MapCodec<? extends SpecialModelRenderer.Unbaked<VillagerRenderState>> type() {
            return MAP_CODEC;
        }
    }
}
