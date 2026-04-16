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
import com.ricedotwho.rsa.module.impl.dungeon.boss.BreakerAura;
import com.ricedotwho.rsa.module.impl.dungeon.boss.p3.LavaBounce;
import com.ricedotwho.rsa.module.impl.dungeon.boss.p3.autop3.AutoP3;
import com.ricedotwho.rsa.module.impl.dungeon.boss.p3.autop3.CenterType;
import com.ricedotwho.rsa.module.impl.dungeon.boss.p3.autop3.RingType;
import com.ricedotwho.rsa.module.impl.dungeon.boss.p3.autop3.args.ArgumentManager;
import com.ricedotwho.rsa.module.impl.dungeon.boss.p3.autop3.args.RingArgType;
import com.ricedotwho.rsa.module.impl.dungeon.boss.p3.autop3.recorder.MovementRecorder;
import com.ricedotwho.rsa.module.impl.dungeon.boss.p3.autop3.rings.Ring;
import com.ricedotwho.rsa.module.impl.dungeon.boss.p3.autop3.subactions.SubActionManager;
import com.ricedotwho.rsa.module.impl.dungeon.boss.p3.autop3.subactions.SubActionType;
import com.ricedotwho.rsm.RSM;
import com.ricedotwho.rsm.command.Command;
import com.ricedotwho.rsm.command.api.CommandInfo;
import com.ricedotwho.rsm.data.Pos;
import com.ricedotwho.rsm.utils.ChatUtils;
import lombok.Getter;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientSuggestionProvider;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.apache.commons.lang3.EnumUtils;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@CommandInfo(name = "bbg", aliases = "p3", description = "Auto P3 command")
public class BBGCommand extends Command {

    @Override
    public LiteralArgumentBuilder<ClientSuggestionProvider> build() {
        return literal(name())
                .then(literal("center")
                        .then(argument("centerType", BBGCommand.CenterArgumentType.centerArgument())
                                .executes(this::center)
                        )
                        .executes(r -> this.center(CenterType.ALL)) // Won't center pos on server dw
                )
                .then(literal("undo")
                        .executes(this::undo)
                )
                .then(literal("redo")
                        .executes(this::redo)
                )
                .then(literal("remove")
                        .then(argument("index", IntegerArgumentType.integer())
                                .executes(ctx -> this.removeRing(ctx, IntegerArgumentType.getInteger(ctx, "index")))
                        )
                        .executes(this::removeRing)
                )
                .then(literal("add")
                        .then(argument("ring", BBGCommand.RingArgumentType.ringArgument())
                            .executes(ctx -> addRing(ctx, new HashMap<>()))
                                        .then(argument("args", new AP3ListArgumentType())
                                                .executes(ctx -> {
                                                    Map<AP3ArgType, Ap3Arg<?>> args = AP3ListArgumentType.get(ctx, "args");
                                                    return addRing(ctx, args);
                                                })
                                        )
                        )
                )
                .then(literal("play")
                        .then(argument("route", StringArgumentType.greedyString())
                                .executes(ctx -> {
                                    String route = StringArgumentType.getString(ctx, "route");
                                    MovementRecorder.playRecording(route);
                                    AutoP3.modMessage("Playing %s!", route);
                                    return 1;
                                })
                        )
                )
                .then(literal("load")
                        .then(argument("config", StringArgumentType.greedyString())
                                .executes(ctx -> {
                                    String config = StringArgumentType.getString(ctx, "config");
                                    AutoP3.load(config);
                                    AutoP3.modMessage("Loaded %s", config);
                                    return 1;
                                })
                        )
                )
                .then(literal("loadall")
                        .then(argument("config", StringArgumentType.greedyString())
                                .executes(ctx -> {
                                    String config = StringArgumentType.getString(ctx, "config");
                                    AutoP3.load(config);
                                    BreakerAura.load(config);
                                    LavaBounce.load(config);
                                    AutoP3.modMessage("Loaded All \"%s\"", config);
                                    return 1;
                                })
                        )
                )
                .then(literal("help")
                        .executes(ctx -> {
                            StringBuilder sb = new StringBuilder();
                            sb.append("Help:").append("\nRing Types:\n");
                            for (RingType type : RingType.values()) {
                                sb.append(type.getName()).append(" ");
                            }
                            sb.append("\nGeneral Arguments:\n");
                            sb.append("w<number>, h<number>, l<number>, r<number>, exact, blink<number>, yaw<number>, pitch<number> (movement/blink: route\"route\", command: command\"command\", chat: message\"message\")");
                            sb.append("\n\nArguments: ");
                            for (RingArgType type : RingArgType.values()) {
                                sb.append(type.getAliases().getFirst()).append(" ");
                            }
                            sb.append("\n\nSub Actions: ");
                            for (SubActionType type : SubActionType.values()) {
                                sb.append(type.name().toLowerCase()).append(" ");
                            }
                            AutoP3.modMessage(sb.toString());
                            return 1;
                        })
                );
    }

