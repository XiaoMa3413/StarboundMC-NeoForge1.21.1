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
            GpuSkyQuery.discObserverRadius=20F;
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
            verifyNearObservers(report);
            verifyMaterialContinuity(report);
            verifyNoiseGradients(report);
            verifyFiniteBeam(report);
            verifyVolume(report);
            verifyVolumeContinuity(report);
            verifyVolumeBeam(report);
            verifyVolumeNoise(report);
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
    private static void verifyVolume(StringBuilder report) {
        var shader=GpuSkyQuery.shader();int shells=0,transfers=0,captured=0,inside=0;
        double maxPhi=0,maxTau=0,maxTransfer=0,maxCoarse=0,coarseSquared=0,fineSquared=0,maxRadius=0,maxDelay=0;
        int timed=0;
        try {
            for(float observer:new float[]{20,6}) {
                GpuSkyQuery.discObserverRadius=observer;GpuSkyQuery.bindDisc(shader);
                double edge=SchwarzschildRay.shadowAngle(observer);
                for(double raw:new double[]{0,.00001,.0004,.001,.003,.03,.1,edge-.00004,edge+.00004,.2,.4,.7,1.2,1.8,2.6,
                        Math.PI-.003,Math.PI-.001,Math.PI-.0004,Math.PI-.00001,Math.PI}) {
                    float angle=(float)raw;var path=new SchwarzschildPath(observer,Math.clamp(angle,1e-6,Math.PI-1e-6),1e-12);
                    shader.safeGetUniform("TestAngle").set(angle);shader.safeGetUniform("TestMode").set(12F);draw();
                    float[] shell=read();double[] expectedShell=path.shellRanges(4.63,9.35);
                    for(int k=0;k<4;k++) {
                        double error=Math.abs(shell[k]-expectedShell[k]);maxPhi=Math.max(maxPhi,error);
                        if(error>.006)throw new IllegalStateException("GPU volume shell mismatch: "+error);
                    }
                    shells++;
                    for(int half=0;half<2;half++)for(int i=0;i<6;i++) {
                        if(expectedShell[half*2+1]-expectedShell[half*2]<1e-12)continue;
                        float phi=(float)(expectedShell[half*2]+(i+.5)/6*(expectedShell[half*2+1]-expectedShell[half*2]));
                        var reference=path.sample(phi);shader.safeGetUniform("TestMode").set(16F);
                        shader.safeGetUniform("TestSigma").set(phi);draw();float[] trace=read();
                        double dr=Math.abs(1/trace[0]-reference.radius()),dt=Math.abs(trace[2]-reference.delay());
                        maxRadius=Math.max(maxRadius,dr);maxDelay=Math.max(maxDelay,dt);timed++;
                        if(trace[3]!=1 || dr>.005 || dt>.015) {
                            shader.safeGetUniform("TestMode").set(17F);draw();float[] lookup=read();
                            throw new IllegalStateException("Cached/radial volume trajectory mismatch: "+dr+" / "+dt
                                    +" r="+observer+" angle="+angle+" phi="+phi+" exactImpact="+path.impact()
                                    +" trace="+java.util.Arrays.toString(trace)+" lookup="+java.util.Arrays.toString(lookup));
                        }
                    }
                    for(float[] xyz:new float[][]{{-.7F,.698F,.15F},{-.4F,.914F,.06F},{0,1,0},{-.5F,.866F,.0087F}}) {
                        var n=new org.joml.Vector3f(xyz).normalize();
                        shader.safeGetUniform("DiscNormal").set(n.x,n.y,n.z);
                        shader.safeGetUniform("DiscThickness").set(.016F);shader.safeGetUniform("TestMode").set(13F);
                        for(float physical:new float[]{0,1}) {
                            shader.safeGetUniform("DiscPhysical").set(physical);
                            double tau=SchwarzschildVolume.opticalDepth(path,n.z,n.x,n.y,.016F,.12,physical>.5,2048);
                            shader.safeGetUniform("DiscVolumeSteps").set(8F);draw();float[] coarse=read();
                            shader.safeGetUniform("DiscVolumeSteps").set(16F);draw();float[] actual=read();
                            double error=Math.abs(actual[0]-tau);maxTau=Math.max(maxTau,error);
                            double scale=Math.max(1,tau);
                            fineSquared+=Math.pow(error/scale,2);coarseSquared+=Math.pow((coarse[0]-tau)/scale,2);
                            maxCoarse=Math.max(maxCoarse,Math.abs(coarse[0]-tau)/scale);
                            if(!Float.isFinite(coarse[0]) || Math.abs(coarse[0]-tau)/scale>.006)
                                throw new IllegalStateException("Production Gauss8 volume depth mismatch: "+coarse[0]+" / "+tau
                                        +" r="+observer+" angle="+angle+" n="+n+" physical="+physical);
                            if(!Float.isFinite(actual[0]) || error/scale>.006)
                                throw new IllegalStateException("GPU volume optical depth mismatch: "+error+" / "+tau
                                        +" r="+observer+" angle="+angle+" n="+n+" physical="+physical);
                            double transmission=Math.exp(-tau);
                            maxTransfer=Math.max(maxTransfer,Math.abs(actual[1]-transmission));
                            maxTransfer=Math.max(maxTransfer,Math.abs(actual[2]-(1-transmission)));
                            if(Math.abs(actual[1]-transmission)>.006 || Math.abs(actual[2]-(1-transmission))>.006
                                    || Math.abs(actual[1]+actual[2]-1)>2e-6 || actual[1]<0 || actual[1]>1)
                                throw new IllegalStateException("GPU volume constant-source transfer mismatch");
                            transfers++;if(tau>0 && path.fate()==SchwarzschildRay.Fate.CAPTURED)captured++;
                            if(tau>0 && observer==6 && Math.abs(n.z)<.016*3)inside++;
                        }
                    }
                }
            }
        } finally {
            GpuSkyQuery.discObserverRadius=20F;GpuSkyQuery.bindDisc(shader);
            shader.safeGetUniform("DiscPhysical").set(0F);
        }
        if(captured==0 || inside==0 || fineSquared>=coarseSquared)
            throw new IllegalStateException("Volume checks lack capture/inside/convergence coverage: "+fineSquared+" / "+coarseSquared);
        report.append("discGpuVolume shells=").append(shells).append(" transfers=").append(transfers)
                .append(" captured=").append(captured).append(" inside=").append(inside)
                .append(" maxShellPhiError=").append(maxPhi).append(" maxTauAbsError=").append(maxTau)
                .append(" maxTransferAbsError=").append(maxTransfer)
                .append(" rmsScaledTauError16=").append(Math.sqrt(fineSquared/transfers))
                .append(" rmsScaledTauError8=").append(Math.sqrt(coarseSquared/transfers))
                .append(" maxScaledTauError8=").append(maxCoarse)
                .append(" timedQueries=").append(timed).append(" maxRadiusErrorRs=").append(maxRadius)
                .append(" maxDelayErrorRsOverC=").append(maxDelay)
                .append(" independentODE/midpoint2048/GPUgauss2PerCell/constantSource/boundedRadialLimit\n");
    }
    private static void verifyVolumeContinuity(StringBuilder report) {
        var shader=GpuSkyQuery.shader();shader.safeGetUniform("TestMode").set(14F);
        double maxTransition=0,maxSeam=0;int transitions=0,seams=0;
        for(float radius:new float[]{4.8F,6F,8F,9.2F})for(float height:new float[]{0,.7F,2F}) {
            shader.safeGetUniform("TestSigma").set(radius);shader.safeGetUniform("TestAnisotropy").set(height);
            for(float polar:new float[]{-2.5F,-.4F,1.1F}) {
                shader.safeGetUniform("TestAngle").set(polar);
                for(float time:new float[]{-70F,-35F,0F,35F,70F,105F,140F,175F,350F}) {
                    shader.safeGetUniform("DiscTime").set(time-.0001F);draw();float[] a=read();
                    shader.safeGetUniform("DiscTime").set(time+.0001F);draw();float[] b=read();
                    for(int k=0;k<4;k++) {
                        requireMaterial(a,k);requireMaterial(b,k);
                        double error=Math.abs(a[k]-b[k])/Math.max(1,Math.max(a[k],b[k]));
                        maxTransition=Math.max(maxTransition,error);
                        if(error>.0025)throw new IllegalStateException("Volume flow generation discontinuity: "+error);
                    }
                    transitions++;
                }
            }
            for(float time:new float[]{0F,35F,180F,400F}) {
                shader.safeGetUniform("DiscTime").set(time);
                shader.safeGetUniform("TestAngle").set((float)-Math.PI);draw();float[] a=read();
                shader.safeGetUniform("TestAngle").set((float)Math.PI);draw();float[] b=read();
                for(int k=0;k<4;k++) {
                    requireMaterial(a,k);requireMaterial(b,k);
                    double error=Math.abs(a[k]-b[k])/Math.max(1,Math.max(a[k],b[k]));
                    maxSeam=Math.max(maxSeam,error);
                    if(error>.0002)throw new IllegalStateException("Volume flow polar seam: "+error);
                }
                seams++;
            }
        }
        report.append("discGpuVolumeFlow transitions=").append(transitions).append(" seams=").append(seams)
                .append(" maxTransitionScaledError=").append(maxTransition).append(" maxSeamScaledError=").append(maxSeam).append('\n');
    }
    private static void verifyVolumeBeam(StringBuilder report) {
        var shader=GpuSkyQuery.shader();int queries=0,partial=0;double maxError=0,referenceError=0,fineSquared=0,coarseSquared=0;
        var n=new org.joml.Vector3f(-.7F,(float)Math.sqrt(1-.15*.15-.7*.7),.15F).normalize();
        shader.safeGetUniform("DiscNormal").set(n.x,n.y,n.z);
        shader.safeGetUniform("DiscThickness").set(.016F);shader.safeGetUniform("DiscPhysical").set(0F);
        shader.safeGetUniform("DiscVolumeSteps").set(8F);shader.safeGetUniform("TestMode").set(15F);
        double edge=SchwarzschildRay.shadowAngle(20);
        for(float[] beam:new float[][]{{-.0012F,.0012F,1},{-.0005F,.0012F,1},
                {-.0001F,.0012F,1},{.0001F,.0012F,1},{.0005F,.0012F,1},{.0012F,.0012F,1},
                {.0002F,.0024F,.5F},{-.0002F,.0024F,2}}) {
            float angle=(float)(edge+beam[0]),width=beam[1],aspect=beam[2];
            shader.safeGetUniform("TestAngle").set(angle);shader.safeGetUniform("TestSigma").set(width);
            shader.safeGetUniform("TestAnisotropy").set(aspect);
            shader.safeGetUniform("DiscBeamGrid").set(2F);draw();float[] coarse=read();
            shader.safeGetUniform("DiscBeamGrid").set(4F);draw();float[] actual=read();
            double a=volumeBeamReference(angle,width,aspect,32,n),reference=volumeBeamReference(angle,width,aspect,64,n);
            referenceError=Math.max(referenceError,Math.abs(a-reference));
            double error=Math.abs(actual[3]-reference);maxError=Math.max(maxError,error);
            fineSquared+=error*error;coarseSquared+=Math.pow(coarse[3]-reference,2);
            for(int k=0;k<4;k++)if(!Float.isFinite(actual[k]) || actual[k]<0 || actual[k]>1)
                throw new IllegalStateException("Non-finite/unbounded volume beam transfer");
            if(error>.06 || Math.abs(actual[0]+actual[3]-1)>2e-6)
                throw new IllegalStateException("Volume finite beam mismatch: "+error+" offset="+beam[0]);
            if(reference>.05 && reference<.95)partial++;
            queries++;
        }
        if(referenceError>.015 || fineSquared>=coarseSquared || partial<2)
            throw new IllegalStateException("Volume beam reference/convergence/partial coverage failure: "+referenceError
                    +" fine="+fineSquared+" coarse="+coarseSquared+" partial="+partial);
        report.append("discGpuVolumeBeams=").append(queries).append(" grid=4x4 denseCPUGrid=64x64 partial=").append(partial)
                .append(" maxTransferAbsError=").append(maxError).append(" rmsTransferError4=").append(Math.sqrt(fineSquared/queries))
                .append(" rmsTransferError2=").append(Math.sqrt(coarseSquared/queries))
                .append(" dense32To64MaxDifference=").append(referenceError).append(" independentODE/midpoint256/constantSource\n");
    }
    private static double volumeBeamReference(float angle,float width,float aspect,int grid,org.joml.Vector3f normal) {
        double sum=0;
        for(int y=0;y<grid;y++)for(int x=0;x<grid;x++) {
            double ox=((x+.5)/grid-.5)*width,oy=((y+.5)/grid-.5)*width*aspect;
            var direction=new org.joml.Vector3d(Math.sin(angle)+Math.cos(angle)*ox,oy,-Math.cos(angle)+Math.sin(angle)*ox).normalize();
            var tangent=new org.joml.Vector3d(direction.x,direction.y,0).normalize();
            var path=new SchwarzschildPath(20,Math.acos(-direction.z),1e-12);
            double b=tangent.x*normal.x+tangent.y*normal.y;
            double orbitNormal=-tangent.y*normal.x+tangent.x*normal.y;
            double tau=SchwarzschildVolume.opticalDepth(path,normal.z,b,orbitNormal,.016F,.12,false,256);
            sum+=Math.exp(-tau);
        }
        return sum/(grid*grid);
    }
    private static void verifyVolumeNoise(StringBuilder report) {
        var shader=GpuSkyQuery.shader();shader.safeGetUniform("TestMode").set(18F);
        double maxValue=0,maxGradient=0;int count=0;
        for(float[] point:new float[][]{{.21F,.37F,.63F},{3.9F,7.2F,1.7F},{63.97F,63.98F,63.99F},
                {64.03F,64.02F,64.01F},{-64.37F,-1.7F,6.4F},{140.37F,88.21F,57.73F},
                {0F,.37F,.63F},{64F,.37F,.63F}}) {
            shader.safeGetUniform("TestDirection").set(point[0],point[1],point[2]);draw();float[] actual=read();
            double expected=PeriodicFlowNoise.sample(point[0],point[1],point[2]);
            double error=Math.abs(actual[3]-expected);maxValue=Math.max(maxValue,error);
            if(error>1e-6)throw new IllegalStateException("Packed volume lattice value mismatch: "+error);
            for(int axis=0;axis<3;axis++) {
                double[] a={point[0],point[1],point[2]},b=a.clone();a[axis]+=.00001;b[axis]-=.00001;
                double derivative=(PeriodicFlowNoise.sample(a[0],a[1],a[2])-PeriodicFlowNoise.sample(b[0],b[1],b[2]))/.00002;
                error=Math.abs(actual[axis]-derivative);maxGradient=Math.max(maxGradient,error);
                if(error>.00005)throw new IllegalStateException("Packed volume lattice gradient mismatch: "+error);
            }
            count++;
        }
        report.append("discGpuVolumeNoise points=").append(count).append(" maxValueError=").append(maxValue)
                .append(" maxGradientError=").append(maxGradient).append(" packedRGBA/negativeAndXYZWrap/independentCPUInterpolation\n");
    }
    private static void verifyNearObservers(StringBuilder report) {
        var shader=GpuSkyQuery.shader();int count=0;double maxRadius=0,maxDelay=0;
        try {
            for(float observer:new float[]{12,6}) {
                GpuSkyQuery.discObserverRadius=observer;GpuSkyQuery.bindDisc(shader);
                shader.safeGetUniform("TestMode").set(3F);
                double edge=SchwarzschildRay.shadowAngle(observer);
                for(double raw:new double[]{.03,edge-.00004,edge+.00004,.65,1.2,1.8,2.6}) {
                    float angle=(float)raw;var path=new SchwarzschildPath(observer,angle,1e-12);
                    shader.safeGetUniform("TestAngle").set(angle);
                    for(int i=1;i<100;i++) {
                        float phi=(float)(path.endAzimuth()*i/100);var expected=path.sample(phi);
                        if(expected.radius()<4 || expected.radius()>10)continue;
                        shader.safeGetUniform("TestSigma").set(phi);draw();float[] actual=read();
                        double dr=Math.abs(1/actual[0]-expected.radius()),dt=Math.abs(actual[2]-expected.delay());
                        maxRadius=Math.max(maxRadius,dr);maxDelay=Math.max(maxDelay,dt);count++;
                        if(actual[3]!=1 || dr>.005 || dt>.015)
                            throw new IllegalStateException("Near observer GPU trajectory mismatch at r="+observer+": "+dr+" / "+dt
                                    +" angle="+angle+" phi="+phi);
                    }
                }
                shader.safeGetUniform("TestMode").set(5F);
                shader.safeGetUniform("TestSigma").set(6F);shader.safeGetUniform("TestAngle").set(.3F);draw();
                if(Math.abs(read()[0]-SchwarzschildDisc.frequencyRatio(observer,6,.3))>2e-6)
                    throw new IllegalStateException("Near observer detector lapse mismatch");
            }
        } finally {GpuSkyQuery.discObserverRadius=20F;GpuSkyQuery.bindDisc(shader);}
        report.append("nearObserverGpuTimedQueries=").append(count).append(" radiiRs=12/6 maxRadiusErrorRs=")
                .append(maxRadius).append(" maxDelayErrorRsOverC=").append(maxDelay).append(" detectorLapse=verified\n");
    }
    private static void verifyMaterialContinuity(StringBuilder report) {
        var shader=GpuSkyQuery.shader();shader.safeGetUniform("TestMode").set(8F);
        int transitions=0,seams=0;double maxTransition=0,maxSeam=0;
        for(float radius:new float[]{4.8F,6F,8F,9.2F}) {
            shader.safeGetUniform("TestSigma").set(radius);
            for(float polar:new float[]{-2.5F,-.4F,1.1F}) {
                shader.safeGetUniform("TestAngle").set(polar);
                for(float time:new float[]{-70F,-35F,0F,35F,70F,105F,140F,175F,350F}) {
                    shader.safeGetUniform("DiscTime").set(time-.0001F);draw();float[] a=read();
                    shader.safeGetUniform("DiscTime").set(time+.0001F);draw();float[] b=read();
                    for(int k=0;k<4;k++) {
                        requireMaterial(a,k);requireMaterial(b,k);
                        double error=Math.abs(a[k]-b[k])/Math.max(1,Math.max(a[k],b[k]));
                        maxTransition=Math.max(maxTransition,error);
                        if(error>.0025)throw new IllegalStateException("Disc generation discontinuity at t="+time+" r="+radius+": "+error);
                    }
                    transitions++;
                }
            }
            for(float time:new float[]{0F,17.5F,35F,180F,400F,10000F}) {
                shader.safeGetUniform("DiscTime").set(time);
                shader.safeGetUniform("TestAngle").set((float)-Math.PI);draw();float[] a=read();
                shader.safeGetUniform("TestAngle").set((float)Math.PI);draw();float[] b=read();
                for(int k=0;k<4;k++) {
                    requireMaterial(a,k);requireMaterial(b,k);
                    double error=Math.abs(a[k]-b[k])/Math.max(1,Math.max(a[k],b[k]));
                    maxSeam=Math.max(maxSeam,error);
                    if(error>.0002)throw new IllegalStateException("Disc polar material seam at t="+time+": "+error);
                }
                seams++;
            }
        }
        report.append("discGpuMaterialContinuity transitions=").append(transitions).append(" seams=").append(seams)
                .append(" maxTransitionScaledError=").append(maxTransition).append(" maxSeamScaledError=").append(maxSeam).append('\n');
    }
    private static void verifyNoiseGradients(StringBuilder report) {
        var shader=GpuSkyQuery.shader();double maxValue=0,maxGradient=0;int points=0;
        for(float[] point:new float[][]{{.21F,.37F,.63F},{1.001F,2.999F,3.49F},{7.72F,31.41F,13.29F},
                {50.33F,73.61F,94.83F},{-7.3F,-1.2F,6.4F},{140.37F,88.21F,57.73F}}) {
            shader.safeGetUniform("TestMode").set(9F);
            shader.safeGetUniform("TestDirection").set(point[0],point[1],point[2]);draw();float[] analytic=read();
            shader.safeGetUniform("TestMode").set(10F);draw();float expected=read()[0];
            double error=Math.abs(analytic[3]-expected);maxValue=Math.max(maxValue,error);
            if(error>1e-6)throw new IllegalStateException("Disc gradient noise changed its value "+error);
            for(int axis=0;axis<3;axis++) {
                float[] plus=point.clone(),minus=point.clone();plus[axis]+=.005F;minus[axis]-=.005F;
                shader.safeGetUniform("TestDirection").set(plus[0],plus[1],plus[2]);draw();float a=read()[0];
                shader.safeGetUniform("TestDirection").set(minus[0],minus[1],minus[2]);draw();float b=read()[0];
                double finite=(a-b)/(plus[axis]-minus[axis]);error=Math.abs(analytic[axis]-finite);
                maxGradient=Math.max(maxGradient,error);
                if(error>.002)throw new IllegalStateException("Disc analytic noise gradient mismatch "+error);
            }
            points++;
        }
        report.append("discGpuNoiseGradient points=").append(points).append(" maxValueError=").append(maxValue)
                .append(" maxFiniteDifferenceError=").append(maxGradient).append('\n');
    }
    private static void verifyFiniteBeam(StringBuilder report) {
        var shader=GpuSkyQuery.shader();
        // This plane exposes higher-order images instead of hiding them behind
        // an opaque direct image. Dense CPU rays use independent ODE trajectories
        // and exact plane events, with no packed GPU table or edge coverage.
        var normal=new org.joml.Vector3f(.15F,(float)Math.sqrt(1-.15*.15-.7*.7),.7F).normalize();
        var axisU=new org.joml.Vector3f(0,0,1).fma(-normal.z,normal).normalize();
        var axisV=new org.joml.Vector3f(normal).cross(axisU).normalize();
        shader.safeGetUniform("DiscNormal").set(normal.x,normal.y,normal.z);
        shader.safeGetUniform("DiscU").set(axisU.x,axisU.y,axisU.z);
        shader.safeGetUniform("DiscV").set(axisV.x,axisV.y,axisV.z);
        shader.safeGetUniform("DiscDiagnostic").set(1F);
        shader.safeGetUniform("DiscBeamGrid").set(8F);
        shader.safeGetUniform("TestMode").set(11F);
        double edge=SchwarzschildRay.shadowAngle(20),maxError=0,squaredError=0,coarseSquaredError=0,referenceError=0;
        int queries=0,visible=0,partial=0;
        for(float[] beam:new float[][]{{-.0012F,.0012F,1F},{-.0005F,.0012F,1F},
                {-.0001F,.0012F,1F},{.0001F,.0012F,1F},{.0005F,.0012F,1F},{.0012F,.0012F,1F},
                {.0002F,.0024F,.5F},{-.0002F,.0024F,2F}}) {
            float angle=(float)(edge+beam[0]),width=beam[1],aspect=beam[2];
            shader.safeGetUniform("TestAngle").set(angle);
            shader.safeGetUniform("TestSigma").set(width);
            shader.safeGetUniform("TestAnisotropy").set(aspect);
            shader.safeGetUniform("DiscBeamGrid").set(4F);draw();float[] coarse=read();
            shader.safeGetUniform("DiscBeamGrid").set(8F);draw();float[] actual=read();
            double[] reference64=opaqueBeamReference(angle,width,aspect,64,normal,axisU,axisV);
            double[] reference=opaqueBeamReference(angle,width,aspect,128,normal,axisU,axisV);
            for(int k=0;k<4;k++) {
                if(!Float.isFinite(actual[k]) || actual[k]<0 || actual[k]>1)
                    throw new IllegalStateException("Non-finite/unbounded finite disc beam");
                double error=Math.abs(actual[k]-reference[k]);maxError=Math.max(maxError,error);
                squaredError+=error*error;
                coarseSquaredError+=Math.pow(coarse[k]-reference[k],2);
                referenceError=Math.max(referenceError,Math.abs(reference[k]-reference64[k]));
                if(error>.1)throw new IllegalStateException("Finite disc beam disagrees with dense CPU plane events: "
                        +error+" offset="+beam[0]+" width="+width+" aspect="+aspect+" actual="
                        +java.util.Arrays.toString(actual)+" reference="+java.util.Arrays.toString(reference));
            }
            if(reference[3]<1)visible++;
            if(reference[3]>.05 && reference[3]<.95)partial++;
            queries++;
        }
        if(visible<4 || partial<2)throw new IllegalStateException("Finite beam checks did not expose narrow/partial images");
        if(referenceError>.025 || squaredError>=coarseSquaredError)
            throw new IllegalStateException("Finite beam convergence check failed: referenceError="+referenceError
                    +" squaredError8="+squaredError+" squaredError4="+coarseSquaredError);
        shader.safeGetUniform("DiscDiagnostic").set(0F);shader.safeGetUniform("DiscBeamGrid").set(1F);
        report.append("discGpuFiniteBeams=").append(queries).append(" grid=8x8 denseCPUGrid=128x128 visible=")
                .append(visible).append(" partial=").append(partial).append(" maxAbsRGBAError=").append(maxError)
                .append(" rmsRGBAError=").append(Math.sqrt(squaredError/(queries*4)))
                .append(" coarse4x4RmsError=").append(Math.sqrt(coarseSquaredError/(queries*4)))
                .append(" dense64To128MaxDifference=").append(referenceError).append('\n');
    }
    private static double[] opaqueBeamReference(float angle,float width,float aspect,int grid,
            org.joml.Vector3f normal,org.joml.Vector3f axisU,org.joml.Vector3f axisV) {
        double[] sum=new double[4];
        for(int y=0;y<grid;y++)for(int x=0;x<grid;x++) {
            double ox=((x+.5)/grid-.5)*width,oy=((y+.5)/grid-.5)*width*aspect;
            var direction=new org.joml.Vector3d(Math.sin(angle)+Math.cos(angle)*ox,oy,
                    -Math.cos(angle)+Math.sin(angle)*ox).normalize();
            double theta=Math.acos(-direction.z);
            var tangent=new org.joml.Vector3d(direction.x,direction.y,0).normalize();
            var path=new SchwarzschildPath(20,theta,1e-12);
            var hits=SchwarzschildDisc.intersections(path,normal.z,tangent.x*normal.x+tangent.y*normal.y,4.63,9.35);
            if(hits.isEmpty()) {sum[3]+=1;continue;}
            var hit=hits.getFirst();
            var position=new org.joml.Vector3d(tangent).mul(Math.sin(hit.azimuth()));
            position.z=Math.cos(hit.azimuth());
            double polar=Math.atan2(position.x*axisV.x+position.y*axisV.y+position.z*axisV.z,
                    position.x*axisU.x+position.y*axisU.y+position.z*axisU.z);
            int sector=(int)Math.floor((polar+Math.PI)/(2*Math.PI)*12);
            int band=(int)Math.floor((hit.radius()-4.63)/(9.35-4.63)*6);
            if((sector+band)%2==0) {sum[0]+=.042;sum[1]+=.49;sum[2]+=.7;}
            else {sum[0]+=.7;sum[1]+=.21;sum[2]+=.028;}
        }
        for(int k=0;k<4;k++)sum[k]/=grid*grid;
        return sum;
    }
    private static void requireMaterial(float[] value,int channel) {
        if(!Float.isFinite(value[channel]) || value[channel]<0 || channel==3 && value[channel]>1)
            throw new IllegalStateException("Non-finite/unbounded disc radiative coefficient");
    }
    private static void draw() {
        RenderSystem.setShader(GpuSkyQuery::shader);
        // Bind explicitly: another fixture restored its VAO behind BufferUploader's immediate cache.
        triangle.bind();triangle.drawWithShader(new org.joml.Matrix4f(),new org.joml.Matrix4f(),GpuSkyQuery.shader());
    }
    private static float[] read() {float[] pixel=new float[4];GL11.glReadPixels(0,0,1,1,GL11.GL_RGBA,GL11.GL_FLOAT,pixel);return pixel;}
}
