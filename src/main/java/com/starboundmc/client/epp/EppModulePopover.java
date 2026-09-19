package com.starboundmc.client.epp;

import com.starboundmc.StarboundMC;
import com.starboundmc.epp.EppItem;
import com.starboundmc.epp.EppModuleItem;
import com.starboundmc.item.ModDataComponents;
import com.starboundmc.network.EppModuleClickPacket;
import com.starboundmc.network.ModNetwork;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemContainerContents;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ScreenEvent;
import net.neoforged.neoforge.client.event.RenderTooltipEvent;

/** A stable, interactive tooltip anchored to a real menu slot, never a second inventory. */
@EventBusSubscriber(modid = StarboundMC.MODID, value = Dist.CLIENT)
public final class EppModulePopover {
    private static AbstractContainerScreen<?> screen;
    private static Slot source;
    private static int x, y, width, anchorX, anchorY;
    private static final int HEIGHT = 43;
    private static boolean active;

    private static void clear() { active = false; source = null; screen = null; }
    private static boolean inside(double mx, double my) {
        return mx >= x && mx < x + width && my >= y && my < y + HEIGHT;
    }
    private static boolean bridge(double mx, double my) {
        return mx >= Math.min(anchorX, x) - 2 && mx < Math.max(anchorX + 16, x + width) + 2
                && my >= Math.min(anchorY, y) - 2 && my < Math.max(anchorY + 16, y + HEIGHT) + 2;
    }
    @SubscribeEvent public static void render(ScreenEvent.Render.Post event) {
        var mc = Minecraft.getInstance();
        if (event.getScreen() instanceof CreativeModeInventoryScreen
                || !(event.getScreen() instanceof AbstractContainerScreen<?> current) || mc.player == null
                || current.getMenu() != mc.player.containerMenu || mc.player.isSpectator()) { clear(); return; }
        var hovered = current.getSlotUnderMouse();
        boolean holdingModule = current.getMenu().getCarried().isEmpty()
                || current.getMenu().getCarried().getItem() instanceof EppModuleItem;
        if (screen != current) clear();
        if (active && (!holdingModule || !(source.getItem().getItem() instanceof EppItem)
                || !bridge(event.getMouseX(), event.getMouseY()))) clear();
        if (!active && holdingModule && hovered != null && hovered.isActive()
                && hovered.getItem().getItem() instanceof EppItem chassis && chassis.moduleSlots() > 0
                && hovered.mayPickup(mc.player) && hovered.mayPlace(hovered.getItem())) {
            screen = current; source = hovered; active = true;
            anchorX = current.getGuiLeft() + source.x; anchorY = current.getGuiTop() + source.y;
            width = Math.max(88, mc.font.width(Component.translatable("gui.starboundmc.epp.modules")) + 12);
            x = Math.clamp(anchorX + 20, 4, Math.max(4, current.width - width - 4));
            y = Math.clamp(anchorY - 8, 4, Math.max(4, current.height - HEIGHT - 4));
        }
        if (!active) return;
        var g = event.getGuiGraphics();
        var chassis = (EppItem) source.getItem().getItem();
        var modules = source.getItem().getOrDefault(ModDataComponents.EPP_MODULES, ItemContainerContents.EMPTY).stream().toList();
        g.pose().pushPose(); g.pose().translate(0, 0, 500);
        g.fill(x - 1, y - 1, x + width + 1, y + HEIGHT + 1, 0xFFE0E0E0);
        g.fill(x, y, x + width, y + HEIGHT, 0xF21B1B1B);
        g.drawString(mc.font, Component.translatable("gui.starboundmc.epp.modules"), x + 6, y + 5, 0xFFEAEAEA, false);
        for (int i = 0; i < chassis.moduleSlots(); i++) {
            int sx = x + 6 + i * 22, sy = y + 19;
            boolean hover = event.getMouseX() >= sx && event.getMouseX() < sx + 18
                    && event.getMouseY() >= sy && event.getMouseY() < sy + 18;
            var carried = current.getMenu().getCarried();
            boolean compatible = carried.isEmpty() || carried.getItem() instanceof EppModuleItem module && module.compatibleWith(chassis);
            g.fill(sx, sy, sx + 18, sy + 18, hover ? compatible ? 0xFFCCCCCC : 0xFFE28373 : 0xFF666666);
            g.fill(sx + 1, sy + 1, sx + 17, sy + 17, 0xFF292929);
            ItemStack item = i < modules.size() ? modules.get(i) : ItemStack.EMPTY;
            if (!item.isEmpty()) g.renderItem(item, sx + 1, sy + 1);
            if (hover) g.renderTooltip(mc.font, !item.isEmpty() ? item.getHoverName()
                    : Component.translatable(compatible ? "gui.starboundmc.epp.empty_module" : "gui.starboundmc.epp.incompatible_module"),
                    event.getMouseX(), y + HEIGHT);
        }
        if (inside(event.getMouseX(), event.getMouseY()) && !current.getMenu().getCarried().isEmpty())
            g.renderItem(current.getMenu().getCarried(), event.getMouseX() - 8, event.getMouseY() - 8);
        g.pose().popPose();
    }
    @SubscribeEvent public static void tooltip(RenderTooltipEvent.Pre event) {
        if (active && Minecraft.getInstance().screen == screen && event.getItemStack() == source.getItem())
            event.setCanceled(true);
    }
    @SubscribeEvent public static void click(ScreenEvent.MouseButtonPressed.Pre event) {
        if (!active || event.getScreen() != screen || !inside(event.getMouseX(), event.getMouseY())) return;
        event.setCanceled(true);
        if (event.getButton() != 0 || !(source.getItem().getItem() instanceof EppItem chassis)) return;
        for (int i = 0; i < chassis.moduleSlots(); i++) {
            int sx = x + 6 + i * 22;
            if (event.getMouseX() >= sx && event.getMouseX() < sx + 18
                    && event.getMouseY() >= y + 19 && event.getMouseY() < y + 37) {
                ModNetwork.sendToServer(new EppModuleClickPacket(screen.getMenu().containerId,
                        screen.getMenu().getStateId(), source.index, i));
                return;
            }
        }
    }
    @SubscribeEvent public static void release(ScreenEvent.MouseButtonReleased.Pre event) {
        if (active && event.getScreen() == screen && inside(event.getMouseX(), event.getMouseY())) event.setCanceled(true);
    }
    @SubscribeEvent public static void scroll(ScreenEvent.MouseScrolled.Pre event) {
        if (active && event.getScreen() == screen && inside(event.getMouseX(), event.getMouseY())) event.setCanceled(true);
    }
    @SubscribeEvent public static void drag(ScreenEvent.MouseDragged.Pre event) {
        if (active && event.getScreen() == screen && inside(event.getMouseX(), event.getMouseY())) event.setCanceled(true);
    }
}
