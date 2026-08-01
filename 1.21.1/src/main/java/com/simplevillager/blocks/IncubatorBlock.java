package com.simplevillager.blocks;

import com.simplevillager.blockentity.IncubatorBlockEntity;
import com.simplevillager.blockentity.ModBlockEntities;
import com.simplevillager.items.VillagerItem;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.SimpleMenuProvider;
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

public class IncubatorBlock extends VillagerBlockBase {

    public IncubatorBlock(BlockBehaviour.Properties properties) {
        super(properties);
    }

    @Override
    protected MapCodec<? extends VillagerBlockBase> codec() {
        return simpleCodec(IncubatorBlock::new);
    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if (level.isClientSide()) return ItemInteractionResult.SUCCESS;
        BlockEntity tileEntity = level.getBlockEntity(pos);
        if (!(tileEntity instanceof IncubatorBlockEntity incubator)) {
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }

        if (!incubator.hasVillager() && stack.getItem() instanceof VillagerItem && VillagerItem.isBaby(stack)) {
            // Insert villager into input inventory
            ItemStack toInsert = stack.copy();
            toInsert.setCount(1);
            ItemStack remaining = incubator.getInputInventory().addItem(toInsert);
            if (remaining.isEmpty()) {
                stack.shrink(1);
                VillagerBlockBase.playVillagerSound(level, pos, SoundEvents.VILLAGER_NO);
                return ItemInteractionResult.SUCCESS;
            }
        }
        return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        BlockEntity tileEntity = level.getBlockEntity(pos);
        if (!(tileEntity instanceof IncubatorBlockEntity incubator)) {
            return InteractionResult.PASS;
        }

        if (player.isShiftKeyDown()) {
            if (level.isClientSide()) {
                // Optimistic local update so the rendered villager disappears immediately
                incubator.removeVillager();
                return InteractionResult.SUCCESS;
            }
            // Shift + right click: extract villager from output
            for (int i = 0; i < incubator.getOutputInventory().getContainerSize(); i++) {
                ItemStack stack = incubator.getOutputInventory().getItem(i);
                if (!stack.isEmpty()) {
                    ItemStack extracted = stack.copy();
                    stack.shrink(1);
                    if (!player.getInventory().add(extracted)) {
                        Containers.dropItemStack(level, pos.getX() + 0.5, pos.getY() + 1.5, pos.getZ() + 0.5, extracted);
                    }
                    incubator.syncData();
                    incubator.setChanged();
                    VillagerBlockBase.playVillagerSound(level, pos, SoundEvents.VILLAGER_CELEBRATE);
                    return InteractionResult.SUCCESS;
                }
            }
            // Also extract the internal villager if present
            ItemStack internalVillager = incubator.removeVillager();
            if (!internalVillager.isEmpty()) {
                if (!player.getInventory().add(internalVillager)) {
                    Containers.dropItemStack(level, pos.getX() + 0.5, pos.getY() + 1.5, pos.getZ() + 0.5, internalVillager);
                }
                incubator.syncData();
                VillagerBlockBase.playVillagerSound(level, pos, SoundEvents.VILLAGER_CELEBRATE);
                return InteractionResult.SUCCESS;
            }
        } else {
            if (level.isClientSide()) return InteractionResult.SUCCESS;
            player.openMenu(new SimpleMenuProvider(
                    (containerId, playerInventory, p) -> new com.simplevillager.gui.IncubatorContainer(
                            containerId, playerInventory, incubator.getInputInventory(), incubator.getOutputInventory()
                    ),
                    state.getBlock().getName()
            ));
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    public BlockState playerWillDestroy(Level level, BlockPos pos, BlockState state, Player player) {
        BlockEntity tileEntity = level.getBlockEntity(pos);
        if (tileEntity instanceof IncubatorBlockEntity incubator) {
            Containers.dropContents(level, pos, incubator.getInputInventory());
            Containers.dropContents(level, pos, incubator.getOutputInventory());
            ItemStack villager = incubator.getVillager();
            if (!villager.isEmpty()) {
                Containers.dropItemStack(level, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, villager);
            }
        }
        return super.playerWillDestroy(level, pos, state, player);
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new IncubatorBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> blockEntityType) {
        return level.isClientSide() ? null : createTickerHelper(blockEntityType, ModBlockEntities.INCUBATOR, IncubatorBlockEntity::serverTick);
    }
}
