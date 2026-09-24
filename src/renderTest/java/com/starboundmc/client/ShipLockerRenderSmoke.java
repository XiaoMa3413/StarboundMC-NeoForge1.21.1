package com.starboundmc.client;

import com.mojang.authlib.GameProfile;
import com.starboundmc.StarboundMC;
import com.starboundmc.block.ModBlocks;
import com.starboundmc.block.entity.ShipCrateBlockEntity;
import com.starboundmc.menu.ShipCrateMenu;
import com.starboundmc.world.ShipDimensions;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.Difficulty;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.levelgen.WorldOptions;
import net.minecraft.world.level.levelgen.presets.WorldPresets;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderFrameEvent;
import net.neoforged.neoforge.common.util.FakePlayerFactory;

import java.nio.file.Files;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/** Fresh isolated world: normal models, server menu lifecycle, facing and reload checks. */
@EventBusSubscriber(modid = StarboundMC.MODID, value = Dist.CLIENT)
public final class ShipLockerRenderSmoke {
    private static final BlockPos LOCKER = new BlockPos(1000, 190, 1000);
    private static final String[] NAMES = {"closed-north", "open", "closed-again", "south", "east", "west", "dark", "resource-reload"};
    private static boolean started, done;
    private static long start, stageStart;
    private static int stage;
    private static CompletableFuture<?> setup, stageSetup, reload;

