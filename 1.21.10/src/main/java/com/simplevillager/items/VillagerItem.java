package com.simplevillager.items;

import com.simplevillager.SimpleVillagerMod;
import com.simplevillager.entity.SimpleVillagerEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.DispenserBlock;
import net.minecraft.world.level.block.state.BlockState;

public class VillagerItem extends Item {

    public VillagerItem(Properties properties) {
        super(properties.stacksTo(1));
        DispenserBlock.registerBehavior(this, (source, stack) -> {
            Direction direction = source.state().getValue(DispenserBlock.FACING);
            BlockPos blockpos = source.pos().relative(direction);
            Level world = source.level();
            Villager villager = createVillagerFromStack(world, stack);
            if (villager != null) {
                villager.snapTo(blockpos.getX() + 0.5, blockpos.getY(), blockpos.getZ() + 0.5, direction.toYRot(), 0.0f);
                world.addFreshEntity(villager);
                stack.shrink(1);
            }
            return stack;
        });
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level world = context.getLevel();
        if (world.isClientSide()) {
            return InteractionResult.SUCCESS;
        }
        ItemStack itemstack = context.getItemInHand();
        BlockPos blockpos = context.getClickedPos();
        Direction direction = context.getClickedFace();
        BlockState blockstate = world.getBlockState(blockpos);
        if (!blockstate.getCollisionShape(world, blockpos).isEmpty()) {
            blockpos = blockpos.relative(direction);
        }
        Villager villager = createVillagerFromStack(world, itemstack);
        if (villager == null) {
            villager = new Villager(net.minecraft.world.entity.EntityType.VILLAGER, world);
        }
        villager.setPos(blockpos.getX() + 0.5, blockpos.getY(), blockpos.getZ() + 0.5);
        if (world.addFreshEntity(villager)) {
            itemstack.shrink(1);
        }
        return InteractionResult.CONSUME;
    }

    @Override
    public Component getName(ItemStack stack) {
        com.simplevillager.datacomponent.VillagerData data = stack.get(ModItems.VILLAGER_DATA);
        if (data != null && !data.isEmpty()) {
            net.minecraft.nbt.CompoundTag nbt = data.getNbt();
            Integer age = nbt.getInt("Age").orElse(null);
            if (age != null && age < 0) {
                return Component.translatable("item.simplevillager.baby_villager");
            }
            net.minecraft.nbt.CompoundTag vd = nbt.getCompound("VillagerData").orElse(null);
            if (vd != null) {
                String profession = vd.getString("profession").orElse("");
                if (!profession.isEmpty()) {
                    int colonIndex = profession.indexOf(':');
                    String path = colonIndex >= 0 ? profession.substring(colonIndex + 1) : profession;
                    if (!path.equals("none")) {
                        return Component.translatable("entity.minecraft.villager." + path);
                    }
                }
            }
        }
        return super.getName(stack);
    }

    @Override
    public void inventoryTick(ItemStack stack, ServerLevel level, Entity entity, EquipmentSlot equipmentSlot) {
        super.inventoryTick(stack, level, entity, equipmentSlot);
        if (!(entity instanceof ServerPlayer)) {
            return;
        }
        // TODO: play villager sounds based on config
    }

    public static boolean isBaby(ItemStack stack) {
        com.simplevillager.datacomponent.VillagerData data = stack.get(ModItems.VILLAGER_DATA);
        if (data == null || data.isEmpty()) return false;
        return data.getNbt().getInt("Age").orElse(0) < 0;
    }

    public static Villager createVillagerFromStack(Level level, ItemStack stack) {
        com.simplevillager.datacomponent.VillagerData data = stack.get(ModItems.VILLAGER_DATA);
        if (data == null || data.isEmpty()) {
            return null;
        }
        CompoundTag tag = data.getNbt();
        return SimpleVillagerEntity.fromTag(level, tag);
    }

    public static ItemStack createDefaultVillager() {
        ItemStack stack = new ItemStack(ModItems.VILLAGER);
        CompoundTag compound = new CompoundTag();
        compound.putInt("Age", 0);
        stack.set(ModItems.VILLAGER_DATA, com.simplevillager.datacomponent.VillagerData.of(compound));
        return stack;
    }

    public static ItemStack createBabyVillager() {
        ItemStack stack = new ItemStack(ModItems.VILLAGER);
        CompoundTag compound = new CompoundTag();
        compound.putInt("Age", -24000);
        stack.set(ModItems.VILLAGER_DATA, com.simplevillager.datacomponent.VillagerData.of(compound));
        return stack;
    }
}
