// SPDX-License-Identifier: MPL-2.0
package com.starboundmc.epp;

import com.starboundmc.story.ModAttachments;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import java.util.Optional;
import java.util.TreeMap;
import java.util.function.Function;

public final class EppEquipmentResolver {
    private static final TreeMap<String, Function<ServerPlayer, ItemStack>> SOURCES = new TreeMap<>();
    private EppEquipmentResolver() { }
    /** Live, mutable stack from the external equipment inventory; native slot always takes priority. */
    public static void registerSource(String id, Function<ServerPlayer, ItemStack> source) {
        if (SOURCES.putIfAbsent(id, source) != null) throw new IllegalArgumentException("Duplicate EPP source " + id);
    }
    public static Optional<ItemStack> getActiveEpp(ServerPlayer player) {
        ItemStack nativeStack = player.getData(ModAttachments.EPP_EQUIPMENT);
        if (valid(nativeStack)) return Optional.of(nativeStack);
        for (var source : SOURCES.values()) {
            ItemStack stack = source.apply(player);
            if (valid(stack)) return Optional.of(stack);
        }
        return Optional.empty();
    }
    public static boolean valid(ItemStack stack) { return stack != null && !stack.isEmpty() && stack.getItem() instanceof EppItem; }
}
