package fr.citizen.guilds;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

/**
 * Procedural 3D gate: gothic dark-metal arch, circular animated dimensional
 * aperture and a suspended faceted amethyst with two golden orbital rings.
 */
public final class DungeonGateVisualRenderer extends EntityRenderer<DungeonGateVisualEntity> {
    public DungeonGateVisualRenderer(EntityRendererProvider.Context context) {
        super(context);
        shadowRadius = 0.0F;
    }

    @Override
    public void render(DungeonGateVisualEntity entity, float yaw, float partialTick,
                       PoseStack pose, MultiBufferSource buffers, int packedLight) {
        pose.pushPose();
        float time = (entity.tickCount + partialTick) * 0.035F;
        VertexConsumer out = buffers.getBuffer(RenderType.lines());

        // Gothic frame in a vertical plane, built from layered bevelled outlines.
        gothicFrame(pose, out);
        // Circular aperture: three rotating rings and a multi-turn spiral.
        aperture(pose, out, time);
        // Floating crystal: elongated faceted core, side shards and gold armillary bands.
        crystal(pose, out, time);
        pose.popPose();
        super.render(entity, yaw, partialTick, pose, buffers, packedLight);
    }

    private static void gothicFrame(PoseStack pose, VertexConsumer out) {
        float z = 0.0F;
        // Twin uprights with bevels and carved inner rails.
        for (float x : new float[]{-1.72F, 1.72F}) {
            line(out, pose.last(), x, -1.95F, z, x, 1.35F, z, .12F, .10F, .16F);
            line(out, pose.last(), x + Math.copySign(.13F, x), -1.82F, z + .04F,
                x + Math.copySign(.13F, x), 1.28F, z + .04F, .34F, .22F, .39F);
            line(out, pose.last(), x, -1.95F, z, x + Math.copySign(.18F, x), -1.76F, z, .62F, .38F, .68F);
            for (int y = -1; y <= 1; y++) {
                float yy = y * 0.82F;
                line(out, pose.last(), x - .12F, yy, z - .04F, x + .12F, yy, z - .04F, .72F, .35F, .68F);
            }
            // Small pointed side finials.
            line(out, pose.last(), x, 1.35F, z, x, 1.67F, z, .58F, .31F, .64F);
            line(out, pose.last(), x, 1.67F, z, x - Math.copySign(.19F, x), 1.42F, z, .58F, .31F, .64F);
            line(out, pose.last(), x, 1.67F, z, x + Math.copySign(.19F, x), 1.42F, z, .58F, .31F, .64F);
        }
        // Pointed crown arch, with a second recessed contour.
        for (int i = 0; i < 48; i++) {
            float a0 = (float)Math.PI * i / 48.0F;
            float a1 = (float)Math.PI * (i + 1) / 48.0F;
            float x0 = Mth.cos(a0) * 1.72F, y0 = 1.25F + Mth.sin(a0) * .78F;
            float x1 = Mth.cos(a1) * 1.72F, y1 = 1.25F + Mth.sin(a1) * .78F;
            line(out, pose.last(), x0, y0, z, x1, y1, z, .16F, .11F, .19F);
            line(out, pose.last(), x0 * .91F, y0 - .12F, z + .035F,
                x1 * .91F, y1 - .12F, z + .035F, .70F, .31F, .66F);
        }
        // Heavy foot and crown rails.
        for (float yy : new float[]{-1.94F, -1.78F, 1.26F}) {
            line(out, pose.last(), -1.78F, yy, z, 1.78F, yy, z, .22F, .16F, .25F);
        }
        // Pink rune diamonds on both columns.
        for (float x : new float[]{-1.72F, 1.72F}) {
            for (float y : new float[]{-.95F, .0F, .95F}) {
                line(out, pose.last(), x, y + .18F, z - .07F, x + .12F, y, z - .07F, .95F, .28F, .78F);
                line(out, pose.last(), x + .12F, y, z - .07F, x, y - .18F, z - .07F, .95F, .28F, .78F);
                line(out, pose.last(), x, y - .18F, z - .07F, x - .12F, y, z - .07F, .95F, .28F, .78F);
                line(out, pose.last(), x - .12F, y, z - .07F, x, y + .18F, z - .07F, .95F, .28F, .78F);
            }
        }
    }

