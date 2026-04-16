package com.ricedotwho.rsa.module.impl.dungeon;

import com.mojang.blaze3d.platform.InputConstants;
import com.ricedotwho.rsa.RSA;
import com.ricedotwho.rsa.component.impl.managers.PacketOrderManager;
import com.ricedotwho.rsa.component.impl.managers.SwapManager;
import com.ricedotwho.rsa.utils.GuiUtils;
import com.ricedotwho.rsm.RSM;
import com.ricedotwho.rsm.component.impl.Terminals;
import com.ricedotwho.rsm.component.impl.location.Floor;
import com.ricedotwho.rsm.component.impl.location.Island;
import com.ricedotwho.rsm.component.impl.location.Location;
import com.ricedotwho.rsm.component.impl.map.handler.Dungeon;
import com.ricedotwho.rsm.component.impl.task.TaskComponent;
import com.ricedotwho.rsm.data.DungeonClass;
import com.ricedotwho.rsm.data.DungeonPlayer;
import com.ricedotwho.rsm.data.Keybind;
import com.ricedotwho.rsm.data.Phase7;
import com.ricedotwho.rsm.event.api.SubscribeEvent;
import com.ricedotwho.rsm.event.impl.client.PacketEvent;
import com.ricedotwho.rsm.event.impl.game.ChatEvent;
import com.ricedotwho.rsm.event.impl.game.TerminalEvent;
import com.ricedotwho.rsm.event.impl.world.WorldEvent;
import com.ricedotwho.rsm.module.Module;
import com.ricedotwho.rsm.module.api.Category;
import com.ricedotwho.rsm.module.api.ModuleInfo;
import com.ricedotwho.rsm.ui.clickgui.settings.impl.*;
import com.ricedotwho.rsm.utils.DungeonUtils;
import com.ricedotwho.rsm.utils.ItemUtils;
import com.ricedotwho.rsm.utils.Utils;
import lombok.Getter;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.protocol.game.ClientboundContainerSetSlotPacket;
import net.minecraft.network.protocol.game.ClientboundOpenScreenPacket;
import net.minecraft.network.protocol.game.ServerboundContainerClosePacket;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.Arrays;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Getter
@ModuleInfo(aliases = "Fast Leap", id = "FastLeap", category = Category.DUNGEONS)
public class FastLeap extends Module {

    private final KeybindSetting key = new KeybindSetting("Key", new Keybind(InputConstants.MOUSE_BUTTON_LEFT, false, true, true, null) {
        @Override
        public boolean run() {
            return doAutoLeap();
        }
    });

    private final NumberSetting cooldown = new NumberSetting("Cooldown", 0, 5000, 2000, 50);
    private final BooleanSetting chatCommand = new BooleanSetting("!leap", false);

    private final BooleanSetting flMessage = new BooleanSetting("Chat Message", false);
    private final BooleanSetting flP3 = new BooleanSetting("P3 Only", true);
    private final ModeSetting flS1 = new ModeSetting("S1", "Archer", Arrays.asList("Archer", "Mage", "Berserk", "Healer", "Tank", "Custom"));
    private final StringSetting flS1Custom = new StringSetting("S1 Custom", "", true, false, () -> flS1.is("Custom"));
    private final ModeSetting flS2 = new ModeSetting("S2", "Healer", Arrays.asList("Archer", "Mage", "Berserk", "Healer", "Tank", "Custom"));
    private final StringSetting flS2Custom = new StringSetting("S2 Custom", "", true, false, () -> flS2.is("Custom"));
    private final ModeSetting flS3 = new ModeSetting("S3", "Mage", Arrays.asList("Archer", "Mage", "Berserk", "Healer", "Tank", "Custom"));
    private final StringSetting flS3Custom = new StringSetting("S3 Custom", "", true, false, () -> flS3.is("Custom"));
    private final ModeSetting flS4 = new ModeSetting("S4", "Mage", Arrays.asList("Archer", "Mage", "Berserk", "Healer", "Tank", "Custom"));
    private final StringSetting flS4Custom = new StringSetting("S4 Custom", "", true, false, () -> flS4.is("Custom"));

