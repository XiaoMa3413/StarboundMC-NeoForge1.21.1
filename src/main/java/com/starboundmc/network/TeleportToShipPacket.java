package com.starboundmc.network;

import com.starboundmc.sound.ModSounds;
import com.starboundmc.world.ShipDimensions;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/** Client -> Server: teleport the player back to the ship's teleporter. */
public class TeleportToShipPacket
{
    public TeleportToShipPacket()
    {
    }

    public void encode(FriendlyByteBuf buf)
    {
    }

    public static TeleportToShipPacket decode(FriendlyByteBuf buf)
    {
        return new TeleportToShipPacket();
    }

    public static void handle(TeleportToShipPacket msg, Supplier<NetworkEvent.Context> ctx)
    {
        ctx.get().enqueueWork(() ->
        {
            ServerPlayer player = ctx.get().getSender();
            if (player == null)
                return;
            ServerLevel ship = player.getServer().getLevel(ShipDimensions.SHIP_LEVEL);
            if (ship == null)
                return;
            BlockPos dest = ShipDimensions.shipTeleporterDestination(ship);
            player.teleportTo(ship, dest.getX() + 0.5, dest.getY(), dest.getZ() + 0.5,
                    player.getYRot(), player.getXRot());
            ship.playSound(null, dest, ModSounds.TELEPORTER_USE.get(), SoundSource.BLOCKS, 1.0F, 1.0F);
        });
        ctx.get().setPacketHandled(true);
    }
}
