package com.simplevillager.client.renderer;

import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.entity.state.VillagerRenderState;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;

public class TraderRenderState extends BlockEntityRenderState {
    public Direction facing = Direction.NORTH;
    public VillagerRenderState villagerRenderState;
    public BlockState workstation;
    public boolean renderVillager = false;
    public int worldLight = 15728880;
    public boolean renderContents = true;
}
