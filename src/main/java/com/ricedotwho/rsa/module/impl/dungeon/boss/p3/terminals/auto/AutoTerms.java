package com.ricedotwho.rsa.module.impl.dungeon.boss.p3.terminals.auto;

import com.google.common.collect.Lists;
import com.google.common.primitives.Shorts;
import com.google.common.primitives.SignedBytes;
import com.ricedotwho.rsa.RSA;
import com.ricedotwho.rsa.event.impl.RawTickEvent;
import com.ricedotwho.rsa.module.impl.dungeon.boss.Blink;
import com.ricedotwho.rsa.module.impl.dungeon.boss.p3.terminals.auto.terminals.*;
import com.ricedotwho.rsa.module.impl.dungeon.boss.p3.terminals.auto.terminals.Colors;
import com.ricedotwho.rsa.module.impl.dungeon.boss.p3.terminals.auto.terminals.Melody;
import com.ricedotwho.rsm.RSM;
import com.ricedotwho.rsm.component.impl.Terminals;
import com.ricedotwho.rsm.component.impl.location.Island;
import com.ricedotwho.rsm.component.impl.location.Location;
import com.ricedotwho.rsm.event.api.EventPriority;
import com.ricedotwho.rsm.event.api.SubscribeEvent;
import com.ricedotwho.rsm.event.impl.client.InputPollEvent;
import com.ricedotwho.rsm.event.impl.client.PacketEvent;
import com.ricedotwho.rsm.event.impl.game.ClientTickEvent;
import com.ricedotwho.rsm.event.impl.render.Render3DEvent;
import com.ricedotwho.rsm.event.impl.world.WorldEvent;
import com.ricedotwho.rsm.module.Module;
import com.ricedotwho.rsm.module.api.Category;
import com.ricedotwho.rsm.module.api.ModuleInfo;
import com.ricedotwho.rsm.ui.clickgui.settings.group.GroupSetting;
import com.ricedotwho.rsm.ui.clickgui.settings.impl.*;
import com.ricedotwho.rsm.utils.ChatUtils;
import com.ricedotwho.rsm.utils.DungeonUtils;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import lombok.Getter;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.MenuAccess;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.core.NonNullList;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.HashedStack;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.common.ClientboundPingPacket;
import net.minecraft.network.protocol.game.*;
import net.minecraft.world.entity.player.Input;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

import java.util.List;

@Getter
@ModuleInfo(aliases = "AutoTerms", id = "AutoTerms", category = Category.DUNGEONS)
public class AutoTerms extends Module {
    private long lastClickTime = 0L;
    private boolean clickedWindow = false;
    private boolean firstClick = true;
    private Terminal terminal;

    private int lastPingTicks = 100;

    private AbstractContainerMenu terminalContainer;
    private final ClickedSlotsTracker clickedSlotsTracker;

    private TerminalState predictedState = null;

    private final NumberSetting firstClickDelay = new NumberSetting("First Click Delay", 200d, 600d, 400d, 5d);
    private final NumberSetting delay = new NumberSetting("Delay", 100d, 250d, 150d, 5d);
    private final NumberSetting breakThreshold = new NumberSetting("Break Threshold", 200d, 800d, 500d, 10d);

    @Getter private static final MultiBoolSetting terminals = new MultiBoolSetting("Terminals", List.of("Colours", "Melody", "Numbers", "Red Green", "Rubix", "Starts With"), List.of("Colours", "Melody", "Numbers", "Red Green", "Rubix", "Starts With"));

    private final BooleanSetting melodySkip = new BooleanSetting("Melody Skip", true);
    private final BooleanSetting melodySkipFirst = new BooleanSetting("Don't Skip First", true);
    private final BooleanSetting announceMelody = new BooleanSetting("Announce Melody", true);

    private final BooleanSetting noLimbo = new BooleanSetting("No Limbo", true);

    private final GroupSetting<InvWalk> invWalkGroup = new GroupSetting<>("Invwalk", new InvWalk(this));


    public AutoTerms() {
        this.clickedSlotsTracker = new ClickedSlotsTracker();
        registerProperty(
                terminals,
                firstClickDelay,
                delay,
                breakThreshold,
                melodySkip,
                melodySkipFirst,
                announceMelody,
                noLimbo,
                invWalkGroup
        );
    }

    @SubscribeEvent
    public void onLoadWorld(WorldEvent.Load event) {
        close();
        lastPingTicks = 100;
    }

