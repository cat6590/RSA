package com.ricedotwho.rsa.module.impl.dungeon.boss.p3.autop3;

import com.google.common.reflect.TypeToken;
import com.google.gson.GsonBuilder;
import com.ricedotwho.rsa.module.impl.dungeon.boss.p3.autop3.args.Argument;
import com.ricedotwho.rsa.module.impl.dungeon.boss.p3.autop3.args.type.LeapArg;
import com.ricedotwho.rsa.module.impl.dungeon.boss.p3.autop3.args.type.TermArg;
import com.ricedotwho.rsa.module.impl.dungeon.boss.p3.autop3.args.type.TermCloseArg;
import com.ricedotwho.rsa.module.impl.dungeon.boss.p3.autop3.args.type.TriggerArg;
import com.ricedotwho.rsa.module.impl.dungeon.boss.p3.autop3.recorder.MovementRecorder;
import com.ricedotwho.rsa.module.impl.dungeon.boss.p3.autop3.rings.Ring;
import com.ricedotwho.rsm.RSM;
import com.ricedotwho.rsm.component.impl.camera.ClientRotationHandler;
import com.ricedotwho.rsm.component.impl.camera.ClientRotationProvider;
import com.ricedotwho.rsm.component.impl.location.Island;
import com.ricedotwho.rsm.component.impl.location.Location;
import com.ricedotwho.rsm.component.impl.map.handler.Dungeon;
import com.ricedotwho.rsm.data.Colour;
import com.ricedotwho.rsm.data.Keybind;
import com.ricedotwho.rsm.data.MutableInput;
import com.ricedotwho.rsm.event.api.SubscribeEvent;
import com.ricedotwho.rsm.event.impl.client.InputPollEvent;
import com.ricedotwho.rsm.event.impl.game.ClientTickEvent;
import com.ricedotwho.rsm.event.impl.game.TerminalEvent;
import com.ricedotwho.rsm.event.impl.render.Render3DEvent;
import com.ricedotwho.rsm.event.impl.world.WorldEvent;
import com.ricedotwho.rsm.module.Module;
import com.ricedotwho.rsm.module.api.Category;
import com.ricedotwho.rsm.module.api.ModuleInfo;
import com.ricedotwho.rsm.ui.clickgui.settings.group.DefaultGroupSetting;
import com.ricedotwho.rsm.ui.clickgui.settings.group.GroupSetting;
import com.ricedotwho.rsm.ui.clickgui.settings.impl.*;
import com.ricedotwho.rsm.utils.ChatUtils;
import com.ricedotwho.rsm.utils.Utils;
import lombok.Getter;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.entity.player.Input;
import net.minecraft.world.phys.Vec3;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.IntStream;

@Getter
@ModuleInfo(aliases = "Auto P3", id = "AutoP3", category = Category.DUNGEONS, hasKeybind = true)
public class AutoP3 extends Module implements ClientRotationProvider {

    private static final MutableComponent PREFIX = Component.empty()
            .append(Component.literal("[").withStyle(ChatFormatting.GOLD))
            .append(Component.literal("byebyebalding").withStyle(ChatFormatting.DARK_GRAY))
            .append(Component.literal("] ").withStyle(ChatFormatting.GOLD))
            .append(Component.empty().withStyle(ChatFormatting.WHITE));

    private final BooleanSetting forceSkyblock = new BooleanSetting("Force Skyblock", false);
    private final BooleanSetting yap = new BooleanSetting("Feedback", false);
    private final KeybindSetting triggerBind = new KeybindSetting("Trigger", new Keybind(GLFW.GLFW_MOUSE_BUTTON_1, true, this::trigger));
    @Getter private static final NumberSetting edgeDist = new NumberSetting("Edge Dist", 0, 0.1, 0.001, 0.001);
    private final BooleanSetting depth = new BooleanSetting("Depth", false);
    private final BooleanSetting strafe = new BooleanSetting("45", true);
    private final GroupSetting<MovementRecorder> movementCat = new GroupSetting<>("Movement", new MovementRecorder(this));

