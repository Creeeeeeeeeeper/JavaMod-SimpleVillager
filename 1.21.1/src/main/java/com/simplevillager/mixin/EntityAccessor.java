package com.simplevillager.mixin;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(Entity.class)
public interface EntityAccessor {
    @Accessor("id")
    int SimpleVillager$getId();

    @Accessor("id")
    void SimpleVillager$setId(int id);

    @Invoker("addAdditionalSaveData")
    void SimpleVillager$callAddAdditionalSaveData(CompoundTag tag);

    @Invoker("readAdditionalSaveData")
    void SimpleVillager$callReadAdditionalSaveData(CompoundTag tag);
}
