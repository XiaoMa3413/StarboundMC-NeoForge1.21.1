package com.starboundmc.network;

import com.starboundmc.menu.TeleporterMenu;
import com.starboundmc.world.TeleporterManager;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/** Client -> Server: give the teleporter whose UI is open a display name (empty clears it). */
public class TeleporterRenamePacket
{
    private final String name;

    public TeleporterRenamePacket(String name)
    {
        this.name = name;
    }

    public TeleporterRenamePacket(FriendlyByteBuf buf)
    {
        this.name = buf.readUtf();
    }

    public void encode(FriendlyByteBuf buf)
    {
        buf.writeUtf(name);
    }

    public static TeleporterRenamePacket decode(FriendlyByteBuf buf)
    {
        return new TeleporterRenamePacket(buf.readUtf());
    }

    public static void handle(TeleporterRenamePacket msg, Supplier<NetworkEvent.Context> ctx)
    {
        ctx.get().enqueueWork(() ->
        {
            ServerPlayer player = ctx.get().getSender();
            if (player == null || !(player.containerMenu instanceof TeleporterMenu menu))
                return;
            if (menu.pos.equals(BlockPos.ZERO))
                return;

            TeleporterManager.setName(player.getServer(), player.level().dimension(), menu.pos, msg.name);
            refreshAllTeleporterUIs(player.getServer());
        });
        ctx.get().setPacketHandled(true);
    }

    private static void refreshAllTeleporterUIs(MinecraftServer server)
    {
        for (ServerPlayer p : server.getPlayerList().getPlayers())
        {
            if (p.containerMenu instanceof TeleporterMenu menu)
            {
                TeleporterListPacket packet = TeleporterListPacketHelper.build(server, p.level().dimension(), menu.pos);
                ModNetwork.CHANNEL.send(net.minecraftforge.network.PacketDistributor.PLAYER.with(() -> p), packet);
            }
        }
    }
}