    private final DefaultGroupSetting colours = new DefaultGroupSetting("Colours", this);
    @Getter private static final ColourSetting align = new ColourSetting("Align", Colour.GREEN.copy());
    @Getter private static final ColourSetting blink = new ColourSetting("Blink", Colour.PINK.copy());
    @Getter private static final ColourSetting bonzo = new ColourSetting("Bonzo", Colour.MAGENTA.copy());
    @Getter private static final ColourSetting boom = new ColourSetting("Boom", Colour.RED.darker());
    @Getter private static final ColourSetting chat = new ColourSetting("Chat", Colour.YELLOW.copy());
    @Getter private static final ColourSetting command = new ColourSetting("Command", Colour.DARK_GRAY.copy());
    @Getter private static final ColourSetting edge = new ColourSetting("Edge", Colour.BLACK.copy());
    @Getter private static final ColourSetting fastAlign = new ColourSetting("Fast Align", Colour.GREEN.darker());
    @Getter private static final ColourSetting fastBonzo = new ColourSetting("Fast Bonzo", Colour.MAGENTA.darker());
    @Getter private static final ColourSetting jump = new ColourSetting("Jump", Colour.ORANGE.copy());
    @Getter private static final ColourSetting leap = new ColourSetting("Leap", Colour.BLUE.copy());
    @Getter private static final ColourSetting look = new ColourSetting("Look", Colour.GREEN.brighter());
    @Getter private static final ColourSetting movement = new ColourSetting("Movement", Colour.WHITE.copy());
    @Getter private static final ColourSetting pet = new ColourSetting("Pet", Colour.YELLOW.darker());
    @Getter private static final ColourSetting stop = new ColourSetting("Stop", Colour.RED.copy());
    @Getter private static final ColourSetting stopWatch = new ColourSetting("Stop Watch", Colour.DARK_GRAY.copy());
    @Getter private static final ColourSetting use = new ColourSetting("Use", Colour.GRAY.copy());
    @Getter private static final ColourSetting walk = new ColourSetting("Walk", Colour.CYAN.copy());

    private final SaveSetting<List<Ring>> data = new SaveSetting<>("Rings", "dungeon/ap3", "rings.json", ArrayList::new,
            new TypeToken<List<Ring>>() {}.getType(),
            new GsonBuilder()
                    .registerTypeHierarchyAdapter(Ring.class, new RingAdapter())
                    .setPrettyPrinting().create(),
            true, this::reload, null);

    private final List<Ring> rings;
    private boolean desync = false;
    private boolean lastDesync = false;
    @Getter
    private final List<Ring> activeRings;
    private final List<Ring> temp = new ArrayList<>();
    private final List<Ring> redoList = new ArrayList<>();

    public AutoP3() {
        this.registerProperty(
                yap,
                triggerBind,
                edgeDist,
                depth,
                strafe,
                forceSkyblock,
                movementCat,
                data,
                colours
        );

        colours.add(align, blink, bonzo, boom, chat, command, edge, fastAlign, fastBonzo, jump, leap, look, movement, pet, stop, stopWatch, use, walk);

        this.rings = new ArrayList<>();
        this.activeRings = new ArrayList<>(5);
    }

    @SubscribeEvent
    public void onTickEnd(ClientTickEvent.End event) {
        if (!desync && lastDesync && Minecraft.getInstance().player != null) {
            Minecraft.getInstance().player.setYRot(ClientRotationHandler.getClientYaw());
            Minecraft.getInstance().player.setXRot(ClientRotationHandler.getClientPitch());
        }
        lastDesync = desync;
    }
//
//    int last = -1;
//    @SubscribeEvent
//    public void onSendPacket(PacketEvent.Send event) {
//        if (!(event.getPacket() instanceof ServerboundPongPacket packet)) return;
//        System.out.println(packet.getId());
//        if (packet.getId() != last - 1) {
//            ChatUtils.chat("Mismatch at id : " + packet.getId());
//        }
//        last = packet.getId();
//    }

