package com.starboundmc.client.shipai;

import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.gui.ui.data.Horizontal;
import com.lowdragmc.lowdraglib2.gui.ui.data.TextWrap;
import com.lowdragmc.lowdraglib2.gui.ui.data.Transform2D;
import com.lowdragmc.lowdraglib2.gui.ui.data.Vertical;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Label;
import com.lowdragmc.lowdraglib2.gui.ui.style.PropertyRegistry;
import com.lowdragmc.lowdraglib2.math.interpolate.Eases;
import com.lowdragmc.lowdraglib2.syncdata.ISubscription;
import com.starboundmc.story.CoreState;
import dev.vfyjxf.taffy.style.AlignContent;
import dev.vfyjxf.taffy.style.AlignItems;
import dev.vfyjxf.taffy.style.FlexDirection;
import it.unimi.dsi.fastutil.floats.FloatObjectPair;
import net.minecraft.network.chat.Component;

/** Stable LDLib2 element tree for the compact lower-left remote communication window. */
final class NovaBroadcastHudRoot extends UIElement
{
    private static final float SHOW_DURATION = 0.14F;
    private static final float HIDE_DURATION = 0.16F;
    private static final String EMERGENCY_KEY = "message.starboundmc.nova.prologue.emergency";
    private static final String LOCATE_TERMINAL_KEY =
            "message.starboundmc.nova.prologue.locate_terminal";

    private final UIElement panel = new UIElement();
    private final NovaPortraitElement portrait = new NovaPortraitElement();
    private final Label body = new Label();
    private long observedRevision = Long.MIN_VALUE;
    private int observedPulseSequence;
    private NovaBroadcastTimeline.Phase observedPhase = NovaBroadcastTimeline.Phase.IDLE;
    private ISubscription transition = () -> { };

    NovaBroadcastHudRoot()
    {
        addClass("nova-remote-root");
        setAllowHitTest(false);
        setOverflowVisible(false);
        layout(layout -> layout
                .widthPercent(100)
                .heightPercent(100)
                .paddingLeft(7)
                .paddingBottom(42)
                .flexDirection(FlexDirection.COLUMN)
                .alignItems(AlignItems.FLEX_START)
                .justifyContent(AlignContent.FLEX_END));

        panel.addClass("nova-remote-panel");
        panel.setAllowHitTest(false);
        panel.setOverflowVisible(false);
        panel.layout(layout -> layout
                .width(248)
                .height(66)
                .paddingAll(5)
                .gapAll(5)
                .flexDirection(FlexDirection.ROW)
                .alignItems(AlignItems.CENTER));

        panel.addChildren(buildPortraitPane(), buildTextPane());
        addChild(panel);
    }

    void sync(NovaBroadcastTimeline.Snapshot snapshot, int pulseSequence)
    {
        if (snapshot.revision() == observedRevision && pulseSequence == observedPulseSequence)
            return;

        if (snapshot.phase() == NovaBroadcastTimeline.Phase.OPENING
                && observedPhase != NovaBroadcastTimeline.Phase.OPENING)
            animateIn();
        else if (snapshot.phase() == NovaBroadcastTimeline.Phase.CLOSING
                && observedPhase != NovaBroadcastTimeline.Phase.CLOSING)
            animateOut();

        body.setText(Component.literal(snapshot.visibleText()));
        portrait.setSpeaking(snapshot.speaking());
        portrait.setCoreState(presentationCoreState(snapshot.translationKey()));
        portrait.setActivity(NovaPortraitActivity.fromBroadcastKey(snapshot.translationKey()));
        while (observedPulseSequence < pulseSequence)
        {
            portrait.onTextAdvanced();
            observedPulseSequence++;
        }

        observedRevision = snapshot.revision();
        observedPhase = snapshot.phase();
    }

    private UIElement buildPortraitPane()
    {
        UIElement stage = new UIElement().addClass("nova-remote-portrait-stage");
        stage.setAllowHitTest(false);
        stage.setOverflowVisible(false);
        stage.layout(layout -> layout
                .width(48)
                .height(56)
                .alignItems(AlignItems.CENTER)
                .justifyContent(AlignContent.CENTER));

        portrait.layout(layout -> layout.width(42).aspectRatio(6F / 7F));
        stage.addChild(portrait);
        return stage;
    }