    private int addRing(CommandContext<ClientSuggestionProvider> ctx, Map<AP3ArgType, Ap3Arg<?>> args) {
        if (Minecraft.getInstance().player == null) return 0;
        RingType type = BBGCommand.RingArgumentType.getRing(ctx, "ring");

        Ring ring = createRing(type, args);
        if (ring == null) return 0;
        RSM.getModule(AutoP3.class).addRing(ring);
        return 1;
    }

    private Ring createRing(RingType type, Map<AP3ArgType, Ap3Arg<?>> args) {
        Pos whl = new Pos(0.5, 1, 0.5);
        ArgumentManager manager = new ArgumentManager();
        SubActionManager subActions = new SubActionManager();
        Map<String, Object> dataMap = new HashMap<>();

        args.forEach((k, v) -> {
            switch (k) {
                case RADIUS -> whl.set((float) v.getValue(), (float) v.getValue(), (float) v.getValue());
                case EXACT -> dataMap.put("exact", true);
                case WIDTH -> whl.x((float) v.getValue());
                case HEIGHT -> whl.y((float) v.getValue());
                case LENGTH -> whl.z((float) v.getValue());
                case YAW -> dataMap.put("yaw", v.getValue());
                case PITCH -> dataMap.put("pitch", v.getValue());
                case ROUTE -> dataMap.put("route", v.getValue());
                case MESSAGE -> dataMap.put("message", v.getValue());
                case COMMAND -> dataMap.put("command", v.getValue());
                case BLINK -> dataMap.put("blink", v.getValue());
                case UUID -> dataMap.put("uuid", v.getValue());

                // conditional
                case TERM, RELIC, LEAP, GROUND, TRIGGER, DELAY, TERM_CLOSE, SECTION, VELOCITY -> {
                    RingArgType a = RingArgType.fromAliases(v.key.toLowerCase());
                    if (a == null) {
                        AutoP3.modMessage("Failed to parse arg type! %s, key: %s, value: %s", k, v.key(), v.getValue());
                        return;
                    }
                    manager.addArg(a.create(v.getValue()));
                }

                // sub actions
                case LOOK, JUMP, EDGE, STOP -> {
                    SubActionType s = EnumUtils.getEnum(SubActionType.class, v.key.toUpperCase());
                    if (s == null) {
                        AutoP3.modMessage("Failed to parse sub action! %s, key: %s, value: %s", k, v.key(), v.getValue());
                        return;
                    }
                    subActions.addAction(s.create());
                }
            }
        });

        for (String s : type.getRequired()) {
            if (!dataMap.containsKey(s)) {
                AutoP3.modMessage("Failed to place ring! %s required the argument %s!", type.getName(),  s);
                return null;
            }
        }

        if (type.getHitResult() != null && mc.hitResult != null && mc.hitResult.getType() != type.getHitResult()) {
            AutoP3.modMessage("Failed to place ring! %s requires you to look at %s!", type.getName(), type.getHitResult() == HitResult.Type.BLOCK ? "block" : "entity"); //  idk why it would need entity but wtv
            return null;
        }

        Pos playerPos = getPlayerPos(dataMap.containsKey("exact"));
        return type.supply(playerPos.subtract(whl.x(), 0, whl.z()), playerPos.add(whl), manager, subActions, dataMap);
    }

    private Pos getPlayerPos(boolean exact) {
        Vec3 pos = mc.player.position();
        if (exact) {
            return new Pos(pos);
        } else {
            return new Pos(Math.round(pos.x() * 2) / 2.0, Math.round(pos.y() * 2) / 2.0, Math.round(pos.z() * 2) / 2.0);
        }
    }

    private int undo(CommandContext<ClientSuggestionProvider> ctx) {
        RSM.getModule(AutoP3.class).undo();
        return 1;
    }

    private int redo(CommandContext<ClientSuggestionProvider> ctx) {
        RSM.getModule(AutoP3.class).redo();
        return 1;
    }

    private int removeRing(CommandContext<ClientSuggestionProvider> ctx, int index) {
        if (Minecraft.getInstance().player == null) return 0;

        if (RSM.getModule(AutoP3.class).removeIndexed(index)) {
            ChatUtils.chat("Removed ring at index " + index);
            return 1;
        }

        ChatUtils.chat("Could not find ring at index " + index);
        return 0;
    }

