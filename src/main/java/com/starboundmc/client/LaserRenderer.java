package com.starboundmc.client;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.starboundmc.StarboundMC;
import com.starboundmc.item.MatterManipulatorItem;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import org.joml.Matrix4f;

import java.util.Random;

/**
 * Renders the matter manipulator's mining laser while the use key is held
 * (with a short afterglow so single clicks are visible). The beam originates
 * from the handheld gun's muzzle (approximated from the first-person item
 * position), highlights the targeted block's outline and sprays impact
 * sparks. Also suppresses the arm-swing animation while the manipulator is in
 * hand — it is a laser tool, not a melee weapon.
 */
@EventBusSubscriber(modid = StarboundMC.MODID, value = Dist.CLIENT)
public class LaserRenderer
{
    private static final long BEAM_LINGER_MS = 250L;
    private static long lastFired = 0;
    private static final Random RANDOM = new Random();

    // Client-side crack overlay: mirrors the server's charge progress with the
    // same destroy-progress formula, so the targeted block visibly cracks
    // while the beam is held (the server stays authoritative for the break).
    private static BlockPos crackPos = null;
    private static BlockState crackState = null;
    private static float crackProgress = 0.0F;

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event)
    {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null)
            return;
        boolean firing = mc.player.isUsingItem()
                && mc.player.getMainHandItem().getItem() instanceof MatterManipulatorItem;
        if (firing)
        {
            lastFired = System.currentTimeMillis();
            // Sparse sparks: only every other tick, one particle.
            if ((mc.level.getGameTime() & 1) == 0)
            {
                spawnSparks(mc.player);
            }
            updateCrack(mc);
        }
        else
        {
            clearCrack();
        }
    }

    /** Advance the client-side crack overlay while the beam is held. */
    private static void updateCrack(Minecraft mc)
    {
        ItemStack stack = mc.player.getMainHandItem();
        double range = MatterManipulatorItem.LASER_RANGE_BASE
                + MatterManipulatorItem.getRangeLevel(stack) * MatterManipulatorItem.LASER_RANGE_PER_LEVEL;
        HitResult rawHit = mc.player.pick(range, 0.0F, false);
        if (rawHit.getType() != HitResult.Type.BLOCK)
        {
            clearCrack();
            return;
        }
        BlockHitResult hit = (BlockHitResult) rawHit;
        BlockPos pos = hit.getBlockPos();
        BlockState state = mc.level.getBlockState(pos);
        if (state.isAir() || state.getDestroySpeed(mc.level, pos) < 0.0F)
        {
            clearCrack();
            return;
        }
        if (!pos.equals(crackPos) || state != crackState)
        {
            clearCrack();
            crackPos = pos;
            crackState = state;
            crackProgress = 0.0F;
        }
        // Same formula as the server's charge tick: laser dig speed / hardness
        // / 30|100 correct-tool factor, ×LASER_PROGRESS_MULTIPLIER — without
        // the vanilla player-environment modifiers (airborne/underwater).
        float hardness = state.getDestroySpeed(mc.level, pos);
        boolean correct = !state.requiresCorrectToolForDrops()
                || MatterManipulatorItem.isCorrectToolForDrops(state, MatterManipulatorItem.getMiningLevel(stack));
        float factor = correct ? 30.0F : 100.0F;
        crackProgress += (MatterManipulatorItem.laserDigSpeed(stack, state) / hardness / factor)
                * MatterManipulatorItem.LASER_PROGRESS_MULTIPLIER;
        int stage = Math.min(9, Math.max(0, (int) (crackProgress * 10.0F)));
        mc.levelRenderer.destroyBlockProgress(mc.player.getId(), pos, stage);
    }

    /** Remove the crack overlay from the previously targeted block. */
    private static void clearCrack()
    {
        if (crackPos == null)
            return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.level != null)
        {
            mc.levelRenderer.destroyBlockProgress(mc.player.getId(), crackPos, -1);
        }
        crackPos = null;
        crackState = null;
        crackProgress = 0.0F;
    }

    /** Suppress the melee swing animation: the manipulator fires a laser, it never swings. */
    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event)
    {
        Player player = event.getEntity();
        if (!player.level().isClientSide)
            return;
        if (player.getMainHandItem().getItem() instanceof MatterManipulatorItem)
        {
            player.swinging = false;
            player.swingTime = 0;
        }
    }

    /** Small glowing sparks at the impact point while firing (sparse: one per tick). */
    private static void spawnSparks(Player player)
    {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null)
            return;
        double range = MatterManipulatorItem.LASER_RANGE_BASE
                + MatterManipulatorItem.getRangeLevel(player.getMainHandItem()) * MatterManipulatorItem.LASER_RANGE_PER_LEVEL;
        HitResult rawHit = player.pick(range, 0.0F, false);
        if (rawHit.getType() != HitResult.Type.BLOCK)
            return;
        Vec3 p = rawHit.getLocation();
        double vx = (RANDOM.nextDouble() - 0.5) * 0.16;
        double vy = RANDOM.nextDouble() * 0.22;
        double vz = (RANDOM.nextDouble() - 0.5) * 0.16;
        mc.level.addParticle(ParticleTypes.END_ROD, p.x, p.y, p.z, vx, vy, vz);
    }

    @SubscribeEvent
    public static void onRenderLevel(RenderLevelStageEvent event)
    {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_TRANSLUCENT_BLOCKS)
            return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null || mc.screen != null)
            return;
        ItemStack stack = mc.player.getMainHandItem();
        if (!(stack.getItem() instanceof MatterManipulatorItem))
            return;

        long elapsed = System.currentTimeMillis() - lastFired;
        if (elapsed > BEAM_LINGER_MS)
            return;
        float alpha = elapsed < 150 ? 1.0F : Math.max(0.0F, 1.0F - (elapsed - 150) / 100.0F);

        double range = MatterManipulatorItem.LASER_RANGE_BASE
                + MatterManipulatorItem.getRangeLevel(stack) * MatterManipulatorItem.LASER_RANGE_PER_LEVEL;
        float partialTick = event.getPartialTick().getGameTimeDeltaPartialTick(false);
        HitResult rawHit = mc.player.pick(range, partialTick, false);
        if (rawHit.getType() != HitResult.Type.BLOCK)
            return;
        BlockHitResult hit = (BlockHitResult) rawHit;

        // Muzzle: approximate the first-person right-hand item position — the
        // beam must appear to come out of the gun, not the eye.
        Vec3 eye = mc.player.getEyePosition(partialTick);
        Vec3 look = mc.player.getLookAngle();
        Vec3 up = new Vec3(0.0, 1.0, 0.0);
        Vec3 right = look.cross(up);
        if (right.lengthSqr() < 1.0E-6)
            right = new Vec3(1.0, 0.0, 0.0);
        right = right.normalize();
        Vec3 muzzle = eye.add(look.scale(0.42)).add(right.scale(0.30)).subtract(0.0, 0.22, 0.0);
        // Very close targets would sit "behind" the muzzle; fall back to the eye.
        if (hit.getLocation().subtract(muzzle).dot(look) < 0.15)
        {
            muzzle = eye.subtract(0.0, 0.05, 0.0);
        }

        Camera cam = event.getCamera();
        Vec3 camPos = cam.getPosition();
        Vec3 start = muzzle.subtract(camPos);
        Vec3 end = hit.getLocation().subtract(camPos);
        renderBeam(event.getPoseStack(), start, end, alpha);
        renderBlockOutline(event.getPoseStack(), hit, camPos, alpha);
    }

    private static void renderBeam(PoseStack pose, Vec3 start, Vec3 end, float alpha)
    {
        Vec3 dir = end.subtract(start).normalize();
        Vec3 right = dir.cross(new Vec3(0.0, 1.0, 0.0));
        if (right.lengthSqr() < 1.0E-6)
            right = dir.cross(new Vec3(1.0, 0.0, 0.0));
        right = right.normalize();

        float w = 0.06F;
        Vec3 p1 = start.add(right.scale(w));
        Vec3 p2 = start.subtract(right.scale(w));
        Vec3 p3 = end.subtract(right.scale(w));
        Vec3 p4 = end.add(right.scale(w));

        Matrix4f matrix = pose.last().pose();
        RenderSystem.enableBlend();
        RenderSystem.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE);
        RenderSystem.disableCull();
        RenderSystem.depthMask(false);
        RenderSystem.setShader(GameRenderer::getPositionColorShader);

        BufferBuilder bb = Tesselator.getInstance().begin(
                VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);

        vertex(bb, matrix, p1, 0.55F, 0.9F, 1.0F, alpha * 0.8F);
        vertex(bb, matrix, p2, 0.55F, 0.9F, 1.0F, alpha * 0.8F);
        vertex(bb, matrix, p3, 0.55F, 0.9F, 1.0F, alpha);
        vertex(bb, matrix, p4, 0.55F, 0.9F, 1.0F, alpha);

        // Bright tip at the impact point.
        Vec3 t1 = end.add(right.scale(0.12F));
        Vec3 t2 = end.subtract(right.scale(0.12F));
        vertex(bb, matrix, t1, 1.0F, 1.0F, 1.0F, alpha);
        vertex(bb, matrix, t2, 1.0F, 1.0F, 1.0F, alpha);
        vertex(bb, matrix, t2.add(dir.scale(0.18F)), 1.0F, 1.0F, 1.0F, 0.0F);
        vertex(bb, matrix, t1.add(dir.scale(0.18F)), 1.0F, 1.0F, 1.0F, 0.0F);

        BufferUploader.drawWithShader(bb.buildOrThrow());

        RenderSystem.depthMask(true);
        RenderSystem.enableCull();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableBlend();
    }

    /** Cyan block-outline highlight on the targeted block. */
    private static void renderBlockOutline(PoseStack pose, BlockHitResult hit, Vec3 camPos, float alpha)
    {
        BlockPos pos = hit.getBlockPos();
        BlockState state = Minecraft.getInstance().level.getBlockState(pos);
        AABB box = state.getShape(Minecraft.getInstance().level, pos).bounds().move(pos);

        Vec3[] corners = new Vec3[] {
                new Vec3(box.minX - camPos.x, box.minY - camPos.y, box.minZ - camPos.z),
                new Vec3(box.maxX - camPos.x, box.minY - camPos.y, box.minZ - camPos.z),
                new Vec3(box.maxX - camPos.x, box.minY - camPos.y, box.maxZ - camPos.z),
                new Vec3(box.minX - camPos.x, box.minY - camPos.y, box.maxZ - camPos.z),
                new Vec3(box.minX - camPos.x, box.maxY - camPos.y, box.minZ - camPos.z),
                new Vec3(box.maxX - camPos.x, box.maxY - camPos.y, box.minZ - camPos.z),
                new Vec3(box.maxX - camPos.x, box.maxY - camPos.y, box.maxZ - camPos.z),
                new Vec3(box.minX - camPos.x, box.maxY - camPos.y, box.maxZ - camPos.z)
        };
        int[][] edges = { { 0, 1 }, { 1, 2 }, { 2, 3 }, { 3, 0 }, { 4, 5 }, { 5, 6 }, { 6, 7 }, { 7, 4 },
                { 0, 4 }, { 1, 5 }, { 2, 6 }, { 3, 7 } };

        Matrix4f matrix = pose.last().pose();
        RenderSystem.enableBlend();
        RenderSystem.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE);
        RenderSystem.disableDepthTest();
        RenderSystem.disableCull();
        RenderSystem.depthMask(false);
        RenderSystem.setShader(GameRenderer::getPositionColorShader);

        BufferBuilder bb = Tesselator.getInstance().begin(
                VertexFormat.Mode.LINES, DefaultVertexFormat.POSITION_COLOR);
        for (int[] edge : edges)
        {
            Vec3 a = corners[edge[0]];
            Vec3 b = corners[edge[1]];
            vertex(bb, matrix, a, 0.55F, 0.9F, 1.0F, alpha * 0.9F);
            vertex(bb, matrix, b, 0.55F, 0.9F, 1.0F, alpha * 0.9F);
        }
        BufferUploader.drawWithShader(bb.buildOrThrow());

        RenderSystem.depthMask(true);
        RenderSystem.enableCull();
        RenderSystem.enableDepthTest();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableBlend();
    }

    private static void vertex(BufferBuilder bb, Matrix4f matrix, Vec3 p, float r, float g, float b, float a)
    {
        bb.addVertex(matrix, (float) p.x, (float) p.y, (float) p.z).setColor(r, g, b, a);
    }
}
