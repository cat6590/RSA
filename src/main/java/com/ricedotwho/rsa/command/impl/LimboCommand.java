package com.ricedotwho.rsa.command.impl;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.ricedotwho.rsa.IMixin.IConnection;
import com.ricedotwho.rsm.command.Command;
import com.ricedotwho.rsm.command.api.CommandInfo;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientSuggestionProvider;
import net.minecraft.network.protocol.game.ServerboundSetCarriedItemPacket;

@CommandInfo(name = "limbo", description = "Sends an invalid slot packet to send you to limbo.")
public class LimboCommand extends Command {

    @Override
    public LiteralArgumentBuilder<ClientSuggestionProvider> build() {
        return literal(name())
                .executes(this::limbo);
    }

    private int limbo(CommandContext<ClientSuggestionProvider> clientSuggestionProviderCommandContext) {
        if (Minecraft.getInstance().getConnection() == null) return 0;
        // nneed to send immediately because swap manager blocks
        ((IConnection) Minecraft.getInstance().getConnection().getConnection()).sendPacketImmediately(new ServerboundSetCarriedItemPacket(9));
        return 1;
    }

}