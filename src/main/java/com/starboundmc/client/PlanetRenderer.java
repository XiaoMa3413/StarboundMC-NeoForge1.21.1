package com.starboundmc.client;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexBuffer;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.blaze3d.vertex.VertexFormatElement;
import com.starboundmc.StarboundMC;
import com.starboundmc.client.space.CelestialLod;
import com.starboundmc.client.space.CelestialLodPolicy;
import com.starboundmc.client.space.CelestialLodTransitions;
import com.starboundmc.client.space.SpaceCoordinateFrame;
import com.starboundmc.client.space.SpaceRenderContext;
import com.starboundmc.client.space.StarSystemResolver;
import com.starboundmc.client.space.StellarLod;
import com.starboundmc.space.UniversePosition;
import com.starboundmc.warp.ShipFlightController;
import com.starboundmc.warp.UniverseNavigation;
import com.starboundmc.world.universe.BodySpaceVisualProfile;
import com.starboundmc.world.universe.CelestialBodyDefinition;
import com.starboundmc.client.StarmapUniverse;
import com.starboundmc.world.universe.StarSystemDefinition;
import net.minecraft.client.Camera;
import net.minecraft.client.renderer.FogRenderer;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.joml.Vector3f;


/**
 * Draws visible planets and moons in the ship's sky frame.
 *
 * <p>The top-level {@link SpaceRenderer} owns the render event and pass order;
 * background, system-star, ring and warp drawing live in their own renderers.</p>
 */
public class PlanetRenderer
{
    static final float PLANET_RADIUS = 50.0F;

    /**
     * Bodies drawn outside the window, sourced from the universe
     * catalog and re-read when it changes, so a datapack body renders without a
     * new enum constant.
     */
    private static CelestialBodyDefinition[] drawOrder = new CelestialBodyDefinition[0];
    private static double[] drawDistanceSq = new double[0];
    /**
     * Distance-driven body quality with a temporal blend to avoid popping.
     *
     * <p>Null until the draw order is known: the table rejects a zero capacity, and
     * the catalog is not readable during class initialisation.</p>
     */
    private static CelestialLodTransitions planetLodTransitions;
    /** The catalog the arrays above were built from, so a rebuild is detectable. */
    private static Object drawOrderCatalog;
    private static final float PLANET_SKY_DISTANCE = 280.0F;
    private static final float MIN_PLANET_SKY_RADIUS = 0.12F;
    /** One shared, unlit mesh; per-body lighting is supplied as shader uniforms. */
    static final VertexFormat PLANET_SURFACE_FORMAT = VertexFormat.builder()
            .add("Position", VertexFormatElement.POSITION)
            .add("UV0", VertexFormatElement.UV0)
            .add("Normal", VertexFormatElement.NORMAL)
            .build();
    private static VertexBuffer planetSurfaceBuffer;
    /** The overworld moon changes lighting only when its discrete moon phase changes. */
    private static VertexBuffer moonSurfaceBuffer;
    private static float moonSurfaceSunX = Float.NaN;
    private static float moonSurfaceSunY = Float.NaN;
    private static float moonSurfaceSunZ = Float.NaN;


    /**
     * Rebuilds the draw arrays when the active universe changes.
     *
     * <p>Compared by identity for the same reason the route cache is: the arrays
     * bake in positions and radii, and a rebuilt catalog invalidates all of them.
     * A transient empty catalog (a client that has not synced yet) keeps the
     * previous arrays rather than blanking the sky.</p>
     */
    private static void refreshDrawOrder()
    {
        Object catalog = StarmapUniverse.catalogIdentity();
        if (drawOrderCatalog == catalog && drawOrder.length > 0)
            return;

        java.util.List<CelestialBodyDefinition> bodies = StarmapUniverse.spaceRenderedBodies();
        if (bodies.isEmpty())
            return;

        drawOrder = bodies.toArray(CelestialBodyDefinition[]::new);
        drawDistanceSq = new double[drawOrder.length];
        planetLodTransitions = new CelestialLodTransitions(drawOrder.length * 2);
        drawOrderCatalog = catalog;
    }

    /**
     * The cockpit-window visual for a body, or a neutral default.
     *
     * <p>Falls back rather than throwing: the renderer is called from the frame
     * loop, and a body with no authored visual should draw plainly, not crash the
     * client. The default is only reachable for a body whose definition omits the
     * optional profile.</p>
     */
    private static BodySpaceVisualProfile visual(CelestialBodyDefinition body)
    {
        return body.spaceVisual().orElse(FALLBACK_VISUAL);
    }