    private int removeRing(CommandContext<ClientSuggestionProvider> ctx) {
        if (Minecraft.getInstance().player == null) return 0;

        Vec3 position = Minecraft.getInstance().player.position();
        RSM.getModule(AutoP3.class).removeNearest(position);
        return 1;
    }

    private int center(CenterType centerType) {
        if (centerType == null) return 0;
        switch (centerType) {
            case ALL -> {
                centerYaw();
                centerPitch();
                centerPos();
                break;
            }

            case POS -> {
                centerPos();
                break;
            }

            case ANGLES -> {
                centerYaw();
                centerPitch();
                break;
            }

            case YAW -> {
                centerYaw();
            }

            case PITCH -> {
                centerPitch();
            }
        }
        return 1;
    }

    private int center(CommandContext<ClientSuggestionProvider> ctx) {
        CenterType centerType = CenterArgumentType.getType(ctx, "centerType");
        return center(centerType);
    }

    private void centerYaw() {
        if (Minecraft.getInstance().player == null) return;
        Minecraft.getInstance().player.setYRot((Math.round((mc.player.getYRot()) / 45f)) * 45f);
    }

    private void centerPitch() {
        if (Minecraft.getInstance().player == null) return;
        Minecraft.getInstance().player.setXRot(0f);
    }

    private void centerPos() {
        if (Minecraft.getInstance().player == null || !Minecraft.getInstance().isSingleplayer()) return;

        Vec3 position = Minecraft.getInstance().player.position();
        Vec3 target = new Vec3(Mth.floor(position.x) + 0.5d, position.y, Mth.floor(position.z) + 0.5d);
        Minecraft.getInstance().player.setPos(target);
    }


//    private int test(CommandContext<ClientSuggestionProvider> ctx) {
//        if (lastMovement != null && Minecraft.getInstance().player != null) {
//            ChatUtils.chat("Delta : " + Minecraft.getInstance().player.position().subtract(lastMovement).length());
//            ChatUtils.chat("Guess : " + MovementPredictor.getDisplacementMagnitude(new Vec2(1f, 1f)));
//        }
//        if (Minecraft.getInstance().player != null) {
//            lastMovement = Minecraft.getInstance().player.position();
//            Minecraft.getInstance().player.setDeltaMovement(1f, Minecraft.getInstance().player.getDeltaMovement().y, 1f);
//        }
//        return 1;
//    }
//
//    private int test1(CommandContext<ClientSuggestionProvider> ctx) {
//        AutoP3 autoP3 = RSM.getModule(AutoP3.class);
//        if (lastMovement != null && Minecraft.getInstance().player != null) {
//            ChatUtils.chat("Delta : " + Minecraft.getInstance().player.position().subtract(lastMovement).length());
//            ChatUtils.chat("Guess : " + MovementPredictor.getDisplacementFromInput(Minecraft.getInstance().player.getSpeed() * 10, true));
//        }
//        if (Minecraft.getInstance().player != null)
//            lastMovement = Minecraft.getInstance().player.position();
//        autoP3.queueYaw(0f, false);
//        return 1;
//    }

    private static class RingArgumentType implements ArgumentType<RingType> {
        private static final Collection<String> EXAMPLES = Stream.of(RingType.ALIGN, RingType.WALK)
                .map(RingType::getName)
                .collect(Collectors.toList());
        private static final RingType[] VALUES = RingType.values();
        private static final DynamicCommandExceptionType INVALID_RING_EXCEPTION = new DynamicCommandExceptionType(
                ring -> Component.literal("Invalid ring type : " + ring)
        );

        public RingType parse(StringReader stringReader) throws CommandSyntaxException {
            String string = stringReader.readUnquotedString();
            RingType ring = RingType.byName(string);
            if (ring == null) {
                throw INVALID_RING_EXCEPTION.createWithContext(stringReader, string);
            } else {
                return ring;
            }
        }

        @Override
        public <S> CompletableFuture<Suggestions> listSuggestions(CommandContext<S> context, SuggestionsBuilder builder) {
            return context.getSource() instanceof SharedSuggestionProvider
                    ? SharedSuggestionProvider.suggest(Arrays.stream(VALUES).map(RingType::getName), builder)
                    : Suggestions.empty();
        }

        @Override
        public Collection<String> getExamples() {
            return EXAMPLES;
        }

        public static BBGCommand.RingArgumentType ringArgument() {
            return new BBGCommand.RingArgumentType();
        }

