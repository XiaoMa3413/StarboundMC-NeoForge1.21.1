package com.starboundmc.client.shipai;

import com.lowdragmc.lowdraglib2.gui.texture.IGuiTexture;
import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.gui.ui.data.Horizontal;
import com.lowdragmc.lowdraglib2.gui.ui.data.ScrollDisplay;
import com.lowdragmc.lowdraglib2.gui.ui.data.ScrollerMode;
import com.lowdragmc.lowdraglib2.gui.ui.data.TextWrap;
import com.lowdragmc.lowdraglib2.gui.ui.data.Vertical;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Button;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Label;
import com.lowdragmc.lowdraglib2.gui.ui.elements.ScrollerView;
import com.lowdragmc.lowdraglib2.gui.ui.event.UIEvents;
import dev.vfyjxf.taffy.style.AlignContent;
import dev.vfyjxf.taffy.style.AlignItems;
import dev.vfyjxf.taffy.style.FlexDirection;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;

/** Stable LDLib2 element tree for the local N.O.V.A. interaction prototype. */
public final class ShipAiTerminalRoot extends UIElement {
    private static final int NOVA_COLOR = 0xFF8CF5F2;
    private static final int PLAYER_COLOR = 0xFFE4E7E8;
    private static final int SYSTEM_COLOR = 0xFF7E939A;
    private static final int BODY_COLOR = 0xFFC6D3D6;

    private static final String[] OPTION_KEYS = {
            "gui.starboundmc.ship_ai.demo.option.identity",
            "gui.starboundmc.ship_ai.demo.option.projection",
            "gui.starboundmc.ship_ai.demo.option.status"
    };
    private static final String[] RESPONSE_KEYS = {
            "gui.starboundmc.ship_ai.demo.response.identity",
            "gui.starboundmc.ship_ai.demo.response.projection",
            "gui.starboundmc.ship_ai.demo.response.status"
    };

    private final NovaPortraitElement portrait = new NovaPortraitElement();
    private final Label portraitState = new Label();
    private final ScrollerView history = new ScrollerView();
    private final Button returnLatest = new Button();
    private final Label optionHint = new Label();
    private final Button[] optionButtons = {new Button(), new Button(), new Button()};
    private final ClientShipAiTerminalState.Session session = ClientShipAiTerminalState.current();
    private final List<Label> transcriptLabels = new ArrayList<>();

    private boolean autoFollow = true;
    private boolean applyingAutoScroll;
    private float manualScrollPixels;

    public ShipAiTerminalRoot() {
        addClass("ship-ai-screen");
        layout(layout -> layout
                .widthPercent(100)
                .heightPercent(100)
                .alignItems(AlignItems.CENTER)
                .justifyContent(AlignContent.CENTER));

        var shell = new UIElement().addClass("ship-ai-shell");
        shell.layout(layout -> layout
                .widthPercent(90)
                .heightPercent(88)
                .maxWidth(360)
                .maxHeight(210)
                .flexDirection(FlexDirection.COLUMN));
        shell.addChildren(buildHeader(), buildBody());
        addChild(shell);

        configureOptions();
        addEventListener(UIEvents.TICK, event -> advanceStream());
        restoreSessionView();
    }

    private UIElement buildHeader() {
        var header = new UIElement().addClass("ship-ai-header");
        header.layout(layout -> layout
                .widthPercent(100)
                .height(21)
                .paddingHorizontal(8)
                .gapAll(6)
                .alignItems(AlignItems.CENTER)
                .flexDirection(FlexDirection.ROW));

        var title = new Label();
        title.setText(Component.translatable("gui.starboundmc.ship_ai.title"));
        title.addClass("ship-ai-title");
        title.setAllowHitTest(false);
        title.layout(layout -> layout.width(48).height(10));
        title.textStyle(style -> style.textAlignVertical(Vertical.CENTER));

        var subtitle = new Label();
        subtitle.setText(Component.translatable("gui.starboundmc.ship_ai.subtitle"));
        subtitle.addClass("ship-ai-subtitle");
        subtitle.setAllowHitTest(false);
        subtitle.setOverflowVisible(false);
        subtitle.layout(layout -> layout.flex(1).height(9));
        subtitle.textStyle(style -> style
                .adaptiveWidth(false)
                .textAlignVertical(Vertical.CENTER)
                .textWrap(TextWrap.HIDE));

        var status = new Label();
        status.setText(Component.translatable("gui.starboundmc.ship_ai.status.online"));
        status.addClass("ship-ai-link-status");
        status.setAllowHitTest(false);
        status.setOverflowVisible(false);
        status.layout(layout -> layout.width(58).height(9));
        status.textStyle(style -> style
                .adaptiveWidth(false)
                .textAlignHorizontal(Horizontal.RIGHT)
                .textAlignVertical(Vertical.CENTER)
                .textWrap(TextWrap.HIDE));

        header.addChildren(title, subtitle, status);
        return header;
    }

