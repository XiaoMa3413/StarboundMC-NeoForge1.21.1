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
import com.starboundmc.network.ModNetwork;
import com.starboundmc.network.ShipAiActionPacket;
import com.starboundmc.story.CoreState;
import com.starboundmc.story.EngineState;
import com.starboundmc.story.MineralScanState;
import com.starboundmc.story.PrologueDialogueNode;
import com.starboundmc.story.SituationTopic;
import com.starboundmc.story.SurfaceMissionState;
import dev.vfyjxf.taffy.style.AlignContent;
import dev.vfyjxf.taffy.style.AlignItems;
import dev.vfyjxf.taffy.style.FlexDirection;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/** Stable LDLib2 view of the server-authoritative N.O.V.A. prologue. */
public final class ShipAiTerminalRoot extends UIElement
{
    private static final int NOVA_COLOR = 0xFF8CF5F2;
    private static final int PLAYER_COLOR = 0xFFE4E7E8;
    private static final int SYSTEM_COLOR = 0xFF7E939A;
    private static final int BODY_COLOR = 0xFFFFFFFF;

    private static final String TOPIC_READ_CLASS = "ship-ai-topic-read";
    private static final String TOPIC_CURRENT_CLASS = "ship-ai-topic-current";

    private final int containerId;
    private final NovaPortraitElement portrait = new NovaPortraitElement();
    private final Label portraitState = new Label();
    private final Label linkStatus = new Label();
    private final ScrollerView history = new ScrollerView();
    private final Button returnLatest = new Button();
    private final Label optionHint = new Label();
    private final Button singleAction = new Button();
    private final UIElement topicControls = new UIElement();
    private final Map<SituationTopic, Button> topicButtons =
            new EnumMap<>(SituationTopic.class);
    private final Button progressionButton = new Button();
    private final ClientShipAiTerminalState.Session session =
            ClientShipAiTerminalState.current();
    private final List<Label> transcriptLabels = new ArrayList<>();

    private ClientShipStoryState.Snapshot authoritativeSnapshot;
    private PrologueDialogueNode baseNode;
    private long observedUpdateSequence;
    private boolean applyingAutoScroll;

    public ShipAiTerminalRoot(int containerId)
    {
        if (containerId < 0)
            throw new IllegalArgumentException("containerId must be non-negative");
        this.containerId = containerId;

        addClass("ship-ai-screen");
        layout(layout -> layout
                .widthPercent(100)
                .heightPercent(100)
                .alignItems(AlignItems.CENTER)
                .justifyContent(AlignContent.CENTER));

        UIElement shell = new UIElement().addClass("ship-ai-shell");
        shell.setOverflowVisible(false);
        shell.layout(layout -> layout
                .widthPercent(90)
                .heightPercent(88)
                .maxWidth(360)
                .maxHeight(210)
                .minHeight(96)
                .flexDirection(FlexDirection.COLUMN));
        shell.addChildren(buildHeader(), buildBody());
        addChild(shell);

        addEventListener(UIEvents.TICK, event -> tickTerminal());
        refreshAuthoritativeSnapshot();
        restoreSessionView();
        syncPresentation();
    }

    private UIElement buildHeader()
    {
        UIElement header = new UIElement().addClass("ship-ai-header");
        header.layout(layout -> layout
                .widthPercent(100)
                .height(21)
                .paddingHorizontal(5)
                .gapAll(4)
                .alignItems(AlignItems.CENTER)
                .flexDirection(FlexDirection.ROW));

        Label title = new Label();
        title.setText(Component.translatable("gui.starboundmc.ship_ai.title"));
        title.addClass("ship-ai-title");
        title.setAllowHitTest(false);
        title.setOverflowVisible(false);
        title.layout(layout -> layout
                .width(40)
                .minWidth(30)
                .height(10)
                .flexShrink(1));
        title.textStyle(style -> style
                .adaptiveWidth(false)
                .textAlignVertical(Vertical.CENTER)
                .textWrap(TextWrap.HIDE));

        Label subtitle = new Label();
        subtitle.setText(Component.translatable("gui.starboundmc.ship_ai.subtitle"));
        subtitle.addClass("ship-ai-subtitle");
        subtitle.setAllowHitTest(false);
        subtitle.setOverflowVisible(false);
        subtitle.layout(layout -> layout.flex(1).minWidth(0).height(9));
        subtitle.textStyle(style -> style
                .adaptiveWidth(false)
                .textAlignVertical(Vertical.CENTER)
                .textWrap(TextWrap.HIDE));

        linkStatus.addClass("ship-ai-link-status");
        linkStatus.setAllowHitTest(false);
        linkStatus.setOverflowVisible(false);
        linkStatus.layout(layout -> layout
                .width(48)
                .minWidth(38)
                .height(9)
                .flexShrink(1));
        linkStatus.textStyle(style -> style
                .adaptiveWidth(false)
                .textAlignHorizontal(Horizontal.RIGHT)
                .textAlignVertical(Vertical.CENTER)
                .textWrap(TextWrap.HIDE));

        header.addChildren(title, subtitle, linkStatus);
        return header;
    }

