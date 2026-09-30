package com.starboundmc.client;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.vertex.*;
import com.starboundmc.client.space.CelestialDepth;
import com.starboundmc.client.space.AtmosphereSolarOptics;
import net.minecraft.resources.ResourceLocation;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL13;
import org.lwjgl.opengl.GL15;
import org.lwjgl.opengl.GL20;
import org.lwjgl.opengl.GL21;
import org.lwjgl.opengl.GL30;
import org.lwjgl.system.MemoryUtil;
import java.util.Arrays;

/** Real shader/target checks, with draw order reversed as well as distance ordering. */
final class SpaceDepthSmoke {
    private SpaceDepthSmoke() {}

    static String verify() {
        StringBuilder report = new StringBuilder();
        verifySolarLutUpload(report);
        if (StarfieldClientConfig.SPACE_PIPELINE_MODE.get() != StarfieldClientConfig.PipelineMode.ISOLATED)
            return report.append("Depth integration checks skipped for direct pipeline.\n").toString();
        var state = SpaceRenderPassState.capture();
        String stateBefore = stateSignature();
        Matrix4f projection = new Matrix4f(RenderSystem.getProjectionMatrix());
        VertexSorting sorting = RenderSystem.getVertexSorting();
        int drawFramebuffer = GL11.glGetInteger(GL30.GL_DRAW_FRAMEBUFFER_BINDING);
        float originalMainDepth = readDepth();
        writeDepthSentinel(.37);
        float mainDepthBefore = readDepth();
        VertexBuffer sphere = buildSphere(), quad = buildQuad();
        try {
            RenderSystem.setProjectionMatrix(new Matrix4f().perspective((float) Math.toRadians(70), 1280F / 720, .05F, 1000),
                    VertexSorting.DISTANCE_TO_ORIGIN);
            for (boolean starNear : new boolean[] {false, true}) for (boolean starFirst : new boolean[] {false, true}) {
                if (!SpaceSceneTarget.begin()) throw new IllegalStateException("Isolated target did not activate");
                double starDistance = starNear ? 180 : 480, bodyDistance = starNear ? 480 : 180;
                if (starFirst) drawStar(quad, starDistance);
                drawPlanet(sphere, bodyDistance);
                if (!starFirst) drawStar(quad, starDistance);
                float actual = readDepth();
                double expected = starNear ? starDistance * 280 / 320 : bodyDistance * 210 / 280;
                double decoded = CelestialDepth.decode(actual);
                if (Math.abs(decoded - expected) > .05)
                    throw new IllegalStateException("Incorrect celestial depth: " + decoded + " expected " + expected);
                float[] color = new float[4];
                GL11.glReadPixels(width() / 2, height() / 2, 1, 1, GL11.GL_RGBA, GL11.GL_FLOAT, color);
                if (SpaceSceneTarget.linear()) {
                    float maximum = Math.max(color[0], Math.max(color[1], color[2]));
                    if (starNear != (maximum > 1))
                        throw new IllegalStateException("Wrong foreground radiance: " + maximum);
                }
                report.append("starNear=").append(starNear).append(" starFirst=").append(starFirst)
                        .append(" expectedDistance=").append(expected).append(" actualDistance=").append(decoded).append('\n');
                SpaceSceneTarget.finish();
            }
            if (AtmosphereShader.scattering() == null)
                throw new IllegalStateException("Scattering shader did not load");
            // This limb ray crosses only gas, without an opaque sphere on its path.
            for (boolean foreground : new boolean[] {true,false}) {
                if (!SpaceSceneTarget.begin()) throw new IllegalStateException("Isolated target did not activate");
                drawStar(quad,foreground ? 180 : 480);
                float depthBefore = readDepth();
                float[] before = readColor();
                SpaceSceneTarget.distanceScale(1F);
                // Pass one scale height above ground; the upper shell is now nearly vacuum.
                Matrix4f model = new Matrix4f().translate(63.85F,0,-280).scale(1.4F);
                AtmosphereShellRenderer.render(model,new Vector3f(1,0,0),
                        PlanetSurfaceLighting.cameraPositionMesh(model),new Vector3f(.3F,.6F,1),
                        .5F,1F,1.1F,0,0);
                float[] after = readColor();
                if (Float.compare(depthBefore,readDepth()) != 0)
                    throw new IllegalStateException("Atmosphere modified opaque celestial depth");
                if (foreground) {
                    for (int i=0;i<3;i++) if (Math.abs(before[i]-after[i]) > .001F)
                        throw new IllegalStateException("Atmosphere attenuated a foreground star");
                } else if (!(after[2] < before[2]-.01F))
                    throw new IllegalStateException("Atmosphere did not transmit/attenuate background radiance");
                else if (!(after[2]/before[2] < after[0]/before[0]))
                    throw new IllegalStateException("Atmosphere extinction did not preserve wavelength ordering");
                report.append("atmosphereForeground=").append(foreground)
                        .append(" before=").append(Arrays.toString(before))
                        .append(" after=").append(Arrays.toString(after)).append(" opaqueDepthPreserved\n");
                SpaceSceneTarget.finish();
            }
            verifyAtmosphereQuality(report);
            verifyBackgroundField(report);
            verifyCloudOptics(quad,report);
        } finally {
            SpaceSceneTarget.abort();
            sphere.close(); quad.close(); VertexBuffer.unbind();
            RenderSystem.setProjectionMatrix(projection, sorting);
            state.restore();
        }
        float mainDepthAfter = readDepth();
        writeDepthSentinel(originalMainDepth);
        state.restore();
        if (!stateBefore.equals(stateSignature()))
            throw new IllegalStateException("Render event state was not restored");
        if (drawFramebuffer != GL11.glGetInteger(GL30.GL_DRAW_FRAMEBUFFER_BINDING)
                || Float.compare(mainDepthBefore, mainDepthAfter) != 0)
            throw new IllegalStateException("World framebuffer/depth was modified by the astronomical target");
        report.append("World depth sentinel preserved: ").append(mainDepthAfter)
                .append("\nRender event state restored.\n");
        return report.toString();
    }