    private static void aperture(PoseStack pose, VertexConsumer out, float time) {
        pose.pushPose();
        pose.translate(0.0D, -0.12D, -0.12D);
        for (int ring = 0; ring < 4; ring++) {
            float radius = 1.34F - ring * .19F;
            float rotation = time * (ring % 2 == 0 ? 1.0F : -1.25F) + ring * .8F;
            circle(out, pose.last(), radius, rotation,
                ring % 2 == 0 ? .88F : .38F, ring % 2 == 0 ? .22F : .68F, ring % 2 == 0 ? .95F : 1.0F);
        }
        // Three interwoven spiral arms give the aperture a visible vortex motion.
        for (int arm = 0; arm < 3; arm++) {
            float previousX = 0, previousY = 0;
            for (int i = 0; i <= 100; i++) {
                float t = i / 100.0F;
                float a = t * (float)Math.PI * 5.0F + time * 2.1F + arm * (float)(Math.PI * 2.0 / 3.0);
                float r = .08F + t * 1.12F;
                float x = Mth.cos(a) * r, y = Mth.sin(a) * r;
                if (i > 0) line(out, pose.last(), previousX, previousY, -.045F,
                    x, y, -.045F, .88F, .28F + .25F * t, 1.0F);
                previousX = x; previousY = y;
            }
        }
        // Radial lightning-like cracks flicker around the aperture rim.
        for (int i = 0; i < 12; i++) {
            float a = i * (float)Math.PI / 6.0F + time * .7F;
            float r0 = .92F + .12F * Mth.sin(time * 5.0F + i);
            float r1 = 1.28F;
            float x0 = Mth.cos(a) * r0, y0 = Mth.sin(a) * r0;
            float x1 = Mth.cos(a + .08F) * r1, y1 = Mth.sin(a + .08F) * r1;
            line(out, pose.last(), x0, y0, -.08F, x1, y1, -.08F, .22F, .78F, 1.0F);
        }
        pose.popPose();
    }

    private static void crystal(PoseStack pose, VertexConsumer out, float time) {
        pose.pushPose();
        pose.translate(0.0D, 3.28D + Mth.sin(time * 1.4F) * .10F, -.02D);
        pose.mulPose(Axis.YP.rotation(time * .35F));
        // Long faceted spindle with pointed ends.
        float[][] ring = new float[8][3];
        for (int i = 0; i < 8; i++) {
            float a = i * (float)Math.PI / 4.0F;
            ring[i][0] = Mth.cos(a) * .43F;
            ring[i][1] = 0.0F;
            ring[i][2] = Mth.sin(a) * .43F;
        }
        for (int i = 0; i < 8; i++) {
            int n = (i + 1) % 8;
            line(out, pose.last(), 0, 1.05F, 0, ring[i][0], .0F, ring[i][2], .72F, .36F, 1.0F);
            line(out, pose.last(), ring[i][0], 0, ring[i][2], 0, -1.05F, 0, .42F, .18F, .82F);
            line(out, pose.last(), ring[i][0], 0, ring[i][2], ring[n][0], 0, ring[n][2], .92F, .62F, 1.0F);
        }
        // Smaller side crystals point outward from the central gem.
        for (float side : new float[]{-1.0F, 1.0F}) {
            line(out, pose.last(), side * .24F, .25F, 0, side * .78F, .62F, 0, .85F, .45F, 1.0F);
            line(out, pose.last(), side * .78F, .62F, 0, side * .72F, -.12F, 0, .65F, .30F, .96F);
            line(out, pose.last(), side * .72F, -.12F, 0, side * .24F, .25F, 0, .75F, .38F, 1.0F);
        }
        // Two tilted gold orbital rings around the floating crystal.
        goldRing(pose, out, .86F, time * 1.15F, .92F, 0.0F);
        goldRing(pose, out, .98F, -time * .82F, 0.0F, .92F);
        pose.popPose();
    }

    private static void goldRing(PoseStack pose, VertexConsumer out, float radius,
                                 float rotation, float tiltX, float tiltZ) {
        pose.pushPose();
        pose.mulPose(Axis.YP.rotation(rotation));
        pose.mulPose(Axis.XP.rotation(tiltX));
        pose.mulPose(Axis.ZP.rotation(tiltZ));
        for (int i = 0; i < 96; i++) {
            float a0 = (float)(Math.PI * 2.0 * i / 96.0);
            float a1 = (float)(Math.PI * 2.0 * (i + 1) / 96.0);
            line(out, pose.last(), Mth.cos(a0) * radius, 0, Mth.sin(a0) * radius,
                Mth.cos(a1) * radius, 0, Mth.sin(a1) * radius, 1.0F, .72F, .20F);
        }
        pose.popPose();
    }

    private static void circle(VertexConsumer out, PoseStack.Pose pose, float radius, float rotation,
                               float red, float green, float blue) {
        for (int i = 0; i < 96; i++) {
            float a0 = (float)(Math.PI * 2.0 * i / 96.0) + rotation;
            float a1 = (float)(Math.PI * 2.0 * (i + 1) / 96.0) + rotation;
            line(out, pose, Mth.cos(a0) * radius, Mth.sin(a0) * radius, -.04F,
                Mth.cos(a1) * radius, Mth.sin(a1) * radius, -.04F, red, green, blue);
        }
    }

    private static void line(VertexConsumer out, PoseStack.Pose pose,
                             float x1, float y1, float z1, float x2, float y2, float z2,
                             float r, float g, float b) {
        out.addVertex(pose.pose(), x1, y1, z1).setColor(r, g, b, 1.0F).setNormal(pose, 0, 1, 0);
        out.addVertex(pose.pose(), x2, y2, z2).setColor(r, g, b, 1.0F).setNormal(pose, 0, 1, 0);
    }

    @Override public ResourceLocation getTextureLocation(DungeonGateVisualEntity entity) {
        return ResourceLocation.withDefaultNamespace("textures/atlas/blocks.png");
    }
}
