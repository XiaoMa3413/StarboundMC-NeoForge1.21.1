package com.starboundmc.client.shipai;

import com.lowdragmc.lowdraglib2.gui.texture.IGuiTexture;
import com.starboundmc.client.ui.MachineUiSkin;
import com.starboundmc.client.ui.components.TooltipLines;
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
import com.starboundmc.item.MatterManipulatorItem;
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
import net.minecraft.client.Minecraft;
import net.minecraft.world.item.ItemStack;
import com.starboundmc.item.ModItems;
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
    private static final int BODY_COLOR = 0xFFD8EDF0;

    private static final String TOPIC_READ_CLASS = "ship-ai-topic-read";
    private static final String TOPIC_CURRENT_CLASS = "ship-ai-topic-current";

    private final int containerId;
    private final NovaPortraitElement portrait = new NovaPortraitElement();
    private final NovaDialogueSounds dialogueSounds = new NovaDialogueSounds();
    private final Label portraitState = new Label();
    private final ScrollerView history = new ScrollerView();
    private final Button transcriptToggle = new NovaTerminalButton();
    private final ScrollerView reply = NovaCommandLayout.scroll();
    private final Label replySpeaker = NovaCommandLayout.label("", "ship-ai-message-speaker");
    private final Label replyBody = NovaCommandLayout.label("", "ship-ai-reply-text");
    private final UIElement replyHeader = NovaCommandLayout.row("ship-ai-reply-header");
    private final UIElement optionSection = NovaCommandLayout.column("ship-ai-options");
    private final UIElement topicGrid = NovaCommandLayout.column("ship-ai-topic-grid");
    private final Button topicToggle = new NovaTerminalButton();
    private boolean topicsExpanded = !NovaCommandLayout.compact();
    private final Label optionHint = new Label();
    private final Button singleAction = new NovaTerminalButton();
    private final UIElement topicControls = new UIElement();
    private final Map<SituationTopic, Button> topicButtons =
            new EnumMap<>(SituationTopic.class);
    private final Button progressionButton = new NovaTerminalButton();
    private final ClientShipAiTerminalState.Session session =
            ClientShipAiTerminalState.current();
    private final List<Label> transcriptLabels = new ArrayList<>();

    private ClientShipStoryState.Snapshot authoritativeSnapshot;
    private PrologueDialogueNode baseNode;
    private long observedUpdateSequence;
    private int observedRepairInventoryState = Integer.MIN_VALUE;
    private boolean applyingAutoScroll;
    private final NovaCommandLayout commandLayout;

    public ShipAiTerminalRoot(int containerId)
    {
        if (containerId < 0)
            throw new IllegalArgumentException("containerId must be non-negative");
        this.containerId = containerId;

        addClasses("ship-ai-screen", "shipboard-machine");
        layout(layout -> layout
                .widthPercent(100)
                .heightPercent(100)
                .alignItems(AlignItems.CENTER)
                .justifyContent(AlignContent.CENTER));

        commandLayout = new NovaCommandLayout(containerId, buildPortraitPane(), buildConversationPane(), text -> {
            showCurrentReply();
            session.selectOption(Component.translatable("gui.starboundmc.tasks.ask_question"), text,
                    ClientShipAiTerminalState.CompletionIntent.NONE, null);
            syncTranscriptViews();
            syncPresentation();
        });
        addChild(commandLayout);
        MachineUiSkin.shell(commandLayout);
        MachineUiSkin.scrollbars(this);

        addEventListener(UIEvents.TICK, event -> tickTerminal());
        commandLayout.tick(session.isTransmitting() || session.pendingRequest() != null);
        refreshAuthoritativeSnapshot();
        syncTranscriptViews();
        syncPresentation();
    }

    private UIElement buildPortraitPane() {
        UIElement pane = NovaCommandLayout.column("ship-ai-portrait-pane");
        pane.layout(l -> l.alignItems(AlignItems.CENTER).gapAll(6).paddingRight(10));
        pane.style(s -> s.overlay((g, mx, my, x, y, w, h, partial) ->
                g.fill(Math.round(x + w - 1), Math.round(y), Math.round(x + w), Math.round(y + h), 0xff233a44)));
        UIElement stage = NovaCommandLayout.column("ship-ai-portrait-stage");
        stage.layout(l -> l.widthPercent(100).flex(1).minHeight(0).alignItems(AlignItems.CENTER)
                .justifyContent(AlignContent.CENTER));
        portrait.layout(l -> l.widthPercent(100).maxWidth(144).maxHeight(168)
                .aspectRatio(6F / 7F).flexShrink(1));
        stage.addChild(portrait);
        portraitState.addClass("ship-ai-portrait-state");
        portraitState.setAllowHitTest(false);
        portraitState.setOverflowVisible(false);
        portraitState.layout(l -> l.widthPercent(100).minHeight(12).flexShrink(0).marginBottom(18));
        portraitState.textStyle(s -> s.adaptiveWidth(false).adaptiveHeight(true).textWrap(TextWrap.WRAP)
                .textAlignHorizontal(Horizontal.CENTER).textAlignVertical(Vertical.TOP).lineSpacing(2));
        pane.addChildren(stage, portraitState);
        return pane;
    }

    private UIElement buildConversationPane() {
        UIElement pane = NovaCommandLayout.column("ship-ai-conversation-pane");
        pane.layout(l -> l.flex(1).minHeight(0).widthPercent(100).gapAll(8));
        replyHeader.layout(l -> l.widthPercent(100).minHeight(18).flexShrink(0).gapAll(6));
        replySpeaker.layout(l -> l.flex(1).widthAuto().minWidth(0));
        configureButton(transcriptToggle, "command-link", Horizontal.RIGHT);
        transcriptToggle.layout(l -> l.width(NovaCommandLayout.compact() ? 70 : 84).height(18).paddingHorizontal(2).flexShrink(0));
        ((NovaTerminalButton) transcriptToggle).onActivate(() -> {
            preserveManualScroll();
            session.view().historyOpen(!session.view().historyOpen());
            syncPresentation();
        });
        replyHeader.addChildren(replySpeaker, transcriptToggle);
        reply.addClass("ship-ai-reply");
        reply.layout(l -> l.widthPercent(100).flex(1).minHeight(0));
        reply.addScrollViewChild(replyBody);
        reply.viewPort.addEventListener(UIEvents.CLICK, event -> {
            if (event.button == GLFW.GLFW_MOUSE_BUTTON_LEFT && session.isStreaming()) {
                completeStream();
                event.stopPropagation();
            }
        });
        configureHistory();
        pane.addChildren(replyHeader, reply, history, buildOptions());
        return pane;
    }

    private void configureHistory() {
        history.addClasses("ship-ai-history", "command-scroll");
        history.setOverflowVisible(false);
        history.layout(l -> l.widthPercent(100).flex(1).minHeight(0));
        history.scrollerStyle(s -> s.mode(ScrollerMode.VERTICAL).verticalScrollDisplay(ScrollDisplay.AUTO)
                .horizontalScrollDisplay(ScrollDisplay.NEVER).minScrollPixel(8).maxScrollPixel(22));
        history.viewPort(v -> v.layout(l -> l.paddingAll(0)).style(s -> s.backgroundTexture(IGuiTexture.EMPTY)));
        history.viewContainer(v -> v.layout(l -> l.widthPercent(100).gapAll(14).flexDirection(FlexDirection.COLUMN)));
        history.viewContainer.addEventListener(UIEvents.LAYOUT_CHANGED, event -> {
            if (session.autoFollow()) applyAutoScroll(); else restoreManualScroll();
        });
        history.verticalScroller.setOnValueChanged(value -> {
            if (applyingAutoScroll || !session.view().historyOpen()
                    || session.view().page() != NovaTerminalViewState.Page.COMMS || currentScrollRange() <= 0) return;
            boolean atLatest = isAtLatest(value);
            session.setAutoFollow(atLatest);
            if (!atLatest) session.setManualScrollPixels(value * currentScrollRange());
        });
        history.viewPort.addEventListener(UIEvents.CLICK, event -> {
            if (event.button == GLFW.GLFW_MOUSE_BUTTON_LEFT && session.isStreaming()) {
                completeStream();
                event.stopPropagation();
            }
        });
    }

    private UIElement buildOptions() {
        optionSection.layout(l -> l.widthPercent(100).flexShrink(0).gapAll(5));
        optionHint.addClass("ship-ai-option-hint");
        optionHint.setAllowHitTest(false);
        optionHint.setOverflowVisible(false);
        optionHint.layout(l -> l.widthPercent(100).minHeight(16).flexShrink(0));
        optionHint.textStyle(s -> s.adaptiveWidth(false).adaptiveHeight(true).textWrap(TextWrap.WRAP)
                .textAlignHorizontal(Horizontal.LEFT).lineSpacing(2));
        configureButton(singleAction, "command-primary", Horizontal.LEFT);
        singleAction.layout(l -> l.widthPercent(100).minHeight(26).heightAuto().paddingLeft(14).flexShrink(0));
        singleAction.text.layout(l -> l.heightAuto());
        singleAction.textStyle(s -> s.adaptiveHeight(true));
        ((NovaTerminalButton) singleAction).onActivate(this::beginCoreReboot);
        topicControls.addClass("ship-ai-option-controls");
        topicControls.setOverflowVisible(false);
        topicControls.layout(l -> l.widthPercent(100).gapAll(5).flexDirection(FlexDirection.COLUMN));
        configureButton(progressionButton, "command-primary", Horizontal.LEFT);
        progressionButton.layout(l -> l.widthPercent(100).maxWidth(150).heightAuto().minHeight(26).paddingLeft(14).flexShrink(0));
        progressionButton.text.layout(l -> l.heightAuto());
        progressionButton.textStyle(s -> s.adaptiveHeight(true));
        ((NovaTerminalButton) progressionButton).onActivate(this::selectProgression);
        configureButton(topicToggle, "command-link", Horizontal.LEFT);
        topicToggle.layout(l -> l.widthPercent(100).height(18).paddingHorizontal(0).flexShrink(0));
        ((NovaTerminalButton) topicToggle).onActivate(() -> {
            topicsExpanded = !topicsExpanded;
            syncPresentation();
        });
        topicGrid.layout(l -> l.widthPercent(100).gapAll(4).flexShrink(0));
        SituationTopic[] topics = SituationTopic.values();
        for (int rowIndex = 0; rowIndex < 2; rowIndex++) {
            UIElement row = NovaCommandLayout.row("ship-ai-topic-row");
            row.layout(l -> l.widthPercent(100).heightAuto().minHeight(26).gapAll(4).flexShrink(0)
                    .alignItems(AlignItems.STRETCH));
            for (int columnIndex = 0; columnIndex < 2; columnIndex++) {
                SituationTopic topic = topics[rowIndex * 2 + columnIndex];
                NovaTerminalButton button = new NovaTerminalButton().onActivate(() -> selectTopic(topic));
                configureButton(button, "ship-ai-topic-button", Horizontal.LEFT);
                button.layout(l -> l.flex(1).minWidth(0).heightAuto().minHeight(26));
                button.text.layout(l -> l.heightAuto());
                button.textStyle(s -> s.adaptiveHeight(true));
                topicButtons.put(topic, button);
                row.addChild(button);
            }
            topicGrid.addChild(row);
        }
        topicControls.addChildren(progressionButton, topicToggle, topicGrid);
        optionSection.addChildren(optionHint, singleAction, topicControls);
        return optionSection;
    }

    private static void configureButton(Button button, String styleClass, Horizontal alignment) {
        button.addClasses("command-button", styleClass);
        button.textStyle(s -> s.textAlignHorizontal(alignment));
    }

    private void showCurrentReply() {
        session.view().historyOpen(false);
        if (NovaCommandLayout.compact()) topicsExpanded = false;
        reply.verticalScroller.setNormalizedValue(0);
    }

    private void updateConversationLayout() {
        boolean transcript = session.view().historyOpen();
        boolean selectingTopics = NovaCommandLayout.compact() && topicsExpanded && topicControls.isDisplayed() && !transcript;
        replyHeader.setDisplay(!selectingTopics);
        reply.setDisplay(!transcript && !selectingTopics);
        history.setDisplay(transcript);
        optionSection.setDisplay(!transcript);
        topicGrid.setDisplay(topicsExpanded);
        progressionButton.setDisplay(!selectingTopics);
        optionSection.layout(l -> l.flexGrow(selectingTopics ? 1 : 0).flexShrink(0));
        topicToggle.setText(Component.translatable(selectingTopics ? "gui.starboundmc.ship_ai.back_to_dialogue"
                : topicsExpanded ? "gui.starboundmc.ship_ai.topics_hide" : "gui.starboundmc.ship_ai.topics_show"));
        transcriptToggle.setText(Component.translatable(transcript ? "gui.starboundmc.ship_ai.back_to_dialogue" : "gui.starboundmc.ship_ai.transcript"));
    }

    private void tickTerminal()
    {
        commandLayout.tick(session.isTransmitting() || session.pendingRequest() != null);
        boolean snapshotChanged = refreshAuthoritativeSnapshot();
        int repairInventoryState = localRepairInventoryState();
        boolean repairInventoryChanged = repairInventoryState != observedRepairInventoryState;
        observedRepairInventoryState = repairInventoryState;
        int previousStreamingIndex = session.streamingIndex();
        int revealedCodePoint = session.nextUnrevealedCodePoint();
        ClientShipAiTerminalState.CompletionIntent completed = session.advanceStream();
        if (revealedCodePoint >= 0)
        {
            portrait.onTextAdvanced();
            dialogueSounds.onCodePointRevealed(revealedCodePoint);
        }

        syncTranscriptViews();
        if (previousStreamingIndex >= 0 && previousStreamingIndex < transcriptLabels.size())
            updateTranscriptLine(previousStreamingIndex);
        int nextStreamingIndex = session.streamingIndex();
        if (nextStreamingIndex >= 0 && nextStreamingIndex < transcriptLabels.size())
            updateTranscriptLine(nextStreamingIndex);

        if (!completed.isNone())
            dispatchCompletion(completed);
        if (snapshotChanged || repairInventoryChanged
                || previousStreamingIndex != nextStreamingIndex || !completed.isNone())
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
        boolean acknowledged = session.acknowledge(containerId, pending.requestId());
        if (acknowledged)
            portrait.triggerConfirmation();
        return acknowledged;
    }

    private void enqueueAutomaticCues()
    {
        if (authoritativeSnapshot == null || baseNode == null)
            return;

        switch (baseNode)
        {
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
                            currentStatusText(),
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
                "gui.starboundmc.ship_ai.prologue.boot.restarting"
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

        showCurrentReply();
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

        showCurrentReply();
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
            case CLAIM_TASK_REWARD, TRACK_TASK -> throw new IllegalArgumentException("Task requests use the command page");
        };
        ModNetwork.sendToServer(packet);
        syncPresentation();
    }

    private boolean canBeginCoreReboot()
    {
        return authoritativeSnapshot != null && session.pendingRequest() == null && !session.isTransmitting()
                && baseNode == PrologueDialogueNode.REBOOT_REQUIRED
                && authoritativeSnapshot.shared().core() == CoreState.OFFLINE;
    }

    private boolean canSelectTopics()
    {
        return authoritativeSnapshot != null && session.pendingRequest() == null && !session.isTransmitting()
                && authoritativeSnapshot.player().identityConfirmed()
                && (baseNode == PrologueDialogueNode.SITUATION_HUB
                    || baseNode == PrologueDialogueNode.CURRENT_OBJECTIVE);
    }

    private boolean canSelectProgression()
    {
        if (!canSelectTopics())
            return false;
        if (authoritativeSnapshot.shared().sublightEngine() == EngineState.IGNITING)
            return false;
        return true;
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

    private void syncTranscriptViews()
    {
        while (transcriptLabels.size() < session.messageCount())
            appendTranscriptView(transcriptLabels.size());
        updateCurrentReply();
    }

    private void syncPresentation()
    {
        syncTranscriptViews();
        boolean transmitting = session.isTransmitting();
        portrait.setCoreState(authoritativeSnapshot != null ? authoritativeSnapshot.shared().core() : null);
        portrait.setActivity(portraitActivity());
        portrait.setSpeaking(transmitting);
        portraitState.setText(portraitStatusText(transmitting));
        setClass(portraitState, "command-fault", authoritativeSnapshot != null && authoritativeSnapshot.shared().core() == CoreState.OFFLINE);
        updateConversationLayout();

        if (authoritativeSnapshot == null)
        {
            showHint(Component.translatable("gui.starboundmc.ship_ai.prologue.syncing"));
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
        optionHint.style(style -> style.tooltips(TooltipLines.split(text)));
        optionHint.setDisplay(true);
        singleAction.setDisplay(false);
        topicControls.setDisplay(false);
        updateConversationLayout();
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
        singleAction.style(style -> style.tooltips(TooltipLines.split(actionText)));
        updateConversationLayout();
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
            button.setText(option);
            Component tooltip = interactive
                    ? option
                    : session.isTransmitting()
                            ? Component.translatable("gui.starboundmc.ship_ai.receiving")
                            : session.pendingRequest() != null
                                    ? Component.translatable(
                                            "gui.starboundmc.ship_ai.prologue.action_pending")
                                    : option;
            button.style(style -> style.tooltips(TooltipLines.split(tooltip)));
            button.setActive(interactive);
            setClass(button, TOPIC_READ_CLASS, read);
            setClass(button, TOPIC_CURRENT_CLASS, topic == session.selectedTopic());
        }

        Component nextText = Component.translatable("gui.starboundmc.ship_ai.prologue.option.next");
        boolean progressionActive = canSelectProgression();
        progressionButton.setText(nextText);
        Component progressionTooltip = progressionActive ? nextText : currentStatusText();
        progressionButton.style(style -> style.tooltips(TooltipLines.split(progressionTooltip)));
        progressionButton.setActive(progressionActive);
        topicToggle.setActive(interactive);
        updateConversationLayout();
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

    private Component portraitStatusText(boolean transmitting)
    {
        if (transmitting)
            return Component.translatable("gui.starboundmc.ship_ai.portrait.speaking");
        if (authoritativeSnapshot == null)
            return Component.translatable("gui.starboundmc.ship_ai.portrait.syncing");
        return Component.translatable(switch (authoritativeSnapshot.shared().core())
        {
            case OFFLINE -> "gui.starboundmc.ship_ai.portrait.offline";
            case REBOOTING -> "gui.starboundmc.ship_ai.portrait.recovering";
            case ONLINE -> portrait.hasPortraitAsset()
                    ? "gui.starboundmc.ship_ai.portrait.idle"
                    : "gui.starboundmc.ship_ai.portrait.prototype";
        });
    }

    private NovaPortraitActivity portraitActivity()
    {
        if (authoritativeSnapshot == null)
            return NovaPortraitActivity.IDLE;
        ClientShipStoryState.SharedView shared = authoritativeSnapshot.shared();
        if (shared.core() != CoreState.ONLINE)
            return NovaPortraitActivity.WARNING;
        return shared.mineralScan() == MineralScanState.SCANNING
                ? NovaPortraitActivity.SCANNING : NovaPortraitActivity.IDLE;
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
        if (shared.core() == CoreState.ONLINE && shared.surfaceMission() != SurfaceMissionState.LOCKED)
            return commandLayout.currentGuidance();
        return shared.surfaceMission() == SurfaceMissionState.COMPLETE
                ? repairObjectiveText(shared)
                : Component.translatable("gui.starboundmc.ship_ai.prologue.current_objective.summary");
    }

    private Component repairObjectiveText(ClientShipStoryState.SharedView shared)
    {
        if (shared.sublightEngine() == EngineState.IGNITING)
            return Component.translatable("gui.starboundmc.ship_ai.repair.igniting");
        if (shared.sublightEngine() == EngineState.ONLINE)
            return Component.translatable("gui.starboundmc.ship_ai.repair.complete");
        if (shared.mineralScan() != MineralScanState.COMPLETE)
            return Component.translatable(NovaTaskGuidance.translation(
                    shared.mineralScan() == MineralScanState.LOCKED ? "repair.wood" : "repair.scanning", "help"));
        if (!hasUpgradedManipulator())
            return Component.translatable("gui.starboundmc.ship_ai.repair.need_upgrade");
        return Component.translatable(hasIgnitionCore()
                ? "gui.starboundmc.ship_ai.repair.install_core"
                : "gui.starboundmc.ship_ai.repair.print_core");
    }

    private boolean hasUpgradedManipulator()
    {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null)
            return false;
        for (ItemStack stack : minecraft.player.getInventory().items)
        {
            if (stack.getItem() instanceof MatterManipulatorItem
                    && MatterManipulatorItem.getMiningLevel(stack) >= 1)
                return true;
        }
        for (ItemStack stack : minecraft.player.getInventory().offhand)
        {
            if (stack.getItem() instanceof MatterManipulatorItem
                    && MatterManipulatorItem.getMiningLevel(stack) >= 1)
                return true;
        }
        return false;
    }

    private boolean hasIgnitionCore()
    {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null)
            return false;
        for (ItemStack stack : minecraft.player.getInventory().items)
        {
            if (stack.is(ModItems.SUBLIGHT_IGNITION_CORE.get())) return true;
        }
        for (ItemStack stack : minecraft.player.getInventory().offhand)
        {
            if (stack.is(ModItems.SUBLIGHT_IGNITION_CORE.get())) return true;
        }
        return false;
    }

    private int localRepairInventoryState()
    {
        return (hasUpgradedManipulator() ? 2 : 0) + (hasIgnitionCore() ? 1 : 0);
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

    private void appendTranscriptView(int messageIndex)
    {
        preserveManualScroll();
        var message = session.message(messageIndex);
        boolean fromPlayer = message.speaker() == ClientShipAiTerminalState.Speaker.PLAYER;
        UIElement row = new UIElement();
        row.setAllowHitTest(false);
        row.layout(l -> l.widthPercent(100).flexShrink(0).flexDirection(FlexDirection.ROW)
                .justifyContent(AlignContent.FLEX_START));
        UIElement entry = new UIElement().addClass(fromPlayer ? "ship-ai-message-player" : "ship-ai-message-nova");
        entry.setAllowHitTest(false);
        entry.layout(l -> l.widthPercent(100).flexShrink(0).gapAll(4).flexDirection(FlexDirection.COLUMN));
        Label speaker = new Label();
        speaker.addClass("ship-ai-message-speaker");
        speaker.setAllowHitTest(false);
        speaker.layout(l -> l.widthPercent(100).height(9).flexShrink(0));
        speaker.setText(Component.translatable(message.speaker().translationKey())
                .withStyle(style -> style.withColor(speakerColor(message.speaker()))));
        speaker.textStyle(s -> s.adaptiveWidth(false).textWrap(TextWrap.HIDE)
                .textAlignHorizontal(Horizontal.LEFT));
        Label label = new Label();
        label.addClass("ship-ai-history-text");
        label.setAllowHitTest(false);
        label.setOverflowVisible(false);
        label.layout(layout -> layout.widthPercent(100).minHeight(11).flexShrink(0));
        label.textStyle(style -> style
                .adaptiveWidth(false)
                .adaptiveHeight(true)
                .textWrap(TextWrap.WRAP)
                .textAlignHorizontal(Horizontal.LEFT)
                .lineSpacing(3));

        transcriptLabels.add(label);
        updateTranscriptLine(messageIndex);
        entry.addChildren(speaker, label);
        row.addChild(entry);
        history.addScrollViewChild(row);
    }

    private void updateTranscriptLine(int messageIndex)
    {
        ClientShipAiTerminalState.Message message = session.message(messageIndex);
        Component body = session.isStreamingLine(messageIndex)
                ? Component.literal(session.visibleStreamingText())
                : message.body().copy();
        transcriptLabels.get(messageIndex).setText(body.copy().withStyle(style -> style.withColor(BODY_COLOR)));
    }

    private void updateCurrentReply() {
        int index = session.latestReplyIndex();
        if (index < 0) {
            replySpeaker.setText(Component.translatable("gui.starboundmc.ship_ai.speaker.nova"));
            replyBody.setText(Component.translatable("gui.starboundmc.ship_ai.prologue.syncing"));
            return;
        }
        var message = session.message(index);
        replySpeaker.setText(Component.translatable(message.speaker().translationKey())
                .withStyle(s -> s.withColor(speakerColor(message.speaker()))));
        replyBody.setText(session.isStreamingLine(index) ? Component.literal(session.visibleStreamingText()) : message.body().copy());
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

    private void applyAutoScroll()
    {
        applyingAutoScroll = true;
        history.verticalScroller.setNormalizedValue(1.0F);
        applyingAutoScroll = false;
    }

    private void preserveManualScroll()
    {
        if (!session.autoFollow() && session.view().historyOpen())
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
