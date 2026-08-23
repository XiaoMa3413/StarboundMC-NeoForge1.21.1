package com.starboundmc.world;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.core.RegistryCodecs;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeSource;
import net.minecraft.world.level.biome.Climate;

import java.util.stream.Stream;

/**
 * Wraps another biome source (usually the overworld multi-noise source) and
 * remaps any biome outside an allowed set to a fallback biome. This lets a
 * planet keep overworld terrain but expose only the requested biome families.
 */
public class FilteredBiomeSource extends BiomeSource
{
    public static final MapCodec<FilteredBiomeSource> CODEC = RecordCodecBuilder.mapCodec(instance ->
            instance.group(
                    BiomeSource.CODEC.fieldOf("source").forGetter(s -> s.source),
                    RegistryCodecs.homogeneousList(Registries.BIOME).fieldOf("allowed").forGetter(s -> s.allowed),
                    Biome.CODEC.fieldOf("fallback").forGetter(s -> s.fallback))
                    .apply(instance, FilteredBiomeSource::new));

    private final BiomeSource source;
    private final HolderSet<Biome> allowed;
    private final Holder<Biome> fallback;

    public FilteredBiomeSource(BiomeSource source, HolderSet<Biome> allowed, Holder<Biome> fallback)
    {
        this.source = source;
        this.allowed = allowed;
        this.fallback = fallback;
    }

    @Override
    protected MapCodec<? extends BiomeSource> codec()
    {
        return CODEC;
    }

    @Override
    protected Stream<Holder<Biome>> collectPossibleBiomes()
    {
        return Stream.concat(allowed.stream(), Stream.of(fallback));
    }

    @Override
    public Holder<Biome> getNoiseBiome(int x, int y, int z, Climate.Sampler sampler)
    {
        Holder<Biome> biome = source.getNoiseBiome(x, y, z, sampler);
        for (Holder<Biome> holder : allowed)
        {
            if (biome.unwrapKey().isPresent() && holder.is(biome.unwrapKey().get()))
                return biome;
        }
        return fallback;
    }
}