    private UIElement buildBody() {
        var body = new UIElement().addClass("ship-ai-body");
        body.layout(layout -> layout
                .widthPercent(100)
                .flex(1)
                .flexDirection(FlexDirection.ROW));
        body.addChildren(buildPortraitPane(), buildConversationPane());
        return body;
    }

    private UIElement buildPortraitPane() {
        var pane = new UIElement().addClass("ship-ai-portrait-pane");
        pane.setOverflowVisible(false);
        pane.layout(layout -> layout
                .widthPercent(34)
                .heightPercent(100)
                .paddingAll(7)
                .gapAll(3)
                .alignItems(AlignItems.CENTER)
                .flexDirection(FlexDirection.COLUMN));

        var portraitStage = new UIElement().addClass("ship-ai-portrait-stage");
        portraitStage.setOverflowVisible(false);
        portraitStage.layout(layout -> layout
                .widthPercent(100)
                .flex(1)
                .alignItems(AlignItems.CENTER)
                .justifyContent(AlignContent.CENTER));

        portrait.layout(layout -> layout
                .widthPercent(88)
                .maxWidth(96)
                .maxHeight(112)
                .aspectRatio(6F / 7F));
        portraitStage.addChild(portrait);

        var identity = new Label();
        identity.setText(Component.translatable("gui.starboundmc.ship_ai.portrait.identity"));
        identity.addClass("ship-ai-portrait-identity");
        identity.setAllowHitTest(false);
        identity.setOverflowVisible(false);
        identity.layout(layout -> layout.widthPercent(100).height(8));
        identity.textStyle(style -> style
                .adaptiveWidth(false)
                .textAlignHorizontal(Horizontal.CENTER)
                .textAlignVertical(Vertical.CENTER)
                .textWrap(TextWrap.HIDE));

        portraitState.setText(Component.translatable(portrait.hasPortraitAsset()
                ? "gui.starboundmc.ship_ai.portrait.idle"
                : "gui.starboundmc.ship_ai.portrait.prototype"));
        portraitState.addClass("ship-ai-portrait-state");
        portraitState.setAllowHitTest(false);
        portraitState.setOverflowVisible(false);
        portraitState.layout(layout -> layout.widthPercent(100).height(8));
        portraitState.textStyle(style -> style
                .adaptiveWidth(false)
                .textAlignHorizontal(Horizontal.CENTER)
                .textAlignVertical(Vertical.CENTER)
                .textWrap(TextWrap.HIDE));

        pane.addChildren(portraitStage, identity, portraitState);
        return pane;
    }

    private UIElement buildConversationPane() {
        var pane = new UIElement().addClass("ship-ai-conversation-pane");
        pane.setOverflowVisible(false);
        pane.layout(layout -> layout
                .flex(1)
                .heightPercent(100)
                .paddingAll(7)
                .gapAll(4)
                .flexDirection(FlexDirection.COLUMN));

        var logHeader = new UIElement().addClass("ship-ai-log-header");
        logHeader.layout(layout -> layout
                .widthPercent(100)
                .height(12)
                .gapAll(4)
                .alignItems(AlignItems.CENTER)
                .flexDirection(FlexDirection.ROW));

        var logTitle = new Label();
        logTitle.setText(Component.translatable("gui.starboundmc.ship_ai.log"));
        logTitle.addClass("ship-ai-section-title");
        logTitle.setAllowHitTest(false);
        logTitle.layout(layout -> layout.flex(1).height(8));

        returnLatest.setText(Component.translatable("gui.starboundmc.ship_ai.return_latest"));
        returnLatest.addClass("ship-ai-return-button");
        returnLatest.setDisplay(false);
        returnLatest.setOverflowVisible(false);
        returnLatest.layout(layout -> layout.width(62).height(12));
        returnLatest.textStyle(style -> style
                .adaptiveWidth(false)
                .textAlignHorizontal(Horizontal.CENTER)
                .textAlignVertical(Vertical.CENTER)
                .textWrap(TextWrap.HIDE));
        returnLatest.addEventListener(UIEvents.CLICK, event -> {
            if (event.button == GLFW.GLFW_MOUSE_BUTTON_LEFT) {
                scrollToLatest();
                event.stopPropagation();
            }
        });
        logHeader.addChildren(logTitle, returnLatest);

        configureHistory();
        pane.addChildren(logHeader, history, buildOptions());
        return pane;
    }

