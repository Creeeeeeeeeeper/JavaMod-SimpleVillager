package com.simplevillager.blockentity;

import com.simplevillager.config.ModConfig;
import com.simplevillager.datacomponent.VillagerData;
import com.simplevillager.entity.SimpleVillagerEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.util.Mth;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.Container;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.entity.ai.gossip.GossipContainer;
import net.minecraft.world.entity.ai.gossip.GossipType;
import net.minecraft.world.entity.ai.village.poi.PoiTypes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.npc.VillagerProfession;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.trading.ItemCost;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.world.item.trading.MerchantOffers;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import com.simplevillager.blocks.VillagerBlockBase;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

public class AutoTraderBlockEntity extends VillagerBlockEntityBase implements WorkstationBlockEntity, Container, WorldlyContainer {

    private static final int[] INPUT_SLOTS = {0, 1, 2, 3};  // top (hopper insert)
    private static final int[] OUTPUT_SLOTS = {4, 5, 6, 7}; // bottom (hopper extract)

    private ItemStack villager = ItemStack.EMPTY;
    private SimpleVillagerEntity villagerEntity = null;
    private Block workstation = Blocks.AIR;
    private long nextRestock = 0;
    private int tradeIndex = 0;

    private final SimpleContainer tradeGuiInv = new SimpleContainer(3);
    private final SimpleContainer inputInventory = new SimpleContainer(4);
    private final SimpleContainer outputInventory = new SimpleContainer(4);

