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
 * Procedural celestial armillary: nested gold hoops, raised rails, radial braces,
 * engraved ticks, orbital studs and a faceted central spindle.
 */
public final class RiftMechanismRenderer extends EntityRenderer<RiftMechanismEntity> {
    private static final float GOLD_R = 1.0F;
    private static final float GOLD_G = 0.66F;
    private static final float GOLD_B = 0.12F;

    public RiftMechanismRenderer(EntityRendererProvider.Context context) {
        super(context);
        shadowRadius = 0.0F;
    }

    @Override
    public void render(RiftMechanismEntity entity, float yaw, float partialTick,
                       PoseStack stack, MultiBufferSource buffers, int packedLight) {
        stack.pushPose();
        float age = entity.tickCount + partialTick;
        float bob = (float)Math.sin(age * 0.045F) * 0.12F;
        stack.translate(0.0D, 1.48D + bob, 0.0D);
        float speed = entity.rotationSpeed();
        float t = age * 0.012F * speed;
        VertexConsumer out = buffers.getBuffer(RenderType.lines());

        // Three differently tilted armillary hoops, each built as a raised metal band.
        hoop(stack, out, 1.34F, t, 0.26F, 0.0F);
        hoop(stack, out, 1.13F, -t * 0.73F, 1.04F, 0.0F);
        hoop(stack, out, 0.91F, t * 1.31F, 0.0F, 0.88F);

        // Fine orbital meridians and radial braces.
        stack.pushPose();
        stack.mulPose(Axis.YP.rotation(-t * 0.42F));
        stack.mulPose(Axis.ZP.rotation(0.62F));
        meridian(stack, out, 1.29F, 0.0F, 0.0F);
        stack.popPose();

        stack.pushPose();
        stack.mulPose(Axis.YP.rotation(t * 0.57F));
        stack.mulPose(Axis.XP.rotation(0.92F));
        meridian(stack, out, 1.08F, 0.0F, 0.0F);
        stack.popPose();

        // Twelve engraved radial marks on the equatorial ring.
        stack.pushPose();
        stack.mulPose(Axis.YP.rotation(t * 0.26F));
        PoseStack.Pose equator = stack.last();
        for (int i = 0; i < 24; i++) {
            float a = (float)(Math.PI * 2.0 * i / 24.0);
            float r0 = i % 2 == 0 ? 1.20F : 1.25F;
            float r1 = 1.39F;
            line(out, equator, Mth.cos(a) * r0, 0.0F, Mth.sin(a) * r0,
                Mth.cos(a) * r1, 0.0F, Mth.sin(a) * r1,
                i % 2 == 0 ? 1.0F : 0.78F, i % 2 == 0 ? 0.88F : 0.48F, 0.24F);
            if (i % 2 == 0) {
                float x = Mth.cos(a) * 1.43F, z = Mth.sin(a) * 1.43F;
                stud(out, equator, x, 0.0F, z, 0.045F);
            }
        }
        stack.popPose();

        // Central spindle, cross axle and a small faceted star cage.
        stack.pushPose();
        stack.mulPose(Axis.YP.rotation(t * 0.6F));
        PoseStack.Pose core = stack.last();
        line(out, core, 0, -1.02F, 0, 0, 1.02F, 0, 1.0F, 0.78F, 0.25F);
        line(out, core, -0.43F, 0, 0, 0.43F, 0, 0, 1.0F, 0.91F, 0.54F);
        line(out, core, 0, 0, -0.43F, 0, 0, 0.43F, 1.0F, 0.91F, 0.54F);
        for (int i = 0; i < 8; i++) {
            float a = (float)(Math.PI * 2.0 * i / 8.0);
            float x = Mth.cos(a) * 0.34F, z = Mth.sin(a) * 0.34F;
            float y = (i % 2 == 0 ? 0.22F : -0.22F);
            line(out, core, x, y, z, -x, -y, -z, 1.0F, 0.72F, 0.20F);
        }
        stud(out, core, 0, 0, 0, 0.12F);
        stud(out, core, 0, 0.94F, 0, 0.075F);
        stud(out, core, 0, -0.94F, 0, 0.075F);
        stack.popPose();

        stack.popPose();
        super.render(entity, yaw, partialTick, stack, buffers, packedLight);
    }

