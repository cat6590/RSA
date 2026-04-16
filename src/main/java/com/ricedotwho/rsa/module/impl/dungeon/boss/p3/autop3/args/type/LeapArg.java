package com.ricedotwho.rsa.module.impl.dungeon.boss.p3.autop3.args.type;

import com.google.gson.JsonObject;
import com.ricedotwho.rsa.module.impl.dungeon.boss.p3.autop3.args.Argument;
import com.ricedotwho.rsa.module.impl.dungeon.boss.p3.autop3.args.RingArgType;
import com.ricedotwho.rsm.component.impl.map.handler.Dungeon;
import net.minecraft.util.Mth;

public class LeapArg extends Argument<Boolean> {
    private final int players;
    private boolean override = false;

    public LeapArg(int players) {
        super(RingArgType.LEAP);
        this.players = Mth.clamp(players, 0, 5);
    }

    @Override
    public boolean check() {
        if (override) {
            override = false;
            return true;
        }
        return Dungeon.getPlayersLeapt() >= players;
    }

    @Override
    public void consume(Boolean bl) {
        override = true;
    }

    @Override
    public void reset() {
        override = false;
    }

    public static LeapArg create(Object arg) {
        return new LeapArg((Integer) arg);
    }

    public void serialize(JsonObject json) {
        json.addProperty(getType().name(), players);
    }

    @Override
    public String stringValue() {
        return "leap " + players;
    }

}