    private UIElement buildBody()
    {
        UIElement body = new UIElement().addClass("ship-ai-body");
        body.layout(layout -> layout
                .widthPercent(100)
                .flex(1)
                .flexDirection(FlexDirection.ROW));
        body.addChildren(buildPortraitPane(), buildConversationPane());
        return body;
    }

    private UIElement buildPortraitPane()
    {
        UIElement pane = new UIElement().addClass("ship-ai-portrait-pane");
        pane.setOverflowVisible(false);
        pane.layout(layout -> layout
                .widthPercent(34)
                .heightPercent(100)
                .paddingAll(7)
                .gapAll(3)
                .alignItems(AlignItems.CENTER)
                .flexDirection(FlexDirection.COLUMN));

        UIElement portraitStage = new UIElement().addClass("ship-ai-portrait-stage");
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

        Label identity = new Label();
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

    private UIElement buildConversationPane()
    {
        UIElement pane = new UIElement().addClass("ship-ai-conversation-pane");
        pane.setOverflowVisible(false);
        pane.layout(layout -> layout
                .flex(1)
                .heightPercent(100)
                .paddingAll(6)
                .gapAll(3)
                .flexDirection(FlexDirection.COLUMN));

        UIElement logHeader = new UIElement().addClass("ship-ai-log-header");
        logHeader.layout(layout -> layout
                .widthPercent(100)
                .height(11)
                .flexShrink(1)
                .gapAll(4)
                .alignItems(AlignItems.CENTER)
                .flexDirection(FlexDirection.ROW));

        Label logTitle = new Label();
        logTitle.setText(Component.translatable("gui.starboundmc.ship_ai.log"));
        logTitle.addClass("ship-ai-section-title");
        logTitle.setAllowHitTest(false);
        logTitle.setOverflowVisible(false);
        logTitle.layout(layout -> layout.flex(1).minWidth(0).height(8));
        logTitle.textStyle(style -> style
                .adaptiveWidth(false)
                .textWrap(TextWrap.HIDE));

        returnLatest.setText(Component.translatable("gui.starboundmc.ship_ai.return_latest"));
        returnLatest.addClass("ship-ai-return-button");
        returnLatest.setDisplay(false);
        returnLatest.setOverflowVisible(false);
        returnLatest.layout(layout -> layout.width(62).height(12));
        returnLatest.text.setAllowHitTest(false);
        returnLatest.textStyle(style -> style
                .adaptiveWidth(false)
                .textAlignHorizontal(Horizontal.CENTER)
                .textAlignVertical(Vertical.CENTER)
                .textWrap(TextWrap.HIDE));
        returnLatest.addEventListener(UIEvents.CLICK, event ->
        {
            if (event.button == GLFW.GLFW_MOUSE_BUTTON_LEFT && returnLatest.isActive())
            {
                scrollToLatest();
                event.stopPropagation();
            }
        });
        logHeader.addChildren(logTitle, returnLatest);

        configureHistory();
        pane.addChildren(logHeader, history, buildOptions());
        return pane;
    }

    private void configureHistory()
    {
        history.addClass("ship-ai-history");
        history.setOverflowVisible(false);
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

        history.viewContainer.addEventListener(UIEvents.LAYOUT_CHANGED, event ->
        {
            if (session.autoFollow())
                applyAutoScroll();
            else
                restoreManualScroll();
        });
        history.verticalScroller.setOnValueChanged(value ->
        {
            if (applyingAutoScroll)
                return;
            boolean atLatest = isAtLatest(value);
            session.setAutoFollow(atLatest);
            if (!atLatest)
                session.setManualScrollPixels(value * currentScrollRange());
            returnLatest.setDisplay(!atLatest);
        });
        history.viewPort.addEventListener(UIEvents.CLICK, event ->
        {
            if (event.button == GLFW.GLFW_MOUSE_BUTTON_LEFT && session.isStreaming())
            {
                completeStream();
                event.stopPropagation();
            }
        });
    }

    private UIElement buildOptions()
    {
        UIElement section = new UIElement().addClass("ship-ai-options");
        section.setOverflowVisible(false);
        section.layout(layout -> layout
                .widthPercent(100)
                .height(58)
                .minHeight(40)
                .flexShrink(1)
                .justifyContent(AlignContent.CENTER)
                .flexDirection(FlexDirection.COLUMN));

        optionHint.addClass("ship-ai-option-hint");
        optionHint.setAllowHitTest(false);
        optionHint.setOverflowVisible(false);
        optionHint.layout(layout -> layout.widthPercent(100).heightPercent(100));
        optionHint.textStyle(style -> style
                .adaptiveWidth(false)
                .adaptiveHeight(false)
                .textAlignHorizontal(Horizontal.CENTER)
                .textAlignVertical(Vertical.CENTER)
                .textWrap(TextWrap.HIDE));

        configureButton(singleAction, "ship-ai-primary-action", Horizontal.CENTER);
        singleAction.setDisplay(false);
        singleAction.layout(layout -> layout.widthPercent(100).height(22));
        singleAction.addEventListener(UIEvents.CLICK, event ->
        {
            if (event.button == GLFW.GLFW_MOUSE_BUTTON_LEFT && singleAction.isActive())
            {
                beginCoreReboot();
                event.stopPropagation();
            }
        });

        topicControls.addClass("ship-ai-option-controls");
        topicControls.setOverflowVisible(false);
        topicControls.setDisplay(false);
        topicControls.layout(layout -> layout
                .widthPercent(100)
                .heightPercent(100)
                .minHeight(38)
                .flexShrink(1)
                .gapAll(3)
                .flexDirection(FlexDirection.COLUMN));

        UIElement topicGrid = new UIElement().addClass("ship-ai-topic-grid");
        topicGrid.setOverflowVisible(false);
        topicGrid.layout(layout -> layout
                .widthPercent(100)
                .flex(1)
                .minHeight(21)
                .flexShrink(1)
                .gapAll(3)
                .flexDirection(FlexDirection.COLUMN));

        SituationTopic[] topics = SituationTopic.values();
        for (int rowIndex = 0; rowIndex < 2; rowIndex++)
        {
            UIElement row = new UIElement().addClass("ship-ai-topic-row");
            row.setOverflowVisible(false);
            row.layout(layout -> layout
                    .widthPercent(100)
                    .flex(1)
                    .minHeight(9)
                    .flexShrink(1)
                    .gapAll(3)
                    .flexDirection(FlexDirection.ROW));
            for (int columnIndex = 0; columnIndex < 2; columnIndex++)
            {
                SituationTopic topic = topics[rowIndex * 2 + columnIndex];
                Button button = new Button();
                configureButton(button, "ship-ai-topic-button", Horizontal.LEFT);
                button.layout(layout -> layout.flex(1).heightPercent(100).minHeight(9));
                button.addEventListener(UIEvents.CLICK, event ->
                {
                    if (event.button == GLFW.GLFW_MOUSE_BUTTON_LEFT && button.isActive())
                    {
                        selectTopic(topic);
                        event.stopPropagation();
                    }
                });
                topicButtons.put(topic, button);
                row.addChild(button);
            }
            topicGrid.addChild(row);
        }

        configureButton(progressionButton, "ship-ai-next-button", Horizontal.CENTER);
        progressionButton.addEventListener(UIEvents.CLICK, event ->
        {
            if (event.button == GLFW.GLFW_MOUSE_BUTTON_LEFT && progressionButton.isActive())
            {
                selectProgression();
                event.stopPropagation();
            }
        });
        progressionButton.layout(layout -> layout
                .widthPercent(100)
                .height(17)
                .minHeight(14)
                .flexShrink(1));

        topicControls.addChildren(topicGrid, progressionButton);
        section.addChildren(optionHint, singleAction, topicControls);
        return section;
    }

    private static void configureButton(Button button, String styleClass, Horizontal alignment)
    {
        button.addClass(styleClass);
        button.setOverflowVisible(false);
        button.text.setAllowHitTest(false);
        button.text.setOverflowVisible(false);
        button.textStyle(style -> style
                .adaptiveWidth(false)
                .adaptiveHeight(false)
                .textAlignHorizontal(alignment)
                .textAlignVertical(Vertical.CENTER)
                .textWrap(TextWrap.HIDE)
                .lineSpacing(1));
    }

    private void tickTerminal()
    {
        boolean snapshotChanged = refreshAuthoritativeSnapshot();
        int previousStreamingIndex = session.streamingIndex();
        ClientShipAiTerminalState.CompletionIntent completed = session.advanceStream();

        syncTranscriptViews();
        if (previousStreamingIndex >= 0 && previousStreamingIndex < transcriptLabels.size())
            updateTranscriptLine(previousStreamingIndex);
        int nextStreamingIndex = session.streamingIndex();
        if (nextStreamingIndex >= 0 && nextStreamingIndex < transcriptLabels.size())
            updateTranscriptLine(nextStreamingIndex);

        if (!completed.isNone())
            dispatchCompletion(completed);
        if (snapshotChanged || previousStreamingIndex != nextStreamingIndex || !completed.isNone())
            syncPresentation();
    }

    private boolean refreshAuthoritativeSnapshot()
    {
        boolean requestAcknowledged = acknowledgePendingRequest();
        if (!ClientShipStoryState.hasSnapshot(containerId))
            return requestAcknowledged;

        ClientShipStoryState.Snapshot incoming = ClientShipStoryState.snapshot(containerId);
        if (authoritativeSnapshot != null
                && incoming.updateSequence() == observedUpdateSequence)
            return requestAcknowledged;

        authoritativeSnapshot = incoming;
        observedUpdateSequence = incoming.updateSequence();

        baseNode = PrologueDialogueNode.derive(
                incoming.shared().schemaSupported(),
                incoming.player().schemaSupported(),
                incoming.shared().core(),
                incoming.shared().surfaceMission(),
                incoming.player().identityConfirmed());
        enqueueAutomaticCues();
        return true;
    }

    private boolean acknowledgePendingRequest()
    {
        ClientShipAiTerminalState.PendingRequest pending = session.pendingRequest();
        if (pending == null || pending.containerId() != containerId)
            return false;
        if (!ClientShipStoryState.consumeAcknowledgement(containerId, pending.requestId()))
            return false;
        return session.acknowledge(containerId, pending.requestId());
    }

    private void enqueueAutomaticCues()
    {
        if (authoritativeSnapshot == null || baseNode == null)
            return;

        switch (baseNode)
        {
            case INCOMPATIBLE -> session.enqueueCue(
                    "schema_incompatible",
                    ClientShipAiTerminalState.Speaker.SYSTEM,
                    Component.translatable("gui.starboundmc.ship_ai.prologue.incompatible"),
                    ClientShipAiTerminalState.CompletionIntent.NONE);
            case REBOOT_REQUIRED -> session.enqueueCue(
                    "core_offline",
                    ClientShipAiTerminalState.Speaker.SYSTEM,
                    Component.translatable("gui.starboundmc.ship_ai.status.offline"),
                    ClientShipAiTerminalState.CompletionIntent.NONE);
            case REBOOTING -> enqueueBootSequence();
            case FIRST_CONTACT -> session.enqueueCue(
                    "first_contact",
                    ClientShipAiTerminalState.Speaker.NOVA,
                    Component.translatable(
                            "gui.starboundmc.ship_ai.prologue.first_contact.greeting"),
                    ClientShipAiTerminalState.CompletionIntent.confirmIdentity());
            case SITUATION_HUB ->
            {
                if (!session.hasMessages())
                {
                    session.enqueueCue(
                            "situation_resume",
                            ClientShipAiTerminalState.Speaker.NOVA,
                            Component.translatable(
                                    "gui.starboundmc.ship_ai.prologue.late_join.summary"),
                            ClientShipAiTerminalState.CompletionIntent.NONE);
                }
            }
            case CURRENT_OBJECTIVE ->
            {
                if (!authoritativeSnapshot.player().identityConfirmed())
                {
                    session.enqueueCue(
                            "late_join_identity",
                            ClientShipAiTerminalState.Speaker.NOVA,
                            Component.translatable(
                                    "gui.starboundmc.ship_ai.prologue.late_join.summary"),
                            ClientShipAiTerminalState.CompletionIntent.confirmIdentity());
                }
                else
                {
                    session.enqueueCue(
                            currentStatusCueId(),
                            ClientShipAiTerminalState.Speaker.NOVA,
                            currentStatusText(),
                            ClientShipAiTerminalState.CompletionIntent.NONE);
                }
            }
            default ->
            {
                // TOPIC_RESPONSE and LANDING_BRIEFING are transient local turns.
            }
        }
    }

    private void enqueueBootSequence()
    {
        String[] keys = {
                "gui.starboundmc.ship_ai.prologue.boot.restarting",
                "gui.starboundmc.ship_ai.prologue.boot.restore_power",
                "gui.starboundmc.ship_ai.prologue.boot.navigation_index_failed",
                "gui.starboundmc.ship_ai.prologue.boot.personality_matrix",
                "gui.starboundmc.ship_ai.prologue.boot.nova_online"
        };
        for (int index = 0; index < keys.length; index++)
        {
            session.enqueueCue(
                    "boot_" + index,
                    ClientShipAiTerminalState.Speaker.SYSTEM,
                    Component.translatable(keys[index]),
                    ClientShipAiTerminalState.CompletionIntent.NONE);
        }
    }

    private void beginCoreReboot()
    {
        if (!canBeginCoreReboot())
            return;
        sendAction(ShipAiActionPacket.Action.BEGIN_CORE_REBOOT, null,
                ClientShipAiTerminalState.CompletionKind.NONE);
    }

    private void selectTopic(SituationTopic topic)
    {
        if (!canSelectTopics())
            return;

        Component option = Component.translatable(topic.optionTranslationKey());
        int firstNewIndex = session.selectOption(
                option,
                Component.translatable(topic.responseTranslationKey()),
                ClientShipAiTerminalState.CompletionIntent.markRead(topic),
                topic);
        if (firstNewIndex < 0)
            return;
        syncTranscriptViews();
        syncPresentation();
    }

    private void selectProgression()
    {
        if (!canSelectProgression())
            return;

        SurfaceMissionState mission = authoritativeSnapshot.shared().surfaceMission();
        ClientShipAiTerminalState.CompletionIntent completion =
                mission == SurfaceMissionState.LOCKED
                        ? ClientShipAiTerminalState.CompletionIntent.activateSurfaceMission()
                        : ClientShipAiTerminalState.CompletionIntent.NONE;
        Component response = mission == SurfaceMissionState.LOCKED
                ? Component.translatable("gui.starboundmc.ship_ai.prologue.landing.briefing")
                : currentStatusText();
        int firstNewIndex = session.selectOption(
                Component.translatable("gui.starboundmc.ship_ai.prologue.option.next"),
                response, completion, null);
        if (firstNewIndex < 0)
            return;
        syncTranscriptViews();
        syncPresentation();
    }

    private void dispatchCompletion(ClientShipAiTerminalState.CompletionIntent completion)
    {
        switch (completion.kind())
        {
            case CONFIRM_IDENTITY -> sendAction(
                    ShipAiActionPacket.Action.CONFIRM_IDENTITY, null, completion.kind());
            case MARK_SITUATION_READ -> sendAction(
                    ShipAiActionPacket.Action.MARK_SITUATION_READ,
                    completion.topic(), completion.kind());
            case ACTIVATE_SURFACE_MISSION -> sendAction(
                    ShipAiActionPacket.Action.ACTIVATE_SURFACE_MISSION,
                    null, completion.kind());
            case NONE ->
            {
            }
        }
    }

    private void sendAction(ShipAiActionPacket.Action action,
                            SituationTopic topic,
                            ClientShipAiTerminalState.CompletionKind completionKind)
    {
        if (authoritativeSnapshot == null || session.pendingRequest() != null)
            return;
        long requestId = session.beginRequest(containerId, completionKind);
        ShipAiActionPacket packet = switch (action)
        {
            case BEGIN_CORE_REBOOT -> ShipAiActionPacket.beginCoreReboot(containerId, requestId);
            case CONFIRM_IDENTITY -> ShipAiActionPacket.confirmIdentity(containerId, requestId);
            case MARK_SITUATION_READ -> ShipAiActionPacket.markSituationRead(
                    containerId, requestId, topic);
            case ACTIVATE_SURFACE_MISSION -> ShipAiActionPacket.activateSurfaceMission(
                    containerId, requestId);
        };
        ModNetwork.sendToServer(packet);
        syncPresentation();
    }

    private boolean canBeginCoreReboot()
    {
        return isCompatible() && session.pendingRequest() == null && !session.isTransmitting()
                && baseNode == PrologueDialogueNode.REBOOT_REQUIRED
                && authoritativeSnapshot.shared().core() == CoreState.OFFLINE;
    }

    private boolean canSelectTopics()
    {
        return isCompatible() && session.pendingRequest() == null && !session.isTransmitting()
                && authoritativeSnapshot.player().identityConfirmed()
                && (baseNode == PrologueDialogueNode.SITUATION_HUB
                    || baseNode == PrologueDialogueNode.CURRENT_OBJECTIVE);
    }

    private boolean canSelectProgression()
    {
        if (!canSelectTopics())
            return false;
        SurfaceMissionState mission = authoritativeSnapshot.shared().surfaceMission();
        return mission != SurfaceMissionState.LOCKED
                || hasReadAllRequiredTopics(authoritativeSnapshot.player().readSituationMask());
    }

    private boolean isCompatible()
    {
        return authoritativeSnapshot != null
                && authoritativeSnapshot.shared().schemaSupported()
                && authoritativeSnapshot.player().schemaSupported();
    }

    private void completeStream()
    {
        int previousStreamingIndex = session.streamingIndex();
        if (previousStreamingIndex < 0)
            return;
        preserveManualScroll();
        ClientShipAiTerminalState.CompletionIntent completed = session.completeStream();
        syncTranscriptViews();
        if (previousStreamingIndex < transcriptLabels.size())
            updateTranscriptLine(previousStreamingIndex);
        int nextStreamingIndex = session.streamingIndex();
        if (nextStreamingIndex >= 0 && nextStreamingIndex < transcriptLabels.size())
            updateTranscriptLine(nextStreamingIndex);
        if (!completed.isNone())
            dispatchCompletion(completed);
        syncPresentation();
    }

    private void restoreSessionView()
    {
        syncTranscriptViews();
        returnLatest.setDisplay(!session.autoFollow());
    }

    private void syncTranscriptViews()
    {
        while (transcriptLabels.size() < session.messageCount())
            appendTranscriptView(transcriptLabels.size());
    }

    private void syncPresentation()
    {
        syncTranscriptViews();
        boolean transmitting = session.isTransmitting();
        portrait.setSpeaking(transmitting);
        portraitState.setText(portraitStatusText(transmitting));
        linkStatus.setText(linkStatusText());

        if (authoritativeSnapshot == null)
        {
            showHint(Component.translatable("gui.starboundmc.ship_ai.prologue.syncing"));
            return;
        }
        if (baseNode == PrologueDialogueNode.INCOMPATIBLE)
        {
            showHint(Component.translatable("gui.starboundmc.ship_ai.prologue.incompatible"));
            return;
        }
        if (baseNode == PrologueDialogueNode.REBOOT_REQUIRED)
        {
            if (transmitting || session.pendingRequest() != null)
                showHint(pendingHint());
            else
                showSingleAction();
            return;
        }
        if (baseNode == PrologueDialogueNode.REBOOTING)
        {
            showHint(Component.translatable("gui.starboundmc.ship_ai.status.rebooting"));
            return;
        }
        if ((baseNode == PrologueDialogueNode.SITUATION_HUB
                || baseNode == PrologueDialogueNode.CURRENT_OBJECTIVE)
                && authoritativeSnapshot.player().identityConfirmed())
        {
            showTopicControls();
            return;
        }
        showHint(pendingHint());
    }

    private void showHint(Component text)
    {
        optionHint.setText(text);
        optionHint.style(style -> style.tooltips(text));
        optionHint.setDisplay(true);
        singleAction.setDisplay(false);
        topicControls.setDisplay(false);
    }

    private void showSingleAction()
    {
        optionHint.setDisplay(false);
        topicControls.setDisplay(false);
        singleAction.setDisplay(true);
        Component actionText = Component.translatable(
                "gui.starboundmc.ship_ai.prologue.action.reboot_core");
        singleAction.setText(actionText);
        singleAction.setActive(canBeginCoreReboot());
        singleAction.style(style -> style.tooltips(actionText));
    }

    private void showTopicControls()
    {
        optionHint.setDisplay(false);
        singleAction.setDisplay(false);
        topicControls.setDisplay(true);

        boolean interactive = canSelectTopics();
        int readMask = authoritativeSnapshot.player().readSituationMask();
        for (SituationTopic topic : SituationTopic.values())
        {
            Button button = topicButtons.get(topic);
            boolean read = (readMask & topic.mask()) != 0;
            Component option = Component.translatable(topic.optionTranslationKey());
            Component label = read
                    ? Component.translatable("gui.starboundmc.ship_ai.prologue.topic.read_marker")
                            .append(Component.literal(" · "))
                            .append(option.copy())
                    : option.copy();
            button.setText(label);
            Component tooltip = interactive
                    ? option
                    : session.isTransmitting()
                            ? Component.translatable("gui.starboundmc.ship_ai.receiving")
                            : session.pendingRequest() != null
                                    ? Component.translatable(
                                            "gui.starboundmc.ship_ai.prologue.action_pending")
                                    : option;
            button.style(style -> style.tooltips(tooltip));
            button.setActive(interactive);
            setClass(button, TOPIC_READ_CLASS, read);
            setClass(button, TOPIC_CURRENT_CLASS, topic == session.selectedTopic());
        }

        SurfaceMissionState mission = authoritativeSnapshot.shared().surfaceMission();
        int readCount = Integer.bitCount(readMask & SituationTopic.REQUIRED_MASK);
        boolean allRead = hasReadAllRequiredTopics(readMask);
        Component nextText = mission == SurfaceMissionState.LOCKED
                ? allRead
                        ? Component.translatable("gui.starboundmc.ship_ai.prologue.option.next")
                        : Component.translatable(
                                "gui.starboundmc.ship_ai.prologue.option.next_locked", readCount)
                : Component.translatable(
                        "gui.starboundmc.ship_ai.prologue.option.current_status");
        progressionButton.setText(nextText);
        progressionButton.style(style -> style.tooltips(nextText));
        progressionButton.setActive(canSelectProgression());
    }

    private static void setClass(UIElement element, String className, boolean enabled)
    {
        if (enabled)
            element.addClass(className);
        else
            element.removeClass(className);
    }

    private Component pendingHint()
    {
        return Component.translatable(session.pendingRequest() == null
                ? "gui.starboundmc.ship_ai.receiving"
                : "gui.starboundmc.ship_ai.prologue.action_pending");
    }

    private Component linkStatusText()
    {
        if (authoritativeSnapshot == null)
            return Component.translatable("gui.starboundmc.ship_ai.status.syncing");
        if (!isCompatible())
            return Component.translatable("gui.starboundmc.ship_ai.status.incompatible");
        return Component.translatable(switch (authoritativeSnapshot.shared().core())
        {
            case OFFLINE -> "gui.starboundmc.ship_ai.status.offline";
            case REBOOTING -> "gui.starboundmc.ship_ai.status.rebooting";
            case ONLINE -> "gui.starboundmc.ship_ai.status.online";
        });
    }

    private Component portraitStatusText(boolean transmitting)
    {
        if (transmitting)
            return Component.translatable("gui.starboundmc.ship_ai.portrait.speaking");
        if (authoritativeSnapshot == null)
            return Component.translatable("gui.starboundmc.ship_ai.portrait.syncing");
        if (!isCompatible())
            return Component.translatable("gui.starboundmc.ship_ai.portrait.incompatible");
        return Component.translatable(switch (authoritativeSnapshot.shared().core())
        {
            case OFFLINE -> "gui.starboundmc.ship_ai.portrait.offline";
            case REBOOTING -> "gui.starboundmc.ship_ai.portrait.recovering";
            case ONLINE -> portrait.hasPortraitAsset()
                    ? "gui.starboundmc.ship_ai.portrait.idle"
                    : "gui.starboundmc.ship_ai.portrait.prototype";
        });
    }

    private String currentStatusCueId()
    {
        ClientShipStoryState.SharedView shared = authoritativeSnapshot.shared();
        return "current_status_" + shared.surfaceMission().id()
                + "_" + shared.sublightEngine().id()
                + "_" + shared.hyperdrive().id()
                + "_scan_" + mineralScanCueCategory(shared.mineralScan());
    }

    private Component currentStatusText()
    {
        ClientShipStoryState.SharedView shared = authoritativeSnapshot.shared();
        Component objective = Component.translatable(
                shared.surfaceMission() == SurfaceMissionState.COMPLETE
                        ? "gui.starboundmc.ship_ai.prologue.current_status.complete"
                        : "gui.starboundmc.ship_ai.prologue.current_objective.summary");
        Component sublight = Component.translatable(
                shared.sublightEngine() == EngineState.ONLINE
                        ? "gui.starboundmc.ship_ai.status.sublight.online"
                        : "gui.starboundmc.ship_ai.status.sublight.damaged");
        Component hyperdrive = Component.translatable(
                shared.hyperdrive() == EngineState.ONLINE
                        ? "gui.starboundmc.ship_ai.status.hyperdrive.online"
                        : "gui.starboundmc.ship_ai.status.hyperdrive.damaged");
        var summary = Component.empty().append(objective);
        if (shared.surfaceMission() == SurfaceMissionState.COMPLETE)
        {
            Component mineralScan = Component.translatable(switch (shared.mineralScan())
            {
                case LOCKED -> "gui.starboundmc.ship_ai.status.mineral_scan.waiting";
                case COMPLETE -> "gui.starboundmc.ship_ai.status.mineral_scan.complete";
                default -> "gui.starboundmc.ship_ai.status.mineral_scan.in_progress";
            });
            summary = summary.append(Component.literal("\n")).append(mineralScan);
        }
        return summary.append(Component.literal("\n"))
                .append(sublight)
                .append(Component.literal("\n"))
                .append(hyperdrive);
    }

    private static String mineralScanCueCategory(MineralScanState state)
    {
        return switch (state)
        {
            case LOCKED -> "locked";
            case COMPLETE -> "complete";
            default -> "in_progress";
        };
    }

    private static boolean hasReadAllRequiredTopics(int readMask)
    {
        return (readMask & SituationTopic.REQUIRED_MASK) == SituationTopic.REQUIRED_MASK;
    }

    private void appendTranscriptView(int messageIndex)
    {
        preserveManualScroll();
        Label label = new Label();
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

    private void updateTranscriptLine(int messageIndex)
    {
        ClientShipAiTerminalState.Message message = session.message(messageIndex);
        Component body = session.isStreamingLine(messageIndex)
                ? Component.literal(session.visibleStreamingText())
                : message.body().copy();
        transcriptLabels.get(messageIndex).setText(
                Component.translatable(message.speaker().translationKey())
                        .withStyle(style -> style.withColor(speakerColor(message.speaker())))
                        .append(Component.literal("  "))
                        .append(body.copy().withStyle(style -> style.withColor(BODY_COLOR))));
    }

    private static int speakerColor(ClientShipAiTerminalState.Speaker speaker)
    {
        return switch (speaker)
        {
            case SYSTEM -> SYSTEM_COLOR;
            case NOVA -> NOVA_COLOR;
            case PLAYER -> PLAYER_COLOR;
        };
    }

    private void scrollToLatest()
    {
        session.setAutoFollow(true);
        returnLatest.setDisplay(false);
        applyAutoScroll();
    }

    private void applyAutoScroll()
    {
        applyingAutoScroll = true;
        history.verticalScroller.setNormalizedValue(1.0F);
        applyingAutoScroll = false;
    }

    private void preserveManualScroll()
    {
        if (!session.autoFollow())
        {
            session.setManualScrollPixels(history.verticalScroller.getNormalizedValue()
                    * currentScrollRange());
        }
    }

    private void restoreManualScroll()
    {
        float range = currentScrollRange();
        float normalized = range <= 0.0F
                ? 0.0F
                : Mth.clamp(session.manualScrollPixels() / range, 0.0F, 1.0F);
        applyingAutoScroll = true;
        history.verticalScroller.setNormalizedValue(normalized);
        applyingAutoScroll = false;
    }

    private boolean isAtLatest(float normalizedValue)
    {
        return currentScrollRange() <= 0.5F || normalizedValue >= 0.995F;
    }

    private float currentScrollRange()
    {
        return Math.max(0.0F,
                history.getContainerHeight() - history.viewPort.getContentHeight());
    }

}
