package fr.citizen.guilds;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;

/** Client-rendered 3D decoration for the dungeon gate monument. */
public final class DungeonGateVisualEntity extends Entity {
    public DungeonGateVisualEntity(EntityType<? extends DungeonGateVisualEntity> type, Level level) {
        super(type, level);
        noPhysics = true;
    }
    @Override protected void defineSynchedData(net.minecraft.network.syncher.SynchedEntityData.Builder builder) {}
    @Override protected void readAdditionalSaveData(CompoundTag tag) {}
    @Override protected void addAdditionalSaveData(CompoundTag tag) {}
    @Override public void tick() {
        super.tick();
        noPhysics = true;
        setDeltaMovement(0.0D, 0.0D, 0.0D);
        if (!level().isClientSide && tickCount > 20 && level() instanceof net.minecraft.server.level.ServerLevel server) {
            var data = DungeonData.get(server.getServer());
            if (!data.hasPlacedPortal() || data.lastSite() == null
                || distanceToSqr(data.lastSite().getX() + 0.5D, data.lastSite().getY() + 0.5D, data.lastSite().getZ() + 0.5D) > 4.0D) {
                discard();
            }
        }
    }
}
