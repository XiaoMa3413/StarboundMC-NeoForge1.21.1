// SPDX-License-Identifier: MPL-2.0
package com.starboundmc.epp;

import com.starboundmc.item.ModItems;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.level.Level;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;

public final class OxygenCanisterItem extends Item {
    public OxygenCanisterItem(Properties properties) { super(properties); }
    @Override public int getUseDuration(ItemStack stack, LivingEntity entity) { return EppConfig.USE_TICKS; }
    @Override public UseAnim getUseAnimation(ItemStack stack) { return UseAnim.DRINK; }
    @Override public net.minecraft.sounds.SoundEvent getDrinkingSound() { return SoundEvents.FIRE_EXTINGUISH; }
    @Override public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        if (player instanceof ServerPlayer server && !canUse(server)) {
            player.displayClientMessage(net.minecraft.network.chat.Component.translatable("message.starboundmc.epp.canister_unavailable"), true);
            return InteractionResultHolder.fail(player.getItemInHand(hand));
        }
        player.startUsingItem(hand);
        return InteractionResultHolder.consume(player.getItemInHand(hand));
    }
    public static boolean canUse(ServerPlayer player) {
        return !player.isSpectator() && EppEquipmentResolver.getActiveEpp(player)
                .filter(epp -> OxygenRules.canAcceptCanister(EppItem.oxygen(epp), EppItem.capacity(epp), EppConfig.CANISTER.get())).isPresent();
    }
    @Override public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity user) {
        if (!(user instanceof ServerPlayer player) || !canUse(player) || !stack.is(ModItems.OXYGEN_CANISTER.get())) return stack;
        ItemStack epp = EppEquipmentResolver.getActiveEpp(player).orElseThrow();
        EppItem.setOxygen(epp, EppItem.oxygen(epp) + EppConfig.CANISTER.get());
        level.playSound(null, player.blockPosition(), SoundEvents.FIRE_EXTINGUISH, SoundSource.PLAYERS, .4f, 1.6f);
        return new ItemStack(ModItems.EMPTY_OXYGEN_CANISTER.get());
    }
}
