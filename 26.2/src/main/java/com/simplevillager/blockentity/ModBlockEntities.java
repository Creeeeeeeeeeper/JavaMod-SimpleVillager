package com.simplevillager.blockentity;

import com.simplevillager.SimpleVillagerMod;
import com.simplevillager.blocks.ModBlocks;
import net.fabricmc.fabric.api.object.builder.v1.block.entity.FabricBlockEntityTypeBuilder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.entity.BlockEntityType;

public class ModBlockEntities {
    public static final BlockEntityType<TraderBlockEntity> TRADER = register("trader",
            FabricBlockEntityTypeBuilder.create(TraderBlockEntity::new, ModBlocks.TRADER).build());

    public static final BlockEntityType<AutoTraderBlockEntity> AUTO_TRADER = register("auto_trader",
            FabricBlockEntityTypeBuilder.create(AutoTraderBlockEntity::new, ModBlocks.AUTO_TRADER).build());

    public static final BlockEntityType<FarmerBlockEntity> FARMER = register("farmer",
            FabricBlockEntityTypeBuilder.create(FarmerBlockEntity::new, ModBlocks.FARMER).build());

    public static final BlockEntityType<BreederBlockEntity> BREEDER = register("breeder",
            FabricBlockEntityTypeBuilder.create(BreederBlockEntity::new, ModBlocks.BREEDER).build());

    public static final BlockEntityType<ConverterBlockEntity> CONVERTER = register("converter",
            FabricBlockEntityTypeBuilder.create(ConverterBlockEntity::new, ModBlocks.CONVERTER).build());

    public static final BlockEntityType<IronFarmBlockEntity> IRON_FARM = register("iron_farm",
            FabricBlockEntityTypeBuilder.create(IronFarmBlockEntity::new, ModBlocks.IRON_FARM).build());

    public static final BlockEntityType<IncubatorBlockEntity> INCUBATOR = register("incubator",
            FabricBlockEntityTypeBuilder.create(IncubatorBlockEntity::new, ModBlocks.INCUBATOR).build());

    public static final BlockEntityType<InventoryViewerBlockEntity> INVENTORY_VIEWER = register("inventory_viewer",
            FabricBlockEntityTypeBuilder.create(InventoryViewerBlockEntity::new, ModBlocks.INVENTORY_VIEWER).build());

    private static <T extends BlockEntityType<?>> T register(String name, T type) {
        return Registry.register(
                BuiltInRegistries.BLOCK_ENTITY_TYPE,
                Identifier.fromNamespaceAndPath(SimpleVillagerMod.MOD_ID, name),
                type
        );
    }

    public static void initialize() {
        // Trigger static loading
    }
}
