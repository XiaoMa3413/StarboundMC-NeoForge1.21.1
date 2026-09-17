package com.starboundmc.world.universe;

import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * Proves the STAR_SYSTEM datapack registry actually loads the shipped
 * definitions at runtime.
 *
 * <p>The JSON in {@code src/generated/resources} is only a build artifact until
 * a server reads it back through the registry. This test goes through that
 * round trip: it resolves the registry from the running server, pulls both
 * systems out by key, and checks the geometry survived.</p>
 */
@GameTestHolder("starboundmc")
@PrefixGameTestTemplate(false)
public final class UniverseRegistryGameTests
{
    @GameTest(template = "shuttle_test_empty")
    public static void starSystemRegistryLoadsBothSystems(GameTestHelper helper)
    {
        var registry = helper.getLevel().registryAccess()
                .registryOrThrow(ModUniverseRegistries.STAR_SYSTEM);

        var main = registry.get(ModUniverseRegistries.systemKey("sys1"));
        var cold = registry.get(ModUniverseRegistries.systemKey("sys2"));

        helper.assertTrue(main != null, "sys1 missing from the star_system registry");
        helper.assertTrue(cold != null, "sys2 missing from the star_system registry");
        helper.assertTrue(registry.size() == 2,
                "expected exactly 2 star systems, found " + registry.size());

        helper.assertTrue("sys1".equals(main.systemId()), "sys1 has the wrong system id");
        helper.assertTrue(main.bodies().size() == 5,
                "sys1 should declare 5 bodies, found " + main.bodies().size());
        helper.assertTrue(cold.bodies().size() == 1,
                "sys2 should declare 1 body, found " + cold.bodies().size());

        // Geometry read back through a codec must be bit-identical to the values
        // the generator wrote, or a docked ship would render off-position.
        var lush = main.bodies().stream()
                .filter(body -> body.entryId().equals("sys1:lush")).findFirst().orElse(null);
        helper.assertTrue(lush != null, "sys1:lush missing");
        var navigation = lush.navigation().orElse(null);
        helper.assertTrue(navigation != null, "sys1:lush lost its navigation profile");
        helper.assertTrue(Math.abs(navigation.dockPosition().localY() - 102.0) < 1.0E-9,
                "lush dock Y drifted through the registry: " + navigation.dockPosition().localY());
        helper.assertTrue(Math.abs(navigation.bodyRadius() - 6.0) < 1.0E-9,
                "lush body radius drifted through the registry: " + navigation.bodyRadius());
        helper.assertTrue(lush.orbit().parentEntryId().isEmpty(), "lush must orbit the star");

        // The molten moon must keep both its parent link and its radius.
        var molten = main.bodies().stream()
                .filter(body -> body.entryId().equals("sys1:molten")).findFirst().orElse(null);
        helper.assertTrue(molten != null, "sys1:molten missing");
        helper.assertTrue(molten.orbit().parentEntryId().orElse("").equals("sys1:lush"),
                "molten lost its parent link");

        // The gas giant is flyable but orbit-only, and ringed.
        var gasGiant = main.bodies().stream()
                .filter(body -> body.entryId().equals("sys1:gasgiant")).findFirst().orElse(null);
        helper.assertTrue(gasGiant != null, "sys1:gasgiant missing");
        helper.assertTrue(gasGiant.isNavigable(), "the gas giant must be navigable");
        helper.assertTrue(gasGiant.starmapVisual().getMarkerSize() > 0,
                "the gas giant must appear on the star map");
        helper.assertTrue(gasGiant.isSpaceRendered(), "the gas giant is drawn in space");
        helper.assertTrue(gasGiant.spaceVisual().orElseThrow().hasRings(),
                "the gas giant lost its ring through the registry");

        // Its moon survives the round trip too, ring-free and landable.
        var rockyMoon = main.bodies().stream()
                .filter(body -> body.entryId().equals("sys1:rockymoon")).findFirst().orElse(null);
        helper.assertTrue(rockyMoon != null, "sys1:rockymoon missing");
        helper.assertTrue(rockyMoon.isNavigable(), "the rocky moon must be navigable");
        helper.assertTrue(rockyMoon.isLandable(), "the rocky moon must be landable");
        helper.assertTrue(rockyMoon.parentEntryId().orElse("").equals("sys1:gasgiant"),
                "the rocky moon lost its parent link");
        // The system's body-rendering field has to reach its outer berths, or the
        // giant and moon vanish from the sky exactly when the ship arrives.
        double moonBerth = Math.sqrt(rockyMoon.navigation().orElseThrow().dockPosition()
                .distanceToSqr(main.navigationCenter()));
        helper.assertTrue(moonBerth <= main.planetFieldRadius(),
                "the moon's berth sits outside the system's planet field: "
                        + (long) moonBerth + " > " + (long) main.planetFieldRadius());

        helper.succeed();
    }

