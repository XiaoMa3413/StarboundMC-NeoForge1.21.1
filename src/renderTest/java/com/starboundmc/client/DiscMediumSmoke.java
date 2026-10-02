package com.starboundmc.client;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.starboundmc.client.space.DiscCoefficientLayout;
import com.starboundmc.client.space.PeriodicFlowNoise;
import com.starboundmc.client.space.StarPhotometry;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL30;

/** GPU coefficient charts against a separately evaluated CPU thermal closure. */
final class DiscMediumSmoke {
    private static final StarPhotometry.Color[] PALETTE=new StarPhotometry.Color[256];
    static {for(int i=0;i<256;i++)PALETTE[i]=StarPhotometry.blackbodyRadiance(1500+10500.0*i/255);}
    private DiscMediumSmoke() {}
    static String verify() {
        var state=SpaceRenderPassState.capture();int framebuffer=0;
        double maxBase=0,maxMip=0,maxEnergy=0,maxLookup=0,maxTransition=0,maxSeam=0;int points=0,guards=0,lookups=0,transitions=0,seams=0;
        try {
            for(int i=0;i<8;i++)DiscMediumCache.prepare(180F);
            var layout=DiscMediumCache.layout();
            if(layout==null || DiscMediumCache.texture()==0)throw new IllegalStateException("Coefficient cache unavailable");
            framebuffer=GL30.glGenFramebuffers();
            GlStateManager._glBindFramebuffer(GL30.GL_READ_FRAMEBUFFER,framebuffer);
            GL30.glFramebufferTexture2D(GL30.GL_READ_FRAMEBUFFER,GL30.GL_COLOR_ATTACHMENT0,
                    GL11.GL_TEXTURE_2D,DiscMediumCache.texture(),0);
            if(GL30.glCheckFramebufferStatus(GL30.GL_READ_FRAMEBUFFER)!=GL30.GL_FRAMEBUFFER_COMPLETE)
                throw new IllegalStateException("Coefficient readback framebuffer incomplete");
            int generation=4,bank=DiscCoefficientLayout.bank(generation);
            var shader=GpuSkyQuery.shader();DiscMediumCache.bind(shader,GpuSkyQuery.discTexture(),true);
            double[] baseEnergy=null;
            for(int level=0;level<layout.levels();level++) {
                int r=layout.radial(level),p=layout.azimuth(level),h=layout.height(level),width=layout.width(level);
                float[] data=new float[width*layout.rows(level)*4];
                GL11.glReadPixels(0,bank*layout.stride()+layout.offset(level),width,layout.rows(level),GL11.GL_RGBA,GL11.GL_FLOAT,data);
                double[] energy=new double[4];
                for(int z=0;z<h;z++)for(int y=0;y<p;y++)for(int x=0;x<r;x++) {
                    int pixel=(y+1)*width+z*(r+2)+x+1;
                    for(int c=0;c<4;c++) {
                        float v=data[pixel*4+c];if(!Float.isFinite(v) || v<0)throw new IllegalStateException("Invalid medium coefficient");
                        energy[c]+=v/(double)(r*p*h);
                    }
                }
                if(baseEnergy==null)baseEnergy=energy;
                else for(int c=0;c<4;c++) {
                    double error=Math.abs(energy[c]-baseEnergy[c])/Math.max(.01,baseEnergy[c]);
                    maxEnergy=Math.max(maxEnergy,error);
                    if(error>.005)throw new IllegalStateException("Coefficient mip lost linear energy: "+error);
                }
                for(int z=0;z<h;z++) {
                    for(int y=1;y<=p;y++)for(int c=0;c<4;c++) {
                        exact(data,((y*width+z*(r+2))*4+c),((y*width+z*(r+2)+1)*4+c));
                        exact(data,((y*width+z*(r+2)+r+1)*4+c),((y*width+z*(r+2)+r)*4+c));guards+=2;
                    }
                    for(int x=0;x<r+2;x++)for(int c=0;c<4;c++) {
                        exact(data,((z*(r+2)+x)*4+c),((p*width+z*(r+2)+x)*4+c));
                        exact(data,(((p+1)*width+z*(r+2)+x)*4+c),((width+z*(r+2)+x)*4+c));guards+=2;
                    }
                }
                if(level<=2)for(int z:new int[]{0,h/2,h-1})for(int x:new int[]{1,r/2,r-2}) {
                    int y=p/3;double[] reference=voxel(layout,level,x,y,z,generation);
                    int pixel=(y+1)*width+z*(r+2)+x+1;
                    for(int c=0;c<4;c++) {
                        double error=Math.abs(data[pixel*4+c]-reference[c])/Math.max(.05,reference[c]);
                        if(level==0)maxBase=Math.max(maxBase,error);else maxMip=Math.max(maxMip,error);
                        if(error>.025)throw new IllegalStateException("Baked coefficient disagrees with CPU nonlinear closure level="+level+" c="+c+" error="+error);
                    }
                    points++;
                }
                // Validate the production sampler coordinates, including the
                // periodic chart seam and both height/radial clamp boundaries.
                shader.safeGetUniform("TestMode").set(21F);shader.safeGetUniform("TestSigma").set((float)level);
                shader.safeGetUniform("TestAngle").set((float)bank);
                GlStateManager._glBindFramebuffer(GL30.GL_READ_FRAMEBUFFER,GL11.glGetInteger(GL30.GL_DRAW_FRAMEBUFFER_BINDING));
                for(float[] coordinate:new float[][]{{.0001F,0,0},{.27F,.37F,.55F},{.99F,1,1}}) {
                    shader.safeGetUniform("TestDirection").set(coordinate[0],coordinate[1],coordinate[2]);
                    AccretionDiscSmoke.draw();float[] actual=AccretionDiscSmoke.read();
                    double[] expected=sample(data,r,p,h,coordinate);
                    for(int c=0;c<4;c++) {
                        double error=Math.abs(actual[c]-expected[c])/Math.max(.05,expected[c]);maxLookup=Math.max(maxLookup,error);
                        if(error>.004)throw new IllegalStateException("Coefficient shader lookup mismatch: "+error);
                    }
                    lookups++;
                }
                GlStateManager._glBindFramebuffer(GL30.GL_READ_FRAMEBUFFER,framebuffer);
            }
            for(int i=0;i<8;i++)DiscMediumCache.prepare(-140F);
            var negative=new float[4];
            GL11.glReadPixels(1,DiscCoefficientLayout.bank(-4)*layout.stride()+1,1,1,GL11.GL_RGBA,GL11.GL_FLOAT,negative);
            double[] reference=voxel(layout,0,0,0,0,-4);
            for(int c=0;c<4;c++)if(Math.abs(negative[c]-reference[c])/Math.max(.05,reference[c])>.025)
                throw new IllegalStateException("Negative retarded generation mapped to wrong cache bank");
            GlStateManager._glBindFramebuffer(GL30.GL_READ_FRAMEBUFFER,GL11.glGetInteger(GL30.GL_DRAW_FRAMEBUFFER_BINDING));
            for(float time:new float[]{-140,-105,-70,-35,0,35,70,105,140,175,350}) {
                for(int i=0;i<8;i++)DiscMediumCache.prepare(time);
                DiscMediumCache.bind(shader,GpuSkyQuery.discTexture(),true);
                // Isolate the finite-lived field's algebra from the separate
                // wall-clock startup fade. Runtime animation measures that fade.
                shader.safeGetUniform("DiscCacheGains0").set(1F,1F,1F,1F);
                shader.safeGetUniform("DiscCacheGains1").set(1F,1F,1F,1F);
                shader.safeGetUniform("TestMode").set(22F);
                for(float radius:new float[]{4.8F,6F,8F,9.2F})for(float height:new float[]{-2F,0F,2F}) {
                    shader.safeGetUniform("TestSigma").set(radius);shader.safeGetUniform("TestAnisotropy").set(height);
                    shader.safeGetUniform("TestAngle").set(.37F);
                    shader.safeGetUniform("DiscTime").set(time-.0001F);AccretionDiscSmoke.draw();float[] a=AccretionDiscSmoke.read();
                    shader.safeGetUniform("DiscTime").set(time+.0001F);AccretionDiscSmoke.draw();float[] b=AccretionDiscSmoke.read();
                    maxTransition=Math.max(maxTransition,difference(a,b,.0025));transitions++;
                    shader.safeGetUniform("DiscTime").set(time);
                    shader.safeGetUniform("TestAngle").set((float)-Math.PI);AccretionDiscSmoke.draw();a=AccretionDiscSmoke.read();
                    shader.safeGetUniform("TestAngle").set((float)Math.PI);AccretionDiscSmoke.draw();b=AccretionDiscSmoke.read();
                    maxSeam=Math.max(maxSeam,difference(a,b,.0002));seams++;
                }
            }
        } finally {
            if(framebuffer!=0)GL30.glDeleteFramebuffers(framebuffer);state.restore();
        }
        return "discCoefficientGpuVoxels="+points+" maxBaseScaledError="+maxBase+" maxMipScaledError="+maxMip
                +" guardComparisons="+guards+" maxMipLinearEnergyError="+maxEnergy+" shaderLookups="+lookups
                +" maxLookupScaledError="+maxLookup+" negativeGeneration=verified transitions="+transitions+" seams="+seams
                +" maxTransitionScaledError="+maxTransition+" maxSeamScaledError="+maxSeam+" "+DiscMediumCache.diagnostics()+"\n";
    }
    private static double difference(float[] a,float[] b,double tolerance) {
        double maximum=0;
        for(int i=0;i<4;i++) {
            if(!Float.isFinite(a[i]) || !Float.isFinite(b[i]) || a[i]<0 || b[i]<0)
                throw new IllegalStateException("Invalid cached nonlinear coefficient");
            double error=Math.abs(a[i]-b[i])/Math.max(1,Math.max(a[i],b[i]));maximum=Math.max(maximum,error);
            if(error>tolerance)throw new IllegalStateException("Cached field continuity exceeded tolerance: "+error);
        }
        return maximum;
    }
    private static double[] sample(float[] data,int r,int p,int h,float[] f) {
        double rx=Math.clamp(f[0]*r-.5,0,r-1),py=f[1]*p-.5,hz=Math.clamp(f[2]*h-.5,0,h-1);
        int x=(int)Math.floor(rx),y=(int)Math.floor(py),z=(int)Math.floor(hz),width=(r+2)*h;
        double[] sum=new double[4];
        for(int k=0;k<2;k++)for(int j=0;j<2;j++)for(int i=0;i<2;i++) {
            int pixel=(Math.floorMod(y+j,p)+1)*width+Math.min(h-1,z+k)*(r+2)+Math.min(r-1,x+i)+1;
            double weight=(i==0?1-rx+x:rx-x)*(j==0?1-py+y:py-y)*(k==0?1-hz+z:hz-z);
            for(int c=0;c<4;c++)sum[c]+=data[pixel*4+c]*weight;
        }
        return sum;
    }
    private static void exact(float[] data,int a,int b) {
        if(data[a]!=data[b])throw new IllegalStateException("Medium chart guard differs from its intended source: "+a+"="+data[a]+" / "+b+"="+data[b]);
    }
    private static double[] voxel(DiscCoefficientLayout layout,int level,int x,int y,int z,int generation) {
        int span=1<<level,rs=Math.min(span,layout.radial()),ps=Math.min(span,layout.azimuth()),hs=Math.min(span,layout.height());
        double[] sum=new double[4];
        for(int k=0;k<hs;k++)for(int j=0;j<ps;j++)for(int i=0;i<rs;i++)
            for(int sz=0;sz<2;sz++)for(int sy=0;sy<2;sy++)for(int sx=0;sx<2;sx++) {
                double r=4.63+(9.35-4.63)*(x*rs+i+(sx+.5)*.5)/layout.radial();
                double p=2*Math.PI*(y*ps+j+(sy+.5)*.5)/layout.azimuth();
                double h=-3+6.0*(z*hs+k+(sz+.5)*.5)/layout.height();
                double[] v=coefficient(r,p,h,generation);
                for(int c=0;c<4;c++)sum[c]+=v[c]/(rs*ps*hs*8.0);
            }
        return sum;
    }
    private static double[] coefficient(double r,double phase,double h,int generation) {
        double sx=17.8+5.7*generation+.35*h,sy=41.2+13.1*generation+.9*h,sz=6.3+3.9*generation+.55*h;
        double x=Math.cos(phase),y=Math.sin(phase);
        double broad=PeriodicFlowNoise.sample(r*.65+sx,x*3.3+sy,y*3.3+sz);
        double ribbon=PeriodicFlowNoise.sample(r*2.7+(broad-.5)*3.3+sx+37.1,x*7.3+sy+37.1,y*7.3+sz+37.1);
        double fine=PeriodicFlowNoise.sample(r*9.7+(ribbon-.5)*5+sx+71.3,x*17.3+sy+71.3,y*17.3+sz+71.3);
        double rib=Math.max((ribbon-.16)/.84,0),density=Math.clamp(.006+6*Math.pow(rib,3)*Math.pow(broad,1.3)+2*Math.pow(fine,5)*broad,.006,2);
        double t=Math.clamp(3350*Math.pow(4.63/r,.25)+2600*smooth(.15,.95,density)+(broad-.5)*1700,2100,6900)*(1-.08*Math.min(h*h/9,1));
        double kappa=density*smooth(4.63,4.71,r)*(1-smooth(9.2,9.35,r));
        double p=(t-1500)/10500*255;int lo=(int)p;double f=p-lo;
        var a=PALETTE[lo];var b=PALETTE[Math.min(255,lo+1)];
        return new double[]{(a.red()+(b.red()-a.red())*f)*3.25*kappa,(a.green()+(b.green()-a.green())*f)*3.25*kappa,
                (a.blue()+(b.blue()-a.blue())*f)*3.25*kappa,kappa};
    }
    private static double smooth(double a,double b,double v) {double t=Math.clamp((v-a)/(b-a),0,1);return t*t*(3-2*t);}
}
