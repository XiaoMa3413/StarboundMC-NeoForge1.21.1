package com.starboundmc.client.starmap;

/** Pure action gating shared by button state and unavailable-status text. */
final class StarmapActionAvailability {
    enum Reason {
        NONE,
        NO_SELECTION,
        UNSUPPORTED_TARGET,
        SYSTEM_LOCKED,
        SYSTEM_UNREACHABLE,
        BODY_LOCKED,
        WARP_IN_PROGRESS,
        CURRENT_DESTINATION,
        SUBLIGHT_OFFLINE,
        HYPERDRIVE_OFFLINE,
        INSUFFICIENT_FUEL
    }

    record Result(boolean available, Reason reason, int requiredFuel, int availableFuel) {}

    private StarmapActionAvailability() {}

    static Result galaxy(boolean hasSelection, boolean unlocked, boolean reachable) {
        if (!hasSelection)
            return unavailable(Reason.NO_SELECTION);
        if (!unlocked)
            return unavailable(Reason.SYSTEM_LOCKED);
        if (!reachable)
            return unavailable(Reason.SYSTEM_UNREACHABLE);
        return available(0, 0);
    }

    static Result system(boolean hasSelection, boolean moon) {
        if (!hasSelection)
            return unavailable(Reason.NO_SELECTION);
        if (moon)
            return unavailable(Reason.UNSUPPORTED_TARGET);
        return available(0, 0);
    }

    static Result planet(boolean hasSelection, boolean reachable, boolean warping,
                         boolean currentDestination, int fuel, int cost) {
        // Compatibility overload used by the original map tests and callers.
        // It models a fully repaired propulsion system.
        return planet(hasSelection, reachable, warping, currentDestination,
                true, true, true, fuel, cost);
    }

    /**
     * Evaluates a planetary jump with the propulsion stage included. The
     * caller supplies whether the target belongs to the ship's current star
     * system; unknown topology is intentionally treated as a cross-system
     * route and therefore requires the stricter gate.
     */
    static Result planet(boolean hasSelection, boolean reachable, boolean warping,
                         boolean currentDestination, boolean sameSystem,
                         boolean sublightOnline, boolean hyperdriveOnline,
                         int fuel, int cost) {
        if (!hasSelection)
            return unavailable(Reason.NO_SELECTION);
        if (!reachable)
            return unavailable(Reason.BODY_LOCKED);
        if (warping)
            return unavailable(Reason.WARP_IN_PROGRESS);
        if (currentDestination)
            return unavailable(Reason.CURRENT_DESTINATION);
        if (!sublightOnline)
            return unavailable(Reason.SUBLIGHT_OFFLINE);
        if (!sameSystem && !hyperdriveOnline)
            return unavailable(Reason.HYPERDRIVE_OFFLINE);
        if (fuel < cost)
            return new Result(false, Reason.INSUFFICIENT_FUEL, cost, fuel);
        return available(cost, fuel);
    }

    private static Result available(int requiredFuel, int availableFuel) {
        return new Result(true, Reason.NONE, requiredFuel, availableFuel);
    }

    private static Result unavailable(Reason reason) {
        return new Result(false, reason, 0, 0);
    }
}