    private static final BodySpaceVisualProfile FALLBACK_VISUAL = new BodySpaceVisualProfile(
            java.util.Optional.empty(), 0.0F, 0.0F, 0.0F, 0.0F,
            0.0F, 0.0F, 0.0F, 0xFFFFFFFF, 0.20F, 0.00375F, 0.10F,
            0.0F, 1.0F, 0.0F,
            java.util.Optional.empty());

    /**
     * The texture for a body's sphere.
     *
     * <p>Every body the renderer draws authors its own texture, so there is no
     * fallback to the legacy per-planet naming. A body that somehow lacks one
     * draws as a visible missing-texture marker rather than silently borrowing
     * another body's art.</p>
     */
    private static ResourceLocation textureOf(CelestialBodyDefinition body)
    {
        String authored = visual(body).texture().orElse(null);
        return authored == null ? MISSING_TEXTURE : ResourceLocation.parse(authored);
    }

    private static final ResourceLocation MISSING_TEXTURE =
            ResourceLocation.fromNamespaceAndPath(StarboundMC.MODID, "textures/planet/missing.png");

    // Arrival crossfade: the target planet grows and fades in over the last ~28%
    // of the warp while the ship swings back to face it. Package-visible so the
    // star map (ShipConsoleScreen) can sync its ship animation to the same timing.

    // Cached UV-sphere geometry - 32×64 for 4k/8k textures, silky round (was 16×32, faceting visible at 38°)
    private static final int SPHERE_STACKS = 32;
    private static final int SPHERE_SLICES = 64;
    static final float[] SPHERE_X;
    static final float[] SPHERE_Y;
    static final float[] SPHERE_Z;
    static final float[] SPHERE_U;
    static final float[] SPHERE_V;

    // The additive atmosphere is deliberately lower-detail than the textured
    // surface: its soft alpha gradient hides the extra facets, while this cuts
    // its per-frame CPU work to one quarter of the surface mesh.
    private static final int HALO_STACKS = 16;
    private static final int HALO_SLICES = 32;
    private static final float[] HALO_X;
    private static final float[] HALO_Y;
    private static final float[] HALO_Z;

    static
    {
        int vertices = SPHERE_STACKS * SPHERE_SLICES * 4;
        SPHERE_X = new float[vertices];
        SPHERE_Y = new float[vertices];
        SPHERE_Z = new float[vertices];
        SPHERE_U = new float[vertices];
        SPHERE_V = new float[vertices];

        int idx = 0;
        for (int i = 0; i < SPHERE_STACKS; i++)
        {
            float phi0 = (float) (Math.PI * i / SPHERE_STACKS);
            float phi1 = (float) (Math.PI * (i + 1) / SPHERE_STACKS);
            float v0 = (float) i / SPHERE_STACKS;
            float v1 = (float) (i + 1) / SPHERE_STACKS;
            for (int j = 0; j < SPHERE_SLICES; j++)
            {
                float theta0 = (float) (2.0 * Math.PI * j / SPHERE_SLICES);
                float theta1 = (float) (2.0 * Math.PI * (j + 1) / SPHERE_SLICES);
                float u0 = (float) j / SPHERE_SLICES;
                float u1 = (float) (j + 1) / SPHERE_SLICES;

                putSphereVertex(idx++, phi0, theta0, u0, v0);
                putSphereVertex(idx++, phi0, theta1, u1, v0);
                putSphereVertex(idx++, phi1, theta1, u1, v1);
                putSphereVertex(idx++, phi1, theta0, u0, v1);
            }
        }

        int haloVertices = HALO_STACKS * HALO_SLICES * 4;
        HALO_X = new float[haloVertices];
        HALO_Y = new float[haloVertices];
        HALO_Z = new float[haloVertices];
        idx = 0;
        for (int i = 0; i < HALO_STACKS; i++)
        {
            float phi0 = (float) (Math.PI * i / HALO_STACKS);
            float phi1 = (float) (Math.PI * (i + 1) / HALO_STACKS);
            for (int j = 0; j < HALO_SLICES; j++)
            {
                float theta0 = (float) (2.0 * Math.PI * j / HALO_SLICES);
                float theta1 = (float) (2.0 * Math.PI * (j + 1) / HALO_SLICES);
                putHaloVertex(idx++, phi0, theta0);
                putHaloVertex(idx++, phi0, theta1);
                putHaloVertex(idx++, phi1, theta1);
                putHaloVertex(idx++, phi1, theta0);
            }
        }
    }

