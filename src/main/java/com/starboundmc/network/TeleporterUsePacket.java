package com.starboundmc.network;

import com.starboundmc.menu.TeleporterMenu;
import com.starboundmc.sound.ModSounds;
import com.starboundmc.world.ShipDimensions;
import com.starboundmc.world.TeleporterManager;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/** Client -> Server: teleport to the destination key chosen in the teleporter UI. */
public class TeleporterUsePacket
{
    private final String key;

    public TeleporterUsePacket(String key)
    {
        this.key = key;
    }

    public TeleporterUsePacket(FriendlyByteBuf buf)
    {
        this.key = buf.readUtf();
    }

    public void encode(FriendlyByteBuf buf)
    {
        buf.writeUtf(key);
    }

    public static TeleporterUsePacket decode(FriendlyByteBuf buf)
    {
        return new TeleporterUsePacket(buf.readUtf());
    }

    public static void handle(TeleporterUsePacket msg, Supplier<NetworkEvent.Context> ctx)
    {
        ctx.get().enqueueWork(() ->
        {
            ServerPlayer player = ctx.get().getSender();
            if (player == null || !(player.containerMenu instanceof TeleporterMenu))
                return;

            switch (msg.key)
            {
                case "ship" ->
                {
                    ServerLevel ship = player.getServer().getLevel(ShipDimensions.SHIP_LEVEL);
                    if (ship == null)
                        return;
                    BlockPos dest = ShipDimensions.shipTeleporterDestination(ship);
                    player.teleportTo(ship, dest.getX() + 0.5, dest.getY(), dest.getZ() + 0.5,
                            player.getYRot(), player.getXRot());
                    ship.playSound(null, dest, ModSounds.TELEPORTER_USE.get(), SoundSource.BLOCKS, 1.0F, 1.0F);
                }
                case "planet" ->
                {
                    ShipDimensions.teleportToPlanetSurface(player);
                    player.level().playSound(null, player.blockPosition(), ModSounds.TELEPORTER_USE.get(),
                            SoundSource.BLOCKS, 1.0F, 1.0F);
                }
                default ->
                {
                    if (msg.key.startsWith("n|"))
                    {
                        TeleporterManager.teleportToNamed(player, msg.key.substring(2));
                    }
                }
            }
        });
        ctx.get().setPacketHandled(true);
    }
}
