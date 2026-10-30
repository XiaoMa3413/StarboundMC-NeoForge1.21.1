package com.starboundmc.client.shipai;

import com.lowdragmc.lowdraglib2.client.shader.LDLibShaders;
import com.lowdragmc.lowdraglib2.gui.ui.ModularUI;
import com.lowdragmc.lowdraglib2.gui.ui.UI;
import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Button;
import com.lowdragmc.lowdraglib2.gui.ui.elements.ScrollerView;
import com.lowdragmc.lowdraglib2.utils.TextUtilities;
import com.starboundmc.StarboundMC;
import com.starboundmc.network.ShipStorySnapshotPacket;
import com.starboundmc.story.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ScreenEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import org.lwjgl.glfw.GLFW;

import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

/** Opt-in rendering of the production tree. No world, server or player save is opened. */
@EventBusSubscriber(modid = StarboundMC.MODID, value = Dist.CLIENT)
public final class NovaTerminalRenderSmoke {
    private record Case(String language, int scale, String mode) {}
    private static final List<Case> CASES = new ArrayList<>();
    static {
        for (String language : List.of("zh_cn", "en_us")) {
            for (int scale = 2; scale <= 4; scale++)
                for (String mode : List.of("comms", "tasks")) CASES.add(new Case(language, scale, mode));
            for (String mode : List.of("rewards", "locked", "archive", "history", "topics", "offline"))
                CASES.add(new Case(language, 4, mode));
        }
    }
    private static int stage, frames;
    private static ModularUI ui;
    private static ShipAiTerminalRoot root;
    private static boolean started, finished;

    @SubscribeEvent
    public static void tick(ClientTickEvent.Pre event) {
        var mc = Minecraft.getInstance();
        if (!Boolean.getBoolean("starboundmc.debug.novaTerminalSmoke") || finished
                || mc.level != null || mc.screen == null || mc.getOverlay() != null || LDLibShaders.getGuiTexture() == null) return;
        Case test = CASES.get(stage);
        if (!mc.getLanguageManager().getSelected().equals(test.language())) {
            mc.getLanguageManager().setSelected(test.language());
            mc.options.languageCode = test.language();
            mc.reloadResourcePacks();
            return;
        }
        if (mc.options.guiScale().get() != test.scale()) {
            mc.options.guiScale().set(test.scale());
            mc.resizeDisplay();
        }
    }

    @SubscribeEvent
    public static void render(ScreenEvent.Render.Pre event) throws Exception {
        var mc = Minecraft.getInstance();
        if (!Boolean.getBoolean("starboundmc.debug.novaTerminalSmoke") || finished
                || mc.level != null || mc.getOverlay() != null || LDLibShaders.getGuiTexture() == null) return;
        event.setCanceled(true);
        if (!started) {
            Files.deleteIfExists(mc.gameDirectory.toPath().resolve("screenshots/passed.txt"));
            started = true;
        }
        GLFW.glfwHideWindow(mc.getWindow().getWindow());
        mc.options.pauseOnLostFocus = false;
        Case test = CASES.get(stage);
        if (!mc.getLanguageManager().getSelected().equals(test.language()) || mc.options.guiScale().get() != test.scale()) return;
        if (ui == null) setup(test);
        GuiGraphics g = event.getGuiGraphics();
        if (ui.getScreenWidth() != g.guiWidth() || ui.getScreenHeight() != g.guiHeight()) ui.init(g.guiWidth(), g.guiHeight());
        g.fill(0, 0, g.guiWidth(), g.guiHeight(), 0xff091018);
        ui.tick();
        ui.getWidget().render(g, Integer.MAX_VALUE, Integer.MAX_VALUE, 0);
        if (++frames == 6 && test.mode().equals("topics")) {
            click(g, root.select(".command-button", Button.class)
                    .filter(b -> b.text.getText().getString().equals(Component.translatable("gui.starboundmc.ship_ai.topics_show").getString()))
                    .findFirst().orElseThrow());
        }
        if (frames < 16) return;
        verifyVisibleControls(root);
        g.flush();
        var folder = mc.gameDirectory.toPath().resolve("screenshots");
        Files.createDirectories(folder);
        try (var image = Screenshot.takeScreenshot(mc.getMainRenderTarget())) {
            image.writeToFile(folder.resolve(test.language() + "-scale" + test.scale() + "-" + test.mode() + ".png"));
        }
        if (test.mode().equals("tasks")) {
            verifyQuestionReturn(g);
            verifyNextGuidanceAfterTaskCompletion(g);
        }
        ui.onRemoved(); ui = null; root = null; frames = 0;
        if (++stage == CASES.size()) {
            finished = true;
            Files.writeString(folder.resolve("passed.txt"), "PASS: 24 production-tree captures, zh/en, GUI 2/3/4, task question/return and next-guidance-after-completion hit paths, reward/locked/reference/history/topic/offline states. No world/server opened.\n");
            mc.stop();
        }
    }

