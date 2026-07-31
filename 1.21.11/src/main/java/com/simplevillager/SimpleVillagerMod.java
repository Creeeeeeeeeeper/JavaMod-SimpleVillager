package com.simplevillager;

import com.simplevillager.blockentity.ModBlockEntities;
import com.simplevillager.blocks.ModBlocks;
import com.simplevillager.events.VillagerPickup;
import com.simplevillager.gui.AutoTraderContainer;
import com.simplevillager.gui.ModMenus;
import com.simplevillager.items.ModItems;
import com.simplevillager.config.ModConfig;
import com.simplevillager.loottable.CopyBlockEntityData;
import com.simplevillager.mixin.MerchantMenuAccessor;
import com.simplevillager.net.CycleTradesC2SPacket;
import com.mojang.brigadier.CommandDispatcher;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import com.simplevillager.net.PickUpVillagerC2SPacket;
import com.simplevillager.net.SelectTradeC2SPacket;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.fabricmc.fabric.api.event.player.UseEntityCallback;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MerchantMenu;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.trading.Merchant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SimpleVillagerMod implements ModInitializer {
    public static final String MOD_ID = "simplevillager";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    @Override
    public void onInitialize() {
        ModConfig.init();
        registerCommands();
        ModBlocks.initialize();
        ModItems.initialize();
        ModBlockEntities.initialize();
        ModMenus.initialize();
        ModCreativeTabs.initialize();
        registerLootFunctions();

        PayloadTypeRegistry.playC2S().register(PickUpVillagerC2SPacket.TYPE, PickUpVillagerC2SPacket.STREAM_CODEC);
        PayloadTypeRegistry.playC2S().register(SelectTradeC2SPacket.TYPE, SelectTradeC2SPacket.STREAM_CODEC);
        PayloadTypeRegistry.playC2S().register(CycleTradesC2SPacket.TYPE, CycleTradesC2SPacket.STREAM_CODEC);

        ServerPlayNetworking.registerGlobalReceiver(PickUpVillagerC2SPacket.TYPE, (packet, context) -> {
            ServerPlayer player = context.player();
            if (player == null) return;
            context.server().execute(() -> {
                player.level().getEntitiesOfClass(Villager.class, player.getBoundingBox().inflate(8.0), v ->
                        v.getUUID().equals(packet.villagerUUID())
                ).stream().filter(VillagerPickup::arePickupConditionsMet).findAny().ifPresent(villager -> {
                    VillagerPickup.pickUp(villager, player);
                });
            });
        });

        ServerPlayNetworking.registerGlobalReceiver(SelectTradeC2SPacket.TYPE, (packet, context) -> {
            SelectTradeC2SPacket selectTradePacket = (SelectTradeC2SPacket) packet;
            ServerPlayer player = context.player();
            if (player == null) return;
            context.server().execute(() -> {
                AbstractContainerMenu menu = player.containerMenu;
                if (menu instanceof AutoTraderContainer autoTraderMenu) {
                    if (autoTraderMenu.getTrader() != null) {
                        if (selectTradePacket.next()) {
                            autoTraderMenu.getTrader().nextTrade();
                        } else {
                            autoTraderMenu.getTrader().prevTrade();
                        }
                    }
                }
            });
        });

        ServerPlayNetworking.registerGlobalReceiver(CycleTradesC2SPacket.TYPE, (packet, context) -> {
            ServerPlayer player = context.player();
            if (player == null) return;
            if (!ModConfig.server().tradeCycling) return;
            context.server().execute(() -> {
                cycleTrades(player);
            });
        });

        UseEntityCallback.EVENT.register((player, world, hand, entity, hitResult) -> {
            if (world.isClientSide()) return InteractionResult.PASS;
            if (!ModConfig.client().sneakPickup) return InteractionResult.PASS;
            if (!(entity instanceof Villager villager)) return InteractionResult.PASS;
            if (!player.isShiftKeyDown()) return InteractionResult.PASS;
            if (!VillagerPickup.arePickupConditionsMet(villager)) return InteractionResult.PASS;
            VillagerPickup.pickUp(villager, player);
            return InteractionResult.SUCCESS;
        });

        LOGGER.info("SimpleVillager mod initialized!");
    }

    private static void cycleTrades(ServerPlayer player) {
        if (!(player.containerMenu instanceof MerchantMenu menu)) return;
        Merchant merchant = ((MerchantMenuAccessor) menu).SimpleVillager$getTrader();
        if (merchant == null || !(merchant instanceof Villager villager)) return;
        if (menu.getTraderXp() > 0) {
            var tradeContainer = ((MerchantMenuAccessor) menu).SimpleVillager$getTradeContainer();
            if (tradeContainer.getActiveOffer() != null) return;
        }

        villager.setOffers(null);
        villager.getOffers();

        player.sendMerchantOffers(
                player.containerMenu.containerId,
                villager.getOffers(),
                villager.getVillagerData().level(),
                villager.getVillagerXp(),
                merchant.showProgressBar(),
                merchant.canRestock()
        );
    }

    private void registerCommands() {
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
            dispatcher.register(Commands.literal("sv")
                    .then(Commands.literal("reload")
                            .executes(context -> {
                                ModConfig.reload();
                                context.getSource().sendSuccess(() -> Component.literal("§aSimpleVillager config reloaded"), false);
                                return 1;
                            })
                    ));
        });
    }

    private void registerLootFunctions() {
        Registry.register(
                BuiltInRegistries.LOOT_FUNCTION_TYPE,
                Identifier.fromNamespaceAndPath(MOD_ID, "copy_block_entity"),
                CopyBlockEntityData.TYPE
        );
    }
}
