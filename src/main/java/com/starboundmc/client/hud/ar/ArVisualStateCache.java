// SPDX-License-Identifier: MPL-2.0
package com.starboundmc.client.hud.ar;

import com.starboundmc.client.hud.animation.HudComponentPresentation;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.List;

/** Connection-local recognition memory. It contains no screen position, camera lag or projection. */
public final class ArVisualStateCache {
    private static final int MAX_REMEMBERED = 128;
    private static final double ABSENT_SECONDS = 30;
    private static final boolean DISABLED = Boolean.getBoolean("starboundmc.debug.hudNoTransitions");
    private final Map<ResourceLocation, State> states = new LinkedHashMap<>();
    private double time;
    private double delta;
    private long frame;

    public void beginFrame(double seconds) {
        delta = Double.isFinite(seconds) && seconds > 0 ? Math.min(seconds, .25) : 0;
        time += delta;
        frame++;
        states.values().removeIf(state -> time - state.lastPresent > ABSENT_SECONDS);
    }

    public State present(ArTarget target, boolean edge) {
        State state = states.get(target.id());
        if (state == null) {
            if (states.size() >= MAX_REMEMBERED) {
                var oldest = states.entrySet().stream()
                        .min(Comparator.comparingDouble(entry -> entry.getValue().lastPresent)).orElseThrow();
                states.remove(oldest.getKey());
            }
            state = new State(target);
            states.put(target.id(), state);
        }
        if (state.lastFrame == frame) return state;
        boolean changed = !state.identificationKey.equals(target.identificationKey())
                || state.category != target.category() || state.guidance != target.guidance();
        if (changed) {
            state.previousLabel = state.label;
            state.identificationAge = 0;
            state.identificationKey = target.identificationKey();
            state.category = target.category();
            state.guidance = target.guidance();
        }
        if (state.lastFrame != frame - 1 || state.edge != edge) {
            state.entryAge = 0;
            state.recovering = state.known;
        }
        state.edge = edge;
        state.label = target.label();
        state.lastFrame = frame;
        state.lastPresent = time;
        state.entryAge = Math.min(60, state.entryAge + delta);
        // Keep periodic attention separate from the bounded one-shot acquisition age.
        state.attentionAge = (state.attentionAge + delta) % 3.5;
        state.identificationAge = Math.min(1, state.identificationAge + delta);
        if (!edge && state.entryAge >= .55) state.known = true;
        if (state.identificationAge >= .24) state.previousLabel = null;
        return state;
    }

    /** Different camera, same world: preserve recognized identities, discard interrupted screen entrances. */
    public void leaveView() { states.values().forEach(state -> state.lastFrame = -2); }
    public void clear() { states.clear(); time = 0; frame = 0; delta = 0; }
    public int size() { return states.size(); }

    public static boolean important(ArTarget target) {
        return target.category() == ArTargetCategory.WARNING
                || target.category() == ArTargetCategory.OBJECTIVE && target.priority() >= 100
                || target.category() == ArTargetCategory.INTERACTION && target.priority() >= 120;
    }

    public static ResourceLocation attentionOwner(List<ArTarget> targets) {
        ArTarget selected = null;
        for (var target : targets) {
            if (!important(target)) continue;
            boolean urgent = target.category() == ArTargetCategory.WARNING;
            boolean selectedUrgent = selected != null && selected.category() == ArTargetCategory.WARNING;
            if (selected == null || urgent && !selectedUrgent
                    || urgent == selectedUrgent && target.priority() > selected.priority()) selected = target;
        }
        return selected == null ? null : selected.id();
    }

    public static final class State {
        private String identificationKey;
        private ArTargetCategory category;
        private ArGuidanceMode guidance;
        private Component label;
        private Component previousLabel;
        private long lastFrame = -2;
        private double lastPresent;
        private double entryAge;
        private double attentionAge;
        private double identificationAge = 1;
        private boolean edge;
        private boolean known;
        private boolean recovering;

        private State(ArTarget target) {
            identificationKey = target.identificationKey();
            category = target.category();
            guidance = target.guidance();
            label = target.label();
        }

        public boolean known() { return known; }
        public boolean recovering() { return recovering; }
        public Component previousLabel() { return previousLabel; }
        public float identityBlend() { return DISABLED ? 1 : smooth((identificationAge - .1) / .14); }
        public float previousLabelOpacity() { return DISABLED ? 0 : 1 - smooth(identificationAge / .1); }
        public float lockProgress() { return DISABLED || recovering ? 1 : smooth((entryAge - .06) / .24); }
        public float labelOpacity() {
            if (DISABLED) return 1;
            return edge || recovering ? smooth(entryAge / .12) : smooth((entryAge - .24) / .26);
        }
        public float markerOpacity() { return DISABLED ? 1 : .45F + .55F * smooth(entryAge / .12); }
        public float edgeSettle() { return DISABLED ? 0 : 1 - smooth(entryAge / .18); }

        /** Only the pass's highest-priority eligible target receives a cue at any time. */
        public float attention(boolean selected) {
            if (!selected || DISABLED) return 0;
            if (category == ArTargetCategory.WARNING) {
                double phase = attentionAge;
                return phase < .6 ? (float) Math.pow(Math.sin(Math.PI * phase / .6), 2) : 0;
            }
            return previousLabel == null ? 0 : 1 - identityBlend();
        }

        public String phase() {
            if (edge) return "EDGE";
            if (previousLabel != null) return "IDENTITY";
            if (recovering && entryAge < .12) return "RESTORE";
            if (lockProgress() < 1) return entryAge < .06 ? "SIGNAL" : "LOCK";
            return labelOpacity() < 1 ? "IDENTIFY" : "STABLE";
        }

        private static float smooth(double value) { return HudComponentPresentation.smooth((float) value); }
    }
}
