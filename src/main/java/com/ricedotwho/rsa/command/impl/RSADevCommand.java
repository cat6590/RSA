package com.ricedotwho.rsa.command.impl;

import com.mojang.brigadier.arguments.FloatArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.ricedotwho.rsa.RSA;
import com.ricedotwho.rsa.component.impl.TickFreeze;
import com.ricedotwho.rsa.component.impl.pathfinding.score.DungeonRoomScore;
import com.ricedotwho.rsa.utils.Util;
import com.ricedotwho.rsm.command.Command;
import com.ricedotwho.rsm.command.api.CommandInfo;
import com.ricedotwho.rsm.component.impl.map.handler.DungeonInfo;
import com.ricedotwho.rsm.component.impl.map.map.RoomType;
import com.ricedotwho.rsm.component.impl.map.map.UniqueRoom;
import com.ricedotwho.rsm.component.impl.task.TaskComponent;
import net.minecraft.client.multiplayer.ClientSuggestionProvider;

import java.util.Optional;

@CommandInfo(name = "rdev", description = "Developer")
public class RSADevCommand extends Command {

    @Override
    public LiteralArgumentBuilder<ClientSuggestionProvider> build() {
        return literal(name())
                .then(literal("tickrate")
                        .then(argument("tick rate", FloatArgumentType.floatArg(0, 20))
                                .executes(ctx -> {
                                    Util.setTickRate(FloatArgumentType.getFloat(ctx, "tick rate"));
                                    TaskComponent.onMilli(2500, () -> Util.setTickRate(20, false));
                                    return 1;
                                })
                        )
                        .then(literal("freeze")
                                .executes(ctx -> {
                                    TickFreeze.freeze(5000);
                                    return 1;
                                })
                        )
                )
                .then(literal("iszero")
                        .executes(ctx -> {
                            RSA.chat("Zero: %s", Util.isZero());
                            return 1;
                        })
                )
                .then(literal("score")
                        .executes(ctx -> {
                            Optional<UniqueRoom> entrance = DungeonInfo.getUniqueRooms().stream().filter(r -> r.getType() == RoomType.ENTRANCE).findFirst();
                            if (entrance.isPresent()) {
                                RSA.chat(DungeonRoomScore.score(entrance.get()));
                            } else{
                                RSA.chat("Room Score: Failed to find entrance?");
                            }
                            return 1;
                        })
                );
    }
}