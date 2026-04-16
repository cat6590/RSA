package com.ricedotwho.rsa.module.impl.dungeon.boss;

import com.ricedotwho.rsa.IMixin.IConnection;
import com.ricedotwho.rsa.module.impl.dungeon.autoroutes.FakeKeyboardInput;
import com.ricedotwho.rsa.module.impl.dungeon.boss.p3.autop3.recorder.MovementRecorder;
import com.ricedotwho.rsa.module.impl.dungeon.boss.p3.terminals.auto.AutoTerms;
import com.ricedotwho.rsa.module.impl.dungeon.boss.p3.terminals.auto.terminals.TerminalType;
import com.ricedotwho.rsa.packet.FakeClientPacketListener;
import com.ricedotwho.rsa.packet.FakeLocalPlayer;
import com.ricedotwho.rsm.RSM;
import com.ricedotwho.rsm.component.impl.camera.ClientRotationHandler;
import com.ricedotwho.rsm.component.impl.camera.ClientRotationProvider;
import com.ricedotwho.rsm.event.api.SubscribeEvent;
import com.ricedotwho.rsm.event.impl.client.PacketEvent;
import com.ricedotwho.rsm.event.impl.game.ClientTickEvent;
import com.ricedotwho.rsm.event.impl.render.Render2DEvent;
import com.ricedotwho.rsm.event.impl.world.WorldEvent;
import com.ricedotwho.rsm.mixins.accessor.LocalPlayerAccessor;
import com.ricedotwho.rsm.module.Module;
import com.ricedotwho.rsm.module.api.Category;
import com.ricedotwho.rsm.module.api.ModuleInfo;
import com.ricedotwho.rsm.ui.clickgui.settings.impl.BooleanSetting;
import com.ricedotwho.rsm.ui.clickgui.settings.impl.DragSetting;
import com.ricedotwho.rsm.ui.clickgui.settings.impl.NumberSetting;
import com.ricedotwho.rsm.utils.ChatUtils;
import lombok.Getter;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.ClientInput;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.common.ServerboundPongPacket;
import net.minecraft.network.protocol.game.*;
import org.joml.Vector2d;

import java.util.LinkedList;
import java.util.List;


@ModuleInfo(aliases = "Blink", id = "Blink", category = Category.MOVEMENT, hasKeybind = true)
public class Blink extends Module implements ClientRotationProvider {
    private static Blink INSTANCE;

    private final DragSetting gui = new DragSetting("Balding Blink Hud", new Vector2d(100, 100), new Vector2d(144, 80));
    private final NumberSetting maxBlinkPacket = new NumberSetting("Max Blink Packets", 1, 30, 16, 1);
    private final BooleanSetting flushGUIPongs = new BooleanSetting("Flush GUI Pongs", true);
    private final BooleanSetting flushInMelody = new BooleanSetting("Flush In Melody", true);
    private final NumberSetting smallMovementDelta = new NumberSetting("Velocity Epsilon", 0, 7, 4, 1);

    private boolean sentMove = false;
    private boolean flushedPongs = false;

    private final LinkedList<Packet<?>> queue = new LinkedList<>();
    @Getter
    private boolean flushing = false;
    private int packetCount = 0;


    public Blink() {
        this.registerProperty(
                maxBlinkPacket,
                flushGUIPongs,
                smallMovementDelta,
                flushInMelody,
                gui
        );
    }

    @SubscribeEvent
    public void onRenderGui(Render2DEvent event) {
        if (Minecraft.getInstance().screen != null) return;

        gui.renderScaled(event.getGfx(), () -> {
            event.getGfx().drawCenteredString(Minecraft.getInstance().font, ("Packets : " + packetCount), (int) gui.getPosition().x, (int) gui.getPosition().y, 0xFFFFFFFF);
            event.getGfx().drawCenteredString(Minecraft.getInstance().font, ("Pongs : " + queue.size()), (int) gui.getPosition().x, (int) gui.getPosition().y + Minecraft.getInstance().font.lineHeight, 0xFFFFFFFF);
        }, 5, 5);
    }

    @SubscribeEvent
    public void onWorldLoad(WorldEvent.Load event) {
        synchronized (queue) {
            this.packetCount = 0;
            this.queue.clear();
            if (this.isEnabled())
                this.setEnabled(false);
        }
    }


