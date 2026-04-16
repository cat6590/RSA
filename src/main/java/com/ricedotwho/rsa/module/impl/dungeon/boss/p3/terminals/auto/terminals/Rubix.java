package com.ricedotwho.rsa.module.impl.dungeon.boss.p3.terminals.auto.terminals;

import com.ricedotwho.rsa.module.impl.dungeon.boss.p3.terminals.auto.AutoTerms;
import net.minecraft.network.protocol.game.ClientboundOpenScreenPacket;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.ArrayList;
import java.util.List;

public class Rubix extends Terminal {

    protected Rubix(ClientboundOpenScreenPacket packet, AbstractContainerMenu menu) {
        super(TerminalType.RUBIX, packet, menu);
    }
    public static final Item[] COLOR_ORDER = {Items.ORANGE_STAINED_GLASS_PANE, Items.YELLOW_STAINED_GLASS_PANE, Items.GREEN_STAINED_GLASS_PANE, Items.BLUE_STAINED_GLASS_PANE, Items.RED_STAINED_GLASS_PANE};

    @Override
    public TerminalState getNextState() {
        if (this.solution == null) throw new IllegalStateException("Tried to get next state without solving!");

        List<HashInfo> infos = new ArrayList<>(this.getType().getSlotCount());
        SolutionClick solutionClick = solution.getNext();
        for (int i = 0; i < this.getType().getSlotCount(); i++) {
            Slot slot = this.terminalContainer.getSlot(i);
            HashInfo hashInfo = new HashInfo(slot.getItem());
            if (slot.index == solutionClick.index()) {
                int colorIndex = ((RubixSolutionClick) solutionClick).colorIndex();
                if (solutionClick.button() == 0) {
                    // Need to override these because we pick the item up
                    // Meaning it gets set to air, so we can't actually set them from the itemStack
                    hashInfo.setItem(COLOR_ORDER[(colorIndex + 1) % COLOR_ORDER.length]);
                    hashInfo.setEnchanted(false);
                    hashInfo.setSize(1);
                } else {
                    // Need to override these because we pick the item up
                    // Meaning it gets set to air, so we can't actually set them from the itemStack
                    hashInfo.setItem(COLOR_ORDER[(colorIndex - 1 + COLOR_ORDER.length) % COLOR_ORDER.length]);
                    hashInfo.setEnchanted(false);
                    hashInfo.setSize(1);
                }
//                ChatUtils.chat("Predicting in slot : " + slot.index + " : " + hashInfo.getItem());
//                ChatUtils.chat("old color : " + COLOR_ORDER[colorIndex]);
            }
            infos.add(hashInfo);
        }

        return Terminal.getTerminalState(TerminalType.RUBIX, infos);
    }

    @Override
    public TerminalState getCurrentState() {
        List<HashInfo> infos = new ArrayList<>(this.getType().getSlotCount());
        for (int i = 0; i < this.getType().getSlotCount(); i++) {
            Slot slot = this.terminalContainer.getSlot(i);
            infos.add(new HashInfo(slot.getItem()));
        }

        return Terminal.getTerminalState(TerminalType.RUBIX, infos);
    }

    @Override
    public void solve() {
        super.solve();

        List<Integer> rubixSlots = new ArrayList<>();

        for (Slot slot : this.terminalContainer.slots) {
            ItemStack stack = slot.getItem();
            if (stack.isEmpty()) continue;
            if (stack.getItem() == Items.BLACK_STAINED_GLASS_PANE) continue;
            if (!isRubixPane(stack.getItem())) continue;
            rubixSlots.add(slot.index);
        }

        int minIndex = -1;
        int minTotal = Integer.MAX_VALUE;

        for (int targetIndex = 0; targetIndex < COLOR_ORDER.length; targetIndex++) {
            int totalClicks = 0;

            for (Integer slot : rubixSlots) {
                ItemStack stack = this.terminalContainer.getSlot(slot).getItem();
                int currentIndex = indexOf(COLOR_ORDER, stack.getItem());

                int clockwise = (targetIndex - currentIndex + COLOR_ORDER.length) % COLOR_ORDER.length;
                int counterClockwise = (currentIndex - targetIndex + COLOR_ORDER.length) % COLOR_ORDER.length;

                totalClicks += Math.min(clockwise, counterClockwise);
            }

            if (totalClicks < minTotal) {
                minTotal = totalClicks;
                minIndex = targetIndex;
            }
        }

        List<SolutionClick> solutionClicks = new ArrayList<>();

        for (Integer slot : rubixSlots) {
            ItemStack stack = this.terminalContainer.getSlot(slot).getItem();
            int currentIndex = indexOf(COLOR_ORDER, stack.getItem());

            int clockwise = (minIndex - currentIndex + COLOR_ORDER.length) % COLOR_ORDER.length;
            int counterClockwise = (currentIndex - minIndex + COLOR_ORDER.length) % COLOR_ORDER.length;

            if (clockwise <= counterClockwise) {
                for (int j = 0; j < clockwise; j++) {
                    solutionClicks.add(new RubixSolutionClick(ClickType.PICKUP, slot, 0, currentIndex));
                }
            } else {
                for (int j = 0; j < counterClockwise; j++) {
                    solutionClicks.add(new RubixSolutionClick(ClickType.PICKUP, slot, 1, currentIndex));
                }
            }
        }

        this.solution = new Solution(solutionClicks);
        this.solveState = SolveState.SOLVED;
    }

    private <T> int indexOf(T[] array, T val) {
        for (int i = 0; i < array.length; i++) {
            if (array[i] == val) return i;
        }
        throw new IndexOutOfBoundsException("Could not find color : " + ((Item) val).getName().getString());
    }

    private boolean isRubixPane(Item item) {
        return item == Items.BLUE_STAINED_GLASS_PANE
                || item == Items.RED_STAINED_GLASS_PANE
                || item == Items.ORANGE_STAINED_GLASS_PANE
                || item == Items.YELLOW_STAINED_GLASS_PANE
                || item == Items.GREEN_STAINED_GLASS_PANE;
    }

    @Override
    public boolean isEnabled() {
        return AutoTerms.getTerminals().get("Rubix");
    }

    protected static Rubix supply(ClientboundOpenScreenPacket packet, AbstractContainerMenu menu) {
        return new Rubix(packet, menu);
    }
}