    private static void putSphereVertex(int idx, float phi, float theta, float u, float v)
    {
        float sinPhi = (float) Math.sin(phi);
        SPHERE_X[idx] = PLANET_RADIUS * sinPhi * (float) Math.cos(theta);
        SPHERE_Y[idx] = PLANET_RADIUS * (float) Math.cos(phi);
        SPHERE_Z[idx] = PLANET_RADIUS * sinPhi * (float) Math.sin(theta);
        SPHERE_U[idx] = u;
        SPHERE_V[idx] = v;
    }

    private static void putHaloVertex(int idx, float phi, float theta)
    {
        float sinPhi = (float) Math.sin(phi);
        HALO_X[idx] = PLANET_RADIUS * sinPhi * (float) Math.cos(theta);
        HALO_Y[idx] = PLANET_RADIUS * (float) Math.cos(phi);
        HALO_Z[idx] = PLANET_RADIUS * sinPhi * (float) Math.sin(theta);
    }

    static void renderVisiblePlanets(PoseStack pose, Camera camera, SpaceRenderContext space,
                                     SpaceCoordinateFrame coordinateFrame,
                                     StarSystemResolver.ResolvedStarField stars)
    {
        boolean longRoute = space.warpDurationTicks() > ShipFlightController.SHORT_ROUTE_TICKS;
        float warpProgress = space.warpProgress();
        UniversePosition ship = space.universePosition();
        // Ownership now comes from the universe catalog rather than a Planet
        // lookup, so the comparison below is id-based.
        String sourceSystemId = StarmapUniverse.systemIdOfEntry(space.currentBodyId());
        String targetSystemId = StarmapUniverse.systemIdOfEntry(space.targetBodyId());
        refreshDrawOrder();
        for (int i = 0; i < drawOrder.length; i++)
            drawDistanceSq[i] = UniverseNavigation
                    .universeBodyPosition(drawOrder[i].entryId()).distanceToSqr(ship);

        // Reusable insertion sort; the celestial count is small enough that this
        // beats building and sorting a per-frame collection.
        for (int i = 1; i < drawOrder.length; i++)
        {
            CelestialBodyDefinition body = drawOrder[i];
            double distance = drawDistanceSq[i];
            int j = i;
            while (j > 0 && drawDistanceSq[j - 1] < distance)
            {
                drawOrder[j] = drawOrder[j - 1];
                drawDistanceSq[j] = drawDistanceSq[j - 1];
                j--;
            }
            drawOrder[j] = body;
            drawDistanceSq[j] = distance;
        }

        for (CelestialBodyDefinition body : drawOrder)
        {
            StarSystemDefinition system = StarmapUniverse.systemOf(body.entryId());
            float systemVisibility = stellarVisibility(stars, system);
            boolean departingSystemBody = space.warping() && longRoute
                    && warpProgress < WarpVisualTiming.ARRIVAL_FADE_START
                    && sourceSystemId != null && sourceSystemId.equals(system.systemId());
            boolean arrivingSystemBody = space.warping() && longRoute
                    && warpProgress >= WarpVisualTiming.ARRIVAL_FADE_START
                    && targetSystemId != null && targetSystemId.equals(system.systemId());
            // Keep the entire source system during the departure leg. This
            // preserves the primary/companion relationship (for example the
            // lush world and its molten moon) instead of dropping every body
            // except the one the ship is docked at.
            boolean routePriority = departingSystemBody || arrivingSystemBody;
            if (longRoute && space.warping() && !routePriority)
            {
                updatePlanetLod(body, CelestialLod.CULLED, space.animationTicks());
                continue;
            }

            Vec3 bodyCenter = coordinateFrame.toView(
                    UniverseNavigation.universeBodyPosition(body.entryId()));
            double distance = bodyCenter.length();
            double angularDiameter = CelestialLodPolicy.angularDiameterDegrees(
                    UniverseNavigation.radius(body.entryId()), distance);
            CelestialLod previous = planetLodTransitions.currentLod(body.entryId());
            CelestialLod requested = CelestialLodPolicy.hysteretic(
                    angularDiameter, previous, routePriority ? CelestialLod.POINT : CelestialLod.CULLED);
            // The star's alpha fades with the system's VISUAL influence, but a
            // system's outer berths (the gas giant) sit far beyond that fade.
            // Bodies stay renderable while the ship is inside the system's
            // planet field regardless of how dim the remote star looks.
            if (systemVisibility <= 0.20F && !insidePlanetField(space.universePosition(), system)
                    && !routePriority)
                requested = CelestialLod.CULLED;
            float detail = updatePlanetLod(body, requested, space.animationTicks());

            // The hand-off still switches at the shared arrival boundary, but
            // the body itself is no longer faded in. Distance LOD blending is
            // the only visual transition, so an approaching planet cannot
            // appear, disappear, and then restart a second fade.
            if (detail > 0.001F)
                renderVirtualPlanet(pose, camera, body, space, coordinateFrame, 1.0F, detail);
        }
    }

