package com.starboundmc.item;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;

/** Immutable, persistent and network-synchronized manipulator upgrade state. */
public record MatterManipulatorUpgrades(int speed, int range, int mining, int fortune) {
    public static final MatterManipulatorUpgrades DEFAULT =
            new MatterManipulatorUpgrades(0, 0, 0, 0);

    public static final Codec<MatterManipulatorUpgrades> CODEC = RecordCodecBuilder.create(instance ->
            instance.group(
                    Codec.INT.optionalFieldOf("speed", 0).forGetter(MatterManipulatorUpgrades::speed),
                    Codec.INT.optionalFieldOf("range", 0).forGetter(MatterManipulatorUpgrades::range),
                    Codec.INT.optionalFieldOf("mining", 0).forGetter(MatterManipulatorUpgrades::mining),
                    Codec.INT.optionalFieldOf("fortune", 0).forGetter(MatterManipulatorUpgrades::fortune))
                    .apply(instance, MatterManipulatorUpgrades::new));

    public static final StreamCodec<RegistryFriendlyByteBuf, MatterManipulatorUpgrades> STREAM_CODEC =
            StreamCodec.of((buffer, value) -> {
                buffer.writeVarInt(value.speed);
                buffer.writeVarInt(value.range);
                buffer.writeVarInt(value.mining);
                buffer.writeVarInt(value.fortune);
            }, buffer -> new MatterManipulatorUpgrades(
                    buffer.readVarInt(), buffer.readVarInt(),
                    buffer.readVarInt(), buffer.readVarInt()));

    public MatterManipulatorUpgrades {
        speed = clamp(speed, MatterManipulatorItem.MAX_UPGRADES);
        range = clamp(range, MatterManipulatorItem.MAX_UPGRADES);
        mining = clamp(mining, MatterManipulatorItem.MAX_MINING_UPGRADES);
        fortune = clamp(fortune, MatterManipulatorItem.MAX_FORTUNE_UPGRADES);
    }

    public MatterManipulatorUpgrades withSpeed(int value) {
        return new MatterManipulatorUpgrades(value, range, mining, fortune);
    }

    public MatterManipulatorUpgrades withRange(int value) {
        return new MatterManipulatorUpgrades(speed, value, mining, fortune);
    }

    public MatterManipulatorUpgrades withMining(int value) {
        return new MatterManipulatorUpgrades(speed, range, value, fortune);
    }

    public MatterManipulatorUpgrades withFortune(int value) {
        return new MatterManipulatorUpgrades(speed, range, mining, value);
    }

    private static int clamp(int value, int maximum) {
        return Math.max(0, Math.min(maximum, value));
    }
}
