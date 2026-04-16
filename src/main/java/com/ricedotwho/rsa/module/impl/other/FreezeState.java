package com.ricedotwho.rsa.module.impl.other;

import com.ricedotwho.rsa.RSA;
import com.ricedotwho.rsm.data.Keybind;
import com.ricedotwho.rsm.event.api.SubscribeEvent;
import com.ricedotwho.rsm.event.impl.client.PacketEvent;
import com.ricedotwho.rsm.event.impl.game.ClientTickEvent;
import com.ricedotwho.rsm.event.impl.render.Render3DEvent;
import com.ricedotwho.rsm.module.Module;
import com.ricedotwho.rsm.module.api.Category;
import com.ricedotwho.rsm.module.api.ModuleInfo;
import com.ricedotwho.rsm.ui.clickgui.settings.impl.KeybindSetting;
import com.ricedotwho.rsm.ui.clickgui.settings.impl.NumberSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;
import net.minecraft.world.phys.Vec3;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;

@ModuleInfo(
        aliases = {"FreezeState"},
        id = "FreezeState",
        category = Category.OTHER
)
public class FreezeState extends Module {
    private static boolean frozen;
    // RSA.notInTestEnv; on servers = true, on singleplayer/p3sim = false
    private static final List<Vec3> PLAYER_POS = new ArrayList();
    private static int currentTick;
    private int aHolding = 0;
    private int rHolding  = 0;
    private static final int delay = 5;
    private static final int repeat = 3;
    private static final NumberSetting tickDelay = new NumberSetting("Amount of ticks", 1, 200, 20, 1);
    private static final KeybindSetting freezeKey = new KeybindSetting("Freeze FDKey", new Keybind(GLFW.GLFW_KEY_UNKNOWN, false, FreezeState::toggleFreeze));
    private static final KeybindSetting advanceTick = new KeybindSetting("Advance Tick Key", new Keybind(GLFW.GLFW_KEY_UNKNOWN, false, () -> {} ));
    private static final KeybindSetting rewindTick = new KeybindSetting("Rewind Tick Key", new Keybind(GLFW.GLFW_KEY_UNKNOWN, false, () -> {}));


    public FreezeState() {
        this.registerProperty(
                freezeKey,
                advanceTick,
                rewindTick,
                tickDelay
        );
    }

    @Override
    public void onEnable() {
        frozen = false;
    }

    @Override
    public void onDisable() {
        frozen = false;
    }

    private static void toggleFreeze(){
        if(RSA.notInTestEnv) return;
        currentTick = PLAYER_POS.size();
        frozen = !frozen;
    }

    @SubscribeEvent
    public void onTick(ClientTickEvent.Start event) {
        if (RSA.notInTestEnv) return;
        if (!frozen) return;
        if (Minecraft.getInstance().screen != null) return;
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null || PLAYER_POS.isEmpty()) return;

        boolean adv = advanceTick.getValue().isActive();
        boolean rew = rewindTick.getValue().isActive();

        if (adv) {
            if (aHolding == 0) {
                if (currentTick < PLAYER_POS.size() - 1) currentTick++;
            } else if (aHolding >= delay
                    && (aHolding - delay) % repeat == 0) {
                if (currentTick < PLAYER_POS.size() - 1) currentTick++;
            }
            aHolding++;
        } else {
            aHolding = 0;
        }

        if (rew) {
            if (rHolding == 0) {
                if (currentTick > 0) currentTick--;
            } else if (rHolding >= delay
                    && (rHolding - delay) % repeat == 0) {
                if (currentTick > 0) currentTick--;
            }
            rHolding++;
        } else {
            rHolding = 0;
        }
    }

    @SubscribeEvent
    public void PosSet(Render3DEvent.Extract event) {
        if(RSA.notInTestEnv) return;
        if(frozen) {
            LocalPlayer player = Minecraft.getInstance().player;
            if(player == null) return;
            player.setSpeed(0.0f); player.setDeltaMovement(0, 0, 0);
            if(currentTick >= PLAYER_POS.size()) return;
            if(PLAYER_POS.isEmpty()) return;
            try{player.setPos(PLAYER_POS.get(currentTick).x, PLAYER_POS.get(currentTick).y, PLAYER_POS.get(currentTick).z);}
            catch(Exception ignored){}
        }
    }

    @SubscribeEvent
    public void capturePos(PacketEvent.Send event) {
        if(frozen) return;
        ClientLevel level = Minecraft.getInstance().level;
        LocalPlayer player = Minecraft.getInstance().player;
        if(player == null || level == null) return;
        Packet var3 = event.getPacket();
        if (var3 instanceof ServerboundMovePlayerPacket packet) {
            if (packet.hasPosition()) {
                    PLAYER_POS.add(new Vec3(player.getX(), player.getY(), player.getZ()));
                    if(PLAYER_POS.size() > tickDelay.getValue().intValue()) PLAYER_POS.removeFirst();
            }
        }
    }
}
