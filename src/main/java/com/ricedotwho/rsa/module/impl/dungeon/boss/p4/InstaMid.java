package com.ricedotwho.rsa.module.impl.dungeon.boss.p4;

import com.ricedotwho.rsa.RSA;
import com.ricedotwho.rsa.component.impl.Jump;
import com.ricedotwho.rsm.component.impl.location.Floor;
import com.ricedotwho.rsm.component.impl.location.Island;
import com.ricedotwho.rsm.component.impl.location.Location;
import com.ricedotwho.rsm.component.impl.map.handler.Dungeon;
import com.ricedotwho.rsm.data.Phase7;
import com.ricedotwho.rsm.event.api.SubscribeEvent;
import com.ricedotwho.rsm.event.impl.client.PacketEvent;
import com.ricedotwho.rsm.event.impl.game.ChatEvent;
import com.ricedotwho.rsm.event.impl.world.WorldEvent;
import com.ricedotwho.rsm.module.Module;
import com.ricedotwho.rsm.module.api.Category;
import com.ricedotwho.rsm.module.api.ModuleInfo;
import com.ricedotwho.rsm.ui.clickgui.settings.impl.NumberSetting;
import com.ricedotwho.rsm.utils.DungeonUtils;
import lombok.Getter;
import net.minecraft.ChatFormatting;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;
import net.minecraft.world.phys.Vec3;

@Getter
@ModuleInfo(aliases = "InstaMid", id = "InstaMid", category = Category.DUNGEONS)
public class InstaMid extends Module {

    private final NumberSetting millis = new NumberSetting("Millis", 6000, 7000, 6415, 5);

    private boolean startOnNextFlying = false;
    private int airTicks = 0;

    public InstaMid() {
        this.registerProperty(
                millis
        );
    }

    @Override
    public void reset() {
        startOnNextFlying = false;
        airTicks = 0;
    }

    @SubscribeEvent
    public void onLoad(WorldEvent.Load event) {
        reset();
    }

    @SubscribeEvent
    public void onPacketSend(PacketEvent.Send event) {
        if (!(event.getPacket() instanceof ServerboundMovePlayerPacket packet)
                || !Location.getArea().is(Island.Dungeon)
                || !(Location.getFloor() == Floor.F7 || Location.getFloor() == Floor.M7)
                || !Dungeon.isInBoss()
                || !DungeonUtils.isPhase(Phase7.P4)
                || !startOnNextFlying
                || packet.isOnGround()
                || !isOnPlatform()
        ) return;

        airTicks++;
        if (airTicks > 3) {
            startIMid();
        }
    }

    @SubscribeEvent
    public void onChat(ChatEvent.Chat event) {
        String unformatted = ChatFormatting.stripFormatting(event.getMessage().getString());
        if (!Location.getArea().is(Island.Dungeon)
                || !(Location.getFloor() == Floor.F7 || Location.getFloor() == Floor.M7)
                || !DungeonUtils.isPhase(Phase7.P4)
                || !"[BOSS] Necron: You went further than any human before, congratulations.".equals(unformatted)
                || !isOnPlatform()
                || mc.player == null
        ) return;

        if (mc.player.onGround()) {
            startOnNextFlying = true;
            Jump.jump();
        } else {
            startIMid();
        }
    }

    private void startIMid() {
        startOnNextFlying = false;
        RSA.chat("Attempting to InstaMid");
        //mc.doRunTask(this::freeze);
        freeze();
    }

    public void freeze() {
        try {
            Thread.sleep(this.millis.getValue().longValue());
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    private boolean isOnPlatform() {
        Vec3 pos = mc.player.position();
        return pos.y() > 63 && pos.y() < 100 && (Math.pow(Math.abs(pos.x() - 54.5), 2) + Math.pow(Math.abs(pos.z() - 76.5), 2)) < 56.25;
    }
}
