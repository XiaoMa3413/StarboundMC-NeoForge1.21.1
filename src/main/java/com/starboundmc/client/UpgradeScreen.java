package com.starboundmc.client;

import com.starboundmc.item.MatterManipulatorItem;
import com.starboundmc.item.MatterManipulatorModuleItem;
import com.starboundmc.menu.UpgradeMenu;
import com.starboundmc.network.ModNetwork;
import com.starboundmc.network.UpgradeMatterManipulatorPacket;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;

/**
 * Matter Manipulator upgrade workbench — a fully procedural industrial-sci-fi
 * panel (176x250, pixel-art style):
 *
 * <pre>
 * ┌────────────────────────────────┐
 * │ 物质枪升级工作台        PWR ▪▪ │
 * │   物质枪   ┌──┐                │
 * │            └──┘                │
 * │ 挖掘速度 1/3        [升级速度] │
 * │ ▪▪▪ 下一级需 2 模块            │
 * │ 激光射程 1/3        [升级射程] │
 * │ ...                            │
 * │ ─────────────────────────────  │
 * │ 背包                           │
 * │ [物品栏 3 行 + 快捷栏]         │
 * └────────────────────────────────┘
 * </pre>
 *
 * Every track row shows the name + level, square "pixel" pips for the current
 * level, and the module cost of the next level (amber = affordable, red =
 * not enough modules, gray = already maxed).
 */
public class UpgradeScreen extends AbstractContainerScreen<UpgradeMenu>
{
    private static final int PANEL_W = 176;
    private static final int PANEL_H = 250;

    // Palette shared with the other industrial-sci-fi screens (UiStyle).
    private static final int C_BG_TOP = UiStyle.C_BG_TOP;
    private static final int C_BG_BOTTOM = UiStyle.C_BG_BOTTOM;
    private static final int C_PLATE = UiStyle.C_PLATE;
    private static final int C_BORDER = UiStyle.C_BORDER;
    private static final int C_ACCENT = UiStyle.C_ACCENT;
    private static final int C_AMBER = UiStyle.C_AMBER;
    private static final int C_DANGER = UiStyle.C_DANGER;
    private static final int C_TEXT = UiStyle.C_TEXT;
    private static final int C_DIM = UiStyle.C_DIM;
    private static final int C_SLOT_BG = UiStyle.C_SLOT_BG;

    // Track rows (26px pitch): label + pips/cost on the left, button at right.
    private static final int ROW_Y0 = 54;
    private static final int ROW_PITCH = 26;
    private static final int BTN_X = 100;
    private static final int BTN_W = 70;
    private static final int BTN_H = 18;

    private Button speedButton;
    private Button rangeButton;
    private Button miningButton;
    private Button fortuneButton;

    public UpgradeScreen(UpgradeMenu menu, Inventory playerInventory, Component title)
    {
        super(menu, playerInventory, title);
        this.imageWidth = PANEL_W;
        this.imageHeight = PANEL_H;
    }

    @Override
    protected void init()
    {
        super.init();
        this.speedButton = new PixelButton(
                this.leftPos + BTN_X, this.topPos + ROW_Y0, BTN_W, BTN_H,
                Component.translatable("gui.starboundmc.upgrade_speed"),
                button -> ModNetwork.CHANNEL.sendToServer(new UpgradeMatterManipulatorPacket(UpgradeMenu.TRACK_SPEED)));
        this.rangeButton = new PixelButton(
                this.leftPos + BTN_X, this.topPos + ROW_Y0 + ROW_PITCH, BTN_W, BTN_H,
                Component.translatable("gui.starboundmc.upgrade_range"),
                button -> ModNetwork.CHANNEL.sendToServer(new UpgradeMatterManipulatorPacket(UpgradeMenu.TRACK_RANGE)));
        this.miningButton = new PixelButton(
                this.leftPos + BTN_X, this.topPos + ROW_Y0 + ROW_PITCH * 2, BTN_W, BTN_H,
                Component.translatable("gui.starboundmc.upgrade_mining"),
                button -> ModNetwork.CHANNEL.sendToServer(new UpgradeMatterManipulatorPacket(UpgradeMenu.TRACK_MINING)));
        this.fortuneButton = new PixelButton(
                this.leftPos + BTN_X, this.topPos + ROW_Y0 + ROW_PITCH * 3, BTN_W, BTN_H,
                Component.translatable("gui.starboundmc.upgrade_fortune"),
                button -> ModNetwork.CHANNEL.sendToServer(new UpgradeMatterManipulatorPacket(UpgradeMenu.TRACK_FORTUNE)));
        this.addRenderableWidget(this.speedButton);
        this.addRenderableWidget(this.rangeButton);
        this.addRenderableWidget(this.miningButton);
        this.addRenderableWidget(this.fortuneButton);
    }

