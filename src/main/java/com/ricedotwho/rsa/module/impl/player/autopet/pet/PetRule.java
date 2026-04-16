package com.ricedotwho.rsa.module.impl.player.autopet.pet;

import com.google.gson.JsonObject;
import lombok.Getter;

import java.util.function.Consumer;

public abstract class PetRule {
    @Getter
    private final String id;
    protected transient final Consumer<String> callback;

    public PetRule(String id, Consumer<String> callback) {
        this.id = id;
        this.callback = callback;
    }

    public JsonObject serialize() {
        JsonObject obj = new JsonObject();
        obj.addProperty("type", getType().name());
        obj.addProperty("id", this.id);
        return obj;
    }

    protected abstract RuleType getType();
}