    private void configureHistory() {
        history.addClass("ship-ai-history");
        history.layout(layout -> layout.widthPercent(100).flex(1));
        history.scrollerStyle(style -> style
                .mode(ScrollerMode.VERTICAL)
                .verticalScrollDisplay(ScrollDisplay.AUTO)
                .horizontalScrollDisplay(ScrollDisplay.NEVER)
                .minScrollPixel(8)
                .maxScrollPixel(22));
        history.viewPort(view -> view
                .layout(layout -> layout.paddingAll(5))
                .style(style -> style.backgroundTexture(IGuiTexture.EMPTY)));
        history.viewContainer(view -> view.layout(layout -> layout
                .widthPercent(100)
                .gapAll(5)
                .flexDirection(FlexDirection.COLUMN)));

        history.viewContainer.addEventListener(UIEvents.LAYOUT_CHANGED, event -> {
            if (autoFollow) {
                applyAutoScroll();
            } else {
                restoreManualScroll();
            }
        });
        history.verticalScroller.setOnValueChanged(value -> {
            if (applyingAutoScroll) {
                return;
            }
            autoFollow = isAtLatest(value);
            if (!autoFollow) {
                manualScrollPixels = value * currentScrollRange();
            }
            returnLatest.setDisplay(!autoFollow);
        });
        history.viewPort.addEventListener(UIEvents.CLICK, event -> {
            if (event.button == GLFW.GLFW_MOUSE_BUTTON_LEFT && session.isStreaming()) {
                completeStream();
                event.stopPropagation();
            }
        });
    }

    private UIElement buildOptions() {
        var section = new UIElement().addClass("ship-ai-options");
        section.layout(layout -> layout
                .widthPercent(100)
                .height(64)
                .gapAll(3)
                .flexDirection(FlexDirection.COLUMN));

        optionHint.setText(Component.translatable("gui.starboundmc.ship_ai.receiving"));
        optionHint.addClass("ship-ai-option-hint");
        optionHint.setAllowHitTest(false);
        optionHint.setOverflowVisible(false);
        optionHint.layout(layout -> layout.widthPercent(100).heightPercent(100));
        optionHint.textStyle(style -> style
                .adaptiveWidth(false)
                .textAlignHorizontal(Horizontal.CENTER)
                .textAlignVertical(Vertical.CENTER)
                .textWrap(TextWrap.WRAP));
        section.addChild(optionHint);

        for (Button button : optionButtons) {
            button.addClass("ship-ai-option-button");
            button.setDisplay(false);
            button.setOverflowVisible(false);
            button.layout(layout -> layout.widthPercent(100).height(18));
            button.text.setOverflowVisible(false);
            button.textStyle(style -> style
                    .adaptiveWidth(false)
                    .textAlignHorizontal(Horizontal.LEFT)
                    .textAlignVertical(Vertical.CENTER)
                    .textWrap(TextWrap.HIDE));
            section.addChild(button);
        }
        return section;
    }

    private void configureOptions() {
        for (int i = 0; i < optionButtons.length; i++) {
            int optionIndex = i;
            Component label = Component.translatable(OPTION_KEYS[i]);
            Button button = optionButtons[i];
            button.setText(label);
            button.style(style -> style.tooltips(label));
            button.addEventListener(UIEvents.CLICK, event -> {
                if (event.button == GLFW.GLFW_MOUSE_BUTTON_LEFT && !session.isStreaming()) {
                    selectOption(optionIndex);
                    event.stopPropagation();
                }
            });
        }
    }

