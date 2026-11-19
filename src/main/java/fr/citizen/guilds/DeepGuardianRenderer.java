package fr.citizen.guilds;

import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;

public final class DeepGuardianRenderer extends MobRenderer<DeepGuardianEntity, DeepGuardianModel> {
    private static final ResourceLocation TEXTURE =
        ResourceLocation.fromNamespaceAndPath(CitizenGuilds.MOD_ID, "textures/entity/deep_guardian.png");

    public DeepGuardianRenderer(EntityRendererProvider.Context context) {
        super(context, new DeepGuardianModel(context.bakeLayer(DeepGuardianModel.LAYER)), 1.0F);
    }

    @Override
    public ResourceLocation getTextureLocation(DeepGuardianEntity entity) {
        return TEXTURE;
    }
}