    @SubscribeEvent
    public void render(Render3DEvent.Last event) {
        // issues with race conditions
        if (!isInTerm() || terminal instanceof Melody) return;

        if (terminal.shouldSolve() && !terminal.isSolved()) {
            terminal.solve();
        }

        if (!terminal.isSolved()) return;

        if (predictedState != null) {
            TerminalState newState = this.terminal.getCurrentState();
            if (!predictedState.matches(newState)) {
//                ChatUtils.chat("First click detected!");
//                ChatUtils.chat("Old : " + predictedState.getHash());
//                ChatUtils.chat("new : " + newState.getHash());
                this.firstClick = true;
                this.lastClickTime = System.currentTimeMillis();
                this.clickedSlotsTracker.clear();
            }
            this.predictedState = null;
        }

        if (!terminal.isEnabled()) return;

        // todo: DO NOT DO THIS ON RENDER!

        if (firstClick && (System.currentTimeMillis() - lastClickTime < firstClickDelay.getValue().longValue())) return;

        if (System.currentTimeMillis() - lastClickTime < delay.getValue().longValue()) return;

        if (System.currentTimeMillis() - lastClickTime > breakThreshold.getValue().longValue()) {
            clickedWindow = false;
        }

        // Why is there another check here?
        if (!isInTerm() || clickedWindow) return;

        if (!terminal.isSolved()) return;

        Solution solution = terminal.getSolution();

        if (solution.getLength() < 1) return;

        sendWindowClick(solution.getNext());
        lastClickTime = System.currentTimeMillis();
        clickedWindow = true;
        firstClick = false;
    }


    // Need to change this for inv walk
    private static void sendWindowClick(int windowID, SolutionClick click, Player player, AbstractContainerMenu abstractContainerMenu) {
        if (windowID != abstractContainerMenu.containerId) {
            RSA.chat("Window ID mismatch!");
            return;
        }

        ClientPacketListener connection = Minecraft.getInstance().getConnection();
        if (connection == null) return;

        NonNullList<Slot> nonNullList = abstractContainerMenu.slots;
        int l = nonNullList.size();
        List<ItemStack> list = Lists.newArrayListWithCapacity(l);

        for (Slot slot : nonNullList) {
            list.add(slot.getItem().copy());
        }

        abstractContainerMenu.clicked(click.index(), click.button(), click.type(), player);

        Int2ObjectMap<HashedStack> int2ObjectMap = new Int2ObjectOpenHashMap<>();

        for (int m = 0; m < l; m++) {
            ItemStack itemStack = list.get(m);
            ItemStack itemStack2 = nonNullList.get(m).getItem();
            if (!ItemStack.matches(itemStack, itemStack2)) {
                int2ObjectMap.put(m, HashedStack.create(itemStack2, connection.decoratedHashOpsGenenerator()));
            }
        }

        HashedStack hashedStack = HashedStack.create(abstractContainerMenu.getCarried(), connection.decoratedHashOpsGenenerator());
        connection.send(new ServerboundContainerClickPacket(windowID, abstractContainerMenu.getStateId(), Shorts.checkedCast(click.index()), SignedBytes.checkedCast(click.button()), click.type(), int2ObjectMap, hashedStack));
    }

    public void sendWindowClick(SolutionClick click) {
        if (Minecraft.getInstance().player == null) return;
        if (!isInTerm() || click.index() < 0 || click.index() >= terminal.getType().getSlotCount()) return;
        // Make some checks
        if (this.terminal instanceof StartsWith || this.terminal instanceof Colors)
            this.clickedSlotsTracker.clickSlot(this.terminalContainer.getSlot(click.index()));
        sendWindowClick(terminal.getWindowID(), click, Minecraft.getInstance().player, this.terminalContainer);
    }

    // This works for strafe but not for forwards and backwards for some reason
    @SubscribeEvent
    public void onPollInput(InputPollEvent event) {
        if (this.invWalkGroup.getValue().melodyMoveCounter < 1) return;

        if (Minecraft.getInstance().screen == null && !this.isInTerm()) {
            this.invWalkGroup.getValue().melodyMoveCounter = 0;
            return;
        }

        Input oldInputs = event.getClientInput();
        Input newInputs = new Input(false, false, false, false, false, oldInputs.shift(), false);
        event.getInput().apply(newInputs);

        this.invWalkGroup.getValue().melodyMoveCounter--;
    }

    @SubscribeEvent
    public void onTick(ClientTickEvent.Start event) {
        lastPingTicks--;
        if (!isInTerm()) {
            firstClick = true;
            this.clickedSlotsTracker.clear();
            lastClickTime = System.currentTimeMillis();
        }
    }

    public boolean isAnnounceMelody() {
        return announceMelody.getValue();
    }

    @SubscribeEvent
    public void onRawTick(RawTickEvent event) {
        if (isInTerm() && terminal instanceof Melody melody && melody.isEnabled()) {
            if (melody.onTickStart(this)) {
                this.invWalkGroup.getValue().onMelodyClick();
            }
        }
    }

    @SubscribeEvent
    public void onPlayerTick(ClientTickEvent.Player event) {
        if (Minecraft.getInstance().player == null || Location.getArea() != Island.Dungeon || !DungeonUtils.isPositionInF7Boss(Minecraft.getInstance().player.position()) || !isInTerm() || !invWalkGroup.getValue().isEnabled()) return;
        if (lastPingTicks < 0) event.setCancelled(true);
    }

    // If a gui is open request is sent at the same time as term aura sends a click packet while not in term,
    // if the original gui opens first, the term gui will open after the client has opened it

