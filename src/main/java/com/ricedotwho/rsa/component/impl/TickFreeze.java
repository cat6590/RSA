package com.ricedotwho.rsa.component.impl;

import com.ricedotwho.rsm.component.api.ModComponent;
import com.ricedotwho.rsm.event.api.SubscribeEvent;
import com.ricedotwho.rsm.event.impl.client.PacketEvent;
import com.ricedotwho.rsm.event.impl.world.WorldEvent;
import lombok.Getter;
import lombok.Setter;
import net.minecraft.network.protocol.game.ClientboundRespawnPacket;

public class TickFreeze extends ModComponent {
    private static boolean frozen = false;
    private static long end = 0;
    @Getter
    private static float partialTick = 1.0F;
    @Setter
    private static float lastTickPartialTicks = 1.0F;

    public TickFreeze() {
        super("TickFreeze");
    }

    public static void freeze(long millis) {
        freeze(millis, false);
    }

    public static void freeze(long millis, boolean lastTick) {
        end = System.currentTimeMillis() + millis;
        if (!frozen) freeze(lastTick);
    }

    public static void freeze() {
        freeze(false);
    }

    public static void freeze(boolean lastTick) {
        partialTick = lastTick ? lastTickPartialTicks : mc.getDeltaTracker().getGameTimeDeltaPartialTick(true);
        frozen = true;
    }

    public static void unFreeze() {
        frozen = false;
        end = 0;
        partialTick = 1.0F;
    }

    public static boolean isFrozen() {
        if (frozen && end > 0 && System.currentTimeMillis() > end) {
            unFreeze();
        }
        return frozen;
    }

    @SubscribeEvent
    public void onLoad(WorldEvent.Load event) {
        unFreeze();
    }

    @SubscribeEvent
    public void onPacket(PacketEvent.Receive event) {
        if (event.getPacket() instanceof ClientboundRespawnPacket) {
            unFreeze();
        }
    }
}
