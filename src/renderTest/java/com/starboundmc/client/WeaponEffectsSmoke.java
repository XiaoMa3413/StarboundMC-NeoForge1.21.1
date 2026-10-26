package com.starboundmc.client;

import com.starboundmc.item.ModItems;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.Difficulty;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.*;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.WorldOptions;
import net.minecraft.world.level.levelgen.presets.WorldPresets;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderFrameEvent;
import java.nio.file.*;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

/** Real client input -> payload -> server damage -> client particles, in an isolated scene. */
@EventBusSubscriber(modid = "starboundmc", value = Dist.CLIENT)
public final class WeaponEffectsSmoke {
    private static final String[] NAMES = {"coil-left-auto", "plasma-left-auto", "glaive-fan", "starcore-arc", "rail-blast"};
    private static int stage, phase, chargeCaptures, shotBaseline;
    private static long started, phaseTime;
    private static CompletableFuture<?> work;
    private static Villager target, splashTarget;
    private static boolean fired, finished;
    private static double maxKick;
    private static Item weapon() {
        return switch(stage) {
            case 0 -> ModItems.COIL_PISTOL.get(); case 1 -> ModItems.PLASMA_CARBINE.get();
            case 2 -> ModItems.DURASTEEL_GLAIVE.get(); case 3 -> ModItems.STARCORE_BLADE.get();
            default -> ModItems.STARCORE_RAILGUN.get();
        };
    }
    @SubscribeEvent public static void frame(RenderFrameEvent.Post event) throws Exception {
        if (!Boolean.getBoolean("starboundmc.debug.weaponEffectsSmoke") || finished) return;
        var mc = Minecraft.getInstance();
        Path output = mc.gameDirectory.toPath().resolve("screenshots/approved-art-effects");
        if (started == 0) {
            if (mc.screen == null || mc.getOverlay() != null) return;
            started = System.nanoTime(); Files.createDirectories(output); Files.deleteIfExists(output.resolve("complete.txt"));
            mc.options.pauseOnLostFocus = false; mc.options.renderDistance().set(4); mc.options.fov().set(70);
            mc.options.hideGui = false; mc.options.particles().set(net.minecraft.client.ParticleStatus.ALL);
            org.lwjgl.glfw.GLFW.glfwHideWindow(mc.getWindow().getWindow());
            var name = "weapon-effects-" + System.currentTimeMillis();
            mc.createWorldOpenFlows().createFreshLevel(name,
                    new LevelSettings(name, GameType.CREATIVE, false, Difficulty.PEACEFUL, true,
                            new GameRules(), WorldDataConfiguration.DEFAULT), new WorldOptions(3413, false, false),
                    r -> r.registryOrThrow(Registries.WORLD_PRESET).getHolderOrThrow(WorldPresets.FLAT)
                            .value().createWorldDimensions(), mc.screen);
            return;
        }
        if ((System.nanoTime() - started) / 1e9 > 240) throw new IllegalStateException("Effects smoke timed out " + stage + "/" + phase);
        if (mc.player == null || mc.level == null || mc.screen != null || mc.getOverlay() != null) return;
        // The fixture hides its window; mark its synthetic input focus explicitly.
        mc.setWindowActive(true);
        if (phase == 0) {
            mc.options.keyAttack.setDown(false);
            work = mc.getSingleplayerServer().submit(() -> {
                var server = mc.getSingleplayerServer(); var level = server.overworld();
                var player = server.getPlayerList().getPlayers().getFirst();
                if (target != null) target.discard(); if (splashTarget != null) splashTarget.discard();
                for (int x = 994; x <= 1006; x++) for (int z = 996; z <= 1014; z++)
                    level.setBlockAndUpdate(new BlockPos(x, 100, z), Blocks.SMOOTH_STONE.defaultBlockState());
                for (int x = 994; x <= 1006; x++) for (int y = 101; y <= 106; y++)
                    level.setBlockAndUpdate(new BlockPos(x, y, 1011), Blocks.POLISHED_DEEPSLATE.defaultBlockState());
                level.setDayTime(6000);
                player.teleportTo(level, 1000.5, 101, 1000.5, Set.of(), 0, 0);
                player.setGameMode(GameType.SURVIVAL);
                player.getInventory().selected = 0;
                player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(weapon()));
                player.getInventory().setItem(1, new ItemStack(stage == 0 ? Items.IRON_NUGGET : Items.REDSTONE, 64));
                player.inventoryMenu.broadcastChanges();
                for (int i = 0; i < 2; i++) {
                    var mob = EntityType.VILLAGER.create(level);
                    mob.setNoAi(true); mob.setNoGravity(true);
                    mob.getAttribute(Attributes.MAX_HEALTH).setBaseValue(200); mob.setHealth(200);
                    mob.setPos(1000.5 + i * 2, 101, 1000.5 + (stage == 2 ? 5.25 : stage == 3 ? 3 : stage < 2 ? 2 : 8));
                    level.addFreshEntity(mob);
                    if (i == 0) target = mob; else splashTarget = mob;
                }
            });
            phase = 1; phaseTime = System.nanoTime(); return;
        }
        if (phase == 1) {
            if (!work.isDone()) return; work.join();
            if (!mc.level.dimension().equals(Level.OVERWORLD) || !mc.player.getMainHandItem().is(weapon())
                    || (System.nanoTime() - phaseTime) / 1e9 < 2) return;
            mc.player.setYRot(0); mc.player.setXRot(0); mc.player.yRotO = 0; mc.player.xRotO = 0;
            try (var image = Screenshot.takeScreenshot(mc.getMainRenderTarget())) { image.writeToFile(output.resolve(NAMES[stage] + "-idle.png")); }
            shotBaseline = com.starboundmc.client.weapon.GunMotion.acknowledgedShots;
            maxKick = 0; chargeCaptures = 0;
            if (stage == 2 || stage == 3) mc.gameMode.useItem(mc.player, InteractionHand.MAIN_HAND);
            else mc.options.keyAttack.setDown(true);
            phaseTime = System.nanoTime(); phase = 2; fired = false; return;
        }
        if (phase == 2) {
            double elapsed = (System.nanoTime() - phaseTime) / 1e9;
            maxKick = Math.max(maxKick, com.starboundmc.client.weapon.GunMotion.lastKick);
            if (stage == 4 && chargeCaptures < 2 && elapsed >= (chargeCaptures == 0 ? .6 : 1.15)) {
                double pull = com.starboundmc.client.weapon.GunMotion.lastPull;
                if (pull <= (chargeCaptures == 0 ? .025 : .11)) throw new IllegalStateException("Charge model did not pull back: " + pull);
                try (var image = Screenshot.takeScreenshot(mc.getMainRenderTarget())) { image.writeToFile(output.resolve("rail-charge-" + chargeCaptures + ".png")); }
                Files.writeString(output.resolve("rail-charge-" + chargeCaptures + ".txt"), "pull=" + pull);
                chargeCaptures++;
            }
            if (stage == 4 && !fired && elapsed >= 1.3) {
                mc.options.keyAttack.setDown(false); fired = true;
            }
            if (stage == 4 && !fired || stage < 2 && elapsed < .4) return;
            if (!work.isDone()) return;
            if (!Boolean.TRUE.equals(work.join())) {
                work = mc.getSingleplayerServer().submit(() -> target.getHealth() < 200); return;
            }
            // Hold the frame until server-triggered particle packets have reached the client.
            phase = 3; phaseTime = System.nanoTime(); return;
        }
        if (phase == 3) {
            maxKick = Math.max(maxKick, com.starboundmc.client.weapon.GunMotion.lastKick);
            if ((System.nanoTime() - phaseTime) / 1e9 < .12) return;
            if (stage < 2 || stage == 4) {
                if (maxKick <= .001 || com.starboundmc.client.weapon.GunMotion.acknowledgedShots <= shotBaseline)
                    throw new IllegalStateException("Missing server-acknowledged recoil " + maxKick);
                Files.writeString(output.resolve(NAMES[stage] + "-motion.txt"), "maxKick=" + maxKick
                        + " acknowledgedShots=" + (com.starboundmc.client.weapon.GunMotion.acknowledgedShots - shotBaseline));
            }
            try (var image = Screenshot.takeScreenshot(mc.getMainRenderTarget())) { image.writeToFile(output.resolve(NAMES[stage] + ".png")); }
            mc.options.keyAttack.setDown(false);
            work = mc.getSingleplayerServer().submit(() -> {
                var player = mc.getSingleplayerServer().getPlayerList().getPlayers().getFirst();
                if (target.getHealth() >= 200) throw new IllegalStateException("Client input caused no server damage");
                if (stage == 4 && splashTarget.getHealth() >= 200) throw new IllegalStateException("Rail splash absent");
                return "directHealth=" + target.getHealth() + " splashHealth=" + splashTarget.getHealth()
                        + " ammo=" + player.getInventory().getItem(1).getCount()
                        + " durabilityUsed=" + player.getMainHandItem().getDamageValue();
            });
            phase = 4; return;
        }
        if (phase == 4 && work.isDone()) {
            Files.writeString(output.resolve(NAMES[stage] + ".txt"), work.join().toString());
            if (++stage == NAMES.length) {
                Files.writeString(output.resolve("complete.txt"), "Five real client input / server damage / particle scenes completed.\n");
                finished = true; mc.stop();
            } else { phase = 0; work = null; }
        }
    }
}
