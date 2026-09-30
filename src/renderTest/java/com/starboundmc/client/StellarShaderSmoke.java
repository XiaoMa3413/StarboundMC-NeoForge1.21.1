package com.starboundmc.client;

import com.starboundmc.client.compat.stellarview.StellarViewStarfield;
import net.povstalec.stellarview.client.render.shader.StellarViewShaders;
import net.povstalec.stellarview.common.config.GeneralConfig;

/** Only loaded by the opt-in matrix with the customized optional mod installed. */
final class StellarShaderSmoke {
    private static boolean originalTextured, originalInstanced, changed;
    private static int originalLoadBudget;

    static void configure(boolean textured, boolean instanced) {
        if (!changed) {
            originalTextured = GeneralConfig.textured_stars.get();
            originalInstanced = GeneralConfig.instancing.get();
            changed = true;
            try {
                originalLoadBudget = (int) net.povstalec.stellarview.client.render.SpaceRenderer.class
                        .getMethod("beginExternalStarLoading").invoke(null);
                // Every matrix mode must upload even when the ordinary sky's budget is exhausted.
                net.povstalec.stellarview.client.render.SpaceRenderer.loadedStars(100001);
            } catch (ReflectiveOperationException failure) { throw new IllegalStateException(failure); }
        }
        // Change only this fixture's live values, without saving a config file.
        GeneralConfig.textured_stars.boolean_value.set(textured);
        GeneralConfig.instancing.boolean_value.set(instanced);
        StellarViewStarfield.resetSession();
    }

    static void verifyRestoration() {
        if (changed && net.povstalec.stellarview.client.render.SpaceRenderer.loadNewStars())
            throw new IllegalStateException("External rendering changed the ordinary sky's exhausted upload budget");
        for (var shader : new net.minecraft.client.renderer.ShaderInstance[] {
                StellarViewShaders.starShader(), StellarViewShaders.instancedStarShader(),
                StellarViewShaders.starTexShader(), StellarViewShaders.instancedStarTexShader()}) {
            if (shader == null || shader.getUniform("ExternalRadiance") == null)
                throw new IllegalStateException("Stellar HDR matrix requires all four customized shaders");
            if (shader.getUniform("ExternalRadiance").getFloatBuffer().get(0) != 0F)
                throw new IllegalStateException("External HDR mode leaked into ordinary skies");
            var viewport = shader.getUniform("ExternalViewport").getFloatBuffer();
            if (viewport.get(0) != 1F || viewport.get(1) != 1F)
                throw new IllegalStateException("External viewport uniforms were not restored");
        }
    }

    static void restore() {
        if (!changed) return;
        GeneralConfig.textured_stars.boolean_value.set(originalTextured);
        GeneralConfig.instancing.boolean_value.set(originalInstanced);
        StellarViewStarfield.resetSession();
        try {
            net.povstalec.stellarview.client.render.SpaceRenderer.class
                    .getMethod("endExternalStarLoading",int.class).invoke(null,originalLoadBudget);
        } catch (ReflectiveOperationException failure) { throw new IllegalStateException(failure); }
        changed = false;
    }
}
