package com.starboundmc.client.space;

import com.mojang.logging.LogUtils;
import com.starboundmc.space.UniversePosition;
import com.starboundmc.world.starmap.StarSystem;
import com.starboundmc.world.starmap.StarSystems;
import com.starboundmc.world.starmap.StellarVisualProfile;
import com.starboundmc.world.starmap.StellarDistanceResponse;
import org.slf4j.Logger;

/**
 * Coordinate-driven star-system selection with a hysteresis band. The result
 * owns reusable slots, avoiding a new celestial list and entries every frame.
 */
public final class StarSystemResolver
{
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final double HYSTERESIS_UNITS = 650.0;
    private static final double SWITCH_ADVANTAGE = 0.12;
    private static final float MAX_PROJECTED_RADIUS = 72.0F;
    private static final int MAX_CANDIDATE_SYSTEMS = 64;
    private static final int QUERY_SECTOR_RADIUS = 1;
    private static final long CANDIDATE_REFRESH_TICKS = 10L;
    private static final double CANDIDATE_REFRESH_DISTANCE_SQR = 10_000.0 * 10_000.0;
    private static final StarSystem[] CANDIDATES = new StarSystem[MAX_CANDIDATE_SYSTEMS];
    private static final ResolvedStarField RESULT = new ResolvedStarField(MAX_CANDIDATE_SYSTEMS);
    private static final StellarLodTransitions LOD_TRANSITIONS =
            new StellarLodTransitions(MAX_CANDIDATE_SYSTEMS * 2);
    private static UniversePosition candidateAnchor;
    private static int candidateCount;
    private static long lastCandidateRefreshTick = Long.MIN_VALUE;
    private static String lastActiveSystemId;
    private static String lastCurrentHint;
    private static String lastTargetHint;
    private static boolean initialized;
    private static boolean debugLogging = Boolean.getBoolean("starboundmc.debug.stars");
    private static long lastDebugLogTick = Long.MIN_VALUE;

    private StarSystemResolver()
    {
    }

    public static ResolvedStarField resolve(SpaceRenderContext context)
    {
        UniversePosition ship = context.universePosition();
        long animationTick = (long) context.animationTicks();
        refreshCandidatesIfNeeded(ship, animationTick, context.currentSystemHint(), context.targetSystemHint());
        StarSystem active = resolveActive(ship, context.currentSystemHint(), !initialized);
        initialized = true;
        lastActiveSystemId = active == null ? null : active.getSystemId();
        RESULT.activeSystem = active;
        RESULT.count = 0;

        for (int candidateIndex = 0; candidateIndex < candidateCount; candidateIndex++)
        {
            StarSystem system = CANDIDATES[candidateIndex];
            VisibleStar slot = RESULT.slots[RESULT.count++];
            StellarVisualProfile profile = system.getStellarVisual();
            UniversePosition star = profile.getUniversePosition();
            double rx = ship.deltaXTo(star);
            double ry = ship.deltaYTo(star);
            double rz = ship.deltaZTo(star);
            double starDistance = Math.sqrt(rx * rx + ry * ry + rz * rz);

            double navigationDistance = Math.sqrt(ship.distanceToSqr(system.getUniverseNavigationCenter()));
            double normalized = navigationDistance / Math.max(1.0, system.getInfluenceRadius());
            float influence = 1.0F - smoothstep((float) ((normalized - 0.72) / 0.55));
            float alpha = 0.14F + influence * 0.86F;
            // Stellar light remains readable in deep space. Keep this separate
            // from alpha, which also gates whether a system's planets render.
            float stellarBrightness = 0.62F + influence * 0.38F;

            StellarDistanceResponse response = profile.getDistanceResponse();
            float distanceScale = response.distanceScale(starDistance);
            float projectedRadius = Math.min(MAX_PROJECTED_RADIUS,
                    response.skyRadius(starDistance, influence));
            float coronaDetail = response.coronaWeight(influence);
            float effectDetail = response.effectWeight(influence);

            slot.set(system, rx, ry, rz, starDistance, alpha, stellarBrightness, influence,
                    distanceScale, projectedRadius, coronaDetail, effectDetail);
        }

        // Explicit far-to-near ordering lets nearer stellar layers replace a
        // more distant disc if two systems happen to align on screen.
        for (int i = 1; i < RESULT.count; i++)
        {
            int j = i;
            while (j > 0 && RESULT.slots[j - 1].distance < RESULT.slots[j].distance)
            {
                VisibleStar swap = RESULT.slots[j - 1];
                RESULT.slots[j - 1] = RESULT.slots[j];
                RESULT.slots[j] = swap;
                j--;
            }
        }
        RESULT.targetSystemId = context.targetSystemHint();
        for (int i = 0; i < RESULT.count; i++)
        {
            VisibleStar star = RESULT.slots[i];
            star.navigationTarget = RESULT.targetSystemId != null
                    && RESULT.targetSystemId.equals(star.system.getSystemId());
        }
        StellarLodPolicy.assign(RESULT);
        for (int i = 0; i < RESULT.count; i++)
        {
            VisibleStar star = RESULT.slots[i];
            star.lodDetail = LOD_TRANSITIONS.update(
                    star.system.getSystemId(), star.lod, context.animationTicks());
        }
        RESULT.environment.update(RESULT);
        logDebugState(ship, animationTick);
        return RESULT;
    }

