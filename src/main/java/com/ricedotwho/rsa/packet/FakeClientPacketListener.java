package com.ricedotwho.rsa.packet;

import com.ricedotwho.rsa.IMixin.IClientPacketListener;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.multiplayer.CommonListenerCookie;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.Packet;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;
import java.util.function.Consumer;

public class FakeClientPacketListener extends ClientPacketListener {
    private final Consumer<Packet<?>> packetConsumer;

    public FakeClientPacketListener(Consumer<Packet<?>> packetConsumer) {
        super(Minecraft.getInstance(), Objects.requireNonNull(Minecraft.getInstance().getConnection()).getConnection(), extractCookie(Minecraft.getInstance().getConnection()));
        this.packetConsumer = packetConsumer;
    }

    private static CommonListenerCookie extractCookie(ClientPacketListener listener) {
        return ((IClientPacketListener) listener).getCookie();
    }

    @Override
    public void sendChat(String string) {

    }

    @Override
    public void sendCommand(String string) {

    }

    @Override
    public void sendUnattendedCommand(String string, @Nullable Screen screen) {

    }

    @Override
    protected void sendDeferredPackets() {

    }

    @Override
    public void send(Packet<?> packet) {
        if (this.packetConsumer != null)
            this.packetConsumer.accept(packet);
    }
}