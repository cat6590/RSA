package com.ricedotwho.rsa.command.impl;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import com.ricedotwho.rsa.module.impl.player.autopet.AutoPet;
import com.ricedotwho.rsa.module.impl.player.autopet.pet.ChatPetRule;
import com.ricedotwho.rsa.module.impl.player.autopet.pet.IslandPetRule;
import com.ricedotwho.rsa.module.impl.player.autopet.pet.PetRule;
import com.ricedotwho.rsm.RSM;
import com.ricedotwho.rsm.command.Command;
import com.ricedotwho.rsm.command.api.CommandInfo;
import com.ricedotwho.rsm.component.impl.location.Island;
import com.ricedotwho.rsm.utils.ChatUtils;
import net.minecraft.client.multiplayer.ClientSuggestionProvider;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.network.chat.Component;

import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@CommandInfo(name = "ap", aliases = "pet", description = "Auto Auto Pet Command")
public class AutoPetCommand extends Command {

    @Override
    public LiteralArgumentBuilder<ClientSuggestionProvider> build() {
        return literal(name())
                .then(literal("swap")
                        .then(argument("pet", StringArgumentType.greedyString())
                        .executes(this::swap)
                        )
                )
                .then(literal("add")
                        .then(literal("island")
                                .then(argument("island", IslandArgumentType.islandArgument())
                                        .then(argument("pet", StringArgumentType.greedyString())
                                                .executes(this::addIslandRule)
                                        )
                                )
                        )
                        .then(literal("chat")
                                .then(argument("pet uuid", StringArgumentType.word())
                                        .then(argument("regex", StringArgumentType.greedyString())
                                                .executes(this::addChatRule)
                                        )
                                )
                        )
                )
                .then(literal("list")
                        .executes(this::listRules)
                )
                .then(literal("remove")
                        .then(argument("index", IntegerArgumentType.integer(0))
                                .executes(this::removeRule)
                        )
                )
                .then(literal("get")
                        .executes(this::listPets)
                );
    }

    private int listPets(CommandContext<ClientSuggestionProvider> clientSuggestionProviderCommandContext) {
        RSM.getModule(AutoPet.class).listPets();
        return 1;
    }

    private int addIslandRule(CommandContext<ClientSuggestionProvider> ctx) {
        Island island = IslandArgumentType.getIsland(ctx, "island");
        if (island == Island.Unknown) {
            ChatUtils.chat("Invalid island!");
            return 0;
        }
        String petName = ctx.getArgument("pet", String.class);
        AutoPet autoAutoPet = RSM.getModule(AutoPet.class);
        autoAutoPet.addPetRule(new IslandPetRule(petName, autoAutoPet::swapTo, island));
        ChatUtils.chat("Added new pet rule!");
        return 1;
    }

    private int addChatRule(CommandContext<ClientSuggestionProvider> ctx) {
        String regex = StringArgumentType.getString(ctx, "regex");
        if (regex.isBlank()) {
            ChatUtils.chat("Regex must not be blank!");
            return 0;
        }
        String uuid = ctx.getArgument("pet uuid", String.class);
        AutoPet autoAutoPet = RSM.getModule(AutoPet.class);
        autoAutoPet.addPetRule(new ChatPetRule(uuid, autoAutoPet::swapTo, regex));
        ChatUtils.chat("Added new pet rule!");
        return 1;
    }

    private int listRules(CommandContext<ClientSuggestionProvider> ctx) {
        AutoPet autoAutoPet = RSM.getModule(AutoPet.class);
        ChatUtils.chat("------------------");
        int index = 0;
        for (Iterator<PetRule> it = autoAutoPet.iterateRules(); it.hasNext(); ) {
            PetRule petRule = it.next();
            ChatUtils.chat(index + " : " + petRule.toString());
            index++;
        }
        ChatUtils.chat("------------------");
        return 1;
    }

    private int removeRule(CommandContext<ClientSuggestionProvider> ctx) {
        int index = ctx.getArgument("index", Integer.class);
        AutoPet autoAutoPet = RSM.getModule(AutoPet.class);
        autoAutoPet.removeRule(index);
        ChatUtils.chat("Removed pet rule!");
        return 1;
    }

    private int swap(CommandContext<ClientSuggestionProvider> ctx) {
        String petName = ctx.getArgument("pet", String.class);
        if (petName.isEmpty()) {
            ChatUtils.chat("Please enter a pet!");
            return 0;
        }
        AutoPet autoAutoPet = RSM.getModule(AutoPet.class);
        if (!autoAutoPet.isEnabled()) {
            ChatUtils.chat("Please enable auto auto pet!");
            return 0;
        }
        autoAutoPet.swapTo(petName);
        return 1;
    }

    private static class IslandArgumentType implements ArgumentType<Island> {
        private static final Collection<String> EXAMPLES = Stream.of(Island.Hub, Island.Dungeon)
                .map(Island::getName)
                .collect(Collectors.toList());
        private static final Island[] VALUES = Island.values();
        private static final DynamicCommandExceptionType INVALID_ISLAND_EXCEPTION = new DynamicCommandExceptionType(
                island -> Component.literal("Invalid island type : " + island)
        );

        public Island parse(StringReader stringReader) throws CommandSyntaxException {
            String string = stringReader.readUnquotedString();
            Island island = Island.findByEnumName(string);
            if (island == null) {
                throw INVALID_ISLAND_EXCEPTION.createWithContext(stringReader, string);
            } else {
                return island;
            }
        }

        @Override
        public <S> CompletableFuture<Suggestions> listSuggestions(CommandContext<S> context, SuggestionsBuilder builder) {
            return context.getSource() instanceof SharedSuggestionProvider
                    ? SharedSuggestionProvider.suggest(Arrays.stream(VALUES).map(Island::getEnumName), builder)
                    : Suggestions.empty();
        }

        @Override
        public Collection<String> getExamples() {
            return EXAMPLES;
        }

        public static IslandArgumentType islandArgument() {
            return new IslandArgumentType();
        }

        public static Island getIsland(CommandContext<ClientSuggestionProvider> context, String name) {
            return context.getArgument(name, Island.class);
        }
    }
}