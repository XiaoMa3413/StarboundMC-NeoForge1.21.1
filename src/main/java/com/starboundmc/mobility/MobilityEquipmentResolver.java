package com.starboundmc.mobility;

import com.starboundmc.item.ModItems;
import com.starboundmc.story.ModAttachments;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import java.util.Optional;
import java.util.TreeMap;
import java.util.function.Function;

public final class MobilityEquipmentResolver {
    private static final TreeMap<String, Function<ServerPlayer, ItemStack>> SOURCES = new TreeMap<>();
    private MobilityEquipmentResolver() { }
    public static boolean valid(ItemStack stack) { return stack.is(ModItems.JUMP_THRUSTER.get()); }
    public static void registerSource(String id, Function<ServerPlayer, ItemStack> source) {
        if (SOURCES.putIfAbsent(id, source) != null) throw new IllegalArgumentException("Duplicate mobility source " + id);
    }
    public static Optional<ItemStack> active(ServerPlayer player) {
        var nativeStack = player.getData(ModAttachments.MOBILITY_EQUIPMENT);
        if (valid(nativeStack)) return Optional.of(nativeStack);
        return SOURCES.values().stream().map(source -> source.apply(player)).filter(MobilityEquipmentResolver::valid).findFirst();
    }
}
