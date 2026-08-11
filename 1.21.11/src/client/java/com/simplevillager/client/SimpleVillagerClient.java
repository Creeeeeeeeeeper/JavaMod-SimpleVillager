package com.simplevillager.client;

import com.simplevillager.SimpleVillagerMod;
import com.simplevillager.client.gui.AutoTraderScreen;
import com.simplevillager.client.gui.OutputScreen;
import com.simplevillager.client.gui.BreederScreen;
import com.simplevillager.client.gui.ConverterScreen;
import com.simplevillager.client.gui.IncubatorScreen;
import com.simplevillager.client.gui.InventoryViewerScreen;
import com.simplevillager.client.renderer.SimpleVillagerBER;
import com.simplevillager.client.renderer.TraderBER;
import com.simplevillager.client.renderer.BreederBER;
import com.simplevillager.client.renderer.ConverterBER;
import com.simplevillager.client.renderer.IronFarmBER;
import com.simplevillager.client.renderer.FarmerBER;
import com.simplevillager.blockentity.ModBlockEntities;
import com.simplevillager.blocks.ModBlocks;
import com.simplevillager.gui.ModMenus;
import com.simplevillager.net.CycleTradesC2SPacket;
import com.simplevillager.net.PickUpVillagerC2SPacket;
import com.simplevillager.net.SelectTradeC2SPacket;
import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.BlockRenderLayerMap;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.renderer.chunk.ChunkSectionLayer;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.phys.EntityHitResult;
import org.lwjgl.glfw.GLFW;

import static net.fabricmc.fabric.api.client.command.v2.ClientCommandManager.literal;

public class SimpleVillagerClient implements ClientModInitializer {

    public static KeyMapping.Category CATEGORY;
    public static KeyMapping PICK_UP_KEY;
    public static KeyMapping CYCLE_TRADES_KEY;

