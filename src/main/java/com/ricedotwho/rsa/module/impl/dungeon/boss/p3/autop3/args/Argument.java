package com.ricedotwho.rsa.module.impl.dungeon.boss.p3.autop3.args;

import com.google.gson.JsonObject;
import com.ricedotwho.rsm.utils.Accessor;
import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
public abstract class Argument<T> implements Accessor {
    @Getter
    private final RingArgType type;

    public abstract boolean check();

    public abstract void consume(T event);

    public abstract void reset();

    public void serialize(JsonObject json) {
        json.addProperty(getType().name(), true);
    }

    public abstract String stringValue();
}