    public AutoTraderBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.AUTO_TRADER, pos, state);
    }

    // --- Villager storage ---

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
        if (villager.isEmpty()) return null;
        if (villagerEntity == null && level != null) {
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
        updateTradeInv();
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

    // --- Workstation ---

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
        updateTradeInv();
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

    private void applyTradeLimits() {
        if (level == null || level.isClientSide()) return;
        SimpleVillagerEntity v = getVillagerEntity();
        if (v == null) return;
        MerchantOffers offers = v.getOffers();
        int maxUses = ModConfig.server().traderMaxUses;
        boolean infinite = ModConfig.server().autoTraderInfinite;
        for (MerchantOffer o : offers) {
            if (infinite) {
                o.resetUses();
            }
if (maxUses > 0) {
                ((com.simplevillager.mixin.MerchantOfferAccessor) o).SimpleVillager$setMaxUses(maxUses);
            }
        }
    }

    private void applyReputationDiscount() {
        SimpleVillagerEntity v = getVillagerEntity();
        if (v == null) return;
        GossipContainer gossips = v.getGossips();
        if (gossips == null || gossips.getGossipEntries().isEmpty()) return;
        int bestRep = 0;
        for (UUID uuid : gossips.getGossipEntries().keySet()) {
            bestRep = Math.max(bestRep, gossips.getReputation(uuid, type -> type != GossipType.TRADING));
        }
        if (bestRep <= 0) return;
        for (MerchantOffer offer : v.getOffers()) {
            offer.setSpecialPriceDiff(-Mth.floor(bestRep * offer.getPriceMultiplier()));
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

    private void fixProfession() {
        SimpleVillagerEntity v = getVillagerEntity();
        if (v == null || v.getVillagerXp() > 0 || v.getVillagerData().getProfession() == VillagerProfession.NITWIT) {
            return;
        }
        v.setVillagerData(v.getVillagerData().setProfession(getWorkstationProfession()));
    }

    public VillagerProfession getWorkstationProfession() {
        var poiTypeHolder = PoiTypes.forState(workstation.defaultBlockState());
        if (poiTypeHolder.isEmpty()) {
            return VillagerProfession.NONE;
        }
        var poiType = poiTypeHolder.get();
        for (VillagerProfession profession : BuiltInRegistries.VILLAGER_PROFESSION) {
            if (profession.heldJobSite().test(poiType)) {
                return profession;
            }
        }
        return VillagerProfession.NONE;
    }

    // --- Trade GUI ---

    public Container getTradeGuiInv() {
        updateTradeInv();
        return tradeGuiInv;
    }

    public Container getInputInventory() {
        return inputInventory;
    }

    public Container getOutputInventory() {
        return outputInventory;
    }

    public int getTradeIndex() {
        return tradeIndex;
    }

    public void setTradeIndex(int tradeIndex) {
        this.tradeIndex = tradeIndex;
        updateTradeInv();
        setChanged();
    }

    public void nextTrade() {
        int tradeCount = getTradeCount();
        if (tradeCount > 0) {
            setTradeIndex(Math.floorMod(this.tradeIndex + 1, tradeCount));
        }
    }

    public void prevTrade() {
        int tradeCount = getTradeCount();
        if (tradeCount > 0) {
            setTradeIndex(Math.floorMod(this.tradeIndex - 1, tradeCount));
        }
    }

    @Nullable
    public MerchantOffer getOffer() {
        SimpleVillagerEntity villagerEntity = getVillagerEntity();
        if (villagerEntity == null || villagerEntity.level().isClientSide()) {
            return null;
        }
        MerchantOffers offers = villagerEntity.getOffers();
        if (this.tradeIndex < 0 || this.tradeIndex >= offers.size()) {
            return null;
        }
        return offers.get(this.tradeIndex);
    }

    protected int getTradeCount() {
        Villager villagerEntity = getVillagerEntity();
        if (villagerEntity == null) {
            return 0;
        }
        return villagerEntity.getOffers().size();
    }

    protected void updateTradeInv() {
        if (level == null || level.isClientSide()) return;
        SimpleVillagerEntity villagerEntity = getVillagerEntity();
        if (villagerEntity == null) {
            tradeGuiInv.clearContent();
            return;
        }
        MerchantOffer offer = getOffer();
        if (offer == null) {
            tradeGuiInv.clearContent();
            return;
        }
tradeGuiInv.setItem(0, getBaseInputA());
        tradeGuiInv.setItem(1, offer.getCostB());
        tradeGuiInv.setItem(2, offer.getResult());
    }

    public ItemStack getBaseInputA() {
        MerchantOffer offer = getOffer();
        if (offer == null) return ItemStack.EMPTY;
        return offer.getBaseCostA().copy();
    }

    public int getDiscountedCostACount() {
        MerchantOffer offer = getOffer();
        return offer == null ? 0 : offer.getCostA().getCount();
    }

    public boolean isLocked() {
        if (ModConfig.server().autoTraderInfinite) return false;
        MerchantOffer offer = getOffer();
        return offer == null || offer.isOutOfStock();
    }

    // --- Auto-trading ---

    private boolean removeNeededItems(ItemCost cost, int amount) {
        if (amount <= 0) return true;
        int available = 0;
        for (int i = 0; i < inputInventory.getContainerSize(); i++) {
            ItemStack stack = inputInventory.getItem(i);
            if (!stack.isEmpty() && cost.test(stack)) {
                available += stack.getCount();
            }
        }
        if (available < amount) return false;
        int extracted = 0;
        for (int i = 0; i < inputInventory.getContainerSize(); i++) {
            ItemStack stack = inputInventory.getItem(i);
            if (!stack.isEmpty() && cost.test(stack)) {
                int toExtract = Math.min(amount - extracted, stack.getCount());
                stack.shrink(toExtract);
                extracted += toExtract;
                if (extracted >= amount) return true;
            }
        }
        return false;
    }

    private boolean insertItems(ItemStack insert) {
        if (insert.isEmpty()) return true;
        ItemStack remaining = insert.copy();
        for (int i = 0; i < outputInventory.getContainerSize(); i++) {
            ItemStack slot = outputInventory.getItem(i);
            if (slot.isEmpty()) {
                outputInventory.setItem(i, remaining.copy());
                return true;
            } else if (ItemStack.isSameItemSameComponents(slot, remaining)) {
                int space = slot.getMaxStackSize() - slot.getCount();
                int toAdd = Math.min(space, remaining.getCount());
                slot.grow(toAdd);
                remaining.shrink(toAdd);
                if (remaining.isEmpty()) return true;
            }
        }
        return remaining.isEmpty();
    }

    private void executeTrade() {
        MerchantOffer offer = getOffer();
        if (offer == null || offer.isOutOfStock()) return;

        if (!hasNeededItems(offer.getItemCostA(), offer.getCostA().getCount())) return;
        if (offer.getItemCostB().isPresent()) {
            if (!hasNeededItems(offer.getItemCostB().get(), offer.getCostB().getCount())) return;
        }

        removeNeededItems(offer.getItemCostA(), offer.getCostA().getCount());
        if (offer.getItemCostB().isPresent()) {
            removeNeededItems(offer.getItemCostB().get(), offer.getCostB().getCount());
        }
        if (!insertItems(offer.getResult())) return;

        Villager villager = getVillagerEntity();
        if (villager != null) {
            offer.increaseUses();
            if (ModConfig.server().autoTraderInfinite) {
                offer.resetUses();
            }
            offer.updateDemand();
            villager.setVillagerXp(villager.getVillagerXp() + offer.getXp());
            if (level instanceof ServerLevel serverLevel) {
                com.simplevillager.mixin.VillagerAccessor accessor = (com.simplevillager.mixin.VillagerAccessor) villager;
                if (accessor.SimpleVillager$callShouldIncreaseLevel()) {
                    accessor.SimpleVillager$callIncreaseMerchantCareer();
                }
            }
            saveVillagerEntity();
            updateTradeInv();
            setChanged();
            syncData();
        }
    }

    private boolean hasNeededItems(ItemCost cost, int amount) {
        if (amount <= 0) return true;
        int available = 0;
        for (int i = 0; i < inputInventory.getContainerSize(); i++) {
            ItemStack stack = inputInventory.getItem(i);
            if (!stack.isEmpty() && cost.test(stack)) {
                available += stack.getCount();
            }
        }
        return available >= amount;
    }

    // --- Server tick ---

    public static void tick(Level level, BlockPos pos, BlockState state, AutoTraderBlockEntity entity) {
        if (!(level instanceof ServerLevel)) return;
        entity.setChanged();
        if (!entity.hasVillager()) return;

        SimpleVillagerEntity v = entity.getVillagerEntity();
        if (v == null) return;

if (!v.isTrading()) {
            entity.applyTradeLimits();
            entity.applyReputationDiscount();
            // Auto-trade every 20 ticks
            if (level.getGameTime() % ModConfig.server().autoTraderSpeed == 0) {
                entity.executeTrade();
            }
            // Restock check
            if (!ModConfig.server().autoTraderInfinite) {
                boolean needRestock = entity.getTotalUses() >= ModConfig.server().traderRestockUses;
                if (!needRestock && level.getGameTime() - entity.getLastRestock() > entity.nextRestock
                        && v.getVillagerData().getProfession() == entity.getWorkstationProfession()) {
                    needRestock = true;
                }
                if (needRestock) {
                    entity.restock();
                    entity.nextRestock = entity.calculateNextRestock();
                }
            }
        }
    }

    protected long calculateNextRestock() {
        return ModConfig.server().traderRestockTime + level.getRandom().nextInt(Math.max(3600 - 1200, 1));
    }

    protected void restock() {
        try {
            SimpleVillagerEntity villagerEntity = getVillagerEntity();
            if (villagerEntity == null) return;
            villagerEntity.restock();
            SoundEvent workSound = villagerEntity.getVillagerData().getProfession().workSound();
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

    // --- Save/Load ---

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider provider) {
        super.saveAdditional(tag, provider);
        if (hasVillager()) {
            saveVillagerEntity();
            tag.put("Villager", getVillager().saveOptional(provider));
        }
        if (hasWorkstation()) {
            tag.putString("Workstation", BuiltInRegistries.BLOCK.getKey(workstation).toString());
        }
        tag.putLong("NextRestock", nextRestock);
        tag.putInt("Trade", tradeIndex);

        // Save input inventory
        CompoundTag inputChild = new CompoundTag();
        for (int i = 0; i < inputInventory.getContainerSize(); i++) {
            if (!inputInventory.getItem(i).isEmpty()) {
                inputChild.put("Slot_" + i, inputInventory.getItem(i).saveOptional(provider));
            }
        }
        tag.put("InputInventory", inputChild);

        // Save output inventory
        CompoundTag outputChild = new CompoundTag();
        for (int i = 0; i < outputInventory.getContainerSize(); i++) {
            if (!outputInventory.getItem(i).isEmpty()) {
                outputChild.put("Slot_" + i, outputInventory.getItem(i).saveOptional(provider));
            }
        }
        tag.put("OutputInventory", outputChild);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider provider) {
        if (tag.contains("Villager")) {
            villager = ItemStack.parseOptional(provider, tag.getCompound("Villager"));
            villagerEntity = null;
        } else {
            villager = ItemStack.EMPTY;
            villagerEntity = null;
        }
        if (tag.contains("Workstation")) {
            ResourceLocation id = ResourceLocation.tryParse(tag.getString("Workstation"));
            if (id != null) {
                workstation = BuiltInRegistries.BLOCK.getOptional(id).orElse(Blocks.AIR);
            } else {
                workstation = Blocks.AIR;
            }
        } else {
            workstation = Blocks.AIR;
        }
        nextRestock = tag.getLong("NextRestock");
        tradeIndex = tag.getInt("Trade");

        // Load input inventory
        CompoundTag inputChild = tag.getCompound("InputInventory");
        for (int i = 0; i < inputInventory.getContainerSize(); i++) {
            if (inputChild.contains("Slot_" + i)) {
                inputInventory.setItem(i, ItemStack.parseOptional(provider, inputChild.getCompound("Slot_" + i)));
            }
        }

        // Load output inventory
        CompoundTag outputChild = tag.getCompound("OutputInventory");
        for (int i = 0; i < outputInventory.getContainerSize(); i++) {
            if (outputChild.contains("Slot_" + i)) {
                outputInventory.setItem(i, ItemStack.parseOptional(provider, outputChild.getCompound("Slot_" + i)));
            }
        }

        super.loadAdditional(tag, provider);
    }

    @Override
    public void setRemoved() {
        removeTradingPlayer();
        super.setRemoved();
    }

    // --- Container implementation ---

    @Override
    public int getContainerSize() {
        return inputInventory.getContainerSize() + outputInventory.getContainerSize();
    }

    @Override
    public boolean isEmpty() {
        for (int i = 0; i < inputInventory.getContainerSize(); i++) {
            if (!inputInventory.getItem(i).isEmpty()) return false;
        }
        for (int i = 0; i < outputInventory.getContainerSize(); i++) {
            if (!outputInventory.getItem(i).isEmpty()) return false;
        }
        return true;
    }

    @Override
    public ItemStack getItem(int slot) {
        if (slot < inputInventory.getContainerSize()) {
            return inputInventory.getItem(slot);
        }
        return outputInventory.getItem(slot - inputInventory.getContainerSize());
    }

    @Override
    public ItemStack removeItem(int slot, int amount) {
        if (slot < inputInventory.getContainerSize()) {
            return inputInventory.removeItem(slot, amount);
        }
        return outputInventory.removeItem(slot - inputInventory.getContainerSize(), amount);
    }

    @Override
    public ItemStack removeItemNoUpdate(int slot) {
        if (slot < inputInventory.getContainerSize()) {
            return inputInventory.removeItem(slot, inputInventory.getItem(slot).getCount());
        }
        int adjusted = slot - inputInventory.getContainerSize();
        return outputInventory.removeItem(adjusted, outputInventory.getItem(adjusted).getCount());
    }

    @Override
    public void setItem(int slot, ItemStack stack) {
        if (slot < inputInventory.getContainerSize()) {
            inputInventory.setItem(slot, stack);
        } else {
            outputInventory.setItem(slot - inputInventory.getContainerSize(), stack);
        }
    }

    @Override
    public void setChanged() {
        super.setChanged();
    }

    @Override
    public boolean canPlaceItem(int slot, ItemStack stack) {
        return true;
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
    public int[] getSlotsForFace(Direction direction) {
        if (direction == Direction.UP) {
            return INPUT_SLOTS;
        } else if (direction == Direction.DOWN) {
            return OUTPUT_SLOTS;
        }
        return new int[0];
    }

    @Override
    public boolean canPlaceItemThroughFace(int slot, ItemStack stack, Direction direction) {
        return direction == Direction.UP;
    }

    @Override
    public boolean canTakeItemThroughFace(int slot, ItemStack stack, Direction direction) {
        return direction == Direction.DOWN;
    }
}
