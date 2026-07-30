package com.simplevillager.items;

import com.simplevillager.SimpleVillagerMod;
import com.simplevillager.blocks.ModBlocks;
import com.simplevillager.datacomponent.VillagerData;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;

public class ModItems {

    private static ResourceKey<Item> itemKey(String name) {
        return ResourceKey.create(Registries.ITEM, Identifier.fromNamespaceAndPath(SimpleVillagerMod.MOD_ID, name));
    }

    private static Item.Properties itemProps(String name) {
        Item.Properties p = new Item.Properties();
        p = p.setId(itemKey(name));
        return p;
    }

    public static final DataComponentType<VillagerData> VILLAGER_DATA = registerComponent("villager",
            DataComponentType.<VillagerData>builder()
                    .persistent(VillagerData.CODEC)
                    .networkSynchronized(VillagerData.STREAM_CODEC)
                    .build()
    );

    public static final VillagerItem VILLAGER = registerItem("villager",
            new VillagerItem(itemProps("villager"))
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
        ResourceKey<Item> key = itemKey(name);
        return net.minecraft.core.Registry.register(
                BuiltInRegistries.ITEM,
                key,
                item
        );
    }

    private static BlockItem registerBlockItem(String name, net.minecraft.world.level.block.Block block) {
        ResourceKey<Item> key = itemKey(name);
        Item.Properties p = new Item.Properties();
        p.setId(key);
        p.useBlockDescriptionPrefix();
        return net.minecraft.core.Registry.register(
                BuiltInRegistries.ITEM,
                key,
                new BlockItem(block, p)
        );
    }

    private static <T> DataComponentType<T> registerComponent(String name, DataComponentType<T> component) {
        ResourceKey<DataComponentType<?>> key = (ResourceKey<DataComponentType<?>>) (ResourceKey<?>) ResourceKey.create(
                Registries.DATA_COMPONENT_TYPE,
                Identifier.fromNamespaceAndPath(SimpleVillagerMod.MOD_ID, name)
        );
        return (DataComponentType<T>) net.minecraft.core.Registry.register(
                BuiltInRegistries.DATA_COMPONENT_TYPE,
                key,
                component
        );
    }

    public static void initialize() {
        // Trigger static loading
    }
}
