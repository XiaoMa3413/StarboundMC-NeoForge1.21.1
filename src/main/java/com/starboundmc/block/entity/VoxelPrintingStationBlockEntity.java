package com.starboundmc.block.entity;

import com.starboundmc.block.ModBlockEntities;
import com.starboundmc.economy.VoxelWalletService;
import com.starboundmc.network.ModNetwork;
import com.starboundmc.network.SyncPrintQueuePacket;
import com.starboundmc.network.SyncVoxelMachinePacket;
import com.starboundmc.recipe.VoxelPrintingRecipe;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Deque;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;

/**
 * Voxel printing station logic: reserves matched inventory materials directly
 * from the operator, deducts any wallet-backed voxel material up front, then
 * produces the recipe result after the print duration. Only the output slot is
 * machine storage; reserved materials belong to active and queued jobs.
 */
public final class VoxelPrintingStationBlockEntity extends BlockEntity implements Container {
    public static final int TOTAL_SLOTS = 1;
    public static final int OUTPUT_SLOT = 0;
    public static final int MAX_OUTSTANDING_CRAFTS = 64;

    private final ItemStack[] items = new ItemStack[TOTAL_SLOTS];

    private int printProgress = 0;
    private int printTotalTicks = 0;
    private ItemStack pendingResult = ItemStack.EMPTY;
    private UUID activeTaskId;
    private UUID activeRequesterId;
    private String activeRequesterName = "";
    private ResourceLocation activeRecipeId;
    private int activeVoxelCost = 0;
    private List<ItemStack> activeMaterials = List.of();
    private final Deque<PrintQueueEntry> printQueue = new ArrayDeque<>();

    public enum PrintEnqueueResult {
        QUEUED,
        INVALID_QUANTITY,
        QUEUE_FULL,
        MISSING_MATERIALS,
        INSUFFICIENT_VOXELS
    }

    public enum QueueCancelResult {
        CANCELLED,
        NOT_FOUND,
        NOT_OWNER,
        ACTIVE
    }

