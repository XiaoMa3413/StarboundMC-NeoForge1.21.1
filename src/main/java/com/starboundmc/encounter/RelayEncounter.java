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
        if (data.phase != RelayData.Phase.AVAILABLE || ShipWarpManager.isWarping()) return;
        if (!ShipEnvironmentService.canTravelWithinSystem(server)) { tell(player, "engines_offline"); return; }
        if (hasOutsideCrew(server, data)) { tell(player, "crew_outside"); return; }
        if (!Objects.equals(ShipWarpManager.currentEntryId(), data.homeBody)) {
            if (ShipWarpManager.startWarp(player, data.homeBody)) {
                data.phase = RelayData.Phase.ROUTING; data.setDirty(); broadcast(server, data);
            }
        } else prepare(player.serverLevel(), data);
    }
    public static void tick(MinecraftServer server) {
        var level = server.getLevel(ShipDimensions.SHIP_LEVEL); if (level == null) return;
        var data = RelayData.get(server);
        if (data.phase == RelayData.Phase.UNDISCOVERED && ShipEnvironmentService.canTravelWithinSystem(server)
                && !ShipWarpManager.isWarping()) {
            data.homeBody = ShipWarpManager.currentEntryId(); data.phase = RelayData.Phase.AVAILABLE; data.setDirty();
            announce(level, "discovered"); broadcast(server, data);
        }
        if (data.phase == RelayData.Phase.ROUTING && !ShipWarpManager.isWarping()) {
            if (Objects.equals(ShipWarpManager.currentEntryId(), data.homeBody)) prepare(level, data);
            else { data.phase = RelayData.Phase.AVAILABLE; data.setDirty(); broadcast(server, data); }
        }
        if (data.phase == RelayData.Phase.APPROACHING) {
            refreshCrew(server, data);
            if (!hasOutsideCrew(server, data)) {
                data.approachTicks++; data.setDirty();
                if (data.approachTicks >= APPROACH_TICKS) materialize(level, data);
            }
            if (level.getGameTime() % 10 == 0) broadcast(server, data);
        }
        if (data.phase == RelayData.Phase.ACTIVE && level.getGameTime() % 10 == 0) {
            refreshCrew(server, data);
            for (var player : level.players()) {
                if (!player.isAlive() || player.isSpectator()) continue;
                if (!data.recovered && player.getInventory().contains(new net.minecraft.world.item.ItemStack(ModItems.RELAY_DATA_CORE.get()))) {
                    data.recovered = true; data.setDirty(); announce(level, "recovered");
                }
            }
            if (data.recovered && !data.completed && !hasOutsideCrew(server, data) && !level.players().isEmpty()) {
                data.completed = true; data.setDirty(); announce(level, "completed");
            }
            broadcast(server, data);
        }
    }
    static boolean prepare(ServerLevel level, RelayData data) {
        try {
            var blocks = RelayGeometry.survey(p -> !level.getBlockState(p).isAir());
            for (var origin : RelayGeometry.candidates(blocks)) {
                if (origin.getY() - RelayGeometry.MARGIN < level.getMinBuildHeight()
                        || origin.getY() + RelayGeometry.HEIGHT + RelayGeometry.MARGIN >= level.getMaxBuildHeight()
                        || !level.getWorldBorder().isWithinBounds(RelayGeometry.bounds(origin).inflate(RelayGeometry.MARGIN))) continue;
                if (RelayGeometry.clear(origin, RelayGeometry.MARGIN, p -> !level.getBlockState(p).isAir())) {
                    var template = RelayStructure.template(level, data.snapshot);
                    if (data.snapshot.isEmpty()) data.snapshot = template.save(new net.minecraft.nbt.CompoundTag());
                    data.origin = origin; data.approachTicks = 0; data.phase = RelayData.Phase.APPROACHING;
                    data.setDirty(); announce(level, "approaching"); broadcast(level.getServer(), data); return true;
                }
            }
        } catch (IllegalStateException error) {
            com.mojang.logging.LogUtils.getLogger().warn("Relay approach refused: {}", error.getMessage());
        }
        data.phase = RelayData.Phase.AVAILABLE; data.setDirty(); announce(level, "no_space"); broadcast(level.getServer(), data);
        return false;
    }
    static void materialize(ServerLevel level, RelayData data) {
        // Persist an interruption marker before touching the world. A crash during placement is
        // quarantined on restart rather than replaying loot or overwriting partially placed blocks.
        if (!RelayGeometry.clear(data.origin, RelayGeometry.MARGIN, p -> !level.getBlockState(p).isAir())
                || !level.getEntities(null, RelayGeometry.bounds(data.origin).inflate(RelayGeometry.MARGIN)).isEmpty()) {
            data.phase = RelayData.Phase.AVAILABLE; data.setDirty(); announce(level, "no_space"); broadcast(level.getServer(), data); return;
        }
        data.phase = RelayData.Phase.MATERIALIZING; data.transaction = 1; durable(level.getServer(), data);
        if (!RelayStructure.place(level, data.origin, data.snapshot)) {
            data.phase = RelayData.Phase.ERROR; data.setDirty(); announce(level, "interrupted"); return;
        }
        // Save chunks before marking ACTIVE: a restart may never reseed a looted barrel.
        level.getChunkSource().save(true);
        data.phase = RelayData.Phase.ACTIVE; data.transaction = 0; durable(level.getServer(), data);
        announce(level, "arrived"); broadcast(level.getServer(), data);
    }
    public static boolean beforeWarp(ServerPlayer player) {
        var data = RelayData.get(player.getServer());
        if (data.phase == RelayData.Phase.ACTIVE) return leave(player, data);
        if (data.phase != RelayData.Phase.AVAILABLE && data.phase != RelayData.Phase.UNDISCOVERED) {
            tell(player, "busy"); return false;
        }
        return true;
    }
    private static boolean leave(ServerPlayer player, RelayData data) {
        if (data.phase != RelayData.Phase.ACTIVE) return false;
        var server = player.getServer(); var level = player.serverLevel(); refreshCrew(server, data);
        if (hasOutsideCrew(server, data)) { tell(player, "crew_outside"); return false; }
        if (RelayStructure.touchesBoundary(level, data.origin)) { tell(player, "extension"); return false; }
        // Items, mobs and vehicles must not be deleted or duplicated with a block-only snapshot.
        if (!level.getEntities(null, RelayGeometry.bounds(data.origin)).isEmpty()) { tell(player, "entities"); return false; }
        data.snapshot = RelayStructure.capture(level, data.origin);
        data.phase = RelayData.Phase.LEAVING; data.transaction = 2; durable(server, data);
        RelayStructure.clear(level, data.origin);
        level.getChunkSource().save(true);
        data.phase = RelayData.Phase.AVAILABLE; data.transaction = 0; data.approachTicks = 0; durable(server, data);
        announce(level, "departed"); broadcast(server, data); return true;
    }
    static void refreshCrew(MinecraftServer server, RelayData data) {
        if (data.phase != RelayData.Phase.ACTIVE && data.phase != RelayData.Phase.APPROACHING) return;
        boolean changed = false;
        for (var player : server.getPlayerList().getPlayers()) {
            boolean outside = ShipCrewSafety.blocksDeparture(player);
            changed |= outside ? data.outsideCrew.add(player.getUUID()) : data.outsideCrew.remove(player.getUUID());
        }
        if (changed) data.setDirty();
    }
    private static boolean hasOutsideCrew(MinecraftServer server, RelayData data) {
        var level = server.getLevel(ShipDimensions.SHIP_LEVEL);
        return !data.outsideCrew.isEmpty() || level != null && ShipCrewSafety.hasOutsideCrew(level.players());
    }
    @SubscribeEvent public static void logout(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            LAST_REQUEST.remove(player.getUUID());
            var data = RelayData.get(player.getServer());
            if ((data.phase == RelayData.Phase.ACTIVE || data.phase == RelayData.Phase.APPROACHING)
                    && ShipCrewSafety.blocksDeparture(player)) { data.outsideCrew.add(player.getUUID()); durable(player.getServer(), data); }
            else if (data.outsideCrew.remove(player.getUUID())) durable(player.getServer(), data);
        }
    }
    @SubscribeEvent public static void died(net.neoforged.neoforge.event.entity.living.LivingDeathEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            var data = RelayData.get(player.getServer());
            if (data.outsideCrew.remove(player.getUUID())) data.setDirty();
        }
    }
    @SubscribeEvent public static void recover(ServerStartedEvent event) {
        LAST_REQUEST.clear();
        var data = RelayData.get(event.getServer());
        if (data.transaction != 0) recoverTransaction(event.getServer(), data);
    }
    static boolean recoverTransaction(MinecraftServer server, RelayData data) {
        var level = server.getLevel(ShipDimensions.SHIP_LEVEL);
        if (level == null || data.transaction == 0) return false;
        try {
            if (RelayStructure.recover(level, data.origin, data.snapshot, data.transaction == 2)) {
                level.getChunkSource().save(true);
                data.phase = data.transaction == 2 ? RelayData.Phase.AVAILABLE : RelayData.Phase.ACTIVE;
                data.transaction = 0; durable(server, data); broadcast(server, data); return true;
            }
        } catch (RuntimeException error) { com.mojang.logging.LogUtils.getLogger().error("Relay recovery failed", error); }
        data.phase = RelayData.Phase.ERROR; durable(server, data);
        com.mojang.logging.LogUtils.getLogger().error("Relay ownership conflict at {}. Site and snapshot preserved. Resolve conflicting blocks/containers, then /starboundmc_relay_recover", data.origin);
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
    private static void durable(MinecraftServer server, RelayData data) { data.setDirty(); server.overworld().getDataStorage().save(); }
    public static void sync(ServerPlayer player) { ModNetwork.sendToPlayer(player, packet(RelayData.get(player.getServer()))); }
    private static RelaySnapshotPacket packet(RelayData data) {
        return new RelaySnapshotPacket(data.phase.ordinal(), data.origin, data.approachTicks, data.homeBody, data.recovered, data.completed, data.outsideCrew.size());
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
