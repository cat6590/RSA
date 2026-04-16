package com.ricedotwho.rsa.module.impl.dungeon.autoroutes;

import com.google.gson.JsonObject;
import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
public abstract class AwaitCondition<T> {
    @Getter
    private final AwaitType type;

    public abstract boolean test(Node node);
    protected abstract void consume(T event);

    public abstract void onEnter();
    public abstract void reset();
    public abstract void serialize(JsonObject json);

}
