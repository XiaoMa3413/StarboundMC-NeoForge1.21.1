// SPDX-License-Identifier: MPL-2.0
package com.starboundmc.client.hud.ar;

import java.util.function.Consumer;

@FunctionalInterface
public interface ArTargetProvider {
    void collect(ArContext context, Consumer<ArTarget> output);
}
