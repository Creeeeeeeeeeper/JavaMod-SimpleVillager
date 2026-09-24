package com.simplevillager.client.renderer;

import net.minecraft.client.renderer.block.BlockModelRenderState;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.entity.state.VillagerRenderState;
import net.minecraft.core.Direction;

public class TraderRenderState extends BlockEntityRenderState {
    public Direction facing = Direction.NORTH;
    public VillagerRenderState villagerRenderState;
    public final BlockModelRenderState workstation = new BlockModelRenderState();
    public boolean renderVillager = false;
    public int worldLight = 15728880;
    public boolean renderContents = true;
}
