package com.starboundmc.client;

import com.starboundmc.armor.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.Difficulty;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.*;
import net.minecraft.world.level.*;
import net.minecraft.world.level.levelgen.WorldOptions;
import net.minecraft.world.level.levelgen.presets.WorldPresets;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.*;
import java.nio.file.*;
import java.util.concurrent.CompletableFuture;

/** Opt-in native equipment / resource / tooltip rendering evidence; not a production screen. */
@EventBusSubscriber(modid = "starboundmc", value = Dist.CLIENT)
public final class ArmorRenderSmoke {
    private static long start, equippedAt;
    private static int stage;
    private static boolean finished;
    private static CompletableFuture<?> work, languageLoad;
    private static ArmorProfile profile() { return ArmorProfile.values()[stage / 4]; }
    private static ArmorItem.Type type() { return ModArmor.TYPES.get(stage % 4); }
    private static boolean enabled() { return Boolean.getBoolean("starboundmc.debug.armorSmoke"); }

    @SubscribeEvent public static void overlay(RenderGuiEvent.Post event) {
        var mc = Minecraft.getInstance();
        if (!enabled() || finished || mc.player == null || equippedAt == 0) return;
        var g = event.getGuiGraphics();
        g.fill(0, 0, g.guiWidth(), g.guiHeight(), 0xff17212c);
        g.drawString(mc.font, "ARMOR VALIDATION / " + profile().id() + " / " + type().getName(), 24, 20, 0xffe1edf2);
        InventoryScreen.renderEntityInInventoryFollowsAngle(g, 30, 55, 270, 400, 140, 0, -.5f, 0, mc.player);
        for (int i = 0; i < 4; i++) g.renderItem(new ItemStack(ModArmor.item(profile(), ModArmor.TYPES.get(i))), 300 + i * 35, 65);
        g.renderTooltip(mc.font, new ItemStack(ModArmor.item(profile(), type())), 300, 110);
        g.drawString(mc.font, "Armor=" + mc.player.getArmorValue() + "  Health=" + mc.player.getMaxHealth()
                + "  Toughness=" + mc.player.getAttributeValue(Attributes.ARMOR_TOUGHNESS), 300, 380, 0xffa2bdc9);
    }
    @SubscribeEvent public static void frame(RenderFrameEvent.Post event) throws Exception {
        if (!enabled() || finished) return;
        var mc = Minecraft.getInstance();
        var output = mc.gameDirectory.toPath().resolve("screenshots/armor");
        if (start == 0) {
            if (mc.screen == null || mc.getOverlay() != null) return;
            if (languageLoad == null) {
                mc.getLanguageManager().setSelected("zh_cn"); mc.options.languageCode = "zh_cn";
                languageLoad = mc.reloadResourcePacks(); return;
            }
            if (!languageLoad.isDone()) return; languageLoad.join();
            start = System.nanoTime(); Files.createDirectories(output); Files.deleteIfExists(output.resolve("complete.txt"));
            mc.options.pauseOnLostFocus = false; mc.options.renderDistance().set(4); mc.options.guiScale().set(2);
            org.lwjgl.glfw.GLFW.glfwHideWindow(mc.getWindow().getWindow());
            var name = "armor-fixture-" + System.currentTimeMillis();
            mc.createWorldOpenFlows().createFreshLevel(name,
                    new LevelSettings(name, GameType.CREATIVE, false, Difficulty.PEACEFUL, true,
                            new GameRules(), WorldDataConfiguration.DEFAULT), new WorldOptions(3413, false, false),
                    r -> r.registryOrThrow(Registries.WORLD_PRESET).getHolderOrThrow(WorldPresets.FLAT).value().createWorldDimensions(), mc.screen);
            return;
        }
        if ((System.nanoTime() - start) / 1e9 > 240) throw new IllegalStateException("Armor smoke timeout " + stage);
        if (mc.player == null || mc.level == null || mc.screen != null || mc.getOverlay() != null) return;
        if (equippedAt == 0) {
            for (var part : ModArmor.TYPES) {
                var stack = new ItemStack(ModArmor.item(profile(), part));
                if (mc.getItemRenderer().getModel(stack, mc.level, mc.player, 0) == mc.getModelManager().getMissingModel())
                    throw new IllegalStateException("Missing armor item model");
            }
            for (int layer = 1; layer <= 2; layer++) {
                var location = ResourceLocation.fromNamespaceAndPath("starboundmc", "textures/models/armor/" + profile().id() + "_layer_" + layer + ".png");
                if (mc.getResourceManager().getResource(location).isEmpty()) throw new IllegalStateException("Missing armor UV texture");
            }
            var expected = profile();
            work = mc.getSingleplayerServer().submit(() -> {
                var player = mc.getSingleplayerServer().getPlayerList().getPlayers().getFirst();
                for (var part : ModArmor.TYPES) player.setItemSlot(part.getSlot(), new ItemStack(ModArmor.item(expected, part)));
                player.inventoryMenu.broadcastChanges();
            });
            equippedAt = System.nanoTime(); return;
        }
        if (!work.isDone() || (System.nanoTime() - equippedAt) / 1e9 < 2) return;
        work.join();
        for (var part : ModArmor.TYPES) if (!mc.player.getItemBySlot(part.getSlot()).is(ModArmor.item(profile(), part))) return;
        if (mc.player.getArmorValue() != profile().helmet + profile().chestplate + profile().leggings + profile().boots)
            throw new IllegalStateException("Native equipment attributes did not reach client");
        double expectedHealth = switch (profile()) { case PIONEER, VANGUARD -> 24; case RANGER -> 22; case STARWARDEN -> 30; };
        if (mc.player.getMaxHealth() != expectedHealth) throw new IllegalStateException("Set health not synchronized");
        var id = profile().id() + "_" + type().getName();
        try (var image = Screenshot.takeScreenshot(mc.getMainRenderTarget())) { image.writeToFile(output.resolve(id + ".png")); }
        Files.writeString(output.resolve(id + ".txt"), "armor=" + mc.player.getArmorValue() + " maxHealth=" + mc.player.getMaxHealth()
                + " toughness=" + mc.player.getAttributeValue(Attributes.ARMOR_TOUGHNESS));
        if (++stage == 16) {
            Files.writeString(output.resolve("complete.txt"), "All 16 items, four equipped sets, native attributes and localized tooltips rendered.\n");
            finished = true; mc.stop();
        } else equippedAt = 0;
    }
}
