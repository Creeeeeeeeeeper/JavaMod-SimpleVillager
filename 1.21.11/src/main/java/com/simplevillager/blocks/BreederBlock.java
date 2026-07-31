package com.simplevillager.blocks;

import com.simplevillager.blockentity.BreederBlockEntity;
import com.simplevillager.blockentity.ModBlockEntities;
import com.simplevillager.items.VillagerItem;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionHand;
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

public class BreederBlock extends VillagerBlockBase {

    static {
        System.out.println("[SV] BreederBlock class loaded!");
    }

    public BreederBlock(BlockBehaviour.Properties properties) {
        super(properties);
    }

    @Override
    protected MapCodec<? extends VillagerBlockBase> codec() {
        return simpleCodec(BreederBlock::new);
    }

    @Override
    protected InteractionResult useItemOn(ItemStack heldItem, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        System.out.println("[SV] useItemOn called! item=" + heldItem.getItem().getClass().getSimpleName());
        BlockEntity tileEntity = level.getBlockEntity(pos);
        if (!(tileEntity instanceof BreederBlockEntity breeder)) {
            return super.useItemOn(heldItem, state, level, pos, player, hand, hit);
        }

        if (heldItem.getItem() instanceof VillagerItem) {
            com.simplevillager.datacomponent.VillagerData data = heldItem.get(com.simplevillager.items.ModItems.VILLAGER_DATA);
            if (data == null || data.isEmpty()) {
                System.out.println("[SV] breeder: no villager data");
                return InteractionResult.TRY_WITH_EMPTY_HAND;
            }
            int age = data.getNbt().getInt("Age").orElse(0);
            System.out.println("[SV] breeder: age=" + age + " nbt=" + data.getNbt());
            if (age >= 0) return InteractionResult.TRY_WITH_EMPTY_HAND;

            if (!breeder.hasVillager1()) {
                breeder.setVillager1(heldItem.copy());
                heldItem.shrink(1);
                if (heldItem.isEmpty()) player.setItemInHand(hand, ItemStack.EMPTY);
                playVillagerSound(level, pos, SoundEvents.VILLAGER_YES);
                return InteractionResult.SUCCESS;
            } else if (!breeder.hasVillager2()) {
                breeder.setVillager2(heldItem.copy());
                heldItem.shrink(1);
                if (heldItem.isEmpty()) player.setItemInHand(hand, ItemStack.EMPTY);
                playVillagerSound(level, pos, SoundEvents.VILLAGER_YES);
                return InteractionResult.SUCCESS;
            }
        } else if (player.isShiftKeyDown() && breeder.hasVillager2()) {
            ItemStack stack = breeder.removeVillager2();
            if (heldItem.isEmpty()) {
                player.setItemInHand(hand, stack);
            } else if (!player.getInventory().add(stack)) {
                Direction direction = state.getValue(FACING);
                Containers.dropItemStack(level, direction.getStepX() + pos.getX() + 0.5, pos.getY() + 0.5, direction.getStepZ() + pos.getZ() + 0.5, stack);
            }
            playVillagerSound(level, pos, SoundEvents.VILLAGER_CELEBRATE);
            return InteractionResult.SUCCESS;
        } else if (player.isShiftKeyDown() && breeder.hasVillager1()) {
            ItemStack stack = breeder.removeVillager1();
            if (heldItem.isEmpty()) {
                player.setItemInHand(hand, stack);
            } else if (!player.getInventory().add(stack)) {
                Direction direction = state.getValue(FACING);
                Containers.dropItemStack(level, direction.getStepX() + pos.getX() + 0.5, pos.getY() + 0.5, direction.getStepZ() + pos.getZ() + 0.5, stack);
            }
            playVillagerSound(level, pos, SoundEvents.VILLAGER_CELEBRATE);
            return InteractionResult.SUCCESS;
        }

        return InteractionResult.TRY_WITH_EMPTY_HAND;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        System.out.println("[SV] useWithoutItem called! client=" + level.isClientSide());
        if (level.isClientSide()) return InteractionResult.SUCCESS;
        BlockEntity tileEntity = level.getBlockEntity(pos);
        if (!(tileEntity instanceof BreederBlockEntity breeder)) {
            return super.useWithoutItem(state, level, pos, player, hit);
        }

        player.openMenu(new SimpleMenuProvider(
                (containerId, playerInventory, p) -> new com.simplevillager.gui.BreederContainer(
                        containerId, playerInventory, breeder.getFoodInventory(), breeder.getOutputInventory()
                ),
                state.getBlock().getName()
        ));
        return InteractionResult.SUCCESS;
    }

    @Override
    public net.minecraft.world.level.block.state.BlockState playerWillDestroy(Level level, BlockPos pos, BlockState state, Player player) {
        BlockEntity tileEntity = level.getBlockEntity(pos);
        if (tileEntity instanceof BreederBlockEntity breeder) {
            Containers.dropContents(level, pos, breeder.getOutputInventory());
            ItemStack v1 = breeder.getVillager1();
            if (!v1.isEmpty()) Containers.dropItemStack(level, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, v1);
            ItemStack v2 = breeder.getVillager2();
            if (!v2.isEmpty()) Containers.dropItemStack(level, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, v2);
        }
        return super.playerWillDestroy(level, pos, state, player);
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new BreederBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> blockEntityType) {
        return level.isClientSide() ? null : createTickerHelper(blockEntityType, ModBlockEntities.BREEDER, BreederBlockEntity::serverTick);
    }
}
