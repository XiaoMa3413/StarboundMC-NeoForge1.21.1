package com.starboundmc.story;

import com.starboundmc.item.ModItems;
import com.starboundmc.menu.ShipAiTerminalMenu;
import com.starboundmc.network.ModNetwork;
import com.starboundmc.network.NovaBroadcastPacket;
import com.starboundmc.warp.ShipStateData;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

/** Main-thread task evaluation. Never invokes printers, recipes or remote repairs. */
public final class NovaTaskService {
    private NovaTaskService() { }

    public static NovaTaskProgress refresh(ServerPlayer player) {
        NovaTaskProgress previous = player.getData(ModAttachments.NOVA_TASKS);
        if (player.isSpectator() || player.getServer() == null || !previous.writable()) return previous;
        SharedShipProgress ship = ShipStateData.get(player.getServer()).getStoryProgress();
        PlayerStoryState story = player.getData(ModAttachments.PLAYER_STORY);
        if (!ship.isWritable() || !story.isWritable()) return previous;
        boolean contacted = ship.core() == CoreState.ONLINE
                && (ship.surfaceMission() != SurfaceMissionState.LOCKED
                || story.identityConfirmed() && story.hasReadAllRequiredTopics());
        boolean visited = story.hasFlag(PlayerStoryFlag.SURFACE_ARRIVAL_BROADCAST)
                || story.hasSeenTutorial(TutorialTopic.MATTER_MANIPULATOR);
        boolean core = player.getInventory().contains(new ItemStack(ModItems.SUBLIGHT_IGNITION_CORE.get()));
        NovaTaskProgress next = previous.observe(contacted, visited, ShipStoryService.hasUpgradedManipulator(player),
                core, ship.sublightEngine() == EngineState.ONLINE);
        if (next != previous) {
            player.setData(ModAttachments.NOVA_TASKS, next);
            for (NovaTask task : NovaTask.values()) if (next.completed(task) && !previous.completed(task)
                    && task.reward() > 0) ModNetwork.sendToPlayer(player,
                    new NovaBroadcastPacket("message.starboundmc.nova.task." + task.key() + ".complete"));
        }
        return next;
    }

    public static void onSurfaceArrival(ServerPlayer player) {
        NovaTaskProgress previous = player.getData(ModAttachments.NOVA_TASKS);
        boolean online = ShipStateData.get(player.getServer()).getStoryProgress().sublightEngine() == EngineState.ONLINE;
        NovaTaskProgress next = previous.arrive(player.level().dimension().location().toString(), online);
        if (next != previous) player.setData(ModAttachments.NOVA_TASKS, next);
        refresh(player);
    }

    public static void tick(MinecraftServer server) {
        if (server.overworld().getGameTime() % 20 != 0) return;
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            NovaTaskProgress before = player.getData(ModAttachments.NOVA_TASKS);
            NovaTaskProgress after = refresh(player);
            if (before != after && player.containerMenu instanceof ShipAiTerminalMenu menu)
                ShipStoryService.sendSnapshot(player, menu.containerId);
        }
    }

    /** Caller has already validated terminal binding, range, spectator and story schemas. */
    public static void claim(ServerPlayer player, NovaTask task) {
        refresh(player);
        if (grantReward(player, task)) {
            ModNetwork.sendToPlayer(player, new NovaBroadcastPacket("message.starboundmc.nova.task.reward"));
        } else if (player.getData(ModAttachments.NOVA_TASKS).claimable(task)) {
            player.sendSystemMessage(Component.translatable("gui.starboundmc.tasks.inventory_full"));
        }
    }

    /** One-item transaction: preflight a full slot, then mutate inventory and ledger on the same thread.
     * Both live in the same player save; normal save/restart cannot replay a claim.
     */
    static boolean grantReward(ServerPlayer player, NovaTask task) {
        NovaTaskProgress before = player.getData(ModAttachments.NOVA_TASKS);
        if (!before.claimable(task) || player.isSpectator()) return false;
        ItemStack reward = new ItemStack(ModItems.MATTER_MANIPULATOR_MODULE.get(), task.reward());
        for (int slot = 0; slot < player.getInventory().items.size(); slot++) {
            ItemStack existing = player.getInventory().getItem(slot);
            if (!existing.isEmpty() && (!ItemStack.isSameItemSameComponents(existing, reward)
                    || existing.getCount() + reward.getCount() > existing.getMaxStackSize())) continue;
            ItemStack result = existing.isEmpty() ? reward : existing.copy();
            if (!existing.isEmpty()) result.grow(reward.getCount());
            player.setData(ModAttachments.NOVA_TASKS, before.claim(task));
            player.getInventory().setItem(slot, result);
            player.getInventory().setChanged();
            player.inventoryMenu.broadcastChanges();
            return true;
        }
        return false;
    }
}
