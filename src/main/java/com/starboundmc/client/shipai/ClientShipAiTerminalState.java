package com.starboundmc.client.shipai;

import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * Connection-scoped local state for the T0 ship AI dialogue prototype.
 *
 * <p>This intentionally stores only translation keys and streaming progress.
 * UI elements remain owned by each screen instance, while future authoritative
 * story and shared-ship state will live on the server.</p>
 */
public final class ClientShipAiTerminalState {
    private static Session current = new Session();

    private ClientShipAiTerminalState() {
    }

    static Session current() {
        return current;
    }

    public static void resetConnectionState() {
        current = new Session();
    }

    enum Speaker {
        SYSTEM("gui.starboundmc.ship_ai.speaker.system"),
        NOVA("gui.starboundmc.ship_ai.speaker.nova"),
        PLAYER("gui.starboundmc.ship_ai.speaker.player");

        private final String translationKey;

        Speaker(String translationKey) {
            this.translationKey = translationKey;
        }

        String translationKey() {
            return translationKey;
        }
    }

    record Message(Speaker speaker, String bodyKey) {
    }

    static final class Session {
        private final List<Message> messages = new ArrayList<>();
        private int streamingIndex = -1;
        private int revealedCodePoints;

        private Session() {
            appendCompleted(Speaker.SYSTEM, "gui.starboundmc.ship_ai.demo.notice");
            startStreaming("gui.starboundmc.ship_ai.demo.welcome");
        }

        int messageCount() {
            return messages.size();
        }

        Message message(int index) {
            return messages.get(index);
        }

        boolean isStreaming() {
            return streamingIndex >= 0;
        }

        boolean isStreamingLine(int index) {
            return index == streamingIndex;
        }

        int streamingIndex() {
            return streamingIndex;
        }

        String visibleStreamingText() {
            if (!isStreaming()) {
                return "";
            }
            int[] codePoints = localizedBody(streamingIndex).codePoints().toArray();
            int visibleLength = Math.min(revealedCodePoints, codePoints.length);
            return new String(codePoints, 0, visibleLength);
        }

        int selectOption(String optionKey, String responseKey) {
            if (isStreaming()) {
                return -1;
            }
            int firstNewIndex = messages.size();
            appendCompleted(Speaker.PLAYER, optionKey);
            startStreaming(responseKey);
            return firstNewIndex;
        }

        /** @return true when this tick completed the active transmission. */
        boolean advanceStream() {
            if (!isStreaming()) {
                return false;
            }
            String body = localizedBody(streamingIndex);
            int length = body.codePointCount(0, body.length());
            revealedCodePoints = Math.min(length, revealedCodePoints + 1);
            if (revealedCodePoints >= length) {
                finishStream();
                return true;
            }
            return false;
        }

        void completeStream() {
            if (isStreaming()) {
                finishStream();
            }
        }

        private void appendCompleted(Speaker speaker, String bodyKey) {
            messages.add(new Message(speaker, bodyKey));
        }

        private void startStreaming(String bodyKey) {
            messages.add(new Message(Speaker.NOVA, bodyKey));
            streamingIndex = messages.size() - 1;
            revealedCodePoints = 0;
        }

        private void finishStream() {
            streamingIndex = -1;
            revealedCodePoints = 0;
        }

        private String localizedBody(int index) {
            return Component.translatable(messages.get(index).bodyKey()).getString();
        }
    }
}
