package com.simplevillager.blocks;

import com.simplevillager.blockentity.ConverterBlockEntity;
import com.simplevillager.blockentity.ModBlockEntities;
import com.simplevillager.items.VillagerItem;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.Nullable;

public class ConverterBlock extends VillagerBlockBase {

    public ConverterBlock(BlockBehaviour.Properties properties) {
        super(properties);
    }

    @Override
    protected MapCodec<? extends VillagerBlockBase> codec() {
        return simpleCodec(ConverterBlock::new);
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack stack) {
        if (placer != null) {
            BlockEntity tile = level.getBlockEntity(pos);
            if (tile instanceof ConverterBlockEntity converter) {
                converter.setOwner(placer.getUUID());
            }
        }
        super.setPlacedBy(level, pos, state, placer, stack);
    }

    @Override
    protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if (level.isClientSide()) return InteractionResult.TRY_WITH_EMPTY_HAND;
        BlockEntity tileEntity = level.getBlockEntity(pos);
        if (!(tileEntity instanceof ConverterBlockEntity converter)) {
            return InteractionResult.TRY_WITH_EMPTY_HAND;
        }

        // Right click: always open GUI
        return InteractionResult.TRY_WITH_EMPTY_HAND;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (level.isClientSide()) return InteractionResult.SUCCESS;
        BlockEntity tileEntity = level.getBlockEntity(pos);
        if (!(tileEntity instanceof ConverterBlockEntity converter)) {
            return super.useWithoutItem(state, level, pos, player, hit);
        }

        player.openMenu(new SimpleMenuProvider(
                (containerId, playerInventory, p) -> new com.simplevillager.gui.ConverterContainer(
                        containerId, playerInventory, converter.getInputInventory(), converter.getOutputInventory()
                ),
                state.getBlock().getName()
        ));
        return InteractionResult.SUCCESS;
    }

    @Override
    public net.minecraft.world.level.block.state.BlockState playerWillDestroy(Level level, BlockPos pos, BlockState state, Player player) {
        BlockEntity tileEntity = level.getBlockEntity(pos);
        if (tileEntity instanceof ConverterBlockEntity converter) {
            Containers.dropContents(level, pos, converter.getInputInventory());
            Containers.dropContents(level, pos, converter.getOutputInventory());
        }
        return super.playerWillDestroy(level, pos, state, player);
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new ConverterBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> blockEntityType) {
        return level.isClientSide() ? null : createTickerHelper(blockEntityType, ModBlockEntities.CONVERTER, ConverterBlockEntity::serverTick);
    }
}
