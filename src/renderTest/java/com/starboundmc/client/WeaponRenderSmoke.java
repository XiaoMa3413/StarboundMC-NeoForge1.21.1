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
    private static int itemIndex() { return stage < 14 ? stage / 2 : new int[]{2, 4, 6}[(stage - 14) / 6]; }
    private static boolean leftHand() { return stage < 14 ? stage % 2 == 1 : (stage - 14) % 6 >= 3; }
    private static float pitch() { return stage < 14 ? 0 : new float[]{-30, 0, 30}[(stage - 14) % 3]; }
    private static String label() { return IDS[itemIndex()] + (leftHand() ? "-left" : "-right") + (stage < 14 ? "" : "-pose-" + (int)pitch()); }
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
        g.fill(320, 100, 600, 400, 0xff17212c);
        g.drawString(mc.font, "THIRD PERSON / FRONT + SIDE", 330, 108, 0xffe1edf2);
        renderPlayer(g, 380, 260, 0);
        renderPlayer(g, 510, 260, 90);
        event.getGuiGraphics().renderTooltip(mc.font, mc.player.getMainHandItem(), 28, 42);
    }

    private static void renderPlayer(net.minecraft.client.gui.GuiGraphics g, float x, float y, float angle) {
        var player = Minecraft.getInstance().player;
        float body = player.yBodyRot, yaw = player.getYRot(), head = player.yHeadRot, oldHead = player.yHeadRotO;
        player.yBodyRot = player.yHeadRot = player.yHeadRotO = 180 + angle; player.setYRot(180 + angle);
        net.minecraft.client.gui.screens.inventory.InventoryScreen.renderEntityInInventory(g, x, y, 95,
                new org.joml.Vector3f(0, player.getBbHeight() / 2, 0),
                new org.joml.Quaternionf().rotateZ((float)Math.PI), new org.joml.Quaternionf(), player);
        player.yBodyRot = body; player.yHeadRot = head; player.yHeadRotO = oldHead; player.setYRot(yaw);
    }

    private static void verifyMuzzle() {
        var mc = Minecraft.getInstance();
        if (!(mc.player.getMainHandItem().getItem() instanceof com.starboundmc.weapon.EnergyGunItem)) return;
        var renderer = (net.minecraft.client.renderer.entity.player.PlayerRenderer) mc.getEntityRenderDispatcher().getRenderer(mc.player);
        var model = renderer.getModel();
        model.setupAnim(mc.player, 0, 0, 0, 0, pitch());
        var poses = new com.mojang.blaze3d.vertex.PoseStack();
        model.translateToHand(mc.player.getMainArm(), poses);
        poses.mulPose(com.mojang.math.Axis.XP.rotationDegrees(-90));
        poses.mulPose(com.mojang.math.Axis.YP.rotationDegrees(180));
        var context = leftHand() ? net.minecraft.world.item.ItemDisplayContext.THIRD_PERSON_LEFT_HAND : net.minecraft.world.item.ItemDisplayContext.THIRD_PERSON_RIGHT_HAND;
        mc.getItemRenderer().getModel(mc.player.getMainHandItem(), mc.level, mc.player, 0).applyTransform(context, poses, leftHand());
        var direction = poses.last().pose().transformDirection(new org.joml.Vector3f(1, 0, 0)).normalize();
        float rad = (float)Math.toRadians(pitch());
        var aim = new org.joml.Vector3f(0, (float)Math.sin(rad), -(float)Math.cos(rad));
        if (direction.dot(aim) < .85f) throw new IllegalStateException("Muzzle is not forward: " + label() + " direction=" + direction + " aim=" + aim);
    }

    @SubscribeEvent
    public static void frame(RenderFrameEvent.Post event) throws Exception {
        if (!enabled() || finished) return;
        var mc = Minecraft.getInstance();
        Path output = mc.gameDirectory.toPath().resolve("screenshots/weapons-v04-game");
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
            mc.player.getInventory().selected = itemIndex();
            mc.player.setMainArm(leftHand() ? net.minecraft.world.entity.HumanoidArm.LEFT : net.minecraft.world.entity.HumanoidArm.RIGHT);
            mc.options.keyShift.setDown(stage >= 14 && (stage - 14) % 3 == 1);
            mc.player.setItemInHand(InteractionHand.OFF_HAND, stage >= 14 && (stage - 14) % 3 == 2 ? new ItemStack(net.minecraft.world.item.Items.SHIELD) : ItemStack.EMPTY);
            var active = mc.getItemRenderer().getModel(mc.player.getMainHandItem(), mc.level, mc.player, 0);
            if (itemIndex() > 0) {
                var gui = active.applyTransform(net.minecraft.world.item.ItemDisplayContext.GUI, new com.mojang.blaze3d.vertex.PoseStack(), false);
                var hand = active.applyTransform(net.minecraft.world.item.ItemDisplayContext.FIRST_PERSON_RIGHT_HAND, new com.mojang.blaze3d.vertex.PoseStack(), false);
                var expected = ResourceLocation.fromNamespaceAndPath("starboundmc", "item/" + IDS[itemIndex()]);
                if (gui == hand || !gui.getParticleIcon().contents().name().equals(expected)
                        || gui.getQuads(null, null, net.minecraft.util.RandomSource.create()).isEmpty())
                    throw new IllegalStateException("Missing rendered GUI sprite: " + expected);
            }
            mc.player.setXRot(pitch()); mc.player.setYRot(0);
            selected = System.nanoTime(); return;
        }
        if ((System.nanoTime() - selected) / 1e9 < 2) return;
        verifyMuzzle();
        if (stage >= 14 && (stage - 14) % 3 == 1 && !mc.player.isCrouching())
            throw new IllegalStateException("Crouch input was not applied: " + label());
        if (stage >= 14 && (stage - 14) % 3 == 2 && !mc.player.getOffhandItem().is(net.minecraft.world.item.Items.SHIELD))
            throw new IllegalStateException("Offhand fixture missing shield: " + label());
        try (var image = Screenshot.takeScreenshot(mc.getMainRenderTarget())) { image.writeToFile(output.resolve(label() + ".png")); }
        Files.writeString(output.resolve(label() + ".txt"), "item=" + IDS[itemIndex()] + "\npitch=" + pitch() + "\ncrouch=" + mc.player.isCrouching() + "\n");
        if (++stage == 32) {
            Files.writeString(output.resolve("complete.txt"), "32 views: 7 items in both hands, three guns with +/-30 degree pitch, crouch, offhand shield, forward muzzle matrix and resource reload.\n");
            finished = true; mc.stop();
        } else { selected = 0; languageLoad = null; }
    }
}
