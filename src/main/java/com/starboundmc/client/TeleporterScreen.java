package com.starboundmc.client;

import com.starboundmc.menu.TeleporterMenu;
import com.starboundmc.network.ModNetwork;
import com.starboundmc.network.TeleporterRenamePacket;
import com.starboundmc.network.TeleporterUsePacket;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

import java.util.ArrayList;
import java.util.List;

/** Destination-selection UI for the unified teleporter block. */
public class TeleporterScreen extends AbstractContainerScreen<TeleporterMenu>
{
    private static final int VISIBLE = 4;

    private final List<String[]> destinations = new ArrayList<>();
    private final List<SciFiButton> destButtons = new ArrayList<>();
    private EditBox nameBox;
    private SciFiButton saveButton;
    private SciFiButton upButton;
    private SciFiButton downButton;
    private int scroll = 0;

    public TeleporterScreen(TeleporterMenu menu, Inventory playerInventory, Component title)
    {
        super(menu, playerInventory, title);
        this.imageWidth = 176;
        this.imageHeight = 166;
    }

    @Override
    protected void init()
    {
        super.init();
        this.nameBox = new EditBox(this.font, this.leftPos + 8, this.topPos + 129, 96, 18, Component.literal(""));
        this.nameBox.setMaxLength(24);
        this.addRenderableWidget(this.nameBox);

        this.saveButton = new SciFiButton(this.leftPos + 110, this.topPos + 128, 58, 20,
                Component.translatable("gui.starboundmc.teleporter.save"),
                button -> ModNetwork.CHANNEL.sendToServer(new TeleporterRenamePacket(this.nameBox.getValue())));
        this.addRenderableWidget(this.saveButton);

        this.upButton = new SciFiButton(this.leftPos + 152, this.topPos + 30, 16, 16, Component.literal("▲"),
                button ->
                {
                    this.scroll = Math.max(0, this.scroll - 1);
                    this.rebuildDestinations();
                });
        this.downButton = new SciFiButton(this.leftPos + 152, this.topPos + 96, 16, 16, Component.literal("▼"),
                button ->
                {
                    this.scroll = Math.min(this.destinations.size() - VISIBLE, this.scroll + 1);
                    this.rebuildDestinations();
                });
        this.addRenderableWidget(this.upButton);
        this.addRenderableWidget(this.downButton);

        this.receiveState();
    }

    @Override
    public void containerTick()
    {
        super.containerTick();
        if (ClientTeleporterState.consumeDirty())
        {
            this.receiveState();
        }
    }

    private void receiveState()
    {
        this.destinations.clear();
        this.destinations.addAll(ClientTeleporterState.getDestinations());
        this.nameBox.setValue(ClientTeleporterState.getCurrentName());
        this.scroll = 0;
        this.rebuildDestinations();
    }

    private void rebuildDestinations()
    {
        for (SciFiButton button : this.destButtons)
        {
            this.removeWidget(button);
        }
        this.destButtons.clear();

        int max = Math.max(0, this.destinations.size() - VISIBLE);
        this.scroll = Math.min(this.scroll, max);
        for (int i = 0; i < VISIBLE; i++)
        {
            int index = this.scroll + i;
            if (index >= this.destinations.size())
                break;
            String[] entry = this.destinations.get(index);
            Component label = Component.literal(destinationLabel(entry));
            final String key = entry[1];
            SciFiButton button = new SciFiButton(this.leftPos + 8, this.topPos + 30 + i * 22, 140, 20, label,
                    b -> ModNetwork.CHANNEL.sendToServer(new TeleporterUsePacket(key)));
            this.destButtons.add(button);
            this.addRenderableWidget(button);
        }
        this.upButton.active = this.scroll > 0;
        this.downButton.active = this.scroll < max;
    }

    private String destinationLabel(String[] entry)
    {
        if ("0".equals(entry[0]))
            return Component.translatable("gui.starboundmc.teleporter.ship").getString();
        if ("1".equals(entry[0]))
            return Component.translatable("gui.starboundmc.teleporter.planet").getString();
        return entry[2];
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
        graphics.fillGradient(0, 0, this.width, this.height, 0xFF141A26, 0xFF0A0E14);
        graphics.fill(this.leftPos, this.topPos, this.leftPos + this.imageWidth, this.topPos + this.imageHeight, 0xFF1B222E);
        graphics.fill(this.leftPos, this.topPos, this.leftPos + this.imageWidth, this.topPos + 1, 0xFF33506B);
        graphics.fill(this.leftPos, this.topPos + this.imageHeight - 1, this.leftPos + this.imageWidth, this.topPos + this.imageHeight, 0xFF33506B);
        graphics.fill(this.leftPos, this.topPos, this.leftPos + 1, this.topPos + this.imageHeight, 0xFF33506B);
        graphics.fill(this.leftPos + this.imageWidth - 1, this.topPos, this.leftPos + this.imageWidth, this.topPos + this.imageHeight, 0xFF33506B);
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY)
    {
        graphics.drawString(this.font, this.title, this.titleLabelX, this.titleLabelY, 0xE8E8E8, true);
        graphics.drawString(this.font, Component.translatable("gui.starboundmc.teleporter.name"),
                8, 120, 0x9AA0A6, true);
    }
}
