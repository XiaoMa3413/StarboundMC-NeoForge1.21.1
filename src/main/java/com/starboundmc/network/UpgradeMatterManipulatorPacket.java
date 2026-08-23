package com.starboundmc.network;

import com.starboundmc.menu.UpgradeMenu;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/** Client -> Server: upgrade the manipulator in the workbench (0 = mining speed, 1 = laser range, 2 = mining tier). */
public class UpgradeMatterManipulatorPacket
{
    private final byte track;

    public UpgradeMatterManipulatorPacket(int track)
    {
        this.track = (byte) track;
    }

    public UpgradeMatterManipulatorPacket(FriendlyByteBuf buf)
    {
        this.track = buf.readByte();
    }

    public void encode(FriendlyByteBuf buf)
    {
        buf.writeByte(track);
    }

    public static UpgradeMatterManipulatorPacket decode(FriendlyByteBuf buf)
    {
        return new UpgradeMatterManipulatorPacket(buf.readByte());
    }

    public static void handle(UpgradeMatterManipulatorPacket msg, Supplier<NetworkEvent.Context> ctx)
    {
        ctx.get().enqueueWork(() ->
        {
            Player player = ctx.get().getSender();
            if (player != null && player.containerMenu instanceof UpgradeMenu menu)
            {
                menu.tryUpgrade(player, msg.track);
            }
        });
        ctx.get().setPacketHandled(true);
    }
}