    @SubscribeEvent
    public static void frame(RenderFrameEvent.Post event) throws Exception {
        if (!Boolean.getBoolean("starboundmc.debug.lockerSmoke") || done) return;
        var mc = Minecraft.getInstance();
        if (!started) {
            if (mc.screen == null || mc.getOverlay() != null) return;
            started = true;
            start = System.nanoTime();
            mc.options.pauseOnLostFocus = false;
            mc.options.hideGui = true;
            mc.options.renderDistance().set(4);
            mc.options.fov().set(35);
            String name = "locker-" + System.currentTimeMillis();
            mc.createWorldOpenFlows().createFreshLevel(name,
                    new LevelSettings(name, GameType.CREATIVE, false, Difficulty.PEACEFUL, true,
                            new GameRules(), WorldDataConfiguration.DEFAULT), new WorldOptions(3413, false, false),
                    r -> r.registryOrThrow(Registries.WORLD_PRESET).getHolderOrThrow(WorldPresets.FLAT)
                            .value().createWorldDimensions(), mc.screen);
            return;
        }
        if ((System.nanoTime() - start) / 1e9 > 240) throw new IllegalStateException("Locker smoke timed out at " + stage);
        if (mc.player == null || mc.level == null || mc.getOverlay() != null || mc.screen != null) return;
        if (setup == null) {
            setup = mc.getSingleplayerServer().submit(() -> {
                var level = mc.getSingleplayerServer().getLevel(ShipDimensions.SHIP_LEVEL);
                for (int x = 995; x <= 1006; x++) for (int z = 995; z <= 1006; z++)
                    level.setBlockAndUpdate(new BlockPos(x, 188, z), Blocks.SMOOTH_STONE.defaultBlockState());
                level.setBlockAndUpdate(LOCKER, ModBlocks.SHIP_CRATE.get().defaultBlockState());
                var crate = (ShipCrateBlockEntity) level.getBlockEntity(LOCKER);
                crate.setItem(0, new ItemStack(Items.DIAMOND, 7));
                var saved = crate.saveWithFullMetadata(level.registryAccess());
                var restored = new ShipCrateBlockEntity(LOCKER, crate.getBlockState());
                restored.loadWithComponents(saved, level.registryAccess());
                check(restored.getContainerSize() == 54 && restored.getItem(0).getCount() == 7, "Inventory round trip");
                var first = FakePlayerFactory.get(level, new GameProfile(UUID.fromString("97e79f54-4dd3-42d9-84ca-110e0b197100"), "LockerFirst"));
                var second = FakePlayerFactory.get(level, new GameProfile(UUID.fromString("97e79f54-4dd3-42d9-84ca-110e0b197101"), "LockerSecond"));
                var a = new ShipCrateMenu(10, first.getInventory(), crate, crate::setChanged);
                var b = new ShipCrateMenu(11, second.getInventory(), crate, crate::setChanged);
                check(crate.isOpen(), "Two viewers must open locker");
                restored.handleUpdateTag(crate.getUpdateTag(level.registryAccess()), level.registryAccess());
                check(restored.isOpen() && restored.getItem(0).getCount() == 7,
                        "Late chunk update must synchronize doors without replacing inventory");
                a.removed(first);
                check(crate.isOpen(), "First viewer leaving must not close locker");
                b.removed(second);
                check(!crate.isOpen(), "Last viewer must close locker");
                // A removed viewer must not resurrect a broken container.
                var removed = new ShipCrateBlockEntity(LOCKER.above(), ModBlocks.SHIP_CRATE.get().defaultBlockState());
                removed.setLevel(level);
                removed.setRemoved();
                removed.stopOpen(first);
                check(level.getBlockState(LOCKER.above()).isAir(), "Removed entity resurrected its block");
            });
            return;
        }
        if (!setup.isDone()) return;
        setup.join();
        if (stageSetup == null) {
            stageSetup = mc.getSingleplayerServer().submit(() -> {
                var level = mc.getSingleplayerServer().getLevel(ShipDimensions.SHIP_LEVEL);
                var player = mc.getSingleplayerServer().getPlayerList().getPlayers().getFirst();
                Direction facing = switch (stage) { case 3 -> Direction.SOUTH; case 4 -> Direction.EAST; case 5 -> Direction.WEST; default -> Direction.NORTH; };
                for (int x = 998; x <= 1002; x++) for (int z = 998; z <= 1002; z++)
                    for (int y = 189; y <= 193; y++) {
                        BlockPos pos = new BlockPos(x, y, z);
                        if (!pos.equals(LOCKER)) level.setBlockAndUpdate(pos, Blocks.AIR.defaultBlockState());
                    }
                for (int offset = -2; offset <= 2; offset++) for (int y = 189; y <= 193; y++) {
                    BlockPos wall = LOCKER.relative(facing.getOpposite()).relative(facing.getClockWise(), offset).atY(y);
                    level.setBlockAndUpdate(wall, Blocks.LIGHT_GRAY_CONCRETE.defaultBlockState());
                }
                if (stage < 6) level.setBlockAndUpdate(LOCKER.above(2).relative(facing), Blocks.SEA_LANTERN.defaultBlockState());
                level.setBlockAndUpdate(LOCKER, level.getBlockState(LOCKER).setValue(BlockStateProperties.HORIZONTAL_FACING, facing));
                double angle = Math.toRadians(facing.toYRot() - 180);
                double dx = 1.8 * Math.cos(angle) - (-3.1) * Math.sin(angle);
                double dz = 1.8 * Math.sin(angle) + (-3.1) * Math.cos(angle);
                float yaw = (float) Math.toDegrees(Math.atan2(dx, -dz));
                player.teleportTo(level, 1000.5 + dx, 189.5, 1000.5 + dz, Set.of(), yaw, 9.5F);
                player.getAbilities().flying = true;
                player.onUpdateAbilities();
                var crate = (ShipCrateBlockEntity) level.getBlockEntity(LOCKER);
                if (stage == 1) {
                    player.containerMenu = new ShipCrateMenu(12, player.getInventory(), crate,
                            ContainerLevelAccess.create(level, LOCKER), crate::setChanged);
                } else if (player.containerMenu instanceof ShipCrateMenu menu) {
                    menu.removed(player);
                    player.containerMenu = player.inventoryMenu;
                }
                check(crate.getItem(0).getCount() == 7, "Animation changed inventory contents");
                check(crate.getBlockState().getShape(level, LOCKER).bounds().getYsize() < 1, "Full cube shape returned");
            });
            return;
        }
        if (!stageSetup.isDone() || !mc.level.dimension().equals(ShipDimensions.SHIP_LEVEL)) return;
        stageSetup.join();
        if (stage == 7 && reload == null) {
            reload = mc.reloadResourcePacks();
            return;
        }
        if (reload != null && !reload.isDone()) return;
        if (reload != null) reload.join();
        if (stageStart == 0) stageStart = System.nanoTime();
        if ((System.nanoTime() - stageStart) / 1e9 < 3) return;
        var clientCrate = (ShipCrateBlockEntity) mc.level.getBlockEntity(LOCKER);
        check(clientCrate.isOpen() == (stage == 1), "Client door state did not match server viewers");
        if (stage == 7) {
            var atlas = mc.getTextureAtlas(net.minecraft.client.renderer.texture.TextureAtlas.LOCATION_BLOCKS);
            for (String name : new String[]{"locker_hologram", "locker_hologram_frame"}) {
                var id = net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("starboundmc", "effect/" + name);
                check(atlas.apply(id).contents().name().equals(id), "Missing inventory hologram texture: " + name);
            }
        }
        var directory = mc.gameDirectory.toPath().resolve("screenshots");
        Files.createDirectories(directory);
        try (var image = Screenshot.takeScreenshot(mc.getMainRenderTarget())) {
            image.writeToFile(directory.resolve(NAMES[stage] + ".png"));
        }
        if (++stage == NAMES.length) {
            Files.writeString(mc.gameDirectory.toPath().resolve("locker-smoke.txt"),
                    "Eight captures; viewer lifecycle, inventory, removed entity, facing and reload checks passed.\n");
            done = true;
            mc.stop();
        }
        stageStart = 0;
        stageSetup = null;
    }

    private static void check(boolean condition, String message) {
        if (!condition) throw new IllegalStateException(message);
    }
}
