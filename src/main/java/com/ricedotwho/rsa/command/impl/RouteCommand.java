package com.ricedotwho.rsa.command.impl;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import com.ricedotwho.rsa.RSA;
import com.ricedotwho.rsa.module.impl.dungeon.autoroutes.*;
import com.ricedotwho.rsa.module.impl.dungeon.autoroutes.awaits.AwaitClick;
import com.ricedotwho.rsa.module.impl.dungeon.autoroutes.awaits.AwaitEWRaytrace;
import com.ricedotwho.rsa.module.impl.dungeon.autoroutes.awaits.AwaitSecrets;
import com.ricedotwho.rsa.module.impl.dungeon.autoroutes.nodes.UseNode;
import com.ricedotwho.rsm.RSM;
import com.ricedotwho.rsm.command.Command;
import com.ricedotwho.rsm.command.api.CommandInfo;
import com.ricedotwho.rsm.component.impl.location.Island;
import com.ricedotwho.rsm.component.impl.location.Location;
import com.ricedotwho.rsm.component.impl.map.Map;
import com.ricedotwho.rsm.component.impl.map.map.Room;
import net.minecraft.client.multiplayer.ClientSuggestionProvider;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@CommandInfo(name = "autoroute", aliases = {"r", "ar", "route"}, description = "Handles creating autoroutes")
public class RouteCommand extends Command {

    @Override
    public LiteralArgumentBuilder<ClientSuggestionProvider> build() {
        return literal(name())
                .then(literal("add")
                        .then(argument("node", NodeArgumentType.nodeArgument())
                                .executes((ctx) -> addNode(ctx, 0, false, false, false))
                                .then(argument("await secrets", IntegerArgumentType.integer(0))
                                        .executes((ctx) -> addNode(ctx, IntegerArgumentType.getInteger(ctx, "await secrets"), false, false, false))
                                        .then(argument("await click", BoolArgumentType.bool())
                                                .executes((ctx) -> addNode(ctx, IntegerArgumentType.getInteger(ctx, "await secrets"), BoolArgumentType.getBool(ctx, "await click"), false, false))
                                                .then(argument("start",  BoolArgumentType.bool())
                                                        .executes(ctx -> addNode(ctx, IntegerArgumentType.getInteger(ctx, "await secrets"), BoolArgumentType.getBool(ctx, "await click"), BoolArgumentType.getBool(ctx, "start"), false))
                                                        .then(argument("await ew raytrace", BoolArgumentType.bool())
                                                                .executes((ctx) -> addNode(ctx, IntegerArgumentType.getInteger(ctx, "await secrets"), BoolArgumentType.getBool(ctx, "await click"), BoolArgumentType.getBool(ctx, "start"), BoolArgumentType.getBool(ctx, "await ew raytrace")))
                                                                .then(argument("extra", StringArgumentType.string())
                                                                    .executes(ctx -> addNode(ctx, IntegerArgumentType.getInteger(ctx, "await secrets"), BoolArgumentType.getBool(ctx, "await click"), BoolArgumentType.getBool(ctx, "start"), BoolArgumentType.getBool(ctx, "await ew raytrace")))
                                                                )
                                                        )
                                                )
                                                .executes((ctx) -> addNode(ctx, IntegerArgumentType.getInteger(ctx, "await secrets"), BoolArgumentType.getBool(ctx, "await click"), false, false))

                                        )
                                )
                                .then(argument("start",  BoolArgumentType.bool())
                                        .executes(ctx -> addNode(ctx, 0, false, BoolArgumentType.getBool(ctx, "start"), false))
                                )
                        )
                )
                .then(literal("clear")
                        .executes(RouteCommand::clearNodes)
                )
                .then(literal("remove")
                        .executes(RouteCommand::removeNode)
                )
                .then(literal("load")
                        .executes(RouteCommand::loadNodes)
                )
                .then(literal("redo")
                        .executes(RouteCommand::redoNode)
                )
//                .then(literal("backup")
//                        .executes((ctx) -> {
//                            AutoroutesFileManager.createBackup();
//                            RSA.chat("Made backup!");
//                            return 1;
//                        })
//                )
                .then(literal("undo")
                        .executes(RouteCommand::undoNode)
                );
    }

    private static int loadNodes(CommandContext<ClientSuggestionProvider> ctx) {
        RSM.getModule(AutoRoutes.class).load();
        RSA.chat("Loaded nodes");
        return 1;
    }

