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

    public static final DeferredHolder<AttachmentType<?>, AttachmentType<com.starboundmc.epp.EvaState>> EVA =
            ATTACHMENTS.register("eva", () -> AttachmentType.builder(com.starboundmc.epp.EvaState::new).build());

    public static final DeferredHolder<AttachmentType<?>, AttachmentType<net.minecraft.world.item.ItemStack>> EPP_EQUIPMENT =
            ATTACHMENTS.register("epp_equipment", () -> AttachmentType.builder(() -> net.minecraft.world.item.ItemStack.EMPTY)
                    .serialize(net.minecraft.world.item.ItemStack.OPTIONAL_CODEC).copyOnDeath().build());
    public static final DeferredHolder<AttachmentType<?>, AttachmentType<Integer>> SUFFOCATION =
            ATTACHMENTS.register("suffocation", () -> AttachmentType.builder(() -> 0)
                    .serialize(com.mojang.serialization.Codec.intRange(0, 1000)).build());
    public static final DeferredHolder<AttachmentType<?>, AttachmentType<Integer>> EPP_WARNING =
            ATTACHMENTS.register("epp_warning", () -> AttachmentType.builder(() -> 0)
                    .serialize(com.mojang.serialization.Codec.intRange(0, 4)).build());
    public static final DeferredHolder<AttachmentType<?>, AttachmentType<Integer>> EPP_VISUAL =
            ATTACHMENTS.register("epp_visual", () -> AttachmentType.builder(() -> 0).build());
    public static final DeferredHolder<AttachmentType<?>, AttachmentType<Integer>> COLD_EXPOSURE =
            ATTACHMENTS.register("cold_exposure", () -> AttachmentType.builder(() -> 0)
                    .serialize(com.mojang.serialization.Codec.intRange(0, 100)).build());
    public static final DeferredHolder<AttachmentType<?>, AttachmentType<Integer>> COLD_WARNING =
            ATTACHMENTS.register("cold_warning", () -> AttachmentType.builder(() -> 0)
                    .serialize(com.mojang.serialization.Codec.intRange(0, 3)).build());
    public static final DeferredHolder<AttachmentType<?>, AttachmentType<Integer>> HEAT_EXPOSURE =
            ATTACHMENTS.register("heat_exposure", () -> AttachmentType.builder(() -> 0)
                    .serialize(com.mojang.serialization.Codec.intRange(0, 100)).build());
    public static final DeferredHolder<AttachmentType<?>, AttachmentType<Integer>> HEAT_WARNING =
            ATTACHMENTS.register("heat_warning", () -> AttachmentType.builder(() -> 0)
                    .serialize(com.mojang.serialization.Codec.intRange(0, 3)).build());

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

    public static final DeferredHolder<AttachmentType<?>, AttachmentType<NovaTaskProgress>> NOVA_TASKS =
            ATTACHMENTS.register("nova_tasks", () -> AttachmentType.builder(() -> NovaTaskProgress.DEFAULT)
                    .serialize(NovaTaskProgress.CODEC).copyOnDeath().build());

    private ModAttachments()
    {
    }

    public static void register(IEventBus modEventBus)
    {
        ATTACHMENTS.register(modEventBus);
    }
}
