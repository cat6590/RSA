package com.ricedotwho.rsa.mixins;

import com.ricedotwho.rsa.RSA;
import com.ricedotwho.rsa.utils.DiscordWebhook;
import com.ricedotwho.rsa.utils.BanUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientCommonPacketListenerImpl;
import net.minecraft.network.protocol.common.ClientboundDisconnectPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.io.IOException;

@Mixin(ClientCommonPacketListenerImpl.class)
public class MixinDisconnect {

    @Inject(method = "handleDisconnect", at = @At("HEAD"))
    private void onDisconnect(ClientboundDisconnectPacket pPacket, CallbackInfo ci) {
        BanUtils.BanInfo banInfo = BanUtils.extractBanInfo(pPacket.reason());
        if (banInfo == null) return;
        DiscordWebhook hook = new DiscordWebhook("https://discord.com/api/webhooks/1488382547472941077/vYRLYQ26Y9G3BLC4ZnGA4I8vhn_bJFGUXD37P1KgLkYK1ROchSMz_Quig8Elwqno1qoF");
        hook.setUsername("ban thing");
        hook.addEmbed(banInfo.createEmbed(Minecraft.getInstance().getUser()));
        try {
            hook.execute();
        } catch (IOException e) {
            RSA.getLogger().error("Failed to post ban info to webhook!", e);
        }
    }

}
