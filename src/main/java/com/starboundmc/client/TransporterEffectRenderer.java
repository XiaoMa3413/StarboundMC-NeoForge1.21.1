package com.starboundmc.client;

import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.starboundmc.StarboundMC;
import com.starboundmc.network.TransporterEffectPacket;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.RenderStateShard;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.BlockPos;
import com.starboundmc.block.TransporterBlock;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import org.joml.Matrix4f;

import java.util.ArrayDeque;

/** Short additive filaments and paired emitter flashes, with no persistent block entity. */
@EventBusSubscriber(modid = StarboundMC.MODID, value = Dist.CLIENT)
public final class TransporterEffectRenderer {
    private static final int DURATION = 22;
    private static final ArrayDeque<Effect> EFFECTS = new ArrayDeque<>();
    private static ClientLevel world;
    private static final RenderType LIGHT = RenderType.create("transporter_light", DefaultVertexFormat.POSITION_COLOR,
            VertexFormat.Mode.QUADS, 8192, false, false, RenderType.CompositeState.builder()
                    .setShaderState(RenderStateShard.RENDERTYPE_LIGHTNING_SHADER)
                    .setTransparencyState(RenderStateShard.LIGHTNING_TRANSPARENCY)
                    .setDepthTestState(RenderStateShard.LEQUAL_DEPTH_TEST)
                    .setCullState(RenderStateShard.NO_CULL)
                    .setWriteMaskState(RenderStateShard.COLOR_WRITE).createCompositeState(false));
    private static final class Effect {
        final Vec3 position;
        final boolean device, arrival;
        final long received;
        long start = -1;
        Effect(TransporterEffectPacket packet) {
            position = packet.position(); device = packet.device(); arrival = packet.arrival();
            received = System.nanoTime();
        }
    }
    private TransporterEffectRenderer() {}

    public static void receive(TransporterEffectPacket packet) {
        var level = Minecraft.getInstance().level;
        if (level != world) { EFFECTS.clear(); world = level; }
        if (level == null || !level.dimension().location().equals(packet.dimension())
                || !Double.isFinite(packet.position().x) || !Double.isFinite(packet.position().y)
                || !Double.isFinite(packet.position().z)) return;
        while (EFFECTS.size() >= 32) EFFECTS.removeFirst();
        EFFECTS.add(new Effect(packet));
    }

    @SubscribeEvent public static void logout(ClientPlayerNetworkEvent.LoggingOut event) { EFFECTS.clear(); world = null; }

    @SubscribeEvent public static void render(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_ENTITIES) return;
        var mc = Minecraft.getInstance();
        if (mc.level != world) { EFFECTS.clear(); world = mc.level; }
        if (world == null || EFFECTS.isEmpty()) return;
        double now = world.getGameTime() + event.getPartialTick().getGameTimeDeltaPartialTick(false);
        EFFECTS.removeIf(e -> (e.start >= 0 && now - e.start >= DURATION)
                || System.nanoTime() - e.received > 10_000_000_000L);
        var pose = event.getPoseStack();
        var buffers = mc.renderBuffers().bufferSource();
        VertexConsumer out = buffers.getBuffer(LIGHT);
        Vec3 camera = event.getCamera().getPosition();
        for (var effect : EFFECTS) {
            if (effect.position.distanceToSqr(camera) > 64*64) continue;
            if (effect.start < 0) {
                BlockPos pos = BlockPos.containing(effect.position);
                if (!world.hasChunk(pos.getX() >> 4, pos.getZ() >> 4) || mc.getOverlay() != null) continue;
                if (effect.device && !(world.getBlockState(pos).getBlock() instanceof TransporterBlock)) continue;
                if (effect.device && (!mc.levelRenderer.isSectionCompiled(pos)
                        || !mc.levelRenderer.isSectionCompiled(pos.above(2)))) continue;
                effect.start = world.getGameTime() + (effect.arrival ? 4 : 0);
            }
            if (now < effect.start) continue;
            double age = (now - effect.start) / DURATION;
            float alpha = (float)(Math.sin(Math.PI * Math.min(1, age * 4)) * .22 + (1-age)*.38);
            pose.pushPose();
            pose.translate(effect.position.x-camera.x, effect.position.y-camera.y, effect.position.z-camera.z);
            var matrix = pose.last().pose();
            if (effect.device) { ring(out,matrix,.004,.398,.022,alpha); ring(out,matrix,2.091,.398,.022,alpha); }
            double sweep = (effect.arrival ? 1-age : age)*1.9;
            ring(out,matrix,sweep,.29,.009,alpha*.7f);
            for (int i=0;i<18;i++) {
                double a=i*Math.PI*2/18, radius=.23+.05*Math.sin(i*2.4);
                double x=Math.cos(a)*radius,z=Math.sin(a)*radius;
                double lo=Math.max(.025, sweep-.5-(i%3)*.1),hi=Math.min(1.98,sweep+.55+(i%4)*.07);
                double w=.0035,dx=Math.cos(a)*w,dz=Math.sin(a)*w;
                quad(out,matrix,x-dx,lo,z-dz,x+dx,lo,z+dz,x+dx,hi,z+dz,x-dx,hi,z-dz,alpha);
                quad(out,matrix,x-dz,lo,z+dx,x+dz,lo,z-dx,x+dz,hi,z-dx,x-dz,hi,z+dx,alpha);
            }
            for (int i=0;i<34;i++) {
                double a=i*2.39996, radius=.1+.22*fract(i*.61803);
                double y=fract(i*.38197+age*(effect.arrival?-.23:.23))*1.9;
                float spark=(float)(Math.pow(Math.max(0,Math.sin(i*2.13+age*22)),3)*(1-age));
                double size=.008+.006*fract(i*.741);
                pose.pushPose();
                pose.translate(Math.cos(a)*radius,y,Math.sin(a)*radius);
                pose.mulPose(event.getCamera().rotation());
                quad(out,pose.last().pose(),-size,0,0,0,-size*1.6,0,size,0,0,0,size*1.6,0,spark);
                pose.popPose();
            }
            pose.popPose();
        }
        buffers.endBatch(LIGHT);
    }
    private static double fract(double value) { return value - Math.floor(value); }

    private static void ring(VertexConsumer out, Matrix4f m, double y, double radius, double width, float a) {
        for(int i=0;i<48;i++) {
            double t=i*Math.PI/24,n=(i+1)*Math.PI/24,r=radius-width;
            quad(out,m,Math.cos(t)*r,y,Math.sin(t)*r,Math.cos(t)*radius,y,Math.sin(t)*radius,
                    Math.cos(n)*radius,y,Math.sin(n)*radius,Math.cos(n)*r,y,Math.sin(n)*r,a);
        }
    }
    private static void quad(VertexConsumer out, Matrix4f m,
                             double x0,double y0,double z0,double x1,double y1,double z1,
                             double x2,double y2,double z2,double x3,double y3,double z3,float a) {
        vertex(out,m,x0,y0,z0,a);vertex(out,m,x1,y1,z1,a);vertex(out,m,x2,y2,z2,a);vertex(out,m,x3,y3,z3,a);
    }
    private static void vertex(VertexConsumer out, Matrix4f m,double x,double y,double z,float a) {
        out.addVertex(m,(float)x,(float)y,(float)z).setColor(.48f,.82f,1f,a);
    }
}
