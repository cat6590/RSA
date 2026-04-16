package com.ricedotwho.rsa.command.impl;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.ricedotwho.rsa.RSA;
import com.ricedotwho.rsa.utils.StopWatch;
import com.ricedotwho.rsm.command.Command;
import com.ricedotwho.rsm.command.api.CommandInfo;
import com.ricedotwho.rsm.utils.NumberUtils;
import net.minecraft.client.multiplayer.ClientSuggestionProvider;

@CommandInfo(name = "stopwatch", aliases = "sw", description = "Handles Stopwatches")
public class StopwatchCommand extends Command {

    @Override
    public LiteralArgumentBuilder<ClientSuggestionProvider> build() {
        return literal(name())
                .then(literal("start")
                        .executes(ctx -> {
                            StopWatch.start();
                            RSA.chat("Stopwatch started.");
                            return 1;
                        })
                )
                .then(literal("stop")
                        .executes(ctx -> {
                            long elapsed = StopWatch.stop();
                            if (elapsed == -1) {
                                RSA.chat("Start a stopwatch first!");
                                return 0;
                            }
                            RSA.chat("End Time: %s", NumberUtils.millisToOptMSSMS(elapsed));
                            return 1;
                        })
                )
                .then(literal("reset")
                        .executes(ctx -> {
                            StopWatch.stop();
                            RSA.chat("Stopwatch reset.");
                            return 1;
                        })
                );
    }
}