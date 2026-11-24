package fr.citizen.guilds;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;

/** Floating visual gyroscope announcing the opening of a guild dungeon rift. */
public final class RiftMechanismEntity extends Entity {
    private static final EntityDataAccessor<Boolean> CHARGING =
        SynchedEntityData.defineId(RiftMechanismEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Float> SPEED =
        SynchedEntityData.defineId(RiftMechanismEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> TARGET =
        SynchedEntityData.defineId(RiftMechanismEntity.class, EntityDataSerializers.FLOAT);
    private int chargeTicks;

    public RiftMechanismEntity(EntityType<? extends RiftMechanismEntity> type, Level level) {
        super(type, level);
        noPhysics = true;
    }

    @Override protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(CHARGING, false);
        builder.define(SPEED, 1.0F);
        builder.define(TARGET, 1.0F);
    }

    public boolean isCharging() { return entityData.get(CHARGING); }
    public float rotationSpeed() { return entityData.get(SPEED); }

    public void setCharging(boolean charging, float targetSpeed) {
        entityData.set(CHARGING, charging);
        entityData.set(TARGET, charging ? Math.max(1.0F, Math.min(20.0F, targetSpeed)) : 1.0F);
        chargeTicks = 0;
        entityData.set(SPEED, 1.0F);
    }

    @Override protected void readAdditionalSaveData(CompoundTag tag) {
        entityData.set(CHARGING, tag.getBoolean("Charging"));
        entityData.set(SPEED, tag.getFloat("Speed"));
        entityData.set(TARGET, tag.contains("Target") ? tag.getFloat("Target") : 1.0F);
        chargeTicks = tag.getInt("ChargeTicks");
    }

    @Override protected void addAdditionalSaveData(CompoundTag tag) {
        tag.putBoolean("Charging", isCharging());
        tag.putFloat("Speed", rotationSpeed());
        tag.putFloat("Target", entityData.get(TARGET));
        tag.putInt("ChargeTicks", chargeTicks);
    }

    @Override public void tick() {
        super.tick();
        noPhysics = true;
        setDeltaMovement(0, 0, 0);
        if (!level().isClientSide && level() instanceof net.minecraft.server.level.ServerLevel serverLevel) {
            // A server restart clears the controller's in-flight state. Remove the
            // saved visual entity rather than leaving a permanently charging relic.
            if (isCharging() && !RiftDungeonController.isCharging()) {
                discard();
                return;
            }
            // Once the associated gate has been removed, the idle mechanism has no
            // reason to remain in the overworld.
            if (!isCharging() && !DungeonData.get(serverLevel.getServer()).hasPlacedPortal()) {
                discard();
                return;
            }
            if (isCharging()) {
                chargeTicks = Math.min(400, chargeTicks + 1);
                float p = chargeTicks / 400.0F;
                float eased = p * p * (3.0F - 2.0F * p);
                entityData.set(SPEED, 1.0F + (entityData.get(TARGET) - 1.0F) * eased);
            }
        }
        if (level().isClientSide && tickCount % 2 == 0) {
            double angle = tickCount * 0.11D;
            double radius = isCharging() ? 1.7D : 1.35D;
            double px = getX() + Math.cos(angle) * radius;
            double pz = getZ() + Math.sin(angle) * radius;
            double py = getY() + 1.5D + Math.sin(angle * 1.7D) * 0.35D;
            level().addParticle(isCharging() ? ParticleTypes.END_ROD : ParticleTypes.ENCHANT,
                px, py, pz, 0.0D, isCharging() ? 0.035D : 0.008D, 0.0D);
            if (isCharging() && tickCount % 6 == 0)
                level().addParticle(ParticleTypes.PORTAL, getX(), getY() + 1.5D, getZ(),
                    Math.cos(angle) * 0.04D, 0.0D, Math.sin(angle) * 0.04D);
        }
    }
}