    private static int clearNodes(CommandContext<ClientSuggestionProvider> ctx) {
        Room room = Map.getCurrentRoom();
        if (room == null || room.getUniqueRoom() == null) {
            RSA.chat("Failed to find room!");
            return 0;
        }

        if (!RSM.getModule(AutoRoutes.class).clearNodes(room.getUniqueRoom())) {
            RSA.chat("No nodes found in this room!");
            return 0;
        }
        RSA.chat("Cleared all nodes!");
        return 1;
    }

    private static int removeNode(CommandContext<ClientSuggestionProvider> ctx) {
        Room room = Map.getCurrentRoom();
        if (room == null || room.getUniqueRoom() == null) {
            RSA.chat("Failed to find room!");
            return 0;
        }

        if (!RSM.getModule(AutoRoutes.class).removeNearest(room.getUniqueRoom())) {
            RSA.chat("No nodes found in this room!");
            return 0;
        }
        RSA.chat("Removed node!");
        return 1;
    }

    private static int undoNode(CommandContext<ClientSuggestionProvider> ctx) {
        Room room = Map.getCurrentRoom();
        if (room == null || room.getUniqueRoom() == null) {
            RSA.chat("Failed to find room!");
            return 0;
        }

        if (!RSM.getModule(AutoRoutes.class).undoNode(room.getUniqueRoom())) {
            RSA.chat("No nodes found in this room!");
            return 0;
        }
        return 1;
    }

    private static int redoNode(CommandContext<ClientSuggestionProvider> ctx) {
        Room room = Map.getCurrentRoom();
        if (room == null || room.getUniqueRoom() == null) {
            RSA.chat("Failed to find room!");
            return 0;
        }

        if (!RSM.getModule(AutoRoutes.class).redoNode(room.getUniqueRoom())) {
            RSA.chat("No nodes found in this room!");
            return 0;
        }
        return 1;
    }

    private static int addNode(CommandContext<ClientSuggestionProvider> ctx, int secrets, boolean click, boolean start, boolean raytrace) {
        Room room = Map.getCurrentRoom();
        if (!Location.getArea().is(Island.Dungeon) || room == null) {
            RSA.chat("Failed to add node, please enter a dungeon!");
            return 0;
        }

        if (room.getUniqueRoom() == null) {
            RSA.chat("Null unique room!");
            return 0;
        }
        List<AwaitCondition<?>> conditions = new ArrayList<>();
        if (secrets > 0) {
            conditions.add(new AwaitSecrets(secrets));
        }
        if (click) conditions.add(new AwaitClick());
        if (raytrace) conditions.add(new AwaitEWRaytrace());

        AwaitManager awaits = null;
        if (!conditions.isEmpty()) awaits = new AwaitManager(conditions);

        NodeType type = NodeArgumentType.getNode(ctx, "node");
        Node node = type.supply(room.getUniqueRoom(), awaits, start);
        if (node == null) {
            RSA.chat("Failed to add node, invalid player information!");
            return 0;
        }

        // ???
        if (node instanceof UseNode n) {
            if (ctx.getInput().toLowerCase().contains(" sneak")) {
                n.setSneak(true);
            }
        }


        RSM.getModule(AutoRoutes.class).addNode(node, room.getUniqueRoom());
        RSA.chat("Added " + type + " node!");
        return 1;
    }

    private static class NodeArgumentType implements ArgumentType<NodeType> {
        private static final Collection<String> EXAMPLES = Stream.of(NodeType.ETHERWARP, NodeType.BOOM)
                .map(NodeType::getName)
                .collect(Collectors.toList());
        private static final NodeType[] VALUES = NodeType.values();
        private static final DynamicCommandExceptionType INVALID_NODE_EXCEPTION = new DynamicCommandExceptionType(
                node -> Component.literal("Invalid node type : " + node)
        );

        public NodeType parse(StringReader stringReader) throws CommandSyntaxException {
            String string = stringReader.readUnquotedString();
            NodeType node = NodeType.byName(string);
            if (node == null) {
                throw INVALID_NODE_EXCEPTION.createWithContext(stringReader, string);
            } else {
                return node;
            }
        }

        @Override
        public <S> CompletableFuture<Suggestions> listSuggestions(CommandContext<S> context, SuggestionsBuilder builder) {
            return context.getSource() instanceof SharedSuggestionProvider
                    ? SharedSuggestionProvider.suggest(Arrays.stream(VALUES).map(NodeType::getName), builder)
                    : Suggestions.empty();
        }

        @Override
        public Collection<String> getExamples() {
            return EXAMPLES;
        }

        public static NodeArgumentType nodeArgument() {
            return new NodeArgumentType();
        }

        public static NodeType getNode(CommandContext<ClientSuggestionProvider> context, String name) {
            return context.getArgument(name, NodeType.class);
        }
    }
}