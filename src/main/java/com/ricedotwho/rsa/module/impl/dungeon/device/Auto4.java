package com.ricedotwho.rsa.module.impl.dungeon.device;

import com.ricedotwho.rsa.RSA;
import com.ricedotwho.rsa.component.impl.managers.PacketOrderManager;
import com.ricedotwho.rsa.component.impl.managers.SwapManager;
import com.ricedotwho.rsa.module.impl.dungeon.FastLeap;
import com.ricedotwho.rsm.component.impl.location.Island;
import com.ricedotwho.rsm.component.impl.location.Location;
import com.ricedotwho.rsm.component.impl.map.handler.Dungeon;
import com.ricedotwho.rsm.component.impl.task.TaskComponent;
import com.ricedotwho.rsm.data.DungeonPlayer;
import com.ricedotwho.rsm.data.Pos;
import com.ricedotwho.rsm.data.Rotation;
import com.ricedotwho.rsm.event.api.SubscribeEvent;
import com.ricedotwho.rsm.event.impl.game.ChatEvent;
import com.ricedotwho.rsm.event.impl.world.BlockChangeEvent;
import com.ricedotwho.rsm.module.Module;
import com.ricedotwho.rsm.module.api.Category;
import com.ricedotwho.rsm.module.api.ModuleInfo;
import com.ricedotwho.rsm.ui.clickgui.settings.impl.BooleanSetting;
import com.ricedotwho.rsm.ui.clickgui.settings.impl.ModeSetting;
import com.ricedotwho.rsm.ui.clickgui.settings.impl.NumberSetting;
import com.ricedotwho.rsm.ui.clickgui.settings.impl.StringSetting;
import com.ricedotwho.rsm.utils.ItemUtils;
import lombok.Getter;
import net.minecraft.ChatFormatting;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;

@Getter
@ModuleInfo(aliases = "Auto4", id = "Auto4", category = Category.DUNGEONS)
public class Auto4 extends Module {
    private final NumberSetting delay = new NumberSetting("Delay", 0, 400, 250, 10);
    private final BooleanSetting auto = new BooleanSetting("Auto", true);
    private final BooleanSetting leap = new BooleanSetting("Leap out", false);
    private final ModeSetting who = new ModeSetting("Target", "Tank", Arrays.asList("Archer", "Mage", "Berserk", "Healer", "Tank", "Custom"));
    private final StringSetting custom = new StringSetting("Custom", "", true, false, () -> who.is("Custom"));

    private final List<Integer> done = new ArrayList<>();
    private static final List<Pos> blocks = Arrays.asList(
            new Pos(68, 130, 50), new Pos(66, 130, 50), new Pos(64, 130, 50),
            new Pos(68, 128, 50), new Pos(66, 128, 50), new Pos(64, 128, 50),
            new Pos(68, 126, 50), new Pos(66, 126, 50), new Pos(64, 126, 50)

    );

    private long lastShot = 0;

    public Auto4() {
        this.registerProperty(
                delay,
                auto,
                leap,
                who,
                custom
        );
    }

    @SubscribeEvent
    public void onBlockChange(BlockChangeEvent event) {
        if (!Location.getArea().is(Island.Dungeon) || mc.player == null || !on4thDev() || !isHoldingBow() || !auto.getValue()) return;
        Pos pos = event.getPos();
        int index = blocks.indexOf(pos);
        if (index == -1) return;

        if (event.getNewState().is(Blocks.BLUE_TERRACOTTA)) {
            done.add(index);
        }

        if (!event.getNewState().is(Blocks.EMERALD_BLOCK)) return;

        long now = System.currentTimeMillis();
        long delay = this.delay.getValue().longValue() - (now - lastShot);

        Rotation rotation = calculateAim(
                event.getPos(),
                index,
                "TERMINATOR".equals(ItemUtils.getID(mc.player.getInventory().getSelectedItem()))
        );

        TaskComponent.onMilli(delay, () -> {
            if (isHoldingBow()) {
                PacketOrderManager.register(PacketOrderManager.STATE.ITEM_USE, () -> {
                    SwapManager.sendAirC08(rotation, false);
                    lastShot = System.currentTimeMillis();
                });
            }
        });
    }

    private boolean isHoldingBow() {
        return mc.player.getInventory().getSelectedItem().getItem().equals(Items.BOW);
    }

    private boolean on4thDev() {
        Vec3 pos = mc.player.position();
        return pos.x() > 63 && pos.x() < 64
                && pos.y() == 127
                && pos.z() > 35 && pos.z() < 36;
    }

    private Rotation calculateAim(Pos pos, int index, boolean term) {
        Pos target = pos.copy();

        if (!term) return Rotation.from(target.add(0.5, 1, 0).asVec3());

        switch (index % 3) {
            case 0:
                target.selfAdd(
                        -0.5,
                        1,
                        0
                );
                break;
            case 1:
                boolean f1 = this.done.contains(index - 1), f2 = this.done.contains(index + 1);
                // This if statement is a crime against humanity -Hyper
                if (f1 && !f2) {
                    target.selfAdd(-0.5, 1, 0);
                } else if (f2 && !f1) {
                    target.selfAdd(1.5, 1, 0);
                } else {
                    // ??????
                    target.selfAdd(0.5 + (Math.random() < 0.5 ? -1 : 1), 1, 0);
                }
                break;
            case 2:
                target.selfAdd(
                        1.5,
                        1,
                        0
                );
                break;
            default:
                target.selfAdd(
                        0.5,
                        1,
                        0
                );
        }
        return Rotation.from(target.asVec3());
    }

    @SubscribeEvent
    public void onChat(ChatEvent.Chat event) {
        if (!Location.getArea().is(Island.Dungeon) || mc.player == null || !on4thDev() || !leap.getValue()) return;
        String text = ChatFormatting.stripFormatting(event.getMessage().getString());
        Matcher m = Dungeon.TERM.matcher(text);
        if (m.find() && m.group(1).contains(mc.player.getName().getString()) && m.group(2).contains("device")) {
            if (SwapManager.swapItem("SPIRIT_LEAP", "INFINITE_SPIRIT_LEAP")) {
                String leap = getLeap();
                if (leap == null) {
                    RSA.chat("Failed to find i4 leap player!");
                    return;
                }
                FastLeap.doLeap(leap);
            }
        }
    }

    private String getLeap() {
        if (this.who.is("Custom")) {
            return this.custom.getValue();
        }
        DungeonPlayer dp = Dungeon.getClazz(this.who.getIndex());
        if (dp == null) return null;
        return dp.getName();
    }
}
