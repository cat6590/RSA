package com.ricedotwho.rsa.module.impl.dungeon.boss.p3.autop3.args.type;

import com.ricedotwho.rsa.module.impl.dungeon.boss.p3.autop3.args.Argument;
import com.ricedotwho.rsa.module.impl.dungeon.boss.p3.autop3.args.RingArgType;

public class TriggerArg extends Argument<Boolean> {

    private boolean triggered = false;

    public TriggerArg() {
        super(RingArgType.TRIGGER);
    }

    @Override
    public boolean check() {
        return triggered;
    }

    @Override
    public void consume(Boolean bl) {
        triggered = bl;
    }

    @Override
    public void reset() {
        triggered = false;
    }

    @Override
    public String stringValue() {
        return "trigger";
    }

    public static TriggerArg create(Object ignored) {
        return new TriggerArg();
    }
}
