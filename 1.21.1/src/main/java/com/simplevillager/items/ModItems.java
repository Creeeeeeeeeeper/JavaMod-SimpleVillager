package com.simplevillager.items;

import com.simplevillager.SimpleVillagerMod;
import com.simplevillager.blocks.ModBlocks;
import com.simplevillager.datacomponent.VillagerData;
import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;

public class ModItems {

    private static ResourceLocation itemId(String name) {
        return ResourceLocation.fromNamespaceAndPath(SimpleVillagerMod.MOD_ID, name);
    }

    private static Item.Properties itemProps() {
        return new Item.Properties();
    }

    public static final DataComponentType<VillagerData> VILLAGER_DATA = registerComponent("villager",
            DataComponentType.<VillagerData>builder()
                    .persistent(VillagerData.CODEC)
                    .networkSynchronized(VillagerData.STREAM_CODEC)
                    .build()
    );

    public static final VillagerItem VILLAGER = registerItem("villager",
            new VillagerItem(itemProps())
    );

    public static final BlockItem TRADER = registerBlockItem("trader", ModBlocks.TRADER);
    public static final BlockItem AUTO_TRADER = registerBlockItem("auto_trader", ModBlocks.AUTO_TRADER);
    public static final BlockItem FARMER = registerBlockItem("farmer", ModBlocks.FARMER);
    public static final BlockItem BREEDER = registerBlockItem("breeder", ModBlocks.BREEDER);
    public static final BlockItem CONVERTER = registerBlockItem("converter", ModBlocks.CONVERTER);
    public static final BlockItem IRON_FARM = registerBlockItem("iron_farm", ModBlocks.IRON_FARM);
    public static final BlockItem INCUBATOR = registerBlockItem("incubator", ModBlocks.INCUBATOR);
    public static final BlockItem INVENTORY_VIEWER = registerBlockItem("inventory_viewer", ModBlocks.INVENTORY_VIEWER);

    private static <T extends Item> T registerItem(String name, T item) {
        return Registry.register(
                BuiltInRegistries.ITEM,
                itemId(name),
                item
        );
    }

    private static BlockItem registerBlockItem(String name, net.minecraft.world.level.block.Block block) {
        return Registry.register(
                BuiltInRegistries.ITEM,
                itemId(name),
                new BlockItem(block, new Item.Properties())
        );
    }

    private static <T> DataComponentType<T> registerComponent(String name, DataComponentType<T> component) {
        return Registry.register(
                BuiltInRegistries.DATA_COMPONENT_TYPE,
                itemId(name),
                component
        );
    }

    public static void initialize() {
        // Trigger static loading
    }
}
