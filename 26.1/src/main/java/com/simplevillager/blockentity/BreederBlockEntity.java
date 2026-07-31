package com.simplevillager.blockentity;

import com.simplevillager.config.ModConfig;
import com.simplevillager.blocks.VillagerBlockBase;
import com.simplevillager.datacomponent.VillagerData;
import com.simplevillager.entity.SimpleVillagerEntity;
import com.simplevillager.items.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.entity.npc.villager.VillagerType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jetbrains.annotations.Nullable;

public class BreederBlockEntity extends VillagerBlockEntityBase implements Container, WorldlyContainer {

    private static final int FOOD_SLOTS = 4;
    private static final int OUTPUT_SLOTS = 4;
    private static final int FOOD_THRESHOLD = 24;

    private ItemStack villager1 = ItemStack.EMPTY;
    private ItemStack villager2 = ItemStack.EMPTY;
    @Nullable
    private SimpleVillagerEntity villagerEntity1 = null;
    @Nullable
    private SimpleVillagerEntity villagerEntity2 = null;

    private final SimpleContainer foodInventory = new SimpleContainer(FOOD_SLOTS);
    private final SimpleContainer outputInventory = new SimpleContainer(OUTPUT_SLOTS);

    public BreederBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.BREEDER, pos, state);
    }

    // --- Villager 1 ---

    public ItemStack getVillager1() { return villager1; }
    public boolean hasVillager1() { return !villager1.isEmpty(); }

    @Nullable
    public SimpleVillagerEntity getVillagerEntity1() {
        if (villagerEntity1 == null && !villager1.isEmpty() && level != null) {
            villagerEntity1 = VillagerData.createSimpleVillager(villager1, level);
        }
        return villagerEntity1;
    }

    public void setVillager1(ItemStack stack) {
        this.villager1 = stack;
        this.villagerEntity1 = stack.isEmpty() ? null : VillagerData.createSimpleVillager(stack, level);
        setChanged();
        syncData();
    }

    public ItemStack removeVillager1() {
        ItemStack v = villager1;
        setVillager1(ItemStack.EMPTY);
        return v;
    }

    // --- Villager 2 ---

    public ItemStack getVillager2() { return villager2; }
    public boolean hasVillager2() { return !villager2.isEmpty(); }

    @Nullable
    public SimpleVillagerEntity getVillagerEntity2() {
        if (villagerEntity2 == null && !villager2.isEmpty() && level != null) {
            villagerEntity2 = VillagerData.createSimpleVillager(villager2, level);
        }
        return villagerEntity2;
    }

    public void setVillager2(ItemStack stack) {
        this.villager2 = stack;
        this.villagerEntity2 = stack.isEmpty() ? null : VillagerData.createSimpleVillager(stack, level);
        setChanged();
        syncData();
    }

    public ItemStack removeVillager2() {
        ItemStack v = villager2;
        setVillager2(ItemStack.EMPTY);
        return v;
    }

    // --- Inventories ---

    public SimpleContainer getFoodInventory() { return foodInventory; }
    public SimpleContainer getOutputInventory() { return outputInventory; }

    // --- Breeding ---

    public boolean canBreed() {
        if (!hasVillager1() || !hasVillager2()) return false;
        SimpleVillagerEntity v1 = getVillagerEntity1();
        SimpleVillagerEntity v2 = getVillagerEntity2();
        if (v1 == null || v2 == null) return false;
        if (v1.isBaby() || v2.isBaby()) return false;

        int foodValue = 0;
        for (int i = 0; i < foodInventory.getContainerSize(); i++) {
            ItemStack stack = foodInventory.getItem(i);
            if (!stack.isEmpty()) {
                foodValue += Villager.FOOD_POINTS.getOrDefault(stack.getItem(), 0) * stack.getCount();
            }
        }
        return foodValue >= FOOD_THRESHOLD;
    }

    private boolean removeBreedingItems() {
        int value = 0;
        for (int i = 0; i < foodInventory.getContainerSize(); i++) {
            ItemStack stack = foodInventory.getItem(i);
            if (stack.isEmpty()) continue;
            int itemValue = Villager.FOOD_POINTS.getOrDefault(stack.getItem(), 0);
            if (itemValue <= 0) continue;

            int needed = FOOD_THRESHOLD - value;
            int toRemove = (needed + itemValue - 1) / itemValue;
            int removed = Math.min(toRemove, stack.getCount());
            stack.shrink(removed);
            value += removed * itemValue;
            if (value >= FOOD_THRESHOLD) return true;
        }
        return value >= FOOD_THRESHOLD;
    }

    private boolean createBaby() {
        SimpleVillagerEntity baby = new SimpleVillagerEntity(EntityType.VILLAGER, level);
        baby.setVillagerData(baby.getVillagerData().withType(
                level.registryAccess(),
                VillagerType.byBiome(level.getBiome(worldPosition))
        ));
        baby.setAge(-24000);

        ItemStack babyStack = new ItemStack(ModItems.VILLAGER);
        VillagerData.applyToItem(babyStack, baby);

        for (int i = 0; i < outputInventory.getContainerSize(); i++) {
            if (outputInventory.getItem(i).isEmpty()) {
                outputInventory.setItem(i, babyStack);
                return true;
            }
        }
        return false;
    }

    private void spawnParticles() {
        if (level instanceof ServerLevel serverLevel) {
            for (var player : serverLevel.getPlayers(p -> p.blockPosition().closerThan(worldPosition, 32))) {
                // Send particles to nearby players
            }
        }
        if (level != null) {
            for (int i = 0; i < 5; i++) {
                level.addParticle(ParticleTypes.HEART,
                        worldPosition.getX() + (level.getRandom().nextDouble() - 0.5) + 0.5,
                        worldPosition.getY() + level.getRandom().nextDouble() + 1.0,
                        worldPosition.getZ() + (level.getRandom().nextDouble() - 0.5) + 0.5,
                        0, 0, 0);
            }
        }
    }

    private void tryBreed() {
        if (!canBreed()) return;
        if (!removeBreedingItems()) return;
        if (!createBaby()) return;

        VillagerBlockBase.playVillagerSound(level, worldPosition, SoundEvents.VILLAGER_CELEBRATE);
        spawnParticles();
        setChanged();
        syncData();
    }

    // --- Server tick ---

    public static void serverTick(Level level, BlockPos pos, BlockState state, BreederBlockEntity entity) {
        if (level.isClientSide()) return;

        if (level.getGameTime() % ModConfig.server().breedingTime == 0) {
            entity.tryBreed();
        }

        if (entity.hasVillager1() || entity.hasVillager2()) {
            entity.setChanged();
            if (level.getGameTime() % 20 == 0 && level.getRandom().nextInt(40) == 0) {
                VillagerBlockBase.playVillagerSound(level, pos, SoundEvents.VILLAGER_AMBIENT);
            }
        }
    }

    // --- NBT ---

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        if (hasVillager1()) {
            output.store("Villager1", ItemStack.CODEC, getVillager1());
        }
        if (hasVillager2()) {
            output.store("Villager2", ItemStack.CODEC, getVillager2());
        }

        ValueOutput foodChild = output.child("FoodInventory");
        for (int i = 0; i < foodInventory.getContainerSize(); i++) {
            int idx = i;
            ItemStack stack = foodInventory.getItem(idx);
            if (!stack.isEmpty()) {
                foodChild.store("Slot_" + idx, ItemStack.CODEC, stack);
            }
        }

        ValueOutput outputChild = output.child("OutputInventory");
        for (int i = 0; i < outputInventory.getContainerSize(); i++) {
            int idx = i;
            ItemStack stack = outputInventory.getItem(idx);
            if (!stack.isEmpty()) {
                outputChild.store("Slot_" + idx, ItemStack.CODEC, stack);
            }
        }
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        var opt1 = input.read("Villager1", ItemStack.CODEC);
        if (opt1.isPresent()) {
            villager1 = opt1.get();
            villagerEntity1 = null;
        } else {
            villager1 = ItemStack.EMPTY;
            villagerEntity1 = null;
        }

        var opt2 = input.read("Villager2", ItemStack.CODEC);
        if (opt2.isPresent()) {
            villager2 = opt2.get();
            villagerEntity2 = null;
        } else {
            villager2 = ItemStack.EMPTY;
            villagerEntity2 = null;
        }

        ValueInput foodChild = input.childOrEmpty("FoodInventory");
        for (int i = 0; i < foodInventory.getContainerSize(); i++) {
            int idx = i;
            var opt = foodChild.read("Slot_" + idx, ItemStack.CODEC);
            opt.ifPresent(s -> foodInventory.setItem(idx, s));
        }

        ValueInput outputChild = input.childOrEmpty("OutputInventory");
        for (int i = 0; i < outputInventory.getContainerSize(); i++) {
            int idx = i;
            var opt = outputChild.read("Slot_" + idx, ItemStack.CODEC);
            opt.ifPresent(s -> outputInventory.setItem(idx, s));
        }

        super.loadAdditional(input);
    }

    @Override
    public void setRemoved() {
        super.setRemoved();
        villagerEntity1 = null;
        villagerEntity2 = null;
    }

    // --- Container + WorldlyContainer for hopper support ---

    private static final int[] HOPPER_INPUT_SLOTS = {0, 1, 2, 3};
    private static final int[] HOPPER_OUTPUT_SLOTS = {4, 5, 6, 7};

    private int combinedSize() {
        return foodInventory.getContainerSize() + outputInventory.getContainerSize();
    }

    private Container getInventoryForSlot(int slot) {
        if (slot < foodInventory.getContainerSize()) return foodInventory;
        return outputInventory;
    }

    private int getLocalSlot(int slot) {
        if (slot < foodInventory.getContainerSize()) return slot;
        return slot - foodInventory.getContainerSize();
    }

    @Override
    public int getContainerSize() {
        return combinedSize();
    }

    @Override
    public boolean isEmpty() {
        return foodInventory.isEmpty() && outputInventory.isEmpty();
    }

    @Override
    public ItemStack getItem(int slot) {
        return getInventoryForSlot(slot).getItem(getLocalSlot(slot));
    }

    @Override
    public ItemStack removeItem(int slot, int amount) {
        return getInventoryForSlot(slot).removeItem(getLocalSlot(slot), amount);
    }

    @Override
    public ItemStack removeItemNoUpdate(int slot) {
        return getInventoryForSlot(slot).removeItemNoUpdate(getLocalSlot(slot));
    }

    @Override
    public void setItem(int slot, ItemStack stack) {
        getInventoryForSlot(slot).setItem(getLocalSlot(slot), stack);
    }

    @Override
    public void setChanged() {
        super.setChanged();
    }

    @Override
    public boolean stillValid(Player player) {
        return Container.stillValidBlockEntity(this, player);
    }

    @Override
    public void clearContent() {
        foodInventory.clearContent();
        outputInventory.clearContent();
    }

    @Override
    public boolean canPlaceItem(int slot, ItemStack stack) {
        if (slot < foodInventory.getContainerSize()) {
            return Villager.FOOD_POINTS.getOrDefault(stack.getItem(), 0) > 0;
        }
        return false;
    }

    @Override
    public int[] getSlotsForFace(Direction side) {
        if (side == Direction.UP) return HOPPER_INPUT_SLOTS;
        if (side == Direction.DOWN) return HOPPER_OUTPUT_SLOTS;
        return new int[0];
    }

    @Override
    public boolean canPlaceItemThroughFace(int slot, ItemStack stack, Direction side) {
        if (side == Direction.UP) return canPlaceItem(slot, stack);
        return false;
    }

    @Override
    public boolean canTakeItemThroughFace(int slot, ItemStack stack, Direction side) {
        return side == Direction.DOWN && slot >= foodInventory.getContainerSize();
    }
}
