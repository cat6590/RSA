package com.ricedotwho.rsa.module.impl.dungeon.boss.p2;

import com.ricedotwho.rsa.RSA;
import com.ricedotwho.rsm.component.impl.location.Island;
import com.ricedotwho.rsm.component.impl.location.Location;
import com.ricedotwho.rsm.data.Colour;
import com.ricedotwho.rsm.data.Phase7;
import com.ricedotwho.rsm.event.api.SubscribeEvent;
import com.ricedotwho.rsm.event.impl.game.ChatEvent;
import com.ricedotwho.rsm.event.impl.game.ServerTickEvent;
import com.ricedotwho.rsm.event.impl.render.Render2DEvent;
import com.ricedotwho.rsm.event.impl.world.WorldEvent;
import com.ricedotwho.rsm.module.Module;
import com.ricedotwho.rsm.module.api.Category;
import com.ricedotwho.rsm.module.api.ModuleInfo;
import com.ricedotwho.rsm.ui.clickgui.settings.impl.BooleanSetting;
import com.ricedotwho.rsm.ui.clickgui.settings.impl.ButtonSetting;
import com.ricedotwho.rsm.ui.clickgui.settings.impl.DragSetting;
import com.ricedotwho.rsm.utils.DungeonUtils;
import com.ricedotwho.rsm.utils.render.render2d.NVGUtils;
import lombok.Getter;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.util.StringUtil;
import org.joml.Vector2d;

@Getter
@ModuleInfo(aliases = "Pad Timer", id = "PadTimer", category = Category.DUNGEONS)
public class PadTimer extends Module {
    private int seconds = 4; // 4 seconds
    private int second = 20; // 20 ticks
    private int padSeconds = 4;// 4 seconds
    private boolean IsEnabled = false;
    private boolean pPadcountdown = false;
    private boolean countdownP = false;
    private boolean pPadmsg = false;
    private int stopShowing = 44;
    private int stopShowing2 = 1;

    private int pPadTicks = 80; // 80 ticks [4 seconds]
    private int yPadTicks = 48;// 40 ticks [2 seconds]
    private final ButtonSetting rsvalues = new ButtonSetting("Restart Values", "restartvalues", this::reset);
    private final BooleanSetting debug = new BooleanSetting("Debug", false, () -> true);
    private final DragSetting padAlert = new DragSetting("Pad Alert", new Vector2d(0, 0), new Vector2d(0, 0));

    String string = "Pad in " + seconds;

    public PadTimer() {
        this.registerProperty(
                padAlert,
                rsvalues,
                debug
        );
    }

    @Override
    public void onEnable() {
        IsEnabled = true;
    }

    @Override
    public void onDisable() {
        IsEnabled = false;
    }

    @Override
    public void reset() {
        seconds = 4;// 4 seconds
        second = 20;// 20 ticks
        padSeconds = 4;// 4 seconds
        IsEnabled = false;
        pPadcountdown = false;
        countdownP = false;
        pPadmsg = false;
        stopShowing = 44;
        stopShowing2 = 1;
        pPadTicks = 80;// 80 ticks [4 seconds]
        yPadTicks = 48;// 40 ticks [2 seconds]
    }

    @SubscribeEvent
    public void onWorldLoad(WorldEvent.Load event) {
        reset();
    }

    @SubscribeEvent
    public void onChat(ChatEvent.Chat event) {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null) return;
        String unformatted = StringUtil.stripColor(event.getMessage().getString());

        // todo: get the exact message so people cant trigger this by typing it
        if (unformatted.contains("I'd be happy to show you what that's like!") && Location.getArea() == Island.Dungeon && DungeonUtils.isPhase(Phase7.P2) || debug.getValue()){
            pPadcountdown = true;
            pPadmsg = true;
            IsEnabled = true;
            RSA.chat("Pad Countdown Started.");
        }
    }

    @SubscribeEvent
    public void onTick(ServerTickEvent event) {
        if (Location.getArea() == Island.Dungeon && DungeonUtils.isPhase(Phase7.P2) && IsEnabled || debug.getValue()) {
            if (pPadcountdown) {
                countdownP = true;
            }
            if (padSeconds <= 0) countdownP = false;

            if (second > 0 && pPadTicks <= 0 && countdownP) {
                second--;
                if (second == 0) {
                    RSA.chat("PAD IN: " + padSeconds);
                    padSeconds--;
                }
                if (padSeconds <= 0) countdownP = false;
                return;
            }
            seconds--;
            if (seconds <= 0 && pPadcountdown) {
                second = 20;
            }

            if (stopShowing > 0 && pPadTicks <= 0) {
                stopShowing--;
                return;
            }

            if (pPadTicks > 0 && pPadcountdown && countdownP) {
                pPadTicks--;
                return;
            }

            if (pPadTicks == 0) {
                pPadTicks = 1;
            }

            pPadcountdown = false;
        }
    }

    @SubscribeEvent
    public void onRender2D(Render2DEvent event) {
        if (padSeconds <= 0 && stopShowing > stopShowing2 && Location.getArea() == Island.Dungeon) {
            this.padAlert.renderScaled(event.getGfx(), () -> NVGUtils.drawText("Pad Now", 0, 0, 50f, Colour.blue, NVGUtils.JOSEFIN), 60, 30);
        }
    }
}