package fr.citizen.guilds;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.PacketDistributor;

/** Server-authoritative state for the global sky tear, synchronized to every client. */
public record RiftSkyStatePayload(boolean active) implements CustomPacketPayload {
    public static final Type<RiftSkyStatePayload> TYPE =
        new Type<>(ResourceLocation.fromNamespaceAndPath(CitizenGuilds.MOD_ID, "rift_sky_state"));
    public static final StreamCodec<ByteBuf, RiftSkyStatePayload> CODEC =
        StreamCodec.composite(ByteBufCodecs.BOOL, RiftSkyStatePayload::active, RiftSkyStatePayload::new);

    @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }

    public static void broadcast(boolean active) {
        PacketDistributor.sendToAllPlayers(new RiftSkyStatePayload(active));
    }
}
