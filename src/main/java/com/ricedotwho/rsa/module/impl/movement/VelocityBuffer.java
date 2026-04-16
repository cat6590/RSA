package com.ricedotwho.rsa.module.impl.movement;

import com.ricedotwho.rsa.event.impl.VelocityBufferedEvent;
import com.ricedotwho.rsm.RSM;
import com.ricedotwho.rsm.data.Keybind;
import com.ricedotwho.rsm.event.api.SubscribeEvent;
import com.ricedotwho.rsm.event.impl.render.Render2DEvent;
import com.ricedotwho.rsm.event.impl.world.WorldEvent;
import com.ricedotwho.rsm.module.Module;
import com.ricedotwho.rsm.module.api.Category;
import com.ricedotwho.rsm.module.api.ModuleInfo;
import com.ricedotwho.rsm.ui.clickgui.settings.impl.DragSetting;
import com.ricedotwho.rsm.ui.clickgui.settings.impl.KeybindSetting;
import lombok.Getter;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.common.ClientboundPingPacket;
import net.minecraft.network.protocol.game.ClientboundBundlePacket;
import net.minecraft.network.protocol.game.ClientboundPlayerPositionPacket;
import net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket;
import net.minecraft.sounds.SoundEvents;
import org.joml.Vector2d;
import org.lwjgl.glfw.GLFW;

import java.util.Set;
import java.util.concurrent.ConcurrentLinkedQueue;


@ModuleInfo(aliases = "VelocityBuffer", id = "VelocityBuffer", category = Category.MOVEMENT, hasKeybind = true)
public class VelocityBuffer extends Module {
    private static VelocityBuffer INSTANCE;

    private final KeybindSetting popKey = new KeybindSetting("Queue Pop Key", new Keybind(GLFW.GLFW_KEY_UNKNOWN, false, this::popQueue));
    private final DragSetting gui = new DragSetting("Velocity Buffer Hud", new Vector2d(100, 100), new Vector2d(144, 80));
    @Getter
    private static int bufferedCount = 0;


    private static final Set<Class<? extends Packet<?>>> PACKET_SET = Set.of(
            ClientboundPingPacket.class,
            ClientboundBundlePacket.class // maybe just check eaach clientBoundPingPacket within the bundle?
    );

    private final ConcurrentLinkedQueue<Packet<?>> queue = new ConcurrentLinkedQueue<>();


    public VelocityBuffer() {
        this.registerProperty(
                popKey,
                gui
        );
    }

    @SubscribeEvent
    public void onRenderGui(Render2DEvent event) {
        if (queue.isEmpty()) return;
        gui.renderScaled(event.getGfx(), () -> event.getGfx().drawCenteredString(Minecraft.getInstance().font, "Buffered Packets : " + bufferedCount, 0, 0, 0xFFFFFFFF), 10, 10);
    }


    @SubscribeEvent
    public void onWorldLoad(WorldEvent.Load event) {
        synchronized (queue) {
            queue.clear();
            bufferedCount = 0;
            if (this.isEnabled()) this.setEnabled(false);
        }
    }

    public static boolean onReceivePacketPre(Packet<?> packet) {
        if (INSTANCE == null) INSTANCE = RSM.getModule(VelocityBuffer.class);
        // if the instance is still null after this, the module should probably just be disabled as well
        return INSTANCE != null && INSTANCE.onReceivePacket(packet);
    }

    private boolean onReceivePacket(Packet<?> packet) {
        synchronized (queue) { // Needed so that it locks if we are currently flushing
            if (Minecraft.getInstance().player == null || !this.isEnabled()) return false;
            if (packet instanceof ClientboundPlayerPositionPacket) {
                this.onKeyToggle();
                return false;
            }

            if (isMotionPacket(packet, Minecraft.getInstance().player)) {
                queue.add(packet);
                bufferedCount++;
                new VelocityBufferedEvent(packet).post();
                Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.NOTE_BLOCK_PLING.value(), 0.5f, 0.5f));
                return true;
            }
            if (packet instanceof ClientboundBundlePacket bundlePacket) {
                bundlePacket.subPackets().forEach(p -> System.out.println(p.getClass()));
            }
            if (!PACKET_SET.contains(packet.getClass())) return false;

            synchronized (queue) {
                if (queue.isEmpty()) return false;
                queue.add(packet);
            }
            return true;
        }
    }

    @Override
    public void onEnable() {
        super.onEnable();
        this.flush();
    }

    @Override
    public void onDisable() {
        flush();
        super.onDisable();
    }

    public void popQueue() {
        if (Minecraft.getInstance().player == null) return;
        synchronized (queue) {
            if (queue.isEmpty()) return;
            while (!queue.isEmpty()) {
                Packet<?> packet = queue.poll();
                this.receivePacket(packet);

                if (isMotionPacket(packet, Minecraft.getInstance().player)) {
                    bufferedCount--;
                    if (queue.stream().anyMatch(p -> isMotionPacket(p, Minecraft.getInstance().player))) break;
                    flush();

                    if (this.isEnabled())
                        this.onKeyToggle();
                    break;
                }
            }
        }
        Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.NOTE_BLOCK_PLING.value(), 2f, 2f));
    }


    private void receivePacket(Packet<?> packet) {
        if (Minecraft.getInstance().getConnection() == null) return;
        ((Packet<ClientPacketListener>) packet).handle(Minecraft.getInstance().getConnection());
    }

    private boolean isMotionPacket(Packet<?> packet, LocalPlayer player) {
        return packet instanceof ClientboundSetEntityMotionPacket motionPacket && motionPacket.getId() == player.getId();
    }

    private void flush() {
        synchronized (queue) {
            if (!queue.isEmpty())
                queue.forEach(this::receivePacket);
            this.queue.clear();
        }
        bufferedCount = 0;
    }

}