    private static void refreshCandidatesIfNeeded(UniversePosition ship, long animationTick,
                                                   String currentHint, String targetHint)
    {
        boolean tickExpired = lastCandidateRefreshTick == Long.MIN_VALUE
                || animationTick < lastCandidateRefreshTick
                || animationTick - lastCandidateRefreshTick >= CANDIDATE_REFRESH_TICKS;
        boolean movedFar = candidateAnchor == null
                || candidateAnchor.distanceToSqr(ship) >= CANDIDATE_REFRESH_DISTANCE_SQR;
        boolean changedSector = candidateAnchor == null || !candidateAnchor.sameSector(ship);
        boolean changedHints = !java.util.Objects.equals(lastCurrentHint, currentHint)
                || !java.util.Objects.equals(lastTargetHint, targetHint);
        if (!tickExpired && !movedFar && !changedSector && !changedHints)
            return;

        int reservedHints = reservedHintCount(currentHint, targetHint);
        candidateCount = StarSystems.spatialIndex().queryNearby(
                ship.sector(), QUERY_SECTOR_RADIUS, CANDIDATES, CANDIDATES.length - reservedHints);
        candidateCount = appendHintCandidate(currentHint, candidateCount);
        candidateCount = appendHintCandidate(targetHint, candidateCount);
        candidateAnchor = ship;
        lastCandidateRefreshTick = animationTick;
        lastCurrentHint = currentHint;
        lastTargetHint = targetHint;
    }

    private static int appendHintCandidate(String systemId, int count)
    {
        StarSystem hinted = StarSystems.byId(systemId);
        if (hinted == null || count >= CANDIDATES.length)
            return count;
        for (int i = 0; i < count; i++)
            if (CANDIDATES[i] == hinted)
                return count;
        CANDIDATES[count] = hinted;
        return count + 1;
    }

    private static int reservedHintCount(String currentHint, String targetHint)
    {
        StarSystem current = StarSystems.byId(currentHint);
        StarSystem target = StarSystems.byId(targetHint);
        if (current == null)
            return target == null ? 0 : 1;
        return target == null || target == current ? 1 : 2;
    }

    private static StarSystem resolveActive(UniversePosition ship, String hintId, boolean seedFromHint)
    {
        StarSystem previous = StarSystems.byId(lastActiveSystemId);
        StarSystem nearest = null;
        double nearestScore = Double.POSITIVE_INFINITY;
        for (int i = 0; i < candidateCount; i++)
        {
            StarSystem system = CANDIDATES[i];
            double distance = Math.sqrt(ship.distanceToSqr(system.getUniverseNavigationCenter()));
            double score = distance / Math.max(1.0, system.getInfluenceRadius());
            if (score <= 1.0 && score < nearestScore)
            {
                nearest = system;
                nearestScore = score;
            }
        }

        if (previous != null)
        {
            double previousDistance = Math.sqrt(ship.distanceToSqr(previous.getUniverseNavigationCenter()));
            double previousScore = previousDistance / Math.max(1.0, previous.getInfluenceRadius());
            boolean insideHysteresis = previousDistance <= previous.getInfluenceRadius() + HYSTERESIS_UNITS;
            if (insideHysteresis && (nearest == null || nearest == previous
                    || nearestScore + SWITCH_ADVANTAGE >= previousScore))
                return previous;
        }

        if (nearest != null)
            return nearest;
        // Hints seed old saves and docked states but do not override a valid
        // coordinate result, so manual flight remains coordinate-authoritative.
        return seedFromHint ? StarSystems.byId(hintId) : null;
    }

    public static void reset()
    {
        initialized = false;
        lastActiveSystemId = null;
        candidateAnchor = null;
        candidateCount = 0;
        lastCandidateRefreshTick = Long.MIN_VALUE;
        lastCurrentHint = null;
        lastTargetHint = null;
        lastDebugLogTick = Long.MIN_VALUE;
        LOD_TRANSITIONS.reset();
        RESULT.environment.reset();
    }

    public static GalaxyEnvironmentBlend latestEnvironment()
    {
        return RESULT.environment;
    }

    public static void setDebugLogging(boolean enabled)
    {
        debugLogging = enabled;
        lastDebugLogTick = Long.MIN_VALUE;
    }

    public static boolean isDebugLogging()
    {
        return debugLogging;
    }

