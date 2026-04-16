package com.ricedotwho.rsa.component.impl;

import com.google.common.collect.Streams;
import com.ricedotwho.rsa.module.impl.dungeon.boss.p3.autop3.AutoP3;
import com.ricedotwho.rsm.component.api.ModComponent;
import com.ricedotwho.rsm.event.api.SubscribeEvent;
import com.ricedotwho.rsm.event.impl.client.InputPollEvent;
import lombok.Getter;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.shapes.VoxelShape;

import java.util.stream.Stream;

public class Edge extends ModComponent {
    @Getter
    private static boolean edge = false;

    public Edge() {
        super("Edge");
    }

    public static void edge() {
        edge = true;
    }

    /// [from meteor](https://github.com/MeteorDevelopment/meteor-client/blob/master/src/main/java/meteordevelopment/meteorclient/systems/modules/movement/Parkour.java)
    @SubscribeEvent
    public void onInput(InputPollEvent event) {
        if (!edge || mc.player == null || !mc.player.onGround() || mc.options.keyJump.isDown()) return;
        if (mc.player.isShiftKeyDown() || mc.options.keyShift.isDown()) return;

        double dist = AutoP3.getEdgeDist().getDefaultValue().doubleValue();
        AABB box = mc.player.getBoundingBox();
        AABB adjustedBox = box.move(0, -0.5, 0).inflate(-dist, 0, -dist);
        Stream<VoxelShape> blockCollisions = Streams.stream(mc.level.getBlockCollisions(mc.player, adjustedBox));

        if (blockCollisions.findAny().isPresent()) return;

        edge = false;
        event.getInput().jump(true);
    }
}