    private static void verifySolarLutUpload(StringBuilder report) {
        var shader = AtmosphereShader.scattering();
        if (shader == null) throw new IllegalStateException("Solar LUT shader did not load");
        var state = SpaceRenderPassState.capture();
        int[] unpackQueries = {GL11.GL_UNPACK_ALIGNMENT, GL11.GL_UNPACK_ROW_LENGTH,
                GL11.GL_UNPACK_SKIP_PIXELS, GL11.GL_UNPACK_SKIP_ROWS, GL11.GL_UNPACK_SWAP_BYTES};
        int[] packQueries = {GL11.GL_PACK_ALIGNMENT, GL11.GL_PACK_ROW_LENGTH,
                GL11.GL_PACK_SKIP_PIXELS, GL11.GL_PACK_SKIP_ROWS, GL11.GL_PACK_SWAP_BYTES};
        int[] unpackBefore = pixelStoreValues(unpackQueries), packBefore = pixelStoreValues(packQueries);
        int unpackBuffer = GL11.glGetInteger(GL21.GL_PIXEL_UNPACK_BUFFER_BINDING);
        int packBuffer = GL11.glGetInteger(GL21.GL_PIXEL_PACK_BUFFER_BINDING);
        int pbo = GL15.glGenBuffers();
        float[] expected = AtmosphereSolarOptics.generate();
        var actual = MemoryUtil.memAllocFloat(expected.length);
        try {
            // Readback is also a CPU transfer and must not inherit Minecraft's pack layout.
            GlStateManager._glBindBuffer(GL21.GL_PIXEL_PACK_BUFFER, 0);
            setPixelStore(packQueries, new int[] {4,0,0,0,0});
            GlStateManager._glBindBuffer(GL21.GL_PIXEL_UNPACK_BUFFER, 0);
            setPixelStore(unpackQueries, new int[] {4,0,0,0,0});
            try (var partial = new net.minecraft.client.renderer.texture.DynamicTexture(
                    new com.mojang.blaze3d.platform.NativeImage(1024,4,true))) {
                for (String condition : new String[] {"nativeImageSubregion", "foreignCpuLayout", "foreignUnpackPbo"}) {
                    partial.bind();
                    if (condition.equals("nativeImageSubregion")) {
                        // The same global state left by a real atlas subregion upload.
                        partial.getPixels().upload(0,0,0,3,2,5,1,false,false,false,false);
                    } else {
                        setPixelStore(unpackQueries, new int[] {8,2048,11,7,1});
                        if (condition.equals("foreignUnpackPbo")) {
                            GlStateManager._glBindBuffer(GL21.GL_PIXEL_UNPACK_BUFFER, pbo);
                            GL15.glBufferData(GL21.GL_PIXEL_UNPACK_BUFFER, 64L, GL15.GL_STREAM_DRAW);
                        }
                    }
                    int[] before = pixelStoreValues(unpackQueries);
                    int bufferBefore = GL11.glGetInteger(GL21.GL_PIXEL_UNPACK_BUFFER_BINDING);
                    int textureBefore = GL11.glGetInteger(GL11.GL_TEXTURE_BINDING_2D);
                    AtmosphereSolarLut.release();
                    AtmosphereSolarLut.bind(shader);
                    String uploaded = AtmosphereSolarLut.diagnostics();
                    AtmosphereSolarLut.bind(shader); // Cached use must neither upload nor modify GL state.
                    if (!uploaded.equals(AtmosphereSolarLut.diagnostics())
                            || !Arrays.equals(before, pixelStoreValues(unpackQueries))
                            || bufferBefore != GL11.glGetInteger(GL21.GL_PIXEL_UNPACK_BUFFER_BINDING)
                            || textureBefore != GL11.glGetInteger(GL11.GL_TEXTURE_BINDING_2D))
                        throw new IllegalStateException("Solar LUT changed caller transfer/texture state: "+condition);
                    shader.apply();
                    int sampler = GL20.glGetUniformLocation(shader.getId(), "SolarOpticalDepth");
                    if (sampler < 0) throw new IllegalStateException("Missing solar LUT sampler");
                    GlStateManager._activeTexture(GL13.GL_TEXTURE0 + GL20.glGetUniformi(shader.getId(), sampler));
                    if (GL11.glGetTexLevelParameteri(GL11.GL_TEXTURE_2D,0,GL11.GL_TEXTURE_WIDTH) != AtmosphereSolarOptics.WIDTH
                            || GL11.glGetTexLevelParameteri(GL11.GL_TEXTURE_2D,0,GL11.GL_TEXTURE_HEIGHT) != AtmosphereSolarOptics.HEIGHT
                            || GL11.glGetTexLevelParameteri(GL11.GL_TEXTURE_2D,0,GL11.GL_TEXTURE_INTERNAL_FORMAT) != GL30.GL_RG32F)
                        throw new IllegalStateException("Invalid solar LUT allocation: "+condition);
                    actual.clear();
                    GL11.glGetTexImage(GL11.GL_TEXTURE_2D,0,GL30.GL_RG,GL11.GL_FLOAT,actual);
                    for (int i=0;i<expected.length;i++) if (Float.compare(expected[i],actual.get(i)) != 0)
                        throw new IllegalStateException("Solar LUT transfer corrupted texel "+i+" under "+condition);
                    shader.clear();
                    int error = GL11.glGetError();
                    if (error != GL11.GL_NO_ERROR)
                        throw new IllegalStateException("Solar LUT OpenGL error "+error+" under "+condition);
                    report.append("solarLutUpload=").append(condition).append(" unpack=")
                            .append(Arrays.toString(before)).append(" pboBound=").append(bufferBefore != 0)
                            .append(" all65536FloatsExact/stateRestored/cachedBindStable/recreated\n");
                }
            }
        } finally {
            shader.clear();
            GlStateManager._glBindBuffer(GL21.GL_PIXEL_UNPACK_BUFFER, unpackBuffer);
            GlStateManager._glBindBuffer(GL21.GL_PIXEL_PACK_BUFFER, packBuffer);
            GL15.glDeleteBuffers(pbo);
            setPixelStore(unpackQueries, unpackBefore);
            setPixelStore(packQueries, packBefore);
            MemoryUtil.memFree(actual);
            state.restore();
        }
    }

