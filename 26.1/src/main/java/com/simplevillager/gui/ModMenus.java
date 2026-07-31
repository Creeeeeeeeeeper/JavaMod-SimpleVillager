package com.simplevillager.gui;

import com.simplevillager.SimpleVillagerMod;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;

public class ModMenus {
    public static final MenuType<AutoTraderContainer> AUTO_TRADER = register("auto_trader",
            new MenuType<>((windowId, inv) -> new AutoTraderContainer(windowId, inv), FeatureFlags.DEFAULT_FLAGS));
    public static final MenuType<OutputContainer> OUTPUT = register("output",
            new MenuType<>((windowId, inv) -> new OutputContainer(windowId, inv), FeatureFlags.DEFAULT_FLAGS));
    public static final MenuType<BreederContainer> BREEDER = register("breeder",
            new MenuType<>((windowId, inv) -> new BreederContainer(windowId, inv), FeatureFlags.DEFAULT_FLAGS));
    public static final MenuType<ConverterContainer> CONVERTER = register("converter",
            new MenuType<>((windowId, inv) -> new ConverterContainer(windowId, inv), FeatureFlags.DEFAULT_FLAGS));
    public static final MenuType<IncubatorContainer> INCUBATOR = register("incubator",
            new MenuType<>((windowId, inv) -> new IncubatorContainer(windowId, inv), FeatureFlags.DEFAULT_FLAGS));
    public static final MenuType<InventoryViewerContainer> INVENTORY_VIEWER = register("inventory_viewer",
            new MenuType<>((windowId, inv) -> new InventoryViewerContainer(windowId, inv), FeatureFlags.DEFAULT_FLAGS));

    private static <T extends AbstractContainerMenu> MenuType<T> register(String name, MenuType<T> type) {
        return Registry.register(
                BuiltInRegistries.MENU,
                Identifier.fromNamespaceAndPath(SimpleVillagerMod.MOD_ID, name),
                type
        );
    }

    public static void initialize() {
        // Trigger static loading
    }
}
