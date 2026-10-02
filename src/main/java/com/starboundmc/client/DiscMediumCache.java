package com.starboundmc.client;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.logging.LogUtils;
import com.starboundmc.StarboundMC;
import com.starboundmc.client.space.DiscCoefficientLayout;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.client.event.RegisterShadersEvent;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL30;

/** Camera-independent local j_rgb / grey extinction, baked before filtering.
 * Two scratch targets avoid texture feedback; guarded charts have explicit mips. */
final class DiscMediumCache {
    private static ShaderInstance bake;
    private static DiscCoefficientLayout layout;
    private static Scratch[] scratch;
    private static int texture;
    private static final float[] keys=new float[8],gains=new float[8];
    private static final long[] ready=new long[8];
    private static boolean failed;
    private static long generations;
    private static double worstBakeMs;
    private static final DiscCoefficientLayout ULTRA=new DiscCoefficientLayout(128,512,16);
    private static final DiscCoefficientLayout HIGH=new DiscCoefficientLayout(128,256,16);
    private static final DiscCoefficientLayout SMALL=new DiscCoefficientLayout(64,256,8);
    static {java.util.Arrays.fill(keys,Float.NEGATIVE_INFINITY);}
    private DiscMediumCache() {}

    static void register(RegisterShadersEvent event) {
        release();bake=null;failed=false;
        try {
            event.registerShader(new ShaderInstance(event.getResourceProvider(),
                    ResourceLocation.fromNamespaceAndPath(StarboundMC.MODID,"space_disc_coefficients"),
                    DefaultVertexFormat.POSITION),s->bake=s);
        } catch(java.io.IOException|RuntimeException error) {
            LogUtils.getLogger().warn("Disc coefficient bake unavailable; retaining procedural medium",error);
        }
    }
    /** At most one generation per draw. Upcoming fields are prefetched before their weight rises. */
    static void prepare(float time) {
        if(bake==null || failed)return;
        var quality=StarfieldClientConfig.SPACE_VISUAL_QUALITY.get();
        var wanted=switch(quality) {
            case ULTRA -> ULTRA;
            case HIGH,CUSTOM -> HIGH;
            case BALANCED,PERFORMANCE -> SMALL;
        };
        int newest=DiscCoefficientLayout.newest(time),missing=Integer.MIN_VALUE;
        int[] priority={newest-3,newest-2,newest-4,newest-5,newest-1,newest,newest-6,newest-7};
        if(wanted.equals(layout)) {
            for(int generation:priority)if(keys[DiscCoefficientLayout.bank(generation)]!=generation) {missing=generation;break;}
            // A resident window only updates CPU fade weights; no GL state
            // queries or framebuffer work occur on the ordinary cached draw.
            if(missing==Integer.MIN_VALUE) {updateGains();return;}
        }
        var state=SpaceRenderPassState.capture();
        try {
            if(!wanted.equals(layout)) {
                release();layout=wanted;
                int maximum=GL11.glGetInteger(GL11.GL_MAX_TEXTURE_SIZE);
                if(layout.width()>maximum || layout.rows()>maximum)
                    throw new IllegalStateException("Disc coefficient atlas exceeds texture limit");
                texture=SpaceFloatTexture.allocateHalf(layout.width(),layout.rows());
                scratch=new Scratch[2];
                for(int i=0;i<2;i++) {
                    int color=SpaceFloatTexture.allocateHalf(layout.width(),layout.rows(0));
                    int framebuffer=GL30.glGenFramebuffers();scratch[i]=new Scratch(framebuffer,color);
                    GlStateManager._glBindFramebuffer(GL30.GL_FRAMEBUFFER,framebuffer);
                    GL30.glFramebufferTexture2D(GL30.GL_FRAMEBUFFER,GL30.GL_COLOR_ATTACHMENT0,GL11.GL_TEXTURE_2D,color,0);
                    if(GL30.glCheckFramebufferStatus(GL30.GL_FRAMEBUFFER)!=GL30.GL_FRAMEBUFFER_COMPLETE)
                        throw new IllegalStateException("Incomplete disc coefficient bake target");
                }
            }
            // First fill current/retarded fields, then the future pair, then older delay tails.
            for(int generation:priority) {
                int bank=DiscCoefficientLayout.bank(generation);
                if(keys[bank]!=generation) {build(generation,bank);break;}
            }
            updateGains();
        } catch(RuntimeException error) {
            failed=true;release();LogUtils.getLogger().warn("Disc coefficient cache failed; retaining procedural medium until reload",error);
        } finally {state.restore();}
    }
    private static void updateGains() {
        long now=System.nanoTime();
        for(int i=0;i<8;i++)gains[i]=ready[i]==0?0:Math.min(1,(now-ready[i])/250_000_000F);
    }
    private static void build(int generation,int bank) {
        long start=System.nanoTime();
        bake.setSampler("LensAzimuth",GpuSkyQuery.discTexture());
        bake.safeGetUniform("FieldDetail").set(1F);
        bake.safeGetUniform("BakeGeneration").set((float)generation);
        bake.safeGetUniform("ScratchSize").set((float)layout.width(),(float)layout.rows(0));
        for(int level=0;level<layout.levels();level++) {
            Scratch output=scratch[level&1],input=scratch[(level+1)&1];
            GlStateManager._glBindFramebuffer(GL30.GL_DRAW_FRAMEBUFFER,output.framebuffer());
            RenderSystem.viewport(0,0,layout.width(level),layout.rows(level));
            RenderSystem.disableScissor();RenderSystem.colorMask(true,true,true,true);
            GL11.glDisable(GL30.GL_FRAMEBUFFER_SRGB);
            bake.safeGetUniform("BakeLevel").set((float)level);
            bake.safeGetUniform("BakeSize").set((float)layout.radial(level),(float)layout.azimuth(level),(float)layout.height(level));
            bake.safeGetUniform("PreviousSize").set((float)layout.radial(Math.max(0,level-1)),
                    (float)layout.azimuth(Math.max(0,level-1)),(float)layout.height(Math.max(0,level-1)));
            SpaceSceneTarget.drawFullscreen(bake,input.texture(),false);
            GlStateManager._glBindFramebuffer(GL30.GL_READ_FRAMEBUFFER,output.framebuffer());
            GlStateManager._bindTexture(texture);
            GL11.glCopyTexSubImage2D(GL11.GL_TEXTURE_2D,0,0,bank*layout.stride()+layout.offset(level),
                    0,0,layout.width(level),layout.rows(level));
        }
        keys[bank]=generation;ready[bank]=System.nanoTime();generations++;
        worstBakeMs=Math.max(worstBakeMs,(ready[bank]-start)/1e6);
    }
    static void bind(ShaderInstance shader,int fallback,boolean enabled) {
        shader.setSampler("DiscCoefficients",texture==0?fallback:texture);
        shader.safeGetUniform("DiscCacheEnabled").set(enabled && texture!=0?1F:0F);
        if(layout==null)return;
        shader.safeGetUniform("DiscCacheSize").set((float)layout.radial(),(float)layout.azimuth(),(float)layout.height());
        shader.safeGetUniform("DiscCacheAtlas").set((float)layout.width(),(float)layout.rows(),(float)layout.stride(),(float)(layout.levels()-1));
        shader.safeGetUniform("DiscCacheKeys0").set(keys[0],keys[1],keys[2],keys[3]);
        shader.safeGetUniform("DiscCacheKeys1").set(keys[4],keys[5],keys[6],keys[7]);
        shader.safeGetUniform("DiscCacheGains0").set(gains[0],gains[1],gains[2],gains[3]);
        shader.safeGetUniform("DiscCacheGains1").set(gains[4],gains[5],gains[6],gains[7]);
    }
    static void release() {
        if(texture!=0)GlStateManager._deleteTexture(texture);texture=0;
        if(scratch!=null)for(var target:scratch)if(target!=null) {
            GlStateManager._deleteTexture(target.texture());GL30.glDeleteFramebuffers(target.framebuffer());
        }
        scratch=null;
        layout=null;java.util.Arrays.fill(keys,Float.NEGATIVE_INFINITY);java.util.Arrays.fill(ready,0);java.util.Arrays.fill(gains,0);
    }
    static String diagnostics() {
        return " discCache="+(texture!=0)+" discCacheFailed="+failed+" discCacheGenerations="+generations
                +" discCacheGpuBytes="+(layout==null?0:layout.gpuBytes()+2L*layout.width()*layout.rows(0)*8)
                +" discCacheBakeCpuMaxMs="+worstBakeMs;
    }
    static int texture() {return texture;}
    static DiscCoefficientLayout layout() {return layout;}
    private record Scratch(int framebuffer,int texture) {}
}