    public static boolean onSendPacket(Packet<?> packet) {
        if (INSTANCE == null) INSTANCE = RSM.getModule(Blink.class);
        // if the instance is still null after this, the module should probably just be disabled as well
        return INSTANCE != null && INSTANCE.onPreSendPacket(packet);
    }

    public void blinkMovement(List<MovementRecorder.PlayerInput> movements) {
        if (Minecraft.getInstance().level == null || Minecraft.getInstance().player == null || Minecraft.getInstance().getConnection() == null || packetCount < 1 || movements.isEmpty()) return;
        int tickCount = Math.min(movements.size(), packetCount);
        if (movements.size() > packetCount) {
            movements = movements.subList(0, packetCount);
        }


        LocalPlayer player = Minecraft.getInstance().player;

        ClientInput oldInputs = player.input;
        player.input = new FakeKeyboardInput(Minecraft.getInstance().options);

        boolean bl = flushing;
        flushing = true;
        for (int i = 0; i < tickCount; i++) {
            MovementRecorder.PlayerInput input = movements.get(i);
            player.input.keyPresses = input.input();

            player.setYRot(input.yaw);
            player.setXRot(input.pitch);
            player.tick();
            Minecraft.getInstance().getConnection().send(new ServerboundClientTickEndPacket());
            packetCount--;
        }
        flushing = bl;
        this.flushPongs();

        player.input = oldInputs;
    }

    @SubscribeEvent
    public void onTickStart(ClientTickEvent.Start event) {
        this.flushedPongs = false;
    }

    @SubscribeEvent
    public void onSendPacket(PacketEvent.Send event) {
        if (getPongCount() < 8 || !flushGUIPongs.getValue()) return;
        Packet<?> packet = event.getPacket();
        if (packet instanceof ServerboundContainerClickPacket || packet instanceof ServerboundInteractPacket || packet instanceof ServerboundChatCommandSignedPacket || packet instanceof ServerboundChatCommandPacket || packet instanceof ServerboundUseItemOnPacket || packet instanceof ServerboundUseItemPacket) {
            this.flushPongs();
        }
    }


    @SubscribeEvent
    public void onReceivePacket(PacketEvent.Receive event) {
        if (Minecraft.getInstance().player == null) return;

        if (event.getPacket() instanceof ClientboundSetEntityMotionPacket motionPacket) {
            if (motionPacket.getId() != Minecraft.getInstance().player.getId()) return;
            synchronized (queue) {
                this.flushPongs();
                this.flushedPongs = true;
            }
        }
    }

    @SubscribeEvent
    public void onLocalPlayerTick(ClientTickEvent.Player event) {
        if (packetCount >= maxBlinkPacket.getValue().intValue() || smallMovementDelta.getValue().intValue() == 0 || !MovementRecorder.isIdle()) return;

        LocalPlayer player = event.getPlayer();
        if (Minecraft.getInstance().level == null || Minecraft.getInstance().player == null || player.getId() != Minecraft.getInstance().player.getId()) return;

        FakeClientPacketListener packetListener = new FakeClientPacketListener(null);
        LocalPlayer copy = new FakeLocalPlayer(Minecraft.getInstance(), Minecraft.getInstance().level, packetListener, player.getStats(), player.getRecipeBook(), player.input.keyPresses, false);

        copy.setPos(player.position());
        copy.setYRot(player.getYRot());
        copy.setXRot(player.getXRot());
        copy.setDeltaMovement(player.getDeltaMovement());
        copy.xxa = player.xxa;
        copy.yya = player.yya;
        copy.zza = player.zza;

        copy.input = new ClientInput();
        copy.input.keyPresses = player.input.keyPresses;

        copy.fallDistance = player.fallDistance;
        copy.noPhysics = player.noPhysics;
        copy.setSprinting(player.isSprinting());
        copy.setBoundingBox(player.getBoundingBox());
        copy.setSpeed(player.getSpeed());
        copy.setOnGround(player.onGround());
        copy.setPose(player.getPose());
        copy.setClientLoaded(true);

        copy.tick();
        double delta = smallMovementDelta.getValue().doubleValue() / 100d;
        double distSq = copy.position().distanceToSqr(player.position());

        if (distSq < delta * delta) {
            event.setCancelled(true);
        }
    }


