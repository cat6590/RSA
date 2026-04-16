package com.ricedotwho.rsa.module.impl.dungeon.boss.p3.autop3;

import com.mojang.datafixers.util.Function5;
import com.ricedotwho.rsa.module.impl.dungeon.boss.p3.autop3.args.ArgumentManager;
import com.ricedotwho.rsa.module.impl.dungeon.boss.p3.autop3.rings.*;
import com.ricedotwho.rsa.module.impl.dungeon.boss.p3.autop3.subactions.SubActionManager;
import com.ricedotwho.rsm.data.Pos;
import lombok.Getter;
import net.minecraft.client.Minecraft;
import net.minecraft.world.phys.HitResult;

import java.util.Arrays;
import java.util.Map;
import java.util.Set;

public enum RingType {
    ALIGN("align", AlignRing::new, 0f, Set.of(), null),
    FAST_ALIGN("fastalign", FastAlign::new, 0.01f, Set.of(), null),
    STOP("stop", StopRing::new, 0.02f, Set.of(), null),
    WALK("walk", WalkRing::new, 0.03f, Set.of(), null),
    JUMP("jump", JumpRing::new, 0.04f, Set.of(), null),
    BONZO("bonzo", BonzoRing::new, 0.05f, Set.of(), null),
    FAST_BONZO("fastbonzo", FastBonzoRing::new, 0.06f, Set.of(), null),
    EDGE("edge", EdgeRing::new, 0.06f, Set.of(), null),
    MOVEMENT("movement", MovementRing::new, 0.07f, Set.of("route"), null),
    LOOK("look", LookRing::new, 0.08f, Set.of(), null),
    BOOM("boom", BoomRing::new, 0.009f, Set.of(), HitResult.Type.BLOCK),
    LEAP("leap", LeapRing::new, 0.010f, Set.of(), null),
    USE("use", UseRing::new, 0.011f, Set.of(), null),
    CHAT("chat", ChatRing::new, 0.012f, Set.of("message"), null),
    COMMAND("command", CommandRing::new, 0.013f, Set.of("command"), null),
    BLINK("blink", BlinkRing::new, 0.014f, Set.of(), null),
    PET("pet", PetRing::new, 0.015f, Set.of("uuid"), null),
    STOPWATCH("stopwatch", StopWatchRing::new, 0.016f, Set.of(), null),
    BONZO2("bonzo2", BonzoRing2::new, 0.016f, Set.of(), null);


    @Getter
    private final String name;
    private final Function5<Pos, Pos, ArgumentManager, SubActionManager, Map<String, Object>, Ring> factory;
    @Getter
    private final float renderSizeOffset;
    @Getter
    private final Set<String> required;
    @Getter
    private final HitResult.Type hitResult;

    RingType(String s, Function5<Pos, Pos, ArgumentManager, SubActionManager, Map<String, Object>, Ring> factory, float renderSizeOffset, Set<String> required, HitResult.Type hitResult) {
        this.name = s;
        this.renderSizeOffset = renderSizeOffset;
        this.factory = factory;
        this.required = required;
        this.hitResult = hitResult;
    }

    public Ring supply(Pos min, Pos max, ArgumentManager manager, SubActionManager actions, Map<String, Object> extraData) {
        if (this.factory == null || Minecraft.getInstance().player == null) return null;
        return this.factory.apply(min, max, manager, actions, extraData);
    }

    public static RingType byName(String name) {
        return Arrays.stream(RingType.values()).filter(n -> n.getName().equalsIgnoreCase(name)).findAny().orElse(null);
    }
}
