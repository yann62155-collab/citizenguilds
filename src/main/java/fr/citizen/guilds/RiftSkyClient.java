package fr.citizen.guilds;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.util.Mth;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;

/** Client-only full-sky dimensional tear; independent of chunks and world coordinates. */
@EventBusSubscriber(modid = CitizenGuilds.MOD_ID, value = Dist.CLIENT)
public final class RiftSkyClient {
    private static volatile boolean active;
    private RiftSkyClient() {}

    public static void setActive(boolean value) { active = value; }

    @SubscribeEvent
    public static void renderSkyTear(RenderLevelStageEvent event) {
        if (!active || event.getStage() != RenderLevelStageEvent.Stage.AFTER_SKY) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null || mc.level.dimension() != net.minecraft.world.level.Level.OVERWORLD) return;

        PoseStack pose = event.getPoseStack();
        MultiBufferSource.BufferSource buffers = mc.renderBuffers().bufferSource();
        VertexConsumer lines = buffers.getBuffer(RenderType.lines());
        float time = (mc.level.getGameTime() + event.getPartialTick().getGameTimeDeltaPartialTick(false)) * 0.025F;

        pose.pushPose();
        // The tear is drawn in camera space, high above and far beyond the player.
        pose.mulPose(event.getCamera().rotation());
        pose.translate(0.0D, 34.0D, -92.0D);
        pose.mulPose(Axis.XP.rotation(0.12F));
        drawTear(pose, lines, time);
        pose.popPose();
        buffers.endBatch(RenderType.lines());
    }

    private static void drawTear(PoseStack pose, VertexConsumer out, float time) {
        PoseStack.Pose p = pose.last();
        final int segments = 100;
        float[] topX = new float[segments + 1];
        float[] topY = new float[segments + 1];
        float[] botX = new float[segments + 1];
        float[] botY = new float[segments + 1];

        // Jagged horizontal split, opening and breathing with the same global clock for every player.
        for (int i = 0; i <= segments; i++) {
            float t = i / (float)segments * 2.0F - 1.0F;
            float x = t * 54.0F;
            float wave = Mth.sin(t * 13.0F + time * 1.8F) * 2.3F
                + Mth.sin(t * 31.0F - time * 1.2F) * 0.95F;
            float jag = Mth.sin(i * 73.1F + time * 4.0F) * 0.7F;
            float half = (1.4F + 3.0F * (0.5F + 0.5F * Mth.sin(time * 1.35F + t * 4.0F)))
                * (0.35F + 0.65F * (1.0F - Math.abs(t) * 0.55F));
            topX[i] = x; topY[i] = wave + jag + half;
            botX[i] = x; botY[i] = wave + jag - half;
        }

        // Dark fractured membrane: irregular longitudinal seams and cross fractures.
        for (int row = 0; row < 7; row++) {
            float f = row / 6.0F;
            for (int i = 0; i < segments; i++) {
                float x0 = topX[i], x1 = topX[i + 1];
                float y0 = Mth.lerp(f, topY[i], botY[i]);
                float y1 = Mth.lerp(f, topY[i + 1], botY[i + 1]);
                float shade = 0.20F + 0.13F * ((row + i) % 3);
                line(out, p, x0, y0, 0.0F, x1, y1, 0.0F, shade, 0.035F, 0.25F, 0.82F);
            }
        }
        for (int i = 4; i < segments - 4; i += 5) {
            line(out, p, topX[i], topY[i], -0.02F, botX[i + 2], botY[i + 2], -0.02F,
                0.28F, 0.035F, 0.35F, 0.9F);
        }

        // Two torn, luminous edges: violet core with cyan-white outer filaments.
        for (int i = 0; i < segments; i++) {
            line(out, p, topX[i], topY[i], -0.12F, topX[i + 1], topY[i + 1], -0.12F, 0.75F, 0.08F, 1.0F, 1.0F);
            line(out, p, botX[i], botY[i], -0.12F, botX[i + 1], botY[i + 1], -0.12F, 0.66F, 0.10F, 1.0F, 1.0F);
            if (i % 2 == 0) {
                line(out, p, topX[i], topY[i] + .22F, -0.16F, topX[i + 1], topY[i + 1] + .22F, -0.16F, 0.12F, 0.72F, 1.0F, 1.0F);
                line(out, p, botX[i], botY[i] - .18F, -0.16F, botX[i + 1], botY[i + 1] - .18F, -0.16F, 0.15F, 0.62F, 1.0F, 1.0F);
            }
        }

        // Branching electrical arcs shoot away from the tear in sharp zig-zags.
        for (int branch = 0; branch < 18; branch++) {
            int index = 3 + (branch * 53) % (segments - 6);
            float x = topX[index];
            float y = (branch % 2 == 0 ? topY[index] : botY[index]);
            float direction = branch % 2 == 0 ? 1.0F : -1.0F;
            float reach = 2.5F + 2.5F * (0.5F + 0.5F * Mth.sin(time * 7.0F + branch));
            float px = x, py = y;
            for (int step = 0; step < 5; step++) {
                float nx = px + (Mth.sin(branch * 19.0F + step * 8.1F + time * 5.0F) * .85F);
                float ny = py + direction * reach * (0.22F + step * .12F);
                line(out, p, px, py, -0.24F, nx, ny, -0.24F, 0.34F, 0.82F, 1.0F, 1.0F);
                px = nx; py = ny;
            }
        }

        // Bright central fracture pulses like a distant lightning storm.
        for (int i = 0; i < 14; i++) {
            float x = Mth.sin(i * 2.17F + time) * 35.0F;
            float y = Mth.sin(i * 4.3F + time * 2.0F) * 1.4F;
            float pulse = 0.7F + 0.3F * Mth.sin(time * 8.0F + i);
            line(out, p, x - 1.0F, y, -0.3F, x + 1.0F, y + Mth.sin(time * 4.0F + i) * 1.3F, -0.3F,
                0.62F * pulse, 0.22F * pulse, 1.0F, 1.0F);
        }
    }

    private static void line(VertexConsumer out, PoseStack.Pose pose,
                             float x1, float y1, float z1, float x2, float y2, float z2,
                             float r, float g, float b, float a) {
        out.addVertex(pose.pose(), x1, y1, z1).setColor(r, g, b, a).setNormal(pose, 0, 1, 0);
        out.addVertex(pose.pose(), x2, y2, z2).setColor(r, g, b, a).setNormal(pose, 0, 1, 0);
    }
}