    /// This should run before {@link Terminals#onPacket(PacketEvent.Receive)}
    @SubscribeEvent(priority = EventPriority.HIGH) 
    public void onReceivePacket(PacketEvent.Receive event) {
        if (event.getPacket() instanceof ClientboundPingPacket) {
            lastPingTicks = 5;
        }

        if (event.getPacket() instanceof ClientboundOpenScreenPacket packet) {
            if (packet.getContainerId() < 1 || packet.getContainerId() > 100) return;
            if (Minecraft.getInstance().player == null) return;

            TerminalState predictionState = new TerminalState(null, 0);
            if (this.terminal != null && this.terminal.isSolved()) {
                predictionState = this.terminal.getNextState();
            }

            this.terminalContainer = packet.getType().create(packet.getContainerId(), Minecraft.getInstance().player.getInventory());

            this.terminal = Terminal.fromPacket(packet, terminalContainer);
            if (this.terminal == null) {
                this.terminalContainer = null;
                return;
            }

            // should run after?
            if (Minecraft.getInstance().screen instanceof AbstractContainerScreen<?> abstractContainerScreen && abstractContainerScreen.getMenu().containerId != 0) {
                // o7 Balding
                Minecraft.getInstance().setScreen(null);
            }

            if (announceMelody.getValue() && this.terminal instanceof Melody) {
                Melody.sendMelodyMessage(0);
            }

            this.predictedState = predictionState;
            this.clickedWindow = false;
            this.invWalkGroup.getValue().getTerminalRenderer().newWindow(terminalContainer);

            if (invWalkGroup.getValue().isEnabled()) {
                event.setCancelled(true);
                if (invWalkGroup.getValue().getInvwalkMaybeFix().getValue()) setContainerMenu(packet.getType(), mc, packet.getContainerId(), packet.getTitle());
            }
            return;
        }

        boolean cancel = !invWalkGroup.getValue().getInvwalkMaybeFix().getValue();

        if (isInTerm() && event.getPacket() instanceof ClientboundContainerSetSlotPacket packet) {
            if (packet.getContainerId() == 0 || packet.getContainerId() != this.terminalContainer.containerId) return;
            terminalContainer.setItem(packet.getSlot(), packet.getStateId(), packet.getItem());
            terminal.loadSlot(packet);

            if (invWalkGroup.getValue().isEnabled() && cancel) event.setCancelled(true);
            return;
        }


        if (isInTerm() && event.getPacket() instanceof ClientboundContainerClosePacket packet) {
            if (packet.getContainerId() != terminalContainer.containerId) {
                RSA.chat("Container ID mismatch on close!");
                this.close();
                return;
            }

            this.close();
            if (invWalkGroup.getValue().isEnabled()) event.setCancelled(true);
            return;
        }

        if (isInTerm() && event.getPacket() instanceof ClientboundSetCursorItemPacket packet) {
            if (invWalkGroup.getValue().isEnabled() && cancel) event.setCancelled(true);
            return;
        }

        if (isInTerm() && event.getPacket() instanceof ClientboundContainerSetContentPacket packet) {
            if (packet.containerId() != 0 && invWalkGroup.getValue().isEnabled() && cancel) event.setCancelled(true);
            return;
        }

        if (isInTerm() && event.getPacket() instanceof ClientboundHorseScreenOpenPacket) {
            reset();
            return;
        }

        if (isInTerm() && event.getPacket() instanceof ClientboundContainerSetDataPacket packet) {
            if (packet.getContainerId() != 0 && invWalkGroup.getValue().isEnabled() && cancel) event.setCancelled(true);
            return;
        }

        if (isInTerm() && event.getPacket() instanceof ClientboundMerchantOffersPacket) {
            reset();
            return;
        }
    }

    public static <T extends AbstractContainerMenu> void setContainerMenu(MenuType<T> menuType, Minecraft minecraft, int i, Component component) {
        MenuScreens.ScreenConstructor<T, ?> screenConstructor = MenuScreens.getConstructor(menuType);
        if (screenConstructor == null) {
            RSA.getLogger().warn("Failed to create screen for menu type: {}", BuiltInRegistries.MENU.getKey(menuType));
        } else {
            Screen screen = screenConstructor.create(menuType.create(i, minecraft.player.getInventory()), minecraft.player.getInventory(), component);
            mc.player.containerMenu = ((MenuAccess) screen).getMenu();
        }
    }

    private void close() {
        this.terminal = null;
        this.invWalkGroup.getValue().getTerminalRenderer().close();
        this.terminalContainer = null;
        this.predictedState = null;
        this.firstClick = true;
        this.lastClickTime = System.currentTimeMillis();
        this.clickedSlotsTracker.clear();
    }

    @SubscribeEvent
    public void onSendPacket(PacketEvent.Send event) {
        if (isInTerm() && event.getPacket() instanceof ServerboundContainerClosePacket packet) {
            this.close();
            // If we close while we are receiving the next window id it will believe that the new window is a new term
            // even though it's now invalid, this doesn't seem to ban though?
            // Im scared to change it because it might have side effects
            // If it doesn't ban im fine with this

            // We could just cancel this packet and send one once we receive the next term, that would fix it
            return;
        }
    }

    public static boolean isInTerminal() {
        return RSM.getModule(AutoTerms.class).isInTerm();
    }

    public boolean isInTerm() {
        return this.terminal != null && terminalContainer != null;
    }
}