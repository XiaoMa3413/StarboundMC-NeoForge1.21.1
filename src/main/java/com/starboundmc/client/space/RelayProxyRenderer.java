// SPDX-License-Identifier: MPL-2.0
package com.starboundmc.client.space;
import com.starboundmc.StarboundMC;
import com.starboundmc.encounter.*;
import com.starboundmc.world.ShipDimensions;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.RenderType;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;

/** Non-interactive distant hull/solar-array proxy converges on the reserved block-space anchor. */
@EventBusSubscriber(modid = StarboundMC.MODID, value = Dist.CLIENT)
public final class RelayProxyRenderer {
    private RelayProxyRenderer() { }
    @SubscribeEvent public static void render(RenderLevelStageEvent event) {
        var mc = Minecraft.getInstance(); var state = RelayClientState.snapshot;
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_ENTITIES || mc.level == null
                || !mc.level.dimension().equals(ShipDimensions.SHIP_LEVEL) || state == null
                || !(state.phase() == RelayData.Phase.APPROACHING.ordinal()
                || state.phase() == RelayData.Phase.ACTIVE.ordinal() && !mc.level.hasChunkAt(state.origin().offset(8, 4, 6)))) return;
        double extra = state.outsideCrew() > 0 ? 0 : Math.clamp(mc.level.getGameTime() - RelayClientState.receivedTick
                + event.getPartialTick().getGameTimeDeltaPartialTick(false), 0, 10);
        double t = state.phase() == RelayData.Phase.ACTIVE.ordinal() ? 1
                : Math.clamp((state.ticks() + extra) / RelayEncounter.APPROACH_TICKS, 0, 1);
        double distanceFactor = 1 + 7 * Math.pow(1 - t, 2);
        var target = RelayGeometry.center(state.origin());
        var proxy = RelayGeometry.SHIP_CENTER.add(target.subtract(RelayGeometry.SHIP_CENTER).scale(distanceFactor));
        var camera = event.getCamera().getPosition(); var pose = event.getPoseStack();
        pose.pushPose();
        pose.translate(proxy.x - camera.x - RelayGeometry.WIDTH / 2.0,
                proxy.y - camera.y - RelayGeometry.HEIGHT / 2.0, proxy.z - camera.z - RelayGeometry.DEPTH / 2.0);
        var buffers = mc.renderBuffers().bufferSource(); var type = RenderType.debugFilledBox(); var vertices = buffers.getBuffer(type);
        LevelRenderer.addChainedFilledBoxVertices(pose, vertices, 8, 4, 6, 23, 9, 25, .25f, .32f, .36f, 1);
        LevelRenderer.addChainedFilledBoxVertices(pose, vertices, 1, 7, 9, 8, 7.3, 22, .09f, .20f, .42f, 1);
        LevelRenderer.addChainedFilledBoxVertices(pose, vertices, 23, 7, 9, 30, 7.3, 22, .09f, .20f, .42f, 1);
        LevelRenderer.addChainedFilledBoxVertices(pose, vertices, 14, 9, 19, 16, 13, 21, .48f, .52f, .55f, 1);
        buffers.endBatch(type); pose.popPose();
    }
}
