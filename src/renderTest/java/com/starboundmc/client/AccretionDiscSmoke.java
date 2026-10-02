package com.starboundmc.client;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import com.starboundmc.client.space.*;
import org.lwjgl.opengl.*;
import org.lwjgl.system.MemoryUtil;

/** Real float-target checks of timed GPU rays and radiative coefficients. */
final class AccretionDiscSmoke {
    private static VertexBuffer triangle;
    private AccretionDiscSmoke() {}
    static String verify() {
        var shader=GpuSkyQuery.shader();if(shader==null)throw new IllegalStateException("Disc shader unavailable");
        var state=SpaceRenderPassState.capture();String before=SpaceDepthSmoke.stateSignature();
        int framebuffer=0,color=0,oldPackBuffer=GL11.glGetInteger(GL21.GL_PIXEL_PACK_BUFFER_BINDING);
        int[] names={GL11.GL_PACK_ALIGNMENT,GL11.GL_PACK_ROW_LENGTH,GL11.GL_PACK_SKIP_PIXELS,GL11.GL_PACK_SKIP_ROWS,GL11.GL_PACK_SWAP_BYTES};
        int[] pack=new int[names.length];for(int i=0;i<names.length;i++)pack[i]=GL11.glGetInteger(names[i]);
        float[] depthBefore=new float[1],depthAfter=new float[1];
        var report=new StringBuilder();
        try {
            GlStateManager._glBindBuffer(GL21.GL_PIXEL_PACK_BUFFER,0);
            for(int i=0;i<names.length;i++)GlStateManager._pixelStore(names[i],i==0?4:0);
            GL11.glReadPixels(0,0,1,1,GL11.GL_DEPTH_COMPONENT,GL11.GL_FLOAT,depthBefore);
            GpuSkyQuery.bind(shader,false);GpuSkyQuery.bindDisc(shader);
            var expected=GpuSkyQuery.discTable().data();var buffer=MemoryUtil.memAllocFloat(expected.length);
            GlStateManager._bindTexture(GpuSkyQuery.discTexture());
            try {
                GL11.glGetTexImage(GL11.GL_TEXTURE_2D,0,GL11.GL_RGBA,GL11.GL_FLOAT,buffer);
                for(int i=0;i<expected.length;i++)if(expected[i]!=buffer.get(i))throw new IllegalStateException("Corrupt disc atlas at "+i);
            } finally {MemoryUtil.memFree(buffer);}
            report.append("discAtlas=allFloatsExact bytes=").append(GpuSkyQuery.discTable().gpuBytes()).append('\n');
            color=SpaceFloatTexture.upload(1,1,4,new float[4]);framebuffer=GL30.glGenFramebuffers();
            GlStateManager._glBindFramebuffer(GL30.GL_FRAMEBUFFER,framebuffer);
            GL30.glFramebufferTexture2D(GL30.GL_FRAMEBUFFER,GL30.GL_COLOR_ATTACHMENT0,GL11.GL_TEXTURE_2D,color,0);
            GL11.glDrawBuffer(GL30.GL_COLOR_ATTACHMENT0);GL11.glReadBuffer(GL30.GL_COLOR_ATTACHMENT0);
            if(GL30.glCheckFramebufferStatus(GL30.GL_FRAMEBUFFER)!=GL30.GL_FRAMEBUFFER_COMPLETE)throw new IllegalStateException("Disc float target unavailable");
            GL11.glViewport(0,0,1,1);RenderSystem.disableDepthTest();RenderSystem.depthMask(false);
            RenderSystem.disableCull();RenderSystem.disableBlend();RenderSystem.disableScissor();RenderSystem.colorMask(true,true,true,true);
            var vertices=Tesselator.getInstance().begin(VertexFormat.Mode.TRIANGLES,DefaultVertexFormat.POSITION);
            vertices.addVertex(-1,-1,0);vertices.addVertex(3,-1,0);vertices.addVertex(-1,3,0);
            triangle=new VertexBuffer(VertexBuffer.Usage.STATIC);triangle.bind();triangle.upload(vertices.buildOrThrow());VertexBuffer.unbind();
            shader.safeGetUniform("TestMode").set(3F);
            double edge=SchwarzschildRay.shadowAngle(20),maxR=0,maxTime=0;int count=0;
            for(double raw:new double[]{.02,.08,edge-.00004,edge+.00004,.23,.4,.65,1.2,1.8,2.6}) {
                float angle=(float)raw;var path=new SchwarzschildPath(20,angle,1e-12);
                for(int i=1;i<150;i++) {
                    float phi=(float)(path.endAzimuth()*i/150);var exact=path.sample(phi);
                    if(exact.radius()<4 || exact.radius()>10)continue;
                    shader.safeGetUniform("TestAngle").set(angle);shader.safeGetUniform("TestSigma").set(phi);draw();
                    float[] actual=read();double radius=1/actual[0];
                    double dr=Math.abs(radius-exact.radius()),dt=Math.abs(actual[2]-exact.delay());
                    maxR=Math.max(maxR,dr);maxTime=Math.max(maxTime,dt);count++;
                    if(actual[3]!=1 || dr>.005 || dt>.015) {
                        shader.safeGetUniform("TestMode").set(6F);draw();float[] header=read();
                        throw new IllegalStateException("GPU timed ray failed: "+dr+" / "+dt+" angle="+angle+" phi="+phi
                                +" actual="+java.util.Arrays.toString(actual)+" header="+java.util.Arrays.toString(header));
                    }
                }
            }
            report.append("discGpuTimedQueries=").append(count).append(" maxRadiusErrorRs=").append(maxR)
                    .append(" maxDelayErrorRsOverC=").append(maxTime).append('\n');
            shader.safeGetUniform("TestMode").set(4F);double spectrumError=0;
            for(float temperature:new float[]{1500,2100,3300,4500,5500,7200,10000,12000}) {
                shader.safeGetUniform("TestSigma").set(temperature);draw();float[] actual=read();
                var c=StarPhotometry.blackbodyRadiance(temperature);float[] reference={c.red(),c.green(),c.blue()};
                for(int k=0;k<3;k++) {
                    double error=Math.abs(actual[k]-reference[k])/Math.max(reference[k],1e-5);spectrumError=Math.max(spectrumError,error);
                    if(error>.025 && Math.abs(actual[k]-reference[k])>1e-5)throw new IllegalStateException("GPU spectrum failed "+error);
                }
            }
            report.append("discGpuSpectrumQueries=8 maxRelativeError=").append(spectrumError).append('\n');
            shader.safeGetUniform("TestMode").set(5F);double gError=0;
            for(float radius:new float[]{4.63F,6F,8F,9.35F})for(float mu:new float[]{-1F,-.3F,.3F,1F}) {
                shader.safeGetUniform("TestSigma").set(radius);shader.safeGetUniform("TestAngle").set(mu);draw();
                double error=Math.abs(read()[0]-SchwarzschildDisc.frequencyRatio(20,radius,mu));gError=Math.max(gError,error);
                if(error>2e-6)throw new IllegalStateException("GPU frequency shift failed");
            }
            report.append("discGpuFrequencyQueries=16 maxError=").append(gError).append('\n');
            // The opaque diagnostic must show the first actual plane hit, even
            // when the same ray is eventually captured. CPU events are independent
            // of the packed GPU trajectory and the shader's intersection loop.
            var normal=new org.joml.Vector3f(-.7F,(float)Math.sqrt(1-.7*.7-.15*.15),.15F).normalize();
            var axisU=new org.joml.Vector3f(0,0,1).fma(-normal.z,normal).normalize();
            var axisV=new org.joml.Vector3f(normal).cross(axisU).normalize();
            shader.safeGetUniform("DiscNormal").set(normal.x,normal.y,normal.z);
            shader.safeGetUniform("DiscU").set(axisU.x,axisU.y,axisU.z);
            shader.safeGetUniform("DiscV").set(axisV.x,axisV.y,axisV.z);
            shader.safeGetUniform("DiscTime").set(180F);shader.safeGetUniform("DiscPhysical").set(0F);
            shader.safeGetUniform("DiscDiagnostic").set(1F);shader.safeGetUniform("TestDirection").set(1F,0F,0F);
            shader.safeGetUniform("TestMode").set(7F);int images=0,capturedImages=0,multiImages=0;
            for(float angle=.02F;angle<.6F;angle+=.002F) {
                var path=new SchwarzschildPath(20,angle,1e-12);
                var hits=SchwarzschildDisc.intersections(path,normal.z,normal.x,4.63,9.35);
                if(hits.isEmpty())continue;
                var hit=hits.getFirst();double band=(hit.radius()-4.63)/(9.35-4.63)*6;
                var point=new org.joml.Vector3f((float)Math.sin(hit.azimuth()),0,(float)Math.cos(hit.azimuth()));
                double sector=(Math.atan2(point.dot(axisV),point.dot(axisU))+Math.PI)/(2*Math.PI)*12;
                // Exclude footprint edges and cell boundaries from a point test.
                if(hit.radius()<4.71 || hit.radius()>9.27 || Math.abs(band-Math.rint(band))<.08
                        || Math.abs(sector-Math.rint(sector))<.08)continue;
                float[] expectedColor=((int)band+(int)sector)%2==0?new float[]{.042F,.49F,.7F,0F}
                        :new float[]{.7F,.21F,.028F,0F};
                shader.safeGetUniform("TestAngle").set(angle);draw();float[] actual=read();
                for(int k=0;k<4;k++)if(Math.abs(actual[k]-expectedColor[k])>2e-5)
                    throw new IllegalStateException("GPU diagnostic first image failed angle="+angle+" channel="+k);
                images++;if(path.fate()==SchwarzschildRay.Fate.CAPTURED)capturedImages++;
                if(hits.size()>1)multiImages++;
            }
            if(images==0 || capturedImages==0 || multiImages==0)throw new IllegalStateException("Disc image checks did not cover all ray outcomes");
            report.append("discGpuFirstImages=").append(images).append(" captured=").append(capturedImages)
                    .append(" multiple=").append(multiImages).append(" CPUPlaneEvents/opaqueDiagnostic\n");
            if(GL11.glGetError()!=GL11.GL_NO_ERROR)throw new IllegalStateException("Disc OpenGL error");
        } finally {
            shader.safeGetUniform("TestMode").set(0F);shader.safeGetUniform("DiscEnabled").set(0F);
            shader.safeGetUniform("DiscDiagnostic").set(0F);shader.clear();
            if(framebuffer!=0)GL30.glDeleteFramebuffers(framebuffer);if(color!=0)GlStateManager._deleteTexture(color);
            if(triangle!=null)triangle.close();triangle=null;VertexBuffer.unbind();
            state.restore();
            GL11.glReadPixels(0,0,1,1,GL11.GL_DEPTH_COMPONENT,GL11.GL_FLOAT,depthAfter);
            GlStateManager._glBindBuffer(GL21.GL_PIXEL_PACK_BUFFER,oldPackBuffer);
            for(int i=0;i<names.length;i++)GlStateManager._pixelStore(names[i],pack[i]);
        }
        if(!before.equals(SpaceDepthSmoke.stateSignature()) || depthBefore[0]!=depthAfter[0])throw new IllegalStateException("Disc checks changed GL state/main depth");
        return report.append("discStateRestored=fiveTextureUnits/program/framebuffers/viewport/buffers/mainDepth\n").toString();
    }
    private static void draw() {
        RenderSystem.setShader(GpuSkyQuery::shader);
        // Bind explicitly: another fixture restored its VAO behind BufferUploader's immediate cache.
        triangle.bind();triangle.drawWithShader(new org.joml.Matrix4f(),new org.joml.Matrix4f(),GpuSkyQuery.shader());
    }
    private static float[] read() {float[] pixel=new float[4];GL11.glReadPixels(0,0,1,1,GL11.GL_RGBA,GL11.GL_FLOAT,pixel);return pixel;}
}
