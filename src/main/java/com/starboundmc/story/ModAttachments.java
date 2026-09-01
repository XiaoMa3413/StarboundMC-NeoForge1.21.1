package com.starboundmc.story;

import com.starboundmc.StarboundMC;
import com.starboundmc.economy.VoxelWalletState;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

/** Data attachments for per-player story knowledge and wallet balance. */
public final class ModAttachments
{
    public static final DeferredRegister<AttachmentType<?>> ATTACHMENTS =
            DeferredRegister.create(NeoForgeRegistries.Keys.ATTACHMENT_TYPES, StarboundMC.MODID);

    public static final DeferredHolder<AttachmentType<?>, AttachmentType<PlayerStoryState>> PLAYER_STORY =
            ATTACHMENTS.register("player_story", () -> AttachmentType.builder(() -> PlayerStoryState.DEFAULT)
                    .serialize(PlayerStoryState.CODEC)
                    .copyOnDeath()
                    .build());

    public static final DeferredHolder<AttachmentType<?>, AttachmentType<VoxelWalletState>> VOXEL_WALLET =
            ATTACHMENTS.register("voxel_wallet", () -> AttachmentType.builder(() -> VoxelWalletState.DEFAULT)
                    .serialize(VoxelWalletState.CODEC)
                    .copyOnDeath()
                    .build());

    private ModAttachments()
    {
    }

    public static void register(IEventBus modEventBus)
    {
        ATTACHMENTS.register(modEventBus);
    }
}