    private static int[] pixelStoreValues(int[] queries) {
        return Arrays.stream(queries).map(GL11::glGetInteger).toArray();
    }

    private static void setPixelStore(int[] queries, int[] values) {
        for (int i=0;i<queries.length;i++) GlStateManager._pixelStore(queries[i],values[i]);
    }

    private static void verifyAtmosphereQuality(StringBuilder report) {
        var previous = StarfieldClientConfig.SPACE_VISUAL_QUALITY.get();
        var qualities = new SpaceVisualQuality[] {SpaceVisualQuality.ULTRA,SpaceVisualQuality.HIGH,
                SpaceVisualQuality.BALANCED,SpaceVisualQuality.PERFORMANCE};
        float[] reference = null;
        try {
            for (var quality : qualities) {
                StarfieldClientConfig.SPACE_VISUAL_QUALITY.set(quality);
                if (!SpaceSceneTarget.begin()) throw new IllegalStateException("Missing atmosphere quality target");
                SpaceSceneTarget.distanceScale(1F);
                Matrix4f model = new Matrix4f().translate(63.85F,0,-280).scale(1.4F);
                AtmosphereShellRenderer.render(model,new Vector3f(.16F,0,.98F).normalize(),
                        PlanetSurfaceLighting.cameraPositionMesh(model),new Vector3f(.3F,.6F,1),
                        .5F,1F,1.1F,0,0);
                float[] color = readColor();
                for (int i=0;i<3;i++) {
                    if (!Float.isFinite(color[i]) || color[i] < 0 || color[i] > 6.92F)
                        throw new IllegalStateException("Atmosphere violated finite radiance bounds: "+Arrays.toString(color));
                    if (reference != null) {
                        float tolerance = switch (quality) {
                            case HIGH -> .0008F+reference[i]*.08F;
                            case BALANCED -> .001F+reference[i]*.16F;
                            default -> .004F+reference[i]*.35F;
                        };
                        if (Math.abs(color[i]-reference[i]) > tolerance)
                            throw new IllegalStateException("Atmosphere quadrature failed convergence for "+quality
                                    +": "+Arrays.toString(color)+" vs "+Arrays.toString(reference));
                    }
                }
                if (reference == null) reference = color;
                report.append("atmosphereQuality=").append(quality).append(" grazingTwilight=")
                        .append(Arrays.toString(color)).append(" bounded/converged\n");
                SpaceSceneTarget.finish();
            }
        } finally { StarfieldClientConfig.SPACE_VISUAL_QUALITY.set(previous); }
    }

