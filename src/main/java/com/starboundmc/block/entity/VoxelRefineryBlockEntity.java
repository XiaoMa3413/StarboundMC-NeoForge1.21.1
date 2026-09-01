package com.starboundmc.block.entity;

import com.starboundmc.block.ModBlockEntities;
import com.starboundmc.economy.VoxelWalletService;
import com.starboundmc.menu.VoxelRefineryMenu;
import com.starboundmc.network.ModNetwork;
import com.starboundmc.network.SyncVoxelMachinePacket;
import com.starboundmc.recipe.MachineRecipeInput;
import com.starboundmc.recipe.VoxelDecompositionRecipe;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import java.util.Optional;
/**
 * Voxel refinery logic: consumes one matching input item after a short
 * refinement delay and exposes the recipe's voxel amount as a public,
 * machine-owned reward that any viewer may claim.
 */
public final class VoxelRefineryBlockEntity extends BlockEntity implements Container {
    public static final int INPUT_SLOTS = 1;
    public static final int REFINE_TICKS = 30;

    private ItemStack input = ItemStack.EMPTY;
    private int refineProgress = 0;
    private int jobVoxels = 0;
    private int pendingVoxels = 0;

    public enum RefinementStartResult {
        STARTED,
        BUSY,
        EMPTY_INPUT,
        UNSUPPORTED_INPUT
    }