        public static RingType getRing(CommandContext<ClientSuggestionProvider> context, String name) {
            return context.getArgument(name, RingType.class);
        }
    }

    private static class CenterArgumentType implements ArgumentType<CenterType> {
        private static final Collection<String> EXAMPLES = Stream.of(CenterType.POS, CenterType.ANGLES)
                .map(CenterType::getName)
                .collect(Collectors.toList());
        private static final CenterType[] VALUES = CenterType.values();
        private static final DynamicCommandExceptionType INVALID_CENTER_EXCEPTION = new DynamicCommandExceptionType(
                ring -> Component.literal("Invalid center type : " + ring)
        );

        public CenterType parse(StringReader stringReader) throws CommandSyntaxException {
            String string = stringReader.readUnquotedString();
            CenterType ring = CenterType.fromName(string);
            if (ring == null) {
                throw INVALID_CENTER_EXCEPTION.createWithContext(stringReader, string);
            } else {
                return ring;
            }
        }

        @Override
        public <S> CompletableFuture<Suggestions> listSuggestions(CommandContext<S> context, SuggestionsBuilder builder) {
            return context.getSource() instanceof SharedSuggestionProvider
                    ? SharedSuggestionProvider.suggest(Arrays.stream(VALUES).map(CenterType::getName), builder)
                    : Suggestions.empty();
        }

        @Override
        public Collection<String> getExamples() {
            return EXAMPLES;
        }

        public static BBGCommand.CenterArgumentType centerArgument() {
            return new BBGCommand.CenterArgumentType();
        }

        public static CenterType getType(CommandContext<ClientSuggestionProvider> context, String name) {
            return context.getArgument(name, CenterType.class);
        }
    }

    public static class AP3ArgumentType implements ArgumentType<Ap3Arg<?>> {
        private static final DynamicCommandExceptionType INVALID =
                new DynamicCommandExceptionType(o -> Component.literal("Invalid arg: " + o));

        public Ap3Arg<?> parse(StringReader reader) throws CommandSyntaxException {
            StringBuilder prefix = new StringBuilder();
            while (reader.canRead() && Character.isLetter(reader.peek())) {
                prefix.append(reader.read());
            }

            String key = prefix.toString();

            AP3ArgType type = AP3ArgType.byPrefix(key);
            if (type == null) throw INVALID.createWithContext(reader, key);

            if (type.getValueClass() == Void.class) {
                return new Ap3Arg<>(type, null, key, false);
            }

            if (type.getValueClass() == String.class) {
                if (reader.canRead() && reader.peek() != '"') throw INVALID.createWithContext(reader, "Expected quoted string");
                String quoted = reader.readQuotedString();
                if (quoted.isEmpty()) throw INVALID.createWithContext(reader, "Expected quoted string");
                return new Ap3Arg<>(type, quoted, key, true);
            }

            int numberStart = reader.getCursor();
            while (reader.canRead() && ("0123456789.".indexOf(reader.peek()) != -1)) reader.skip();
            String part = reader.getString().substring(numberStart, reader.getCursor());

            if (type.getValueClass() == Integer.class) {
                try {
                    return new Ap3Arg<>(type, Integer.parseInt(part), key, true);
                } catch (NumberFormatException e) {
                    throw INVALID.createWithContext(reader, "Expected integer");
                }
            }
            try {
                return new Ap3Arg<>(type, Float.parseFloat(part), key, true);
            } catch (NumberFormatException e) {
                throw INVALID.createWithContext(reader, "Expected float");
            }
        }

        @Override
        public <S> CompletableFuture<Suggestions> listSuggestions(CommandContext<S> context, SuggestionsBuilder builder) {
            String remaining = builder.getRemaining();

            for (AP3ArgType t : AP3ArgType.values()) {
                for (String a : t.getAliases()) {
                    if (a.startsWith(remaining)) {
                        builder.suggest(a);
                    }
                }
            }

            return builder.buildFuture();
        }

        public static Ap3Arg<?> get(CommandContext<ClientSuggestionProvider> context, String name) {
            return context.getArgument(name, Ap3Arg.class);
        }
    }

    @Getter
    public enum AP3ArgType {
        RADIUS(Double.class, "radius", "r"),
        EXACT(Void.class, "exact"),
        WIDTH(Double.class, "width", "w"),
        HEIGHT(Double.class, "height", "h"),
        LENGTH(Double.class, "length", "l"),
        YAW(Double.class, "yaw"),
        PITCH(Double.class, "pitch"),
        ROUTE(String.class, "route"),
        MESSAGE(String.class, "message", "m"),
        COMMAND(String.class, "command", "c"),
        BLINK(Integer.class, "blink", "b"),
        UUID(String.class, "uuid"),

