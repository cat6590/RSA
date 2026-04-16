package com.ricedotwho.rsa.mixins;

import com.mojang.authlib.GameProfile;
import com.ricedotwho.rsa.IMixin.IClientPacketListener;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.multiplayer.CommonListenerCookie;
import net.minecraft.client.multiplayer.LevelLoadTracker;
import net.minecraft.core.RegistryAccess;
import net.minecraft.world.flag.FeatureFlagSet;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

import java.util.Collections;

@Mixin(ClientPacketListener.class)
public class ClientPacketListenerMixin implements IClientPacketListener {
    @Shadow
    private @Nullable LevelLoadTracker levelLoadTracker;

    @Shadow
    @Final
    private GameProfile localGameProfile;

    @Shadow
    @Final
    private RegistryAccess.Frozen registryAccess;

    @Shadow
    @Final
    private FeatureFlagSet enabledFeatures;

    public CommonListenerCookie getCookie() {
        ClientPacketListener packetListener = (ClientPacketListener) (Object) this;
        return new CommonListenerCookie(
                this.levelLoadTracker,
                this.localGameProfile,
                null,
                this.registryAccess,
                this.enabledFeatures,
                packetListener.serverBrand(),
                packetListener.getServerData(),
                null,
                Collections.emptyMap(),
                null,
                Collections.emptyMap(),
                null,
                Collections.emptyMap(),
                true
                );
    }
}
