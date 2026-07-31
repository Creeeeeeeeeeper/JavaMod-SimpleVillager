package com.simplevillager.client.renderer;

import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.entity.state.VillagerRenderState;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;

public class FarmerRenderState extends BlockEntityRenderState {
    public Direction facing = Direction.NORTH;
    public boolean renderVillager;
    public VillagerRenderState villagerRenderState = new VillagerRenderState();
    public int lightCoords = 15728880;
    public BlockState crop;
}