    private void selectOption(int optionIndex) {
        int firstNewIndex = session.selectOption(OPTION_KEYS[optionIndex], RESPONSE_KEYS[optionIndex]);
        if (firstNewIndex < 0) {
            return;
        }
        for (int index = firstNewIndex; index < session.messageCount(); index++) {
            appendTranscriptView(index);
        }
        syncSessionPresentation();
    }

    private void advanceStream() {
        int streamingIndex = session.streamingIndex();
        if (streamingIndex < 0) {
            return;
        }
        preserveManualScroll();
        boolean completed = session.advanceStream();
        updateTranscriptLine(streamingIndex);
        if (completed) {
            syncSessionPresentation();
        }
    }

    private void completeStream() {
        int streamingIndex = session.streamingIndex();
        if (streamingIndex < 0) {
            return;
        }
        preserveManualScroll();
        session.completeStream();
        updateTranscriptLine(streamingIndex);
        syncSessionPresentation();
    }

    private void restoreSessionView() {
        for (int index = 0; index < session.messageCount(); index++) {
            appendTranscriptView(index);
        }
        syncSessionPresentation();
    }

    private void syncSessionPresentation() {
        boolean streaming = session.isStreaming();
        portrait.setSpeaking(streaming);
        portraitState.setText(Component.translatable(streaming
                ? "gui.starboundmc.ship_ai.portrait.speaking"
                : portrait.hasPortraitAsset()
                        ? "gui.starboundmc.ship_ai.portrait.idle"
                        : "gui.starboundmc.ship_ai.portrait.prototype"));
        setOptionsVisible(!streaming);
    }

    private void setOptionsVisible(boolean visible) {
        optionHint.setDisplay(!visible);
        for (Button optionButton : optionButtons) {
            optionButton.setDisplay(visible);
        }
    }

    private void appendTranscriptView(int messageIndex) {
        preserveManualScroll();
        var label = new Label();
        label.addClass("ship-ai-history-text");
        label.setAllowHitTest(false);
        label.setOverflowVisible(false);
        label.layout(layout -> layout.widthPercent(100).minHeight(9));
        label.textStyle(style -> style
                .adaptiveWidth(false)
                .adaptiveHeight(true)
                .textWrap(TextWrap.WRAP)
                .lineSpacing(2));

        transcriptLabels.add(label);
        updateTranscriptLine(messageIndex);
        history.addScrollViewChild(label);
    }

    private void updateTranscriptLine(int messageIndex) {
        var message = session.message(messageIndex);
        var body = session.isStreamingLine(messageIndex)
                ? Component.literal(session.visibleStreamingText())
                : Component.translatable(message.bodyKey());
        transcriptLabels.get(messageIndex).setText(Component.translatable(message.speaker().translationKey())
                .withStyle(style -> style.withColor(speakerColor(message.speaker())))
                .append(Component.literal("  "))
                .append(body.copy()
                        .withStyle(style -> style.withColor(BODY_COLOR))));
    }

    private static int speakerColor(ClientShipAiTerminalState.Speaker speaker) {
        return switch (speaker) {
            case SYSTEM -> SYSTEM_COLOR;
            case NOVA -> NOVA_COLOR;
            case PLAYER -> PLAYER_COLOR;
        };
    }

    private void scrollToLatest() {
        autoFollow = true;
        returnLatest.setDisplay(false);
        applyAutoScroll();
    }

    private void applyAutoScroll() {
        applyingAutoScroll = true;
        history.verticalScroller.setNormalizedValue(1.0F);
        applyingAutoScroll = false;
    }

    private void preserveManualScroll() {
        if (!autoFollow) {
            manualScrollPixels = history.verticalScroller.getNormalizedValue()
                    * currentScrollRange();
        }
    }

    private void restoreManualScroll() {
        float range = currentScrollRange();
        float normalized = range <= 0.0F
                ? 0.0F
                : Mth.clamp(manualScrollPixels / range, 0.0F, 1.0F);
        applyingAutoScroll = true;
        history.verticalScroller.setNormalizedValue(normalized);
        applyingAutoScroll = false;
    }

    private boolean isAtLatest(float normalizedValue) {
        return currentScrollRange() <= 0.5F || normalizedValue >= 0.995F;
    }

    private float currentScrollRange() {
        return Math.max(0.0F,
                history.getContainerHeight() - history.viewPort.getContentHeight());
    }

}
