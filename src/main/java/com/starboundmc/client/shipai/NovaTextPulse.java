package com.starboundmc.client.shipai;

/** Pure text-driven pulse curve kept independent from the LDLib2 client runtime. */
final class NovaTextPulse {
    private static final int STEPS = 12;
    private static final double TWO_PI = Math.PI * 2.0;

    private NovaTextPulse() {
    }

    static int nextStep(int currentStep) {
        return Math.floorMod(currentStep + 1, STEPS);
    }

    static float targetForStep(int step) {
        if (step < 0)
            return 0F;
        int normalizedStep = Math.floorMod(step, STEPS);
        double phase = (normalizedStep + 1.0) * TWO_PI / STEPS - TWO_PI * 0.25;
        return (float) (0.5 + Math.sin(phase) * 0.5);
    }
}