    private static void writeDepthSentinel(double depth) {
        RenderSystem.enableScissor(width() / 2, height() / 2, 1, 1);
        RenderSystem.depthMask(true); RenderSystem.clearDepth(depth);
        RenderSystem.clear(GL11.GL_DEPTH_BUFFER_BIT, false);
        RenderSystem.disableScissor();
    }

    private static void verifyBackgroundField(StringBuilder report) {
        var previous = StarfieldClientConfig.SPACE_VISUAL_QUALITY.get();
        try {
            StarfieldClientConfig.SPACE_VISUAL_QUALITY.set(SpaceVisualQuality.HIGH);
            Vector3f centre = new Vector3f(.87F,-.24F,0).normalize();
            Vector3f axis = new Vector3f(.24F,.87F,.43F).normalize();
            Vector3f tangent = new Vector3f(axis).cross(centre).normalize();
            Vector3f coreDirection = new Vector3f(centre).add(new Vector3f(axis).mul(.09F)).normalize();
            float[] core = backgroundMean(coreDirection,1);
            float[] outer = backgroundMean(new Vector3f(tangent).add(new Vector3f(axis).mul(.09F)).normalize(),1);
            float[] reduced = backgroundMean(coreDirection,2);
            float[] seamLeft = backgroundMean(new Vector3f(centre).negate().add(new Vector3f(tangent).mul(.00001F)),1);
            float[] seamRight = backgroundMean(new Vector3f(centre).negate().sub(new Vector3f(tangent).mul(.00001F)),1);
            float[] pole = backgroundMean(axis,1);
            for (int i=0;i<3;i++) {
                if (core[i] < outer[i]*1.8F)
                    throw new IllegalStateException("Galactic bulge has no stellar-density contrast");
                if (Math.abs(core[i]-reduced[i]) > core[i]*.08F+.0001F)
                    throw new IllegalStateException("Subpixel galaxy radiance changed under minification");
                if (Math.abs(seamLeft[i]-seamRight[i]) > seamLeft[i]*.03F+.00003F)
                    throw new IllegalStateException("Galactic longitude has a visible seam");
                if (pole[i] > .001F)
                    throw new IllegalStateException("Galactic pole did not preserve deep-space black level");
            }
            report.append("galaxy core=").append(Arrays.toString(core)).append(" outer=")
                    .append(Arrays.toString(outer)).append(" halfResolution=").append(Arrays.toString(reduced))
                    .append(" finite/contrast/filtered/seamContinuous/poleBlack/depthPreserved\n");
        } finally { StarfieldClientConfig.SPACE_VISUAL_QUALITY.set(previous); }
    }

