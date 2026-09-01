package com.starboundmc.client.shipai;

import com.lowdragmc.lowdraglib2.gui.hud.ModularHudLayer;
import com.lowdragmc.lowdraglib2.gui.ui.ModularUI;
import com.lowdragmc.lowdraglib2.gui.ui.UI;
import com.starboundmc.StarboundMC;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;

/** LDLib2-backed lower-left HUD for N.O.V.A. remote transmissions. */
public final class NovaBroadcastHudLayer implements ModularHudLayer
{
    public static final NovaBroadcastHudLayer INSTANCE = new NovaBroadcastHudLayer();
    private static final ResourceLocation STYLESHEET = ResourceLocation.fromNamespaceAndPath(
            StarboundMC.MODID, "lss/nova_broadcast_hud.lss");

    @Nullable
    private ModularUI modularUI;
    @Nullable
    private NovaBroadcastHudRoot root;

    private NovaBroadcastHudLayer()
    {
    }

    @Nullable
    @Override
    public ModularUI getModularUI()
    {
        if (!ClientNovaBroadcastState.shouldRender())
            return null;
        if (modularUI == null)
            buildUI();
        return modularUI;
    }

    @Override
    public void render(GuiGraphics graphics, DeltaTracker deltaTracker)
    {
        ModularUI current = getModularUI();
        if (current == null || root == null || !validModularUI(current))
            return;

        root.sync(ClientNovaBroadcastState.snapshot(),
                ClientNovaBroadcastState.textPulseSequence());
        float partialTick = deltaTracker.getGameTimeDeltaPartialTick(false);
        current.getWidget().render(graphics, Integer.MAX_VALUE, Integer.MAX_VALUE, partialTick);
    }

    /** Releases connection-local layout and animation state. */
    public void resetConnectionState()
    {
        if (modularUI != null)
            modularUI.onRemoved();
        modularUI = null;
        root = null;
    }

    private void buildUI()
    {
        root = new NovaBroadcastHudRoot();
        modularUI = ModularUI.of(UI.of(root, STYLESHEET));
    }
}
