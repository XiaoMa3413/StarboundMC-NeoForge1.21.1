package com.starboundmc.client;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import com.starboundmc.client.space.SchwarzschildLensTable;
import com.starboundmc.client.space.SchwarzschildRay;
import com.starboundmc.client.space.SkyCube;
import com.starboundmc.client.space.StellarBackground;
import org.joml.Matrix4f;
import org.lwjgl.opengl.*;
import org.lwjgl.system.MemoryUtil;
import java.util.Arrays;

/** Tests actual packed texture transfers and GLSL queries, without modifying gameplay worlds. */
final class SkyQuerySmoke {
    private SkyQuerySmoke() {}
    static String verify() {
        var shader=GpuSkyQuery.shader();
        if(shader==null)throw new IllegalStateException("Sky query shader did not load");
        var state=SpaceRenderPassState.capture();String before=SpaceDepthSmoke.stateSignature();
        float mainDepthBefore=readDepth();StringBuilder report=new StringBuilder();
        int framebuffer=0,colorTexture=0;
        try {
            transfers(report);
            GpuSkyQuery.bind(shader,true);
            colorTexture=SpaceFloatTexture.upload(1,1,4,new float[4]);framebuffer=GL30.glGenFramebuffers();
            GlStateManager._glBindFramebuffer(GL30.GL_FRAMEBUFFER,framebuffer);
            GL30.glFramebufferTexture2D(GL30.GL_FRAMEBUFFER,GL30.GL_COLOR_ATTACHMENT0,GL11.GL_TEXTURE_2D,colorTexture,0);
            GL11.glDrawBuffer(GL30.GL_COLOR_ATTACHMENT0);GL11.glReadBuffer(GL30.GL_COLOR_ATTACHMENT0);
            if(GL30.glCheckFramebufferStatus(GL30.GL_FRAMEBUFFER)!=GL30.GL_FRAMEBUFFER_COMPLETE)
                throw new IllegalStateException("Query validation RGBA32F target unavailable");
            RenderSystem.disableDepthTest();RenderSystem.depthMask(false);RenderSystem.disableBlend();RenderSystem.disableCull();
            RenderSystem.disableScissor();RenderSystem.colorMask(true,true,true,true);
            // One pixel is sufficient for the constant-direction reference queries.
            GL11.glViewport(0,0,1,1);
            shader.safeGetUniform("LinearColor").set(1F);
            shader.safeGetUniform("LensEnabled").set(0F);
            shader.safeGetUniform("TestMode").set(1F);
            shader.safeGetUniform("TestImagePsf").set(0F);
            shader.safeGetUniform("TestAnisotropy").set(1F);shader.safeGetUniform("TestRotation").set(0F);
            shader.safeGetUniform("InverseViewProjection").set(new Matrix4f());
            var sky=GpuSkyQuery.catalog();var sources=sky.sourceData();
            double maximumRelative=0;
            for(int i:new int[]{0,5,100,sky.count()-1})for(float sigma:new float[]{0,.001F,.004F})
                for(double offset:new double[]{0,.0003,.0008}) {
                    int p=i*8;double x=sources[p]+offset,y=sources[p+1],z=sources[p+2];
                    double length=Math.sqrt(x*x+y*y+z*z);x/=length;y/=length;z/=length;
                    shader.safeGetUniform("TestDirection").set((float)x,(float)y,(float)z);
                    shader.safeGetUniform("TestSigma").set(sigma);draw();
                    float[] actual=read();double[] expected=sky.radiance((float)x,(float)y,(float)z,sigma);
                    for(int k=0;k<3;k++) {
                        double error=Math.abs(actual[k]-expected[k])/Math.max(expected[k],.000001);
                        maximumRelative=Math.max(maximumRelative,error);
                        if(error>.002 && Math.abs(actual[k]-expected[k])>2e-6)
                            throw new IllegalStateException("GPU stellar query disagrees with reference: "+Arrays.toString(actual)+" / "+Arrays.toString(expected));
                    }
                }
            report.append("indexedGpuQueries=36 subpixelOffsets/circularBeams maxRelativeError=").append(maximumRelative)
                    .append(" RGBA32F\n");
            double ellipseError=0;
            for(int i:new int[]{0,5,100})for(float rotation:new float[]{.2F,1.2F})for(float ratio:new float[]{.01F,.3F}) {
                int p=i*8;float x=sources[p]+.0008F,y=sources[p+1],z=sources[p+2];
                shader.safeGetUniform("TestDirection").set(x,y,z);shader.safeGetUniform("TestSigma").set(.003F);
                shader.safeGetUniform("TestAnisotropy").set(ratio);shader.safeGetUniform("TestRotation").set(rotation);draw();
                double[] expected=ellipseReference(sources,sky.count(),x,y,z,.003F,ratio,rotation,false);float[] actual=read();
                for(int k=0;k<3;k++) {
                    double error=Math.abs(actual[k]-expected[k])/Math.max(expected[k],1e-6);ellipseError=Math.max(ellipseError,error);
                    if(error>.004 && Math.abs(actual[k]-expected[k])>2e-6)
                        throw new IllegalStateException("GPU elliptical beam failed: "+i+" / "+rotation+" / "+ratio+" / "+error+" / "+Arrays.toString(actual)+" / "+Arrays.toString(expected));
                }
            }
            report.append("ellipticalGpuQueries=12 rotation/100to1Beam maxRelativeError=").append(ellipseError).append('\n');
            shader.safeGetUniform("TestImagePsf").set(1F);double psfError=0;
            for(int i:new int[]{0,5,100})for(float rotation:new float[]{.2F,1.2F})for(float ratio:new float[]{.1F,.3F}) {
                int p=i*8;float x=sources[p]+.0002F,y=sources[p+1],z=sources[p+2];
                shader.safeGetUniform("TestDirection").set(x,y,z);shader.safeGetUniform("TestSigma").set(.001F);
                shader.safeGetUniform("TestAnisotropy").set(ratio);shader.safeGetUniform("TestRotation").set(rotation);draw();
                double[] expected=ellipseReference(sources,sky.count(),x,y,z,.001F,ratio,rotation,true);float[] actual=read();
                for(int k=0;k<3;k++) {
                    double error=Math.abs(actual[k]-expected[k])/Math.max(expected[k],1e-6);psfError=Math.max(psfError,error);
                    if(error>.004 && Math.abs(actual[k]-expected[k])>2e-6)
                        throw new IllegalStateException("Image plane PSF failed: "+error);
                }
            }
            report.append("imagePlanePsfQueries=12 ellipticalFlux/rotation maxRelativeError=").append(psfError).append('\n');
            shader.safeGetUniform("TestImagePsf").set(0F);
            shader.safeGetUniform("TestAnisotropy").set(1F);shader.safeGetUniform("TestRotation").set(0F);
            // A broad beam switches to prefiltered flux; seams and poles must remain finite.
            for(int face=0;face<6;face++)for(float sigma:new float[]{.006F,.008F,.02F,.2F}) {
                var d=SkyCube.direction(face,1,1);
                shader.safeGetUniform("TestDirection").set((float)d[0],(float)d[1],(float)d[2]);
                shader.safeGetUniform("TestSigma").set(sigma);draw();
                for(float c:read())if(!Float.isFinite(c)||c<0)throw new IllegalStateException("Nonfinite broad beam");
            }
            report.append("wideBeamGpuQueries=24 cubeCorners/levels finiteNonnegative\n");
            double seamError=0;
            for(float sigma:new float[]{.012F,.04F,.15F})for(float v:new float[]{-.6F,0,.6F}) {
                shader.safeGetUniform("TestSigma").set(sigma);
                shader.safeGetUniform("TestDirection").set(1F,v,1F-.000001F);draw();float[] left=read();
                shader.safeGetUniform("TestDirection").set(1F-.000001F,v,1F);draw();float[] right=read();
                for(int k=0;k<3;k++) {
                    double relative=Math.abs(left[k]-right[k])/Math.max(1e-7,(left[k]+right[k])/2);
                    seamError=Math.max(seamError,relative);
                    if(relative>.03 && Math.abs(left[k]-right[k])>1e-6)
                        throw new IllegalStateException("Prefiltered sky face seam: "+relative);
                }
            }
            report.append("wideBeamSeamPairs=9 maxRelativeDifference=").append(seamError).append('\n');
            shader.safeGetUniform("TestMode").set(2F);
            var table=new SchwarzschildLensTable(20);double maximumAngleError=0;
            for(int i=0;i<48;i++) {
                float angle=(float)(table.shadow()+.0001*Math.exp(i/47.0*Math.log((Math.PI-table.shadow())/.0001)));
                shader.safeGetUniform("TestAngle").set(angle);draw();
                double expected=SchwarzschildRay.trace(20,Math.min(angle,Math.PI)).azimuth();
                double error=Math.abs(read()[0]-expected);maximumAngleError=Math.max(maximumAngleError,error);
                if(error>.00025)throw new IllegalStateException("GPU lens table disagrees with geodesic: "+error);
            }
            report.append("lensGpuQueries=48 maxAzimuthErrorRad=").append(maximumAngleError)
                    .append(" RGBA32F (CPU table separately checked at 6e-5 rad)\n");
            shader.safeGetUniform("TestMode").set(0F);shader.safeGetUniform("LensEnabled").set(1F);
            shader.safeGetUniform("LensDirection").set(GpuSkyQuery.LENS_DIRECTION.x,GpuSkyQuery.LENS_DIRECTION.y,GpuSkyQuery.LENS_DIRECTION.z);
            shader.safeGetUniform("FieldDetail").set(1F);shader.safeGetUniform("TintAmount").set(0F);
            // Constant ray through the center of the shadow; the captured beam has no radiance.
            var d=GpuSkyQuery.LENS_DIRECTION;
            shader.safeGetUniform("InverseViewProjection").set(new Matrix4f().zero().m30(d.x).m31(d.y).m32(d.z).m33(1));
            draw();float[] captured=read();
            for(int k=0;k<3;k++)if(captured[k]!=0)throw new IllegalStateException("Captured light escaped: "+Arrays.toString(captured));
            report.append("centralCapturedBeam=black safeAxis\n");
            shader.safeGetUniform("LensEnabled").set(0F);shader.safeGetUniform("TestMode").set(0F);
            if(GL11.glGetError()!=GL11.GL_NO_ERROR)throw new IllegalStateException("Sky query OpenGL error");
        } finally {
            shader.clear();if(framebuffer!=0)GL30.glDeleteFramebuffers(framebuffer);
            if(colorTexture!=0)GlStateManager._deleteTexture(colorTexture);state.restore();
        }
        if(!before.equals(SpaceDepthSmoke.stateSignature()) || mainDepthBefore!=readDepth())
            throw new IllegalStateException("Background query did not restore GL state/main depth");
        report.append("queryStateRestored=fiveTextureUnits/program/buffers/framebuffers/viewport/mainDepth\n");
        return report.toString();
    }