    @GameTest(template = "shuttle_test_empty")
    public static void catalogBuiltFromTheLiveRegistryMatchesLegacyGeometry(GameTestHelper helper)
    {
        var registry = helper.getLevel().registryAccess()
                .registryOrThrow(ModUniverseRegistries.STAR_SYSTEM);

        UniverseCatalog catalog = UniverseCatalog.of(registry.stream().toList());
        helper.assertTrue(catalog.systemCount() == 2, "catalog lost a system");
        helper.assertTrue(catalog.bodyCount() == 6, "catalog lost a body");
        helper.assertTrue(catalog.navigableBodies().size() == 6, "expected 6 navigable bodies");
        // The gas giant is flyable but has no surface, so it is orbit-only.
        helper.assertTrue(catalog.surfaceBodies().size() == 5, "expected 5 landable bodies");

        // Every navigable body must resolve to the dock the flight layer uses, so
        // the refactor cannot move a ship parked at any of them. The body list is
        // the catalog's own; the legacy enum it used to walk is gone (A10).
        for (var entry : com.starboundmc.world.universe.LegacyUniverseCompatibility
                .legacyNameMapping().entrySet())
        {
            String entryId = entry.getValue();
            var body = catalog.body(entryId).orElse(null);
            helper.assertTrue(body != null, "catalog missing " + entryId);
            var navigation = body.navigation().orElse(null);
            helper.assertTrue(navigation != null, entryId + " lost navigation in the catalog");

            // The ship's flight geometry resolves through this same catalog, so a
            // mismatch here would place a docked ship off its planet.
            var flownDock = com.starboundmc.warp.UniverseNavigation.universeDock(entryId);
            helper.assertTrue(navigation.dockPosition().equals(flownDock),
                    entryId + " dock differs from the dock the flight layer uses");
            var flownRadius = com.starboundmc.warp.UniverseNavigation.radius(entryId);
            helper.assertTrue(Math.abs(navigation.bodyRadius() - flownRadius) < 1.0E-9,
                    entryId + " radius differs from the radius the flight layer uses");
        }

        // Ownership comes from the data, not from splitting the id on ':'.
        helper.assertTrue("sys1".equals(catalog.systemOfBody("sys1:lush").orElseThrow().systemId()),
                "sys1:lush resolved to the wrong system");
        helper.assertTrue("sys2".equals(catalog.systemOfBody("sys2:frozen").orElseThrow().systemId()),
                "sys2:frozen resolved to the wrong system");

        helper.succeed();
    }

    /**
     * The client's sync path, exercised against a real {@code RegistryAccess}.
     *
     * <p>{@code refreshFrom} is what makes a datapack universe reach the star map;
     * a unit test can only reach it by faking the registry, so this goes through
     * the live one and then checks the client drops back to the built-in baseline
     * on disconnect.</p>
     */
    @GameTest(template = "shuttle_test_empty")
    public static void clientCatalogAdoptsTheServerRegistryAndResetsAfterwards(GameTestHelper helper)
    {
        try
        {
            ClientUniverseCatalog.reset();
            helper.assertTrue(!ClientUniverseCatalog.isSynced(),
                    "the client should start on the built-in baseline");
            helper.assertTrue(ClientUniverseCatalog.current().systemCount() == 2,
                    "the baseline should carry both built-in systems");

            ClientUniverseCatalog.refreshFrom(helper.getLevel().registryAccess());

            helper.assertTrue(ClientUniverseCatalog.isSynced(),
                    "refreshFrom must adopt the live registry");
            helper.assertTrue(ClientUniverseCatalog.current().systemCount() == 2,
                    "the synced universe should carry both systems");
            helper.assertTrue(ClientUniverseCatalog.current().bodyCount() == 6,
                    "the synced universe should carry all six bodies");

            // Geometry read from the synced registry must match what the ship uses.
            var lush = ClientUniverseCatalog.current().body("sys1:lush").orElse(null);
            helper.assertTrue(lush != null, "the synced universe lost sys1:lush");
            helper.assertTrue(lush.navigation().orElseThrow().dockPosition()
                            .equals(com.starboundmc.warp.UniverseNavigation.universeDock("sys1:lush")),
                    "the synced dock position differs from the dock the flight layer uses");

            ClientUniverseCatalog.reset();
            helper.assertTrue(!ClientUniverseCatalog.isSynced(),
                    "reset must clear the synced flag");
            helper.assertTrue(ClientUniverseCatalog.current().systemCount() == 2,
                    "reset must restore the built-in universe");
        }
        finally
        {
            ClientUniverseCatalog.reset();
        }

        helper.succeed();
    }

    /**
     * The server's catalog is built from the same registry, and is independent of
     * the client's.
     *
     * <p>The server is the authority for warps and fuel, so its catalog must be
     * built from the world it is actually running before any travel check reads
     * it.</p>
     */
    @GameTest(template = "shuttle_test_empty")
    public static void serverCatalogBuildsFromTheRunningRegistry(GameTestHelper helper)
    {
        var server = helper.getLevel().getServer();
        try
        {
            ServerUniverseCatalog.initialize(server);

            helper.assertTrue(ServerUniverseCatalog.isSynced(),
                    "the server catalog should have adopted the live registry");
            helper.assertTrue(ServerUniverseCatalog.current().systemCount() == 2,
                    "the server universe should carry both systems");
            helper.assertTrue(ServerUniverseCatalog.current().bodyCount() == 6,
                    "the server universe should carry all six bodies");
            helper.assertTrue(ServerUniverseCatalog.current().navigableBodies().size() == 6,
                    "the server should see six navigable bodies");

            // Geometry from the server's catalog must match what the ship uses.
            var lush = ServerUniverseCatalog.current().body("sys1:lush").orElse(null);
            helper.assertTrue(lush != null, "the server universe lost sys1:lush");
            helper.assertTrue(lush.navigation().orElseThrow().dockPosition()
                            .equals(com.starboundmc.warp.UniverseNavigation.universeDock("sys1:lush")),
                    "the server dock position differs from the dock the flight layer uses");

            // Ownership resolves from data, not from the id prefix.
            helper.assertTrue("sys1".equals(ServerUniverseCatalog.current()
                            .systemOfBody("sys1:lush").orElseThrow().systemId()),
                    "the server resolved sys1:lush to the wrong system");
        }
        finally
        {
            ServerUniverseCatalog.clear();
        }

        helper.assertTrue(!ServerUniverseCatalog.isSynced(),
                "clearing must drop the server back to the built-in universe");

        helper.succeed();
    }
}
