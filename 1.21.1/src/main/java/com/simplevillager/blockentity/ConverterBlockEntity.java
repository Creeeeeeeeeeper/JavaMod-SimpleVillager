package com.simplevillager.blockentity;

import com.simplevillager.config.ModConfig;
import com.simplevillager.blocks.VillagerBlockBase;
import com.simplevillager.datacomponent.VillagerData;
import com.simplevillager.entity.SimpleVillagerEntity;
import com.simplevillager.items.VillagerItem;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.entity.ai.village.ReputationEventType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.item.alchemy.Potions;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3f;

import java.util.UUID;

public class ConverterBlockEntity extends VillagerBlockEntityBase implements Container, WorldlyContainer {

    private static final int INPUT_SLOTS = 4;
    private static final int OUTPUT_SLOTS = 4;

    private static final DustParticleOptions EFFECT_GREEN = new DustParticleOptions(new Vector3f(0.0F, 0.8F, 0.2F), 1.0F);
    private static final DustParticleOptions EFFECT_PURPLE = new DustParticleOptions(new Vector3f(0.5F, 0.0F, 0.8F), 1.0F);

    // Phase 1: 0 → ZOMBIFY_TIME (5s) — villager being zombified
    private static final int ZOMBIFY_TIME = 100;
    // Phase 2: ZOMBIFY_TIME → CURE_TIME (configurable) — zombie villager being cured
    private static final int CURE_TIME = 1900;
    // Phase 3: CURE_TIME → FINALIZE_TIME (5s) — villager restored, output delay
    private static final int FINALIZE_TIME = 2000;

    private static final int[] HOPPER_INPUT_SLOTS = {0, 1, 2, 3};
    private static final int[] HOPPER_OUTPUT_SLOTS = {4, 5, 6, 7};

    private ItemStack villager = ItemStack.EMPTY;
    @Nullable
    private SimpleVillagerEntity villagerEntity = null;
    private final SimpleContainer inputInventory = new SimpleContainer(INPUT_SLOTS);
    private final SimpleContainer outputInventory = new SimpleContainer(OUTPUT_SLOTS);
    private long timer = 0;
    @Nullable
    private UUID owner = null;

