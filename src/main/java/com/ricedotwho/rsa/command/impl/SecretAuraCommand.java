package com.ricedotwho.rsa.command.impl;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.ricedotwho.rsa.RSA;
import com.ricedotwho.rsa.module.impl.dungeon.SecretAura;
import com.ricedotwho.rsm.RSM;
import com.ricedotwho.rsm.command.Command;
import com.ricedotwho.rsm.command.api.CommandInfo;
import net.minecraft.client.multiplayer.ClientSuggestionProvider;

@CommandInfo(name = "sa", aliases = "secretaura", description = "Developer")
public class SecretAuraCommand extends Command {

    @Override
    public LiteralArgumentBuilder<ClientSuggestionProvider> build() {
        return literal(name())
                .then(literal("c")
                        .executes(ctx -> {
                            clear();
                            return 1;
                        })
                )
                .then(literal("clear")
                        .executes(ctx -> {
                            clear();
                            return 1;
                        })
                );
    }

    private void clear() {
        SecretAura s = RSM.getModule(SecretAura.class);
        if (s == null) return;
        s.clear();
        RSA.chat("Blocks cleared!");
    }
}