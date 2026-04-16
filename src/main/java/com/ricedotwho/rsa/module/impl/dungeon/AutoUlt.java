package com.ricedotwho.rsa.module.impl.dungeon;

import com.ricedotwho.rsm.component.impl.location.Island;
import com.ricedotwho.rsm.component.impl.location.Location;
import com.ricedotwho.rsm.component.impl.map.handler.Dungeon;
import com.ricedotwho.rsm.component.impl.task.TaskComponent;
import com.ricedotwho.rsm.data.DungeonClass;
import com.ricedotwho.rsm.event.api.SubscribeEvent;
import com.ricedotwho.rsm.event.impl.game.ChatEvent;
import com.ricedotwho.rsm.module.api.Category;
import com.ricedotwho.rsm.module.api.ModuleInfo;
import com.ricedotwho.rsm.module.impl.dungeon.Abilities;
import com.ricedotwho.rsm.ui.clickgui.settings.impl.BooleanSetting;
import com.ricedotwho.rsm.ui.clickgui.settings.impl.NumberSetting;
import lombok.Getter;
import net.minecraft.ChatFormatting;

import java.util.regex.Pattern;

@Getter
@ModuleInfo(aliases = "Abilities", id = "Abilities", category = Category.DUNGEONS, isOverwrite = true)
public class AutoUlt extends Abilities {
    private final BooleanSetting autoUlt = new BooleanSetting("Auto Ult", false);
    private final NumberSetting tankUltDelay = new NumberSetting("Tank Delay", 0, 40, 15, 1);
    private final NumberSetting healerUltDelay = new NumberSetting("Healer Delay", 0, 40, 3, 1);
    private final BooleanSetting wishCommand = new BooleanSetting("Wish Chat Command", false);

    private static final Pattern wishPattern = Pattern.compile("Party > (?:\\[(.*?)] )?(.+?): !wish");

    public AutoUlt() {
        super();
        this.registerProperty(
                autoUlt,
                tankUltDelay,
                healerUltDelay,
                wishCommand
        );
    }

    @SubscribeEvent
    public void onChat(ChatEvent.Chat event) {
        if (!Location.getArea().is(Island.Dungeon) || !Dungeon.isStarted()) return;
        String value = ChatFormatting.stripFormatting(event.getMessage().getString());
        if (wishCommand.getValue() && wishPattern.matcher(value).find()) {
            drop(false);
            return;
        }

        switch (Location.getFloor()) {
            case F6, M6 -> {
                if ("[BOSS] Sadan: My giants! Unleashed!".equals(value)) drop(false);
            }
            case F7, M7 -> {
                if (Dungeon.isMyClass(DungeonClass.TANK) && "[BOSS] Maxor: DON'T DISAPPOINT ME, I HAVEN'T HAD A GOOD FIGHT IN A WHILE.".equals(value)) {
                    useUlt(tankUltDelay.getValue().longValue());
                }
                else if (Dungeon.isMyClass(DungeonClass.HEALER) && ("⚠ Maxor is enraged! ⚠".equals(value) || "[BOSS] Goldor: You have done it, you destroyed the factory…".equals(value))) {
                    useUlt(healerUltDelay.getValue().longValue());
                }
            }
        }
    }

    private void useUlt(long delay) {
        TaskComponent.onServerTick(delay, () -> drop(false));
    }
}