    @SubscribeEvent
    public void onWorldLoad(WorldEvent.Load event) {
        this.activeRings.clear();
        reload();
    }

    public static void load(String config) {
        AutoP3 ap3 = RSM.getModule(AutoP3.class);
        ap3.getData().setFileName(config);
        ap3.getData().updateFile();
        ap3.getData().load();
        ap3.reload();
    }

    @SubscribeEvent
    public void onPollInputs(InputPollEvent event) {
        if (!dungeonCheck()) return;
        if (activeRings.isEmpty()) return;

        MutableInput mutableInput = event.getInput();
        Input input = event.getClientInput();

        for (int i = 0 ; i < activeRings.size(); i++) {
            Ring r = activeRings.get(i);
            boolean bl2 = r.isActive() && r.tick(mutableInput, input, this);
            if (!bl2) continue;
            r.setInactive();
            activeRings.remove(i--);
        }
    }

//    @SubscribeEvent
//    public void onSendPacket(PacketEvent.Send event) {
//        if (!dungeonCheck()) return;
//        if (activeRings.isEmpty()) return;
//
//        Packet<?> packet = event.getPacket();
//        if (!(packet instanceof ServerboundMovePlayerPacket movePlayerPacket)) return;
//        for (int i = 0 ; i < activeRings.size(); i++) {
//            Ring r = activeRings.get(i);
//            if (!(r instanceof BlinkRing blinkRing)) continue;
//            blinkRing.onSendPacket(movePlayerPacket);
//        }
//    }

    private void reload() {
        this.rings.clear();
        this.rings.addAll(data.getValue());
    }

    protected void onDesyncEnable() {
        ClientRotationHandler.registerProvider(this);

        if (Minecraft.getInstance().player == null) return;
        ClientRotationHandler.setYaw(Minecraft.getInstance().player.getYRot());
    }

    @SubscribeEvent
    public void onTick(ClientTickEvent.Start event) {
        if (!dungeonCheck() || mc.player == null) return;
        desync = false;

        Vec3 playerPos = mc.player.position();
        Vec3 oldPos = mc.player.oldPosition();

        List<Ring> sorted;
        synchronized (rings) {
            sorted = rings.stream().filter(r -> r.updateState(playerPos, oldPos) && (activeRings.isEmpty() || activeRings.stream().allMatch(active -> r.getPriority() >= active.getPriority()))).sorted(Comparator.comparingInt(Ring::getPriority).reversed()).toList();
        }

        if (sorted.isEmpty()) return;

        boolean feedback = yap.getValue();
        activeRings.removeIf(r -> !r.isActive());
        //activeRings.clear();
        temp.clear();

        boolean stop = false;

        for (Ring ring : sorted) {
            temp.add(ring);
            if (ring.checkArg()) continue;
            if (ring.isStop()) stop = true;
            ring.setTriggered();
            ring.setActive();
            if (feedback) ring.feedback();
            if (!ring.execute()) break;
        }

        if (stop) activeRings.removeIf(r -> {
            if (r.shouldStop()) {
                r.setInactive();
                return true;
            }
            return false;
        });
        activeRings.addAll(temp.stream().filter(r -> !activeRings.contains(r)).toList());
    }

//    private int last = -1;
//    @SubscribeEvent
//    public void onSendPacket(PacketEvent.Send event) {
//        if (!(event.getPacket() instanceof ServerboundPongPacket pongPacket)) return;
//        System.out.println(pongPacket.getId());
//        if (last - 1 != pongPacket.getId()) {
//            ChatUtils.chat("Id Mismatch : " + pongPacket.getId());
//        }
//        last = pongPacket.getId();
//    }