    private UIElement buildTextPane()
    {
        UIElement pane = new UIElement().addClass("nova-remote-text-pane");
        pane.setAllowHitTest(false);
        pane.setOverflowVisible(false);
        pane.layout(layout -> layout
                .flex(1)
                .heightPercent(100)
                .gapAll(2)
                .flexDirection(FlexDirection.COLUMN));

        UIElement header = new UIElement().addClass("nova-remote-header");
        header.setAllowHitTest(false);
        header.setOverflowVisible(false);
        header.layout(layout -> layout
                .widthPercent(100)
                .height(9)
                .flexDirection(FlexDirection.ROW)
                .alignItems(AlignItems.CENTER)
                .justifyContent(AlignContent.SPACE_BETWEEN));

        Label speaker = new Label();
        speaker.setText(Component.translatable("gui.starboundmc.ship_ai.speaker.nova"));
        speaker.addClass("nova-remote-speaker");
        configureSingleLine(speaker, Horizontal.LEFT);
        speaker.layout(layout -> layout.width(58).height(9));

        Label link = new Label();
        link.setText(Component.translatable("gui.starboundmc.ship_ai.remote_link"));
        link.addClass("nova-remote-link");
        configureSingleLine(link, Horizontal.RIGHT);
        link.layout(layout -> layout.flex(1).height(9));
        header.addChildren(speaker, link);

        body.addClass("nova-remote-body");
        body.setText(Component.empty());
        body.setAllowHitTest(false);
        body.setOverflowVisible(false);
        body.layout(layout -> layout.widthPercent(100).flex(1));
        body.textStyle(style -> style
                .adaptiveWidth(false)
                .adaptiveHeight(false)
                .textAlignHorizontal(Horizontal.LEFT)
                .textAlignVertical(Vertical.TOP)
                .textWrap(TextWrap.WRAP)
                .lineSpacing(1));

        pane.addChildren(header, body);
        return pane;
    }

    private static void configureSingleLine(Label label, Horizontal alignment)
    {
        label.setAllowHitTest(false);
        label.setOverflowVisible(false);
        label.textStyle(style -> style
                .adaptiveWidth(false)
                .adaptiveHeight(false)
                .textAlignHorizontal(alignment)
                .textAlignVertical(Vertical.CENTER)
                .textWrap(TextWrap.HIDE));
    }

    private void animateIn()
    {
        transition.unsubscribe();
        Transform2D from = new Transform2D().translate(-7F, 0F);
        Transform2D to = Transform2D.identity();
        panel.style(style -> style.opacity(0F).transform2D(from));
        transition = panel.animation()
                .duration(SHOW_DURATION)
                .ease(Eases.QUAD_OUT)
                .style(PropertyRegistry.OPACITY,
                        FloatObjectPair.of(0F, 0F), FloatObjectPair.of(1F, 1F))
                .style(PropertyRegistry.TRANSFORM_2D,
                        FloatObjectPair.of(0F, from), FloatObjectPair.of(1F, to))
                .start();
    }

    private void animateOut()
    {
        transition.unsubscribe();
        Transform2D from = Transform2D.identity();
        Transform2D to = new Transform2D().translate(-4F, 0F);
        panel.style(style -> style.opacity(1F).transform2D(from));
        transition = panel.animation()
                .duration(HIDE_DURATION)
                .ease(Eases.QUAD_OUT)
                .style(PropertyRegistry.OPACITY,
                        FloatObjectPair.of(0F, 1F), FloatObjectPair.of(1F, 0F))
                .style(PropertyRegistry.TRANSFORM_2D,
                        FloatObjectPair.of(0F, from), FloatObjectPair.of(1F, to))
                .start();
    }

    private static CoreState presentationCoreState(String translationKey)
    {
        return EMERGENCY_KEY.equals(translationKey) || LOCATE_TERMINAL_KEY.equals(translationKey)
                ? CoreState.REBOOTING : CoreState.ONLINE;
    }
}