    private static float updatePlanetLod(CelestialBodyDefinition body, CelestialLod requested, float animationTicks)
    {
        return planetLodTransitions.update(body.entryId(), requested, animationTicks);
    }
    private static float stellarVisibility(StarSystemResolver.ResolvedStarField stars, StarSystemDefinition system)
    {
        if (system == null)
            return 0.0F;
        for (int i = 0; i < stars.count(); i++)
        {
            StarSystemResolver.VisibleStar star = stars.star(i);
            // The resolver still reports the legacy system type; compare by id so
            // the two representations of "the same system" agree.
            if (star.system() != null && star.system().systemId().equals(system.systemId()))
                return star.alpha();
        }
        return 0.0F;
    }

    /** Whether the ship sits inside the system's body-rendering field. */
    private static boolean insidePlanetField(UniversePosition ship, StarSystemDefinition system)
    {
        return system != null
                && ship.distanceToSqr(system.navigationCenter())
                        <= system.planetFieldRadius() * system.planetFieldRadius();
    }

    /** Draw one body at true near distance or angularly projected on the sky shell. */
    private static void renderVirtualPlanet(PoseStack pose, Camera camera, CelestialBodyDefinition body,
                                            SpaceRenderContext space, SpaceCoordinateFrame coordinateFrame,
                                            float alpha, float lodDetail)
    {
        Vec3 bodyCenter = coordinateFrame.toView(
                UniverseNavigation.universeBodyPosition(body.entryId()));
        float bodyScale = (float) (UniverseNavigation.radius(body.entryId()) / PLANET_RADIUS);
        double distance = bodyCenter.length();
        if (distance > PLANET_SKY_DISTANCE)
        {
            float projectionScale = (float) (PLANET_SKY_DISTANCE / distance);
            bodyCenter = bodyCenter.scale(projectionScale);
            bodyScale *= projectionScale;
        }
        float renderedRadius = PLANET_RADIUS * bodyScale;
        float pointWeight = CelestialLodTransitions.pointWeight(lodDetail);
        float reducedWeight = CelestialLodTransitions.reducedWeight(lodDetail);
        float fullWeight = CelestialLodTransitions.fullWeight(lodDetail);
        float bodyWeight = pointWeight + reducedWeight + fullWeight;
        if (renderedRadius < MIN_PLANET_SKY_RADIUS)
        {
            // A body can cross the point -> reduced boundary while it is still
            // smaller than the minimum sphere radius. Keep one marker alive
            // during that transition instead of fading the point out before a
            // reduced sphere can become visible.
            if (bodyWeight > 0.002F)
                renderPlanetPoint(pose, body, alpha * bodyWeight, (float) bodyCenter.x,
                        (float) bodyCenter.y, (float) bodyCenter.z, renderedRadius);
            return;
        }
        float cx = (float) bodyCenter.x;
        float cy = (float) bodyCenter.y;
        float cz = (float) bodyCenter.z;
        if (fullWeight > 0.002F && renderedRadius >= 0.45F)
            renderAtmosphereGlow(pose, body, bodyScale, alpha * fullWeight,
                    cx, cy, cz);
        if (reducedWeight + fullWeight > 0.002F)
            renderPlanet(pose, camera, body, bodyScale, alpha * (reducedWeight + fullWeight),
                    cx, cy, cz, (float) space.yaw(), (float) space.pitch(), space.animationTicks());
        if (pointWeight > 0.002F)
            renderPlanetPoint(pose, body, alpha * pointWeight, cx, cy, cz, renderedRadius);
    }