    private final ModeSetting flP1 = new ModeSetting("P1", "Berserk", Arrays.asList("Archer", "Mage", "Berserk", "Healer", "Tank", "Custom"));
    private final StringSetting flP1Custom = new StringSetting("P1 Custom", "", true, false, () -> flP1.is("Custom"));

    private final ModeSetting flP2 = new ModeSetting("P2", "Auto", Arrays.asList("Archer", "Mage", "Berserk", "Healer", "Tank", "Custom", "Auto"));
    private final StringSetting flP2Custom = new StringSetting("P2 Custom", "", true, false, () -> flP2.is("Custom"));

    private final ModeSetting flP4 = new ModeSetting("P4", "Berserk", Arrays.asList("Archer", "Mage", "Berserk", "Healer", "Tank", "Custom"));
    private final StringSetting flP4Custom = new StringSetting("P4 Custom", "", true, false, () -> flP4.is("Custom"));

    private final ModeSetting flP5Orange = new ModeSetting("P5 Orange", "Berserk", Arrays.asList("Archer", "Mage", "Berserk", "Healer", "Tank", "Custom"));
    private final StringSetting flP5OrangeCustom = new StringSetting("Orange Custom", "", true, false, () -> flP5Orange.is("Custom"));
    private final ModeSetting flP5Red = new ModeSetting("P5 Red", "Archer", Arrays.asList("Archer", "Mage", "Berserk", "Healer", "Tank", "Custom"));
    private final StringSetting flP5RedCustom = new StringSetting("Red Custom", "", true, false, () -> flP5Red.is("Custom"));

    private static final Pattern leapPattern = Pattern.compile("Party > (?:\\[(.*?)] )?(.+?): !leap");

    private static String toLeap = null;
    private static boolean openingGui = false;
    private static long lastUsed = 0;
    private boolean windowOpen = false;

    private static boolean queuedLeap = false;

    private AbstractContainerMenu container;

    public FastLeap() {
        this.registerProperty(
                key,
                cooldown,
                chatCommand,
                flMessage,
                flP3,
                flS1, flS1Custom,
                flS2, flS2Custom,
                flS3, flS3Custom,
                flS4, flS4Custom,
                flP1, flP1Custom,
                flP2, flP2Custom,
                flP4, flP4Custom,
                flP5Orange, flP5OrangeCustom,
                flP5Red, flP5RedCustom
        );
    }

    @Override
    public void reset() {
        toLeap = null;
        openingGui = false;
        windowOpen = false;
        container = null;
        queuedLeap = false;
    }

    @SubscribeEvent
    public void onChat(ChatEvent.Chat event) {
        if (!Location.getArea().is(Island.Dungeon) || !(Dungeon.isStarted() || Dungeon.isInBoss()) || !chatCommand.getValue()) return;
        String value = ChatFormatting.stripFormatting(event.getMessage().getString());
        Matcher matcher = leapPattern.matcher(value);
        if (matcher.find() && SwapManager.swapItem("SPIRIT_LEAP", "INFINITE_SPIRIT_LEAP")) {
            doLeap(matcher.group(2));
        }
    }

    @SubscribeEvent
    public void onLoad(WorldEvent.Load event) {
        reset();
    }

    public static boolean doAutoLeap() {
        FastLeap module = RSM.getModule(FastLeap.class);
        if (
                module.flP3.getValue() && !DungeonUtils.isPhase(Phase7.P3)
                || !Location.getArea().is(Island.Dungeon)
                || mc.player == null || mc.level == null
                || !Utils.equalsOneOf(ItemUtils.getID(mc.player.getInventory().getSelectedItem()), "SPIRIT_LEAP", "INFINITE_SPIRIT_LEAP")
                || (System.currentTimeMillis() - lastUsed) < module.cooldown.getValue().longValue()
                || module.windowOpen
                || openingGui
                || !Terminals.isInTerminal() && mc.screen != null
                || module.container != null
                || !Dungeon.isInBoss()
        ) return false;

        // todo: queue leap
        if (Terminals.isInTerminal()) {
            queuedLeap = true;
            module.modMessage("Queued leap");
            return true;
        }

        String leap = getLeap();
        if (leap == null || "NONE".equals(leap) || mc.player.getName().getString().equalsIgnoreCase(leap)) {
            module.modMessage(ChatFormatting.RED + "Couldn't find who to leap to! (" + leap + ")");
            return false;
        }

        doLeap(leap);
        return true;
    }

