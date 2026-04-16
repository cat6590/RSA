package com.ricedotwho.rsa.packet;

import com.ricedotwho.rsa.IMixin.IClientPacketListener;
import net.minecraft.client.ClientRecipeBook;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.multiplayer.CommonListenerCookie;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.protocol.Packet;
import net.minecraft.stats.StatsCounter;
import net.minecraft.world.entity.player.Input;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;
import java.util.function.Consumer;

public class FakeLocalPlayer extends LocalPlayer {

    public FakeLocalPlayer(Minecraft minecraft, ClientLevel clientLevel, ClientPacketListener clientPacketListener, StatsCounter statsCounter, ClientRecipeBook clientRecipeBook, Input input, boolean bl) {
        super(minecraft, clientLevel, clientPacketListener, statsCounter, clientRecipeBook, input, bl);
    }

    @Override
    public boolean isControlledCamera() {
        return true;
    }
}