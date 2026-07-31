package com.simplevillager.blocks;

import com.simplevillager.blockentity.InventoryViewerBlockEntity;
import com.simplevillager.blockentity.ModBlockEntities;
import com.simplevillager.items.VillagerItem;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.SimpleContainer;
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

public class InventoryViewerBlock extends VillagerBlockBase {

    public InventoryViewerBlock(BlockBehaviour.Properties properties) {
        super(properties);
    }

    @Override
    protected MapCodec<? extends VillagerBlockBase> codec() {
        return simpleCodec(InventoryViewerBlock::new);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (level.isClientSide()) return InteractionResult.SUCCESS;
        BlockEntity tileEntity = level.getBlockEntity(pos);
        if (!(tileEntity instanceof InventoryViewerBlockEntity viewer)) {
            return super.useWithoutItem(state, level, pos, player, hit);
        }

        if (player.isSecondaryUseActive()) {
            // Shift + right click: extract villager
            if (viewer.hasVillager()) {
                ItemStack villager = viewer.removeVillager();
                if (!player.getInventory().add(villager)) {
                    Containers.dropItemStack(level, pos.getX() + 0.5, pos.getY() + 1.5, pos.getZ() + 0.5, villager);
                }
                VillagerBlockBase.playVillagerSound(level, pos, SoundEvents.VILLAGER_CELEBRATE);
            }
        } else if (viewer.hasVillager()) {
            // Normal right click: open GUI
            player.openMenu(new SimpleMenuProvider(
                    (containerId, playerInventory, p) -> new com.simplevillager.gui.InventoryViewerContainer(
                            containerId, playerInventory, viewer
                    ),
                    state.getBlock().getName()
            ));
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if (level.isClientSide()) return InteractionResult.TRY_WITH_EMPTY_HAND;
        BlockEntity tileEntity = level.getBlockEntity(pos);
        if (!(tileEntity instanceof InventoryViewerBlockEntity viewer)) {
            return InteractionResult.TRY_WITH_EMPTY_HAND;
        }

        // Right click with villager item: insert villager
        if (!viewer.hasVillager() && stack.getItem() instanceof VillagerItem) {
            viewer.setVillager(stack.copy());
            stack.shrink(1);
            VillagerBlockBase.playVillagerSound(level, pos, SoundEvents.VILLAGER_NO);
            return InteractionResult.SUCCESS;
        }
        return InteractionResult.TRY_WITH_EMPTY_HAND;
    }

    @Override
    public net.minecraft.world.level.block.state.BlockState playerWillDestroy(Level level, BlockPos pos, BlockState state, Player player) {
        BlockEntity tileEntity = level.getBlockEntity(pos);
        if (tileEntity instanceof InventoryViewerBlockEntity viewer) {
            // Drop villager's inventory contents
            SimpleContainer villagerInv = viewer.getVillagerInventory();
            Containers.dropContents(level, pos, villagerInv);

            // Drop villager item
            ItemStack villager = viewer.getVillager();
            if (!villager.isEmpty()) {
                Containers.dropItemStack(level, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, villager);
            }
        }
        return super.playerWillDestroy(level, pos, state, player);
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new InventoryViewerBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> blockEntityType) {
        return level.isClientSide() ? null : createTickerHelper(blockEntityType, ModBlockEntities.INVENTORY_VIEWER, InventoryViewerBlockEntity::serverTick);
    }
}