    public static void doLeap(String name) {
        toLeap = name;
        openingGui = true;
        lastUsed = System.currentTimeMillis();
        PacketOrderManager.register(PacketOrderManager.STATE.ITEM_USE, () -> SwapManager.sendAirC08(mc.player.yRotO, mc.player.xRotO, false));
    }

    public static void doLeap(DungeonPlayer player){
        doLeap(player.getName());
    }

    public static boolean doLeapFromOpenMenu(DungeonPlayer player){
        return doLeapFromOpenMenu(player.getName());
    }

    public static boolean doLeapFromOpenMenu(String leap) {
        if (mc.player == null || !(mc.player.containerMenu instanceof ChestMenu menu) || mc.screen == null || !mc.screen.getTitle().getString().equals("Spirit Leap")) return false;

        for (Slot slot : menu.slots) {
            ItemStack item = slot.getItem();
            if (!item.getItem().equals(Items.PLAYER_HEAD)) continue;

            String name = ChatFormatting.stripFormatting(item.getHoverName().getString());
            if (!name.equals(leap)) continue;
            GuiUtils.sendWindowClick(slot.index, mc.player, menu);

            FastLeap fl = RSM.getModule(FastLeap.class);
            if (fl == null) return true;
            if (fl.getFlMessage().getValue()) {
                mc.getConnection().sendCommand("pc Leaping to " + toLeap);
            }
            else {
                fl.modMessage("Leaping to " + toLeap);
            }
            return true;
        }
        return false;
    }

    @SubscribeEvent
    public void onTerminalClose(TerminalEvent.Close event) {
        if (event.isServer() && queuedLeap) {
            TaskComponent.onTick(0, FastLeap::doAutoLeap);
        }
        queuedLeap = false;
    }

    @SubscribeEvent
    public void onOpenWindow(PacketEvent.Receive event) {
        if (event.getPacket() instanceof ClientboundOpenScreenPacket packet) {
            if (packet.getContainerId() < 1
                    || packet.getContainerId() > 100
                    || mc.player == null
                    || !Location.getArea().is(Island.Dungeon)
            ) return;

            if (openingGui && "Spirit Leap".equals(packet.getTitle().getString())) {
                openingGui = false;
                windowOpen = true;
                this.container = packet.getType().create(packet.getContainerId(), mc.player.getInventory());
                event.setCancelled(true);
                if (Minecraft.getInstance().screen instanceof AbstractContainerScreen<?> abstractContainerScreen && abstractContainerScreen.getMenu().containerId != 0) {
                    // o7 Balding
                    Minecraft.getInstance().setScreen(null);
                }
            } else {
                reset();
            }
        }
        else if (event.getPacket() instanceof ClientboundContainerSetSlotPacket packet) {
            if (
                    packet.getContainerId() < 1
                    || packet.getContainerId() > 100
                    || mc.player == null
                    || !windowOpen
                    || openingGui
                    || this.container.containerId != packet.getContainerId()
                    || packet.getSlot() < 11
                    || toLeap == null
            ) return;

            container.setItem(packet.getSlot(), packet.getStateId(), packet.getItem());

            if (packet.getSlot() > 16) {
                modMessage(ChatFormatting.RED + "Failed to find player!");
                close();
                return;
            }
            ItemStack item = packet.getItem();
            if (!item.getItem().equals(Items.PLAYER_HEAD)) return;

            String name = ChatFormatting.stripFormatting(item.getHoverName().getString());
            if (!name.equals(toLeap)) return;
            GuiUtils.sendWindowClick(packet.getSlot(), mc.player, this.container);

            if (getFlMessage().getValue()) {
                mc.getConnection().sendCommand("pc Leaping to " + toLeap);
            }
            else {
                modMessage("Leaping to " + toLeap);
            }
            reset();
        }
    }

