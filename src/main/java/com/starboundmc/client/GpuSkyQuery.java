package com.starboundmc.client;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import com.mojang.logging.LogUtils;
import com.starboundmc.StarboundMC;
import com.starboundmc.client.space.GalaxyEnvironmentBlend;
import com.starboundmc.client.space.SchwarzschildLensTable;
import com.starboundmc.client.space.SchwarzschildRay;
import com.starboundmc.client.space.SchwarzschildDiscTable;
import com.starboundmc.client.space.SchwarzschildVolume;
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
    enum Mode { DISABLED, QUERY, SCHWARZSCHILD, DISC }
    static Mode mode=Boolean.getBoolean("starboundmc.debug.accretionDisc")
            || Boolean.getBoolean("starboundmc.debug.accretionDiscVolume") ? Mode.DISC
            : Boolean.getBoolean("starboundmc.debug.schwarzschild") ? Mode.SCHWARZSCHILD
            : Boolean.getBoolean("starboundmc.debug.backgroundQuery") ? Mode.QUERY : Mode.DISABLED;
    // Fixed celestial direction and static observer; this is not a new universe/gameplay body.
    static final Vector3f LENS_DIRECTION=new Vector3f(.87F,-.24F,0).normalize();
    private static final String[] SAMPLERS={"SkyCells","SkyIndices","SkySources","SkyFlux","LensAzimuth"};
    private static ShaderInstance shader;
    private static VertexBuffer triangle;
    private static StellarBackground catalog;
    private static SchwarzschildLensTable lens;
    private static SchwarzschildDiscTable discTable;
    private static int discTexture;
    static float discInclination=86.56F,discTimeOverride=Float.NaN,discObserverRadius=20F;
    static float lastDiscTime;
    static boolean discDiagnostic,discPhysical,discWideGlare=true;
    static boolean discFiniteBeam=true;
    static boolean discVolume=Boolean.getBoolean("starboundmc.debug.accretionDiscVolume");
    static float discThickness=(float)SchwarzschildVolume.ASPECT;
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
            if(mode==Mode.DISC)bindDisc(shader);
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
            shader.safeGetUniform("LensEnabled").set(mode==Mode.SCHWARZSCHILD || mode==Mode.DISC?1F:0F);
            shader.safeGetUniform("DiscEnabled").set(mode==Mode.DISC?1F:0F);
            shader.safeGetUniform("DiscDiagnostic").set(discDiagnostic?1F:0F);
            shader.safeGetUniform("DiscPhysical").set(discPhysical?1F:0F);
            shader.safeGetUniform("DiscBeamGrid").set((float)discBeamGrid());
            shader.safeGetUniform("DiscVolumeEnabled").set(discVolume?1F:0F);
            shader.safeGetUniform("DiscThickness").set(discThickness);
            shader.safeGetUniform("DiscVolumeSteps").set((float)discVolumeSteps());
            lastDiscTime=Float.isFinite(discTimeOverride)?discTimeOverride:space.animationTicks()*.3F;
            shader.safeGetUniform("DiscTime").set(lastDiscTime);
            Vector3f radial=new Vector3f(LENS_DIRECTION).negate(),up=new Vector3f(.24F,.87F,0).normalize();
            float inclination=(float)Math.toRadians(discInclination);
            Vector3f normal=new Vector3f(up).mul((float)Math.sin(inclination)).fma((float)Math.cos(inclination),radial).normalize();
            Vector3f u=new Vector3f(radial).fma(-radial.dot(normal),normal);
            if(u.lengthSquared()<1e-8)u.set(0,0,1);else u.normalize();
            Vector3f v=new Vector3f(normal).cross(u).normalize();
            shader.safeGetUniform("DiscNormal").set(normal.x,normal.y,normal.z);
            shader.safeGetUniform("DiscU").set(u.x,u.y,u.z);shader.safeGetUniform("DiscV").set(v.x,v.y,v.z);
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
        target.safeGetUniform("DiscEnabled").set(0F);
    }

    static void bindDisc(ShaderInstance target) {
        if(discTable!=null && discTable.radius()!=discObserverRadius) {
            if(discTexture!=0)GlStateManager._deleteTexture(discTexture);
            discTexture=0;discTable=null;
        }
        if(discTable==null)discTable=new SchwarzschildDiscTable(discObserverRadius);
        if(discTexture==0) {
            discTexture=SpaceFloatTexture.upload(SchwarzschildDiscTable.WIDTH,SchwarzschildDiscTable.HEIGHT,4,discTable.data());uploads++;
        }
        double shadow=SchwarzschildRay.shadowAngle(discObserverRadius);
        target.setSampler("LensAzimuth",discTexture);target.safeGetUniform("DiscEnabled").set(1F);
        target.safeGetUniform("DiscObserverRadius").set(discObserverRadius);
        target.safeGetUniform("DiscPiHigh").set((float)Math.PI);
        target.safeGetUniform("DiscPiLow").set((float)(Math.PI-(float)Math.PI));
        target.safeGetUniform("LensShadow").set((float)shadow);
        // Preserve the small distance from the critical angle after float upload.
        target.safeGetUniform("DiscShadowLow").set((float)(shadow-(float)shadow));
        target.safeGetUniform("LensLogRange").set((float)Math.log((Math.PI/2-shadow)/SchwarzschildDiscTable.EDGE));
    }
    static SchwarzschildDiscTable discTable() { return discTable; }
    static int discTexture() { return discTexture; }
    static int discBeamGrid() {
        if(!discFiniteBeam)return 1;
        return switch(StarfieldClientConfig.SPACE_VISUAL_QUALITY.get()) {
            case ULTRA -> discVolume?4:8;case HIGH,CUSTOM -> discVolume?3:4;case BALANCED -> 2;case PERFORMANCE -> 1;
        };
    }
    static int discVolumeSteps() {
        return switch(StarfieldClientConfig.SPACE_VISUAL_QUALITY.get()) {
            case ULTRA -> 8;case HIGH,CUSTOM -> 6;case BALANCED -> 4;case PERFORMANCE -> 4;
        };
    }

    static ShaderInstance shader() { return shader; }
    static long reloads() { return reloads; }
    static boolean active() { return shader!=null && !failed && catalog!=null
            && (mode!=Mode.SCHWARZSCHILD || textures[4]!=0) && (mode!=Mode.DISC || discTexture!=0); }
    static StellarBackground catalog() { return catalog; }
    static int[] textureIds() { return textures.clone(); }
    private static void releaseCatalog() {
        for(int i=0;i<4;i++){if(textures[i]!=0)GlStateManager._deleteTexture(textures[i]);textures[i]=0;}
        catalog=null;
    }
    static void release() {
        releaseCatalog();if(textures[4]!=0)GlStateManager._deleteTexture(textures[4]);textures[4]=0;
        if(discTexture!=0)GlStateManager._deleteTexture(discTexture);discTexture=0;discTable=null;
        if(triangle!=null)triangle.close();triangle=null;
    }
    static String diagnostics() {
        return "queryMode="+mode+" queryShader="+(shader!=null)+" queryFailed="+failed+" queryReloads="+reloads+" queryUploads="+uploads
                +" queryDraws="+draws+" querySources="+(catalog==null?0:catalog.count())
                +" queryMaxCandidates="+(catalog==null?0:catalog.maximumCandidates())
                +" queryGpuBytes="+(catalog==null?0:catalog.gpuBytes())+" lensTexture="+(textures[4]!=0)
                +" discTexture="+(discTexture!=0)+" discGpuBytes="+(discTexture==0?0:discTable.gpuBytes())
                +" discInclination="+discInclination+" discDiagnostic="+discDiagnostic+" discPhysical="+discPhysical
                +" discTimeOverride="+discTimeOverride+" discObserverRadius="+discObserverRadius+" discWideGlare="+discWideGlare
                +" discBeamGrid="+discBeamGrid()+" discVolume="+discVolume+" discThickness="+discThickness+" discVolumeSteps="+discVolumeSteps();
    }
}
