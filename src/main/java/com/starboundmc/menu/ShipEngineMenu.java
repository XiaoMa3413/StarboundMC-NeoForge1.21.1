package com.starboundmc.menu;

import com.starboundmc.block.entity.ShipEngineBlockEntity;
import com.starboundmc.block.ModBlocks;
import com.starboundmc.item.ModItems;
import com.starboundmc.story.CoreState;
import com.starboundmc.story.EngineState;
import com.starboundmc.story.MineralScanState;
import com.starboundmc.story.ShipStoryService;
import com.starboundmc.story.SurfaceMissionState;
import com.starboundmc.warp.ShipStateData;
import com.starboundmc.world.ShipDimensions;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.TagKey;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.DataSlot;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/** One module socket + 36 vanilla inventory slots. All display values use vanilla menu synchronization. */
public final class ShipEngineMenu extends AbstractContainerMenu {
    public static final int WIDTH = 280, HEIGHT = 232;
    public static final TagKey<Item> REPAIR_MODULES = TagKey.create(Registries.ITEM,
            ResourceLocation.fromNamespaceAndPath("starboundmc", "engine_repair_modules"));
    public enum Status { DISCONNECTED, INCOMPATIBLE, PREREQUISITES, NEED_UPGRADE, AWAITING_CORE,
        UNSUPPORTED_MODULE, IGNITING, ONLINE }

    private final Container socket;
    private final Player owner;
    private final DataSlot status = DataSlot.standalone();
    private final DataSlot remaining = DataSlot.standalone();
    private final DataSlot hyperdrive = DataSlot.standalone();

    public ShipEngineMenu(int id, Inventory inventory) {
        this(id, inventory, new SimpleContainer(1));
    }

    public ShipEngineMenu(int id, Inventory inventory, Container socket) {
        super(ModMenus.SHIP_ENGINE_MENU.get(), id);
        this.socket = socket;
        this.owner = inventory.player;
        checkContainerSize(socket, 1);
        addSlot(new Slot(socket, 0, 58, 70) {
            @Override public boolean mayPlace(ItemStack stack) { return acceptsModule(stack); }
        });
        for (int row = 0; row < 3; row++)
            for (int col = 0; col < 9; col++)
                addSlot(new Slot(inventory, 9 + row * 9 + col, 60 + col * 18, 146 + row * 18));
        for (int col = 0; col < 9; col++) addSlot(new Slot(inventory, col, 60 + col * 18, 204));
        addDataSlot(status);
        addDataSlot(remaining);
        addDataSlot(hyperdrive);
        refreshState();
    }

    public static boolean acceptsModule(ItemStack stack) { return stack.is(REPAIR_MODULES); }
    public boolean isBoundTo(ShipEngineBlockEntity engine) { return socket == engine; }
    public Status status() {
        int id = status.get();
        return id >= 0 && id < Status.values().length ? Status.values()[id] : Status.INCOMPATIBLE;
    }
    public int remainingTicks() { return remaining.get(); }
    public boolean hyperdriveOnline() { return hyperdrive.get() == EngineState.ONLINE.networkId(); }

    @Override public void broadcastChanges() {
        if (owner instanceof ServerPlayer player && player.containerMenu == this && stillValid(player)
                && socket instanceof ShipEngineBlockEntity engine)
            ShipStoryService.installSublightCore(player, engine);
        refreshState();
        super.broadcastChanges();
    }

    private void refreshState() {
        if (!(owner instanceof ServerPlayer player) || player.getServer() == null) return;
        var shared = ShipStateData.get(player.getServer()).getStoryProgress();
        hyperdrive.set(shared.hyperdrive().networkId());
        remaining.set((int) Math.clamp(shared.sublightIgnitionCompleteGameTime()
                - player.getServer().overworld().getGameTime(), 0L, ShipStoryService.SUBLIGHT_IGNITION_TICKS));
        Status value;
        if (!player.level().dimension().equals(ShipDimensions.SHIP_LEVEL)) value = Status.DISCONNECTED;
        else if (!shared.isWritable()) value = Status.INCOMPATIBLE;
        else if (shared.sublightEngine() == EngineState.ONLINE) value = Status.ONLINE;
        else if (shared.sublightEngine() == EngineState.IGNITING) value = Status.IGNITING;
        else if (shared.core() != CoreState.ONLINE || shared.surfaceMission() != SurfaceMissionState.COMPLETE
                || shared.mineralScan() != MineralScanState.COMPLETE) value = Status.PREREQUISITES;
        else if (!ShipStoryService.hasUpgradedManipulator(player)) value = Status.NEED_UPGRADE;
        else if (!socket.isEmpty() && !socket.getItem(0).is(ModItems.SUBLIGHT_IGNITION_CORE.get()))
            value = Status.UNSUPPORTED_MODULE;
        else value = Status.AWAITING_CORE;
        status.set(value.ordinal());
    }

    @Override public boolean stillValid(Player player) {
        return socket instanceof ShipEngineBlockEntity engine && engine.stillValid(player)
                && player.level().getBlockState(engine.getBlockPos()).is(ModBlocks.SHIP_ENGINE_UNIT.get());
    }

    @Override public ItemStack quickMoveStack(Player player, int index) {
        if (index < 0 || index >= slots.size()) return ItemStack.EMPTY;
        Slot slot = slots.get(index);
        if (!slot.hasItem()) return ItemStack.EMPTY;
        ItemStack stack = slot.getItem(), original = stack.copy();
        boolean moved;
        if (index == 0) moved = moveItemStackTo(stack, 1, 37, true);
        else if (acceptsModule(stack)) moved = moveItemStackTo(stack, 0, 1, false);
        else if (index < 28) moved = moveItemStackTo(stack, 28, 37, false);
        else moved = moveItemStackTo(stack, 1, 28, false);
        if (!moved) return ItemStack.EMPTY;
        if (stack.isEmpty()) slot.set(ItemStack.EMPTY); else slot.setChanged();
        slot.onTake(player, stack);
        return original;
    }
}
