package com.simplevillager.blockentity;

import com.simplevillager.datacomponent.VillagerData;
import com.simplevillager.entity.SimpleVillagerEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.entity.ai.village.poi.PoiTypes;
import net.minecraft.world.entity.npc.villager.VillagerProfession;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import com.simplevillager.config.ModConfig;
import com.simplevillager.blocks.VillagerBlockBase;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.world.item.trading.MerchantOffers;
import org.jetbrains.annotations.Nullable;

import net.minecraft.world.entity.npc.villager.Villager;

public class TraderBlockEntity extends VillagerBlockEntityBase implements WorkstationBlockEntity, Container, WorldlyContainer {

    private ItemStack villager = ItemStack.EMPTY;
    private SimpleVillagerEntity villagerEntity = null;
    private Block workstation = Blocks.AIR;
    private long nextRestock = 0;
    private final SimpleContainer inputInventory = new SimpleContainer(4);
    private final SimpleContainer outputInventory = new SimpleContainer(4);

    public TraderBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.TRADER, pos, state);
    }

    public SimpleContainer getInputInventory() {
        return inputInventory;
    }

    public SimpleContainer getOutputInventory() {
        return outputInventory;
    }

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
            villagerEntity = VillagerData.createSimpleVillager(villager, level);
            if (villagerEntity != null) {
                villagerEntity.setupBrainForBlock(level, getBlockPos());
                applyTradeLimits();
            }
        }
        return villagerEntity;
    }

    public void saveVillagerEntity() {
        if (villagerEntity != null && !villager.isEmpty()) {
            VillagerData.applyToItem(villager, villagerEntity);
        }
    }

    public void setVillager(ItemStack villagerStack) {
        this.villager = villagerStack;
        removeTradingPlayer();
        if (villagerStack.isEmpty()) {
            villagerEntity = null;
        } else {
            villagerEntity = VillagerData.createSimpleVillager(villagerStack, level);
            if (hasWorkstation()) {
                fixProfession();
            }
            applyTradeLimits();
        }
        setChanged();
        syncData();
    }

    public ItemStack removeVillager() {
        ItemStack v = getVillager();
        setVillager(ItemStack.EMPTY);
        return v;
    }

    public void removeTradingPlayer() {
        if (villagerEntity != null) {
            villagerEntity.setTradingPlayer(null);
        }
    }

    public Block getWorkstation() {
        return workstation;
    }

    public boolean hasWorkstation() {
        return workstation != Blocks.AIR;
    }

    public void setWorkstation(Block block) {
        this.workstation = block;
        if (hasVillager()) {
            fixProfession();
        }
        setChanged();
        syncData();
    }

    public Block removeWorkstation() {
        Block w = workstation;
        setWorkstation(Blocks.AIR);
        return w;
    }

    public boolean isValidBlock(Block block) {
        return PoiTypes.forState(block.defaultBlockState()).isPresent();
    }

    public Holder<VillagerProfession> getWorkstationProfession() {
        var poiTypeHolder = PoiTypes.forState(workstation.defaultBlockState());
        if (poiTypeHolder.isEmpty()) {
            return BuiltInRegistries.VILLAGER_PROFESSION.get(VillagerProfession.NONE).orElseThrow();
        }
        var poiType = poiTypeHolder.get();
        for (VillagerProfession profession : BuiltInRegistries.VILLAGER_PROFESSION) {
            if (profession.heldJobSite().test(poiType)) {
                return BuiltInRegistries.VILLAGER_PROFESSION.wrapAsHolder(profession);
            }
        }
        return BuiltInRegistries.VILLAGER_PROFESSION.get(VillagerProfession.NONE).orElseThrow();
    }

    private void fixProfession() {
        SimpleVillagerEntity v = getVillagerEntity();
        if (v == null || v.getVillagerXp() > 0 || v.getVillagerData().profession().is(VillagerProfession.NITWIT)) {
            return;
        }
        v.setVillagerData(v.getVillagerData().withProfession(getWorkstationProfession()));
    }

    private void applyTradeLimits() {
        if (level == null || level.isClientSide()) return;
        SimpleVillagerEntity v = getVillagerEntity();
        if (v == null) return;
        MerchantOffers offers = v.getOffers();
        int maxUses = ModConfig.server().traderMaxUses;
        for (MerchantOffer o : offers) {
            if (maxUses > 0) {
                ((com.simplevillager.mixin.MerchantOfferAccessor) o).SimpleVillager$setMaxUses(maxUses);
            }
        }
    }

    private int getTotalUses() {
        SimpleVillagerEntity v = getVillagerEntity();
        if (v == null) return 0;
        int total = 0;
        for (MerchantOffer o : v.getOffers()) {
            total += o.getUses();
        }
        return total;
    }

    public boolean openTradingGUI(Player playerEntity) {
        SimpleVillagerEntity villagerEntity = getVillagerEntity();
        if (villagerEntity == null || villagerEntity.isBaby()) {
            return false;
        }
        Holder<VillagerProfession> profession = villagerEntity.getVillagerData().profession();
        if (profession.is(VillagerProfession.NONE) || profession.is(VillagerProfession.NITWIT) || villagerEntity.isTrading()) {
            return false;
        }
        if (level == null || level.isClientSide()) {
            return true;
        }
        villagerEntity.setPos(getBlockPos().getX() + 0.5, getBlockPos().getY() + 1.0, getBlockPos().getZ() + 0.5);
        for (MerchantOffer offer : villagerEntity.getOffers()) {
            offer.resetSpecialPriceDiff();
        }
        villagerEntity.setTradingPlayer(playerEntity);
        ((com.simplevillager.mixin.VillagerAccessor) villagerEntity).SimpleVillager$callUpdateSpecialPrices(playerEntity);
        villagerEntity.openTradingScreen(playerEntity, villagerEntity.getDisplayName(), villagerEntity.getVillagerData().level());
        return true;
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, TraderBlockEntity entity) {
        if (level instanceof ServerLevel serverLevel) {
            if (!entity.hasVillager()) {
                entity.setChanged();
                return;
            }

            SimpleVillagerEntity v = entity.getVillagerEntity();
            if (v == null) {
                entity.setChanged();
                return;
            }

            entity.saveVillagerEntity();
            entity.setChanged();

            if (processLevelUp(v, serverLevel)) {
                entity.saveVillagerEntity();
                entity.setChanged();
                entity.syncData();
            }

            if (!v.isTrading()) {
                entity.applyTradeLimits();
                boolean needRestock = entity.getTotalUses() >= ModConfig.server().traderRestockUses;
                if (!needRestock && level.getGameTime() - entity.getLastRestock() > entity.nextRestock
                        && v.getVillagerData().profession().is(entity.getWorkstationProfession())) {
                    needRestock = true;
                }
                if (needRestock) {
                    entity.restock();
                    entity.nextRestock = entity.calculateNextRestock();
                }
            }
        }
    }

    private static boolean processLevelUp(Villager villager, ServerLevel serverLevel) {
        com.simplevillager.mixin.VillagerAccessor accessor = (com.simplevillager.mixin.VillagerAccessor) villager;
        if (accessor.SimpleVillager$getIncreaseProfessionLevelOnUpdate()) {
            accessor.SimpleVillager$callIncreaseMerchantCareer(serverLevel);
            accessor.SimpleVillager$setIncreaseProfessionLevelOnUpdate(false);
            return true;
        }
        return false;
    }

    protected long calculateNextRestock() {
        return ModConfig.server().traderRestockTime + level.getRandom().nextInt(Math.max(3600 - 1200, 1));
    }

    protected void restock() {
        try {
            SimpleVillagerEntity villagerEntity = getVillagerEntity();
            if (villagerEntity == null) return;
            villagerEntity.restock();
            SoundEvent workSound = villagerEntity.getVillagerData().profession().value().workSound();
            if (workSound != null) {
                VillagerBlockBase.playVillagerSound(level, getBlockPos(), workSound);
            }
        } catch (Exception e) {
            // ignore
        }
    }

    protected long getLastRestock() {
        SimpleVillagerEntity villagerEntity = getVillagerEntity();
        if (villagerEntity == null) return 0L;
        return ((com.simplevillager.mixin.VillagerAccessor) villagerEntity).SimpleVillager$getLastRestockGameTime();
    }

    // --- Container interface (hopper support) ---

    @Override
    public int getContainerSize() {
        return 8;
    }

    @Override
    public boolean isEmpty() {
        for (int i = 0; i < 4; i++) {
            if (!inputInventory.getItem(i).isEmpty()) return false;
        }
        for (int i = 0; i < 4; i++) {
            if (!outputInventory.getItem(i).isEmpty()) return false;
        }
        return true;
    }

    @Override
    public ItemStack getItem(int index) {
        return index < 4 ? inputInventory.getItem(index) : outputInventory.getItem(index - 4);
    }

    @Override
    public ItemStack removeItem(int index, int count) {
        return index < 4 ? inputInventory.removeItem(index, count) : outputInventory.removeItem(index - 4, count);
    }

    @Override
    public ItemStack removeItemNoUpdate(int index) {
        return index < 4 ? inputInventory.removeItemNoUpdate(index) : outputInventory.removeItemNoUpdate(index - 4);
    }

    @Override
    public void setItem(int index, ItemStack stack) {
        if (index < 4) inputInventory.setItem(index, stack);
        else outputInventory.setItem(index - 4, stack);
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

    // --- WorldlyContainer (direction-based hopper) ---

    @Override
    public int[] getSlotsForFace(Direction side) {
        if (side == Direction.DOWN) {
            return new int[]{4, 5, 6, 7};
        } else if (side == Direction.UP) {
            return new int[]{0, 1, 2, 3};
        }
        return new int[0];
    }

    @Override
    public boolean canPlaceItemThroughFace(int index, ItemStack stack, Direction side) {
        return side == Direction.UP && index < 4;
    }

    @Override
    public boolean canTakeItemThroughFace(int index, ItemStack stack, Direction side) {
        return side == Direction.DOWN && index >= 4;
    }

    // --- Save/Load ---

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        if (hasVillager()) {
            saveVillagerEntity();
            output.store("Villager", ItemStack.CODEC, getVillager());
        }
        if (hasWorkstation()) {
            output.putString("Workstation", BuiltInRegistries.BLOCK.getKey(workstation).toString());
        }
        output.putLong("NextRestock", nextRestock);

        ValueOutput inputChild = output.child("InputInventory");
        for (int i = 0; i < inputInventory.getContainerSize(); i++) {
            if (!inputInventory.getItem(i).isEmpty()) {
                inputChild.store("Slot_" + i, ItemStack.CODEC, inputInventory.getItem(i));
            }
        }

        ValueOutput outputChild = output.child("OutputInventory");
        for (int i = 0; i < outputInventory.getContainerSize(); i++) {
            if (!outputInventory.getItem(i).isEmpty()) {
                outputChild.store("Slot_" + i, ItemStack.CODEC, outputInventory.getItem(i));
            }
        }
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        var optionalItemStack = input.read("Villager", ItemStack.CODEC);
        if (optionalItemStack.isPresent()) {
            villager = optionalItemStack.get();
            villagerEntity = null;
        } else {
            villager = ItemStack.EMPTY;
            villagerEntity = null;
        }
        var optionalWorkstation = input.read("Workstation", Identifier.CODEC);
        if (optionalWorkstation.isPresent()) {
            workstation = BuiltInRegistries.BLOCK.get(optionalWorkstation.get()).map(v -> (Block) v.value()).orElse(Blocks.AIR);
        } else {
            workstation = Blocks.AIR;
        }
        nextRestock = input.getLongOr("NextRestock", 0L);

        ValueInput inputChild = input.childOrEmpty("InputInventory");
        for (int i = 0; i < inputInventory.getContainerSize(); i++) {
            var opt = inputChild.read("Slot_" + i, ItemStack.CODEC);
            if (opt.isPresent()) inputInventory.setItem(i, opt.get());
        }

        ValueInput outputChild = input.childOrEmpty("OutputInventory");
        for (int i = 0; i < outputInventory.getContainerSize(); i++) {
            var opt = outputChild.read("Slot_" + i, ItemStack.CODEC);
            if (opt.isPresent()) outputInventory.setItem(i, opt.get());
        }

        super.loadAdditional(input);
    }

    @Override
    public void setRemoved() {
        removeTradingPlayer();
        super.setRemoved();
    }
}
