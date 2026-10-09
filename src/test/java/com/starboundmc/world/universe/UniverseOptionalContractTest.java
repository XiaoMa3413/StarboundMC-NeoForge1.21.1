package com.starboundmc.world.universe;

import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class UniverseOptionalContractTest {
    @Test
    void visualProfileAcceptsAbsentCapabilitiesButRejectsNullOptionals() {
        var profile = BodySpaceVisualProfile.CODEC.parse(JsonOps.INSTANCE,
                JsonParser.parseString("{\"point_color\":16777215}")).getOrThrow();
        assertTrue(profile.texture().isEmpty());
        assertTrue(profile.materialMask().isEmpty());
        assertTrue(profile.ringTexture().isEmpty());
        assertTrue(profile.cloudTexture().isEmpty());
        assertEquals(profile, copyVisual(profile, Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty()));
        assertThrows(NullPointerException.class, () -> copyVisual(profile, null, Optional.empty(), Optional.empty(), Optional.empty()));
        assertThrows(NullPointerException.class, () -> copyVisual(profile, Optional.empty(), null, Optional.empty(), Optional.empty()));
        assertThrows(NullPointerException.class, () -> copyVisual(profile, Optional.empty(), Optional.empty(), null, Optional.empty()));
        assertThrows(NullPointerException.class, () -> copyVisual(profile, Optional.empty(), Optional.empty(), Optional.empty(), null));
    }

    @Test
    void bodyAcceptsAbsentCapabilitiesButRejectsNullOptionals() {
        var body = BuiltInUniverse.systems().getFirst().bodies().getFirst();
        var json = CelestialBodyDefinition.CODEC.encodeStart(JsonOps.INSTANCE, body).getOrThrow().getAsJsonObject();
        json.remove("navigation");
        json.remove("space_visual");
        json.remove("surface");
        var absent = CelestialBodyDefinition.CODEC.parse(JsonOps.INSTANCE, json).getOrThrow();
        assertEquals(absent, copyBody(body, Optional.empty(), Optional.empty(), Optional.empty()));
        assertThrows(NullPointerException.class, () -> copyBody(body, null, Optional.empty(), Optional.empty()));
        assertThrows(NullPointerException.class, () -> copyBody(body, Optional.empty(), null, Optional.empty()));
        assertThrows(NullPointerException.class, () -> copyBody(body, Optional.empty(), Optional.empty(), null));
    }

    private static CelestialBodyDefinition copyBody(CelestialBodyDefinition b, Optional<BodyNavigationProfile> navigation,
                                                    Optional<BodySpaceVisualProfile> visual, Optional<BodySurfaceDefinition> surface) {
        return new CelestialBodyDefinition(b.entryId(), b.nameKey(), b.typeKey(), b.descriptionKey(), b.threatLevel(),
                b.orbit(), b.starmapVisual(), navigation, visual, surface);
    }

    private static BodySpaceVisualProfile copyVisual(BodySpaceVisualProfile p, Optional<String> texture,
                                                     Optional<String> mask, Optional<String> ring, Optional<String> cloud) {
        return new BodySpaceVisualProfile(texture, mask, p.emissiveStrength(), p.atmosphereRed(), p.atmosphereGreen(),
                p.atmosphereBlue(), p.atmospherePeak(), p.orientationTilt(), p.orientationYaw(), p.orientationRoll(),
                p.pointColor(), p.terminatorWidth(), p.spinRate(), p.nightFloor(), p.specularStrength(), p.roughness(),
                p.fresnelStrength(), ring, p.atmosphereShellScale(), p.atmosphereNightFraction(),
                p.atmosphereTwilightStrength(), cloud, p.cloudShellScale(), p.cloudOpacity(), p.cloudDriftRate());
    }
}
