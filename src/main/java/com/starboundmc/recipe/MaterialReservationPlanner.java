package com.starboundmc.recipe;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.BiFunction;
import java.util.function.BiPredicate;
import java.util.function.ObjIntConsumer;
import java.util.function.ToIntFunction;

/** Shared source-order reservation algorithm used by client simulation and server execution. */
public final class MaterialReservationPlanner {
    private MaterialReservationPlanner() {
    }

    /**
     * Mutates working-copy sources while satisfying each requirement in order.
     * Callers must discard the working copies when the result is empty.
     */
    public static <S, Q, R> Optional<List<R>> reserve(
            List<S> sources,
            List<Q> requirements,
            ToIntFunction<Q> requiredCount,
            BiPredicate<Q, S> matches,
            ToIntFunction<S> availableCount,
            ObjIntConsumer<S> consume,
            BiFunction<S, Integer, R> snapshot) {
        List<R> reserved = new ArrayList<>();
        for (Q requirement : requirements) {
            int stillNeeded = requiredCount.applyAsInt(requirement);
            for (S source : sources) {
                if (stillNeeded <= 0) {
                    break;
                }
                int available = availableCount.applyAsInt(source);
                if (available <= 0 || !matches.test(requirement, source)) {
                    continue;
                }
                int taken = Math.min(stillNeeded, available);
                reserved.add(snapshot.apply(source, taken));
                consume.accept(source, taken);
                stillNeeded -= taken;
            }
            if (stillNeeded > 0) {
                return Optional.empty();
            }
        }
        return Optional.of(List.copyOf(reserved));
    }
}
