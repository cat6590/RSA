package com.ricedotwho.rsa.module.impl.dungeon.boss.p3.autop3.args.type;

import com.google.gson.JsonObject;
import com.ricedotwho.rsa.module.impl.dungeon.boss.p3.autop3.args.Argument;
import com.ricedotwho.rsa.module.impl.dungeon.boss.p3.autop3.args.RingArgType;

public class DelayArg extends Argument<Void> {
    private final int delay;
    private long ran = 0;
    public DelayArg(int delay) {
        super(RingArgType.DELAY);
        this.delay = delay;
    }

    @Override
    public boolean check() {
        long ago = System.currentTimeMillis() - ran;
        if (ran == 0 || ago > delay + 200) {
            ran = System.currentTimeMillis();
            return false;
        }
        return ago >= delay;
    }

    @Override
    public void consume(Void event) {

    }

    @Override
    public void reset() {
        ran = 0;
    }

    public static DelayArg create(Object obj) {
        return new DelayArg((int) obj);
    }

    @Override
    public void serialize(JsonObject json) {
        json.addProperty(getType().name(), delay);
    }

    @Override
    public String stringValue() {
        return "delay " + delay;
    }
}
