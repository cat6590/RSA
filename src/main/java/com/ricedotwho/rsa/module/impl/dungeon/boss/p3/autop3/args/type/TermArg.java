package com.ricedotwho.rsa.module.impl.dungeon.boss.p3.autop3.args.type;

import com.ricedotwho.rsa.module.impl.dungeon.boss.p3.autop3.args.Argument;
import com.ricedotwho.rsa.module.impl.dungeon.boss.p3.autop3.args.RingArgType;
import com.ricedotwho.rsm.event.impl.game.TerminalEvent;

public class TermArg extends Argument<TerminalEvent.Open> {
    private boolean inTerm = false;

    public TermArg() {
        super(RingArgType.TERM);
    }

    @Override
    public boolean check() {
        return inTerm;
    }

    @Override
    public void consume(TerminalEvent.Open event) {
        inTerm = true;
    }

    @Override
    public void reset() {
        inTerm = false;
    }

    @Override
    public String stringValue() {
        return "term";
    }

    public static TermArg create(Object ignored) {
        return new TermArg();
    }

}
