package fr.citizen.guilds;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Ravager;
import net.minecraft.world.level.Level;

/** Original Citizen Guilds dungeon boss. */
public final class DeepGuardianEntity extends Ravager {
    private int slamCooldown = 100;

    public DeepGuardianEntity(EntityType<? extends Ravager> type, Level level) {
        super(type, level);
        this.setPersistenceRequired();
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Ravager.createAttributes()
            .add(Attributes.MAX_HEALTH, 180.0D)
            .add(Attributes.ATTACK_DAMAGE, 16.0D)
            .add(Attributes.MOVEMENT_SPEED, 0.28D)
            .add(Attributes.KNOCKBACK_RESISTANCE, 0.85D);
    }

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide || !isAlive()) return;
        if (slamCooldown > 0) slamCooldown--;
        if (slamCooldown == 0 && getTarget() != null && distanceToSqr(getTarget()) < 64.0D) {
            for (var entity : level().getEntitiesOfClass(net.minecraft.world.entity.LivingEntity.class,
                    getBoundingBox().inflate(4.5D), e -> e != this && e.isAlive() && e.getType() != EntityType.ARMOR_STAND)) {
                entity.hurt(damageSources().mobAttack(this), 8.0F);
                double dx = entity.getX() - getX();
                double dz = entity.getZ() - getZ();
                double length = Math.max(0.1D, Math.sqrt(dx * dx + dz * dz));
                entity.knockback(1.8D, -dx / length, -dz / length);
            }
            level().levelEvent(200 удар, blockPosition(), 0);
            slamCooldown = 100;
        }
    }
}
