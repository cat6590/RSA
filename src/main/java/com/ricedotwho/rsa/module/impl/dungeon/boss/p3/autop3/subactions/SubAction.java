package com.ricedotwho.rsa.module.impl.dungeon.boss.p3.autop3.subactions;

import com.google.gson.JsonObject;
import com.ricedotwho.rsm.utils.Accessor;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public abstract class SubAction implements Accessor {
    private final SubActionType type;
    public abstract boolean execute();
    public abstract void serialize(JsonObject json);
}
