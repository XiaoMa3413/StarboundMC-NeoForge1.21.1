// SPDX-License-Identifier: MPL-2.0
package com.starboundmc.encounter;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Clearable;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;

public final class RelayStructure {
    public static final ResourceLocation TEMPLATE = ResourceLocation.fromNamespaceAndPath("starboundmc", "abandoned_relay_station");
    private RelayStructure() { }
    public static StructureTemplate template(ServerLevel level, CompoundTag snapshot) {
        StructureTemplate template;
        if (snapshot.isEmpty()) template = level.getStructureManager().get(TEMPLATE).orElseThrow(() -> new IllegalStateException("missing_template"));
        else { template = new StructureTemplate(); template.load(level.registryAccess().lookupOrThrow(Registries.BLOCK), snapshot); }
        if (!template.getSize().equals(new Vec3i(RelayGeometry.WIDTH, RelayGeometry.HEIGHT, RelayGeometry.DEPTH)))
            throw new IllegalStateException("invalid_template_size");
        return template;
    }
    public static boolean place(ServerLevel level, BlockPos origin, CompoundTag snapshot) {
        // Repeat the physical check at the instant of placement, after approach animation.
        if (!RelayGeometry.clear(origin, RelayGeometry.MARGIN, p -> !level.getBlockState(p).isAir())) return false;
        return template(level, snapshot).placeInWorld(level, origin, origin,
                new StructurePlaceSettings().setIgnoreEntities(true), level.random, 2 | 16);
    }
    public static CompoundTag capture(ServerLevel level, BlockPos origin) {
        var template = new StructureTemplate();
        template.fillFromWorld(level, origin, new Vec3i(RelayGeometry.WIDTH, RelayGeometry.HEIGHT, RelayGeometry.DEPTH), false, null);
        return template.save(new CompoundTag());
    }
    public static boolean touchesBoundary(ServerLevel level, BlockPos origin) {
        for (var relative : BlockPos.betweenClosed(0, 0, 0, RelayGeometry.WIDTH - 1, RelayGeometry.HEIGHT - 1, RelayGeometry.DEPTH - 1))
            if (RelayGeometry.onBoundary(relative) && !level.getBlockState(origin.offset(relative)).isAir()) return true;
        return false;
    }
    public static void clear(ServerLevel level, BlockPos origin) {
        for (var pos : BlockPos.betweenClosed(origin, origin.offset(RelayGeometry.WIDTH - 1, RelayGeometry.HEIGHT - 1, RelayGeometry.DEPTH - 1))) {
            // Contents are already durably snapshotted; removing a container must not drop a second copy.
            Clearable.tryClear(level.getBlockEntity(pos));
            level.setBlock(pos, Blocks.AIR.defaultBlockState(), 2 | 16);
        }
    }
    /** Retry a journaled operation only when every surviving block still belongs to its snapshot. */
    public static boolean recover(ServerLevel level, BlockPos origin, CompoundTag snapshot, boolean removing) {
        if (!level.getEntities(null, RelayGeometry.bounds(origin)).isEmpty()) return false;
        template(level, snapshot); // reject wrong size or missing data before inspecting ownership
        var palette = snapshot.getList("palette", net.minecraft.nbt.Tag.TAG_COMPOUND);
        var entries = snapshot.getList("blocks", net.minecraft.nbt.Tag.TAG_COMPOUND);
        var expected = new java.util.HashMap<BlockPos, CompoundTag>();
        for (int i = 0; i < entries.size(); i++) {
            var entry = entries.getCompound(i); var coordinates = entry.getList("pos", net.minecraft.nbt.Tag.TAG_INT);
            var pos = origin.offset(coordinates.getInt(0), coordinates.getInt(1), coordinates.getInt(2));
            if (!RelayGeometry.bounds(origin).contains(net.minecraft.world.phys.Vec3.atCenterOf(pos))) return false;
            expected.put(pos, entry);
        }
        for (var pos : BlockPos.betweenClosed(origin, origin.offset(RelayGeometry.WIDTH - 1, RelayGeometry.HEIGHT - 1, RelayGeometry.DEPTH - 1))) {
            var existing = level.getBlockState(pos); if (existing.isAir()) continue;
            var entry = expected.get(pos); if (entry == null) return false;
            var state = net.minecraft.nbt.NbtUtils.readBlockState(level.registryAccess().lookupOrThrow(Registries.BLOCK), palette.getCompound(entry.getInt("state")));
            if (!existing.equals(state)) return false;
            var be = level.getBlockEntity(pos);
            if (be != null && entry.contains("nbt")) {
                var actual = be.saveWithFullMetadata(level.registryAccess());
                var intended = entry.getCompound("nbt").copy();
                for (String key : java.util.List.of("x", "y", "z")) { actual.remove(key); intended.remove(key); }
                // A changed container belongs to the player; keep both it and the journal for inspection.
                if (!net.minecraft.nbt.NbtUtils.compareNbt(intended, actual, true)
                        || !intended.getList("Items", net.minecraft.nbt.Tag.TAG_COMPOUND)
                        .equals(actual.getList("Items", net.minecraft.nbt.Tag.TAG_COMPOUND))) return false;
            }
        }
        if (removing) { clear(level, origin); return true; }
        for (var pair : expected.entrySet()) {
            var pos = pair.getKey(); if (!level.getBlockState(pos).isAir()) continue;
            var entry = pair.getValue();
            var state = net.minecraft.nbt.NbtUtils.readBlockState(level.registryAccess().lookupOrThrow(Registries.BLOCK), palette.getCompound(entry.getInt("state")));
            if (state.isAir()) continue;
            level.setBlock(pos, state, 2 | 16);
            var be = level.getBlockEntity(pos);
            if (be != null && entry.contains("nbt")) {
                be.loadWithComponents(entry.getCompound("nbt"), level.registryAccess()); be.setChanged();
            }
        }
        return true;
    }
}
