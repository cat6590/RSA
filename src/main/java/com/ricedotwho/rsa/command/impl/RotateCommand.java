package com.ricedotwho.rsa.command.impl;

import com.mojang.brigadier.arguments.FloatArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.ricedotwho.rsa.RSA;
import com.ricedotwho.rsm.command.Command;
import com.ricedotwho.rsm.command.api.CommandInfo;
import net.minecraft.client.KeyboardHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientSuggestionProvider;
import net.minecraft.client.player.LocalPlayer;

@CommandInfo(name = "rotate", aliases = "rt", description = "Rotate your camera to a yaw and pitch")
public class RotateCommand extends Command {

    @Override
    public LiteralArgumentBuilder<ClientSuggestionProvider> build() {
        return literal(name())
                .then(argument("yaw", FloatArgumentType.floatArg(-180, 180))
                        .then(argument("pitch", FloatArgumentType.floatArg(-90, 90))
                            .executes(ctx -> {
                                float yaw = FloatArgumentType.getFloat(ctx, "yaw");
                                float pitch = FloatArgumentType.getFloat(ctx, "pitch");

                                LocalPlayer player = Minecraft.getInstance().player;
                                if (player == null) return 0;
                                player.setYRot(yaw);
                                player.setXRot(pitch);
                                player.yBodyRot = yaw;
                                return 1;
                            })
                        )
                )
                .then(literal("getRot")
                        .executes(ctx -> {
                            LocalPlayer player = Minecraft.getInstance().player;
                            KeyboardHandler keyboard = mc.keyboardHandler;
                            if (player == null) return 0;
                            RSA.chat("Yaw: %s, Pitch: %s", player.getYRot(), player.getXRot());
                            keyboard.setClipboard(player.getYRot() + " " + player.getXRot());
                            RSA.chat("Copied to clipboard");
                            return 1;
                        })
                );
    }
}
