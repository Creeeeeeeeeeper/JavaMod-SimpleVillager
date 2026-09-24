package com.simplevillager.client.renderer;

import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.core.Direction;

import java.util.ArrayList;
import java.util.List;

public class VillagerBERState extends BlockEntityRenderState {

    public record EntityData(EntityRenderState renderState, float offsetX, float offsetY, float offsetZ, float scale, float yRotOffset) {}

    public Direction facing = Direction.NORTH;
    public final List<EntityData> entities = new ArrayList<>();
    public long timer;
    public boolean renderContents = true;

    public void addEntity(EntityRenderState renderState, float x, float y, float z, float scale, float yRot) {
        entities.add(new EntityData(renderState, x, y, z, scale, yRot));
    }

    public void clear() {
        entities.clear();
    }
}
