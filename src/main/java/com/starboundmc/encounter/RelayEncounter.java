// SPDX-License-Identifier: MPL-2.0
package com.starboundmc.encounter;

import com.starboundmc.StarboundMC;
import com.starboundmc.item.ModItems;
import com.starboundmc.menu.StarmapTerminalMenu;
import com.starboundmc.network.*;
import com.starboundmc.story.ShipEnvironmentService;
import com.starboundmc.warp.ShipCrewSafety;
import com.starboundmc.warp.ShipWarpManager;
import com.starboundmc.world.ShipDimensions;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import java.util.Objects;

/** One relay encounter, deliberately separate from the celestial catalog and planet travel. */
@EventBusSubscriber(modid = StarboundMC.MODID)
public final class RelayEncounter {
    public static final int APPROACH_TICKS = 160;
    private static final java.util.Map<java.util.UUID, Integer> LAST_REQUEST = new java.util.HashMap<>();
    private RelayEncounter() { }
    public static void request(ServerPlayer player, int containerId, boolean leave) {
        if (!player.isAlive() || player.isSpectator()
                || !(player.containerMenu instanceof StarmapTerminalMenu menu)
                || menu.containerId != containerId || !menu.stillValid(player)
                || !ShipCrewSafety.isAboard(player)) return;
        var server = player.getServer(); var data = RelayData.get(server);
        int now = server.getTickCount(); var previous = LAST_REQUEST.get(player.getUUID());
        if (previous != null && now - previous < 20) return;
        LAST_REQUEST.put(player.getUUID(), now);
        refreshCrew(server, data);
        if (leave) { leave(player, data); return; }
        if (data.phase() != RelayData.Phase.AVAILABLE || ShipWarpManager.isWarping()) return;
        if (!ShipEnvironmentService.canTravelWithinSystem(server)) { tell(player, "engines_offline"); return; }
        if (hasOutsideCrew(server, data)) { tell(player, "crew_outside"); return; }
        if (!Objects.equals(ShipWarpManager.currentEntryId(), data.homeBody())) {
            if (ShipWarpManager.startWarp(player, data.homeBody())) {
                data.beginRouting(); broadcast(server, data);
            }
        } else prepare(player.serverLevel(), data);
    }
    public static void tick(MinecraftServer server) {
        var level = server.getLevel(ShipDimensions.SHIP_LEVEL); if (level == null) return;
        var data = RelayData.get(server);
        if (data.phase() == RelayData.Phase.UNDISCOVERED && ShipEnvironmentService.canTravelWithinSystem(server)
                && !ShipWarpManager.isWarping()) {
            data.discover(ShipWarpManager.currentEntryId());
            announce(level, "discovered"); broadcast(server, data);
        }
        if (data.phase() == RelayData.Phase.ROUTING && !ShipWarpManager.isWarping()) {
            if (Objects.equals(ShipWarpManager.currentEntryId(), data.homeBody())) prepare(level, data);
            else { data.cancelApproach(); broadcast(server, data); }
        }
        if (data.phase() == RelayData.Phase.APPROACHING) {
            refreshCrew(server, data);
            if (!hasOutsideCrew(server, data)) {
                if (data.advanceApproach()) materialize(level, data);
            }
            if (level.getGameTime() % 10 == 0) broadcast(server, data);
        }
        if (data.phase() == RelayData.Phase.ACTIVE && level.getGameTime() % 10 == 0) {
            refreshCrew(server, data);
            for (var player : level.players()) {
                if (!player.isAlive() || player.isSpectator()) continue;
                if (!data.recovered() && player.getInventory().contains(new net.minecraft.world.item.ItemStack(ModItems.RELAY_DATA_CORE.get()))) {
                    data.markRecovered(); announce(level, "recovered");
                }
            }
            if (data.recovered() && !data.completed() && !hasOutsideCrew(server, data) && !level.players().isEmpty()) {
                data.markCompleted(); announce(level, "completed");
            }
            broadcast(server, data);
        }
    }
    static boolean prepare(ServerLevel level, RelayData data) {
        java.util.List<net.minecraft.core.BlockPos> blocks;
        try {
            blocks = RelayGeometry.survey(p -> !level.getBlockState(p).isAir());
        } catch (IllegalStateException surveyRefusal) {
            com.mojang.logging.LogUtils.getLogger().warn("Relay approach refused: {}", surveyRefusal.getMessage());
            data.cancelApproach(); announce(level, "no_space"); broadcast(level.getServer(), data);
            return false;
        }
        for (var origin : RelayGeometry.candidates(blocks, com.starboundmc.epp.EppConfig.RELAY_EVA_GAP.get())) {
            if (origin.getY() - RelayGeometry.MARGIN < level.getMinBuildHeight()
                    || origin.getY() + RelayGeometry.HEIGHT + RelayGeometry.MARGIN >= level.getMaxBuildHeight()
                    || !level.getWorldBorder().isWithinBounds(RelayGeometry.bounds(origin).inflate(RelayGeometry.MARGIN))) continue;
            if (RelayGeometry.clear(origin, RelayGeometry.MARGIN, p -> !level.getBlockState(p).isAir())) {
                var template = RelayStructure.template(level, data.snapshot());
                data.beginApproach(origin, template.save(new net.minecraft.nbt.CompoundTag()));
                announce(level, "approaching"); broadcast(level.getServer(), data);
                return true;
            }
        }
        data.cancelApproach(); announce(level, "no_space"); broadcast(level.getServer(), data);
        return false;
    }
    static void materialize(ServerLevel level, RelayData data) {
        // Persist an interruption marker before touching the world. A crash during placement is
        // quarantined on restart rather than replaying loot or overwriting partially placed blocks.
        if (!RelayGeometry.clear(data.origin(), RelayGeometry.MARGIN, p -> !level.getBlockState(p).isAir())
                || !level.getEntities(null, RelayGeometry.bounds(data.origin()).inflate(RelayGeometry.MARGIN)).isEmpty()) {
            data.cancelApproach(); announce(level, "no_space"); broadcast(level.getServer(), data); return;
        }
        data.beginMaterialization(level.getServer());
        if (!RelayStructure.place(level, data.origin(), data.snapshot())) {
            data.markRecoveryError(level.getServer()); announce(level, "interrupted"); return;
        }
        // Save chunks before marking ACTIVE: a restart may never reseed a looted barrel.
        level.getChunkSource().save(true);
        data.finishMaterialization(level.getServer());
        announce(level, "arrived"); broadcast(level.getServer(), data);
    }
    public static boolean beforeWarp(ServerPlayer player) {
        var data = RelayData.get(player.getServer());
        if (data.phase() == RelayData.Phase.ACTIVE) return leave(player, data);
        if (data.phase() != RelayData.Phase.AVAILABLE && data.phase() != RelayData.Phase.UNDISCOVERED) {
            tell(player, "busy"); return false;
        }
        return true;
    }
    private static boolean leave(ServerPlayer player, RelayData data) {
        if (data.phase() != RelayData.Phase.ACTIVE) return false;
        var server = player.getServer(); var level = player.serverLevel(); refreshCrew(server, data);
        if (hasOutsideCrew(server, data)) { tell(player, "crew_outside"); return false; }
        if (RelayStructure.touchesBoundary(level, data.origin())) { tell(player, "extension"); return false; }
        // Items, mobs and vehicles must not be deleted or duplicated with a block-only snapshot.
        if (!level.getEntities(null, RelayGeometry.bounds(data.origin())).isEmpty()) { tell(player, "entities"); return false; }
        data.beginDeparture(server, RelayStructure.capture(level, data.origin()));
        RelayStructure.clear(level, data.origin());
        level.getChunkSource().save(true);
        data.finishDeparture(server);
        announce(level, "departed"); broadcast(server, data); return true;
    }
    static void refreshCrew(MinecraftServer server, RelayData data) {
        if (data.phase() != RelayData.Phase.ACTIVE && data.phase() != RelayData.Phase.APPROACHING) return;
        for (var player : server.getPlayerList().getPlayers()) {
            boolean outside = ShipCrewSafety.blocksDeparture(player);
            data.setCrewOutside(player.getUUID(), outside);
        }
    }
    private static boolean hasOutsideCrew(MinecraftServer server, RelayData data) {
        var level = server.getLevel(ShipDimensions.SHIP_LEVEL);
        return !data.outsideCrew().isEmpty() || level != null && ShipCrewSafety.hasOutsideCrew(level.players());
    }
    @SubscribeEvent public static void logout(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            LAST_REQUEST.remove(player.getUUID());
            var data = RelayData.get(player.getServer());
            boolean outside = (data.phase() == RelayData.Phase.ACTIVE || data.phase() == RelayData.Phase.APPROACHING)
                    && ShipCrewSafety.blocksDeparture(player);
            data.recordDisconnectedCrew(player.getServer(), player.getUUID(), outside);
        }
    }
    @SubscribeEvent public static void died(net.neoforged.neoforge.event.entity.living.LivingDeathEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            var data = RelayData.get(player.getServer());
            data.setCrewOutside(player.getUUID(), false);
        }
    }
    @SubscribeEvent public static void recover(ServerStartedEvent event) {
        LAST_REQUEST.clear();
        var data = RelayData.get(event.getServer());
        if (data.transaction() != 0) recoverTransaction(event.getServer(), data);
    }
    static boolean recoverTransaction(MinecraftServer server, RelayData data) {
        var level = server.getLevel(ShipDimensions.SHIP_LEVEL);
        if (level == null || data.transaction() == 0) return false;
        try {
            if (RelayStructure.recover(level, data.origin(), data.snapshot(), data.transaction() == 2)) {
                level.getChunkSource().save(true);
                data.finishRecovery(server); broadcast(server, data); return true;
            }
        } catch (RuntimeException error) { com.mojang.logging.LogUtils.getLogger().error("Relay recovery failed", error); }
        data.markRecoveryError(server);
        com.mojang.logging.LogUtils.getLogger().error("Relay ownership conflict at {}. Site and snapshot preserved. Resolve conflicting blocks/containers, then /starboundmc_relay_recover", data.origin());
        return false;
    }
    @SubscribeEvent public static void commands(net.neoforged.neoforge.event.RegisterCommandsEvent event) {
        event.getDispatcher().register(net.minecraft.commands.Commands.literal("starboundmc_relay_recover")
                .requires(source -> source.hasPermission(2)).executes(context -> {
                    var source = context.getSource();
                    boolean recovered = recoverTransaction(source.getServer(), RelayData.get(source.getServer()));
                    source.sendSuccess(() -> Component.literal(recovered ? "Relay transaction recovered." : "No recoverable transaction; check the server log for conflicts."), true);
                    return recovered ? 1 : 0;
                }));
    }
    public static void sync(ServerPlayer player) { ModNetwork.sendToPlayer(player, packet(RelayData.get(player.getServer()))); }
    private static RelaySnapshotPacket packet(RelayData data) {
        return new RelaySnapshotPacket(data.phase(), data.origin(), data.approachTicks(), data.homeBody(), data.recovered(), data.completed(), data.outsideCrew().size());
    }
    private static void broadcast(MinecraftServer server, RelayData data) {
        for (var p : server.getPlayerList().getPlayers()) ModNetwork.sendToPlayer(p, packet(data));
    }
    private static void announce(ServerLevel level, String key) {
        // N.O.V.A. packets deliberately accept only the nova namespace; relay
        // keys remain ordinary chat/status translations used by tell().
        var packet = new NovaBroadcastPacket("message.starboundmc.nova.relay." + key);
        for (var p : level.players()) ModNetwork.sendToPlayer(p, packet);
    }
    private static void tell(ServerPlayer player, String key) { player.displayClientMessage(Component.translatable("message.starboundmc.relay." + key), true); }
}
