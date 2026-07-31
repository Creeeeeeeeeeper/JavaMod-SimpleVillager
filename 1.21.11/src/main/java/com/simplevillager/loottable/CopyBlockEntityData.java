package com.simplevillager.loottable;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.simplevillager.util.NbtHelper;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.storage.TagValueOutput;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.functions.LootItemConditionalFunction;
import net.minecraft.world.level.storage.loot.functions.LootItemFunctionType;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;

import java.util.List;

public class CopyBlockEntityData extends LootItemConditionalFunction {

    public static final MapCodec<CopyBlockEntityData> CODEC = RecordCodecBuilder.mapCodec(instance ->
            commonFields(instance).apply(instance, CopyBlockEntityData::new)
    );

    public static final LootItemFunctionType<CopyBlockEntityData> TYPE = new LootItemFunctionType<>(CODEC);

    protected CopyBlockEntityData(List<LootItemCondition> conditions) {
        super(conditions);
    }

    @Override
    protected ItemStack run(ItemStack stack, LootContext context) {
        BlockEntity blockEntity = context.getOptionalParameter(LootContextParams.BLOCK_ENTITY);
        if (blockEntity == null) {
            return stack;
        }
        TagValueOutput valueOutput = NbtHelper.createValueOutput(context.getLevel().registryAccess());
        blockEntity.saveWithFullMetadata(valueOutput);
        BlockItem.setBlockEntityData(stack, blockEntity.getType(), valueOutput);
        stack.applyComponents(blockEntity.collectComponents());
        return stack;
    }

    @Override
    public LootItemFunctionType<? extends LootItemConditionalFunction> getType() {
        return TYPE;
    }
}
