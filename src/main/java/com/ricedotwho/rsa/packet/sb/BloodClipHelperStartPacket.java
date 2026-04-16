package com.ricedotwho.rsa.packet.sb;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;

public record BloodClipHelperStartPacket(int roofHeight) implements CustomPacketPayload {
    public static final StreamCodec<FriendlyByteBuf, BloodClipHelperStartPacket> CODEC = CustomPacketPayload.codec(BloodClipHelperStartPacket::write, BloodClipHelperStartPacket::new);
    public static final Type<BloodClipHelperStartPacket> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath("zero", "bloodcliphelper/start"));

    public BloodClipHelperStartPacket(FriendlyByteBuf buf) {
        this(buf.readVarInt());
    }

    public void write(FriendlyByteBuf buf) {
        buf.writeVarInt(roofHeight);
    }

    @Override
    public @NotNull Type<BloodClipHelperStartPacket> type() {
        return TYPE;
    }
}