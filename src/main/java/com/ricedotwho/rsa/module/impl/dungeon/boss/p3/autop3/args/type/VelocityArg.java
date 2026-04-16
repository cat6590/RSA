package com.ricedotwho.rsa.module.impl.dungeon.boss.p3.autop3.args.type;

import com.google.gson.JsonObject;
import com.ricedotwho.rsa.module.impl.dungeon.boss.p3.autop3.args.Argument;
import com.ricedotwho.rsa.module.impl.dungeon.boss.p3.autop3.args.RingArgType;
import com.ricedotwho.rsa.module.impl.movement.VelocityBuffer;

public class VelocityArg extends Argument<Void> {
    private final int required;


    public VelocityArg(int required) {
        super(RingArgType.VELOCITY);
        this.required = required;
    }

    @Override
    public boolean check() {
        return VelocityBuffer.getBufferedCount() >= required;
    }

    @Override
    public void consume(Void ignored) {

    }

    @Override
    public void reset() {

    }

    @Override
    public String stringValue() {
        return "velo";
    }

    public void serialize(JsonObject json) {
        json.addProperty(getType().name(), this.required);
    }

    public static VelocityArg create(Object obj) {
        return new VelocityArg((int) obj);
    }

}
