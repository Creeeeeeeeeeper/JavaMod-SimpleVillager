package com.simplevillager;

import com.simplevillager.blocks.ModBlocks;
import com.simplevillager.items.ModItems;
import com.simplevillager.items.VillagerItem;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;

public class ModCreativeTabs {

    private static final ResourceKey<CreativeModeTab> TAB_KEY = ResourceKey.create(
            Registries.CREATIVE_MODE_TAB,
            Identifier.fromNamespaceAndPath(SimpleVillagerMod.MOD_ID, SimpleVillagerMod.MOD_ID)
    );

    public static final CreativeModeTab TAB = CreativeModeTab.builder(CreativeModeTab.Row.TOP, 7)
            .title(Component.translatable("itemGroup.simplevillager"))
            .icon(() -> new ItemStack(ModItems.VILLAGER))
            .displayItems((params, output) -> {
                output.accept(VillagerItem.createDefaultVillager());
                output.accept(VillagerItem.createBabyVillager());
                output.accept(new ItemStack(ModItems.TRADER));
                output.accept(new ItemStack(ModItems.AUTO_TRADER));
                output.accept(new ItemStack(ModItems.FARMER));
                output.accept(new ItemStack(ModItems.BREEDER));
                output.accept(new ItemStack(ModItems.CONVERTER));
                output.accept(new ItemStack(ModItems.IRON_FARM));
                output.accept(new ItemStack(ModItems.INCUBATOR));
                output.accept(new ItemStack(ModItems.INVENTORY_VIEWER));
            })
            .build();

    public static void initialize() {
        net.minecraft.core.Registry.register(BuiltInRegistries.CREATIVE_MODE_TAB, TAB_KEY, TAB);
    }
}