    private static void logDebugState(UniversePosition ship, long animationTick)
    {
        if (!debugLogging || (lastDebugLogTick != Long.MIN_VALUE
                && animationTick >= lastDebugLogTick
                && animationTick - lastDebugLogTick < 20L))
            return;
        lastDebugLogTick = animationTick;
        LOGGER.info("Stellar debug ship={} active={} candidates={}", ship,
                RESULT.activeSystem == null ? "deep-space" : RESULT.activeSystem.getSystemId(), RESULT.count);
        for (int i = 0; i < RESULT.count; i++)
        {
            VisibleStar star = RESULT.slots[i];
            LOGGER.info("Stellar debug system={} distance={} influence={} brightness={} distanceScale={} skyRadius={} stage={} lod={} blend={}",
                    star.system.getSystemId(), String.format("%.1f", star.distance),
                    String.format("%.3f", star.systemInfluence), String.format("%.3f", star.stellarBrightness),
                    String.format("%.3f", star.distanceScale),
                    String.format("%.2f", star.projectedRadius), star.visualStage(), star.lod,
                    String.format("%.2f/%.2f/%.2f", star.pointLodWeight(),
                            star.simplifiedLodWeight(), star.fullLodWeight()));
        }
    }

    private static float smoothstep(float value)
    {
        float t = clamp(value, 0.0F, 1.0F);
        return t * t * (3.0F - 2.0F * t);
    }

    private static float clamp(float value, float min, float max)
    {
        return Math.max(min, Math.min(max, value));
    }

    public static final class ResolvedStarField implements StellarLodPolicy.Candidates
    {
        private final VisibleStar[] slots;
        private final GalaxyEnvironmentBlend environment;
        private int count;
        private StarSystem activeSystem;
        private String targetSystemId;

        private ResolvedStarField(int capacity)
        {
            slots = new VisibleStar[capacity];
            environment = new GalaxyEnvironmentBlend(capacity);
            for (int i = 0; i < capacity; i++)
                slots[i] = new VisibleStar();
        }

        public int count()
        {
            return count;
        }

        public VisibleStar star(int index)
        {
            if (index < 0 || index >= count)
                throw new IndexOutOfBoundsException(index);
            return slots[index];
        }

        public StarSystem activeSystem()
        {
            return activeSystem;
        }

        public GalaxyEnvironmentBlend environment()
        {
            return environment;
        }

        @Override
        public float projectedRadius(int index)
        {
            return slots[index].projectedRadius;
        }

        @Override
        public float systemInfluence(int index)
        {
            return slots[index].systemInfluence;
        }

        @Override
        public boolean activeSystem(int index)
        {
            return slots[index].system == activeSystem;
        }

        @Override
        public boolean navigationTarget(int index)
        {
            return slots[index].navigationTarget;
        }

        @Override
        public StellarLod lod(int index)
        {
            return slots[index].lod;
        }

        @Override
        public void setLod(int index, StellarLod lod)
        {
            slots[index].lod = lod;
        }
    }

    public static final class VisibleStar
    {
        private StarSystem system;
        private double relativeX;
        private double relativeY;
        private double relativeZ;
        private double distance;
        private float alpha;
        private float stellarBrightness;
        private float systemInfluence;
        private float distanceScale;
        private float projectedRadius;
        private float coronaDetail;
        private float effectDetail;
        private StellarLod lod = StellarLod.POINT;
        private float lodDetail;
        private boolean navigationTarget;

        private void set(StarSystem system, double relativeX, double relativeY, double relativeZ,
                         double distance, float alpha, float stellarBrightness,
                         float systemInfluence, float distanceScale,
                         float projectedRadius, float coronaDetail, float effectDetail)
        {
            this.system = system;
            this.relativeX = relativeX;
            this.relativeY = relativeY;
            this.relativeZ = relativeZ;
            this.distance = distance;
            this.alpha = alpha;
            this.stellarBrightness = stellarBrightness;
            this.systemInfluence = systemInfluence;
            this.distanceScale = distanceScale;
            this.projectedRadius = projectedRadius;
            this.coronaDetail = coronaDetail;
            this.effectDetail = effectDetail;
        }

        public StarSystem system() { return system; }
        public double relativeX() { return relativeX; }
        public double relativeY() { return relativeY; }
        public double relativeZ() { return relativeZ; }
        public double distance() { return distance; }
        public float alpha() { return alpha; }
        public float stellarBrightness() { return stellarBrightness; }
        public float systemInfluence() { return systemInfluence; }
        public float distanceScale() { return distanceScale; }
        public float projectedRadius() { return projectedRadius; }
        public float coronaDetail() { return coronaDetail; }
        public float effectDetail() { return effectDetail; }
        public StellarLod lod() { return lod; }
        public boolean navigationTarget() { return navigationTarget; }
        public float pointLodWeight() { return StellarLodTransitions.pointWeight(lodDetail); }
        public float simplifiedLodWeight() { return StellarLodTransitions.simplifiedWeight(lodDetail); }
        public float fullLodWeight() { return StellarLodTransitions.fullWeight(lodDetail); }
        public String visualStage()
        {
            if (effectDetail > 0.02F) return "FULL";
            if (coronaDetail > 0.02F) return "CORONA";
            return "POINT";
        }
    }
}
