package com.starboundmc.block.entity;

import com.starboundmc.block.ModBlockEntities;
import com.starboundmc.menu.ShipCrateMenu;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.Level;
import net.minecraft.util.Mth;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class ShipCrateBlockEntity extends BlockEntity implements Container
{
    private final SimpleContainer container = new SimpleContainer(ShipCrateMenu.SLOTS);
    private final Set<UUID> viewers = new HashSet<>();
    private boolean open;
    private float previousOpening;
    private float opening;

    public ShipCrateBlockEntity(BlockPos pos, BlockState state)
    {
        super(ModBlockEntities.SHIP_CRATE.get(), pos, state);
        container.addListener(ignored -> setChanged());
    }

    public SimpleContainer getContainer()
    {
        return container;
    }

    @Override
    public void startOpen(Player player)
    {
        if (level != null && !level.isClientSide && !isRemoved() && !player.isSpectator()) {
            viewers.add(player.getUUID());
            updateOpenState();
        }
    }

    @Override
    public void stopOpen(Player player)
    {
        if (level != null && !level.isClientSide) {
            viewers.remove(player.getUUID());
            updateOpenState();
        }
    }

    private void updateOpenState()
    {
        BlockState state = getBlockState();
        if (level != null && !isRemoved() && level.getBlockEntity(worldPosition) == this
                && open != !viewers.isEmpty()) {
            open = !viewers.isEmpty();
            level.sendBlockUpdated(worldPosition, state, state, 3);
        }
    }

    public boolean isOpen() { return open; }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries)
    {
        CompoundTag tag = new CompoundTag();
        tag.putBoolean("Open", open);
        return tag;
    }

    @Override
    public ClientboundBlockEntityDataPacket getUpdatePacket()
    {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public void handleUpdateTag(CompoundTag tag, HolderLookup.Provider registries)
    {
        open = tag.getBoolean("Open");
    }

    @Override
    public void onDataPacket(Connection connection, ClientboundBlockEntityDataPacket packet, HolderLookup.Provider registries)
    {
        handleUpdateTag(packet.getTag(), registries);
    }

    public static void tick(Level level, BlockPos pos, BlockState state, ShipCrateBlockEntity crate)
    {
        if (level.isClientSide) {
            crate.previousOpening = crate.opening;
            crate.opening = Mth.approach(crate.opening, crate.open ? 1 : 0, 1.0F / 16);
        } else if (level.getGameTime() % 10 == 0) {
            // Reconcile disconnects and dimension changes. Viewer state is never persisted.
            crate.viewers.removeIf(id -> {
                Player player = level.getPlayerByUUID(id);
                return player == null || player.isSpectator() || !crate.stillValid(player)
                        || !(player.containerMenu instanceof ShipCrateMenu menu) || !menu.isUsing(crate);
            });
            crate.updateOpenState();
        }
    }

    public float getOpening(float partialTick)
    {
        return Mth.lerp(partialTick, previousOpening, opening);
    }

    @Override
    public int getContainerSize()
    {
        return container.getContainerSize();
    }

    @Override
    public boolean isEmpty()
    {
        return container.isEmpty();
    }

    @Override
    public ItemStack getItem(int slot)
    {
        return container.getItem(slot);
    }

    @Override
    public ItemStack removeItem(int slot, int amount)
    {
        return container.removeItem(slot, amount);
    }

    @Override
    public ItemStack removeItemNoUpdate(int slot)
    {
        ItemStack removed = container.removeItemNoUpdate(slot);
        if (!removed.isEmpty())
            setChanged();
        return removed;
    }

    @Override
    public void setItem(int slot, ItemStack stack)
    {
        container.setItem(slot, stack);
    }

    @Override
    public boolean stillValid(Player player)
    {
        return Container.stillValidBlockEntity(this, player);
    }

    @Override
    public void clearContent()
    {
        container.clearContent();
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries)
    {
        super.saveAdditional(tag, registries);
        tag.put("Items", container.createTag(registries));
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries)
    {
        super.loadAdditional(tag, registries);
        container.fromTag(tag.getList("Items", Tag.TAG_COMPOUND), registries);
    }
}
