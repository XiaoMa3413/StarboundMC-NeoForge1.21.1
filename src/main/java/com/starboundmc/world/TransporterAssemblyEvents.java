package com.starboundmc.world;

import com.starboundmc.StarboundMC;
import com.starboundmc.block.TransporterBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.chunk.LevelChunk;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.level.ChunkEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

import java.lang.ref.WeakReference;
import java.util.concurrent.ConcurrentLinkedQueue;

/** Upgrade old single-block anchors after chunk promotion, without touching authored NBT. */
@EventBusSubscriber(modid = StarboundMC.MODID)
public final class TransporterAssemblyEvents {
    private static final ConcurrentLinkedQueue<WeakReference<LevelChunk>> PENDING = new ConcurrentLinkedQueue<>();
    @SubscribeEvent public static void load(ChunkEvent.Load event) {
        if (event.getChunk() instanceof LevelChunk chunk && event.getLevel() instanceof ServerLevel)
            PENDING.add(new WeakReference<>(chunk));
    }
    @SubscribeEvent public static void tick(ServerTickEvent.Post event) {
        for (int budget = 0; budget < 16; budget++) {
            var reference = PENDING.poll();
            if (reference == null) break;
            var chunk = reference.get();
            if (chunk == null || !(chunk.getLevel() instanceof ServerLevel level)
                    || level.getServer() != event.getServer() || !level.hasChunk(chunk.getPos().x, chunk.getPos().z)) continue;
            var sections = chunk.getSections();
            for (int index = 0; index < sections.length; index++) {
                var section = sections[index];
                if (!section.getStates().maybeHas(s -> s.getBlock() instanceof TransporterBlock)) continue;
                int y0 = level.getMinBuildHeight() + index * 16;
                for (int x = 0; x < 16; x++) for (int y = 0; y < 16; y++) for (int z = 0; z < 16; z++) {
                    var state = section.getBlockState(x,y,z);
                    if (state.getBlock() instanceof TransporterBlock)
                        level.scheduleTick(new BlockPos(chunk.getPos().getMinBlockX()+x, y0+y, chunk.getPos().getMinBlockZ()+z), state.getBlock(), 1);
                }
            }
        }
    }
    @SubscribeEvent public static void stop(ServerStoppedEvent event) { PENDING.clear(); }
}
