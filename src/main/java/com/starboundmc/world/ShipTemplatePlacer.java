package com.starboundmc.world;

import com.mojang.logging.LogUtils;
import com.starboundmc.StarboundMC;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.minecraft.world.level.saveddata.SavedData;
import org.slf4j.Logger;

import java.util.Optional;

/**
 * Places a player-built ship structure over the procedural fallback hull.
 *
 * At server start this tries to load the structure template
 * {@code data/starboundmc/structures/ship.nbt} (exported with a structure block,
 * or hand-placed in the mod's resources). If it exists it is placed with its
 * origin at {@link #TEMPLATE_ORIGIN} — i.e. the corner block of the structure
 * block's region lands exactly on the spawn point (0,102,0).
 *
 * A per-world flag (stored in the ship dimension's data storage, so deleting
 * {@code dimensions/starboundmc/ship} also resets it) makes sure the template is
 * only placed once. While no template exists, the procedural ShipStructure hull
 * remains, so the mod works out of the box.
 */
public class ShipTemplatePlacer
{
    private static final Logger LOGGER = LogUtils.getLogger();

    public static final ResourceLocation SHIP_TEMPLATE =
            ResourceLocation.fromNamespaceAndPath(StarboundMC.MODID, "ship");

    /** The template's (0,0,0) block (the structure block's corner) lands here. */
    public static final BlockPos TEMPLATE_ORIGIN = new BlockPos(0, 102, 0);

    private static final String DATA_NAME = "starboundmc_ship_template";

    public static void placeOnServerStart(MinecraftServer server)
    {
        ServerLevel ship = server.getLevel(ShipDimensions.SHIP_LEVEL);
        if (ship == null)
            return;

        ShipTemplateData data = ship.getDataStorage()
                .computeIfAbsent(ShipTemplateData::load, ShipTemplateData::new, DATA_NAME);
        if (data.placed)
            return;

        Optional<StructureTemplate> template = server.getStructureManager().get(SHIP_TEMPLATE);
        if (template.isEmpty())
            return; // no custom ship yet — keep the procedural hull

        StructurePlaceSettings settings = new StructurePlaceSettings()
                .setRotation(Rotation.NONE)
                .setIgnoreEntities(true);
        template.get().placeInWorld(ship, TEMPLATE_ORIGIN, TEMPLATE_ORIGIN, settings, ship.getRandom(), 2);

        data.placed = true;
        data.setDirty();
        LOGGER.info("Placed custom ship template {} at {}", SHIP_TEMPLATE, TEMPLATE_ORIGIN);
    }

    public static class ShipTemplateData extends SavedData
    {
        private boolean placed;

        public static ShipTemplateData load(CompoundTag tag)
        {
            ShipTemplateData data = new ShipTemplateData();
            data.placed = tag.getBoolean("Placed");
            return data;
        }

        @Override
        public CompoundTag save(CompoundTag tag)
        {
            tag.putBoolean("Placed", placed);
            return tag;
        }
    }
}
