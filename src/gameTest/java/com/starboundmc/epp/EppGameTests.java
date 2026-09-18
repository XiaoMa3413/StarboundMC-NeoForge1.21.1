// SPDX-License-Identifier: MPL-2.0
package com.starboundmc.epp;

import com.mojang.authlib.GameProfile;
import com.starboundmc.block.ModBlocks;
import com.starboundmc.item.ModItems;
import com.starboundmc.story.ModAttachments;
import com.starboundmc.world.RockyMoonPlanet;
import com.starboundmc.world.ShipDimensions;
import io.netty.channel.embedded.EmbeddedChannel;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.server.level.ClientInformation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.CommonListenerCookie;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameRules;
import net.neoforged.neoforge.event.entity.living.LivingDropsEvent;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import java.util.ArrayList;
import java.util.UUID;

@GameTestHolder("starboundmc")
@PrefixGameTestTemplate(false)
public final class EppGameTests {
    private static final java.util.Set<BlockPos> COLD_SERVICE_POSITIONS = new java.util.HashSet<>();
    private static final EnvironmentState COLD = new EnvironmentState(EnvironmentState.Atmosphere.BREATHABLE, 1, 0, 0, 1, false);
    static {
        EppEquipmentResolver.registerSource("starboundmc:gametest_extra", player ->
                player.getTags().contains("epp_resolver_test") ? player.getInventory().getItem(35) : ItemStack.EMPTY);
        // GameTestServer supplies only vanilla dimensions. Inject two spatial fixtures through the
        // same public zone API used by future stations, while exercising real players and items.
        PlayerEnvironmentService.registerZone("starboundmc:gametest_zone", (level, pos) ->
                pos.getX() == -30000 ? java.util.Optional.of(EnvironmentState.SPACE)
                        : pos.getX() == -30001 ? java.util.Optional.of(EnvironmentState.SHIP_INTERIOR)
                        : pos.getX() == -30002 || COLD_SERVICE_POSITIONS.contains(pos) ? java.util.Optional.of(COLD)
                        : java.util.Optional.empty());
    }
    private record TestPlayer(ServerPlayer player, EmbeddedChannel channel) implements AutoCloseable {
        @Override public void close() { player.discard(); channel.finishAndReleaseAll(); }
    }
    private static TestPlayer player(ServerLevel level, String name) {
        var p = new ServerPlayer(level.getServer(), level, new GameProfile(UUID.randomUUID(), name), ClientInformation.createDefault());
        var connection = new Connection(PacketFlow.SERVERBOUND);
        var channel = new EmbeddedChannel(connection);
        var channels = new java.util.HashMap<net.minecraft.resources.ResourceLocation,
                net.neoforged.neoforge.network.registration.NetworkChannel>();
        for (var type : java.util.List.of(com.starboundmc.network.EppSnapshotPacket.TYPE,
                com.starboundmc.network.EppVisualPacket.TYPE, com.starboundmc.network.NovaBroadcastPacket.TYPE,
                com.starboundmc.network.EvaStatePacket.TYPE)) {
            channels.put(type.id(), new net.neoforged.neoforge.network.registration.NetworkChannel(
                    type.id(), com.starboundmc.network.ModNetwork.PROTOCOL_VERSION));
        }
        // Embedded clients do not perform the normal login handshake; install its negotiated result.
        net.neoforged.neoforge.network.registration.ChannelAttributes.setPayloadSetup(connection,
                new net.neoforged.neoforge.network.registration.NetworkPayloadSetup(
                        java.util.Map.of(net.minecraft.network.ConnectionProtocol.PLAY, channels)));
        p.connection = new ServerGamePacketListenerImpl(level.getServer(), connection, p, CommonListenerCookie.createInitial(p.getGameProfile(), false));
        return new TestPlayer(p, channel);
    }
    private static ItemStack pack(int oxygen) {
        var stack = new ItemStack(ModItems.EPP_MK1.get()); EppItem.setOxygen(stack, oxygen); return stack;
    }
    @GameTest(template = "shuttle_test_empty")
    public static void emergencyRecallClearsDriftWithoutRefillingOrLosingEquipment(GameTestHelper h) {
        var destination = new BlockPos(0, 103, 0);
        var positions = java.util.List.of(destination.below(), destination, destination.above());
        var states = positions.stream().map(h.getLevel()::getBlockState).toList();
        var tags = positions.stream().map(pos -> {
            var be = h.getLevel().getBlockEntity(pos);
            return be == null ? null : be.saveWithFullMetadata(h.getLevel().registryAccess());
        }).toList();
        try (var fixture = player(h.getLevel(), "EvaRescue")) {
            var p = fixture.player;
            for (var pos : positions) h.getLevel().setBlockAndUpdate(pos, net.minecraft.world.level.block.Blocks.AIR.defaultBlockState());
            h.getLevel().setBlockAndUpdate(destination.below(), net.minecraft.world.level.block.Blocks.STONE.defaultBlockState());
            for (var equipment : java.util.List.of(ItemStack.EMPTY, pack(42))) {
                p.setPos(-29999.5, 220, 0); p.setDeltaMovement(.1, .2, .1); p.fallDistance = 30;
                p.setData(ModAttachments.EPP_EQUIPMENT, equipment);
                p.getData(ModAttachments.EVA).input = EvaMotion.UP;
                h.assertTrue(EvaEmergencyRecall.tryDestination(p, destination), "Safe rescue refused");
                h.assertTrue(p.blockPosition().equals(destination), "Rescue did not reach cabin");
                h.assertTrue(p.getDeltaMovement().equals(net.minecraft.world.phys.Vec3.ZERO) && p.fallDistance == 0, "Rescue retained drift/fall damage");
                h.assertTrue(p.getData(ModAttachments.EVA).input == 0, "Rescue retained propulsion input");
                h.assertTrue(p.getData(ModAttachments.EPP_EQUIPMENT) == equipment, "Rescue replaced equipment");
                if (!equipment.isEmpty()) h.assertTrue(EppItem.oxygen(equipment) == 42, "Rescue refilled oxygen for free");
            }
            h.getLevel().setBlockAndUpdate(destination, net.minecraft.world.level.block.Blocks.STONE.defaultBlockState());
            var before = p.position();
            h.assertTrue(!EvaEmergencyRecall.tryDestination(p, destination) && p.position().equals(before), "Blocked rescue moved player into blocks");
            h.assertTrue(!EvaEmergencyRecall.tryDestination(p, new BlockPos(20, 103, 0)), "Rescue destination outside cabin accepted");
            h.assertTrue(!EvaEmergencyRecall.canRequest(p), "Planet player gained emergency ship recall");
        } finally {
            for (int i = 0; i < positions.size(); i++) {
                h.getLevel().setBlockAndUpdate(positions.get(i), states.get(i));
                var be = h.getLevel().getBlockEntity(positions.get(i));
                if (be != null && tags.get(i) != null) be.loadWithComponents(tags.get(i), h.getLevel().registryAccess());
            }
        }
        h.succeed();
    }
    @GameTest(template = "shuttle_test_empty")
    public static void evaPermissionsUseOnlyActivePackAndCurrentEnvironment(GameTestHelper h) {
        try (var fixture = player(h.getLevel(), "EvaPermissions")) {
            var p = fixture.player; p.setPos(-29999.5, 220, 0);
            h.assertTrue(EvaMovement.serverMode(p) == EvaState.DRIFT, "Vacuum must be zero gravity even without EPP");
            p.getInventory().setItem(0, new ItemStack(ModItems.EPP_MK2.get()));
            p.setData(ModAttachments.EPP_EQUIPMENT, pack(100));
            EvaMovement.acceptInput(p, EvaMotion.UP);
            h.assertTrue(EvaMovement.serverMode(p) == EvaState.DRIFT && p.getData(ModAttachments.EVA).input == 0, "Spare Mk.II or Mk.I granted thrust");
            p.setData(ModAttachments.EPP_EQUIPMENT, new ItemStack(ModItems.EPP_MK2.get()));
            EvaMovement.update(p); EvaMovement.acceptInput(p, EvaMotion.UP);
            h.assertTrue(p.getData(ModAttachments.EVA).input == EvaMotion.UP, "Equipped Mk.II did not authorize thrust");
            h.assertTrue(EppItem.oxygen(p.getData(ModAttachments.EPP_EQUIPMENT)) == 0, "EVA must not add oxygen or require a new energy resource");
            p.setPos(-30000.5, 220, 0); EvaMovement.update(p);
            h.assertTrue(EvaMovement.serverMode(p) == EvaState.NORMAL && p.getData(ModAttachments.EVA).input == 0, "Cabin kept stale propulsion");
            p.setPos(-29999.5, 220, 0); p.setGameMode(net.minecraft.world.level.GameType.CREATIVE);
            h.assertTrue(EvaMovement.serverMode(p) == EvaState.NORMAL, "Creative movement was overridden");
            p.setGameMode(net.minecraft.world.level.GameType.SPECTATOR);
            h.assertTrue(EvaMovement.serverMode(p) == EvaState.NORMAL, "Spectator movement was overridden");
        }
        h.succeed();
    }
    @GameTest(template = "shuttle_test_empty")
    public static void evaTravelMixinMovesWithoutGravityAndRestoresNormalTravel(GameTestHelper h) {
        try (var fixture = player(h.getLevel(), "EvaTravel")) {
            var p = fixture.player; p.setPos(-29999.5, 220, 0);
            h.getLevel().getChunk(p.blockPosition());
            p.setData(ModAttachments.EPP_EQUIPMENT, new ItemStack(ModItems.EPP_MK2.get()));
            EvaMovement.update(p); p.setDeltaMovement(net.minecraft.world.phys.Vec3.ZERO);
            for (int i = 0; i < 20; i++) { p.tickCount++; p.travel(net.minecraft.world.phys.Vec3.ZERO); }
            h.assertTrue(p.getY() == 220, "Zero gravity fell; Player.travel mixin not active");
            EvaMovement.acceptInput(p, EvaMotion.UP);
            p.fallDistance = 20; p.travel(net.minecraft.world.phys.Vec3.ZERO);
            h.assertTrue(p.getY() > 220 && p.fallDistance == 0, "EVA failed to propel or clear fall distance");
            p.setPos(-30000.5, 220, 0); EvaMovement.update(p);
            p.setDeltaMovement(net.minecraft.world.phys.Vec3.ZERO);
            p.travel(net.minecraft.world.phys.Vec3.ZERO); p.travel(net.minecraft.world.phys.Vec3.ZERO);
            h.assertTrue(p.getY() < 220, "Normal gravity did not resume in cabin");
            h.assertTrue(!p.getAbilities().mayfly && !p.getAbilities().flying, "EVA leaked creative flight permission");
        }
        h.succeed();
    }
    @GameTest(template = "shuttle_test_empty")
    public static void evaRejectsInvalidStaleAndMenuInputs(GameTestHelper h) {
        try (var fixture = player(h.getLevel(), "EvaInputs")) {
            var p = fixture.player; p.setPos(-29999.5, 220, 0);
            p.setData(ModAttachments.EPP_EQUIPMENT, new ItemStack(ModItems.EPP_MK2.get()));
            EvaMovement.update(p); EvaMovement.acceptInput(p, 255);
            h.assertTrue(p.getData(ModAttachments.EVA).input == 0, "Invalid control mask accepted");
            EvaMovement.acceptInput(p, EvaMotion.UP); p.tickCount += 11;
            p.setDeltaMovement(net.minecraft.world.phys.Vec3.ZERO); p.travel(net.minecraft.world.phys.Vec3.ZERO);
            h.assertTrue(p.getY() == 220, "Expired input kept accelerating");
            p.containerMenu = new EppMenu(8, p.getInventory());
            EvaMovement.acceptInput(p, EvaMotion.UP);
            h.assertTrue(p.getData(ModAttachments.EVA).input == 0, "Open equipment menu accepted propulsion");
            p.containerMenu = p.inventoryMenu;
            EvaMovement.acceptInput(p, EvaMotion.UP);
            p.setData(ModAttachments.EPP_EQUIPMENT, ItemStack.EMPTY);
            p.setDeltaMovement(net.minecraft.world.phys.Vec3.ZERO); p.travel(net.minecraft.world.phys.Vec3.ZERO);
            h.assertTrue(p.getY() == 220, "Unequipped pack kept thrust until next sync");
        }
        h.succeed();
    }
    @GameTest(template = "shuttle_test_empty")
    public static void evaUsesRealBlockCollision(GameTestHelper h) {
        var wall = new BlockPos(-30000, 220, 2);
        var old = h.getLevel().getBlockState(wall);
        var oldUpper = h.getLevel().getBlockState(wall.above());
        try (var fixture = player(h.getLevel(), "EvaCollision")) {
            h.getLevel().setBlockAndUpdate(wall, net.minecraft.world.level.block.Blocks.STONE.defaultBlockState());
            h.getLevel().setBlockAndUpdate(wall.above(), net.minecraft.world.level.block.Blocks.STONE.defaultBlockState());
            var p = fixture.player; p.setPos(-29999.5, 220, 0);
            p.setYRot(0); p.setXRot(0);
            p.setData(ModAttachments.EPP_EQUIPMENT, new ItemStack(ModItems.EPP_MK2.get()));
            EvaMovement.update(p);
            for (int i = 0; i < 60; i++) {
                p.tickCount++; EvaMovement.acceptInput(p, EvaMotion.FORWARD); p.travel(net.minecraft.world.phys.Vec3.ZERO);
            }
            h.assertTrue(p.getZ() > 1 && p.getZ() <= 1.71, "EVA bypassed wall collision or failed to reach wall");
        } finally {
            h.getLevel().setBlockAndUpdate(wall, old); h.getLevel().setBlockAndUpdate(wall.above(), oldUpper);
        }
        h.succeed();
    }
    @GameTest(template = "shuttle_test_empty")
    public static void zeroGravityMovementDoesNotTriggerVanillaFloatingKick(GameTestHelper h) throws Exception {
        try (var fixture = player(h.getLevel(), "EvaFloating")) {
            var p = fixture.player; p.setPos(-29999.5, 225, 0);
            h.getLevel().getChunk(p.blockPosition());
            // Real movement packets require registration with the server's chunk tracker.
            h.getLevel().addNewPlayer(p);
            p.connection.resetPosition();
            for (int i = 0; i < 100; i++) {
                p.connection.handleMovePlayer(new net.minecraft.network.protocol.game.ServerboundMovePlayerPacket.Pos(
                        p.getX(), 225, p.getZ(), false));
                p.connection.tick();
            }
            h.assertTrue(fixture.channel.isOpen(), "Legitimate zero-gravity drift was kicked as flying");
            h.assertTrue(!p.getAbilities().mayfly, "Floating exception granted unrestricted flight");
            var floating = ServerGamePacketListenerImpl.class.getDeclaredField("clientIsFloating");
            floating.setAccessible(true);
            h.assertTrue(!floating.getBoolean(p.connection), "Zero gravity retained floating violation");
            p.setPos(-30000.5, 225, 0); p.connection.resetPosition();
            floating.setBoolean(p.connection, true); p.connection.tick();
            h.assertTrue(floating.getBoolean(p.connection), "Floating exception leaked into ordinary gravity");
        }
        h.succeed();
    }
    private static EppServiceMenu service(GameTestHelper h, ServerPlayer player) {
        var pos = h.absolutePos(new BlockPos(1, 1, 1));
        h.getLevel().setBlockAndUpdate(pos, ModBlocks.EPP_SERVICE_STATION.get().defaultBlockState());
        player.setPos(pos.getX(), pos.getY(), pos.getZ());
        var menu = new EppServiceMenu(1, player.getInventory(), pos); player.containerMenu = menu;
        return menu;
    }
    @GameTest(template = "shuttle_test_empty")
    public static void upgradePreservesOriginalDeviceAndConsumesKitOnce(GameTestHelper h) {
        try (var p = player(h.getLevel(), "EppUpgrade")) {
            var menu = service(h, p.player); var old = pack(321);
            var name = net.minecraft.network.chat.Component.literal("Expedition One");
            old.set(net.minecraft.core.component.DataComponents.CUSTOM_NAME, name);
            menu.getSlot(0).set(old); menu.getSlot(2).set(new ItemStack(ModItems.EPP_MK2_UPGRADE_KIT.get()));
            h.assertTrue(menu.clickMenuButton(p.player, EppServiceMenu.UPGRADE), "Valid upgrade rejected");
            var upgraded = menu.getSlot(0).getItem();
            h.assertTrue(upgraded.is(ModItems.EPP_MK2.get()) && EppItem.oxygen(upgraded) == 321, "Upgrade reset oxygen or chassis");
            h.assertTrue(name.equals(upgraded.get(net.minecraft.core.component.DataComponents.CUSTOM_NAME)), "Custom name lost");
            h.assertTrue(EppItem.capacity(upgraded) == EppConfig.MK2_CAPACITY.get(), "Mk.II capacity not applied");
            h.assertTrue(menu.getSlot(2).getItem().isEmpty() && !menu.clickMenuButton(p.player, EppServiceMenu.UPGRADE), "Replay duplicated upgrade");
            p.player.setData(ModAttachments.EPP_EQUIPMENT, upgraded);
            EppItem.setOxygen(upgraded, EppItem.capacity(upgraded) - EppConfig.CANISTER.get());
            h.assertTrue(OxygenCanisterItem.canUse(p.player), "Canister still uses Mk.I capacity");
            new ItemStack(ModItems.OXYGEN_CANISTER.get()).finishUsingItem(h.getLevel(), p.player);
            h.assertTrue(EppItem.oxygen(upgraded) == EppItem.capacity(upgraded), "Canister did not fill Mk.II");
        }
        h.succeed();
    }
    @GameTest(template = "shuttle_test_empty")
    public static void mk3UpgradeAddsSecondModuleSlotAndRetainsExistingModule(GameTestHelper h) {
        try (var p = player(h.getLevel(), "EppMk3")) {
            var menu = service(h, p.player);
            var old = new ItemStack(ModItems.EPP_MK2.get());
            old.set(com.starboundmc.item.ModDataComponents.EPP_MODULES,
                    net.minecraft.world.item.component.ItemContainerContents.fromItems(
                            java.util.List.of(new ItemStack(ModItems.HEATING_MODULE_1.get()))));
            menu.getSlot(0).set(old); menu.getSlot(2).set(new ItemStack(ModItems.EPP_MK3_UPGRADE_KIT.get()));
            h.assertTrue(menu.clickMenuButton(p.player, EppServiceMenu.UPGRADE), "Mk.III upgrade rejected");
            var upgraded = menu.getSlot(0).getItem();
            h.assertTrue(upgraded.is(ModItems.EPP_MK3.get()) && ((EppItem) upgraded.getItem()).moduleSlots() == 2,
                    "Mk.III did not expose two module slots");
            h.assertTrue(EppProtection.from(upgraded).coldTier() == 1, "Existing module was not retained");
            menu.getSlot(1).set(new ItemStack(ModItems.HEATING_MODULE_1.get()));
            h.assertTrue(menu.clickMenuButton(p.player, EppServiceMenu.INSTALL), "Mk.III second module rejected");
            h.assertTrue(EppProtection.from(upgraded).coldTier() == 1
                    && upgraded.getOrDefault(com.starboundmc.item.ModDataComponents.EPP_MODULES,
                    net.minecraft.world.item.component.ItemContainerContents.EMPTY).nonEmptyStream().count() == 2,
                    "Mk.III did not retain two installed modules");
        }
        h.succeed();
    }
    @GameTest(template = "shuttle_test_empty")
    public static void modulesRequireSupportedChassisAndReturnWithoutDuplication(GameTestHelper h) {
        try (var p = player(h.getLevel(), "EppModule")) {
            var menu = service(h, p.player); var module = new ItemStack(ModItems.HEATING_MODULE_1.get());
            module.set(net.minecraft.core.component.DataComponents.CUSTOM_NAME, net.minecraft.network.chat.Component.literal("Heater A"));
            var expectedModule = module.copy();
            menu.getSlot(0).set(pack(100)); menu.getSlot(1).set(module);
            h.assertTrue(!menu.clickMenuButton(p.player, EppServiceMenu.INSTALL), "Mk.I accepted a module");
            var upgraded = new ItemStack(ModItems.EPP_MK2.get()); menu.getSlot(0).set(upgraded);
            h.assertTrue(menu.clickMenuButton(p.player, EppServiceMenu.INSTALL), "Mk.II rejected Heating I");
            h.assertTrue(menu.getSlot(1).getItem().isEmpty() && EppProtection.from(upgraded).coldTier() == 1, "Module not moved into pack");
            menu.getSlot(1).set(new ItemStack(ModItems.HEATING_MODULE_1.get()));
            h.assertTrue(!menu.clickMenuButton(p.player, EppServiceMenu.INSTALL), "Second module stacked protection");
            h.assertTrue(!menu.clickMenuButton(p.player, EppServiceMenu.REMOVE), "Removal overwrote occupied tray");
            menu.getSlot(1).set(ItemStack.EMPTY);
            h.assertTrue(menu.clickMenuButton(p.player, EppServiceMenu.REMOVE), "Module could not be removed");
            h.assertTrue(ItemStack.matches(expectedModule, menu.getSlot(1).getItem()), "Removed module lost its components");
            h.assertTrue(EppProtection.from(upgraded).coldTier() == 0, "Removed module retained protection");
            h.assertTrue(!menu.clickMenuButton(p.player, EppServiceMenu.REMOVE), "Removal replay duplicated module");
        }
        h.succeed();
    }
    @GameTest(template = "shuttle_test_empty")
    public static void stationRejectsUnsafeRemoteStaleAndForeignActions(GameTestHelper h) {
        try (var p = player(h.getLevel(), "EppSecure"); var other = player(h.getLevel(), "EppOther")) {
            var menu = service(h, p.player);
            menu.getSlot(0).set(pack(200)); menu.getSlot(2).set(new ItemStack(ModItems.EPP_MK2_UPGRADE_KIT.get()));
            h.assertTrue(!menu.clickMenuButton(other.player, EppServiceMenu.UPGRADE), "Foreign player changed tray");
            COLD_SERVICE_POSITIONS.add(menu.blockPos());
            try { h.assertTrue(!menu.clickMenuButton(p.player, EppServiceMenu.UPGRADE), "Hazardous station allowed service"); }
            finally { COLD_SERVICE_POSITIONS.remove(menu.blockPos()); }
            p.player.setPos(p.player.getX() + 20, p.player.getY(), p.player.getZ());
            h.assertTrue(!menu.clickMenuButton(p.player, EppServiceMenu.UPGRADE), "Remote action accepted");
            p.player.setPos(menu.blockPos().getX(), menu.blockPos().getY(), menu.blockPos().getZ());
            p.player.containerMenu = p.player.inventoryMenu;
            h.assertTrue(!menu.clickMenuButton(p.player, EppServiceMenu.UPGRADE), "Closed menu accepted action");
            p.player.containerMenu = menu;
            h.getLevel().removeBlock(menu.blockPos(), false);
            h.assertTrue(!menu.stillValid(p.player) && !menu.clickMenuButton(p.player, EppServiceMenu.UPGRADE), "Removed block accepted action");
            h.assertTrue(menu.getSlot(2).getItem().getCount() == 1 && EppItem.generation(menu.getSlot(0).getItem()) == 1, "Rejected action consumed items");
        }
        h.succeed();
    }
    @GameTest(template = "shuttle_test_empty")
    public static void serviceTraysAreIndependentAndCloseReturnsAllInputs(GameTestHelper h) {
        try (var a = player(h.getLevel(), "TrayAlice"); var b = player(h.getLevel(), "TrayBob")) {
            var first = service(h, a.player); var second = service(h, b.player);
            a.player.getInventory().setItem(9, pack(222));
            h.assertTrue(!first.quickMoveStack(a.player, 3).isEmpty(), "Shift-click did not move EPP into tray");
            h.assertTrue(second.getSlot(0).getItem().isEmpty(), "Players shared service input");
            first.getSlot(1).set(new ItemStack(ModItems.HEATING_MODULE_1.get()));
            first.getSlot(2).set(new ItemStack(ModItems.EPP_MK2_UPGRADE_KIT.get()));
            first.removed(a.player); first.removed(a.player);
            h.assertTrue(a.player.getInventory().countItem(ModItems.EPP_MK1.get()) == 1, "Close lost or duplicated pack");
            h.assertTrue(a.player.getInventory().countItem(ModItems.HEATING_MODULE_1.get()) == 1
                    && a.player.getInventory().countItem(ModItems.EPP_MK2_UPGRADE_KIT.get()) == 1, "Close lost service materials");
            h.assertTrue(b.player.getInventory().countItem(ModItems.EPP_MK1.get()) == 0, "Return leaked to second player");
        }
        h.succeed();
    }
    @GameTest(template = "shuttle_test_empty")
    public static void coldProtectionRecoveryAndPersistenceAreIndependentOfOxygen(GameTestHelper h) {
        try (var p = player(h.getLevel(), "ColdExplorer"); var restored = player(h.getLevel(), "ColdReload")) {
            p.player.setPos(-30002, 64, 0);
            var pack = new ItemStack(ModItems.EPP_MK2.get()); EppItem.setOxygen(pack, 888);
            p.player.setData(ModAttachments.EPP_EQUIPMENT, pack);
            for (int i = 0; i < 30; i++) EppEvents.tickSecond(p.player);
            int exposure = p.player.getData(ModAttachments.COLD_EXPOSURE);
            h.assertTrue(exposure == 60 && EppItem.oxygen(pack) == 888, "Cold consumed oxygen or failed to accumulate");
            h.assertTrue(p.player.hasEffect(net.minecraft.world.effect.MobEffects.MOVEMENT_SLOWDOWN), "Cold symptoms not applied");
            var heating = new ItemStack(ModItems.HEATING_MODULE_1.get());
            pack.set(com.starboundmc.item.ModDataComponents.EPP_MODULES,
                    net.minecraft.world.item.component.ItemContainerContents.fromItems(java.util.List.of(heating)));
            EppEvents.tickSecond(p.player);
            h.assertTrue(p.player.getData(ModAttachments.COLD_EXPOSURE) == 55, "Equal protection did not recover");
            restored.player.load(p.player.saveWithoutId(new CompoundTag()));
            var restoredPack = restored.player.getData(ModAttachments.EPP_EQUIPMENT);
            h.assertTrue(EppItem.oxygen(restoredPack) == 888 && EppProtection.from(restoredPack).coldTier() == 1, "Module or oxygen lost on NBT round-trip");
            h.assertTrue(restored.player.getData(ModAttachments.COLD_EXPOSURE) == 55, "Relog reset exposure");
            p.player.setData(ModAttachments.EPP_EQUIPMENT, ItemStack.EMPTY);
            EppEvents.tickSecond(p.player);
            h.assertTrue(p.player.getData(ModAttachments.COLD_EXPOSURE) == 57, "Removing EPP retained protection");
            p.player.setPos(-30001, 64, 0); EppEvents.tickSecond(p.player);
            h.assertTrue(p.player.getData(ModAttachments.COLD_EXPOSURE) == 52, "Safe cabin failed to recover");
            p.player.setGameMode(net.minecraft.world.level.GameType.CREATIVE); EppEvents.tickSecond(p.player);
            h.assertTrue(p.player.getData(ModAttachments.COLD_EXPOSURE) == 0, "Creative player accumulated exposure");
        }
        h.succeed();
    }
    @GameTest(template = "shuttle_test_empty")
    public static void incompatibleModulesAreInactiveButNeverDestroyed(GameTestHelper h) {
        try (var p = player(h.getLevel(), "EppRecovery")) {
            var menu = service(h, p.player); var old = pack(123);
            var unsupported = new ItemStack(Items.DIAMOND);
            old.set(com.starboundmc.item.ModDataComponents.EPP_MODULES,
                    net.minecraft.world.item.component.ItemContainerContents.fromItems(java.util.List.of(unsupported)));
            menu.getSlot(0).set(old); menu.getSlot(2).set(new ItemStack(ModItems.EPP_MK2_UPGRADE_KIT.get()));
            h.assertTrue(EppProtection.from(old).coldTier() == 0, "Mk.I granted module protection");
            h.assertTrue(menu.clickMenuButton(p.player, EppServiceMenu.UPGRADE), "Upgrade rejected retained component data");
            h.assertTrue(EppProtection.from(menu.getSlot(0).getItem()).coldTier() == 0, "Unsupported module granted protection");
            h.assertTrue(p.player.getInventory().countItem(Items.DIAMOND) == 1, "Upgrade did not automatically return unsupported contents");
            h.assertTrue(!menu.canRemove(), "Returned contents still present inside pack");
        }
        h.succeed();
    }
    @GameTest(template = "shuttle_test_empty")
    public static void severeColdDamagesOnlyWhileUnprotected(GameTestHelper h) {
        try (var p = player(h.getLevel(), "ColdDamage")) {
            // Exercise real damage after vanilla's initial login invulnerability expires.
            for (int i = 0; i < 61; i++) p.player.tick();
            p.player.setPos(-30002, 64, 0); p.player.invulnerableTime = 0;
            p.player.setData(ModAttachments.COLD_EXPOSURE, 98);
            float before = p.player.getHealth(); EppEvents.tickSecond(p.player);
            h.assertTrue(p.player.getHealth() < before, "Severe unprotected cold did not damage player");
            p.player.setPos(-30001, 64, 0); p.player.invulnerableTime = 0;
            before = p.player.getHealth(); EppEvents.tickSecond(p.player);
            h.assertTrue(p.player.getHealth() == before, "Cold kept damaging in safe cabin");
        }
        h.succeed();
    }
    @GameTest(template = "shuttle_test_empty")
    public static void extraEquipmentSourcesNeverStackTanks(GameTestHelper h) {
        try (var p = player(h.getLevel(), "EppResolver")) {
            p.player.setPos(-30000, 64, 0);
            p.player.addTag("epp_resolver_test");
            var nativePack = pack(100); var externalPack = pack(600);
            p.player.setData(ModAttachments.EPP_EQUIPMENT, nativePack);
            p.player.getInventory().setItem(35, externalPack);
            h.assertTrue(EppEquipmentResolver.getActiveEpp(p.player).orElseThrow() == nativePack, "Native priority not deterministic");
            EppEvents.tickSecond(p.player);
            h.assertTrue(EppItem.oxygen(nativePack) == 99 && EppItem.oxygen(externalPack) == 600, "Tanks were combined or both consumed");
            p.player.setData(ModAttachments.EPP_EQUIPMENT, ItemStack.EMPTY);
            EppEvents.tickSecond(p.player);
            h.assertTrue(EppItem.oxygen(externalPack) == 599, "External source did not take over");
        }
        h.succeed();
    }
    @GameTest(template = "shuttle_test_empty")
    public static void independentTanksAndRestartKeepOxygenAndGrace(GameTestHelper h) {
        try (var a = player(h.getLevel(), "EppAlice"); var b = player(h.getLevel(), "EppBob"); var restored = player(h.getLevel(), "EppReload")) {
            a.player.setPos(-30000, 64, 0); b.player.setPos(-30000, 64, 0);
            a.player.setData(ModAttachments.EPP_EQUIPMENT, pack(100)); b.player.setData(ModAttachments.EPP_EQUIPMENT, pack(50));
            EppEvents.tickSecond(a.player);
            h.assertTrue(EppItem.oxygen(a.player.getData(ModAttachments.EPP_EQUIPMENT)) == 99, "Active pack did not consume");
            h.assertTrue(EppItem.oxygen(b.player.getData(ModAttachments.EPP_EQUIPMENT)) == 50, "Another player's tank changed");
            a.player.setData(ModAttachments.SUFFOCATION, 9);
            CompoundTag saved = a.player.saveWithoutId(new CompoundTag()); restored.player.load(saved);
            h.assertTrue(EppItem.oxygen(restored.player.getData(ModAttachments.EPP_EQUIPMENT)) == 99, "Tank reset on reload");
            h.assertTrue(restored.player.getData(ModAttachments.SUFFOCATION) == 9, "Grace reset on reload");
            h.assertTrue(b.player.getData(ModAttachments.SUFFOCATION) == 0, "Exposure leaked between players");
        }
        h.succeed();
    }
    @GameTest(template = "shuttle_test_empty")
    public static void equipmentMenuMovesExactlyOnePackAndRejectsArmor(GameTestHelper h) {
        try (var p = player(h.getLevel(), "EppSlots")) {
            var menu = new EppMenu(1, p.player.getInventory()); p.player.containerMenu = menu;
            p.player.getInventory().setItem(9, pack(321));
            h.assertTrue(!menu.quickMoveStack(p.player, 1).isEmpty(), "Shift equip failed");
            h.assertTrue(p.player.getInventory().getItem(9).isEmpty(), "Equip duplicated source");
            h.assertTrue(EppItem.oxygen(p.player.getData(ModAttachments.EPP_EQUIPMENT)) == 321, "Equip reset oxygen");
            h.assertTrue(!menu.getSlot(0).mayPlace(new ItemStack(Items.DIAMOND_CHESTPLATE)), "Armor accepted in EPP slot");
            menu.quickMoveStack(p.player, 0);
            h.assertTrue(p.player.getData(ModAttachments.EPP_EQUIPMENT).isEmpty(), "Unequip left a second pack");
            h.assertTrue(p.player.getInventory().countItem(ModItems.EPP_MK1.get()) == 1, "Unequip duplicated/lost pack");
            h.assertTrue(menu.slots.size() == 37, "Unexpected slot count");
        }
        h.succeed();
    }
    @GameTest(template = "shuttle_test_empty")
    public static void canisterCapacityAndEmptyReturnAreAtomic(GameTestHelper h) {
        try (var p = player(h.getLevel(), "EppCanister")) {
            var canister = new ItemStack(ModItems.OXYGEN_CANISTER.get());
            h.assertTrue(!OxygenCanisterItem.canUse(p.player), "Canister accepted without EPP");
            var pack = pack(541); p.player.setData(ModAttachments.EPP_EQUIPMENT, pack);
            h.assertTrue(!OxygenCanisterItem.canUse(p.player), "Partial canister room accepted");
            EppItem.setOxygen(pack, 540);
            var result = canister.finishUsingItem(h.getLevel(), p.player);
            h.assertTrue(result.is(ModItems.EMPTY_OXYGEN_CANISTER.get()), "Empty canister not returned");
            h.assertTrue(EppItem.oxygen(pack) == 720, "Wrong oxygen transfer");
            h.assertTrue(!OxygenCanisterItem.canUse(p.player), "Full EPP accepts replay");
            h.assertTrue(canister.getMaxStackSize() == 1 && result.getMaxStackSize() == 8, "Incorrect stack limits");
        }
        h.succeed();
    }
    @GameTest(template = "shuttle_test_empty")
    public static void stationProgressAndContentsSurviveReload(GameTestHelper h) {
        BlockPos pos = h.absolutePos(new BlockPos(1, 1, 1));
        h.getLevel().setBlockAndUpdate(pos, ModBlocks.LIFE_SUPPORT_STATION.get().defaultBlockState());
        var station = (LifeSupportStationEntity) h.getLevel().getBlockEntity(pos);
        station.setItem(0, new ItemStack(ModItems.EMPTY_OXYGEN_CANISTER.get()));
        for (int i = 0; i < EppConfig.FILL_TICKS / 2; i++) LifeSupportStationEntity.tick(h.getLevel(), pos, station.getBlockState(), station);
        var saved = station.saveWithFullMetadata(h.getLevel().registryAccess());
        station.loadWithComponents(saved, h.getLevel().registryAccess());
        h.assertTrue(station.progress() == EppConfig.FILL_TICKS / 2, "Filling progress lost");
        for (int i = 0; i < EppConfig.FILL_TICKS / 2; i++) LifeSupportStationEntity.tick(h.getLevel(), pos, station.getBlockState(), station);
        h.assertTrue(station.getItem(0).is(ModItems.OXYGEN_CANISTER.get()), "Filling did not produce oxygen");
        for (int i = 0; i < EppConfig.FILL_TICKS; i++) LifeSupportStationEntity.tick(h.getLevel(), pos, station.getBlockState(), station);
        h.assertTrue(station.getItem(0).getCount() == 1 && station.progress() == 0, "Station duplicated a full canister");
        try (var a = player(h.getLevel(), "FillAlice"); var b = player(h.getLevel(), "FillBob")) {
            a.player.setPos(pos.getX(), pos.getY(), pos.getZ()); b.player.setPos(pos.getX(), pos.getY(), pos.getZ());
            var first = new LifeSupportMenu(1, a.player.getInventory(), station);
            var second = new LifeSupportMenu(2, b.player.getInventory(), station);
            first.quickMoveStack(a.player, 0); second.quickMoveStack(b.player, 0);
            h.assertTrue(a.player.getInventory().countItem(ModItems.OXYGEN_CANISTER.get()) + b.player.getInventory().countItem(ModItems.OXYGEN_CANISTER.get()) == 1, "Two viewers took the same canister");
            h.getLevel().removeBlock(pos, false);
            h.assertTrue(!first.stillValid(a.player), "Removed station menu remains valid");
        }
        h.succeed();
    }
    @GameTest(template = "shuttle_test_empty")
    public static void deathRespectsKeepInventoryWithoutDuplicatingPack(GameTestHelper h) {
        var rule = h.getLevel().getGameRules().getRule(GameRules.RULE_KEEPINVENTORY);
        boolean previous = rule.get();
        try (var p = player(h.getLevel(), "EppDeath")) {
            p.player.setData(ModAttachments.EPP_EQUIPMENT, pack(234));
            var drops = new ArrayList<net.minecraft.world.entity.item.ItemEntity>();
            rule.set(true, h.getLevel().getServer());
            EppEvents.drops(new LivingDropsEvent(p.player, p.player.damageSources().generic(), drops, false));
            h.assertTrue(drops.isEmpty() && !p.player.getData(ModAttachments.EPP_EQUIPMENT).isEmpty(), "keepInventory lost pack");
            rule.set(false, h.getLevel().getServer());
            EppEvents.drops(new LivingDropsEvent(p.player, p.player.damageSources().generic(), drops, false));
            EppEvents.drops(new LivingDropsEvent(p.player, p.player.damageSources().generic(), drops, false));
            h.assertTrue(drops.size() == 1 && p.player.getData(ModAttachments.EPP_EQUIPMENT).isEmpty(), "Death duplicated pack");
            h.assertTrue(EppItem.oxygen(drops.getFirst().getItem()) == 234, "Death reset oxygen");
        } finally { rule.set(previous, h.getLevel().getServer()); }
        h.succeed();
    }
    @GameTest(template = "shuttle_test_empty")
    public static void controlledZoneRefillsButOrdinaryAirOnlyStopsConsumption(GameTestHelper h) {
        try (var lush = player(h.getLevel(), "EppAir"); var ship = player(h.getLevel(), "EppCabin")) {
            lush.player.setData(ModAttachments.EPP_EQUIPMENT, pack(100)); ship.player.setData(ModAttachments.EPP_EQUIPMENT, pack(100));
            ship.player.setPos(-30001, 64, 0);
            EppEvents.tickSecond(lush.player); EppEvents.tickSecond(ship.player);
            h.assertTrue(EppItem.oxygen(lush.player.getData(ModAttachments.EPP_EQUIPMENT)) == 100, "Lush refilled oxygen for free");
            h.assertTrue(EppItem.oxygen(ship.player.getData(ModAttachments.EPP_EQUIPMENT)) == Math.min(EppConfig.CAPACITY.get(), 100 + EppConfig.REFILL.get()), "Cabin did not refill");
            ship.player.setPos(-30000, 64, 0); EppEvents.tickSecond(ship.player);
            h.assertTrue(EppItem.oxygen(ship.player.getData(ModAttachments.EPP_EQUIPMENT)) == Math.max(0, Math.min(EppConfig.CAPACITY.get(), 100 + EppConfig.REFILL.get()) - EppConfig.CONSUMPTION.get()), "Ship exterior treated as cabin");
        }
        h.succeed();
    }
}
