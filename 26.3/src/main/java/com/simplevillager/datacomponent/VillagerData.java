package com.simplevillager.datacomponent;

import com.simplevillager.entity.SimpleVillagerEntity;
import com.mojang.serialization.Codec;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

public class VillagerData {
    public static final Codec<VillagerData> CODEC = CompoundTag.CODEC.xmap(VillagerData::of, data -> data.nbt);
    public static final StreamCodec<RegistryFriendlyByteBuf, VillagerData> STREAM_CODEC = new StreamCodec<>() {
        public VillagerData decode(RegistryFriendlyByteBuf buf) {
            return new VillagerData(buf.readNbt());
        }

        public void encode(RegistryFriendlyByteBuf buf, VillagerData villager) {
            buf.writeNbt(villager.nbt);
        }
    };

    private final CompoundTag nbt;

    private VillagerData(CompoundTag nbt) {
        this.nbt = nbt.copy();
    }

    public static VillagerData of(CompoundTag nbt) {
        return new VillagerData(nbt);
    }

    public static VillagerData of(Villager villager) {
        return new VillagerData(SimpleVillagerEntity.saveVillager(villager));
    }

    public static SimpleVillagerEntity createSimpleVillager(ItemStack stack, Level level) {
        VillagerData data = stack.get(com.simplevillager.items.ModItems.VILLAGER_DATA);
        if (data == null || data.isEmpty()) return null;
        SimpleVillagerEntity entity = SimpleVillagerEntity.fromTag(level, data.getNbt());
        if (stack.getCustomName() != null) {
            entity.setCustomName(stack.getCustomName());
        }
        return entity;
    }

    public static void applyToItem(ItemStack stack, Villager villager) {
        if (stack.isEmpty()) return;
        stack.set(com.simplevillager.items.ModItems.VILLAGER_DATA, of(villager));
        if (villager.hasCustomName()) {
            stack.set(DataComponents.CUSTOM_NAME, villager.getCustomName());
        }
    }

    public CompoundTag getNbt() {
        return nbt.copy();
    }

    public boolean isEmpty() {
        return nbt.isEmpty();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        VillagerData that = (VillagerData) o;
        return nbt.equals(that.nbt);
    }

    @Override
    public int hashCode() {
        return nbt.hashCode();
    }
}
