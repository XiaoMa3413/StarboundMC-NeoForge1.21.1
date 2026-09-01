package com.starboundmc.loot;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.starboundmc.item.ModItems;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import net.neoforged.neoforge.common.loot.IGlobalLootModifier;
import net.neoforged.neoforge.common.loot.LootModifier;

/**
 * Appends a fixed amount of voxels to entity loot when the JSON conditions
 * (entity-type tag checks) pass. Amounts live in the loot modifier JSONs.
 */
public final class VoxelDropLootModifier extends LootModifier {
    public static final MapCodec<VoxelDropLootModifier> CODEC = RecordCodecBuilder.mapCodec(instance ->
            codecStart(instance)
                    .and(Codec.intRange(1, 999).fieldOf("amount").forGetter(modifier -> modifier.amount))
                    .apply(instance, VoxelDropLootModifier::new));

    private final int amount;

    private VoxelDropLootModifier(LootItemCondition[] conditions, int amount) {
        super(conditions);
        this.amount = amount;
    }

    @Override
    protected ObjectArrayList<ItemStack> doApply(ObjectArrayList<ItemStack> generatedLoot, LootContext context) {
        generatedLoot.add(new ItemStack(ModItems.VOXEL.get(), amount));
        return generatedLoot;
    }

    @Override
    public MapCodec<? extends IGlobalLootModifier> codec() {
        return CODEC;
    }
}
