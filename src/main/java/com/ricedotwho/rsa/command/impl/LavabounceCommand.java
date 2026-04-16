package com.ricedotwho.rsa.command.impl;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.ricedotwho.rsa.RSA;
import com.ricedotwho.rsa.module.impl.dungeon.boss.p3.LavaBounce;
import com.ricedotwho.rsm.command.Command;
import com.ricedotwho.rsm.command.api.CommandInfo;
import net.minecraft.client.multiplayer.ClientSuggestionProvider;

@CommandInfo(name = "lavabounce", aliases = "lb", description = "Breaker Aura command")
public class LavabounceCommand extends Command {

    @Override
    public LiteralArgumentBuilder<ClientSuggestionProvider> build() {
        return literal(name())
                .then(literal("load")
                        .then(argument("config", StringArgumentType.greedyString())
                                .executes(ctx -> {
                                    String config = StringArgumentType.getString(ctx, "config");
                                    LavaBounce.load(config);
                                    RSA.chat("Lavabounce » Loaded %s", config);
                                    return 1;
                                })
                        )
                );
    }
}