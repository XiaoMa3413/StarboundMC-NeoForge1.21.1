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
    static {
        EppEquipmentResolver.registerSource("starboundmc:gametest_extra", player ->
                player.getTags().contains("epp_resolver_test") ? player.getInventory().getItem(35) : ItemStack.EMPTY);
        // GameTestServer supplies only vanilla dimensions. Inject two spatial fixtures through the
        // same public zone API used by future stations, while exercising real players and items.
        PlayerEnvironmentService.registerZone("starboundmc:gametest_zone", (level, pos) ->
                pos.getX() == -30000 ? java.util.Optional.of(EnvironmentState.SPACE)
                        : pos.getX() == -30001 ? java.util.Optional.of(EnvironmentState.SHIP_INTERIOR)
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
                com.starboundmc.network.EppVisualPacket.TYPE, com.starboundmc.network.NovaBroadcastPacket.TYPE)) {
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