    private static void transfers(StringBuilder report) {
        var shader=GpuSkyQuery.shader();
        int[] unpack={GL11.GL_UNPACK_ALIGNMENT,GL11.GL_UNPACK_ROW_LENGTH,GL11.GL_UNPACK_SKIP_PIXELS,
                GL11.GL_UNPACK_SKIP_ROWS,GL11.GL_UNPACK_SWAP_BYTES};
        int[] pack={GL11.GL_PACK_ALIGNMENT,GL11.GL_PACK_ROW_LENGTH,GL11.GL_PACK_SKIP_PIXELS,
                GL11.GL_PACK_SKIP_ROWS,GL11.GL_PACK_SWAP_BYTES};
        int[] oldUnpack=values(unpack),oldPack=values(pack);
        int oldUnpackBuffer=GL11.glGetInteger(GL21.GL_PIXEL_UNPACK_BUFFER_BINDING),oldPackBuffer=GL11.glGetInteger(GL21.GL_PIXEL_PACK_BUFFER_BINDING);
        int pbo=GL15.glGenBuffers();
        try {
            GlStateManager._glBindBuffer(GL21.GL_PIXEL_PACK_BUFFER,0);set(pack,new int[]{4,0,0,0,0});
            GlStateManager._glBindBuffer(GL21.GL_PIXEL_UNPACK_BUFFER,0);set(unpack,new int[]{4,0,0,0,0});
            try(var partial=new net.minecraft.client.renderer.texture.DynamicTexture(new com.mojang.blaze3d.platform.NativeImage(1024,4,true))) {
                for(String condition:new String[]{"nativeImageSubregion","foreignCpuLayout","foreignUnpackPbo"}) {
                    GlStateManager._glBindBuffer(GL21.GL_PIXEL_UNPACK_BUFFER,0);set(unpack,new int[]{4,0,0,0,0});
                    partial.bind();
                    if(condition.equals("nativeImageSubregion"))partial.getPixels().upload(0,0,0,3,2,5,1,false,false,false,false);
                    else {
                        set(unpack,new int[]{8,2048,11,7,1});
                        if(condition.equals("foreignUnpackPbo")) {
                            GlStateManager._glBindBuffer(GL21.GL_PIXEL_UNPACK_BUFFER,pbo);
                            GL15.glBufferData(GL21.GL_PIXEL_UNPACK_BUFFER,64L,GL15.GL_STREAM_DRAW);
                        }
                    }
                    var before=values(unpack);int binding=GL11.glGetInteger(GL11.GL_TEXTURE_BINDING_2D);
                    int buffer=GL11.glGetInteger(GL21.GL_PIXEL_UNPACK_BUFFER_BINDING);
                    GpuSkyQuery.release();GpuSkyQuery.bind(shader,true);String created=GpuSkyQuery.diagnostics();
                    GpuSkyQuery.bind(shader,true);
                    if(!created.equals(GpuSkyQuery.diagnostics()) || !Arrays.equals(before,values(unpack))
                            || buffer!=GL11.glGetInteger(GL21.GL_PIXEL_UNPACK_BUFFER_BINDING)
                            || binding!=GL11.glGetInteger(GL11.GL_TEXTURE_BINDING_2D))
                        throw new IllegalStateException("Query texture upload changed caller state");
                    var sky=GpuSkyQuery.catalog();float[][] expected={sky.cellData(),sky.indices(),sky.sourceData(),sky.fluxData(),new SchwarzschildLensTable(20).data()};
                    int[] formats={GL30.GL_RG,GL11.GL_RED,GL11.GL_RGBA,GL11.GL_RGBA,GL11.GL_RED};
                    int[] ids=GpuSkyQuery.textureIds();
                    for(int i=0;i<5;i++) {
                        GlStateManager._bindTexture(ids[i]);var readback=MemoryUtil.memAllocFloat(expected[i].length);
                        try {
                            GL11.glGetTexImage(GL11.GL_TEXTURE_2D,0,formats[i],GL11.GL_FLOAT,readback);
                            for(int p=0;p<expected[i].length;p++)if(Float.compare(expected[i][p],readback.get(p))!=0)
                                throw new IllegalStateException("Corrupt query texture "+i+" / "+p+" / "+condition);
                        } finally {MemoryUtil.memFree(readback);}
                    }
                    if(GL11.glGetError()!=GL11.GL_NO_ERROR)throw new IllegalStateException("Query upload GL error");
                    report.append("queryTextureUpload=").append(condition).append(" fiveTextures/allFloatsExact/stateRestored/cacheStable\n");
                }
            }
        } finally {
            GlStateManager._glBindBuffer(GL21.GL_PIXEL_UNPACK_BUFFER,oldUnpackBuffer);
            GlStateManager._glBindBuffer(GL21.GL_PIXEL_PACK_BUFFER,oldPackBuffer);
            set(unpack,oldUnpack);set(pack,oldPack);GL15.glDeleteBuffers(pbo);
        }
    }
    private static int[] values(int[] queries){return Arrays.stream(queries).map(GL11::glGetInteger).toArray();}
    private static void set(int[] queries,int[] values){for(int i=0;i<queries.length;i++)GlStateManager._pixelStore(queries[i],values[i]);}
    private static void draw() {
        RenderSystem.setShader(GpuSkyQuery::shader);
        var b=Tesselator.getInstance().begin(VertexFormat.Mode.TRIANGLES,DefaultVertexFormat.POSITION);
        b.addVertex(-1,-1,0);b.addVertex(3,-1,0);b.addVertex(-1,3,0);BufferUploader.drawWithShader(b.buildOrThrow());
    }
    private static float[] read(){return readPixel(GL11.GL_RGBA,4);}
    private static float readDepth(){return readPixel(GL11.GL_DEPTH_COMPONENT,1)[0];}
    private static float[] readPixel(int format,int channels) {
        int[] pack={GL11.GL_PACK_ALIGNMENT,GL11.GL_PACK_ROW_LENGTH,GL11.GL_PACK_SKIP_PIXELS,
                GL11.GL_PACK_SKIP_ROWS,GL11.GL_PACK_SWAP_BYTES};
        int[] previous=values(pack);int buffer=GL11.glGetInteger(GL21.GL_PIXEL_PACK_BUFFER_BINDING);
        try {
            GlStateManager._glBindBuffer(GL21.GL_PIXEL_PACK_BUFFER,0);set(pack,new int[]{4,0,0,0,0});
            float[] data=new float[channels];GL11.glReadPixels(0,0,1,1,format,GL11.GL_FLOAT,data);return data;
        } finally {set(pack,previous);GlStateManager._glBindBuffer(GL21.GL_PIXEL_PACK_BUFFER,buffer);}
    }
    private static double[] ellipseReference(float[] data,int count,double x,double y,double z,double sigma,double ratio,double angle,boolean imagePsf) {
        double length=Math.sqrt(x*x+y*y+z*z);x/=length;y/=length;z/=length;
        double[] t=Math.abs(y)<.9 ? new double[]{z,0,-x} : new double[]{0,-z,y};
        length=Math.sqrt(t[0]*t[0]+t[1]*t[1]+t[2]*t[2]);for(int k=0;k<3;k++)t[k]/=length;
        double[] b={y*t[2]-z*t[1],z*t[0]-x*t[2],x*t[1]-y*t[0]},rgb=new double[3];
        double cs=Math.cos(angle),sn=Math.sin(angle);
        for(int i=0;i<count;i++) {
            int p=i*8;double dx=data[p]-x,dy=data[p+1]-y,dz=data[p+2]-z;
            double s2=imagePsf ? 1e-16 : data[p+7]*data[p+7];
            double factor=imagePsf ? 1+12*Math.pow(data[p+7]*StellarBackground.FOCAL_REFERENCE,2) : 1;
            double major=sigma*(imagePsf?Math.sqrt(1+12*.48*.48):1);
            if(dx*dx+dy*dy+dz*dz>StellarBackground.SUPPORT_SIGMAS*StellarBackground.SUPPORT_SIGMAS*(major*major+s2))continue;
            double tx=dx*t[0]+dy*t[1]+dz*t[2],by=dx*b[0]+dy*b[1]+dz*b[2];
            double u=tx*cs+by*sn,v=-tx*sn+by*cs;
            double a=sigma*sigma*factor+s2,c=sigma*sigma*ratio*ratio*factor+s2;
            double exponent=u*u/a+v*v/c;
            if(exponent>StellarBackground.SUPPORT_SIGMAS*StellarBackground.SUPPORT_SIGMAS)continue;
            double kernel=Math.exp(-exponent/2)/(2*Math.PI*Math.sqrt(a*c));
            for(int k=0;k<3;k++)rgb[k]+=data[p+4+k]*kernel;
        }
        return rgb;
    }
}
