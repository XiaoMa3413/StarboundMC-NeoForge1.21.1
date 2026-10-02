package com.starboundmc.client;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import com.mojang.logging.LogUtils;
import com.starboundmc.StarboundMC;
import com.starboundmc.client.space.GalaxyEnvironmentBlend;
import com.starboundmc.client.space.SchwarzschildLensTable;
import com.starboundmc.client.space.SchwarzschildRay;
import com.starboundmc.client.space.SpaceRenderContext;
import com.starboundmc.client.space.StellarBackground;
import net.minecraft.client.renderer.FogRenderer;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.client.event.RegisterShadersEvent;
import org.joml.Matrix4f;
import org.joml.Vector3f;

/** On-demand background queries and opt-in static Schwarzschild research preview. */
final class GpuSkyQuery {
    enum Mode { DISABLED, QUERY, SCHWARZSCHILD }
    static Mode mode=Boolean.getBoolean("starboundmc.debug.schwarzschild") ? Mode.SCHWARZSCHILD
            : Boolean.getBoolean("starboundmc.debug.backgroundQuery") ? Mode.QUERY : Mode.DISABLED;
    // Fixed celestial direction and static observer; this is not a new universe/gameplay body.
    static final Vector3f LENS_DIRECTION=new Vector3f(.87F,-.24F,0).normalize();
    private static final String[] SAMPLERS={"SkyCells","SkyIndices","SkySources","SkyFlux","LensAzimuth"};
    private static ShaderInstance shader;
    private static VertexBuffer triangle;
    private static StellarBackground catalog;
    private static SchwarzschildLensTable lens;
    private static final int[] textures=new int[5];
    private static long uploads,draws,reloads;
    private static boolean failed;
    private static final float SHADOW=(float)SchwarzschildRay.shadowAngle(SchwarzschildLensTable.OBSERVER_RADIUS);
    private static final float LOG_RANGE=(float)Math.log((Math.PI/2-SchwarzschildRay.shadowAngle(SchwarzschildLensTable.OBSERVER_RADIUS))
            /SchwarzschildLensTable.MIN_EDGE_ANGLE);
    private GpuSkyQuery() {}

    static void register(RegisterShadersEvent event) {
        reloads++;
        release();shader=null;failed=false;
        try {
            event.registerShader(new ShaderInstance(event.getResourceProvider(),
                    ResourceLocation.fromNamespaceAndPath(StarboundMC.MODID,"space_query_background"),
                    DefaultVertexFormat.POSITION),s->shader=s);
        } catch(java.io.IOException|RuntimeException error) {
            LogUtils.getLogger().warn("Background query research shader unavailable",error);
        }
    }

    static boolean render(Matrix4f skyModelView,Matrix4f projection,SpaceRenderContext space,
                          GalaxyEnvironmentBlend environment) {
        if(mode==Mode.DISABLED || shader==null || failed) return false;
        var previous=RenderSystem.getShader();
        try {
            bind(shader,mode==Mode.SCHWARZSCHILD);
            if(triangle==null) {
                var builder=Tesselator.getInstance().begin(VertexFormat.Mode.TRIANGLES,DefaultVertexFormat.POSITION);
                builder.addVertex(-1,-1,0);builder.addVertex(3,-1,0);builder.addVertex(-1,3,0);
                triangle=new VertexBuffer(VertexBuffer.Usage.STATIC);
                triangle.bind();triangle.upload(builder.buildOrThrow());VertexBuffer.unbind();
            }
            FogRenderer.setupNoFog();RenderSystem.disableDepthTest();RenderSystem.depthMask(false);
            RenderSystem.disableCull();RenderSystem.disableBlend();RenderSystem.setShader(()->shader);
            Matrix4f modelView=new Matrix4f(skyModelView).rotateX((float)Math.toRadians(-space.pitch()))
                    .rotateY((float)Math.toRadians(-space.yaw()));
            shader.safeGetUniform("InverseViewProjection").set(new Matrix4f(projection).mul(modelView).invert());
            shader.safeGetUniform("LinearColor").set(SpaceSceneTarget.linear()?1F:0F);
            shader.safeGetUniform("FieldDetail").set(StarfieldClientConfig.SPACE_VISUAL_QUALITY.get()==SpaceVisualQuality.PERFORMANCE?0F:1F);
            int tint=environment.skyTintColor();
            shader.safeGetUniform("TintColor").set(((tint>>16)&255)/255F,((tint>>8)&255)/255F,(tint&255)/255F);
            shader.safeGetUniform("TintAmount").set(environment.skyTintAmount());
            shader.safeGetUniform("LensEnabled").set(mode==Mode.SCHWARZSCHILD?1F:0F);
            shader.safeGetUniform("LensDirection").set(LENS_DIRECTION.x,LENS_DIRECTION.y,LENS_DIRECTION.z);
            shader.safeGetUniform("TestMode").set(0F);
            triangle.bind();triangle.drawWithShader(new Matrix4f(),new Matrix4f(),shader);draws++;
            return true;
        } catch(RuntimeException error) {
            failed=true;LogUtils.getLogger().warn("Background query preview failed; retaining native sky until reload",error);
            return false;
        } finally {
            VertexBuffer.unbind();RenderSystem.setShader(()->previous);SpaceRenderPassState.restoreDefaults();
        }
    }

