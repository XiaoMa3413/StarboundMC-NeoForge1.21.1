package com.starboundmc.client;

import com.starboundmc.client.compat.stellarview.StellarViewStarfield;
import net.minecraft.client.renderer.ShaderInstance;
import net.neoforged.neoforge.common.ModConfigSpec;

/** Optional runtime matrix; the fixture itself compiles without the external mod. */
final class StellarShaderSmoke {
    private static boolean originalTextured,originalInstanced,changed;
    private static int originalLoadBudget;
    private static ModConfigSpec.BooleanValue setting(String name) throws ReflectiveOperationException {
        Object wrapper=Class.forName("net.povstalec.stellarview.common.config.GeneralConfig").getField(name).get(null);
        return (ModConfigSpec.BooleanValue)wrapper.getClass().getField("boolean_value").get(wrapper);
    }
    private static Class<?> renderer() throws ClassNotFoundException {
        return Class.forName("net.povstalec.stellarview.client.render.SpaceRenderer");
    }
    static void configure(boolean textured,boolean instanced) {
        try {
            if (!changed) {
                originalTextured=setting("textured_stars").get();
                originalInstanced=setting("instancing").get();
                originalLoadBudget=(int)renderer().getMethod("beginExternalStarLoading").invoke(null);
                renderer().getMethod("loadedStars",int.class).invoke(null,100001);
                changed=true;
            }
            setting("textured_stars").set(textured);
            setting("instancing").set(instanced);
            StellarViewStarfield.resetSession();
        } catch (ReflectiveOperationException failure) { throw new IllegalStateException("Stellar matrix API missing",failure); }
    }
    static void verifyRestoration() {
        try {
            if (changed && (boolean)renderer().getMethod("loadNewStars").invoke(null))
                throw new IllegalStateException("External rendering changed the ordinary sky upload budget");
            Class<?> shaders=Class.forName("net.povstalec.stellarview.client.render.shader.StellarViewShaders");
            for (String method:new String[] {"starShader","instancedStarShader","starTexShader","instancedStarTexShader"}) {
                ShaderInstance shader=(ShaderInstance)shaders.getMethod(method).invoke(null);
                if (shader == null || shader.getUniform("ExternalRadiance") == null)
                    throw new IllegalStateException("Stellar matrix requires customized HDR shaders");
                if (shader.getUniform("ExternalRadiance").getFloatBuffer().get(0) != 0F)
                    throw new IllegalStateException("External HDR mode leaked into ordinary skies");
                var viewport=shader.getUniform("ExternalViewport").getFloatBuffer();
                if (viewport.get(0) != 1F || viewport.get(1) != 1F)
                    throw new IllegalStateException("External viewport was not restored");
            }
        } catch (ReflectiveOperationException failure) { throw new IllegalStateException(failure); }
    }
    static void restore() {
        if (!changed) return;
        try {
            setting("textured_stars").set(originalTextured);
            setting("instancing").set(originalInstanced);
            StellarViewStarfield.resetSession();
            renderer().getMethod("endExternalStarLoading",int.class).invoke(null,originalLoadBudget);
            changed=false;
        } catch (ReflectiveOperationException failure) { throw new IllegalStateException(failure); }
    }
}
