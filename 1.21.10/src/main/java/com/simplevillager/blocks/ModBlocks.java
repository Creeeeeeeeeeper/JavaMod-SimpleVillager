package com.simplevillager.blocks;

import com.simplevillager.SimpleVillagerMod;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.storage.loot.LootTable;

import java.util.Optional;

public class ModBlocks {

    private static ResourceKey<Block> key(String name) {
        return ResourceKey.create(Registries.BLOCK, ResourceLocation.fromNamespaceAndPath(SimpleVillagerMod.MOD_ID, name));
    }

    private static ResourceKey<LootTable> lootTable(String name) {
        return ResourceKey.create(Registries.LOOT_TABLE, ResourceLocation.fromNamespaceAndPath(SimpleVillagerMod.MOD_ID, "blocks/" + name));
    }

    private static BlockBehaviour.Properties props(String name) {
        BlockBehaviour.Properties p = BlockBehaviour.Properties.of();
        p = p.setId(key(name));
        p = p.overrideLootTable(Optional.of(lootTable(name)));
        return p;
    }

    public static final TraderBlock TRADER = register("trader", new TraderBlock(props("trader")));
    public static final AutoTraderBlock AUTO_TRADER = register("auto_trader", new AutoTraderBlock(props("auto_trader")));
    public static final FarmerBlock FARMER = register("farmer", new FarmerBlock(props("farmer")));
    public static final BreederBlock BREEDER = register("breeder", new BreederBlock(props("breeder")));
    public static final ConverterBlock CONVERTER = register("converter", new ConverterBlock(props("converter")));
    public static final IronFarmBlock IRON_FARM = register("iron_farm", new IronFarmBlock(props("iron_farm")));
    public static final IncubatorBlock INCUBATOR = register("incubator", new IncubatorBlock(props("incubator")));
    public static final InventoryViewerBlock INVENTORY_VIEWER = register("inventory_viewer", new InventoryViewerBlock(props("inventory_viewer")));

    private static <T extends Block> T register(String name, T block) {
        return net.minecraft.core.Registry.register(
                net.minecraft.core.registries.BuiltInRegistries.BLOCK,
                key(name),
                block
        );
    }

    public static void initialize() {
        // Trigger static loading
    }
}
