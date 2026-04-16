package com.ricedotwho.rsa.module.impl.render;

import com.ricedotwho.rsm.RSM;
import com.ricedotwho.rsm.component.impl.location.Island;
import com.ricedotwho.rsm.component.impl.location.Location;
import com.ricedotwho.rsm.module.api.Category;
import com.ricedotwho.rsm.module.api.ModuleInfo;
import com.ricedotwho.rsm.ui.clickgui.settings.impl.ModeSetting;
import com.ricedotwho.rsm.utils.Utils;
import lombok.Getter;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.boss.wither.WitherBoss;
import net.minecraft.world.entity.player.Player;

import java.util.Arrays;

@Getter
@ModuleInfo(aliases = "Hide", id = "HideEntity", category = Category.RENDER, isOverwrite = true)
public class HidePlayers extends com.ricedotwho.rsm.module.impl.render.HidePlayers {
    private final ModeSetting hitThroughMode = new ModeSetting("Hit Through", "Off", Arrays.asList("Off", "Dungeon & Kuudra", "Always"));

    public HidePlayers() {
        super();
        this.registerProperty(
                hitThroughMode
        );
    }

    public static boolean shouldHitThrough(Entity e) {
        HidePlayers hidePlayers = RSM.getModule(HidePlayers.class);
        if (hidePlayers == null || !hidePlayers.isEnabled()) return false;
        if (hidePlayers.getWither().getValue() && e instanceof WitherBoss wither && wither.getMaxHealth() == 300F) return true;
        return e instanceof Player && (hidePlayers.getHitThroughMode().getIndex() == 1 && Utils.equalsOneOf(Location.getArea(), Island.Dungeon, Island.Kuudra) || hidePlayers.getHitThroughMode().getIndex() == 2);
    }
}
