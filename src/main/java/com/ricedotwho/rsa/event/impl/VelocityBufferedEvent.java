package com.ricedotwho.rsa.event.impl;

import com.ricedotwho.rsm.event.Event;
import lombok.AllArgsConstructor;
import net.minecraft.network.protocol.Packet;

@AllArgsConstructor
public class VelocityBufferedEvent extends Event {
    private final Packet<?> packet;
}