    /**
     * Additive limb glow drawn as a single sphere shell around the planet. Alpha
     * is computed from the angular distance to the planet's projected limb: it is
     * brightest at the limb and fades to zero at the outer edge of the shell, so
     * the halo always hugs the planet and never has a bright outer rim.
     */
    private static void renderAtmosphereGlow(PoseStack pose, CelestialBodyDefinition body, float scale, float alpha,
                                             float cx, float cy, float cz)
    {
        Matrix4f matrix = pose.last().pose();
        BodySpaceVisualProfile profile = visual(body);
        Vector3f color = new Vector3f(profile.atmosphereRed(),
                profile.atmosphereGreen(), profile.atmosphereBlue());
        float peak = profile.atmospherePeak();

        float distC = (float) Math.sqrt(cx * cx + cy * cy + cz * cz);
        float axisX = cx / distC;
        float axisY = cy / distC;
        float axisZ = cz / distC;

        float planetRadius = PLANET_RADIUS * scale;
        float outerFactor = 1.15F;
        float outerRadius = planetRadius * outerFactor;
        float limbAngle = (float) Math.asin(Math.min(1.0, planetRadius / distC));
        float outerAngle = (float) Math.asin(Math.min(1.0, outerRadius / distC));
        float angleRange = Math.max(0.0001F, outerAngle - limbAngle);
        // Start the glow slightly inside the planet's projected limb so the halo
        // visibly touches the surface instead of leaving a dark gap at the edge.
        float innerAngle = Math.max(0.0F, limbAngle - 0.05F);
        float innerRange = Math.max(0.0001F, limbAngle - innerAngle);

        FogRenderer.setupNoFog();
        try
        {
            RenderSystem.enableBlend();
            RenderSystem.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE);
            RenderSystem.disableCull();
            RenderSystem.disableDepthTest();
            RenderSystem.depthMask(false);
            RenderSystem.setShader(GameRenderer::getPositionColorShader);
            RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);

