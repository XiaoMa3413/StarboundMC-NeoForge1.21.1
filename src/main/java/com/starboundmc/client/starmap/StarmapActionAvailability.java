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
        if (!hasSelection)
            return unavailable(Reason.NO_SELECTION);
        if (!reachable)
            return unavailable(Reason.BODY_LOCKED);
        if (warping)
            return unavailable(Reason.WARP_IN_PROGRESS);
        if (currentDestination)
            return unavailable(Reason.CURRENT_DESTINATION);
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
