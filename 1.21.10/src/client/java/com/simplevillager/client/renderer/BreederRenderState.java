package com.simplevillager.client.renderer;

import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.entity.state.VillagerRenderState;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;

public class BreederRenderState extends BlockEntityRenderState {
    public Direction facing = Direction.NORTH;
    public boolean renderVillager1;
    public boolean renderVillager2;
    public VillagerRenderState villagerRenderState1 = new VillagerRenderState();
    public VillagerRenderState villagerRenderState2 = new VillagerRenderState();
    public int lightCoords = 15728880;
    public BlockState bedFoot;
    public BlockState bedHead;
}