    private static float[] backgroundMean(Vector3f direction, int divisor) {
        if (!SpaceSceneTarget.begin()) throw new IllegalStateException("Missing background field target");
        int w=width()/divisor,h=height()/divisor;
        RenderSystem.viewport(0,0,w,h);
        float depth = readDepth();
        Vector3f up = Math.abs(new Vector3f(direction).normalize().dot(new Vector3f(.24F,.87F,.43F).normalize())) > .95F
                ? new Vector3f(0,0,1) : new Vector3f(.24F,.87F,.43F);
        Matrix4f view = new Matrix4f().lookAt(new Vector3f(),direction,up);
        Matrix4f projection = new Matrix4f().perspective((float)Math.toRadians(70),(float)w/h,.05F,1000F);
        if (!GpuSpaceBackground.renderBackground(view,projection,
                com.starboundmc.client.space.StarSystemResolver.latestEnvironment()))
            throw new IllegalStateException("Background field shader failed to draw");
        int side=64/divisor;
        float[] pixels=new float[side*side*4],mean=new float[3];
        GL11.glReadPixels(w/2-side/2,h/2-side/2,side,side,GL11.GL_RGBA,GL11.GL_FLOAT,pixels);
        for (int p=0;p<pixels.length;p+=4) for (int i=0;i<3;i++) {
            if (!Float.isFinite(pixels[p+i]) || pixels[p+i] < 0 || pixels[p+i] > 2F)
                throw new IllegalStateException("Galaxy produced unbounded radiance");
            mean[i]+=pixels[p+i]/(side*side);
        }
        if (Float.compare(depth,readDepth()) != 0)
            throw new IllegalStateException("Background field modified celestial depth");
        SpaceSceneTarget.finish();
        return mean;
    }

    private static void verifyCloudOptics(VertexBuffer star, StringBuilder report) {
        if (CloudShader.current() == null) throw new IllegalStateException("Cloud shader did not load");
        var textures = net.minecraft.client.Minecraft.getInstance().getTextureManager();
        var id = ResourceLocation.fromNamespaceAndPath("starboundmc","render_test_half_cloud");
        var image = new com.mojang.blaze3d.platform.NativeImage(1,1,false);
        image.setPixelRGBA(0,0,0x80FFFFFF);
        textures.register(id,new net.minecraft.client.renderer.texture.DynamicTexture(image));
        var profile = StarmapUniverse.body("sys1:lush").spaceVisual().orElseThrow();
        float[] alpha = new float[2];
        float[] cloudContribution = null;
        try {
            for (int i=0;i<2;i++) {
                if (!SpaceSceneTarget.begin()) throw new IllegalStateException("Missing cloud optical target");
                float depth=readDepth();
                CloudShellRenderer.render(new Matrix4f().translate(i==0 ? 0F : 63F,0,-280).scale(1.4F),
                        id.toString(),new Vector3f(0,0,1),1F,1F,profile);
                float[] color=readColor();
                if (i==0) cloudContribution=color;
                alpha[i]=color[3];
                for (float value : color) if (!Float.isFinite(value) || value < 0 || value > 7F)
                    throw new IllegalStateException("Cloud produced nonfinite/unbounded radiance");
                if (Float.compare(depth,readDepth()) != 0)
                    throw new IllegalStateException("Cloud modified opaque celestial depth");
                SpaceSceneTarget.finish();
            }
            if (Math.abs(alpha[0]-.5F) > .015F || alpha[1] < alpha[0]+.1F || alpha[1] > 1F)
                throw new IllegalStateException("Cloud grazing path did not preserve normal coverage: "+Arrays.toString(alpha));
            for (boolean foreground : new boolean[] {true,false}) {
                if (!SpaceSceneTarget.begin()) throw new IllegalStateException("Missing cloud occlusion target");
                drawStar(star,foreground ? 180 : 480);
                float depth=readDepth();
                float[] before=readColor();
                CloudShellRenderer.render(new Matrix4f().translate(0,0,-280).scale(1.4F),
                        id.toString(),new Vector3f(0,0,1),1F,1F,profile);
                float[] after=readColor();
                if (Float.compare(depth,readDepth()) != 0)
                    throw new IllegalStateException("Cloud replaced star depth");
                if (foreground) {
                    for (int i=0;i<3;i++) if (Math.abs(before[i]-after[i]) > .001F)
                        throw new IllegalStateException("Cloud attenuated a foreground star");
                } else if (after[0] >= before[0]-.1F)
                    throw new IllegalStateException("Cloud did not attenuate a background star");
                else for (int i=0;i<3;i++) {
                    float expected=before[i]*(1F-alpha[0])+cloudContribution[i];
                    if (Math.abs(after[i]-expected) > .01F)
                        throw new IllegalStateException("Cloud compositing did not preserve linear transmission");
                }
                SpaceSceneTarget.finish();
            }
            report.append("cloud normalAlpha=").append(alpha[0]).append(" grazingAlpha=").append(alpha[1])
                    .append(" finite/slantPath/linearComposite/foregroundPreserved/backgroundAttenuated/depthPreserved\n");
        } finally { textures.release(id); }
    }

