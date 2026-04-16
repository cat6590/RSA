package com.ricedotwho.rsa.module.impl.dungeon.boss.p3.terminals.auto.terminals;

import lombok.Getter;

public class TerminalState {
    private final TerminalType type;

    @Getter
    private final int hash;

    public TerminalState(TerminalType type, int hash) {
        this.type = type;
        this.hash = hash;
    }

    public boolean matches(TerminalState other) {
        return this.type == other.type && this.hash == other.hash;
    }



}
