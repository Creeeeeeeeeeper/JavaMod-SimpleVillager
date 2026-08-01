package com.simplevillager.blocks;

import com.simplevillager.blockentity.FarmerBlockEntity;
import com.simplevillager.blockentity.ModBlockEntities;
import com.simplevillager.datacomponent.VillagerData;
import com.simplevillager.entity.SimpleVillagerEntity;
import com.simplevillager.items.VillagerItem;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.npc.VillagerProfession;
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

public class FarmerBlock extends VillagerBlockBase {

    public FarmerBlock(BlockBehaviour.Properties properties) {
        super(properties);
    }

    @Override
    protected MapCodec<? extends VillagerBlockBase> codec() {
        return simpleCodec(FarmerBlock::new);
    }

    private static boolean isVillagerProfessionValid(ItemStack villagerStack, Level level) {
        if (villagerStack.isEmpty() || level == null) return false;
        SimpleVillagerEntity v = VillagerData.createSimpleVillager(villagerStack, level);
        if (v == null || v.isBaby()) return false;
        VillagerProfession p = v.getVillagerData().getProfession();
        return p == VillagerProfession.NONE || p == VillagerProfession.FARMER;
    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack heldItem, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if (level.isClientSide()) {
            BlockEntity t = level.getBlockEntity(pos);
            if (t instanceof FarmerBlockEntity farmer
                    && player.isShiftKeyDown()
                    && farmer.getCrop() == null
                    && farmer.hasVillager()) {
                // Optimistic local update so the rendered villager disappears immediately
                farmer.removeVillager();
            }
            return ItemInteractionResult.SUCCESS;
        }
        BlockEntity tileEntity = level.getBlockEntity(pos);
        if (!(tileEntity instanceof FarmerBlockEntity farmer)) {
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }

        if (!farmer.hasVillager() && heldItem.getItem() instanceof VillagerItem) {
            if (!isVillagerProfessionValid(heldItem, level)) {
                return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
            }
            farmer.setVillager(heldItem.copy());
            heldItem.shrink(1);
            if (heldItem.isEmpty()) {
                player.setItemInHand(hand, ItemStack.EMPTY);
            }
            playVillagerSound(level, pos, SoundEvents.VILLAGER_YES);
            return ItemInteractionResult.SUCCESS;
        } else if (farmer.getCrop() == null && farmer.isValidSeed(heldItem.getItem())) {
            farmer.setCrop(((net.minecraft.world.item.BlockItem) heldItem.getItem()).getBlock());
            heldItem.shrink(1);
            if (heldItem.isEmpty()) {
                player.setItemInHand(hand, ItemStack.EMPTY);
            }
            if (farmer.hasVillager()) {
                playVillagerSound(level, pos, SoundEvents.VILLAGER_WORK_FARMER);
            }
            playVillagerSound(level, pos, SoundEvents.CROP_PLANTED);
            return ItemInteractionResult.SUCCESS;
        } else if (player.isShiftKeyDown() && farmer.getCrop() != null) {
            net.minecraft.world.level.block.Block seedBlock = farmer.removeSeed();
            if (seedBlock != null) {
                ItemStack blockStack = new ItemStack(seedBlock);
                if (heldItem.isEmpty()) {
                    player.setItemInHand(hand, blockStack);
                } else if (!player.getInventory().add(blockStack)) {
                    Direction direction = state.getValue(FACING);
                    Containers.dropItemStack(level, direction.getStepX() + pos.getX() + 0.5, pos.getY() + 0.5, direction.getStepZ() + pos.getZ() + 0.5, blockStack);
                }
                if (farmer.hasVillager()) {
                    playVillagerSound(level, pos, SoundEvents.VILLAGER_NO);
                }
            }
            return ItemInteractionResult.SUCCESS;
        } else if (player.isShiftKeyDown() && farmer.hasVillager()) {
            ItemStack stack = farmer.removeVillager();
            if (heldItem.isEmpty()) {
                player.setItemInHand(hand, stack);
            } else if (!player.getInventory().add(stack)) {
                Direction direction = state.getValue(FACING);
                Containers.dropItemStack(level, direction.getStepX() + pos.getX() + 0.5, pos.getY() + 0.5, direction.getStepZ() + pos.getZ() + 0.5, stack);
            }
            playVillagerSound(level, pos, SoundEvents.VILLAGER_CELEBRATE);
            return ItemInteractionResult.SUCCESS;
        }

        return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (level.isClientSide()) return InteractionResult.SUCCESS;
        BlockEntity tileEntity = level.getBlockEntity(pos);
        if (!(tileEntity instanceof FarmerBlockEntity farmer)) {
            return InteractionResult.PASS;
        }

        player.openMenu(new SimpleMenuProvider(
                (containerId, playerInventory, p) -> new com.simplevillager.gui.OutputContainer(
                        containerId, playerInventory, farmer.getOutputInventory()
                ),
                state.getBlock().getName()
        ));
        return InteractionResult.SUCCESS;
    }

    @Override
    public BlockState playerWillDestroy(Level level, BlockPos pos, BlockState state, Player player) {
        BlockEntity tileEntity = level.getBlockEntity(pos);
        if (tileEntity instanceof FarmerBlockEntity farmer) {
            Containers.dropContents(level, pos, farmer.getOutputInventory());
            ItemStack villager = farmer.getVillager();
            if (!villager.isEmpty()) {
                Containers.dropItemStack(level, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, villager);
            }
        }
        return super.playerWillDestroy(level, pos, state, player);
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new FarmerBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> blockEntityType) {
        return level.isClientSide() ? null : createTickerHelper(blockEntityType, ModBlockEntities.FARMER, FarmerBlockEntity::serverTick);
    }
}