    private static String stateSignature() {
        StringBuilder result = new StringBuilder();
        for (int query : new int[] { GL11.GL_DEPTH_FUNC, GL11.GL_DEPTH_WRITEMASK,
                org.lwjgl.opengl.GL14.GL_BLEND_SRC_RGB, org.lwjgl.opengl.GL14.GL_BLEND_DST_RGB,
                org.lwjgl.opengl.GL14.GL_BLEND_SRC_ALPHA, org.lwjgl.opengl.GL14.GL_BLEND_DST_ALPHA,
                org.lwjgl.opengl.GL20.GL_BLEND_EQUATION_RGB, org.lwjgl.opengl.GL20.GL_BLEND_EQUATION_ALPHA,
                GL30.GL_DRAW_FRAMEBUFFER_BINDING, GL30.GL_READ_FRAMEBUFFER_BINDING,
                org.lwjgl.opengl.GL20.GL_CURRENT_PROGRAM, GL30.GL_VERTEX_ARRAY_BINDING,
                org.lwjgl.opengl.GL15.GL_ARRAY_BUFFER_BINDING, org.lwjgl.opengl.GL13.GL_ACTIVE_TEXTURE })
            result.append(GL11.glGetInteger(query)).append(',');
        for (int flag : new int[] { GL11.GL_DEPTH_TEST, GL11.GL_BLEND, GL11.GL_CULL_FACE,
                GL11.GL_SCISSOR_TEST, GL30.GL_FRAMEBUFFER_SRGB }) result.append(GL11.glIsEnabled(flag));
        for (int query : new int[] {GL11.GL_VIEWPORT, GL11.GL_SCISSOR_BOX, GL11.GL_COLOR_WRITEMASK}) {
            int[] values = new int[4]; GL11.glGetIntegerv(query, values); result.append(Arrays.toString(values));
        }
        result.append(Arrays.toString(RenderSystem.getShaderColor()))
                .append(RenderSystem.getShaderFogStart()).append(RenderSystem.getShaderFogEnd())
                .append(RenderSystem.getShaderFogShape()).append(Arrays.toString(RenderSystem.getShaderFogColor()));
        return result.toString();
    }

    private static int width() { int[] viewport = new int[4]; GL11.glGetIntegerv(GL11.GL_VIEWPORT, viewport); return viewport[2]; }
    private static int height() { int[] viewport = new int[4]; GL11.glGetIntegerv(GL11.GL_VIEWPORT, viewport); return viewport[3]; }
    private static float readDepth() {
        float[] value = new float[1];
        GL11.glReadPixels(width() / 2, height() / 2, 1, 1, GL11.GL_DEPTH_COMPONENT, GL11.GL_FLOAT, value);
        return value[0];
    }