    @SubscribeEvent
    public void onRender(Render3DEvent.Extract event) {
        if (!dungeonCheck()) return;
        synchronized (rings) {
            this.rings.forEach(r -> r.render(this.depth.getValue()));
        }
    }

    @SubscribeEvent
    public void onTermOpen(TerminalEvent.Open event) {
        consumeArg(TermArg.class, event);
    }

    @SubscribeEvent
    public void onTermOpen(TerminalEvent.Close event) {
        if (event.isServer()) {
            consumeArg(TermCloseArg.class, true);
        }
    }

    private void trigger() {
        consumeArg(TriggerArg.class, true);
        consumeArg(TermCloseArg.class, true);
        consumeArg(TermArg.class, null);
        consumeArg(LeapArg.class, true);
    }

    private <T> void consumeArg(Class<? extends Argument<T>> clazz, T value) {
        if (mc.player == null) return;
        Vec3 playerPos = mc.player.position();
        Vec3 oldPos = mc.player.oldPosition();
        activeRings.stream().filter(s -> s.isInNode(playerPos, oldPos)).toList().forEach(r -> {
            r.consumeArg(clazz, value);
        });
    }

    private boolean dungeonCheck() {
        return this.forceSkyblock.getValue() || (mc.player != null && Location.getArea().is(Island.Dungeon) && Dungeon.isInBoss());
    }

    public void addRing(Ring ring) {
        ring.setTriggered(); // So it doesn't activate instantly
        synchronized (rings) {
            this.rings.add(ring);
        }
        save();
        modMessage("Added %s %s%s", Utils.capitalise(ring.getType().getName()), ChatFormatting.GRAY, ring.getArgManager().getList(ring.getSubManager().getList()));
    }

    public boolean insertRing(Ring ring, int index) {
        if (index < 0 || index > rings.size()) return false;
        ring.setTriggered(); // So it doesn't activate instantly
        synchronized (rings) {
            this.rings.add(index, ring);
        }
        save();
        return true;
    }

    public boolean removeIndexed(int index) {
        synchronized (rings) {
            if (index < 0 || index >= rings.size()) return false;
            rings.remove(index);
        }

        save();
        return true;
    }

    public void removeNearest(Vec3 pos) {
        synchronized (rings) {
            int index = IntStream.range(0, rings.size())
                .boxed()
                .min(Comparator.comparingDouble(i -> rings.get(i).getDistanceSq(pos)))
                .orElse(-1);
            if (index < 0) return;
            Ring ring = rings.remove(index);
            save();
            modMessage("Removed %s", Utils.capitalise(ring.getType().getName()));
            data.setValue(List.copyOf(this.rings));
        }
    }

    public void undo() {
        if (rings.isEmpty()) {
            modMessage("No Rings!");
            return;
        }
        Ring ring = rings.removeLast();
        redoList.add(ring);
        save();
        modMessage("Undid %s", Utils.capitalise(ring.getType().getName()));
    }

    public void redo() {
        if (redoList.isEmpty()) {
            modMessage("No Rings!");
            return;
        }
        Ring ring = redoList.removeLast();
        rings.add(ring);
        save();
        modMessage("Redid %s", Utils.capitalise(ring.getType().getName()));
    }

    public void setDesync(boolean bl) {
        if (bl && !desync && !lastDesync) onDesyncEnable();
        this.desync = bl;
    }

    protected boolean getDesync() {
        return this.desync;
    }

    protected boolean getLastDesync() {
        return this.lastDesync;
    }

    @Override
    public boolean isClientRotationActive() {
        return this.isEnabled() && desync;
    }

    @Override
    public boolean allowClientKeyInputs() {
        return true;
    }

    public void save() {
        data.setValue(List.copyOf(this.rings));
        data.save();
    }

    public void load() {
        data.load();
    }

    public static void modMessage(Object message, Object ... objects) {
        ChatUtils.chatClean(PREFIX.copy().append(String.format(message.toString(), objects)));
    }
}
