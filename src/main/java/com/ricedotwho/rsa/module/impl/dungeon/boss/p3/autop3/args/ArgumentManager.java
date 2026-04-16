package com.ricedotwho.rsa.module.impl.dungeon.boss.p3.autop3.args;

import com.google.gson.JsonObject;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;

public record ArgumentManager(HashMap<RingArgType, Argument<?>> args) {
    public ArgumentManager() {
        this(new HashMap<>());
    }

    public boolean check() {
        for (Argument<?> arg : args.values()) {
            if (!arg.check()) return true;
        }
        return false;
    }

    public Collection<Argument<?>> getArgs() {
        return args.values();
    }

    public void addArg(Argument<?> argument) {
        args.put(argument.getType(), argument);
    }

    public  <T> void consume(Class<? extends Argument<T>> clazz, T value) {
        Argument<T> argument = getArg(clazz);
        if (argument == null) return;
        argument.consume(value);
    }

    public <T extends Argument<?>> T getArg(Class<T> clazz) {
        Argument<?> arg = args.get(RingArgType.byClass(clazz));
        if (arg == null) return null;
        return clazz.cast(arg);
    }

    public JsonObject serialize() {
        JsonObject obj = new JsonObject();
        for (Argument<?> arg : args.values()) {
            arg.serialize(obj);
        }
        return obj;
    }

    public boolean has(RingArgType type) {
        return args.containsKey(type);
    }

    public void reset() {
        getArgs().forEach(Argument::reset);
    }

    public String getList(String before) {
        if (args.isEmpty()) return "";
        StringBuilder sb = new StringBuilder();
        sb.append(before);
        if (!before.isBlank()) sb.append(", ");
        else sb.append("(");
        List<Argument<?>> args = getArgs().stream().toList();
        for (int i = 0; i < args.size(); i++) {
            Argument<?> arg = args.get(i);
            boolean last = i == args.size() - 1;
            sb.append(arg.stringValue());
            if (!last) sb.append(", ");
        }
        sb.append(")");
        return sb.toString();
    }
}