    private static float[] readColor() {
        float[] value = new float[4];
        GL11.glReadPixels(width()/2,height()/2,1,1,GL11.GL_RGBA,GL11.GL_FLOAT,value);
        return value;
    }

    private static void drawStar(VertexBuffer quad, double distance) {
        var shader = SpaceStellarRenderer.current();
        RenderSystem.disableBlend(); RenderSystem.disableCull(); RenderSystem.enableDepthTest();
        RenderSystem.depthMask(true); RenderSystem.depthFunc(GL11.GL_LEQUAL);
        RenderSystem.setShader(() -> shader);
        shader.safeGetUniform("CenterView").set(0F, 0F, -320F);
        shader.safeGetUniform("SphereRadius").set(40F);
        shader.safeGetUniform("QuadRadius").set(42F);
        shader.safeGetUniform("DistanceScale").set((float) (distance / 320));
        shader.safeGetUniform("CoronaPass").set(0F);
        shader.safeGetUniform("Brightness").set(1F);
        shader.safeGetUniform("Detail").set(0F);
        shader.safeGetUniform("SurfaceColor").set(1F, .9F, .7F);
        shader.safeGetUniform("LinearColor").set(SpaceSceneTarget.linear() ? 1F : 0F);
        shader.safeGetUniform("ViewToWorld").set(new Matrix4f());
        quad.bind(); quad.drawWithShader(new Matrix4f(), RenderSystem.getProjectionMatrix(), shader);
        VertexBuffer.unbind();
    }

    private static void drawPlanet(VertexBuffer sphere, double distance) {
        var shader = PlanetSurfaceShader.current();
        Matrix4f model = new Matrix4f().translate(0, 0, -280).scale(1.4F);
        var profile = StarmapUniverse.body("sys1:barren").spaceVisual().orElseThrow();
        AtmosphereShader.setSurfaceLighting(shader,profile,1F);
        RenderSystem.disableBlend(); RenderSystem.disableCull(); RenderSystem.enableDepthTest();
        RenderSystem.depthMask(true); RenderSystem.depthFunc(GL11.GL_LEQUAL);
        RenderSystem.setShader(() -> shader);
        RenderSystem.setShaderTexture(0, ResourceLocation.parse(profile.texture().orElseThrow()));
        SpaceSceneTarget.distanceScale((float) (distance / 280));
        SpaceSceneTarget.configure(shader);
        PlanetSurfaceShader.setLighting(shader, new Vector3f(0,0,1), PlanetSurfaceLighting.cameraPositionMesh(model),
                .003F, .1F, 0, 1, 0, false, 0, 1, .1F);
        shader.safeGetUniform("CloudShadowEnabled").set(0F);
        sphere.bind(); sphere.drawWithShader(model, RenderSystem.getProjectionMatrix(), shader);
        VertexBuffer.unbind();
    }

    private static VertexBuffer buildQuad() {
        BufferBuilder builder = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION);
        builder.addVertex(-1,-1,0); builder.addVertex(1,-1,0); builder.addVertex(1,1,0); builder.addVertex(-1,1,0);
        return upload(builder);
    }

    private static VertexBuffer buildSphere() {
        BufferBuilder builder = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, PlanetRenderer.PLANET_SURFACE_FORMAT);
        for (int lat = 0; lat < 32; lat++) for (int lon = 0; lon < 64; lon++) {
            vertex(builder, lat, lon); vertex(builder, lat, lon + 1);
            vertex(builder, lat + 1, lon + 1); vertex(builder, lat + 1, lon);
        }
        return upload(builder);
    }

    private static void vertex(BufferBuilder builder, int lat, int lon) {
        double phi = Math.PI * lat / 32, theta = Math.PI * 2 * lon / 64;
        float x = (float) (Math.sin(phi) * Math.cos(theta)), y = (float) Math.cos(phi),
                z = (float) (Math.sin(phi) * Math.sin(theta));
        builder.addVertex(x * 50, y * 50, z * 50).setUv(lon / 64F, lat / 32F).setNormal(x,y,z);
    }

    private static VertexBuffer upload(BufferBuilder builder) {
        VertexBuffer buffer = new VertexBuffer(VertexBuffer.Usage.STATIC);
        buffer.bind(); buffer.upload(builder.buildOrThrow()); VertexBuffer.unbind();
        return buffer;
    }
}