            BufferBuilder bb = Tesselator.getInstance().begin(
                    VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
            for (int i = 0; i < HALO_X.length; i++)
            {
                float wx = cx + HALO_X[i] * outerFactor * scale;
                float wy = cy + HALO_Y[i] * outerFactor * scale;
                float wz = cz + HALO_Z[i] * outerFactor * scale;

                float len = (float) Math.sqrt(wx * wx + wy * wy + wz * wz);
                float vx = wx / len;
                float vy = wy / len;
                float vz = wz / len;
                float dot = axisX * vx + axisY * vy + axisZ * vz;
                dot = Math.max(-1.0F, Math.min(1.0F, dot));
                float angle = (float) Math.acos(dot);

                float a = 0.0F;
                if (angle >= innerAngle && angle <= limbAngle)
                {
                    // Ramp up from the inner edge to the limb so the glow overlaps
                    // the planet's rim and appears glued to the surface.
                    float t = (angle - innerAngle) / innerRange;
                    a = peak * smoothstep(t) * alpha;
                }
                else if (angle > limbAngle && angle <= outerAngle)
                {
                    float t = (angle - limbAngle) / angleRange;
                    float fade = (float) Math.pow(1.0F - t, 1.5);
                    a = peak * fade * alpha;
                }
                vertexColor(bb, matrix, wx, wy, wz, color.x, color.y, color.z, a);
            }
            BufferUploader.drawWithShader(bb.buildOrThrow());
        }
        finally
        {
            SpaceRenderPassState.restoreDefaults();
        }
    }

    private static Vector3f fixedSunDirection(CelestialBodyDefinition body)
    {
        Vec3 sun = UniverseNavigation.sunDirection(body.entryId());
        return new Vector3f((float) sun.x, (float) sun.y, (float) sun.z);
    }

    private static float smoothstep(float t)
    {
        t = Math.max(0.0F, Math.min(1.0F, t));
        return t * t * (3.0F - 2.0F * t);
    }

    private static float lerp(float a, float b, float t)
    {
        return a + (b - a) * t;
    }

    /**
     * Small bodies remain readable without keeping a textured sphere alive.
     * The eight-sided marker is intentionally softer than a square billboard,
     * so distant planets do not look like stray pixels in the starfield.
     */
    private static void renderPlanetPoint(PoseStack pose, CelestialBodyDefinition body, float alpha,
                                          float cx, float cy, float cz, float projectedRadius)
    {
        if (alpha <= 0.002F)
            return;
        float size = Math.max(0.30F, Math.min(1.10F, projectedRadius * 0.85F));
        int color = visual(body).pointColor();
        float r = ((color >> 16) & 0xFF) / 255.0F;
        float g = ((color >> 8) & 0xFF) / 255.0F;
        float b = (color & 0xFF) / 255.0F;

        FogRenderer.setupNoFog();
        try
        {
            RenderSystem.enableBlend();
            RenderSystem.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE);
            RenderSystem.disableCull();
            RenderSystem.disableDepthTest();
            RenderSystem.depthMask(false);
            RenderSystem.setShader(GameRenderer::getPositionColorShader);
            RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);

            Matrix4f matrix = pose.last().pose();
            BufferBuilder bb = Tesselator.getInstance().begin(
                    VertexFormat.Mode.TRIANGLES, DefaultVertexFormat.POSITION_COLOR);
            for (int i = 0; i < 8; i++)
            {
                double a0 = Math.PI * 2.0 * i / 8.0;
                double a1 = Math.PI * 2.0 * (i + 1) / 8.0;
                vertexColor(bb, matrix, cx, cy, cz, r, g, b, alpha);
                vertexColor(bb, matrix,
                        cx + (float) Math.cos(a0) * size,
                        cy + (float) Math.sin(a0) * size, cz,
                        r, g, b, alpha * 0.72F);
                vertexColor(bb, matrix,
                        cx + (float) Math.cos(a1) * size,
                        cy + (float) Math.sin(a1) * size, cz,
                        r, g, b, alpha * 0.72F);
            }
            BufferUploader.drawWithShader(bb.buildOrThrow());
        }
        finally
        {
            SpaceRenderPassState.restoreDefaults();
        }
    }

    private static void renderPlanet(PoseStack pose, Camera cam, CelestialBodyDefinition body, float scale, float alpha,
                                     float cx, float cy, float cz, float shipYaw, float shipPitch,
                                     float animationTicks)
    {
        // Skybox-style: the planet is drawn at a fixed offset in the rotation-only
        // AFTER_SKY frame, so it stays visible through the bridge window at all times.
        // The ring writes no depth either, so its far half is drawn first, the disk
        // second, and the near half last to read as orbiting the body.
        BodySpaceVisualProfile profile = visual(body);
        if (RingRenderer.hasRings(profile))
            RingRenderer.drawPlanetRings(pose, profile, cx, cy, cz, scale,
                    shipYaw, shipPitch, alpha, false);
        drawOrientedPlanetSphere(pose.last().pose(), body, cx, cy, cz, scale,
                fixedSunDirection(body), 1.0F, alpha, shipYaw, shipPitch, animationTicks);
        if (RingRenderer.hasRings(profile))
            RingRenderer.drawPlanetRings(pose, profile, cx, cy, cz, scale,
                    shipYaw, shipPitch, alpha, true);
    }

    /** Draws a planet with a fixed body-space orientation, transformed by the ship view. */
    private static void drawOrientedPlanetSphere(Matrix4f matrix, CelestialBodyDefinition body,
                                                  float cx, float cy, float cz, float scale,
                                                  Vector3f worldSun, float brightness, float alpha,
                                                  float shipYaw, float shipPitch, float animationTicks)
    {
        BodySpaceVisualProfile profile = visual(body);
        ResourceLocation diffuseTexture = textureOf(body);
        ResourceLocation materialMaskTexture = profile.materialMask()
                .map(ResourceLocation::parse).orElse(diffuseTexture);
        ShaderInstance previousShader = RenderSystem.getShader();
        int previousDiffuseTexture = RenderSystem.getShaderTexture(0);
        int previousMaterialMaskTexture = RenderSystem.getShaderTexture(1);
        FogRenderer.setupNoFog();
        try
        {
            RenderSystem.enableBlend();
            RenderSystem.defaultBlendFunc();
            RenderSystem.enableDepthTest();
            RenderSystem.enableCull();
            RenderSystem.depthMask(false);
            RenderSystem.setShaderTexture(0, diffuseTexture);
            // Bind a valid texture even for profiles without a mask; the shader's
            // MaterialMaskEnabled uniform controls whether this sampler is read.
            RenderSystem.setShaderTexture(1, materialMaskTexture);

            float spinDegrees = animationTicks * spinRate(body);
            // Keep the sphere mesh shared and unrotated. Body orientation and spin
            // stay in the model matrix, while the matching inverse is applied to
            // the fixed virtual-space sun direction for per-fragment lighting.
            Matrix4f model = new Matrix4f(matrix)
                    .translate(cx, cy, cz)
                    .rotateX((float) Math.toRadians(-shipPitch))
                    .rotateY((float) Math.toRadians(-shipYaw));
            PlanetSurfaceLighting.appendBodyOrientation(model, spinDegrees,
                    profile.orientationTilt(), profile.orientationYaw()).scale(scale);
            Vector3f meshSpaceSun = PlanetSurfaceLighting.toMeshSpaceSun(worldSun, spinDegrees,
                    profile.orientationTilt(), profile.orientationYaw());
            VertexBuffer surface = getPlanetSurfaceBuffer();
            ShaderInstance surfaceShader = PlanetSurfaceShader.current();
            if (surfaceShader != null)
            {
                RenderSystem.setShader(() -> surfaceShader);
                RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
                Vector3f cameraPositionMesh = PlanetSurfaceLighting.cameraPositionMesh(model);
                PlanetSurfaceShader.setLighting(surfaceShader, meshSpaceSun, cameraPositionMesh,
                        terminatorWidth(body), nightFloor(body),
                        profile.specularStrength(), profile.roughness(), profile.fresnelStrength(),
                        profile.materialMask().isPresent(), alpha, brightness);
            }
            else
            {
                RenderSystem.setShader(GameRenderer::getPositionTexShader);
                RenderSystem.setShaderColor(brightness, brightness, brightness, alpha);
            }
            surface.bind();
            try
            {
                try
                {
                    surface.drawWithShader(model, RenderSystem.getProjectionMatrix(),
                            surfaceShader == null ? RenderSystem.getShader() : surfaceShader);
                }
                catch (RuntimeException shaderFailure)
                {
                    if (surfaceShader == null)
                        throw shaderFailure;

                    PlanetSurfaceShader.disableAfterFailure(surfaceShader, shaderFailure);
                    RenderSystem.setShader(GameRenderer::getPositionTexShader);
                    RenderSystem.setShaderColor(brightness, brightness, brightness, alpha);
                    surface.drawWithShader(model, RenderSystem.getProjectionMatrix(), RenderSystem.getShader());
                }
            }
            finally
            {
                VertexBuffer.unbind();
            }
        }
        finally
        {
            RenderSystem.setShader(() -> previousShader);
            RenderSystem.setShaderTexture(0, previousDiffuseTexture);
            RenderSystem.setShaderTexture(1, previousMaterialMaskTexture);
            SpaceRenderPassState.restoreDefaults();
        }
    }

    private static VertexBuffer getPlanetSurfaceBuffer()
    {
        if (planetSurfaceBuffer != null && !planetSurfaceBuffer.isInvalid())
            return planetSurfaceBuffer;
        if (planetSurfaceBuffer != null)
            planetSurfaceBuffer.close();

        BufferBuilder bb = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, PLANET_SURFACE_FORMAT);
        for (int i = 0; i < SPHERE_X.length; i++)
        {
            float localX = SPHERE_X[i];
            float localY = SPHERE_Y[i];
            float localZ = SPHERE_Z[i];
            bb.addVertex(localX, localY, localZ)
                    .setUv(SPHERE_U[i], SPHERE_V[i])
                    .setNormal(localX / PLANET_RADIUS, localY / PLANET_RADIUS, localZ / PLANET_RADIUS);
        }

        VertexBuffer buffer = new VertexBuffer(VertexBuffer.Usage.STATIC);
        try
        {
            buffer.bind();
            buffer.upload(bb.buildOrThrow());
        }
        catch (RuntimeException failure)
        {
            buffer.close();
            throw failure;
        }
        finally
        {
            VertexBuffer.unbind();
        }
        planetSurfaceBuffer = buffer;
        return buffer;
    }

    /**
     * Draws a textured, sun-lit planet sphere at the given center. Also used by
     * {@link MoltenMoonRenderer} for the overworld moon (with a phase-dependent
     * sun direction and brightness).
     */
    static void drawPlanetSphere(PoseStack pose, Matrix4f matrix, ResourceLocation texture,
                                 float cx, float cy, float cz, float scale,
                                 Vector3f sun, float brightness, float alpha)
    {
        FogRenderer.setupNoFog();
        try
        {
            RenderSystem.enableBlend();
            RenderSystem.defaultBlendFunc();
            RenderSystem.enableDepthTest();
            RenderSystem.enableCull();
            RenderSystem.depthMask(false);
            RenderSystem.setShader(GameRenderer::getPositionTexColorShader);
            RenderSystem.setShaderTexture(0, texture);
            RenderSystem.setShaderColor(brightness, brightness, brightness, alpha);

            // This shared helper serves normal-world sky renderers, which own
            // camera rotation through RenderSystem's model-view. Ship-space
            // planets use drawOrientedPlanetSphere with the caller's full frame.
            Matrix4f model = new Matrix4f(RenderSystem.getModelViewMatrix())
                    .mul(matrix)
                    .translate(cx, cy, cz)
                    .scale(scale);
            VertexBuffer surface = getMoonSurfaceBuffer(sun);
            surface.bind();
            try
            {
                surface.drawWithShader(model, RenderSystem.getProjectionMatrix(), RenderSystem.getShader());
            }
            finally
            {
                VertexBuffer.unbind();
            }
        }
        finally
        {
            SpaceRenderPassState.restoreDefaults();
        }
    }

    private static VertexBuffer getMoonSurfaceBuffer(Vector3f sun)
    {
        boolean lightingChanged = moonSurfaceBuffer == null || moonSurfaceBuffer.isInvalid()
                || Float.compare(moonSurfaceSunX, sun.x) != 0
                || Float.compare(moonSurfaceSunY, sun.y) != 0
                || Float.compare(moonSurfaceSunZ, sun.z) != 0;
        if (!lightingChanged)
            return moonSurfaceBuffer;

        BufferBuilder bb = Tesselator.getInstance().begin(
                VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX_COLOR);
        for (int i = 0; i < SPHERE_X.length; i++)
            addLitSphereVertex(bb, SPHERE_X[i], SPHERE_Y[i], SPHERE_Z[i], SPHERE_U[i], SPHERE_V[i], sun);

        if (moonSurfaceBuffer == null || moonSurfaceBuffer.isInvalid())
            moonSurfaceBuffer = new VertexBuffer(VertexBuffer.Usage.DYNAMIC);
        moonSurfaceBuffer.bind();
        try
        {
            moonSurfaceBuffer.upload(bb.buildOrThrow());
        }
        finally
        {
            VertexBuffer.unbind();
        }
        moonSurfaceSunX = sun.x;
        moonSurfaceSunY = sun.y;
        moonSurfaceSunZ = sun.z;
        return moonSurfaceBuffer;
    }

    private static float terminatorWidth(CelestialBodyDefinition body)
    {
        return visual(body).terminatorWidth();
    }

    /** Degrees per game tick; one revolution takes several real minutes. */
    private static float spinRate(CelestialBodyDefinition body)
    {
        return visual(body).spinRate();
    }

    private static float nightFloor(CelestialBodyDefinition body)
    {
        return visual(body).nightFloor();
    }

    private static void addLitSphereVertex(BufferBuilder bb, float x, float y, float z,
                                           float u, float v, Vector3f sun)
    {
        addLitSphereVertex(bb, x, y, z, u, v, sun, 0.22F, 0.08F);
    }

    private static void addLitSphereVertex(BufferBuilder bb, float x, float y, float z,
                                           float u, float v, Vector3f sun,
                                           float terminatorWidth, float nightFloor)
    {
        float dot = x / PLANET_RADIUS * sun.x
                + y / PLANET_RADIUS * sun.y
                + z / PLANET_RADIUS * sun.z;
        float shade = 1.0F - smoothstep((dot + terminatorWidth) / (terminatorWidth * 2.0F));
        shade *= 1.0F - nightFloor;
        float r = lerp(1.0F, 0.06F, shade);
        float g = lerp(1.0F, 0.08F, shade);
        float b = lerp(1.0F, 0.20F, shade);
        float terminator = Math.max(0.0F, 1.0F - Math.abs(dot) / terminatorWidth);
        r += (0.90F - r) * terminator * 0.35F;
        g += (0.55F - g) * terminator * 0.25F;
        b += (0.25F - b) * terminator * 0.18F;
        bb.addVertex(x, y, z).setColor(r, g, b, 1.0F).setUv(u, v);
    }

    private static void vertexColor(BufferBuilder bb, Matrix4f matrix, float x, float y, float z,
                                    float r, float g, float b, float a)
    {
        bb.addVertex(matrix, x, y, z).setColor(r, g, b, a);
    }
}