    @Override
    public void containerTick()
    {
        super.containerTick();
        ItemStack stack = this.menu.getManipulatorContainer().getItem(0);
        boolean hasManipulator = stack.getItem() instanceof MatterManipulatorItem;
        this.speedButton.active = hasManipulator
                && MatterManipulatorItem.getSpeedLevel(stack) < MatterManipulatorItem.MAX_UPGRADES;
        this.rangeButton.active = hasManipulator
                && MatterManipulatorItem.getRangeLevel(stack) < MatterManipulatorItem.MAX_UPGRADES;
        this.miningButton.active = hasManipulator
                && MatterManipulatorItem.getMiningLevel(stack) < MatterManipulatorItem.MAX_MINING_LEVEL;
        this.fortuneButton.active = hasManipulator
                && MatterManipulatorItem.getFortuneLevel(stack) < MatterManipulatorItem.MAX_FORTUNE_UPGRADES;
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick)
    {
        super.render(graphics, mouseX, mouseY, partialTick);
        this.renderTooltip(graphics, mouseX, mouseY);
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY)
    {
        int x = this.leftPos;
        int y = this.topPos;

        // Panel background.
        graphics.fillGradient(x, y, x + PANEL_W, y + PANEL_H, C_BG_TOP, C_BG_BOTTOM);

        // 2px pixel frame.
        graphics.fill(x, y, x + PANEL_W, y + 2, C_BORDER);
        graphics.fill(x, y + PANEL_H - 2, x + PANEL_W, y + PANEL_H, C_BORDER);
        graphics.fill(x, y, x + 2, y + PANEL_H, C_BORDER);
        graphics.fill(x + PANEL_W - 2, y, x + PANEL_W, y + PANEL_H, C_BORDER);
        // Amber corner brackets (industrial touch).
        graphics.fill(x + 2, y + 2, x + 6, y + 6, C_AMBER);
        graphics.fill(x + PANEL_W - 6, y + 2, x + PANEL_W - 2, y + 6, C_AMBER);
        graphics.fill(x + 2, y + PANEL_H - 6, x + 6, y + PANEL_H - 2, C_AMBER);
        graphics.fill(x + PANEL_W - 6, y + PANEL_H - 6, x + PANEL_W - 2, y + PANEL_H - 2, C_AMBER);

        // Manipulator slot frame (menu slot at 80,16).
        int sx = x + 79;
        int sy = y + 15;
        graphics.fill(sx, sy, sx + 18, sy + 18, C_SLOT_BG);
        graphics.fill(sx, sy, sx + 18, sy + 1, C_BORDER);
        graphics.fill(sx, sy + 17, sx + 18, sy + 18, C_BORDER);
        graphics.fill(sx, sy, sx + 1, sy + 18, C_BORDER);
        graphics.fill(sx + 17, sy, sx + 18, sy + 18, C_BORDER);

        // Track row plates.
        for (int i = 0; i < 4; i++)
        {
            int ry = y + ROW_Y0 + ROW_PITCH * i;
            graphics.fill(x + 4, ry - 2, x + 92, ry + 21, C_PLATE);
            graphics.fill(x + 4, ry - 2, x + 92, ry - 1, C_BORDER);
            graphics.fill(x + 4, ry + 20, x + 92, ry + 21, C_BORDER);
        }

        // Separator before the player inventory.
        graphics.fill(x + 4, y + 154, x + 172, y + 155, C_BORDER);
        graphics.fill(x + 4, y + 155, x + 172, y + 156, C_AMBER);
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY)
    {
        // Title + power indicator (industrial).
        graphics.drawString(this.font, this.title, 8, 7, C_TEXT, true);
        graphics.fill(8, 19, 8 + this.font.width(this.title), 20, C_ACCENT);
        graphics.drawString(this.font, Component.literal("PWR"), 138, 7, C_DIM, true);
        graphics.fill(168, 8, 168 + 4, 8 + 8, C_AMBER);
        graphics.fill(172, 8, 172 + 4, 8 + 8, C_ACCENT);
        graphics.drawString(this.font, Component.translatable("gui.starboundmc.upgrade.slot_label"), 10, 22, C_DIM, true);

        ItemStack stack = this.menu.getManipulatorContainer().getItem(0);
        boolean has = stack.getItem() instanceof MatterManipulatorItem;
        int modules = countModules();

        renderTrack(graphics, ROW_Y0, "gui.starboundmc.speed_level",
                has ? MatterManipulatorItem.getSpeedLevel(stack) : 0,
                MatterManipulatorItem.MAX_UPGRADES, has, modules);
        renderTrack(graphics, ROW_Y0 + ROW_PITCH, "gui.starboundmc.range_level",
                has ? MatterManipulatorItem.getRangeLevel(stack) : 0,
                MatterManipulatorItem.MAX_UPGRADES, has, modules);
        renderTrack(graphics, ROW_Y0 + ROW_PITCH * 2, "gui.starboundmc.mining_level",
                has ? MatterManipulatorItem.getMiningLevel(stack) : 0,
                MatterManipulatorItem.MAX_MINING_LEVEL, has, modules);
        renderTrack(graphics, ROW_Y0 + ROW_PITCH * 3, "gui.starboundmc.fortune_level",
                has ? MatterManipulatorItem.getFortuneLevel(stack) : 0,
                MatterManipulatorItem.MAX_FORTUNE_UPGRADES, has, modules);

        graphics.drawString(this.font, this.playerInventoryTitle, 8, 161, C_DIM, true);
    }

