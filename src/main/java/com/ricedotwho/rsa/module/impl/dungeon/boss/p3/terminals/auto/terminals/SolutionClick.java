package com.ricedotwho.rsa.module.impl.dungeon.boss.p3.terminals.auto.terminals;

import net.minecraft.world.inventory.ClickType;

// Button 0 is middleclick and left click
// Buton 1 is right click
public class SolutionClick {
    private final ClickType type;
    private final int index;
    private final int button;

    public SolutionClick(ClickType type, int index, int button) {
        this.type = type;
        this.index = index;
        this.button = button;
    }

    public ClickType type() {
        return this.type;
    }

    public int index() {
        return this.index;
    }

    public int button() {
        return this.button;
    }


}
