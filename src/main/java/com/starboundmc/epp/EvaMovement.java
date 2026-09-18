// SPDX-License-Identifier: MPL-2.0
package com.starboundmc.epp;

import com.starboundmc.StarboundMC;
import com.starboundmc.network.EvaStatePacket;
import com.starboundmc.network.ModNetwork;
import com.starboundmc.story.ModAttachments;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.event.entity.living.LivingFallEvent;

@EventBusSubscriber(modid = StarboundMC.MODID)
public final class EvaMovement {
    private EvaMovement() { }
    public static boolean eligible(Player player) {
        return player.isAlive() && !player.isCreative() && !player.isSpectator()
                && !player.isPassenger() && !player.isSleeping() && !player.isInWater() && !player.isInLava();
    }
    public static int serverMode(ServerPlayer player) {
        if (!eligible(player) || PlayerEnvironmentService.at(player).gravityScale() > 0) return EvaState.NORMAL;
        ItemStack pack = EppEquipmentResolver.getActiveEpp(player).orElse(ItemStack.EMPTY);
        return EppItem.generation(pack) >= 2 ? EvaState.THRUST : EvaState.DRIFT;
    }
    public static int mode(Player player) {
        if (player instanceof ServerPlayer serverPlayer) return serverMode(serverPlayer);
        var state = player.getData(ModAttachments.EVA);
        return eligible(player) && player.level().dimension().location().equals(state.dimension) ? state.mode : EvaState.NORMAL;
    }
    @SubscribeEvent public static void tick(PlayerTickEvent.Pre event) {
        if (event.getEntity() instanceof ServerPlayer player) update(player);
    }
    public static void update(ServerPlayer player) {
        var state = player.getData(ModAttachments.EVA);
        int mode = serverMode(player);
        var dimension = player.level().dimension().location();
        boolean changed = mode != state.mode || !dimension.equals(state.dimension);
        if (changed) {
            state.clearInput();
            player.resetFallDistance();
        }
        state.mode = mode;
        state.dimension = dimension;
        if (mode != EvaState.NORMAL) player.resetFallDistance();
        if (changed || player.tickCount % 20 == 0)
            ModNetwork.sendToPlayer(player, new EvaStatePacket(dimension, mode));
    }
    public static void acceptInput(ServerPlayer player, int input) {
        var state = player.getData(ModAttachments.EVA);
        // Receiving more packets cannot produce extra movement ticks.
        if (serverMode(player) != EvaState.THRUST || !EvaMotion.validInput(input)
                || player.containerMenu != player.inventoryMenu) {
            state.clearInput();
            return;
        }
        state.input = input;
        state.lastInputTick = player.tickCount;
    }
    /** Called at Player.travel; normal movement/collisions still own all non-EVA players. */
    public static boolean travel(Player player) {
        int mode = mode(player);
        if (mode == EvaState.NORMAL) return false;
        player.resetFallDistance();
        if (player.isControlledByLocalInstance()) {
            var state = player.getData(ModAttachments.EVA);
            int input = player.containerMenu == player.inventoryMenu ? state.freshInput(player.tickCount) : 0;
            player.setDeltaMovement(EvaMotion.step(player.getDeltaMovement(), input,
                    player.getYRot(), player.getXRot(), mode == EvaState.THRUST));
            player.move(MoverType.SELF, player.getDeltaMovement());
        }
        return true;
    }
    @SubscribeEvent public static void fall(LivingFallEvent event) {
        if (event.getEntity() instanceof Player player && mode(player) != EvaState.NORMAL) event.setCanceled(true);
    }
}
