package fr.citizen.guilds;

import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.resources.ResourceLocation;

public final class DeepGuardianModel extends EntityModel<DeepGuardianEntity> {
    public static final ModelLayerLocation LAYER = new ModelLayerLocation(
        ResourceLocation.fromNamespaceAndPath(CitizenGuilds.MOD_ID, "deep_guardian"), "main");
    private final ModelPart root, head, leftArm, rightArm, leftLeg, rightLeg;

    public DeepGuardianModel(ModelPart root) {
        super(root);
        this.root = root;
        this.head = root.getChild("head");
        this.leftArm = root.getChild("left_arm");
        this.rightArm = root.getChild("right_arm");
        this.leftLeg = root.getChild("left_leg");
        this.rightLeg = root.getChild("right_leg");
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        root.addOrReplaceChild("body", CubeListBuilder.create()
            .texOffs(0, 0).addBox(-8, -12, -5, 16, 18, 10)
            .texOffs(0, 30).addBox(-9, -10, -6, 18, 12, 12),
            PartPose.offset(0, 5, 0));
        PartDefinition head = root.addOrReplaceChild("head", CubeListBuilder.create()
            .texOffs(0, 54).addBox(-5, -7, -5, 10, 10, 10)
            .texOffs(40, 54).addBox(-3, -3, -6, 6, 4, 2)
            .texOffs(40, 0).addBox(-8, -13, -1, 4, 9, 3)
            .texOffs(40, 0).addBox(4, -13, -1, 4, 9, 3),
            PartPose.offset(0, -9, -1));
        head.addOrReplaceChild("crown_crystal", CubeListBuilder.create()
            .texOffs(56, 30).addBox(-2, -5, -2, 4, 6, 4),
            PartPose.offset(0, -7, 0));
        root.addOrReplaceChild("left_arm", CubeListBuilder.create()
            .texOffs(0, 42).addBox(-3, -2, -4, 7, 19, 8)
            .texOffs(30, 42).addBox(-4, 12, -5, 9, 6, 10),
            PartPose.offset(10, -3, 0));
        root.addOrReplaceChild("right_arm", CubeListBuilder.create()
            .texOffs(0, 42).addBox(-4, -2, -4, 7, 19, 8)
            .texOffs(30, 42).addBox(-5, 12, -5, 9, 6, 10),
            PartPose.offset(-10, -3, 0));
        root.addOrReplaceChild("left_leg", CubeListBuilder.create()
            .texOffs(40, 12).addBox(-3, 0, -4, 7, 12, 8),
            PartPose.offset(5, 12, 0));
        root.addOrReplaceChild("right_leg", CubeListBuilder.create()
            .texOffs(40, 12).addBox(-4, 0, -4, 7, 12, 8),
            PartPose.offset(-5, 12, 0));
        root.addOrReplaceChild("back_crystals", CubeListBuilder.create()
            .texOffs(56, 40).addBox(-7, -12, 0, 4, 9, 4)
            .addBox(3, -15, 0, 4, 12, 4)
            .addBox(-2, -17, 2, 4, 10, 4),
            PartPose.offset(0, 5, 5));
        return LayerDefinition.create(mesh, 96, 96);
    }

    @Override
    public void setupAnim(DeepGuardianEntity entity, float limbSwing, float limbSwingAmount,
                          float ageInTicks, float netHeadYaw, float headPitch) {
        head.yRot = netHeadYaw * ((float)Math.PI / 180F);
        head.xRot = headPitch * ((float)Math.PI / 180F);
        leftArm.xRot = (float)Math.cos(limbSwing * 0.6662F + Math.PI) * 0.8F * limbSwingAmount;
        rightArm.xRot = (float)Math.cos(limbSwing * 0.6662F) * 0.8F * limbSwingAmount;
        leftLeg.xRot = (float)Math.cos(limbSwing * 0.6662F) * 1.2F * limbSwingAmount;
        rightLeg.xRot = (float)Math.cos(limbSwing * 0.6662F + Math.PI) * 1.2F * limbSwingAmount;
        root.getChild("back_crystals").zRot = (float)Math.sin(ageInTicks * 0.08F) * 0.035F;
    }
}
