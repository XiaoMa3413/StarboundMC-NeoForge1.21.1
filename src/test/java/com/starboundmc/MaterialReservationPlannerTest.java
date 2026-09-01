package com.starboundmc;

import com.starboundmc.recipe.MaterialReservationPlanner;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MaterialReservationPlannerTest {
    @Test
    void reservesAcrossMachineAndBackpackStacksWhileIgnoringUnrelatedItems() {
        List<Requirement> requirements = List.of(new Requirement("iron", 3));
        List<Stack> sources = new ArrayList<>(List.of(
                new Stack("dirt", 64),
                new Stack("iron", 2),
                new Stack("iron", 4)));

        List<Reserved> reserved = reserve(sources, requirements).orElseThrow();

        assertEquals(64, sources.get(0).count);
        assertEquals(0, sources.get(1).count);
        assertEquals(3, sources.get(2).count);
        assertEquals(3, reserved.stream().mapToInt(Reserved::count).sum());
    }

    @Test
    void repeatedReservationsUseBothSourceGroupsAndStopAtTheCombinedLimit() {
        List<Requirement> requirements = List.of(
                new Requirement("iron", 2),
                new Requirement("lapis", 1));
        List<Stack> sources = new ArrayList<>(List.of(
                new Stack("iron", 1),
                new Stack("lapis", 1),
                new Stack("iron", 3),
                new Stack("lapis", 1)));

        assertTrue(reserve(sources, requirements).isPresent());
        assertTrue(reserve(sources, requirements).isPresent());
        assertTrue(reserve(sources, requirements).isEmpty());
    }

    private static java.util.Optional<List<Reserved>> reserve(
            List<Stack> sources, List<Requirement> requirements) {
        return MaterialReservationPlanner.reserve(
                sources,
                requirements,
                Requirement::count,
                (requirement, stack) -> requirement.material.equals(stack.material),
                stack -> stack.count,
                (stack, amount) -> stack.count -= amount,
                (stack, amount) -> new Reserved(stack.material, amount));
    }

    private record Requirement(String material, int count) {
    }

    private record Reserved(String material, int count) {
    }

    private static final class Stack {
        private final String material;
        private int count;

        private Stack(String material, int count) {
            this.material = material;
            this.count = count;
        }
    }
}
