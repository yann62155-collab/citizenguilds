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

/** Procedural golden multi-ring celestial gyroscope. */
public final class RiftMechanismRenderer extends EntityRenderer<RiftMechanismEntity> {
    public RiftMechanismRenderer(EntityRendererProvider.Context context) {
        super(context);
        shadowRadius = 0.0F;
    }

    @Override
    public void render(RiftMechanismEntity entity, float yaw, float partialTick,
                       PoseStack stack, MultiBufferSource buffers, int packedLight) {
        stack.pushPose();
        float age = entity.tickCount + partialTick;
        stack.translate(0.0D, 1.5D + Math.sin(age * 0.045F) * 0.12D, 0.0D);
        float speed = entity.rotationSpeed();
        float t = age * 0.012F * speed;
        VertexConsumer out = buffers.getBuffer(RenderType.lines());
        ring(stack, out, 1.25F, t, 1.0F, 0.68F, 0.12F);
        ring(stack, out, 1.05F, -t * 0.73F, 1.0F, 0.82F, 0.30F);
        ring(stack, out, 0.82F, t * 1.31F, 1.0F, 0.94F, 0.58F);
        stack.pushPose();
        stack.mulPose(Axis.YP.rotation(t * 0.6F));
        PoseStack.Pose pose = stack.last();
        line(out, pose, 0, -1.35F, 0, 0, 1.35F, 0, 1.0F, 0.78F, 0.22F);
        line(out, pose, -0.32F, 0, 0, 0.32F, 0, 0, 1.0F, 0.9F, 0.5F);
        line(out, pose, 0, 0, -0.32F, 0, 0, 0.32F, 1.0F, 0.9F, 0.5F);
        stack.popPose();
        stack.popPose();
        super.render(entity, yaw, partialTick, stack, buffers, packedLight);
    }

    private static void ring(PoseStack stack, VertexConsumer out, float radius,
                             float rotation, float r, float g, float b) {
        stack.pushPose();
        stack.mulPose(Axis.YP.rotation(rotation));
        stack.mulPose(Axis.XP.rotation(0.22F + rotation * 0.19F));
        PoseStack.Pose pose = stack.last();
        int segments = 128;
        for (int i = 0; i < segments; i++) {
            float a0 = (float)(Math.PI * 2.0 * i / segments);
            float a1 = (float)(Math.PI * 2.0 * (i + 1) / segments);
            float x0 = Mth.cos(a0) * radius, z0 = Mth.sin(a0) * radius;
            float x1 = Mth.cos(a1) * radius, z1 = Mth.sin(a1) * radius;
            line(out, pose, x0, 0, z0, x1, 0, z1, r, g, b);
            line(out, pose, x0, 0.045F, z0, x1, 0.045F, z1, r, g, b);
            line(out, pose, x0, 0, z0, x0, 0.045F, z0, r, g, b);
            if (i % 8 == 0) {
                float inner = radius - 0.13F;
                line(out, pose, Mth.cos(a0) * inner, 0.022F, Mth.sin(a0) * inner,
                    x0, 0.022F, z0, 1.0F, Math.min(1.0F, g + 0.1F), b);
            }
        }
        stack.popPose();
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