    @Override
    public void onInitializeClient() {
        CATEGORY = KeyMapping.Category.register(
                Identifier.fromNamespaceAndPath(SimpleVillagerMod.MOD_ID, "category")
        );

        PICK_UP_KEY = KeyBindingHelper.registerKeyBinding(new KeyMapping(
                "key.simplevillager.pick_up",
                InputConstants.Type.KEYSYM,
                GLFW.GLFW_KEY_V,
                CATEGORY
        ));

        CYCLE_TRADES_KEY = KeyBindingHelper.registerKeyBinding(new KeyMapping(
                "key.simplevillager.cycle_trades",
                InputConstants.Type.KEYSYM,
                GLFW.GLFW_KEY_C,
                CATEGORY
        ));

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (PICK_UP_KEY.consumeClick()) {
                handlePickUpKey(client);
            }
            while (CYCLE_TRADES_KEY.consumeClick()) {
                handleCycleTradesKey(client);
            }
        });

        BedConfig.load();
        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) ->
                dispatcher.register(literal("sv").then(literal("reload").executes(ctx -> {
                    BedConfig.load();
                    ctx.getSource().sendFeedback(Component.literal("test.toml reloaded"));
                    return 1;
                }))));

        MenuScreens.register(ModMenus.AUTO_TRADER, AutoTraderScreen::new);
        MenuScreens.register(ModMenus.OUTPUT, OutputScreen::new);
        MenuScreens.register(ModMenus.BREEDER, BreederScreen::new);
        MenuScreens.register(ModMenus.CONVERTER, ConverterScreen::new);
        MenuScreens.register(ModMenus.INCUBATOR, IncubatorScreen::new);
        MenuScreens.register(ModMenus.INVENTORY_VIEWER, InventoryViewerScreen::new);

        // Villager item special renderer
        com.simplevillager.client.mixin.SpecialModelRenderersAccessor.SimpleVillager$getIDMapper().put(
                com.simplevillager.client.renderer.VillagerItemSpecialRenderer.ID,
                com.simplevillager.client.renderer.VillagerItemSpecialRenderer.Unbaked.MAP_CODEC
        );

        // Block Entity Renderers
        registerBERs();

        // Render layer: glass panels must be cutout (render_type in model JSON is ignored since 1.21.5)
        BlockRenderLayerMap.putBlocks(ChunkSectionLayer.CUTOUT,
                ModBlocks.TRADER,
                ModBlocks.AUTO_TRADER,
                ModBlocks.FARMER,
                ModBlocks.BREEDER,
                ModBlocks.CONVERTER,
                ModBlocks.IRON_FARM,
                ModBlocks.INCUBATOR,
                ModBlocks.INVENTORY_VIEWER);
    }

    public static void sendSelectTrade(boolean next) {
        ClientPlayNetworking.send(new SelectTradeC2SPacket(next));
    }

    public static void sendCycleTrades() {
        ClientPlayNetworking.send(new CycleTradesC2SPacket());
    }

    private void handlePickUpKey(Minecraft client) {
        if (client.player == null || client.level == null) return;
        if (client.hitResult == null || client.hitResult.getType() != EntityHitResult.Type.ENTITY) return;
        if (!(client.hitResult instanceof EntityHitResult entityHit)) return;
        if (!(entityHit.getEntity() instanceof Villager villager)) return;
        if (!villager.isAlive() || villager.isSleeping()) return;
        ClientPlayNetworking.send(new PickUpVillagerC2SPacket(villager.getUUID()));
    }

    private void handleCycleTradesKey(Minecraft client) {
        if (client.player == null) return;
        if (!(client.screen instanceof net.minecraft.client.gui.screens.inventory.MerchantScreen)) return;
        ClientPlayNetworking.send(new CycleTradesC2SPacket());
    }

    public static boolean onCycleTradesKeyPressed(KeyEvent event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.options == null) return false;
        if (!CYCLE_TRADES_KEY.matches(event)) return false;
        if (mc.screen instanceof net.minecraft.client.gui.screens.inventory.MerchantScreen screen) {
            sendCycleTrades();
            mc.getSoundManager().play(
                    net.minecraft.client.resources.sounds.SimpleSoundInstance.forUI(
                            net.minecraft.sounds.SoundEvents.UI_BUTTON_CLICK, 1.0F));
            return true;
        }
        return false;
    }

    private void registerBERs() {
        // Scale 0.45, center position, no Y rotation
        var simpleBER = new SimpleVillagerBER(0.45f, 0, -0.25f, 0);
        net.minecraft.client.renderer.blockentity.BlockEntityRenderers.register(ModBlockEntities.TRADER, (ctx) -> new TraderBER(ctx));
        net.minecraft.client.renderer.blockentity.BlockEntityRenderers.register(ModBlockEntities.AUTO_TRADER, (ctx) -> new TraderBER(ctx));
        net.minecraft.client.renderer.blockentity.BlockEntityRenderers.register(ModBlockEntities.FARMER, (ctx) -> new FarmerBER(ctx));
        net.minecraft.client.renderer.blockentity.BlockEntityRenderers.register(ModBlockEntities.INCUBATOR, (ctx) -> new SimpleVillagerBER(0.45f, 0, 0, 0));
        net.minecraft.client.renderer.blockentity.BlockEntityRenderers.register(ModBlockEntities.INVENTORY_VIEWER, (ctx) -> simpleBER);

        // Breeder: 2 villagers
        net.minecraft.client.renderer.blockentity.BlockEntityRenderers.register(ModBlockEntities.BREEDER, (ctx) -> new BreederBER(ctx));

        // Converter: villager + zombie villager
        net.minecraft.client.renderer.blockentity.BlockEntityRenderers.register(ModBlockEntities.CONVERTER, (ctx) -> new ConverterBER());

        // Iron Farm: villager + zombie + iron golem
        net.minecraft.client.renderer.blockentity.BlockEntityRenderers.register(ModBlockEntities.IRON_FARM, (ctx) -> new IronFarmBER());
    }
}