    /** All five samplers are stock 2D textures, compatible with Minecraft ShaderInstance. */
    static void bind(ShaderInstance target,boolean withLens) {
        int count=StarfieldClientConfig.SPACE_VISUAL_QUALITY.get().backgroundStarBudget();
        if(catalog==null || catalog.count()!=count) {
            releaseCatalog();catalog=new StellarBackground(count);
        }
        if(textures[0]==0) {
            try {
                textures[0]=SpaceFloatTexture.upload(StellarBackground.GRID,6*StellarBackground.GRID,2,catalog.cellData());
                textures[1]=SpaceFloatTexture.upload(StellarBackground.DATA_WIDTH,StellarBackground.height(catalog.memberships()),1,catalog.indices());
                textures[2]=SpaceFloatTexture.upload(StellarBackground.DATA_WIDTH,StellarBackground.height(count*2),4,catalog.sourceData());
                textures[3]=SpaceFloatTexture.upload(StellarBackground.FLUX_SIZE,StellarBackground.FLUX_HEIGHT,4,catalog.fluxData());
                uploads+=4;
            } catch(RuntimeException error) { releaseCatalog();throw error; }
        }
        if(withLens && textures[4]==0) {
            if(lens==null)lens=new SchwarzschildLensTable(SchwarzschildLensTable.OBSERVER_RADIUS);
            textures[4]=SpaceFloatTexture.upload(SchwarzschildLensTable.WIDTH,2,1,lens.data());uploads++;
        }
        for(int i=0;i<4;i++)target.setSampler(SAMPLERS[i],textures[i]);
        // The lens sampler must have a complete binding even when its branch is disabled.
        target.setSampler(SAMPLERS[4],textures[4]!=0?textures[4]:textures[1]);
        target.safeGetUniform("LensShadow").set(SHADOW);
        target.safeGetUniform("LensLogRange").set(LOG_RANGE);
    }

    static ShaderInstance shader() { return shader; }
    static long reloads() { return reloads; }
    static boolean active() { return shader!=null && !failed && catalog!=null && (mode!=Mode.SCHWARZSCHILD || textures[4]!=0); }
    static StellarBackground catalog() { return catalog; }
    static int[] textureIds() { return textures.clone(); }
    private static void releaseCatalog() {
        for(int i=0;i<4;i++){if(textures[i]!=0)GlStateManager._deleteTexture(textures[i]);textures[i]=0;}
        catalog=null;
    }
    static void release() {
        releaseCatalog();if(textures[4]!=0)GlStateManager._deleteTexture(textures[4]);textures[4]=0;
        if(triangle!=null)triangle.close();triangle=null;
    }
    static String diagnostics() {
        return "queryMode="+mode+" queryShader="+(shader!=null)+" queryFailed="+failed+" queryReloads="+reloads+" queryUploads="+uploads
                +" queryDraws="+draws+" querySources="+(catalog==null?0:catalog.count())
                +" queryMaxCandidates="+(catalog==null?0:catalog.maximumCandidates())
                +" queryGpuBytes="+(catalog==null?0:catalog.gpuBytes())+" lensTexture="+(textures[4]!=0);
    }
}