    public VoxelRefineryBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.VOXEL_REFINERY.get(), pos, state);
    }

    public static void tick(Level level, BlockPos pos, BlockState state, VoxelRefineryBlockEntity refinery) {
        if (!(level instanceof ServerLevel serverLevel)) {
            return;
        }
        if (refinery.refineProgress <= 0) {
            return;
        }
        if (refinery.jobVoxels <= 0 || refinery.input.isEmpty()) {
            refinery.cancelActiveJob();
            refinery.syncProgress(serverLevel, pos);
            return;
        }
        refinery.refineProgress--;
        if (refinery.refineProgress > 0) {
            if (level.getGameTime() % 10 == 0) {
                refinery.syncProgress(serverLevel, pos);
            }
            return;
        }
        // Job complete: consume one item and leave a public claimable result.
        refinery.input.shrink(1);
        refinery.refineProgress = 0;
        refinery.pendingVoxels = (int) Math.min(Integer.MAX_VALUE,
                (long) refinery.pendingVoxels + refinery.jobVoxels);
        refinery.jobVoxels = 0;
        refinery.startNextJob(serverLevel);
        refinery.setChanged();
        refinery.syncProgress(serverLevel, pos);
    }

    private void cancelActiveJob() {
        refineProgress = 0;
        jobVoxels = 0;
        setChanged();
    }

    /** Starts a refinement job if the input matches a decomposition recipe. */
    public boolean startRefinement(ServerLevel level) {
        return tryStartRefinement(level) == RefinementStartResult.STARTED;
    }

    /** Starts a job and reports the exact refusal reason for player-facing feedback. */
    public RefinementStartResult tryStartRefinement(ServerLevel level) {
        if (refineProgress > 0) {
            return RefinementStartResult.BUSY;
        }
        if (input.isEmpty()) {
            return RefinementStartResult.EMPTY_INPUT;
        }
        Optional<RecipeHolder<VoxelDecompositionRecipe>> match = level.getRecipeManager()
                .getRecipeFor(VoxelDecompositionRecipe.TYPE, new MachineRecipeInput(input), level);
        if (match.isEmpty()) {
            return RefinementStartResult.UNSUPPORTED_INPUT;
        }
        beginJob(match.get().value().voxels());
        setChanged();
        syncProgress(level, worldPosition);
        return RefinementStartResult.STARTED;
    }

    /** Stops only the unfinished cycle; input and completed public output remain untouched. */
    public boolean stopRefinement(ServerLevel level) {
        if (refineProgress <= 0) {
            return false;
        }
        cancelActiveJob();
        syncProgress(level, worldPosition);
        return true;
    }

    private boolean startNextJob(ServerLevel level) {
        if (input.isEmpty() || pendingVoxels == Integer.MAX_VALUE) {
            return false;
        }
        Optional<RecipeHolder<VoxelDecompositionRecipe>> match = level.getRecipeManager()
                .getRecipeFor(VoxelDecompositionRecipe.TYPE, new MachineRecipeInput(input), level);
        if (match.isEmpty()) {
            return false;
        }
        beginJob(match.get().value().voxels());
        return true;
    }

    private void beginJob(int voxels) {
        jobVoxels = voxels;
        refineProgress = REFINE_TICKS;
    }

    /**
     * Atomically transfers the completed public output to the first player
     * whose valid claim reaches the server. Returns the claimed amount.
     */
    public int claimPendingVoxels(ServerPlayer player) {
        if (refineProgress > 0 || pendingVoxels <= 0) {
            return 0;
        }
        int voxels = pendingVoxels;
        pendingVoxels = 0;
        setChanged();
        VoxelWalletService.add(player, voxels);
        syncProgress(player.serverLevel(), worldPosition);
        return voxels;
    }

    private void syncProgress(ServerLevel level, BlockPos pos) {
        level.getChunkSource().blockChanged(pos);
        setChanged();
        SyncVoxelMachinePacket snapshot = new SyncVoxelMachinePacket(
                pos, refineProgress, REFINE_TICKS, pendingVoxels,
                net.minecraft.resources.ResourceLocation.withDefaultNamespace("air"), 0);
        for (ServerPlayer viewer : level.players()) {
            if (viewer.containerMenu instanceof VoxelRefineryMenu menu
                    && menu.isBoundToBlock() && menu.blockPos().equals(pos)) {
                ModNetwork.sendToPlayer(viewer, snapshot);
            }
        }
    }

    /** Sends the current job state to a player who has just opened the refinery. */
    public void syncTo(ServerPlayer viewer) {
        if (level instanceof ServerLevel serverLevel) {
            ModNetwork.sendToPlayer(viewer, new SyncVoxelMachinePacket(
                    worldPosition, refineProgress, REFINE_TICKS, pendingVoxels,
                    net.minecraft.resources.ResourceLocation.withDefaultNamespace("air"), 0));
        }
    }

    public int refineProgress() {
        return refineProgress;
    }

    public int pendingVoxels() {
        return pendingVoxels;
    }

    /** Removes completed public output so block destruction can drop it exactly once. */
    public int drainPendingVoxelsForDrop() {
        int voxels = pendingVoxels;
        pendingVoxels = 0;
        setChanged();
        return voxels;
    }

    @Override
    public int getContainerSize() {
        return INPUT_SLOTS;
    }

    @Override
    public boolean isEmpty() {
        return input.isEmpty();
    }

    @Override
    public ItemStack getItem(int index) {
        return index == 0 ? input : ItemStack.EMPTY;
    }

    @Override
    public ItemStack removeItem(int index, int count) {
        if (index != 0 || input.isEmpty()) {
            return ItemStack.EMPTY;
        }
        boolean wasRunning = refineProgress > 0;
        ItemStack removed = input.split(count);
        if (!removed.isEmpty()) {
            stopAfterInputChange(wasRunning);
        }
        return removed;
    }

    @Override
    public ItemStack removeItemNoUpdate(int index) {
        if (index != 0 || input.isEmpty()) {
            return ItemStack.EMPTY;
        }
        boolean wasRunning = refineProgress > 0;
        ItemStack removed = input;
        input = ItemStack.EMPTY;
        stopAfterInputChange(wasRunning);
        return removed;
    }

    @Override
    public void setItem(int index, ItemStack stack) {
        if (index != 0) {
            return;
        }
        boolean wasRunning = refineProgress > 0;
        if (wasRunning) {
            cancelActiveJob();
        }
        input = stack;
        if (stack.getCount() > getMaxStackSize()) {
            stack.setCount(getMaxStackSize());
        }
        setChanged();
        if (level instanceof ServerLevel serverLevel) {
            syncProgress(serverLevel, worldPosition);
        }
    }

    private void stopAfterInputChange(boolean wasRunning) {
        if (wasRunning) {
            cancelActiveJob();
        } else {
            setChanged();
        }
        if (wasRunning && level instanceof ServerLevel serverLevel) {
            syncProgress(serverLevel, worldPosition);
        }
    }

    /** Covers partial shift-click transfers that mutate the slot stack in place. */
    public void stopAfterManualInputTake() {
        if (refineProgress > 0) {
            stopAfterInputChange(true);
        }
    }

    @Override
    public int getMaxStackSize() {
        return 64;
    }

    @Override
    public boolean stillValid(Player player) {
        return Container.stillValidBlockEntity(this, player);
    }

    @Override
    public void clearContent() {
        boolean wasRunning = refineProgress > 0;
        input = ItemStack.EMPTY;
        stopAfterInputChange(wasRunning);
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        if (!input.isEmpty()) {
            tag.put("input", input.save(registries));
        }
        if (refineProgress > 0 || pendingVoxels > 0) {
            tag.putInt("refine_progress", refineProgress);
            tag.putInt("pending_voxels", pendingVoxels);
            tag.putInt("job_voxels", jobVoxels);
        }
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        input = tag.contains("input") ? ItemStack.parse(registries, tag.getCompound("input"))
                .orElse(ItemStack.EMPTY) : ItemStack.EMPTY;
        refineProgress = tag.getInt("refine_progress");
        int savedVoxels = Math.max(0, tag.getInt("pending_voxels"));
        if (refineProgress > 0 && !tag.contains("job_voxels", net.minecraft.nbt.Tag.TAG_INT)) {
            // M4 development saves used pending_voxels for the in-flight recipe value.
            jobVoxels = savedVoxels;
            pendingVoxels = 0;
        } else {
            jobVoxels = Math.max(0, tag.getInt("job_voxels"));
            pendingVoxels = savedVoxels;
        }
    }
}
