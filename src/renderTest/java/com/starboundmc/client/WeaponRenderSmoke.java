package com.starboundmc.client;

import com.starboundmc.StarboundMC;
import com.starboundmc.weapon.WeaponProfile;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.core.registries.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.Difficulty;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.*;
import net.minecraft.world.level.levelgen.WorldOptions;
import net.minecraft.world.level.levelgen.presets.WorldPresets;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderFrameEvent;
import net.neoforged.neoforge.client.event.RenderGuiEvent;
import java.nio.file.*;

/** Isolated visual evidence for every weapon, enabled only with -PweaponSmoke. */
@EventBusSubscriber(modid = StarboundMC.MODID, value = Dist.CLIENT)
public final class WeaponRenderSmoke {
    private static boolean started, finished;
    private static long start, selected;
    private static int stage;
    private static boolean reloaded;
    private static java.util.concurrent.CompletableFuture<Void> languageLoad;
    private static final String[] IDS = {"survival_knife", "tungsten_sabre", "coil_pistol", "durasteel_glaive",
            "plasma_carbine", "starcore_blade", "starcore_railgun"};
    private static boolean enabled() { return Boolean.getBoolean("starboundmc.debug.weaponSmoke"); }

    @SubscribeEvent
    public static void overlay(RenderGuiEvent.Post event) {
        var mc = Minecraft.getInstance();
        if (!enabled() || mc.player == null || selected == 0 || finished) return;
        var g = event.getGuiGraphics();
        for (int i = 0; i < IDS.length; i++) g.renderItem(mc.player.getInventory().getItem(i), 190 + i * 24, 20);
        g.flush();
        g.pose().pushPose();
        g.pose().translate(180, 195, 150);
        g.pose().scale(135, -135, 135);
        g.pose().mulPose(com.mojang.math.Axis.XP.rotationDegrees(20));
        g.pose().mulPose(com.mojang.math.Axis.YP.rotationDegrees(35));
        var buffers = mc.renderBuffers().bufferSource();
        mc.getItemRenderer().renderStatic(mc.player.getMainHandItem(), net.minecraft.world.item.ItemDisplayContext.FIXED,
                15728880, net.minecraft.client.renderer.texture.OverlayTexture.NO_OVERLAY, g.pose(), buffers, mc.level, 0);
        buffers.endBatch();
        g.pose().popPose();
        event.getGuiGraphics().renderTooltip(mc.font, mc.player.getMainHandItem(), 28, 42);
    }

    @SubscribeEvent
    public static void frame(RenderFrameEvent.Post event) throws Exception {
        if (!enabled() || finished) return;
        var mc = Minecraft.getInstance();
        Path output = mc.gameDirectory.toPath().resolve("screenshots/weapons-approved-v01-final");
        if (!started) {
            if (mc.screen == null || mc.getOverlay() != null) return;
            String language = System.getProperty("starboundmc.debug.weaponSmokeLanguage", "zh_cn");
            if (languageLoad == null) {
                mc.getLanguageManager().setSelected(language); mc.options.languageCode = language;
                languageLoad = mc.reloadResourcePacks(); return;
            }
            if (!languageLoad.isDone()) return;
            languageLoad.join();
            started = true; start = System.nanoTime();
            Files.createDirectories(output); Files.deleteIfExists(output.resolve("complete.txt"));
            org.lwjgl.glfw.GLFW.glfwHideWindow(mc.getWindow().getWindow());
            mc.options.pauseOnLostFocus = false; mc.options.renderDistance().set(4); mc.options.fov().set(70);
            mc.options.guiScale().set(2);
            var name = "weapons-fixture-" + System.currentTimeMillis();
            mc.createWorldOpenFlows().createFreshLevel(name,
                    new LevelSettings(name, GameType.CREATIVE, false, Difficulty.PEACEFUL,
                            true, new GameRules(), WorldDataConfiguration.DEFAULT), new WorldOptions(3413, false, false),
                    r -> r.registryOrThrow(Registries.WORLD_PRESET).getHolderOrThrow(WorldPresets.FLAT)
                            .value().createWorldDimensions(), mc.screen);
            return;
        }
        if ((System.nanoTime() - start) / 1e9 > 240) throw new IllegalStateException("Weapon smoke timed out at " + stage);
        if (mc.player == null || mc.level == null || mc.screen != null || mc.getOverlay() != null) return;
        if (stage == 7 && !reloaded) {
            if (languageLoad == null) languageLoad = mc.reloadResourcePacks();
            if (!languageLoad.isDone()) return;
            languageLoad.join(); reloaded = true; languageLoad = null;
        }
        if (selected == 0) {
            for (int slot = 0; slot < IDS.length; slot++) {
                var item = BuiltInRegistries.ITEM.get(ResourceLocation.fromNamespaceAndPath("starboundmc", IDS[slot]));
                var stack = new ItemStack(item);
                var model = mc.getItemRenderer().getModel(stack, mc.level, mc.player, 0);
                if (model == mc.getModelManager().getMissingModel()) throw new IllegalStateException("Missing model: " + IDS[slot]);
                mc.player.getInventory().setItem(slot, stack);
                mc.gameMode.handleCreativeModeItemAdd(stack, 36 + slot);
            }
            mc.player.getInventory().selected = stage / 2;
            mc.player.setMainArm(stage % 2 == 0 ? net.minecraft.world.entity.HumanoidArm.RIGHT : net.minecraft.world.entity.HumanoidArm.LEFT);
            var active = mc.getItemRenderer().getModel(mc.player.getMainHandItem(), mc.level, mc.player, 0);
            if (stage / 2 > 0) {
                var gui = active.applyTransform(net.minecraft.world.item.ItemDisplayContext.GUI, new com.mojang.blaze3d.vertex.PoseStack(), false);
                var hand = active.applyTransform(net.minecraft.world.item.ItemDisplayContext.FIRST_PERSON_RIGHT_HAND, new com.mojang.blaze3d.vertex.PoseStack(), false);
                var expected = ResourceLocation.fromNamespaceAndPath("starboundmc", "item/" + IDS[stage / 2]);
                if (gui == hand || !gui.getParticleIcon().contents().name().equals(expected)
                        || gui.getQuads(null, null, net.minecraft.util.RandomSource.create()).isEmpty())
                    throw new IllegalStateException("Missing rendered GUI sprite: " + expected);
            }
            mc.player.setXRot(0); mc.player.setYRot(0);
            selected = System.nanoTime(); return;
        }
        if ((System.nanoTime() - selected) / 1e9 < 3) return;
        try (var image = Screenshot.takeScreenshot(mc.getMainRenderTarget())) { image.writeToFile(output.resolve(IDS[stage / 2] + (stage % 2 == 0 ? "-right" : "-left") + ".png")); }
        Files.writeString(output.resolve(IDS[stage / 2] + (stage % 2 == 0 ? "-right" : "-left") + ".txt"),
                "item=" + IDS[stage / 2] + "\nmaxDamage=" + mc.player.getMainHandItem().getMaxDamage() + "\n");
        if (++stage == IDS.length * 2) {
            Files.writeString(output.resolve("complete.txt"), "Rendered 7 items in both hands, separate inventory icons and a resource reload.\n");
            finished = true; mc.stop();
        } else { selected = 0; languageLoad = null; }
    }
}
