package com.starboundmc.client.fuel;

import java.util.function.IntUnaryOperator;

/** Read-only preview of the menu's ordered whole-item refill transaction. */
public final class FuelPreview {
    private FuelPreview() {}

    public static int accepted(int fuel, int capacity, int slots,
                               IntUnaryOperator valueAt, IntUnaryOperator countAt) {
        int remaining = Math.max(0, capacity - Math.clamp(fuel, 0, Math.max(0, capacity)));
        int available = remaining;
        for (int i = 0; i < slots; i++) {
            int value = valueAt.applyAsInt(i), count = countAt.applyAsInt(i);
            if (count <= 0 || value <= 0) continue;
            int used = Math.min(count, remaining / value);
            remaining -= used * value;
            // The server stops at the first item that cannot fit, even if later items are smaller.
            if (used < count) break;
        }
        return available - remaining;
    }
}