    public ConverterBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.CONVERTER, pos, state);
    }

    // --- Villager management ---

    public ItemStack getVillager() {
        if (villagerEntity != null) {
            VillagerData.applyToItem(villager, villagerEntity);
        }
        return villager;
    }

    public boolean hasVillager() {
        return !villager.isEmpty();
    }

    @Nullable
    public SimpleVillagerEntity getVillagerEntity() {
        if (villager.isEmpty()) return null;
        if (villagerEntity == null && level != null) {
            villagerEntity = VillagerData.createSimpleVillager(villager, level);
        }
        return villagerEntity;
    }

    public void setVillager(ItemStack stack) {
        this.villager = stack;
        this.villagerEntity = stack.isEmpty() ? null : VillagerData.createSimpleVillager(stack, level);
        setChanged();
        syncData();
    }

    public ItemStack removeVillager() {
        ItemStack v = getVillager();
        setVillager(ItemStack.EMPTY);
        return v;
    }

    // --- Inventories ---

    public SimpleContainer getInputInventory() { return inputInventory; }
    public SimpleContainer getOutputInventory() { return outputInventory; }

    // --- Owner ---

    public void setOwner(UUID uuid) { this.owner = uuid; }
    @Nullable public UUID getOwner() { return owner; }

    @Nullable
    public Player getOwnerPlayer() {
        if (owner == null || level == null) return null;
        if (level instanceof ServerLevel serverLevel) {
            return serverLevel.getServer().getPlayerList().getPlayer(owner);
        }
        return level.getPlayerByUUID(owner);
    }

    // --- Timer ---

    public long getTimer() { return timer; }
    private boolean isZombiePhase() { return timer >= ZOMBIFY_TIME && timer < ModConfig.server().convertingTime; }

    // --- Potion check ---

    public static boolean isWeaknessPotion(ItemStack stack) {
        PotionContents potionContents = stack.get(net.minecraft.core.component.DataComponents.POTION_CONTENTS);
        if (potionContents == null) return false;
        return potionContents.potion().filter(p ->
                p.is(Potions.WEAKNESS) || p.is(Potions.LONG_WEAKNESS)
        ).isPresent();
    }

    public static boolean isValidInput(ItemStack stack) {
        return stack.getItem() instanceof VillagerItem
                || stack.getItem() == Items.GOLDEN_APPLE
                || isWeaknessPotion(stack);
    }

    // --- Conversion logic ---

    private boolean tryStartConversion() {
        if (hasVillager() || timer > 0) return false;

        int villagerSlot = -1;
        int appleSlot = -1;
        int potionSlot = -1;

        for (int i = 0; i < inputInventory.getContainerSize(); i++) {
            ItemStack stack = inputInventory.getItem(i);
            if (stack.isEmpty()) continue;
            if (villagerSlot < 0 && stack.getItem() instanceof VillagerItem) {
                villagerSlot = i;
            } else if (appleSlot < 0 && stack.getItem() == Items.GOLDEN_APPLE) {
                appleSlot = i;
            } else if (potionSlot < 0 && isWeaknessPotion(stack)) {
                potionSlot = i;
            }
        }

        if (villagerSlot < 0 || appleSlot < 0 || potionSlot < 0) return false;

        ItemStack villagerStack = inputInventory.getItem(villagerSlot).copy();
        villagerStack.setCount(1);
        setVillager(villagerStack);
        inputInventory.getItem(villagerSlot).shrink(1);
        inputInventory.getItem(appleSlot).shrink(1);
        inputInventory.getItem(potionSlot).shrink(1);
        return true;
    }

    // --- Server tick ---

    public static void serverTick(Level level, BlockPos pos, BlockState state, ConverterBlockEntity entity) {
        if (level.isClientSide()) return;

        // Try to start conversion if all 3 items are present
        if (entity.timer <= 0 && !entity.hasVillager()) {
            if (entity.tryStartConversion()) {
                entity.syncData();
            }
        }

        // Process conversion
        int cureTime = ModConfig.server().convertingTime;
        if (entity.hasVillager()) {
            // Phase 1 → Phase 2: villager → zombie villager
            if (entity.timer == ZOMBIFY_TIME) {
                VillagerBlockBase.playVillagerSound(level, pos, SoundEvents.ZOMBIE_INFECT);
                if (level instanceof ServerLevel serverLevel) {
                    serverLevel.sendParticles(EFFECT_PURPLE, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, 15, 0.3, 0.3, 0.3, 0.1);
                }
                entity.syncData();
            }
            // Phase 2 → Phase 3: zombie villager → villager
            else if (entity.timer == cureTime) {
                VillagerBlockBase.playVillagerSound(level, pos, SoundEvents.ZOMBIE_VILLAGER_CONVERTED);
                if (level instanceof ServerLevel serverLevel) {
                    serverLevel.sendParticles(EFFECT_GREEN, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, 20, 0.3, 0.3, 0.3, 0.1);
                }
                entity.syncData();
            }
            // Phase 3 → output: output villager after delay
            else if (entity.timer >= cureTime + 100) {
                Player ownerPlayer = entity.getOwnerPlayer();
                if (ownerPlayer != null) {
                    SimpleVillagerEntity v = entity.getVillagerEntity();
                    if (v != null) {
                    if (ModConfig.server().universalReputation) {
                        v.onReputationEventFrom(ReputationEventType.ZOMBIE_VILLAGER_CURED, ownerPlayer);
                    }
                }
                    ItemStack villagerStack = entity.removeVillager();
                    if (!villagerStack.isEmpty()) {
                        for (int i = 0; i < entity.outputInventory.getContainerSize(); i++) {
                            if (entity.outputInventory.getItem(i).isEmpty()) {
                                entity.outputInventory.setItem(i, villagerStack);
                                entity.timer = 0;
                                entity.syncData();
                                entity.setChanged();
                                break;
                            }
                        }
                    }
                }
            }

            // Effect particles during zombie phase (every 30 ticks)
            if (entity.timer > ZOMBIFY_TIME && entity.timer < cureTime && entity.timer % 30 == 0) {
                if (level instanceof ServerLevel serverLevel) {
                    serverLevel.sendParticles(EFFECT_PURPLE, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, 5, 0.3, 0.3, 0.3, 0.02);
                }
            }

            // Ambient sounds
            if (entity.timer % 40 == 0 && level.getRandom().nextInt(3) == 0) {
                if (entity.isZombiePhase()) {
                    VillagerBlockBase.playVillagerSound(level, pos, SoundEvents.ZOMBIE_VILLAGER_AMBIENT);
                } else {
                    VillagerBlockBase.playVillagerSound(level, pos, SoundEvents.VILLAGER_AMBIENT);
                }
            }
        }

        // Increment timer
        if (entity.hasVillager() || entity.timer > 0) {
            entity.timer++;
            entity.setChanged();
        }

        // Reset if no villager
        if (!entity.hasVillager() && entity.timer != 0) {
            entity.timer = 0;
            entity.setChanged();
        }
    }

    // --- NBT ---

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider provider) {
        super.saveAdditional(tag, provider);
        if (hasVillager()) {
            tag.put("Villager", getVillager().saveOptional(provider));
        }

        CompoundTag inputChild = new CompoundTag();
        for (int i = 0; i < inputInventory.getContainerSize(); i++) {
            ItemStack stack = inputInventory.getItem(i);
            if (!stack.isEmpty()) {
                inputChild.put("Slot_" + i, stack.saveOptional(provider));
            }
        }
        tag.put("InputInventory", inputChild);

        CompoundTag outputChild = new CompoundTag();
        for (int i = 0; i < outputInventory.getContainerSize(); i++) {
            ItemStack stack = outputInventory.getItem(i);
            if (!stack.isEmpty()) {
                outputChild.put("Slot_" + i, stack.saveOptional(provider));
            }
        }
        tag.put("OutputInventory", outputChild);

        tag.putLong("Timer", timer);
        if (owner != null) {
            tag.putUUID("Owner", owner);
        }
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider provider) {
        if (tag.contains("Villager")) {
            villager = ItemStack.parseOptional(provider, tag.getCompound("Villager"));
        } else {
            villager = ItemStack.EMPTY;
        }
        villagerEntity = null;

        CompoundTag inputChild = tag.getCompound("InputInventory");
        for (int i = 0; i < inputInventory.getContainerSize(); i++) {
            if (inputChild.contains("Slot_" + i)) {
                inputInventory.setItem(i, ItemStack.parseOptional(provider, inputChild.getCompound("Slot_" + i)));
            }
        }

        CompoundTag outputChild = tag.getCompound("OutputInventory");
        for (int i = 0; i < outputInventory.getContainerSize(); i++) {
            if (outputChild.contains("Slot_" + i)) {
                outputInventory.setItem(i, ItemStack.parseOptional(provider, outputChild.getCompound("Slot_" + i)));
            }
        }

        timer = tag.getLong("Timer");
        owner = tag.hasUUID("Owner") ? tag.getUUID("Owner") : null;

        super.loadAdditional(tag, provider);
    }

    @Override
    public void setRemoved() {
        super.setRemoved();
        villagerEntity = null;
    }

    // --- Container + WorldlyContainer for hopper support ---

    private int combinedSize() {
        return INPUT_SLOTS + OUTPUT_SLOTS;
    }

    private Container getInventoryForSlot(int slot) {
        if (slot < INPUT_SLOTS) return inputInventory;
        return outputInventory;
    }

    private int getLocalSlot(int slot) {
        if (slot < INPUT_SLOTS) return slot;
        return slot - INPUT_SLOTS;
    }

    @Override
    public int getContainerSize() {
        return combinedSize();
    }

    @Override
    public boolean isEmpty() {
        return inputInventory.isEmpty() && outputInventory.isEmpty();
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
        inputInventory.clearContent();
        outputInventory.clearContent();
    }

    @Override
    public boolean canPlaceItem(int slot, ItemStack stack) {
        if (slot < INPUT_SLOTS) {
            return isValidInput(stack);
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
        return side == Direction.DOWN && slot >= INPUT_SLOTS;
    }
}
