package com.starboundmc.client.shipai;

import com.lowdragmc.lowdraglib2.gui.ui.ModularUI;
import com.lowdragmc.lowdraglib2.gui.ui.UI;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

/** Test-only access to the real communication tree and timeline, never included in ordinary builds. */
public final class NovaBroadcastRenderFixture {
    private final NovaBroadcastTimeline timeline = new NovaBroadcastTimeline();
    private final NovaBroadcastHudRoot root = new NovaBroadcastHudRoot();
    private final ModularUI ui = ModularUI.of(UI.of(root,
            ResourceLocation.fromNamespaceAndPath("starboundmc", "lss/nova_broadcast_hud.lss")));
    private int pulses;

    public NovaBroadcastRenderFixture() {
        timeline.enqueue("message.starboundmc.nova.tutorial.voxel_use",
                Component.translatable("message.starboundmc.nova.tutorial.voxel_use").getString());
    }

    public void draw(GuiGraphics graphics, boolean paused) {
        pulses += timeline.tick(paused).revealedCount();
        if (!timeline.snapshot().visible()) return;
        if (ui.getScreenWidth() != graphics.guiWidth() || ui.getScreenHeight() != graphics.guiHeight())
            ui.init(graphics.guiWidth(), graphics.guiHeight());
        if (!paused) ui.tick();
        root.sync(timeline.snapshot(), pulses, timeline.presentationProgress(0));
        ui.getWidget().render(graphics, Integer.MAX_VALUE, Integer.MAX_VALUE, 0);
    }

    public void close() { ui.onRemoved(); }

    public boolean complete() { return timeline.queuedMessageCount() == 0; }
}
