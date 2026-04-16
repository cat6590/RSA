package com.ricedotwho.rsa.module.impl.dungeon.autoroutes;

import com.google.common.reflect.TypeToken;
import com.ricedotwho.rsa.module.impl.dungeon.autoroutes.awaits.AwaitClick;
import com.ricedotwho.rsa.module.impl.dungeon.autoroutes.awaits.AwaitEWRaytrace;
import com.ricedotwho.rsa.module.impl.dungeon.autoroutes.awaits.AwaitSecrets;
import lombok.Getter;

import java.lang.reflect.Type;
import java.util.Arrays;

public enum AwaitType {
    CLICK(AwaitClick.class, "awaitClick", new TypeToken<AwaitClick>() {}.getType()),
    SECRETS(AwaitSecrets.class, "awaitSecrets", new TypeToken<AwaitSecrets>() {}.getType()),
    ETHERWARP_TRACE(AwaitEWRaytrace.class, "awaitEWRaytrace", new TypeToken<AwaitEWRaytrace>() {}.getType());

    @Getter
    private final Class<? extends AwaitCondition<?>> clazz;
    @Getter
    private final Type type;
    @Getter
    private final String name;

    AwaitType(Class<? extends AwaitCondition<?>> s, String name, Type type) {
        this.clazz = s;
        this.type = type;
        this.name = name;
    }


    public static AwaitType byClass(Class<? extends AwaitCondition<?>> clazz) {
        return Arrays.stream(AwaitType.values()).filter(n -> n.getClazz().equals(clazz)).findAny().orElse(null);
    }

    public static AwaitType byName(String name) {
        return Arrays.stream(AwaitType.values()).filter(n -> n.getName().equalsIgnoreCase(name)).findAny().orElse(null);
    }
}
