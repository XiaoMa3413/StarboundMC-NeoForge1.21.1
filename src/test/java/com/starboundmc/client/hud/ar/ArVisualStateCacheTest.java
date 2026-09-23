// SPDX-License-Identifier: MPL-2.0
package com.starboundmc.client.hud.ar;

import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;
import org.junit.jupiter.api.Test;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ArVisualStateCacheTest {
    @Test
    void firstAcquisitionCompletesAndReentryUsesRecognitionMemory() {
        var cache = new ArVisualStateCache();
        var target = target("ship", "ship", "SHIP 50m");
        cache.beginFrame(0);
        var state = cache.present(target, false);
        assertEquals(0, state.labelOpacity());
        assertFalse(state.known());
        advance(cache, target, 60, false);
        assertTrue(state.known());
        assertEquals(1, state.labelOpacity());
        cache.beginFrame(.02);
        cache.present(target, true);
        cache.beginFrame(.02);
        cache.present(target, false);
        assertTrue(state.recovering());
        assertEquals(1, state.lockProgress());
        advance(cache, target, 12, false);
        assertEquals(1, state.labelOpacity());
    }

    @Test
    void seeingAnEdgeArrowDoesNotPretendTheObjectWasIdentified() {
        var cache = new ArVisualStateCache();
        var target = target("relay", "relay", "RELAY");
        var state = advance(cache, target, 60, true);
        assertFalse(state.known());
        cache.beginFrame(0);
        cache.present(target, false);
        assertEquals(0, state.lockProgress());
        assertFalse(state.recovering());
    }

    @Test
    void telemetryChangesNeverRestartIdentificationButSemanticChangesBlend() {
        var cache = new ArVisualStateCache();
        var state = advance(cache, target("relay", "signal", "SIGNAL 100m"), 60, false);
        cache.beginFrame(.01);
        cache.present(target("relay", "signal", "SIGNAL 99m"), false);
        assertNull(state.previousLabel());
        assertEquals(1, state.labelOpacity());
        cache.beginFrame(0);
        cache.present(target("relay", "abandoned", "ABANDONED RELAY 99m"), false);
        assertEquals("SIGNAL 99m", state.previousLabel().getString());
        assertEquals(0, state.identityBlend());
        assertEquals(1, state.lockProgress());
        advance(cache, target("relay", "abandoned", "ABANDONED RELAY 98m"), 30, false);
        assertNull(state.previousLabel());
        assertEquals(1, state.identityBlend());
    }

    @Test
    void pausesFreezeAgesAndMissingTargetsExpireWithBoundedMemory() {
        var cache = new ArVisualStateCache();
        var target = target("ship", "ship", "SHIP");
        cache.beginFrame(.1);
        var state = cache.present(target, false);
        float before = state.lockProgress();
        for (int i = 0; i < 100; i++) {
            cache.beginFrame(0);
            cache.present(target, false);
        }
        assertEquals(before, state.lockProgress());
        for (int i = 0; i < 121; i++) cache.beginFrame(.25);
        assertEquals(0, cache.size());
        for (int i = 0; i < 200; i++) cache.present(target("target_" + i, "poi", "POI"), true);
        assertTrue(cache.size() <= 128);
        cache.clear();
        assertEquals(0, cache.size());
    }

    @Test
    void firstAcquisitionHasTheSameProgressAt30_60_And144Fps() {
        for (int fps : new int[]{30, 60, 144}) {
            var cache = new ArVisualStateCache();
            var target = target("ship", "ship", "SHIP");
            ArVisualStateCache.State state = null;
            for (int i = 0; i < fps / 2; i++) {
                cache.beginFrame(1D / fps);
                state = cache.present(target, false);
            }
            assertEquals(1, state.labelOpacity(), .00001);
            assertEquals(1, state.lockProgress(), .00001);
        }
    }

    @Test
    void labelReplacementNeverOverlaysTwoDifferentNames() {
        var cache = new ArVisualStateCache();
        var state = advance(cache, target("relay", "signal", "SIGNAL"), 60, false);
        for (int frame = 0; frame < 40; frame++) {
            cache.beginFrame(1D / 144);
            cache.present(target("relay", "relay", "RELAY"), false);
            assertEquals(0, state.identityBlend() * state.previousLabelOpacity(), .00001);
        }
    }

    @Test
    void briefProviderAbsenceAndCameraChangesRetainKnownIdentities() {
        var cache = new ArVisualStateCache();
        var target = target("ship", "ship", "SHIP");
        var state = advance(cache, target, 60, false);
        for (int frame = 0; frame < 10; frame++) cache.beginFrame(.1);
        cache.beginFrame(0);
        cache.present(target, false);
        assertTrue(state.recovering());
        cache.leaveView();
        cache.beginFrame(0);
        cache.present(target, false);
        assertTrue(state.known());
        assertEquals(1, state.lockProgress());
    }

    @Test
    void warningWinsAttentionAndOrdinaryTargetsHaveNoRepeatingCue() {
        var poi = target("poi", "poi", "POI");
        var objective = new ArTarget(ResourceLocation.fromNamespaceAndPath("starboundmc", "objective"),
                ArTargetCategory.OBJECTIVE, Vec3.ZERO, Component.literal("OBJECTIVE"), ArGuidanceMode.TARGET,
                200, 100, 0xFFFFFF);
        var warning = new ArTarget(ResourceLocation.fromNamespaceAndPath("starboundmc", "warning"),
                ArTargetCategory.WARNING, Vec3.ZERO, Component.literal("WARNING"), ArGuidanceMode.TARGET,
                50, 100, 0xFFFFFF);
        assertEquals(warning.id(), ArVisualStateCache.attentionOwner(List.of(poi, objective, warning)));
        assertNull(ArVisualStateCache.attentionOwner(List.of(poi)));
        var cache = new ArVisualStateCache();
        int activeFrames = 0;
        // Run past the acquisition-age cap: warning pulses must still return to silence.
        for (int frame = 0; frame < 60 * 90; frame++) {
            cache.beginFrame(1D / 60);
            assertEquals(0, cache.present(poi, false).attention(true));
            var state = cache.present(warning, false);
            assertEquals(0, state.attention(false));
            if (frame >= 60 * 80 && state.attention(true) > .01) activeFrames++;
        }
        assertTrue(activeFrames > 0 && activeFrames < 150);
    }

    private static ArVisualStateCache.State advance(ArVisualStateCache cache, ArTarget target, int frames, boolean edge) {
        ArVisualStateCache.State result = null;
        for (int i = 0; i < frames; i++) {
            cache.beginFrame(1D / 60);
            result = cache.present(target, edge);
        }
        return result;
    }

    private static ArTarget target(String id, String identity, String label) {
        return new ArTarget(ResourceLocation.fromNamespaceAndPath("starboundmc", id), ArTargetCategory.POI,
                Vec3.ZERO, Component.literal(label), ArGuidanceMode.TARGET, 50, 100, 0x95E8E2, identity);
    }
}
