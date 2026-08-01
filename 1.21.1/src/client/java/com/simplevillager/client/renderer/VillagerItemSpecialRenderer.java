package com.simplevillager.client.renderer;

import com.simplevillager.client.BedConfig;
import com.simplevillager.datacomponent.VillagerData;
import com.simplevillager.entity.SimpleVillagerEntity;
import com.simplevillager.items.ModItems;
import com.mojang.blaze3d.vertex.PoseStack;
import net.fabricmc.fabric.api.client.rendering.v1.BuiltinItemRendererRegistry;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.npc.VillagerProfession;
import net.minecraft.world.entity.npc.VillagerType;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

public class VillagerItemSpecialRenderer implements BuiltinItemRendererRegistry.DynamicItemRenderer {

    private SimpleVillagerEntity cachedVillager;

    @Override
    public void render(ItemStack stack, ItemDisplayContext mode, PoseStack poseStack, MultiBufferSource buffer, int light, int overlay) {
        Level level = Minecraft.getInstance().level;
        if (level == null) return;
        if (cachedVillager == null) {
            cachedVillager = new SimpleVillagerEntity(EntityType.VILLAGER, level);
            cachedVillager.setNoAi(true);
        }
        VillagerData data = stack.get(ModItems.VILLAGER_DATA);
        boolean baby = data != null && !data.isEmpty() && data.getNbt().getInt("Age") < 0;
        cachedVillager.setBaby(baby);
        if (data != null && !data.isEmpty()) {
            applyVillagerData(data.getNbt());
        }

        EntityRenderDispatcher dispatcher = Minecraft.getInstance().getEntityRenderDispatcher();
        var renderer = dispatcher.getRenderer(cachedVillager);
        if (renderer == null) return;

        poseStack.pushPose();
        if (mode == ItemDisplayContext.GROUND) {
            float s = (float) BedConfig.villagerGroundScale;
            poseStack.translate(0.0f, (float) BedConfig.villagerGroundY, 0.0f);
            poseStack.scale(s, s, s);
        } else {
            poseStack.translate(0.0f, 0.0f, 0.0f);
            float s = baby ? 1.15f : 1.0f;
            poseStack.scale(s, s, s);
        }
        renderer.render(cachedVillager, 0f, 0f, poseStack, buffer, light);
        poseStack.popPose();
    }

    private void applyVillagerData(CompoundTag nbt) {
        try {
            CompoundTag vd = nbt.getCompound("VillagerData");
            VillagerProfession profession = VillagerProfession.NONE;
            String profStr = vd.getString("profession");
            if (!profStr.isEmpty()) {
                ResourceLocation id = ResourceLocation.tryParse(profStr);
                if (id != null) {
                    VillagerProfession p = BuiltInRegistries.VILLAGER_PROFESSION.get(id);
                    if (p != null) profession = p;
                }
            }
            VillagerType type = VillagerType.PLAINS;
            String typeStr = vd.getString("type");
            if (!typeStr.isEmpty()) {
                ResourceLocation id = ResourceLocation.tryParse(typeStr);
                if (id != null) {
                    VillagerType t = BuiltInRegistries.VILLAGER_TYPE.get(id);
                    if (t != null) type = t;
                }
            }
            int level = vd.getInt("level");
            cachedVillager.setVillagerData(new net.minecraft.world.entity.npc.VillagerData(type, profession, level));
        } catch (Exception ignored) {
        }
    }
}
