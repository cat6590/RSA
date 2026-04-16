package com.ricedotwho.rsa.module.impl.dungeon.boss.p3.terminals.auto.terminals;

import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.Item;

import java.util.HashMap;

public class ClickedSlotsTracker {
    public HashMap<Integer, Item> clickedSlots = new HashMap<>();

    public void clickSlot(Slot slot) {
        this.clickedSlots.put(slot.index, slot.getItem().getItem());
    }

    public boolean contains(Slot slot) {
        return this.clickedSlots.containsKey(slot.index) && this.clickedSlots.get(slot.index) == slot.getItem().getItem();
    }

    public void clear() {
        this.clickedSlots.clear();
    }
}
