package com.ricedotwho.rsa.module.impl.dungeon.boss.p3.autop3.args;

import com.ricedotwho.rsa.module.impl.dungeon.boss.p3.autop3.args.type.*;
import lombok.Getter;

import java.util.Arrays;
import java.util.List;
import java.util.function.Function;

public enum RingArgType {
    TERM(TermArg::create, TermArg.class, List.of("term")),
    RELIC(RelicArg::create, RelicArg.class, List.of("relic")),
    LEAP(LeapArg::create, LeapArg.class, List.of("leap")),
    GROUND(GroundArg::create, GroundArg.class, List.of("ground", "g")),
    TRIGGER(TriggerArg::create, TriggerArg.class, List.of("trigger", "click", "c")),
    DELAY(DelayArg::create, DelayArg.class, List.of("delay", "d")),
    TERM_CLOSE(TermCloseArg::create, TermCloseArg.class, List.of("termclose", "close", "tc")),
    SECTION(SectionArg::create, SectionArg.class, List.of("section", "s")),
    VELOCITY(VelocityArg::create, VelocityArg.class, List.of("velobuffered", "velo"));

    private final Function<Object, Argument<?>> factory;
    @Getter
    private final List<String> aliases;
    @Getter
    private final Class<? extends Argument<?>> clazz;

    RingArgType(Function<Object, Argument<?>> factory, Class<? extends Argument<?>> clazz , List<String> aliases) {
        this.factory = factory;
        this.clazz = clazz;
        this.aliases = aliases;
    }

    public Argument<?> create(Object arg) {
        return this.factory.apply(arg);
    }

    public static RingArgType fromAliases(String string) {
        for (RingArgType type : values()) {
            if (type.getAliases().contains(string)) return type;
        }
        return null;
    }

    public static RingArgType byClass(Class<? extends Argument<?>> clazz) {
        return Arrays.stream(RingArgType.values()).filter(n -> n.getClazz().equals(clazz)).findAny().orElse(null);
    }
}
