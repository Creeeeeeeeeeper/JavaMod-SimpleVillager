package com.simplevillager.blockentity;

import com.simplevillager.config.ModConfig;
import com.simplevillager.blocks.VillagerBlockBase;
import com.simplevillager.entity.SimpleVillagerEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.npc.VillagerProfession;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Optional;

public class FarmerBlockEntity extends VillagerBlockEntityBase implements Container, WorldlyContainer {

    private static int farmSpeed() { return ModConfig.server().farmerSpeed; }
    private static final int OUTPUT_SLOTS = 4;

    private ItemStack villager = ItemStack.EMPTY;
    @Nullable
    private SimpleVillagerEntity villagerEntity = null;
    @Nullable
    private BlockState crop = null;
    private final SimpleContainer outputInventory = new SimpleContainer(OUTPUT_SLOTS);

    public FarmerBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.FARMER, pos, state);
    }

    // --- Villager management ---

    public ItemStack getVillager() {
        if (villagerEntity != null) {
            saveVillagerEntity();
        }
        return villager;
    }

    public boolean hasVillager() {
        return !villager.isEmpty();
    }

    @Nullable
    public SimpleVillagerEntity getVillagerEntity() {
        if (villagerEntity == null && !villager.isEmpty() && level != null) {
            villagerEntity = com.simplevillager.datacomponent.VillagerData.createSimpleVillager(villager, level);
        }
        return villagerEntity;
    }

    public void saveVillagerEntity() {
        if (villagerEntity != null && !villager.isEmpty()) {
            com.simplevillager.datacomponent.VillagerData.applyToItem(villager, villagerEntity);
        }
    }

    public void setVillager(ItemStack villagerStack) {
        this.villager = villagerStack;
        if (villagerStack.isEmpty()) {
            villagerEntity = null;
        } else {
            villagerEntity = com.simplevillager.datacomponent.VillagerData.createSimpleVillager(villagerStack, level);
            onAddVillager();
        }
        setChanged();
        syncData();
    }

    public ItemStack removeVillager() {
        ItemStack v = getVillager();
        setVillager(ItemStack.EMPTY);
        return v;
    }

    private void onAddVillager() {
        SimpleVillagerEntity v = getVillagerEntity();
        if (v == null) return;
        if (v.getVillagerXp() <= 0 && !v.getVillagerData().profession().is(VillagerProfession.NITWIT)) {
            if (level != null) {
                v.setVillagerData(v.getVillagerData().withProfession(level.registryAccess(), VillagerProfession.FARMER));
            }
        }
    }

    // --- Crop management ---

    public void setCrop(@Nullable Block seed) {
        if (seed == null) {
            this.crop = null;
        } else {
            this.crop = seed.defaultBlockState();
        }
        setChanged();
        syncData();
    }

    @Nullable
    public Block removeSeed() {
        if (this.crop == null) return null;
        Block block = this.crop.getBlock();
        setCrop(null);
        return block;
    }

    @Nullable
    public BlockState getCrop() {
        return this.crop;
    }

    public boolean isValidSeed(net.minecraft.world.item.Item seed) {
        return getSeedCrop(seed) != null;
    }

    @Nullable
    private BlockState getSeedCrop(net.minecraft.world.item.Item seed) {
        if (!(seed instanceof BlockItem blockItem)) return null;
        ItemStack seedStack = new ItemStack(seed);
        if (!seedStack.is(ItemTags.VILLAGER_PLANTABLE_SEEDS)) return null;
        return blockItem.getBlock().defaultBlockState();
    }

    // --- Output inventory ---

    public SimpleContainer getOutputInventory() {
        return outputInventory;
    }

    // --- Server tick ---

    public static void serverTick(Level level, BlockPos pos, BlockState state, FarmerBlockEntity entity) {
        if (!(level instanceof ServerLevel serverLevel)) return;

        SimpleVillagerEntity v = entity.getVillagerEntity();
        if (v != null) {
            VillagerBlockBase.playVillagerSound(level, pos, SoundEvents.VILLAGER_AMBIENT);
        }

        if (level.getGameTime() % 20 == 0 && level.getRandom().nextInt(farmSpeed()) == 0) {
            if (entity.ageCrop(v)) {
                entity.syncData();
                entity.setChanged();
            }
        }
    }

    private boolean ageCrop(@Nullable SimpleVillagerEntity villager) {
        BlockState c = getCrop();
        if (c == null) return false;

        Optional<Property<?>> ageProp = c.getProperties().stream()
                .filter(p -> p.getName().equals("age"))
                .findFirst();

        if (ageProp.isEmpty() || !(ageProp.get() instanceof IntegerProperty intProp)) return false;

        int max = intProp.getPossibleValues().stream().max(Integer::compareTo).orElse(7);
        int age = c.getValue(intProp);

        if (age >= max) {
            if (villager == null || villager.isBaby()) return false;
            if (level != null) {
                Block cropBlock = c.getBlock();
                String cropId = BuiltInRegistries.BLOCK.getKey(cropBlock).toString();
                for (String blacklisted : ModConfig.server().cropBlacklist) {
                    if (cropId.equals(blacklisted)) return false;
                }
            }
            if (!villager.getVillagerData().profession().is(VillagerProfession.FARMER)) return false;

            if (level instanceof ServerLevel serverLevel) {
                List<ItemStack> drops = Block.getDrops(c, serverLevel, worldPosition, null);
                for (ItemStack drop : drops) {
                    if (!drop.isEmpty()) {
                        addItemToOutput(drop);
                    }
                }
                this.crop = c.setValue(intProp, 0);
                VillagerBlockBase.playVillagerSound(level, worldPosition, SoundEvents.VILLAGER_WORK_FARMER);
                return true;
            }
            return false;
        }

        this.crop = c.setValue(intProp, age + 1);
        return true;
    }

    private void addItemToOutput(ItemStack stack) {
        for (int i = 0; i < outputInventory.getContainerSize(); i++) {
            ItemStack existing = outputInventory.getItem(i);
            if (existing.isEmpty()) {
                outputInventory.setItem(i, stack.copy());
                return;
            } else if (ItemStack.isSameItemSameComponents(existing, stack)) {
                int space = existing.getMaxStackSize() - existing.getCount();
                if (space > 0) {
                    int toAdd = Math.min(stack.getCount(), space);
                    existing.grow(toAdd);
                    stack.shrink(toAdd);
                    if (stack.isEmpty()) return;
                }
            }
        }
    }

    // --- NBT ---

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        if (hasVillager()) {
            saveVillagerEntity();
            output.store("Villager", net.minecraft.world.item.ItemStack.CODEC, getVillager());
        }
        if (crop != null) {
            output.putString("Crop", BuiltInRegistries.BLOCK.getKey(crop.getBlock()).toString());
            Optional<Property<?>> ageProp = crop.getProperties().stream()
                    .filter(p -> p.getName().equals("age"))
                    .findFirst();
            if (ageProp.isPresent() && ageProp.get() instanceof IntegerProperty intProp) {
                output.putInt("CropAge", crop.getValue(intProp));
            }
        }
        for (int i = 0; i < outputInventory.getContainerSize(); i++) {
            ItemStack stack = outputInventory.getItem(i);
            if (!stack.isEmpty()) {
                output.store("Slot_" + i, net.minecraft.world.item.ItemStack.CODEC, stack);
            }
        }
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        var optVillager = input.read("Villager", net.minecraft.world.item.ItemStack.CODEC);
        if (optVillager.isPresent()) {
            villager = optVillager.get();
            villagerEntity = null;
        } else {
            villager = ItemStack.EMPTY;
            villagerEntity = null;
        }

        var optCrop = input.getString("Crop");
        if (optCrop.isPresent()) {
            var id = ResourceLocation.tryParse(optCrop.get());
            if (id != null) {
                var block = BuiltInRegistries.BLOCK.getOptional(id);
                if (block.isPresent() && block.get() != Blocks.AIR) {
                    this.crop = block.get().defaultBlockState();
                    int age = input.getIntOr("CropAge", 0);
                    Optional<Property<?>> ageProp = this.crop.getProperties().stream()
                            .filter(p -> p.getName().equals("age"))
                            .findFirst();
                    if (ageProp.isPresent() && ageProp.get() instanceof IntegerProperty intProp) {
                        this.crop = this.crop.setValue(intProp, age);
                    }
                }
            }
        }

        for (int i = 0; i < outputInventory.getContainerSize(); i++) {
            int idx = i;
            var optStack = input.read("Slot_" + idx, net.minecraft.world.item.ItemStack.CODEC);
            optStack.ifPresent(s -> outputInventory.setItem(idx, s));
        }

        super.loadAdditional(input);
    }

    @Override
    public void setRemoved() {
        super.setRemoved();
        villagerEntity = null;
    }

    // --- Container + WorldlyContainer for hopper support ---

    private static final int[] HOPPER_OUTPUT_SLOTS = {0, 1, 2, 3};

    @Override
    public int getContainerSize() {
        return OUTPUT_SLOTS;
    }

    @Override
    public boolean isEmpty() {
        return outputInventory.isEmpty();
    }

    @Override
    public ItemStack getItem(int slot) {
        return outputInventory.getItem(slot);
    }

    @Override
    public ItemStack removeItem(int slot, int amount) {
        return outputInventory.removeItem(slot, amount);
    }

    @Override
    public ItemStack removeItemNoUpdate(int slot) {
        return outputInventory.removeItemNoUpdate(slot);
    }

    @Override
    public void setItem(int slot, ItemStack stack) {
        outputInventory.setItem(slot, stack);
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
        outputInventory.clearContent();
    }

    @Override
    public int[] getSlotsForFace(Direction side) {
        if (side == Direction.DOWN) return HOPPER_OUTPUT_SLOTS;
        return new int[0];
    }

    @Override
    public boolean canPlaceItemThroughFace(int slot, ItemStack stack, Direction side) {
        return false;
    }

    @Override
    public boolean canTakeItemThroughFace(int slot, ItemStack stack, Direction side) {
        return side == Direction.DOWN;
    }
}