    private boolean onPreSendPacket(Packet<?> packet) {
//        if (packet instanceof ServerboundPongPacket && (!this.isEnabled() || flushing)) {
//            System.out.println(((ServerboundPongPacket) packet).getId());
//            int id  = ((ServerboundPongPacket) packet).getId();
//            if (id + 1 != last) {
//                ChatUtils.chat("mismatch!");
//            }
//            last = id;
//            return false;
//        }
        if (Minecraft.getInstance().player == null || !this.isEnabled() || flushing) return false;

        if (packet instanceof ServerboundAcceptTeleportationPacket) {
            this.flushPongs();
            this.flushedPongs = true;
            return false;
        }

        if (packet instanceof ServerboundPongPacket) {
            if (flushedPongs) return false;

            queue.add(packet);

            if (queue.size() > Math.min(14, packetCount - 2)) {
                Packet<?> ping = queue.removeFirst();
                actuallySendImmediately(ping);
            }

            if (getPongCount() >= 8 && flushInMelody.getValue()) {
                AutoTerms autoTerms = RSM.getModule(AutoTerms.class);
                if (autoTerms.isEnabled() && autoTerms.isInTerm() && autoTerms.getTerminal().getType() == TerminalType.MELODY)
                    this.flushPongs();
            }
            return true;
        }

        if (packetCount >= maxBlinkPacket.getValue().intValue()) return false;

        LocalPlayerAccessor accessor = (LocalPlayerAccessor) Minecraft.getInstance().player;

        if (packet instanceof ServerboundClientTickEndPacket) {
            if (!sentMove) {
                int reminder = accessor.getPositionReminder();
                if (reminder > 0) {
                    accessor.setPositionReminder(reminder - 1);
                }
                packetCount++;
                return true;
            }
            sentMove = false;
            return false;
        }

        if (packet instanceof ServerboundMovePlayerPacket || packet instanceof ServerboundPlayerInputPacket) {
            sentMove = true;
            return false;
        }
        return false;
    }

    public int getChargedCount() {
        return this.packetCount;
    }

    public int getPongCount() {
        return this.queue.size();
    }

    public void actuallySendImmediately(Packet<?> packet) {
        if (Minecraft.getInstance().getConnection() == null) return;

        synchronized (queue) { // Need to block, to make sure that other threads don't modify flushing
            this.flushing = true;
            ((IConnection) Minecraft.getInstance().getConnection().getConnection()).sendPacketImmediately(packet);
            this.flushing = false;
        }
    }

    public void actuallySend(Packet<?> packet) {
        if (Minecraft.getInstance().getConnection() == null) return;

        synchronized (queue) { // Need to block, to make sure that other threads don't modify flushing
            this.flushing = true;
            Minecraft.getInstance().getConnection().send(packet);
            this.flushing = true;
        }
    }

    @Override
    public void onEnable() {
        super.onEnable();
        ClientRotationHandler.registerProvider(this);
    }

    @Override
    public void onDisable() {
        super.onDisable();
        this.flushPongs();
        this.packetCount = 0;
    }

    public void flushPongs(int remainder) {
        if (Minecraft.getInstance().getConnection() == null) return;
        synchronized (queue) {
            if (remainder >= queue.size() || queue.isEmpty()) return;
            if (remainder <= 0) {
                flushPongs();
                return;
            }

            int sendCount = queue.size() - remainder;
            flushing = true;

            for (int i = 0; i < sendCount; i++) {
                Packet<?> packet = queue.removeFirst();
                ((IConnection) Minecraft.getInstance().getConnection().getConnection()).sendPacketImmediately(packet);
            }

            flushing = false;
        }
    }


    public void flushPongs() {
        if (Minecraft.getInstance().getConnection() == null) return;
        synchronized (queue) {
            flushing = true;
            if (queue.isEmpty()) {
                flushing = false;
                return;
            }
            queue.forEach(packet -> {
                ((IConnection) Minecraft.getInstance().getConnection().getConnection()).sendPacketImmediately(packet);
            });

            this.queue.clear();
            flushing = false;
        }
    }

    @Override
    public boolean isClientRotationActive() {
        return this.isEnabled();
    }

    @Override
    public void onDesyncDisable() {
        if (packetCount < maxBlinkPacket.getValue().intValue())
            ClientRotationHandler.syncServerRotationToClient();
    }

    @Override
    public void onDesyncPause() {
        ClientRotationHandler.syncServerRotationToClient();
    }


    @Override
    public boolean isDesyncPaused() {
        return Minecraft.getInstance().player == null || Minecraft.getInstance().player.input.getMoveVector().lengthSquared() != 0f || packetCount >= maxBlinkPacket.getValue().intValue();
    }

    @Override
    public boolean allowClientKeyInputs() {
        return true;
    }
}
