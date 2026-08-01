package com.simplevillager.blocks;

import com.simplevillager.blockentity.AutoTraderBlockEntity;
import com.simplevillager.blockentity.ModBlockEntities;
import com.simplevillager.gui.AutoTraderContainer;
import com.simplevillager.items.VillagerItem;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.Nullable;

public class AutoTraderBlock extends VillagerBlockBase {

    public AutoTraderBlock(BlockBehaviour.Properties properties) {
        super(properties);
    }

    @Override
    protected MapCodec<? extends VillagerBlockBase> codec() {
        return simpleCodec(AutoTraderBlock::new);
    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack heldItem, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if (level.isClientSide()) return ItemInteractionResult.SUCCESS;
        BlockEntity tileEntity = level.getBlockEntity(pos);
        if (!(tileEntity instanceof AutoTraderBlockEntity trader)) {
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }

        if (!trader.hasVillager() && heldItem.getItem() instanceof VillagerItem) {
            trader.setVillager(heldItem.copy());
            heldItem.shrink(1);
            if (heldItem.isEmpty()) player.setItemInHand(hand, ItemStack.EMPTY);
            playVillagerSound(level, pos, SoundEvents.VILLAGER_CELEBRATE);
            return ItemInteractionResult.SUCCESS;
        } else if (!trader.hasWorkstation() && heldItem.getItem() instanceof BlockItem blockItem && trader.isValidBlock(blockItem.getBlock())) {
            Block block = blockItem.getBlock();
            trader.setWorkstation(block);
            heldItem.shrink(1);
            if (heldItem.isEmpty()) player.setItemInHand(hand, ItemStack.EMPTY);
            SoundType type = block.defaultBlockState().getSoundType();
            level.playSound(null, pos, type.getPlaceSound(), SoundSource.BLOCKS, type.getVolume(), type.getPitch());
            return ItemInteractionResult.SUCCESS;
        } else if (player.isShiftKeyDown() && trader.hasVillager()) {
            ItemStack stack = trader.removeVillager();
            if (heldItem.isEmpty()) {
                player.setItemInHand(hand, stack);
            } else if (!player.getInventory().add(stack)) {
                Direction direction = state.getValue(FACING);
                Containers.dropItemStack(level, direction.getStepX() + pos.getX() + 0.5, pos.getY() + 0.5, direction.getStepZ() + pos.getZ() + 0.5, stack);
            }
            playVillagerSound(level, pos, SoundEvents.VILLAGER_CELEBRATE);
            return ItemInteractionResult.SUCCESS;
        } else if (player.isShiftKeyDown() && trader.hasWorkstation()) {
            ItemStack blockStack = new ItemStack(trader.removeWorkstation());
            if (heldItem.isEmpty()) {
                player.setItemInHand(hand, blockStack);
            } else if (!player.getInventory().add(blockStack)) {
                Direction direction = state.getValue(FACING);
                Containers.dropItemStack(level, direction.getStepX() + pos.getX() + 0.5, pos.getY() + 0.5, direction.getStepZ() + pos.getZ() + 0.5, blockStack);
            }
            if (trader.hasVillager()) playVillagerSound(level, pos, SoundEvents.VILLAGER_NO);
            return ItemInteractionResult.SUCCESS;
        }

        return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (level.isClientSide()) return InteractionResult.SUCCESS;
        BlockEntity tileEntity = level.getBlockEntity(pos);
        if (!(tileEntity instanceof AutoTraderBlockEntity trader)) {
            return InteractionResult.PASS;
        }
        if (trader.hasVillager() && !player.isShiftKeyDown()) {
            player.openMenu(new SimpleMenuProvider(
                    (containerId, playerInventory, p) -> new AutoTraderContainer(containerId, playerInventory, trader),
                    state.getBlock().getName()
            ));
            return InteractionResult.SUCCESS;
        }
        return InteractionResult.PASS;
    }

    @Override
    public net.minecraft.world.level.block.state.BlockState playerWillDestroy(Level level, BlockPos pos, net.minecraft.world.level.block.state.BlockState state, Player player) {
        BlockEntity tileEntity = level.getBlockEntity(pos);
        if (tileEntity instanceof AutoTraderBlockEntity trader) {
            Containers.dropContents(level, pos, trader.getInputInventory());
            Containers.dropContents(level, pos, trader.getOutputInventory());
            ItemStack v = trader.getVillager();
            if (!v.isEmpty()) Containers.dropItemStack(level, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, v);
            if (trader.hasWorkstation()) {
                ItemStack ws = new ItemStack(trader.removeWorkstation());
                if (!ws.isEmpty()) Containers.dropItemStack(level, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, ws);
            }
        }
        return super.playerWillDestroy(level, pos, state, player);
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new AutoTraderBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> blockEntityType) {
        return level.isClientSide() ? null : createTickerHelper(blockEntityType, ModBlockEntities.AUTO_TRADER, AutoTraderBlockEntity::tick);
    }
}
