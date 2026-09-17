package com.starboundmc.world;

import com.starboundmc.world.universe.BodyOrbitDefinition;
import com.starboundmc.world.universe.BodySurfaceDefinition;
import com.starboundmc.world.universe.CelestialBodyDefinition;
import com.starboundmc.world.universe.PlanetEnvironmentProfile;
import com.starboundmc.world.universe.UniverseTestSupport;
import com.starboundmc.world.starmap.StarmapBodyType;
import com.starboundmc.world.starmap.StarmapBodyVisual;
import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * A landing that cannot be performed must be refused, not redirected.
 *
 * <p>The old path sent the player to the overworld whenever the real destination
 * was unusable. That is not a harmless fallback: the overworld is a planet surface
 * as far as the story code is concerned, so a redirected landing completed the
 * prologue's surface mission and ran the arrival tutorial for a planet the player
 * never reached.</p>
 *
 * <p>{@code landingRefusal} is the whole decision, kept pure so the table below can
 * be checked without a server. The cases are exhaustive because the failure it
 * prevents is silent: a relocation looks like success to every caller.</p>
 */
class SurfaceLandingRefusalTest
{
    private static final String NO_SURFACE = "message.starboundmc.warp.no_landing";
    private static final String UNKNOWN = "message.starboundmc.warp.unknown_landing";
    private static final String UNAVAILABLE = "message.starboundmc.warp.surface_unavailable";

    @Test
    void aLandableBodyWithItsDimensionPresentIsAllowed()
    {
        CelestialBodyDefinition body = UniverseTestSupport.body("sys1:barren");

        assertNull(Stage6TravelService.landingRefusal(body, true),
                "a body with a surface and a loaded dimension must be landable");
    }

    /**
     * The gas giant is the shipped example: it is flyable but deliberately has no
     * surface, so it is orbit-only.
     */
    @Test
    void aBodyWithoutASurfaceIsRefusedByName()
    {
        CelestialBodyDefinition gasGiant = UniverseTestSupport.body("sys1:gasgiant");

        assertEquals(NO_SURFACE, Stage6TravelService.landingRefusal(gasGiant, false));
        assertEquals(NO_SURFACE, Stage6TravelService.landingRefusal(gasGiant, true),
                "the missing surface definition is the reason, not the dimension");
    }

    /**
     * A saved body id this world does not provide: the case that used to relocate
     * the player to the overworld and hand out the surface mission.
     */
    @Test
    void anUnknownBodyIsRefusedRatherThanRelocated()
    {
        assertEquals(UNKNOWN, Stage6TravelService.landingRefusal(null, false));
        assertEquals(UNKNOWN, Stage6TravelService.landingRefusal(null, true));
    }

    /**
     * A body that declares a surface whose dimension is absent — a datapack body
     * removed from the world. This is the other input that used to fall through to
     * the overworld.
     */
    @Test
    void aBodyWhoseDimensionIsMissingIsRefused()
    {
        CelestialBodyDefinition body = UniverseTestSupport.body("sys1:barren");

        assertEquals(UNAVAILABLE, Stage6TravelService.landingRefusal(body, false),
                "a declared surface with no loaded dimension must not relocate the player");
    }

    /**
     * The same table driven over every shipped body, so a new body cannot slip
     * through as "landable by default".
     *
     * <p>The dimension flag is passed as available, so what this isolates is the
     * body's own definition: a body that declares a surface is allowed, and one that
     * does not is refused. The gas giant is the shipped refusal.</p>
     */
    @Test
    void everyShippedBodyRefusesLandingExactlyWhenItShould()
    {
        for (CelestialBodyDefinition body : UniverseTestSupport.universe().navigableBodies())
        {
            if (body.isLandable())
            {
                assertNull(Stage6TravelService.landingRefusal(body, true),
                        body.entryId() + " declares a surface, so it must be landable");
            }
            else
            {
                assertEquals(NO_SURFACE, Stage6TravelService.landingRefusal(body, true),
                        body.entryId() + " declares no surface, so it must be refused");
            }
            // And with no dimension loaded nothing is landable, whatever the body says.
            assertNotNull(Stage6TravelService.landingRefusal(body, false),
                    body.entryId() + " must be refused when no surface dimension is loaded");
        }
    }

    /**
     * The overworld is a surface as far as the story code is concerned, which is
     * precisely why a redirected landing was able to complete the surface mission.
     * Pinning that fact keeps the refusal in place: as long as the overworld counts
     * as a surface, sending the player there on failure is a progression bug.
     */
    @Test
    void theOverworldCountsAsASurfaceSoAMislandingWouldAdvanceTheStory()
    {
        CelestialBodyDefinition lush = UniverseTestSupport.body("sys1:lush");

        assertEquals(ResourceLocation.parse("minecraft:overworld"),
                lush.surface().orElseThrow().dimension());
        assertEquals(BodySurfaceDefinition.LandingPolicy.OVERWORLD_RESPAWN,
                lush.surface().orElseThrow().landingPolicy());
    }

    /** A body with a surface definition really is landable, per the record. */
    @Test
    void aSurfaceDefinitionMakesABodyLandable()
    {
        CelestialBodyDefinition barren = UniverseTestSupport.body("sys1:barren");
        CelestialBodyDefinition gasGiant = UniverseTestSupport.body("sys1:gasgiant");

        assertEquals(true, barren.isLandable());
        assertEquals(false, gasGiant.isLandable(),
                "the gas giant is the shipped orbit-only body");
    }

    /** A synthetic body pins the "declares a surface, dimension absent" shape. */
    @Test
    void aDeclaredSurfaceWithAMissingDimensionIsTheOnlyOtherRefusal()
    {
        CelestialBodyDefinition declared = new CelestialBodyDefinition(
                "synthetic:gone", "starmap.entry.gone.name", "starmap.type.rocky_moon",
                "starmap.entry.gone.desc", 1,
                BodyOrbitDefinition.aroundStar(40, 0.0F),
                StarmapBodyVisual.builder(StarmapBodyType.ROCKY, 0xFF909090, 10, 1L).build(),
                Optional.empty(), Optional.empty(),
                Optional.of(new BodySurfaceDefinition(
                        ResourceLocation.parse("starboundmc:not_loaded"),
                        BodySurfaceDefinition.LandingPolicy.SURFACE_SCAN,
                        PlanetEnvironmentProfile.TEMPERATE)));

        assertEquals(true, declared.isLandable(),
                "the definition itself claims a surface, which is what makes it a trap");
        assertEquals(UNAVAILABLE, Stage6TravelService.landingRefusal(declared, false),
                "the loaded-dimension check is the only thing that catches this");
        assertNull(Stage6TravelService.landingRefusal(declared, true));
    }
}