    /** Double-railed hoop with cross ties and a fine inner engraved line. */
    private static void hoop(PoseStack stack, VertexConsumer out, float radius,
                             float rotation, float tiltX, float tiltZ) {
        stack.pushPose();
        stack.mulPose(Axis.YP.rotation(rotation));
        if (tiltX != 0.0F) stack.mulPose(Axis.XP.rotation(tiltX));
        if (tiltZ != 0.0F) stack.mulPose(Axis.ZP.rotation(tiltZ));
        PoseStack.Pose pose = stack.last();
        int segments = 144;
        float rail = 0.052F;
        for (int i = 0; i < segments; i++) {
            float a0 = (float)(Math.PI * 2.0 * i / segments);
            float a1 = (float)(Math.PI * 2.0 * (i + 1) / segments);
            float x0 = Mth.cos(a0) * radius, z0 = Mth.sin(a0) * radius;
            float x1 = Mth.cos(a1) * radius, z1 = Mth.sin(a1) * radius;
            line(out, pose, x0, -rail, z0, x1, -rail, z1, GOLD_R, GOLD_G, GOLD_B);
            line(out, pose, x0, rail, z0, x1, rail, z1, 1.0F, 0.84F, 0.35F);
            line(out, pose, x0, -rail, z0, x0, rail, z0, 0.92F, 0.53F, 0.10F);
            if (i % 6 == 0) {
                float inner = radius - 0.105F;
                line(out, pose, Mth.cos(a0) * inner, 0.0F, Mth.sin(a0) * inner,
                    x0, 0.0F, z0, 1.0F, 0.91F, 0.56F);
            }
            if (i % 12 == 0) {
                float midX = Mth.cos(a0) * radius, midZ = Mth.sin(a0) * radius;
                line(out, pose, midX, -rail, midZ, midX * 1.035F, 0.0F, midZ * 1.035F,
                    0.82F, 0.46F, 0.08F);
                line(out, pose, midX * 1.035F, 0.0F, midZ * 1.035F, midX, rail, midZ,
                    1.0F, 0.82F, 0.30F);
            }
        }
        stack.popPose();
    }

    /** A slender great-circle meridian with cross braces and end finials. */
    private static void meridian(PoseStack stack, VertexConsumer out, float radius,
                                 float ignoredA, float ignoredB) {
        PoseStack.Pose pose = stack.last();
        int segments = 96;
        for (int i = 0; i < segments; i++) {
            float a0 = (float)(Math.PI * 2.0 * i / segments);
            float a1 = (float)(Math.PI * 2.0 * (i + 1) / segments);
            float x0 = Mth.cos(a0) * radius, y0 = Mth.sin(a0) * radius;
            float x1 = Mth.cos(a1) * radius, y1 = Mth.sin(a1) * radius;
            line(out, pose, x0, y0, 0, x1, y1, 0, 0.92F, 0.62F, 0.16F);
            if (i % 12 == 0) {
                line(out, pose, x0 * 0.94F, y0 * 0.94F, -0.045F,
                    x0 * 1.04F, y0 * 1.04F, 0.045F, 1.0F, 0.88F, 0.42F);
            }
        }
        stud(out, pose, 0, radius, 0, 0.065F);
        stud(out, pose, 0, -radius, 0, 0.065F);
    }

    /** Small octahedral metal fitting, rendered as crisp golden edges. */
    private static void stud(VertexConsumer out, PoseStack.Pose pose,
                             float x, float y, float z, float size) {
        float[][] p = {
            {x, y + size, z}, {x + size, y, z}, {x, y, z + size},
            {x - size, y, z}, {x, y, z - size}, {x, y - size, z}
        };
        int[][] edges = {{0,1},{0,2},{0,3},{0,4},{5,1},{5,2},{5,3},{5,4},
            {1,2},{2,3},{3,4},{4,1}};
        for (int[] e : edges) {
            float shade = e[0] == 0 || e[1] == 0 ? 1.0F : 0.82F;
            line(out, pose, p[e[0]][0], p[e[0]][1], p[e[0]][2],
                p[e[1]][0], p[e[1]][1], p[e[1]][2], shade, 0.78F, 0.28F);
        }
    }

    private static void line(VertexConsumer out, PoseStack.Pose pose,
                             float x1, float y1, float z1, float x2, float y2, float z2,
                             float r, float g, float b) {
        out.addVertex(pose.pose(), x1, y1, z1).setColor(r, g, b, 1.0F).setNormal(pose, 0, 1, 0);
        out.addVertex(pose.pose(), x2, y2, z2).setColor(r, g, b, 1.0F).setNormal(pose, 0, 1, 0);
    }

    @Override public ResourceLocation getTextureLocation(RiftMechanismEntity entity) {
        return ResourceLocation.withDefaultNamespace("textures/atlas/blocks.png");
    }
}
