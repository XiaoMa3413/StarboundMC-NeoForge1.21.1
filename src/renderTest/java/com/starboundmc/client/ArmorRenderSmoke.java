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
    private static boolean finished, reloaded;
    private static CompletableFuture<?> work, languageLoad;
    private static ArmorProfile profile() { return ArmorProfile.values()[(stage % 16) / 4]; }
    private static ArmorItem.Type type() { return ModArmor.TYPES.get(stage % 4); }
    private static boolean enabled() { return Boolean.getBoolean("starboundmc.debug.armorSmoke"); }

    @SubscribeEvent public static void overlay(RenderGuiEvent.Post event) {
        var mc = Minecraft.getInstance();
        if (!enabled() || finished || mc.player == null || equippedAt == 0) return;
        var g = event.getGuiGraphics();
        g.fill(0, 0, g.guiWidth(), g.guiHeight(), 0xff17212c);
        g.drawString(mc.font, (stage < 16 ? "FULL SET / " : "SINGLE PIECE / ") + profile().id() + " / " + type().getName(), 24, 20, 0xffe1edf2);
        renderView(g, 130, 240, 125, 0);
        renderView(g, 320, 240, 125, 90);
        renderView(g, 510, 240, 125, 180);
        g.drawString(mc.font, "FRONT", 105, 65, 0xff85a5b0);
        g.drawString(mc.font, "SIDE", 295, 65, 0xff85a5b0);
        g.drawString(mc.font, "BACK", 485, 65, 0xff85a5b0);
        for (int i = 0; i < 4; i++) g.renderItem(new ItemStack(ModArmor.item(profile(), ModArmor.TYPES.get(i))), 640 + i * 35, 65);
        g.renderTooltip(mc.font, new ItemStack(ModArmor.item(profile(), type())), 640, 110);
        g.drawString(mc.font, "Armor=" + mc.player.getArmorValue() + "  Health=" + mc.player.getMaxHealth()
                + "  Toughness=" + mc.player.getAttributeValue(Attributes.ARMOR_TOUGHNESS), 620, 430, 0xffa2bdc9);
    }
    private static void renderView(net.minecraft.client.gui.GuiGraphics g, float x, float y, int scale, float angle) {
        var player = Minecraft.getInstance().player;
        float body = player.yBodyRot, yaw = player.getYRot(), pitch = player.getXRot();
        float head = player.yHeadRot, oldHead = player.yHeadRotO;
        player.yBodyRot = player.yHeadRot = player.yHeadRotO = 180 + angle;
        player.setYRot(180 + angle); player.setXRot(0);
        InventoryScreen.renderEntityInInventory(g, x, y, scale,
                new org.joml.Vector3f(0, player.getBbHeight() / 2, 0),
                new org.joml.Quaternionf().rotateZ((float)Math.PI), new org.joml.Quaternionf(), player);
        player.yBodyRot = body; player.yHeadRot = head; player.yHeadRotO = oldHead;
        player.setYRot(yaw); player.setXRot(pitch);
    }
    @SubscribeEvent public static void frame(RenderFrameEvent.Post event) throws Exception {
        if (!enabled() || finished) return;
        var mc = Minecraft.getInstance();
        var output = mc.gameDirectory.toPath().resolve("screenshots/armor-v04-game");
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
        if (stage == 8 && !reloaded) {
            if (languageLoad == null) languageLoad = mc.reloadResourcePacks();
            if (!languageLoad.isDone()) return;
            languageLoad.join(); reloaded = true; languageLoad = null;
        }
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
            boolean withEpp = type() == ArmorItem.Type.BOOTS;
            mc.options.keyShift.setDown(stage < 16 && type() == ArmorItem.Type.LEGGINGS);
            work = mc.getSingleplayerServer().submit(() -> {
                var player = mc.getSingleplayerServer().getPlayerList().getPlayers().getFirst();
                for (var part : ModArmor.TYPES) player.setItemSlot(part.getSlot(),
                        stage < 16 || part == type() ? new ItemStack(ModArmor.item(expected, part)) : ItemStack.EMPTY);
                player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, ItemStack.EMPTY);
                player.setData(com.starboundmc.story.ModAttachments.EPP_EQUIPMENT, withEpp
                        ? new ItemStack(com.starboundmc.item.ModItems.EPP_MK1.get()) : ItemStack.EMPTY);
                player.inventoryMenu.broadcastChanges();
            });
            equippedAt = System.nanoTime(); return;
        }
        if (!work.isDone() || (System.nanoTime() - equippedAt) / 1e9 < 2) return;
        work.join();
        for (var part : ModArmor.TYPES) {
            var stack = mc.player.getItemBySlot(part.getSlot());
            if (stage < 16 || part == type()) { if (!stack.is(ModArmor.item(profile(), part))) return; }
            else if (!stack.isEmpty()) return;
        }
        if (stage < 16 && mc.player.getArmorValue() != profile().helmet + profile().chestplate + profile().leggings + profile().boots)
            throw new IllegalStateException("Native equipment attributes did not reach client");
        double expectedHealth = switch (profile()) { case PIONEER, VANGUARD -> 24; case RANGER -> 22; case STARWARDEN -> 30; };
        if (stage < 16 && mc.player.getMaxHealth() != expectedHealth) throw new IllegalStateException("Set health not synchronized");
        if (com.starboundmc.client.armor.EquipmentArmorModels.renderedSelections == 0)
            throw new IllegalStateException("Custom armor models were never selected");
        var id = profile().id() + "_" + type().getName() + (stage < 16 ? "_set" : "_single");
        try (var image = Screenshot.takeScreenshot(mc.getMainRenderTarget())) { image.writeToFile(output.resolve(id + ".png")); }
        Files.writeString(output.resolve(id + ".txt"), "armor=" + mc.player.getArmorValue() + " maxHealth=" + mc.player.getMaxHealth()
                + " toughness=" + mc.player.getAttributeValue(Attributes.ARMOR_TOUGHNESS));
        if (++stage == 32) {
            Files.writeString(output.resolve("complete.txt"), "All 16 items separately and four complete sets, crouch, EPP, resource reload, native attributes and localized tooltips rendered.\n");
            finished = true; mc.stop();
        } else { equippedAt = 0; languageLoad = null; }
    }
}