    private void close() {
        if (container == null || mc.getConnection() == null) return;
        mc.getConnection().send(new ServerboundContainerClosePacket(this.container.containerId));
        reset();
    }

    private static String getLeap() {
        DungeonPlayer player = getClassPlayer();
        if (player == null) return null;
        return player.getName();
    }

    private static Object getStageClass() {
        if (!Utils.equalsOneOf(Location.getFloor(), Floor.F7, Floor.M7) || !Dungeon.isInBoss()) return -1;

        FastLeap module = RSM.getModule(FastLeap.class);

        DungeonPlayer me = Dungeon.getMyPlayer();

        switch (DungeonUtils.getF7Phase()) {
            case P1:
                return module.getFlP1().is("Custom") ? module.getFlP1Custom().getValue() : module.getFlP1().getIndex();
            case P2:
                if (module.getFlP2().is("Auto")) {
                    if (me == null) return -1;
                    DungeonPlayer healer = Dungeon.getClazz(DungeonClass.HEALER);
                    DungeonPlayer mage = Dungeon.getClazz(DungeonClass.MAGE);
                    DungeonPlayer bers = Dungeon.getClazz(DungeonClass.BERSERKER);
                    if(me.getDClass().equals(DungeonClass.TANK) && mage != null && Location.getFloor().equals(Floor.F7)) return mage;
                    if(healer != null && !me.equals(healer)) return healer;
                    if(bers != null && me.equals(healer)) return bers;
                    return -1;
                }
                return module.getFlP2().is("Custom") ? module.getFlP2Custom().getValue() : module.getFlP2().getIndex();
            case P3:
                return switch (DungeonUtils.getP3Section()) {
                    case S1 -> module.getFlS1().is("Custom") ? module.getFlS1Custom().getValue() : module.getFlS1().getIndex();
                    case S2 -> module.getFlS2().is("Custom") ? module.getFlS2Custom().getValue() : module.getFlS2().getIndex();
                    case S3 -> module.getFlS3().is("Custom") ? module.getFlS3Custom().getValue() : module.getFlS3().getIndex();
                    case S4 -> module.getFlS4().is("Custom") ? module.getFlS4Custom().getValue() : module.getFlS4().getIndex();
                    default -> -1;
                };
            case P4:
                return module.getFlS4().is("Custom") ? module.getFlS4Custom().getValue() : module.getFlS4().getIndex();
            case P5:
                if (me == null) return -1;
                if (DungeonClass.HEALER.equals(me.getDClass())) return module.getFlP5Orange().is("Custom") ? module.getFlP5OrangeCustom().getValue() : module.getFlP5Orange().getIndex();
                if (DungeonClass.MAGE.equals(me.getDClass())) return module.getFlP5Orange().is("Custom") ? module.getFlP5OrangeCustom().getValue() : module.getFlP5Orange().getIndex();
                if (DungeonClass.TANK.equals(me.getDClass())) return module.getFlP5Red().is("Custom") ? module.getFlP5RedCustom().getValue() : module.getFlP5Red().getIndex();
                return -1;
            default:
                return -1;
        }
    }

    private static DungeonPlayer getClassPlayer() {
        Object yuh = getStageClass();
        if (yuh instanceof DungeonPlayer dp) {
            return dp;
        }
        if (yuh instanceof String s) {
            return Dungeon.getPlayer(s);
        }
        return Dungeon.getClazz((int) yuh);
    }

    private void modMessage(String message) {
        RSA.chat(ChatFormatting.BLUE + "Fast Leap » " + ChatFormatting.RESET + message);
    }
}
