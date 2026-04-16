package com.ricedotwho.rsa.packet.sb;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;

public record BloodClipHelperStopPacket() implements CustomPacketPayload {
    public static final StreamCodec<FriendlyByteBuf, BloodClipHelperStopPacket> CODEC = CustomPacketPayload.codec(BloodClipHelperStopPacket::write, BloodClipHelperStopPacket::new);
    public static final Type<BloodClipHelperStopPacket> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath("zero", "bloodcliphelper/stop"));

    public BloodClipHelperStopPacket(FriendlyByteBuf buf) {
        this();
    }

    public void write(FriendlyByteBuf buf) {

    }

    @Override
    public @NotNull Type<BloodClipHelperStopPacket> type() {
        return TYPE;
    }
}