    public VoxelPrintingStationBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.VOXEL_PRINTING_STATION.get(), pos, state);
        Arrays.fill(items, ItemStack.EMPTY);
    }

    public static void tick(Level level, BlockPos pos, BlockState state, VoxelPrintingStationBlockEntity station) {
        if (!(level instanceof ServerLevel serverLevel)
                || (station.printProgress <= 0 && station.pendingResult.isEmpty()
                && station.printQueue.isEmpty())) {
            return;
        }
        if (station.pendingResult.isEmpty()) {
            if (station.startNextQueuedCraft()) {
                station.setChanged();
                station.syncAll(serverLevel, pos);
            }
            return;
        }
        if (station.printProgress > 0) {
            station.printProgress--;
            if (station.printProgress > 0) {
                if (level.getGameTime() % 10 == 0) {
                    station.syncProgress(serverLevel, pos);
                }
                return;
            }
        }
        // Job complete: move the result into the output slot if possible.
        ItemStack output = station.items[OUTPUT_SLOT];
        if (!output.isEmpty()) {
            if (!ItemStack.isSameItemSameComponents(output, station.pendingResult)
                    || output.getCount() + station.pendingResult.getCount() > output.getMaxStackSize()) {
                // Output blocked: keep the completed result and retry on a later tick.
                station.printProgress = 0;
                station.setChanged();
                if (level.getGameTime() % 10 == 0) {
                    station.syncProgress(serverLevel, pos);
                }
                return;
            }
            output.grow(station.pendingResult.getCount());
        } else {
            station.items[OUTPUT_SLOT] = station.pendingResult.copy();
        }
        station.pendingResult = ItemStack.EMPTY;
        station.printTotalTicks = 0;
        station.clearActiveTask();
        station.setChanged();
        station.startNextQueuedCraft();
        station.syncAll(serverLevel, pos);
    }

    /** Reserves all resources up front and appends one requester-owned FIFO entry. */
    public PrintEnqueueResult tryEnqueuePrint(
            ServerLevel level, ServerPlayer operator, RecipeHolder<VoxelPrintingRecipe> recipe, int quantity) {
        if (quantity < 1 || quantity > 64) {
            return PrintEnqueueResult.INVALID_QUANTITY;
        }
        if (outstandingCrafts() + quantity > MAX_OUTSTANDING_CRAFTS) {
            return PrintEnqueueResult.QUEUE_FULL;
        }
        VoxelPrintingRecipe printing = recipe.value();
        List<ItemStack> simulated = new ArrayList<>(operator.getInventory().items.size());
        for (ItemStack stack : operator.getInventory().items) {
            simulated.add(stack.copy());
        }
        List<ReservedCraft> reservations = new ArrayList<>(quantity);
        for (int craft = 0; craft < quantity; craft++) {
            ReservedCraft reserved = reserveOneCraft(printing, simulated);
            if (reserved == null) {
                return PrintEnqueueResult.MISSING_MATERIALS;
            }
            reservations.add(reserved);
        }

        long voxelMaterialCount = printing.voxelMaterialCount();
        long totalCost = voxelMaterialCount * quantity;
        if (totalCost > Integer.MAX_VALUE
                || (totalCost > 0 && !VoxelWalletService.trySpend(operator, (int) totalCost))) {
            return PrintEnqueueResult.INSUFFICIENT_VOXELS;
        }

        ItemStack result = printing.getResultItem(level.registryAccess());
        for (int slot = 0; slot < operator.getInventory().items.size(); slot++) {
            operator.getInventory().items.set(slot, simulated.get(slot));
        }
        operator.getInventory().setChanged();
        printQueue.addLast(new PrintQueueEntry(UUID.randomUUID(), operator.getUUID(),
                operator.getGameProfile().getName(), recipe.id(), Math.toIntExact(voxelMaterialCount),
                printing.printSeconds() * 20, result, reservations));
        startNextQueuedCraft();
        setChanged();
        syncAll(level, worldPosition);
        return PrintEnqueueResult.QUEUED;
    }

    private static ReservedCraft reserveOneCraft(VoxelPrintingRecipe recipe, List<ItemStack> simulated) {
        return recipe.reserveMaterials(simulated)
                .map(ReservedCraft::new)
                .orElse(null);
    }

    private boolean canAcceptResult(ItemStack result) {
        ItemStack output = items[OUTPUT_SLOT];
        return output.isEmpty()
                || ItemStack.isSameItemSameComponents(output, result)
                && output.getCount() + result.getCount() <= output.getMaxStackSize();
    }

    private boolean startNextQueuedCraft() {
        if (!pendingResult.isEmpty() || printQueue.isEmpty()) {
            return false;
        }
        PrintQueueEntry entry = printQueue.peekFirst();
        if (!canAcceptResult(entry.result)) {
            return false;
        }
        ReservedCraft craft = entry.crafts.removeFirst();
        if (entry.crafts.isEmpty()) {
            printQueue.removeFirst();
        }
        activeTaskId = UUID.randomUUID();
        activeRequesterId = entry.requesterId;
        activeRequesterName = entry.requesterName;
        activeRecipeId = entry.recipeId;
        activeVoxelCost = entry.voxelCost;
        activeMaterials = List.copyOf(craft.materials);
        pendingResult = entry.result.copy();
        printTotalTicks = entry.printTicks;
        printProgress = printTotalTicks;
        return true;
    }

    private void clearActiveTask() {
        activeTaskId = null;
        activeRequesterId = null;
        activeRequesterName = "";
        activeRecipeId = null;
        activeVoxelCost = 0;
        activeMaterials = List.of();
    }

    public int outstandingCrafts() {
        int count = pendingResult.isEmpty() ? 0 : 1;
        for (PrintQueueEntry entry : printQueue) {
            count += entry.crafts.size();
        }
        return count;
    }

    /** Cancels only queued work owned by the requester; an active craft is never cancelled here. */
    public QueueCancelResult cancelQueuedPrint(ServerPlayer requester, UUID queueId) {
        if (queueId != null && queueId.equals(activeTaskId)) {
            return QueueCancelResult.ACTIVE;
        }
        Iterator<PrintQueueEntry> iterator = printQueue.iterator();
        while (iterator.hasNext()) {
            PrintQueueEntry entry = iterator.next();
            if (!entry.id.equals(queueId)) {
                continue;
            }
            if (!entry.requesterId.equals(requester.getUUID())) {
                return QueueCancelResult.NOT_OWNER;
            }
            int refund = Math.multiplyExact(entry.voxelCost, entry.crafts.size());
            iterator.remove();
            refundVoxels(requester, refund);
            for (ReservedCraft craft : entry.crafts) {
                returnMaterials(requester, craft.materials);
            }
            setChanged();
            if (level instanceof ServerLevel serverLevel) {
                syncAll(serverLevel, worldPosition);
            }
            return QueueCancelResult.CANCELLED;
        }
        return QueueCancelResult.NOT_FOUND;
    }

    private static void refundVoxels(ServerPlayer player, int amount) {
        int before = VoxelWalletService.balanceOf(player);
        VoxelWalletService.add(player, amount);
        int credited = VoxelWalletService.balanceOf(player) - before;
        int remainder = Math.max(0, amount - credited);
        if (remainder > 0) {
            VoxelWalletService.dropVoxels(player.level(), player.getX(), player.getY(), player.getZ(), remainder);
        }
    }

    private static void returnMaterials(ServerPlayer player, List<ItemStack> materials) {
        for (ItemStack reserved : materials) {
            ItemStack refund = reserved.copy();
            player.getInventory().add(refund);
            if (!refund.isEmpty()) {
                player.drop(refund, false);
            }
        }
    }

    private void syncProgress(ServerLevel level, BlockPos pos) {
        level.getChunkSource().blockChanged(pos);
        setChanged();
        ModNetwork.sendToPlayersTrackingChunk(level, new ChunkPos(pos), machineSnapshot(pos));
    }

    private SyncVoxelMachinePacket machineSnapshot(BlockPos pos) {
        return new SyncVoxelMachinePacket(
                pos, printProgress, printTotalTicks, 0,
                pendingResult.isEmpty()
                        ? net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("minecraft", "air")
                        : BuiltInRegistries.ITEM.getKey(pendingResult.getItem()),
                pendingResult.getCount());
    }

    private void syncQueue(ServerLevel level, BlockPos pos) {
        ModNetwork.sendToPlayersTrackingChunk(level, new ChunkPos(pos), queueSnapshot(pos));
    }

    private void syncAll(ServerLevel level, BlockPos pos) {
        syncProgress(level, pos);
        syncQueue(level, pos);
    }

    private SyncPrintQueuePacket queueSnapshot(BlockPos pos) {
        List<SyncPrintQueuePacket.Entry> entries = new ArrayList<>();
        if (!pendingResult.isEmpty()) {
            UUID taskId = java.util.Objects.requireNonNull(activeTaskId, "activeTaskId");
            UUID requesterId = java.util.Objects.requireNonNull(activeRequesterId, "activeRequesterId");
            entries.add(new SyncPrintQueuePacket.Entry(taskId, requesterId,
                    activeRequesterName.isBlank() ? "—" : activeRequesterName,
                    BuiltInRegistries.ITEM.getKey(pendingResult.getItem()),
                    pendingResult.getCount(), 1, true));
        }
        for (PrintQueueEntry entry : printQueue) {
            entries.add(new SyncPrintQueuePacket.Entry(entry.id, entry.requesterId,
                    entry.requesterName, BuiltInRegistries.ITEM.getKey(entry.result.getItem()),
                    entry.result.getCount(), entry.crafts.size(), false));
        }
        return new SyncPrintQueuePacket(pos, entries);
    }

    /** Sends the current job state to a player who has just opened the station. */
    public void syncTo(ServerPlayer viewer) {
        if (level instanceof ServerLevel serverLevel) {
            ModNetwork.sendToPlayer(viewer, machineSnapshot(worldPosition));
            ModNetwork.sendToPlayer(viewer, queueSnapshot(worldPosition));
        }
    }

    public int printProgress() {
        return printProgress;
    }

    public int printTotalTicks() {
        return printTotalTicks;
    }

    public boolean isPrinting() {
        return printProgress > 0;
    }

    /** Drops every reserved resource exactly once when the station is destroyed. */
    public void dropReservedResources(ServerLevel level, BlockPos pos) {
        double x = pos.getX() + 0.5;
        double y = pos.getY() + 0.5;
        double z = pos.getZ() + 0.5;
        if (!pendingResult.isEmpty()) {
            if (printProgress <= 0) {
                net.minecraft.world.Containers.dropItemStack(level, x, y, z, pendingResult.copy());
            } else {
                dropMaterials(level, x, y, z, activeMaterials);
                VoxelWalletService.dropVoxels(level, x, y, z, activeVoxelCost);
            }
        }
        for (PrintQueueEntry entry : printQueue) {
            for (ReservedCraft craft : entry.crafts) {
                dropMaterials(level, x, y, z, craft.materials);
            }
            VoxelWalletService.dropVoxels(level, x, y, z,
                    Math.multiplyExact(entry.voxelCost, entry.crafts.size()));
        }
        printQueue.clear();
        pendingResult = ItemStack.EMPTY;
        printProgress = 0;
        printTotalTicks = 0;
        clearActiveTask();
        setChanged();
    }

    private static void dropMaterials(
            Level level, double x, double y, double z, List<ItemStack> materials) {
        for (ItemStack stack : materials) {
            net.minecraft.world.Containers.dropItemStack(level, x, y, z, stack.copy());
        }
    }

    @Override
    public int getContainerSize() {
        return TOTAL_SLOTS;
    }

    @Override
    public boolean isEmpty() {
        for (ItemStack stack : items) {
            if (!stack.isEmpty()) {
                return false;
            }
        }
        return true;
    }

    @Override
    public ItemStack getItem(int index) {
        return index >= 0 && index < items.length ? items[index] : ItemStack.EMPTY;
    }

    @Override
    public ItemStack removeItem(int index, int count) {
        if (index >= 0 && index < items.length && !items[index].isEmpty()) {
            ItemStack removed = items[index].split(count);
            if (!removed.isEmpty()) {
                setChanged();
            }
            return removed;
        }
        return ItemStack.EMPTY;
    }

    @Override
    public ItemStack removeItemNoUpdate(int index) {
        if (index >= 0 && index < items.length) {
            ItemStack stack = items[index];
            items[index] = ItemStack.EMPTY;
            if (!stack.isEmpty()) {
                setChanged();
            }
            return stack;
        }
        return ItemStack.EMPTY;
    }

    @Override
    public void setItem(int index, ItemStack stack) {
        if (index < 0 || index >= items.length) {
            return;
        }
        items[index] = stack;
        if (stack.getCount() > getMaxStackSize()) {
            stack.setCount(getMaxStackSize());
        }
        setChanged();
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
        Arrays.fill(items, ItemStack.EMPTY);
        setChanged();
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putInt("storage_version", 1);
        if (!items[OUTPUT_SLOT].isEmpty()) tag.put("output", items[OUTPUT_SLOT].save(registries));
        if (printProgress > 0 || !pendingResult.isEmpty()) {
            tag.putInt("print_progress", printProgress);
            tag.putInt("print_total", printTotalTicks);
            if (!pendingResult.isEmpty()) {
                tag.put("pending_result", pendingResult.save(registries));
            }
            if (activeTaskId != null) {
                tag.putUUID("active_task_id", activeTaskId);
                tag.putUUID("active_requester_id", activeRequesterId);
                tag.putString("active_requester_name", activeRequesterName);
                tag.putString("active_recipe_id", activeRecipeId.toString());
                tag.putInt("active_voxel_cost", activeVoxelCost);
                tag.put("active_materials", saveStacks(activeMaterials, registries));
            }
        }
        if (!printQueue.isEmpty()) {
            ListTag queueTag = new ListTag();
            for (PrintQueueEntry entry : printQueue) {
                CompoundTag entryTag = new CompoundTag();
                entryTag.putUUID("id", entry.id);
                entryTag.putUUID("requester_id", entry.requesterId);
                entryTag.putString("requester_name", entry.requesterName);
                entryTag.putString("recipe_id", entry.recipeId.toString());
                entryTag.putInt("voxel_cost", entry.voxelCost);
                entryTag.putInt("print_ticks", entry.printTicks);
                entryTag.put("result", entry.result.save(registries));
                ListTag craftsTag = new ListTag();
                for (ReservedCraft craft : entry.crafts) {
                    CompoundTag craftTag = new CompoundTag();
                    craftTag.put("materials", saveStacks(craft.materials, registries));
                    craftsTag.add(craftTag);
                }
                entryTag.put("crafts", craftsTag);
                queueTag.add(entryTag);
            }
            tag.put("print_queue", queueTag);
        }
    }

    private static ListTag saveStacks(List<ItemStack> stacks, HolderLookup.Provider registries) {
        ListTag list = new ListTag();
        for (ItemStack stack : stacks) {
            if (!stack.isEmpty()) {
                list.add(stack.save(registries));
            }
        }
        return list;
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        require(tag, "storage_version", Tag.TAG_INT);
        if (tag.getInt("storage_version") != 1 || tag.contains("items"))
            throw new IllegalArgumentException("Unsupported pre-release printer storage");
        Arrays.fill(items, ItemStack.EMPTY);
        if (tag.contains("output")) items[OUTPUT_SLOT] = loadStack(tag, "output", registries);
        printProgress = printTotalTicks = 0;
        pendingResult = ItemStack.EMPTY;
        printQueue.clear();
        clearActiveTask();
        if (tag.contains("pending_result")) {
            pendingResult = loadStack(tag, "pending_result", registries);
            printTotalTicks = positiveInt(tag, "print_total");
            printProgress = nonNegativeInt(tag, "print_progress");
            if (printProgress > printTotalTicks) throw new IllegalArgumentException("Invalid printer progress");
            activeTaskId = uuid(tag, "active_task_id");
            activeRequesterId = uuid(tag, "active_requester_id");
            activeRequesterName = text(tag, "active_requester_name");
            activeRecipeId = ResourceLocation.parse(text(tag, "active_recipe_id"));
            activeVoxelCost = nonNegativeInt(tag, "active_voxel_cost");
            activeMaterials = loadStacks(list(tag, "active_materials"), registries);
        } else if (tag.contains("print_progress") || tag.contains("print_total") || tag.contains("active_task_id")) {
            throw new IllegalArgumentException("Printer task is missing its reserved result");
        }
        if (tag.contains("print_queue")) {
            ListTag queueTag = list(tag, "print_queue");
            int loadedCrafts = pendingResult.isEmpty() ? 0 : 1;
            for (int i = 0; i < queueTag.size(); i++) {
                CompoundTag entry = queueTag.getCompound(i);
                ListTag craftsTag = list(entry, "crafts");
                loadedCrafts += craftsTag.size();
                if (craftsTag.isEmpty() || loadedCrafts > MAX_OUTSTANDING_CRAFTS)
                    throw new IllegalArgumentException("Invalid printer reservation count");
                List<ReservedCraft> crafts = new ArrayList<>();
                for (int j = 0; j < craftsTag.size(); j++)
                    crafts.add(new ReservedCraft(loadStacks(list(craftsTag.getCompound(j), "materials"), registries)));
                Math.multiplyExact(nonNegativeInt(entry, "voxel_cost"), crafts.size());
                printQueue.addLast(new PrintQueueEntry(uuid(entry, "id"), uuid(entry, "requester_id"),
                        text(entry, "requester_name"), ResourceLocation.parse(text(entry, "recipe_id")),
                        nonNegativeInt(entry, "voxel_cost"), positiveInt(entry, "print_ticks"),
                        loadStack(entry, "result", registries), crafts));
            }
        }
    }

    private static void require(CompoundTag tag, String key, int type) {
        if (!tag.contains(key, type)) throw new IllegalArgumentException("Missing or invalid printer field: " + key);
    }

    private static String text(CompoundTag tag, String key) {
        require(tag, key, Tag.TAG_STRING);
        String value = tag.getString(key);
        if (value.isBlank()) throw new IllegalArgumentException("Empty printer field: " + key);
        return value;
    }

    private static UUID uuid(CompoundTag tag, String key) {
        if (!tag.hasUUID(key)) throw new IllegalArgumentException("Missing printer UUID: " + key);
        return tag.getUUID(key);
    }

    private static int nonNegativeInt(CompoundTag tag, String key) {
        require(tag, key, Tag.TAG_INT);
        int value = tag.getInt(key);
        if (value < 0) throw new IllegalArgumentException("Negative printer field: " + key);
        return value;
    }

    private static int positiveInt(CompoundTag tag, String key) {
        int value = nonNegativeInt(tag, key);
        if (value == 0) throw new IllegalArgumentException("Zero printer field: " + key);
        return value;
    }

    private static ListTag list(CompoundTag tag, String key) {
        require(tag, key, Tag.TAG_LIST);
        ListTag value = (ListTag) tag.get(key);
        if (!value.isEmpty() && value.getElementType() != Tag.TAG_COMPOUND)
            throw new IllegalArgumentException("Invalid printer list: " + key);
        return value;
    }

    private static ItemStack loadStack(CompoundTag tag, String key, HolderLookup.Provider registries) {
        require(tag, key, Tag.TAG_COMPOUND);
        return parseStack(tag.getCompound(key), registries);
    }

    private static ItemStack parseStack(CompoundTag tag, HolderLookup.Provider registries) {
        ItemStack stack = ItemStack.parse(registries, tag).orElseThrow(
                () -> new IllegalArgumentException("Invalid reserved printer item: " + tag));
        if (stack.isEmpty() || stack.getCount() > stack.getMaxStackSize())
            throw new IllegalArgumentException("Invalid printer item count");
        return stack;
    }

    private static List<ItemStack> loadStacks(ListTag list, HolderLookup.Provider registries) {
        List<ItemStack> stacks = new ArrayList<>();
        for (int i = 0; i < list.size(); i++) stacks.add(parseStack(list.getCompound(i), registries));
        return List.copyOf(stacks);
    }

    private static final class ReservedCraft {
        private final List<ItemStack> materials;

        private ReservedCraft(List<ItemStack> materials) {
            this.materials = List.copyOf(materials);
        }
    }

    private static final class PrintQueueEntry {
        private final UUID id;
        private final UUID requesterId;
        private final String requesterName;
        private final ResourceLocation recipeId;
        private final int voxelCost;
        private final int printTicks;
        private final ItemStack result;
        private final Deque<ReservedCraft> crafts;

        private PrintQueueEntry(UUID id, UUID requesterId, String requesterName,
                                ResourceLocation recipeId, int voxelCost, int printTicks,
                                ItemStack result, List<ReservedCraft> crafts) {
            this.id = id;
            this.requesterId = requesterId;
            this.requesterName = requesterName == null || requesterName.isBlank() ? "—"
                    : requesterName.substring(0, Math.min(64, requesterName.length()));
            this.recipeId = recipeId;
            this.voxelCost = voxelCost;
            this.printTicks = printTicks;
            this.result = result.copy();
            this.crafts = new ArrayDeque<>(crafts);
        }
    }
}