        // conditional
        TERM(Void.class, "term"),
        RELIC(Void.class, "relic"),
        LEAP(Integer.class, "leap"),
        GROUND(Void.class, "ground"),
        TRIGGER(Void.class, "trigger"),
        DELAY(Integer.class, "delay"),
        TERM_CLOSE(Void.class, "termclose", "close"),
        SECTION(Integer.class, "section", "s"),
        VELOCITY(Integer.class, "velobuffered", "velo"),

        // sub actions
        LOOK(Void.class, "look"),
        JUMP(Void.class, "jump"),
        EDGE(Void.class, "edge"),
        STOP(Void.class, "stop");


        private final String[] aliases;
        private final Class<?> valueClass;

        AP3ArgType(Class<?> valueClass, String ... aliases) {
            this.aliases = aliases;
            this.valueClass = valueClass;
        }

        public static AP3ArgType byPrefix(String input) {
            AP3ArgType best = null;
            int len = -1;
            for (AP3ArgType t : values()) {
                for (String a : t.aliases) {
                    if (input.equals(a)) {
                        return t;
                    }
                    if (input.startsWith(a) && a.length() > len) {
                        best = t;
                        len = a.length();
                    }
                }
            }
            return best;
        }
    }

    public static class AP3ListArgumentType implements ArgumentType<Map<AP3ArgType, Ap3Arg<?>>> {

        private final AP3ArgumentType single = new AP3ArgumentType();

        @Override
        public Map<AP3ArgType, Ap3Arg<?>> parse(StringReader reader) throws CommandSyntaxException {
            Map<AP3ArgType, Ap3Arg<?>> result = new EnumMap<>(AP3ArgType.class);

            while (reader.canRead()) {
                int before = reader.getCursor();
                Ap3Arg<?> arg = single.parse(reader);

                if (result.putIfAbsent(arg.type(), arg) != null) {
                    throw CommandSyntaxException.BUILT_IN_EXCEPTIONS.dispatcherParseException().createWithContext(reader, "Duplicate argument: " + arg.type().name());
                }

                if (reader.canRead() && reader.peek() == ' ') {
                    reader.skip();
                }

                if (reader.getCursor() == before) {
                    throw CommandSyntaxException.BUILT_IN_EXCEPTIONS.dispatcherUnknownArgument()
                            .createWithContext(reader);
                }
            }

            return result;
        }

        @Override
        public <S> CompletableFuture<Suggestions> listSuggestions(CommandContext<S> context, SuggestionsBuilder builder) {
            String input = builder.getRemaining();
            StringReader reader = new StringReader(input);
            boolean end = input.endsWith(" ");

            Set<AP3ArgType> used = EnumSet.noneOf(AP3ArgType.class);
            boolean failed = false;

            try {
                while (reader.canRead()) {
                    int before = reader.getCursor();
                    Ap3Arg<?> arg = single.parse(reader);
                    used.add(arg.type());

                    if (reader.canRead() && reader.peek() == ' ') {
                        reader.skip();
                    } else {
                        break;
                    }

                    if (reader.getCursor() == before) break;
                }
            } catch (CommandSyntaxException ignored) {
                failed = true;
            }

            if (!used.isEmpty() && !failed && !end && reader.getCursor() == input.length()) {
                return Suggestions.empty();
            }

            int offset;
            if (failed) {
                int lastSpace = input.lastIndexOf(' ');
                offset = builder.getStart() + (lastSpace == -1 ? 0 : lastSpace + 1);
            } else {
                offset = builder.getStart() + reader.getCursor();
            }
            SuggestionsBuilder b = builder.createOffset(offset);
            String remaining = b.getRemaining().toLowerCase();
            for (AP3ArgType t : AP3ArgType.values()) {
                if (used.contains(t)) continue;
                for (String a : t.getAliases()) {
                    if (a.startsWith(remaining)) {
                        b.suggest(a);
                    }
                }
            }

            return b.buildFuture();
        }

        @SuppressWarnings("unchecked")
        public static Map<AP3ArgType, Ap3Arg<?>> get(CommandContext<?> ctx, String name) {
            return (Map<AP3ArgType, Ap3Arg<?>>) ctx.getArgument(name, Map.class);
        }
    }

    public record Ap3Arg<T>(AP3ArgType type, T value, String key, boolean hasValue) {
        public T getValue() {
            return hasValue ? value : null;
        }
    }
}