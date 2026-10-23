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
    private static java.util.concurrent.CompletableFuture<Void> languageLoad;
    private static final String[] IDS = {"survival_knife", "tungsten_sabre", "coil_pistol", "durasteel_glaive",
            "plasma_carbine", "starcore_blade", "starcore_railgun"};
    private static boolean enabled() { return Boolean.getBoolean("starboundmc.debug.weaponSmoke"); }

    @SubscribeEvent
    public static void overlay(RenderGuiEvent.Post event) {
        var mc = Minecraft.getInstance();
        if (!enabled() || mc.player == null || selected == 0 || finished) return;
        event.getGuiGraphics().renderTooltip(mc.font, mc.player.getMainHandItem(), 28, 42);
    }

    @SubscribeEvent
    public static void frame(RenderFrameEvent.Post event) throws Exception {
        if (!enabled() || finished) return;
        var mc = Minecraft.getInstance();
        Path output = mc.gameDirectory.toPath().resolve("screenshots/weapons");
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
        if (selected == 0) {
            for (int slot = 0; slot < IDS.length; slot++) {
                var item = BuiltInRegistries.ITEM.get(ResourceLocation.fromNamespaceAndPath("starboundmc", IDS[slot]));
                var stack = new ItemStack(item);
                var model = mc.getItemRenderer().getModel(stack, mc.level, mc.player, 0);
                if (model == mc.getModelManager().getMissingModel()) throw new IllegalStateException("Missing model: " + IDS[slot]);
                mc.player.getInventory().setItem(slot, stack);
                mc.gameMode.handleCreativeModeItemAdd(stack, 36 + slot);
            }
            mc.player.getInventory().selected = stage;
            mc.player.setXRot(0); mc.player.setYRot(0);
            selected = System.nanoTime(); return;
        }
        if ((System.nanoTime() - selected) / 1e9 < 3) return;
        try (var image = Screenshot.takeScreenshot(mc.getMainRenderTarget())) { image.writeToFile(output.resolve(IDS[stage] + ".png")); }
        Files.writeString(output.resolve(IDS[stage] + ".txt"),
                "item=" + IDS[stage] + "\nmaxDamage=" + mc.player.getMainHandItem().getMaxDamage() + "\n");
        if (++stage == IDS.length) {
            Files.writeString(output.resolve("complete.txt"), "Rendered all 7 weapons in a fresh integrated-server world.\n");
            finished = true; mc.stop();
        } else selected = 0;
    }
}