    private static void setup(Case test) {
        ClientShipAiTerminalState.resetConnectionState();
        ClientShipAiTerminalState.beginTerminalSession(77);
        ClientShipStoryState.beginContainer(77);
        boolean offline = test.mode().equals("offline");
        var progress = new NovaTaskProgress(NovaTaskProgress.SCHEMA, 2,
                offline ? 0 : NovaTask.CONTACT.mask() | NovaTask.SURFACE.mask(), 0,
                NovaTaskProgress.UPGRADED, offline ? NovaTask.CONTACT.id() : NovaTask.REPAIR.id(), "");
        ClientShipStoryState.apply(77, new ShipStorySnapshotPacket(77, 0,
                SharedShipProgress.CURRENT_SCHEMA_VERSION, 2, offline ? CoreState.OFFLINE : CoreState.ONLINE,
                offline ? SurfaceMissionState.LOCKED : SurfaceMissionState.COMPLETE,
                EngineState.DAMAGED, EngineState.DAMAGED, MineralScanState.COMPLETE, 0,
                PlayerStoryState.CURRENT_SCHEMA_VERSION, 2, !offline, 0, 0, 0, progress));
        var session = ClientShipAiTerminalState.current();
        var view = session.view();
        if (test.mode().equals("tasks")) view.openTask(NovaTask.REPAIR, progress);
        if (test.mode().equals("locked")) view.openTask(NovaTask.LUNAR_SORTIE, progress);
        if (test.mode().equals("rewards")) { view.filter(true, progress); view.show(NovaTerminalViewState.Page.TASKS); }
        if (test.mode().equals("archive")) view.show(NovaTerminalViewState.Page.ARCHIVE);
        if (test.mode().equals("history")) view.historyOpen(true);
        root = new ShipAiTerminalRoot(77);
        session.completeStream();
        if (test.mode().equals("comms") || test.mode().equals("history")) {
            session.selectOption(Component.literal(""), Component.translatable(SituationTopic.FLIGHT_CAPABILITY.responseTranslationKey()),
                    ClientShipAiTerminalState.CompletionIntent.NONE, SituationTopic.FLIGHT_CAPABILITY);
            session.completeStream();
        }
        ui = ModularUI.of(UI.of(root, ResourceLocation.fromNamespaceAndPath(StarboundMC.MODID, "lss/ship_ai_terminal.lss")));
    }

    private static void verifyQuestionReturn(GuiGraphics g) {
        var view = ClientShipAiTerminalState.current().view();
        Button ask = root.select(".command-button", Button.class)
                .filter(b -> b.text.getText().getString().equals(Component.translatable("gui.starboundmc.tasks.ask").getString()))
                .findFirst().orElseThrow();
        click(g, ask);
        float scroll = view.taskScroll();
        if (view.page() != NovaTerminalViewState.Page.COMMS || view.inquiry() != NovaTask.REPAIR)
            throw new IllegalStateException("Task question did not enter context Comms");
        ui.tick(); ui.getWidget().render(g, Integer.MAX_VALUE, Integer.MAX_VALUE, 0);
        Button back = root.select(".command-button", Button.class)
                .filter(b -> b.text.getText().getString().equals(Component.translatable("gui.starboundmc.tasks.return").getString()))
                .findFirst().orElseThrow();
        click(g, back);
        ui.tick(); ui.getWidget().render(g, Integer.MAX_VALUE, Integer.MAX_VALUE, 0);
        if (view.page() != NovaTerminalViewState.Page.TASKS || view.selected() != NovaTask.REPAIR)
            throw new IllegalStateException("Context return did not restore the task");
        if (Math.abs(view.taskScroll() - scroll) > 1)
            throw new IllegalStateException("Context return lost the task reading position");
        ui.onRemoved();
        root = new ShipAiTerminalRoot(77);
        ui = ModularUI.of(UI.of(root, ResourceLocation.fromNamespaceAndPath(StarboundMC.MODID, "lss/ship_ai_terminal.lss")));
        ui.init(g.guiWidth(), g.guiHeight());
        ui.tick(); ui.getWidget().render(g, Integer.MAX_VALUE, Integer.MAX_VALUE, 0);
        if (view.page() != NovaTerminalViewState.Page.TASKS || Math.abs(view.taskScroll() - scroll) > 1)
            throw new IllegalStateException("Root rebuild lost task navigation or reading position");
    }

