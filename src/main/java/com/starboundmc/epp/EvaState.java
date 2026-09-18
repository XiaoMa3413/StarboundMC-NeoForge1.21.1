// SPDX-License-Identifier: MPL-2.0
package com.starboundmc.epp;

/** Transient per-player control state; never survives respawn or a new connection. */
public final class EvaState {
    public static final int NORMAL = 0, DRIFT = 1, THRUST = 2;
    public int mode;
    public int input;
    public int lastInputTick = Integer.MIN_VALUE;
    public int lastSentInput = -1;
    public net.minecraft.resources.ResourceLocation dimension;

    public int freshInput(int tick) {
        return (long) tick - lastInputTick <= 10 && tick >= lastInputTick ? input : 0;
    }
    public void clearInput() { input = 0; lastInputTick = Integer.MIN_VALUE; lastSentInput = -1; }
}
