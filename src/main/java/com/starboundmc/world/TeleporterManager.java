package com.starboundmc.world;

import com.starboundmc.block.ModBlocks;
import com.starboundmc.sound.ModSounds;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * Server-persistent registry of named teleporters (dimension + position -> display name).
 * A teleporter only shows up in other teleporters' UIs once it has been given a name.
 */
public class TeleporterManager extends SavedData
{
    public static final String NAME = "starboundmc_teleporters";
    public static final int MAX_NAME_LENGTH = 64;
    private static final int MAX_SAVED_ENTRIES = 4096;
    private static final int MAX_KEY_LENGTH = 256;
    private static final SavedData.Factory<TeleporterManager> FACTORY =
            new SavedData.Factory<>(TeleporterManager::new, TeleporterManager::load);

    private final Map<String, String> names = new HashMap<>();

    public static TeleporterManager get(MinecraftServer server)
    {
        return server.overworld().getDataStorage()
                .computeIfAbsent(FACTORY, NAME);
    }

    public static TeleporterManager load(CompoundTag tag, HolderLookup.Provider registries)
    {
        TeleporterManager manager = new TeleporterManager();
        CompoundTag entries = tag.getCompound("Entries");
        for (String key : entries.getAllKeys())
        {
            String name = sanitizeName(entries.getString(key));
            if (manager.names.size() >= MAX_SAVED_ENTRIES)
                break;
            if (key.length() <= MAX_KEY_LENGTH && name != null)
                manager.names.put(key, name);
        }
        return manager;
    }

    public static TeleporterManager load(CompoundTag tag)
    {
        return load(tag, HolderLookup.Provider.create(java.util.stream.Stream.empty()));
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries)
    {
        CompoundTag entries = new CompoundTag();
        for (Map.Entry<String, String> e : names.entrySet())
        {
            entries.putString(e.getKey(), e.getValue());
        }
        tag.put("Entries", entries);
        return tag;
    }

    public static String key(ResourceKey<Level> dimension, BlockPos pos)
    {
        return dimension.location() + "|" + pos.getX() + "," + pos.getY() + "," + pos.getZ();
    }

    public static String getName(MinecraftServer server, ResourceKey<Level> dimension, BlockPos pos)
    {
        return get(server).names.get(key(dimension, pos));
    }

    public static void setName(MinecraftServer server, ResourceKey<Level> dimension, BlockPos pos, String name)
    {
        TeleporterManager manager = get(server);
        String key = key(dimension, pos);
        String safeName = sanitizeName(name);
        if (safeName == null)
        {
            manager.names.remove(key);
        }
        else
        {
            manager.names.put(key, safeName);
        }
        manager.setDirty();
    }

    public static void remove(MinecraftServer server, ResourceKey<Level> dimension, BlockPos pos)
    {
        setName(server, dimension, pos, null);
    }

    /** All named teleporters whose block still exists; stale entries are pruned. */
    public static List<TeleporterEntry> validEntries(MinecraftServer server)
    {
        TeleporterManager manager = get(server);
        List<TeleporterEntry> result = new ArrayList<>();
        boolean pruned = false;
        Iterator<Map.Entry<String, String>> it = manager.names.entrySet().iterator();
        while (it.hasNext())
        {
            Map.Entry<String, String> e = it.next();
            TeleporterEntry entry = parse(server, e.getKey(), e.getValue());
            if (entry == null)
            {
                it.remove();
                pruned = true;
                continue;
            }
            result.add(entry);
        }
        if (pruned)
        {
            manager.setDirty();
        }
        return result;
    }

    /** Teleport the player to a named teleporter, landing on top of its block. */
    public static void teleportToNamed(ServerPlayer player, String key)
    {
        TeleporterEntry entry = parse(player.getServer(), key, "");
        if (entry == null)
            return;
        ServerLevel level = player.getServer().getLevel(entry.dimension());
        if (level == null)
            return;
        BlockPos dest = entry.pos().above();
        player.teleportTo(level, dest.getX() + 0.5, dest.getY(), dest.getZ() + 0.5,
                player.getYRot(), player.getXRot());
        level.playSound(null, dest, ModSounds.TELEPORTER_USE.get(), SoundSource.BLOCKS, 1.0F, 1.0F);
    }

    private static TeleporterEntry parse(MinecraftServer server, String key, String name)
    {
        int sep = key.indexOf('|');
        if (sep <= 0)
            return null;
        ResourceLocation dimensionId = ResourceLocation.tryParse(key.substring(0, sep));
        if (dimensionId == null)
            return null;
        ResourceKey<Level> dim = ResourceKey.create(Registries.DIMENSION, dimensionId);
        ServerLevel level = server.getLevel(dim);
        if (level == null)
            return null;
        String[] parts = key.substring(sep + 1).split(",");
        if (parts.length != 3)
            return null;
        BlockPos pos;
        try
        {
            pos = new BlockPos(Integer.parseInt(parts[0]), Integer.parseInt(parts[1]), Integer.parseInt(parts[2]));
        }
        catch (NumberFormatException ex)
        {
            return null;
        }
        if (!level.getBlockState(pos).is(ModBlocks.TELEPORTER.get()))
            return null;
        return new TeleporterEntry(dim, pos, name);
    }

    private static String sanitizeName(String name)
    {
        if (name == null)
            return null;
        String trimmed = name.trim();
        if (trimmed.isEmpty())
            return null;
        return trimmed.substring(0, Math.min(MAX_NAME_LENGTH, trimmed.length()));
    }

    public record TeleporterEntry(ResourceKey<Level> dimension, BlockPos pos, String name)
    {
    }
}