    private static void verifyNextGuidanceAfterTaskCompletion(GuiGraphics g) {
        var session = ClientShipAiTerminalState.current();
        while (session.isStreaming()) ui.tick();
        var progress = new NovaTaskProgress(NovaTaskProgress.SCHEMA, 3,
                NovaTask.CONTACT.mask() | NovaTask.SURFACE.mask() | NovaTask.REPAIR.mask(),
                NovaTask.SURFACE.mask() | NovaTask.REPAIR.mask(),
                NovaTaskProgress.UPGRADED | NovaTaskProgress.CORE_OBTAINED, NovaTask.EXPLORATION.id(), "");
        ClientShipStoryState.apply(77, new ShipStorySnapshotPacket(77, 0,
                SharedShipProgress.CURRENT_SCHEMA_VERSION, 3, CoreState.ONLINE, SurfaceMissionState.COMPLETE,
                EngineState.ONLINE, EngineState.DAMAGED, MineralScanState.COMPLETE, 0,
                PlayerStoryState.CURRENT_SCHEMA_VERSION, 2, true, 0, 0, 0, progress));
        ui.tick();
        while (session.isStreaming()) ui.tick();
        ui.tick();

        var command = root.select(".command-shell", NovaCommandLayout.class).findFirst().orElseThrow();
        String expected = Component.translatable("gui.starboundmc.guidance.exploration.help").getString();
        if (!command.currentGuidance().getString().equals(expected))
            throw new IllegalStateException("Current guidance stayed on the completed inquiry task");
        if (session.view().inquiry() != NovaTask.REPAIR)
            throw new IllegalStateException("Task completion discarded the inquiry return path prematurely");

        ui.getWidget().render(g, Integer.MAX_VALUE, Integer.MAX_VALUE, 0);
        Button comms = root.select(".command-button", Button.class)
                .filter(b -> b.hasClass("command-nav-button")
                        && b.text.getText().getString().equals(Component.translatable("gui.starboundmc.command.comms").getString()))
                .findFirst().orElseThrow();
        click(g, comms);
        ui.tick();
        ui.getWidget().render(g, Integer.MAX_VALUE, Integer.MAX_VALUE, 0);
        Button next = root.select(".command-button", Button.class)
                .filter(b -> b.text.getText().getString().equals(Component.translatable("gui.starboundmc.ship_ai.prologue.option.next").getString()))
                .findFirst().orElseThrow();
        click(g, next);
        if (session.view().inquiry() != null)
            throw new IllegalStateException("Asking what to do next retained the old inquiry context");
        if (!session.message(session.latestReplyIndex()).body().getString().equals(expected))
            throw new IllegalStateException("Next-action reply did not recommend the next available task");
    }

    private static void click(GuiGraphics g, Button button) {
        for (UIElement parent = button.getParent(); parent != null; parent = parent.getParent()) {
            if (!(parent instanceof ScrollerView scroller)) continue;
            float overflow = button.getPositionY() + button.getSizeHeight() - scroller.viewPort.getPositionY() - scroller.viewPort.getSizeHeight();
            float range = scroller.getContainerHeight() - scroller.viewPort.getContentHeight();
            if (overflow > 0 && range > 0) {
                float pixels = scroller.verticalScroller.getNormalizedValue() * range;
                scroller.verticalScroller.setNormalizedValue(Math.min(1, (pixels + overflow + 4) / range));
                ui.getWidget().render(g, Integer.MAX_VALUE, Integer.MAX_VALUE, 0);
            }
        }
        int x = Math.round(button.getPositionX() + button.getSizeWidth() / 2);
        int y = Math.round(button.getPositionY() + button.getSizeHeight() / 2);
        ui.getWidget().render(g, x, y, 0);
        ui.getWidget().mouseClicked(x, y, 0);
        ui.getWidget().mouseReleased(x, y, 0);
    }

    private static void verifyVisibleControls(UIElement element) {
        if (!element.isDisplayed()) return;
        if (element instanceof NovaTerminalButton && element.getSizeHeight() < 14)
            throw new IllegalStateException("Collapsed button " + element.getClasses());
        if (element instanceof Button button && button.hasClass("command-nav-button")) {
            var text = button.text;
            if (TextUtilities.computeFormattedLines(text.getFont(), TextUtilities.withFont(text.getText(), text.getTextStyle().font()),
                    text.getTextStyle().fontSize(), text.getContentWidth()).size() != 1)
                throw new IllegalStateException("Navigation label wraps: " + text.getText().getString());
        }
        if (element instanceof Button button && button.hasClass("command-task-row") && button.hasClass("command-selected")) {
            for (UIElement parent = button.getParent(); parent != null; parent = parent.getParent()) {
                if (!(parent instanceof ScrollerView scroller)) continue;
                if (button.getPositionY() < scroller.viewPort.getPositionY() - 1
                        || button.getPositionY() + button.getSizeHeight() > scroller.viewPort.getPositionY() + scroller.viewPort.getSizeHeight() + 1)
                    throw new IllegalStateException("Selected task header is outside the reading viewport");
            }
        }
        for (UIElement child : element.getChildren()) verifyVisibleControls(child);
    }
}