    /** One upgrade track: "name X/Y" + square pips + next-level module cost. */
    private void renderTrack(GuiGraphics graphics, int rowY, String levelKey, int level, int max,
                             boolean hasManipulator, int modules)
    {
        graphics.drawString(this.font, Component.translatable(levelKey, level, max), 8, rowY, C_TEXT, true);

        int pipX = 8;
        for (int i = 0; i < max; i++)
        {
            graphics.fill(pipX, rowY + 11, pipX + 6, rowY + 17, i < level ? C_ACCENT : C_BORDER);
            pipX += 9;
        }
        pipX += 4;
        if (!hasManipulator)
            return;
        if (level >= max)
        {
            graphics.drawString(this.font, Component.translatable("gui.starboundmc.upgrade.maxed"),
                    pipX, rowY + 10, C_DIM, true);
        }
        else
        {
            int cost = UpgradeMenu.modulesRequiredForTargetLevel(level + 1);
            int color = modules >= cost ? C_AMBER : C_DANGER;
            graphics.drawString(this.font, Component.translatable("gui.starboundmc.upgrade.cost", cost),
                    pipX, rowY + 10, color, true);
        }
    }

    private int countModules()
    {
        if (this.minecraft.player == null)
            return 0;
        int total = 0;
        for (ItemStack stack : this.minecraft.player.getInventory().items)
        {
            if (stack.getItem() instanceof MatterManipulatorModuleItem)
                total += stack.getCount();
        }
        return total;
    }
}
