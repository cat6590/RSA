package com.ricedotwho.rsa.module.impl.dungeon.boss.p3.terminals.auto.terminals;

import java.util.List;

public class Solution {
    List<SolutionClick> clicks;

    protected Solution(List<SolutionClick> clicks) {
        this.clicks = clicks;
    }

    public int getLength() {
        return this.clicks.size();
    }

    public SolutionClick getNext() {
        return clicks.getFirst();
    }

    public boolean containsIndex(int index) {
        return this.clicks.stream().anyMatch(c -> c.index() == index);
    }

}
